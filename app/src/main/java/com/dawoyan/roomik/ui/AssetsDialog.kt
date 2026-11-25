package com.dawoyan.roomik.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dawoyan.roomik.AppViewModel
import com.dawoyan.roomik.R
import com.dawoyan.roomik.core.Asset
import com.dawoyan.roomik.core.ModelPart
import com.dawoyan.roomik.core.SITE
import com.dawoyan.roomik.core.levels

/** A model seen from above, its parts in their colours (the higher ones over the lower). */
@Composable
fun ModelTop(parts: List<ModelPart>, modifier: Modifier) {
    val tris = remember(parts) {
        var x0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
        for (p in parts) for (k in p.xyz.indices step 3) { x0 = minOf(x0, p.xyz[k]); x1 = maxOf(x1, p.xyz[k]); y0 = minOf(y0, p.xyz[k + 1]); y1 = maxOf(y1, p.xyz[k + 1]) }
        val list = ArrayList<Triple<FloatArray, Float, Int>>()
        for (p in parts) for (t in p.tri.indices step 3) {
            val a = p.tri[t] * 3; val b = p.tri[t + 1] * 3; val c = p.tri[t + 2] * 3
            if (c + 2 >= p.xyz.size || a + 2 >= p.xyz.size || b + 2 >= p.xyz.size) continue
            list += Triple(floatArrayOf(p.xyz[a], p.xyz[a + 1], p.xyz[b], p.xyz[b + 1], p.xyz[c], p.xyz[c + 1]), (p.xyz[a + 2] + p.xyz[b + 2] + p.xyz[c + 2]) / 3, p.color)
        }
        list.sortBy { it.second }
        Triple(list, floatArrayOf(x0, y0, x1, y1), Unit)
    }
    Canvas(modifier) {
        val (list, b) = tris
        val w = (b[2] - b[0]).coerceAtLeast(1f); val h = (b[3] - b[1]).coerceAtLeast(1f)
        val k = minOf(size.width / w, size.height / h) * 0.9f
        val ox = (size.width - w * k) / 2 - b[0] * k; val oy = (size.height - h * k) / 2 - b[1] * k
        for ((t, z, c) in list) {
            val path = Path().apply { moveTo(ox + t[0] * k, oy + t[1] * k); lineTo(ox + t[2] * k, oy + t[3] * k); lineTo(ox + t[4] * k, oy + t[5] * k); close() }
            // a little darker lower down, so shapes read from above
            drawPath(path, Color(0xFF000000 or c.toLong()))
        }
    }
}

/**
 * The library of 3D assets: every model brought from Idea 3D or a file, kept for all plans. Pick the floor (or the
 * ground) first, then a model to put there; [onInsert] gets it and the level, and a tap on the plan places it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssetsDialog(vm: AppViewModel, onAddFile: () -> Unit, onDismiss: () -> Unit, onInsert: (Asset, Int) -> Unit) {
    val root = vm.root
    var level by remember { mutableStateOf(vm.floor) }
    var renaming by remember { mutableStateOf<Asset?>(null) }
    var deleting by remember { mutableStateOf<Asset?>(null) }
    renaming?.let { a -> TextDialog(stringResource(R.string.assets_3d), stringResource(R.string.name), a.name, onDismiss = { renaming = null }) { t -> renaming = null; vm.renameAsset(a, t) } }
    deleting?.let { a ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text(a.name) }, text = { Text(stringResource(R.string.asset_delete_q)) },
            confirmButton = { TextButton(onClick = { vm.deleteAsset(a); deleting = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📦 " + stringResource(R.string.assets_3d)) },
        text = {
            Column {
                // where it goes: a floor, or the ground round the house
                if (root != null) {
                    Text(stringResource(R.string.asset_where), style = MaterialTheme.typography.labelLarge)
                    FlowRow(Modifier.padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val lv = (root.levels.filter { it != SITE }.reversed() + SITE).distinct()
                        for (i in lv) FilterChip(level == i, onClick = { level = i },
                            label = { Text(if (i == SITE) "🌳 " + stringResource(R.string.ground) else floorName(i, root)) })
                    }
                }
                if (vm.assets.isEmpty()) Text(stringResource(R.string.assets_empty), Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(vm.assets, key = { it.id }) { a ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            ModelTop(a.parts, Modifier.size(56.dp).background(Color(0xFFF2F1EC), RoundedCornerShape(8.dp)).border(1.dp, Color(0x22000000), RoundedCornerShape(8.dp)))
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val sz = remember(a) { sizeOf(a.parts) }
                                Text(sz, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (root != null) FilledTonalButton(onClick = { onInsert(a, level) }) { Text(stringResource(R.string.asset_insert)) }
                                    IconButton(onClick = { renaming = a }) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
                                    IconButton(onClick = { deleting = a }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        dismissButton = { TextButton(onClick = onAddFile) { Text("＋ " + stringResource(R.string.asset_from_file)) } },
    )
}

/** Width × depth × height of a model, cm. */
private fun sizeOf(parts: List<ModelPart>): String {
    var x0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var y1 = -Float.MAX_VALUE; var z0 = Float.MAX_VALUE; var z1 = -Float.MAX_VALUE
    for (p in parts) for (k in p.xyz.indices step 3) {
        x0 = minOf(x0, p.xyz[k]); x1 = maxOf(x1, p.xyz[k]); y0 = minOf(y0, p.xyz[k + 1]); y1 = maxOf(y1, p.xyz[k + 1]); z0 = minOf(z0, p.xyz[k + 2]); z1 = maxOf(z1, p.xyz[k + 2])
    }
    if (x0 > x1) return ""
    fun c(v: Float) = Math.round(v / 10).toString()
    return c(x1 - x0) + " × " + c(y1 - y0) + " × " + c(z1 - z0) + " cm"
}
