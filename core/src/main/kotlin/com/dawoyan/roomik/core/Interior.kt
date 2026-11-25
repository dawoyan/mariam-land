package com.dawoyan.roomik.core

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Furniture the user can place: default size in mm and colour (0xRRGGBB). Names are in the app's resources. */
class CatalogItem(val id: String, val w: Int, val d: Int, val h: Int, val color: Int, val z: Int = 0)

/** The furniture id stairs had in 0.5.0; such items become [Stair]s when a plan is opened. */
const val STAIRS = "stairs"

object Catalog {
    val all = listOf(
        CatalogItem("bed", 1600, 2000, 500, 0xB9C7E0), CatalogItem("bed1", 900, 2000, 500, 0xB9C7E0),
        CatalogItem("sofa", 2000, 900, 800, 0xC9B49A), CatalogItem("armchair", 850, 850, 800, 0xC9B49A),
        CatalogItem("table", 1200, 800, 750, 0xB98E63), CatalogItem("chair", 450, 450, 450, 0xA67C52),
        CatalogItem("desk", 1200, 600, 750, 0xB98E63), CatalogItem("wardrobe", 1200, 600, 2200, 0xD8C6A8),
        CatalogItem("tv", 1500, 400, 500, 0x6E6E78), CatalogItem("cabinet", 600, 600, 850, 0xE3DCCF),
        CatalogItem("fridge", 600, 650, 1850, 0xDfE4E8), CatalogItem("stove", 600, 600, 850, 0x8A8F98),
        CatalogItem("sink", 600, 500, 850, 0xBFD8E6), CatalogItem("washer", 600, 550, 850, 0xE8ECEF),
        CatalogItem("bath", 1700, 750, 600, 0xCFE6F2), CatalogItem("toilet", 400, 650, 400, 0xEEF2F4),
        CatalogItem("vacuum", 350, 400, 1000, 0xA9494D), CatalogItem("robot_vacuum", 350, 350, 100, 0x555C67),
        CatalogItem("car", 1800, 4500, 1450, 0x3B6EA5),
        CatalogItem("fireplace", 1300, 550, 1100, 0xA0523D),
        CatalogItem("radiator", 800, 100, 600, 0xECEFF1),
        CatalogItem("ac_out", 850, 320, 600, 0xE8E8E6, z = 1800), CatalogItem("ac_in", 900, 250, 300, 0xF4F4F2, z = 2200),
        CatalogItem("chimney", 600, 600, 4500, 0x6E4A3A),
        // Network equipment: active gear, termination and typical endpoints. They remain ordinary editable plan items;
        // LAN routes themselves are drawn with the Network system.
        CatalogItem("router", 300, 220, 60, 0x26384A), CatalogItem("network_switch", 440, 220, 45, 0x26384A),
        CatalogItem("access_point", 220, 220, 45, 0xF5F5F0), CatalogItem("modem_ont", 220, 160, 50, 0xF5F5F0),
        CatalogItem("patch_panel", 480, 80, 45, 0x313942), CatalogItem("network_rack", 600, 600, 1200, 0x313942),
        CatalogItem("server_nas", 450, 400, 180, 0x38434D), CatalogItem("nvr", 400, 300, 80, 0x38434D),
        CatalogItem("ip_camera", 150, 180, 120, 0xE8ECEF, z = 2400), CatalogItem("network_outlet", 100, 60, 100, 0xF5F5F5, z = 300),
    )
    fun of(id: String): CatalogItem = all.firstOrNull { it.id == id } ?: all[0]

    /** Groups the catalogue is shown in: furniture, kitchen, bathroom, heating (indoors); the car goes with the yard. */
    enum class Group(val ids: List<String>) {
        FURNITURE(listOf("sofa", "armchair", "bed", "bed1", "table", "chair", "desk", "wardrobe", "tv", "cabinet", "vacuum", "robot_vacuum")),
        KITCHEN(listOf("cabinet", "sink", "stove", "fridge", "washer", "table", "chair")),
        BATHROOM(listOf("bath", "toilet", "sink", "washer", "cabinet")),
        HEATING(listOf("radiator", "fireplace", "chimney", "ac_in", "ac_out")),
        NETWORK(listOf("router", "network_switch", "access_point", "modem_ont", "patch_panel", "network_rack", "server_nas", "nvr", "ip_camera", "network_outlet")),
    }

    /** What stands outside: shown with the yard constructions. */
    val outdoor = listOf("car", "ac_out")

    /** What usually stands in a room of [type], first; the rest after. */
    fun forRoom(type: com.dawoyan.elektronik.core.RoomType?): List<CatalogItem> {
        val first = when (type) {
            com.dawoyan.elektronik.core.RoomType.LIVING -> listOf("sofa", "armchair", "tv", "fireplace", "table", "chair")
            com.dawoyan.elektronik.core.RoomType.BEDROOM -> listOf("bed", "wardrobe", "bed1", "desk", "chair", "armchair", "tv")
            com.dawoyan.elektronik.core.RoomType.CHILD -> listOf("bed1", "desk", "chair", "wardrobe", "armchair")
            com.dawoyan.elektronik.core.RoomType.OFFICE -> listOf("desk", "chair", "wardrobe", "armchair", "sofa")
            com.dawoyan.elektronik.core.RoomType.KITCHEN -> listOf("cabinet", "sink", "stove", "fridge", "table", "chair", "washer")
            com.dawoyan.elektronik.core.RoomType.BATHROOM -> listOf("bath", "toilet", "sink", "washer", "cabinet")
            com.dawoyan.elektronik.core.RoomType.TOILET -> listOf("toilet", "sink")
            com.dawoyan.elektronik.core.RoomType.HALL -> listOf("wardrobe", "cabinet", "chair")
            com.dawoyan.elektronik.core.RoomType.GARAGE -> listOf("car", "cabinet", "wardrobe", "table")
            com.dawoyan.elektronik.core.RoomType.WORKSHOP -> listOf("table", "cabinet", "wardrobe", "chair")
            com.dawoyan.elektronik.core.RoomType.STORAGE -> listOf("wardrobe", "cabinet", "washer")
            com.dawoyan.elektronik.core.RoomType.BALCONY, com.dawoyan.elektronik.core.RoomType.YARD -> listOf("chair", "table", "armchair")
            com.dawoyan.elektronik.core.RoomType.BASEMENT -> listOf("cabinet", "wardrobe", "washer")
            else -> emptyList()
        }
        return first.mapNotNull { id -> all.firstOrNull { it.id == id } } + all.filter { it.id !in first }
    }
}

/** Where a wall point really is: on the wall face, with the wall's direction. */
class PlacedPoint(val point: ElPoint, val face: P, val normal: P)

/** An opening on its wall: from [t0] to [t1] mm along the wall, measured from the wall's end a. */
class PlacedOpening(val opening: Opening, val wall: Wall, val t0: Double, val t1: Double)

object Interior {
    /** Openings by wall id, in order along the wall; ones that do not fit or overlap an earlier one are left out. */
    fun openings(plan: Plan): Map<Int, List<PlacedOpening>> {
        val out = HashMap<Int, MutableList<PlacedOpening>>()
        for (o in plan.openings) {
            val c = P(o.x.toDouble(), o.y.toDouble())
            val wall = plan.walls.minByOrNull { w ->
                val a = plan.pos(w.a); val b = plan.pos(w.b)
                if (a == null || b == null) Double.MAX_VALUE else Geo.distToSegment(c, a, b)
            } ?: continue
            val a = plan.pos(wall.a) ?: continue
            val b = plan.pos(wall.b) ?: continue
            if (Geo.distToSegment(c, a, b) > 5) continue
            val len = (b - a).len
            val tc = dot(c - a, (b - a).norm())
            val t0 = (tc - o.width / 2.0).coerceAtLeast(50.0)
            val t1 = (tc + o.width / 2.0).coerceAtMost(len - 50.0)
            if (t1 - t0 < 200) continue
            out.getOrPut(wall.id) { mutableListOf() } += PlacedOpening(o, wall, t0, t1)
        }
        for ((id, l) in out) {
            l.sortBy { it.t0 }
            val kept = ArrayList<PlacedOpening>()
            for (p in l) if (kept.isEmpty() || p.t0 >= kept.last().t1 + 50) kept += p
            out[id] = kept
        }
        return out
    }

    /** True when one can walk straight from [from] to [to]: no wall in the way, except through a door. */
    fun canWalk(plan: Plan, from: P, to: P): Boolean {
        val doors = openings(plan)
        for (w in plan.walls) {
            val a = plan.pos(w.a) ?: continue
            val b = plan.pos(w.b) ?: continue
            val x = Geo.intersect(from, to, a, b) ?: continue
            val t = (x - a).len
            if (doors[w.id].orEmpty().none { it.opening.kind != OpeningKind.WINDOW && t > it.t0 + 100 && t < it.t1 - 100 }) return false
        }
        return true
    }

    /** Corners of an item on the plan, running so that the inside is on the left of each edge. */
    fun corners(i: Item): List<P> {
        val r = Math.toRadians(i.rot.toDouble())
        val u = P(cos(r), sin(r))
        val v = u.left()
        val c = P(i.x.toDouble(), i.y.toDouble())
        val hw = i.w / 2.0; val hd = i.d / 2.0
        return listOf(c - u * hw - v * hd, c + u * hw - v * hd, c + u * hw + v * hd, c - u * hw + v * hd)
    }

    /**
     * Move along [axis] that takes [a] off [b], towards the side [a]'s middle is on (so a chair half in a wall goes back
     * into the room, not through it); null when they do not overlap on it.
     */
    private fun overlap(a: List<P>, b: List<P>, axis: P): Double? {
        val pa = a.map { dot(it, axis) }; val pb = b.map { dot(it, axis) }
        if (minOf(pa.max(), pb.max()) - maxOf(pa.min(), pb.min()) <= 0.5) return null
        return if (pa.average() < pb.average()) pb.min() - pa.max() else pb.max() - pa.min()
    }

    /** True when convex [a] and convex [b] overlap. */
    fun overlaps(a: List<P>, b: List<P>): Boolean = push(a, b) != null

    /** The move that takes convex [a] off convex [b] along the shortest way, or null when they do not overlap. */
    private fun push(a: List<P>, b: List<P>): P? {
        var best: P? = null
        for (poly in listOf(a, b)) for (i in poly.indices) {
            val e = poly[(i + 1) % poly.size] - poly[i]
            if (e.len < 1e-6) continue
            val axis = e.norm().left()
            val o = overlap(a, b, axis) ?: return null
            if (best == null || kotlin.math.abs(o) < best.len) best = axis * o
        }
        return best
    }

    /**
     * [item] moved off every wall it overlaps, the shortest way, so furniture stays in its room and slides along walls
     * while it is dragged. Doorways are not walls: a gap cut by a door or window is kept out of the wall's body.
     */
    fun keepOffWalls(plan: Plan, item: Item, bodies: List<List<P>> = wallBodies(plan)): Item {
        var cur = item
        repeat(12) {
            var moved = false
            for (b in bodies) {
                val m = push(corners(cur), b) ?: continue
                val step = m + m.norm() * 1.0
                cur = cur.copy(x = (cur.x + step.x).roundToInt(), y = (cur.y + step.y).roundToInt())
                moved = true
            }
            if (!moved) return cur
        }
        return cur
    }

    /** Wall bodies as convex pieces: each wall's mitred outline, cut at its doors (windows keep the wall below). */
    fun wallBodies(plan: Plan): List<List<P>> {
        val cuts = openings(plan)
        val out = ArrayList<List<P>>()
        for (s in WallShapes.build(plan)) {
            val d = (s.b - s.a).norm(); val n = d.left(); val ht = s.wall.thickness / 2.0
            val gaps = cuts[s.wall.id].orEmpty().filter { it.opening.kind != OpeningKind.WINDOW }
            if (gaps.isEmpty()) { out += s.outline; continue }
            var from = listOf(s.aR, s.a, s.aL)
            for (g in gaps) {
                val p0 = s.a + d * g.t0
                out += from + listOf(p0 + n * ht, p0 - n * ht)
                val p1 = s.a + d * g.t1
                from = listOf(p1 - n * ht, p1 + n * ht)
            }
            out += from + listOf(s.bL, s.b, s.bR)
        }
        return out
    }

    /** The wall nearest to [w] within [maxDist] mm: the point on its centre line and the normal towards [w]. */
    fun onWall(plan: Plan, w: P, maxDist: Double, prefer: P? = null): Pair<P, P>? {
        var best: Pair<P, P>? = null
        var bestD = maxDist
        for (wall in plan.walls) {
            val a = plan.pos(wall.a) ?: continue
            val b = plan.pos(wall.b) ?: continue
            if ((b - a).len < 1) continue
            val q = Geo.project(w, a, b).first
            val d = (q - w).len
            if (d > bestD) continue
            var n = (b - a).norm().left()
            val side = prefer?.let { dot(it, n) } ?: dot(w - q, n)
            if (side < 0) n = n * -1.0
            bestD = d; best = q to n
        }
        return best
    }

    fun place(plan: Plan): List<PlacedPoint> = plan.points.map { p ->
        val c = P(p.x.toDouble(), p.y.toDouble())
        val n = P(p.nx, p.ny)
        if (p.kind.free) return@map PlacedPoint(p, c, n)
        val t = plan.walls.filter { w ->
            val a = plan.pos(w.a); val b = plan.pos(w.b)
            a != null && b != null && Geo.distToSegment(c, a, b) <= 5
        }.maxOfOrNull { it.thickness } ?: 0
        PlacedPoint(p, c + n * (t / 2.0), n)
    }

    /** After walls changed: sockets and switches go back onto the nearest wall, keeping the side they faced. */
    fun reattach(plan: Plan): Plan = plan.copy(points = plan.points.map { p ->
        if (p.kind.free) return@map p
        val hit = onWall(plan, P(p.x.toDouble(), p.y.toDouble()), 1500.0, P(p.nx, p.ny)) ?: return@map p
        p.copy(x = hit.first.x.roundToInt(), y = hit.first.y.roundToInt(), nx = hit.second.x, ny = hit.second.y)
    }, openings = plan.openings.map { o ->
        val hit = onWall(plan, P(o.x.toDouble(), o.y.toDouble()), 1500.0, P(o.nx, o.ny)) ?: return@map o
        o.copy(x = hit.first.x.roundToInt(), y = hit.first.y.roundToInt(), nx = hit.second.x, ny = hit.second.y)
    })
}
