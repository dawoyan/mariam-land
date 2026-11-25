package com.dawoyan.roomik.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Pillars on the plan and the grid their centre lines make. */
object Pillars {
    /** The outline of [p] on the plan: a rectangle, or a 20-sided circle for a round one. */
    fun corners(p: Pillar): List<P> {
        val c = P(p.x.toDouble(), p.y.toDouble())
        if (p.round) return (0 until 20).map { k -> val a = Math.PI * 2 * k / 20; c + P(cos(a), sin(a)) * (p.w / 2.0) }
        val hw = p.w / 2.0; val hd = p.d / 2.0
        return listOf(c + P(-hw, -hd), c + P(hw, -hd), c + P(hw, hd), c + P(-hw, hd))
    }

    fun covers(p: Pillar, w: P, slack: Double = 0.0): Boolean =
        if (p.round) (w - P(p.x.toDouble(), p.y.toDouble())).len <= p.w / 2.0 + slack
        else abs(w.x - p.x) <= p.w / 2.0 + slack && abs(w.y - p.y) <= p.d / 2.0 + slack

    /** The grid lines through the pillars' centres: distinct x's (vertical lines) and y's (horizontal lines), sorted. */
    fun axes(pillars: List<Pillar>): Pair<List<Int>, List<Int>> {
        fun merge(v: List<Int>): List<Int> {
            val out = ArrayList<Int>()
            for (x in v.sorted()) if (out.isEmpty() || x - out.last() > 20) out += x
            return out
        }
        return merge(pillars.map { it.x }) to merge(pillars.map { it.y })
    }

    /**
     * Ids of [pillars] not standing on one of [below] (centres more than 20 mm apart). Nothing when the floor below has
     * no pillars: then these stand on its walls or slab as drawn.
     */
    fun offBelow(pillars: List<Pillar>, below: List<Pillar>): Set<Int> =
        if (below.isEmpty()) emptySet()
        else pillars.filter { p -> below.none { abs(it.x - p.x) <= 20 && abs(it.y - p.y) <= 20 } }.map { it.id }.toSet()

    /**
     * Pillars [ids] of [pillars] moved exactly over the nearest of [below] (centre on centre), when one is within
     * [reach] mm. Returns the pillars and how many moved.
     */
    fun alignToBelow(pillars: List<Pillar>, below: List<Pillar>, ids: Set<Int>, reach: Double = 1500.0): Pair<List<Pillar>, Int> {
        var moved = 0
        val out = pillars.map { p ->
            if (p.id !in ids) return@map p
            val b = below.minByOrNull { abs(it.x - p.x).toDouble() + abs(it.y - p.y) } ?: return@map p
            val d = Math.hypot((b.x - p.x).toDouble(), (b.y - p.y).toDouble())
            if (d < 1 || d > reach) p else { moved++; p.copy(x = b.x, y = b.y) }
        }
        return out to moved
    }

    /**
     * Where [p] should stand to be on the grid lines of [others] (pillars of this and every other floor): for x and for
     * y, the value most of them share within [reach] mm (the nearest on a tie); null where it is on its line already or
     * none is near.
     */
    fun axisTarget(p: Pillar, others: List<Pillar>, reach: Int = 300): Pair<Int?, Int?> {
        fun best(mine: Int, vals: List<Int>): Int? {
            val near = vals.filter { abs(it - mine) <= reach }
            if (near.isEmpty()) return null
            val counts = near.groupingBy { it }.eachCount()
            val top = counts.maxOf { it.value }
            val pick = counts.filter { it.value == top }.keys.minByOrNull { abs(it - mine) }!!
            // already on a line shared as much as any: stays
            if ((counts[mine] ?: 0) >= top) return null
            return pick.takeIf { it != mine }
        }
        return best(p.x, others.map { it.x }) to best(p.y, others.map { it.y })
    }

    /** Pillars [ids] moved onto the grid lines of the others here and of [otherFloors]. Returns them and how many moved. */
    fun centerOnAxes(pillars: List<Pillar>, otherFloors: List<Pillar>, ids: Set<Int>): Pair<List<Pillar>, Int> {
        var cur = pillars; var moved = 0
        for (id in ids) {
            val p = cur.firstOrNull { it.id == id } ?: continue
            val (tx, ty) = axisTarget(p, cur.filter { it.id != id } + otherFloors)
            if (tx == null && ty == null) continue
            cur = cur.map { if (it.id == id) it.copy(x = tx ?: it.x, y = ty ?: it.y) else it }; moved++
        }
        return cur to moved
    }

    /** Pillars picked on several levels ([picks]: level → pillar ids) put on the grid lines of every pillar of the house. */
    fun centerOnAxesAcross(root: Plan, picks: Map<Int, Set<Int>>): Pair<Plan, Int> {
        var r = root; var moved = 0
        for ((f, ids) in picks) for (id in ids) {
            val here = r.floor(f).pillars
            val p = here.firstOrNull { it.id == id } ?: continue
            val others = here.filter { it.id != id } + r.levels.filter { it != f }.flatMap { r.floor(it).pillars }
            val (tx, ty) = axisTarget(p, others)
            if (tx == null && ty == null) continue
            r = r.withFloor(f, r.floor(f).copy(pillars = here.map { if (it.id == id) it.copy(x = tx ?: it.x, y = ty ?: it.y) else it })); moved++
        }
        return r to moved
    }

    /** Pillars picked on several levels each put exactly over the nearest pillar of the floor under its own. */
    fun alignToBelowAcross(root: Plan, picks: Map<Int, Set<Int>>): Pair<Plan, Int> {
        var r = root; var moved = 0
        for ((f, ids) in picks.toSortedMap()) {
            if (f == SITE || f - 1 !in r.floorRange) continue
            val (ps, n) = alignToBelow(r.floor(f).pillars, r.floor(f - 1).pillars, ids)
            if (n > 0) { r = r.withFloor(f, r.floor(f).copy(pillars = ps)); moved += n }
        }
        return r to moved
    }

    fun height(plan: Plan, p: Pillar): Int = p.height ?: plan.wallHeight

    /**
     * A pillar hidden inside walls: every corner of it is within a wall's body (+20 mm), so it is built into the wall and
     * plastered over, not seen in 3D. A pillar thicker than the wall, or standing free, shows.
     */
    fun inWall(plan: Plan, p: Pillar): Boolean = corners(p).all { c ->
        plan.walls.any { w ->
            val a = plan.pos(w.a) ?: return@any false; val b = plan.pos(w.b) ?: return@any false
            // inside the wall's rectangle (its ends cut square), with 20 mm to spare
            val len = (b - a).len; if (len < 1) return@any false
            val d = (b - a).norm(); val t = (c - a).x * d.x + (c - a).y * d.y
            val off = abs((c - a).x * d.y - (c - a).y * d.x)
            t >= -20 && t <= len + 20 && off <= w.thickness / 2.0 + 20
        }
    }
}

/** Walls of a floor that have the outside on one side: the house's outer walls. */
object Outline {
    fun outerWalls(plan: Plan): List<Wall> {
        val rooms = Rooms.detect(plan)
        fun inRoom(q: P) = rooms.any { Geo.inside(q, it.polygon) }
        return WallShapes.build(plan).filter { s ->
            val n = (s.b - s.a).norm().left()
            val off = s.wall.thickness / 2.0 + 80
            !inRoom(s.mid + n * off) || !inRoom(s.mid - n * off)
        }.map { it.wall }
    }
}
