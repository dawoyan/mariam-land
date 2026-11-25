package com.dawoyan.roomik.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dawoyan.roomik.AppViewModel
import com.dawoyan.roomik.R
import com.dawoyan.roomik.core.Palette
import com.dawoyan.roomik.core.Plan
import com.dawoyan.roomik.core.Rooms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(vm: AppViewModel) {
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Plan?>(null) }
    var deleting by remember { mutableStateOf<Plan?>(null) }
    val copy = stringResource(R.string.copy_suffix)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.plans)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, stringResource(R.string.new_plan)) }
        },
    ) { pad ->
        if (vm.plans.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_plans), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp), modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(vm.plans, key = { it.id }) { p ->
                    PlanCard(p, onOpen = { vm.open(p) }, onRename = { renaming = p },
                        onDuplicate = { vm.duplicate(p, "${p.name} ($copy)") }, onDelete = { deleting = p })
                }
            }
        }
    }

    if (creating) TextDialog(stringResource(R.string.new_plan), stringResource(R.string.name),
        stringResource(R.string.plan_n, vm.plans.size + 1), onDismiss = { creating = false }) { creating = false; vm.create(it.trim()) }
    renaming?.let { p ->
        TextDialog(stringResource(R.string.rename), stringResource(R.string.name), p.name, onDismiss = { renaming = null }) {
            renaming = null; vm.rename(p, it.trim())
        }
    }
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text(stringResource(R.string.delete_plan_q, p.name)) },
            confirmButton = { TextButton(onClick = { deleting = null; vm.delete(p) }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun PlanCard(p: Plan, onOpen: () -> Unit, onRename: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val rooms = remember(p) { Rooms.detect(p) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1.3f).padding(14.dp)) {
                if (p.nodes.isEmpty()) return@Canvas
                val minX = p.nodes.minOf { it.x }.toFloat(); val maxX = p.nodes.maxOf { it.x }.toFloat()
                val minY = p.nodes.minOf { it.y }.toFloat(); val maxY = p.nodes.maxOf { it.y }.toFloat()
                val s = minOf(size.width / (maxX - minX).coerceAtLeast(1f), size.height / (maxY - minY).coerceAtLeast(1f))
                val ox = (size.width - (maxX - minX) * s) / 2 - minX * s
                val oy = (size.height - (maxY - minY) * s) / 2 - minY * s
                rooms.forEachIndexed { i, r ->
                    val path = Path()
                    r.polygon.forEachIndexed { k, q ->
                        val x = q.x.toFloat() * s + ox; val y = q.y.toFloat() * s + oy
                        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path, Color(0xFF000000 or Palette.floor(r, i).toLong()))
                }
                for (w in p.walls) {
                    val a = p.node(w.a) ?: continue; val b = p.node(w.b) ?: continue
                    drawLine(Color(0xFF3A3F4B), Offset(a.x * s + ox, a.y * s + oy), Offset(b.x * s + ox, b.y * s + oy),
                        strokeWidth = (w.thickness * s).coerceAtLeast(2.dp.toPx()), cap = StrokeCap.Square)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 12.dp, bottom = 10.dp)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.rooms_count, rooms.size) + " · " + num(rooms.sumOf { it.area }, 1) + " " + stringResource(R.string.m2),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = { menu = false; onRename() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.duplicate)) }, onClick = { menu = false; onDuplicate() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; onDelete() })
                    }
                }
            }
        }
    }
}
