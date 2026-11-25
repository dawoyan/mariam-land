package com.dawoyan.roomik.core

/**
 * Points that only sit on a straight line between their neighbours, and start nothing else, taken out: a wall corner
 * joining just two walls in line (same thickness and height) makes the two one wall; a fence point in line with the
 * points either side goes.
 */
object Tidy {
    /** Within this many mm of the line through its neighbours a point counts as in line. */
    const val TOL = 15.0

    fun straighten(plan: Plan): Plan {
        var p = plan
        if (p.walls.isNotEmpty()) p = Arrange.simplify(p, p.nodes.map { it.id }.toSet()).first
        if (p.fences.any { it.points.size > 2 }) p = p.copy(fences = p.fences.map { fence(it) })
        return p
    }

    private fun fence(f: Fence): Fence {
        val pts = f.points.toMutableList()
        var k = 1
        while (k < pts.size - 1) {
            val a = pts[k - 1].pos; val c = pts[k].pos; val b = pts[k + 1].pos
            if ((b - a).len > 1 && Geo.distToSegment(c, a, b) <= TOL) pts.removeAt(k) else k++
        }
        return if (pts.size == f.points.size) f else f.copy(points = pts)
    }

    private val Node.pos get() = P(x.toDouble(), y.toDouble())
}
