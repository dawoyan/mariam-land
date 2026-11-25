package com.dawoyan.roomik.core

import kotlin.math.abs
import kotlin.math.hypot

data class P(val x: Double, val y: Double) {
    operator fun plus(o: P) = P(x + o.x, y + o.y)
    operator fun minus(o: P) = P(x - o.x, y - o.y)
    operator fun times(k: Double) = P(x * k, y * k)
    val len: Double get() = hypot(x, y)
    fun norm(): P = len.let { if (it < 1e-9) P(0.0, 0.0) else P(x / it, y / it) }
    /** Rotated a quarter turn towards growing atan2 angle. */
    fun left(): P = P(-y, x)
}

fun cross(a: P, b: P): Double = a.x * b.y - a.y * b.x
fun dot(a: P, b: P): Double = a.x * b.x + a.y * b.y

object Geo {
    /** Closest point of segment [a]-[b] to [p] and its position 0..1 along the segment. */
    fun project(p: P, a: P, b: P): Pair<P, Double> {
        val d = b - a
        val l2 = dot(d, d)
        if (l2 < 1e-9) return a to 0.0
        val t = (dot(p - a, d) / l2).coerceIn(0.0, 1.0)
        return (a + d * t) to t
    }

    fun distToSegment(p: P, a: P, b: P): Double = (project(p, a, b).first - p).len

    /** Where the infinite lines p1 + s*d1 and p2 + u*d2 meet; null when (nearly) parallel. */
    fun lineIntersect(p1: P, d1: P, p2: P, d2: P, minSin: Double = 1e-6): P? {
        val c = cross(d1, d2)
        if (abs(c) < minSin * d1.len * d2.len || abs(c) < 1e-12) return null
        val s = cross(p2 - p1, d2) / c
        return p1 + d1 * s
    }

    /** Crossing point of two segments strictly inside both; null when they only touch at ends or are parallel. */
    fun intersect(a: P, b: P, c: P, d: P): P? {
        val r = b - a
        val s = d - c
        val den = cross(r, s)
        if (abs(den) < 1e-9) return null
        val t = cross(c - a, s) / den
        val u = cross(c - a, r) / den
        val e = 1e-6
        if (t <= e || t >= 1 - e || u <= e || u >= 1 - e) return null
        return a + r * t
    }

    /** Signed shoelace area. */
    fun area(poly: List<P>): Double {
        var s = 0.0
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            s += cross(a, b)
        }
        return s / 2
    }

    fun inside(p: P, poly: List<P>): Boolean {
        var c = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[j]
            if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) c = !c
            j = i
        }
        return c
    }

    private fun inTriangle(p: P, a: P, b: P, c: P): Boolean {
        val d1 = cross(b - a, p - a)
        val d2 = cross(c - b, p - b)
        val d3 = cross(a - c, p - c)
        return d1 > 1e-9 && d2 > 1e-9 && d3 > 1e-9
    }

    /** Ear clipping of a simple polygon; triangles as index triples into [poly]. */
    fun triangulate(poly: List<P>): List<IntArray> {
        val n = poly.size
        if (n < 3) return emptyList()
        val idx = (0 until n).toMutableList()
        if (area(poly) < 0) idx.reverse()
        val out = ArrayList<IntArray>()
        var guard = 0
        while (idx.size > 3 && guard < n * n + 10) {
            guard++
            var clipped = false
            for (i in idx.indices) {
                val ip = idx[(i - 1 + idx.size) % idx.size]
                val ic = idx[i]
                val inx = idx[(i + 1) % idx.size]
                val a = poly[ip]
                val b = poly[ic]
                val c = poly[inx]
                val turn = cross(b - a, c - b)
                if (abs(turn) <= 1e-9) { // straight or folded back: drop the vertex
                    idx.removeAt(i); clipped = true; break
                }
                if (turn < 0) continue
                if (idx.any { it != ip && it != ic && it != inx && inTriangle(poly[it], a, b, c) }) continue
                out += intArrayOf(ip, ic, inx)
                idx.removeAt(i); clipped = true; break
            }
            if (!clipped) { // not simple; cut anything so we always finish
                out += intArrayOf(idx[0], idx[1], idx[2])
                idx.removeAt(1)
            }
        }
        if (idx.size == 3) out += intArrayOf(idx[0], idx[1], idx[2])
        return out
    }
}
