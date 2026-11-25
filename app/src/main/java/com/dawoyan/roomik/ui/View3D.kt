package com.dawoyan.roomik.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.Search
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.dawoyan.roomik.core.floorRange
import com.dawoyan.roomik.core.floor
import com.dawoyan.roomik.core.allFloors
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.dawoyan.roomik.core.Geo
import com.dawoyan.roomik.core.Interior
import com.dawoyan.roomik.core.P
import com.dawoyan.roomik.core.Plan
import com.dawoyan.roomik.core.Rooms
import com.dawoyan.roomik.core.elevation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dawoyan.roomik.AppViewModel
import com.dawoyan.roomik.R
import com.dawoyan.roomik.core.Mesh
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun View3DScreen(vm: AppViewModel) {
    val root = vm.root ?: return
    val context = LocalContext.current
    // What is shown: the whole house (null), or one floor on its own. Walking inside is on that floor, or on the one
    // being edited when the whole house is shown.
    var shown by remember { mutableStateOf<Int?>(if (root.floors.isEmpty() && root.basements.isEmpty()) vm.floor else null) }
    val floorNo = shown ?: vm.floor
    val plan = root.floor(floorNo)
    // with a roof the house is first seen whole; low walls take the roof off to look in
    var cutaway by remember { mutableStateOf(true) }
    val hidden = vm.hidden + if (cutaway) setOf(com.dawoyan.roomik.core.Layer.ROOF) else emptySet()
    var ghost by remember { mutableStateOf(false) }
    fun meshFor(h: Set<com.dawoyan.roomik.core.Layer>, step: Int = 0) = shown?.let { Mesh.build(root, it, it, markClashes = true, hidden = h, residentFloor = floorNo, residentStep = step) } ?: Mesh.build(root, root.floors.size, markClashes = true, hidden = h, residentFloor = floorNo, residentStep = step)
    val renderer = remember(root, shown) {
        val mesh = meshFor(hidden)
        // "Low walls" cuts the top storey shown, not the one being edited
        val topShown = shown ?: root.floors.size
        PlanRenderer(mesh, plan, root.elevation(floorNo) / 1000f, root.elevation(topShown) / 1000f).also { it.cutaway = cutaway; it.ghost = ghost; it.south = root.south }
    }
    var xray by remember { mutableStateOf(false) }
    var lensAt by remember { mutableStateOf<Offset?>(null) }
    val lensR = with(LocalDensity.current) { 95.dp.toPx() }
    var inside by remember { mutableStateOf(false) }
    var sun by remember { mutableFloatStateOf(0.45f) }
    // the season: the sun's declination (winter, spring/autumn, summer)
    var season by remember { mutableStateOf(0.0) }
    // photos of each side of the plot (north, east, south, west), stood round it in 3D
    var photosOpen by remember { mutableStateOf(false) }
    var photoTick by remember { mutableStateOf(0) }
    var photosReady by remember { mutableStateOf(0) }
    LaunchedEffect(renderer, photoTick) {
        val bms = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Array(4) { i -> SidePhotos.bitmap(context, root.id, i) } }
        renderer.photos = bms; renderer.photosChanged = true; photosReady++
    }
    var lightsOn by remember { mutableStateOf(true) }
    var followResident by remember { mutableStateOf(true) }
    var fadeToken by remember { mutableStateOf(0) }
    // Each switch controls only the lamps linked to it in the electrical plan.  Values survive recomposition while this view is open.
    val switchOn = remember(plan) { mutableStateMapOf<Int, Boolean>().apply { plan.points.filter { it.kind == com.dawoyan.roomik.core.PointKind.SWITCH }.forEach { put(it.id, true) } } }
    // with a roof the house is first seen whole; low walls take the roof off to look in

    val view = remember(renderer) { PlanGlView(context, renderer) }
    // This GL view normally renders on demand. Request the intervening frames so a switch change visibly fades.
    LaunchedEffect(fadeToken) { if (fadeToken > 0) repeat(36) { withFrameNanos { }; view.requestRender() } }
    LaunchedEffect(view, photosReady) { view.requestRender() }
    if (photosOpen) SidePhotosDialog(root.id, onChanged = { photoTick++ }) { photosOpen = false }
    // layers turned on or off: the same view gets the new mesh, so the camera stays
    LaunchedEffect(renderer, ghost) { renderer.ghost = ghost; view.requestRender() }
    LaunchedEffect(renderer, hidden, followResident) {
        var step = 0
        if (followResident) renderer.startFollowing()
        while (true) {
            val frame = step++
            val (m, position) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { meshFor(hidden, frame) to Mesh.residentPosition(plan, frame) }
            if (followResident && !renderer.inside) position?.let { renderer.follow(it) }
            renderer.swap(m); view.requestRender()
            kotlinx.coroutines.delay(250)
        }
    }
    // Opened from a room ("View inside" in the editor, or from elektronik): start inside it.
    LaunchedEffect(renderer) {
        vm.insideAt?.let { at -> vm.insideAt = null; if (renderer.goInside(P(at.x / 1000, at.y / 1000))) { inside = true; view.requestRender() } }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, view) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) view.onPause() else if (e == Lifecycle.Event.ON_RESUME) view.onResume()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { LayersButton(vm) },
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    SingleChoiceSegmentedButtonRow(Modifier.padding(end = 8.dp)) {
                        SegmentedButton(!inside, { inside = false; renderer.inside = false; view.requestRender() }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.outside)) }
                        SegmentedButton(inside, { inside = true; renderer.goInside(null); view.requestRender() }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.inside)) }
                    }
                    FilledTonalButton(onClick = { vm.back() }, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.view_2d)) } },
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            androidx.compose.runtime.key(view) { AndroidView(factory = { view }, modifier = Modifier.fillMaxSize()) }
            // Gestures sit on top: one finger turns the camera around the plan, two fingers move and zoom.
            // Gestures sit on top. Outside: one finger turns around the plan, two move and zoom, a tap on a room walks in.
            // Inside: one finger looks around, two fingers step, a tap on the floor walks there.
            Box(Modifier.fillMaxSize().pointerInput(renderer, xray) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var moved = false
                    var multi = false
                    // with X-ray on, a drag that starts on the lens moves the lens
                    val lensDrag = xray && lensAt?.let { (down.position - it).getDistance() <= lensR * 1.15f } == true
                    while (true) {
                        val ev = awaitPointerEvent()
                        val pressed = ev.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size == 1 && !multi) {
                            if ((pressed[0].position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                            if (moved && lensDrag) {
                                val c = pressed[0].position
                                lensAt = c
                                renderer.lens = floatArrayOf(c.x, size.height - c.y, lensR)
                            } else if (moved) {
                                val d = pressed[0].position - pressed[0].previousPosition
                                if (renderer.inside) renderer.look(d.x * 0.18f, d.y * 0.15f) else renderer.orbit(-d.x * 0.22f, d.y * 0.18f)
                            }
                        } else if (pressed.size >= 2) {
                            multi = true
                            val pan = ev.calculatePan()
                            // inside: two fingers step, and pinching widens or narrows the view (zoom out / in)
                            if (renderer.inside) { renderer.step(pan.y / size.height * 4f, -pan.x / size.height * 2f); renderer.zoomInside(ev.calculateZoom()) }
                            else { renderer.zoom(ev.calculateZoom()); renderer.pan(pan.x / size.height, pan.y / size.height) }
                        }
                        ev.changes.forEach { it.consume() }
                        view.requestRender()
                    }
                    if (!moved && !multi && lensDrag) {
                        // a tap on the lens does nothing more
                    } else if (!moved && !multi) {
                        val hit = renderer.floorAt(down.position.x / size.width, down.position.y / size.height)
                        if (hit != null) {
                            if (renderer.inside) renderer.walkTo(hit) else if (renderer.goInside(hit)) inside = true
                            view.requestRender()
                        }
                    }
                }
            })
            if (xray) lensAt?.let { c ->
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color.White, lensR, c, style = Stroke(3.dp.toPx()))
                    drawCircle(Color(0xFF2F7FD6), lensR + 2.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
                    // a handle at the bottom right of the lens, like a magnifier's
                    val k = (lensR + 2.dp.toPx()) / 1.414f
                    drawLine(Color(0xFF2F7FD6), Offset(c.x + k, c.y + k), Offset(c.x + k + 28.dp.toPx(), c.y + k + 28.dp.toPx()), 7.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            if (root.floors.isNotEmpty() || root.basements.isNotEmpty())
                Row(Modifier.align(Alignment.TopStart).padding(8.dp).hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FloorChip(stringResource(R.string.whole_house), shown == null, null) { shown = null }
                    for (i in root.floorRange.reversed()) FloorChip(floorName(i, root), shown == i, null) { shown = i }
                }
            Surface(Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    // what is shown: outer walls faint, and each system on or off
                    Row(Modifier.hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(followResident, { followResident = !followResident }, label = { Text("👣 Следить") })
                        // the sun's season, and the photos round the plot
                        for ((d, icon) in listOf(-23.4 to "❄", 0.0 to "☀", 23.4 to "🌞"))
                            FilterChip(season == d, { season = d; renderer.declination = d; view.requestRender() }, label = { Text(icon) })
                        if (!inside) AssistChip(onClick = { photosOpen = true }, label = { Text("📷 " + stringResource(R.string.side_photos)) })
                        if (!inside) FilterChip(ghost, { ghost = !ghost; renderer.ghost = ghost; view.requestRender() }, label = { Text(stringResource(R.string.outer_walls) + " 10%") })
                        for ((l, name) in listOf(com.dawoyan.roomik.core.Layer.FURNITURE to R.string.tool_interior)) {
                            val on = l !in vm.hidden
                            FilterChip(on, { vm.hidden = if (on) vm.hidden + l else vm.hidden - l }, label = { Text(stringResource(name)) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WbSunny, stringResource(R.string.sun), Modifier.size(20.dp))
                        val hr = 5.0 + 16.0 * sun
                        Text(String.format(java.util.Locale.US, "%d:%02d", hr.toInt(), ((hr % 1) * 60).toInt()), Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
                        Slider(sun, { sun = it; renderer.sun = it; view.requestRender() }, Modifier.weight(1f).padding(horizontal = 8.dp))
                        if (inside) FilterChip(lightsOn, { lightsOn = !lightsOn; renderer.lightsOn = lightsOn; fadeToken++; view.requestRender() },
                            label = { Text(stringResource(R.string.lights)) }, leadingIcon = { Icon(Icons.Default.Lightbulb, null, Modifier.size(18.dp)) })
                        else FilterChip(cutaway, { cutaway = !cutaway; renderer.cutaway = cutaway; view.requestRender() },
                            label = { Text(stringResource(R.string.cutaway)) })
                        FilterChip(xray, {
                            xray = !xray
                            if (xray) {
                                val c = lensAt ?: Offset(view.width / 2f, view.height * 0.45f)
                                lensAt = c
                                renderer.lens = floatArrayOf(c.x, view.height - c.y, lensR)
                            } else renderer.lens = null
                            view.requestRender()
                        }, label = { Text(stringResource(R.string.xray)) }, modifier = Modifier.padding(start = 6.dp),
                            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) })
                    }
                    if (inside) {
                        val switches = plan.points.filter { it.kind == com.dawoyan.roomik.core.PointKind.SWITCH && plan.points.any { lamp -> lamp.kind == com.dawoyan.roomik.core.PointKind.LAMP && lamp.switchId == it.id } }
                        if (switches.isNotEmpty()) Row(Modifier.hScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            switches.forEachIndexed { index, sw ->
                                val on = switchOn[sw.id] != false
                                FilterChip(on, {
                                    // Only an explicit two-way pair shares a control state; cables alone never do.
                                    val next = !on
                                    switchOn[sw.id] = next
                                    sw.pairId?.takeIf { pid -> plan.points.firstOrNull { it.id == pid && it.kind == com.dawoyan.roomik.core.PointKind.SWITCH }?.pairId == sw.id }
                                        ?.let { switchOn[it] = next }
                                    renderer.switches = switchOn.toMap()
                                    fadeToken++
                                    view.requestRender()
                                }, label = { Text(stringResource(R.string.switch_) + " " + (index + 1)) }, leadingIcon = { Icon(Icons.Default.Lightbulb, null, Modifier.size(16.dp)) })
                            }
                        }
                    }
                    Text(stringResource(if (xray) R.string.hint_xray else if (inside) R.string.hint_inside else R.string.hint_3d), Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private class PlanGlView(context: Context, renderer: PlanRenderer) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(Msaa())
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
}

/** Asks for 4x anti-aliasing and falls back to none where the device has no such config. */
private class Msaa : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        fun pick(samples: Int): EGLConfig? {
            val attrs = intArrayOf(
                EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_DEPTH_SIZE, 24,
                EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_SAMPLE_BUFFERS, if (samples > 0) 1 else 0, EGL10.EGL_SAMPLES, samples, EGL10.EGL_NONE,
            )
            val n = IntArray(1)
            val out = arrayOfNulls<EGLConfig>(1)
            return if (egl.eglChooseConfig(display, attrs, out, 1, n) && n[0] > 0) out[0] else null
        }
        return pick(4) ?: pick(0) ?: error("no EGL config")
    }
}

/** Draws the plan mesh with an orbit camera: [yaw]/[pitch] in degrees around a target on the ground, [dist] metres away. */
class PlanRenderer(private var mesh: Mesh, private val plan: Plan, private val base: Float = 0f, private val cutBase: Float = base) : GLSurfaceView.Renderer {
    private val span = max(mesh.maxX - mesh.minX, mesh.maxZ - mesh.minZ)
    @Volatile private var yaw = 25f
    @Volatile private var pitch = 50f
    @Volatile private var dist = span * 1.3f + 4f
    @Volatile private var tx = (mesh.minX + mesh.maxX) / 2
    @Volatile private var tz = (mesh.minZ + mesh.maxZ) / 2
    fun startFollowing() { framed = true; dist = 5f; pitch = 22f; yaw = 15f }
    fun follow(p: P) { tx = (p.x / 1000).toFloat(); tz = (p.y / 1000).toFloat() }

    private var program = 0
    private var shadowProgram = 0
    private var shadowFbo = 0
    private var shadowTex = 0
    private var shadows = false
    @Volatile var sun = 0.45f
    @Volatile var lightsOn = true
    /** Requested switch positions. The render loop eases each lamp to its new brightness. */
    @Volatile var switches: Map<Int, Boolean> = emptyMap()
    private val switchLevels = HashMap<Int, Float>()
    @Volatile var cutaway = true
    /** Outer walls drawn faint (10%), to see into the house from outside. */
    @Volatile var ghost = false
    /** A mesh to show instead, from the next frame (layers turned on or off); the camera stays where it is. */
    @Volatile private var nextMesh: Mesh? = null
    fun swap(m: Mesh) { nextMesh = m }
    private var aspect = 1f
    @Volatile private var framed = false
    private lateinit var buffer: FloatBuffer

    // Inside: eye at [eyeX], [eyeZ] (metres), looking along [lookYaw]/[lookPitch] degrees.
    @Volatile var inside = false
    /** Field of view inside, degrees: pinching out narrows it (zoom in), pinching in widens it (zoom out). */
    @Volatile var fov = 70f
    /** The X-ray lens: centre x, y in GL pixels (from the bottom) and radius; null when off. */
    @Volatile var lens: FloatArray? = null
    fun zoomInside(k: Float) { fov = (fov / k).coerceIn(30f, 115f) }
    @Volatile private var eyeX = 0f
    @Volatile private var eyeZ = 0f
    @Volatile private var lookYaw = 0f
    @Volatile private var lookPitch = -8f
    private var placed = false
    private val rooms = Rooms.detect(plan)
    @Volatile private var lastMvp: FloatArray? = null

    private fun mm(x: Float, z: Float) = P(x * 1000.0, z * 1000.0)

    /** Goes inside at [at] (a floor point, metres), or the biggest room's middle. False when [at] is in no room. */
    fun goInside(at: P?): Boolean {
        val target = when {
            // in a room, or anywhere near the walls: an open garage or a yard has no closed room
            at != null -> at.takeIf { p -> rooms.any { Geo.inside(mm(p.x.toFloat(), p.y.toFloat()), it.polygon) } ||
                (p.x >= mesh.minX - 3 && p.x <= mesh.maxX + 3 && p.y >= mesh.minZ - 3 && p.y <= mesh.maxZ + 3) } ?: return false
            placed -> null
            else -> rooms.maxByOrNull { it.area }?.let { roomy(it) } ?: P(tx.toDouble(), tz.toDouble())
        }
        if (target != null) {
            eyeX = target.x.toFloat(); eyeZ = target.y.toFloat()
            // Face the longest free view, so the first look is into the room, not at a wall.
            lookYaw = (0 until 16).map { it * 22.5f }.maxBy { freeView(target, it) }
        }
        placed = true
        inside = true
        return true
    }

    /** The point of [room] farthest from any wall, in metres. */
    private fun roomy(room: com.dawoyan.roomik.core.Room): P {
        val xs = room.polygon.map { it.x }; val ys = room.polygon.map { it.y }
        var best = room.anchor; var bestD = -1.0
        var x = xs.min()
        while (x <= xs.max()) {
            var y = ys.min()
            while (y <= ys.max()) {
                val q = P(x, y)
                if (Geo.inside(q, room.polygon)) {
                    // every wall counts, also partitions standing inside the room
                    val d = plan.walls.minOf { w ->
                        val wa = plan.pos(w.a); val wb = plan.pos(w.b)
                        if (wa == null || wb == null) Double.MAX_VALUE else Geo.distToSegment(q, wa, wb) - w.thickness / 2.0
                    }
                    if (d > bestD) { bestD = d; best = q }
                }
                y += 100.0
            }
            x += 100.0
        }
        return P(best.x / 1000, best.y / 1000)
    }

    /** How far one sees from [from] (metres) looking along [yawDeg] before a wall, up to 15 m. */
    private fun freeView(from: P, yawDeg: Float): Double {
        val y = Math.toRadians(yawDeg.toDouble())
        val d = P(-sin(y), -cos(y))
        var r = 0.2
        while (r < 15 && Interior.canWalk(plan, mm(from.x.toFloat(), from.y.toFloat()), mm((from.x + d.x * r).toFloat(), (from.y + d.y * r).toFloat()))) r += 0.2
        return r
    }

    fun look(dx: Float, dy: Float) { lookYaw += dx; lookPitch = (lookPitch + dy).coerceIn(-70f, 70f) }

    /** Moves the eye towards [to] (metres) as far as walls let it, stopping 30 cm short of anything in the way. */
    private fun moveTo(to: P) {
        val from = P(eyeX.toDouble(), eyeZ.toDouble())
        val d = to - from
        val n = (d.len / 0.05).toInt().coerceAtLeast(1)
        var ok = from
        for (i in 1..n) {
            val q = from + d * (i.toDouble() / n)
            val ahead = q + d.norm() * 0.3
            if (!Interior.canWalk(plan, mm(from.x.toFloat(), from.y.toFloat()), mm(ahead.x.toFloat(), ahead.y.toFloat()))) break
            ok = q
        }
        eyeX = ok.x.toFloat(); eyeZ = ok.y.toFloat()
    }

    fun walkTo(p: P) = moveTo(p)

    /** [forward] and [right] in metres, along the ground. */
    fun step(forward: Float, right: Float) {
        val y = Math.toRadians(lookYaw.toDouble())
        val f = P(-sin(y), -cos(y)); val r = P(cos(y), -sin(y))
        moveTo(P(eyeX.toDouble(), eyeZ.toDouble()) + f * forward.toDouble() + r * right.toDouble())
    }

    /** The floor point under a screen point given as shares of the view's width and height, in metres; null above the horizon. */
    fun floorAt(fx: Float, fy: Float): P? {
        val m = lastMvp ?: return null
        val inv = FloatArray(16)
        if (!Matrix.invertM(inv, 0, m, 0)) return null
        fun un(z: Float): FloatArray {
            val o = FloatArray(4)
            Matrix.multiplyMV(o, 0, inv, 0, floatArrayOf(fx * 2 - 1, 1 - fy * 2, z, 1f), 0)
            return floatArrayOf(o[0] / o[3], o[1] / o[3], o[2] / o[3])
        }
        val a = un(-1f); val b = un(1f)
        // the floor of the storey being looked at
        if (a[1] - b[1] <= 1e-6f) return null
        val t = (a[1] - base) / (a[1] - b[1])
        if (t < 0) return null
        return P((a[0] + (b[0] - a[0]) * t).toDouble(), (a[2] + (b[2] - a[2]) * t).toDouble())
    }

    fun orbit(dYaw: Float, dPitch: Float) { yaw += dYaw; pitch = (pitch + dPitch).coerceIn(8f, 89f) }
    fun zoom(k: Float) { dist = (dist / k).coerceIn(1.5f, 200f) }

    /** [dx], [dy] are finger moves as a share of the view height; the ground follows the fingers. */
    fun pan(dx: Float, dy: Float) {
        val y = Math.toRadians(yaw.toDouble())
        val k = dist * 1.1f
        val rx = cos(y).toFloat(); val rz = -sin(y).toFloat()     // camera right on the ground
        val fx = -sin(y).toFloat(); val fz = -cos(y).toFloat()    // camera forward on the ground
        tx += (-dx * rx + dy * fx) * k
        tz += (-dx * rz + dy * fz) * k
    }

    private fun program(vs: String, fs: String): Int {
        val prog = GLES20.glCreateProgram()
        for ((type, src) in listOf(GLES20.GL_VERTEX_SHADER to vs, GLES20.GL_FRAGMENT_SHADER to fs)) {
            val sh = GLES20.glCreateShader(type)
            GLES20.glShaderSource(sh, src); GLES20.glCompileShader(sh)
            val ok = IntArray(1); GLES20.glGetShaderiv(sh, GLES20.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) android.util.Log.e("Roomik", "shader: " + GLES20.glGetShaderInfoLog(sh))
            GLES20.glAttachShader(prog, sh)
        }
        listOf("aPos", "aNormal", "aColor", "aExtra").forEachIndexed { i, n -> GLES20.glBindAttribLocation(prog, i, n) }
        GLES20.glLinkProgram(prog)
        return prog
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // a new GL context: textures and programs of the old one are gone, and their numbers may now be others' (the shadow map)
        for (i in 0 until 4) photoTex[i] = 0
        photoProgram = 0
        photosChanged = true
        buffer = ByteBuffer.allocateDirect(mesh.data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(mesh.data); position(0) }
        program = program(VS, FS)
        shadowProgram = program(SHADOW_VS, SHADOW_FS)
        // Shadow map: depth packed into an RGBA texture, which every GLES 2 device can render to.
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0); shadowTex = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadowTex)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, SHADOW_SIZE, SHADOW_SIZE, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        for (pn in intArrayOf(GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_TEXTURE_MAG_FILTER)) GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, pn, GLES20.GL_NEAREST)
        for (pn in intArrayOf(GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_TEXTURE_WRAP_T)) GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, pn, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glGenFramebuffers(1, ids, 0); shadowFbo = ids[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadowFbo)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, shadowTex, 0)
        GLES20.glGenRenderbuffers(1, ids, 0)
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, ids[0])
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, SHADOW_SIZE, SHADOW_SIZE)
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, ids[0])
        shadows = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
    }

    private var width = 1
    private var height = 1

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width; this.height = height
        GLES20.glViewport(0, 0, width, height)
        aspect = width.toFloat() / max(1, height)
        if (!framed) { // far enough to see the whole plan, also on a narrow portrait screen
            framed = true
            val t = Math.tan(Math.toRadians(22.5)).toFloat()
            dist = ((span / 2 + 1f) / (t * minOf(1f, aspect)) * 1.15f).coerceIn(3f, 200f)
        }
    }

    /** Where south is on the plan (degrees clockwise from its bottom), the season (sun's declination) and latitude. */
    @Volatile var south = 0
    @Volatile var declination = 0.0
    @Volatile var latitude = 40.2

    /** The hour of day [sun] stands for: 5:00 … 21:00. */
    fun hour(): Double = 5.0 + 16.0 * sun.coerceIn(0f, 1f)

    /** The sun's direction (towards it) at [hour], for the season and the plan's south (east in the morning, west at evening). */
    private fun sunDir(): FloatArray {
        val f = Math.toRadians(latitude); val d = Math.toRadians(declination); val h = Math.toRadians((hour() - 12.0) * 15.0)
        var el = Math.asin(sin(f) * sin(d) + cos(f) * cos(d) * cos(h))
        val az = Math.atan2(sin(h), cos(h) * sin(f) - Math.tan(d) * cos(f))   // from south, towards west
        el = el.coerceAtLeast(Math.toRadians(2.0))
        val s = Math.toRadians(south.toDouble())
        // on the plan (y down): south and west
        val sx = -sin(s); val sy = cos(s); val wx = -cos(s); val wy = -sin(s)
        val hx = sx * cos(az) + wx * sin(az); val hy = sy * cos(az) + wy * sin(az)
        return floatArrayOf((hx * cos(el)).toFloat(), sin(el).toFloat(), (hy * cos(el)).toFloat())
    }

    /** Photos of what is round the plot, looking north, east, south and west (index 0..3); loaded on the GL thread. */
    @Volatile var photos: Array<android.graphics.Bitmap?> = arrayOfNulls(4)
    @Volatile var photosChanged = false
    private val photoTex = IntArray(4)
    private val photoAspect = FloatArray(4)
    private var photoProgram = 0

    private fun loadPhotos() {
        photosChanged = false
        for (i in 0 until 4) {
            if (photoTex[i] != 0) { GLES20.glDeleteTextures(1, photoTex, i); photoTex[i] = 0 }
            val b = photos[i] ?: continue
            val ids = IntArray(1); GLES20.glGenTextures(1, ids, 0); photoTex[i] = ids[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0)
            photoAspect[i] = b.width.toFloat() / b.height.coerceAtLeast(1)
        }
    }

    /**
     * The photos as one view all round: each covers its quarter (and blends into the next one at the seams). Its upper
     * half (the sky and what is far) stands up on a ring round the plot; its lower half (the near ground) lies on the
     * ground, coming in towards the house and fading into the grass, so the house stands in the picture.
     */
    private fun drawPhotos(mvp: FloatArray, eye: FloatArray) {
        if (photoTex.all { it == 0 }) return
        if (photoProgram == 0) {
            val prog = GLES20.glCreateProgram()
            for ((type, src) in listOf(GLES20.GL_VERTEX_SHADER to PHOTO_VS, GLES20.GL_FRAGMENT_SHADER to PHOTO_FS)) {
                val sh = GLES20.glCreateShader(type); GLES20.glShaderSource(sh, src); GLES20.glCompileShader(sh); GLES20.glAttachShader(prog, sh)
            }
            GLES20.glBindAttribLocation(prog, 0, "aPos"); GLES20.glBindAttribLocation(prog, 1, "aUv"); GLES20.glBindAttribLocation(prog, 2, "aA")
            GLES20.glLinkProgram(prog); photoProgram = prog
        }
        GLES20.glUseProgram(photoProgram)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(photoProgram, "uMvp"), 1, false, mvp, 0)
        GLES20.glDisableVertexAttribArray(3)
        val cx = (mesh.minX + mesh.maxX) / 2; val cz = (mesh.minZ + mesh.maxZ) / 2
        val half = max(mesh.maxX - mesh.minX, mesh.maxZ - mesh.minZ) / 2
        // the ring the pictures stand on, close round the plot; the near ground comes in to just off the house
        val ringR = half + max(8f, half * 0.7f)
        val nearR = half + 0.3f
        val s = Math.toRadians(south.toDouble())
        val dirs = listOf(floatArrayOf(sin(s).toFloat(), -cos(s).toFloat()), floatArrayOf(cos(s).toFloat(), sin(s).toFloat()),
            floatArrayOf(-sin(s).toFloat(), cos(s).toFloat()), floatArrayOf(-cos(s).toFloat(), -sin(s).toFloat()))
        val gy = mesh.groundY + 0.02f
        val span = 110.0                 // degrees each picture covers: 90 and 10 more each side to blend with the next
        val cols = 28; val groundRows = 10
        GLES20.glEnable(GLES20.GL_BLEND); GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDepthMask(false)
        for (i in 0 until 4) {
            if (photoTex[i] == 0) continue
            val d = dirs[i]; val ax = -d[1]; val az = d[0]
            val hasLeft = photoTex[(i + 3) % 4] != 0; val hasRight = photoTex[(i + 1) % 4] != 0
            val arc = (ringR * Math.toRadians(span)).toFloat()
            // a photo taken standing has its horizon a little under the middle: above it stands, below it lies down
            val hz = 0.6f
            val upH = arc / photoAspect[i].coerceAtLeast(0.3f) * hz
            val q = ArrayList<Float>(cols * (groundRows + 1) * 36)
            fun col(k: Int): FloatArray {
                val al = Math.toRadians(-span / 2 + span * k / cols)
                val dx = (d[0] * cos(al) + ax * sin(al)).toFloat(); val dz = (d[1] * cos(al) + az * sin(al)).toFloat()
                val u = k.toFloat() / cols
                // the seams: fade out where the next picture fades in (none there: the picture goes on to its edge)
                val deg = Math.toDegrees(al)
                val edge = if ((deg < 0 && hasLeft) || (deg > 0 && hasRight)) ((span / 2 - kotlin.math.abs(deg)) / 20.0).coerceIn(0.0, 1.0).toFloat() else 1f
                return floatArrayOf(dx, dz, u, edge)
            }
            fun put(c: FloatArray, r: Float, y: Float, v: Float, a: Float) { q += cx + c[0] * r; q += y; q += cz + c[1] * r; q += c[2]; q += v; q += a * c[3] }
            for (k in 0 until cols) {
                val c0 = col(k); val c1 = col(k + 1)
                // standing part: hidden where the camera is out past it on that side, so it never blocks the view
                val mx = (c0[0] + c1[0]) / 2; val mz = (c0[1] + c1[1]) / 2
                val ex = eye[0] - cx; val ez = eye[2] - cz
                val outside = ex * ex + ez * ez > ringR * ringR * 0.9f
                val stand = !(outside && ex * mx + ez * mz > 0f)
                if (stand) {
                    put(c0, ringR, gy + upH, 0f, 1f); put(c1, ringR, gy + upH, 0f, 1f); put(c1, ringR, gy, hz, 1f)
                    put(c0, ringR, gy + upH, 0f, 1f); put(c1, ringR, gy, hz, 1f); put(c0, ringR, gy, hz, 1f)
                    // the photo's sky goes on up, its top row stretched, fading out high above
                    val top = gy + upH * 3f
                    put(c0, ringR, top, 0.002f, 0f); put(c1, ringR, top, 0.002f, 0f); put(c1, ringR, gy + upH, 0.002f, 1f)
                    put(c0, ringR, top, 0.002f, 0f); put(c1, ringR, gy + upH, 0.002f, 1f); put(c0, ringR, gy + upH, 0.002f, 1f)
                }
                // lying part: from the ring in to near the house, fading to nothing at the house
                for (j in 0 until groundRows) {
                    val f0 = j.toFloat() / groundRows; val f1 = (j + 1).toFloat() / groundRows
                    val r0 = ringR - (ringR - nearR) * f0; val r1 = ringR - (ringR - nearR) * f1
                    val v0 = hz + (1 - hz) * f0; val v1 = hz + (1 - hz) * f1
                    // the picture's ground right up to the house, only its last stretch fading into the plain ground
                    fun fade(f: Float) = ((1f - f) / 0.3f).coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
                    put(c0, r0, gy, v0, fade(f0)); put(c1, r0, gy, v0, fade(f0)); put(c1, r1, gy, v1, fade(f1))
                    put(c0, r0, gy, v0, fade(f0)); put(c1, r1, gy, v1, fade(f1)); put(c0, r1, gy, v1, fade(f1))
                }
            }
            val arr = q.toFloatArray()
            val fb = ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(arr); position(0) }
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, photoTex[i])
            GLES20.glUniform1i(GLES20.glGetUniformLocation(photoProgram, "uTex"), 1)
            fb.position(0); GLES20.glEnableVertexAttribArray(0); GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 24, fb)
            fb.position(3); GLES20.glEnableVertexAttribArray(1); GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 24, fb)
            fb.position(5); GLES20.glEnableVertexAttribArray(2); GLES20.glVertexAttribPointer(2, 1, GLES20.GL_FLOAT, false, 24, fb)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, arr.size / 6)
        }
        GLES20.glDepthMask(true); GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisableVertexAttribArray(2)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    }

    private fun attribs() {
        val stride = Mesh.STRIDE * 4
        for (i in 0..3) {
            buffer.position(i * 3)
            GLES20.glEnableVertexAttribArray(i)
            GLES20.glVertexAttribPointer(i, if (i == 3) 4 else 3, GLES20.GL_FLOAT, false, stride, buffer)
        }
    }

    /** Uniforms both programs share: the wall cutaway. */
    private fun cutUniforms(prog: Int, eye: FloatArray) {
        val cx = (mesh.minX + mesh.maxX) / 2; val cz = (mesh.minZ + mesh.maxZ) / 2
        var dx = eye[0] - cx; var dz = eye[2] - cz
        val l = kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(1e-3f); dx /= l; dz /= l
        GLES20.glUniform4f(GLES20.glGetUniformLocation(prog, "uCut"), cx, cz, dx, dz)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(prog, "uCutOn"), if (cutaway && !inside) 1f else 0f, Math.round(cutBase * 10).toFloat())
    }

    override fun onDrawFrame(gl: GL10?) {
        nextMesh?.let { m ->
            nextMesh = null
            mesh = m
            buffer = ByteBuffer.allocateDirect(m.data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(m.data); position(0) }
        }
        val ghostOn = ghost && !inside
        val proj = FloatArray(16); val viewM = FloatArray(16); val mvp = FloatArray(16)
        val eye: FloatArray
        if (inside) {
            Matrix.perspectiveM(proj, 0, fov, aspect, 0.05f, 300f)
            val y = Math.toRadians(lookYaw.toDouble()); val p = Math.toRadians(lookPitch.toDouble())
            val eh = base + 1.6f
            eye = floatArrayOf(eyeX, eh, eyeZ)
            Matrix.setLookAtM(viewM, 0, eyeX, eh, eyeZ,
                eyeX - (sin(y) * cos(p)).toFloat(), eh + sin(p).toFloat(), eyeZ - (cos(y) * cos(p)).toFloat(), 0f, 1f, 0f)
        } else {
            Matrix.perspectiveM(proj, 0, 45f, aspect, 0.2f, 600f)
            val y = Math.toRadians(yaw.toDouble()); val p = Math.toRadians(pitch.toDouble())
            val ex = tx + dist * (cos(p) * sin(y)).toFloat()
            val ey = base + dist * sin(p).toFloat()
            val ez = tz + dist * (cos(p) * cos(y)).toFloat()
            eye = floatArrayOf(ex, ey, ez)
            Matrix.setLookAtM(viewM, 0, ex, ey, ez, tx, base, tz, 0f, 1f, 0f)
        }
        Matrix.multiplyMM(mvp, 0, proj, 0, viewM, 0)
        lastMvp = mvp
        if (photosChanged) loadPhotos()
        // with photos round the plot the ground is theirs: the plain ground under them grey, not grass
        run {
            val c = if (photoTex.any { it != 0 }) 0x8F8D88 else 0x9DB58E
            for (k in 0 until mesh.groundCount) {
                val o = k * Mesh.STRIDE + 6
                buffer.put(o, (c shr 16 and 255) / 255f); buffer.put(o + 1, (c shr 8 and 255) / 255f); buffer.put(o + 2, (c and 255) / 255f)
            }
        }
        val count = if (inside) mesh.vertexCount else mesh.openCount

        // 1. Shadow map from the sun.
        val sd = sunDir()
        val cx = (mesh.minX + mesh.maxX) / 2; val cz = (mesh.minZ + mesh.maxZ) / 2
        val r = max(mesh.maxX - mesh.minX, mesh.maxZ - mesh.minZ) / 2 + 3f
        val lp = FloatArray(16); val lv = FloatArray(16); val light = FloatArray(16)
        Matrix.orthoM(lp, 0, -r, r, -r, r, 0.1f, 4 * r + 20f)
        Matrix.setLookAtM(lv, 0, cx + sd[0] * (2 * r + 8), sd[1] * (2 * r + 8), cz + sd[2] * (2 * r + 8), cx, 0f, cz, 0f, 1f, 0f)
        Matrix.multiplyMM(light, 0, lp, 0, lv, 0)
        if (shadows) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadowFbo)
            GLES20.glViewport(0, 0, SHADOW_SIZE, SHADOW_SIZE)
            GLES20.glClearColor(1f, 1f, 1f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            GLES20.glUseProgram(shadowProgram)
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(shadowProgram, "uMvp"), 1, false, light, 0)
            cutUniforms(shadowProgram, eye)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(shadowProgram, "uGhost"), if (ghostOn) 1f else 0f)
            attribs()
            // The ground (first 6 vertices) casts nothing.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 6, count - 6)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glViewport(0, 0, width, height)
        }

        // 2. The scene.
        val day = sd[1].coerceIn(0f, 1f)
        GLES20.glClearColor(0.62f + 0.18f * day, 0.74f + 0.14f * day, 0.86f + 0.09f * day, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uMvp"), 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uLight"), 1, false, light, 0)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uSun"), sd[0], sd[1], sd[2])
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uSunStrength"), 0.15f + 0.42f * kotlin.math.sqrt(day))
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uAmbient"), if (inside) (if (lightsOn) 0.5f else 0.42f) else 0.6f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uShadows"), if (shadows) 1f else 0f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uGlow"), if (inside) 1f else 0f)
        val ln = lens
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uLens"), ln?.get(0) ?: 0f, ln?.get(1) ?: 0f, ln?.get(2) ?: 0f)
        cutUniforms(program, eye)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadowTex)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uShadowMap"), 0)
        // Lamps, nearest to the eye first; only inside, and only while they are on.
        val lamps = if (inside) mesh.lights.sortedBy { (it.x - eye[0]) * (it.x - eye[0]) + (it.z - eye[2]) * (it.z - eye[2]) }.take(MAX_LAMPS) else emptyList()
        val lpos = FloatArray(MAX_LAMPS * 4)
        val levels = FloatArray(MAX_LAMPS)
        val ids = FloatArray(MAX_LAMPS)
        lamps.forEachIndexed { i, l ->
            val target = if (lightsOn && (l.switchId == null || switches[l.switchId] != false)) 1f else 0f
            val key = l.switchId ?: -l.hashCode()
            val now = (switchLevels[key] ?: 1f) + (target - (switchLevels[key] ?: 1f)) * 0.1f
            switchLevels[key] = now
            lpos[i * 4] = l.x; lpos[i * 4 + 1] = l.y; lpos[i * 4 + 2] = l.z; lpos[i * 4 + 3] = (l.room + 1) + l.watts.coerceIn(1, 999) * 10000f
            levels[i] = now
            ids[i] = l.id.toFloat()
        }
        GLES20.glUniform4fv(GLES20.glGetUniformLocation(program, "uLamps"), MAX_LAMPS, lpos, 0)
        GLES20.glUniform1fv(GLES20.glGetUniformLocation(program, "uLampLevels"), MAX_LAMPS, levels, 0)
        GLES20.glUniform1fv(GLES20.glGetUniformLocation(program, "uLampIds"), MAX_LAMPS, ids, 0)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uLampCount"), lamps.size)
        attribs()
        val ghostLoc = GLES20.glGetUniformLocation(program, "uGhost")
        GLES20.glUniform1f(ghostLoc, if (ghostOn) 1f else 0f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
        if (ghostOn) {
            // the outer walls last, see-through, over what is inside
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glDepthMask(false)
            GLES20.glUniform1f(ghostLoc, 2f)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
            GLES20.glDepthMask(true)
            GLES20.glDisable(GLES20.GL_BLEND)
        }
        if (!inside) drawPhotos(mvp, eye)
    }

    private companion object {
        const val PHOTO_VS = """
            uniform mat4 uMvp; attribute vec3 aPos; attribute vec2 aUv; attribute float aA; varying vec2 vUv; varying float vA;
            void main() { gl_Position = uMvp * vec4(aPos, 1.0); vUv = aUv; vA = aA; }"""
        const val PHOTO_FS = """
            precision mediump float; uniform sampler2D uTex; varying vec2 vUv; varying float vA;
            void main() { gl_FragColor = vec4(texture2D(uTex, vUv).rgb, vA); }"""
        const val SHADOW_SIZE = 2048
        const val MAX_LAMPS = 12

        /** Lowers a wall vertex when its wall stands between the camera and the middle of the plan. */
        const val CUT = """
            uniform vec4 uCut; uniform vec2 uCutOn;
            attribute vec3 aPos; attribute vec3 aNormal; attribute vec3 aColor; attribute vec4 aExtra;
            // aExtra.z is the kind plus ten times the storey's base height in decimetres.
            float kindOf() { return aExtra.z - floor(aExtra.z / 10.0 + 0.001) * 10.0; }
            vec3 cutPos() {
                vec3 p = aPos;
                float k = kindOf();
                // every storey shown is cut, each to a metre over its own floor
                if (uCutOn.x > 0.5 && (abs(k - 1.0) < 0.5 || abs(k - 4.0) < 0.5)) {
                    float d = dot(aExtra.xy - uCut.xy, uCut.zw);
                    if (d > 0.3) p.y = min(p.y, floor(aExtra.z / 10.0 + 0.001) / 10.0 + 1.0);
                }
                return p;
            }"""

        const val SHADOW_VS = CUT + """
            uniform mat4 uMvp;
            varying float vDepth; varying float vKind;
            void main() { gl_Position = uMvp * vec4(cutPos(), 1.0); vDepth = gl_Position.z / gl_Position.w * 0.5 + 0.5; vKind = kindOf(); }"""
        const val SHADOW_FS = """
            precision highp float;
            varying float vDepth; varying float vKind;
            uniform float uGhost;
            void main() {
                if (uGhost > 0.5 && (abs(vKind - 4.0) < 0.5 || abs(vKind - 3.0) < 0.5)) discard;  // faint walls cast no shadow
                vec4 e = fract(vec4(1.0, 255.0, 65025.0, 16581375.0) * vDepth);
                e -= e.yzww * vec4(1.0 / 255.0, 1.0 / 255.0, 1.0 / 255.0, 0.0);
                gl_FragColor = e;
            }"""

        const val VS = CUT + """
            uniform mat4 uMvp; uniform mat4 uLight;
            varying vec3 vColor; varying vec3 vNormal; varying vec3 vPos; varying vec4 vShadow; varying float vRoom; varying float vKind; varying float vTag;
            void main() {
                vec3 p = cutPos();
                vColor = aColor; vNormal = aNormal; vPos = p; vRoom = aExtra.w; vKind = kindOf(); vTag = aExtra.x;
                vShadow = uLight * vec4(p + aNormal * 0.02, 1.0);
                gl_Position = uMvp * vec4(p, 1.0);
            }"""
        const val FS = """
            #ifdef GL_FRAGMENT_PRECISION_HIGH
            precision highp float;
            #else
            precision mediump float;
            #endif
            uniform vec3 uSun; uniform float uSunStrength; uniform float uAmbient; uniform float uShadows; uniform float uGlow;
            uniform vec3 uLens; uniform float uGhost;
            uniform sampler2D uShadowMap;
            uniform vec4 uLamps[12]; uniform float uLampLevels[12]; uniform float uLampIds[12]; uniform int uLampCount;
            varying vec3 vColor; varying vec3 vNormal; varying vec3 vPos; varying vec4 vShadow; varying float vRoom; varying float vKind; varying float vTag;
            float unpack(vec4 c) { return dot(c, vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0)); }
            float lit() {
                if (uShadows < 0.5) return 1.0;
                vec3 s = vShadow.xyz / vShadow.w * 0.5 + 0.5;
                if (s.x < 0.0 || s.x > 1.0 || s.y < 0.0 || s.y > 1.0) return 1.0;
                float sum = 0.0;
                for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
                    float d = unpack(texture2D(uShadowMap, s.xy + vec2(float(i), float(j)) / 2048.0));
                    sum += s.z - 0.0015 > d ? 0.0 : 1.0;
                }
                return sum / 9.0;
            }
            void main() {
                // faint outer walls: left out of the first pass, drawn alone in the second
                // (from outside, the roofs are faint too: the ceilings are not drawn there)
                bool outer = abs(vKind - 4.0) < 0.5 || abs(vKind - 3.0) < 0.5;
                if (uGhost > 0.5 && uGhost < 1.5 && outer) discard;
                if (uGhost > 1.5 && !outer) discard;
                // the X-ray lens: walls (1), roofs and ceilings (3) are not drawn inside it
                if (uLens.z > 0.0 && (abs(vKind - 1.0) < 0.5 || abs(vKind - 3.0) < 0.5 || abs(vKind - 4.0) < 0.5) && distance(gl_FragCoord.xy, uLens.xy) < uLens.z) discard;
                vec3 n = normalize(vNormal);
                float sun = max(dot(n, normalize(uSun)), 0.0) * uSunStrength * lit();
                float sky = uAmbient * (0.8 + 0.2 * n.y);
                vec3 light = vec3(sky) + vec3(1.0, 0.97, 0.9) * sun;
                for (int i = 0; i < 12; i++) {
                    if (i >= uLampCount) break;
                    vec4 l = uLamps[i];
                    float w = floor(l.w / 10000.0);
                    if (abs(l.w - w * 10000.0 - (vRoom + 1.0)) > 0.5) continue;  // a lamp lights its own room only
                    vec3 d = l.xyz - vPos;
                    float k = 1.0 / (1.0 + 0.35 * dot(d, d));
                    light += vec3(1.0, 0.85, 0.65) * uLampLevels[i] * (w / 12.0) * k * (0.35 + 0.65 * max(dot(n, normalize(d)), 0.0));
                }
                if (abs(vKind - 2.0) < 0.5 && uGlow > 0.5) {
                    float glow = 0.0;
                    for (int i = 0; i < 12; i++) { if (i >= uLampCount) break; if (abs(vTag - uLampIds[i]) < 0.5) glow = uLampLevels[i]; }
                    gl_FragColor = vec4(mix(vColor * light, vec3(1.0, 0.95, 0.8), glow), 1.0); return;
                }
                gl_FragColor = vec4(min(vColor * light, vec3(1.0)), uGhost > 1.5 ? 0.1 : 1.0);
            }"""
    }
}

/** Photos of the plot's four sides, kept with the plan: what is seen looking north, east, south and west. */
object SidePhotos {
    fun file(context: Context, planId: String, side: Int) = java.io.File(java.io.File(context.filesDir, "photos").apply { mkdirs() }, "$planId-$side.jpg")
    /** The photo of [side], made small enough for the 3D view; null when there is none. */
    fun bitmap(context: Context, planId: String, side: Int): android.graphics.Bitmap? {
        val f = file(context, planId, side)
        if (!f.isFile) return null
        val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(f.absolutePath, o)
        var k = 1; while (maxOf(o.outWidth, o.outHeight) / k > 1024) k *= 2
        return android.graphics.BitmapFactory.decodeFile(f.absolutePath, android.graphics.BitmapFactory.Options().apply { inSampleSize = k })
    }
}

@Composable
private fun SidePhotosDialog(planId: String, onChanged: () -> Unit, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var side by remember { mutableStateOf(0) }
    var tick by remember { mutableStateOf(0) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)!!.use { input -> SidePhotos.file(context, planId, side).outputStream().use { input.copyTo(it) } }
            tick++; onChanged()
        }
    }
    val names = listOf(R.string.north, R.string.east, R.string.south, R.string.west)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📷 " + stringResource(R.string.side_photos)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.side_photos_about), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (i in 0 until 4) {
                    val bm = remember(tick, i) { SidePhotos.bitmap(context, planId, i) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(listOf("N", "E", "S", "W")[i] + " · " + stringResource(names[i]), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        if (bm != null) androidx.compose.foundation.Image(bm.asImageBitmap(), null, Modifier.size(64.dp, 40.dp))
                        TextButton(onClick = { side = i; picker.launch(arrayOf("image/*")) }) { Text(stringResource(if (bm != null) R.string.replace_model else R.string.add_photo)) }
                        if (bm != null) IconButton(onClick = { SidePhotos.file(context, planId, i).delete(); tick++; onChanged() }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}
