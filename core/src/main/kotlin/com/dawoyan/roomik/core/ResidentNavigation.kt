package com.dawoyan.roomik.core

import kotlin.math.ceil

/** A modest, deterministic route through connected indoor rooms, avoiding solid walls and furniture. */
object ResidentNavigation {
    data class Diagnosis(val rooms: Int, val doors: Int, val exteriorEntry: Boolean, val points: Int, val lengthMm: Int, val reason: String)

    fun diagnose(plan: Plan): Diagnosis {
        val rooms = Rooms.detect(plan).filter { it.info?.type?.name !in setOf("YARD", "BALCONY", "GARAGE") }
        val doors = Interior.openings(plan).values.flatten().count { it.opening.kind != OpeningKind.WINDOW }
        val path = route(plan)
        val outside = path.firstOrNull()?.let { p -> rooms.none { Geo.inside(p, it.polygon) } } ?: false
        val length = path.zipWithNext().sumOf { (a, b) -> (b - a).len }.toInt()
        val reason = when {
            rooms.isEmpty() -> "no_detected_indoor_room"
            doors == 0 -> "no_usable_door"
            !outside -> "no_safe_exterior_entry_found"
            path.size < 2 -> "no_walkable_route"
            else -> "exterior_entry_route_found"
        }
        return Diagnosis(rooms.size, doors, outside, path.size, length, reason)
    }

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
        val doors = Interior.openings(plan)
        val walls = plan.walls.mapNotNull { wall ->
            val a = plan.pos(wall.a); val b = plan.pos(wall.b)
            if (a == null || b == null) null else Triple(wall, a, b)
        }
        fun canWalk(from: P, to: P): Boolean = walls.all { (wall, a, b) ->
            val cross = Geo.intersect(from, to, a, b)
            cross == null || doors[wall.id].orEmpty().any { it.opening.kind != OpeningKind.WINDOW && (cross - a).len > it.t0 + 100 && (cross - a).len < it.t1 - 100 }
        }
        fun valid(p: P): Boolean = rooms.any { Geo.inside(p, it.polygon) } && furniture.none { Geo.inside(p, it) } &&
            walls.all { (wall, a, b) -> Geo.distToSegment(p, a, b) >= wall.thickness / 2.0 + 25 }
        val valid = BooleanArray(nx * ny) { valid(at(it)) }
        val startRoom = rooms.maxByOrNull { room -> plan.items.count { Geo.inside(P(it.x.toDouble(), it.y.toDouble()), room.polygon) } * 10000.0 + room.area }!!
        val entry = doors.values.flatten().asSequence().filter { it.opening.kind != OpeningKind.WINDOW }.mapNotNull { opening ->
            val a = plan.pos(opening.wall.a) ?: return@mapNotNull null
            val b = plan.pos(opening.wall.b) ?: return@mapNotNull null
            val dir = (b - a).norm(); val mid = a + dir * ((opening.t0 + opening.t1) / 2.0)
            val left = mid + dir.left() * 700.0; val right = mid - dir.left() * 700.0
            val leftIndoor = rooms.any { Geo.inside(left, it.polygon) }; val rightIndoor = rooms.any { Geo.inside(right, it.polygon) }
            if (leftIndoor == rightIndoor) null else if (leftIndoor) right else left
        }.mapNotNull { outside ->
            val inside = valid.indices.asSequence().filter { valid[it] && (at(it) - outside).len < 2000.0 && canWalk(outside, at(it)) &&
                furniture.none { f -> (1..4).any { k -> Geo.inside(outside + (at(it) - outside) * (k / 5.0), f) } } }
                .minByOrNull { (at(it) - outside).len }
            inside?.let { outside to it }
        }.firstOrNull()
        val start = entry?.second ?: valid.indices.filter { valid[it] && Geo.inside(at(it), startRoom.polygon) }.minByOrNull { (at(it) - startRoom.anchor).len } ?: return listOf(startRoom.anchor)
        val parents = IntArray(valid.size) { -1 }; val queue = ArrayDeque<Int>(); queue.add(start); parents[start] = start
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst(); val x = i % nx; val y = i / nx
            for ((xx, yy) in listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1, x - 2 to y, x + 2 to y, x to y - 2, x to y + 2)) {
                if (xx !in 0 until nx || yy !in 0 until ny) continue
                val n = yy * nx + xx
                if (!valid[n] || parents[n] >= 0) continue
                val longStep = kotlin.math.abs(xx - x) + kotlin.math.abs(yy - y) == 2
                val middle = (at(i) + at(n)) * .5
                val crossesWall = longStep && walls.any { (wall, a, b) -> Geo.distToSegment(middle, a, b) < wall.thickness / 2.0 + 25 }
                if ((!longStep || crossesWall && furniture.none { Geo.inside(middle, it) }) && canWalk(at(i), at(n))) { parents[n] = i; queue.add(n) }
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
        return listOfNotNull(entry?.first) + result.map(::at)
    }
}
