package com.dawoyan.roomik.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawoyan.roomik.R
import com.dawoyan.roomik.core.Facing
import com.dawoyan.roomik.core.Facings
import com.dawoyan.roomik.core.Pattern
import com.dawoyan.roomik.core.Stone

private fun col(c: Int) = Color(0xFF000000 or c.toLong())

/** A piece of wall 1.5 m × 0.75 m laid as [f]. */
@Composable
fun FacingSample(f: Facing, modifier: Modifier) {
    val cells = remember(f) { Facings.cells(f, 1500.0, 750.0, 3) }
    Canvas(modifier) {
        val k = size.width / 1500f
        drawRect(col(if (cells.isEmpty()) f.color else Facings.mortar(f)))
        val j = (Facings.JOINT / 2).toFloat()
        for (c in cells) {
            val top = size.height - (c.y1.toFloat() - j) * k
            drawRect(col(c.color), Offset((c.t0.toFloat() + j) * k, top),
                Size(((c.t1 - c.t0).toFloat() - 2 * j) * k, ((c.y1 - c.y0).toFloat() - 2 * j) * k))
        }
    }
}

/** Choose a wall's or fence's stone: a known one (tuf pink, basalt black…), or any stone, colour and way of laying. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FacingDialog(
    initial: Facing?, allowNone: Boolean, onDismiss: () -> Unit,
    /** Which faces it can go on (walls); empty: one choice (fences). */
    sides: List<Facings.Side> = emptyList(),
    /** What is on those faces now. */
    current: (Facings.Side) -> Facing? = { null },
    initialSide: Facings.Side? = null,
    onOk: (Facings.Side, Facing?) -> Unit,
) {
    val lang = language()
    var side by remember { mutableStateOf(initialSide ?: sides.firstOrNull() ?: Facings.Side.OUTSIDE) }
    var f by remember { mutableStateOf(initial ?: current(side) ?: Facings.presets[1].facing) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.facing)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (sides.isNotEmpty()) FlowRow(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (sd in sides) FilterChip(side == sd, onClick = { side = sd; current(sd)?.let { f = it } }, label = { Text(stringResource(when (sd) {
                        Facings.Side.OUTSIDE -> R.string.side_outside; Facings.Side.INSIDE -> R.string.side_inside
                        Facings.Side.LEFT -> R.string.side_left; Facings.Side.RIGHT -> R.string.side_right; Facings.Side.BOTH -> R.string.side_both })) })
                }
                FacingSample(f, Modifier.fillMaxWidth().aspectRatio(2f).border(1.dp, Color(0x33000000)))
                Text(Facings.name(f, lang), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleSmall)
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (p in Facings.presets) FilterChip(f.stone == p.facing.stone && f.color == p.facing.color, onClick = {
                        // a known stone keeps the way of laying chosen, unless it has its own (bricks, plaster)
                        f = p.facing.copy(pattern = if (p.facing.pattern == Pattern.BLOCKS && f.pattern != Pattern.SMOOTH) f.pattern else p.facing.pattern)
                    }, label = { Text(p.name(lang)) }, leadingIcon = { Box(Modifier.size(14.dp).background(col(p.facing.color), CircleShape).border(1.dp, Color(0x44000000), CircleShape)) })
                }
                Text(stringResource(R.string.stone), Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (s in Stone.entries) FilterChip(f.stone == s, onClick = { f = f.copy(stone = s, pattern = if (s == Stone.PLASTER) Pattern.SMOOTH else if (f.pattern == Pattern.SMOOTH) Pattern.BLOCKS else f.pattern) },
                        label = { Text(Facings.stoneName(s, lang)) })
                }
                Text(stringResource(R.string.pattern), Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((p, label) in listOf(Pattern.BLOCKS to R.string.pat_blocks, Pattern.BOND to R.string.pat_bond, Pattern.RUBBLE to R.string.pat_rubble, Pattern.SMOOTH to R.string.pat_smooth))
                        FilterChip(f.pattern == p, onClick = { f = f.copy(pattern = p) }, label = { Text(stringResource(label)) })
                }
                Text(stringResource(R.string.colour), Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelLarge)
                FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in Facings.colors) Box(Modifier.size(32.dp).background(col(c), RoundedCornerShape(6.dp))
                        .border(if (f.color == c) 3.dp else 1.dp, if (f.color == c) MaterialTheme.colorScheme.primary else Color(0x44000000), RoundedCornerShape(6.dp))
                        .clickable { f = f.copy(color = c) })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onOk(side, f) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = {
            if (allowNone) TextButton(onClick = { onOk(side, null) }) { Text(stringResource(R.string.none)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
