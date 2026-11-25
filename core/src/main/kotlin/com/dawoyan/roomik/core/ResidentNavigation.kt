package com.dawoyan.roomik.core

import kotlin.math.ceil

/** A modest, deterministic route through connected indoor rooms, avoiding solid walls and furniture. */
object ResidentNavigation {
    fun route(plan: Plan): List<P> {
        val rooms = Rooms.detect(plan).filter { it.info?.type?.name !in setOf("YARD", "BALCONY", "GARAGE") }
        if (rooms.isEmpty()) return emptyList()
        val all = rooms.flatMap { it.polygon }
        val x0 = all.minOf { it.x }; val y0 = all.minOf { it.y }
        val w = all.maxOf { it.x } - x0; val h = all.maxOf { it.y } - y0
        val grid = maxOf(250.0, w / 55.0, h / 55.0)
        val nx = ceil(w / grid).toInt().coerceAtMost(56) + 1
        val ny = ceil(h / grid).toInt().coerceAtMost(56) + 1
        fun at(i: Int) = P(x0 + (i % nx) * grid, y0 + (i / nx) * grid)
        val furniture = plan.items.map { Interior.corners(it) }
        fun valid(p: P): Boolean = rooms.any { Geo.inside(p, it.polygon) } && furniture.none { Geo.inside(p, it) } &&
            plan.walls.all { wall ->
                val a = plan.pos(wall.a); val b = plan.pos(wall.b)
                a == null || b == null || Geo.distToSegment(p, a, b) >= wall.thickness / 2.0 + 25
            }
        val valid = BooleanArray(nx * ny) { valid(at(it)) }
        val startRoom = rooms.maxByOrNull { room -> plan.items.count { Geo.inside(P(it.x.toDouble(), it.y.toDouble()), room.polygon) } * 10000.0 + room.area }!!
        val start = valid.indices.filter { valid[it] && Geo.inside(at(it), startRoom.polygon) }.minByOrNull { (at(it) - startRoom.anchor).len } ?: return listOf(startRoom.anchor)
        val parents = IntArray(valid.size) { -1 }; val queue = ArrayDeque<Int>(); queue.add(start); parents[start] = start
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst(); val x = i % nx; val y = i / nx
            for ((xx, yy) in listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1, x - 2 to y, x + 2 to y, x to y - 2, x to y + 2)) {
                if (xx !in 0 until nx || yy !in 0 until ny) continue
                val n = yy * nx + xx
                val longStep = kotlin.math.abs(xx - x) + kotlin.math.abs(yy - y) == 2
                val middle = (at(i) + at(n)) * .5
                val crossesWall = plan.walls.any { wall ->
                    val a = plan.pos(wall.a); val b = plan.pos(wall.b)
                    a != null && b != null && Geo.distToSegment(middle, a, b) < wall.thickness / 2.0 + 25
                }
                if (valid[n] && parents[n] < 0 && (!longStep || crossesWall && furniture.none { Geo.inside(middle, it) }) && Interior.canWalk(plan, at(i), at(n))) { parents[n] = i; queue.add(n) }
            }
        }
        val reachable = valid.indices.filter { parents[it] >= 0 }
        val targets = plan.items.filter { it.kind in setOf("fridge", "toilet", "bed", "bed1", "sofa", "chair") }
        val end = targets.mapNotNull { item ->
            val p = P(item.x.toDouble(), item.y.toDouble())
            reachable.minByOrNull { (at(it) - p).len }?.let { it to (at(it) - p).len }
        }.filter { it.second < 1600 }.maxByOrNull { (at(it.first) - at(start)).len }?.first
            ?: reachable.maxByOrNull { (at(it) - at(start)).len } ?: start
        val result = mutableListOf<Int>(); var i = end
        while (i != start) { result.add(i); i = parents[i] }
        result.add(start); result.reverse()
        return result.map(::at)
    }
}
