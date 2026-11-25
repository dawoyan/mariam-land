package com.dawoyan.roomik

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.dawoyan.roomik.core.P
import com.dawoyan.roomik.core.Plan
import com.dawoyan.roomik.core.Rooms
import com.dawoyan.roomik.core.PlanJson
import com.dawoyan.roomik.ui.DebugLog
import kotlinx.coroutines.launch
import com.dawoyan.roomik.core.allFloors
import com.dawoyan.roomik.core.PlanOps
import com.dawoyan.roomik.core.floor
import com.dawoyan.roomik.core.withFloor
import com.dawoyan.roomik.core.floorRange
import java.io.File
import java.util.UUID

/** For [AppViewModel.under]: no floor drawn under. */
const val NO_FLOOR = Int.MIN_VALUE

enum class Screen { PROJECTS, EDITOR, VIEW3D, MATERIALS }

/** Plans live as one JSON file each in filesDir/plans; every change is saved at once. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dir = File(app.filesDir, "plans").apply { mkdirs() }

    var plans by mutableStateOf(load()); private set
    /** The whole plan with all its floors, as saved. */
    var root by mutableStateOf<Plan?>(null); private set
    /** The floor being edited: 0 is the ground floor. */
    var floor by mutableStateOf(0); private set
    /** The floor being edited; everything the editor changes goes here. */
    val plan: Plan? get() = root?.floor(floor)
    var screen by mutableStateOf(Screen.PROJECTS)
    var canUndo by mutableStateOf(false); private set
    var canRedo by mutableStateOf(false); private set
    /** Layers hidden on the plan and in 3D. */
    var hidden by mutableStateOf(setOf<com.dawoyan.roomik.core.Layer>())

    private val prefs = app.getSharedPreferences("roomik", 0)
    fun editorTool(): String? = prefs.getString("editorTool", null)
    fun editorPointKind(): String? = prefs.getString("editorPointKind", null)
    fun editorItemKind(): String? = prefs.getString("editorItemKind", null)
    fun saveEditorContext(tool: String, pointKind: String?, itemKind: String?) {
        prefs.edit().putString("editorTool", tool).putString("editorPointKind", pointKind).putString("editorItemKind", itemKind).apply()
    }
    /** Electric points and cables shown small in the other menus too (otherwise only in Electrics). */
    var alwaysElectric by mutableStateOf(prefs.getBoolean("alwaysElectric", false)); private set
    /** Pipes shown thin in the other menus too (otherwise only in Systems). */
    var alwaysPipes by mutableStateOf(prefs.getBoolean("alwaysPipes", false)); private set
    fun showElectricAlways(v: Boolean) { alwaysElectric = v; prefs.edit().putBoolean("alwaysElectric", v).apply() }
    fun showPipesAlways(v: Boolean) { alwaysPipes = v; prefs.edit().putBoolean("alwaysPipes", v).apply() }
    /** Kitchen, bath and other fittings (sink, stove, bath, toilet…) shown on the plan outside the Interior menu. */
    var showFittings by mutableStateOf(prefs.getBoolean("showFittings", true)); private set
    /** Movable furniture (beds, sofas, tables…) shown on the plan outside the Interior menu. */
    var showFurniture by mutableStateOf(prefs.getBoolean("showFurniture", true)); private set
    /** Walls drawn see-through, with only their corners solid. */
    var seeThrough by mutableStateOf(prefs.getBoolean("seeThrough", false)); private set
    fun showFittingsOn(v: Boolean) { showFittings = v; prefs.edit().putBoolean("showFittings", v).apply() }
    fun showFurnitureOn(v: Boolean) { showFurniture = v; prefs.edit().putBoolean("showFurniture", v).apply() }
    /** Wall lengths written beside the walls. */
    var showDims by mutableStateOf(prefs.getBoolean("showDims", true)); private set
    // labels on the plan's electrics and systems, each kind shown or hidden (kept)
    var labelCables by mutableStateOf(prefs.getBoolean("labelCables", true)); private set
    var labelLengths by mutableStateOf(prefs.getBoolean("labelLengths", true)); private set
    var labelPipes by mutableStateOf(prefs.getBoolean("labelPipes", true)); private set
    var labelPower by mutableStateOf(prefs.getBoolean("labelPower", true)); private set
    var labelNames by mutableStateOf(prefs.getBoolean("labelNames", true)); private set
    fun labelNamesOn(v: Boolean) { labelNames = v; prefs.edit().putBoolean("labelNames", v).apply() }
    fun labelCablesOn(v: Boolean) { labelCables = v; prefs.edit().putBoolean("labelCables", v).apply() }
    fun labelLengthsOn(v: Boolean) { labelLengths = v; prefs.edit().putBoolean("labelLengths", v).apply() }
    fun labelPipesOn(v: Boolean) { labelPipes = v; prefs.edit().putBoolean("labelPipes", v).apply() }
    fun labelPowerOn(v: Boolean) { labelPower = v; prefs.edit().putBoolean("labelPower", v).apply() }
    val anyLabels: Boolean get() = labelCables || labelLengths || labelPipes || labelPower || labelNames
    fun allLabels(v: Boolean) { labelCablesOn(v); labelLengthsOn(v); labelPipesOn(v); labelPowerOn(v); labelNamesOn(v) }
    /** A 3D model waiting to be put on the plan: its name there, and the file's name and bytes. */
    /** The settings last chosen for each kind of yard construction, offered again next time. */
    val lastYard = HashMap<com.dawoyan.roomik.core.YardKind, com.dawoyan.roomik.core.YardSpec>()
    var pendingModel by mutableStateOf<Pair<String, Pair<String, ByteArray>>?>(null)

    // The library of 3D assets: one JSON file each in filesDir/assets, shared by all plans.
    private val assetDir = File(app.filesDir, "assets").apply { mkdirs() }
    var assets by mutableStateOf(loadAssets()); private set
    /** A library model chosen to put on the plan: placed where the plan is tapped next. */
    var assetToPlace by mutableStateOf<com.dawoyan.roomik.core.Asset?>(null)

    private fun loadAssets(): List<com.dawoyan.roomik.core.Asset> {
        // the first time: every 3D model already on a plan goes into the library
        if (!prefs.getBoolean("assetsCollected", false)) {
            val seen = HashSet<String>()
            for (pl in plans) for (fl in pl.allFloors) for (m in fl.models) if (m.yard == null && m.parts.isNotEmpty() && seen.add(m.name))
                writeAsset(com.dawoyan.roomik.core.Asset(UUID.randomUUID().toString(), m.name, m.parts, pl.updatedAt))
            prefs.edit().putBoolean("assetsCollected", true).apply()
        }
        return (assetDir.listFiles { f -> f.name.endsWith(".json") } ?: emptyArray())
            .mapNotNull { runCatching { com.dawoyan.roomik.core.AssetJson.decode(it.readText()) }.getOrNull() }
            .sortedByDescending { it.updatedAt }
    }

    private fun writeAsset(a: com.dawoyan.roomik.core.Asset) {
        val tmp = File(assetDir, "${a.id}.json.tmp")
        tmp.writeText(com.dawoyan.roomik.core.AssetJson.encode(a))
        tmp.renameTo(File(assetDir, "${a.id}.json"))
    }

    /** Keeps [parts] in the library as [name]: one of that name already there is replaced (sent again from Idea 3D). */
    fun saveAsset(name: String, parts: List<com.dawoyan.roomik.core.ModelPart>): com.dawoyan.roomik.core.Asset {
        val old = assets.firstOrNull { it.name == name }
        val a = com.dawoyan.roomik.core.Asset(old?.id ?: UUID.randomUUID().toString(), name, parts, System.currentTimeMillis())
        writeAsset(a)
        assets = listOf(a) + assets.filter { it.id != a.id }
        return a
    }

    fun renameAsset(a: com.dawoyan.roomik.core.Asset, name: String) {
        val n = a.copy(name = name.trim().ifEmpty { a.name })
        writeAsset(n); assets = assets.map { if (it.id == n.id) n else it }
    }

    /**
     * A model sent here (from Idea 3D, or a file opened with Roomik). One coming back from an edit ("#rk…" in its name)
     * waits for the editor, to replace what it was made from; any other goes into the library.
     */
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
    override fun onCleared() { scope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }

    fun receiveModel(name: String, file: String, bytes: ByteArray) {
        if (name.contains(Regex("#rk-?\\d+[mi]\\d+"))) { pendingModel = name to (file to bytes); return }
        val ctx = getApplication<Application>()
        scope.launch {
            val parts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { runCatching { com.dawoyan.roomik.core.Models.read(file, bytes) }.getOrNull() }
            if (parts == null) { android.widget.Toast.makeText(ctx, com.dawoyan.roomik.R.string.model_bad, android.widget.Toast.LENGTH_LONG).show(); return@launch }
            saveAsset(name, parts)
            android.widget.Toast.makeText(ctx, ctx.getString(com.dawoyan.roomik.R.string.asset_added, name), android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun deleteAsset(a: com.dawoyan.roomik.core.Asset) {
        File(assetDir, "${a.id}.json").delete()
        assets = assets.filter { it.id != a.id }
    }
    var showPillarGrid by mutableStateOf(prefs.getBoolean("pillarGrid", true)); private set
    fun showPillarGridOn(v: Boolean) { showPillarGrid = v; prefs.edit().putBoolean("pillarGrid", v).apply() }
    fun showDimsOn(v: Boolean) { showDims = v; prefs.edit().putBoolean("showDims", v).apply() }
    fun showSeeThrough(v: Boolean) { seeThrough = v; prefs.edit().putBoolean("seeThrough", v).apply() }
    /** The floor whose walls are drawn faintly under the one edited: null = the next one towards the ground, [NO_FLOOR] = none. */
    var under by mutableStateOf<Int?>(null)

    /** A plan point (mm) the 3D view should start inside, set by "View inside" or a link from elektronik. */
    var insideAt: P? = null
    /** A room the editor should select and show, by its lasting id; set by a link from elektronik. */
    var focusRoom by mutableStateOf<String?>(null)

    // Undo keeps whole plans with the floor that was being edited.
    private val undo = ArrayDeque<Pair<Plan, Int>>()
    private val redo = ArrayDeque<Pair<Plan, Int>>()

    private var restored = false
    fun restoreLastProject() {
        if (restored) return
        restored = true
        // Restore only after construction has finished; keep saved files intact if restoration fails.
        prefs.getString("lastProject", null)?.let { id -> plans.firstOrNull { it.id == id } }?.let { project ->
            runCatching { open(project) }.onFailure { android.util.Log.e("MariamLand", "Saved house could not be opened", it) }
        }
    }

    private fun load(): List<Plan> = (dir.listFiles { f -> f.name.endsWith(".json") } ?: emptyArray())
        .mapNotNull { runCatching { PlanJson.decode(it.readText()) }.getOrNull() }
        .sortedByDescending { it.updatedAt }

    private fun save(p: Plan) {
        val tmp = File(dir, "${p.id}.json.tmp")
        tmp.writeText(PlanJson.encode(p))
        tmp.renameTo(File(dir, "${p.id}.json"))
    }

    private fun stamp(p: Plan) = p.copy(updatedAt = System.currentTimeMillis())
    private fun history() { canUndo = undo.isNotEmpty(); canRedo = redo.isNotEmpty() }

    fun create(name: String) {
        val p = stamp(Plan(UUID.randomUUID().toString(), name))
        save(p); open(p); screen = Screen.EDITOR
    }

    fun open(p: Plan) {
        undo.clear(); redo.clear(); history()
        // Grass made before Backyard became a site-only tool could be attached to a storey and appear below it.
        // Remove that one-time bad format; all new Backyard platforms live in the site's plan.
        fun clearLegacyGrass(q: Plan): Plan = q.copy(
            roads = q.roads.filterNot { it.surface == com.dawoyan.roomik.core.Surface.GRASS },
            floors = q.floors.map(::clearLegacyGrass), basements = q.basements.map(::clearLegacyGrass),
            site = q.site?.let(::clearLegacyGrass), schemaVersion = 3,
        )
        // Version 3 repeats the cleanup for plans already opened by the transitional Backyard builds.
        val source = if (p.schemaVersion < 3) clearLegacyGrass(p) else p
        // Plans from older versions get lasting room ids here, which elektronik knows the rooms by.
        fun fix(f0: Plan): Plan {
            val f = com.dawoyan.roomik.core.Stairs.migrate(f0)
            return if (f.rooms.size != Rooms.detect(f).size || f.rooms.any { it.id.isEmpty() }) Rooms.rematch(f) else f
        }
        // boards sharing a lasting id get their own, so each can be picked as another's source
        val fixed = com.dawoyan.roomik.core.Boards.fixUids(fix(source).copy(floors = source.floors.map(::fix), basements = source.basements.map(::fix), site = source.site?.let(::fix)))
        if (fixed != p) save(fixed)
        root = fixed
        floor = keepFloor(fixed, prefs.getInt("lastFloor:${fixed.id}", 0))
        prefs.edit().putString("lastProject", fixed.id).putInt("lastFloor:${fixed.id}", floor).apply()
        screen = if (fixed.allFloors.any { it.walls.isNotEmpty() }) Screen.VIEW3D else Screen.EDITOR
    }

    fun selectFloor(i: Int) {
        floor = if (i == com.dawoyan.roomik.core.SITE) i else root?.floorRange?.let { i.coerceIn(it.first, it.last) } ?: 0
        root?.let { prefs.edit().putInt("lastFloor:${it.id}", floor).apply() }
        val electrical = plan?.points?.groupingBy { it.kind }?.eachCount()?.entries?.joinToString { "${it.key}=${it.value}" } ?: "none"
        DebugLog.add("selected floor=$floor, roads=" + (plan?.roads?.joinToString { "${it.surface}/${it.closed}/${it.points.size}" } ?: "none") + ", electric=$electrical")
    }

    fun addFloor() {
        val r = root ?: return
        commitRoot(PlanOps.addFloor(r))
        floor = r.floors.size + 1
    }

    fun addBasement() {
        val r = root ?: return
        commitRoot(PlanOps.addBasement(r))
        floor = -(r.basements.size + 1)
    }

    fun deleteFloor() {
        val r = root ?: return
        if (floor == 0) return
        commitRoot(PlanOps.deleteFloor(r, floor))
        floor += if (floor > 0) -1 else 1
    }

    /** How high the ground floor is above the ground outside, mm. */
    fun setGround(mm: Int) { root?.let { commitRoot(it.copy(groundLevel = mm.coerceIn(0, 5000))) } }

    /** A riser at [w] on every floor. */
    fun addRiser(w: P) { root?.let { commitRoot(PlanOps.addRiser(it, w, 1500.0)) } }

    /** Replaces the whole house (changes on several floors at once), with undo. */
    fun commitHouse(after: Plan) = commitRoot(after)

    /** The whole house shown changed, without saving or history (the middle of a drag across floors). */
    fun previewHouse(r: Plan) { root = r }

    /** The whole house replaced; [before] is what undo returns to. */
    fun commitHouse(after: Plan, before: Plan) {
        undo.addLast(before to floor); if (undo.size > 100) undo.removeFirst(); redo.clear()
        val p = stamp(after); root = p; save(p); history()
    }

    private fun commitRoot(after: Plan) {
        val r = root ?: return
        undo.addLast(r to floor); if (undo.size > 100) undo.removeFirst(); redo.clear()
        val p = stamp(after)
        root = p; save(p); history()
    }

    fun rename(p: Plan, name: String) { save(stamp(p.copy(name = name))); plans = load() }
    fun duplicate(p: Plan, name: String) { save(stamp(p.copy(id = UUID.randomUUID().toString(), name = name))); plans = load() }
    fun delete(p: Plan) { File(dir, "${p.id}.json").delete(); plans = load() }

    /** Opens what a roomik://open?plan=…&room=… link points at: the plan, on the room's floor, with the room selected. */
    fun openLink(planId: String?, roomId: String?) {
        val p = load().firstOrNull { it.id == planId } ?: return
        plans = load()
        open(p)
        screen = Screen.EDITOR
        val m = roomId?.let { Regex("^([fb])(\\d+)-(.+)$").find(it) }
        if (m != null) {
            val n = m.groupValues[2].toInt()
            selectFloor(if (m.groupValues[1] == "f") n else -n); focusRoom = m.groupValues[3]
        } else focusRoom = roomId
    }

    fun back() {
        when (screen) {
            Screen.VIEW3D, Screen.MATERIALS -> screen = Screen.EDITOR
            Screen.EDITOR -> { plans = load(); root = null; screen = Screen.PROJECTS }
            Screen.PROJECTS -> {}
        }
    }

    /** Shows [p] as the floor being edited, without saving or history: the state in the middle of a drag. */
    fun preview(p: Plan) { root = root?.withFloor(floor, p) }

    /** Replaces the floor being edited; [before] is what undo returns to (the floor as it was before the drag began). */
    fun commit(after0: Plan, before: Plan? = plan, tidy: Boolean = true) {
        val r = root ?: return
        // points left in line between their neighbours (and starting nothing else) are taken out
        val after = if (tidy) com.dawoyan.roomik.core.Tidy.straighten(after0) else after0
        if (before != null && before != after) {
            undo.addLast(r.withFloor(floor, before) to floor)
            if (undo.size > 100) undo.removeFirst()
            redo.clear()
        }
        val p = stamp(r.withFloor(floor, after))
        root = p; save(p); history()
    }

    fun undo() {
        val (p, f) = undo.removeLastOrNull() ?: return
        root?.let { redo.addLast(it to floor) }
        root = stamp(p); floor = keepFloor(p, f); save(root!!); history()
    }

    /** [f] when the house [p] still has it (the ground round it too), else the nearest floor it has. */
    private fun keepFloor(p: Plan, f: Int): Int = if (f == com.dawoyan.roomik.core.SITE) f else f.coerceIn(p.floorRange.first, p.floorRange.last)

    fun redo() {
        val (p, f) = redo.removeLastOrNull() ?: return
        root?.let { undo.addLast(it to floor) }
        root = stamp(p); floor = keepFloor(p, f); save(root!!); history()
    }
}
