package com.dawoyan.roomik.core

import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A room found in the wall graph: a closed loop of walls. [polygon] runs along wall centre lines, [inner] along the
 * inside faces of the walls; [area] is the clear floor area in m².
 */
class Room(
    val nodeIds: List<Int>,
    val polygon: List<P>,
    val inner: List<P>,
    val area: Double,
    /** A point safely inside the room; ties [info] to it. */
    val anchor: P,
    val info: RoomInfo?,
) {
    /** Clear perimeter in metres. */
    val perimeter: Double get() = inner.indices.sumOf { (inner[(it + 1) % inner.size] - inner[it]).len } / 1000.0
}

object Rooms {
    private fun key(a: Int, b: Int): Long = a.toLong() shl 32 or (b.toLong() and 0xffffffffL)

    fun detect(plan: Plan): List<Room> {
        val pos = plan.nodes.associate { it.id to P(it.x.toDouble(), it.y.toDouble()) }
        var walls = plan.walls
            .filter { it.a != it.b && it.a in pos && it.b in pos }
            .distinctBy { key(minOf(it.a, it.b), maxOf(it.a, it.b)) }
        // Dead-end walls (partitions, half-drawn chains) bound no room.
        while (true) {
            val deg = HashMap<Int, Int>()
            walls.forEach { deg[it.a] = (deg[it.a] ?: 0) + 1; deg[it.b] = (deg[it.b] ?: 0) + 1 }
            val kept = walls.filter { deg[it.a]!! > 1 && deg[it.b]!! > 1 }
            if (kept.size == walls.size) break
            walls = kept
        }
        val thick = HashMap<Long, Int>()
        val out = HashMap<Int, MutableList<Int>>()
        for (w in walls) {
            thick[key(w.a, w.b)] = w.thickness
            thick[key(w.b, w.a)] = w.thickness
            out.getOrPut(w.a) { mutableListOf() } += w.b
            out.getOrPut(w.b) { mutableListOf() } += w.a
        }
        for ((n, l) in out) {
            val p = pos[n]!!
            l.sortBy { atan2(pos[it]!!.y - p.y, pos[it]!!.x - p.x) }
        }
        val seen = HashSet<Long>()
        val free = plan.rooms.toMutableList()
        val rooms = ArrayList<Room>()
        for (w in walls) for ((u, v) in listOf(w.a to w.b, w.b to w.a)) {
            if (key(u, v) in seen) continue
            val loop = ArrayList<Int>()
            var a = u
            var b = v
            while (seen.add(key(a, b))) {
                loop += a
                val l = out[b]!!
                val nx = l[(l.indexOf(a) - 1 + l.size) % l.size]
                a = b
                b = nx
            }
            val poly = loop.map { pos[it]!! }
            if (loop.size < 3 || Geo.area(poly) <= 1.0) continue // the outside face runs the other way round
            val inner = inset(poly, loop.indices.map { thick[key(loop[it], loop[(it + 1) % loop.size])]!! / 2.0 })
            val info = free.firstOrNull { Geo.inside(P(it.x.toDouble(), it.y.toDouble()), poly) }?.also { free.remove(it) }
            rooms += Room(loop, poly, inner, max(0.0, Geo.area(inner)) / 1e6, interiorPoint(poly), info)
        }
        return rooms
    }

    /** The polygon moved inwards by [offsets] (one per edge, edge i runs from vertex i to i+1). */
    private fun inset(poly: List<P>, offsets: List<Double>): List<P> {
        val n = poly.size
        return List(n) { i ->
            val pi = (i - 1 + n) % n
            val d0 = (poly[i] - poly[pi]).norm()
            val d1 = (poly[(i + 1) % n] - poly[i]).norm()
            val p0 = poly[i] + d0.left() * offsets[pi]
            val p1 = poly[i] + d1.left() * offsets[i]
            val x = Geo.lineIntersect(p0, d0, p1, d1, 0.05)
            val limit = 4 * max(offsets[pi], offsets[i]) + 1
            if (x != null && (x - poly[i]).len <= limit) x else (p0 + p1) * 0.5
        }
    }

    private fun interiorPoint(poly: List<P>): P {
        val t = Geo.triangulate(poly).maxByOrNull {
            kotlin.math.abs(cross(poly[it[1]] - poly[it[0]], poly[it[2]] - poly[it[0]]))
        } ?: return poly[0]
        return (poly[t[0]] + poly[t[1]] + poly[t[2]]) * (1.0 / 3)
    }

    /**
     * Gives every room its attributes with a lasting id (elektronik knows the room by it), drops those of rooms that are
     * gone and moves the rest to a point inside their room. A room split in two keeps its id in one half.
     */
    fun rematch(plan: Plan): Plan {
        val used = plan.rooms.map { it.id }.toMutableSet()
        var next = (used.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0) + 1
        val infos = detect(plan).map { r ->
            val info = r.info?.takeIf { it.id.isNotEmpty() } ?: (r.info ?: RoomInfo(0, 0)).copy(id = (next++).toString().also { used += it })
            info.copy(x = r.anchor.x.roundToInt(), y = r.anchor.y.roundToInt())
        }
        return plan.copy(rooms = infos)
    }
}
