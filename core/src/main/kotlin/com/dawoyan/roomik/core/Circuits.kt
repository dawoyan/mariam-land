package com.dawoyan.roomik.core

import kotlin.math.roundToInt

/**
 * Drawn cables as circuits: cables joined end to end (through sockets in a chain, or through junction boxes) make one
 * line from a board. Lights and sockets never share a line.
 */
object Circuits {
    /**
     * An appliance a cable comes to and goes on from: its drop meets the cable in a junction box, drawn and bought like
     * one. A socket needs none: the cable goes in at its terminals and on out of them (a loop through the socket).
     */
    fun tapped(plan: Plan, p: ElPoint): Boolean =
        p.kind == PointKind.APPLIANCE && plan.routes.count { it.from == p.id || it.to == p.id } >= 2

    /** Junction boxes to buy on a floor: the drawn ones and those where sockets are taken off a cable going on. */
    fun boxes(plan: Plan): Int = plan.points.count { it.kind == PointKind.JUNCTION || tapped(plan, it) }

    /** What a line feeds. */
    enum class Kind { LIGHT, POWER, POWER_110 }

    /** What [p] puts on its line: 110 V sockets make a line of their own. */
    fun kindOf(p: ElPoint): Kind? = if (p.kind == PointKind.SOCKET && p.volts == 110) Kind.POWER_110 else kindOf(p.kind)

    fun kindOf(k: PointKind): Kind? = when (k) {
        PointKind.LAMP, PointKind.SWITCH -> Kind.LIGHT
        PointKind.SOCKET, PointKind.APPLIANCE -> Kind.POWER
        else -> null
    }

    /** Points a line passes on through: everything but boards and risers, where a line starts. */
    private fun through(k: PointKind) = k != PointKind.BOARD && k != PointKind.RISER

    /** The points joined to [start] by cables, not going on past boards and risers (which are included but not crossed). */
    fun component(plan: Plan, start: Int, routes: List<Route> = plan.routes): Set<Int> {
        val kinds = plan.points.associate { it.id to it.kind }
        val seen = linkedSetOf(start)
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            if (n != start && kinds[n]?.let { through(it) } == false) continue
            for (r in routes) {
                val m = when (n) { r.from -> r.to; r.to -> r.from; else -> continue }
                if (seen.add(m)) queue += m
            }
        }
        return seen
    }

    /** The kinds of things on the line through point [id]. */
    fun kinds(plan: Plan, id: Int): Set<Kind> {
        val p = plan.points.firstOrNull { it.id == id } ?: return emptySet()
        if (!through(p.kind)) return setOfNotNull(kindOf(p))
        return component(plan, id).mapNotNull { c -> plan.points.firstOrNull { it.id == c }?.let { kindOf(it) } }.toSet()
    }

    /** True when a cable between [a] and [b] would put lights and sockets on one line. */
    fun mixes(plan: Plan, a: Int, b: Int): Boolean = (kinds(plan, a) + kinds(plan, b)).size > 1

    /** Where on cable [r] (plan mm) [w] is nearest: the point, and how many of the cable's turns come before it. */
    fun nearestOn(plan: Plan, r: Route, w: P): Pair<P, Int>? {
        val pts = Cables.plan(plan, r) ?: return null
        var best: Pair<P, Int>? = null; var bd = Double.MAX_VALUE
        for (k in 1 until pts.size) {
            val (q, _) = Geo.project(w, pts[k - 1], pts[k])
            val d = (q - w).len
            if (d < bd) { bd = d; best = q to (k - 1) }
        }
        return best
    }

    /**
     * Cable [routeId] cut at [w] by a new junction box: the two halves keep its size and level. Returns the plan and the
     * box's id. A point right at an end of the cable is that end, with no box.
     */
    fun split(plan: Plan, routeId: Int, w: P): Pair<Plan, Int>? {
        val r = plan.routes.firstOrNull { it.id == routeId } ?: return null
        val (q0, before) = nearestOn(plan, r, w) ?: return null
        val q = P((q0.x / 10).roundToInt() * 10.0, (q0.y / 10).roundToInt() * 10.0)
        val pts = Cables.plan(plan, r) ?: return null
        if ((q - pts.first()).len < 150) return plan to r.from
        if ((q - pts.last()).len < 150) return plan to r.to
        val id = (plan.points.maxOfOrNull { it.id } ?: 0) + 1
        val box = ElPoint(id, PointKind.JUNCTION, q.x.roundToInt(), q.y.roundToInt(), height = Cables.levelMm(plan, r.level))
        val rid = (plan.routes.maxOfOrNull { it.id } ?: 0) + 1
        val a = r.copy(to = id, via = r.via.take(before))
        val b = r.copy(id = rid, from = id, via = r.via.drop(before))
        return plan.copy(points = plan.points + box, routes = plan.routes.map { if (it.id == r.id) a else it } + b) to id
    }

    /**
     * Junction boxes among [ids] tidied after cables went: with no cable or one cable left a box goes (and that cable
     * with it); with two left they are made one cable again.
     */
    fun tidy(plan: Plan, ids: List<Int>): Plan {
        var p = plan
        for (id in ids.distinct()) {
            val box = p.points.firstOrNull { it.id == id && it.kind == PointKind.JUNCTION } ?: continue
            val rs = p.routes.filter { it.from == id || it.to == id }
            when (rs.size) {
                0 -> p = p.copy(points = p.points - box)
                1 -> { val r = rs[0]; p = tidy(p.copy(points = p.points - box, routes = p.routes - r), listOf(if (r.from == id) r.to else r.from)) }
                2 -> {
                    // turned so that the first runs into the box and the second out of it
                    val a = rs[0].let { if (it.to == id) it else it.copy(from = it.to, to = it.from, via = it.via.reversed()) }
                    val b = rs[1].let { if (it.from == id) it else it.copy(from = it.to, to = it.from, via = it.via.reversed()) }
                    if (a.from == b.to) continue
                    val via = (a.via + Node(0, box.x, box.y) + b.via).mapIndexed { i, n -> n.copy(id = i) }
                    val merged = a.copy(to = b.to, via = Lines.straighten(via.map { P(it.x.toDouble(), it.y.toDouble()) }).mapIndexed { i, q -> Node(i, q.x.roundToInt(), q.y.roundToInt()) })
                    p = p.copy(points = p.points - box, routes = p.routes.filter { it.id != rs[0].id && it.id != rs[1].id } + merged)
                }
            }
        }
        return p
    }

    /**
     * The cables a device's power comes along: from point [id] through sockets and junction boxes on the way to the
     * nearest board (or riser bringing a board's cables), fewest cables first. Empty when no cable reaches one.
     */
    fun toBoard(plan: Plan, id: Int): List<Route> {
        val kinds = plan.points.associate { it.id to it.kind }
        val came = HashMap<Int, Route>()
        val seen = hashSetOf(id)
        val queue = ArrayDeque(listOf(id))
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            if (n != id && kinds[n]?.let { through(it) } == false) {
                val out = ArrayList<Route>()
                var c = n
                while (c != id) { val r = came[c] ?: break; out += r; c = if (r.from == c) r.to else r.from }
                return out.reversed()
            }
            for (r in plan.routes) {
                val m = when (n) { r.from -> r.to; r.to -> r.from; else -> continue }
                if (seen.add(m)) { came[m] = r; queue += m }
            }
        }
        return emptyList()
    }

    /** Shortest drawn cable path between two specific electrical points, for inspecting a lamp's switch run. */
    fun path(plan: Plan, from: Int, to: Int): List<Route> {
        if (from == to) return emptyList()
        val came = HashMap<Int, Route>()
        val seen = hashSetOf(from)
        val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            if (n == to) break
            for (r in plan.routes) {
                val next = when (n) { r.from -> r.to; r.to -> r.from; else -> continue }
                if (seen.add(next)) { came[next] = r; queue += next }
            }
        }
        if (to !in seen) return emptyList()
        val out = ArrayList<Route>(); var n = to
        while (n != from) { val r = came[n] ?: return emptyList(); out += r; n = if (r.from == n) r.to else r.from }
        return out.reversed()
    }

    /** A line leaving a board: its cables, their length (mm), and the devices on it. */
    class Line(val routes: List<Route>, val mm: Double, val points: List<ElPoint>) {
        val mm2: Double get() = routes.firstOrNull()?.mm2 ?: 0.0
        val cores: Int get() = routes.firstOrNull()?.cores ?: 0
        /** Devices on the line (not boxes, boards or risers). */
        val devices: List<ElPoint> get() = points.filter { kindOf(it.kind) != null }
    }

    /** The lines that leave point [boardId] (a board, or a riser bringing the board's cables to this floor). */
    fun lines(plan: Plan, boardId: Int): List<Line> {
        val out = ArrayList<Line>()
        val used = HashSet<Int>()
        for (r in plan.routes.filter { it.from == boardId || it.to == boardId }) {
            if (r.id in used) continue
            val other = if (r.from == boardId) r.to else r.from
            // a cable straight to another board or riser is a line of its own
            val through = if (isEnd(plan, other)) emptySet() else component(plan, other, plan.routes.filter { it.from != boardId && it.to != boardId }).filter { !isEnd(plan, it) }.toSet()
            val rs = if (through.isEmpty()) listOf(r) else plan.routes.filter { it.from in through || it.to in through }
            rs.forEach { used += it.id }
            out += Line(rs, rs.sumOf { Cables.length(plan, it) }, through.mapNotNull { id -> plan.points.firstOrNull { it.id == id } })
        }
        return out
    }

    /**
     * The number of the board line each cable is on (route id → number): lines of each board (or riser) in the order
     * they were drawn, 1, 2, 3…; a number the user gave a line ([Route.num]) is kept and skipped by the others.
     */
    fun numbers(plan: Plan): Map<Int, Int> {
        val out = HashMap<Int, Int>()
        for (b in plan.points.filter { it.kind == PointKind.BOARD || it.kind == PointKind.RISER }.sortedBy { it.id }) {
            val ls = lines(plan, b.id).sortedBy { l -> l.routes.minOf { it.id } }
            val given = ls.mapNotNull { l -> l.routes.firstOrNull { it.num > 0 }?.num }.toSet()
            var n = 0
            for (l in ls) {
                val k = l.routes.firstOrNull { it.num > 0 }?.num ?: run { do n++ while (n in given); n }
                for (r in l.routes) out.putIfAbsent(r.id, k)
            }
        }
        return out
    }

    /** The cables of the line [routeId] is on (it alone when it reaches no board). */
    fun lineOf(plan: Plan, routeId: Int): List<Route> {
        for (b in plan.points.filter { it.kind == PointKind.BOARD || it.kind == PointKind.RISER })
            lines(plan, b.id).firstOrNull { l -> l.routes.any { it.id == routeId } }?.let { return it.routes }
        return plan.routes.filter { it.id == routeId }
    }

    /** True when switch [sw] has lamps (its own, or its two-way partner's). */
    fun hasLamps(plan: Plan, sw: ElPoint): Boolean = plan.points.any { it.kind == PointKind.LAMP && it.switchId != null && (it.switchId == sw.id || it.switchId == sw.pairId) }

    private fun isEnd(plan: Plan, id: Int) = plan.points.firstOrNull { it.id == id }?.kind.let { it == PointKind.BOARD || it == PointKind.RISER }
}
