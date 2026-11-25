package com.dawoyan.roomik.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawoyan.elektronik.core.Cable
import com.dawoyan.roomik.AppViewModel
import com.dawoyan.roomik.R
import com.dawoyan.roomik.core.BomKey
import com.dawoyan.roomik.core.Materials
import com.dawoyan.roomik.core.P

/** One line of the list: what and how much. */
private class Line(val name: String, val qty: String, val note: String = "")


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(vm: AppViewModel) {
    val plan = vm.root ?: return
    val context = LocalContext.current
    val ru = language() == "ru"
    val bom = remember(plan, ru) { Materials.build(plan, ru) }
    val m = stringResource(R.string.m)
    val pcs = stringResource(R.string.pcs)
    val drawn = stringResource(R.string.drawn_note)
    val exports = rememberExports(vm)

    val cable = bom.cables.map { Line(stringResource(R.string.cable_name, it.cores.toString(), size(it.mm2)), "${it.meters} $m", stringResource(R.string.cable_note, num(it.net, 1))) }
    val lines = bom.lines.map {
        Line(it.name, if (it.meters > 0) num(it.meters, 1) + " $m" else "", it.rating + " · ${it.cores}×${size(it.mm2)}" + if (it.drawn) " · " + drawn else "")
    }
    val parts = bom.items.map { Line(bomName(context, it.key, it.detail), "${it.qty} " + if (it.key == BomKey.CONDUIT) m else pcs) }
    // House systems: lengths with 10 % to spare, radiators, warm floor area.
    val systems = buildList {
        for (k in com.dawoyan.roomik.core.SystemKind.entries) {
            val mm = plan.allFloorsList().sumOf { f -> f.pipes.filter { it.system == k }.sumOf { pipeLength(it) } }
            if (mm > 0) add(Line(systemName(k), "${kotlin.math.ceil(mm / 1000 * 1.1).toInt()} $m", stringResource(R.string.sys_note, num(mm / 1000, 1))))
        }
        val rad = plan.allFloorsList().sumOf { f -> f.items.count { it.kind == "radiator" } }
        if (rad > 0) add(Line(stringResource(R.string.f_radiator), "$rad $pcs"))
        val warm = plan.allFloorsList().sumOf { f -> com.dawoyan.roomik.core.Rooms.detect(f).filter { it.info?.warmFloor == true }.sumOf { it.area } }
        if (warm > 0) add(Line(stringResource(R.string.warm_floor), num(warm, 1) + " " + stringResource(R.string.m2)))
        for (f in plan.allFloorsList()) for (rf in f.roofs)
            add(Line(stringResource(R.string.roof) + " · " + roofName(rf.kind) + " ${rf.pitch}° · " + roofCoverName(rf.cover), num(com.dawoyan.roomik.core.Roofs.area(rf) * 1.1, 1) + " " + stringResource(R.string.m2),
                stringResource(R.string.roof_note) + if (rf.cover.sheetW > 0) " · ≈ " + kotlin.math.ceil(com.dawoyan.roomik.core.Roofs.area(rf) * 1.1 / (rf.cover.sheetW * rf.cover.sheetL / 1e6)).toInt() + " × " + rf.cover.sheetW / 10 + "×" + rf.cover.sheetL / 10 + " " + stringResource(R.string.cm) else ""))
        for (f in plan.allFloorsList()) for (pf in f.platforms) {
            val rail = if (pf.railing) com.dawoyan.roomik.core.Platforms.openSides(f, pf).sumOf { (a, b) -> (b - a).len } else 0.0
            add(Line(stringResource(R.string.platform) + " ↥ " + cm(pf.height.toDouble()), num(com.dawoyan.roomik.core.Platforms.area(pf), 1) + " " + stringResource(R.string.m2),
                if (rail > 0) stringResource(R.string.platform_note, num(rail / 1000, 1)) else ""))
        }
        for (k in com.dawoyan.roomik.core.FenceKind.entries) {
            val fs = plan.allFloorsList().flatMap { it.fences }.filter { it.kind == k }
            if (fs.isEmpty()) continue
            val mm = fs.sumOf { com.dawoyan.roomik.core.Fences.length(it) }
            val posts = fs.sumOf { f -> (1 until f.points.size).sumOf { kk -> maxOf(1, Math.ceil(P((f.points[kk].x - f.points[kk - 1].x).toDouble(), (f.points[kk].y - f.points[kk - 1].y).toDouble()).len / com.dawoyan.roomik.core.Fences.POST_STEP).toInt()) } + 1 }
            add(Line(stringResource(R.string.fence) + " · " + fenceName(k), num(mm / 1000, 1) + " $m", stringResource(R.string.fence_note, posts)))
        }
    }
    val gear = bom.board.map { Line((it.title + " " + it.rating).trim(), "${it.qty} $pcs") }
    // electrics are bought by board in elektronik (cable, fittings and board devices); here only the house
    // 3D models (a shed, a canopy…): each part with its top seen from above (a roof sheet) and its size
    val models = buildList {
        // yard constructions made here: what they take (bars by profile in metres, covering in m² and sheets)
        for (f in plan.allFloorsList()) for (md in f.models) md.yard?.let { spec ->
            for (l in com.dawoyan.roomik.core.Yard.bill(spec, ru)) add(Line(md.name + " · " + l.name, num(l.qty, 1) + " " + l.unit, l.note))
        }
        for (f in plan.allFloorsList()) for (md in f.models.filter { it.yard == null }) for (part in md.parts) {
            val ps = com.dawoyan.roomik.core.Models.measure(part)
            add(Line(md.name + " · " + part.name, num(ps.top / 1e6, 2) + " " + stringResource(R.string.m2),
                cm(ps.w) + "×" + cm(ps.d) + "×" + cm(ps.h) + " " + stringResource(R.string.cm) + " · Σ " + num(ps.surface / 1e6, 2) + " " + stringResource(R.string.m2)))
        }
    }
    // stone on the walls' outsides and stone fences: m² of each, and about how many blocks
    val lang = language()
    val facades = remember(plan) { com.dawoyan.roomik.core.Facings.areas(plan) }.filterValues { it > 0.01 }.map { (f, m2) ->
        val n = com.dawoyan.roomik.core.Facings.blocks(f, m2)
        Line(com.dawoyan.roomik.core.Facings.name(f, lang), num(m2 * 1.1, 1) + " " + stringResource(R.string.m2),
            if (n > 0) stringResource(R.string.facade_note, num(m2, 1), n) + " · " + f.stone.blockW / 10 + "×" + f.stone.blockH / 10 + " " + stringResource(R.string.cm) else "")
    }
    // roads and paths: m² of each surface
    val roads = plan.allFloorsList().flatMap { it.roads }.groupBy { it.surface }.map { (k, rs) ->
        val len = rs.sumOf { r -> (1 until r.points.size).sumOf { i -> P((r.points[i].x - r.points[i - 1].x).toDouble(), (r.points[i].y - r.points[i - 1].y).toDouble()).len } }
        val m2 = rs.sumOf { r -> (1 until r.points.size).sumOf { i -> P((r.points[i].x - r.points[i - 1].x).toDouble(), (r.points[i].y - r.points[i - 1].y).toDouble()).len } * r.width } / 1e6
        Line(surfaceName(k), num(m2, 1) + " " + stringResource(R.string.m2), num(len / 1000, 1) + " " + stringResource(R.string.m))
    }
    val sections = listOf(R.string.facades to facades, R.string.roads_sec to roads, R.string.tool_systems to systems, R.string.models_sec to models)
        .map { stringResource(it.first) to it.second }.filter { it.second.isNotEmpty() }
    val estimate = stringResource(R.string.estimate)
    val title = stringResource(R.string.materials)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    IconButton(onClick = {
                        val text = buildString {
                            append(plan.name).append(" — ").append(title).append('\n')
                            for ((head, list) in sections) {
                                append('\n').append(head.uppercase()).append('\n')
                                for (l in list) append("• ").append(l.name).append(if (l.qty.isNotEmpty()) " — " + l.qty else "").append(if (l.note.isNotEmpty()) " (" + l.note + ")" else "").append('\n')
                            }
                        }
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
                    }, enabled = sections.isNotEmpty()) { Icon(Icons.Default.Share, stringResource(R.string.share)) }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Electrics: each board to elektronik, with its lines, cable, sockets, boxes and devices to buy there
            val boards = remember(plan) { com.dawoyan.roomik.core.Boards.all(plan) }
            androidx.compose.material3.Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("⚡ " + stringResource(R.string.tool_electric), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.el_in_elektronik), Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.bodyMedium)
                    if (!bom.hasBoard) Text(stringResource(R.string.no_board), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    if (bom.unlinkedLamps > 0) Text(stringResource(R.string.unlinked, bom.unlinkedLamps), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    for (b in boards) Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(com.dawoyan.roomik.core.Boards.name(b, ru) + " · " + floorName(b.floor, plan), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        FilledTonalButton(onClick = { exports.sendBoard(b) }) { Text("⇢ elektronik") }
                    }
                    if (boards.size > 1) androidx.compose.material3.OutlinedButton(onClick = exports.sendToElektronik, modifier = Modifier.padding(top = 6.dp)) {
                        Text(stringResource(R.string.send_all_boards)) }
                    androidx.compose.material3.TextButton(onClick = exports.saveForElektronik) { Text(stringResource(R.string.save_file)) }
                }
            }
            if (sections.isEmpty()) Text(stringResource(R.string.nothing_house), Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else {
                for ((head, list) in sections) {
                    Text(head, Modifier.padding(top = 18.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    for (l in list) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(l.name, style = MaterialTheme.typography.bodyLarge)
                                if (l.note.isNotEmpty()) Text(l.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(l.qty, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
                        }
                        HorizontalDivider()
                    }
                }
                Text(estimate, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.material3.OutlinedButton(onClick = exports.pdf, modifier = Modifier.padding(vertical = 16.dp)) { Text(stringResource(R.string.pdf)) }
        }
    }
}


private fun com.dawoyan.roomik.core.Plan.allFloorsList() = with(com.dawoyan.roomik.core.Plan::class) { this@allFloorsList.let { r -> (-r.basements.size..r.floors.size).map { r.floorAt(it) } + listOfNotNull(r.site) } }
private fun com.dawoyan.roomik.core.Plan.floorAt(i: Int): com.dawoyan.roomik.core.Plan = if (i == 0) this else if (i > 0) floors[i - 1] else basements[-i - 1]
