package com.dawoyan.roomik.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dawoyan.roomik.AppViewModel
import com.dawoyan.roomik.R
import com.dawoyan.roomik.core.Materials
import com.dawoyan.roomik.core.Boards
import com.dawoyan.roomik.core.allFloors

/** What the editor and the materials list can send out: the plan as PDF, and the rooms to elektronik. */
class Exports(val pdf: () -> Unit, val sendToElektronik: () -> Unit, val saveForElektronik: () -> Unit, val obj: () -> Unit,
    /** Complete editable plan data, formatted for attaching to an AI design/optimisation request. */
    val aiPlan: () -> Unit,
    /** One board with all its loads, to a project chosen in elektronik (the same board there is replaced). */
    val sendBoard: (com.dawoyan.roomik.core.Boards.Ref) -> Unit = {})

private fun fileName(name: String) = name.replace(Regex("[^\\p{L}\\p{N} _-]"), "_").trim().ifEmpty { "plan" }

@Composable
fun rememberExports(vm: AppViewModel): Exports {
    val context = LocalContext.current
    val ru = language() == "ru"
    var boardToExport by remember { mutableStateOf<Boards.Ref?>(null) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val plan = vm.root ?: return@rememberLauncherForActivityResult
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { PlanPdf.write(context, plan, it) }
            Toast.makeText(context, R.string.pdf_saved, Toast.LENGTH_SHORT).show()
        }.onFailure { Toast.makeText(context, it.message ?: "PDF", Toast.LENGTH_LONG).show() }
    }
    val objLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("model/obj")) { uri ->
        val plan = vm.root ?: return@rememberLauncherForActivityResult
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { it.write(com.dawoyan.roomik.core.ObjExport.write(plan).toByteArray()) }
            Toast.makeText(context, R.string.saved_3d, Toast.LENGTH_LONG).show()
        }.onFailure { Toast.makeText(context, it.message ?: "OBJ", Toast.LENGTH_LONG).show() }
    }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val plan = vm.root ?: return@rememberLauncherForActivityResult
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { it.write(Materials.elektronikFile(plan, ru).toByteArray()) }
            Toast.makeText(context, R.string.saved, Toast.LENGTH_LONG).show()
        }
    }
    val aiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val plan = vm.root ?: return@rememberLauncherForActivityResult
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { it.write(com.dawoyan.roomik.core.PlanJson.encode(plan).toByteArray()) }
            Toast.makeText(context, R.string.ai_exported, Toast.LENGTH_LONG).show()
        }.onFailure { Toast.makeText(context, it.message ?: "JSON", Toast.LENGTH_LONG).show() }
    }
    fun sendExactBoard(requested: Boards.Ref) {
        vm.root?.let { plan ->
            // Re-find by the tapped point's floor and id. A board's `source` only says how it is fed and never
            // changes which board this action exports.
            val b = Boards.all(plan).firstOrNull { it.floor == requested.floor && it.point.id == requested.point.id } ?: requested
            val file = Materials.elektronikBoardFile(plan, b, ru)
            val sub = Boards.only(plan, b, requireEvidence = true)
            val points = sub.allFloors.flatMap { floor -> floor.points.filter { it.kind != com.dawoyan.roomik.core.PointKind.BOARD && it.kind != com.dawoyan.roomik.core.PointKind.RISER && it.kind != com.dawoyan.roomik.core.PointKind.JUNCTION } }
            val byKind = points.groupBy { it.kind }.entries.joinToString { (k, ps) -> "$k=${ps.map { it.id }}" }
            val sent = com.dawoyan.elektronik.core.Roomik.singleBoard(file)
            val loads = sent?.rooms.orEmpty().flatMap { it.loads }.groupBy { it.kind }.entries.joinToString { (k, ls) -> "$k=${ls.sumOf { it.count }}" }
            DebugLog.add("elektronik export board=${Boards.name(b, ru)} key=${b.key}; points: $byKind; loads: $loads")
            if (!send(context, file)) Toast.makeText(context, R.string.no_elektronik, Toast.LENGTH_LONG).show()
        }
    }
    boardToExport?.let { requested ->
        val plan = vm.root
        val b = plan?.let { root -> Boards.all(root).firstOrNull { it.floor == requested.floor && it.point.id == requested.point.id } } ?: requested
        val outgoing = plan?.let { com.dawoyan.elektronik.core.Roomik.singleBoard(Materials.elektronikBoardFile(it, b, ru)) }
        AlertDialog(
            onDismissRequest = { boardToExport = null },
            title = { Text("Export board: " + Boards.name(b, ru)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Only this selected board will be sent. Its feeder is not exported.")
                    val rooms = outgoing?.rooms.orEmpty()
                    if (rooms.none { it.loads.isNotEmpty() }) Text("No loads on this board.", Modifier.padding(top = 8.dp))
                    for (room in rooms.filter { it.loads.isNotEmpty() }) {
                        Text(room.name.ifBlank { room.zone.ifBlank { "Area" } }, Modifier.padding(top = 10.dp))
                        for (load in room.loads) {
                            val type = when (load.kind) {
                                com.dawoyan.elektronik.core.LoadKind.LIGHTS -> "Lights"
                                com.dawoyan.elektronik.core.LoadKind.SOCKETS -> "Sockets"
                                else -> "Load"
                            }
                            val name = load.name.takeIf { it.isNotBlank() }?.let { "$type · $it" } ?: type
                            Text("• $name ×${load.count}", Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { boardToExport = null; sendExactBoard(b) }) { Text("Export") } },
            dismissButton = { TextButton(onClick = { boardToExport = null }) { Text("Cancel") } },
        )
    }
    return Exports(
        pdf = { vm.root?.let { pdfLauncher.launch(fileName(it.name) + ".pdf") } },
        sendToElektronik = {
            vm.root?.let { plan ->
                // Anything goes: closed rooms, open places, or just sockets and lights along open walls.
                val any = plan.allFloors.any { f -> f.points.any { it.kind != com.dawoyan.roomik.core.PointKind.BOARD } || f.places.isNotEmpty() ||
                    com.dawoyan.roomik.core.Rooms.detect(f).isNotEmpty() }
                if (!any) {
                    Toast.makeText(context, R.string.nothing_to_send, Toast.LENGTH_LONG).show()
                    return@let
                }
                if (!send(context, Materials.elektronikFile(plan, ru))) {
                    Toast.makeText(context, R.string.no_elektronik, Toast.LENGTH_LONG).show()
                    jsonLauncher.launch(fileName(plan.name) + "-elektronik.json")
                }
            }
        },
        saveForElektronik = { vm.root?.let { jsonLauncher.launch(fileName(it.name) + "-elektronik.json") } },
        obj = { vm.root?.let { objLauncher.launch(fileName(it.name) + ".obj") } },
        aiPlan = { vm.root?.let { aiLauncher.launch(fileName(it.name) + "-ai-plan.json") } },
        sendBoard = { b -> boardToExport = b },
    )
}

/** Hands the rooms to elektronik, which asks where to attach them. False when elektronik is not installed. */
private fun send(context: Context, json: String): Boolean = try {
    context.startActivity(
        Intent("com.dawoyan.elektronik.ATTACH_ROOMS").setPackage("com.dawoyan.elektronik").putExtra("json", json)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    true
} catch (e: ActivityNotFoundException) {
    false
}
