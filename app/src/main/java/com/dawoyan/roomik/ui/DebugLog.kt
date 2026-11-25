package com.dawoyan.roomik.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/** Small in-app trace for diagnosing a user's plan without requiring Android Studio or logcat. */
object DebugLog {
    private data class Entry(val time: Long, val text: String)
    private val entries = ArrayDeque<Entry>()
    fun add(text: String) {
        entries += Entry(System.currentTimeMillis(), text)
        while (entries.size > 200) entries.removeFirst()
    }
    fun copyLastFiveMinutes(context: Context) {
        val since = System.currentTimeMillis() - 5 * 60_000
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US)
        val report = entries.filter { it.time >= since }.joinToString("\n") { stamp.format(Date(it.time)) + "  " + it.text }
            .ifBlank { "No Roomik diagnostics recorded in the last 5 minutes." }
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Roomik diagnostics", report))
        Toast.makeText(context, "Diagnostics copied", Toast.LENGTH_SHORT).show()
    }
}
