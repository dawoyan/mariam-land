package com.dawoyan.roomik.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.dawoyan.elektronik.core.RoomType
import com.dawoyan.roomik.R
import java.util.Locale

private val HY = mapOf(
    RoomType.LIVING to "Հյուրասենյակ", RoomType.BEDROOM to "Ննջասենյակ", RoomType.CHILD to "Մանկական",
    RoomType.OFFICE to "Աշխատասենյակ", RoomType.KITCHEN to "Խոհանոց", RoomType.BATHROOM to "Լոգարան",
    RoomType.TOILET to "Զուգարան", RoomType.HALL to "Միջանցք", RoomType.BALCONY to "Պատշգամբ",
    RoomType.STORAGE to "Պահեստ", RoomType.GARAGE to "Ավտոտնակ", RoomType.WORKSHOP to "Արհեստանոց",
    RoomType.BASEMENT to "Նկուղ", RoomType.YARD to "Բակ", RoomType.OTHER to "Այլ",
)

@Composable
fun language(): String = LocalConfiguration.current.locales[0].language

/** elektronik's room types have en and ru titles; Armenian ones are added here. */
fun RoomType.label(lang: String): String = when (lang) {
    "ru" -> titleRu
    "hy" -> HY[this] ?: title
    else -> title
}

/** Millimetres as centimetres: 3200 → "320", 3205 → "320.5". */
fun cm(mm: Double): String {
    val v = Math.round(mm) / 10.0
    return if (v == Math.floor(v)) v.toLong().toString() else String.format(Locale.US, "%.1f", v)
}

fun num(v: Double, digits: Int = 2): String = String.format(Locale.US, "%.${digits}f", v)

@Composable
fun TextDialog(title: String, label: String, initial: String, numeric: Boolean = false, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, label = { Text(label) }, singleLine = true,
                keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
            )
        },
        confirmButton = { TextButton(onClick = { onOk(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * Scrolls a row of chips sideways and shows that it can: a round ‹ at the left when there is more to the left, a › at
 * the right when there is more to the right.
 */
@Composable
fun androidx.compose.ui.Modifier.hScroll(): androidx.compose.ui.Modifier {
    val st = androidx.compose.foundation.rememberScrollState()
    val tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
    return this.then(androidx.compose.ui.Modifier.drawWithContent {
        drawContent()
        val r = 11.dp.toPx()
        val cy = size.height / 2
        fun mark(cx: Float, left: Boolean) {
            drawCircle(tint.copy(alpha = 0.9f), r, androidx.compose.ui.geometry.Offset(cx, cy))
            val k = r * 0.38f; val d = if (left) 1f else -1f
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx + k * 0.6f * d, cy - k); lineTo(cx - k * 0.6f * d, cy); lineTo(cx + k * 0.6f * d, cy + k)
            }
            drawPath(p, androidx.compose.ui.graphics.Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
        if (st.value > 0) mark(r + 1f, true)
        if (st.value < st.maxValue) mark(size.width - r - 1f, false)
    }).horizontalScroll(st)
}
