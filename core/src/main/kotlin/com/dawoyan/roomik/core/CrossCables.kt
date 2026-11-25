package com.dawoyan.roomik.core

import kotlin.math.abs

/** Cross-floor cable endpoints and their simple, editable orthogonal plan route. */
object CrossCables {
    data class Ends(val from: ElPoint, val to: ElPoint)

    fun endpoint(p: ElPoint): Boolean = p.kind in setOf(
        PointKind.BOARD, PointKind.JUNCTION, PointKind.RISER, PointKind.LOW_CURRENT_BOARD, PointKind.LOW_CURRENT_BOX,
    )

    /** A physical cable joins proper termination points only; touching another drawn line never makes a connection. */
    fun valid(cable: CommCable, a: ElPoint, b: ElPoint): Boolean {
        val powerEnds = setOf(PointKind.BOARD, PointKind.JUNCTION, PointKind.RISER)
        val lowCurrentEnds = setOf(PointKind.LOW_CURRENT_BOARD, PointKind.LOW_CURRENT_BOX)
        val allowed = if (cable == CommCable.POWER) powerEnds else lowCurrentEnds
        return a.kind in allowed && b.kind in allowed
    }

    fun ends(root: Plan, c: CrossCable): Ends? {
        val from = root.floor(c.fromFloor).points.firstOrNull { it.id == c.fromId } ?: return null
        val to = root.floor(c.toFloor).points.firstOrNull { it.id == c.toId } ?: return null
        return Ends(from, to)
    }

    /** Route shown on the source floor, ending at the vertical drop/rise above the destination. */
    fun path(root: Plan, c: CrossCable): List<P> {
        val e = ends(root, c) ?: return emptyList()
        val a = P(e.from.x.toDouble(), e.from.y.toDouble())
        val b = P(e.to.x.toDouble(), e.to.y.toDouble())
        if (c.via.isNotEmpty()) return listOf(a) + c.via.map { P(it.x.toDouble(), it.y.toDouble()) } + b
        val turn = if (abs(b.x - a.x) >= abs(b.y - a.y)) P(b.x, a.y) else P(a.x, b.y)
        return if ((turn - a).len < 1 || (turn - b).len < 1) listOf(a, b) else listOf(a, turn, b)
    }

    fun horizontal(root: Plan, c: CrossCable): Double = path(root, c).zipWithNext().sumOf { (a, b) -> (b - a).len }
    fun vertical(root: Plan, c: CrossCable): Double {
        val e = ends(root, c) ?: return 0.0
        val a = root.elevation(c.fromFloor) + e.from.height
        val b = root.elevation(c.toFloor) + e.to.height
        val run = when (c.level) {
            CrossLevel.CEILING -> root.elevation(c.fromFloor) + root.floor(c.fromFloor).wallHeight - 100
            CrossLevel.FLOOR -> root.elevation(c.fromFloor) + 100
            CrossLevel.WALL -> a
        }
        return (abs(run - a) + abs(b - run)).toDouble()
    }

    fun add(root: Plan, fromFloor: Int, fromId: Int, toFloor: Int, toId: Int, cable: CommCable, level: CrossLevel): Plan {
        // Point ids repeat independently on each floor, so equal ids are valid when the floors differ.
        val probe = CrossCable(0, fromFloor, fromId, toFloor, toId)
        val e = ends(root, probe)
        if (fromFloor == toFloor || e == null || !valid(cable, e.from, e.to)) return root
        val id = (root.crossCables.maxOfOrNull { it.id } ?: 0) + 1
        return root.copy(crossCables = root.crossCables.filterNot { it.fromFloor == fromFloor && it.fromId == fromId && it.toFloor == toFloor && it.toId == toId } +
            CrossCable(id, fromFloor, fromId, toFloor, toId, cable, level))
    }
}
