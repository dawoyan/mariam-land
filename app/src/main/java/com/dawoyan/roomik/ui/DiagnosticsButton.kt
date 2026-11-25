package com.dawoyan.roomik.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
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
import com.dawoyan.roomik.Diagnostics

@Composable fun DiagnosticsButton() {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("📋 Лог") }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Диагностика") }, text = {
        Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            Text("Лог хранится на устройстве и никуда не отправляется. В ошибках могут быть технические данные и имена файлов. Проверьте перед отправкой.")
            Text(Diagnostics.report(context))
        }
    }, confirmButton = { TextButton(onClick = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Mariam Land diagnostics", Diagnostics.report(context)))
        open = false
    }) { Text("Копировать") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Закрыть") } })
}
