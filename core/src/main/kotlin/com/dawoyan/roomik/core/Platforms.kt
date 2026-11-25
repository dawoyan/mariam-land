package com.dawoyan.roomik.core

/** Platforms (balconies, terraces, decks) as blocks: the slab, a railing on the open sides, columns to the ground. */
object Platforms {
    fun corners(p: Platform): List<P> = listOf(P(p.x1.toDouble(), p.y1.toDouble()), P(p.x2.toDouble(), p.y1.toDouble()),
        P(p.x2.toDouble(), p.y2.toDouble()), P(p.x1.toDouble(), p.y2.toDouble()))

    fun covers(p: Platform, w: P): Boolean = w.x >= p.x1 && w.x <= p.x2 && w.y >= p.y1 && w.y <= p.y2

    fun area(p: Platform): Double = (p.x2 - p.x1).toDouble() * (p.y2 - p.y1) / 1e6

    /** Sides of [p] that stand free: not along a wall of [plan]. */
    fun openSides(plan: Plan, p: Platform): List<Pair<P, P>> {
        val c = corners(p)
        return (0 until 4).map { c[it] to c[(it + 1) % 4] }.filter { (a, b) ->
            val m = (a + b) * 0.5
            plan.walls.none { w ->
                val wa = plan.pos(w.a) ?: return@none false; val wb = plan.pos(w.b) ?: return@none false
                Geo.distToSegment(m, wa, wb) <= w.thickness / 2.0 + 80
            }
        }
    }

    /** The blocks of [p]; heights in mm above the floor, [groundZ] is the ground outside (negative above the ground floor). */
    fun parts(plan: Plan, p: Platform, groundZ: Double): List<FPart> {
        val out = ArrayList<FPart>()
        val top = p.height.toDouble()
        val slab = 0x9A8F84
        out += FPart(corners(p), top - p.thickness, top, if (p.height <= 0) 0xB7A58C else slab)
        if (p.columns && top - p.thickness > groundZ + 50) {
            val s = 90.0
            for (c in corners(p)) {
                val q = P(c.x + if (c.x <= p.x1) s else -s, c.y + if (c.y <= p.y1) s else -s)
                out += FPart(listOf(q + P(-s, -s), q + P(s, -s), q + P(s, s), q + P(-s, s)), groundZ, top - p.thickness, 0xD8D2C6)
            }
        }
        if (p.railing) {
            val rh = p.railingHeight.toDouble()
            val metal = 0x3F4348
            for ((a, b) in openSides(plan, p)) {
                val len = (b - a).len
                if (len < 50) continue
                val d = (b - a).norm()
                // inside the edge by 30 mm, so the railing stands on the slab
                val inward = d.left() * 30.0
                fun bar(u: P, v: P, half: Double, z0: Double, z1: Double) {
                    val n = (v - u).norm().left() * half
                    out += FPart(listOf(u + n, v + n, v - n, u - n), z0, z1, metal)
                }
                bar(a + inward, b + inward, 25.0, top + rh - 50, top + rh)
                bar(a + inward, b + inward, 10.0, top + 100, top + 130)
                val n = maxOf(1, (len / 120).toInt())
                for (i in 0..n) {
                    val c = a + d * (len * i / n) + inward
                    val s = if (i % 10 == 0 || i == n) 22.0 else 7.0
                    out += FPart(listOf(c + P(-s, -s), c + P(s, -s), c + P(s, s), c + P(-s, s)), top, top + rh - 50, metal)
                }
            }
        }
        return out
    }
}
