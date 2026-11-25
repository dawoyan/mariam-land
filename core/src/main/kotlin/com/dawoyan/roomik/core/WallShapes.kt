package com.dawoyan.roomik.core

import kotlin.math.atan2
import kotlin.math.max

/**
 * The outline of one wall with its thickness, mitred against its neighbours at both ends. Seen from [a] to [b],
 * [aL]/[bL] are on the left face and [aR]/[bR] on the right. The outline is a, aL, bL, b, bR, aR.
 */
class WallShape(val wall: Wall, val a: P, val aL: P, val aR: P, val b: P, val bL: P, val bR: P) {
    val outline: List<P> get() = listOf(a, aL, bL, b, bR, aR)
    val mid: P get() = (a + b) * 0.5
}

object WallShapes {
    fun build(plan: Plan): List<WallShape> {
        val pos = plan.nodes.associate { it.id to P(it.x.toDouble(), it.y.toDouble()) }
        val walls = plan.walls.filter { it.a in pos && it.b in pos && (pos[it.a]!! - pos[it.b]!!).len > 0.5 }
        val at = HashMap<Int, MutableList<Wall>>()
        walls.forEach { at.getOrPut(it.a) { mutableListOf() } += it; at.getOrPut(it.b) { mutableListOf() } += it }

        fun dir(w: Wall, from: Int): P = (pos[if (w.a == from) w.b else w.a]!! - pos[from]!!).norm()
        for ((n, l) in at) l.sortBy { val d = dir(it, n); atan2(d.y, d.x) }

        /** Corners of [w] at [node], left and right looking away from the node. */
        fun corners(w: Wall, node: Int): Pair<P, P> {
            val c = pos[node]!!
            val l = at[node]!!
            val d = dir(w, node)
            val n = d.left()
            val h = w.thickness / 2.0
            val plainL = c + n * h
            val plainR = c - n * h
            if (l.size < 2) return plainL to plainR
            val i = l.indexOf(w)
            fun meet(other: Wall, mySide: Double): P? {
                val od = dir(other, node)
                val oh = other.thickness / 2.0
                // my left face meets the neighbour's right face, and the other way round
                val x = Geo.lineIntersect(c + n * (h * mySide), d, c - od.left() * (oh * mySide), od, 0.08) ?: return null
                return x.takeIf { (it - c).len <= 3 * max(h, oh) }
            }
            val left = meet(l[(i + 1) % l.size], 1.0) ?: plainL
            val right = meet(l[(i - 1 + l.size) % l.size], -1.0) ?: plainR
            return left to right
        }

        return walls.map { w ->
            val (aL, aR) = corners(w, w.a)
            val (bR, bL) = corners(w, w.b) // looking away from b, left and right swap
            WallShape(w, pos[w.a]!!, aL, aR, pos[w.b]!!, bL, bR)
        }
    }
}
