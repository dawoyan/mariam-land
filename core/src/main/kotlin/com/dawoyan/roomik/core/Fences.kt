package com.dawoyan.roomik.core

import kotlin.math.abs
import kotlin.math.roundToInt

/** Fences as blocks: posts every 2.5 m and an infill that looks like the kind (pickets, boards, sheet, bars, mesh…). */
object Fences {
    const val POST_STEP = 2500.0

    fun parts(f: Fence): List<FPart> {
        val out = ArrayList<FPart>()
        val h = f.height.toDouble()
        val pts = f.points.map { P(it.x.toDouble(), it.y.toDouble()) }
        fun bar(a: P, b: P, half: Double, z0: Double, z1: Double, col: Int, clad: Facing? = null) {
            val d = (b - a).norm(); val n = d.left() * half
            out += FPart(listOf(a + n, b + n, b - n, a - n), z0, z1, col, clad)
        }
        val stone = facingOf(f)
        val half = f.thickness.coerceIn(80, 800) / 2.0
        fun post(c: P, s: Double, z1: Double, col: Int) {
            out += FPart(listOf(c + P(-s, -s), c + P(s, -s), c + P(s, s), c + P(-s, s)), 0.0, z1, col)
        }
        val col = f.kind.color
        val gaps = openings(f)
        fun inGate(s: Double) = gaps.any { (g0, g1) -> s > g0 - 60 && s < g1 + 60 }
        var start = 0.0
        for (k in 1 until pts.size) {
            val a0 = pts[k - 1]; val b0 = pts[k]
            val len0 = (b0 - a0).len
            if (len0 < 10) { start += len0; continue }
            val d = (b0 - a0).norm()
            val masonry = f.kind == FenceKind.STONE || f.kind == FenceKind.BRICK
            val posts = maxOf(1, Math.ceil(len0 / (if (masonry) f.postStep.coerceIn(800, 10000).toDouble() else POST_STEP)).toInt())
            for (i in 0..posts) {
                val s = len0 * i / posts
                if (inGate(start + s)) continue
                val c = a0 + d * s
                when (f.kind) {
                    FenceKind.BRICK, FenceKind.STONE -> when (f.posts) {
                        FencePosts.NONE -> {}
                        // cast concrete posts flush with the wall, a little wider along it
                        FencePosts.CONCRETE -> bar(c - d * 110.0, c + d * 110.0, half + 8, 0.0, h + 80, CONCRETE)
                        FencePosts.OWN -> post(c, (if (f.kind == FenceKind.BRICK) 125.0 else half) + 65, h + 120, Facings.shade(stone!!.color, 0.88))
                    }
                    FenceKind.HEDGE -> {}
                    FenceKind.METAL, FenceKind.BARS, FenceKind.MESH -> post(c, 40.0, h + 60, 0x3A3D42)
                    else -> post(c, 45.0, h + 50, 0x6E5440)
                }
            }
            // the infill, in the stretches between gates
            var from = 0.0
            val cuts = gaps.filter { (g0, g1) -> g1 > start && g0 < start + len0 }.sortedBy { it.first }
            val pieces = ArrayList<Pair<Double, Double>>()
            for ((g0, g1) in cuts) { if (g0 - start > from) pieces += from to (g0 - start); from = maxOf(from, g1 - start) }
            if (from < len0) pieces += from to len0
            for ((p0, p1) in pieces) {
            if (p1 - p0 < 10) continue
            val a = a0 + d * p0; val b = a0 + d * p1
            val len = p1 - p0
            when (f.kind) {
                FenceKind.BOARD -> bar(a, b, 12.0, 50.0, h, col)
                FenceKind.METAL -> { bar(a, b, 4.0, 50.0, h, col); bar(a, b, 15.0, h - 40, h, 0x4A4D52) }
                FenceKind.BRICK, FenceKind.STONE -> {
                    val hw = if (f.kind == FenceKind.BRICK) 125.0 else half
                    bar(a, b, hw, 0.0, h, Facings.mortar(stone!!), stone)
                    // the top: a concrete band with concrete posts, else a course of the stone
                    if (f.posts == FencePosts.CONCRETE) bar(a, b, hw + 8, h, h + 80, CONCRETE)
                    else bar(a, b, hw + 25, h, h + 60, Facings.shade(stone.color, 0.88))
                }
                FenceKind.HEDGE -> bar(a, b, 300.0, 0.0, h, col)
                FenceKind.PICKET -> {
                    bar(a, b, 12.0, 300.0, 380.0, 0x6E5440); bar(a, b, 12.0, h - 380, h - 300, 0x6E5440)
                    var x = 30.0
                    while (x + 90 < len) { bar(a + d * x, a + d * (x + 90), 10.0, 100.0, h, col); x += 150 }
                }
                FenceKind.BARS -> {
                    // wrought iron: strong rails, square bars with spikes over the top rail
                    bar(a, b, 20.0, 120.0, 200.0, col); bar(a, b, 20.0, h - 200, h - 120, col)
                    var x = 55.0
                    while (x < len) { bar(a + d * (x - 13), a + d * (x + 13), 13.0, 80.0, h + 40, col); x += 110 }
                }
                FenceKind.MESH -> {
                    // chain link: seen from afar as a grey see-through sheet in a frame
                    bar(a, b, 15.0, 80.0, 120.0, 0x6C737B); bar(a, b, 15.0, h - 40, h, 0x6C737B)
                    bar(a, b, 3.0, 120.0, h - 40, col)
                }
            }
            }
            start += len0
        }
        // the gates: strong posts either side and the leaves
        for ((i, g) in f.gates.withIndex()) {
            val (g0, g1) = gaps[i]
            val (pa, _) = at(pts, g0); val (pb, _) = at(pts, g1)
            if ((pb - pa).len < 100) continue
            post(pa, 60.0, maxOf(h, 1500.0) + 100, 0x3A3D42); post(pb, 60.0, maxOf(h, 1500.0) + 100, 0x3A3D42)
            out += gate(pa, pb, g.kind, maxOf(h, 1500.0))
        }
        return out
    }

    /**
     * The leaves of a gate of [kind] between its posts [pa] and [pb] (plan mm), [gh] high, closed: two swinging leaves,
     * one wide leaf, a sliding leaf on its rail (running on past [pb] where it slides open), a small wicket, a garage
     * door of panels, or a roller shutter in its box.
     */
    fun gate(pa: P, pb: P, kind: GateKind, gh: Double): List<FPart> {
        val out = ArrayList<FPart>()
        fun bar(a: P, b: P, half: Double, z0: Double, z1: Double, col: Int) {
            val d = (b - a).norm(); val n = d.left() * half
            out += FPart(listOf(a + n, b + n, b - n, a - n), z0, z1, col)
        }
        val da = (pb - pa).norm(); val len = (pb - pa).len
        when (kind) {
            GateKind.SECTIONAL -> {
                // a garage door: panels one over another with grooves between
                bar(pa, pb, 20.0, 0.0, gh, 0xE7E5E0)
                var z = gh / 4
                while (z < gh - 50) { bar(pa, pb, 24.0, z - 8, z + 8, 0xB9B6B0); z += gh / 4 }
            }
            GateKind.ROLLER -> {
                bar(pa, pb, 15.0, 0.0, gh, 0xC9CCCF)
                var z = 60.0
                while (z < gh) { bar(pa, pb, 18.0, z, z + 6, 0x9A9EA3); z += 80 }
                bar(pa - da * 40.0, pb + da * 40.0, 140.0, gh, gh + 260, 0xB9BCC0)   // the box it rolls into
            }
            else -> {
                val leaves = when (kind) {
                    GateKind.SWING -> listOf(pa + da * 70.0 to (pa + pb) * 0.5 - da * 10.0, (pa + pb) * 0.5 + da * 10.0 to pb - da * 70.0)
                    else -> listOf(pa + da * 70.0 to pb - da * 70.0)
                }
                for ((la, lb) in leaves) {
                    val ld = (lb - la).norm(); val ll = (lb - la).len
                    bar(la, lb, 20.0, 80.0, 140.0, GATE); bar(la, lb, 20.0, gh - 60, gh, GATE)
                    bar(la, la + ld * 50.0, 20.0, 80.0, gh, GATE); bar(lb - ld * 50.0, lb, 20.0, 80.0, gh, GATE)
                    // a sheet in the lower part, bars above it
                    bar(la, lb, 5.0, 140.0, gh * 0.55, GATE_SHEET)
                    var x = 120.0
                    while (x < ll - 60) { bar(la + ld * (x - 10), la + ld * (x + 10), 10.0, gh * 0.55, gh - 60, GATE); x += 120 }
                }
                if (kind == GateKind.SLIDING) bar(pa, pb + da * len, 30.0, 0.0, 40.0, 0x55585E)
            }
        }
        return out
    }

    /** The stone a brick or stone fence is laid of; null for other kinds. */
    fun facingOf(f: Fence): Facing? = when (f.kind) {
        FenceKind.STONE -> f.facing ?: Facing(Stone.TUF, 0xA9827A)
        FenceKind.BRICK -> f.facing ?: Facing(Stone.BRICK, FenceKind.BRICK.color, Pattern.BOND)
        else -> null
    }

    /** How many m² of stone face fence [f] has (both sides, gates left out). */
    fun faceArea(f: Fence): Double = parts(f).filter { it.clad != null }.sumOf { (it.corners[1] - it.corners[0]).len * (it.z1 - it.z0) * 2 } / 1e6

    /**
     * How high fence [f], drawn on level [fi] of [root], stands over that level's floor, mm: on the ground when drawn on
     * the ground or a floor at it or under it; higher up, on the deck or flat roof it runs round, else on that floor.
     */
    fun standsAt(root: Plan, fi: Int, f: Fence): Double {
        val plan = root.floor(fi)
        val pts = f.points.map { P(it.x.toDouble(), it.y.toDouble()) }
        // all of it on (or at the edge of) a raised deck or a flat roof: up there
        fun over(x1: Int, y1: Int, x2: Int, y2: Int): Boolean = pts.isNotEmpty() &&
            pts.all { it.x >= minOf(x1, x2) - 150 && it.x <= maxOf(x1, x2) + 150 && it.y >= minOf(y1, y2) - 150 && it.y <= maxOf(y1, y2) + 150 }
        if (fi != SITE) {
            plan.platforms.firstOrNull { it.height > 0 && over(it.x1, it.y1, it.x2, it.y2) }?.let { return it.height.toDouble() }
            plan.roofs.firstOrNull { it.kind == RoofKind.FLAT && over(it.x1, it.y1, it.x2, it.y2) }?.let { r ->
                return Roofs.faces(r, Roofs.baseOf(plan, r)).flatMap { it.pts }.maxOfOrNull { it.h } ?: Roofs.baseOf(plan, r).toDouble()
            }
        }
        return if (onGround(root, fi)) (root.groundMm - root.elevation(fi)).toDouble() else 0.0
    }

    /** Whether fence [f] on level [fi] stands on the ground (not up on a roof, a deck or a floor over the ground). */
    fun grounded(root: Plan, fi: Int, f: Fence): Boolean =
        onGround(root, fi) && standsAt(root, fi, f) == (root.groundMm - root.elevation(fi)).toDouble()

    /** Fences drawn on level [fi] stand on the ground. */
    fun onGround(root: Plan, fi: Int) = fi == SITE || fi <= root.groundFloor

    private const val GATE = 0x2E3136
    private const val CONCRETE = 0xA9A69F
    private const val GATE_SHEET = 0x4B5058

    /** Where [f]'s gates cut it: from and to, mm along the fence, each kept on the straight side it is on. */
    fun openings(f: Fence): List<Pair<Double, Double>> {
        val sides = ArrayList<Pair<Double, Double>>(); var acc = 0.0
        for (k in 1 until f.points.size) {
            val len = P((f.points[k].x - f.points[k - 1].x).toDouble(), (f.points[k].y - f.points[k - 1].y).toDouble()).len
            sides += acc to acc + len; acc += len
        }
        if (sides.isEmpty()) return f.gates.map { 0.0 to 0.0 }
        return f.gates.map { g ->
            val (s0, s1) = sides.firstOrNull { g.at <= it.second } ?: sides.last()
            val w = g.width.toDouble().coerceAtMost(s1 - s0)
            val c = g.at.toDouble().coerceIn(s0 + w / 2, s1 - w / 2)
            c - w / 2 to c + w / 2
        }
    }

    /** [f] as the lines between its gates. */
    fun pieces(f: Fence): List<List<P>> {
        val pts = f.points.map { P(it.x.toDouble(), it.y.toDouble()) }
        val total = length(f)
        val cuts = openings(f).sortedBy { it.first }
        val spans = ArrayList<Pair<Double, Double>>(); var from = 0.0
        for ((g0, g1) in cuts) { if (g0 > from) spans += from to g0; from = maxOf(from, g1) }
        if (from < total) spans += from to total
        // each span: its ends and the turns inside it
        val turnsAt = ArrayList<Double>(); var acc = 0.0
        for (k in 1 until pts.size) { acc += (pts[k] - pts[k - 1]).len; turnsAt += acc }
        return spans.filter { it.second - it.first > 1 }.map { (s0, s1) ->
            listOf(at(pts, s0).first) + turnsAt.withIndex().filter { (_, t) -> t > s0 + 0.5 && t < s1 - 0.5 }.map { pts[it.index + 1] } + at(pts, s1).first
        }
    }

    /** The point [s] mm along the line through [pts], and the way it runs there. */
    fun at(pts: List<P>, s: Double): Pair<P, P> {
        var left = s
        for (k in 1 until pts.size) {
            val a = pts[k - 1]; val b = pts[k]; val len = (b - a).len
            if (len < 1) continue
            if (left < len - 0.5 || k == pts.size - 1) return a + (b - a).norm() * left.coerceIn(0.0, len) to (b - a).norm()
            left -= len
        }
        return (pts.firstOrNull() ?: P(0.0, 0.0)) to P(1.0, 0.0)
    }

    /** How far along [f] (mm from its first point) the point of it nearest [w] is, and how far [w] is from it. */
    fun project(f: Fence, w: P): Pair<Double, Double> {
        var start = 0.0; var best = Double.MAX_VALUE; var bestS = 0.0
        for (k in 1 until f.points.size) {
            val a = P(f.points[k - 1].x.toDouble(), f.points[k - 1].y.toDouble()); val b = P(f.points[k].x.toDouble(), f.points[k].y.toDouble())
            val len = (b - a).len; if (len < 1) continue
            val d = (b - a).norm()
            val t = ((w - a).x * d.x + (w - a).y * d.y).coerceIn(0.0, len)
            val dist = (a + d * t - w).len
            if (dist < best) { best = dist; bestS = start + t }
            start += len
        }
        return bestS to best
    }

    /** The turns of the fence, then the middle of each side (dragging a middle adds a turn there). */
    fun handles(f: Fence): List<P> {
        val pts = f.points.map { P(it.x.toDouble(), it.y.toDouble()) }
        return pts + (1 until pts.size).map { (pts[it - 1] + pts[it]) * 0.5 }
    }

    /** Drag handle [h] (see [handles]) to [to]; -1 moves the whole fence by [dx], [dy].
     *  A turn dropped on its neighbour is removed. */
    fun drag(f: Fence, h: Int, to: P, dx: Int, dy: Int): Fence {
        val n = f.points.size
        val q = Node(0, to.x.roundToInt(), to.y.roundToInt())
        val pts = when {
            h < 0 -> f.points.map { Node(0, it.x + dx, it.y + dy) }
            h < n -> f.points.mapIndexed { i, it -> if (i == h) q else it }.let { list ->
                if (list.size > 2 && listOfNotNull(list.getOrNull(h - 1), list.getOrNull(h + 1)).any { o -> abs(o.x - q.x) + abs(o.y - q.y) < 150 })
                    list.filterIndexed { i, _ -> i != h } else list
            }
            h < 2 * n - 1 -> f.points.toMutableList().apply { add(h - n + 1, q) }
            else -> f.points
        }
        return f.copy(points = pts.mapIndexed { i, it -> Node(i, it.x, it.y) })
    }

    fun length(f: Fence): Double = (1 until f.points.size).sumOf { k ->
        P((f.points[k].x - f.points[k - 1].x).toDouble(), (f.points[k].y - f.points[k - 1].y).toDouble()).len
    }
}
