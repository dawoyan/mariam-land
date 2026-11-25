package com.dawoyan.roomik.core

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A flight of steps: [corners] like an item's (c0→c3 is the way up, c0–c1 the bottom edge), climbing from [h0] to [h1]
 * mm in [steps] steps.
 */
class Flight(val corners: List<P>, val h0: Double, val h1: Double, val steps: Int)

/** A landing (лестничная площадка): a flat platform at height [h] mm. */
class Landing(val corners: List<P>, val h: Double)

class StairShape(val flights: List<Flight>, val landings: List<Landing>) {
    val pieces: List<List<P>> get() = flights.map { it.corners } + landings.map { it.corners }
}

object Stairs {
    /** Usual height of one step, mm. */
    const val STEP = 175.0

    fun rise(plan: Plan, s: Stair): Int = if (s.fromGround) plan.groundLevel.coerceAtLeast(STEP.toInt()) else s.rise ?: (plan.wallHeight + SLAB)

    /** Where the stairs start, mm from this floor's floor: the ground for stairs from outside. */
    fun base(plan: Plan, s: Stair): Double = if (s.fromGround) -rise(plan, s).toDouble() else 0.0

    /** A rectangle like an item's: from [start] (middle of its first edge) [len] mm along [dir], [w] mm wide. */
    private fun rect(start: P, dir: P, w: Double, len: Double): List<P> {
        val u = P(dir.y, -dir.x)
        val e = start + dir * len
        return listOf(start - u * (w / 2), start + u * (w / 2), e + u * (w / 2), e - u * (w / 2))
    }

    private fun flight(start: P, dir: P, w: Double, len: Double, h0: Double, h1: Double) =
        Flight(rect(start, dir, w, len), h0, h1, maxOf(1, ((h1 - h0) / STEP).roundToInt()))

    fun shape(plan: Plan, s: Stair): StairShape {
        val up = upShape(plan, s)
        val h = rise(plan, s).toDouble()
        val b = base(plan, s)
        // the same footprint climbed from its far end: each flight turned round, heights mirrored
        val shp = if (!s.reversed) up else StairShape(
            up.flights.reversed().map { f -> Flight(listOf(f.corners[2], f.corners[3], f.corners[0], f.corners[1]), h - f.h1, h - f.h0, f.steps) },
            up.landings.map { Landing(it.corners, h - it.h) },
        )
        if (b == 0.0) return shp
        return StairShape(shp.flights.map { Flight(it.corners, it.h0 + b, it.h1 + b, it.steps) }, shp.landings.map { Landing(it.corners, it.h + b) })
    }

    /** A wall the stairs cross below their underside, and the height it would need so they pass over it (0: none). */
    class Clash(val wall: Wall, val fitHeight: Int)

    /** Underside of a step or landing above the floor, mm: they are solid this deep. */
    const val TREAD = 220
    const val LANDING_SLAB = 180

    /**
     * Walls [s] runs into: the wall is higher than the underside of a step or landing over it. Each comes with the height
     * it could be lowered to (rounded down to 1 cm) so the stairs pass over it.
     */
    fun clashes(plan: Plan, s: Stair): List<Clash> {
        val parts = undersides(plan, s)
        val out = ArrayList<Clash>()
        for (ws in WallShapes.build(plan)) {
            if (ws.wall.id in s.cropped) continue // the user had it cut by these stairs
            val under = parts.filter { Interior.overlaps(it.first, ws.outline) }.minOfOrNull { it.second } ?: continue
            if (plan.heightOf(ws.wall) > under + 1) out += Clash(ws.wall, ((under / 10).toInt() * 10))
        }
        return out
    }

    private fun upShape(plan: Plan, s: Stair): StairShape {
        val r = Math.toRadians(s.rot.toDouble())
        val d = P(cos(r), sin(r))
        val a = P(s.x.toDouble(), s.y.toDouble())
        val w = s.width.toDouble().coerceAtLeast(300.0)
        val l = s.length.toDouble().coerceAtLeast(w + 300)
        val h = rise(plan, s).toDouble()
        return when (s.kind) {
            StairKind.STRAIGHT -> StairShape(listOf(flight(a, d, w, l, 0.0, h)), emptyList())
            StairKind.LANDING -> {
                val f = (l - w) / 2
                StairShape(
                    listOf(flight(a, d, w, f, 0.0, h / 2), flight(a + d * (f + w), d, w, f, h / 2, h)),
                    listOf(Landing(rect(a + d * f, d, w, w), h / 2)),
                )
            }
            StairKind.L_LEFT, StairKind.L_RIGHT -> {
                val side = if (s.kind == StairKind.L_LEFT) d.left() else d.left() * -1.0
                val f = l - w
                val corner = a + d * (l - w / 2)
                StairShape(
                    listOf(flight(a, d, w, f, 0.0, h / 2), flight(corner + side * (w / 2), side, w, s.length2.toDouble().coerceAtLeast(300.0), h / 2, h)),
                    listOf(Landing(rect(a + d * f, d, w, w), h / 2)),
                )
            }
            StairKind.U, StairKind.U_RIGHT -> {
                // the second flight comes back on the left (U) or on the right (U_RIGHT) of the first
                val side = if (s.kind == StairKind.U) d.left() else d.left() * -1.0
                val f = l - w
                StairShape(
                    listOf(flight(a, d, w, f, 0.0, h / 2), flight(a + side * w + d * f, d * -1.0, w, f, h / 2, h)),
                    listOf(Landing(rect(a + d * f + side * (w / 2), d, 2 * w, w), h / 2)),
                )
            }
        }
    }

    /**
     * The stairwell openings stairs on [plan] cut into the floor above (and into this floor's ceiling): every piece of
     * stairs that go up to the next floor. Porch stairs and short ones that stay under the ceiling cut nothing.
     */
    fun wells(plan: Plan): List<List<P>> =
        plan.stairs.filter { !it.fromGround && rise(plan, it) > plan.wallHeight }.flatMap { shape(plan, it).pieces }

    /**
     * Triangles covering [poly] except the [holes] (convex): the room's triangles are split along the holes' edges until
     * the pieces are small, and those inside a hole are left out.
     */
    fun fillAround(poly: List<P>, holes: List<List<P>>): List<Triple<P, P, P>> {
        val out = ArrayList<Triple<P, P, P>>()
        fun inHole(p: P) = holes.any { Geo.inside(p, it) }
        fun sub(a: P, b: P, c: P, depth: Int) {
            val tri = listOf(a, b, c)
            val near = holes.filter { Interior.overlaps(tri, it) }
            if (near.isEmpty()) { out += Triple(a, b, c); return }
            if (near.any { h -> Geo.inside(a, h) && Geo.inside(b, h) && Geo.inside(c, h) }) return
            val ab = (b - a).len; val bc = (c - b).len; val ca = (a - c).len
            if (maxOf(ab, bc, ca) < 40 || depth > 18) { if (!inHole((a + b + c) * (1.0 / 3))) out += Triple(a, b, c); return }
            // split the longest edge in two, keeping the winding
            when (maxOf(ab, bc, ca)) {
                ab -> { val m = (a + b) * 0.5; sub(a, m, c, depth + 1); sub(m, b, c, depth + 1) }
                bc -> { val m = (b + c) * 0.5; sub(a, b, m, depth + 1); sub(a, m, c, depth + 1) }
                else -> { val m = (c + a) * 0.5; sub(a, b, m, depth + 1); sub(m, b, c, depth + 1) }
            }
        }
        for (t in Geo.triangulate(poly)) sub(poly[t[0]], poly[t[1]], poly[t[2]], 0)
        return out
    }

    /** Each step and landing of [s] with its underside height, mm. */
    private fun undersides(plan: Plan, s: Stair): List<Pair<List<P>, Double>> {
        val shp = shape(plan, s)
        val parts = ArrayList<Pair<List<P>, Double>>()
        for (f in shp.flights) {
            val c = f.corners
            for (k in 0 until f.steps) {
                val f0 = k.toDouble() / f.steps; val f1 = (k + 1).toDouble() / f.steps
                val top = f.h0 + (f.h1 - f.h0) * f1
                val q = listOf(c[0] + (c[3] - c[0]) * f0, c[1] + (c[2] - c[1]) * f0, c[1] + (c[2] - c[1]) * f1, c[0] + (c[3] - c[0]) * f1)
                parts += q to (top - TREAD).coerceAtLeast(base(plan, s))
            }
        }
        for (l in shp.landings) parts += l.corners to (l.h - LANDING_SLAB).coerceAtLeast(base(plan, s))
        return parts
    }

    /**
     * Where walls are cut by stairs ([Stair.cropped]): for each wall, stretches along it (mm from its end a) and the
     * height the wall keeps there, just under the stairs.
     */
    fun crops(plan: Plan): Map<Int, List<Triple<Double, Double, Double>>> {
        val out = HashMap<Int, MutableList<Triple<Double, Double, Double>>>()
        val shapes = WallShapes.build(plan).associateBy { it.wall.id }
        for (s in plan.stairs) {
            if (s.cropped.isEmpty()) continue
            val parts = undersides(plan, s)
            for (wid in s.cropped) {
                val ws = shapes[wid] ?: continue
                val len = (ws.b - ws.a).len
                if (len < 1) continue
                val d = (ws.b - ws.a).norm()
                // each piece over the wall gives a stretch; where stretches overlap the lower underside wins
                val pieces = parts.filter { Interior.overlaps(it.first, ws.outline) }.map { (pc, under) ->
                    val ts = pc.map { dot(it - ws.a, d) }
                    Triple(ts.min().coerceIn(0.0, len), ts.max().coerceIn(0.0, len), under)
                }.filter { it.second - it.first > 1 }
                if (pieces.isEmpty()) continue
                val cuts = (pieces.flatMap { listOf(it.first, it.second) }).distinct().sorted()
                val list = out.getOrPut(wid) { mutableListOf() }
                for (i in 0 until cuts.size - 1) {
                    val t0 = cuts[i]; val t1 = cuts[i + 1]; val m = (t0 + t1) / 2
                    val h = pieces.filter { m >= it.first && m <= it.second }.minOfOrNull { it.third } ?: continue
                    if (list.isNotEmpty() && kotlin.math.abs(list.last().second - t0) < 1 && kotlin.math.abs(list.last().third - h) < 1)
                        list[list.size - 1] = Triple(list.last().first, t1, h) else list += Triple(t0, t1, h)
                }
            }
        }
        return out
    }

    /** Walls any stairs on [plan] run into, by id, with the height each could be lowered to. */
    fun allClashes(plan: Plan): Map<Int, Int> {
        val out = HashMap<Int, Int>()
        for (s in plan.stairs) for (c in clashes(plan, s)) out[c.wall.id] = minOf(out[c.wall.id] ?: Int.MAX_VALUE, c.fitHeight)
        return out
    }

    fun hit(plan: Plan, s: Stair, p: P): Boolean = shape(plan, s).pieces.any { Geo.inside(p, it) }

    /** Stairs saved as furniture by version 0.5.0 become stairs. */
    fun migrate(plan: Plan): Plan {
        val old = plan.items.filter { it.kind == STAIRS }
        if (old.isEmpty()) return plan
        var id = plan.stairs.maxOfOrNull { it.id } ?: 0
        val made = old.map { i ->
            val c = Interior.corners(i)
            val start = (c[0] + c[1]) * 0.5
            val dir = (c[3] - c[0]).norm()
            Stair(++id, StairKind.STRAIGHT, start.x.roundToInt(), start.y.roundToInt(),
                Math.toDegrees(Math.atan2(dir.y, dir.x)).roundToInt(), i.w, i.d)
        }
        return plan.copy(items = plan.items - old.toSet(), stairs = plan.stairs + made)
    }
}
