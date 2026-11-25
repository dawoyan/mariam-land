package com.dawoyan.roomik.core

import kotlin.math.roundToInt

/**
 * Many corners or pillars at once, picked with a rectangle: lined up, spaced evenly, moved together; wall corners in a
 * straight run taken out, and new ones put into a wall.
 */
object Arrange {
    /** Where to line things up: the left or top edge, the middle, the right or bottom edge. */
    enum class Align { MIN, MID, MAX, OLDEST }
    /** Coordinate to line up.  Null retains the convenient row/column auto choice. */
    enum class Axis { X, Y }

    fun nodesIn(plan: Plan, a: P, b: P): Set<Int> = plan.nodes.filter { inside(P(it.x.toDouble(), it.y.toDouble()), a, b) }.map { it.id }.toSet()
    fun pillarsIn(plan: Plan, a: P, b: P): Set<Int> = plan.pillars.filter { inside(P(it.x.toDouble(), it.y.toDouble()), a, b) }.map { it.id }.toSet()
    private fun inside(p: P, a: P, b: P) = p.x >= minOf(a.x, b.x) && p.x <= maxOf(a.x, b.x) && p.y >= minOf(a.y, b.y) && p.y <= maxOf(a.y, b.y)

    /** True when [pts] lie more along x than along y: a row, lined up by their y; else a column, by their x. */
    fun isRow(pts: List<P>): Boolean = pts.isEmpty() || (pts.maxOf { it.x } - pts.minOf { it.x }) >= (pts.maxOf { it.y } - pts.minOf { it.y })

    /** [v] in the order the things were made (oldest first, by id): OLDEST takes the first. */
    private fun target(v: List<Int>, how: Align): Int = when (how) {
        Align.MIN -> v.min(); Align.MAX -> v.max(); Align.MID -> ((v.min() + v.max()) / 2.0).roundToInt()
        Align.OLDEST -> v.first()
    }

    /** New x (column) or y (row) for each of [pos] by id: lined up as [how] says. */
    private fun aligned(pos: Map<Int, P>, how: Align, axis: Axis? = null): Map<Int, P> {
        if (pos.size < 2) return pos
        // A row normally aligns vertically; explicit controls may instead align its left/centre/right edge.
        val alignX = axis == Axis.X || (axis == null && !isRow(pos.values.toList()))
        val t = target(pos.entries.sortedBy { it.key }.map { (if (alignX) it.value.x else it.value.y).roundToInt() }, how).toDouble()
        return pos.mapValues { (_, p) -> if (alignX) P(t, p.y) else P(p.x, t) }
    }

    /** Spaced evenly along the row or column, the first and last staying where they are. */
    private fun spaced(pos: Map<Int, P>): Map<Int, P> {
        if (pos.size < 3) return pos
        val row = isRow(pos.values.toList())
        val order = pos.entries.sortedBy { if (row) it.value.x else it.value.y }
        val first = if (row) order.first().value.x else order.first().value.y
        val step = ((if (row) order.last().value.x else order.last().value.y) - first) / (order.size - 1)
        return order.withIndex().associate { (i, e) ->
            val v = (first + step * i).roundToInt().toDouble()
            e.key to if (row) P(v, e.value.y) else P(e.value.x, v)
        }
    }

    private fun nodePos(plan: Plan, ids: Set<Int>) = plan.nodes.filter { it.id in ids }.associate { it.id to P(it.x.toDouble(), it.y.toDouble()) }
    private fun pillarPos(plan: Plan, ids: Set<Int>) = plan.pillars.filter { it.id in ids }.associate { it.id to P(it.x.toDouble(), it.y.toDouble()) }

    private fun setNodes(plan: Plan, to: Map<Int, P>): Plan {
        // sockets, switches, doors and windows on walls whose both ends move go along with the wall's middle
        fun shift(x: Int, y: Int): Pair<Int, Int>? {
            val p = P(x.toDouble(), y.toDouble())
            val w = plan.walls.firstOrNull { w -> val a = plan.pos(w.a); val b = plan.pos(w.b); a != null && b != null && Geo.distToSegment(p, a, b) <= 5 } ?: return null
            val na = to[w.a] ?: return null; val nb = to[w.b] ?: return null
            val a = plan.pos(w.a)!!; val b = plan.pos(w.b)!!
            val len = (b - a).len
            val t = if (len < 1) 0.0 else dot(p - a, b - a) / (len * len)
            val q = na + (nb - na) * t
            return q.x.roundToInt() to q.y.roundToInt()
        }
        return PlanOps.finish(plan.copy(
            nodes = plan.nodes.map { n -> to[n.id]?.let { n.copy(x = it.x.roundToInt(), y = it.y.roundToInt()) } ?: n },
            points = plan.points.map { p -> if (p.kind.free) p else shift(p.x, p.y)?.let { (x, y) -> p.copy(x = x, y = y) } ?: p },
            openings = plan.openings.map { o -> shift(o.x, o.y)?.let { (x, y) -> o.copy(x = x, y = y) } ?: o },
        ))
    }

    private fun setPillars(plan: Plan, to: Map<Int, P>): Plan =
        plan.copy(pillars = plan.pillars.map { p -> to[p.id]?.let { p.copy(x = it.x.roundToInt(), y = it.y.roundToInt()) } ?: p })

    private fun pointPos(plan: Plan, ids: Set<Int>) = plan.points.filter { it.id in ids }.associate { it.id to P(it.x.toDouble(), it.y.toDouble()) }
    /** Electric points moved to [to]; those on walls stay on the nearest wall. */
    private fun setPoints(plan: Plan, to: Map<Int, P>): Plan { var p = plan; for ((id, q) in to) p = PlanOps.movePoint(p, id, q, 400.0); return p }
    fun alignPoints(plan: Plan, ids: Set<Int>, how: Align, axis: Axis? = null) = setPoints(plan, aligned(pointPos(plan, ids), how, axis))
    fun spacePoints(plan: Plan, ids: Set<Int>) = setPoints(plan, spaced(pointPos(plan, ids)))
    fun movePoints(plan: Plan, ids: Set<Int>, dx: Int, dy: Int) = setPoints(plan, pointPos(plan, ids).mapValues { it.value + P(dx.toDouble(), dy.toDouble()) })
    fun pointsBounds(plan: Plan, ids: Set<Int>): Pair<P, P>? = pointPos(plan, ids).values.takeIf { it.isNotEmpty() }?.let { v ->
        P(v.minOf { it.x }, v.minOf { it.y }) to P(v.maxOf { it.x }, v.maxOf { it.y }) }

    /**
     * Corners and pillars picked on several levels of the house ([picks]: level → corner ids and pillar ids) lined up
     * together: one row or column for them all, the line taken from all of them, each level changed in place.
     */
    fun alignAcross(root: Plan, picks: Map<Int, Pair<Set<Int>, Set<Int>>>, how: Align, axis: Axis? = null): Plan {
        // oldest: of the lowest level, the one made first there
        val all = picks.toSortedMap().flatMap { (f, s) -> val fl = root.floor(f)
            nodePos(fl, s.first).entries.sortedBy { it.key }.map { it.value } + pillarPos(fl, s.second).entries.sortedBy { it.key }.map { it.value } }
        if (all.size < 2) return root
        val alignX = axis == Axis.X || (axis == null && !isRow(all))
        val t = target(all.map { (if (alignX) it.x else it.y).roundToInt() }, how).toDouble()
        fun put(m: Map<Int, P>) = m.mapValues { (_, p) -> if (alignX) P(t, p.y) else P(p.x, t) }
        var r = root
        for ((f, s) in picks) {
            var fl = r.floor(f)
            if (s.second.isNotEmpty()) fl = setPillars(fl, put(pillarPos(fl, s.second)))
            if (s.first.isNotEmpty()) fl = setNodes(fl, put(nodePos(fl, s.first)))
            r = r.withFloor(f, fl)
        }
        return r
    }

    /**
     * Wall corners picked on several levels ([picks]: level → corner ids) put on the pillars' grid lines (pillars of
     * every level) when one is within [reach] mm, x and y each. Returns the house and how many corners moved.
     */
    fun nodesToAxes(root: Plan, picks: Map<Int, Set<Int>>, reach: Int = 300): Pair<Plan, Int> {
        val (xs, ys) = Pillars.axes(root.levels.flatMap { root.floor(it).pillars })
        fun near(v: Int, lines: List<Int>) = lines.minByOrNull { kotlin.math.abs(it - v) }?.takeIf { kotlin.math.abs(it - v) in 1..reach }
        var r = root; var moved = 0
        for ((f, ids) in picks) {
            val fl = r.floor(f)
            val to = nodePos(fl, ids).mapNotNull { (id, p) ->
                val nx = near(p.x.roundToInt(), xs); val ny = near(p.y.roundToInt(), ys)
                if (nx == null && ny == null) null else id to P((nx ?: p.x.roundToInt()).toDouble(), (ny ?: p.y.roundToInt()).toDouble())
            }.toMap()
            if (to.isNotEmpty()) { r = r.withFloor(f, setNodes(fl, to)); moved += to.size }
        }
        return r to moved
    }

    /** True when the picks across levels lie more along x than along y. */
    fun isRowAcross(root: Plan, picks: Map<Int, Pair<Set<Int>, Set<Int>>>): Boolean =
        isRow(picks.flatMap { (f, s) -> val fl = root.floor(f); nodePos(fl, s.first).values + pillarPos(fl, s.second).values })

    fun alignNodes(plan: Plan, ids: Set<Int>, how: Align, axis: Axis? = null) = setNodes(plan, aligned(nodePos(plan, ids), how, axis))
    fun alignPillars(plan: Plan, ids: Set<Int>, how: Align, axis: Axis? = null) = setPillars(plan, aligned(pillarPos(plan, ids), how, axis))
    fun spaceNodes(plan: Plan, ids: Set<Int>) = setNodes(plan, spaced(nodePos(plan, ids)))
    fun spacePillars(plan: Plan, ids: Set<Int>) = setPillars(plan, spaced(pillarPos(plan, ids)))

    /** Corners [nodes] and pillars [pillars] moved by ([dx], [dy]); walls between moved corners take what is on them along. */
    fun move(plan: Plan, nodes: Set<Int>, pillars: Set<Int>, dx: Int, dy: Int, finish: Boolean = true): Plan {
        val d = P(dx.toDouble(), dy.toDouble())
        val moved = setPillars(plan, pillarPos(plan, pillars).mapValues { it.value + d })
        if (nodes.isEmpty()) return moved
        val to = nodePos(moved, nodes).mapValues { it.value + d }
        if (finish) return setNodes(moved, to)
        return moved.copy(nodes = moved.nodes.map { n -> to[n.id]?.let { n.copy(x = it.x.roundToInt(), y = it.y.roundToInt()) } ?: n })
    }

    /**
     * Corners among [ids] that only join two walls in a straight line (same thickness and height) taken out, the two
     * walls made one. Returns the plan and how many corners went.
     */
    fun simplify(plan: Plan, ids: Set<Int>): Pair<Plan, Int> {
        var p = plan
        var gone = 0
        for (id in ids) {
            val ws = p.walls.filter { it.a == id || it.b == id }
            if (ws.size != 2) continue
            val (w1, w2) = ws
            if (w1.thickness != w2.thickness || w1.height != w2.height || w1.facing != w2.facing) continue
            val c = p.pos(id) ?: continue
            val o1 = (if (w1.a == id) w1.b else w1.a); val o2 = (if (w2.a == id) w2.b else w2.a)
            if (o1 == o2) continue
            // both run o1 → id → o2: their sides must match to be one wall
            val r1 = if (w1.a == id) w1.flipped() else w1; val r2 = if (w2.b == id) w2.flipped() else w2
            if (r1.left != r2.left || r1.right != r2.right) continue
            val a = p.pos(o1) ?: continue; val b = p.pos(o2) ?: continue
            // straight: the corner is within 15 mm of the line through the other two, between them
            if (Geo.distToSegment(c, a, b) > 15) continue
            p = p.copy(nodes = p.nodes.filter { it.id != id }, walls = p.walls.filter { it.id != w1.id && it.id != w2.id } + r1.copy(a = o1, b = o2))
            gone++
        }
        return (if (gone > 0) PlanOps.finish(p) else p) to gone
    }

    /** Wall [wallId] cut in two at the point of it nearest [w], with a new corner there; null when too near an end. */
    fun split(plan: Plan, wallId: Int, w: P): Pair<Plan, Int>? {
        val wall = plan.wall(wallId) ?: return null
        val a = plan.pos(wall.a) ?: return null; val b = plan.pos(wall.b) ?: return null
        val (q0, _) = Geo.project(w, a, b)
        val q = P((q0.x / 10).roundToInt() * 10.0, (q0.y / 10).roundToInt() * 10.0)
        if ((q - a).len < 100 || (q - b).len < 100) return null
        val id = maxOf(plan.nodes.maxOfOrNull { it.id } ?: 0, plan.walls.maxOfOrNull { it.id } ?: 0) + 1
        val n = Node(id, q.x.roundToInt(), q.y.roundToInt())
        val p = plan.copy(nodes = plan.nodes + n, walls = plan.walls.filter { it.id != wallId } + wall.copy(b = id) + wall.copy(id = id + 1, a = id))
        return p to id
    }
}
