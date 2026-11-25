package com.dawoyan.roomik.core

import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Cables laid by themselves: from devices to their board the short way a fitter would take, along x and y with few
 * turns, through doors and passages rather than through walls (a wall is crossed only when that saves a long way round).
 */
object Autoroute {
    /** Grid step, mm. */
    const val STEP = 100.0
    /** What a turn costs, as so many mm of cable. */
    const val TURN = 400.0
    /** What going through a wall (not at a door) costs, as so many mm of cable. */
    const val CROSS = 3000.0
    /** What each step inside a wall costs more: a cable runs beside a wall, never along its top. */
    const val IN_WALL = 3000.0
    /** What each step out in the open floor costs more for a socket cable: it keeps to the walls. */
    const val OPEN = 250.0
    /** A cable runs at least this far from a wall's face. */
    const val OFF_FACE = 40.0
    /** Within this of a wall's face a cable is "along the wall". */
    const val HUG = 300.0
    /** A cable along a wall is fixed this far from its face. */
    const val LANE = 80.0
    /** What each step outside the house costs more: cables keep indoors. */
    const val OUTSIDE = 400.0

    /** The floor as a grid; [h] and [v]: what moving right from a cell, or down from it, costs over its length. */
    class Grid(val x0: Double, val y0: Double, val nx: Int, val ny: Int) {
        val h = DoubleArray(nx * ny); val v = DoubleArray(nx * ny)
        /** Cells inside a wall (doors and passages left out). */
        val inWall = BooleanArray(nx * ny)
        /** Cells close beside a wall, where a cable is fixed along it. */
        val near = BooleanArray(nx * ny)
        /** Cells in no room. */
        val outside = BooleanArray(nx * ny)
        /** The walls running along x or y: (one end, the other end, thickness), for laying cables beside them. */
        val walls = ArrayList<Triple<P, P, Double>>()
        fun at(i: Int, j: Int) = P(x0 + i * STEP, y0 + j * STEP)
        fun cell(p: P): Int = ((p.x - x0) / STEP).roundToInt().coerceIn(0, nx - 1) + ((p.y - y0) / STEP).roundToInt().coerceIn(0, ny - 1) * nx
    }

    fun grid(plan: Plan): Grid {
        val xs = plan.nodes.map { it.x.toDouble() } + plan.points.map { it.x.toDouble() }
        val ys = plan.nodes.map { it.y.toDouble() } + plan.points.map { it.y.toDouble() }
        if (xs.isEmpty()) return Grid(0.0, 0.0, 1, 1)
        val x0 = floor((xs.min() - 1000) / STEP) * STEP; val y0 = floor((ys.min() - 1000) / STEP) * STEP
        val nx = (((xs.max() + 1000 - x0) / STEP).toInt() + 1).coerceAtMost(800)
        val ny = (((ys.max() + 1000 - y0) / STEP).toInt() + 1).coerceAtMost(800)
        val g = Grid(x0, y0, nx, ny)
        val doors = plan.openings.filter { it.kind != OpeningKind.WINDOW }
        val inWall = BooleanArray(nx * ny); val open = BooleanArray(nx * ny)
        val reach = ((HUG + 400) / STEP).toInt() + 1
        for (w in plan.walls) {
            val a = plan.pos(w.a) ?: continue; val b = plan.pos(w.b) ?: continue
            if ((b - a).len < 1) continue
            val dir = (b - a) * (1.0 / (b - a).len)
            val mine = doors.filter { o -> Geo.distToSegment(P(o.x.toDouble(), o.y.toDouble()), a, b) < w.thickness }
            val i0 = max(0, floor((min(a.x, b.x) - x0) / STEP).toInt() - reach); val i1 = min(nx - 1, ((max(a.x, b.x) - x0) / STEP).toInt() + reach)
            val j0 = max(0, floor((min(a.y, b.y) - y0) / STEP).toInt() - reach); val j1 = min(ny - 1, ((max(a.y, b.y) - y0) / STEP).toInt() + reach)
            fun cost(c: P, d: P): Double {
                val q = meet(c, d, a, b) ?: return 0.0
                val gap = mine.any { o -> (P(o.x.toDouble(), o.y.toDouble()) - q).len < o.width / 2.0 - 50 }
                return if (gap) 0.0 else CROSS
            }
            for (j in j0..j1) for (i in i0..i1) {
                val c = g.at(i, j); val k = i + j * nx
                val d = Geo.distToSegment(c, a, b)
                if (d < w.thickness / 2.0 + OFF_FACE) {
                    val o = mine.firstOrNull { o -> abs((c - P(o.x.toDouble(), o.y.toDouble())).let { it.x * dir.x + it.y * dir.y }) < o.width / 2.0 - 50 }
                    if (o == null) inWall[k] = true else open[k] = true
                }
                if (d < w.thickness / 2.0 + HUG) g.near[k] = true
                if (i + 1 < nx) g.h[k] = max(g.h[k], cost(c, g.at(i + 1, j)))
                if (j + 1 < ny) g.v[k] = max(g.v[k], cost(c, g.at(i, j + 1)))
            }
        }
        // a door through one wall stays open where another wall's end runs into it
        for (k in 0 until nx * ny) g.inWall[k] = inWall[k] && !open[k]
        val rooms = Rooms.detect(plan).map { it.polygon }
        if (rooms.isNotEmpty()) for (j in 0 until ny) for (i in 0 until nx) {
            val c = g.at(i, j); g.outside[i + j * nx] = rooms.none { Geo.inside(c, it) }
        }
        for (w in plan.walls) {
            val a = plan.pos(w.a) ?: continue; val b = plan.pos(w.b) ?: continue
            if ((b - a).len >= 1 && (abs(a.x - b.x) < 1 || abs(a.y - b.y) < 1)) g.walls += Triple(a, b, w.thickness.toDouble())
        }
        return g
    }

    /** Where segments [c]-[d] and [a]-[b] cross, or null. */
    private fun meet(c: P, d: P, a: P, b: P): P? {
        val r = d - c; val s = b - a
        val den = cross(r, s)
        if (abs(den) < 1e-9) return null
        val t = cross(a - c, s) / den; val u = cross(a - c, r) / den
        return if (t in 0.0..1.0 && u in 0.0..1.0) c + r * t else null
    }

    /**
     * The cells a cable may leave [p] from: straight out from its wall ([n]) to the first cell beside the wall; with
     * [back] (a board) also straight through the wall behind it, to the other side.
     */
    private fun startCells(g: Grid, p: P, n: P?, back: Boolean): List<Int> {
        val c = g.cell(p)
        if (n == null) return listOf(c)
        fun out(d: P): Int? { for (k in 0..8) { val m = g.cell(p + d * (k * STEP / 2)); if (!g.inWall[m]) return m }; return null }
        val front = out(n) ?: c
        // behind: into the wall, then out of it on the other side
        fun through(d: P): Int? {
            var crossed = false
            for (k in 0..12) { val m = g.cell(p + d * (k * STEP / 2)); if (g.inWall[m]) crossed = true else if (crossed) return m }
            return null
        }
        val behind = if (back) through(n * -1.0)?.takeIf { it != front } else null
        return listOfNotNull(front, behind)
    }

    /**
     * The turns of the cheapest way from [a] to [b] on [g] (the ends themselves left out), running along x and y.
     * [na], [nb]: the way out of the wall a device at that end sits on (null for one hanging free): its cable comes
     * straight out to beside the wall and runs on there. [hug]: keep to the walls (socket and appliance cables).
     */
    fun route(g: Grid, a: P, b: P, na: P? = null, nb: P? = null, hug: Boolean = false, backA: Boolean = false, backB: Boolean = false): List<P> {
        val ss = startCells(g, a, na, backA); val ts = startCells(g, b, nb, backB)
        val n = g.nx * g.ny
        val best = DoubleArray(n * 4) { Double.MAX_VALUE }
        val came = IntArray(n * 4) { -1 }
        fun est(c: Int) = ts.minOf { t -> (abs(c % g.nx - t % g.nx) + abs(c / g.nx - t / g.nx)) * STEP }
        val pq = PriorityQueue<DoubleArray>(compareBy { it[0] })
        for (s in ss) for (d in 0..3) { best[s * 4 + d] = 0.0; pq += doubleArrayOf(est(s), (s * 4 + d).toDouble(), 0.0) }
        var end = -1
        while (pq.isNotEmpty()) {
            val top = pq.poll()
            val st = top[1].toInt(); val gc = top[2]
            if (gc > best[st]) continue
            val c = st / 4; val d = st % 4
            if (c in ts) { end = st; break }
            val i = c % g.nx; val j = c / g.nx
            for (nd in 0..3) {
                if (nd == (d xor 1)) continue
                val (m, extra) = when (nd) {
                    0 -> if (i + 1 < g.nx) (c + 1) to g.h[c] else continue
                    1 -> if (i > 0) (c - 1) to g.h[c - 1] else continue
                    2 -> if (j + 1 < g.ny) (c + g.nx) to g.v[c] else continue
                    else -> if (j > 0) (c - g.nx) to g.v[c - g.nx] else continue
                }
                val ns = m * 4 + nd
                val nc = gc + STEP + extra + (if (nd != d) TURN else 0.0) + (if (g.inWall[m]) IN_WALL else 0.0) +
                    (if (hug && !g.near[m]) OPEN else 0.0) + (if (g.outside[m]) OUTSIDE else 0.0)
                if (nc < best[ns]) { best[ns] = nc; came[ns] = st; pq += doubleArrayOf(nc + est(m), ns.toDouble(), nc) }
            }
        }
        if (end < 0) return elbow(a, b, true)
        // the cells walked, from the start
        val cells = ArrayList<Int>()
        var st = end
        while (st >= 0) { cells += st / 4; st = came[st] }
        cells.reverse()
        val dedup = cells.filterIndexed { k, c -> k == 0 || cells[k - 1] != c }
        val corners = Lines.straighten(dedup.map { g.at(it % g.nx, it / g.nx) }).toMutableList()
        if (corners.size < 2) return if (na == null && nb == null) elbow(a, b, true) else square(listOf(a, corners[0], b))
        attach(corners, a, na)
        corners.reverse(); attach(corners, b, nb); corners.reverse()
        return square(beside(g, square(listOf(a) + corners + b))).drop(1).dropLast(1)
    }

    /**
     * [pts] with each run in the middle that goes along a wall, close beside it, put at [LANE] from the wall's face:
     * every cable along a wall at the same distance from it, never on it.
     */
    private fun beside(g: Grid, pts: List<P>): List<P> {
        val out = pts.toMutableList()
        for (k in 1 until out.size - 2) {
            val u = out[k]; val v = out[k + 1]
            val alongX = abs(u.y - v.y) < 1; val alongY = abs(u.x - v.x) < 1
            if (alongX == alongY) continue
            var bestD = Double.MAX_VALUE; var to: Double? = null
            for ((a, b, t) in g.walls) {
                if (alongX && abs(a.y - b.y) < 1) {
                    val lo = min(a.x, b.x); val hi = max(a.x, b.x)
                    if (min(u.x, v.x) > hi - 1 || max(u.x, v.x) < lo + 1) continue
                    val d = abs(u.y - a.y); if (d > t / 2 + HUG || d >= bestD) continue
                    bestD = d; to = a.y + (if (u.y >= a.y) 1 else -1) * (t / 2 + LANE)
                } else if (alongY && abs(a.x - b.x) < 1) {
                    val lo = min(a.y, b.y); val hi = max(a.y, b.y)
                    if (min(u.y, v.y) > hi - 1 || max(u.y, v.y) < lo + 1) continue
                    val d = abs(u.x - a.x); if (d > t / 2 + HUG || d >= bestD) continue
                    bestD = d; to = a.x + (if (u.x >= a.x) 1 else -1) * (t / 2 + LANE)
                }
            }
            val c = to ?: continue
            if (alongX) { out[k] = u.copy(y = c); out[k + 1] = v.copy(y = c) } else { out[k] = u.copy(x = c); out[k + 1] = v.copy(x = c) }
        }
        return out
    }

    /**
     * The grid run at the start of [cs] joined onto end [e]: a run going away from the wall is moved onto the end; one
     * along the wall stays where it is, beside the wall, and a short stub comes out to it from the end.
     */
    private fun attach(cs: MutableList<P>, e: P, n: P?) {
        val along = abs(cs[0].y - cs[1].y) < 1          // the first run goes along x
        val outY = n != null && abs(n.y) > abs(n.x)     // the end comes out of its wall along y
        val outX = n != null && !outY
        when {
            along && outY -> cs[0] = P(e.x, cs[0].y)                                   // stub along y, then the run
            !along && outX -> cs[0] = P(cs[0].x, e.y)                                  // stub along x, then the run
            along -> { cs[0] = cs[0].copy(y = e.y); cs[1] = cs[1].copy(y = e.y) }     // the run moved onto the end
            else -> { cs[0] = cs[0].copy(x = e.x); cs[1] = cs[1].copy(x = e.x) }
        }
    }

    /** [pts] straightened, with a corner put in wherever two points in a row are not on one x or one y. */
    private fun square(pts: List<P>): List<P> {
        val out = ArrayList<P>()
        for (q in Lines.straighten(pts)) {
            val l = out.lastOrNull()
            if (l != null && abs(l.x - q.x) >= 1 && abs(l.y - q.y) >= 1) out += P(q.x, l.y)
            out += q
        }
        return Lines.straighten(out)
    }

    /** [a] to [b] with one turn at most, the first run along x when [horizontal]. */
    private fun elbow(a: P, b: P, horizontal: Boolean): List<P> =
        if (abs(a.x - b.x) < 1 || abs(a.y - b.y) < 1) emptyList() else listOf(if (horizontal) P(b.x, a.y) else P(a.x, b.y))

    /**
     * The board (or else the riser bringing one) feeding [p] on this floor ([floor] of [root]): the one it is given to,
     * else the nearest. A board chosen on another floor comes in by the riser nearest to [p].
     */
    fun boardFor(plan: Plan, p: ElPoint, root: Plan? = null, floor: Int = 0): ElPoint? {
        val here = plan.points.filter { it.kind == PointKind.BOARD }
        val risers = plan.points.filter { it.kind == PointKind.RISER }
        fun near(l: List<ElPoint>) = l.minByOrNull { abs(it.x - p.x) + abs(it.y - p.y) }
        p.board?.let { k ->
            here.firstOrNull { Boards.keyOf(floor, it) == k || it.uid.isNotBlank() && it.uid == k }?.let { return it }
            // given to a board on another floor: up or down the riser
            if (root != null && Boards.all(root).any { it.key == k && it.floor != floor }) near(risers)?.let { return it }
        }
        return near(here) ?: near(risers)
    }

    /** Pairs joining every one of [rest] to [roots] or to one joined before, the shortest first (a short spanning tree). */
    private fun tree(roots: List<ElPoint>, rest: List<ElPoint>): List<Pair<ElPoint, ElPoint>> {
        val inTree = roots.toMutableList(); val left = rest.toMutableList()
        val out = ArrayList<Pair<ElPoint, ElPoint>>()
        while (left.isNotEmpty()) {
            var bp: Pair<ElPoint, ElPoint>? = null; var bd = Int.MAX_VALUE
            for (u in inTree) for (v in left) { val d = abs(u.x - v.x) + abs(u.y - v.y); if (d < bd) { bd = d; bp = u to v } }
            val (u, v) = bp ?: break
            out += u to v; inTree += v; left -= v
        }
        return out
    }

    /** The room (index in [rooms]) device [p] serves: the one in front of its wall, or the one it is in; -1: none. */
    fun roomOf(rooms: List<Room>, p: ElPoint): Int {
        val at = P(p.x.toDouble(), p.y.toDouble())
        val tries = if (p.nx != 0.0 || p.ny != 0.0) listOf(at + P(p.nx, p.ny) * 200.0, at + P(p.nx, p.ny) * 60.0, at) else listOf(at)
        for (t in tries) rooms.indexOfFirst { Geo.inside(t, it.polygon) }.takeIf { it >= 0 }?.let { return it }
        return -1
    }

    /** Sockets and small appliances: they share the cables of their room (a socket marked [ElPoint.ownLine] has a line of its own). */
    private fun shared(p: ElPoint) = p.kind == PointKind.SOCKET && !p.ownLine || p.kind == PointKind.APPLIANCE && !(p.ownLine || p.kw >= 3.0 || p.threePhase)

    /**
     * The part of the floor [p] is in, for sharing a cable: its room, else (outside every closed room: a garage drawn
     * open, a yard) the nearest place, else all of the outside together.
     */
    fun areaOf(plan: Plan, rooms: List<Room>, p: ElPoint): Int {
        val r = roomOf(rooms, p)
        if (r >= 0) return r
        val i = plan.places.withIndex().minByOrNull { (_, pl) -> abs(pl.x - p.x).toDouble() + abs(pl.y - p.y) }?.index ?: return -1
        return -2 - i
    }

    /**
     * Cables laid from devices [ids] to their boards: lamps on a switch go board → switch → lamps; other lights join
     * the nearest light already joined, back to the board; sockets (and small appliances) join the cable of the sockets
     * already in their room going to the same board — a room's sockets are one cable — and only a room with none gets a
     * cable of its own from the board. 110 V sockets keep to their own line. An appliance on a line of its own goes
     * straight to the board. Lights at the ceiling, the rest at [powerLevel]. Devices already reaching a board are left
     * as they are. [root] and [floor]: the house and this floor's place in it, for boards chosen on other floors.
     * Returns the plan and how many cables were laid.
     */
    fun join(plan: Plan, ids: Collection<Int>, powerLevel: RouteLevel, root: Plan? = null, floor: Int = 0): Pair<Plan, Int> {
        var p = plan
        val g = grid(plan)
        val rooms = Rooms.detect(plan)
        var laid = 0
        fun connected(id: Int) = Circuits.toBoard(p, id).isNotEmpty()
        /** The board or riser the cables from [id] reach, or null. */
        fun reaches(id: Int): Int? = Circuits.toBoard(p, id).let { rs ->
            if (rs.isEmpty()) null else {
                var c = id
                for (r in rs) c = if (r.from == c) r.to else r.from
                c
            }
        }
        fun lay(u: ElPoint, v: ElPoint, level: RouteLevel, followExisting: Boolean = true) {
            val a = Cables.end(p, u).first; val b = Cables.end(p, v).first
            val others = if (followExisting) p.routes.mapNotNull { Cables.plan(p, it) } else emptyList()
            val via = Lines.follow(listOf(a) + route(g, a, b, Cables.normal(p, u), Cables.normal(p, v), Circuits.kindOf(u) != Circuits.Kind.LIGHT && Circuits.kindOf(v) != Circuits.Kind.LIGHT,
                u.kind == PointKind.BOARD, v.kind == PointKind.BOARD) + b, others, 150.0)
                .drop(1).dropLast(1)
            p = PlanOps.addRoute(p, u.id, v.id, via, level); laid++
        }
        fun boardOf(d: ElPoint) = boardFor(plan, d, root, floor)
        val chosen = plan.points.filter { it.id in ids && Circuits.kindOf(it.kind) != null && !connected(it.id) }
        // lamps under a switch bring the switch along
        val switches0 = (chosen.filter { it.kind == PointKind.SWITCH } +
            chosen.mapNotNull { l -> l.switchId?.let { s -> plan.points.firstOrNull { it.id == s } } }).distinctBy { it.id }
        // a two-way pair goes together: the partner of a switch wired now is wired too
        val switches = (switches0 + switches0.mapNotNull { s -> s.pairId?.let { pid -> plan.points.firstOrNull { it.id == pid && it.kind == PointKind.SWITCH } } }).distinctBy { it.id }
        val lights = (chosen.filter { !shared(it) && it.kind != PointKind.APPLIANCE } + switches).distinctBy { it.id }
        for ((board, mine) in lights.groupBy { boardOf(it) }) {
            board ?: continue
            for (sw in switches.filter { it in mine }.sortedBy { it.id }) {
                // a switch joins the switch put before it in its room (the line goes on from box to box), else the board
                if (!connected(sw.id)) {
                    // the second switch of a two-way pair: the cable (travellers) comes from its partner
                    val partner = sw.pairId?.let { pid -> p.points.firstOrNull { it.id == pid && it.pairId == sw.id } }
                    if (partner != null && connected(partner.id)) lay(partner, sw, RouteLevel.CEILING)
                    else {
                        val area = areaOf(plan, rooms, sw)
                        val prev = p.points.filter { a -> a.kind == PointKind.SWITCH && a.id != sw.id && areaOf(plan, rooms, a) == area &&
                            reaches(a.id)?.let { b -> sw.board == null || b == board.id } == true }
                        // Switches may share a cable run, but they are never functional endpoints for one another.
                        // The router can still follow existing cable geometry; electrically each switch starts at the board.
                        lay(board, sw, RouteLevel.CEILING)
                    }
                }
                for ((u, v) in tree(listOf(sw), chosen.filter { it.kind == PointKind.LAMP && it.switchId == sw.id })) lay(u, v, RouteLevel.CEILING)
            }
            for ((u, v) in tree(listOf(board), mine.filter { it.kind == PointKind.LAMP && switches.none { s -> s.id == it.switchId } })) lay(u, v, RouteLevel.CEILING)
        }
        // Appliances and Own-line sockets start at the board on a separate routed run. They may cross another cable,
        // but they do not follow it visually or electrically through another device.
        for (a in chosen.filter { (it.kind == PointKind.APPLIANCE || it.kind == PointKind.SOCKET) && !shared(it) }) boardOf(a)?.let { lay(it, a, powerLevel, followExisting = false) }
        // sockets: by room, line voltage and the board asked for
        val power = chosen.filter { shared(it) }
        for ((key, group) in power.groupBy { Triple(areaOf(plan, rooms, it), Circuits.kindOf(it), it.board) }) {
            val (area, kind, asked) = key
            val board = boardOf(group.first()) ?: continue
            // each socket goes on from the one put just before it (in time) in its room, on the same kind of line and
            // board; the first of a room comes from the board
            for (v in group.sortedBy { it.id }) {
                val prev = p.points.filter { a ->
                    shared(a) && a.id != v.id && Circuits.kindOf(a) == kind && areaOf(plan, rooms, a) == area &&
                        reaches(a.id)?.let { b -> asked == null || b == board.id } == true
                }
                lay(prev.filter { it.id < v.id }.maxByOrNull { it.id } ?: prev.maxByOrNull { it.id } ?: board, v, powerLevel)
            }
        }
        return p to laid
    }

    /**
     * Device [id] wired again after its board or voltage changed: its own cables go, and it and the devices that hung
     * on them are joined anew.
     */
    fun rewire(plan: Plan, id: Int, powerLevel: RouteLevel, root: Plan? = null, floor: Int = 0): Plan =
        rewire(plan, listOf(id), powerLevel, root, floor)

    /** Devices [ids] wired again: their cables go, and they and the devices that hung on them are joined anew. */
    fun rewire(plan: Plan, ids: Collection<Int>, powerLevel: RouteLevel, root: Plan? = null, floor: Int = 0): Plan {
        val before = plan.points.filter { Circuits.kindOf(it.kind) != null && Circuits.toBoard(plan, it.id).isNotEmpty() }.map { it.id }
        var p = plan
        for (r in plan.routes.filter { it.from in ids || it.to in ids }) p = PlanOps.deleteRoute(p, r.id)
        val lost = before.filter { Circuits.toBoard(p, it).isEmpty() }
        return join(p, (lost + ids).distinct(), powerLevel, root, floor).first
    }
}
