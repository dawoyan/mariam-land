package com.dawoyan.roomik

import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local, bounded diagnostic history. Never sends anything; the player chooses whether to copy it. */
object Diagnostics {
    private const val PREFS = "mariam_diagnostics"
    private const val MAX_CHARS = 32000
    private var installed = false
    @Synchronized fun record(context: Context, event: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val prior = prefs.getString("history", "").orEmpty()
        prefs.edit().putString("history", (prior + "\n" + timestamp + " " + event).takeLast(MAX_CHARS)).commit()
    }
    @Synchronized fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val original = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try { record(app, "CRASH " + error.stackTraceToString().take(12000)) } catch (_: Throwable) { }
            original?.uncaughtException(thread, error)
        }
        record(app, "app_started version=" + app.packageManager.getPackageInfo(app.packageName, 0).versionName)
    }
    fun report(context: Context): String {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val history = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("history", "").orEmpty()
        return "Mariam Land $version | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) | ${Build.MANUFACTURER} ${Build.MODEL}\n" + history.ifBlank { "No events recorded." }
    }
}
