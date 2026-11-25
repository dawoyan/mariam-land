package com.dawoyan.roomik.core

import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Edits of the wall graph. Walls are kept planar: a wall drawn across another splits both, a corner dropped on a wall
 * splits it, corners that meet merge. [finish] re-establishes that after free moves.
 */
object PlanOps {
    /** Corners closer than this (mm) are one corner; a point this close to a wall lies on it. */
    const val MERGE = 30.0

    private class Builder(nodes: List<Node>, walls: List<Wall>, firstFree: Int) {
        val nodes = nodes.toMutableList()
        val walls = walls.toMutableList()
        var next = firstFree

        fun pos(id: Int): P = nodes.first { it.id == id }.let { P(it.x.toDouble(), it.y.toDouble()) }

        private fun newId(prefer: Int?): Int =
            if (prefer != null && nodes.none { it.id == prefer } && walls.none { it.id == prefer }) prefer else next++

        fun nodeAt(x: Int, y: Int, prefer: Int? = null): Int {
            nodes.minByOrNull { hypot((it.x - x).toDouble(), (it.y - y).toDouble()) }
                ?.takeIf { hypot((it.x - x).toDouble(), (it.y - y).toDouble()) <= MERGE }
                ?.let { return it.id }
            val p = P(x.toDouble(), y.toDouble())
            val w = walls.firstOrNull { Geo.distToSegment(p, pos(it.a), pos(it.b)) <= MERGE }
            if (w == null) {
                val n = Node(newId(prefer), x, y)
                nodes += n
                return n.id
            }
            val q = Geo.project(p, pos(w.a), pos(w.b)).first
            val n = Node(newId(prefer), q.x.roundToInt(), q.y.roundToInt())
            nodes += n
            walls.remove(w)
            walls += w.copy(b = n.id)
            walls += w.copy(id = next++, a = n.id)
            return n.id
        }

        fun connect(a: Int, b: Int, thickness: Int, prefer: Int? = null, like: Wall? = null) {
            if (a == b) return
            if (walls.any { (it.a == a && it.b == b) || (it.a == b && it.b == a) }) return
            walls += (like ?: Wall(0, 0, 0)).copy(id = newId(prefer), a = a, b = b, thickness = thickness)
        }

        fun addSegment(x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int, ids: Wall? = null, like: Wall? = ids) {
            val s = nodeAt(x1, y1, ids?.a)
            val e = nodeAt(x2, y2, ids?.b)
            if (s == e) return
            val ps = pos(s)
            val pe = pos(e)
            for (w in walls.toList()) {
                val ip = Geo.intersect(ps, pe, pos(w.a), pos(w.b)) ?: continue
                nodeAt(ip.x.roundToInt(), ip.y.roundToInt())
            }
            val chain = nodes
                .map { it.id to Geo.project(pos(it.id), ps, pe) }
                .filter { (id, pr) -> id == s || id == e || (pr.first - pos(id)).len <= MERGE }
                .sortedBy { it.second.second }
                .map { it.first }
                .let { ids2 -> // keep s first and e last even when neighbours project onto the ends
                    listOf(s) + ids2.filter { it != s && it != e } + listOf(e)
                }
            for (i in 0 until chain.size - 1) connect(chain[i], chain[i + 1], thickness, if (chain.size == 2) ids?.id else null, like)
        }
    }

    private fun firstFree(plan: Plan): Int =
        maxOf(plan.nodes.maxOfOrNull { it.id } ?: 0, plan.walls.maxOfOrNull { it.id } ?: 0) + 1

    fun addWall(plan: Plan, x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int = DEFAULT_THICKNESS): Plan {
        val b = Builder(plan.nodes, plan.walls, firstFree(plan))
        b.addSegment(x1, y1, x2, y2, thickness)
        return finish(plan.copy(nodes = b.nodes, walls = b.walls))
    }

    fun addRect(plan: Plan, x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int = DEFAULT_THICKNESS): Plan {
        val b = Builder(plan.nodes, plan.walls, firstFree(plan))
        b.addSegment(x1, y1, x2, y1, thickness)
        b.addSegment(x2, y1, x2, y2, thickness)
        b.addSegment(x2, y2, x1, y2, thickness)
        b.addSegment(x1, y2, x1, y1, thickness)
        return finish(plan.copy(nodes = b.nodes, walls = b.walls))
    }

    /** Free move, used while dragging; call [finish] when the drag ends. */
    fun moveNode(plan: Plan, id: Int, x: Int, y: Int): Plan =
        plan.copy(nodes = plan.nodes.map { if (it.id == id) it.copy(x = x, y = y) else it })

    fun moveWall(plan: Plan, id: Int, dx: Int, dy: Int): Plan {
        val w = plan.wall(id) ?: return plan
        val a = plan.pos(w.a); val b = plan.pos(w.b)
        return plan.copy(
            nodes = plan.nodes.map { if (it.id == w.a || it.id == w.b) it.copy(x = it.x + dx, y = it.y + dy) else it },
            // sockets and switches on this wall ride along
            points = plan.points.map {
                if (!it.kind.free && a != null && b != null && Geo.distToSegment(P(it.x.toDouble(), it.y.toDouble()), a, b) <= 5)
                    it.copy(x = it.x + dx, y = it.y + dy) else it
            },
            openings = plan.openings.map {
                if (a != null && b != null && Geo.distToSegment(P(it.x.toDouble(), it.y.toDouble()), a, b) <= 5) it.copy(x = it.x + dx, y = it.y + dy) else it
            },
        )
    }

    /** Sets the centre-line length by moving the freer end (the one fewer walls meet at; end b on a tie). */
    fun setWallLength(plan: Plan, id: Int, length: Int): Plan {
        val w = plan.wall(id) ?: return plan
        val a = plan.pos(w.a) ?: return plan
        val b = plan.pos(w.b) ?: return plan
        val d = (b - a).norm()
        if (d.len < 0.5 || length < 1) return plan
        fun degree(n: Int) = plan.walls.count { it.a == n || it.b == n }
        val moved = if (degree(w.a) < degree(w.b)) {
            val p = b - d * length.toDouble()
            moveNode(plan, w.a, p.x.roundToInt(), p.y.roundToInt())
        } else {
            val p = a + d * length.toDouble()
            moveNode(plan, w.b, p.x.roundToInt(), p.y.roundToInt())
        }
        return finish(moved)
    }

    fun setThickness(plan: Plan, id: Int, thickness: Int): Plan =
        plan.copy(walls = plan.walls.map { if (it.id == id) it.copy(thickness = thickness) else it })

    /** Walls [ids] changed by [f] (thickness, height, facing…) all at once. */
    fun editWalls(plan: Plan, ids: Set<Int>, f: (Wall) -> Wall): Plan =
        plan.copy(walls = plan.walls.map { if (it.id in ids) f(it).let { w -> w.copy(height = w.height?.takeIf { h -> h != plan.wallHeight }) } else it })

    fun deleteWalls(plan: Plan, ids: Set<Int>): Plan = finish(plan.copy(walls = plan.walls.filter { it.id !in ids }))

    /** Walls with the outside on one side at least: the house's outer walls. */
    fun outerWallIds(plan: Plan): Set<Int> {
        val rooms = Rooms.detect(plan)
        return plan.walls.filter { w -> Facings.cladSides(plan, w, rooms) == 1 }.map { it.id }.toSet()
    }

    fun deleteWall(plan: Plan, id: Int): Plan = finish(plan.copy(walls = plan.walls.filter { it.id != id }))

    fun deleteNode(plan: Plan, id: Int): Plan = finish(plan.copy(walls = plan.walls.filter { it.a != id && it.b != id }))

    /**
     * Rebuilds the graph wall by wall, so crossings split, touching corners merge, and corners no wall uses go away.
     * Ids are kept wherever a corner or wall survives as it was. Room attributes are re-tied to the rooms that remain.
     */
    fun finish(plan: Plan): Plan {
        val b = Builder(emptyList(), emptyList(), firstFree(plan))
        for (w in plan.walls) {
            val p = plan.node(w.a) ?: continue
            val q = plan.node(w.b) ?: continue
            b.addSegment(p.x, p.y, q.x, q.y, w.thickness, w)
        }
        return Interior.reattach(Rooms.rematch(plan.copy(nodes = b.nodes, walls = b.walls)))
    }

    /** Adds a point at [w]; a socket or switch lands on the nearest wall (null when none is within reach). */
    fun addPoint(plan: Plan, kind: PointKind, w: P, reach: Double): Plan? {
        val id = (plan.points.maxOfOrNull { it.id } ?: 0) + 1
        if (kind == PointKind.LAMP) return plan.copy(points = plan.points + ElPoint(id, kind, w.x.roundToInt(), w.y.roundToInt(), height = plan.wallHeight))
        val (q, n) = Interior.onWall(plan, w, reach) ?: return null
        val height = when (kind) {
            PointKind.SOCKET -> plan.heights.socket
            PointKind.BOARD, PointKind.LOW_CURRENT_BOARD -> plan.heights.board
            PointKind.APPLIANCE -> plan.heights.appliance
            else -> plan.heights.switch
        }
        // a house can have many boards (main, garage, floor 2…); each gets a lasting id and a name
        val uid = if (kind == PointKind.BOARD) java.util.UUID.randomUUID().toString().take(8) else ""
        return plan.copy(points = plan.points + ElPoint(id, kind, q.x.roundToInt(), q.y.roundToInt(), n.x, n.y, height = height, uid = uid))
    }

    /** Moves a point to [w]; a socket or switch slides to the nearest wall and faces the side [w] is on. */
    fun movePoint(plan: Plan, id: Int, w: P, reach: Double): Plan = keepSquare(plan, plan.copy(points = plan.points.map { p ->
        if (p.id != id) p
        else if (p.kind.free) p.copy(x = w.x.roundToInt(), y = w.y.roundToInt())
        else Interior.onWall(plan, w, reach)?.let { (q, n) -> p.copy(x = q.x.roundToInt(), y = q.y.roundToInt(), nx = n.x, ny = n.y) } ?: p
    }), id)

    /** The cables of point [id], moved from [before] to [after]: their first turn follows it, so runs stay along x and y. */
    fun keepSquare(before: Plan, after: Plan, id: Int): Plan {
        val p0 = before.points.firstOrNull { it.id == id } ?: return after
        val p1 = after.points.firstOrNull { it.id == id } ?: return after
        val a = Cables.end(before, p0).first; val b = Cables.end(after, p1).first
        if ((a - b).len < 1) return after
        fun fix(turns: List<Node>, far: P): List<Node> {
            val v = turns.map { P(it.x.toDouble(), it.y.toDouble()) }.toMutableList()
            if (v.isEmpty()) {
                val wasSquare = kotlin.math.abs(a.x - far.x) < 1 || kotlin.math.abs(a.y - far.y) < 1
                if (wasSquare && kotlin.math.abs(b.x - far.x) >= 1 && kotlin.math.abs(b.y - far.y) >= 1)
                    v += if (kotlin.math.abs(a.y - far.y) < 1) P(far.x, b.y) else P(b.x, far.y)
            } else if (kotlin.math.abs(v[0].y - a.y) < 1) v[0] = P(v[0].x, b.y)
            else if (kotlin.math.abs(v[0].x - a.x) < 1) v[0] = P(b.x, v[0].y)
            return Lines.straighten(listOf(b) + v + far).drop(1).dropLast(1).mapIndexed { i, q -> Node(i, q.x.roundToInt(), q.y.roundToInt()) }
        }
        return after.copy(routes = after.routes.map { r ->
            val ends = Cables.plan(after, r) ?: return@map r
            when (id) {
                r.from -> r.copy(via = fix(r.via, ends.last()))
                r.to -> r.copy(via = fix(r.via.reversed(), ends.first()).reversed().mapIndexed { i, n -> n.copy(id = i) })
                else -> r
            }
        })
    }

    fun updatePoint(plan: Plan, p: ElPoint): Plan = plan.copy(points = plan.points.map { if (it.id == p.id) p else it })
    fun deletePoint(plan: Plan, id: Int): Plan {
        val p = plan.points.firstOrNull { it.id == id }
        // a junction box with two cables: they become one again
        if (p?.kind == PointKind.JUNCTION && plan.routes.count { it.from == id || it.to == id } == 2) return Circuits.tidy(plan, listOf(id))
        val attached = plan.routes.filter { it.from == id || it.to == id }
        val ends = attached.map { if (it.from == id) it.to else it.from }.distinct()
        val remaining = plan.routes - attached.toSet()
        // A socket is normally a pass-through in a daisy chain.  Keep the downstream devices powered by
        // joining them to the neighbour which still reaches a board/riser after this point is removed.
        // This deliberately also works for a branch: each surviving branch is rejoined to that source.
        val without = plan.copy(routes = remaining)
        val source = ends.firstOrNull { n ->
            Circuits.component(without, n).any { q -> without.points.firstOrNull { it.id == q }?.kind in setOf(PointKind.BOARD, PointKind.RISER) }
        } ?: ends.firstOrNull()
        val sourceRoute = source?.let { s -> attached.firstOrNull { it.from == s || it.to == s } }
        fun turns(from: Int, toDeleted: Int): List<Node> {
            val r = attached.firstOrNull { (it.from == from && it.to == toDeleted) || (it.to == from && it.from == toDeleted) } ?: return emptyList()
            return if (r.from == from) r.via else r.via.reversed().mapIndexed { i, n -> n.copy(id = i) }
        }
        val rejoined = if (p != null && source != null && sourceRoute != null) ends.filter { it != source }.mapIndexed { i, target ->
            val via = turns(source, id) + Node(0, p.x, p.y) + turns(target, id).reversed().mapIndexed { n, q -> q.copy(id = n) }
            sourceRoute.copy(id = (plan.routes.maxOfOrNull { it.id } ?: 0) + 1 + i, from = source, to = target,
                via = Lines.straighten(via.map { P(it.x.toDouble(), it.y.toDouble()) }).mapIndexed { n, q -> Node(n, q.x.roundToInt(), q.y.roundToInt()) })
        } else emptyList()
        return Circuits.tidy(plan.copy(
            points = plan.points.filter { it.id != id }.map { if (it.switchId == id) it.copy(switchId = null) else it }.map { if (it.pairId == id) it.copy(pairId = null) else it },
            routes = remaining + rejoined,
        ), ends)
    }

    /** Sets the height of every point of [kind] to [height] and makes it the height new ones get. */
    fun setAllHeights(plan: Plan, kind: PointKind, height: Int): Plan {
        val h = plan.heights
        val heights = when (kind) {
            PointKind.SOCKET -> h.copy(socket = height); PointKind.SWITCH -> h.copy(switch = height)
            PointKind.BOARD, PointKind.LOW_CURRENT_BOARD -> h.copy(board = height); PointKind.APPLIANCE -> h.copy(appliance = height)
            PointKind.LAMP, PointKind.RISER, PointKind.JUNCTION, PointKind.LOW_CURRENT_BOX -> h
        }
        return plan.copy(heights = heights, points = plan.points.map { if (it.kind == kind) it.copy(height = height) else it })
    }

    fun addRoute(plan: Plan, from: Int, to: Int, via: List<P>, level: RouteLevel): Plan {
        val a = plan.points.firstOrNull { it.id == from }; val b = plan.points.firstOrNull { it.id == to }
        if (from == to) return plan
        val id = (plan.routes.maxOfOrNull { it.id } ?: 0) + 1
        val turns = via.mapIndexed { i, p -> Node(i, p.x.roundToInt(), p.y.roundToInt()) }
        // one cable between two points: a new drawing replaces the old
        val others = plan.routes.filter { !((it.from == from && it.to == to) || (it.from == to && it.to == from)) }
        val (mm2, cores) = sizeFor(plan.copy(routes = others), a, b)
        return plan.copy(routes = others + Route(id, from, to, turns, level, mm2, cores, volts = voltsFor(plan.copy(routes = others), a, b)))
    }

    /**
     * The wire size and wire count for a new cable between [a] and [b]: that of the line it joins (cables already at a
     * socket, switch or box it ends on); else by what the whole line feeds: lights 1.5 mm², sockets 2.5, an appliance
     * by its power.
     */
    /** True when [a] and [b] are the two switches of one two-way pair. */
    fun travellers(a: ElPoint, b: ElPoint): Boolean = a.kind == PointKind.SWITCH && b.kind == PointKind.SWITCH && a.pairId == b.id && b.pairId == a.id

    /** True when cable [r] joins the two switches of a two-way pair. */
    fun travellers(plan: Plan, r: Route): Boolean {
        val a = plan.points.firstOrNull { it.id == r.from } ?: return false
        val b = plan.points.firstOrNull { it.id == r.to } ?: return false
        return travellers(a, b)
    }

    fun sizeFor(plan: Plan, a: ElPoint?, b: ElPoint?): Pair<Double, Int> {
        // between the two switches of a two-way pair: the travellers, 4 wires of 1.5 mm²
        if (a != null && b != null && travellers(a, b)) return 1.5 to 4
        fun start(p: ElPoint?) = p?.takeIf { it.kind != PointKind.BOARD && it.kind != PointKind.RISER }
        val ends = listOfNotNull(start(a), start(b))
        if (ends.isEmpty()) return Boards.cableFor(a, b)
        val ids = ends.flatMap { Circuits.component(plan, it.id) }.toSet()
        val pts = plan.points.filter { it.id in ids }
        val joined = plan.routes.filter { r -> listOf(r.from, r.to).any { id -> id in ids && pts.firstOrNull { it.id == id }?.kind.let { it != PointKind.BOARD && it != PointKind.RISER } } }
            .filter { r -> !travellers(plan, r) }
        if (joined.isNotEmpty()) return joined.maxOf { it.mm2 } to joined.maxOf { it.cores }
        val kinds = pts.mapNotNull { Circuits.kindOf(it.kind) }.toSet()
        if (Circuits.Kind.LIGHT in kinds) return 1.5 to 3
        val heavy = pts.filter { it.kind == PointKind.APPLIANCE }.maxByOrNull { it.kw }
        return Boards.cableFor(heavy ?: a, if (heavy != null) null else b)
    }

    /** 110 when the cable joins a 110 V line (a 110 V socket at an end, or on the line it joins), else 220. */
    fun voltsFor(plan: Plan, a: ElPoint?, b: ElPoint?): Int {
        val ends = listOfNotNull(a, b).filter { it.kind != PointKind.BOARD && it.kind != PointKind.RISER }
        val ids = ends.flatMap { Circuits.component(plan, it.id) }.toSet()
        return if (plan.points.any { it.id in ids && it.kind == PointKind.SOCKET && it.volts == 110 }) 110 else 220
    }

    fun updateRoute(plan: Plan, r: Route): Plan = plan.copy(routes = plan.routes.map { if (it.id == r.id) r else it })
    /** Without cable [id]; a junction box left with one or no cable goes, one left with two joins them back into one. */
    fun deleteRoute(plan: Plan, id: Int): Plan {
        val r = plan.routes.firstOrNull { it.id == id } ?: return plan
        return Circuits.tidy(plan.copy(routes = plan.routes.filter { it.id != id }), listOf(r.from, r.to))
    }

    /** Makes [load] receive its incoming cable from [source]. Only its old upstream cable is replaced; any branches
     * leaving the load remain attached. The new cable can be long-pressed and edited afterwards. */
    fun setPointSource(plan: Plan, load: Int, source: Int, level: RouteLevel): Plan {
        if (load == source || plan.points.none { it.id == load } || plan.points.none { it.id == source }) return plan
        val incoming = Circuits.toBoard(plan, load).firstOrNull()
        val cleared = incoming?.let { deleteRoute(plan, it.id) } ?: plan
        val sourcePoint = cleared.points.firstOrNull { it.id == source }
        // A lamp fed directly from another device is no longer controlled by its former switch. A switch selected as
        // source remains its control link; any other source clears the dotted switch-to-lamp indication.
        val relinked = if (cleared.points.any { it.id == load && it.kind == PointKind.LAMP }) cleared.copy(points = cleared.points.map { p ->
            if (p.id == load) p.copy(switchId = source.takeIf { sourcePoint?.kind == PointKind.SWITCH }, sourceId = source) else p
        }) else cleared.copy(points = cleared.points.map { p -> if (p.id == load) p.copy(sourceId = source) else p })
        return addRoute(relinked, source, load, emptyList(), level)
    }

    /** Lamp [lampId] put under the nearest switch in its room (or open area); left on the board line when there is none. */
    fun linkNearestSwitch(plan: Plan, lampId: Int): Plan {
        val lamp = plan.points.firstOrNull { it.id == lampId && it.kind == PointKind.LAMP } ?: return plan
        val rooms = Rooms.detect(plan)
        val area = Autoroute.areaOf(plan, rooms, lamp)
        val sw = plan.points.filter { it.kind == PointKind.SWITCH && Autoroute.areaOf(plan, rooms, it) == area }
            .minByOrNull { kotlin.math.abs(it.x - lamp.x) + kotlin.math.abs(it.y - lamp.y) } ?: return plan
        return plan.copy(points = plan.points.map { if (it.id == lampId) it.copy(switchId = sw.id) else it })
    }

    /** Switches [a] and [b] made a two-way pair (each one's old partner let go); with [a] == [b], [a] is let go. */
    fun pairSwitches(plan: Plan, a: Int, b: Int): Plan {
        val olds = plan.points.filter { it.id == a || it.id == b }.mapNotNull { it.pairId }.toSet()
        return plan.copy(points = plan.points.map { p ->
            when {
                a == b && (p.id == a || p.pairId == a) -> p.copy(pairId = null)
                a == b -> p
                p.id == a -> p.copy(pairId = b)
                p.id == b -> p.copy(pairId = a)
                p.id in olds -> p.copy(pairId = null)
                else -> p
            }
        })
    }

    /**
     * A 3D model put on the plan at [at]; one of the same [name] already there gets the new parts and keeps its place
     * (sent again from Idea 3D after changes). Returns the plan and the model's id.
     */
    fun putModel(plan: Plan, name: String, parts: List<ModelPart>, at: P): Pair<Plan, Int> {
        plan.models.firstOrNull { it.name == name }?.let { old ->
            return plan.copy(models = plan.models.map { if (it.id == old.id) it.copy(parts = parts) else it }) to old.id
        }
        val id = (plan.models.maxOfOrNull { it.id } ?: 0) + 1
        return plan.copy(models = plan.models + Model3D(id, name, at.x.roundToInt(), at.y.roundToInt(), parts = parts)) to id
    }

    /** A new copy of a library model on [plan] at [at]; a second copy of the same name gets a number. */
    fun addModel(plan: Plan, name: String, parts: List<ModelPart>, at: P): Pair<Plan, Int> {
        val names = plan.models.map { it.name }.toSet()
        val nm = if (name !in names) name else (2..999).map { "$name $it" }.first { it !in names }
        val id = (plan.models.maxOfOrNull { it.id } ?: 0) + 1
        return plan.copy(models = plan.models + Model3D(id, nm, at.x.roundToInt(), at.y.roundToInt(), parts = parts)) to id
    }

    fun updateModel(plan: Plan, m: Model3D): Plan = plan.copy(models = plan.models.map { if (it.id == m.id) m else it })
    fun deleteModel(plan: Plan, id: Int): Plan = plan.copy(models = plan.models.filter { it.id != id })

    /** Puts all the lamps among [ids] under switch [switchId]. */
    fun groupSwitch(plan: Plan, switchId: Int, ids: Set<Int>): Plan = plan.copy(points = plan.points.map {
        if (it.id in ids && it.kind == PointKind.LAMP) it.copy(switchId = switchId) else it
    })

    /** Puts lamp [lampId] under switch [switchId], or frees it when it already is. */
    fun toggleLink(plan: Plan, switchId: Int, lampId: Int): Plan = plan.copy(points = plan.points.map {
        if (it.id == lampId && it.kind == PointKind.LAMP) it.copy(switchId = if (it.switchId == switchId) null else switchId) else it
    })

    /** Adds a door or window on the wall nearest to [w]; null when no wall is within [reach]. */
    fun addOpening(plan: Plan, kind: OpeningKind, w: P, reach: Double): Plan? {
        val (q, n) = Interior.onWall(plan, w, reach) ?: return null
        val id = (plan.openings.maxOfOrNull { it.id } ?: 0) + 1
        // a new one takes the size of the last one of its kind, so a house of equal windows is set once
        val last = plan.openings.lastOrNull { it.kind == kind || (kind == OpeningKind.DOOR && it.kind == OpeningKind.PASSAGE) }
        if (kind == OpeningKind.GATE) {
            // a gate: as the last one, else a sliding car gate, cut full height in a fence wall (a lintel in a high one)
            val g = last?.gate ?: GateKind.SLIDING
            return plan.copy(openings = plan.openings + Opening(id, kind, q.x.roundToInt(), q.y.roundToInt(), n.x, n.y, last?.width ?: g.width, last?.height ?: 2200, gate = g))
        }
        val o = if (kind == OpeningKind.DOOR) Opening(id, kind, q.x.roundToInt(), q.y.roundToInt(), n.x, n.y, last?.width ?: 900, last?.height ?: 2050.coerceAtMost(plan.wallHeight - 100))
        else Opening(id, kind, q.x.roundToInt(), q.y.roundToInt(), n.x, n.y, last?.width ?: 1200, last?.height ?: 1400, last?.sill ?: 900)
        return plan.copy(openings = plan.openings + o)
    }

    fun moveOpening(plan: Plan, id: Int, w: P, reach: Double): Plan = plan.copy(openings = plan.openings.map { o ->
        if (o.id != id) o else Interior.onWall(plan, w, reach, P(o.nx, o.ny))?.let { (q, n) -> o.copy(x = q.x.roundToInt(), y = q.y.roundToInt(), nx = n.x, ny = n.y) } ?: o
    })

    fun updateOpening(plan: Plan, o: Opening): Plan = plan.copy(openings = plan.openings.map { if (it.id == o.id) o else it })
    /** Every opening of [kind] gets the size of [o] (doors keep their way of opening). */
    fun sizeAll(plan: Plan, o: Opening): Plan = plan.copy(openings = plan.openings.map {
        if (it.kind == o.kind) it.copy(width = o.width, height = o.height, sill = o.sill) else it
    })
    fun deleteOpening(plan: Plan, id: Int): Plan = plan.copy(openings = plan.openings.filter { it.id != id })

    fun addItem(plan: Plan, kind: String, x: Int, y: Int): Plan {
        val c = Catalog.of(kind)
        // stairs reach the floor above; a chimney goes up through the roof
        val h = when (c.id) { STAIRS -> plan.wallHeight + SLAB; "chimney" -> plan.wallHeight + 2200; else -> c.h }
        val item = Item((plan.items.maxOfOrNull { it.id } ?: 0) + 1, c.id, x, y, c.w, c.d, h, z = c.z)
        return plan.copy(items = plan.items + Interior.keepOffWalls(plan, item))
    }

    /**
     * Replaces an item; it is kept off the walls. A move goes there in short steps, so the item slides along a wall in
     * its way instead of jumping through it.
     */
    fun updateItem(plan: Plan, i: Item): Plan {
        val bodies = Interior.wallBodies(plan)
        val old = plan.items.firstOrNull { it.id == i.id }
        var cur = Interior.keepOffWalls(plan, (old ?: i).copy(w = i.w, d = i.d, h = i.h, rot = i.rot, kind = i.kind), bodies)
        if (old != null) {
            val dx = (i.x - cur.x).toDouble(); val dy = (i.y - cur.y).toDouble()
            val n = (kotlin.math.hypot(dx, dy) / 40).toInt().coerceIn(1, 500)
            val sx = dx / n; val sy = dy / n
            var fx = cur.x.toDouble(); var fy = cur.y.toDouble()
            repeat(n) {
                fx += sx; fy += sy
                cur = Interior.keepOffWalls(plan, cur.copy(x = fx.roundToInt(), y = fy.roundToInt()), bodies)
                fx = cur.x.toDouble(); fy = cur.y.toDouble()
            }
        }
        val kept = if (old == null) Interior.keepOffWalls(plan, i, bodies) else cur
        return plan.copy(items = plan.items.map { if (it.id == i.id) kept else it })
    }

    fun addStair(plan: Plan, x: Int, y: Int): Plan =
        plan.copy(stairs = plan.stairs + Stair((plan.stairs.maxOfOrNull { it.id } ?: 0) + 1, x = x, y = y, rot = -90))

    /** Replaces stairs; when their place or shape changes, the walls they cut are made whole again. */
    fun updateStair(plan: Plan, s: Stair): Plan = plan.copy(stairs = plan.stairs.map {
        if (it.id != s.id) it else if (it.copy(cropped = emptyList()) != s.copy(cropped = emptyList())) s.copy(cropped = emptyList()) else s
    })

    /** Cuts every wall [s] runs into, where it passes. */
    fun cropWalls(plan: Plan, id: Int): Plan {
        val s = plan.stairs.firstOrNull { it.id == id } ?: return plan
        val walls = Stairs.clashes(plan, s).map { it.wall.id }
        return plan.copy(stairs = plan.stairs.map { if (it.id == id) it.copy(cropped = (it.cropped + walls).distinct()) else it })
    }
    fun deleteStair(plan: Plan, id: Int): Plan = plan.copy(stairs = plan.stairs.filter { it.id != id })

    /** Sets wall [id]'s height; null makes it as high as the floor's walls. */
    fun setWallHeight(plan: Plan, id: Int, height: Int?): Plan =
        plan.copy(walls = plan.walls.map { if (it.id == id) it.copy(height = height?.takeIf { h -> h != plan.wallHeight }) else it })

    fun addRoof(plan: Plan, x1: Int, y1: Int, x2: Int, y2: Int): Plan {
        val id = (plan.roofs.maxOfOrNull { it.id } ?: 0) + 1
        val r = Roof(id, minOf(x1, x2), minOf(y1, y2), maxOf(x1, x2), maxOf(y1, y2))
        return plan.copy(roofs = plan.roofs + r.copy(alongX = (r.x2 - r.x1) >= (r.y2 - r.y1)))
    }

    fun addPlatform(plan: Plan, x1: Int, y1: Int, x2: Int, y2: Int, height: Int = 0): Plan {
        val id = (plan.platforms.maxOfOrNull { it.id } ?: 0) + 1
        return plan.copy(platforms = plan.platforms + Platform(id, minOf(x1, x2), minOf(y1, y2), maxOf(x1, x2), maxOf(y1, y2), height))
    }
    fun updatePlatform(plan: Plan, p: Platform): Plan = plan.copy(platforms = plan.platforms.map { if (it.id == p.id) p else it })
    fun deletePlatform(plan: Plan, id: Int): Plan = plan.copy(platforms = plan.platforms.filter { it.id != id })

    fun addPillar(plan: Plan, x: Int, y: Int, like: Pillar? = null): Plan {
        val id = (plan.pillars.maxOfOrNull { it.id } ?: 0) + 1
        val p = like?.copy(id = id, x = x, y = y) ?: Pillar(id, x, y)
        return plan.copy(pillars = plan.pillars + p)
    }
    fun updatePillar(plan: Plan, p: Pillar): Plan = plan.copy(pillars = plan.pillars.map { if (it.id == p.id) p else it })
    fun deletePillar(plan: Plan, id: Int): Plan = plan.copy(pillars = plan.pillars.filter { it.id != id })

    /** [walls] of [from] (another floor) added to [plan] where they stand, joined to its own corners and walls. */
    fun copyWalls(plan: Plan, from: Plan, walls: List<Wall>): Plan {
        val b = Builder(plan.nodes, plan.walls, firstFree(plan))
        for (w in walls) {
            val a = from.node(w.a) ?: continue; val c = from.node(w.b) ?: continue
            b.addSegment(a.x, a.y, c.x, c.y, w.thickness, like = w)
        }
        return finish(plan.copy(nodes = b.nodes, walls = b.walls))
    }

    fun updateRoof(plan: Plan, r: Roof): Plan = plan.copy(roofs = plan.roofs.map { if (it.id == r.id) r else it })
    fun deleteRoof(plan: Plan, id: Int): Plan = plan.copy(roofs = plan.roofs.filter { it.id != id })

    fun addFence(plan: Plan, kind: FenceKind, points: List<P>): Plan {
        if (points.size < 2) return plan
        val id = (plan.fences.maxOfOrNull { it.id } ?: 0) + 1
        return plan.copy(fences = plan.fences + Fence(id, kind, points.mapIndexed { i, p -> Node(i, p.x.roundToInt(), p.y.roundToInt()) }))
    }

    fun addRoad(plan: Plan, points: List<P>, surface: Surface, width: Int, closed: Boolean = false): Plan {
        if (points.size < if (closed) 3 else 2) return plan
        val id = (plan.roads.maxOfOrNull { it.id } ?: 0) + 1
        return plan.copy(roads = plan.roads + Road(id, points.mapIndexed { i, p -> Node(i, p.x.roundToInt(), p.y.roundToInt()) }, width, surface, closed))
    }
    fun updateRoad(plan: Plan, r: Road): Plan = plan.copy(roads = plan.roads.map { if (it.id == r.id) r else it })
    fun deleteRoad(plan: Plan, id: Int): Plan = plan.copy(roads = plan.roads.filter { it.id != id })

    fun updateFence(plan: Plan, f: Fence): Plan = plan.copy(fences = plan.fences.map { if (it.id == f.id) f else it })
    fun deleteFence(plan: Plan, id: Int): Plan = plan.copy(fences = plan.fences.filter { it.id != id })

    fun addPipe(plan: Plan, system: SystemKind, points: List<P>): Plan {
        if (points.size < 2) return plan
        val id = (plan.pipes.maxOfOrNull { it.id } ?: 0) + 1
        return plan.copy(pipes = plan.pipes + Pipe(id, system, points.mapIndexed { i, p -> Node(i, p.x.roundToInt(), p.y.roundToInt()) }, system.defaultHeight))
    }

    fun updatePipe(plan: Plan, p: Pipe): Plan = plan.copy(pipes = plan.pipes.map { if (it.id == p.id) p else it })
    fun deletePipe(plan: Plan, id: Int): Plan = plan.copy(pipes = plan.pipes.filter { it.id != id })

    /** Turns the warm floor of [room] on or off. */
    fun toggleWarmFloor(plan: Plan, room: Room): Plan {
        val on = !(room.info?.warmFloor ?: false)
        val id = room.info?.id?.takeIf { it.isNotEmpty() } ?: ((plan.rooms.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
        val others = plan.rooms.filter { !Geo.inside(P(it.x.toDouble(), it.y.toDouble()), room.polygon) }
        val info = (room.info ?: RoomInfo(0, 0)).copy(x = room.anchor.x.roundToInt(), y = room.anchor.y.roundToInt(), id = id, warmFloor = on)
        return plan.copy(rooms = others + info)
    }

    fun addPlace(plan: Plan, x: Int, y: Int): Plan {
        val id = "p" + ((plan.places.mapNotNull { it.id.removePrefix("p").toIntOrNull() }.maxOrNull() ?: 0) + 1)
        return plan.copy(places = plan.places + Place(id, x, y))
    }

    fun updatePlace(plan: Plan, p: Place): Plan = plan.copy(places = plan.places.map { if (it.id == p.id) p else it })
    fun deletePlace(plan: Plan, id: String): Plan = plan.copy(places = plan.places.filter { it.id != id })

    /** Adds a floor on top, with the walls of the floor below to start from (inner ones can be deleted). */
    /**
     * The outer walls of [p]: those with outside (no closed room) on at least one side. Walls inside the house are left
     * out, so a new floor starts with its shell only. Low walls (a parapet, a railing) are left out too.
     */
    fun outerWalls(p: Plan): Pair<List<Node>, List<Wall>> {
        val rooms = Rooms.detect(p)
        val walls = p.walls.filter { w ->
            val a = p.pos(w.a) ?: return@filter false
            val b = p.pos(w.b) ?: return@filter false
            if (p.heightOf(w) < p.wallHeight) return@filter false
            val m = (a + b) * 0.5; val n = (b - a).norm().left() * (w.thickness / 2.0 + 50)
            rooms.none { Geo.inside(m + n, it.polygon) } || rooms.none { Geo.inside(m - n, it.polygon) }
        }.map { it.copy(height = null) }
        val used = walls.flatMap { listOf(it.a, it.b) }.toSet()
        return p.nodes.filter { it.id in used } to walls
    }

    fun addFloor(root: Plan): Plan {
        val top = root.floor(root.floors.size)
        val (nodes, walls) = outerWalls(top)
        val f = Plan(id = root.id + "-f" + (root.floors.size + 1), name = "", wallHeight = top.wallHeight, nodes = nodes, walls = walls)
        return root.copy(floors = root.floors + Rooms.rematch(f))
    }

    /** Adds a floor under the lowest one, with its walls to start from. */
    fun addBasement(root: Plan): Plan {
        val low = root.floor(-root.basements.size)
        val (nodes, walls) = outerWalls(low)
        val f = Plan(id = root.id + "-b" + (root.basements.size + 1), name = "", wallHeight = 2500, nodes = nodes, walls = walls)
        return root.copy(basements = root.basements + Rooms.rematch(f))
    }

    fun deleteFloor(root: Plan, index: Int): Plan = when {
        index > 0 -> root.copy(floors = root.floors.filterIndexed { i, _ -> i != index - 1 })
        index < 0 -> root.copy(basements = root.basements.filterIndexed { i, _ -> i != -index - 1 })
        else -> root
    }

    /** Puts a riser on every floor at the same plan coordinate.  A riser may pass through an open garage/room with no
     * nearby wall, so it must not disappear merely because that level has no wall at the chosen spot. */
    fun addRiser(root: Plan, w: P, reach: Double): Plan {
        var r = root
        for (fi in root.floorRange) {
            val f = r.floor(fi)
            val placed = addPoint(f, PointKind.RISER, w, reach) ?: run {
                val id = (f.points.maxOfOrNull { it.id } ?: 0) + 1
                f.copy(points = f.points + ElPoint(id, PointKind.RISER, w.x.roundToInt(), w.y.roundToInt(), height = f.wallHeight))
            }
            r = r.withFloor(fi, placed)
        }
        return r
    }
    fun deleteItem(plan: Plan, id: Int): Plan = plan.copy(items = plan.items.filter { it.id != id })

    fun setRoomInfo(plan: Plan, room: Room, name: String, type: com.dawoyan.elektronik.core.RoomType?): Plan {
        val others = plan.rooms.filter { !Geo.inside(P(it.x.toDouble(), it.y.toDouble()), room.polygon) }
        val id = room.info?.id?.takeIf { it.isNotEmpty() } ?: ((plan.rooms.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
        return plan.copy(rooms = others + RoomInfo(room.anchor.x.roundToInt(), room.anchor.y.roundToInt(), name.trim(), type, id))
    }
}
