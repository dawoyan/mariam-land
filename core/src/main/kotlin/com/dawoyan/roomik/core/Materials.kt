package com.dawoyan.roomik.core

import com.dawoyan.elektronik.core.BoardBundle
import com.dawoyan.elektronik.core.Cable
import com.dawoyan.elektronik.core.DeviceType
import com.dawoyan.elektronik.core.I18n
import com.dawoyan.elektronik.core.LoadKind
import com.dawoyan.elektronik.core.Project
import com.dawoyan.elektronik.core.RoomLoad
import com.dawoyan.elektronik.core.RoomPlanner
import com.dawoyan.elektronik.core.RoomType
import com.dawoyan.elektronik.core.Roomik
import com.dawoyan.elektronik.core.localTitle
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import com.dawoyan.elektronik.core.Room as ERoom

enum class BomKey { BACK_BOX, SOCKET, SOCKET_IP44, POWER_OUTLET, SWITCH, FRAME, LAMP, CONNECTOR, CONDUIT, CLIP, JUNCTION_BOX }

/** One thing to buy. [detail] is the number of gangs of a switch or posts of a frame, 0 otherwise. */
class BomItem(val key: BomKey, val detail: Int, val qty: Int)

/** Cable of one kind to buy: [meters] includes the reserve, rounded up; [net] is what the routes add up to. */
class CableTotal(val cores: Int, val mm2: Double, val meters: Int, val net: Double)

/** One line from the board: its breaker, cable and estimated length in metres (0 without a board on the plan). */
class LineInfo(val name: String, val rating: String, val cores: Int, val mm2: Double, val meters: Double, val drawn: Boolean = false, val id: String = "")

class BoardItem(val title: String, val rating: String, val qty: Int)

class Bom(
    val hasBoard: Boolean,
    val lines: List<LineInfo>,
    val cables: List<CableTotal>,
    val items: List<BomItem>,
    val board: List<BoardItem>,
    /** Lamps no switch controls yet. */
    val unlinkedLamps: Int,
)

/**
 * What the electrics on the plan need. Lines, breakers and cable sizes come from elektronik's planner, so both apps
 * agree. Cable lengths are an estimate: cables run along walls at [LEVEL_BELOW_CEILING] under the ceiling and drop
 * to each point; sockets of a line are looped one after another, lights go board → switches → lamps.
 */
object Materials {
    const val LEVEL_BELOW_CEILING = 150
    /** Spare cable left in every box, and in the board for every line, mm. */
    const val BOX_SLACK = 150
    const val BOARD_SLACK = 1000
    const val RESERVE = 0.10

    private class Group(val id: String, val type: RoomType, val name: String, val floor: Int, val zone: String) {
        val sockets = ArrayList<ElPoint>()
        val appliances = ArrayList<ElPoint>()
        val switches = ArrayList<ElPoint>()
        val lamps = ArrayList<ElPoint>()
    }

    /**
     * The places electrics are grouped by, on every floor: each closed room, each open place (a garage drawn with open
     * walls, a yard), and for points in neither, the nearest open place or else one group for what is outside.
     */
    private fun groups(root: Plan, ru: Boolean): List<Group> {
        val out = ArrayList<Group>()
        var unnamed = 0
        val many0 = root.floors.isNotEmpty() || root.basements.isNotEmpty()
        // every level, the ground round the house too (a garage or a gate drawn there)
        val many = many0 || root.site != null
        for (fi in root.levels) {
            val plan = root.floor(fi)
            val prefix = when { fi == SITE -> "g-"; fi > 0 -> "f$fi-"; fi < 0 -> "b${-fi}-"; else -> "" }
            val zone = if (!many) "" else if (fi == SITE) (if (ru) "Участок" else "Ground") else (if (ru) "Этаж ${floorNumber(fi)}" else "Floor ${floorNumber(fi)}")
            val rooms = Rooms.detect(plan)
            val byRoom = LinkedHashMap<Int, Group>()
            val byPlace = LinkedHashMap<String, Group>()
            fun group(i: Int): Group = byRoom.getOrPut(i) {
                val info = rooms[i].info
                val name = info?.name?.takeIf { it.isNotEmpty() } ?: if (info?.type == null) (if (ru) "Комната ${++unnamed}" else "Room ${++unnamed}") else ""
                Group(Roomik.roomId(root.id, prefix + (info?.id?.takeIf { it.isNotEmpty() } ?: "i${i + 1}")), info?.type ?: RoomType.OTHER, name, fi, zone)
            }
            fun place(pl: Place): Group = byPlace.getOrPut(pl.id) {
                val name = pl.name.ifBlank { if (pl.type == null) (if (ru) "Место ${++unnamed}" else "Place ${++unnamed}") else "" }
                Group(Roomik.roomId(root.id, prefix + pl.id), pl.type ?: RoomType.OTHER, name, fi, zone)
            }
            var outside: Group? = null
            fun outside(): Group = outside ?: (
                if (fi == SITE) Group(Roomik.roomId(root.id, prefix + "out"), RoomType.YARD, if (ru) "Участок" else "Ground", fi, zone)
                else if (rooms.isEmpty()) Group(Roomik.roomId(root.id, prefix + "out"), RoomType.OTHER, root.name + if (fi != 0) " ${floorNumber(fi)}" else "", fi, zone)
                else Group(Roomik.roomId(root.id, prefix + "out"), RoomType.YARD, if (ru) "Улица" else "Outside", fi, zone)
            ).also { outside = it }

            // Every room and place is a group, also with nothing electrical in it yet, so elektronik shows all of them.
            rooms.indices.forEach { group(it) }
            plan.places.forEach { place(it) }
            val placed = Interior.place(plan)
            fun at(pp: PlacedPoint) = if (pp.point.kind == PointKind.LAMP) pp.face else pp.face + pp.normal * 150.0
            fun of(pp: PlacedPoint): Group {
                val q = at(pp)
                val r = rooms.indexOfFirst { Geo.inside(q, it.polygon) }
                if (r >= 0) return group(r)
                plan.places.minByOrNull { (P(it.x.toDouble(), it.y.toDouble()) - q).len }?.let { return place(it) }
                return outside()
            }
            val switchGroup = placed.filter { it.point.kind == PointKind.SWITCH }.associate { it.point.id to of(it) }
            for (pp in placed) {
                val p = pp.point
                when (p.kind) {
                    PointKind.SOCKET -> of(pp).sockets += p
                    PointKind.APPLIANCE -> of(pp).appliances += p
                    PointKind.SWITCH -> of(pp).switches += p
                    // A lamp is on the light line of the place its switch is in, wherever it hangs.
                    PointKind.LAMP -> (p.switchId?.let { switchGroup[it] } ?: of(pp)).lamps += p
                    PointKind.BOARD, PointKind.LOW_CURRENT_BOARD, PointKind.LOW_CURRENT_BOX, PointKind.RISER, PointKind.JUNCTION -> {}
                }
            }
            out += byRoom.values
            out += byPlace.values
            outside?.let { o -> if (o.sockets.size + o.appliances.size + o.switches.size + o.lamps.size > 0) out += o }
        }
        return out
    }

    /** On level [fi] of [root]: each device on a drawn board line → that line's number on its board and its length, mm. */
    private fun lineOfSocket(root: Plan, fi: Int): Map<Int, Pair<Int, Double>> {
        val f = root.floor(fi)
        val nos = Circuits.numbers(f)
        val out = HashMap<Int, Pair<Int, Double>>()
        for (b in f.points.filter { it.kind == PointKind.BOARD || it.kind == PointKind.RISER })
            for (l in Circuits.lines(f, b.id)) {
                val no = nos[l.routes.first().id] ?: continue
                for (d in l.devices) out.putIfAbsent(d.id, no to l.mm)
            }
        return out
    }

    /** The plan's loads as an elektronik project with its lines and protection planned. */
    fun elektronikProject(plan: Plan, ru: Boolean, keepEmpty: Boolean = true, name: String = plan.name): Project {
        I18n.ru = ru
        val rooms = groups(plan, ru).filter { g -> keepEmpty || g.sockets.size + g.appliances.size + g.switches.size + g.lamps.size > 0 }.map { g ->
            val loads = ArrayList<RoomLoad>()
            if (g.lamps.isNotEmpty()) loads += RoomLoad("lights", "", g.lamps.sumOf { it.watts } / 1000.0, kind = LoadKind.LIGHTS, count = g.lamps.size)
            // sockets on lines drawn to the board: when the room's sockets are on more than one drawn line, or one is
            // marked as its own line, each drawn line is a breaker of its own (named by its number on the board)
            val drawnLine = lineOfSocket(plan, g.floor)
            val byLine = g.sockets.groupBy { drawnLine[it.id] }
            val separate = byLine.keys.filterNotNull().size >= 2 || g.sockets.any { it.ownLine && drawnLine[it.id] != null }
            val rest = if (separate) byLine[null].orEmpty() else g.sockets
            if (separate) for ((ln, list) in byLine) if (ln != null)
                loads += RoomLoad("L${ln.first}", "№${ln.first}", 0.0, ownLine = true, kind = LoadKind.SOCKETS, count = list.sumOf { it.count },
                    lineM = listOf(ln.second / 1000.0))
            val outlets = rest.sumOf { it.count }
            if (outlets > 0) loads += RoomLoad("sockets", "", 0.0, kind = LoadKind.SOCKETS, count = outlets)
            for (a in g.appliances) loads += RoomLoad("a${a.id}", a.name.ifBlank { if (ru) "Прибор" else "Appliance" }, a.kw, a.ownLine, a.threePhase)
            ERoom(id = g.id, type = g.type, name = g.name, sockets = 0, loads = loads, zone = g.zone, explicit = true)
        }
        return RoomPlanner.apply(Project(name = name, rooms = rooms))
    }

    /**
     * One elektronik board for each board on the plan, with the rooms it feeds; one board for the whole plan when it
     * has one board or none. Rooms with nothing electrical yet go to the first board.
     */
    fun elektronikProjects(plan: Plan, ru: Boolean): List<Project> {
        val boards = Boards.all(plan)
        if (boards.size <= 1) return listOf(withLengths(plan, ru, elektronikProject(plan, ru, name = boards.firstOrNull()?.let { Boards.name(it, ru) }?.takeIf { boards[0].point.name.isNotBlank() } ?: plan.name))
            .copy(materials = wiring(plan, ru)))
        return boards.map { b -> Boards.only(plan, b).let { sub -> withFeeds(plan, b, ru, withLengths(sub, ru, elektronikProject(sub, ru, keepEmpty = b.index == 0, name = Boards.name(b, ru))))
            .copy(materials = wiring(sub, ru) + feedCables(plan, b)) } }
    }

    /**
     * What the lines of [plan] (one board's part of it) need besides the board's devices: cable by size with the reserve,
     * sockets, switches, frames, boxes, lamps, connectors… for elektronik's shopping list. Names in Russian or English.
     */
    fun wiring(plan: Plan, ru: Boolean): List<com.dawoyan.elektronik.core.Material> {
        val bom = buildOne(plan, ru)
        fun f1(v: Double) = String.format(java.util.Locale.US, "%.1f", v)
        // both languages: elektronik shows the one it is set to
        val cables = bom.cables.map { c ->
            val size = if (c.mm2 % 1.0 == 0.0) c.mm2.toInt().toString() else c.mm2.toString()
            com.dawoyan.elektronik.core.Material("Cable ${c.cores}×$size mm²", c.meters.toDouble(), "m", "on the plan ${f1(c.net)} m + 10 %",
                "Кабель ВВГнг-LS ${c.cores}×$size", "по плану ${f1(c.net)} м + 10 %")
        }
        val items = bom.items.map { i ->
            val (en, r) = when (i.key) {
                BomKey.BACK_BOX -> "Back box" to "Подрозетник"
                BomKey.SOCKET -> "Socket, earthed" to "Розетка с заземлением"
                BomKey.SOCKET_IP44 -> "Socket IP44, splash-proof" to "Розетка IP44, влагозащищённая"
                BomKey.POWER_OUTLET -> "Power outlet / terminal box for an appliance" to "Силовая розетка / клеммная коробка для прибора"
                BomKey.SWITCH -> "Switch, ${i.detail}-gang" to "Выключатель, ${i.detail}-клавишный"
                BomKey.FRAME -> "Frame, ${i.detail}-post" to "Рамка, ${i.detail}-местная"
                BomKey.LAMP -> "Light fitting" to "Светильник"
                BomKey.CONNECTOR -> "Wago connector, 3-way" to "Клемма Wago на 3 провода"
                BomKey.CONDUIT -> "Conduit 16 mm, if used" to "Гофра 16 мм, если нужна"
                BomKey.CLIP -> "Cable clips" to "Крепёж кабеля (клипсы)"
                BomKey.JUNCTION_BOX -> "Junction box" to "Распаечная коробка"
            }
            com.dawoyan.elektronik.core.Material(en, i.qty.toDouble(), if (i.key == BomKey.CONDUIT) "m" else "pcs", nameRu = r)
        }
        return cables + items
    }

    /**
     * [project] (board [b]) with a line for each board it feeds: a load of its own named after that board, its power
     * guessed from the feed cable's size, its cable length the feed's.
     */
    private fun withFeeds(plan: Plan, b: Boards.Ref, ru: Boolean, project: Project): Project {
        val out = Feeds.all(plan).filter { Boards.same(it.from, b) }
        if (out.isEmpty()) return project
        fun kwFor(mm2: Double) = when { mm2 >= 25 -> 22.0; mm2 >= 16 -> 15.0; mm2 >= 10 -> 11.0; mm2 >= 6 -> 7.0; else -> 5.5 }
        val loads = out.map { f ->
            RoomLoad("feed-" + f.to.key, (if (ru) "Щит " else "Board ") + Boards.name(f.to, ru).removePrefix("Щит ").removePrefix("Board "),
                kwFor(f.mm2), ownLine = true, threePhase = f.cores == 5, lineM = listOf(f.length / 1000.0))
        }
        val room = ERoom(id = Roomik.roomId(plan.id, "feeds-" + b.key), type = RoomType.OTHER, name = if (ru) "Питание других щитов" else "Feeds to other boards",
            sockets = 0, loads = loads, explicit = true)
        return RoomPlanner.apply(project.copy(rooms = project.rooms + room))
    }

    /** The feed cables leaving board [b], for its shopping list. */
    private fun feedCables(plan: Plan, b: Boards.Ref): List<com.dawoyan.elektronik.core.Material> =
        Feeds.all(plan).filter { Boards.same(it.from, b) }.groupBy { it.cores to it.mm2 }.map { (k, fs) ->
            val net = fs.sumOf { it.length } / 1000.0
            val size = if (k.second % 1.0 == 0.0) k.second.toInt().toString() else k.second.toString()
            com.dawoyan.elektronik.core.Material("Feed cable ${k.first}×$size mm²", kotlin.math.ceil(net * 1.1), "m",
                "to other boards " + String.format(java.util.Locale.US, "%.1f", net) + " m + 10 %",
                "Питающий кабель ВВГнг-LS ${k.first}×$size", "к другим щитам " + String.format(java.util.Locale.US, "%.1f", net) + " м + 10 %")
        }

    /** [project] (made from [plan]) with each line's cable length, drawn or estimated, on the loads it is made from. */
    private fun withLengths(plan: Plan, ru: Boolean, project: Project): Project {
        val m = buildOne(plan, ru).lines.filter { it.id.isNotEmpty() }.associate { it.id to it.meters }
        if (m.values.none { it > 0 }) return project
        val rooms = project.rooms.map { room ->
            val pre = "room-${room.id}-"
            room.copy(loads = room.loads.map { l ->
                when (l.kind) {
                    LoadKind.LIGHTS -> l.copy(lineM = listOfNotNull(m[pre + "light"]))
                    LoadKind.SOCKETS -> if (l.ownLine) l else l.copy(lineM = generateSequence(1) { it + 1 }.map { m[pre + "sockets-$it"] }.takeWhile { it != null }.filterNotNull().toList())
                    else -> l.copy(lineM = listOfNotNull(m[pre + "load-" + l.id]))
                }
            })
        }
        return RoomPlanner.apply(project.copy(rooms = rooms))
    }

    /**
     * Board [b] alone for elektronik, with every load it feeds (on any floor), its lines and cable lengths; marked with
     * the plan and board, so elektronik puts it into the project chosen there and replaces it when sent again.
     */
    fun elektronikBoardFile(plan: Plan, b: Boards.Ref, ru: Boolean): String {
        val sub = Boards.only(plan, b, requireEvidence = true)
        val project = withFeeds(plan, b, ru, withLengths(sub, ru, elektronikProject(sub, ru, keepEmpty = false, name = Boards.name(b, ru))))
            .copy(roomikBoard = plan.id + ":" + b.key, materials = wiring(sub, ru) + feedCables(plan, b))
        return BoardBundle.encode(plan.name, listOf(project), id = Roomik.BUNDLE_PREFIX + plan.id)
    }

    /** Cable lengths, fittings and line subtotals for exactly one selected board (not its feeder). */
    fun boardBom(plan: Plan, b: Boards.Ref, ru: Boolean): Bom = buildOne(Boards.only(plan, b, requireEvidence = true), ru)

    /** A file elektronik opens as a project (Projects → open file). */
    fun elektronikFile(plan: Plan, ru: Boolean): String =
        BoardBundle.encode(plan.name, elektronikProjects(plan, ru), id = Roomik.BUNDLE_PREFIX + plan.id)

    /** Distances for cable: along the walls between the points nearest to two places, plus the way to those walls. */
    private class Router(val plan: Plan) {
        val pos = plan.nodes.associate { it.id to P(it.x.toDouble(), it.y.toDouble()) }
        val adj = HashMap<Int, MutableList<Pair<Int, Double>>>().also { m ->
            for (w in plan.walls) {
                val l = ((pos[w.a] ?: continue) - (pos[w.b] ?: continue)).len
                m.getOrPut(w.a) { mutableListOf() } += w.b to l
                m.getOrPut(w.b) { mutableListOf() } += w.a to l
            }
        }

        inner class Att(val p: P) {
            val wall: Wall? = plan.walls.filter { it.a in pos && it.b in pos }.minByOrNull { Geo.distToSegment(p, pos[it.a]!!, pos[it.b]!!) }
            val q: P = wall?.let { Geo.project(p, pos[it.a]!!, pos[it.b]!!).first } ?: p
            val off: Double = (q - p).len
        }

        private fun manhattan(a: P, b: P) = abs(a.x - b.x) + abs(a.y - b.y)

        private fun along(a: Att, b: Att): Double {
            val wa = a.wall ?: return manhattan(a.p, b.p)
            val wb = b.wall ?: return manhattan(a.p, b.p)
            if (wa.id == wb.id) return (a.q - b.q).len
            val dist = HashMap<Int, Double>()
            val queue = PriorityQueue<Pair<Int, Double>>(compareBy { it.second })
            for (n in listOf(wa.a, wa.b)) { val d = (a.q - pos[n]!!).len; dist[n] = d; queue += n to d }
            while (queue.isNotEmpty()) {
                val (n, d) = queue.poll()
                if (d > (dist[n] ?: Double.MAX_VALUE)) continue
                for ((m, l) in adj[n].orEmpty()) if (d + l < (dist[m] ?: Double.MAX_VALUE)) { dist[m] = d + l; queue += m to d + l }
            }
            val r = min((dist[wb.a] ?: 1e18) + (b.q - pos[wb.a]!!).len, (dist[wb.b] ?: 1e18) + (b.q - pos[wb.b]!!).len)
            return if (r >= 1e17) manhattan(a.p, b.p) else r
        }

        fun dist(p: ElPoint, q: ElPoint): Double {
            val a = Att(P(p.x.toDouble(), p.y.toDouble()))
            val b = Att(P(q.x.toDouble(), q.y.toDouble()))
            val via = a.off + along(a, b) + b.off
            return if (p.kind == PointKind.LAMP && q.kind == PointKind.LAMP) min(via, manhattan(a.p, b.p)) else via
        }

        /** [points] in the order a cable would visit them from [from]: always the nearest one next. */
        fun order(from: ElPoint?, points: List<ElPoint>): List<ElPoint> {
            val left = points.toMutableList()
            val out = ArrayList<ElPoint>()
            var cur = from
            while (left.isNotEmpty()) {
                val c = cur
                val next = if (c == null) left[0] else left.minBy { dist(c, it) }
                left.remove(next); out += next; cur = next
            }
            return out
        }
    }

    /** What to buy for the whole house: with several boards, each board's lines added up. */
    fun build(root: Plan, ru: Boolean): Bom = withFeeds(root, ru, buildAll(root, ru))

    /** [bom] with the feed cables between boards added: a line each, and their cable in the totals. */
    private fun withFeeds(root: Plan, ru: Boolean, bom: Bom): Bom {
        val feeds = Feeds.all(root)
        if (feeds.isEmpty()) return bom
        val feedLines = feeds.map { f ->
            LineInfo(Boards.name(f.from, ru) + " → " + Boards.name(f.to, ru), "", f.cores, f.mm2, f.length / 1000.0, drawn = true, id = "feed-" + f.to.key)
        }
        val extra = feedLines.groupBy { it.cores to it.mm2 }.mapValues { (_, l) -> l.sumOf { it.meters } }
        val keys = (bom.cables.map { it.cores to it.mm2 } + extra.keys).distinct()
        val cables = keys.map { k ->
            val old = bom.cables.firstOrNull { (it.cores to it.mm2) == k }
            val add = extra[k] ?: 0.0
            CableTotal(k.first, k.second, (old?.meters ?: 0) + if (add > 0) ceil(add * (1 + RESERVE)).toInt() else 0, (old?.net ?: 0.0) + add)
        }.sortedWith(compareBy({ it.mm2 }, { it.cores }))
        return Bom(bom.hasBoard, bom.lines + feedLines, cables, bom.items, bom.board, bom.unlinkedLamps)
    }

    private fun buildAll(root: Plan, ru: Boolean): Bom {
        val boards = Boards.all(root)
        if (boards.size <= 1) return buildOne(root, ru)
        val parts = boards.map { b -> b to buildOne(Boards.only(root, b), ru) }
        val lines = parts.flatMap { (b, bom) -> bom.lines.map { LineInfo(Boards.name(b, ru) + " · " + it.name, it.rating, it.cores, it.mm2, it.meters, it.drawn) } }
        val cables = parts.flatMap { it.second.cables }.groupBy { it.cores to it.mm2 }
            .map { (k, l) -> CableTotal(k.first, k.second, l.sumOf { it.meters }, l.sumOf { it.net }) }.sortedWith(compareBy({ it.mm2 }, { it.cores }))
        val items = parts.flatMap { it.second.items }.groupBy { it.key to it.detail }.map { (k, l) -> BomItem(k.first, k.second, l.sumOf { it.qty }) }
        val gear = parts.flatMap { it.second.board }.groupBy { it.title to it.rating }.map { (k, l) -> BoardItem(k.first, k.second, l.sumOf { it.qty }) }
        val unlinked = root.allFloors.sumOf { f -> f.points.count { l -> l.kind == PointKind.LAMP && (l.switchId == null || f.points.none { it.id == l.switchId }) } }
        return Bom(true, lines, cables, items, gear, unlinked)
    }

    private fun buildOne(root: Plan, ru: Boolean): Bom {
        val project = elektronikProject(root, ru)
        val groups = groups(root, ru)
        // The board, and the floor it is on. Floors without it reach it straight up or down from its place.
        val boardFloor = root.levels.firstOrNull { fi -> root.floor(fi).points.any { it.kind == PointKind.BOARD } }
        val realBoard = boardFloor?.let { bf -> root.floor(bf).points.first { it.kind == PointKind.BOARD } }
        val lines = ArrayList<LineInfo>()
        var drawn = false
        fun xy(p: ElPoint) = P(p.x.toDouble(), p.y.toDouble())
        // On the board's floor, the riser nearest the board: cables for other floors go there and up or down it.
        val boardRiser = realBoard?.let { b -> root.floor(boardFloor!!).points.filter { it.kind == PointKind.RISER }.minByOrNull { (xy(it) - xy(b)).len } }
        val toRiser = if (realBoard != null && boardRiser != null) Router(root.floor(boardFloor!!)).dist(realBoard, boardRiser) else 0.0
        for (fi in root.levels) {
        val plan = root.floor(fi)
        var boardExtra = 0.0
        // drawn lines start at the riser here: the cable also runs up or down the riser from the board's floor
        var riserExtra = 0.0
        val board = realBoard?.let { b ->
            if (fi == boardFloor) return@let b
            val h = b.height + root.elevation(boardFloor!!) - root.elevation(fi)
            // Cables come to this floor up the riser (its counterpart here), or else straight up from the board's place.
            val riser = plan.points.filter { it.kind == PointKind.RISER }.minByOrNull { (xy(it) - xy(boardRiser ?: b)).len }
            if (riser != null) { boardExtra = toRiser; riserExtra = abs(h - riser.height).toDouble(); riser.copy(height = h) } else b.copy(id = -1, height = h)
        }
        val router = Router(plan)
        val level = plan.wallHeight - LEVEL_BELOW_CEILING
        // A lamp hangs where its cable already runs, under the ceiling.
        fun rise(p: ElPoint) = if (p.kind == PointKind.LAMP) 0.0 else abs(level - p.height).toDouble()
        /** Cable between two points one after another on a line: as drawn, or up the wall, along it and down. */
        fun seg(a: ElPoint, b: ElPoint): Double {
            Cables.between(plan, a.id, b.id)?.let { drawn = true; return Cables.length(plan, it) }
            return rise(a) + router.dist(a, b) + rise(b)
        }
        /** A cable from [start] through [points] in this order, with spare cable in each box ([slack] mm). */
        fun chain(start: ElPoint, points: List<ElPoint>, slack: Int): Double {
            var mm = 0.0
            var cur = start
            for (p in points) { mm += seg(cur, p) + slack; cur = p }
            return mm
        }
        fun line(name: String, rating: String, cores: Int, mm2: Double, mm: Double, id: String) {
            lines += LineInfo(name, rating, cores, mm2, mm / 1000, drawn, id); drawn = false
        }
        // Lines drawn from the board (or, on another floor, from a riser bringing its cables), with junction boxes.
        val starts = if (fi == boardFloor) listOfNotNull(realBoard) else if (realBoard != null) plan.points.filter { it.kind == PointKind.RISER } else emptyList()
        val drawnLines = starts.flatMap { Circuits.lines(plan, it.id) }.filter { it.devices.isNotEmpty() }
        /** Cable for [points] when every one of them is on a drawn line: those lines, shared by how many devices each has. */
        fun drawnMm(points: List<ElPoint>): Double? {
            if (points.isEmpty() || drawnLines.isEmpty()) return null
            val ids = points.map { it.id }.toSet()
            val ls = drawnLines.filter { l -> l.devices.any { it.id in ids } }
            if (ids.any { id -> ls.none { l -> l.devices.any { it.id == id } } }) return null
            drawn = true
            return ls.sumOf { l -> (l.mm + boardExtra + riserExtra) * l.devices.count { it.id in ids } / l.devices.size }
        }

        for (g in groups.filter { it.floor == fi }) {
            // Lights: board → the switches one after another, then from each switch to the lamps it switches.
            project.device("room-${g.id}-light")?.let { dev ->
                val earth = dev.loads.firstOrNull()?.groundWire ?: true
                var mm = 0.0
                val byDrawing = if (board != null) drawnMm(g.switches + g.lamps) else null
                if (byDrawing != null) mm = BOARD_SLACK + byDrawing
                else if (board != null) {
                    val switches = router.order(board, g.switches)
                    mm += BOARD_SLACK + boardExtra + chain(board, switches, BOX_SLACK)
                    val loose = g.lamps.filter { l -> l.switchId == null || plan.points.none { it.id == l.switchId } }
                    if (loose.isNotEmpty()) mm += chain(switches.lastOrNull() ?: board, router.order(switches.lastOrNull() ?: board, loose), BOX_SLACK)
                    for (s in switches) mm += chain(s, router.order(s, g.lamps.filter { it.switchId == s.id }), BOX_SLACK)
                }
                line(dev.name, dev.ratingText, if (earth) 3 else 2, Cable.minFor(dev.ratingA, false) ?: 1.5, mm, dev.id)
            }
            // Sockets, with small appliances plugged in: elektronik says how many lines and how many outlets on each.
            val small = g.appliances.filter { project.device("room-${g.id}-load-a${it.id}") == null }
            val left = router.order(board, g.sockets + small).toMutableList()
            var i = 1
            while (true) {
                val dev = project.device("room-${g.id}-sockets-$i") ?: break
                val share = dev.loads.filter { it.kind == LoadKind.SOCKETS }.sumOf { it.count }
                val mine = ArrayList<ElPoint>()
                var n = 0
                while (left.isNotEmpty() && (n < share || project.device("room-${g.id}-sockets-${i + 1}") == null)) { n += left[0].count; mine += left.removeAt(0) }
                val mm = if (board != null && mine.isNotEmpty()) BOARD_SLACK + (drawnMm(mine) ?: (boardExtra + chain(board, mine, 2 * BOX_SLACK))) else 0.0
                line(dev.name, dev.ratingText, 3, Cable.minFor(dev.ratingA, false) ?: 2.5, mm, dev.id)
                i++
            }
            // Appliances with a line of their own: straight from the board.
            for (a in g.appliances) {
                val dev = project.device("room-${g.id}-load-a${a.id}") ?: continue
                val mm = if (board != null) BOARD_SLACK + (drawnMm(listOf(a)) ?: (boardExtra + seg(board, a) + 2 * BOX_SLACK)) else 0.0
                val cores = if (dev.poles >= 3) 5 else 3
                line(dev.name, dev.ratingText, cores, dev.cableMm2 ?: Cable.minFor(dev.ratingA, false) ?: 2.5, mm, dev.id)
            }
        }
        }

        val cables = lines.filter { it.meters > 0 }.groupBy { it.cores to it.mm2 }.map { (k, l) ->
            val net = l.sumOf { it.meters }
            CableTotal(k.first, k.second, ceil(net * (1 + RESERVE)).toInt(), net)
        }.sortedWith(compareBy({ it.mm2 }, { it.cores }))

        val items = ArrayList<BomItem>()
        val sockets = groups.flatMap { g -> g.sockets.map { g to it } }
        val switches = groups.flatMap { it.switches }
        val lamps = groups.flatMap { it.lamps }
        val appliances = groups.flatMap { it.appliances }
        val own = appliances.filter { a -> groups.any { g -> project.device("room-${g.id}-load-a${a.id}") != null && a in g.appliances } }
        val plugged = appliances - own.toSet()
        val boxes = sockets.sumOf { it.second.count } + switches.size + appliances.size
        if (boxes > 0) items += BomItem(BomKey.BACK_BOX, 0, boxes)
        // Splash-proof sockets where people stand wet or outdoors.
        fun wet(g: Group) = g.type == RoomType.BATHROOM || g.type == RoomType.YARD || g.type == RoomType.BALCONY || g.type == RoomType.BASEMENT
        sockets.filter { !wet(it.first) }.sumOf { it.second.count }.takeIf { it > 0 }?.let { items += BomItem(BomKey.SOCKET, 0, it) }
        sockets.filter { wet(it.first) }.sumOf { it.second.count }.takeIf { it > 0 }?.let { items += BomItem(BomKey.SOCKET_IP44, 0, it) }
        // A small appliance plugs into a socket; one with its own line gets a power outlet or terminal box.
        if (plugged.isNotEmpty()) items.indexOfFirst { it.key == BomKey.SOCKET }.let { k ->
            if (k >= 0) items[k] = BomItem(BomKey.SOCKET, 0, items[k].qty + plugged.size) else items += BomItem(BomKey.SOCKET, 0, plugged.size)
        }
        if (own.isNotEmpty()) items += BomItem(BomKey.POWER_OUTLET, 0, own.size)
        for ((gangs, l) in switches.groupBy { it.count }.toSortedMap()) items += BomItem(BomKey.SWITCH, gangs, l.size)
        val frames = sockets.map { it.second.count } + switches.map { 1 } + plugged.map { 1 }
        for ((posts, l) in frames.groupBy { it }.toSortedMap()) items += BomItem(BomKey.FRAME, posts, l.size)
        if (lamps.isNotEmpty()) {
            items += BomItem(BomKey.LAMP, 0, lamps.size)
            items += BomItem(BomKey.CONNECTOR, 0, lamps.size * 3)
        }
        // junction boxes where drawn cables branch, each with a set of connectors
        val boxes2 = root.allFloors.sumOf { Circuits.boxes(it) }
        if (boxes2 > 0) {
            items += BomItem(BomKey.JUNCTION_BOX, 0, boxes2)
            items.indexOfFirst { it.key == BomKey.CONNECTOR }.let { k ->
                if (k >= 0) items[k] = BomItem(BomKey.CONNECTOR, 0, items[k].qty + boxes2 * 3) else items += BomItem(BomKey.CONNECTOR, 0, boxes2 * 3)
            }
        }
        val total = cables.sumOf { it.net }
        if (total > 0) {
            items += BomItem(BomKey.CONDUIT, 0, ceil(total).toInt())
            items += BomItem(BomKey.CLIP, 0, ceil(total / 0.4).toInt())
        }

        val gear = project.devices.filter { it.type != DeviceType.MAIN_SWITCH || true }
            .groupBy { it.type.localTitle to it.ratingText }
            .map { (k, l) -> BoardItem(k.first, k.second, l.size) }

        val unlinked = root.allFloors.sumOf { f -> f.points.count { l -> l.kind == PointKind.LAMP && (l.switchId == null || f.points.none { it.id == l.switchId }) } }
        return Bom(realBoard != null, lines, cables, items, gear, unlinked)
    }
}
