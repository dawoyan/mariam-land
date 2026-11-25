package com.dawoyan.roomik.core

import kotlin.math.abs

/**
 * Electrical boards (щиты) of a house: there can be many, on any floor, each with a name. Every point is fed by one:
 * the one the user chose ([ElPoint.board]) or else the nearest. Each board goes to elektronik as a board of its own.
 */
object Boards {
    class Ref(val floor: Int, val point: ElPoint, val index: Int) {
        val key: String get() = keyOf(floor, point)
    }

    fun keyOf(floor: Int, p: ElPoint): String = p.uid.ifBlank { "$floor:${p.id}" }

    /** Every board of the house, from the lowest floor up. */
    fun all(root: Plan): List<Ref> {
        val out = ArrayList<Ref>()
        for (fi in root.levels) for (p in root.floor(fi).points) if (p.kind == PointKind.BOARD) out += Ref(fi, p, out.size)
        return out
    }

    /** True when [a] and [b] are the same board (the same point on the same level). */
    fun same(a: Ref, b: Ref): Boolean = a.floor == b.floor && a.point.id == b.point.id

    /**
     * [root] with every board's lasting id unique: a board that shares one with another (copied over somehow) gets a
     * new one, so each can be told apart, chosen as a source and fed.
     */
    fun fixUids(root: Plan): Plan {
        val seen = HashSet<String>()
        var r = root
        for (fi in root.levels) {
            val f = r.floor(fi)
            var changed = false
            val pts = f.points.map { p ->
                if (p.kind != PointKind.BOARD || p.uid.isBlank()) p
                else if (seen.add(p.uid)) p
                else { changed = true; var u: String; do { u = java.util.UUID.randomUUID().toString().take(8) } while (!seen.add(u)); p.copy(uid = u) }
            }
            if (changed) r = r.withFloor(fi, f.copy(points = pts))
        }
        return r
    }

    /** The name people see: what they called it, or "Board 2". */
    fun name(r: Ref, ru: Boolean): String = r.point.name.ifBlank { (if (ru) "Щит " else "Board ") + (if (r.point.num > 0) r.point.num else r.index + 1) }

    /** The board feeding point [p] on floor [floor]: the chosen one, else the nearest on its floor, else the nearest. */
    fun of(root: Plan, boards: List<Ref>, floor: Int, p: ElPoint): Ref? {
        if (boards.isEmpty()) return null
        p.board?.let { k -> boards.firstOrNull { it.key == k }?.let { return it } }
        boards.filter { it.floor == floor }.minByOrNull { abs(it.point.x - p.x) + abs(it.point.y - p.y) }?.let { return it }
        val h = root.elevation(floor) + p.height
        return boards.minByOrNull { b ->
            val bh = root.elevation(b.floor) + b.point.height
            abs(b.point.x - p.x) + abs(b.point.y - p.y) + abs(bh - h) * 2.0
        }
    }

    /** The house with only what board [b] feeds: its own points (other boards and their points left out) and cables.
     * With [requireEvidence], an automatic point is included only when its cable reaches this board; this is used for
     * exporting one board, where silently assigning an unconnected point to the nearest board is unsafe. */
    fun only(root: Plan, b: Ref, requireEvidence: Boolean = false): Plan {
        val boards = all(root)
        var r = root
        for (fi in root.levels) {
            val f = root.floor(fi)
            val byId = f.points.associateBy { it.id }
            /** The board reached by the currently drawn cable. */
            fun wiredBoard(p: ElPoint): Ref? {
                val last = Circuits.toBoard(f, p.id).lastOrNull() ?: return null
                val end = listOf(last.from, last.to).firstOrNull { id -> byId[id]?.kind == PointKind.BOARD } ?: return null
                return boards.firstOrNull { it.floor == fi && it.point.id == end }
            }
            /** In a one-board house every load is naturally on that board. In a multi-board export, ownership is an
             * explicit device setting, not a room/place name and not a cable route which may be a shared feeder. */
            fun automaticBoard(p: ElPoint): Ref? = if (requireEvidence)
                p.board?.let { of(root, boards, fi, p) } ?: boards.singleOrNull()
            else wiredBoard(p) ?: p.board?.let { of(root, boards, fi, p) } ?: of(root, boards, fi, p)
            // A lamp belongs to the board of the switch which controls it.  Its position can be closer to another
            // board (especially for outdoor and garage lights), but it is still part of that switch's circuit.
            fun feeds(p: ElPoint): Ref? = if (requireEvidence) {
                p.board?.let { of(root, boards, fi, p) } ?: if (p.kind == PointKind.LAMP) p.switchId?.let(byId::get)?.let(::feeds) else automaticBoard(p)
            } else wiredBoard(p) ?: p.board?.let { of(root, boards, fi, p) } ?: if (p.kind == PointKind.LAMP)
                p.switchId?.let(byId::get)?.let(::feeds) ?: automaticBoard(p)
            else automaticBoard(p)
            val keep = f.points.filter { p ->
                when (p.kind) {
                    PointKind.BOARD -> keyOf(fi, p) == b.key
                    PointKind.RISER -> true
                    else -> feeds(p)?.key == b.key
                }
            }
            val ids = keep.map { it.id }.toSet()
            r = r.withFloor(fi, f.copy(points = keep, routes = f.routes.filter { it.from in ids && it.to in ids }))
        }
        return r
    }

    /** A cable for a line from [a] to [b]: 1.5 mm² for lights, 2.5 for sockets, more for big appliances; 5 wires for three phases. */
    fun cableFor(a: ElPoint?, b: ElPoint?): Pair<Double, Int> {
        val ends = listOfNotNull(a, b)
        val three = ends.any { it.threePhase }
        val kw = ends.filter { it.kind == PointKind.APPLIANCE }.maxOfOrNull { it.kw } ?: 0.0
        val mm2 = when {
            ends.any { it.kind == PointKind.LAMP } || ends.isNotEmpty() && ends.all { it.kind == PointKind.SWITCH || it.kind == PointKind.LAMP || it.kind == PointKind.BOARD } && ends.any { it.kind == PointKind.SWITCH } -> 1.5
            kw > (if (three) 15.0 else 7.0) -> 10.0
            kw > (if (three) 10.0 else 5.5) -> 6.0
            kw > (if (three) 6.0 else 3.5) -> 4.0
            else -> 2.5
        }
        return mm2 to if (three) 5 else 3
    }

    val SIZES = listOf(0.75, 1.0, 1.5, 2.5, 4.0, 6.0, 10.0, 16.0)
    val CORES = listOf(2, 3, 4, 5)
}

/** Hand-drawn lines made neat: straight runs at right angles, and corners drawn rounded. */
object Lines {
    /** [pts] with the points that add less than [tol] mm of shape left out (Douglas–Peucker). */
    fun simplify(pts: List<P>, tol: Double): List<P> {
        if (pts.size < 3) return pts
        val keep = BooleanArray(pts.size); keep[0] = true; keep[pts.size - 1] = true
        fun go(i: Int, j: Int) {
            if (j <= i + 1) return
            var far = -1.0; var k = -1
            for (m in i + 1 until j) { val d = Geo.distToSegment(pts[m], pts[i], pts[j]); if (d > far) { far = d; k = m } }
            if (far > tol) { keep[k] = true; go(i, k); go(k, j) }
        }
        go(0, pts.size - 1)
        return pts.filterIndexed { i, _ -> keep[i] }
    }

    /**
     * A stroke drawn by finger as runs along x and y only, starting at its first point and ending exactly at its last.
     * Jogs shorter than [tol] are dropped; turns are snapped to [grid].
     */
    fun orthogonal(stroke: List<P>, tol: Double, grid: Double = 50.0): List<P> {
        if (stroke.size < 2) return stroke
        fun g(v: Double) = Math.round(v / grid) * grid
        val s = simplify(stroke, tol)
        val out = arrayListOf(s.first())
        var cur = s.first()
        for ((k, q0) in s.withIndex().drop(1)) {
            val last = k == s.size - 1
            val q = if (last) q0 else P(g(q0.x), g(q0.y))
            val d = q - cur
            val first = if (abs(d.x) >= abs(d.y)) P(q.x, cur.y) else P(cur.x, q.y)
            if ((first - cur).len > 1) { out += first; cur = first }
            if ((q - cur).len > (if (last) 1.0 else tol)) { out += q; cur = q }
        }
        return straighten(out)
    }

    /**
     * Handles of a drawn line through [pts]: its turns (with [fixedEnds], the ends stay where they are, on their
     * devices), then the middle of each run. Dragging a run's middle moves the run sideways.
     */
    fun handles(pts: List<P>, fixedEnds: Boolean): List<P> {
        if (pts.size < 2) return emptyList()
        val turns = if (fixedEnds) pts.subList(1, pts.size - 1) else pts
        return turns + (1 until pts.size).map { (pts[it - 1] + pts[it]) * 0.5 }
    }

    /**
     * [pts] with handle [h] (from [handles]) moved to [to]. A turn just moves; a straight run along x or y moves sideways
     * with both its ends (a fixed end keeps its place, a new turn is made next to it); a slanting run gets a turn at [to].
     * Turns brought within [merge] mm of a neighbour are dropped.
     */
    fun drag(pts: List<P>, h: Int, to: P, fixedEnds: Boolean, merge: Double = 100.0): List<P> {
        if (pts.size < 2 || h < 0) return pts
        val turns = if (fixedEnds) pts.size - 2 else pts.size
        val out = pts.toMutableList()
        if (h < turns) {
            out[if (fixedEnds) h + 1 else h] = to
        } else {
            val k = h - turns
            if (k >= pts.size - 1) return pts
            val a = pts[k]; val b = pts[k + 1]
            val horiz = abs(a.y - b.y) < 1; val vert = abs(a.x - b.x) < 1
            if (!horiz && !vert) out.add(k + 1, to)
            else {
                val na = if (horiz) P(a.x, to.y) else P(to.x, a.y)
                val nb = if (horiz) P(b.x, to.y) else P(to.x, b.y)
                val lastFixed = fixedEnds && k + 1 == pts.size - 1
                out[k + 1] = nb
                if (lastFixed) out.add(k + 2, b)
                out[k] = na
                if (fixedEnds && k == 0) out.add(0, a)
            }
        }
        // turns that came onto a neighbour go
        val res = ArrayList<P>()
        for ((i, q) in out.withIndex()) {
            val end = i == 0 || i == out.size - 1
            if (!end && res.isNotEmpty() && (res.last() - q).len < merge) continue
            if (end && i > 0 && res.size > 1 && (res.last() - q).len < merge) res.removeAt(res.size - 1)
            res += q
        }
        return straighten(res)
    }

    /**
     * [pts] (runs along x and y) with its middle runs moved onto the runs of [others] lying within [tol] mm alongside, so a
     * line drawn next to another follows it and turns at the very same corners. The ends stay where they are.
     */
    fun follow(pts: List<P>, others: List<List<P>>, tol: Double): List<P> {
        if (pts.size < 4 || others.isEmpty()) return pts
        val xs = ArrayList<Double>(); val ys = ArrayList<Double>()
        for (o in others) for (k in 1 until o.size) {
            val a = o[k - 1]; val b = o[k]
            if ((b - a).len < 1) continue
            if (abs(a.x - b.x) < 1) xs += a.x else if (abs(a.y - b.y) < 1) ys += a.y
        }
        val out = pts.toMutableList()
        for (k in 2 until out.size - 1) {
            val a = out[k - 1]; val b = out[k]
            if (abs(a.x - b.x) < 1) xs.minByOrNull { abs(it - a.x) }?.takeIf { abs(it - a.x) <= tol }?.let { x ->
                out[k - 1] = P(x, a.y); out[k] = P(x, b.y) }
            else if (abs(a.y - b.y) < 1) ys.minByOrNull { abs(it - a.y) }?.takeIf { abs(it - a.y) <= tol }?.let { y ->
                out[k - 1] = P(a.x, y); out[k] = P(b.x, y) }
        }
        return straighten(out)
    }

    /** [pts] without points in the middle of a straight run. */
    fun straighten(pts: List<P>): List<P> {
        val out = ArrayList<P>()
        for (p in pts) {
            if (out.isNotEmpty() && (out.last() - p).len < 1) continue
            if (out.size >= 2) {
                val a = out[out.size - 2]; val b = out.last()
                val cross = (b.x - a.x) * (p.y - b.y) - (b.y - a.y) * (p.x - b.x)
                if (abs(cross) < 1e-3 * ((b - a).len * (p - b).len + 1)) { out[out.size - 1] = p; continue }
            }
            out += p
        }
        return out
    }
}
