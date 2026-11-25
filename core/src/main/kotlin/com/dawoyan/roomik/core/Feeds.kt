package com.dawoyan.roomik.core

import kotlin.math.abs

/**
 * Feed cables between boards: a board fed from another ([ElPoint.source]). The cable leaves the source board, runs on
 * the source's floor straight through walls (one turn at most) to the spot under or over the board it feeds, and goes
 * straight up or down there, through the slabs, to it.
 */
object Feeds {
    class Feed(
        val from: Boards.Ref, val to: Boards.Ref,
        /** The run on the source's floor, plan mm: from the source board to the spot below or above [to]. */
        val path: List<P>,
        /** Along the floor and up or down, mm. */
        val across: Double, val vertical: Double,
        val mm2: Double, val cores: Int,
        /** +1: the board fed is on a level above the source's, -1: below, 0: the same level. */
        val dir: Int,
    ) {
        val length: Double get() = across + vertical
    }

    /** The feeds of the house, one per board with a source that still exists (and is not itself). */
    fun all(root: Plan): List<Feed> {
        val boards = Boards.all(root)
        return boards.mapNotNull { b ->
            val k = b.point.source ?: return@mapNotNull null
            val src = boards.firstOrNull { it.key == k && !Boards.same(it, b) } ?: return@mapNotNull null
            make(root, src, b)
        }
    }

    fun make(root: Plan, src: Boards.Ref, to: Boards.Ref): Feed {
        val a = P(src.point.x.toDouble(), src.point.y.toDouble()); val b = P(to.point.x.toDouble(), to.point.y.toDouble())
        // the turns the user gave it, else along the longer way first, so the turn falls near the board fed
        val path = if (to.point.feedVia.isNotEmpty()) listOf(a) + to.point.feedVia.map { P(it.x.toDouble(), it.y.toDouble()) } + b
        else {
            val turn = if (abs(b.x - a.x) >= abs(b.y - a.y)) P(b.x, a.y) else P(a.x, b.y)
            if ((turn - a).len < 1 || (turn - b).len < 1) listOf(a, b) else listOf(a, turn, b)
        }
        val across = (1 until path.size).sumOf { (path[it] - path[it - 1]).len }
        // from the source up to its ceiling (or down to its floor), then to the height of the board fed
        val hs = root.elevation(src.floor) + src.point.height
        val ht = root.elevation(to.floor) + to.point.height
        val runAt = if (ht >= hs) root.elevation(src.floor) + root.floor(src.floor).wallHeight - 100 else root.elevation(src.floor) + 100
        val vertical = (abs(runAt - hs) + abs(ht - runAt)).toDouble()
        val dir = if (to.floor == src.floor) 0 else root.elevation(to.floor).compareTo(root.elevation(src.floor)).let { if (it == 0) 1 else it }
        return Feed(src, to, path, across, vertical, to.point.feedMm2, if (to.point.threePhase || src.point.threePhase) 5 else 3, dir)
    }

    /** Boards [b] may take its feed from: any other board not fed (through others) from [b]. */
    fun candidates(root: Plan, b: Boards.Ref): List<Boards.Ref> {
        val boards = Boards.all(root)
        fun fedFrom(x: Boards.Ref, seen: Set<String> = emptySet()): Boolean {
            val k = x.point.source ?: return false
            if (k == b.key) return true
            if (k in seen) return false
            return boards.firstOrNull { it.key == k }?.let { fedFrom(it, seen + x.key) } ?: false
        }
        return boards.filter { !Boards.same(it, b) && !fedFrom(it) }
    }

    val SIZES = listOf(4.0, 6.0, 10.0, 16.0, 25.0)

    /** The feed of the board [toId] on level [toFloor], if it has one. */
    fun of(root: Plan, toFloor: Int, toId: Int): Feed? = all(root).firstOrNull { it.to.floor == toFloor && it.to.point.id == toId }

    /** [root] with the board [toId] on [toFloor] changed by [f] (its feed's source, size or turns). */
    fun setBoard(root: Plan, toFloor: Int, toId: Int, f: (ElPoint) -> ElPoint): Plan {
        val fl = root.floor(toFloor)
        return root.withFloor(toFloor, fl.copy(points = fl.points.map { if (it.id == toId) f(it) else it }))
    }

    /** The feed's turns after its line through [path] was dragged: the inner points, kept to whole mm. */
    fun via(path: List<P>): List<Node> = path.drop(1).dropLast(1).mapIndexed { i, q -> Node(i, kotlin.math.round(q.x).toInt(), kotlin.math.round(q.y).toInt()) }
}
