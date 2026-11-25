package com.dawoyan.roomik.core

/** A drawn cable in 3D, metres: a list of points (x, y up, z) one after another. */
object Cables {
    /** Cables run this far under the ceiling, or this far over the floor. */
    const val CEILING_GAP = 150
    const val FLOOR_GAP = 50

    fun levelMm(plan: Plan, level: RouteLevel) = if (level == RouteLevel.CEILING) plan.wallHeight - CEILING_GAP else FLOOR_GAP

    /** Where a point's cable comes out, on the plan (mm), and its height. */
    fun end(plan: Plan, p: ElPoint): Pair<P, Int> {
        val placed = Interior.place(plan).firstOrNull { it.point.id == p.id }
        val at = placed?.let { if (p.kind.free) it.face else it.face + it.normal * 10.0 } ?: P(p.x.toDouble(), p.y.toDouble())
        return at to (if (p.kind == PointKind.LAMP) plan.wallHeight else p.height)
    }

    /** The way out of the wall point [p] sits on, or null for one hanging free. */
    fun normal(plan: Plan, p: ElPoint): P? =
        if (p.kind.free) null else Interior.place(plan).firstOrNull { it.point.id == p.id }?.normal

    /** A cable on the plan (mm): from its first point through its turns to its last; null when an end is missing. */
    fun plan(plan: Plan, r: Route): List<P>? {
        val a = plan.points.firstOrNull { it.id == r.from } ?: return null
        val b = plan.points.firstOrNull { it.id == r.to } ?: return null
        return listOf(end(plan, a).first) + r.via.map { P(it.x.toDouble(), it.y.toDouble()) } + end(plan, b).first
    }

    /** The cable as 3D points in mm (x, height, y). */
    fun path(plan: Plan, r: Route): List<Triple<Double, Double, Double>>? {
        val a = plan.points.firstOrNull { it.id == r.from } ?: return null
        val b = plan.points.firstOrNull { it.id == r.to } ?: return null
        val (pa, ha) = end(plan, a)
        val (pb, hb) = end(plan, b)
        val lv = levelMm(plan, r.level).toDouble()
        val out = ArrayList<Triple<Double, Double, Double>>()
        out += Triple(pa.x, ha.toDouble(), pa.y)
        out += Triple(pa.x, lv, pa.y)
        for (n in r.via) out += Triple(n.x.toDouble(), lv, n.y.toDouble())
        out += Triple(pb.x, lv, pb.y)
        out += Triple(pb.x, hb.toDouble(), pb.y)
        return out
    }

    /** Cable length in mm. */
    fun length(plan: Plan, r: Route): Double {
        val p = path(plan, r) ?: return 0.0
        var s = 0.0
        for (i in 1 until p.size) {
            val (x0, y0, z0) = p[i - 1]; val (x1, y1, z1) = p[i]
            s += Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0))
        }
        return s
    }

    /** Horizontal distance drawn on the 2D plan, excluding the rises/drops at devices. */
    fun planLength(plan: Plan, r: Route): Double {
        val p = plan(plan, r) ?: return 0.0
        return p.zipWithNext().sumOf { (a, b) -> (b - a).len }
    }

    /** Of [length], the pieces going up or down the walls at the cable's ends, mm. */
    fun vertical(plan: Plan, r: Route): Double {
        val p = path(plan, r) ?: return 0.0
        return Math.abs(p[1].second - p[0].second) + Math.abs(p[p.size - 1].second - p[p.size - 2].second)
    }

    fun between(plan: Plan, a: Int, b: Int): Route? = plan.routes.firstOrNull { (it.from == a && it.to == b) || (it.from == b && it.to == a) }

    /** Drawn cable of each kind on [floors]: wires, mm², total length mm; thinnest first. */
    fun totals(floors: List<Plan>): List<Triple<Int, Double, Double>> =
        floors.flatMap { f -> f.routes.map { Triple(it.cores, it.mm2, length(f, it)) } }.groupBy { it.first to it.second }
            .map { (k, l) -> Triple(k.first, k.second, l.sumOf { it.third }) }.sortedWith(compareBy({ it.second }, { it.first }))
}
