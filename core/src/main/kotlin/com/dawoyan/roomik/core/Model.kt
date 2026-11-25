package com.dawoyan.roomik.core

import com.dawoyan.elektronik.core.RoomType
import kotlinx.serialization.Serializable

/** A corner of the wall graph. Coordinates are millimetres; y grows downwards on the plan. */
@Serializable
data class Node(val id: Int, val x: Int, val y: Int)

/** A wall between two corners, drawn on its centre line. [thickness] in mm. */
@Serializable
data class Wall(
    val id: Int, val a: Int, val b: Int, val thickness: Int = DEFAULT_THICKNESS,
    /** Height in mm when this wall is lower (a parapet, a half wall) or higher than the floor's walls; null = as the floor's. */
    val height: Int? = null,
    /** What its outside face is clad with (tuf, travertine, basalt…); null: plain plaster. */
    val facing: Facing? = null,
    /** Set side by side instead (left and right looking from corner [a] to [b]); when either is set, [facing] is not used. */
    val left: Facing? = null,
    val right: Facing? = null,
) {
    /** The same wall run the other way (b to a): its sides swap. */
    fun flipped(): Wall = copy(a = b, b = a, left = right, right = left)
}

/**
 * What the user said about a room. Rooms themselves are derived from the walls ([Rooms.detect]); this is tied to one
 * by a point inside it ([x], [y]), so it survives edits that change the wall loop. [type] is elektronik's room type,
 * so the electrical hand-off needs no mapping.
 */
@Serializable
data class RoomInfo(
    val x: Int, val y: Int, val name: String = "", val type: RoomType? = null, val id: String = "",
    /** Warm floor (тёплый пол) in this room. */
    val warmFloor: Boolean = false,
)

/** Electrical points on the plan. Sockets and switches sit on a wall; lamps hang from the ceiling. */
@Serializable
enum class PointKind { SOCKET, SWITCH, LAMP, BOARD, APPLIANCE,
    /** Low-current / communications board: router, patch panel, alarm and intercom feeds. */
    LOW_CURRENT_BOARD,
    /** Communications junction / termination box, normally installed on a wall. */
    LOW_CURRENT_BOX,
    /** A vertical channel for cables between floors; placed on every floor at the same spot. */
    RISER,
    /** A junction box (распаечная коробка) where a cable branches: made by joining a cable onto another one. */
    JUNCTION }

/** Points that hang free (a lamp, a junction box under the ceiling), not on a wall. */
val PointKind.free: Boolean get() = this == PointKind.LAMP || this == PointKind.JUNCTION

/**
 * One electrical point. For a socket or switch, ([x], [y]) is on the wall's centre line and ([nx], [ny]) is the unit
 * normal towards the room it faces; [count] is the number of outlets or switch gangs; [height] is mm above the floor.
 * For a lamp, ([x], [y]) is where it hangs and [watts] its power.
 */
@Serializable
data class ElPoint(
    val id: Int, val kind: PointKind, val x: Int, val y: Int,
    val nx: Double = 0.0, val ny: Double = 0.0,
    val count: Int = 1, val height: Int = 300, val watts: Int = 10,
    /** For a lamp: the switch that controls it. */
    val switchId: Int? = null,
    /** A device explicitly chosen as this load's electrical source; unlike [board], it keeps the actual upstream device. */
    val sourceId: Int? = null,
    /** For a switch: the other switch of a two-way pair (проходной), controlling the same lamps from another place. */
    val pairId: Int? = null,
    /** For an appliance outlet: what it feeds and its power; [ownLine] gives it a breaker of its own even when small. */
    val name: String = "",
    val kw: Double = 0.0,
    val ownLine: Boolean = false,
    val threePhase: Boolean = false,
    /** For a board: a lasting id that points name in [board]; its name is [name]. */
    val uid: String = "",
    /** The board that feeds this point ([uid] of a board, or "floor:id" of an old one); null: the nearest board. */
    val board: String? = null,
    /** For a board: the number it is known by ("Board 3"); 0: by its place in the list. */
    val num: Int = 0,
    /** For a socket: the voltage of its line, 220 or 110 (a line and cable of its own). */
    val volts: Int = 220,
    /** For a board: the board it is fed from (that board's key, on any floor); null: fed from the mains. */
    val source: String? = null,
    /** For a board with a [source]: the feed cable's wire size, mm². */
    val feedMm2: Double = 6.0,
    /** For a board with a [source]: turns of its feed cable on the source's floor (plan mm); empty: the straight way. */
    val feedVia: List<Node> = emptyList(),
)

/** Where a cable runs between its turns: in the ceiling (just under it) or in the floor. */
@Serializable
enum class RouteLevel { CEILING, FLOOR }

/**
 * A cable the user drew: from point [from] through the turns [via] (plan mm) to point [to]. Between the turns it runs at
 * [level]; at each end it goes straight up or down the wall to the point.
 */
@Serializable
data class Route(
    val id: Int, val from: Int, val to: Int, val via: List<Node> = emptyList(), val level: RouteLevel = RouteLevel.CEILING,
    /** Cross-section of each wire, mm², and how many wires the cable has (3: L, N, PE). */
    val mm2: Double = 2.5, val cores: Int = 3,
    /** What the user calls the cable ("Kitchen sockets"), shown on the plan. */
    val label: String = "",
    /** The line's voltage: a 110 V cable is a separate line, drawn in a colour of its own. */
    val volts: Int = 220,
    /** The line's number on its board, as the user set it; 0: numbered in order. */
    val num: Int = 0,
)

/** Shapes of stairs: one flight; two flights with a landing between; turning left or right at a landing; or back (U). */
@Serializable
enum class StairKind { STRAIGHT, LANDING, L_LEFT, L_RIGHT, U, U_RIGHT }

/**
 * Stairs, part of the building: they start at ([x], [y]) (the middle of the bottom step's front edge) and go up in
 * direction [rot] degrees. [width] of a flight; [length] of the first leg (with its landing); [length2] of the second
 * flight after an L turn. [rise] is how high they go, mm; null = up to the floor above (this floor's walls plus the slab).
 * They are not furniture: they pass over walls and things below them.
 */
@Serializable
data class Stair(
    val id: Int, val kind: StairKind = StairKind.STRAIGHT, val x: Int, val y: Int, val rot: Int = 0,
    val width: Int = 1000, val length: Int = 3000, val length2: Int = 1500, val rise: Int? = null,
    /** Climbs the other way: the top is at ([x], [y]) and it goes down along the shape. */
    val reversed: Boolean = false,
    /** Starts on the ground outside and goes up to this floor (a porch); only on the ground floor. */
    val fromGround: Boolean = false,
    /**
     * Walls the user agreed to cut where these stairs pass ("crop wall by stairs"). The cut follows the stairs and is
     * forgotten when they are moved, turned or resized, so the walls come back whole.
     */
    val cropped: List<Int> = emptyList(),
)

/** House systems drawn as lines: network cable, cold and hot water, sewer, heating pipes. */
@Serializable
enum class SystemKind(val defaultHeight: Int, val thickness: Int, val color: Int) {
    NETWORK(2550, 12, 0x2E9E5B),
    WATER_COLD(300, 25, 0x2F7FD6),
    WATER_HOT(300, 25, 0xF08C1A),
    SEWER(50, 50, 0x8D6E63),
    HEATING(100, 25, 0xC2185B),
}

/** A pipe or cable of a house system: through [points] (plan mm) at [height] mm above the floor. */
@Serializable
data class Pipe(
    val id: Int, val system: SystemKind, val points: List<Node>, val height: Int,
    /** Outer diameter, mm (0: the system's usual one); for a network line, the wires in each cable. */
    val size: Int = 0,
    /** How many pipes or cables run here side by side. */
    val count: Int = 1,
    /** Communication carried by a NETWORK run.  POWER is only used by cross-floor links. */
    val cable: CommCable = CommCable.CAT6,
)

/** The communication inside a low-current cable. */
@Serializable
enum class CommCable { POWER, CAT5E, CAT6, CAT6A, FIBER, COAX, ALARM, INTERCOM }

/** The surface used for the horizontal part of a cable travelling between floors. */
@Serializable
enum class CrossLevel { CEILING, FLOOR, WALL }

/** A cable whose ends are on different floors.  Its horizontal route is stored on [fromFloor]. */
@Serializable
data class CrossCable(
    val id: Int,
    val fromFloor: Int, val fromId: Int,
    val toFloor: Int, val toId: Int,
    val cable: CommCable = CommCable.POWER,
    val level: CrossLevel = CrossLevel.CEILING,
    val via: List<Node> = emptyList(),
)

/** Usual sizes: pipe diameters in mm; for a network cable, its wires. */
val SystemKind.sizes: List<Int> get() = when (this) {
    SystemKind.NETWORK -> listOf(4, 8)
    SystemKind.WATER_COLD, SystemKind.WATER_HOT -> listOf(16, 20, 25, 32, 40)
    SystemKind.SEWER -> listOf(32, 40, 50, 110, 160)
    SystemKind.HEATING -> listOf(16, 20, 25, 32)
}
val SystemKind.defaultSize: Int get() = when (this) {
    SystemKind.NETWORK -> 8; SystemKind.SEWER -> 110; else -> 20
}
fun Pipe.sizeOrDefault(): Int = if (size > 0) size else system.defaultSize

/** What can be shown or hidden on the plan and in 3D. */
enum class Layer { ELECTRIC, NETWORK, WATER, HEATING, WARM_FLOOR, FURNITURE, ROOF, FENCES, SEWER }

/** Roof shapes: flat, one slope (односкатная), two slopes (двускатная), four slopes (вальмовая). */
@Serializable
enum class RoofKind { FLAT, SHED, GABLE, HIP }

/**
 * A roof over the rectangle ([x1], [y1])–([x2], [y2]) (plan mm, on the wall lines), resting on top of this floor's walls.
 * [pitch] in degrees; [alongX]: the ridge (or, for a single slope, the slope's level edges) runs along plan x;
 * [flip] turns a single slope round; [overhang] how far the eaves stick out, mm. [attic]: the rooms under it are an
 * attic (мансарда) with the sloping roof as their ceiling, not a flat one.
 */
@Serializable
data class Roof(
    val id: Int, val x1: Int, val y1: Int, val x2: Int, val y2: Int,
    val kind: RoofKind = RoofKind.GABLE, val pitch: Int = 30, val alongX: Boolean = true,
    val flip: Boolean = false, val overhang: Int = 400, val attic: Boolean = false,
    /** Height of the roof's base (where the slopes meet the wall lines) above the floor, mm; null: the highest wall under it. */
    val base: Int? = null,
    /** What it is covered with, and its colour (null: the covering's usual one). */
    val cover: RoofCover = RoofCover.TILES,
    val color: Int? = null,
)

/** Roof coverings: clay tiles, profiled (corrugated) sheet, metal tile, soft (bitumen) tile, slate. */
@Serializable
enum class RoofCover(val color: Int, val sheetW: Int, val sheetL: Int) {
    TILES(0xA0463A, 0, 0), PROFILE_SHEET(0x3A3D42, 1150, 6000), METAL_TILE(0x6B2E2A, 1180, 3600), SOFT_TILE(0x5A3A30, 1000, 3000), SLATE(0x4E5358, 0, 0),
}

/** Usual roof colours to pick from. */
val ROOF_COLORS = listOf(0xA0463A, 0x6B2E2A, 0x3B2F2A, 0x2E3034, 0x3A3D42, 0x7A7E84, 0x3E6B48, 0x34506E, 0xB0805A)

/** A roof's colour as shown. */
val Roof.shownColor: Int get() = color ?: cover.color

/**
 * A platform: a balcony, terrace, porch deck or mezzanine, a slab whose top is [height] mm above this floor, with a
 * railing along its open sides and, when it has [columns], posts down to the ground.
 */
@Serializable
data class Platform(
    val id: Int, val x1: Int, val y1: Int, val x2: Int, val y2: Int,
    val height: Int = 0, val thickness: Int = 200,
    val railing: Boolean = true, val railingHeight: Int = 1000,
    val columns: Boolean = false,
)

/**
 * A pillar (колонна) centred on ([x], [y]): [w] × [d] mm, or round with diameter [w]; [height] mm, null = up to the
 * floor's ceiling (and through the slab when a floor stands on it). The pillars' centre lines make a grid that walls
 * snap to while they are drawn.
 */
@Serializable
data class Pillar(
    val id: Int, val x: Int, val y: Int, val w: Int = 400, val d: Int = 400,
    val round: Boolean = false, val height: Int? = null,
)

/** Kinds of fence, with their usual colour. */
@Serializable
enum class FenceKind(val color: Int) {
    PICKET(0xA87B4F), BOARD(0x8A6440), METAL(0x3E6B48), BARS(0x2B2D31), MESH(0x8D949C), BRICK(0xA0523D), HEDGE(0x4F7A3A),
    /** Stone blocks (tuf, basalt…) laid as a solid wall. */
    STONE(0xA9827A),
}

/** Posts of a stone or brick fence: of its own stone, cast concrete (with a concrete top), or none (a plain wall). */
@Serializable
enum class FencePosts { OWN, CONCRETE, NONE }

/** Gates in a fence: a car gate with two swinging leaves or one sliding leaf, or a small gate for people. */
enum class GateKind(val width: Int) { SWING(3500), SLIDING(4000), WICKET(1000),
    /** One wide swinging leaf; a garage door lifting up in panels (sectional); a roller shutter. */
    SINGLE(3000), SECTIONAL(2500), ROLLER(2500) }

/** A gate in a fence, its middle [at] mm along the fence from its first point, [width] mm wide. */
@Serializable
data class Gate(val at: Int, val width: Int, val kind: GateKind = GateKind.SWING)

/** A fence along [points] (plan mm), [height] mm high, with [gates] in it. */
@Serializable
data class Fence(val id: Int, val kind: FenceKind, val points: List<Node>, val height: Int = 1600, val gates: List<Gate> = emptyList(),
    /** For a stone or brick fence: its posts (of the same stone, concrete, or none) and how far apart, mm. */
    val posts: FencePosts = FencePosts.OWN, val postStep: Int = 2500,
    /** For a stone or brick fence: the stone it is laid of; null: the kind's own. */
    val facing: Facing? = null,
    /** For a stone or brick fence: how thick its wall is, mm. */
    val thickness: Int = 250)

val SystemKind.layer: Layer get() = when (this) {
    SystemKind.NETWORK -> Layer.NETWORK
    SystemKind.WATER_COLD, SystemKind.WATER_HOT -> Layer.WATER
    SystemKind.SEWER -> Layer.SEWER
    SystemKind.HEATING -> Layer.HEATING
}

/** Heights in mm above the floor that new points get. */
@Serializable
data class Heights(val socket: Int = 300, val switch: Int = 900, val board: Int = 1500, val appliance: Int = 600)

/** A door with a leaf, a doorway with only a frame (no leaf), or a window. */
@Serializable
enum class OpeningKind { DOOR, WINDOW, PASSAGE,
    /** A gate for cars or people in a wall (a fence built as a wall, a garage): of the kind [Opening.gate]. */
    GATE }

/**
 * A door or window in a wall: ([x], [y]) is its middle on the wall's centre line, ([nx], [ny]) the side a door opens to.
 * [width] and [height] in mm; [sill] is the height of its bottom edge above the floor.
 */
@Serializable
data class Opening(
    val id: Int, val kind: OpeningKind, val x: Int, val y: Int, val nx: Double = 0.0, val ny: Double = 0.0,
    val width: Int, val height: Int, val sill: Int = 0,
    /** For a door: the hinge is at the far end of the opening (seen from the wall's start) instead of the near end. */
    val hingeEnd: Boolean = false,
    /** For a gate: how it opens. */
    val gate: GateKind = GateKind.SLIDING,
)

/** What a road or path is made of, with its colour. */
@Serializable
enum class Surface(val color: Int) { ASPHALT(0x4A4D52), CONCRETE(0xB9B6AE), PAVERS(0xA87E66), STONE(0x8A8580), GRAVEL(0xA79F90), DIRT(0x9C8668), GRASS(0x67A84D), GRASS_GRID(0x7E9A6A) }

/** A road, drive or path on the ground: along [points] (plan mm), [width] mm wide, of [surface]. */
@Serializable
data class Road(val id: Int, val points: List<Node>, val width: Int = 3500, val surface: Surface = Surface.ASPHALT, val closed: Boolean = false)

/**
 * A place that is not a closed room: a garage drawn with open walls, a yard, a terrace. Points outside every closed room
 * belong to the nearest place. ([x], [y]) is where its label is; [id] is a lasting id elektronik knows it by.
 */
@Serializable
data class Place(val id: String, val x: Int, val y: Int, val name: String = "", val type: RoomType? = null)

/**
 * One part of a 3D model, in the model's own millimetres: [xyz] holds x (plan x), y (plan y, toward the front) and z
 * (up) for each corner, [tri] three corner indices per triangle. [color] is 0xRRGGBB.
 */
@Serializable
data class ModelPart(val name: String, val color: Int, val xyz: List<Float>, val tri: List<Int>)

/**
 * A 3D model on the plan: its parts centred on ([x], [y]) (plan mm) and standing [z] mm above the floor, turned by [rot]
 * degrees. [name] is what it was sent as: sent again under that name, its parts are replaced and it stays in place.
 */
@Serializable
data class Model3D(
    val id: Int, val name: String, val x: Int, val y: Int, val z: Int = 0, val rot: Int = 0, val parts: List<ModelPart> = emptyList(),
    /** For a yard construction made in Roomik (a canopy, gazebo…): its sizes, from which [parts] are made. */
    val yard: YardSpec? = null,
)

/** A piece of furniture: a box [w] × [d] × [h] mm centred on ([x], [y]), turned by [rot] degrees. [kind] is a [Catalog] id. */
@Serializable
data class Item(val id: Int, val kind: String, val x: Int, val y: Int, val w: Int, val d: Int, val h: Int, val rot: Int = 0,
    /** How high above the floor it hangs, mm (an air conditioner on a wall); 0: standing on the floor. */
    val z: Int = 0)

@Serializable
data class Plan(
    val id: String,
    val name: String,
    val updatedAt: Long = 0,
    /** Wall height in mm. */
    val wallHeight: Int = 2700,
    val nodes: List<Node> = emptyList(),
    val walls: List<Wall> = emptyList(),
    val rooms: List<RoomInfo> = emptyList(),
    val points: List<ElPoint> = emptyList(),
    val items: List<Item> = emptyList(),
    val openings: List<Opening> = emptyList(),
    val routes: List<Route> = emptyList(),
    val heights: Heights = Heights(),
    val places: List<Place> = emptyList(),
    val stairs: List<Stair> = emptyList(),
    val pipes: List<Pipe> = emptyList(),
    val roofs: List<Roof> = emptyList(),
    val fences: List<Fence> = emptyList(),
    val platforms: List<Platform> = emptyList(),
    val pillars: List<Pillar> = emptyList(),
    /** Roads and paths drawn on this level; they lie on the ground. */
    val roads: List<Road> = emptyList(),
    /** Floors above this one, from the lowest; only the ground floor (the plan itself) has them. */
    val floors: List<Plan> = emptyList(),
    /** Floors below ground, from the one just under the ground floor down. */
    val basements: List<Plan> = emptyList(),
    /** How high the ground floor's floor is above the ground outside, mm (a plinth); 0 = level with it. */
    val groundLevel: Int = 0,
    /** Where south is on the plan, degrees clockwise from the bottom of the plan (0: south is down). Only the plan itself has it. */
    val south: Int = 0,
    /** For a floor below the first: it stands on the ground (a ground floor with a garage, stores), not dug into it. */
    val onGround: Boolean = false,
    /** The ground round the house (only the plan itself has it): fences, cars, paths, trees… standing on the ground. */
    val site: Plan? = null,
    /** 3D models brought in from Idea 3D (or any 3MF, STL, OBJ): a shed, a canopy, a pergola… standing on this level. */
    val models: List<Model3D> = emptyList(),
    /** Cross-floor power and low-current cables.  Kept only on the root plan. */
    val crossCables: List<CrossCable> = emptyList(),
    val schemaVersion: Int = 3,
) {
    fun node(id: Int): Node? = nodes.firstOrNull { it.id == id }
    fun wall(id: Int): Wall? = walls.firstOrNull { it.id == id }
    fun pos(id: Int): P? = node(id)?.let { P(it.x.toDouble(), it.y.toDouble()) }
    /** Height of [w], mm. */
    fun heightOf(w: Wall): Int = w.height ?: wallHeight

    fun length(w: Wall): Double {
        val a = pos(w.a) ?: return 0.0
        val b = pos(w.b) ?: return 0.0
        return (b - a).len
    }
}

const val DEFAULT_THICKNESS = 200

/** Thickness of the slab between two floors, mm. */
const val SLAB = 200

/** The ground round the house as a level of its own, between the floors under the ground and those over it. */
const val SITE = 1000

/** Floor [index] of [root]: 0 is the root itself (the ground floor), 1 the first floor above, -1 the first below… */
fun Plan.floor(index: Int): Plan = when {
    index == SITE -> site ?: Plan("site", "site")
    index == 0 -> this
    index > 0 -> floors.getOrNull(index - 1) ?: this
    else -> basements.getOrNull(-index - 1) ?: this
}

/** The floor numbers [this] has, from the lowest basement up. */
val Plan.floorRange: IntRange get() = -basements.size..floors.size
/** Every level things can stand on: the floors, and the ground round the house when there is one. */
val Plan.levels: List<Int> get() = floorRange.toList() + listOfNotNull(site?.let { SITE })

/** All floors of [this] from the lowest up; the ground floor is the plan itself. */
val Plan.allFloors: List<Plan> get() = levels.map { floor(it) }

/** [this] with floor [index] replaced by [p]. */
fun Plan.withFloor(index: Int, p: Plan): Plan {
    val bare = p.copy(floors = emptyList(), basements = emptyList())
    return when {
        index == SITE -> copy(site = bare.copy(site = null))
        index == 0 -> p.copy(floors = floors, basements = basements, site = site)
        index > 0 -> copy(floors = floors.mapIndexed { i, f -> if (i == index - 1) bare else f })
        else -> copy(basements = basements.mapIndexed { i, f -> if (i == -index - 1) bare else f })
    }
}

/** Height of floor [index]'s floor above the ground floor's, mm; below ground it is negative. */
fun Plan.elevation(index: Int): Int =
    if (index == SITE) groundMm
    else if (index >= 0) (0 until index.coerceAtMost(floors.size)).sumOf { floor(it).wallHeight + SLAB }
    else -(index.coerceAtLeast(-basements.size)..-1).sumOf { floor(it).wallHeight + SLAB }

/** The floor standing on the ground: the lowest of the floors on the ground under the first, else the first (0). */
val Plan.groundFloor: Int get() {
    var g = 0
    while (g - 1 >= -basements.size && floor(g - 1).onGround) g--
    return g
}

/** Where the ground outside is, mm from the first floor's floor (below it: negative). */
val Plan.groundMm: Int get() = elevation(groundFloor) - groundLevel

/** How a floor is called to people: 1, 2… above ground, -1, -2… below. */
fun floorNumber(index: Int): Int = if (index >= 0) index + 1 else index
