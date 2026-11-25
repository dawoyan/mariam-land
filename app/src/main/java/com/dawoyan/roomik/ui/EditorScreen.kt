package com.dawoyan.roomik.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Chair
import com.dawoyan.roomik.core.withFloor
import com.dawoyan.roomik.core.Model3D
import com.dawoyan.roomik.core.Cover
import com.dawoyan.roomik.core.TrussKind
import com.dawoyan.roomik.core.RoofShape
import com.dawoyan.roomik.core.YardSpec
import com.dawoyan.roomik.core.YardKind
import com.dawoyan.roomik.core.Yard
import androidx.compose.foundation.layout.height
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.graphics.drawscope.withTransform
import com.dawoyan.roomik.core.Models
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.HighlightAlt
import com.dawoyan.roomik.core.Pillar
import com.dawoyan.roomik.core.Autoroute
import com.dawoyan.roomik.core.groundFloor
import com.dawoyan.roomik.core.SITE
import com.dawoyan.roomik.core.Pillars
import com.dawoyan.roomik.core.Feeds
import com.dawoyan.roomik.core.CrossCables
import com.dawoyan.roomik.core.CommCable
import com.dawoyan.roomik.core.CrossLevel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.RadioButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawoyan.elektronik.core.RoomType
import com.dawoyan.roomik.AppViewModel
import com.dawoyan.roomik.R
import com.dawoyan.roomik.Screen
import com.dawoyan.roomik.core.Catalog
import com.dawoyan.roomik.core.Boards
import com.dawoyan.roomik.core.Lines
import com.dawoyan.roomik.core.Node
import com.dawoyan.roomik.core.sizeOrDefault
import com.dawoyan.roomik.core.sizes
import com.dawoyan.roomik.core.ElPoint
import com.dawoyan.roomik.core.Geo
import com.dawoyan.roomik.core.Interior
import com.dawoyan.roomik.core.Item
import com.dawoyan.roomik.core.PointKind
import com.dawoyan.roomik.core.OpeningKind
import com.dawoyan.roomik.core.Opening
import com.dawoyan.roomik.core.Place
import com.dawoyan.roomik.core.STAIRS
import com.dawoyan.roomik.core.SystemKind
import com.dawoyan.roomik.core.FenceKind
import com.dawoyan.roomik.core.RoofKind
import com.dawoyan.roomik.core.Roofs
import com.dawoyan.roomik.core.Platforms
import com.dawoyan.roomik.core.Fences
import com.dawoyan.roomik.core.Facing
import com.dawoyan.roomik.core.Facings
import com.dawoyan.roomik.core.Road
import com.dawoyan.roomik.core.Surface
import com.dawoyan.roomik.core.FencePosts
import com.dawoyan.roomik.core.shownColor
import com.dawoyan.roomik.core.ROOF_COLORS
import com.dawoyan.roomik.core.RoofCover
import androidx.compose.foundation.border
import com.dawoyan.roomik.core.Gate
import com.dawoyan.roomik.core.GateKind
import com.dawoyan.roomik.core.Layer
import com.dawoyan.roomik.core.layer
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Check
import com.dawoyan.roomik.core.Stair
import com.dawoyan.roomik.core.StairKind
import com.dawoyan.roomik.core.Stairs
import com.dawoyan.roomik.core.floor
import com.dawoyan.roomik.core.floorRange
import com.dawoyan.roomik.core.levels
import com.dawoyan.roomik.core.floorNumber
import androidx.compose.material.icons.filled.Place as PlaceIcon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.dawoyan.roomik.core.RouteLevel
import com.dawoyan.roomik.core.Route
import com.dawoyan.roomik.core.Cables
import com.dawoyan.roomik.core.Arrange
import com.dawoyan.roomik.core.Circuits
import com.dawoyan.roomik.core.free
import com.dawoyan.roomik.core.Materials
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.RotateLeft
import com.dawoyan.roomik.core.Furniture
import androidx.compose.ui.graphics.PathEffect
import com.dawoyan.roomik.core.P
import com.dawoyan.roomik.core.Palette
import com.dawoyan.roomik.core.Plan
import com.dawoyan.roomik.core.PlanOps
import com.dawoyan.roomik.core.Room
import com.dawoyan.roomik.core.Rooms
import com.dawoyan.roomik.core.WallShapes
import com.dawoyan.roomik.core.dot
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Tool { SELECT, WALL, PILLAR, ROOM, DOOR, WINDOW, STAIRS, ROOF, PLATFORM, FENCE, PLACE, ELECTRIC, INTERIOR, YARD, SYSTEMS, GATE, ROAD }

private val BUILD = setOf(Tool.WALL, Tool.PILLAR, Tool.ROOM, Tool.DOOR, Tool.WINDOW, Tool.STAIRS, Tool.ROOF, Tool.PLATFORM, Tool.FENCE, Tool.PLACE, Tool.GATE, Tool.ROAD)

sealed interface Sel {
    data class Corner(val id: Int) : Sel
    data class WallSel(val id: Int) : Sel
    /** A room, remembered by a point inside it. */
    data class RoomSel(val at: P) : Sel
    data class PointSel(val id: Int) : Sel
    data class ItemSel(val id: Int) : Sel
    data class OpeningSel(val id: Int) : Sel
    data class RouteSel(val id: Int) : Sel
    data class PlaceSel(val id: String) : Sel
    data class StairSel(val id: Int) : Sel
    data class PipeSel(val id: Int) : Sel
    data class RoofSel(val id: Int) : Sel
    data class PlatformSel(val id: Int) : Sel
    data class FenceSel(val id: Int) : Sel
    data class RoadSel(val id: Int) : Sel
    data class PillarSel(val id: Int) : Sel
    /** A 3D model brought in from Idea 3D (or a 3MF/STL/OBJ file). */
    data class ModelSel(val id: Int) : Sel
    /** A feed cable between boards, known by the board it feeds ([toId] on level [toFloor]). */
    data class FeedSel(val toFloor: Int, val toId: Int) : Sel
    /** A manually created cable travelling between two different floors. */
    data class CrossCableSel(val id: Int) : Sel
    /** Wall corners or pillars picked with a rectangle. */
    data class GroupSel(
        val nodes: Set<Int>, val pillars: Set<Int>,
        /** Picked on the other levels too (all floors on): level → corners and pillars there. */
        val others: Map<Int, Pair<Set<Int>, Set<Int>>> = emptyMap(),
        val ofPillars: Boolean = pillars.isNotEmpty(),
    ) : Sel {
        val otherCount: Int get() = others.values.sumOf { (if (ofPillars) it.second else it.first).size }
    }
    /** Several electric points picked together (by a rectangle in Electrics). */
    data class PointsSel(val ids: Set<Int>) : Sel
    /** Several walls picked together, to change them all at once. */
    data class WallsSel(val ids: Set<Int>) : Sel
}

/** Grid step for snapping, mm. */
private const val GRID = 50.0

class EditorState {
    var tool by mutableStateOf(Tool.WALL)
    /**
     * The menu open below: Walls (everything built), Electrics, Interior, Backyard or Systems. Opening it picks only its
     * things ([Tool.SELECT] with [selectOnly] = this); a type from the row above adds new ones.
     */
    var category by mutableStateOf(Tool.WALL)
    /** ✋ Select in the row: picked things can be moved and their handles dragged; else a drag only moves the plan. */
    var moveMode by mutableStateOf(false)
    /** Picking only, in the open menu: nothing being added, nothing moved by a drag. */
    val pickOnly: Boolean get() = tool == Tool.SELECT && !moveMode
    /** Neutral plan browsing: no bottom tool is selected and one finger pans instead of changing the drawing. */
    var browse by mutableStateOf(false)
    /** The road tool can draw an open route or a closed lawn/paving/gravel area. */
    var roadArea by mutableStateOf(false)
    /** Vertices selected on the currently picked green platform; tap handles to add/remove them. */
    var yardPointIds by mutableStateOf<Set<Int>>(emptySet())
    /** With the Select tool: pick only things of this tool (roofs, walls, electrics…); null picks anything. */
    var selectOnly by mutableStateOf<Tool?>(Tool.WALL)
    /** Electrical subtypes Select is allowed to hit; long-pressing Select edits this filter. */
    var electricPickKinds by mutableStateOf(PointKind.entries.toSet())
    var electricPickCables by mutableStateOf(true)
    var electricPickDialog by mutableStateOf(false)
    /** What taps and drags may pick and change: the open tool's things, or with Select, [selectOnly]. */
    val scope: Tool? get() = if (browse) null else (if (tool == Tool.SELECT) selectOnly else tool)?.let { if (it == Tool.WINDOW || it == Tool.GATE) Tool.DOOR else it }
    /** A cable or pipe being drawn by finger (plan mm). */
    var stroke by mutableStateOf<List<P>?>(null)
    /** A short message to show once (a string resource). */
    var toast by mutableStateOf<Int?>(null)
    var toastText by mutableStateOf<String?>(null)
    /** The board that feeds the point just placed: it blinks for a moment. */
    var feeds by mutableStateOf<Int?>(null)
    /** Electric points are drawn in full only in the Electrics menu (or picking electrics); elsewhere as small dots. */
    val electricsShown: Boolean get() = scope == Tool.ELECTRIC
    /** Pipes are drawn in full only in the Systems menu (or picking systems). */
    val systemsShown: Boolean get() = scope == Tool.SYSTEMS
    /** The last tap (time, screen place): a second one soon after at the same place picks what lies under. */
    var lastTap: Pair<Long, Offset>? = null
    /** What was picked before the last tap: a double tap on a wall already picked puts a corner into it. */
    var selBeforeTap: Sel? = null
    /** Picking by a rectangle: a drag draws it and picks the corners (or pillars) inside. */
    var area by mutableStateOf(false)
    /** Picking by a rectangle takes the corners or pillars inside it on every floor (and the ground), not only this one. */
    var areaAllFloors by mutableStateOf(false)
    /** Taps add walls to those picked (or take them out), to change many at once. */
    var pickWalls by mutableStateOf(false)
    /** A side of the picked wall tapped on the plan (its L / R arrow): its stone is chosen next. */
    var facingSide by mutableStateOf<Facings.Side?>(null)
    /** The rectangle being drawn (plan mm). */
    var box by mutableStateOf<Pair<P, P>?>(null)
    /** Pick pillars by rectangle (else wall corners). */
    val areaPillars: Boolean get() = tool == Tool.PILLAR || (tool == Tool.SELECT && selectOnly == Tool.PILLAR)
    var sel by mutableStateOf<Sel?>(null)
    /** What the Electrics and Interior tools place on a tap. */
    var pointKind by mutableStateOf<PointKind?>(null)
    var itemKind by mutableStateOf<String?>(null)
    /** In Walls with nothing chosen yet: taps and drags pick and move, nothing is drawn. */
    var idle by mutableStateOf(false)
    /** The switch whose lamps are being picked; taps on lamps link or unlink them. */
    var linking by mutableStateOf<Int?>(null)
    /** Lamps waiting for their switch: the next tap on a switch (or a wall, for a new one) puts them on it. */
    var groupFor by mutableStateOf<Set<Int>?>(null)
    /** Drawing a cable: on while the Cable chip is chosen; [routeFrom] is the point it starts at, [routeVia] its turns. */
    var cableMode by mutableStateOf(false)
    var routeFrom by mutableStateOf<Int?>(null)
    var routeVia by mutableStateOf<List<P>>(emptyList())
    /** A selected cable is inspected first; its turns become draggable only after Edit (or a second tap). */
    var editRouteId by mutableStateOf<Int?>(null)
    /** Long-press a load, then tap its upstream device to rewire that load's incoming cable. */
    var sourcePickFrom by mutableStateOf<Int?>(null)
    var sourcePickTo by mutableStateOf<Int?>(null)
    var routeLevel by mutableStateOf(RouteLevel.CEILING)
    /** Cables being added or changed: the plan before, until the change is saved (✓) or cancelled (✕). */
    var cableBase by mutableStateOf<Plan?>(null)
    var cableFloor = 0
    var seenPlan: Plan? = null
    /** A point just placed whose details should be asked at once (an appliance). */
    var askFor by mutableStateOf<Int?>(null)
    /** A place just put down, whose name and kind should be asked at once. */
    var askPlace by mutableStateOf<String?>(null)
    /** Kind of the room last picked or named: furniture usual there is offered first. */
    var roomType by mutableStateOf<RoomType?>(null)
    /** In the Systems tab: which system is drawn (null: warm floor; "radiator" places a radiator). */
    var system by mutableStateOf<SystemKind?>(SystemKind.WATER_COLD)
    var systemMode by mutableStateOf("")
    /** Points of the pipe being drawn. */
    var pipePts by mutableStateOf<List<P>>(emptyList())
    var fenceKind by mutableStateOf(FenceKind.PICKET)
    /** The gate the Gate tool puts in a wall or fence. */
    var gateKind by mutableStateOf(GateKind.SLIDING)
    /** The library of 3D assets is open. */
    var assetsOpen by mutableStateOf(false)
    /** The road the Road tool draws: its surface and width, mm. */
    var surface by mutableStateOf(Surface.ASPHALT)
    var roadWidth by mutableStateOf(3500)
    /** The gate picked in the fence picked, or null. */
    var gateIndex by mutableStateOf<Int?>(null)
    /** A gate of this kind goes where the fence picked is tapped next (or the picked one moves there, [gateMoving]). */
    var gatePlacing by mutableStateOf<GateKind?>(null)
    var gateMoving by mutableStateOf(false)
    /** A yard construction to put where the plan is tapped next (a canopy, gazebo…), with the settings chosen for it. */
    var yardKind by mutableStateOf<YardKind?>(null)
    var yardSpec by mutableStateOf<YardSpec?>(null)
    /** The Interior tab shows the yard (outside things) instead of the rooms' furniture; null: by the level open. */
    var outdoor by mutableStateOf<Boolean?>(null)
    /** A yard construction whose settings are being chosen before it is put down. */
    var yardSetup by mutableStateOf<YardKind?>(null)
    /** A switch waiting for its two-way partner to be tapped. */
    var pairing by mutableStateOf<Int?>(null)
    /** First endpoint of a power/low-current cable crossing to another floor. */
    var crossFrom by mutableStateOf<Pair<Int, Int>?>(null)
    var crossPickFloor by mutableStateOf(false)
    var crossCable by mutableStateOf(CommCable.POWER)
    var crossLevel by mutableStateOf(CrossLevel.CEILING)
    /** Where the finger is while putting a gate: a mark shows the spot on the fence. */
    var gateAim by mutableStateOf<P?>(null)
    /** Height new platforms get: the last one set. */
    var platformHeight by mutableStateOf(0)
    /** Stairs that ran into walls: the plan they would make, the plan before, and the walls in the way. */
    var clash by mutableStateOf<Triple<Plan, Plan, List<Stairs.Clash>>?>(null)
    /** Screen pixels per millimetre, and where the plan's origin is on screen. */
    var scale by mutableFloatStateOf(0.1f)
    var ox by mutableFloatStateOf(0f)
    var oy by mutableFloatStateOf(0f)
    /** The wall or rectangle being drawn. */
    var preview by mutableStateOf<Pair<P, P>?>(null)
    var size by mutableStateOf(IntSize.Zero)
    var fitted = false

    fun toWorld(o: Offset) = P(((o.x - ox) / scale).toDouble(), ((o.y - oy) / scale).toDouble())
    fun toScreen(p: P) = Offset(p.x.toFloat() * scale + ox, p.y.toFloat() * scale + oy)

    /** Shows [poly] (plan mm) filling most of the canvas. */
    fun fitTo(poly: List<P>) {
        if (size.width == 0 || poly.isEmpty()) return
        val minX = poly.minOf { it.x }.toFloat(); val maxX = poly.maxOf { it.x }.toFloat()
        val minY = poly.minOf { it.y }.toFloat(); val maxY = poly.maxOf { it.y }.toFloat()
        scale = minOf(size.width * 0.7f / (maxX - minX).coerceAtLeast(500f), size.height * 0.7f / (maxY - minY).coerceAtLeast(500f))
        ox = size.width / 2f - (minX + maxX) / 2 * scale
        oy = size.height / 2f - (minY + maxY) / 2 * scale
    }

    fun fit(plan: Plan) {
        if (size.width == 0) return
        if (plan.nodes.isEmpty()) {
            scale = size.width / 7000f
            ox = size.width * 0.15f; oy = size.height * 0.15f
            return
        }
        val minX = plan.nodes.minOf { it.x }.toFloat(); val maxX = plan.nodes.maxOf { it.x }.toFloat()
        val minY = plan.nodes.minOf { it.y }.toFloat(); val maxY = plan.nodes.maxOf { it.y }.toFloat()
        scale = minOf(size.width * 0.8f / (maxX - minX).coerceAtLeast(1000f), size.height * 0.8f / (maxY - minY).coerceAtLeast(1000f))
        ox = size.width / 2f - (minX + maxX) / 2 * scale
        oy = size.height / 2f - (minY + maxY) / 2 * scale
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(vm: AppViewModel) {
    val plan = vm.plan ?: return
    val ed = remember(vm.root?.id) { EditorState().apply {
        // opens picking in the menu used last (nothing being added); an empty plan starts drawing walls
        val last = vm.editorTool()?.let { n -> Tool.entries.firstOrNull { it.name == n } }
        category = last?.takeIf { it != Tool.SELECT }?.let { categoryOf(it) } ?: Tool.WALL
        if (plan.walls.isEmpty() && category == Tool.WALL) tool = Tool.WALL
        else { tool = Tool.SELECT; selectOnly = category }
    } }
    LaunchedEffect(ed.tool, ed.category, ed.pointKind, ed.itemKind) { vm.saveEditorContext((if (ed.tool == Tool.SELECT) ed.category else ed.tool).name, ed.pointKind?.name, ed.itemKind) }
    LaunchedEffect(vm.itemToPlace) {
        vm.itemToPlace?.let { item ->
            ed.category = Tool.INTERIOR; ed.tool = Tool.INTERIOR; ed.outdoor = false
            ed.itemKind = item; ed.yardKind = null; ed.pointKind = null; ed.sel = null
            vm.itemToPlace = null
        }
    }
    // a type chosen in the row while picking: from now on taps add that (the menu's own tool, as the row's chips expect)
    LaunchedEffect(ed.pointKind, ed.cableMode, ed.itemKind, ed.yardKind, ed.systemMode, ed.roadArea) {
        if (ed.tool != Tool.SELECT) return@LaunchedEffect
        val adding = ed.pointKind != null || ed.cableMode || ed.itemKind != null || ed.yardKind != null || ed.systemMode.isNotEmpty() || (ed.category == Tool.YARD && ed.roadArea)
        if (adding && ed.category != Tool.WALL) { ed.tool = ed.category; ed.moveMode = false; ed.sel = null }
    }
    val rooms = remember(plan) { Rooms.detect(plan) }
    val exports = rememberExports(vm)
    // a 3D model arrived (from Idea 3D, or a file picked): put in the middle of the view, or replacing the one of its name
    val modelPlaced = stringResource(R.string.model_placed); val modelBad = stringResource(R.string.model_bad)
    LaunchedEffect(vm.pendingModel) {
        val (name, file) = vm.pendingModel ?: return@LaunchedEffect
        // cleared only when done: clearing it first would restart this effect and stop the reading half way
        val plan = vm.plan ?: return@LaunchedEffect
        val parts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            runCatching { Models.read(file.first, file.second) }.onFailure { android.util.Log.e("Roomik", "model ${file.first}", it) }.getOrNull() }
        vm.pendingModel = null
        if (parts == null) { ed.toastText = modelBad; return@LaunchedEffect }
        // back from Idea 3D: "#rk<level><m|i><id>" in its name says which model or piece of furniture it replaces
        val tag = Regex("""\s*#rk(-?\d+)([mi])(\d+)""").find(name)
        val clean = tag?.let { name.removeRange(it.range).trim() } ?: name
        val r = vm.root
        if (tag != null && r != null) {
            val f = tag.groupValues[1].toInt(); val id = tag.groupValues[3].toInt()
            val fl = r.floor(f)
            val back = when (tag.groupValues[2]) {
                "m" -> fl.models.firstOrNull { it.id == id }?.let { m -> PlanOps.updateModel(fl, m.copy(parts = parts, yard = null)) to id }
                else -> fl.items.firstOrNull { it.id == id }?.let { it2 ->
                    val nid = (fl.models.maxOfOrNull { it.id } ?: 0) + 1
                    fl.copy(items = fl.items.filter { it.id != id }, models = fl.models + Model3D(nid, clean, it2.x, it2.y, rot = it2.rot, parts = parts)) to nid
                }
            }
            if (back != null) {
                vm.saveAsset(clean, parts)
                vm.commitHouse(r.withFloor(f, back.first))
                if (vm.floor != f) vm.selectFloor(f)
                ed.tool = Tool.INTERIOR; ed.sel = Sel.ModelSel(back.second); ed.toastText = modelPlaced.format(clean)
                return@LaunchedEffect
            }
        }
        val plan0 = vm.plan ?: plan
        vm.saveAsset(clean, parts)
        // a model of that name on this floor ("⇅ Replace"): its parts replaced; else it waits in the library
        plan0.models.firstOrNull { it.name == clean }?.let { old ->
            vm.commit(PlanOps.updateModel(plan0, old.copy(parts = parts, yard = null)), plan0)
            ed.tool = Tool.INTERIOR; ed.sel = Sel.ModelSel(old.id); ed.toastText = modelPlaced.format(clean)
            return@LaunchedEffect
        }
        ed.assetsOpen = true
    }
    // "Send to elektronik": each board on its own (with all its loads), or the whole plan at once
    var sendPick by remember { mutableStateOf(false) }
    if (sendPick) vm.root?.let { root ->
        val boards = remember(root) { Boards.all(root) }
        val ru = language() == "ru"
        AlertDialog(
            onDismissRequest = { sendPick = false },
            title = { Text(stringResource(R.string.send_elektronik)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.el_in_elektronik), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (boards.isEmpty()) Text(stringResource(R.string.no_board), Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error)
                    for (b in boards) Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Boards.name(b, ru) + " · " + floorName(b.floor, root), Modifier.weight(1f))
                        FilledTonalButton(onClick = { sendPick = false; exports.sendBoard(b) }) { Text("⇢") }
                    }
                }
            },
            confirmButton = { if (boards.size > 1) TextButton(onClick = { sendPick = false; exports.sendToElektronik() }) { Text(stringResource(R.string.send_all_boards)) } },
            dismissButton = { TextButton(onClick = { sendPick = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    var editGround by remember { mutableStateOf(false) }
    var editFloorH by remember { mutableStateOf(false) }
    if (editFloorH) {
        var text by remember { mutableStateOf(cm(plan.wallHeight.toDouble())) }
        AlertDialog(
            onDismissRequest = { editFloorH = false },
            title = { Text(stringResource(R.string.floor_height, floorName(vm.floor, vm.root))) },
            text = {
                Column {
                    OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.height_cm)) }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                    Row(Modifier.padding(top = 8.dp).hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (h in listOf(250, 270, 300)) FilterChip(text == h.toString(), onClick = { text = h.toString() }, label = { Text("$h") })
                        FilterChip(text == "120", onClick = { text = "120" }, label = { Text(stringResource(R.string.attic_knee)) })
                    }
                    Text(stringResource(R.string.attic_about), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { editFloorH = false; text.replace(',', '.').toDoubleOrNull()?.let { vm.commit(plan.copy(wallHeight = (it * 10).roundToInt().coerceIn(500, 6000))) } }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { editFloorH = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    var deletingFloor by remember { mutableStateOf(false) }
    if (deletingFloor) AlertDialog(
        onDismissRequest = { deletingFloor = false },
        text = { Text(stringResource(R.string.delete_floor_q, floorName(vm.floor, vm.root))) },
        confirmButton = { TextButton(onClick = { deletingFloor = false; ed.sel = null; vm.deleteFloor() }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deletingFloor = false }) { Text(stringResource(R.string.cancel)) } },
    )
    if (editGround) vm.root?.let { root ->
        var text by remember { mutableStateOf(cm(root.groundLevel.toDouble())) }
        AlertDialog(
            onDismissRequest = { editGround = false },
            title = { Text(stringResource(R.string.ground)) },
            text = {
                Column {
                    Text(stringResource(R.string.ground_about), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.ground_cm)) }, singleLine = true, modifier = Modifier.padding(top = 8.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (g in listOf(0, 30, 60, 90, 120)) FilterChip(text == g.toString(), onClick = { text = g.toString() }, label = { Text("$g") })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editGround = false; text.replace(',', '.').toDoubleOrNull()?.let { vm.setGround((it * 10).roundToInt()) } }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { editGround = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    // Stairs that would go through a wall: offer to lower the wall (or take it away) instead.
    ed.clash?.let { (after, before, list) ->
        val lang0 = language()
        AlertDialog(
            onDismissRequest = { ed.clash = null; vm.preview(before) },
            title = { Text(stringResource(R.string.clash_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.clash_about), style = MaterialTheme.typography.bodyMedium)
                    for (c in list) Text(
                        "• " + stringResource(R.string.wall) + " " + cm(after.length(c.wall)) + " " + stringResource(R.string.cm) + ": " +
                            if (c.fitHeight >= 200) stringResource(R.string.clash_lower, cm(after.heightOf(c.wall).toDouble()), cm(c.fitHeight.toDouble()))
                            else stringResource(R.string.clash_remove),
                        Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    var p = after
                    for (c in list) p = if (c.fitHeight >= 200) PlanOps.setWallHeight(p, c.wall.id, c.fitHeight) else PlanOps.deleteWall(p, c.wall.id)
                    ed.clash = null
                    vm.commit(p, before)
                }) { Text(stringResource(R.string.clash_ok)) }
            },
            dismissButton = { TextButton(onClick = { ed.clash = null; vm.preview(before) }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    val toastContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(ed.toast) { ed.toast?.let { android.widget.Toast.makeText(toastContext, it, android.widget.Toast.LENGTH_SHORT).show(); ed.toast = null } }
    LaunchedEffect(ed.toastText) { ed.toastText?.let { android.widget.Toast.makeText(toastContext, it, android.widget.Toast.LENGTH_SHORT).show(); ed.toastText = null } }
    LaunchedEffect(ed.feeds) { if (ed.feeds != null) { kotlinx.coroutines.delay(2600); ed.feeds = null } }
    // A link from elektronik: show that room, selected.
    LaunchedEffect(vm.focusRoom, ed.size) {
        val id = vm.focusRoom ?: return@LaunchedEffect
        if (ed.size.width == 0) return@LaunchedEffect
        rooms.firstOrNull { it.info?.id == id }?.let { r ->
            ed.tool = Tool.SELECT; ed.category = Tool.WALL; ed.moveMode = false
            ed.selectOnly = Tool.WALL
            ed.sel = Sel.RoomSel(r.anchor)
            ed.fitTo(r.polygon)
            ed.fitted = true
        }
        vm.focusRoom = null
    }
    /** A bottom menu opened (or tapped again): picking only its things, nothing being added. */
    fun openCategory(cat: Tool) {
        finishDrawing(vm, ed)
        ed.browse = false
        ed.category = cat; ed.tool = Tool.SELECT; ed.selectOnly = cat; ed.moveMode = false; ed.idle = false
        ed.pointKind = null; ed.cableMode = false; ed.routeFrom = null; ed.routeVia = emptyList(); ed.linking = null
        ed.itemKind = null; ed.yardKind = null; ed.systemMode = ""; ed.roadArea = false; ed.area = false; vm.assetToPlace = null
        ed.sel = null; ed.lastTap = null
        if (cat == Tool.YARD) { vm.selectFloor(SITE); ed.outdoor = true }
        if (cat == Tool.INTERIOR) ed.outdoor = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ViewMenu(vm, ed) },
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    IconButton(onClick = { ed.sel = null; vm.undo() }, enabled = vm.canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo)) }
                    IconButton(onClick = { ed.sel = null; vm.redo() }, enabled = vm.canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.redo)) }
                    Box {
                        var menu by remember { mutableStateOf(false) }
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null) }
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.fit)) }, leadingIcon = { Icon(Icons.Default.FitScreen, null) }, onClick = { menu = false; ed.fit(plan) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.add_floor)) }, onClick = { menu = false; ed.sel = null; vm.addFloor() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.add_basement)) }, onClick = { menu = false; ed.sel = null; vm.addBasement() })
                            if (vm.floor != 0 && vm.floor != SITE) DropdownMenuItem(text = { Text(stringResource(R.string.delete_floor, floorName(vm.floor, vm.root))) }, onClick = { menu = false; ed.sel = null; vm.deleteFloor() })
                        }
                    }
                    FilledTonalButton(onClick = { finishDrawing(vm, ed); vm.screen = Screen.VIEW3D }, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.view_3d)) }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                @Composable fun tab(cat: Tool, icon: @Composable () -> Unit, label: String) =
                    NavigationBarItem(categoryOf(if (ed.tool == Tool.SELECT) ed.category else ed.tool) == cat, { openCategory(cat) }, icon,
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                tab(Tool.WALL, { Icon(Icons.Default.CropSquare, null) }, stringResource(R.string.tool_build))
                tab(Tool.INTERIOR, { Icon(Icons.Default.Chair, null) }, stringResource(R.string.tool_interior))
                tab(Tool.YARD, { Text("🌿") }, stringResource(R.string.backyard))
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                // a change to the cables in Electrics waits for ✓ (kept) or ✕ (put back as it was)
                LaunchedEffect(plan, vm.floor, ed.tool) {
                    val prev = ed.seenPlan
                    ed.seenPlan = plan
                    if (ed.tool != Tool.ELECTRIC || ed.cableFloor != vm.floor) ed.cableBase = null
                    if (ed.tool == Tool.ELECTRIC && ed.cableBase == null && prev != null && prev !== plan && prev.id == plan.id && ed.cableFloor == vm.floor &&
                        prev.routes != plan.routes) ed.cableBase = prev
                    ed.cableFloor = vm.floor
                    // undone back to how it was: nothing to save
                    if (ed.cableBase?.routes == plan.routes) ed.cableBase = null
                }
                PlanCanvas(vm, ed, plan, rooms)
                // the compass: where north and south are; tapped, south is set
                Compass(vm, Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 10.dp))
                if (ed.tool == Tool.ELECTRIC && (ed.cableBase != null || ed.routeFrom != null)) Row(Modifier.align(Alignment.BottomCenter).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // ✕: the cables as they were before this change; ✓: kept, and the next cable can be drawn
                    FilledTonalButton(onClick = {
                        // only the cables go back: points placed meanwhile stay (boxes cables made go, those removed come back)
                        ed.cableBase?.let { base -> vm.commit(plan.copy(routes = base.routes,
                            points = plan.points.filter { p -> p.kind != PointKind.JUNCTION || base.points.any { it.id == p.id } } +
                                base.points.filter { b -> b.kind == PointKind.JUNCTION && plan.points.none { it.id == b.id } })) }
                        // the undoing itself is no new cable change
                        ed.seenPlan = null
                        ed.cableBase = null; ed.routeFrom = null; ed.routeVia = emptyList(); ed.sel = null
                    }) { Icon(Icons.Default.Close, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.cancel)) }
                    Button(onClick = {
                        ed.cableBase = null; ed.routeFrom = null; ed.routeVia = emptyList(); ed.sel = null
                        ed.toast = R.string.cable_saved
                    }) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.save)) }
                }
                val clashes = remember(plan) { Stairs.allClashes(plan) }
                if (clashes.isNotEmpty()) Surface(Modifier.align(Alignment.BottomCenter).padding(8.dp), shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(stringResource(R.string.clash_banner, clashes.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        // the user confirms: the walls get cut where the stairs pass; moving the stairs makes them whole again
                        FilledTonalButton(onClick = {
                            var p = plan
                            for (st in plan.stairs) if (Stairs.clashes(p, st).isNotEmpty()) p = PlanOps.cropWalls(p, st.id)
                            vm.commit(p)
                        }, modifier = Modifier.padding(top = 6.dp)) { Text(stringResource(R.string.crop_walls)) }
                    }
                }
                val root = vm.root
                // Floors: add one above or below, switch between them; the ground level of the house. Solid chips, readable over the plan.
                if (root != null) Row(Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 60.dp).hScroll(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlanChip("+ " + stringResource(R.string.floor_above)) { ed.sel = null; vm.addFloor() }
                    for (i in root.floorRange.reversed()) {
                        FloorChip(floorName(i, vm.root), vm.floor == i,
                            // the floor shown can be deleted with its ✕ (not floor 1: the house stands on it)
                            onDelete = if (vm.floor == i && i != 0) ({ deletingFloor = true }) else null) { ed.sel = null; ed.routeFrom = null; vm.selectFloor(i) }
                        // the ground round the house: under the floor standing on it, over the ones dug in
                        if (i == root.groundFloor) FloorChip(floorName(SITE, vm.root), vm.floor == SITE, null) { ed.sel = null; ed.routeFrom = null; vm.selectFloor(SITE) }
                    }
                    PlanChip("+ " + stringResource(R.string.floor_below)) { ed.sel = null; vm.addBasement() }
                    if (vm.floor != SITE) PlanChip("↕ " + cm(plan.wallHeight.toDouble())) { editFloorH = true }
                    // a floor under the first: on the ground (garage, stores) or dug into it
                    if (vm.floor < 0) PlanChip(stringResource(if (plan.onGround) R.string.on_ground else R.string.underground)) { vm.commit(plan.copy(onGround = !plan.onGround)) }
                    if (vm.floor == root.groundFloor) PlanChip(stringResource(R.string.ground_chip, cm(root.groundLevel.toDouble()))) { editGround = true }
                }
                // What the tool does: shown when the ? in the corner is tapped.
                val hint = stringResource(when {
                    ed.pickOnly -> R.string.hint_pick
                    (ed.tool == Tool.SYSTEMS && ed.systemMode == "") || (ed.tool == Tool.ELECTRIC && !ed.cableMode && ed.pointKind == null) ||
                        (ed.tool == Tool.INTERIOR && ed.itemKind == null) || (ed.tool == Tool.WALL && ed.idle) -> R.string.hint_choose
                    ed.tool == Tool.SYSTEMS -> when (ed.systemMode) { "warm" -> R.string.hint_warm; "radiator" -> R.string.hint_radiator; else -> R.string.hint_pipe_1 }
                    ed.tool == Tool.ELECTRIC -> if (ed.cableMode) R.string.hint_cable_1 else R.string.hint_electric
                    else -> when (ed.tool) { Tool.WALL -> R.string.hint_wall; Tool.PILLAR -> R.string.hint_pillar; Tool.ROOM -> R.string.hint_room; Tool.DOOR, Tool.WINDOW -> R.string.hint_opening; Tool.PLACE -> R.string.hint_place; Tool.STAIRS -> R.string.hint_stairs; Tool.ROOF -> R.string.hint_roof; Tool.PLATFORM -> R.string.hint_platform; Tool.FENCE -> R.string.hint_fence; Tool.GATE -> R.string.hint_gate; Tool.ROAD -> R.string.hint_road; Tool.INTERIOR -> R.string.hint_interior; else -> R.string.hint_select }
                }) + when {
                    ed.tool == Tool.ELECTRIC -> "\n" + stringResource(R.string.el_counts, plan.points.filter { it.kind == PointKind.SOCKET }.sumOf { it.count },
                        plan.points.count { it.kind == PointKind.SWITCH }, plan.points.count { it.kind == PointKind.LAMP })
                    else -> ""
                }
                var help by remember { mutableStateOf(false) }
                Surface(onClick = { help = true }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(44.dp), shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Box(contentAlignment = Alignment.Center) { Text("?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                }
                if (help) AlertDialog(
                    onDismissRequest = { help = false },
                    title = { Text(stringResource(R.string.help)) },
                    text = { Column {
                        Text(hint, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.help_general), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } },
                    confirmButton = { TextButton(onClick = { help = false }) { Text(stringResource(R.string.ok)) } },
                )
            }
            SelectionPanel(vm, ed, plan, rooms)
        }
    }
}

@Composable
private fun PlanCanvas(vm: AppViewModel, ed: EditorState, plan: Plan, rooms: List<Room>) {
    val shapes = remember(plan) { WallShapes.build(plan) }
    // another floor drawn faintly underneath (by default the next one towards the ground; View chooses)
    val below = underFloor(vm)?.let { vm.root?.floor(it) }
    val belowShapes = remember(below) { below?.let { WallShapes.build(it) } ?: emptyList() }
    // stairs on the floor under this one that come up here
    val arriving = vm.root?.let { r -> (vm.floor - 1).takeIf { it in r.floorRange }?.let { r.floor(it) } }
        ?.let { lower -> lower to lower.stairs.filter { !it.fromGround && Stairs.rise(lower, it) > lower.wallHeight } }?.takeIf { it.second.isNotEmpty() }
    val fromBelow = stringResource(R.string.stairs_from_below, floorName(vm.floor - 1, vm.root))
    val placed = remember(plan) { Interior.place(plan) }
    // the pillars' grid is the same on every floor: other floors' pillars make lines here too
    val otherPillars = remember(vm.root, vm.floor) {
        vm.root?.let { r -> r.floorRange.filter { it != vm.floor }.flatMap { r.floor(it).pillars } }.orEmpty()
    }
    val axes = remember(plan.pillars, otherPillars) { Pillars.axes(plan.pillars + otherPillars) }
    // the floor below's pillars, and ours that do not stand on one of them (they blink red)
    val belowPillars = remember(vm.root, vm.floor) {
        vm.root?.let { r -> (vm.floor - 1).takeIf { vm.floor != SITE && it in r.floorRange }?.let { r.floor(it).pillars } }.orEmpty()
    }
    val offPillars = remember(plan.pillars, belowPillars) { Pillars.offBelow(plan.pillars, belowPillars) }
    val feeds = remember(vm.root) { vm.root?.let { Feeds.all(it) }.orEmpty() }
    val crossCables = remember(vm.root) { vm.root?.crossCables.orEmpty() }
    // A vertical riser is one column through the house.  Keep its mark visible even on a level where its endpoint
    // cannot sit on a wall (an open garage, for example).
    val riserColumns = remember(vm.root) { vm.root?.let { root ->
        root.floorRange.flatMap { f -> root.floor(f).points.filter { it.kind == PointKind.RISER }.map { p -> P(p.x.toDouble(), p.y.toDouble()) } }
            .groupBy { (it.x.roundToInt() / 50) to (it.y.roundToInt() / 50) }
            .map { (_, ps) -> ps.first() to ps.size }
    }.orEmpty() }
    val gridShown = vm.showPillarGrid || ed.scope == Tool.WALL || ed.scope == Tool.PILLAR || ed.tool == Tool.ROOM
    val cuts = remember(plan) { Interior.openings(plan) }
    val clashing = remember(plan) { Stairs.allClashes(plan) }
    val crops = remember(plan) { Stairs.crops(plan) }
    val blink by androidx.compose.animation.core.rememberInfiniteTransition(label = "clash").animateFloat(
        0.25f, 1f, androidx.compose.animation.core.infiniteRepeatable<Float>(androidx.compose.animation.core.tween<Float>(450), androidx.compose.animation.core.RepeatMode.Reverse), label = "clash",
    )
    val kw = " " + stringResource(R.string.kw)
    // each model's top view, made once: paths in its own mm with their shaded colours
    val modelTops = remember(plan.models) { plan.models.associate { m -> m.id to topShapes(m) } }
    val placeWord = stringResource(R.string.place)
    val yardNames = YardKind.entries.associateWith { yardName(it) }
    val itemNames = Catalog.all.associate { it.id to itemName(it.id) }
    val roomAreaUnit = stringResource(R.string.m2)
    val tm = rememberTextMeasurer()
    val lang = language()
    val accent = MaterialTheme.colorScheme.primary
    val ruBoards = lang == "ru"
    val feedsText = stringResource(R.string.fed_by)
    val groupedText = stringResource(R.string.group_done)

    LaunchedEffect(ed.size) { if (!ed.fitted && ed.size.width > 0) { ed.fit(plan); ed.fitted = true } }

    Canvas(
        Modifier.fillMaxSize().onSizeChanged { ed.size = it }.pointerInput(ed) {
            val nodeR = 16.dp.toPx()
            val wallR = 12.dp.toPx()
            val alignR = 8.dp.toPx()

            /** Snaps a plan point: to a corner, else onto a wall, else to the grid. True when it landed on a corner. */
            fun snap(p: Plan, w: P, exclude: Int? = null, pillar: Boolean = false): Pair<P, Boolean> {
                val rn = nodeR / ed.scale
                // the other floors' pillars as they are now (this gesture code outlives floor changes)
                val r0 = vm.root; val fl = vm.floor
                val otherPillars = r0?.let { r -> r.floorRange.filter { it != fl }.flatMap { r.floor(it).pillars } }.orEmpty()
                val belowPillars = r0?.let { r -> (fl - 1).takeIf { fl != SITE && it in r.floorRange }?.let { r.floor(it).pillars } }.orEmpty()
                // a pillar placed or moved: right over a pillar of the floor below when near one
                if (pillar || ed.tool == Tool.PILLAR) belowPillars.minByOrNull { (P(it.x.toDouble(), it.y.toDouble()) - w).len }
                    ?.let { P(it.x.toDouble(), it.y.toDouble()) }?.takeIf { (it - w).len <= rn * 1.6 }?.let { return it to true }
                p.nodes.filter { it.id != exclude }.map { P(it.x.toDouble(), it.y.toDouble()) }
                    .minByOrNull { (it - w).len }?.takeIf { (it - w).len <= rn }?.let { return it to true }
                val rw = wallR / ed.scale
                // drawing walls or placing pillars: the pillars' grid lines and their crossings pull the point
                if ((pillar || ed.tool == Tool.WALL || ed.tool == Tool.PILLAR || ed.tool == Tool.ROOM || ed.scope == Tool.WALL || ed.scope == Tool.PILLAR) &&
                    (p.pillars.isNotEmpty() || otherPillars.isNotEmpty())) {
                    val (xs, ys) = Pillars.axes(p.pillars + otherPillars)
                    val nx = xs.minByOrNull { abs(it - w.x) }?.takeIf { abs(it - w.x) <= rn }
                    val ny = ys.minByOrNull { abs(it - w.y) }?.takeIf { abs(it - w.y) <= rn }
                    if (nx != null && ny != null) return P(nx.toDouble(), ny.toDouble()) to true
                    if (nx != null) return P(nx.toDouble(), (w.y / GRID).roundToInt() * GRID) to false
                    if (ny != null) return P((w.x / GRID).roundToInt() * GRID, ny.toDouble()) to false
                }
                var best: P? = null
                for (wall in p.walls) {
                    if (wall.a == exclude || wall.b == exclude) continue
                    val a = p.pos(wall.a) ?: continue
                    val b = p.pos(wall.b) ?: continue
                    val len = (b - a).len
                    if (len < 1) continue
                    val (q, t) = Geo.project(w, a, b)
                    if ((q - w).len > rw) continue
                    val along = ((t * len / GRID).roundToInt() * GRID).coerceIn(0.0, len)
                    val s = a + (b - a) * (along / len)
                    if (best == null || (s - w).len < (best - w).len) best = s
                }
                best?.let { return it to false }
                return P((w.x / GRID).roundToInt() * GRID, (w.y / GRID).roundToInt() * GRID) to false
            }

            /** Pulls [q] onto the same x or y as any of [others] when it is nearly there, so walls come out straight. */
            fun align(q: P, others: List<P>): P {
                val r = alignR / ed.scale
                var x = q.x; var y = q.y
                others.minByOrNull { abs(it.x - q.x) }?.takeIf { abs(it.x - q.x) <= r }?.let { x = it.x }
                others.minByOrNull { abs(it.y - q.y) }?.takeIf { abs(it.y - q.y) <= r }?.let { y = it.y }
                return P(x, y)
            }

            /**
             * Everything at [w], most likely first. Only what the open menu is about ([EditorState.scope]: roofs with Roof,
             * cables and points with Electrics…), or with [deep] anything, roofs and platforms also by their whole area.
             */
            fun hits(p: Plan, w: P, deep: Boolean = false): List<Sel> {
                val sc = if (deep) null else ed.scope
                // a menu open to pick in: all of its kinds of thing (Walls: walls, doors, roofs, fences, roads…)
                val inCat = !deep && ed.tool == Tool.SELECT && ed.selectOnly == ed.category
                fun ok(t: Tool) = sc == null || sc == t || (inCat && categoryOf(t) == ed.category)
                val out = ArrayList<Sel>()
                if (ok(Tool.WALL)) p.nodes.minByOrNull { (P(it.x.toDouble(), it.y.toDouble()) - w).len }
                    ?.takeIf { (P(it.x.toDouble(), it.y.toDouble()) - w).len <= nodeR / ed.scale }?.let { out += Sel.Corner(it.id) }
                if (ok(Tool.ELECTRIC) && Layer.ELECTRIC !in vm.hidden && (sc == Tool.ELECTRIC || vm.alwaysElectric)) Interior.place(p).filter { it.point.kind in ed.electricPickKinds }.minByOrNull { (symbolAt(it) - w).len }
                    ?.takeIf { (symbolAt(it) - w).len <= nodeR / ed.scale }?.let { out += Sel.PointSel(it.point.id) }
                if (ok(Tool.DOOR)) for ((wid, list) in Interior.openings(p)) {
                    val wall = p.wall(wid) ?: continue
                    val a = p.pos(wall.a) ?: continue
                    val d = ((p.pos(wall.b) ?: continue) - a).norm()
                    val t = dot(w - a, d)
                    val off = abs(dot(w - a, d.left()))
                    list.firstOrNull { t >= it.t0 && t <= it.t1 && off <= maxOf(wall.thickness / 2.0, (wallR / ed.scale).toDouble()) }?.let { out += Sel.OpeningSel(it.opening.id) }
                }
                if (ok(Tool.FENCE) && Layer.FENCES !in vm.hidden) p.fences.firstOrNull { f ->
                    val pts = f.points.map { P(it.x.toDouble(), it.y.toDouble()) }
                    (1 until pts.size).any { k -> Geo.distToSegment(w, pts[k - 1], pts[k]) <= wallR / ed.scale }
                }?.let { out += Sel.FenceSel(it.id) }
                // In Backyard, accessories (trees, grill…) sit above the ground area and get the first tap.
                val yardAccessoryAtTap = sc == Tool.YARD && p.models.any { it.yard != null && Models.covers(it, w) }
                if ((ok(Tool.ROAD) || sc == Tool.YARD) && !yardAccessoryAtTap) p.roads.lastOrNull { r ->
                    val pts = r.points.map { P(it.x.toDouble(), it.y.toDouble()) }
                    (r.closed && Geo.inside(w, pts)) || (1 until pts.size).any { k -> Geo.distToSegment(w, pts[k - 1], pts[k]) <= maxOf(r.width / 2.0, (wallR / ed.scale).toDouble()) }
                }?.let { out += Sel.RoadSel(it.id) }
                if (ok(Tool.ROOF) && Layer.ROOF !in vm.hidden && !(deep && (ed.electricsShown || ed.systemsShown))) p.roofs.firstOrNull { rf ->
                    roofLines(rf).any { (a, b) -> Geo.distToSegment(w, a, b) <= wallR / ed.scale } || ((sc == Tool.ROOF || deep) && Roofs.covers(rf, w))
                }?.let { out += Sel.RoofSel(it.id) }
                if (sc == Tool.PLATFORM || deep && !ed.electricsShown && !ed.systemsShown) p.platforms.lastOrNull { Platforms.covers(it, w) }?.let { out += Sel.PlatformSel(it.id) }
                if (ok(Tool.SYSTEMS) && (sc == Tool.SYSTEMS || vm.alwaysPipes)) p.pipes.firstOrNull { pipe ->
                    val pts = pipe.points.map { P(it.x.toDouble(), it.y.toDouble()) }
                    pipe.system.layer !in vm.hidden && (1 until pts.size).any { k -> Geo.distToSegment(w, pts[k - 1], pts[k]) <= wallR / ed.scale }
                }?.let { out += Sel.PipeSel(it.id) }
                if (ed.electricPickCables && ok(Tool.ELECTRIC) && Layer.ELECTRIC !in vm.hidden && (sc == Tool.ELECTRIC || vm.alwaysElectric)) p.routes.firstOrNull { r ->
                    val pts = routePlan(p, r) ?: return@firstOrNull false
                    (1 until pts.size).any { k -> Geo.distToSegment(w, pts[k - 1], pts[k]) <= wallR / ed.scale }
                }?.let { out += Sel.RouteSel(it.id) }
                // a feed cable between boards, on the floor it runs across
                if (ok(Tool.ELECTRIC) && Layer.ELECTRIC !in vm.hidden && (sc == Tool.ELECTRIC || vm.alwaysElectric)) vm.root?.let { r ->
                    Feeds.all(r).firstOrNull { f -> f.from.floor == vm.floor && (1 until f.path.size).any { k -> Geo.distToSegment(w, f.path[k - 1], f.path[k]) <= wallR / ed.scale } }
                }?.let { out += Sel.FeedSel(it.to.floor, it.to.point.id) }
                if (ok(Tool.ELECTRIC) && Layer.ELECTRIC !in vm.hidden && (sc == Tool.ELECTRIC || vm.alwaysElectric)) vm.root?.let { r ->
                    r.crossCables.firstOrNull { c -> c.fromFloor == vm.floor && CrossCables.path(r, c).zipWithNext().any { (a, b) -> Geo.distToSegment(w, a, b) <= wallR / ed.scale } }
                }?.let { out += Sel.CrossCableSel(it.id) }
                if (ok(Tool.STAIRS)) p.stairs.lastOrNull { Stairs.hit(p, it, w) }?.let { out += Sel.StairSel(it.id) }
                if ((ok(Tool.INTERIOR) || sc == Tool.YARD) && Layer.FURNITURE !in vm.hidden) p.models.lastOrNull {
                    Models.covers(it, w) && (sc != Tool.YARD || it.yard != null)
                }?.let { out += Sel.ModelSel(it.id) }
                if (ok(Tool.INTERIOR) && Layer.FURNITURE !in vm.hidden) p.items.lastOrNull { Geo.inside(w, Interior.corners(it)) && (sc == Tool.INTERIOR || itemShown(vm, ed, it)) }?.let { out += Sel.ItemSel(it.id) }
                if (ok(Tool.PILLAR) || sc == Tool.WALL) p.pillars.lastOrNull { Pillars.covers(it, w, (wallR / ed.scale).toDouble() * 0.5) }?.let { out += Sel.PillarSel(it.id) }
                if (ok(Tool.WALL)) p.walls.map { it to Geo.distToSegment(w, p.pos(it.a) ?: w, p.pos(it.b) ?: w) }
                    .filter { (wall, d) -> d <= maxOf(wall.thickness / 2.0, (wallR / ed.scale).toDouble()) }
                    .minByOrNull { it.second }?.let { out += Sel.WallSel(it.first.id) }
                if (ok(Tool.PLACE)) p.places.firstOrNull { (P(it.x.toDouble(), it.y.toDouble()) - w).len <= nodeR * 1.5 / ed.scale }?.let { out += Sel.PlaceSel(it.id) }
                if (ok(Tool.PLATFORM)) p.platforms.lastOrNull { Platforms.covers(it, w) }?.let { out += Sel.PlatformSel(it.id) }
                if (ok(Tool.ROOM)) Rooms.detect(p).firstOrNull { Geo.inside(w, it.polygon) }?.let { out += Sel.RoomSel(it.anchor) }
                return out.distinct()
            }
            fun hit(p: Plan, w: P): Sel? = hits(p, w).firstOrNull()

            /** The electrical point whose symbol is near [w], for the ends of a drawn cable. */
            /** The drawn cable at [w], for joining onto it. */
            fun routeNear(p: Plan, w: P): Int? = p.routes.firstOrNull { r ->
                val pts = Cables.plan(p, r) ?: return@firstOrNull false
                (1 until pts.size).any { k -> Geo.distToSegment(w, pts[k - 1], pts[k]) <= wallR * 1.3 / ed.scale }
            }?.id

            fun pointNear(p: Plan, w: P): Int? = Interior.place(p).minByOrNull { (symbolAt(it) - w).len }
                ?.takeIf { (symbolAt(it) - w).len <= nodeR * 2.2 / ed.scale }?.point?.id

            /** The turns of a new cable from point [from] through [via] to [to], its runs laid onto the cables already near. */
            fun followVia(p: Plan, from: Int, via: List<P>, to: Int): List<P> {
                val a = p.points.firstOrNull { it.id == from } ?: return via
                val b = p.points.firstOrNull { it.id == to } ?: return via
                val all = listOf(P(a.x.toDouble(), a.y.toDouble())) + via + P(b.x.toDouble(), b.y.toDouble())
                val tol = (16.dp.toPx() / ed.scale).toDouble().coerceAtLeast(120.0)
                return Lines.follow(Lines.straighten(all), p.routes.mapNotNull { routePlan(p, it) }, tol).drop(1).dropLast(1)
            }

            /** A finger-drawn cable or pipe, made straight with right-angle turns. */
            fun finishStroke(before: Plan, stroke: List<P>) {
                val tol = (16.dp.toPx() / ed.scale).toDouble().coerceAtLeast(80.0)
                if (ed.tool == Tool.SYSTEMS) {
                    val first = ed.pipePts.lastOrNull() ?: snap(before, stroke.first()).first
                    val pts = Lines.orthogonal(listOf(first) + stroke.drop(1), tol)
                    if (pts.size < 2 || (pts.last() - pts.first()).len < tol) return
                    val all = Lines.straighten(ed.pipePts.dropLast(1) + pts)
                    ed.pipePts = emptyList()
                    val after = PlanOps.addPipe(before, ed.system ?: SystemKind.WATER_COLD, all)
                    if (after !== before) { vm.commit(after, before); ed.sel = Sel.PipeSel(after.pipes.last().id) }
                    return
                }
                // started or ended on a cable: a junction box joins them there
                var base = before
                fun at(id: Int) = base.points.first { it.id == id }.let { P(it.x.toDouble(), it.y.toDouble()) }
                var startPoint = pointNear(base, stroke.first())
                if (startPoint == null && ed.routeFrom == null) routeNear(base, stroke.first())?.let { rid ->
                    Circuits.split(base, rid, stroke.first())?.let { (p2, box) -> base = p2; startPoint = box }
                }
                val from = startPoint ?: ed.routeFrom
                if (from == null) { ed.toast = R.string.cable_start_at_point; return }
                // started away from a point: it goes on from the cable already begun
                val prefix = if (startPoint == null) ed.routeVia else emptyList()
                val start = if (startPoint == null) ed.routeVia.lastOrNull() ?: at(from) else at(from)
                var to = pointNear(base, stroke.last())?.takeIf { it != from }
                if (to == null) routeNear(base, stroke.last())?.let { rid ->
                    val r = base.routes.first { it.id == rid }
                    if (Circuits.mixes(base, from, r.from)) { ed.toast = R.string.mixed_circuits; return }
                    Circuits.split(base, rid, stroke.last())?.let { (p2, box) -> if (box != from) { base = p2; to = box } }
                }
                val end = to?.let { at(it) } ?: stroke.last()
                val path = Lines.orthogonal(listOf(start) + stroke.drop(1).dropLast(1) + end, tol)
                val target = to
                if (target != null) {
                    if (Circuits.mixes(base, from, target)) { ed.toast = R.string.mixed_circuits; return }
                    val via = followVia(base, from, Lines.straighten(listOf(at(from)) + prefix + path.drop(1)).drop(1).dropLast(1), target)
                    val after = PlanOps.addRoute(base, from, target, via, ed.routeLevel)
                    vm.commit(after, before)
                    ed.routeFrom = null; ed.routeVia = emptyList()
                    ed.sel = Sel.RouteSel(after.routes.last().id)
                } else {
                    if (base !== before) vm.commit(base, before)
                    ed.routeFrom = from; ed.routeVia = prefix + path.drop(1); ed.sel = null
                }
            }

            awaitEachGesture {
                val down = awaitFirstDown()
                val pressedAt = System.currentTimeMillis()
                val start = down.position
                val before = vm.plan ?: return@awaitEachGesture
                var moved = false
                var multi = false
                var last = start
                var target: Sel? = null       // what a select-drag moves; null pans
                var houseBefore: Plan? = null  // the whole house when a drag changes another floor (a feed cable)
                var startW = ed.toWorld(start)
                var changed = false
                var roofGrab: Int? = null     // a selected roof's handle being dragged (8 = the whole roof)
                // drawing cables or pipes: one finger draws, two move the plan, nothing else is moved
                val drawLines = (ed.tool == Tool.ELECTRIC && ed.cableMode) || (ed.tool == Tool.SYSTEMS && ed.systemMode == "pipe")

                while (true) {
                    val ev = awaitPointerEvent()
                    val pressed = ev.changes.filter { it.pressed }
                    if (pressed.isEmpty()) break
                    if (pressed.size >= 2) {
                        if (!multi) {
                            multi = true
                            ed.preview = null
                            ed.stroke = null
                            if (changed) vm.preview(before)
                        }
                        val zoom = ev.calculateZoom()
                        val pan = ev.calculatePan()
                        val c = ev.calculateCentroid(useCurrent = false)
                        val ns = (ed.scale * zoom).coerceIn(0.005f, 3f)
                        val k = ns / ed.scale
                        ed.ox = c.x - (c.x - ed.ox) * k + pan.x
                        ed.oy = c.y - (c.y - ed.oy) * k + pan.y
                        ed.scale = ns
                    } else if (!multi && (ed.gatePlacing != null || ed.gateMoving)) {
                        // putting a gate: the finger may slide along the fence, the gate goes where it is lifted
                        val pos = pressed[0].position
                        if ((pos - start).getDistance() > viewConfiguration.touchSlop) moved = true
                        last = pos; ed.gateAim = ed.toWorld(pos)
                    } else if (!multi) {
                        val pos = pressed[0].position
                        // Neutral browse mode, and picking only: a single finger moves the plan and changes nothing.
                        if (ed.browse || ed.pickOnly) {
                            val delta = pos - last
                            // a little finger jitter is still a tap (that picks)
                            if (!moved && (pos - start).getDistance() > viewConfiguration.touchSlop) moved = true
                            if (moved && delta.getDistance() > 0f) { ed.ox += delta.x; ed.oy += delta.y }
                            last = pos
                            continue
                        }
                        if (!moved && (pos - start).getDistance() > viewConfiguration.touchSlop) {
                            moved = true
                            // a selected roof: its corner and side handles resize it, inside moves it
                            (ed.sel as? Sel.RoofSel)?.let { rs -> before.roofs.firstOrNull { it.id == rs.id } }?.let { rf ->
                                val w0 = ed.toWorld(start)
                                val near = Roofs.handles(rf).withIndex().minByOrNull { (it.value - w0).len }
                                roofGrab = when {
                                    near != null && (near.value - w0).len <= nodeR * 1.6 / ed.scale -> near.index
                                    Roofs.covers(rf, w0) -> 8
                                    else -> null
                                }
                                if (roofGrab != null) target = Sel.RoofSel(rf.id)
                            }
                            // a selected platform: the same handles
                            if (roofGrab == null) (ed.sel as? Sel.PlatformSel)?.let { ps -> before.platforms.firstOrNull { it.id == ps.id } }?.let { pf ->
                                val w0 = ed.toWorld(start)
                                val near = Roofs.handlesRect(pf.x1, pf.y1, pf.x2, pf.y2).withIndex().minByOrNull { (it.value - w0).len }
                                roofGrab = when {
                                    near != null && (near.value - w0).len <= nodeR * 1.6 / ed.scale -> near.index
                                    Platforms.covers(pf, w0) -> 8
                                    else -> null
                                }
                                if (roofGrab != null) target = Sel.PlatformSel(pf.id)
                            }
                            // a selected fence: drag a turn, a side's middle to add a turn, or the line to move it all
                            if (roofGrab == null) (ed.sel as? Sel.FenceSel)?.let { fs -> before.fences.firstOrNull { it.id == fs.id } }?.let { f ->
                                val w0 = ed.toWorld(start)
                                val near = Fences.handles(f).withIndex().minByOrNull { (it.value - w0).len }
                                val pts = f.points.map { P(it.x.toDouble(), it.y.toDouble()) }
                                roofGrab = when {
                                    near != null && (near.value - w0).len <= nodeR * 1.6 / ed.scale -> near.index
                                    (1 until pts.size).any { Geo.distToSegment(w0, pts[it - 1], pts[it]) <= wallR / ed.scale } -> -1
                                    else -> null
                                }
                                if (roofGrab != null) target = Sel.FenceSel(f.id)
                            }
                            // a selected road: like a fence, drag a turn, a side's middle to add a turn, or the road to move it all
                            if (roofGrab == null) (ed.sel as? Sel.RoadSel)?.let { rs -> before.roads.firstOrNull { it.id == rs.id } }?.let { r ->
                                val w0 = ed.toWorld(start)
                                val near = Fences.handles(roadLine(r)).withIndex().minByOrNull { (it.value - w0).len }
                                val pts = r.points.map { P(it.x.toDouble(), it.y.toDouble()) }
                                roofGrab = when {
                                    near != null && (near.value - w0).len <= nodeR * 1.6 / ed.scale -> near.index
                                    (1 until pts.size).any { Geo.distToSegment(w0, pts[it - 1], pts[it]) <= maxOf(r.width / 2.0, (wallR / ed.scale).toDouble()) } -> -1
                                    else -> null
                                }
                                if (roofGrab != null) target = Sel.RoadSel(r.id)
                            }
                            // a selected feed cable: drag a turn or a run's middle, like a drawn cable
                            if (roofGrab == null) (ed.sel as? Sel.FeedSel)?.let { fs -> vm.root?.let { Feeds.of(it, fs.toFloor, fs.toId) } }?.let { f ->
                                val w0 = ed.toWorld(start)
                                val near = Lines.handles(f.path, true).withIndex().minByOrNull { (it.value - w0).len }
                                if (near != null && (near.value - w0).len <= nodeR * 1.6 / ed.scale) { roofGrab = near.index; target = ed.sel; houseBefore = vm.root }
                            }
                            // a selected cable or pipe: drag a turn, or the middle of a run to move the run sideways
                            if (roofGrab == null) {
                                val line = when (val sl = ed.sel) {
                                    is Sel.RouteSel -> before.routes.firstOrNull { it.id == sl.id }?.let { r -> routePlan(before, r)?.let { it to true } }
                                    is Sel.PipeSel -> before.pipes.firstOrNull { it.id == sl.id }?.let { pp -> pp.points.map { P(it.x.toDouble(), it.y.toDouble()) } to false }
                                    else -> null
                                }
                                // drawing cables: a stroke from a device always starts a new cable
                                if (line != null && !(drawLines && pointNear(before, ed.toWorld(start)) != null)) {
                                    val w0 = ed.toWorld(start)
                                    val near = Lines.handles(line.first, line.second).withIndex().minByOrNull { (it.value - w0).len }
                                    if (near != null && (near.value - w0).len <= nodeR * 1.6 / ed.scale) { roofGrab = near.index; target = ed.sel }
                                }
                            }
                            // a drag on the group picked moves it all; with Area on, or in Select or Pillar on empty space,
                            // it draws the picking rectangle (two fingers move the plan)
                            val w0 = ed.toWorld(start)
                            val g = ed.sel as? Sel.GroupSel
                            val onGroup = g != null && groupBounds(before, g)?.let { (a, b) -> val m = nodeR / ed.scale
                                w0.x >= a.x - m && w0.x <= b.x + m && w0.y >= a.y - m && w0.y <= b.y + m } == true
                            // the electric points picked: a drag inside their frame moves them all
                            val ps = ed.sel as? Sel.PointsSel
                            val onPoints = ps != null && Arrange.pointsBounds(before, ps.ids)?.let { (a, b) -> val m = 14.dp.toPx() / ed.scale
                                w0.x >= a.x - m && w0.x <= b.x + m && w0.y >= a.y - m && w0.y <= b.y + m } == true
                            val boxHere = ed.area || ((ed.tool == Tool.PILLAR || (ed.tool == Tool.WALL && ed.idle) || (ed.tool == Tool.SELECT && (ed.scope == Tool.WALL || ed.scope == Tool.PILLAR || ed.scope == Tool.ELECTRIC)) ||
                                (ed.tool == Tool.ELECTRIC && !ed.cableMode && ed.pointKind == null && ed.groupFor == null)) &&
                                hit(before, w0).let { it == null || it is Sel.RoomSel })
                            if (roofGrab == null && !drawLines && onPoints) {
                                target = ps; startW = w0
                            } else if (roofGrab == null && !drawLines && (onGroup || boxHere)) {
                                if (onGroup) target = g else ed.box = w0 to w0
                            }
                            else if (roofGrab == null && drawLines) ed.stroke = listOf(ed.toWorld(start))
                            else if (roofGrab == null) when (if (ed.idle && ed.tool == Tool.WALL) Tool.SELECT else ed.tool) {
                                Tool.WALL -> startW = snap(before, ed.toWorld(start)).first
                                Tool.ROOM, Tool.ROOF, Tool.PLATFORM -> startW = snap(before, ed.toWorld(start)).first
                                else -> {
                                    target = hit(before, ed.toWorld(start)).takeIf { it !is Sel.RoomSel }
                                    ed.sel = target ?: ed.sel
                                }
                            }
                        }
                        if (moved) {
                            val w = ed.toWorld(pos)
                            val grab = roofGrab
                            val st = ed.stroke
                            if (st != null) {
                                if ((st.last() - w).len * ed.scale > 3f) ed.stroke = st + w
                            } else if (grab != null) {
                                val dx = (((w.x - startW.x) / GRID).roundToInt() * GRID).roundToInt()
                                val dy = (((w.y - startW.y) / GRID).roundToInt() * GRID).roundToInt()
                                when (val t = target) {
                                    is Sel.RoofSel -> before.roofs.firstOrNull { it.id == t.id }?.let { rf ->
                                        vm.preview(PlanOps.updateRoof(before, Roofs.drag(rf, grab, snap(before, w).first, dx, dy))); changed = true }
                                    is Sel.PlatformSel -> before.platforms.firstOrNull { it.id == t.id }?.let { pf ->
                                        val r = Roofs.dragRect(intArrayOf(pf.x1, pf.y1, pf.x2, pf.y2), grab, snap(before, w).first, dx, dy, 300)
                                        vm.preview(PlanOps.updatePlatform(before, pf.copy(x1 = r[0], y1 = r[1], x2 = r[2], y2 = r[3]))); changed = true }
                                    is Sel.FenceSel -> before.fences.firstOrNull { it.id == t.id }?.let { f ->
                                        val q = snap(before, w).first
                                        // a turn keeps straight with its neighbours when nearly so
                                        val nb = if (grab >= 0 && grab < f.points.size) listOfNotNull(f.points.getOrNull(grab - 1), f.points.getOrNull(grab + 1)).map { P(it.x.toDouble(), it.y.toDouble()) } else emptyList()
                                        vm.preview(PlanOps.updateFence(before, Fences.drag(f, grab, if (nb.isEmpty()) q else align(q, nb), dx, dy))); changed = true }
                                    is Sel.RoadSel -> before.roads.firstOrNull { it.id == t.id }?.let { r ->
                                        val q = snap(before, w).first
                                        val nb = if (grab >= 0 && grab < r.points.size) listOfNotNull(r.points.getOrNull(grab - 1), r.points.getOrNull(grab + 1)).map { P(it.x.toDouble(), it.y.toDouble()) } else emptyList()
                                        vm.preview(PlanOps.updateRoad(before, r.copy(points = Fences.drag(roadLine(r), grab, if (nb.isEmpty()) q else align(q, nb), dx, dy).points))); changed = true }
                                    is Sel.FeedSel -> houseBefore?.let { hb -> Feeds.of(hb, t.toFloor, t.toId)?.let { f ->
                                        val np = Lines.drag(f.path, grab, snap(before, w).first, fixedEnds = true)
                                        vm.previewHouse(Feeds.setBoard(hb, t.toFloor, t.toId) { it.copy(feedVia = Feeds.via(np)) }); changed = true } }
                                    is Sel.RouteSel -> if (ed.editRouteId == t.id) before.routes.firstOrNull { it.id == t.id }?.let { r ->
                                        val pts = routePlan(before, r) ?: return@let
                                        val np = Lines.drag(pts, grab, snap(before, w).first, fixedEnds = true)
                                        vm.preview(PlanOps.updateRoute(before, r.copy(via = np.drop(1).dropLast(1).mapIndexed { i, q -> Node(i, q.x.roundToInt(), q.y.roundToInt()) }))); changed = true }
                                    is Sel.PipeSel -> before.pipes.firstOrNull { it.id == t.id }?.let { pp ->
                                        val np = Lines.drag(pp.points.map { P(it.x.toDouble(), it.y.toDouble()) }, grab, snap(before, w).first, fixedEnds = false)
                                        if (np.size >= 2) vm.preview(PlanOps.updatePipe(before, pp.copy(points = np.mapIndexed { i, q -> Node(i, q.x.roundToInt(), q.y.roundToInt()) }))); changed = true }
                                    else -> {}
                                }
                            } else if (ed.box != null) {
                                ed.box = ed.box!!.first to w
                            } else if (target is Sel.PointsSel) {
                                val dx = (((w.x - startW.x) / 50).roundToInt() * 50); val dy = (((w.y - startW.y) / 50).roundToInt() * 50)
                                vm.preview(Arrange.movePoints(before, (target as Sel.PointsSel).ids, dx, dy)); changed = true
                            } else if (target is Sel.GroupSel) {
                                val g = target as Sel.GroupSel
                                val dx = (((w.x - startW.x) / GRID).roundToInt() * GRID).roundToInt()
                                val dy = (((w.y - startW.y) / GRID).roundToInt() * GRID).roundToInt()
                                val here = Arrange.move(before, g.nodes, g.pillars, dx, dy, finish = false)
                                if (g.others.isEmpty()) vm.preview(here)
                                else {
                                    val whole = houseBefore ?: vm.root?.also { houseBefore = it } ?: return@awaitEachGesture
                                    var all = whole.withFloor(vm.floor, here)
                                    for ((floorNo, ids) in g.others) {
                                        val fl = whole.floor(floorNo)
                                        all = all.withFloor(floorNo, Arrange.move(fl, ids.first, ids.second, dx, dy, finish = false))
                                    }
                                    vm.previewHouse(all)
                                }
                                changed = true
                            } else when (if (ed.idle && ed.tool == Tool.WALL) Tool.SELECT else ed.tool) {
                                Tool.WALL -> {
                                    val (q, corner) = snap(before, w)
                                    ed.preview = startW to if (corner) q else align(q, listOf(startW))
                                }
                                Tool.ROOM, Tool.ROOF, Tool.PLATFORM -> ed.preview = startW to snap(before, w).first
                                else -> when (val t = target) {
                                    is Sel.PointSel -> { vm.preview(PlanOps.movePoint(before, t.id, if (before.points.firstOrNull { it.id == t.id }?.kind == PointKind.LAMP) snap(before, w).first else w, 1500.0)); changed = true }
                                    is Sel.OpeningSel -> { vm.preview(PlanOps.moveOpening(before, t.id, w, 1500.0)); changed = true }
                                    is Sel.StairSel -> before.stairs.firstOrNull { it.id == t.id }?.let { st ->
                                        val dx = ((w.x - startW.x) / GRID).roundToInt() * GRID
                                        val dy = ((w.y - startW.y) / GRID).roundToInt() * GRID
                                        vm.preview(PlanOps.updateStair(before, st.copy(x = st.x + dx.roundToInt(), y = st.y + dy.roundToInt()))); changed = true
                                    }
                                    is Sel.PillarSel -> before.pillars.firstOrNull { it.id == t.id }?.let { pl ->
                                        val q = snap(before.copy(pillars = before.pillars - pl), P(pl.x + w.x - startW.x, pl.y + w.y - startW.y), pillar = true).first
                                        vm.preview(PlanOps.updatePillar(before, pl.copy(x = q.x.roundToInt(), y = q.y.roundToInt()))); changed = true
                                    }
                                    is Sel.PlaceSel -> before.places.firstOrNull { it.id == t.id }?.let { pl ->
                                        val q = snap(before, w).first
                                        vm.preview(PlanOps.updatePlace(before, pl.copy(x = q.x.roundToInt(), y = q.y.roundToInt()))); changed = true
                                    }
                                    is Sel.ModelSel -> before.models.firstOrNull { it.id == t.id }?.let { m ->
                                        val dx = ((w.x - startW.x) / GRID).roundToInt() * GRID
                                        val dy = ((w.y - startW.y) / GRID).roundToInt() * GRID
                                        vm.preview(PlanOps.updateModel(before, m.copy(x = m.x + dx.roundToInt(), y = m.y + dy.roundToInt()))); changed = true
                                    }
                                    is Sel.ItemSel -> before.items.firstOrNull { it.id == t.id }?.let { item ->
                                        val dx = ((w.x - startW.x) / GRID).roundToInt() * GRID
                                        val dy = ((w.y - startW.y) / GRID).roundToInt() * GRID
                                        vm.preview(PlanOps.updateItem(before, item.copy(x = item.x + dx.roundToInt(), y = item.y + dy.roundToInt()))); changed = true
                                    }
                                    is Sel.Corner -> {
                                        val (q, corner) = snap(before, w, t.id)
                                        val others = before.walls.filter { it.a == t.id || it.b == t.id }
                                            .mapNotNull { before.pos(if (it.a == t.id) it.b else it.a) }
                                        val r = if (corner) q else align(q, others)
                                        vm.preview(PlanOps.moveNode(before, t.id, r.x.roundToInt(), r.y.roundToInt())); changed = true
                                    }
                                    is Sel.WallSel -> {
                                        val wall = before.wall(t.id)
                                        val a = wall?.let { before.pos(it.a) }
                                        val b = wall?.let { before.pos(it.b) }
                                        if (a != null && b != null) {
                                            val n = (b - a).norm().left()
                                            val d = (dot(w - startW, n) / GRID).roundToInt() * GRID
                                            vm.preview(PlanOps.moveWall(before, t.id, (n.x * d).roundToInt(), (n.y * d).roundToInt())); changed = true
                                        }
                                    }
                                    else -> { ed.ox += pos.x - last.x; ed.oy += pos.y - last.y }
                                }
                            }
                        }
                        last = pos
                    }
                    ev.changes.forEach { it.consume() }
                }

                val pv = ed.preview
                ed.preview = null
                val drawnStroke = ed.stroke
                ed.stroke = null
                if (multi) return@awaitEachGesture
                if (ed.browse) return@awaitEachGesture
                if (ed.pickOnly && moved) return@awaitEachGesture
                if (drawnStroke != null) {
                    if (drawnStroke.size >= 2) finishStroke(before, drawnStroke)
                    return@awaitEachGesture
                }
                if (roofGrab != null) {
                    val hb = houseBefore
                    if (changed && hb != null) vm.root?.let { vm.commitHouse(it, hb) }
                    else if (changed) vm.commit(vm.plan ?: before, before)
                    return@awaitEachGesture
                }
                ed.box?.let { (a, b) ->
                    ed.box = null
                    if (multi) return@awaitEachGesture
                    if (ed.tool == Tool.ELECTRIC || (ed.tool == Tool.SELECT && ed.scope == Tool.ELECTRIC)) {
                        // the devices inside the rectangle
                        val ids = Interior.place(before).filter { Circuits.kindOf(it.point.kind) != null }.filter { pp -> val q = symbolAt(pp)
                            q.x in minOf(a.x, b.x)..maxOf(a.x, b.x) && q.y in minOf(a.y, b.y)..maxOf(a.y, b.y) }.map { it.point.id }.toSet()
                        ed.sel = when (ids.size) { 0 -> null; 1 -> Sel.PointSel(ids.first()); else -> Sel.PointsSel(ids) }
                        return@awaitEachGesture
                    }
                    val others = if (!ed.areaAllFloors) emptyMap() else vm.root?.let { r ->
                        r.levels.filter { it != vm.floor }.associateWith { f -> val fl = r.floor(f)
                            if (ed.areaPillars) emptySet<Int>() to Arrange.pillarsIn(fl, a, b) else Arrange.nodesIn(fl, a, b) to emptySet() }
                            .filterValues { it.first.size + it.second.size > 0 }
                    }.orEmpty()
                    val g = if (ed.areaPillars) Sel.GroupSel(emptySet(), Arrange.pillarsIn(before, a, b), others, true) else Sel.GroupSel(Arrange.nodesIn(before, a, b), emptySet(), others, false)
                    ed.sel = g.takeIf { it.nodes.size + it.pillars.size + it.otherCount > 0 }
                    return@awaitEachGesture
                }
                if (target is Sel.PointsSel) {
                    if (changed && !multi) vm.commit(vm.plan ?: before, before)
                    return@awaitEachGesture
                }
                (target as? Sel.GroupSel)?.let { g ->
                    if (changed && !multi) {
                        if (houseBefore != null) vm.root?.let { vm.commitHouse(it, houseBefore!!) }
                        else {
                            val moved = vm.plan ?: before
                            val after = if (g.nodes.isEmpty()) moved else PlanOps.finish(moved)
                            vm.commit(after, before)
                        }
                        // the corners may have been joined to others where they landed: keep those still there
                        val current = vm.plan ?: before
                        ed.sel = g.copy(nodes = g.nodes.filter { current.node(it) != null }.toSet()).takeIf { it.nodes.size + it.pillars.size > 0 }
                    }
                    return@awaitEachGesture
                }
                val gateMode = ed.gatePlacing != null || ed.gateMoving
                ed.gateAim = null
                if (!moved || gateMode) {
                    val w = ed.toWorld(if (gateMode) last else start)
                    // A held load starts source selection. A held empty part of a cable still opens cable editing.
                    val directHit = hit(before, w)
                    val heldCable = if (ed.tool == Tool.ELECTRIC && System.currentTimeMillis() - pressedAt >= 500 && directHit !is Sel.PointSel) routeNear(before, w) else null
                    val h = heldCable?.let { Sel.RouteSel(it) } ?: directHit
                    // Source-pick is modal only until the next deliberate choice. Tapping empty space, the original
                    // load, or starting another placement cancels it, so ordinary selection can never arm a source.
                    if (ed.sourcePickFrom != null && (h !is Sel.PointSel || h.id == ed.sourcePickFrom)) {
                        ed.sourcePickFrom = null; ed.sourcePickTo = null
                    }
                    val link = ed.linking
                    val pickedFence = (ed.sel as? Sel.FenceSel)?.let { s -> before.fences.firstOrNull { it.id == s.id } }
                    val onFence = pickedFence?.let { Fences.project(it, w) }?.takeIf { it.second <= (if (gateMode) 70.dp else 40.dp).toPx() / ed.scale }
                    val sideHit = (ed.sel as? Sel.WallSel)?.let { ws -> WallShapes.build(before).firstOrNull { it.wall.id == ws.id } }?.let { sh ->
                        listOf(Facings.Side.LEFT to 1.0, Facings.Side.RIGHT to -1.0).firstOrNull { (_, sg) ->
                            (sideBadge(sh, sg, ed, 26.dp.toPx()).first + (sh.b - sh.a).norm().left() * (sg * 6.dp.toPx() / ed.scale) - w).len * ed.scale < 24.dp.toPx() }?.first
                    }
                    val gateHit = if (pickedFence != null && onFence != null) Fences.openings(pickedFence).indexOfFirst { (g0, g1) -> onFence.first in g0..g1 }.takeIf { it >= 0 } else null
                    when {
                        // Cross-floor cable: after choosing the destination floor, the next eligible board/box is its
                        // other end.  The horizontal run is drawn here and the rise/drop is calculated from both floors.
                        ed.crossFrom != null && h is Sel.PointSel && vm.floor != ed.crossFrom!!.first &&
                            before.points.firstOrNull { it.id == h.id }?.let(CrossCables::endpoint) == true -> {
                            val (fromFloor, fromId) = ed.crossFrom!!
                            val root = vm.root
                            if (root != null) {
                                val after = CrossCables.add(root, fromFloor, fromId, vm.floor, h.id, ed.crossCable, ed.crossLevel)
                                if (after != root) {
                                    vm.commitHouse(after)
                                    DebugLog.add("cross cable: $fromFloor#$fromId → ${vm.floor}#${h.id}, ${ed.crossCable}/${ed.crossLevel}")
                                    ed.sel = h; ed.toastText = "Cross-floor cable added"
                                } else ed.toastText = if (ed.crossCable == CommCable.POWER) "Power cable needs a board, junction box, or riser at both ends."
                                else "Communication cable needs low-current boards or communications boxes at both ends."
                            }
                            ed.crossFrom = null; ed.crossPickFloor = false
                        }
                        // Pairing is a modal operation.  It wins over the normal held-load action so holding the
                        // second switch cannot accidentally open “set source” instead of making the two-way pair.
                        ed.tool == Tool.ELECTRIC && ed.pairing != null && h is Sel.PointSel && h.id != ed.pairing &&
                            before.points.firstOrNull { it.id == h.id }?.kind == PointKind.SWITCH -> {
                            val a = ed.pairing!!; ed.pairing = null
                            val np = PlanOps.pairSwitches(before, a, h.id)
                            // A two-way pair is a real traveller cable, routed just like the other cables.  Rewire
                            // even when it is the first circuit: join() will feed it from the board when one exists.
                            vm.commit(Autoroute.rewire(np, listOf(a, h.id), ed.routeLevel, vm.root, vm.floor), before)
                            ed.sel = Sel.PointSel(a); ed.toast = R.string.paired
                        }
                        ed.tool == Tool.ELECTRIC && System.currentTimeMillis() - pressedAt >= 500 && h is Sel.PointSel &&
                            before.points.firstOrNull { it.id == h.id }?.let { Circuits.kindOf(it.kind) != null } == true -> {
                            ed.sourcePickFrom = h.id; ed.sourcePickTo = null; ed.sel = h; ed.linking = null
                            DebugLog.add("electric: load #${h.id} select source")
                            ed.toastText = "Tap the device that feeds this load, then confirm."
                        }
                        ed.tool == Tool.ELECTRIC && ed.sourcePickFrom != null && h is Sel.PointSel && h.id != ed.sourcePickFrom -> {
                            ed.sourcePickTo = h.id; ed.sel = h; ed.linking = null
                            DebugLog.add("electric: load #${ed.sourcePickFrom} source candidate #${h.id}")
                        }
                        // a gate put in the fence picked, or the picked gate moved, where it is tapped
                        pickedFence != null && onFence != null && (ed.gatePlacing != null || ed.gateMoving) -> {
                            val gi = ed.gateIndex
                            val gates = if (ed.gateMoving && gi != null && gi < pickedFence.gates.size)
                                pickedFence.gates.mapIndexed { i, g -> if (i == gi) g.copy(at = onFence.first.roundToInt()) else g }
                            else pickedFence.gates + Gate(onFence.first.roundToInt(), ed.gatePlacing!!.width, ed.gatePlacing!!)
                            vm.commit(PlanOps.updateFence(before, pickedFence.copy(gates = gates)), before)
                            ed.gateIndex = if (ed.gateMoving) gi else gates.size - 1
                            ed.gatePlacing = null; ed.gateMoving = false
                        }
                        // a gate of the fence picked is picked by tapping it
                        gateHit != null -> ed.gateIndex = gateHit
                        // the L / R arrow of the wall picked: choose the stone for that side
                        sideHit != null -> ed.facingSide = sideHit
                        // picking many walls: a tap adds the wall under it, or takes it out again
                        ed.pickWalls -> hits(before, w).firstOrNull { it is Sel.WallSel }?.let { hw ->
                            val id = (hw as Sel.WallSel).id
                            val cur = when (val s0 = ed.sel) { is Sel.WallsSel -> s0.ids; is Sel.WallSel -> setOf(s0.id); else -> emptySet() }
                            val next = if (id in cur) cur - id else cur + id
                            ed.sel = when (next.size) { 0 -> null; 1 -> Sel.WallSel(next.first()); else -> Sel.WallsSel(next) }
                        }
                        ed.tool == Tool.SYSTEMS && ed.systemMode == "warm" -> {
                            Rooms.detect(before).firstOrNull { Geo.inside(w, it.polygon) }?.let { vm.commit(PlanOps.toggleWarmFloor(before, it), before) }
                        }
                        ed.tool == Tool.SYSTEMS && ed.systemMode == "radiator" && h !is Sel.ItemSel -> {
                            val q = snap(before, w).first
                            val after = PlanOps.addItem(before, "radiator", q.x.roundToInt(), q.y.roundToInt())
                            vm.commit(after, before); ed.sel = Sel.ItemSel(after.items.last().id)
                        }
                        ed.tool in setOf(Tool.ROAD, Tool.YARD) && ed.roadArea && ed.pipePts.size >= 3 && (ed.pipePts.first() - w).len <= nodeR * 1.6 / ed.scale -> finishDrawing(vm, ed)
                        (ed.tool == Tool.FENCE || ed.tool in setOf(Tool.ROAD, Tool.YARD)) && ed.pipePts.size >= 2 && (ed.pipePts.last() - w).len <= nodeR * 1.6 / ed.scale -> finishDrawing(vm, ed)
                        ed.tool == Tool.YARD && ed.pipePts.isEmpty() && (ed.sel as? Sel.RoadSel)?.let { s -> before.roads.firstOrNull { it.id == s.id && it.closed } }?.let { r ->
                            r.points.indices.minByOrNull { i -> (P(r.points[i].x.toDouble(), r.points[i].y.toDouble()) - w).len }?.takeIf { i -> (P(r.points[i].x.toDouble(), r.points[i].y.toDouble()) - w).len <= nodeR * 1.6 / ed.scale }
                        } != null -> {
                            val r = before.roads.first { it.id == (ed.sel as Sel.RoadSel).id }
                            val i = r.points.indices.minBy { (P(r.points[it].x.toDouble(), r.points[it].y.toDouble()) - w).len }
                            ed.yardPointIds = if (i in ed.yardPointIds) ed.yardPointIds - i else ed.yardPointIds + i
                        }
                        ed.tool in setOf(Tool.ROAD, Tool.YARD) && ed.yardKind == null && ed.itemKind == null && vm.assetToPlace == null && (h !is Sel.RoadSel || ed.pipePts.isNotEmpty()) -> {
                            var q = snap(before, w).first
                            ed.pipePts.lastOrNull()?.let { q = align(q, listOf(it)) }
                            ed.pipePts = ed.pipePts + q
                        }
                        // a gate: into the fence tapped, else into the wall tapped
                        ed.tool == Tool.GATE && h !is Sel.OpeningSel -> {
                            val near = before.fences.map { it to Fences.project(it, w) }.filter { it.second.second <= 40.dp.toPx() / ed.scale }.minByOrNull { it.second.second }
                            if (near != null) {
                                val (f, pr) = near
                                vm.commit(PlanOps.updateFence(before, f.copy(gates = f.gates + Gate(pr.first.roundToInt(), ed.gateKind.width, ed.gateKind))), before)
                                ed.sel = Sel.FenceSel(f.id); ed.gateIndex = f.gates.size
                            } else PlanOps.addOpening(before, OpeningKind.GATE, snap(before, w).first, 1500.0)
                                ?.let { a -> val g = a.openings.last(); val after = if (g.gate != ed.gateKind) PlanOps.updateOpening(a, g.copy(gate = ed.gateKind, width = ed.gateKind.width)) else a
                                    vm.commit(after, before); ed.sel = Sel.OpeningSel(g.id) }
                        }
                        ed.tool == Tool.FENCE && (h !is Sel.FenceSel || ed.pipePts.isNotEmpty()) -> {
                            var q = snap(before, w).first
                            ed.pipePts.lastOrNull()?.let { q = align(q, listOf(it)) }
                            ed.pipePts = ed.pipePts + q
                            ed.sel = null
                        }
                        ed.tool == Tool.SYSTEMS && ed.systemMode == "pipe" && (h !is Sel.PipeSel || ed.pipePts.isNotEmpty()) -> {
                            // each tap is a turn of the pipe; straight with the previous turn when nearly so
                            var q = snap(before, w).first
                            ed.pipePts.lastOrNull()?.let { q = align(q, listOf(it)) }
                            ed.pipePts = ed.pipePts + q
                            ed.sel = null
                        }
                        ed.tool == Tool.ELECTRIC && ed.cableMode -> {
                            val from = ed.routeFrom
                            when {
                                h is Sel.PointSel && from == null -> { ed.routeFrom = h.id; ed.routeVia = emptyList(); ed.sel = null }
                                h is Sel.RouteSel && from == null -> {
                                    // starts on a cable: a junction box there
                                    Circuits.split(before, h.id, w)?.let { (after, box) -> if (after !== before) vm.commit(after, before); ed.routeFrom = box; ed.routeVia = emptyList(); ed.sel = null }
                                }
                                h is Sel.RouteSel && from != null -> {
                                    // ends on a cable: joined to it in a junction box
                                    val r = before.routes.first { it.id == h.id }
                                    if (Circuits.mixes(before, from, r.from)) ed.toast = R.string.mixed_circuits
                                    else Circuits.split(before, h.id, w)?.takeIf { it.second != from }?.let { (split, box) ->
                                        val after = PlanOps.addRoute(split, from, box, followVia(split, from, ed.routeVia, box), ed.routeLevel)
                                        vm.commit(after, before)
                                        ed.routeFrom = null; ed.routeVia = emptyList()
                                        ed.sel = Sel.PointSel(box)
                                    }
                                }
                                h is Sel.PointSel && from != null && h.id != from && Circuits.mixes(before, from, h.id) -> ed.toast = R.string.mixed_circuits
                                h is Sel.PointSel && from != null && h.id != from -> {
                                    val after = PlanOps.addRoute(before, from, h.id, followVia(before, from, ed.routeVia, h.id), ed.routeLevel)
                                    vm.commit(after, before)
                                    ed.routeFrom = null; ed.routeVia = emptyList()
                                    ed.sel = Sel.RouteSel(after.routes.last().id)
                                }
                                from != null -> {
                                    // a turn: snapped, and straight with the previous one when nearly so
                                    val prev = ed.routeVia.lastOrNull() ?: before.points.firstOrNull { it.id == from }?.let { P(it.x.toDouble(), it.y.toDouble()) }
                                    var q = snap(before, w).first
                                    // onto a turn of a cable already there, so both turn at the same corner
                                    val turns = before.routes.flatMap { it.via }.map { P(it.x.toDouble(), it.y.toDouble()) }
                                    turns.minByOrNull { (it - w).len }?.takeIf { (it - w).len <= nodeR * 1.5 / ed.scale }?.let { q = it }
                                        ?: run { if (prev != null) q = align(q, listOf(prev) + turns) }
                                    ed.routeVia = ed.routeVia + q
                                }
                                else -> ed.sel = h
                            }
                        }
                        ed.groupFor != null && ed.tool == Tool.ELECTRIC -> {
                            // the lamps picked go on the switch tapped, or on a new one put where the tap was
                            val lamps = ed.groupFor!!
                            ed.groupFor = null
                            val hp = (h as? Sel.PointSel)?.let { s -> before.points.firstOrNull { it.id == s.id } }
                            val made = if (hp?.kind == PointKind.SWITCH) before to hp.id else PlanOps.addPoint(before, PointKind.SWITCH, w, 1500.0)?.let { it to it.points.last().id }
                            if (made != null) {
                                // and the cables at once: board → switch → lamps
                                val grouped = PlanOps.groupSwitch(made.first, made.second, lamps)
                                vm.commit(Autoroute.join(grouped, lamps + made.second, ed.routeLevel, vm.root, vm.floor).first, before)
                                ed.sel = Sel.PointSel(made.second); ed.toastText = groupedText.format(lamps.size)
                            }
                        }
                        link != null && h is Sel.PointSel && before.points.firstOrNull { it.id == h.id }?.kind == PointKind.LAMP ->
                            vm.commit(PlanOps.toggleLink(before, link, h.id), before)
                        ed.tool == Tool.STAIRS && h !is Sel.StairSel -> {
                            val q = snap(before, w).first
                            val after = PlanOps.addStair(before, q.x.roundToInt(), q.y.roundToInt())
                            ed.sel = Sel.StairSel(after.stairs.last().id)
                            commitStair(vm, ed, after, before, after.stairs.last().id)
                        }
                        ed.tool == Tool.PILLAR && h !is Sel.PillarSel -> {
                            val q = snap(before, w).first
                            // a new pillar is like the last one picked or placed
                            val like = (ed.sel as? Sel.PillarSel)?.let { s -> before.pillars.firstOrNull { it.id == s.id } } ?: before.pillars.lastOrNull()
                            val after = PlanOps.addPillar(before, q.x.roundToInt(), q.y.roundToInt(), like)
                            vm.commit(after, before); ed.sel = Sel.PillarSel(after.pillars.last().id)
                        }
                        ed.tool == Tool.PLACE && h !is Sel.PlaceSel -> {
                            val q = snap(before, w).first
                            val after = PlanOps.addPlace(before, q.x.roundToInt(), q.y.roundToInt())
                            vm.commit(after, before)
                            ed.sel = Sel.PlaceSel(after.places.last().id); ed.askPlace = after.places.last().id
                        }
                        (ed.tool == Tool.DOOR || ed.tool == Tool.WINDOW) && h !is Sel.OpeningSel -> {
                            PlanOps.addOpening(before, if (ed.tool == Tool.DOOR) OpeningKind.DOOR else OpeningKind.WINDOW, snap(before, w).first, 1500.0)
                                ?.let { vm.commit(it, before); ed.sel = Sel.OpeningSel(it.openings.last().id) }
                        }
                        ed.tool == Tool.ELECTRIC && h is Sel.RouteSel -> {
                            // A normal tap only inspects. Editing cable geometry/properties is deliberately long-press
                            // only, so an ordinary tap cannot accidentally put a cable into edit mode.
                            val longPress = System.currentTimeMillis() - pressedAt >= 500
                            ed.editRouteId = if (longPress) h.id else null
                            ed.sel = h; ed.linking = null
                            DebugLog.add("electric: cable #${h.id} ${if (longPress) "long-press edit" else if (ed.editRouteId == h.id) "editing" else "selected"}")
                        }
                        ed.tool == Tool.ELECTRIC && h !is Sel.PointSel && ed.pointKind != null -> {
                            ed.linking = null
                            val at = if (ed.pointKind == PointKind.LAMP) snap(before, w).first else w
                            if (ed.pointKind == PointKind.RISER) { vm.addRiser(at); return@awaitEachGesture }
                            PlanOps.addPoint(before, ed.pointKind!!, at, 1500.0)?.let { placed0 ->
                                // a lamp goes on the nearest switch of its room, else on a breaker in the board
                                val placed = if (ed.pointKind == PointKind.LAMP) PlanOps.linkNearestSwitch(placed0, placed0.points.last().id) else placed0
                                if (ed.pointKind == PointKind.LAMP) {
                                    val lamp = placed.points.last()
                                    DebugLog.add("electric: lamp #${lamp.id} placed; auto-linked switch=${lamp.switchId ?: "board"}")
                                } else if (ed.pointKind == PointKind.SWITCH) {
                                    DebugLog.add("electric: switch #${placed.points.last().id} placed")
                                }
                                // Every electrical load is routed now. A lamp is routed through its selected switch,
                                // rather than leaving only a direct visual control-link.
                                val (it, laid) = if (ed.category == Tool.INTERIOR && ed.pointKind == PointKind.LAMP) placed to 0 else if (ed.pointKind == PointKind.SOCKET || ed.pointKind == PointKind.APPLIANCE || ed.pointKind == PointKind.LAMP)
                                    Autoroute.join(placed, listOf(placed.points.last().id), ed.routeLevel, vm.root, vm.floor) else placed to 0
                                if (ed.pointKind == PointKind.LAMP) DebugLog.add("electric: lamp #${placed.points.last().id} routed cables=$laid")
                                vm.commit(it, before); ed.sel = Sel.PointSel(placed.points.last().id)
                                if (ed.pointKind == PointKind.APPLIANCE) ed.askFor = placed.points.last().id
                                // the board that will feed it blinks; another can be chosen in its panel
                                if (ed.pointKind != PointKind.BOARD && it === placed) vm.root?.let { root ->
                                    val boards = Boards.all(root)
                                    Boards.of(root, boards, vm.floor, placed.points.last())?.let { b ->
                                        if (b.floor == vm.floor) ed.feeds = b.point.id
                                        else ed.toastText = feedsText.format(Boards.name(b, ruBoards))
                                    }
                                }
                            }
                        }
                        vm.assetToPlace != null && h !is Sel.ModelSel -> {
                            val a = vm.assetToPlace!!
                            val q = snap(before, w).first
                            val (after, id) = PlanOps.addModel(before, a.name, a.parts, q)
                            vm.commit(after, before); ed.sel = Sel.ModelSel(id); vm.assetToPlace = null
                        }
                        ed.tool in setOf(Tool.INTERIOR, Tool.YARD) && ed.yardKind != null -> {
                            // a yard construction from its usual sizes, its name numbered
                            val kind = ed.yardKind!!
                            val spec = ed.yardSpec ?: Yard.default(kind)
                            val base = yardNames[kind] ?: kind.name
                            val n = generateSequence(1) { it + 1 }.first { k -> before.models.none { it.name == "$base $k" } }
                            val q = snap(before, w).first
                            val (after, id) = PlanOps.putModel(before, "$base $n", Yard.parts(spec, ruBoards), q)
                            vm.commit(after.copy(models = after.models.map { if (it.id == id) it.copy(yard = spec) else it }), before)
                            ed.sel = Sel.ModelSel(id); ed.yardKind = null; ed.yardSpec = null
                        }
                        ed.tool in setOf(Tool.INTERIOR, Tool.YARD) && h !is Sel.ItemSel && ed.itemKind != null -> {
                            val q = snap(before, w).first
                            val after = PlanOps.addItem(before, ed.itemKind!!, q.x.roundToInt(), q.y.roundToInt())
                            vm.commit(after, before); ed.sel = Sel.ItemSel(after.items.last().id)
                        }
                        else -> {
                            val now = System.currentTimeMillis()
                            val lt = ed.lastTap
                            val was = ed.selBeforeTap
                            if (lt != null && now - lt.first < 450 && (lt.second - start).getDistance() < 40.dp.toPx() &&
                                was is Sel.WallSel && h == was && !ed.pickOnly) {
                                // a double tap on the wall already picked: a corner put into it there
                                ed.lastTap = null
                                Arrange.split(before, was.id, w)?.let { (after, id) -> vm.commit(after, before, tidy = false); ed.sel = Sel.Corner(id) }
                                return@awaitEachGesture
                            }
                            if (lt != null && now - lt.first < 450 && (lt.second - start).getDistance() < 40.dp.toPx()) {
                                // a double tap: what lies under the thing picked (a wall under a roof…), and its menu opens
                                ed.lastTap = null
                                val all = hits(before, w, deep = true)
                                val cur = ed.sel
                                val next = all.getOrNull(all.indexOf(cur) + 1) ?: all.firstOrNull { it != cur }
                                if (next != null) {
                                    // its menu opens, picking in it (the row's ✋ Select stays as it was)
                                    val t = categoryOf(toolOf(next))
                                    if (ed.tool != Tool.SELECT) { finishDrawing(vm, ed); ed.cableMode = false; ed.pointKind = null; ed.itemKind = null; ed.yardKind = null; ed.systemMode = "" }
                                    ed.category = t; ed.tool = Tool.SELECT; ed.selectOnly = t
                                    ed.sel = next; ed.linking = null
                                    return@awaitEachGesture
                                }
                            } else { ed.lastTap = now to start; ed.selBeforeTap = ed.sel }
                            ed.sel = h; ed.linking = null
                            (h as? Sel.PointSel)?.let { hs -> before.points.firstOrNull { it.id == hs.id }?.let { p0 ->
                                val toBoardIds = Circuits.toBoard(before, p0.id).map { it.id }
                                when (p0.kind) {
                                    PointKind.SWITCH -> {
                                        val reciprocal = p0.pairId?.takeIf { pid -> before.points.firstOrNull { it.id == pid && it.kind == PointKind.SWITCH }?.pairId == p0.id }
                                        val lamps = before.points.filter { it.kind == PointKind.LAMP && (it.switchId == p0.id || it.switchId == reciprocal) }.map { it.id }
                                        val lampRoutes = lamps.flatMap { lamp -> Circuits.path(before, lamp, p0.id).map { it.id } }
                                        DebugLog.add("electric focus: tapped SWITCH #${p0.id}, board=${p0.board ?: "auto"}, pair=${reciprocal ?: "none"}, lamps=$lamps, highlightRoutes=${(toBoardIds + lampRoutes).distinct()}, boardRoutes=$toBoardIds")
                                    }
                                    PointKind.LAMP -> {
                                        val source = p0.sourceId ?: p0.switchId
                                        val routes = source?.let { id -> Circuits.path(before, p0.id, id).map { it.id } }.orEmpty()
                                        DebugLog.add("electric focus: tapped LAMP #${p0.id}, switch=${p0.switchId ?: "none"}, source=${p0.sourceId ?: "auto"}, highlightRoutes=$routes")
                                    }
                                    else -> DebugLog.add("electric focus: tapped ${p0.kind} #${p0.id}, board=${p0.board ?: "auto"}, highlightRoutes=$toBoardIds")
                                }
                            } }
                            // Repair plans made by older builds that chained a switch through a different switch.
                            // A switch with a chosen board gets its own board feed; sharing a run never means control sharing.
                            (h as? Sel.PointSel)?.let { hs -> before.points.firstOrNull { it.id == hs.id && it.kind == PointKind.SWITCH }?.let { sw ->
                                val chained = Circuits.toBoard(before, sw.id).any { r ->
                                    listOf(r.from, r.to).any { id -> id != sw.id && before.points.firstOrNull { it.id == id }?.kind == PointKind.SWITCH }
                                }
                                if (sw.board != null && chained) {
                                    DebugLog.add("electric: repaired switch #${sw.id} direct feed from chosen board")
                                    vm.commit(Autoroute.rewire(before, sw.id, ed.routeLevel, vm.root, vm.floor), before)
                                } else DebugLog.add("electric: selected switch #${sw.id}, lamps=${before.points.count { it.kind == PointKind.LAMP && it.switchId == sw.id }}, pair=${sw.pairId ?: "none"}")
                            } }
                            if (h is Sel.RoomSel) Rooms.detect(before).firstOrNull { Geo.inside(h.at, it.polygon) }?.info?.type?.let { ed.roomType = it }
                        }
                    }
                    return@awaitEachGesture
                }
                when (ed.tool) {
                    Tool.WALL -> if (pv != null && (pv.second - pv.first).len >= 100) vm.commit(
                        PlanOps.addWall(before, pv.first.x.roundToInt(), pv.first.y.roundToInt(), pv.second.x.roundToInt(), pv.second.y.roundToInt()), before)
                    Tool.ROOM -> if (pv != null && abs(pv.second.x - pv.first.x) >= 300 && abs(pv.second.y - pv.first.y) >= 300) vm.commit(
                        PlanOps.addRect(before, pv.first.x.roundToInt(), pv.first.y.roundToInt(), pv.second.x.roundToInt(), pv.second.y.roundToInt()), before)
                    Tool.PLATFORM -> if (pv != null && abs(pv.second.x - pv.first.x) >= 300 && abs(pv.second.y - pv.first.y) >= 300) {
                        val after = PlanOps.addPlatform(before, pv.first.x.roundToInt(), pv.first.y.roundToInt(), pv.second.x.roundToInt(), pv.second.y.roundToInt(), ed.platformHeight)
                        vm.commit(after, before); ed.sel = Sel.PlatformSel(after.platforms.last().id)
                    }
                    Tool.ROOF -> if (pv != null && abs(pv.second.x - pv.first.x) >= 500 && abs(pv.second.y - pv.first.y) >= 500) {
                        val after = PlanOps.addRoof(before, pv.first.x.roundToInt(), pv.first.y.roundToInt(), pv.second.x.roundToInt(), pv.second.y.roundToInt())
                        vm.commit(after, before); ed.sel = Sel.RoofSel(after.roofs.last().id)
                    }
                    else -> if (changed) {
                        val after = if (target is Sel.PointSel || target is Sel.ItemSel || target is Sel.OpeningSel || target is Sel.PlaceSel || target is Sel.StairSel || target is Sel.PillarSel) (vm.plan ?: before) else PlanOps.finish(vm.plan ?: before)
                        val t = target
                        if (t is Sel.StairSel) commitStair(vm, ed, after, before, t.id) else vm.commit(after, before)
                        val s = ed.sel
                        if ((s is Sel.Corner && after.node(s.id) == null) || (s is Sel.WallSel && after.wall(s.id) == null)) ed.sel = null
                    }
                }
            }
        },
    ) {
        drawRect(Color(0xFFF7F7F3))
        grid(ed)

        fun path(points: List<P>) = Path().apply {
            points.forEachIndexed { i, p -> val o = ed.toScreen(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
            close()
        }
        // The floor below, faint, to draw on top of; its stairs show where they come up.
        below?.let { b -> for (w in belowShapes) drawPath(path(w.outline), Color(0x33303A4B)) }
        val sel = ed.sel
        // roads and paths under everything: a wide band of the surface, its edges a little darker
        for (rd in plan.roads) {
            // Closed grass is a ground layer. It is drawn after the floor plan below so it remains visible.
            if (rd.closed && rd.surface == Surface.GRASS) continue
            val pts = rd.points.map { ed.toScreen(P(it.x.toDouble(), it.y.toDouble())) }
            val pth = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }; if (rd.closed) close() }
            val wpx = maxOf(4.dp.toPx(), rd.width * ed.scale)
            val c = Color(0xFF000000 or rd.surface.color.toLong())
            val on = sel is Sel.RoadSel && sel.id == rd.id
            if (rd.closed) { drawPath(pth, c); drawPath(pth, if (on) accent else Color(0xFF555555), style = Stroke(if (on) 3.dp.toPx() else 1.dp.toPx())) }
            else { drawPath(pth, if (on) accent.copy(alpha = 0.6f) else Color(0x55000000), style = Stroke(wpx + 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)); drawPath(pth, c, style = Stroke(wpx, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)) }
            // Fine clipped blades make a lawn read as grass while retaining the user-drawn outline.
            if (rd.closed && rd.surface == Surface.GRASS) clipPath(pth) {
                val left = pts.minOf { it.x }; val right = pts.maxOf { it.x }; val top = pts.minOf { it.y }; val bottom = pts.maxOf { it.y }
                val step = maxOf(8.dp.toPx(), 180f * ed.scale)
                var x = left - step
                while (x < right + step) { drawLine(Color(0x33406038), Offset(x, bottom + step), Offset(x + step * 0.45f, top - step), 1.dp.toPx()); x += step }
            }
            when (rd.surface) {
                Surface.GRAVEL, Surface.DIRT -> drawPath(pth, Color(0x33FFFFFF), style = Stroke(wpx * 0.85f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 7f)), join = androidx.compose.ui.graphics.StrokeJoin.Round))
                Surface.PAVERS, Surface.STONE, Surface.GRASS_GRID -> drawPath(pth, Color(0x33000000), style = Stroke(wpx * 0.9f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5f, maxOf(3f, 200f * ed.scale))), join = androidx.compose.ui.graphics.StrokeJoin.Round))
                Surface.ASPHALT -> if (rd.width >= 3000) drawPath(pth, Color(0xAAFFFFFF), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
                else -> {}
            }
        }
        rooms.forEachIndexed { i, r ->
            val p = path(r.polygon)
            drawPath(p, Color(0xFF000000 or Palette.floor(r, i).toLong()))
            if (sel is Sel.RoomSel && Geo.inside(sel.at, r.polygon)) drawPath(p, accent.copy(alpha = 0.25f))
        }
        // Grass platforms are intentionally painted last among the ground layers.  Earlier builds painted them below
        // the opaque floor fill, making a correctly saved grass polygon invisible in 2D.
        for (rd in plan.roads.filter { it.closed && it.surface == Surface.GRASS }) {
            val pts = rd.points.map { ed.toScreen(P(it.x.toDouble(), it.y.toDouble())) }
            if (pts.size < 3) continue
            val pth = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }; close() }
            drawPath(pth, Color(0xDD000000 or rd.surface.color.toLong()))
            drawPath(pth, Color(0xFF356D2F), style = Stroke(1.5.dp.toPx()))
            clipPath(pth) {
                val left = pts.minOf { it.x }; val right = pts.maxOf { it.x }; val top = pts.minOf { it.y }; val bottom = pts.maxOf { it.y }
                val step = maxOf(8.dp.toPx(), 180f * ed.scale)
                var x = left - step
                while (x < right + step) { drawLine(Color(0x33406038), Offset(x, bottom + step), Offset(x + step * 0.45f, top - step), 1.dp.toPx()); x += step }
            }
        }
        for (i in if (Layer.FURNITURE in vm.hidden) emptyList() else plan.items) {
            if (!itemShown(vm, ed, i)) continue
            val on = sel is Sel.ItemSel && sel.id == i.id
            val pth = path(Interior.corners(i))
            if (i.kind == STAIRS) stairs(this, ed, i, Color(0xFFD9C3A5), Color(0xFF705030))
            else for (part in Furniture.parts(i).sortedBy { it.z1 }) {
                val pp = path(part.corners)
                drawPath(pp, Color(0xFF000000 or part.color.toLong()))
                drawPath(pp, Color(0x40000000), style = Stroke(0.7.dp.toPx()))
            }
            drawPath(pth, if (on) accent else Color(0xFF6B6F78), style = Stroke(if (on) 3.dp.toPx() else 1.dp.toPx()))
            if (i.w * ed.scale > 56.dp.toPx() && i.d * ed.scale > 20.dp.toPx())
                label(tm, itemNames[i.kind] ?: "", ed.toScreen(P(i.x.toDouble(), i.y.toDouble())), TextStyle(fontSize = 10.sp, color = Color(0xFF30343C), textAlign = TextAlign.Center), null)
        }
        // 3D models seen from above, in their colours, the far (low) faces first
        if (Layer.FURNITURE !in vm.hidden) for (m in plan.models) {
            val shapes = modelTops[m.id] ?: continue
            val o = ed.toScreen(P(m.x.toDouble(), m.y.toDouble()))
            withTransform({ translate(o.x, o.y); rotate(m.rot.toFloat(), Offset.Zero); scale(ed.scale, ed.scale, Offset.Zero) }) {
                for ((pth, col) in shapes) drawPath(pth, col)
            }
            val on = sel is Sel.ModelSel && sel.id == m.id
            drawPath(path(Models.corners(m)), if (on) accent else Color(0x886B6F78), style = Stroke(if (on) 3.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (on) null else PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
            if (on) label(tm, m.name, ed.toScreen(Models.corners(m)[0]) + Offset(0f, -12.dp.toPx()), TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold), accent)
        }
        // Stairs coming up from the floor below: the opening in this floor, dashed, with the stairs seen through it.
        arriving?.let { (lower, stairs) ->
            for (st in stairs) {
                val shp = Stairs.shape(lower, st)
                stairShape(this, ed, shp, Color(0x66B08D6A), Color(0xAA705030), null)
                for (pc in shp.pieces) {
                    drawPath(path(pc), Color(0xFF705030), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f))))
                    // an opening in the floor is drawn crossed, as on building plans
                    drawLine(Color(0xAA705030), ed.toScreen(pc[0]), ed.toScreen(pc[2]), 1.2.dp.toPx())
                    drawLine(Color(0xAA705030), ed.toScreen(pc[1]), ed.toScreen(pc[3]), 1.2.dp.toPx())
                }
                val pts = shp.pieces.flatten()
                val c = pts.fold(P(0.0, 0.0)) { acc, q -> acc + q } * (1.0 / pts.size)
                label(tm, fromBelow, ed.toScreen(c), TextStyle(fontSize = 11.sp, color = Color(0xFF5A3D20), fontWeight = FontWeight.Medium), Color(0xDDF7F7F3))
            }
        }

        for (s in shapes) {
            val on = (sel is Sel.WallSel && sel.id == s.wall.id) || (sel is Sel.WallsSel && s.wall.id in sel.ids)
            // a lower wall (parapet, half wall) is lighter
            val low = plan.heightOf(s.wall) < plan.wallHeight
            val ink = if (on) accent else if (low) Color(0xFF8A90A0) else Color(0xFF3A3F4B)
            if (!vm.seeThrough) drawPath(path(s.outline), ink)
            else {
                // see-through: a faint wall with its outline, solid only at its ends (the corners)
                drawPath(path(s.outline), ink.copy(alpha = if (on) 0.35f else 0.14f))
                drawPath(path(s.outline), ink.copy(alpha = 0.45f), style = Stroke(1.dp.toPx()))
                val d = (s.b - s.a).norm(); val n = d.left() * (s.wall.thickness / 2.0)
                val l = minOf(maxOf(s.wall.thickness * 1.2, 250.0), (s.b - s.a).len / 2)
                drawPath(path(listOf(s.a, s.aL, s.a + d * l + n, s.a + d * l - n, s.aR)), ink)
                drawPath(path(listOf(s.b, s.bR, s.b - d * l - n, s.b - d * l + n, s.bL)), ink)
            }
            if (s.wall.id in clashing) drawPath(path(s.outline), Color(0xFFE0353B).copy(alpha = blink))
            if (on && sel is Sel.WallSel) {
                // its two sides, each an arrow out of it to tap and put a stone on that side
                val d = (s.b - s.a).norm()
                val st = TextStyle(fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                val (fl, fr) = Facings.sides(plan, s.wall, rooms)
                for ((name, sign) in listOf("L" to 1.0, "R" to -1.0)) {
                    val c = sideBadge(s, sign, ed, 26.dp.toPx())
                    val dir = d.left() * sign
                    val tip = ed.toScreen(c.first + dir * (24.dp.toPx() / ed.scale).toDouble())
                    val base = ed.toScreen(c.first + dir * (12.dp.toPx() / ed.scale).toDouble())
                    val side = Offset((d.x * 7.dp.toPx()).toFloat(), (d.y * 7.dp.toPx()).toFloat())
                    drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(base.x + side.x, base.y + side.y); lineTo(base.x - side.x, base.y - side.y); close() }, accent)
                    val f = if (sign > 0) fl else fr
                    label(tm, name, ed.toScreen(c.first), st, if (f != null) Color(0xFF000000 or f.color.toLong()).copy(alpha = 1f) else accent)
                    if (f != null) drawCircle(accent, 13.dp.toPx(), ed.toScreen(c.first), style = Stroke(2.dp.toPx()))
                }
            }
            // clad with stone: a band of its colour along the clad face(s)
            val (fL, fR) = Facings.sides(plan, s.wall, rooms)
            if (fL != null || fR != null) {
                val d = (s.b - s.a).norm()
                val bw = maxOf(4.dp.toPx(), 60f * ed.scale)
                for ((f, e0, e1, side) in listOf(Quad(fL, s.aL, s.bL, 1.0), Quad(fR, s.aR, s.bR, -1.0)))
                    if (f != null) {
                        val band = Color(0xFF000000 or f.color.toLong())
                        // just outside the face, so the wall itself stays as it is
                        val o = d.left() * (side * bw / 2 / ed.scale)
                        val p0 = ed.toScreen(e0 + o); val p1 = ed.toScreen(e1 + o)
                        drawLine(band, p0, p1, bw)
                        drawLine(Color(0x66000000), p0, p1, 0.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
                    }
            }
            // stretches cut away above the stairs: lighter, dashed edge
            crops[s.wall.id]?.forEach { (t0, t1, _) ->
                val d = (s.b - s.a).norm(); val n = d.left() * (s.wall.thickness / 2.0)
                val p0 = s.a + d * t0; val p1 = s.a + d * t1
                val cut = path(listOf(p0 + n, p1 + n, p1 - n, p0 - n))
                drawPath(cut, Color(0xFFC9CDD6))
                drawPath(cut, Color(0xFF6B7080), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))))
            }
        }
        for ((wid, list) in cuts) {
            val wall = plan.wall(wid) ?: continue
            val a = plan.pos(wall.a) ?: continue
            val d = ((plan.pos(wall.b) ?: continue) - a).norm()
            val n = d.left()
            val ht = wall.thickness / 2.0
            for (cut in list) {
                val o = cut.opening
                val on = sel is Sel.OpeningSel && sel.id == o.id
                val p0 = a + d * cut.t0; val p1 = a + d * cut.t1
                drawPath(path(listOf(p0 + n * ht, p1 + n * ht, p1 - n * ht, p0 - n * ht)), Color(0xFFF7F7F3))
                val ink = if (on) accent else Color(0xFF3A3F4B)
                val thin = if (on) 2.5.dp.toPx() else 1.2.dp.toPx()
                if (o.kind == OpeningKind.WINDOW) {
                    drawPath(path(listOf(p0 + n * ht, p1 + n * ht, p1 - n * ht, p0 - n * ht)), ink, style = Stroke(thin))
                    drawLine(ink, ed.toScreen(p0), ed.toScreen(p1), thin)
                } else if (o.kind == OpeningKind.GATE) {
                    drawLine(ink, ed.toScreen(p0 + n * ht), ed.toScreen(p0 - n * ht), thin * 1.5f)
                    drawLine(ink, ed.toScreen(p1 + n * ht), ed.toScreen(p1 - n * ht), thin * 1.5f)
                    val no = P(o.nx, o.ny)
                    gateSymbol(ed, p0 + no * ht, p1 + no * ht, no, o.gate, if (on) accent else Color(0xFF2E3136), 2.dp.toPx())
                } else if (o.kind == OpeningKind.PASSAGE) {
                    // a frame and no leaf: the two jambs
                    drawLine(ink, ed.toScreen(p0 + n * ht), ed.toScreen(p0 - n * ht), thin * 1.5f)
                    drawLine(ink, ed.toScreen(p1 + n * ht), ed.toScreen(p1 - n * ht), thin * 1.5f)
                } else {
                    val no = P(o.nx, o.ny)
                    val along = if (o.hingeEnd) d * -1.0 else d
                    val hinge = (if (o.hingeEnd) p1 else p0) + no * ht
                    val w = cut.t1 - cut.t0
                    drawLine(ink, ed.toScreen(hinge), ed.toScreen(hinge + no * w), thin * 1.5f)
                    val arc = Path()
                    for (k in 0..10) {
                        val ang = Math.PI / 2 * k / 10
                        val q = ed.toScreen(hinge + no * (w * Math.cos(ang)) + along * (w * Math.sin(ang)))
                        if (k == 0) arc.moveTo(q.x, q.y) else arc.lineTo(q.x, q.y)
                    }
                    drawPath(arc, ink, style = Stroke(thin, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                }
            }
        }
        // Pillars, solid; with Wall or Pillar open, their centre lines across the plan as a dashed grid with A, B… / 1, 2… marks.
        if (gridShown && (axes.first.isNotEmpty() || axes.second.isNotEmpty())) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))
            val gc = Color(0xCC2F7FD6)
            val mark = TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            axes.first.forEachIndexed { k, x ->
                val sx = ed.toScreen(P(x.toDouble(), 0.0)).x
                drawLine(gc, Offset(sx, 0f), Offset(sx, size.height), 1.2.dp.toPx(), pathEffect = dash)
                label(tm, ('A' + k % 26).toString(), Offset(sx, size.height - 12.dp.toPx()), mark, gc)
            }
            axes.second.forEachIndexed { k, y ->
                val sy = ed.toScreen(P(0.0, y.toDouble())).y
                drawLine(gc, Offset(0f, sy), Offset(size.width, sy), 1.2.dp.toPx(), pathEffect = dash)
                label(tm, "${k + 1}", Offset(12.dp.toPx(), sy), mark, gc)
            }
        }
        // with the grid shown, the pillars of the floor below as dashed outlines: ours should stand on them
        if (gridShown) for (pl in belowPillars) {
            val bp = path(Pillars.corners(pl))
            drawPath(bp, Color(0x552F7FD6))
            drawPath(bp, Color(0xEE2F7FD6), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
        }
        for (pl in plan.pillars) {
            val on = (sel is Sel.PillarSel && sel.id == pl.id) || (sel is Sel.GroupSel && pl.id in sel.pillars)
            val pp = path(Pillars.corners(pl))
            drawPath(pp, if (on) accent else Color(0xFF2B2F38))
            if (pl.id in offPillars) {
                // not over a pillar of the floor below
                drawPath(pp, Color(0xFFE0353B).copy(alpha = blink))
                val c0 = ed.toScreen(P(pl.x.toDouble(), pl.y.toDouble()))
                drawCircle(Color(0xFFE0353B).copy(alpha = blink), maxOf(pl.w, pl.d) * ed.scale * 0.9f + 6.dp.toPx(), c0, style = Stroke(2.5.dp.toPx()))
            }
            drawPath(pp, Color.White.copy(alpha = 0.6f), style = Stroke(1.dp.toPx()))
            // a cross marks a pillar on building plans
            val c = Pillars.corners(pl)
            if (!pl.round) {
                drawLine(Color.White.copy(alpha = 0.5f), ed.toScreen(c[0]), ed.toScreen(c[2]), 1.dp.toPx())
                drawLine(Color.White.copy(alpha = 0.5f), ed.toScreen(c[1]), ed.toScreen(c[3]), 1.dp.toPx())
            }
        }
        // Stairs on top: they pass over low walls and things under them.
        for (st in plan.stairs) {
            val on = sel is Sel.StairSel && sel.id == st.id
            stairShape(this, ed, Stairs.shape(plan, st), Color(0xE6D9C3A5), Color(0xFF705030), if (on) accent else null)
        }
        for (n in plan.nodes) {
            val o = ed.toScreen(P(n.x.toDouble(), n.y.toDouble()))
            val on = (sel is Sel.Corner && sel.id == n.id) || (sel is Sel.GroupSel && n.id in sel.nodes)
            drawCircle(if (on) accent else Color.White, if (on) 7.dp.toPx() else 3.5.dp.toPx(), o)
            drawCircle(if (on) Color.White else Color(0xFF3A3F4B), if (on) 7.dp.toPx() else 3.5.dp.toPx(), o, style = Stroke(1.5.dp.toPx()))
        }

        // picked on other floors too: rings where they stand
        (sel as? Sel.GroupSel)?.let { g -> vm.root?.let { r ->
            for ((f, s) in g.others) { val fl = r.floor(f)
                val at = fl.nodes.filter { it.id in s.first }.map { P(it.x.toDouble(), it.y.toDouble()) } + fl.pillars.filter { it.id in s.second }.map { P(it.x.toDouble(), it.y.toDouble()) }
                for (q in at) { val o = ed.toScreen(q)
                    drawCircle(accent.copy(alpha = 0.25f), 11.dp.toPx(), o); drawCircle(accent, 11.dp.toPx(), o, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))) }
            }
        } }
        // The group picked by a rectangle, and the rectangle being drawn.
        (sel as? Sel.GroupSel)?.let { g -> groupBounds(plan, g) }?.let { (a, b) ->
            val m = 10.dp.toPx()
            val p0 = ed.toScreen(a); val p1 = ed.toScreen(b)
            drawRect(accent, Offset(p0.x - m, p0.y - m), Size(p1.x - p0.x + 2 * m, p1.y - p0.y + 2 * m),
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))))
        }
        (sel as? Sel.PointsSel)?.let { s -> Arrange.pointsBounds(plan, s.ids) }?.let { (a, b) ->
            val m = 14.dp.toPx()
            val p0 = ed.toScreen(a); val p1 = ed.toScreen(b)
            drawRect(accent, Offset(p0.x - m, p0.y - m), Size(p1.x - p0.x + 2 * m, p1.y - p0.y + 2 * m),
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))))
        }
        ed.box?.let { (a, b) ->
            val p0 = ed.toScreen(P(minOf(a.x, b.x), minOf(a.y, b.y))); val p1 = ed.toScreen(P(maxOf(a.x, b.x), maxOf(a.y, b.y)))
            drawRect(accent.copy(alpha = 0.12f), p0, Size(p1.x - p0.x, p1.y - p0.y))
            drawRect(accent, p0, Size(p1.x - p0.x, p1.y - p0.y), style = Stroke(1.5.dp.toPx()))
        }

        // Wall lengths (centre line, cm) beside each wall that is long enough on screen (View can hide them).
        val small = TextStyle(fontSize = 11.sp, color = Color(0xFF3A3F4B))
        // The selected wall always keeps a live length label while it is dragged/resized, even if general dimensions
        // are hidden in View.
        val dimensionShapes = if (vm.showDims) shapes else (sel as? Sel.WallSel)?.let { pick -> shapes.filter { it.wall.id == pick.id } }.orEmpty()
        for (s in dimensionShapes) {
            val a = ed.toScreen(s.a); val b = ed.toScreen(s.b)
            if ((b - a).getDistance() < 44.dp.toPx()) continue
            val n = (s.b - s.a).norm().left()
            val off = s.wall.thickness / 2f * ed.scale + 10.dp.toPx()
            val c = Offset((a.x + b.x) / 2 + n.x.toFloat() * off, (a.y + b.y) / 2 + n.y.toFloat() * off)
            val hh = s.wall.height?.let { "  ↕" + cm(it.toDouble()) } ?: ""
            label(tm, cm((s.b - s.a).len) + hh, c, small, Color(0xCCF7F7F3))
        }
        // Open places: a pin with the name.
        for (pl in plan.places) {
            val o = ed.toScreen(P(pl.x.toDouble(), pl.y.toDouble()))
            val on = sel is Sel.PlaceSel && sel.id == pl.id
            val r = 8.dp.toPx()
            drawCircle(if (on) accent else Color(0xFF6A4FB3), r, o)
            drawCircle(Color.White, r * 0.4f, o)
            val t = pl.name.ifBlank { pl.type?.label(lang) ?: placeWord }
            label(tm, t, Offset(o.x, o.y + r + 10.dp.toPx()), TextStyle(fontSize = 12.sp, color = Color(0xFF3B2A78), fontWeight = FontWeight.Medium), Color(0xCCF7F7F3))
        }
        // Measurements are visible only while selecting in the drawing tools.
        for (r in rooms) {
            val title = r.info?.name?.takeIf { it.isNotEmpty() } ?: r.info?.type?.label(lang)
            val showSize = ed.category == Tool.WALL && ed.tool == Tool.SELECT && ed.moveMode
            val text = listOfNotNull(title, if (showSize) "${num(r.area)} $roomAreaUnit" else null).joinToString("\n")
            if (text.isNotEmpty()) label(tm, text, ed.toScreen(r.anchor), TextStyle(fontSize = 12.sp, color = Color(0xFF20242C), fontWeight = FontWeight.Medium, textAlign = TextAlign.Center), null)
        }

        // Warm floor: wavy lines over the room.
        if (Layer.WARM_FLOOR !in vm.hidden && (ed.systemsShown || vm.alwaysPipes)) for (r in rooms) if (r.info?.warmFloor == true) {
            val clip = path(r.inner)
            clipPath(clip) {
                val xs = r.inner.map { it.x }; val ys = r.inner.map { it.y }
                var y = ys.min() + 75
                var k = 0
                while (y < ys.max()) {
                    drawLine(Color(0xAAE07A1F), ed.toScreen(P(xs.min(), y)), ed.toScreen(P(xs.max(), y)), 1.5.dp.toPx())
                    val xe = if (k % 2 == 0) xs.max() - 60 else xs.min() + 60
                    drawLine(Color(0xAAE07A1F), ed.toScreen(P(xe, y)), ed.toScreen(P(xe, y + 150)), 1.5.dp.toPx())
                    y += 150; k++
                }
            }
        }
        // House systems.
        for (pipe in plan.pipes) {
            if (pipe.system.layer in vm.hidden) continue
            val on = sel is Sel.PipeSel && sel.id == pipe.id
            // full in Systems; elsewhere thin, and only when "Always show" is on
            val full = ed.systemsShown || on
            if (!full && !vm.alwaysPipes) continue
            val ppts = pipe.points.map { ed.toScreen(P(it.x.toDouble(), it.y.toDouble())) }
            val pcol = Color(0xFF000000 or pipe.system.color.toLong())
            drawPath(roundedPath(ppts, 14.dp.toPx()), if (on) accent else pcol.copy(alpha = if (full) 1f else 0.5f), style = Stroke(if (on) 4.5.dp.toPx() else if (full) 3.dp.toPx() else 1.5.dp.toPx(),
                pathEffect = if (pipe.system == SystemKind.SEWER) PathEffect.dashPathEffect(floatArrayOf(16f, 8f)) else null))
            val size = if (pipe.system == SystemKind.NETWORK) pipe.cable.name.replace('_', ' ') else "Ø${pipe.sizeOrDefault()}"
            if (full && vm.labelPipes) longestMiddle(ppts)?.let { label(tm, (if (pipe.count > 1) "${pipe.count}× " else "") + size, it, TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold), if (on) accent else pcol) }
            if (on) lineHandles(Lines.handles(pipe.points.map { P(it.x.toDouble(), it.y.toDouble()) }, false).map { ed.toScreen(it) }, pipe.points.size, accent)
        }
        // feed cables between boards: across this floor (through walls) from the source, then straight up or down
        // the line in Electrics or with electrics always shown or one of its boards picked; the words only in Electrics or when picked
        if (Layer.ELECTRIC !in vm.hidden) {
            val fc = Color(0xFF8E6CC9)
            val fstyle = TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            val pickedId = (sel as? Sel.PointSel)?.id
            for (f in feeds) {
                val picked = (pickedId != null && ((f.from.floor == vm.floor && f.from.point.id == pickedId) || (f.to.floor == vm.floor && f.to.point.id == pickedId))) ||
                    (sel is Sel.FeedSel && sel.toFloor == f.to.floor && sel.toId == f.to.point.id)
                if (!(ed.electricsShown || vm.alwaysElectric || picked)) continue
                val words = ed.electricsShown || picked
                if (f.from.floor == vm.floor) {
                    val pts = f.path.map { ed.toScreen(it) }
                    val pth = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
                    val feedOn = sel is Sel.FeedSel && sel.toFloor == f.to.floor && sel.toId == f.to.point.id
                    drawPath(pth, if (feedOn) accent else fc, style = Stroke(if (feedOn) 5.dp.toPx() else 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 5.dp.toPx()))))
                    if (feedOn) lineHandles(Lines.handles(f.path, true).map { ed.toScreen(it) }, f.path.size - 2, accent)
                    val end = pts.last()
                    if (f.dir != 0) {
                        drawCircle(fc, 9.dp.toPx(), end); drawCircle(Color.White, 9.dp.toPx(), end, style = Stroke(1.5.dp.toPx()))
                        label(tm, if (f.dir > 0) "↑" else "↓", end, fstyle, null)
                    }
                    // the label on the longest run
                    val k = (1 until pts.size).maxByOrNull { (pts[it] - pts[it - 1]).getDistance() } ?: 1
                    val mid = (pts[k - 1] + pts[k]) / 2f
                    if (words && (vm.labelCables || vm.labelLengths)) label(tm, (if (f.dir > 0) "↑ " else if (f.dir < 0) "↓ " else "") + Boards.name(f.to, ruBoards) +
                        (if (vm.labelCables) " · ${f.cores}×${mmTxt(f.mm2)}" else "") + (if (vm.labelLengths) " · ${num(f.length / 1000.0, 1)} m" else ""),
                        mid, fstyle, fc)
                } else if (f.to.floor == vm.floor) {
                    // fed from another floor: the cable comes straight up or down to this board
                    val o = ed.toScreen(P(f.to.point.x.toDouble(), f.to.point.y.toDouble()))
                    if (words && vm.labelCables) label(tm, (if (f.dir > 0) "↓ " else "↑ ") + Boards.name(f.from, ruBoards) + " · ${f.cores}×${mmTxt(f.mm2)}",
                        o + Offset(0f, -22.dp.toPx()), fstyle, fc)
                }
            }
        }
        // Cross-floor cables may terminate at a board, a junction or a low-current box.  Their horizontal run is
        // shown on its source floor; the marked end is where it changes level through wall, floor or ceiling.
        if (Layer.ELECTRIC !in vm.hidden && (ed.electricsShown || vm.alwaysElectric)) for (c in crossCables.filter { it.fromFloor == vm.floor }) {
            val root = vm.root ?: continue
            val path = CrossCables.path(root, c)
            if (path.size < 2) continue
            val color = when (c.cable) {
                CommCable.POWER -> Color(0xFF8E6CC9)
                CommCable.FIBER -> Color(0xFFDA4CE6)
                CommCable.COAX -> Color(0xFF4E77C9)
                CommCable.ALARM, CommCable.INTERCOM -> Color(0xFFC0583A)
                else -> Color(0xFF2E9E5B)
            }
            val pts = path.map(ed::toScreen)
            drawPath(Path().apply { pts.forEachIndexed { i, q -> if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) } }, color,
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 5.dp.toPx()))))
            val end = pts.last()
            drawCircle(color, 9.dp.toPx(), end); drawCircle(Color.White, 9.dp.toPx(), end, style = Stroke(1.5.dp.toPx()))
            label(tm, "↕", end, TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold), null)
            if (vm.labelCables || vm.labelLengths) longestMiddle(pts)?.let { at ->
                val name = c.cable.name.replace('_', ' ')
                val len = (CrossCables.horizontal(root, c) + CrossCables.vertical(root, c)) / 1000.0
                label(tm, "↕ $name" + if (vm.labelLengths) " · ${num(len, 1)} m" else "", at,
                    TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold), color)
            }
        }
        // The vertical channel itself is deliberately visible on every floor in Electrics, not just on the floor
        // where a cable happens to run horizontally.  ↑/↓ tells whether it continues above/below this level.
        if (Layer.ELECTRIC !in vm.hidden && (ed.electricsShown || vm.alwaysElectric)) {
            val root = vm.root
            val arrow = root?.floorRange?.let { fs -> (if (fs.any { it > vm.floor }) "↑" else "") + (if (fs.any { it < vm.floor }) "↓" else "") }.orEmpty()
            for ((p, count) in riserColumns) {
                val o = ed.toScreen(p)
                drawCircle(Color(0x228E6CC9), 14.dp.toPx(), o)
                drawCircle(Color(0xFF8E6CC9), 10.dp.toPx(), o, style = Stroke(2.dp.toPx()))
                label(tm, arrow.ifBlank { "↕" }, o, TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold), Color(0xFF8E6CC9))
                if (ed.electricsShown && count > 1) label(tm, "↕$count", o + Offset(0f, 19.dp.toPx()), TextStyle(fontSize = 9.sp, color = Color.White), Color(0xCC8E6CC9))
            }
        }
        // putting a gate: the stretch of fence it will take, under the finger
        ed.gateAim?.let { aim -> (sel as? Sel.FenceSel)?.let { s -> plan.fences.firstOrNull { it.id == s.id } }?.let { f ->
            val fp = f.points.map { P(it.x.toDouble(), it.y.toDouble()) }
            val along = Fences.project(f, aim).first
            val half = (ed.gatePlacing?.width ?: ed.gateIndex?.let { f.gates.getOrNull(it) }?.width ?: 1000) / 2.0
            val total = Fences.length(f)
            val a = Fences.at(fp, (along - half).coerceAtLeast(0.0)).first; val b = Fences.at(fp, (along + half).coerceAtMost(total)).first
            drawLine(Color(0xFFE08A00), ed.toScreen(a), ed.toScreen(b), 8.dp.toPx(), cap = StrokeCap.Round)
        } }
        if (Layer.FENCES !in vm.hidden) for (f in plan.fences) {
            val on = sel is Sel.FenceSel && sel.id == f.id
            // the fence between its gates
            val pth = Path().apply { for (piece in Fences.pieces(f)) piece.forEachIndexed { i, q -> val o = ed.toScreen(q); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
            val wpx = maxOf(3.dp.toPx(), (if (f.kind == FenceKind.HEDGE) 600f else if (f.kind == FenceKind.BRICK) 250f else if (f.kind == FenceKind.STONE) f.thickness.toFloat() else 60f) * ed.scale)
            drawPath(pth, if (on) accent else Color(0xFF000000 or (Fences.facingOf(f)?.color ?: f.kind.color).toLong()), style = Stroke(wpx,
                pathEffect = when (f.kind) { FenceKind.PICKET -> PathEffect.dashPathEffect(floatArrayOf(6f, 5f)); FenceKind.MESH -> PathEffect.dashPathEffect(floatArrayOf(2f, 4f)); else -> null }))
            // posts
            for (part in Fences.parts(f).filter { it.z0 == 0.0 && it.z1 > f.height }) drawPath(path(part.corners), Color(0xFF4A4D52))
            // gates: leaves drawn swinging open (a quarter circle each), a sliding one with the way it runs
            val fpts = f.points.map { P(it.x.toDouble(), it.y.toDouble()) }
            for ((gi, g) in f.gates.withIndex()) {
                val (g0, g1) = Fences.openings(f)[gi]
                val (pa, d) = Fences.at(fpts, g0); val pb = Fences.at(fpts, g1).first
                val gcol = if (on && ed.gateIndex == gi) Color(0xFFE08A00) else if (on) accent else Color(0xFF2E3136)
                val n = d.left()
                val sw = 2.dp.toPx()
                gateSymbol(ed, pa, pb, n, g.kind, gcol, sw)
                for (q in listOf(pa, pb)) drawCircle(gcol, 4.dp.toPx(), ed.toScreen(q))
            }
            if (on) for ((i, hp) in Fences.handles(f).withIndex()) {
                val q = ed.toScreen(hp); val big = i < f.points.size
                drawCircle(Color.White, if (big) 8.dp.toPx() else 5.dp.toPx(), q)
                drawCircle(accent, if (big) 8.dp.toPx() else 5.dp.toPx(), q, style = Stroke(2.5.dp.toPx()))
            }
        }
        (sel as? Sel.RoadSel)?.let { rs -> plan.roads.firstOrNull { it.id == rs.id } }?.let { r ->
            for ((i, hp) in Fences.handles(roadLine(r)).withIndex()) {
                val q = ed.toScreen(hp); val big = i < r.points.size
                drawCircle(Color.White, if (big) 8.dp.toPx() else 5.dp.toPx(), q)
                drawCircle(if (i in ed.yardPointIds) Color(0xFF2E7D32) else accent, if (big) 8.dp.toPx() else 5.dp.toPx(), q, style = Stroke(2.5.dp.toPx()))
            }
        }
        if (ed.pipePts.isNotEmpty()) {
            val col = if (ed.tool in setOf(Tool.ROAD, Tool.YARD)) Color(0xFF000000 or ed.surface.color.toLong()) else if (ed.tool == Tool.FENCE) Color(0xFF000000 or ed.fenceKind.color.toLong()) else Color(0xFF000000 or (ed.system ?: SystemKind.WATER_COLD).color.toLong())
            drawPath(roundedPath(ed.pipePts.map { ed.toScreen(it) }, 14.dp.toPx()), col, style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))))
            for (q in ed.pipePts) drawCircle(col, 4.dp.toPx(), ed.toScreen(q))
        }
        // Cables: from the device's icon down to the wall, along their way; cables sharing a run lie side by side.
        val shownRoutes = if (Layer.ELECTRIC in vm.hidden) emptyList() else plan.routes.filter { r ->
            ed.electricsShown || vm.alwaysElectric || (sel is Sel.RouteSel && sel.id == r.id) }
        val drawn = shownRoutes.mapNotNull { r -> routeDrawn(plan, r, placed)?.let { r to it } }
        val lanes = laneOffsets(drawn.map { it.second }, if (ed.electricsShown) 5.dp.toPx() / ed.scale else 3.dp.toPx() / ed.scale)
        // Selecting a wire or electrical device puts its complete connected line in focus.  Everything else stays
        // visible as context, but is deliberately quiet so the actual circuit can be read at a glance.
        val selectedPoint = (sel as? Sel.PointSel)?.let { s -> plan.points.firstOrNull { it.id == s.id } }
        val selectedSwitch = selectedPoint?.takeIf { it.kind == PointKind.SWITCH }
        // A stale one-way pairId must not cause another switch or its lamps to appear active.  Pairing is real only
        // when both switches name each other.
        val pairedSwitchId = selectedSwitch?.pairId?.takeIf { pid -> plan.points.firstOrNull { it.id == pid && it.kind == PointKind.SWITCH }?.pairId == selectedSwitch.id }
        val switchLamps = selectedSwitch?.let { sw -> plan.points.filter { it.kind == PointKind.LAMP && (it.switchId == sw.id || it.switchId == pairedSwitchId) } }.orEmpty()
        val focusRouteIds: Set<Int>? = when (sel) {
            // A lamp is controlled by its switch, so show only that physical lamp → switch run, never its board feed.
            is Sel.PointSel -> selectedPoint?.takeIf { it.kind == PointKind.LAMP }?.let { lamp -> (lamp.sourceId ?: lamp.switchId)?.let { source -> Circuits.path(plan, sel.id, source).map { it.id }.toSet() } }
                ?: selectedSwitch?.let { sw ->
                    (Circuits.toBoard(plan, sw.id) + switchLamps.flatMap { lamp -> Circuits.path(plan, lamp.id, sw.id) }).map { it.id }.toSet()
                }
                ?: Circuits.toBoard(plan, sel.id).map { it.id }.toSet()
            // A picked cable intentionally shows its complete line, so its connected devices are clear.
            is Sel.RouteSel -> Circuits.lineOf(plan, sel.id).map { it.id }.toSet()
            else -> null
        }
        val routePointIds = focusRouteIds?.flatMap { id -> plan.routes.firstOrNull { it.id == id }?.let { listOf(it.from, it.to) }.orEmpty() }?.toSet() ?: emptySet()
        // A cable may physically pass through another switch, but it is not a control peer unless it is a two-way pair.
        val focusedIds = if (selectedSwitch != null) {
            routePointIds.filter { id -> plan.points.firstOrNull { it.id == id }?.kind != PointKind.SWITCH || id == selectedSwitch.id || id == pairedSwitchId }.toSet() +
                switchLamps.map { it.id } + selectedSwitch.id
        } else routePointIds + ((sel as? Sel.PointSel)?.id?.let(::setOf) ?: emptySet())
        fun isFocusedRoute(r: Route) = focusRouteIds == null || r.id in focusRouteIds
        // a device picked: the cables its power comes along, from the board
        val pickedDevice = selectedPoint?.takeIf { Circuits.kindOf(it.kind) != null }
        val toBoard = pickedDevice?.let { Circuits.toBoard(plan, it.id) } ?: emptyList()
        val pathIds = if (pickedDevice?.kind == PointKind.LAMP) emptySet() else toBoard.map { it.id }.toSet()
        // wire sizes: written once for cables of one size joined together, a name always
        val labels = ArrayList<Triple<String, Offset, Color>>()
        val sizeShown = HashMap<String, MutableList<Pair<Set<Int>, Offset>>>()
        val lineNos = Circuits.numbers(plan)
        val nosShown = HashSet<Int>()
        for ((ri, rp) in drawn.withIndex().sortedByDescending { (_, rp) -> rp.second.zipWithNext().maxOfOrNull { (a, b) -> (b - a).len } ?: 0.0 }) {
            val (r, dpts) = rp
            val pts = routePlan(plan, r) ?: continue
            val on = sel is Sel.RouteSel && sel.id == r.id
            val related = isFocusedRoute(r)
            val spts = offsetLine(dpts, lanes[ri]).map { ed.toScreen(it) }
            // in Electrics each wire size has its colour; elsewhere all electric cables are red
            val ink = if (ed.electricsShown) (if (r.volts == 110) VOLT110 else mm2Color(r.mm2)) else ELECTRIC_RED
            if (r.id in pathIds) drawPath(roundedPath(spts, 14.dp.toPx()), Color(0xFFFFC21A).copy(alpha = 0.35f + 0.4f * blink), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            drawPath(roundedPath(spts, 14.dp.toPx()), if (on) accent else ink.copy(alpha = if (!related) 0.18f else if (ed.electricsShown || r.id in pathIds) 1f else 0.45f), style = Stroke(if (on) 4.dp.toPx() else if (!related) 1.dp.toPx() else if (ed.electricsShown || r.id in pathIds) 2.5.dp.toPx() else 1.5.dp.toPx(),
                pathEffect = if (r.level == RouteLevel.FLOOR) PathEffect.dashPathEffect(floatArrayOf(14f, 8f)) else null))
            if (ed.electricsShown && !on && (vm.labelCables || vm.labelNames && r.label.isNotBlank())) longestMiddle(spts)?.let { at ->
                val size = "${r.cores}×${mm2Text(r.mm2)}"
                val comp = Circuits.component(plan, r.from) + Circuits.component(plan, r.to)
                val done = sizeShown.getOrPut(size) { ArrayList() }
                val repeat = done.any { (c, o) -> c.intersect(comp).any { id -> plan.points.firstOrNull { it.id == id }?.kind.let { it != PointKind.BOARD && it != PointKind.RISER } } || (o - at).getDistance() < 90.dp.toPx() }
                // the line's number on its board once per line, its name when it has one, the size once per run of one size
                // numbers and sizes with "Cable sizes", the name with "Cable names" (each can be hidden)
                val no = lineNos[r.id]?.takeIf { it !in nosShown && vm.labelCables }
                no?.let { nosShown += it }
                val parts = listOfNotNull(no?.let { "№$it" }, r.label.takeIf { it.isNotBlank() && vm.labelNames }, size.takeIf { !repeat && vm.labelCables })
                if (parts.isNotEmpty()) labels += Triple(parts.joinToString(" · "), at, ink)
                if (!repeat) done += comp to at
            }
            if (on && ed.editRouteId == r.id) lineHandles(Lines.handles(pts, true).map { ed.toScreen(it) }, pts.size - 2, accent)
        }
        for ((t, at, ink) in labels) label(tm, t, at, TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold), ink)
        // the board at the end of the picked device's cables: named, with how much cable it takes to get there
        if (toBoard.isNotEmpty() && pickedDevice?.kind != PointKind.LAMP) {
            val endId = toBoard.last().let { r -> listOf(r.from, r.to).first { id -> plan.points.firstOrNull { it.id == id }?.kind.let { it == PointKind.BOARD || it == PointKind.RISER } } }
            placed.firstOrNull { it.point.id == endId }?.let { b ->
                val o = ed.toScreen(symbolAt(b))
                // Show both dimensions: plan distance is what is visible against the grid, while vertical rises and
                // drops make up the rest of the real cable that must be bought.
                val planMm = toBoard.sumOf { Cables.planLength(plan, it) }
                val verticalMm = toBoard.sumOf { Cables.vertical(plan, it) }
                val mm = planMm + verticalMm
                val name = Boards.all(vm.root ?: plan).firstOrNull { it.floor == vm.floor && it.point.id == endId }?.let { Boards.name(it, ruBoards) } ?: "⇅"
                drawCircle(Color(0xFFFFC21A).copy(alpha = blink * 0.5f), 22.dp.toPx(), o)
                label(tm, "⚡ $name" + (if (vm.labelLengths) " · ${num(planMm / 1000, 1)} + ↕${num(verticalMm / 1000, 1)} = ${num(mm / 1000, 1)} m" else ""), Offset(o.x, o.y - 30.dp.toPx()), TextStyle(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold), Color(0xFFE08A00))
            }
        }
        // A socket on a cable that goes on: its drop meets the cable in a junction box.
        for (pp in placed) {
            if (!Circuits.tapped(plan, pp.point)) continue
            if (shownRoutes.none { it.from == pp.point.id || it.to == pp.point.id }) continue
            val o = ed.toScreen(P(pp.point.x.toDouble(), pp.point.y.toDouble()))
            val rc = plan.routes.first { it.from == pp.point.id || it.to == pp.point.id }
            val jr = if (ed.electricsShown) 6.dp.toPx() else 3.5.dp.toPx()
            drawCircle(if (ed.electricsShown) mm2Color(rc.mm2) else ELECTRIC_RED, jr, o)
            drawCircle(Color.White, jr * 0.45f, o)
            drawCircle(Color(0xFF30343C), jr, o, style = Stroke(1.2.dp.toPx()))
        }
        ed.routeFrom?.let { from ->
            val start = plan.points.firstOrNull { it.id == from }?.let { P(it.x.toDouble(), it.y.toDouble()) }
            if (start != null) {
                val pts = listOf(start) + ed.routeVia
                drawPath(roundedPath(pts.map { ed.toScreen(it) }, 14.dp.toPx()), accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                for (q in pts) drawCircle(accent, 4.dp.toPx(), ed.toScreen(q))
            }
        }
        ed.stroke?.let { st ->
            // the line as the finger draws it, and the neat one it becomes
            drawPath(Path().apply { st.forEachIndexed { i, q -> val o = ed.toScreen(q); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }, accent.copy(alpha = 0.35f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
            if (st.size >= 2) drawPath(roundedPath(Lines.orthogonal(st, (16.dp.toPx() / ed.scale).toDouble().coerceAtLeast(80.0)).map { ed.toScreen(it) }, 14.dp.toPx()), accent,
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        for (pp in if (Layer.ELECTRIC in vm.hidden || !ed.electricsShown) emptyList() else placed) {
            val sw = pp.point.switchId?.let { id -> placed.firstOrNull { it.point.id == id } } ?: continue
            // Once a real routed circuit exists, do not draw a misleading straight control-link over it.
            if (sw.point.id !in Circuits.component(plan, pp.point.id))
                drawLine(Color(0xFF2F7FD6), ed.toScreen(symbolAt(sw)), ed.toScreen(pp.face), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        }
        for (pp in if (Layer.ELECTRIC in vm.hidden) emptyList() else placed) {
            val o = ed.toScreen(symbolAt(pp))
            val on = (sel is Sel.PointSel && sel.id == pp.point.id) || (sel is Sel.PointsSel && pp.point.id in sel.ids)
            // While choosing the companion of a two-way/impulse switch, every other switch is a possible answer.
            // Keep those candidates prominent even if the selected switch has put another circuit in focus.
            val pairCandidate = ed.pairing != null && pp.point.kind == PointKind.SWITCH && pp.point.id != ed.pairing
            val related = pairCandidate || focusRouteIds == null || pp.point.id in focusedIds
            val inactiveFocus = !pairCandidate && focusRouteIds != null && !related
            val quiet = if (related) 1f else 0.7f
            // full size in Electrics; elsewhere a small icon, and only when "Always show" is on
            val small = !ed.electricsShown && !on && ed.feeds != pp.point.id
            if (small && !vm.alwaysElectric) continue
            if (pp.point.kind == PointKind.JUNCTION) {
                // where cables join: a round box in the cable's colour
                val jr0 = if (small) 3.5.dp.toPx() else 6.dp.toPx()
                val jr = if (inactiveFocus) jr0 * 0.7f else jr0
                val rc = plan.routes.firstOrNull { it.from == pp.point.id || it.to == pp.point.id }
                drawCircle((if (ed.electricsShown && rc != null) mm2Color(rc.mm2) else ELECTRIC_RED).copy(alpha = quiet), jr, o)
                drawCircle(Color.White.copy(alpha = quiet), jr * 0.45f, o)
                drawCircle((if (on) accent else Color(0xFF30343C)).copy(alpha = quiet), jr, o, style = Stroke(if (on) 3.dp.toPx() else 1.2.dp.toPx()))
                continue
            }
            // lamps, sockets and switches small; boards and appliances larger
            val big = pp.point.kind == PointKind.BOARD || pp.point.kind == PointKind.LOW_CURRENT_BOARD || pp.point.kind == PointKind.APPLIANCE || pp.point.kind == PointKind.RISER
            val r0 = if (small) (if (big) 6.5 else 4.5).dp.toPx() else if (big) maxOf(9.dp.toPx(), 110 * ed.scale) else (65 * ed.scale).coerceIn(6.dp.toPx(), 11.dp.toPx())
            val r = (if (inactiveFocus) r0 * 0.7f else r0) * if (pairCandidate) 1.45f else 1f
            val color = when (pp.point.kind) {
                PointKind.SOCKET -> Color(0xFFF08A24); PointKind.SWITCH -> Color(0xFF2F7FD6); PointKind.LAMP -> Color(0xFFFFD23F)
                PointKind.BOARD -> Color(0xFF8D97A5); PointKind.LOW_CURRENT_BOARD -> Color(0xFF325A7A); PointKind.LOW_CURRENT_BOX -> Color(0xFF2E9E5B)
                PointKind.APPLIANCE -> Color(0xFFC0392B); PointKind.RISER -> Color(0xFF8E6CC9); PointKind.JUNCTION -> ELECTRIC_RED
            }
            if (ed.feeds == pp.point.id) {
                // the board that feeds the point just placed
                drawCircle(Color(0xFFFFC21A).copy(alpha = blink * 0.55f), r * 2.6f, o)
                drawCircle(Color(0xFFE08A00).copy(alpha = blink), r * 2.6f, o, style = Stroke(3.dp.toPx()))
                Boards.all(vm.root ?: plan).firstOrNull { it.floor == vm.floor && it.point.id == pp.point.id }?.let { b ->
                    label(tm, "⚡ " + Boards.name(b, ruBoards), Offset(o.x, o.y - r * 2.6f - 12.dp.toPx()), TextStyle(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold), Color(0xFFE08A00))
                }
            }
            // a switch with no lamps on it yet: a yellow ring blinks round it
            // While inspecting a circuit, only the selected/related switch may receive an attention ring.
            if (pp.point.kind == PointKind.SWITCH && ed.electricsShown && related && !Circuits.hasLamps(plan, pp.point)) {
                drawCircle(Color(0xFFFFC21A).copy(alpha = 0.25f + 0.5f * blink), r * 2.2f, o)
                drawCircle(Color(0xFFE0A800).copy(alpha = blink), r * 2.2f, o, style = Stroke(2.5.dp.toPx()))
            }
            drawCircle(color.copy(alpha = quiet), r, o)
            val border = if ((on || pairCandidate) && pp.point.kind == PointKind.SWITCH) Color(0xFFFFC21A) else if (on) accent else Color(0xFF30343C)
            drawCircle(border.copy(alpha = quiet), r, o, style = Stroke(if (on) 3.dp.toPx() else 1.2.dp.toPx()))
            val ink = Color(0xFF30343C).copy(alpha = quiet)
            when (pp.point.kind) {
                PointKind.SOCKET -> { drawCircle(ink, r * 0.16f, Offset(o.x - r * 0.38f, o.y)); drawCircle(ink, r * 0.16f, Offset(o.x + r * 0.38f, o.y)) }
                PointKind.SWITCH -> drawLine(ink, Offset(o.x - r * 0.35f, o.y + r * 0.45f), Offset(o.x + r * 0.35f, o.y - r * 0.45f), 2.dp.toPx())
                PointKind.BOARD, PointKind.LOW_CURRENT_BOARD -> drawRect(ink, Offset(o.x - r * 0.45f, o.y - r * 0.5f), Size(r * 0.9f, r), style = Stroke(1.5.dp.toPx()))
                PointKind.LOW_CURRENT_BOX -> drawCircle(Color.White.copy(alpha = quiet), r * 0.36f, o)
                PointKind.RISER -> {
                    drawLine(Color.White, Offset(o.x, o.y - r * 0.55f), Offset(o.x, o.y + r * 0.55f), 2.dp.toPx())
                    drawLine(Color.White, Offset(o.x, o.y - r * 0.55f), Offset(o.x - r * 0.3f, o.y - r * 0.2f), 2.dp.toPx())
                    drawLine(Color.White, Offset(o.x, o.y + r * 0.55f), Offset(o.x + r * 0.3f, o.y + r * 0.2f), 2.dp.toPx())
                }
                PointKind.APPLIANCE -> {
                    val zig = Path().apply { moveTo(o.x + r * 0.15f, o.y - r * 0.6f); lineTo(o.x - r * 0.3f, o.y + r * 0.05f); lineTo(o.x + r * 0.1f, o.y + r * 0.05f); lineTo(o.x - r * 0.15f, o.y + r * 0.6f) }
                    drawPath(zig, Color.White, style = Stroke(2.dp.toPx()))
                }
                PointKind.JUNCTION -> {}
                PointKind.LAMP -> {
                    val k = r * 0.5f
                    drawLine(ink, Offset(o.x - k, o.y - k), Offset(o.x + k, o.y + k), 1.5.dp.toPx())
                    drawLine(ink, Offset(o.x - k, o.y + k), Offset(o.x + k, o.y - k), 1.5.dp.toPx())
                }
            }
            if (small) continue
            if (pp.point.kind == PointKind.APPLIANCE && pp.point.kw > 0 && vm.labelPower)
                label(tm, num(pp.point.kw, 1) + kw, Offset(o.x + pp.normal.x.toFloat() * (r + 16.dp.toPx()), o.y + pp.normal.y.toFloat() * (r + 16.dp.toPx())), TextStyle(fontSize = 10.sp, color = ink, fontWeight = FontWeight.Bold), Color(0xCCF7F7F3))
            if ((pp.point.kind == PointKind.SOCKET || pp.point.kind == PointKind.SWITCH) && pp.point.count > 1 && vm.labelPower)
                label(tm, "×${pp.point.count}", Offset(o.x + pp.normal.x.toFloat() * (r + 9.dp.toPx()), o.y + pp.normal.y.toFloat() * (r + 9.dp.toPx())), TextStyle(fontSize = 10.sp, color = ink, fontWeight = FontWeight.Bold), null)
        }

        // Platforms: a deck-coloured slab with its height; the railing sides drawn thick.
        for (pf in if (ed.electricsShown || ed.systemsShown) emptyList() else plan.platforms) {
            val on = sel is Sel.PlatformSel && sel.id == pf.id
            val a = ed.toScreen(P(pf.x1.toDouble(), pf.y1.toDouble())); val b = ed.toScreen(P(pf.x2.toDouble(), pf.y2.toDouble()))
            val tl = Offset(minOf(a.x, b.x), minOf(a.y, b.y)); val sz = Size(abs(b.x - a.x), abs(b.y - a.y))
            drawRect(Color(0x55B49A6A), tl, sz)
            // deck boards
            val step = maxOf(150 * ed.scale, 4.dp.toPx())
            var yy = tl.y + step
            while (yy < tl.y + sz.height) { drawLine(Color(0x33705A40), Offset(tl.x, yy), Offset(tl.x + sz.width, yy), 1.dp.toPx()); yy += step }
            drawRect(if (on) accent else Color(0xFF7A6248), tl, sz, style = Stroke(if (on) 2.5.dp.toPx() else 1.5.dp.toPx()))
            if (pf.railing) for ((p, q) in Platforms.openSides(plan, pf)) drawLine(Color(0xFF3F4348), ed.toScreen(p), ed.toScreen(q), 4.dp.toPx())
            label(tm, "↥ " + cm(pf.height.toDouble()), Offset(tl.x + sz.width / 2, tl.y + sz.height / 2),
                TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold), Color(0xFF7A6248))
            if (on) for ((i, hp) in Roofs.handlesRect(pf.x1, pf.y1, pf.x2, pf.y2).withIndex()) {
                val q = ed.toScreen(hp)
                drawCircle(Color.White, if (i < 4) 8.dp.toPx() else 6.dp.toPx(), q)
                drawCircle(accent, if (i < 4) 8.dp.toPx() else 6.dp.toPx(), q, style = Stroke(2.5.dp.toPx()))
            }
        }

        // Roofs over everything: outline of the eaves, ridge and hips, dashed dark red.
        // (not in Electrics or Systems: they would cover what is drawn there)
        if (Layer.ROOF !in vm.hidden && !ed.electricsShown && !ed.systemsShown) for (rf in plan.roofs) {
            val on = sel is Sel.RoofSel && sel.id == rf.id
            val ink = if (on) accent else Color(0xFFA0463A)
            for ((a, b) in roofLines(rf)) drawLine(ink, ed.toScreen(a), ed.toScreen(b), if (on) 3.dp.toPx() else 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 7f)))
            // where the rain runs: a blue arrow down each slope
            val water = Color(0xFF1E7FD0)
            val len = maxOf(minOf(rf.x2 - rf.x1, rf.y2 - rf.y1) * 0.22f * ed.scale, 22.dp.toPx())
            for ((c, d) in Roofs.flows(rf)) {
                val o = ed.toScreen(c)
                val dir = Offset(d.x.toFloat(), d.y.toFloat())
                val tail = o - dir * (len / 2); val head = o + dir * (len / 2)
                val side = Offset(-dir.y, dir.x) * (len * 0.22f)
                drawLine(water.copy(alpha = if (on) 1f else 0.7f), tail, head, 3.dp.toPx(), cap = StrokeCap.Round)
                drawPath(Path().apply {
                    moveTo(head.x + dir.x * len * 0.12f, head.y + dir.y * len * 0.12f)
                    lineTo(head.x - dir.x * len * 0.25f + side.x, head.y - dir.y * len * 0.25f + side.y)
                    lineTo(head.x - dir.x * len * 0.25f - side.x, head.y - dir.y * len * 0.25f - side.y); close()
                }, water.copy(alpha = if (on) 1f else 0.7f))
            }
            if (on) {
                // the wall rectangle the roof rests on, with handles to resize it
                val a = ed.toScreen(P(rf.x1.toDouble(), rf.y1.toDouble())); val b = ed.toScreen(P(rf.x2.toDouble(), rf.y2.toDouble()))
                drawRect(accent.copy(alpha = 0.08f), Offset(minOf(a.x, b.x), minOf(a.y, b.y)), Size(abs(b.x - a.x), abs(b.y - a.y)))
                drawRect(accent, Offset(minOf(a.x, b.x), minOf(a.y, b.y)), Size(abs(b.x - a.x), abs(b.y - a.y)), style = Stroke(1.5.dp.toPx()))
                for ((i, hp) in Roofs.handles(rf).withIndex()) {
                    val q = ed.toScreen(hp)
                    drawCircle(Color.White, if (i < 4) 8.dp.toPx() else 6.dp.toPx(), q)
                    drawCircle(accent, if (i < 4) 8.dp.toPx() else 6.dp.toPx(), q, style = Stroke(2.5.dp.toPx()))
                }
                label(tm, cm((rf.x2 - rf.x1).toDouble()) + " × " + cm((rf.y2 - rf.y1).toDouble()),
                    Offset((a.x + b.x) / 2, minOf(a.y, b.y) - 18.dp.toPx()), TextStyle(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold), accent)
            }
        }
        // Select with one kind of thing to pick: the rest pales (while nothing is picked), that kind is marked out.
        if (ed.tool == Tool.SELECT && ed.selectOnly != ed.category) ed.scope?.let { kind ->
            if (sel == null) drawRect(Color(0xFFF7F7F3).copy(alpha = 0.55f), size = size)
            val fill = accent.copy(alpha = 0.3f)
            val line = Stroke(2.dp.toPx())
            fun mark(pts: List<P>, filled: Boolean = true) { val pp = path(pts); if (filled) drawPath(pp, fill); drawPath(pp, accent, style = line) }
            when (kind) {
                Tool.WALL -> for (sh in shapes) mark(sh.outline)
                Tool.PILLAR -> for (pl in plan.pillars) mark(Pillars.corners(pl))
                Tool.ROOM -> for (r in rooms) mark(r.polygon)
                Tool.DOOR -> for ((wid, list) in cuts) {
                    val wall = plan.wall(wid) ?: continue
                    val a = plan.pos(wall.a) ?: continue; val d = ((plan.pos(wall.b) ?: continue) - a).norm(); val n = d.left() * (wall.thickness / 2.0 + 40)
                    for (c in list) { val p0 = a + d * c.t0; val p1 = a + d * c.t1; mark(listOf(p0 + n, p1 + n, p1 - n, p0 - n)) }
                }
                Tool.STAIRS -> for (st in plan.stairs) Stairs.shape(plan, st).let { shp -> for (f in shp.flights) mark(f.corners); for (l in shp.landings) mark(l.corners) }
                Tool.ROOF -> for (rf in plan.roofs) mark(listOf(P(rf.x1.toDouble(), rf.y1.toDouble()), P(rf.x2.toDouble(), rf.y1.toDouble()), P(rf.x2.toDouble(), rf.y2.toDouble()), P(rf.x1.toDouble(), rf.y2.toDouble())))
                Tool.PLATFORM -> for (pf in plan.platforms) mark(listOf(P(pf.x1.toDouble(), pf.y1.toDouble()), P(pf.x2.toDouble(), pf.y1.toDouble()), P(pf.x2.toDouble(), pf.y2.toDouble()), P(pf.x1.toDouble(), pf.y2.toDouble())))
                Tool.FENCE -> for (f in plan.fences) for (piece in Fences.pieces(f))
                    drawPath(Path().apply { piece.forEachIndexed { i, q -> val o = ed.toScreen(q); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }, accent, style = Stroke(maxOf(5.dp.toPx(), 80f * ed.scale)))
                Tool.INTERIOR -> for (i in plan.items) mark(Interior.corners(i))
                Tool.SYSTEMS -> for (pp in plan.pipes) drawPath(Path().apply { pp.points.forEachIndexed { i, n -> val o = ed.toScreen(P(n.x.toDouble(), n.y.toDouble())); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }, accent, style = Stroke(4.dp.toPx()))
                Tool.ELECTRIC -> for (pp in placed) { val o = ed.toScreen(symbolAt(pp)); drawCircle(fill, 14.dp.toPx(), o); drawCircle(accent, 14.dp.toPx(), o, style = line) }
                Tool.PLACE -> for (pl in plan.places) { val o = ed.toScreen(P(pl.x.toDouble(), pl.y.toDouble())); drawCircle(fill, 14.dp.toPx(), o); drawCircle(accent, 14.dp.toPx(), o, style = line) }
                else -> {}
            }
        }
        ed.preview?.let { (p, q) ->
            val a = ed.toScreen(p); val b = ed.toScreen(q)
            val big = TextStyle(fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            if (ed.tool == Tool.ROOM || ed.tool == Tool.ROOF || ed.tool == Tool.PLATFORM) {
                drawRect(accent.copy(alpha = 0.15f), Offset(minOf(a.x, b.x), minOf(a.y, b.y)), Size(abs(b.x - a.x), abs(b.y - a.y)))
                drawRect(accent, Offset(minOf(a.x, b.x), minOf(a.y, b.y)), Size(abs(b.x - a.x), abs(b.y - a.y)), style = Stroke(3.dp.toPx()))
                label(tm, cm(abs(q.x - p.x)) + " × " + cm(abs(q.y - p.y)), Offset((a.x + b.x) / 2, (a.y + b.y) / 2), big, accent)
            } else {
                drawLine(accent, a, b, strokeWidth = maxOf(200 * ed.scale, 3.dp.toPx()))
                drawCircle(accent, 5.dp.toPx(), a); drawCircle(accent, 5.dp.toPx(), b)
                label(tm, cm((q - p).len), Offset(b.x, b.y - 44.dp.toPx()), big, accent)
            }
        }
    }
}

/** Kitchen, bath and other fittings; the rest of the catalogue is movable furniture. */
private val FITTINGS = setOf("cabinet", "fridge", "stove", "sink", "washer", "bath", "toilet", "fireplace", "radiator")

/** Picking by a rectangle on or off; a drag then draws the rectangle instead of a wall. */
@Composable
private fun AreaChip(ed: EditorState) {
    FilterChip(ed.area, onClick = { ed.area = !ed.area; if (!ed.area && ed.sel is Sel.GroupSel) ed.sel = null },
        label = { Text(stringResource(R.string.select_area)) }, leadingIcon = { Icon(Icons.Default.HighlightAlt, null) })
    // the rectangle picks on every floor at once: to line things up one over another
    FilterChip(ed.areaAllFloors, onClick = { ed.areaAllFloors = !ed.areaAllFloors; if (ed.sel is Sel.GroupSel) ed.sel = null },
        label = { Text("↕ " + stringResource(R.string.all_floors)) }, leadingIcon = { Checkbox(ed.areaAllFloors, null, Modifier.size(18.dp)) },
        modifier = Modifier.padding(start = 4.dp))
}

/** Whether [i] is drawn: always in the Interior menu (radiators also in Systems), elsewhere as View says. */
private fun itemShown(vm: AppViewModel, ed: EditorState, i: com.dawoyan.roomik.core.Item): Boolean =
    ed.scope == Tool.INTERIOR || (i.kind == "radiator" && ed.scope == Tool.SYSTEMS) || if (i.kind in FITTINGS) vm.showFittings else vm.showFurniture

/** On a board (or a riser bringing its cables): the drawn lines leaving it and their cable, opening the list. */
@Composable
private fun BoardLinesChip(plan: Plan, board: ElPoint) {
    val lines = remember(plan, board.id) { Circuits.lines(plan, board.id).sortedBy { l -> Circuits.numbers(plan)[l.routes.first().id] ?: 0 } }
    val nos = remember(plan) { Circuits.numbers(plan) }
    var open by remember { mutableStateOf(false) }
    val m = stringResource(R.string.m)
    AssistChip(onClick = { open = true }, label = { Text(stringResource(R.string.board_lines, lines.size, num(lines.sumOf { it.mm } / 1000, 1) + " " + m)) },
        leadingIcon = { Icon(Icons.Default.Cable, null, Modifier.size(16.dp)) }, modifier = Modifier.padding(start = 6.dp))
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.board_lines_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (lines.isEmpty()) Text(stringResource(R.string.board_lines_none), style = MaterialTheme.typography.bodyMedium)
                lines.forEachIndexed { i, l ->
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(mm2Color(l.mm2), androidx.compose.foundation.shape.CircleShape))
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            val no = nos[l.routes.first().id] ?: (i + 1)
                            val name = l.routes.firstOrNull { it.label.isNotBlank() }?.label
                            Text("№$no · ${l.cores}×${mm2Text(l.mm2)} " + stringResource(R.string.mm2) + (name?.let { " · $it" } ?: ""), style = MaterialTheme.typography.titleSmall)
                            val what = l.devices.groupBy { it.kind }.map { (k, v) -> pointName(k) + " " + v.sumOf { if (k == PointKind.SOCKET) it.count else 1 } }
                            Text(what.joinToString(", ").ifEmpty { "—" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(num(l.mm / 1000, 1) + " " + m, style = MaterialTheme.typography.titleSmall)
                    }
                }
                // how much cable of each size the board's lines take
                val bySize = lines.flatMap { it.routes }.distinctBy { it.id }.groupBy { it.cores to it.mm2 }
                    .map { (k, rs) -> k to rs.sumOf { Cables.length(plan, it) } }.sortedWith(compareBy({ it.first.second }, { it.first.first }))
                if (bySize.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                    Text("Σ " + stringResource(R.string.by_size), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    for ((k, mm) in bySize) Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(mm2Color(k.second), androidx.compose.foundation.shape.CircleShape))
                        Text("${k.first}×${mm2Text(k.second)} " + stringResource(R.string.mm2), Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        Text(num(mm / 1000, 1) + " " + m, style = MaterialTheme.typography.titleSmall)
                    }
                }
                Text(stringResource(R.string.board_lines_about), Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.ok)) } },
    )
}

/** The menu a picked thing belongs to. */
private fun toolOf(s: Sel): Tool = when (s) {
    is Sel.Corner, is Sel.WallSel, is Sel.WallsSel -> Tool.WALL
    is Sel.RoomSel -> Tool.ROOM
    is Sel.OpeningSel -> Tool.DOOR
    is Sel.RoadSel -> Tool.ROAD
    is Sel.StairSel -> Tool.STAIRS
    is Sel.RoofSel -> Tool.ROOF
    is Sel.PlatformSel -> Tool.PLATFORM
    is Sel.FenceSel -> Tool.FENCE
    is Sel.PlaceSel -> Tool.PLACE
    is Sel.PointSel, is Sel.RouteSel, is Sel.PointsSel, is Sel.FeedSel, is Sel.CrossCableSel -> Tool.ELECTRIC
    is Sel.ItemSel, is Sel.ModelSel -> Tool.INTERIOR
    is Sel.PipeSel -> Tool.SYSTEMS
    is Sel.PillarSel -> Tool.PILLAR
    is Sel.GroupSel -> if (s.pillars.isNotEmpty()) Tool.PILLAR else Tool.WALL
}

/** The rectangle around a picked group (plan mm): its top-left and bottom-right. */
private fun groupBounds(plan: Plan, g: Sel.GroupSel): Pair<P, P>? {
    val pts = plan.nodes.filter { it.id in g.nodes }.map { P(it.x.toDouble(), it.y.toDouble()) } +
        plan.pillars.filter { it.id in g.pillars }.map { P(it.x.toDouble(), it.y.toDouble()) }
    if (pts.isEmpty()) return null
    return P(pts.minOf { it.x }, pts.minOf { it.y }) to P(pts.maxOf { it.x }, pts.maxOf { it.y })
}

/** A chip over the plan: solid, so it reads on any drawing. */
@Composable
private fun PlanChip(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 9.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** A floor to switch to; the one shown is filled with the main colour. */
@Composable
internal fun FloorChip(text: String, chosen: Boolean, onDelete: (() -> Unit)?, onClick: () -> Unit) {
    val bg = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (chosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = bg, contentColor = fg, shadowElevation = 3.dp,
        border = if (chosen) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (onDelete != null) Icon(Icons.Default.Close, null, Modifier.padding(start = 8.dp).size(18.dp).clickable { onDelete() })
        }
    }
}

/** Handles of a selected cable or pipe (screen): the first [turns] are its turns, the rest the middles of its runs. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.lineHandles(hs: List<Offset>, turns: Int, accent: Color) {
    for ((i, q) in hs.withIndex()) {
        val big = i < turns
        drawCircle(Color.White, if (big) 8.dp.toPx() else 5.dp.toPx(), q)
        drawCircle(accent, if (big) 8.dp.toPx() else 5.dp.toPx(), q, style = Stroke(2.5.dp.toPx()))
    }
}

/** A drawn cable on the plan (mm): from its first point through its turns to its last. */
fun routePlan(plan: Plan, r: com.dawoyan.roomik.core.Route): List<P>? {
    val a = plan.points.firstOrNull { it.id == r.from } ?: return null
    val b = plan.points.firstOrNull { it.id == r.to } ?: return null
    return listOf(P(a.x.toDouble(), a.y.toDouble())) + r.via.map { P(it.x.toDouble(), it.y.toDouble()) } + P(b.x.toDouble(), b.y.toDouble())
}

/** A cable as drawn: from the icon of a wall device to the wall, along its way, and up to the other device's icon (plan mm). */
private fun routeDrawn(plan: Plan, r: com.dawoyan.roomik.core.Route, placed: List<com.dawoyan.roomik.core.PlacedPoint>): List<P>? {
    val pts = routePlan(plan, r) ?: return null
    fun icon(id: Int) = placed.firstOrNull { it.point.id == id }?.takeIf { !it.point.kind.free }?.let { symbolAt(it) }
    return Lines.straighten(listOfNotNull(icon(r.from)) + pts + listOfNotNull(icon(r.to)))
}

/**
 * For each line (plan mm), how far each of its runs moves sideways (to its own left) so that runs lying on one another
 * are drawn side by side, [gap] apart. The lines sharing a run are ordered by the first of them, in its own frame, so a
 * line keeps its side through every turn the lines take together and they never cross at a shared corner.
 */
private fun laneOffsets(lines: List<List<P>>, gap: Float): List<List<Double>> = lines.mapIndexed { li, pts ->
    (1 until pts.size).map { k ->
        val a = pts[k - 1]; val b = pts[k]
        val len = (b - a).len
        if (len < 1) return@map 0.0
        val d = (b - a) * (1 / len)
        // the lines with a run along this one, overlapping it, and that run's direction
        val sharing = ArrayList<Pair<Int, P>>()
        for (lj in lines.indices) {
            if (lj == li) { sharing += li to d; continue }
            for (m in 1 until lines[lj].size) {
                val c = lines[lj][m - 1]; val e = lines[lj][m]
                val dl = (e - c).len
                if (dl >= 1 && kotlin.math.abs(dot((e - c) * (1 / dl), d.left())) < 0.05 &&
                    kotlin.math.abs(dot(c - a, d.left())) < 40 &&
                    minOf(len, maxOf(dot(c - a, d), dot(e - a, d))) - maxOf(0.0, minOf(dot(c - a, d), dot(e - a, d))) > 60) {
                    sharing += lj to (e - c) * (1 / dl); break
                }
            }
        }
        if (sharing.size < 2) return@map 0.0
        val ref = sharing[0].second
        val slot = (sharing.indexOfFirst { it.first == li } - (sharing.size - 1) / 2.0) * gap
        // the slot is in the first line's frame: turned into this line's own
        if (dot(d, ref) >= 0) slot else -slot
    }
}

/** [pts] with each run moved to its left by [offs] (one per run), the corners meeting again. */
private fun offsetLine(pts: List<P>, offs: List<Double>): List<P> {
    if (pts.size < 2 || offs.all { it == 0.0 }) return pts
    val n = (1 until pts.size).map { (pts[it] - pts[it - 1]).norm().left() }
    val out = ArrayList<P>()
    out += pts[0] + n[0] * offs[0]
    for (i in 1 until pts.size - 1) {
        val p1 = pts[i] + n[i - 1] * offs[i - 1]; val d1 = (pts[i] - pts[i - 1]).norm()
        val p2 = pts[i] + n[i] * offs[i]; val d2 = (pts[i + 1] - pts[i]).norm()
        val cr = d1.x * d2.y - d1.y * d2.x
        if (kotlin.math.abs(cr) < 1e-6) { out += p1; out += p2; continue }
        val t = ((p2.x - p1.x) * d2.y - (p2.y - p1.y) * d2.x) / cr
        out += p1 + d1 * t
    }
    out += pts.last() + n.last() * offs.last()
    return out
}

/** Where a point's symbol is drawn on the plan: lamps where they hang, wall points just off the wall face. */
private fun symbolAt(p: com.dawoyan.roomik.core.PlacedPoint): P = if (p.point.kind.free) p.face else p.face + p.normal * 100.0

private val ELECTRIC_RED = Color(0xFFD0342C)

/** Σ: drawn cable of each kind (wires × mm²), on this floor and in the whole house. */
@Composable
fun CableTotalsChip(root: Plan, floor: Plan) {
    var open by remember { mutableStateOf(false) }
    AssistChip(onClick = { open = true }, label = { Text("Σ") }, modifier = Modifier.padding(horizontal = 4.dp))
    if (open) AlertDialog(onDismissRequest = { open = false }, confirmButton = { TextButton(onClick = { open = false }) { Text("OK") } },
        title = { Text(stringResource(R.string.cables_total)) },
        text = {
            Column {
                val m = stringResource(R.string.m)
                val house = Cables.totals(root.floorRange.map { root.floor(it) })
                val here = Cables.totals(listOf(floor))
                if (house.isEmpty()) Text("—")
                for ((cores, mm2, mm) in house) {
                    val f = here.firstOrNull { it.first == cores && it.second == mm2 }?.third ?: 0.0
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Box(Modifier.size(12.dp).background(mm2Color(mm2), androidx.compose.foundation.shape.CircleShape))
                        Text("  $cores×${mm2Text(mm2)} " + stringResource(R.string.mm2), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(num(mm / 1000, 1) + " " + m)
                    }
                    if (root.floorRange.count() > 1) Text(stringResource(R.string.this_floor) + ": " + num(f / 1000, 1) + " " + m, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                }
                if (house.isNotEmpty()) Text(stringResource(R.string.cables_total_about), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        })
}

/** A 110 V line's cables: green, whatever their size. */
val VOLT110 = Color(0xFF2E9E44)

/** Cable colour by wire size, in the Electrics menu. */
fun mm2Color(mm2: Double): Color = Color(when {
    mm2 <= 1.0 -> 0xFF8A8F98
    mm2 <= 1.5 -> 0xFFE8A200
    mm2 <= 2.5 -> 0xFFD0342C
    mm2 <= 4.0 -> 0xFF8E24AA
    mm2 <= 6.0 -> 0xFF3949AB
    mm2 <= 10.0 -> 0xFF00897B
    else -> 0xFF263238
})

@Composable
fun itemName(id: String): String {
    val network = mapOf("router" to "Router", "network_switch" to "Network switch", "access_point" to "Wi‑Fi access point", "modem_ont" to "Modem / ONT",
        "patch_panel" to "Patch panel", "network_rack" to "Network rack", "server_nas" to "Server / NAS", "nvr" to "NVR", "ip_camera" to "IP camera", "network_outlet" to "Network outlet")
    network[id]?.let { return it }
    return stringResource(when (id) {
    "bed" -> R.string.f_bed; "bed1" -> R.string.f_bed1; "sofa" -> R.string.f_sofa; "armchair" -> R.string.f_armchair
    "table" -> R.string.f_table; "chair" -> R.string.f_chair; "desk" -> R.string.f_desk; "wardrobe" -> R.string.f_wardrobe
    "tv" -> R.string.f_tv; "cabinet" -> R.string.f_cabinet; "fridge" -> R.string.f_fridge; "stove" -> R.string.f_stove
    "vacuum" -> R.string.f_vacuum; "robot_vacuum" -> R.string.f_robot_vacuum
    "sink" -> R.string.f_sink; "washer" -> R.string.f_washer; "bath" -> R.string.f_bath; "stairs" -> R.string.f_stairs; "car" -> R.string.f_car; "fireplace" -> R.string.f_fireplace; "radiator" -> R.string.f_radiator
    "ac_out" -> R.string.f_ac_out; "ac_in" -> R.string.f_ac_in; "chimney" -> R.string.f_chimney; else -> R.string.f_toilet
    })
}

@Composable
fun pointName(kind: PointKind): String = when (kind) {
    PointKind.LOW_CURRENT_BOARD -> "Low-current board"
    PointKind.LOW_CURRENT_BOX -> "Communications box"
    else -> stringResource(when (kind) {
        PointKind.SOCKET -> R.string.socket; PointKind.SWITCH -> R.string.switch_; PointKind.LAMP -> R.string.lamp; PointKind.BOARD -> R.string.board
        PointKind.APPLIANCE -> R.string.appliance; PointKind.RISER -> R.string.riser; PointKind.JUNCTION -> R.string.junction
        PointKind.LOW_CURRENT_BOARD, PointKind.LOW_CURRENT_BOX -> error("handled above")
    })
}

private fun DrawScope.grid(ed: EditorState) {
    val step = listOf(100f, 500f, 1000f, 5000f, 10000f).firstOrNull { it * ed.scale >= 10.dp.toPx() } ?: return
    val bold = if (step < 1000f) 1000f else step * 5
    val x0 = Math.floor(((0 - ed.ox) / ed.scale / step).toDouble()).toInt()
    val x1 = Math.ceil(((size.width - ed.ox) / ed.scale / step).toDouble()).toInt()
    for (i in x0..x1) {
        val x = i * step * ed.scale + ed.ox
        val b = (i * step) % bold == 0f
        drawLine(if (b) Color(0xFFCDCDC4) else Color(0xFFE6E6DF), Offset(x, 0f), Offset(x, size.height), if (b) 1.5f else 1f)
    }
    val y0 = Math.floor(((0 - ed.oy) / ed.scale / step).toDouble()).toInt()
    val y1 = Math.ceil(((size.height - ed.oy) / ed.scale / step).toDouble()).toInt()
    for (i in y0..y1) {
        val y = i * step * ed.scale + ed.oy
        val b = (i * step) % bold == 0f
        drawLine(if (b) Color(0xFFCDCDC4) else Color(0xFFE6E6DF), Offset(0f, y), Offset(size.width, y), if (b) 1.5f else 1f)
    }
}

/** Text centred on [center], on a rounded plate when [plate] is given. */
private fun DrawScope.label(tm: TextMeasurer, text: String, center: Offset, style: TextStyle, plate: Color?) {
    val l = tm.measure(text, style)
    val tl = Offset(center.x - l.size.width / 2f, center.y - l.size.height / 2f)
    if (plate != null) {
        val px = 5.dp.toPx(); val py = 2.dp.toPx()
        drawRoundRect(plate, Offset(tl.x - px, tl.y - py), Size(l.size.width + 2 * px, l.size.height + 2 * py), CornerRadius(4.dp.toPx()))
    }
    drawText(l, topLeft = tl)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionPanel(vm: AppViewModel, ed: EditorState, plan: Plan, rooms: List<Room>) {
    val boardExports = rememberExports(vm)
    val lang = language()
    var editLength by remember { mutableStateOf(false) }
    var editWallH by remember { mutableStateOf(false) }
    var editStair by remember { mutableStateOf(false) }
    var editRoom by remember { mutableStateOf(false) }
    val sel = ed.sel
    val wall = (sel as? Sel.WallSel)?.let { plan.wall(it.id) }
    val manyWalls = (sel as? Sel.WallsSel)?.let { s -> plan.walls.filter { it.id in s.ids } }?.takeIf { it.isNotEmpty() }
    // walls being changed together: their width, height, stone
    var widthFor by remember { mutableStateOf<Set<Int>?>(null) }
    var heightFor by remember { mutableStateOf<Set<Int>?>(null) }
    var facingFor by remember { mutableStateOf<Set<Int>?>(null) }
    /** Wall ids selected by an all-floors rectangle: floor → ids. */
    var facingAcross by remember { mutableStateOf<Map<Int, Set<Int>>?>(null) }
    var facingSideStart by remember { mutableStateOf<Facings.Side?>(null) }
    var facingFence by remember { mutableStateOf(false) }
    var fenceWidth by remember { mutableStateOf(false) }
    var fenceStep by remember { mutableStateOf(false) }
    val wallsText = stringResource(R.string.n_walls)
    widthFor?.let { ids ->
        val first = plan.walls.firstOrNull { it.id in ids }
        TextDialog(if (ids.size > 1) wallsText.format(ids.size) else stringResource(R.string.wall), stringResource(R.string.wall_width_cm),
            first?.let { cm(it.thickness.toDouble()) } ?: "", numeric = true, onDismiss = { widthFor = null }) { t ->
            widthFor = null
            t.replace(',', '.').toDoubleOrNull()?.let { v -> val mm = (v * 10).roundToInt().coerceIn(20, 2000)
                vm.commit(PlanOps.editWalls(plan, ids) { it.copy(thickness = mm) }) }
        }
    }
    heightFor?.let { ids ->
        val first = plan.walls.firstOrNull { it.id in ids }
        TextDialog(wallsText.format(ids.size), stringResource(R.string.height_cm), first?.let { cm(plan.heightOf(it).toDouble()) } ?: "", numeric = true,
            onDismiss = { heightFor = null }) { t ->
            heightFor = null
            t.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 10 }?.let { v -> val mm = (v * 10).roundToInt().coerceAtMost(30000)
                vm.commit(PlanOps.editWalls(plan, ids) { it.copy(height = mm) }) }
        }
    }
    ed.facingSide?.let { sd -> if (wall != null) { facingFor = setOf(wall.id); facingSideStart = sd }; ed.facingSide = null }
    facingFor?.let { ids ->
        val ws = plan.walls.filter { it.id in ids }
        // "Outside" only where a wall has an outside face (not a wall inside the house)
        val hasOut = ws.any { w -> Facings.outsides(plan, w, rooms).let { it.first || it.second } }
        val sides = (if (hasOut) listOf(Facings.Side.OUTSIDE) else emptyList()) +
            if (ws.size == 1) listOf(Facings.Side.LEFT, Facings.Side.RIGHT, Facings.Side.BOTH) else listOf(Facings.Side.INSIDE, Facings.Side.BOTH)
        FacingDialog(null, allowNone = true, sides = sides, initialSide = facingSideStart?.takeIf { it in sides },
            current = { side -> ws.firstNotNullOfOrNull { Facings.current(plan, it, rooms, side) } }, onDismiss = { facingFor = null; facingSideStart = null }) { side, f ->
            facingFor = null; facingSideStart = null
            vm.commit(PlanOps.editWalls(plan, ids) { Facings.put(plan, it, rooms, side, f) })
        }
    }
    facingAcross?.let { picked ->
        val root0 = vm.root
        val sample = picked.entries.firstOrNull()
        if (root0 != null && sample != null) {
            val basePlan = root0.floor(sample.key)
            val baseRooms = Rooms.detect(basePlan)
            val baseWalls = basePlan.walls.filter { it.id in sample.value }
            FacingDialog(null, allowNone = true, sides = listOf(Facings.Side.OUTSIDE, Facings.Side.LEFT, Facings.Side.RIGHT, Facings.Side.BOTH),
                current = { side -> baseWalls.firstNotNullOfOrNull { Facings.current(basePlan, it, baseRooms, side) } }, onDismiss = { facingAcross = null }) { side, f ->
                facingAcross = null
                var after: Plan = root0
                for ((floorNo, ids) in picked) {
                    val fl = after.floor(floorNo); val rs = Rooms.detect(fl)
                    val changed = fl.copy(walls = fl.walls.map { w -> if (w.id in ids) Facings.put(fl, w, rs, side, f) else w })
                    after = after.withFloor(floorNo, changed)
                }
                vm.commitHouse(after)
            }
        } else facingAcross = null
    }
    val corner = (sel as? Sel.Corner)?.let { plan.node(it.id) }
    val room = (sel as? Sel.RoomSel)?.let { s -> rooms.firstOrNull { Geo.inside(s.at, it.polygon) } }
    val point = (sel as? Sel.PointSel)?.let { s -> plan.points.firstOrNull { it.id == s.id } }
    val picked = (sel as? Sel.PointsSel)?.let { s -> plan.points.filter { it.id in s.ids } }?.takeIf { it.isNotEmpty() }
    val laidText = stringResource(R.string.cables_laid); val noBoardText = stringResource(R.string.no_board_here); val joinedText = stringResource(R.string.all_joined)
    val relaidText = stringResource(R.string.cables_relaid)
    val feedLaidText = stringResource(R.string.feed_laid)
    ed.sourcePickTo?.let { toId ->
        val fromId = ed.sourcePickFrom
        val load = fromId?.let { id -> plan.points.firstOrNull { it.id == id } }
        val source = plan.points.firstOrNull { it.id == toId }
        if (load == null || source == null) { ed.sourcePickFrom = null; ed.sourcePickTo = null }
        else AlertDialog(
            onDismissRequest = { ed.sourcePickFrom = null; ed.sourcePickTo = null },
            title = { Text("Set source") },
            text = { Text("${pointName(load.kind)} #${load.id} will be fed by ${pointName(source.kind)} #${source.id}.") },
            confirmButton = { TextButton(onClick = {
                val after = PlanOps.setPointSource(plan, load.id, source.id, ed.routeLevel)
                vm.commit(after); ed.sel = Sel.PointSel(load.id)
                DebugLog.add("electric: load #${load.id} source set to #${source.id}")
                ed.sourcePickFrom = null; ed.sourcePickTo = null
            }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { ed.sourcePickFrom = null; ed.sourcePickTo = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (ed.crossPickFloor) {
        val root = vm.root
        val from = ed.crossFrom
        if (root == null || from == null) ed.crossPickFloor = false
        else AlertDialog(
            onDismissRequest = { ed.crossPickFloor = false; ed.crossFrom = null },
            title = { Text("Cross-floor cable") },
            text = {
                Column {
                    Text("Choose the floor containing the other board or communications box.")
                    for (f in root.floorRange.filter { it != from.first }) TextButton(onClick = {
                        ed.crossPickFloor = false
                        vm.selectFloor(f)
                        ed.toastText = "Tap the other board or communications box."
                    }) { Text(floorName(f, root)) }
                }
            },
            dismissButton = { TextButton(onClick = { ed.crossPickFloor = false; ed.crossFrom = null }) { Text(stringResource(R.string.cancel)) } },
            confirmButton = {},
        )
    }
    // cables from the devices to their board, laid the short way along walls and through doors
    fun joinToBoard(ids: Collection<Int>) {
        val (after, n) = Autoroute.join(plan, ids, ed.routeLevel, vm.root, vm.floor)
        if (n > 0) { vm.commit(after); ed.toastText = laidText.format(n) }
        else if (plan.points.none { it.kind == PointKind.BOARD || it.kind == PointKind.RISER }) ed.toastText = noBoardText
        else {
            // all already joined: their cables laid again, the way auto cables go now
            val again = Autoroute.rewire(plan, ids.filter { id -> plan.points.any { it.id == id && Circuits.kindOf(it.kind) != null } }, ed.routeLevel, vm.root, vm.floor)
            if (again != plan) { vm.commit(again); ed.toastText = relaidText } else ed.toastText = joinedText
        }
    }
    val item = (sel as? Sel.ItemSel)?.let { s -> plan.items.firstOrNull { it.id == s.id } }
    val model = (sel as? Sel.ModelSel)?.let { s -> plan.models.firstOrNull { it.id == s.id } }
    var showViews by remember { mutableStateOf(false) }
    var editModelZ by remember { mutableStateOf(false) }
    var editYard by remember { mutableStateOf(false) }
    val ruYard = lang == "ru"
    // a yard construction chosen in the row: its settings first (the last ones used for it), then it waits for a tap
    val tapWhere = stringResource(R.string.yard_tap)
    ed.yardSetup?.let { k ->
        YardDialog(vm.lastYard[k] ?: Yard.default(k), onDismiss = { ed.yardSetup = null }) { spec ->
            vm.lastYard[k] = spec
            ed.yardSetup = null; ed.yardKind = k; ed.yardSpec = spec; ed.itemKind = null; ed.sel = null
            ed.toastText = tapWhere
        }
    }
    val yardSpec = model?.yard
    if (editYard && model != null && yardSpec != null) YardDialog(yardSpec, onDismiss = { editYard = false }) { spec ->
        editYard = false
        vm.commit(PlanOps.updateModel(plan, model.copy(yard = spec, parts = Yard.parts(spec, ruYard))))
    }
    // a file picked to replace the model of that name (or, from the Interior row, a new model)
    val context = androidx.compose.ui.platform.LocalContext.current
    var replaceFor by remember { mutableStateOf<String?>(null) }
    val modelPicker = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            val file = fileNameOf(context, uri)
            vm.pendingModel = (replaceFor ?: file.substringBeforeLast('.')) to (file to bytes)
        }
        replaceFor = null
    }
    if (showViews && model != null) ModelViewsDialog(model) { showViews = false }
    val tapWhereAsset = stringResource(R.string.asset_tap)
    if (ed.assetsOpen) AssetsDialog(vm, onAddFile = { replaceFor = null; modelPicker.launch(arrayOf("*/*")) }, onDismiss = { ed.assetsOpen = false }) { a, fl ->
        ed.assetsOpen = false
        if (fl != vm.floor) vm.selectFloor(fl)
        finishDrawing(vm, ed); ed.tool = Tool.INTERIOR; ed.outdoor = fl == SITE; ed.itemKind = null; ed.yardKind = null; ed.sel = null
        vm.assetToPlace = a
        ed.toastText = tapWhereAsset.format(a.name)
    }
    if (editModelZ && model != null) TextDialog(model.name, stringResource(R.string.model_z_cm), cm(model.z.toDouble()), numeric = true, onDismiss = { editModelZ = false }) { t ->
        editModelZ = false
        t.replace(',', '.').toDoubleOrNull()?.let { vm.commit(PlanOps.updateModel(plan, model.copy(z = (it * 10).roundToInt().coerceIn(-5000, 50000)))) }
    }
    var editPoint by remember { mutableStateOf(false) }
    var editSwitchLabel by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf(false) }
    var itemH by remember { mutableStateOf(false) }
    var itemZ by remember { mutableStateOf(false) }
    val itemNow = (sel as? Sel.ItemSel)?.let { s -> plan.items.firstOrNull { it.id == s.id } }
    if (itemH && itemNow != null) TextDialog(itemName(itemNow.kind), stringResource(R.string.height_cm), cm(itemNow.h.toDouble()), numeric = true, onDismiss = { itemH = false }) { t ->
        itemH = false; t.replace(',', '.').toDoubleOrNull()?.let { vm.commit(PlanOps.updateItem(plan, itemNow.copy(h = (it * 10).roundToInt().coerceIn(10, 30000)))) }
    }
    if (itemZ && itemNow != null) TextDialog(itemName(itemNow.kind), stringResource(R.string.above_floor_cm), cm(itemNow.z.toDouble()), numeric = true, onDismiss = { itemZ = false }) { t ->
        itemZ = false; t.replace(',', '.').toDoubleOrNull()?.let { vm.commit(PlanOps.updateItem(plan, itemNow.copy(z = (it * 10).roundToInt().coerceIn(-5000, 30000)))) }
    }
    val stair = (sel as? Sel.StairSel)?.let { s -> plan.stairs.firstOrNull { it.id == s.id } }
    val pipe = (sel as? Sel.PipeSel)?.let { s -> plan.pipes.firstOrNull { it.id == s.id } }
    val roof = (sel as? Sel.RoofSel)?.let { s -> plan.roofs.firstOrNull { it.id == s.id } }
    val fence = (sel as? Sel.FenceSel)?.let { s -> plan.fences.firstOrNull { it.id == s.id } }
    val road = (sel as? Sel.RoadSel)?.let { s -> plan.roads.firstOrNull { it.id == s.id } }
    var roadWidth by remember { mutableStateOf(false) }
    if (roadWidth && road != null) TextDialog(stringResource(R.string.road), stringResource(R.string.y_width) + ", " + stringResource(R.string.cm), cm(road.width.toDouble()), numeric = true,
        onDismiss = { roadWidth = false }) { t ->
        roadWidth = false; t.replace(',', '.').toDoubleOrNull()?.let { vm.commit(PlanOps.updateRoad(plan, road.copy(width = (it * 10).roundToInt().coerceIn(300, 20000)))) }
    }
    val platform = (sel as? Sel.PlatformSel)?.let { s -> plan.platforms.firstOrNull { it.id == s.id } }
    val pillar = (sel as? Sel.PillarSel)?.let { s -> plan.pillars.firstOrNull { it.id == s.id } }
    val feedSel = sel as? Sel.FeedSel
    val feed = feedSel?.let { fs -> vm.root?.let { Feeds.of(it, fs.toFloor, fs.toId) } }
    val crossCable = (sel as? Sel.CrossCableSel)?.let { s -> vm.root?.crossCables?.firstOrNull { it.id == s.id } }
    var editPillarSize by remember { mutableStateOf(false) }
    // the floor below's pillars, for putting ours exactly over them
    val belowPillarsPanel = vm.root?.let { r -> (vm.floor - 1).takeIf { vm.floor != SITE && it in r.floorRange }?.let { r.floor(it).pillars } }.orEmpty()
    val offPillarsPanel = Pillars.offBelow(plan.pillars, belowPillarsPanel)
    val otherFloorPillars = vm.root?.let { r -> r.floorRange.filter { it != vm.floor }.flatMap { r.floor(it).pillars } }.orEmpty()
    val centeredText = stringResource(R.string.centered_axes); val onAxesText = stringResource(R.string.on_axes_already)
    val alignedText = stringResource(R.string.aligned_below); val noBelowText = stringResource(R.string.none_below_near)
    var editPillarH by remember { mutableStateOf(false) }
    if (editPillarSize && pillar != null) TextDialog(stringResource(R.string.pillar), stringResource(R.string.pillar_size),
        if (pillar.round) cm(pillar.w.toDouble()) else cm(pillar.w.toDouble()) + " x " + cm(pillar.d.toDouble()), onDismiss = { editPillarSize = false }) { t ->
        editPillarSize = false
        val v = t.replace(',', '.').split('x', 'X', 'х', 'Х', '×', '*', ' ').mapNotNull { it.trim().toDoubleOrNull() }.map { (it * 10).roundToInt().coerceIn(50, 3000) }
        if (v.isNotEmpty()) vm.commit(PlanOps.updatePillar(plan, pillar.copy(w = v[0], d = v.getOrElse(1) { v[0] })))
    }
    if (editPillarH && pillar != null) TextDialog(stringResource(R.string.pillar), stringResource(R.string.height_cm), cm(Pillars.height(plan, pillar).toDouble()), numeric = true,
        onDismiss = { editPillarH = false }) { t ->
        editPillarH = false
        t.replace(',', '.').toDoubleOrNull()?.let { val h = (it * 10).roundToInt().coerceIn(100, 12000); vm.commit(PlanOps.updatePillar(plan, pillar.copy(height = h.takeIf { it != plan.wallHeight }))) }
    }
    var editPlatform by remember { mutableStateOf(false) }
    var editPlatformSize by remember { mutableStateOf(false) }
    var editRoof by remember { mutableStateOf(false) }
    var roofColor by remember { mutableStateOf(false) }
    if (roofColor && roof != null) ColorDialog(stringResource(R.string.colour), roof.shownColor, ROOF_COLORS, onDismiss = { roofColor = false }) { c ->
        roofColor = false; vm.commit(PlanOps.updateRoof(plan, roof.copy(color = c.takeIf { it != roof.cover.color })))
    }
    var editRoofBase by remember { mutableStateOf(false) }
    var editFenceH by remember { mutableStateOf(false) }
    var editGateW by remember { mutableStateOf(false) }
    // another fence picked: no gate picked, none being put in
    LaunchedEffect(fence?.id) { ed.gateIndex = null; ed.gatePlacing = null; ed.gateMoving = false }
    val gate = fence?.let { f -> ed.gateIndex?.let { f.gates.getOrNull(it) } }
    if (editGateW && fence != null && gate != null) TextDialog(gateName(gate.kind), stringResource(R.string.gate_width_cm), cm(gate.width.toDouble()), numeric = true,
        onDismiss = { editGateW = false }) { t -> editGateW = false; t.replace(',', '.').toDoubleOrNull()?.let { v ->
            vm.commit(PlanOps.updateFence(plan, fence.copy(gates = fence.gates.mapIndexed { i, g -> if (i == ed.gateIndex) g.copy(width = (v * 10).roundToInt().coerceIn(600, 12000)) else g }))) } }
    if (editFenceH && fence != null) TextDialog(fenceName(fence.kind), stringResource(R.string.height_cm), cm(fence.height.toDouble()), numeric = true,
        onDismiss = { editFenceH = false }) { t -> editFenceH = false; t.replace(',', '.').toDoubleOrNull()?.let { vm.commit(PlanOps.updateFence(plan, fence.copy(height = (it * 10).roundToInt().coerceIn(300, 5000)))) } }
    if (editRoof && roof != null) TextDialog(roofName(roof.kind), stringResource(R.string.roof_pitch), "${roof.pitch} x ${cm(roof.overhang.toDouble())}",
        onDismiss = { editRoof = false }) { t ->
        editRoof = false
        val v = t.replace(',', '.').split('x', 'X', 'х', 'Х', '×', '*', ' ').mapNotNull { it.trim().toDoubleOrNull() }
        if (v.isNotEmpty()) vm.commit(PlanOps.updateRoof(plan, roof.copy(pitch = v[0].roundToInt().coerceIn(0, 75), overhang = v.getOrNull(1)?.let { (it * 10).roundToInt().coerceIn(0, 2000) } ?: roof.overhang)))
    }
    var editPipeH by remember { mutableStateOf(false) }
    if (editPipeH && pipe != null) TextDialog(systemName(pipe.system), stringResource(R.string.height_cm), cm(pipe.height.toDouble()), numeric = true,
        onDismiss = { editPipeH = false }) { t -> editPipeH = false; t.replace(',', '.').toDoubleOrNull()?.let { vm.commit(PlanOps.updatePipe(plan, pipe.copy(height = (it * 10).roundToInt().coerceIn(0, plan.wallHeight)))) } }
    fun setStair(st: Stair) = commitStair(vm, ed, PlanOps.updateStair(plan, st), plan, st.id)
    val opening = (sel as? Sel.OpeningSel)?.let { s -> plan.openings.firstOrNull { it.id == s.id } }
    val route = (sel as? Sel.RouteSel)?.let { s -> plan.routes.firstOrNull { it.id == s.id } }
    val place = (sel as? Sel.PlaceSel)?.let { s -> plan.places.firstOrNull { it.id == s.id } }
    var editPlace by remember { mutableStateOf(false) }
    LaunchedEffect(ed.askPlace) { if (ed.askPlace != null && ed.askPlace == place?.id) { editPlace = true; ed.askPlace = null } }
    var editAppliance by remember { mutableStateOf(false) }
    LaunchedEffect(ed.askFor) { if (ed.askFor != null && ed.askFor == point?.id) { editAppliance = true; ed.askFor = null } }
    var editOpening by remember { mutableStateOf(false) }
    var editBoardName by remember { mutableStateOf(false) }
    var editLineNo by remember { mutableStateOf(false) }
    val routeForNo = (sel as? Sel.RouteSel)?.let { s -> plan.routes.firstOrNull { it.id == s.id } }
    if (editLineNo && routeForNo != null) TextDialog(stringResource(R.string.cable), stringResource(R.string.line_number),
        Circuits.numbers(plan)[routeForNo.id]?.toString() ?: "", numeric = true, onDismiss = { editLineNo = false }) { t ->
        editLineNo = false
        // the whole line takes the number (0 or empty: numbered in order again)
        val n = t.trim().toIntOrNull()?.coerceIn(0, 999) ?: 0
        val ids = Circuits.lineOf(plan, routeForNo.id).map { it.id }.toSet()
        vm.commit(plan.copy(routes = plan.routes.map { if (it.id in ids) it.copy(num = n) else it }))
    }
    var boardSettings by remember { mutableStateOf(false) }
    var boardLoads by remember { mutableStateOf(false) }
    if (boardSettings && point != null && point.kind == PointKind.BOARD) {
        val root = vm.root ?: plan
        val me = Boards.all(root).firstOrNull { it.floor == vm.floor && it.point.id == point.id } ?: Boards.Ref(vm.floor, point, 0)
        val ru = lang == "ru"
        val cands = me?.let { Feeds.candidates(root, it) }.orEmpty()
        val src = Boards.all(root).firstOrNull { it.key == point.source }
        fun setSource(k: String?) {
            vm.commit(PlanOps.updatePoint(plan, point.copy(source = k)))
            if (k != null) ed.toastText = feedLaidText
        }
        AlertDialog(
            onDismissRequest = { boardSettings = false },
            title = { Text(stringResource(R.string.board_settings) + ": " + (me?.let { Boards.name(it, ru) } ?: point.name)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.fed_from), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth().clickable { setSource(null) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(src == null, { setSource(null) }); Text(stringResource(R.string.board_mains))
                    }
                    for (c in cands) Row(Modifier.fillMaxWidth().clickable { setSource(c.key) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(c.key == point.source, { setSource(c.key) })
                        Text(Boards.name(c, ru) + " · " + floorName(c.floor, root))
                    }
                    if (src != null) {
                        Text(stringResource(R.string.feed_cable), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        val cores = if (point.threePhase || src.point.threePhase) 5 else 3
                        Row(Modifier.hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (sz in Feeds.SIZES) FilterChip(point.feedMm2 == sz, onClick = { vm.commit(PlanOps.updatePoint(plan, point.copy(feedMm2 = sz))) },
                                label = { Text("$cores×" + mmTxt(sz)) })
                        }
                        me?.let { m -> Feeds.make(root, src, m) }?.let { f ->
                            Text(stringResource(R.string.feed_length, num(f.across / 1000.0, 1), num(f.vertical / 1000.0, 1), num(f.length / 1000.0, 1)),
                                Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(stringResource(R.string.feed_about), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // this board on its own, with all its loads, to a project in elektronik
                    FilledTonalButton(onClick = { boardSettings = false; boardExports.sendBoard(me) }, Modifier.padding(top = 12.dp)) {
                        Text("⇢ " + stringResource(R.string.send_board_elektronik))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { boardSettings = false }) { Text(stringResource(R.string.ok)) } },
        )
    }
    if (boardLoads && point != null && point.kind == PointKind.BOARD) {
        val root = vm.root ?: plan
        val me = Boards.all(root).firstOrNull { it.floor == vm.floor && it.point.id == point.id } ?: Boards.Ref(vm.floor, point, 0)
        val ru = lang == "ru"
        val source = Boards.all(root).firstOrNull { it.key == point.source }
        val bom = Materials.boardBom(root, me, ru)
        val sent = com.dawoyan.elektronik.core.Roomik.singleBoard(Materials.elektronikBoardFile(root, me, ru))
        AlertDialog(
            onDismissRequest = { boardLoads = false },
            title = { Text(Boards.name(me, ru)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Source: " + (source?.let { Boards.name(it, ru) } ?: stringResource(R.string.board_mains)), style = MaterialTheme.typography.bodyMedium)
                    Text("Loads", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleSmall)
                    val rooms = sent?.rooms.orEmpty().filter { it.loads.isNotEmpty() }
                    if (rooms.isEmpty()) Text("No loads assigned to this board.")
                    for (room in rooms) {
                        Text(room.name.ifBlank { room.zone.ifBlank { "Area" } }, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelLarge)
                        for (load in room.loads) {
                            val kind = when (load.kind) { com.dawoyan.elektronik.core.LoadKind.LIGHTS -> "Lights"; com.dawoyan.elektronik.core.LoadKind.SOCKETS -> "Sockets"; else -> "Load" }
                            Text("• " + (load.name.takeIf { it.isNotBlank() }?.let { "$kind · $it" } ?: kind) + " ×${load.count}")
                        }
                    }
                    Text("Cable lengths", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleSmall)
                    for (line in bom.lines) Text("${line.name}: ${num(line.meters, 1)} m")
                    Text("Subtotal: ${num(bom.lines.sumOf { it.meters }, 1)} m", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelLarge)
                    for (c in bom.cables) Text("${c.cores}×${mm2Text(c.mm2)} mm²: ${num(c.net, 1)} m net · ${c.meters} m with reserve")
                }
            },
            confirmButton = { TextButton(onClick = { boardLoads = false }) { Text(stringResource(R.string.ok)) } },
        )
    }
    var editBoardNum by remember { mutableStateOf(false) }
    var editCableLabel by remember { mutableStateOf(false) }
    if (editBoardNum && point != null) TextDialog(stringResource(R.string.board), stringResource(R.string.board_number), if (point.num > 0) point.num.toString() else "", numeric = true,
        onDismiss = { editBoardNum = false }) { t -> editBoardNum = false; vm.commit(PlanOps.updatePoint(plan, point.copy(num = t.trim().toIntOrNull()?.coerceIn(0, 999) ?: 0))) }
    if (editCableLabel && route != null) TextDialog(stringResource(R.string.cable), stringResource(R.string.cable_label), route.label,
        onDismiss = { editCableLabel = false }) { t -> editCableLabel = false; vm.commit(PlanOps.updateRoute(plan, route.copy(label = t.trim()))) }
    if (editBoardName && point != null) TextDialog(stringResource(R.string.board), stringResource(R.string.board_name), point.name,
        onDismiss = { editBoardName = false }) { t -> editBoardName = false; vm.commit(PlanOps.updatePoint(plan, point.copy(name = t.trim()))) }
    if (ed.electricPickDialog) AlertDialog(
        onDismissRequest = { ed.electricPickDialog = false },
        title = { Text("Select electrical items") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Leave checked only the kinds you want to select.", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().clickable { ed.electricPickCables = !ed.electricPickCables }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(ed.electricPickCables, null); Text("Cables")
                }
                for (kind in PointKind.entries) Row(Modifier.fillMaxWidth().clickable {
                    ed.electricPickKinds = if (kind in ed.electricPickKinds) ed.electricPickKinds - kind else ed.electricPickKinds + kind
                }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(kind in ed.electricPickKinds, null); Text(pointName(kind))
                }
            }
        },
        confirmButton = { TextButton(onClick = { ed.electricPickDialog = false }) { Text(stringResource(R.string.ok)) } },
    )

    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
      Column {
        // What the current tool places: always in view, also while something is selected.
        if (!ed.browse && (ed.tool in BUILD || ed.tool == Tool.ELECTRIC || ed.tool == Tool.INTERIOR || ed.tool == Tool.YARD || ed.tool == Tool.SYSTEMS || ed.tool == Tool.SELECT)) {
            Row(Modifier.fillMaxWidth().hScroll().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // ✋ Select: pick and move, drag handles, change; the rest of the row adds new things of this menu
                val rowTool = if (ed.tool == Tool.SELECT) ed.category else ed.tool
                FilterChip(ed.tool == Tool.SELECT && ed.moveMode, onClick = {
                    val on = !(ed.tool == Tool.SELECT && ed.moveMode)
                    finishDrawing(vm, ed)
                    ed.tool = Tool.SELECT; ed.selectOnly = ed.category; ed.moveMode = on; ed.idle = false
                    ed.pointKind = null; ed.cableMode = false; ed.routeFrom = null; ed.linking = null
                    ed.itemKind = null; ed.yardKind = null; ed.systemMode = ""; ed.roadArea = false; vm.assetToPlace = null
                    if (!on) ed.area = false
                }, label = { Text("✋ " + stringResource(R.string.tool_select)) })
                if (ed.tool == Tool.SELECT && ed.moveMode && ed.category == Tool.WALL) AreaChip(ed)
                Box(Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant).align(Alignment.CenterVertically))
                when {
                    rowTool in BUILD && rowTool != Tool.FENCE && rowTool != Tool.ROAD -> {
                      if (ed.tool == Tool.PILLAR) AreaChip(ed)
                      // a gate: which kind first, then a tap in a wall or a fence puts it there
                      if (ed.tool == Tool.GATE) {
                        for (k in GateKind.entries) FilterChip(ed.gateKind == k, onClick = { ed.gateKind = k }, label = { Text(gateName(k)) })
                        Box(Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant).align(Alignment.CenterVertically))
                      }
                      for ((t, label) in listOf(Tool.WALL to R.string.tool_wall, Tool.PILLAR to R.string.pillar, Tool.ROOM to R.string.tool_room, Tool.DOOR to R.string.door, Tool.WINDOW to R.string.window, Tool.GATE to R.string.gate, Tool.STAIRS to R.string.stairs, Tool.ROOF to R.string.roof, Tool.PLATFORM to R.string.platform, Tool.FENCE to R.string.fence, Tool.ROAD to R.string.road, Tool.PLACE to R.string.place))
                        FilterChip(ed.tool == t && !ed.idle, onClick = { finishDrawing(vm, ed); ed.tool = t; ed.idle = false; ed.moveMode = false; if (t != Tool.PILLAR) ed.area = false }, label = { Text(stringResource(label)) })
                    }
                    rowTool == Tool.ROAD -> {
                        // what the road is made of and how wide, then tap its turns
                        for (k in Surface.entries) FilterChip(ed.surface == k, onClick = { ed.surface = k }, label = { Text(surfaceName(k)) },
                            leadingIcon = { Box(Modifier.size(10.dp).background(Color(0xFF000000 or k.color.toLong()), androidx.compose.foundation.shape.CircleShape)) })
                        FilterChip(!ed.roadArea, onClick = { ed.roadArea = false }, label = { Text(stringResource(R.string.route)) })
                        FilterChip(ed.roadArea, onClick = { ed.roadArea = true }, label = { Text(stringResource(R.string.area_shape)) })
                        if (!ed.roadArea) ChipMenu("↔ " + cm(ed.roadWidth.toDouble()) + " ▾", listOf(1000, 1500, 2500, 3500, 4500, 6000).map { cm(it.toDouble()) }) { k -> ed.roadWidth = listOf(1000, 1500, 2500, 3500, 4500, 6000)[k] }
                        for ((t, label) in listOf(Tool.WALL to R.string.tool_wall, Tool.GATE to R.string.gate, Tool.FENCE to R.string.fence, Tool.ROAD to R.string.road))
                            FilterChip(ed.tool == t, onClick = { finishDrawing(vm, ed); ed.tool = t; ed.idle = false }, label = { Text(stringResource(label)) })
                    }
                    rowTool == Tool.FENCE -> {
                        for ((t, label) in listOf(Tool.WALL to R.string.tool_wall, Tool.GATE to R.string.gate, Tool.ROOF to R.string.roof, Tool.PLATFORM to R.string.platform, Tool.FENCE to R.string.fence, Tool.ROAD to R.string.road))
                            FilterChip(ed.tool == t, onClick = { finishDrawing(vm, ed); ed.tool = t; ed.idle = false }, label = { Text(stringResource(label)) })
                        for (k in FenceKind.entries) FilterChip(ed.fenceKind == k, onClick = { ed.fenceKind = k },
                            label = { Text(fenceName(k)) }, leadingIcon = { Box(Modifier.size(10.dp).background(Color(0xFF000000 or k.color.toLong()), androidx.compose.foundation.shape.CircleShape)) })
                    }
                    rowTool == Tool.SYSTEMS -> {
                        for (k in SystemKind.entries) FilterChip(ed.systemMode == "pipe" && ed.system == k, onClick = { ed.system = k; ed.systemMode = "pipe"; ed.pipePts = emptyList() },
                            label = { Text(systemName(k)) }, leadingIcon = { Box(Modifier.size(10.dp).background(Color(0xFF000000 or k.color.toLong()), androidx.compose.foundation.shape.CircleShape)) })
                        FilterChip(ed.systemMode == "radiator", onClick = { ed.systemMode = "radiator"; ed.pipePts = emptyList() }, label = { Text(stringResource(R.string.f_radiator)) })
                        FilterChip(ed.systemMode == "warm", onClick = { ed.systemMode = "warm"; ed.pipePts = emptyList() }, label = { Text(stringResource(R.string.warm_floor)) })
                    }
                    rowTool == Tool.ELECTRIC && ed.category == Tool.INTERIOR -> {
                        FilterChip(true, onClick = { ed.tool = Tool.INTERIOR; ed.pointKind = null }, label = { Text("💡 " + stringResource(R.string.lamp) + " ✓") })
                        Text("Нажмите внутри комнаты, чтобы поставить свет", modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    rowTool == Tool.ELECTRIC -> {
                        FilterChip(Layer.ELECTRIC !in vm.hidden, onClick = { vm.hidden = if (Layer.ELECTRIC in vm.hidden) vm.hidden - Layer.ELECTRIC else vm.hidden + Layer.ELECTRIC },
                            label = { Text("⚡") }, leadingIcon = { Icon(if (Layer.ELECTRIC in vm.hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, Modifier.size(16.dp)) })
                        for (k in listOf(PointKind.SOCKET, PointKind.SWITCH, PointKind.LAMP, PointKind.APPLIANCE, PointKind.BOARD, PointKind.LOW_CURRENT_BOARD, PointKind.LOW_CURRENT_BOX, PointKind.RISER, PointKind.JUNCTION))
                            FilterChip(!ed.cableMode && ed.pointKind == k, onClick = { ed.pointKind = if (!ed.cableMode && ed.pointKind == k) null else k; ed.cableMode = false; ed.routeFrom = null; ed.linking = null }, label = { Text(pointName(k)) })
                        FilterChip(ed.cableMode, onClick = { finishDrawing(vm, ed); ed.cableMode = !ed.cableMode; ed.pointKind = null; ed.routeFrom = null; ed.routeVia = emptyList(); ed.linking = null; ed.sel = null },
                            label = { Text(stringResource(R.string.cable)) }, leadingIcon = { Icon(Icons.Default.Cable, null) })
                        CableTotalsChip(vm.root ?: plan, plan)
                    }
                    else -> {
                        if (rowTool == Tool.INTERIOR) FilterChip(false, onClick = {
                            finishDrawing(vm, ed); ed.tool = Tool.ELECTRIC; ed.pointKind = PointKind.LAMP
                            ed.itemKind = null; ed.yardKind = null; ed.cableMode = false; ed.sel = null
                            vm.hidden = vm.hidden - Layer.ELECTRIC
                        }, label = { Text("💡 " + stringResource(R.string.lamp)) })
                        // indoors (furniture by group) or the yard (canopies, gazebo…), chosen here; the ground opens on the yard
                        val outside = rowTool == Tool.YARD || ed.outdoor ?: (vm.floor == SITE)
                        FilterChip(!outside, onClick = { ed.outdoor = false; ed.yardKind = null }, label = { Text("🛋 " + stringResource(R.string.tool_interior)) })
                        FilterChip(outside, onClick = { ed.outdoor = true; ed.itemKind = null }, label = { Text("🌳 " + stringResource(R.string.yard)) })
                        // Keep the primary Backyard action at the start of the row; it must never be hidden after assets.
                        if (rowTool == Tool.YARD) FilterChip(false, onClick = {
                            finishDrawing(vm, ed); ed.roadArea = true; ed.surface = Surface.GRASS; ed.yardKind = null; ed.itemKind = null
                            DebugLog.add("Backyard draw mode: grass polygon on SITE")
                        }, label = { Text("✏ " + stringResource(R.string.draw_grass_area)) })
                        Box(Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        // the library of 3D assets (from Idea 3D or files): choose one and the floor, then tap where it goes
                        val placing = vm.assetToPlace
                        FilterChip(placing != null || ed.assetsOpen, onClick = { if (placing != null) vm.assetToPlace = null else ed.assetsOpen = true },
                            label = { Text(if (placing != null) "✓ " + placing.name else "📦 " + stringResource(R.string.assets_3d) + " ▾", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.widthIn(max = 200.dp))
                        if (outside) {
                            FilterChip(ed.yardKind == YardKind.TREE, onClick = {
                                finishDrawing(vm, ed); ed.tool = Tool.YARD; ed.yardKind = YardKind.TREE
                                ed.yardSpec = Yard.default(YardKind.TREE); ed.itemKind = null; ed.roadArea = false
                            }, label = { Text("🌱 Посадить дерево") })
                            // Exterior objects are deliberately grouped: greenery first, then built yard pieces.
                            val green = listOf(YardKind.TREE, YardKind.POPLAR, YardKind.CYPRESS, YardKind.ROCK)
                            // GREEN_AREA is kept only to open older saved plans; new lawns are drawn as closed shapes.
                            val built = YardKind.entries.filter { it !in green && it != YardKind.GREEN_AREA }
                            @Composable fun yardMenu(label: String, kinds: List<YardKind>) = ChipMenu(
                                (ed.yardKind?.takeIf { it in kinds }?.let { "✓ " + yardName(it) } ?: label) + " ▾",
                                kinds.map { yardIcon(it) + " " + yardName(it) + if (ed.yardKind == it) " ✓" else "" },
                            ) { i -> ed.yardSetup = kinds[i] }
                            yardMenu("🌿 " + stringResource(R.string.g_greenery), green)
                            FilterChip(false, onClick = {
                                finishDrawing(vm, ed)
                                ed.tool = Tool.YARD; ed.roadArea = true; ed.surface = Surface.GRASS; ed.outdoor = true
                            }, label = { Text("✏ " + stringResource(R.string.draw_grass_area)) })
                            yardMenu("🏡 " + stringResource(R.string.g_yard_build), built)
                            for (id in Catalog.outdoor) FilterChip(ed.itemKind == id, onClick = { ed.itemKind = if (ed.itemKind == id) null else id; ed.yardKind = null }, label = { Text((if (id == "car") "🚗 " else "❄ ") + itemName(id)) })
                        } else {
                            // the room's usual things in groups, each a short list
                            for (g in Catalog.Group.entries) {
                                val ids = g.ids.filter { it !in Catalog.outdoor }
                                val chosen = ed.itemKind?.takeIf { it in ids }
                                ChipMenu((if (chosen != null) "✓ " + itemName(chosen) else groupName(g)) + " ▾",
                                    ids.map { itemName(it) + if (it == ed.itemKind) " ✓" else "" }) { i -> ed.itemKind = ids[i]; ed.yardKind = null }
                            }
                        }
                    }
                }
            }
        }
        // nothing chosen: no row (the hints flash on the plan instead)
        val group = sel as? Sel.GroupSel
        val idle = group == null && manyWalls == null && road == null && feed == null && crossCable == null && model == null && pillar == null && route == null && wall == null && corner == null && room == null && point == null && roof == null && platform == null && fence == null &&
            pipe == null && stair == null && place == null && opening == null && item == null && picked == null && ed.pipePts.isEmpty() && !(ed.tool == Tool.ELECTRIC && ed.cableMode)
        if (!idle) Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                ed.tool == Tool.ELECTRIC && ed.cableMode && ed.routeFrom != null -> {
                    Text(stringResource(R.string.hint_cable_2), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    LevelChips(ed.routeLevel) { ed.routeLevel = it }
                    IconButton(onClick = { if (ed.routeVia.isNotEmpty()) ed.routeVia = ed.routeVia.dropLast(1) else ed.routeFrom = null }) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo)) }
                    IconButton(onClick = { ed.routeFrom = null; ed.routeVia = emptyList() }) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
                }
                ed.tool == Tool.ELECTRIC && ed.cableMode && route == null -> {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.hint_cable_1), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val tot = Cables.totals(listOf(plan)); val mText = stringResource(R.string.m)
                        if (tot.isNotEmpty()) Text(tot.joinToString("  ·  ") { (cores, mm2, mm) -> "$cores×${mm2Text(mm2)} — ${num(mm / 1000, 1)} $mText" },
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    CableTotalsChip(vm.root ?: plan, plan)
                    LevelChips(ed.routeLevel) { ed.routeLevel = it }
                }
                picked != null -> {
                    Column(Modifier.padding(end = 6.dp).widthIn(max = 150.dp)) {
                        Text(stringResource(R.string.n_picked, picked.size), style = MaterialTheme.typography.titleSmall)
                        if (ed.groupFor != null) Text(stringResource(R.string.hint_group), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        else Text(picked.groupBy { it.kind }.map { (k, l) -> pointName(k) + " ×" + l.size }.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PropsRow(Modifier) {
                        AssistChip(onClick = { joinToBoard(picked.map { it.id }) }, label = { Text(stringResource(R.string.join_board)) },
                            leadingIcon = { Icon(Icons.Default.Bolt, null, Modifier.size(16.dp)) })
                        // Several loads in one room (for example a boiler room) often belong to a sub-board.  Give
                        // the whole picked set that board in one action and re-lay their runs there, so a board export
                        // cannot accidentally include them under the nearest/source board.
                        val root = vm.root ?: plan
                        val boards = Boards.all(root)
                        if (boards.size > 1) {
                            val electrical = picked.filter { Circuits.kindOf(it.kind) != null }
                            if (electrical.isNotEmpty()) {
                                val common = electrical.map { it.board }.distinct().singleOrNull()
                                val auto = stringResource(R.string.board_auto)
                                ChipMenu("⚡ " + (common?.let { key -> boards.firstOrNull { it.key == key }?.let { Boards.name(it, lang == "ru") } } ?: auto),
                                    listOf("⚡ $auto" + (if (common == null) " ✓" else "")) + boards.map { b -> "⚡ " + Boards.name(b, lang == "ru") + if (common == b.key) " ✓" else "" }) { i ->
                                    val key = if (i == 0) null else boards[i - 1].key
                                    val changed = plan.copy(points = plan.points.map { p -> if (p.id in electrical.map { it.id }) p.copy(board = key) else p })
                                    val rootWithChoice = root.withFloor(vm.floor, changed)
                                    val rerouted = Autoroute.rewire(changed, electrical.map { it.id }, ed.routeLevel, rootWithChoice, vm.floor)
                                    vm.commit(rerouted)
                                    DebugLog.add("electric: assigned ${electrical.map { it.id }} to board=" + (key?.let { k -> boards.firstOrNull { it.key == k }?.let { Boards.name(it, lang == "ru") } } ?: "auto"))
                                }
                            }
                        }
                        val ids = picked.map { it.id }.toSet()
                        val pointAligns = listOf(
                            Triple(Arrange.Align.MIN, Arrange.Axis.X, R.string.al_left),
                            Triple(Arrange.Align.MID, Arrange.Axis.X, R.string.al_center),
                            Triple(Arrange.Align.MAX, Arrange.Axis.X, R.string.al_right),
                            Triple(Arrange.Align.MIN, Arrange.Axis.Y, R.string.al_top),
                            Triple(Arrange.Align.MID, Arrange.Axis.Y, R.string.al_middle),
                            Triple(Arrange.Align.MAX, Arrange.Axis.Y, R.string.al_bottom),
                        )
                        for ((how, axis, label) in pointAligns)
                            AssistChip(onClick = { vm.commit(Arrange.alignPoints(plan, ids, how, axis)) }, label = { Text(stringResource(label)) })
                        AssistChip(onClick = { vm.commit(Arrange.alignPoints(plan, ids, Arrange.Align.OLDEST)) }, label = { Text(stringResource(R.string.al_oldest)) })
                        if (picked.size >= 3) AssistChip(onClick = { vm.commit(Arrange.spacePoints(plan, ids)) }, label = { Text(stringResource(R.string.al_even)) })
                        val lamps = picked.filter { it.kind == PointKind.LAMP }
                        if (lamps.isNotEmpty()) FilterChip(ed.groupFor != null, onClick = { ed.groupFor = if (ed.groupFor != null) null else lamps.map { it.id }.toSet() },
                            label = { Text(stringResource(R.string.switch_group) + " (${lamps.size})") }, leadingIcon = { Icon(Icons.Default.Link, null, Modifier.size(16.dp)) })
                    }
                    IconButton(onClick = { ed.sel = null; ed.groupFor = null; vm.commit(picked.fold(plan) { p, pt -> PlanOps.deletePoint(p, pt.id) }) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                    IconButton(onClick = { ed.sel = null; ed.groupFor = null }) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
                }
                route != null -> {
                    Column(Modifier.padding(end = 6.dp)) {
                        Text(stringResource(R.string.cable), style = MaterialTheme.typography.titleSmall)
                        Text(num(Cables.length(plan, route) / 1000, 2) + " " + stringResource(R.string.m), style = MaterialTheme.typography.bodySmall)
                        // of it, up and down the walls at its ends
                        Text("↕ " + num(Cables.vertical(plan, route) / 1000, 2) + " " + stringResource(R.string.m), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // the cable itself: wire cross-section and how many wires
                    PropsRow(Modifier) {
                        FilterChip(ed.editRouteId == route.id, onClick = { ed.editRouteId = if (ed.editRouteId == route.id) null else route.id },
                            label = { Text(if (ed.editRouteId == route.id) "Editing" else "Edit") }, leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)) })
                        // the line's number on its board (set it, or leave it numbered in order); the name is optional
                        val lineNo = remember(plan) { Circuits.numbers(plan) }[route.id]
                        AssistChip(onClick = { editLineNo = true }, label = { Text("№ " + (lineNo?.toString() ?: "–")) })
                        AssistChip(onClick = { editCableLabel = true }, label = { Text(route.label.ifBlank { stringResource(R.string.cable_label) }, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)) })
                        ChipMenu(mm2Text(route.mm2) + " " + stringResource(R.string.mm2), Boards.SIZES.map { mm2Text(it) + " " + stringResource(R.string.mm2) }) {
                            vm.commit(PlanOps.updateRoute(plan, route.copy(mm2 = Boards.SIZES[it]))) }
                        ChipMenu("${route.cores} " + stringResource(R.string.wires), Boards.CORES.map { "$it " + stringResource(R.string.wires) }) {
                            vm.commit(PlanOps.updateRoute(plan, route.copy(cores = Boards.CORES[it]))) }
                        LevelChips(route.level) { vm.commit(PlanOps.updateRoute(plan, route.copy(level = it))) }
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteRoute(plan, route.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                feed != null && feedSel != null -> {
                    // a feed cable between boards: its size, the way it goes, or taken away
                    val ru = lang == "ru"
                    val r0 = vm.root
                    fun change(f: (ElPoint) -> ElPoint) { r0?.let { vm.commitHouse(Feeds.setBoard(it, feedSel.toFloor, feedSel.toId, f)) } }
                    Text("↕ " + Boards.name(feed.from, ru) + " → " + Boards.name(feed.to, ru) + " · " + num(feed.length / 1000.0, 1) + " " + stringResource(R.string.m),
                        Modifier.widthIn(max = 210.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PropsRow(Modifier.padding(start = 8.dp)) {
                        ChipMenu("${feed.cores}×${mmTxt(feed.mm2)} " + stringResource(R.string.mm2), Feeds.SIZES.map { "${feed.cores}×" + mmTxt(it) + " " + stringResource(R.string.mm2) }) { i ->
                            change { it.copy(feedMm2 = Feeds.SIZES[i]) } }
                        // the other way round its one turn: along y first instead of x, or back
                        val a = feed.path.first(); val b = feed.path.last()
                        val elbows = listOf(P(b.x, a.y), P(a.x, b.y)).filter { (it - a).len >= 1 && (it - b).len >= 1 }
                        if (elbows.size == 2) AssistChip(onClick = {
                            val now = feed.path.getOrNull(1)
                            val next = if (feed.path.size == 3 && now != null && (now - elbows[0]).len < 1) elbows[1] else elbows[0]
                            change { it.copy(feedVia = listOf(Node(0, next.x.roundToInt(), next.y.roundToInt()))) }
                        }, label = { Text("⇄ " + stringResource(R.string.feed_flip)) })
                        if (feed.to.point.feedVia.isNotEmpty()) AssistChip(onClick = { change { it.copy(feedVia = emptyList()) } }, label = { Text(stringResource(R.string.feed_straight)) })
                        Text(stringResource(R.string.feed_drag), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(max = 160.dp))
                    }
                    // taken away: the board is fed from the mains again
                    IconButton(onClick = { ed.sel = null; change { it.copy(source = null, feedVia = emptyList()) } }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                crossCable != null -> {
                    val root = vm.root
                    val total = root?.let { (CrossCables.horizontal(it, crossCable) + CrossCables.vertical(it, crossCable)) / 1000.0 } ?: 0.0
                    Text("↕ " + crossCable.cable.name.replace('_', ' ') + " · " + num(total, 1) + " " + stringResource(R.string.m),
                        Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { root?.let { vm.commitHouse(it.copy(crossCables = it.crossCables.filter { c -> c.id != crossCable.id })) }; ed.sel = null }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete))
                    }
                }
                group != null -> {
                    // corners or pillars picked by a rectangle: lined up as a row (top / middle / bottom) or a column
                    // (left / middle / right), spaced evenly, corners in straight runs taken out
                    val pillars = group.ofPillars
                    val pts = if (pillars) plan.pillars.filter { it.id in group.pillars }.map { P(it.x.toDouble(), it.y.toDouble()) }
                        else plan.nodes.filter { it.id in group.nodes }.map { P(it.x.toDouble(), it.y.toDouble()) }
                    val house = vm.root
                    val across = group.others.isNotEmpty() && house != null
                    val picks = if (across) group.others + (vm.floor to (group.nodes to group.pillars)) else emptyMap()
                    val total = pts.size + group.otherCount
                    fun set(after: Plan) {
                        vm.commit(after)
                        if (!pillars) ed.sel = group.copy(nodes = group.nodes.filter { after.node(it) != null }.toSet()).takeIf { it.nodes.size + it.otherCount > 0 }
                    }
                    Column {
                        Text(stringResource(if (pillars) R.string.n_pillars else R.string.n_corners, total), style = MaterialTheme.typography.titleSmall)
                        if (across) Text("↕ " + stringResource(R.string.n_floors, picks.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    PropsRow(Modifier.padding(start = 8.dp)) {
                        if (total >= 2) for ((how, axis, label) in listOf(
                            Triple(Arrange.Align.MIN, Arrange.Axis.X, R.string.al_left),
                            Triple(Arrange.Align.MID, Arrange.Axis.X, R.string.al_center),
                            Triple(Arrange.Align.MAX, Arrange.Axis.X, R.string.al_right),
                            Triple(Arrange.Align.MIN, Arrange.Axis.Y, R.string.al_top),
                            Triple(Arrange.Align.MID, Arrange.Axis.Y, R.string.al_middle),
                            Triple(Arrange.Align.MAX, Arrange.Axis.Y, R.string.al_bottom),
                        ))
                            AssistChip(onClick = {
                                if (across) {
                                    // lined up on every floor picked, one line for them all
                                    val after = Arrange.alignAcross(house!!, picks, how, axis)
                                    vm.commitHouse(after)
                                    if (!pillars) ed.sel = group.copy(nodes = group.nodes.filter { after.floor(vm.floor).node(it) != null }.toSet(),
                                        others = group.others.mapValues { (f, s) -> s.first.filter { after.floor(f).node(it) != null }.toSet() to s.second })
                                } else set(if (pillars) Arrange.alignPillars(plan, group.pillars, how, axis) else Arrange.alignNodes(plan, group.nodes, how, axis))
                            }, label = { Text(stringResource(label)) })
                        if (pts.size >= 3) AssistChip(onClick = { set(if (pillars) Arrange.spacePillars(plan, group.pillars) else Arrange.spaceNodes(plan, group.nodes)) }, label = { Text(stringResource(R.string.al_even)) })
                        // with all floors picked these work on every floor picked, else on this one
                        val pillarPicks = if (across) picks.mapValues { it.value.second }.filterValues { it.isNotEmpty() } else mapOf(vm.floor to group.pillars)
                        if (pillars) AssistChip(onClick = {
                            val r0 = house ?: return@AssistChip
                            val (after, n) = Pillars.centerOnAxesAcross(r0, pillarPicks)
                            if (n > 0) { vm.commitHouse(after); ed.toastText = centeredText.format(n) } else ed.toastText = onAxesText
                        }, label = { Text("⌖ " + stringResource(R.string.on_axis)) })
                        if (pillars && (belowPillarsPanel.isNotEmpty() || across)) AssistChip(onClick = {
                            val r0 = house ?: return@AssistChip
                            val (after, n) = Pillars.alignToBelowAcross(r0, pillarPicks)
                            if (n > 0) { vm.commitHouse(after); ed.toastText = alignedText.format(n) } else ed.toastText = noBelowText
                        }, label = { Text("⇣ " + stringResource(R.string.over_below)) })
                        // the walls between the corners picked, to change them together
                        if (!pillars) plan.walls.filter { it.a in group.nodes && it.b in group.nodes }.map { it.id }.toSet().takeIf { it.isNotEmpty() }?.let { ws ->
                            AssistChip(onClick = { ed.sel = if (ws.size == 1) Sel.WallSel(ws.first()) else Sel.WallsSel(ws) }, label = { Text("▭ " + wallsText.format(ws.size)) })
                        }
                        // Material is a house-level edit: keep the wall ids for every selected floor.
                        // The old wall chip above deliberately remains a normal per-floor selection.
                        if (!pillars && across) {
                            val wallPicks = picks.mapNotNull { (floorNo, selected) ->
                                val ids = house!!.floor(floorNo).walls
                                    .filter { it.a in selected.first && it.b in selected.first }
                                    .map { it.id }.toSet()
                                (floorNo to ids).takeIf { ids.isNotEmpty() }
                            }.toMap()
                            if (wallPicks.isNotEmpty()) FacingChip(null, mixed = false) { facingAcross = wallPicks }
                        }
                        // corners onto the pillars' grid lines (every floor picked)
                        if (!pillars) AssistChip(onClick = {
                            val r0 = house ?: return@AssistChip
                            val nodePicks = if (across) picks.mapValues { it.value.first }.filterValues { it.isNotEmpty() } else mapOf(vm.floor to group.nodes)
                            val (after, n) = Arrange.nodesToAxes(r0, nodePicks)
                            if (n > 0) {
                                vm.commitHouse(after); ed.toastText = centeredText.format(n)
                                ed.sel = group.copy(nodes = group.nodes.filter { after.floor(vm.floor).node(it) != null }.toSet(),
                                    others = group.others.mapValues { (f, s2) -> s2.first.filter { after.floor(f).node(it) != null }.toSet() to s2.second })
                            } else ed.toastText = onAxesText
                        }, label = { Text("⌖ " + stringResource(R.string.on_axis)) })
                        val simplified = stringResource(R.string.simplified)
                        if (!pillars) AssistChip(onClick = {
                            val (after, gone) = Arrange.simplify(plan, group.nodes)
                            if (gone > 0) set(after)
                            ed.toastText = simplified.format(gone)
                        }, label = { Text(stringResource(R.string.simplify)) })
                    }
                    IconButton(onClick = {
                        ed.sel = null
                        vm.commit(if (pillars) plan.copy(pillars = plan.pillars.filter { it.id !in group.pillars })
                            else PlanOps.finish(plan.copy(walls = plan.walls.filter { it.a !in group.nodes && it.b !in group.nodes })))
                    }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                pillar != null -> {
                    Text(stringResource(R.string.pillar), style = MaterialTheme.typography.titleSmall)
                    PropsRow(Modifier.padding(start = 8.dp)) {
                        AssistChip(onClick = { editPillarSize = true }, label = { Text(if (pillar.round) "Ø " + cm(pillar.w.toDouble()) else cm(pillar.w.toDouble()) + "×" + cm(pillar.d.toDouble())) },
                            leadingIcon = { Icon(Icons.Default.Edit, null) })
                        AssistChip(onClick = { editPillarH = true }, label = { Text("↕ " + cm(Pillars.height(plan, pillar).toDouble())) })
                        // a little off the grid line it should be on: put it on the line (x and y)
                        val target = Pillars.axisTarget(pillar, plan.pillars.filter { it.id != pillar.id } + otherFloorPillars + (vm.root?.site?.pillars.orEmpty().takeIf { vm.floor != SITE } ?: emptyList()))
                        if (target.first != null || target.second != null) AssistChip(onClick = {
                            val r0 = vm.root ?: return@AssistChip
                            val (after, n) = Pillars.centerOnAxesAcross(r0, mapOf(vm.floor to setOf(pillar.id)))
                            if (n > 0) { vm.commitHouse(after); ed.toastText = centeredText.format(n) }
                        }, label = { Text("⌖ " + stringResource(R.string.on_axis)) }, colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFE0353B)))
                        // not over a pillar of the floor below: put it exactly over the nearest one
                        if (pillar.id in offPillarsPanel) AssistChip(onClick = {
                            val (ps, n) = Pillars.alignToBelow(plan.pillars, belowPillarsPanel, setOf(pillar.id))
                            if (n > 0) { vm.commit(plan.copy(pillars = ps)); ed.toastText = alignedText.format(n) } else ed.toastText = noBelowText
                        }, label = { Text("⇣ " + stringResource(R.string.over_below)) }, colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFE0353B)))
                        FilterChip(!pillar.round, onClick = { vm.commit(PlanOps.updatePillar(plan, pillar.copy(round = false))) }, label = { Text(stringResource(R.string.square)) })
                        FilterChip(pillar.round, onClick = { vm.commit(PlanOps.updatePillar(plan, pillar.copy(round = true, d = pillar.w))) }, label = { Text(stringResource(R.string.round)) })
                        for (sz in listOf(300, 400, 500)) FilterChip(pillar.w == sz && (pillar.round || pillar.d == sz), onClick = { vm.commit(PlanOps.updatePillar(plan, pillar.copy(w = sz, d = sz))) }, label = { Text("${sz / 10}") })
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deletePillar(plan, pillar.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                manyWalls != null -> {
                    val ids = manyWalls.map { it.id }.toSet()
                    Column(Modifier.padding(end = 6.dp)) {
                        Text(wallsText.format(ids.size), style = MaterialTheme.typography.titleSmall)
                        Text(num(manyWalls.sumOf { plan.length(it) } / 1000, 2) + " " + stringResource(R.string.m), style = MaterialTheme.typography.bodySmall)
                    }
                    val th = manyWalls.map { it.thickness }.distinct(); val hs = manyWalls.map { plan.heightOf(it) }.distinct()
                    val fs = manyWalls.map { w -> Facings.sides(plan, w, rooms).let { it.first ?: it.second } }.distinct()
                    PropsRow(Modifier) {
                        FacingChip(fs.singleOrNull(), mixed = fs.size > 1) { facingFor = ids }
                        AssistChip(onClick = { widthFor = ids }, label = { Text("↔ " + (th.singleOrNull()?.let { cm(it.toDouble()) } ?: "…")) })
                        AssistChip(onClick = { heightFor = ids }, label = { Text("↕ " + (hs.singleOrNull()?.let { cm(it.toDouble()) } ?: "…")) })
                        WallPickChips(ed, plan, ids)
                        for (t in listOf(100, 200, 300)) FilterChip(th.singleOrNull() == t, onClick = { vm.commit(PlanOps.editWalls(plan, ids) { it.copy(thickness = t) }) }, label = { Text("${t / 10}") })
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteWalls(plan, ids)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                    IconButton(onClick = { ed.sel = null; ed.pickWalls = false }) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
                }
                wall != null -> {
                    AssistChip(onClick = { editLength = true }, label = { Text(cm(plan.length(wall)) + " " + stringResource(R.string.cm)) },
                        leadingIcon = { Icon(Icons.Default.Edit, stringResource(R.string.length)) })
                    AssistChip(onClick = { editWallH = true }, label = { Text("↕ " + cm(plan.heightOf(wall).toDouble())) }, modifier = Modifier.padding(start = 6.dp))
                    Stairs.allClashes(plan)[wall.id]?.takeIf { it >= 200 }?.let { fit ->
                        // only when the user taps it: lower this one wall so the stairs pass over it
                        AssistChip(onClick = { vm.commit(PlanOps.setWallHeight(plan, wall.id, fit)) }, label = { Text("↓ " + cm(fit.toDouble())) },
                            modifier = Modifier.padding(start = 4.dp))
                    }
                    PropsRow(Modifier.padding(start = 6.dp)) {
                        AssistChip(onClick = { widthFor = setOf(wall.id) }, label = { Text("↔ " + cm(wall.thickness.toDouble())) })
                        val (wl, wr) = Facings.sides(plan, wall, rooms)
                        FacingChip(wl ?: wr, mixed = wl != null && wr != null && wl != wr) { facingFor = setOf(wall.id) }
                        WallPickChips(ed, plan, setOf(wall.id))
                        for (t in listOf(100, 200, 300)) FilterChip(wall.thickness == t, onClick = { vm.commit(PlanOps.setThickness(plan, wall.id, t)) }, label = { Text("${t / 10}") })
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteWall(plan, wall.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                corner != null -> {
                    Text(stringResource(R.string.corner), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteNode(plan, corner.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                room != null -> {
                    Column(Modifier.weight(1f)) {
                        Text(room.info?.name?.takeIf { it.isNotEmpty() } ?: room.info?.type?.label(lang) ?: stringResource(R.string.room),
                            style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (ed.category == Tool.WALL && ed.tool == Tool.SELECT && ed.moveMode) {
                            Text(stringResource(R.string.area) + " " + num(room.area) + " " + stringResource(R.string.m2) + " · " +
                                stringResource(R.string.perimeter) + " " + num(room.perimeter) + " " + stringResource(R.string.m),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { vm.insideAt = room.anchor; vm.screen = Screen.VIEW3D }) { Icon(Icons.Default.ViewInAr, stringResource(R.string.view_inside)) }
                    IconButton(onClick = { editRoom = true }) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
                }
                point != null -> {
                    val pointTitle = if (point.kind == PointKind.BOARD) {
                        val root = vm.root ?: plan
                        val own = Boards.all(root).firstOrNull { it.floor == vm.floor && it.point.id == point.id } ?: Boards.Ref(vm.floor, point, 0)
                        pointName(point.kind) + ": " + Boards.name(own, lang == "ru")
                    } else pointName(point.kind)
                    Text(pointTitle, style = MaterialTheme.typography.titleSmall)
                    if (CrossCables.endpoint(point) && vm.root != null) {
                        fun cableName(c: CommCable) = when (c) {
                            CommCable.POWER -> "Power"; CommCable.CAT5E -> "Cat5e"; CommCable.CAT6 -> "Cat6"; CommCable.CAT6A -> "Cat6A"
                            CommCable.FIBER -> "Fiber"; CommCable.COAX -> "Coax"; CommCable.ALARM -> "Alarm"; CommCable.INTERCOM -> "Intercom"
                        }
                        PropsRow(Modifier.padding(start = 8.dp)) {
                            ChipMenu("↕ " + when (ed.crossLevel) { CrossLevel.CEILING -> "Ceiling"; CrossLevel.FLOOR -> "Floor"; CrossLevel.WALL -> "Wall" },
                                listOf("Ceiling", "Floor", "Wall")) { ed.crossLevel = CrossLevel.entries[it] }
                            ChipMenu(cableName(ed.crossCable), CommCable.entries.map(::cableName)) { ed.crossCable = CommCable.entries[it] }
                            AssistChip(onClick = {
                                if (point.kind in setOf(PointKind.LOW_CURRENT_BOARD, PointKind.LOW_CURRENT_BOX) && ed.crossCable == CommCable.POWER) ed.crossCable = CommCable.CAT6
                                ed.crossFrom = vm.floor to point.id; ed.crossPickFloor = true
                            }, label = { Text("↕ Other floor") })
                        }
                    }
                    if (point.kind == PointKind.JUNCTION) {
                        Text(stringResource(R.string.junction_hint), Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (point.kind == PointKind.RISER) {
                        Text("↕", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        BoardLinesChip(plan, point)
                    } else if (point.kind == PointKind.APPLIANCE) {
                        AssistChip(onClick = { editAppliance = true }, label = {
                            Text((point.name.ifBlank { "?" }) + " · " + num(point.kw, 1) + " " + stringResource(R.string.kw), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }, modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false))
                        AssistChip(onClick = { editPoint = true }, label = { Text("↕ " + cm(point.height.toDouble())) }, modifier = Modifier.padding(start = 6.dp))
                        Box(Modifier.weight(0.01f))
                    } else if (point.kind == PointKind.BOARD) {
                        val root = vm.root ?: plan
                        val me = Boards.all(root).firstOrNull { it.floor == vm.floor && it.point.id == point.id } ?: Boards.Ref(vm.floor, point, 0)
                        val ownBom = Materials.boardBom(root, me, lang == "ru")
                        val ownProject = com.dawoyan.elektronik.core.Roomik.singleBoard(Materials.elektronikBoardFile(root, me, lang == "ru"))
                        val ownLoads = ownProject?.rooms.orEmpty().sumOf { room -> room.loads.sumOf { it.count } }
                        Text("Loads: $ownLoads · Cable: ${num(ownBom.lines.sumOf { it.meters }, 1)} m", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PropsRow(Modifier.padding(start = 8.dp)) {
                            // board settings: where it is fed from (the mains or another board) and the feed cable
                            val src = Boards.all(root).firstOrNull { it.key == point.source }
                            AssistChip(onClick = { boardSettings = true }, label = { Text("⇠ " + (src?.let { Boards.name(it, lang == "ru") } ?: stringResource(R.string.board_mains)), maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.Settings, null, Modifier.size(16.dp)) }, modifier = Modifier.padding(end = 6.dp))
                            AssistChip(onClick = { editBoardName = true }, label = { Text(me?.let { Boards.name(it, lang == "ru") } ?: point.name, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { editBoardNum = true }, label = { Text("№ " + (if (point.num > 0) point.num else (me?.index ?: 0) + 1)) }, modifier = Modifier.padding(start = 6.dp))
                            BoardLinesChip(plan, point)
                            AssistChip(onClick = { boardLoads = true }, label = { Text("Loads · ${Materials.boardBom(root, me, lang == "ru").lines.size}") }, modifier = Modifier.padding(start = 6.dp))
                            me?.let { m -> AssistChip(onClick = { boardExports.sendBoard(m) }, label = { Text("⇢ elektronik") }, modifier = Modifier.padding(start = 6.dp)) }
                            AssistChip(onClick = { editPoint = true }, label = { Text("↕ " + cm(point.height.toDouble())) }, modifier = Modifier.padding(start = 6.dp))
                        }
                    } else if (point.kind == PointKind.LAMP) {
                        PropsRow(Modifier.padding(start = 8.dp)) {
                            AssistChip(onClick = { editPoint = true }, label = { Text("${point.watts} " + stringResource(R.string.watt)) })
                            point.sourceId?.let { id -> plan.points.firstOrNull { it.id == id } }?.let { source ->
                                AssistChip(onClick = { }, label = { Text("← " + pointName(source.kind) + " #${source.id}") }, modifier = Modifier.padding(start = 6.dp))
                            }
                            // what it is switched by: a switch (nearest first) or straight from a breaker in the board
                            val sws = plan.points.filter { it.kind == PointKind.SWITCH }.sortedBy { kotlin.math.abs(it.x - point.x) + kotlin.math.abs(it.y - point.y) }
                            val swName = stringResource(R.string.switch_); val fromBoard = stringResource(R.string.lamp_from_board)
                            fun switchId(sw: ElPoint): String {
                                val root = vm.root ?: plan
                                val b = Boards.of(root, Boards.all(root), vm.floor, sw)
                                val bn = b?.point?.num?.takeIf { it > 0 } ?: b?.index?.plus(1) ?: 1
                                return "B$bn-S${sw.id}"
                            }
                            fun swLabel(sw: ElPoint) = switchId(sw) + (sw.name.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "") +
                                (if (sw.pairId != null) " ⇄" else "") + " · " + num(Math.hypot((sw.x - point.x).toDouble(), (sw.y - point.y).toDouble()) / 1000, 1) + " m"
                            val cur = sws.firstOrNull { it.id == point.switchId }
                            ChipMenu(if (cur != null) "🔘 " + switchId(cur) else "⚡ " + fromBoard,
                                listOf("⚡ " + fromBoard + if (cur == null) " ✓" else "") + sws.map { "🔘 " + swLabel(it) + if (it.id == point.switchId) " ✓" else "" }) { i ->
                                val selected = if (i == 0) null else sws[i - 1]
                                val np = PlanOps.updatePoint(plan, point.copy(switchId = selected?.id, sourceId = null))
                                DebugLog.add("electric: lamp #${point.id} linked to ${selected?.let(::switchId) ?: "board"}")
                                // A previously unconnected lamp needs its first physical cable too.
                                vm.commit(if (Circuits.toBoard(plan, point.id).isNotEmpty() || point.kind == PointKind.LAMP) Autoroute.rewire(np, point.id, ed.routeLevel, vm.root, vm.floor) else np)
                            }
                        }
                    } else PropsRow(Modifier) {
                        val max = if (point.kind == PointKind.SOCKET) 5 else 3
                        IconButton(onClick = { vm.commit(PlanOps.updatePoint(plan, point.copy(count = point.count - 1))) }, enabled = point.count > 1) { Icon(Icons.Default.Remove, null) }
                        Text("×${point.count}", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { vm.commit(PlanOps.updatePoint(plan, point.copy(count = point.count + 1))) }, enabled = point.count < max) { Icon(Icons.Default.Add, null) }
                        AssistChip(onClick = { editPoint = true }, label = { Text("↕ " + cm(point.height.toDouble())) })
                        // a 110 V socket: a line and cable of its own
                        if (point.kind == PointKind.SOCKET) FilterChip(point.volts == 110, onClick = {
                            val np = PlanOps.updatePoint(plan, point.copy(volts = if (point.volts == 110) 220 else 110))
                            vm.commit(if (Circuits.toBoard(plan, point.id).isNotEmpty()) Autoroute.rewire(np, point.id, ed.routeLevel, vm.root, vm.floor) else np)
                        }, label = { Text("110 V") }, modifier = Modifier.padding(start = 6.dp))
                        // a socket straight from the board (a line of its own) instead of going on from the one before
                        if (point.kind == PointKind.SOCKET) FilterChip(point.ownLine, onClick = {
                            val np = PlanOps.updatePoint(plan, point.copy(ownLine = !point.ownLine))
                            DebugLog.add("electric: socket #${point.id} own-line=${!point.ownLine}; rerouting ${if (!point.ownLine) "direct board run" else "shared run"}")
                            vm.commit(if (Circuits.toBoard(plan, point.id).isNotEmpty()) Autoroute.rewire(np, point.id, ed.routeLevel, vm.root, vm.floor) else np)
                        }, label = { Text(stringResource(R.string.own_line_short)) }, leadingIcon = { Icon(Icons.Default.Bolt, null, Modifier.size(16.dp)) }, modifier = Modifier.padding(start = 6.dp))
                        if (point.kind == PointKind.SWITCH) FilterChip(ed.linking == point.id, onClick = { ed.linking = if (ed.linking == point.id) null else point.id },
                            label = { Text(stringResource(R.string.link_lamps) + " " + plan.points.count { it.switchId == point.id }) }, leadingIcon = { Icon(Icons.Default.Link, null) },
                            modifier = Modifier.padding(start = 6.dp))
                        if (point.kind == PointKind.SWITCH) AssistChip(onClick = { editSwitchLabel = true }, label = {
                            Text(if (point.name.isBlank()) "B?-S${point.id}" else point.name)
                        }, modifier = Modifier.padding(start = 6.dp))
                        // two-way: a second switch somewhere else (another door) works the same lamps
                        if (point.kind == PointKind.SWITCH) FilterChip(point.pairId != null || ed.pairing == point.id, onClick = {
                            when {
                                point.pairId != null -> {
                                    val np = PlanOps.pairSwitches(plan, point.id, point.id)
                                    vm.commit(if (Circuits.toBoard(plan, point.id).isNotEmpty()) Autoroute.rewire(np, listOfNotNull(point.id, point.pairId), ed.routeLevel, vm.root, vm.floor) else np)
                                }
                                ed.pairing == point.id -> ed.pairing = null
                                else -> { ed.pairing = point.id; ed.linking = null; ed.toast = R.string.pair_tap }
                            }
                        }, label = { Text("⇄ " + stringResource(if (point.pairId != null) R.string.two_way_on else R.string.two_way)) }, modifier = Modifier.padding(start = 6.dp))
                    }
                    if (point.kind != PointKind.BOARD && point.kind != PointKind.RISER && point.kind != PointKind.JUNCTION) {
                        // with several boards: which one feeds this point
                        val root = vm.root ?: plan
                        val boards = Boards.all(root)
                        if (boards.size > 1) {
                            val ru = lang == "ru"
                            val feeds = Boards.of(root, boards, vm.floor, point)
                            val auto = stringResource(R.string.board_auto)
                            ChipMenu("⚡" + (feeds?.let { Boards.name(it, ru) } ?: "?").let { if (it.length > 9) it.take(8) + "…" else it },
                                listOf("⚡ $auto" + (if (point.board == null) " ✓" else "")) + boards.map { "⚡ " + Boards.name(it, ru) + if (point.board == it.key) " ✓" else "" },
                                Modifier.widthIn(max = 130.dp)) { i ->
                                val np = PlanOps.updatePoint(plan, point.copy(board = if (i == 0) null else boards[i - 1].key))
                                // Make the new board choice visible to the router, not only to this floor panel.
                                val rootWithChoice = (vm.root ?: plan).withFloor(vm.floor, np)
                                // wired already: its cable goes to the board now chosen
                                // A device source selected by long-press is a deliberate upstream connection. Changing
                                // its board label must not destroy that cable and replace it with an automatic board run.
                                vm.commit(if (point.sourceId == null && Circuits.toBoard(plan, point.id).isNotEmpty()) Autoroute.rewire(np, point.id, ed.routeLevel, rootWithChoice, vm.floor) else np)
                                DebugLog.add("electric: ${point.kind} #${point.id} board=${if (i == 0) "auto" else Boards.name(boards[i - 1], ru)}") }
                        }
                    }
                    if (Circuits.kindOf(point.kind) != null && Circuits.toBoard(plan, point.id).isEmpty())
                        IconButton(onClick = { joinToBoard(listOf(point.id)) }) { Icon(Icons.Default.Bolt, stringResource(R.string.join_board)) }
                    IconButton(onClick = {
                        val branches = plan.routes.count { it.from == point.id || it.to == point.id }
                        DebugLog.add("electric: deleting ${point.kind} #${point.id}, attached cables=$branches; preserving downstream feed")
                        ed.sel = null; vm.commit(PlanOps.deletePoint(plan, point.id))
                    }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                (ed.tool == Tool.FENCE || ed.tool == Tool.ROAD) && ed.pipePts.isNotEmpty() -> {
                    Text(stringResource(R.string.hint_pipe_2), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { ed.pipePts = ed.pipePts.dropLast(1) }) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo)) }
                    IconButton(onClick = { ed.pipePts = emptyList() }) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
                    FilledTonalButton(onClick = { finishDrawing(vm, ed) }, enabled = ed.pipePts.size >= 2) { Icon(Icons.Default.Check, stringResource(R.string.ok)) }
                }
                roof != null -> {
                    PropsRow(Modifier) {
                        // sizes first: base height and pitch, then the shape
                        AssistChip(onClick = { editRoofBase = true }, label = { Text(stringResource(R.string.roof_base_chip, cm(Roofs.baseOf(plan, roof).toDouble())) + if (roof.base == null) " (auto)" else "") })
                        AssistChip(onClick = { editRoof = true }, label = { Text("${roof.pitch}°") })
                        // what covers it, and its colour
                        ChipMenu(roofCoverName(roof.cover) + " ▾", RoofCover.entries.map { roofCoverName(it) }) { k -> vm.commit(PlanOps.updateRoof(plan, roof.copy(cover = RoofCover.entries[k]))) }
                        AssistChip(onClick = { roofColor = true }, label = { Text(stringResource(R.string.colour)) },
                            leadingIcon = { Box(Modifier.size(16.dp).background(Color(0xFF000000 or roof.shownColor.toLong()), RoundedCornerShape(3.dp))) })
                        for (k in RoofKind.entries) FilterChip(roof.kind == k, onClick = { vm.commit(PlanOps.updateRoof(plan, roof.copy(kind = k))) }, label = { Text(roofName(k)) })
                        FilterChip(roof.attic, onClick = { vm.commit(PlanOps.updateRoof(plan, roof.copy(attic = !roof.attic))) }, label = { Text(stringResource(R.string.attic)) })
                    }
                    IconButton(onClick = { vm.commit(PlanOps.updateRoof(plan, if (roof.kind == RoofKind.SHED && !roof.alongX) roof.copy(alongX = true, flip = !roof.flip) else roof.copy(alongX = !roof.alongX))) }) {
                        Icon(Icons.Default.RotateRight, stringResource(R.string.roof_turn))
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteRoof(plan, roof.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                platform != null -> {
                    PropsRow(Modifier) {
                        AssistChip(onClick = { editPlatform = true }, label = { Text("↥ " + cm(platform.height.toDouble())) })
                        AssistChip(onClick = { editPlatformSize = true }, label = { Text(cm((platform.x2 - platform.x1).toDouble()) + " × " + cm((platform.y2 - platform.y1).toDouble())) })
                        FilterChip(platform.railing, onClick = { vm.commit(PlanOps.updatePlatform(plan, platform.copy(railing = !platform.railing))) }, label = { Text(stringResource(R.string.railing)) })
                        FilterChip(platform.columns, onClick = { vm.commit(PlanOps.updatePlatform(plan, platform.copy(columns = !platform.columns))) }, label = { Text(stringResource(R.string.columns)) })
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deletePlatform(plan, platform.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                fence != null -> {
                    PropsRow(Modifier) {
                        val kinds = GateKind.entries
                        if (ed.gatePlacing != null || ed.gateMoving)
                            FilterChip(true, onClick = { ed.gatePlacing = null; ed.gateMoving = false }, label = { Text("✕ " + stringResource(R.string.gate_tap)) })
                        else if (gate == null)
                            ChipMenu("+ " + stringResource(R.string.gate), kinds.map { gateName(it) }) { ed.gatePlacing = kinds[it] }
                        if (gate != null && !ed.gateMoving) {
                            // the gate picked: its kind, width, place; gone with ✕
                            ChipMenu(gateName(gate.kind), kinds.map { gateName(it) }) { k ->
                                vm.commit(PlanOps.updateFence(plan, fence.copy(gates = fence.gates.mapIndexed { i, g -> if (i == ed.gateIndex) g.copy(kind = kinds[k], width = kinds[k].width) else g }))) }
                            AssistChip(onClick = { editGateW = true }, label = { Text("↔ " + cm(gate.width.toDouble())) })
                            AssistChip(onClick = { ed.gateMoving = true }, label = { Text(stringResource(R.string.gate_move)) })
                            AssistChip(onClick = { val gi = ed.gateIndex; ed.gateIndex = null
                                vm.commit(PlanOps.updateFence(plan, fence.copy(gates = fence.gates.filterIndexed { i, _ -> i != gi }))) },
                                label = { Text("✕ " + stringResource(R.string.gate)) })
                            AssistChip(onClick = { ed.gateIndex = null }, label = { Text("↩") })
                        }
                        if (gate == null) for (k in FenceKind.entries) FilterChip(fence.kind == k, onClick = { vm.commit(PlanOps.updateFence(plan, fence.copy(kind = k))) }, label = { Text(fenceName(k)) })
                    }
                    AssistChip(onClick = { editFenceH = true }, label = { Text("↕ " + cm(fence.height.toDouble())) }, modifier = Modifier.padding(start = 4.dp))
                    Fences.facingOf(fence)?.let { f ->
                        Spacer(Modifier.width(4.dp)); FacingChip(f) { facingFence = true }
                        if (fence.kind == FenceKind.STONE) AssistChip(onClick = { fenceWidth = true }, label = { Text("↔ " + cm(fence.thickness.toDouble())) }, modifier = Modifier.padding(start = 4.dp))
                        val postNames = listOf(stringResource(R.string.posts_own), stringResource(R.string.posts_concrete), stringResource(R.string.posts_none))
                        ChipMenu(postNames[fence.posts.ordinal] + " ▾", postNames, Modifier.padding(start = 4.dp)) { k -> vm.commit(PlanOps.updateFence(plan, fence.copy(posts = FencePosts.entries[k]))) }
                        if (fence.posts != FencePosts.NONE) AssistChip(onClick = { fenceStep = true }, label = { Text("⇹ " + cm(fence.postStep.toDouble())) }, modifier = Modifier.padding(start = 4.dp))
                    }
                    if (facingFence) FacingDialog(Fences.facingOf(fence), allowNone = false, onDismiss = { facingFence = false }) { _, f ->
                        facingFence = false
                        vm.commit(PlanOps.updateFence(plan, fence.copy(facing = f)))
                    }
                    if (fenceStep) TextDialog(stringResource(R.string.fence), stringResource(R.string.post_step_cm), cm(fence.postStep.toDouble()), numeric = true,
                        onDismiss = { fenceStep = false }) { t ->
                        fenceStep = false
                        t.replace(',', '.').toDoubleOrNull()?.let { v -> vm.commit(PlanOps.updateFence(plan, fence.copy(postStep = (v * 10).roundToInt().coerceIn(800, 10000)))) }
                    }
                    if (fenceWidth) TextDialog(stringResource(R.string.fence), stringResource(R.string.wall_width_cm), cm(fence.thickness.toDouble()), numeric = true,
                        onDismiss = { fenceWidth = false }) { t ->
                        fenceWidth = false
                        t.replace(',', '.').toDoubleOrNull()?.let { v -> vm.commit(PlanOps.updateFence(plan, fence.copy(thickness = (v * 10).roundToInt().coerceIn(80, 800)))) }
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteFence(plan, fence.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                ed.tool == Tool.SYSTEMS && ed.pipePts.isNotEmpty() -> {
                    Text(stringResource(R.string.hint_pipe_2), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { ed.pipePts = ed.pipePts.dropLast(1) }) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo)) }
                    IconButton(onClick = { ed.pipePts = emptyList() }) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
                    FilledTonalButton(onClick = {
                        val sys = ed.system ?: SystemKind.WATER_COLD
                        val after = PlanOps.addPipe(plan, sys, ed.pipePts)
                        if (after !== plan) { vm.commit(after); ed.sel = Sel.PipeSel(after.pipes.last().id) }
                        ed.pipePts = emptyList()
                    }, enabled = ed.pipePts.size >= 2) { Icon(Icons.Default.Check, stringResource(R.string.ok)) }
                }
                pipe != null -> {
                    Column(Modifier.padding(end = 6.dp)) {
                        Text(systemName(pipe.system), style = MaterialTheme.typography.titleSmall)
                        Text(num(pipeLength(pipe) / 1000, 2) + " " + stringResource(R.string.m), style = MaterialTheme.typography.bodySmall)
                    }
                    val szFmt = stringResource(if (pipe.system == SystemKind.NETWORK) R.string.network_wires else R.string.pipe_d)
                    val sz = { v: Int -> String.format(szFmt, v) }
                    PropsRow(Modifier) {
                        if (pipe.system == SystemKind.NETWORK) ChipMenu(pipe.cable.name.replace('_', ' '),
                            CommCable.entries.filter { it != CommCable.POWER }.map { it.name.replace('_', ' ') }) { i ->
                            vm.commit(PlanOps.updatePipe(plan, pipe.copy(cable = CommCable.entries.filter { it != CommCable.POWER }[i])))
                        }
                        ChipMenu(sz(pipe.sizeOrDefault()), pipe.system.sizes.map { sz(it) }) { vm.commit(PlanOps.updatePipe(plan, pipe.copy(size = pipe.system.sizes[it]))) }
                        val side = stringResource(R.string.side_by_side)
                        ChipMenu("×${pipe.count}", (1..6).map { "×$it $side" }) { vm.commit(PlanOps.updatePipe(plan, pipe.copy(count = it + 1))) }
                        AssistChip(onClick = { editPipeH = true }, label = { Text("↕ " + cm(pipe.height.toDouble())) })
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deletePipe(plan, pipe.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                stair != null -> {
                    PropsRow(Modifier) {
                        for (k in StairKind.entries) FilterChip(stair.kind == k, onClick = { setStair(stair.copy(kind = k)) },
                            label = { StairIcon(k, stair.kind == k) })
                        AssistChip(onClick = { editStair = true }, label = { Text(cm(stair.width.toDouble()) + "×" + cm(stair.length.toDouble())) })
                    }
                    IconButton(onClick = { setStair(stair.copy(rot = (stair.rot + 270) % 360)) }) { Icon(Icons.Default.RotateLeft, stringResource(R.string.rotate)) }
                    IconButton(onClick = { setStair(stair.copy(rot = (stair.rot + 90) % 360)) }) { Icon(Icons.Default.RotateRight, stringResource(R.string.rotate)) }
                    IconButton(onClick = { setStair(stair.copy(reversed = !stair.reversed)) }) { Icon(Icons.Default.SwapVert, stringResource(R.string.stair_flip)) }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteStair(plan, stair.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                place != null -> {
                    Column(Modifier.weight(1f)) {
                        Text(place.name.ifBlank { place.type?.label(lang) ?: stringResource(R.string.place) }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.place_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { editPlace = true }) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deletePlace(plan, place.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                road != null -> {
                    Column(Modifier.padding(end = 6.dp)) {
                        Text(stringResource(if (road.closed) R.string.backyard else R.string.road), style = MaterialTheme.typography.titleSmall)
                        val len = (1 until road.points.size).sumOf { k -> (P(road.points[k].x.toDouble(), road.points[k].y.toDouble()) - P(road.points[k - 1].x.toDouble(), road.points[k - 1].y.toDouble())).len }
                        Text(num(len / 1000, 1) + " " + stringResource(R.string.m) + " · " + num(len * road.width / 1e6, 1) + " " + stringResource(R.string.m2), style = MaterialTheme.typography.bodySmall)
                    }
                    PropsRow(Modifier) {
                        ChipMenu(surfaceName(road.surface) + " ▾", Surface.entries.map { surfaceName(it) }) { k -> vm.commit(PlanOps.updateRoad(plan, road.copy(surface = Surface.entries[k]))) }
                        if (!road.closed) AssistChip(onClick = { roadWidth = true }, label = { Text("↔ " + cm(road.width.toDouble())) })
                        else {
                            val selected = ed.yardPointIds
                            AssistChip(onClick = {
                                if (selected.size < 2) return@AssistChip
                                val x = selected.map { road.points[it].x }.average().roundToInt()
                                val points = road.points.mapIndexed { i, p -> if (i in selected) p.copy(x = x) else p }
                                vm.commit(PlanOps.updateRoad(plan, road.copy(points = points)))
                            }, label = { Text("↕ Align") })
                            AssistChip(onClick = {
                                if (selected.size < 2) return@AssistChip
                                val y = selected.map { road.points[it].y }.average().roundToInt()
                                val points = road.points.mapIndexed { i, p -> if (i in selected) p.copy(y = y) else p }
                                vm.commit(PlanOps.updateRoad(plan, road.copy(points = points)))
                            }, label = { Text("↔ Align") })
                            AssistChip(onClick = {
                                if (selected.isEmpty()) return@AssistChip
                                val pts = road.points
                                val keep = pts.indices.filter { i ->
                                    val a = P(pts[(i - 1 + pts.size) % pts.size].x.toDouble(), pts[(i - 1 + pts.size) % pts.size].y.toDouble())
                                    val b = P(pts[i].x.toDouble(), pts[i].y.toDouble())
                                    val c = P(pts[(i + 1) % pts.size].x.toDouble(), pts[(i + 1) % pts.size].y.toDouble())
                                    i !in selected || Geo.distToSegment(b, a, c) > 20.0
                                }
                                if (keep.size >= 3) { ed.yardPointIds = emptySet(); vm.commit(PlanOps.updateRoad(plan, road.copy(points = keep.mapIndexed { i, old -> pts[old].copy(id = i) }))) }
                            }, label = { Text(stringResource(R.string.simplify)) })
                        }
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteRoad(plan, road.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                opening != null && opening.kind == OpeningKind.GATE -> {
                    Text(gateName(opening.gate), Modifier.widthIn(max = 120.dp), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PropsRow(Modifier.padding(start = 6.dp)) {
                        AssistChip(onClick = { editOpening = true }, label = { Text(cm(opening.width.toDouble()) + " × " + cm(opening.height.toDouble())) })
                        ChipMenu(stringResource(R.string.gate_kind) + " ▾", GateKind.entries.map { gateName(it) }) { k ->
                            vm.commit(PlanOps.updateOpening(plan, opening.copy(gate = GateKind.entries[k], width = GateKind.entries[k].width))) }
                    }
                    // which side it opens to (or slides along)
                    IconButton(onClick = { vm.commit(PlanOps.updateOpening(plan, opening.copy(nx = -opening.nx, ny = -opening.ny))) }) { Icon(Icons.Default.SwapHoriz, stringResource(R.string.flip)) }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteOpening(plan, opening.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                opening != null && opening.kind != OpeningKind.WINDOW -> {
                    // Door: its size first, then four ways to open, or a frame with no leaf.
                    PropsRow(Modifier) {
                        AssistChip(onClick = { editOpening = true }, label = { Text(cm(opening.width.toDouble()) + " × " + cm(opening.height.toDouble())) })
                        for (k in 0..4) {
                            val chosen = if (opening.kind == OpeningKind.PASSAGE) k == 4 else k == doorWay(opening, plan)
                            FilterChip(chosen, onClick = { vm.commit(PlanOps.updateOpening(plan, withDoorWay(opening, plan, k))) },
                                label = { DoorIcon(k, chosen) })
                        }
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteOpening(plan, opening.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                opening != null -> {
                    AssistChip(onClick = { editOpening = true }, label = {
                        Text(cm(opening.width.toDouble()) + " × " + cm(opening.height.toDouble()) + if (opening.kind == OpeningKind.WINDOW) "  ↥ " + cm(opening.sill.toDouble()) else "")
                    })
                    Text(stringResource(if (opening.kind == OpeningKind.DOOR) R.string.door else R.string.window), Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleSmall)
                    if (opening.kind == OpeningKind.DOOR) IconButton(onClick = { vm.commit(PlanOps.updateOpening(plan, opening.copy(nx = -opening.nx, ny = -opening.ny))) }) { Icon(Icons.Default.SwapHoriz, stringResource(R.string.flip)) }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteOpening(plan, opening.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                model != null -> {
                    val (mw, md, mh) = Models.size(model)
                    Text(model.name, Modifier.widthIn(max = 110.dp), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PropsRow(Modifier.padding(start = 6.dp)) {
                        if (model.yard != null) AssistChip(onClick = { editYard = true }, label = { Text("⚙ " + stringResource(R.string.yard_settings)) })
                        AssistChip(onClick = { showViews = true }, label = { Text("▣ " + stringResource(R.string.three_views)) })
                        AssistChip(onClick = { showViews = true }, label = { Text(cm(mw) + "×" + cm(md) + "×" + cm(mh)) })
                        AssistChip(onClick = { editModelZ = true }, label = { Text("↥ " + cm(model.z.toDouble())) })
                        AssistChip(onClick = { vm.commit(PlanOps.updateModel(plan, model.copy(rot = (model.rot + 345) % 360))) }, label = { Text("⟲ 15°") })
                        AssistChip(onClick = { vm.commit(PlanOps.updateModel(plan, model.copy(rot = (model.rot + 15) % 360))) }, label = { Text("⟳ 15°") })
                        AssistChip(onClick = { vm.commit(PlanOps.updateModel(plan, model.copy(rot = (model.rot + 90) % 360))) }, label = { Text("⟳ 90°") })
                        AssistChip(onClick = { replaceFor = model.name; modelPicker.launch(arrayOf("*/*")) }, label = { Text("⇅ " + stringResource(R.string.replace_model)) })
                        AssistChip(onClick = { sendToIdea3d(context, model.parts, model.name + " #rk${vm.floor}m${model.id}") }, label = { Text("✎ Idea 3D") })
                    }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteModel(plan, model.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                item != null -> {
                    Text(itemName(item.kind), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AssistChip(onClick = { editItem = true }, label = { Text(cm(item.w.toDouble()) + " × " + cm(item.d.toDouble())) })
                    PropsRow(Modifier.padding(start = 4.dp)) {
                        AssistChip(onClick = { itemH = true }, label = { Text("↕ " + cm(item.h.toDouble())) })
                        AssistChip(onClick = { itemZ = true }, label = { Text("↥ " + cm(item.z.toDouble())) })
                    }
                    // to shape it in Idea 3D; sent back, it comes in its place as a 3D model
                    val itemTitle = itemName(item.kind)
                    AssistChip(onClick = { sendToIdea3d(context, Models.ofItem(item), itemTitle + " #rk${vm.floor}i${item.id}") },
                        label = { Text("✎ Idea 3D") }, modifier = Modifier.padding(start = 4.dp))
                    IconButton(onClick = { vm.commit(PlanOps.updateItem(plan, item.copy(rot = (item.rot + 90) % 360))) }) { Icon(Icons.Default.RotateRight, stringResource(R.string.rotate)) }
                    IconButton(onClick = { ed.sel = null; vm.commit(PlanOps.deleteItem(plan, item.id)) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
                else -> {}
            }
        }
      }
    }

    if (editLength && wall != null) TextDialog(stringResource(R.string.wall), stringResource(R.string.length_cm), cm(plan.length(wall)), numeric = true,
        onDismiss = { editLength = false }) { text ->
        editLength = false
        text.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 1 }?.let { v ->
            val after = PlanOps.setWallLength(plan, wall.id, (v * 10).roundToInt())
            if (after.wall(wall.id) == null) ed.sel = null
            vm.commit(after)
        }
    }

    if (editPoint && point != null) {
        if (point.kind == PointKind.LAMP) TextDialog(pointName(point.kind), stringResource(R.string.power_w), point.watts.toString(), numeric = true, onDismiss = { editPoint = false }) { text ->
            editPoint = false
            text.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }?.let { v -> vm.commit(PlanOps.updatePoint(plan, point.copy(watts = v.roundToInt()))) }
        } else HeightDialog(point, plan, onDismiss = { editPoint = false }) { mm, all ->
            editPoint = false
            vm.commit(if (all) PlanOps.setAllHeights(plan, point.kind, mm) else PlanOps.updatePoint(plan, point.copy(height = mm)))
        }
    }
    if (editSwitchLabel && point?.kind == PointKind.SWITCH) TextDialog("Switch label", "Optional label", point.name, onDismiss = { editSwitchLabel = false }) { text ->
        editSwitchLabel = false
        vm.commit(PlanOps.updatePoint(plan, point.copy(name = text.trim())))
        DebugLog.add("electric: switch #${point.id} label=${text.trim().ifBlank { "none" }}")
    }
    if (editAppliance && point != null && point.kind == PointKind.APPLIANCE) {
        val roomType = rooms.firstOrNull { Geo.inside(P(point.x.toDouble(), point.y.toDouble()) + P(point.nx, point.ny) * 150.0, it.polygon) }?.info?.type
        ApplianceDialog(point, roomType, onDismiss = { editAppliance = false }) { vm.commit(PlanOps.updatePoint(plan, it)); editAppliance = false }
    }
    if (editOpening && opening != null) OpeningDialog(opening, plan, onDismiss = { editOpening = false }) { o, all ->
        editOpening = false
        vm.commit(if (all) PlanOps.sizeAll(plan, o) else PlanOps.updateOpening(plan, o))
    }
    if (editPlatform && platform != null) PlatformDialog(platform, onDismiss = { editPlatform = false }) { p ->
        editPlatform = false; ed.platformHeight = p.height
        vm.commit(PlanOps.updatePlatform(plan, p))
    }
    if (editPlatformSize && platform != null) TextDialog(stringResource(R.string.platform), stringResource(R.string.size_cm),
        cm((platform.x2 - platform.x1).toDouble()) + " x " + cm((platform.y2 - platform.y1).toDouble()), onDismiss = { editPlatformSize = false }) { text ->
        editPlatformSize = false
        val v = text.replace(',', '.').split('x', 'X', 'х', 'Х', '×', '*', ' ').mapNotNull { it.trim().toDoubleOrNull() }.map { (it * 10).roundToInt() }
        if (v.size == 2 && v.all { it >= 300 }) vm.commit(PlanOps.updatePlatform(plan, platform.copy(x2 = platform.x1 + v[0], y2 = platform.y1 + v[1])))
    }
    if (editRoofBase && roof != null) RoofBaseDialog(roof, plan, onDismiss = { editRoofBase = false }) { base ->
        editRoofBase = false
        vm.commit(PlanOps.updateRoof(plan, roof.copy(base = base)))
    }
    if (editItem && item != null) TextDialog(itemName(item.kind), stringResource(R.string.size_cm), cm(item.w.toDouble()) + " x " + cm(item.d.toDouble()),
        onDismiss = { editItem = false }) { text ->
        editItem = false
        val v = text.replace(',', '.').split('x', 'X', 'х', 'Х', '×', '*', ' ').mapNotNull { it.trim().toDoubleOrNull() }
        if (v.size == 2 && v.all { it >= 5 }) vm.commit(PlanOps.updateItem(plan, item.copy(w = (v[0] * 10).roundToInt(), d = (v[1] * 10).roundToInt())))
    }

    if (editWallH && wall != null) {
        var text by remember { mutableStateOf(cm(plan.heightOf(wall).toDouble())) }
        AlertDialog(
            onDismissRequest = { editWallH = false },
            title = { Text(stringResource(R.string.wall_height)) },
            text = {
                Column {
                    OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.height_cm)) }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                    Row(Modifier.padding(top = 8.dp).hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (h in listOf(90, 110, 120)) FilterChip(text == h.toString(), onClick = { text = h.toString() }, label = { Text("$h") })
                        FilterChip(text == cm(plan.wallHeight.toDouble()), onClick = { text = cm(plan.wallHeight.toDouble()) }, label = { Text(stringResource(R.string.full_height, cm(plan.wallHeight.toDouble()))) })
                    }
                    Text(stringResource(R.string.wall_height_hint), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editWallH = false
                    text.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 10 }?.let { vm.commit(PlanOps.setWallHeight(plan, wall.id, (it * 10).roundToInt())) }
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { editWallH = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (editStair && stair != null) {
        var w by remember { mutableStateOf(cm(stair.width.toDouble())) }
        var l by remember { mutableStateOf(cm(stair.length.toDouble())) }
        var l2 by remember { mutableStateOf(cm(stair.length2.toDouble())) }
        var h by remember { mutableStateOf(stair.rise?.let { cm(it.toDouble()) } ?: "") }
        var fromGround by remember { mutableStateOf(stair.fromGround) }
        val num = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
        fun mm(t: String) = t.replace(',', '.').toDoubleOrNull()?.let { (it * 10).roundToInt() }
        AlertDialog(
            onDismissRequest = { editStair = false },
            title = { Text(stringResource(R.string.stairs)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(w, { w = it }, label = { Text(stringResource(R.string.stair_width)) }, singleLine = true, keyboardOptions = num)
                    OutlinedTextField(l, { l = it }, label = { Text(stringResource(R.string.stair_length)) }, singleLine = true, keyboardOptions = num)
                    if (stair.kind == StairKind.L_LEFT || stair.kind == StairKind.L_RIGHT)
                        OutlinedTextField(l2, { l2 = it }, label = { Text(stringResource(R.string.stair_length2)) }, singleLine = true, keyboardOptions = num)
                    if (vm.floor == 0) Row(Modifier.padding(top = 6.dp).clickable { fromGround = !fromGround }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(fromGround, { fromGround = it })
                        Text(stringResource(R.string.stair_from_ground, cm((vm.root?.groundLevel ?: 0).toDouble())), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!fromGround) OutlinedTextField(h, { h = it }, label = { Text(stringResource(R.string.stair_rise)) }, singleLine = true, keyboardOptions = num,
                        placeholder = { Text(cm((plan.wallHeight + com.dawoyan.roomik.core.SLAB).toDouble())) })
                    Text(stringResource(R.string.stair_hint, Stairs.shape(plan, stair).flights.sumOf { it.steps }), Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editStair = false
                    setStair(stair.copy(
                        width = mm(w)?.coerceAtLeast(400) ?: stair.width, length = mm(l)?.coerceAtLeast(800) ?: stair.length,
                        length2 = mm(l2)?.coerceAtLeast(400) ?: stair.length2, rise = mm(h)?.takeIf { it > 0 }, fromGround = fromGround,
                    ))
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { editStair = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (editPlace && place != null) {
        var name by remember { mutableStateOf(place.name) }
        var type by remember { mutableStateOf(place.type) }
        AlertDialog(
            onDismissRequest = { editPlace = false },
            title = { Text(stringResource(R.string.place)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.place_about), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    Text(stringResource(R.string.room_type), Modifier.padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (t in RoomType.entries) FilterChip(type == t, onClick = { type = if (type == t) null else t }, label = { Text(t.label(lang)) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editPlace = false; vm.commit(PlanOps.updatePlace(plan, place.copy(name = name.trim(), type = type))) }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { editPlace = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (editRoom && room != null) {
        var name by remember { mutableStateOf(room.info?.name ?: "") }
        var type by remember { mutableStateOf(room.info?.type) }
        AlertDialog(
            onDismissRequest = { editRoom = false },
            title = { Text(stringResource(R.string.room)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.room_type), Modifier.padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (t in RoomType.entries) FilterChip(type == t, onClick = { type = if (type == t) null else t }, label = { Text(t.label(lang)) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editRoom = false; ed.roomType = type; vm.commit(PlanOps.setRoomInfo(plan, room, name, type)) }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { editRoom = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}


@Composable
private fun LevelChips(level: RouteLevel, onPick: (RouteLevel) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(level == RouteLevel.CEILING, onClick = { onPick(RouteLevel.CEILING) }, label = { Text(stringResource(R.string.ceiling)) })
        FilterChip(level == RouteLevel.FLOOR, onClick = { onPick(RouteLevel.FLOOR) }, label = { Text(stringResource(R.string.floor)) })
    }
}

/** Height of a socket, switch, appliance outlet or the board; optionally for every point of that kind and new ones. */
@Composable
private fun HeightDialog(point: ElPoint, plan: Plan, onDismiss: () -> Unit, onOk: (Int, Boolean) -> Unit) {
    var text by remember { mutableStateOf(cm(point.height.toDouble())) }
    var all by remember { mutableStateOf(false) }
    val presets = when (point.kind) {
        PointKind.SOCKET -> listOf(30, 90, 110)
        PointKind.SWITCH -> listOf(90, 110, 150)
        PointKind.BOARD -> listOf(150, 170)
        else -> listOf(30, 60, 110, 200)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pointName(point.kind)) },
        text = {
            Column {
                OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.height_cm)) }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (h in presets) FilterChip(text == h.toString(), onClick = { text = h.toString() }, label = { Text("$h") })
                }
                Row(Modifier.padding(top = 8.dp).clickable { all = !all }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(all, { all = it })
                    Text(stringResource(R.string.height_all, pointName(point.kind), plan.points.count { it.kind == point.kind }), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                text.replace(',', '.').toDoubleOrNull()?.let { onOk((it * 10).roundToInt().coerceIn(0, plan.wallHeight), all) } ?: onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Width, height and (for a window) sill height of a door or window, with the usual sizes one tap away. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpeningDialog(o: Opening, plan: Plan, onDismiss: () -> Unit, onOk: (Opening, Boolean) -> Unit) {
    val win = o.kind == OpeningKind.WINDOW
    var w by remember { mutableStateOf(cm(o.width.toDouble())) }
    var h by remember { mutableStateOf(cm(o.height.toDouble())) }
    var sill by remember { mutableStateOf(cm(o.sill.toDouble())) }
    var all by remember { mutableStateOf(false) }
    fun mm(t: String) = t.replace(',', '.').trim().toDoubleOrNull()?.let { (it * 10).roundToInt() }
    val sizes = if (win) listOf(60 to 60, 90 to 120, 120 to 140, 150 to 150, 180 to 150, 240 to 150) else listOf(70 to 200, 80 to 200, 90 to 205, 120 to 210, 150 to 210, 250 to 220)
    val sills = listOf(0, 40, 80, 90, 110)
    val num = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (win) R.string.window else if (o.kind == OpeningKind.PASSAGE) R.string.passage else if (o.kind == OpeningKind.GATE) R.string.gate else R.string.door)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((a, b) in sizes) FilterChip(w == "$a" && h == "$b", onClick = { w = "$a"; h = "$b" }, label = { Text("$a × $b") })
                }
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(w, { w = it }, label = { Text(stringResource(R.string.op_width)) }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                    OutlinedTextField(h, { h = it }, label = { Text(stringResource(R.string.op_height)) }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                }
                if (win) {
                    OutlinedTextField(sill, { sill = it }, label = { Text(stringResource(R.string.op_sill)) }, singleLine = true, keyboardOptions = num, modifier = Modifier.padding(top = 6.dp).fillMaxWidth())
                    FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (v in sills) FilterChip(sill == "$v", onClick = { sill = "$v" }, label = { Text("↥ $v") })
                    }
                    val top = (mm(sill) ?: 0) + (mm(h) ?: 0)
                    Text(stringResource(R.string.op_top, cm(top.toDouble())), style = MaterialTheme.typography.bodySmall,
                        color = if (top > plan.wallHeight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val count = plan.openings.count { it.kind == o.kind }
                if (count > 1) Row(Modifier.padding(top = 8.dp).clickable { all = !all }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(all, { all = it })
                    Text(stringResource(if (win) R.string.op_all_windows else R.string.op_all_doors, count), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ww = mm(w); val hh = mm(h)
                if (ww == null || hh == null || ww < 200 || hh < 200) onDismiss()
                else onOk(o.copy(width = ww, height = hh, sill = if (win) (mm(sill) ?: o.sill).coerceAtLeast(0) else 0), all)
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** A platform's height above the floor, slab thickness and railing height, with usual heights one tap away. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlatformDialog(p: com.dawoyan.roomik.core.Platform, onDismiss: () -> Unit, onOk: (com.dawoyan.roomik.core.Platform) -> Unit) {
    var h by remember { mutableStateOf(cm(p.height.toDouble())) }
    var t by remember { mutableStateOf(cm(p.thickness.toDouble())) }
    var r by remember { mutableStateOf(cm(p.railingHeight.toDouble())) }
    fun mm(s: String) = s.replace(',', '.').trim().toDoubleOrNull()?.let { (it * 10).roundToInt() }
    val num = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.platform)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(h, { h = it }, label = { Text(stringResource(R.string.platform_height)) }, singleLine = true, keyboardOptions = num, modifier = Modifier.fillMaxWidth())
                FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (v in listOf(-50, 0, 15, 30, 45, 60, 100, 150, 250)) FilterChip(h == "$v", onClick = { h = "$v" }, label = { Text("$v") })
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(t, { t = it }, label = { Text(stringResource(R.string.slab_thickness)) }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                    OutlinedTextField(r, { r = it }, label = { Text(stringResource(R.string.railing_height)) }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onOk(p.copy(height = (mm(h) ?: p.height).coerceIn(-10000, 20000), thickness = (mm(t) ?: p.thickness).coerceIn(20, 1000),
                    railingHeight = (mm(r) ?: p.railingHeight).coerceIn(300, 2000)))
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Where a roof starts: a height typed in, a few usual ones, or auto (the top of the highest wall under it). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoofBaseDialog(r: com.dawoyan.roomik.core.Roof, plan: Plan, onDismiss: () -> Unit, onOk: (Int?) -> Unit) {
    val auto = Roofs.baseOf(plan, r.copy(base = null))
    var text by remember { mutableStateOf(cm(Roofs.baseOf(plan, r).toDouble())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.roof_base)) },
        text = {
            Column {
                Text(stringResource(R.string.roof_base_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(text, { text = it }, singleLine = true, modifier = Modifier.padding(top = 8.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (v in listOf(0, 50, 100, 120, 150, 250, 270, 300)) FilterChip(text == "$v", onClick = { text = "$v" }, label = { Text("$v") })
                }
                TextButton(onClick = { onOk(null) }, modifier = Modifier.padding(top = 4.dp)) { Text(stringResource(R.string.roof_base_auto, cm(auto.toDouble()))) }
            }
        },
        confirmButton = {
            TextButton(onClick = { text.replace(',', '.').trim().toDoubleOrNull()?.let { onOk((it * 10).roundToInt().coerceIn(0, 20000)) } ?: onDismiss() }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** What an appliance outlet feeds: a preset from elektronik (oven, cooktop…) or any name, its power and how it is fed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApplianceDialog(point: ElPoint, type: RoomType?, onDismiss: () -> Unit, onOk: (ElPoint) -> Unit) {
    val lang = language()
    com.dawoyan.elektronik.core.I18n.ru = lang == "ru"
    // The room's own presets first, then every other one elektronik knows.
    val presets = ((type?.presets ?: emptyList()) + RoomType.entries.flatMap { it.presets }).distinctBy { it.first }
    var name by remember { mutableStateOf(point.name) }
    var kw by remember { mutableStateOf(if (point.kw > 0) num(point.kw, 1) else "") }
    var own by remember { mutableStateOf(point.ownLine) }
    var three by remember { mutableStateOf(point.threePhase) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.appliance)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((en, p) in presets) {
                        val local = com.dawoyan.elektronik.core.BuiltInNames.of(en)
                        FilterChip(name == local, onClick = { name = local; kw = num(p, 1) }, label = { Text("$local ${num(p, 1)}") })
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(kw, { kw = it }, label = { Text(stringResource(R.string.power_kw)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                Row(Modifier.padding(top = 6.dp).clickable { own = !own }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(own, { own = it }); Text(stringResource(R.string.own_line), style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.clickable { three = !three }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(three, { three = it }); Text(stringResource(R.string.three_phase), style = MaterialTheme.typography.bodyMedium)
                }
                Text(stringResource(R.string.own_line_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onOk(point.copy(name = name.trim(), kw = kw.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0, ownLine = own, threePhase = three))
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}


/**
 * Which of the four ways a door opens, 0..3: the side it opens to (0, 1 = to the left of the wall as drawn, 2, 3 = to
 * the right) and the hinge end. Seen on the plan these are the four quarter-circles a door can sweep.
 */
private fun doorWay(o: Opening, plan: Plan): Int {
    val side = wallNormalSide(o, plan)
    return (if (side) 0 else 2) + if (o.hingeEnd) 1 else 0
}

/** True when the door opens to the left of its wall, seen from the wall's start. */
private fun wallNormalSide(o: Opening, plan: Plan): Boolean {
    val c = P(o.x.toDouble(), o.y.toDouble())
    val w = plan.walls.minByOrNull { w -> val a = plan.pos(w.a); val b = plan.pos(w.b); if (a == null || b == null) Double.MAX_VALUE else Geo.distToSegment(c, a, b) } ?: return true
    val a = plan.pos(w.a) ?: return true; val b = plan.pos(w.b) ?: return true
    return dot(P(o.nx, o.ny), (b - a).norm().left()) >= 0
}

/** [o] made to open way [k] (see [doorWay]), or a frame with no leaf for 4. */
private fun withDoorWay(o: Opening, plan: Plan, k: Int): Opening {
    if (k == 4) return o.copy(kind = OpeningKind.PASSAGE)
    val c = P(o.x.toDouble(), o.y.toDouble())
    val w = plan.walls.minByOrNull { w -> val a = plan.pos(w.a); val b = plan.pos(w.b); if (a == null || b == null) Double.MAX_VALUE else Geo.distToSegment(c, a, b) } ?: return o
    val a = plan.pos(w.a) ?: return o; val b = plan.pos(w.b) ?: return o
    val left = (b - a).norm().left()
    val n = if (k < 2) left else left * -1.0
    return o.copy(kind = OpeningKind.DOOR, nx = n.x, ny = n.y, hingeEnd = k % 2 == 1)
}

/** A small picture of door way [k]: the wall, the leaf and its sweep; 4 is a frame without a leaf. */
@Composable
private fun DoorIcon(k: Int, on: Boolean) {
    val ink = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(width = 26.dp, height = 22.dp)) {
        val y = size.height / 2; val x0 = 2.dp.toPx(); val x1 = size.width - 2.dp.toPx()
        val sw = 1.6.dp.toPx()
        drawLine(ink, Offset(0f, y), Offset(x0, y), sw * 1.6f); drawLine(ink, Offset(x1, y), Offset(size.width, y), sw * 1.6f)
        if (k == 4) {
            drawLine(ink, Offset(x0, y - 5.dp.toPx()), Offset(x0, y + 5.dp.toPx()), sw); drawLine(ink, Offset(x1, y - 5.dp.toPx()), Offset(x1, y + 5.dp.toPx()), sw)
            return@Canvas
        }
        val up = k < 2
        val hx = if (k % 2 == 0) x0 else x1
        val ox = if (k % 2 == 0) x1 else x0
        val len = x1 - x0
        val ty = if (up) y - len else y + len
        drawLine(ink, Offset(hx, y), Offset(hx, ty), sw)
        val arc = Path()
        for (j in 0..8) {
            val a = Math.PI / 2 * j / 8
            val px = hx + (ox - hx) * Math.sin(a).toFloat(); val py = y + (ty - y) * Math.cos(a).toFloat()
            if (j == 0) arc.moveTo(px, py) else arc.lineTo(px, py)
        }
        drawPath(arc, ink, style = Stroke(sw * 0.7f))
    }
}

/** Stairs on the plan: the outline, a line at every step and an arrow going up. */
private fun stairs(scope: DrawScope, ed: EditorState, i: com.dawoyan.roomik.core.Item, fill: Color, ink: Color) = with(scope) {
    val c = Interior.corners(i)
    val pth = Path().apply { c.forEachIndexed { k, p -> val o = ed.toScreen(p); if (k == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }; close() }
    drawPath(pth, fill)
    val n = maxOf(2, Math.round(i.h / 175f))
    for (k in 1 until n) {
        val f = k.toDouble() / n
        drawLine(ink, ed.toScreen(c[0] + (c[3] - c[0]) * f), ed.toScreen(c[1] + (c[2] - c[1]) * f), 1.dp.toPx())
    }
    val a = ed.toScreen((c[0] + c[1]) * 0.5 + (c[3] - c[0]) * 0.08)
    val b = ed.toScreen((c[3] + c[2]) * 0.5 - (c[3] - c[0]) * 0.08)
    drawLine(ink, a, b, 1.6.dp.toPx())
    val dir = (b - a) / (b - a).getDistance().coerceAtLeast(1f)
    val side = Offset(-dir.y, dir.x) * 6.dp.toPx()
    drawLine(ink, b, b - dir * 9.dp.toPx() + side, 1.6.dp.toPx()); drawLine(ink, b, b - dir * 9.dp.toPx() - side, 1.6.dp.toPx())
}


/** Stairs on the plan: flights with a line at every step and an arrow going up, landings plain. */
private fun stairShape(scope: DrawScope, ed: EditorState, shp: com.dawoyan.roomik.core.StairShape, fill: Color, ink: Color, selected: Color?) = with(scope) {
    fun pth(c: List<P>) = Path().apply { c.forEachIndexed { k, p -> val o = ed.toScreen(p); if (k == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }; close() }
    for (l in shp.landings) { drawPath(pth(l.corners), fill); drawPath(pth(l.corners), selected ?: ink, style = Stroke(if (selected != null) 3.dp.toPx() else 1.dp.toPx())) }
    for (f in shp.flights) {
        val c = f.corners
        drawPath(pth(c), fill)
        drawPath(pth(c), selected ?: ink, style = Stroke(if (selected != null) 3.dp.toPx() else 1.dp.toPx()))
        for (k in 1 until f.steps) {
            val t = k.toDouble() / f.steps
            drawLine(ink, ed.toScreen(c[0] + (c[3] - c[0]) * t), ed.toScreen(c[1] + (c[2] - c[1]) * t), 1.dp.toPx())
        }
        val a = ed.toScreen((c[0] + c[1]) * 0.5 + (c[3] - c[0]) * 0.06)
        val b = ed.toScreen((c[3] + c[2]) * 0.5 - (c[3] - c[0]) * 0.06)
        drawLine(ink, a, b, 1.6.dp.toPx())
        val len = (b - a).getDistance()
        if (len > 1f) {
            val dir = (b - a) / len
            val side = Offset(-dir.y, dir.x) * 6.dp.toPx()
            drawLine(ink, b, b - dir * 9.dp.toPx() + side, 1.6.dp.toPx()); drawLine(ink, b, b - dir * 9.dp.toPx() - side, 1.6.dp.toPx())
        }
    }
}

/** A small picture of a stair shape for the chips. */
@Composable
private fun StairIcon(k: StairKind, on: Boolean) {
    val ink = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(width = 24.dp, height = 22.dp)) {
        val plan = Plan("", "")
        val shp = Stairs.shape(plan, Stair(0, k, 0, 0, rot = -90, width = 1000, length = 3000, length2 = 1500))
        val pts = shp.pieces.flatten()
        val minX = pts.minOf { it.x }; val maxX = pts.maxOf { it.x }; val minY = pts.minOf { it.y }; val maxY = pts.maxOf { it.y }
        val sc = minOf(size.width / (maxX - minX), size.height / (maxY - minY)).toFloat()
        val ox = (size.width - (maxX - minX).toFloat() * sc) / 2; val oy = (size.height - (maxY - minY).toFloat() * sc) / 2
        fun o(p: P) = Offset(((p.x - minX) * sc).toFloat() + ox, ((p.y - minY) * sc).toFloat() + oy)
        for (pc in shp.pieces) {
            val path = Path().apply { pc.forEachIndexed { i, p -> if (i == 0) moveTo(o(p).x, o(p).y) else lineTo(o(p).x, o(p).y) }; close() }
            drawPath(path, ink, style = Stroke(1.2.dp.toPx()))
        }
        for (f in shp.flights) for (s in 1 until 4) {
            val t = s / 4.0; val c = f.corners
            drawLine(ink, o(c[0] + (c[3] - c[0]) * t), o(c[1] + (c[2] - c[1]) * t), 0.8.dp.toPx())
        }
    }
}


/** Puts stairs in, unless they would go through a wall: then asks to lower that wall instead (see [EditorState.clash]). */
private fun commitStair(vm: AppViewModel, ed: EditorState, after: Plan, before: Plan, id: Int) {
    // Walls are never changed here: walls the stairs run into blink red, and the user lowers them by hand if wanted.
    vm.commit(after, before)
}


@Composable
fun systemName(k: SystemKind): String = stringResource(when (k) {
    SystemKind.NETWORK -> R.string.sys_network; SystemKind.WATER_COLD -> R.string.sys_cold; SystemKind.WATER_HOT -> R.string.sys_hot
    SystemKind.SEWER -> R.string.sys_sewer; SystemKind.HEATING -> R.string.sys_heating
})

fun pipeLength(p: com.dawoyan.roomik.core.Pipe): Double =
    (1 until p.points.size).sumOf { k -> P((p.points[k].x - p.points[k - 1].x).toDouble(), (p.points[k].y - p.points[k - 1].y).toDouble()).len }

/** Show or hide electrics, systems, warm floor and furniture, on the plan and in 3D. */
/** How a floor is called: 1st floor, 2nd floor… above; under the first: Ground floor (on the ground) or Basement. */
@Composable
fun floorName(i: Int, root: Plan?): String {
    if (i == SITE) return stringResource(R.string.site_level)
    if (i >= 0) {
        val n = i + 1
        val num = if (stringResource(R.string.ordinal_en) == "1")
            n.toString() + when { n % 100 in 11..13 -> "th"; n % 10 == 1 -> "st"; n % 10 == 2 -> "nd"; n % 10 == 3 -> "rd"; else -> "th" }
        else n.toString()
        return stringResource(R.string.floor_plus, num)
    }
    val f = root?.floor(i)
    if (f?.onGround == true) return stringResource(R.string.floor_ground)
    val k = (i..-1).count { root?.floor(it)?.onGround != true }
    return if (k <= 1) stringResource(R.string.floor_basement) else stringResource(R.string.floor_basement_n, k)
}

/** The floor drawn faintly under the one edited (null: none). */
fun underFloor(vm: AppViewModel): Int? {
    val r = vm.root ?: return null
    val u = vm.under
    if (u == com.dawoyan.roomik.NO_FLOOR) return null
    if (u != null && u != vm.floor && u in r.floorRange) return u
    return when { vm.floor == SITE -> r.groundFloor; vm.floor == 0 -> null; vm.floor > 0 -> vm.floor - 1; else -> vm.floor + 1 }
}

/** In place of the project's name: what the plan shows, outside the menus things belong to too. */
@Composable
private fun ViewMenu(vm: AppViewModel, ed: EditorState) {
    var open by remember { mutableStateOf(false) }
    val copied = stringResource(R.string.copied_walls)
    val noOuter = stringResource(R.string.no_outer)
    Box {
        ViewButton { open = true }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            @Composable
            fun check(label: String, on: Boolean, set: () -> Unit) =
                DropdownMenuItem(text = { Text(label) }, leadingIcon = { Checkbox(on, null) }, onClick = set)
            @Composable
            fun header(label: String) = Text(label, Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            header(stringResource(R.string.always_show))
            check(stringResource(R.string.tool_electric), vm.alwaysElectric) { vm.showElectricAlways(!vm.alwaysElectric) }
            check(stringResource(R.string.pipes), vm.alwaysPipes) { vm.showPipesAlways(!vm.alwaysPipes) }
            check(stringResource(R.string.fittings), vm.showFittings) { vm.showFittingsOn(!vm.showFittings) }
            check(stringResource(R.string.furniture), vm.showFurniture) { vm.showFurnitureOn(!vm.showFurniture) }
            HorizontalDivider()
            check(stringResource(R.string.see_through), vm.seeThrough) { vm.showSeeThrough(!vm.seeThrough) }
            check(stringResource(R.string.dimensions), vm.showDims) { vm.showDimsOn(!vm.showDims) }
            check(stringResource(R.string.pillar_grid), vm.showPillarGrid) { vm.showPillarGridOn(!vm.showPillarGrid) }
            val root = vm.root
            if (root != null && (root.floors.isNotEmpty() || root.basements.isNotEmpty())) {
                HorizontalDivider()
                header(stringResource(R.string.under_floor))
                val u = underFloor(vm)
                DropdownMenuItem(text = { Text(stringResource(R.string.none)) }, leadingIcon = { RadioButton(u == null, null) },
                    onClick = { vm.under = com.dawoyan.roomik.NO_FLOOR })
                for (i in root.floorRange.reversed()) if (i != vm.floor)
                    DropdownMenuItem(text = { Text(floorName(i, vm.root)) }, leadingIcon = { RadioButton(u == i, null) }, onClick = { vm.under = i })
                if (u != null) {
                    val name = floorName(u, vm.root)
                    DropdownMenuItem(text = { Text(stringResource(R.string.copy_outer, name)) }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, onClick = {
                        open = false
                        val plan = vm.plan ?: return@DropdownMenuItem
                        val src = root.floor(u)
                        val outer = com.dawoyan.roomik.core.Outline.outerWalls(src)
                        if (outer.isEmpty()) ed.toastText = noOuter
                        else { ed.sel = null; vm.commit(PlanOps.copyWalls(plan, src, outer)); ed.toastText = copied.format(outer.size) }
                    })
                }
            }
            HorizontalDivider()
            header(stringResource(R.string.layers))
            // electrics and furniture are above; these are hidden in 3D too
            for (l in Layer.entries - Layer.FURNITURE) {
                val shown = l !in vm.hidden
                check((if (l == Layer.ELECTRIC) "⚡ " else "") + layerName(l), shown) { vm.hidden = if (shown) vm.hidden + l else vm.hidden - l }
            }
            HorizontalDivider()
            header("🏷 " + stringResource(R.string.labels))
            check(stringResource(R.string.label_cables), vm.labelCables) { vm.labelCablesOn(!vm.labelCables) }
            check(stringResource(R.string.label_names), vm.labelNames) { vm.labelNamesOn(!vm.labelNames) }
            check(stringResource(R.string.label_lengths), vm.labelLengths) { vm.labelLengthsOn(!vm.labelLengths) }
            check(stringResource(R.string.label_power), vm.labelPower) { vm.labelPowerOn(!vm.labelPower) }
            check(stringResource(R.string.label_pipes), vm.labelPipes) { vm.labelPipesOn(!vm.labelPipes) }
        }
    }
}

@Composable
private fun layerName(l: Layer): String = stringResource(when (l) {
    Layer.ELECTRIC -> R.string.tool_electric; Layer.NETWORK -> R.string.sys_network; Layer.WATER -> R.string.layer_water
    Layer.HEATING -> R.string.sys_heating; Layer.WARM_FLOOR -> R.string.warm_floor; Layer.FURNITURE -> R.string.tool_interior
    Layer.ROOF -> R.string.roof; Layer.FENCES -> R.string.fences; Layer.SEWER -> R.string.sys_sewer
})

/** Labels on cables, pipes and devices: all on or all off at once (each kind in View). */
@Composable
private fun LabelsChip(vm: AppViewModel) {
    FilterChip(vm.anyLabels, onClick = { vm.allLabels(!vm.anyLabels) }, label = { Text("🏷 " + stringResource(R.string.labels)) })
}

/**
 * A picked thing's properties in one line (scrolling sideways when long), and a ⋯ that opens them all at once in a
 * sheet, wrapped in rows, so nothing has to be looked for off the edge.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RowScope.PropsRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    var open by remember { mutableStateOf(false) }
    // the first ones, as many as fit whole, in a row that does not scroll; ⋯ has them all
    val scope = this
    androidx.compose.ui.layout.Layout(content = { with(scope) { content() } }, modifier = modifier.weight(1f).clipToBounds()) { ms, cs ->
        val gap = 4.dp.roundToPx()
        val ps = ms.map { it.measure(androidx.compose.ui.unit.Constraints(maxHeight = cs.maxHeight)) }
        val shown = ArrayList<androidx.compose.ui.layout.Placeable>(); var x = 0
        for (p in ps) { if (x + p.width > cs.maxWidth) break; shown += p; x += p.width + gap }
        val h = (ps.maxOfOrNull { it.height } ?: 0).coerceIn(cs.minHeight, cs.maxHeight)
        layout(cs.maxWidth, h) { var px = 0; for (p in shown) { p.placeRelative(px, (h - p.height) / 2); px += p.width + gap } }
    }
    IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreHoriz, stringResource(R.string.more)) }
    if (open) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { open = false }) {
        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

/** "View ▾": opens what is shown. */
@Composable
fun ViewButton(onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, contentPadding = PaddingValues(start = 14.dp, end = 6.dp)) {
        Text(stringResource(R.string.view), maxLines = 1)
        Icon(Icons.Default.ArrowDropDown, null)
    }
}

@Composable
fun LayersButton(vm: AppViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        ViewButton { open = true }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            for (l in Layer.entries) {
                val shown = l !in vm.hidden
                DropdownMenuItem(
                    text = { Text(stringResource(when (l) {
                        Layer.ELECTRIC -> R.string.tool_electric; Layer.NETWORK -> R.string.sys_network; Layer.WATER -> R.string.layer_water
                        Layer.HEATING -> R.string.sys_heating; Layer.WARM_FLOOR -> R.string.warm_floor; Layer.FURNITURE -> R.string.tool_interior
                        Layer.ROOF -> R.string.roof; Layer.FENCES -> R.string.fences; Layer.SEWER -> R.string.sys_sewer
                    })) },
                    leadingIcon = { Checkbox(shown, null) },
                    onClick = { vm.hidden = if (shown) vm.hidden + l else vm.hidden - l },
                )
            }
        }
    }
}


@Composable
fun fenceName(k: FenceKind): String = stringResource(when (k) {
    FenceKind.PICKET -> R.string.fence_picket; FenceKind.BOARD -> R.string.fence_board; FenceKind.METAL -> R.string.fence_metal
    FenceKind.BARS -> R.string.fence_bars; FenceKind.MESH -> R.string.fence_mesh; FenceKind.BRICK -> R.string.fence_brick; FenceKind.HEDGE -> R.string.fence_hedge; FenceKind.STONE -> R.string.fence_stone
})

@Composable
fun roofName(k: RoofKind): String = stringResource(when (k) {
    RoofKind.FLAT -> R.string.roof_flat; RoofKind.SHED -> R.string.roof_shed; RoofKind.GABLE -> R.string.roof_gable; RoofKind.HIP -> R.string.roof_hip
})

/** Lines a roof shows on the plan: the eaves outline, the ridge and, for a hip roof, the hips. */
fun roofLines(r: com.dawoyan.roomik.core.Roof): List<Pair<P, P>> {
    val o = r.overhang.toDouble()
    val a = P(r.x1 - o, r.y1 - o); val b = P(r.x2 + o, r.y1 - o); val c = P(r.x2 + o, r.y2 + o); val d = P(r.x1 - o, r.y2 + o)
    val out = mutableListOf(a to b, b to c, c to d, d to a)
    val faces = Roofs.faces(r, 0)
    val top = faces.filter { !it.gable }.flatMap { it.pts }.maxOfOrNull { it.h } ?: return out
    val ridge = faces.filter { !it.gable }.flatMap { it.pts }.filter { kotlin.math.abs(it.h - top) < 1 }.map { it.p }.distinct()
    if (r.kind == RoofKind.GABLE || r.kind == RoofKind.HIP) {
        if (ridge.size >= 2) out += ridge[0] to ridge[ridge.size - 1]
        if (r.kind == RoofKind.HIP && ridge.isNotEmpty()) for (corner in listOf(a, b, c, d)) out += corner to ridge.minBy { (it - corner).len }
    }
    return out
}

/** Keep a fence or pipe being drawn (two turns or more) when you tap ✓, leave the tool or open 3D, instead of losing it. */
private fun finishDrawing(vm: AppViewModel, ed: EditorState) {
    val pts = ed.pipePts
    ed.pipePts = emptyList()
    if (pts.size < 2) return
    val plan = vm.plan ?: return
    when {
        ed.tool == Tool.FENCE -> PlanOps.addFence(plan, ed.fenceKind, pts).let { if (it !== plan) { vm.commit(it); ed.sel = Sel.FenceSel(it.fences.last().id) } }
        ed.tool == Tool.ROAD -> PlanOps.addRoad(plan, pts, ed.surface, ed.roadWidth, ed.roadArea).let { if (it !== plan) { vm.commit(it); ed.sel = Sel.RoadSel(it.roads.last().id) } }
        // Backyard areas are always a closed ground polygon: no road width, caps or extra space.
        ed.tool == Tool.YARD -> PlanOps.addRoad(plan, pts, Surface.GRASS, ed.roadWidth, closed = true).let { if (it !== plan) { DebugLog.add("Backyard saved: floor=${vm.floor}, grass polygon points=${pts.size}"); vm.commit(it); ed.sel = Sel.RoadSel(it.roads.last().id) } }
        ed.tool == Tool.SYSTEMS && ed.systemMode == "pipe" -> PlanOps.addPipe(plan, ed.system ?: SystemKind.WATER_COLD, pts).let { if (it !== plan) { vm.commit(it); ed.sel = Sel.PipeSel(it.pipes.last().id) } }
    }
}

/** A line through [pts] with each turn drawn as a round bend of radius up to [r] px. */
fun roundedPath(pts: List<Offset>, r: Float): Path = Path().apply {
    if (pts.isEmpty()) return@apply
    moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size - 1) {
        val a = pts[i - 1]; val b = pts[i]; val c = pts[i + 1]
        val ab = b - a; val bc = c - b
        val lab = ab.getDistance(); val lbc = bc.getDistance()
        if (lab < 0.5f || lbc < 0.5f) { lineTo(b.x, b.y); continue }
        val k = minOf(r, lab / 2, lbc / 2)
        val p1 = b - ab * (k / lab); val p2 = b + bc * (k / lbc)
        lineTo(p1.x, p1.y)
        quadraticTo(b.x, b.y, p2.x, p2.y)
    }
    lineTo(pts.last().x, pts.last().y)
}

/** The middle of the longest run of [pts], where a label fits; null when it is too short. */
private fun longestMiddle(pts: List<Offset>): Offset? {
    var best: Offset? = null; var len = 60f
    for (i in 1 until pts.size) { val d = (pts[i] - pts[i - 1]).getDistance(); if (d > len) { len = d; best = (pts[i] + pts[i - 1]) / 2f } }
    return best
}

/** 1.5, 2.5, 10: a cross-section without needless zeros. */
fun mm2Text(v: Double): String = if (v == Math.floor(v)) v.toInt().toString() else num(v, 2).trimEnd('0')

@Composable
fun gateName(k: GateKind): String = stringResource(when (k) { GateKind.SWING -> R.string.gate_swing; GateKind.SLIDING -> R.string.gate_sliding; GateKind.WICKET -> R.string.gate_wicket
    GateKind.SINGLE -> R.string.gate_single; GateKind.SECTIONAL -> R.string.gate_sectional; GateKind.ROLLER -> R.string.gate_roller })

@Composable
fun surfaceName(k: Surface): String = stringResource(when (k) { Surface.ASPHALT -> R.string.s_asphalt; Surface.CONCRETE -> R.string.s_concrete; Surface.PAVERS -> R.string.s_pavers
    Surface.STONE -> R.string.s_stone; Surface.GRAVEL -> R.string.s_gravel; Surface.DIRT -> R.string.s_dirt; Surface.GRASS -> R.string.s_grass; Surface.GRASS_GRID -> R.string.s_grass_grid })

/** A chip that opens a short list to pick from. */
@Composable
fun ChipMenu(label: String, options: List<String>, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(onClick = { open = true }, label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) })
        androidx.compose.material3.DropdownMenu(open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, o -> androidx.compose.material3.DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onPick(i) }) }
        }
    }
}

/** A wire size as people write it: 6, 2.5. */
fun mmTxt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else num(v, 1)

/** A file's name as the phone shows it. */
fun fileNameOf(context: android.content.Context, uri: android.net.Uri): String =
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "model.3mf"

/** A part's colour shaded by how much its face looks at the viewer (0..1), in a few steps. */
private fun shade(color: Int, facing: Float): Color {
    val k = 0.45f + 0.55f * (Math.round(facing * 6) / 6f)
    return Color(((color shr 16 and 255) * k).toInt().coerceIn(0, 255), ((color shr 8 and 255) * k).toInt().coerceIn(0, 255), ((color and 255) * k).toInt().coerceIn(0, 255))
}

/**
 * [m] seen from above as paths in its own mm (x right, plan y down), far faces first; neighbouring triangles of one
 * shade share a path, so even a big model draws quickly.
 */
fun topShapes(m: com.dawoyan.roomik.core.Model3D): List<Pair<Path, Color>> {
    val out = ArrayList<Pair<Path, Color>>()
    var cur: Path? = null; var curCol: Color? = null
    // polycarbonate and glass are seen through, from above too
    val clear = setOf(Cover.POLYCARBONATE.color, Cover.GLASS.color)
    for (f in Models.flat(m, Models.View.TOP)) {
        val c = shade(f.color, f.facing).let { if (f.color in clear) it.copy(alpha = 0.45f) else it }
        if (cur == null || c != curCol) { cur = Path(); curCol = c; out += cur to c }
        // the top view's vertical axis is up the page: plan y is its negative
        cur.moveTo(f.pts[0], -f.pts[1]); cur.lineTo(f.pts[2], -f.pts[3]); cur.lineTo(f.pts[4], -f.pts[5]); cur.close()
    }
    return out
}

/** The model in three views, as in Idea 3D (top, front, side), with what its parts measure for materials. */
@Composable
private fun ModelViewsDialog(m: com.dawoyan.roomik.core.Model3D, onDismiss: () -> Unit) {
    val views = remember(m) { Models.View.entries.associateWith { Models.flat(m, it) } }
    val sizes = remember(m) { m.parts.map { Models.measure(it) } }
    val names = mapOf(Models.View.TOP to R.string.view_top, Models.View.FRONT to R.string.view_front, Models.View.SIDE to R.string.view_side)
    val m2 = stringResource(R.string.m2)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(m.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (v in listOf(Models.View.TOP, Models.View.FRONT, Models.View.SIDE)) {
                    Text(stringResource(names.getValue(v)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    val flats = views.getValue(v)
                    Canvas(Modifier.fillMaxWidth().height(150.dp).padding(vertical = 4.dp).background(Color(0xFFF4F4F0))) {
                        if (flats.isEmpty()) return@Canvas
                        var x0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
                        for (f in flats) for (i in 0 until 6 step 2) { x0 = minOf(x0, f.pts[i]); x1 = maxOf(x1, f.pts[i]); y0 = minOf(y0, f.pts[i + 1]); y1 = maxOf(y1, f.pts[i + 1]) }
                        val k = minOf((size.width - 16f) / (x1 - x0).coerceAtLeast(1f), (size.height - 16f) / (y1 - y0).coerceAtLeast(1f))
                        val ox = (size.width - (x1 - x0) * k) / 2 - x0 * k; val oy = (size.height + (y1 - y0) * k) / 2 + y0 * k
                        for (f in flats) {
                            val p = Path().apply {
                                moveTo(ox + f.pts[0] * k, oy - f.pts[1] * k); lineTo(ox + f.pts[2] * k, oy - f.pts[3] * k); lineTo(ox + f.pts[4] * k, oy - f.pts[5] * k); close()
                            }
                            drawPath(p, shade(f.color, f.facing))
                        }
                    }
                }
                // a yard construction: what it takes to build
                m.yard?.let { spec ->
                    Text(stringResource(R.string.yard_bill), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    for (l in Yard.bill(spec, language() == "ru")) Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(l.name, style = MaterialTheme.typography.bodyMedium)
                            if (l.note.isNotEmpty()) Text(l.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(num(l.qty, 1) + " " + l.unit, style = MaterialTheme.typography.titleSmall)
                    }
                }
                // each part: its size, its top seen from above (a roof sheet) and its whole surface
                Text(stringResource(R.string.model_parts), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                for (ps in sizes) Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(Color(0xFF000000 or ps.part.color.toLong()), androidx.compose.foundation.shape.CircleShape))
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(ps.part.name, style = MaterialTheme.typography.bodyMedium)
                        Text(cm(ps.w) + "×" + cm(ps.d) + "×" + cm(ps.h) + " " + stringResource(R.string.cm), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("⬒ " + num(ps.top / 1e6, 2) + " " + m2, style = MaterialTheme.typography.bodyMedium)
                        Text("Σ " + num(ps.surface / 1e6, 2) + " " + m2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(stringResource(R.string.model_parts_about), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}

fun yardIcon(k: YardKind) = when (k) { YardKind.CANOPY -> "⛺"; YardKind.GAZEBO -> "🛖"; YardKind.ARCH -> "⌒"; YardKind.SWING -> "🎠"; YardKind.BBQ -> "🔥"; YardKind.GREEN_AREA -> "🌱"; YardKind.TREE -> "🌳"
    YardKind.POPLAR -> "🌲"; YardKind.CYPRESS -> "🌲"; YardKind.POLE -> "⚡"; YardKind.ROCK -> "🪨" }

@Composable
fun yardName(k: YardKind): String = stringResource(when (k) {
    YardKind.CANOPY -> R.string.y_canopy; YardKind.GAZEBO -> R.string.y_gazebo; YardKind.ARCH -> R.string.y_arch
    YardKind.SWING -> R.string.y_swing; YardKind.BBQ -> R.string.y_bbq; YardKind.GREEN_AREA -> R.string.y_green_area; YardKind.TREE -> R.string.y_tree
    YardKind.POPLAR -> R.string.y_poplar; YardKind.CYPRESS -> R.string.y_cypress; YardKind.POLE -> R.string.y_pole; YardKind.ROCK -> R.string.y_rock
})

/** A yard construction's sizes and make: for a canopy its roof, trusses and covering too. */
@Composable
private fun YardDialog(initial: YardSpec, onDismiss: () -> Unit, onOk: (YardSpec) -> Unit) {
    var s by remember { mutableStateOf(initial) }
    var w by remember { mutableStateOf(cm(initial.w.toDouble())) }
    var d by remember { mutableStateOf(cm(initial.d.toDouble())) }
    var h by remember { mutableStateOf(cm(initial.h.toDouble())) }
    val ru = language() == "ru"
    fun mm(t: String, old: Int) = t.replace(',', '.').toDoubleOrNull()?.let { (it * 10).roundToInt().coerceIn(300, 30000) } ?: old
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(yardIcon(s.kind) + " " + yardName(s.kind)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(w, { w = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.y_width), maxLines = 1) }, suffix = { Text(stringResource(R.string.cm)) }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                    OutlinedTextField(d, { d = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.y_depth), maxLines = 1) }, suffix = { Text(stringResource(R.string.cm)) }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                    OutlinedTextField(h, { h = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.y_height), maxLines = 1) }, suffix = { Text(stringResource(R.string.cm)) }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                }
                if (s.kind == YardKind.CANOPY || s.kind == YardKind.GAZEBO) {
                    if (s.kind == YardKind.CANOPY) {
                        Text(stringResource(R.string.y_roof), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(Modifier.hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for ((r, label) in listOf(RoofShape.SLOPE to R.string.y_slope, RoofShape.GABLE to R.string.y_gable, RoofShape.ARCH to R.string.y_vault))
                                FilterChip(s.roof == r, onClick = { s = s.copy(roof = r) }, label = { Text(stringResource(label)) })
                        }
                        Text(stringResource(R.string.y_truss), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(Modifier.hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for ((t, label) in listOf(TrussKind.NONE to R.string.y_rafters, TrussKind.TRIANGLE to R.string.y_triangle, TrussKind.WARREN to R.string.y_warren, TrussKind.ARCHED to R.string.y_arched))
                                FilterChip(s.truss == t, onClick = { s = s.copy(truss = t) }, label = { Text(stringResource(label)) })
                        }
                    }
                    Text(stringResource(R.string.y_cover), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in Cover.entries) FilterChip(s.cover == c, onClick = { s = s.copy(cover = c) }, label = { Text(Yard.coverName(c, ru)) },
                            leadingIcon = { if (c != Cover.NONE) Box(Modifier.size(10.dp).background(Color(0xFF000000 or c.color.toLong()), androidx.compose.foundation.shape.CircleShape)) })
                    }
                    Text(stringResource(R.string.y_pitch) + ": ${s.pitch}°", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (p in listOf(5, 10, 15, 20, 25, 30, 35, 45)) FilterChip(s.pitch == p, onClick = { s = s.copy(pitch = p) }, label = { Text("$p°") })
                    }
                    if (s.kind == YardKind.CANOPY) {
                        Text(stringResource(R.string.y_step) + ": " + cm(s.step.toDouble()) + " " + stringResource(R.string.cm), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(Modifier.hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (p in listOf(2000, 2500, 3000, 3500, 4000)) FilterChip(s.step == p, onClick = { s = s.copy(step = p) }, label = { Text(cm(p.toDouble())) })
                        }
                        Row(Modifier.hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (p in listOf(60, 80, 100)) FilterChip(s.post == p, onClick = { s = s.copy(post = p) }, label = { Text("▮ $p×$p") })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onOk(s.copy(w = mm(w, s.w), d = mm(d, s.d), h = mm(h, s.h))) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun groupName(g: Catalog.Group): String {
    if (g == Catalog.Group.NETWORK) return "Network"
    return stringResource(when (g) {
    Catalog.Group.FURNITURE -> R.string.g_furniture; Catalog.Group.KITCHEN -> R.string.g_kitchen
    Catalog.Group.BATHROOM -> R.string.g_bathroom; Catalog.Group.HEATING -> R.string.g_heating
    Catalog.Group.NETWORK -> R.string.g_furniture
    })
}

/**
 * Hands [parts] to Idea 3D as a 3MF (with their colours) under [name], where it opens as a new scene to change; its
 * "→ Roomik" sends it back here, and the tag in the name puts it in place of what it was.
 */
fun sendToIdea3d(context: android.content.Context, parts: List<com.dawoyan.roomik.core.ModelPart>, name: String) {
    val dir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
    val file = java.io.File(dir, name.replace(Regex("[^A-Za-z0-9А-Яа-яЁёԱ-Ֆա-ֆ #._-]"), "_") + ".3mf")
    file.writeBytes(Models.toThreeMf(parts))
    val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val intent = android.content.Intent("com.davoyans.idea3d.IMPORT_MODEL").setPackage("com.davoyans.idea3d")
        .setDataAndType(uri, "model/3mf").putExtra("name", name)
        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(intent) } catch (e: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "Idea 3D is not installed", android.widget.Toast.LENGTH_LONG).show()
    }
}

/** A small compass over the plan, N in red; tapped, it asks where south is. */
@Composable
private fun Compass(vm: AppViewModel, modifier: Modifier) {
    val root = vm.root ?: return
    var open by remember { mutableStateOf(false) }
    val s = root.south
    val tm = rememberTextMeasurer()
    Canvas(modifier.size(46.dp).clickable { open = true }) {
        val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2
        drawCircle(Color.White.copy(alpha = 0.85f), r, c); drawCircle(Color(0xFF6B6F78), r, c, style = Stroke(1.dp.toPx()))
        val a = Math.toRadians(s.toDouble())
        val n = Offset(Math.sin(a).toFloat(), -Math.cos(a).toFloat())
        val side = Offset(-n.y, n.x)
        val tip = c + n * (r * 0.8f); val tail = c - n * (r * 0.8f)
        drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(c.x + side.x * r * 0.25f, c.y + side.y * r * 0.25f); lineTo(c.x - side.x * r * 0.25f, c.y - side.y * r * 0.25f); close() }, Color(0xFFC0392B))
        drawPath(Path().apply { moveTo(tail.x, tail.y); lineTo(c.x + side.x * r * 0.25f, c.y + side.y * r * 0.25f); lineTo(c.x - side.x * r * 0.25f, c.y - side.y * r * 0.25f); close() }, Color(0xFF2F4F7F))
        val st = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF20242C))
        val sl = tm.measure("S", st); val sp = c - n * (r * 0.62f) + side * (r * 0.45f)
        drawText(sl, topLeft = Offset(sp.x - sl.size.width / 2, sp.y - sl.size.height / 2))
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.where_south)) },
        text = {
            Column {
                Text(stringResource(R.string.where_south_about), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // where south is on the plan: down 0°, then clockwise (left 90°, up 180°, right 270°)
                val grid = listOf(listOf(135 to "↖", 180 to "↑", 225 to "↗"), listOf(90 to "←", -1 to "", 270 to "→"), listOf(45 to "↙", 0 to "↓", 315 to "↘"))
                for (row in grid) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((deg, arrow) in row) if (deg < 0) Box(Modifier.size(56.dp)) { Text("S", Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleMedium) }
                    else FilterChip(s == deg, onClick = { vm.commitHouse(root.copy(south = deg)) }, label = { Text(arrow, style = MaterialTheme.typography.titleMedium) }, modifier = Modifier.size(56.dp))
                }
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { vm.commitHouse(root.copy(south = (s + 355) % 360)) }) { Text("⟲ 5°") }
                    Text("$s°", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { vm.commitHouse(root.copy(south = (s + 5) % 360)) }) { Text("⟳ 5°") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.ok)) } },
    )
}

/** Picking more walls by tapping them, and all the floor's outer walls at once. */
@Composable
private fun WallPickChips(ed: EditorState, plan: Plan, ids: Set<Int>) {
    FilterChip(ed.pickWalls, onClick = { ed.pickWalls = !ed.pickWalls }, label = { Text("＋ " + stringResource(R.string.pick_walls)) },
        leadingIcon = { Checkbox(ed.pickWalls, null, Modifier.size(18.dp)) })
    AssistChip(onClick = {
        val outer = PlanOps.outerWallIds(plan)
        if (outer.isNotEmpty()) { ed.sel = if (outer.size == 1) Sel.WallSel(outer.first()) else Sel.WallsSel(outer); ed.pickWalls = true }
    }, label = { Text("⬚ " + stringResource(R.string.outer_walls)) })
}

/** The stone a wall or fence is clad with, opening the choice. */
@Composable
private fun FacingChip(f: Facing?, mixed: Boolean = false, onClick: () -> Unit) {
    val lang = language()
    AssistChip(onClick = onClick, label = { Text(if (mixed) "…" else f?.let { Facings.name(it, lang) } ?: stringResource(R.string.facing)) },
        leadingIcon = { Box(Modifier.size(16.dp).background(f?.let { Color(0xFF000000 or it.color.toLong()) } ?: Color(0xFFF1EEE8), RoundedCornerShape(3.dp))
            .border(1.dp, Color(0x55000000), RoundedCornerShape(3.dp))) })
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/** Where the L (sign 1) or R (-1) badge of wall shape [s] is, plan mm, [gapPx] off its face; and the stone on that side. */
private fun sideBadge(s: com.dawoyan.roomik.core.WallShape, sign: Double, ed: EditorState, gapPx: Float): Pair<P, Facing?> {
    val d = (s.b - s.a).norm()
    val f = if (sign > 0) s.wall.left else s.wall.right
    return s.mid + d.left() * (sign * (s.wall.thickness / 2.0 + gapPx / ed.scale)) to f
}

@Composable
fun roofCoverName(c: RoofCover): String = stringResource(when (c) {
    RoofCover.TILES -> R.string.rc_tiles; RoofCover.PROFILE_SHEET -> R.string.rc_profile; RoofCover.METAL_TILE -> R.string.rc_metal_tile
    RoofCover.SOFT_TILE -> R.string.rc_soft; RoofCover.SLATE -> R.string.rc_slate
})

/** Pick one of [colors]. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ColorDialog(title: String, current: Int, colors: List<Int>, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (c in colors) Box(Modifier.size(44.dp).background(Color(0xFF000000 or c.toLong()), RoundedCornerShape(8.dp))
                .border(if (c == current) 3.dp else 1.dp, if (c == current) MaterialTheme.colorScheme.primary else Color(0x44000000), RoundedCornerShape(8.dp))
                .clickable { onPick(c) })
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

/** A gate on the plan between [pa] and [pb], opening towards [n]: leaves swinging open, a sliding one with its way, a garage door. */
private fun DrawScope.gateSymbol(ed: EditorState, pa: P, pb: P, n: P, kind: GateKind, gcol: Color, sw: Float) {
    val d = (pb - pa).norm()
    fun arc(hinge: P, toward: P) {
        val r = (toward - hinge).len
        drawLine(gcol, ed.toScreen(hinge), ed.toScreen(hinge + n * r), sw * 1.5f)
        val arcPath = Path()
        for (k in 0..12) {
            val a = Math.PI / 2 * k / 12
            val q = hinge + (toward - hinge) * Math.cos(a) + n * (r * Math.sin(a))
            val o = ed.toScreen(q); if (k == 0) arcPath.moveTo(o.x, o.y) else arcPath.lineTo(o.x, o.y)
        }
        drawPath(arcPath, gcol, style = Stroke(sw * 0.7f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
    }
    val mid = (pa + pb) * 0.5
    when (kind) {
        GateKind.SWING -> { arc(pa, mid); arc(pb, mid) }
        GateKind.WICKET, GateKind.SINGLE -> arc(pa, pb)
        GateKind.SLIDING -> {
            drawLine(gcol, ed.toScreen(pa + n * 120.0), ed.toScreen(pb + n * 120.0), sw * 2f)
            drawLine(gcol, ed.toScreen(pb + n * 120.0), ed.toScreen(pb + d * (pb - pa).len + n * 120.0), sw * 0.7f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        }
        GateKind.SECTIONAL, GateKind.ROLLER -> {
            // lifts up: its leaf drawn dashed inside, where it hangs under the ceiling when open
            drawLine(gcol, ed.toScreen(pa), ed.toScreen(pb), sw * 2f)
            val depth = if (kind == GateKind.SECTIONAL) 2400.0 else 300.0
            val box = listOf(pa, pb, pb + n * depth, pa + n * depth, pa).map { ed.toScreen(it) }
            for (k in 0 until 4) drawLine(gcol, box[k], box[k + 1], sw * 0.7f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        }
    }
}

/** A road's centre line as a fence's, to use the fence's handles and dragging for its turns. */
private fun roadLine(r: Road) = com.dawoyan.roomik.core.Fence(r.id, FenceKind.PICKET, r.points)

/** The bottom menu a tool belongs to. */
fun categoryOf(t: Tool): Tool = when (t) {
    Tool.ELECTRIC -> Tool.ELECTRIC
    Tool.INTERIOR -> Tool.INTERIOR
    Tool.YARD -> Tool.YARD
    Tool.SYSTEMS -> Tool.SYSTEMS
    else -> Tool.WALL
}
