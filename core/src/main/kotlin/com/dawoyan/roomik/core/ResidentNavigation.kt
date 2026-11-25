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

    fun route(plan: Plan, from: P? = null, destination: P? = null): List<P> {
        val rooms = Rooms.detect(plan)
        val indoor = rooms.filter { it.info?.type?.name !in setOf("YARD", "BALCONY", "GARAGE") }
        if (indoor.isEmpty()) return emptyList()
        val all = rooms.flatMap { it.polygon }
        val x0 = all.minOf { it.x }; val y0 = all.minOf { it.y }
        val w = all.maxOf { it.x } - x0; val h = all.maxOf { it.y } - y0
        val grid = maxOf(250.0, w / 55.0, h / 55.0)
        val nx = ceil(w / grid).toInt().coerceAtMost(56) + 1
        val ny = ceil(h / grid).toInt().coerceAtMost(56) + 1
        val points = MutableList(nx * ny) { i -> P(x0 + (i % nx) * grid, y0 + (i / nx) * grid) }
        fun at(i: Int) = points[i]
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
        if (from != null && destination != null && rooms.none { Geo.inside(destination, it.polygon) } &&
            canWalk(from, destination) && furniture.none { f -> (1..20).any { k -> Geo.inside(from + (destination - from) * (k / 20.0), f) } }) return listOf(from, destination)
        val gridCount = points.size
        val valid = MutableList(gridCount) { valid(at(it)) }
        val edges = mutableMapOf<Int, MutableSet<Int>>()
        fun link(a: Int, b: Int) { edges.getOrPut(a) { mutableSetOf() }.add(b); edges.getOrPut(b) { mutableSetOf() }.add(a) }
        fun clearSegment(a: P, b: P): Boolean = canWalk(a, b) && furniture.none { f -> (1..9).any { k -> Geo.inside(a + (b - a) * (k / 10.0), f) } }
        // Add waypoints on both sides of each doorway. Grid cells alone can miss narrow or offset doors.
        for (opening in doors.values.flatten().filter { it.opening.kind != OpeningKind.WINDOW }) {
            val a = plan.pos(opening.wall.a) ?: continue; val b = plan.pos(opening.wall.b) ?: continue
            val dir = (b - a).norm(); val mid = a + dir * ((opening.t0 + opening.t1) / 2.0)
            val offset = opening.wall.thickness / 2.0 + 180.0
            val left = mid + dir.left() * offset; val right = mid - dir.left() * offset
            if (!valid(left) || !valid(right) || !clearSegment(left, right)) continue
            val li = points.size; points.add(left); valid.add(true)
            val ri = points.size; points.add(right); valid.add(true); link(li, ri)
            for (portal in listOf(li, ri)) {
                for (i in 0 until gridCount) if (valid[i] && (at(i) - at(portal)).len <= grid * 3 + 350 && clearSegment(at(portal), at(i))) link(portal, i)
            }
        }
        val startRoom = indoor.maxByOrNull { room -> plan.items.count { Geo.inside(P(it.x.toDouble(), it.y.toDouble()), room.polygon) } * 10000.0 + room.area }!!
        val entry = doors.values.flatten().asSequence().filter { it.opening.kind != OpeningKind.WINDOW }.mapNotNull { opening ->
            val a = plan.pos(opening.wall.a) ?: return@mapNotNull null
            val b = plan.pos(opening.wall.b) ?: return@mapNotNull null
            val dir = (b - a).norm(); val mid = a + dir * ((opening.t0 + opening.t1) / 2.0)
            val left = mid + dir.left() * 700.0; val right = mid - dir.left() * 700.0
            val leftIndoor = indoor.any { Geo.inside(left, it.polygon) }; val rightIndoor = indoor.any { Geo.inside(right, it.polygon) }
            if (leftIndoor == rightIndoor) null else if (leftIndoor) right else left
        }.mapNotNull { outside ->
            val inside = valid.indices.asSequence().filter { valid[it] && (at(it) - outside).len < 2000.0 && canWalk(outside, at(it)) &&
                furniture.none { f -> (1..4).any { k -> Geo.inside(outside + (at(it) - outside) * (k / 5.0), f) } } }
                .minByOrNull { (at(it) - outside).len }
            inside?.let { outside to it }
        }.firstOrNull()
        val outsideApproach = from?.let { pos -> entry?.takeIf { (outside, _) ->
            rooms.none { Geo.inside(pos, it.polygon) } && canWalk(pos, outside) &&
                furniture.none { f -> (1..10).any { k -> Geo.inside(pos + (outside - pos) * (k / 10.0), f) } }
        } }
        val requestedStart = from?.let { pos -> valid.indices.asSequence().filter { valid[it] && (at(it) - pos).len < 2000 && canWalk(pos, at(it)) && furniture.none { f -> !Geo.inside(pos, f) && (1..4).any { k -> Geo.inside(pos + (at(it) - pos) * (k / 5.0), f) } } }.minByOrNull { (at(it) - pos).len } } ?: outsideApproach?.second
        if (from != null && requestedStart == null) return emptyList()
        val start = requestedStart ?: entry?.second ?: valid.indices.filter { valid[it] && Geo.inside(at(it), startRoom.polygon) }.minByOrNull { (at(it) - startRoom.anchor).len } ?: return listOf(startRoom.anchor)
        val parents = IntArray(valid.size) { -1 }; val queue = ArrayDeque<Int>(); queue.add(start); parents[start] = start
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            for (n in edges[i].orEmpty()) if (parents[n] < 0) { parents[n] = i; queue.add(n) }
            if (i >= gridCount) continue
            val x = i % nx; val y = i / nx
            for ((xx, yy) in (1..4).flatMap { d -> listOf(x - d to y, x + d to y, x to y - d, x to y + d) }) {
                if (xx !in 0 until nx || yy !in 0 until ny) continue
                val n = yy * nx + xx
                if (!valid[n] || parents[n] >= 0) continue
                val longStep = kotlin.math.abs(xx - x) + kotlin.math.abs(yy - y) > 1
                val crossesWall = longStep && walls.any { (_, a, b) -> Geo.intersect(at(i), at(n), a, b) != null }
                val blockedByFurniture = longStep && furniture.any { f -> (1..9).any { k -> Geo.inside(at(i) + (at(n) - at(i)) * (k / 10.0), f) } }
                if ((!longStep || crossesWall && !blockedByFurniture) && canWalk(at(i), at(n))) { parents[n] = i; queue.add(n) }
            }
        }
        val reachable = valid.indices.filter { parents[it] >= 0 }
        val targets = plan.items.filter { it.kind in setOf("fridge", "toilet", "bed", "bed1", "sofa", "chair") }
        val requestedEnd = destination?.let { target ->
            val targetRoom = rooms.firstOrNull { Geo.inside(target, it.polygon) }
            reachable.asSequence().filter { targetRoom == null || Geo.inside(at(it), targetRoom.polygon) }
                .minByOrNull { (at(it) - target).len }?.takeIf { (at(it) - target).len < 1600.0 }
        }
        if (destination != null && requestedEnd == null) return emptyList()
        val end = requestedEnd ?: targets.mapNotNull { item ->
            val p = P(item.x.toDouble(), item.y.toDouble())
            reachable.minByOrNull { (at(it) - p).len }?.let { it to (at(it) - p).len }
        }.filter { it.second < 1600 }.maxByOrNull { (at(it.first) - at(start)).len }?.first
            ?: reachable.maxByOrNull { (at(it) - at(start)).len } ?: start
        val result = mutableListOf<Int>(); var i = end
        while (i != start) { result.add(i); i = parents[i] }
        result.add(start); result.reverse()
        val approach = if (from != null) listOfNotNull(from, outsideApproach?.first?.takeIf { requestedStart == outsideApproach.second }) else listOfNotNull(entry?.first)
        return approach + result.map(::at)
    }
}
