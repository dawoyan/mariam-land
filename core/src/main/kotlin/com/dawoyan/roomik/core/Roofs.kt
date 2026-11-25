package com.dawoyan.roomik.core

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.tan

/** A corner of a roof surface: plan point (mm) and height (mm above the floor the roof stands on). */
class RP(val p: P, val h: Double)

/** A flat piece of a roof: a polygon in 3D; [gable] pieces are walls (the triangle ends), the rest is roofing. */
class RoofFace(val pts: List<RP>, val gable: Boolean)

object Roofs {
    /**
     * The surfaces of [r] on a floor whose walls are [wallH] mm high: slopes through the top of the walls at the wall
     * lines, continued over the eaves, and the gable triangles that close the ends.
     */
    fun faces(r: Roof, wallH: Int): List<RoofFace> {
        val o = r.overhang.toDouble()
        val t = tan(Math.toRadians(r.pitch.coerceIn(0, 75).toDouble()))
        val base = wallH.toDouble()
        // work in a frame where the ridge runs along "u" (x) and the slopes fall along "v" (y); map back at the end
        val ax = r.alongX
        val u0 = (if (ax) r.x1 else r.y1).toDouble(); val u1 = (if (ax) r.x2 else r.y2).toDouble()
        val v0 = (if (ax) r.y1 else r.x1).toDouble(); val v1 = (if (ax) r.y2 else r.x2).toDouble()
        fun pt(u: Double, v: Double, h: Double) = RP(if (ax) P(u, v) else P(v, u), h)
        val out = ArrayList<RoofFace>()
        val w = v1 - v0
        val eave = base - o * t
        when (r.kind) {
            RoofKind.FLAT -> {
                val top = base + 200
                out += RoofFace(listOf(pt(u0 - o, v0 - o, top), pt(u1 + o, v0 - o, top), pt(u1 + o, v1 + o, top), pt(u0 - o, v1 + o, top)), false)
            }
            RoofKind.GABLE -> {
                val vm = (v0 + v1) / 2; val ridge = base + w / 2 * t
                out += RoofFace(listOf(pt(u0 - o, v0 - o, eave), pt(u1 + o, v0 - o, eave), pt(u1 + o, vm, ridge), pt(u0 - o, vm, ridge)), false)
                out += RoofFace(listOf(pt(u0 - o, vm, ridge), pt(u1 + o, vm, ridge), pt(u1 + o, v1 + o, eave), pt(u0 - o, v1 + o, eave)), false)
                out += RoofFace(listOf(pt(u0, v0, base), pt(u0, v1, base), pt(u0, vm, ridge)), true)
                out += RoofFace(listOf(pt(u1, v0, base), pt(u1, v1, base), pt(u1, vm, ridge)), true)
            }
            RoofKind.HIP -> {
                // the ridge is shorter by half the width at each end; a square house gets a pyramid
                val vm = (v0 + v1) / 2; val ridge = base + w / 2 * t
                val r0 = minOf(u0 + w / 2, (u0 + u1) / 2); val r1 = maxOf(u1 - w / 2, (u0 + u1) / 2)
                val a = pt(u0 - o, v0 - o, eave); val b = pt(u1 + o, v0 - o, eave); val c = pt(u1 + o, v1 + o, eave); val d = pt(u0 - o, v1 + o, eave)
                val p0 = pt(r0, vm, ridge); val p1 = pt(r1, vm, ridge)
                out += RoofFace(listOf(a, b, p1, p0), false)
                out += RoofFace(listOf(b, c, p1), false)
                out += RoofFace(listOf(c, d, p0, p1), false)
                out += RoofFace(listOf(d, a, p0), false)
            }
            RoofKind.SHED -> {
                // low at v0 (or at v1 when flipped), rising to the other side
                val lo = if (r.flip) v1 else v0; val hi = if (r.flip) v0 else v1
                val s = if (r.flip) -1.0 else 1.0
                val top = base + w * t
                out += RoofFace(listOf(pt(u0 - o, lo - s * o, eave), pt(u1 + o, lo - s * o, eave), pt(u1 + o, hi + s * o, top + o * t), pt(u0 - o, hi + s * o, top + o * t)), false)
                out += RoofFace(listOf(pt(u0, lo, base), pt(u0, hi, base), pt(u0, hi, top)), true)
                out += RoofFace(listOf(pt(u1, lo, base), pt(u1, hi, base), pt(u1, hi, top)), true)
                out += RoofFace(listOf(pt(u0, hi, base), pt(u1, hi, base), pt(u1, hi, top), pt(u0, hi, top)), true)
            }
        }
        return out
    }

    /** Roofing area in m², slopes as they lie (not their shadow on the plan). */
    fun area(r: Roof): Double {
        val o = r.overhang * 2.0
        val plan = (r.x2 - r.x1 + o) * (r.y2 - r.y1 + o) / 1e6
        return if (r.kind == RoofKind.FLAT) plan else plan / cos(Math.toRadians(r.pitch.coerceIn(0, 75).toDouble()))
    }

    /**
     * Where rain runs: for each sloped face, its middle on the plan and the downhill unit direction. A flat roof has none.
     */
    fun flows(r: Roof): List<Pair<P, P>> {
        if (r.kind == RoofKind.FLAT || r.pitch <= 0) return emptyList()
        val out = ArrayList<Pair<P, P>>()
        for (f in faces(r, 0)) {
            if (f.gable || f.pts.size < 3) continue
            // plane through three corners: h = a·x + b·y + c; downhill is -(a, b)
            val (p0, p1, p2) = Triple(f.pts[0], f.pts[1], f.pts[2])
            val ux = p1.p.x - p0.p.x; val uy = p1.p.y - p0.p.y; val uh = p1.h - p0.h
            val vx = p2.p.x - p0.p.x; val vy = p2.p.y - p0.p.y; val vh = p2.h - p0.h
            val det = ux * vy - uy * vx
            if (kotlin.math.abs(det) < 1e-6) continue
            val a = (uh * vy - uy * vh) / det
            val b = (ux * vh - uh * vx) / det
            val d = P(-a, -b)
            if (d.len < 1e-9) continue
            val c = P(f.pts.sumOf { it.p.x } / f.pts.size, f.pts.sumOf { it.p.y } / f.pts.size)
            out += c to d.norm()
        }
        return out
    }

    /** Handles of a roof on the plan: 4 corners, then the middles of the top, right, bottom and left sides. */
    fun handles(r: Roof): List<P> {
        val mx = (r.x1 + r.x2) / 2.0; val my = (r.y1 + r.y2) / 2.0
        return listOf(P(r.x1.toDouble(), r.y1.toDouble()), P(r.x2.toDouble(), r.y1.toDouble()), P(r.x2.toDouble(), r.y2.toDouble()), P(r.x1.toDouble(), r.y2.toDouble()),
            P(mx, r.y1.toDouble()), P(r.x2.toDouble(), my), P(mx, r.y2.toDouble()), P(r.x1.toDouble(), my))
    }

    /** [r] with handle [h] (see [handles]; 8 moves the whole roof by [dx], [dy]) dragged to [to]; never smaller than 50 cm. */
    fun drag(r: Roof, h: Int, to: P, dx: Int = 0, dy: Int = 0): Roof =
        dragRect(intArrayOf(r.x1, r.y1, r.x2, r.y2), h, to, dx, dy).let { r.copy(x1 = it[0], y1 = it[1], x2 = it[2], y2 = it[3]) }

    /** Handles of any rectangle (x1, y1, x2, y2), as for a roof. */
    fun handlesRect(x1: Int, y1: Int, x2: Int, y2: Int): List<P> = handles(Roof(0, x1, y1, x2, y2))

    /** A rectangle (x1, y1, x2, y2) with handle [h] dragged to [to], or moved by [dx], [dy] when [h] is 8. */
    fun dragRect(r: IntArray, h: Int, to: P, dx: Int = 0, dy: Int = 0, min: Int = 500): IntArray {
        val (x1, y1, x2, y2) = r.toList()
        if (h == 8) return intArrayOf(x1 + dx, y1 + dy, x2 + dx, y2 + dy)
        val x = to.x.roundToInt(); val y = to.y.roundToInt()
        val left = h == 0 || h == 3 || h == 7; val right = h == 1 || h == 2 || h == 5
        val top = h == 0 || h == 1 || h == 4; val bottom = h == 2 || h == 3 || h == 6
        return intArrayOf(if (left) minOf(x, x2 - min) else x1, if (top) minOf(y, y2 - min) else y1,
            if (right) maxOf(x, x1 + min) else x2, if (bottom) maxOf(y, y1 + min) else y2)
    }

    /** The base height of [r] on [plan]: as set, or the top of the highest wall standing under it. */
    fun baseOf(plan: Plan, r: Roof): Int {
        r.base?.let { return it }
        fun inside(p: P?) = p != null && p.x >= r.x1 - 1 && p.x <= r.x2 + 1 && p.y >= r.y1 - 1 && p.y <= r.y2 + 1
        return plan.walls.filter { inside(plan.pos(it.a)) && inside(plan.pos(it.b)) }.maxOfOrNull { plan.heightOf(it) } ?: plan.wallHeight
    }

    /** The sloped planes of [r] standing on [base]: h = a·x + b·y + c for each (a, b, c). */
    fun planes(r: Roof, base: Int): List<DoubleArray> = faces(r, base).filter { !it.gable && it.pts.size >= 3 }.mapNotNull { f ->
        val (p0, p1, p2) = Triple(f.pts[0], f.pts[1], f.pts[2])
        val ux = p1.p.x - p0.p.x; val uy = p1.p.y - p0.p.y; val uh = p1.h - p0.h
        val vx = p2.p.x - p0.p.x; val vy = p2.p.y - p0.p.y; val vh = p2.h - p0.h
        val det = ux * vy - uy * vx
        if (kotlin.math.abs(det) < 1e-6) null else {
            val a = (uh * vy - uy * vh) / det; val b = (ux * vh - uh * vx) / det
            doubleArrayOf(a, b, p0.h - a * p0.p.x - b * p0.p.y)
        }
    }

    /**
     * Height of the roof surface over [p] (mm above the floor), or null when [p] is not under it. Every roof kind here is
     * convex, so the surface is the lowest of its planes.
     */
    fun surfaceAt(r: Roof, base: Int, p: P, planes: List<DoubleArray> = planes(r, base)): Double? {
        val o = r.overhang
        if (p.x < r.x1 - o || p.x > r.x2 + o || p.y < r.y1 - o || p.y > r.y2 + o || planes.isEmpty()) return null
        return planes.minOf { it[0] * p.x + it[1] * p.y + it[2] }
    }

    fun covers(r: Roof, p: P): Boolean = p.x >= r.x1 && p.x <= r.x2 && p.y >= r.y1 && p.y <= r.y2

    /**
     * Lines on roof face [f] for its covering: the ribs of a profiled sheet or metal tile running down the slope
     * ([spacing] mm apart), each a pair of points (top, bottom) on the face; and, for metal tile, the steps across it.
     * Empty for a flat face.
     */
    fun ribs(f: RoofFace, spacing: Double, across: Boolean = false): List<Pair<RP, RP>> {
        val pts = f.pts
        if (pts.size < 3) return emptyList()
        // the plane h = a x + b y + c through three corners
        val p0 = pts[0]; val p1 = pts[1]; val p2 = pts.first { k -> val cr = (p1.p - p0.p).let { e1 -> val e2 = k.p - p0.p; e1.x * e2.y - e1.y * e2.x }; kotlin.math.abs(cr) > 1 }
        val m = arrayOf(doubleArrayOf(p0.p.x, p0.p.y, 1.0), doubleArrayOf(p1.p.x, p1.p.y, 1.0), doubleArrayOf(p2.p.x, p2.p.y, 1.0))
        fun det(q: Array<DoubleArray>) = q[0][0] * (q[1][1] * q[2][2] - q[1][2] * q[2][1]) - q[0][1] * (q[1][0] * q[2][2] - q[1][2] * q[2][0]) + q[0][2] * (q[1][0] * q[2][1] - q[1][1] * q[2][0])
        val dm = det(m); if (kotlin.math.abs(dm) < 1e-6) return emptyList()
        val hs = doubleArrayOf(p0.h, p1.h, p2.h)
        fun col(k: Int) = Array(3) { i -> DoubleArray(3) { j -> if (j == k) hs[i] else m[i][j] } }
        val a = det(col(0)) / dm; val b = det(col(1)) / dm; val c = det(col(2)) / dm
        val g = P(a, b); if (g.len < 1e-4) return emptyList()
        val down = g.norm() * -1.0; val side = down.left()
        val dir = if (across) side else down; val step = if (across) down else side
        fun hAt(q: P) = a * q.x + b * q.y + c
        val poly = pts.map { it.p }
        val ss = poly.map { dot(it, step) }; val o = poly[0]
        val out = ArrayList<Pair<RP, RP>>()
        var sv = ss.min() + spacing / 2
        while (sv < ss.max()) {
            // the line {q : q·step = sv} cut by the face's outline
            val base = o + step * (sv - dot(o, step))
            val ts = ArrayList<Double>()
            for (i in poly.indices) {
                val e0 = poly[i]; val e1 = poly[(i + 1) % poly.size]
                val d0 = dot(e0, step) - sv; val d1 = dot(e1, step) - sv
                if ((d0 < 0) != (d1 < 0)) { val q = e0 + (e1 - e0) * (d0 / (d0 - d1)); ts += dot(q - base, dir) }
            }
            ts.sort()
            var k = 0
            while (k + 1 < ts.size) {
                val qa = base + dir * (ts[k] + 30); val qb = base + dir * (ts[k + 1] - 30)
                if (ts[k + 1] - ts[k] > 100) out += RP(qa, hAt(qa)) to RP(qb, hAt(qb))
                k += 2
            }
            sv += spacing
        }
        return out
    }
}
