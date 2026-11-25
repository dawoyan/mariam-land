package com.dawoyan.roomik

import android.content.Intent
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import java.util.Locale
import com.dawoyan.roomik.game.HumanoidCompanion
import com.dawoyan.roomik.ui.EditorScreen
import com.dawoyan.roomik.ui.MaterialsScreen
import com.dawoyan.roomik.ui.ProjectsScreen
import com.dawoyan.roomik.ui.View3DScreen

/** Mariam Land combines Roomik's complete house planner with a resident to build for. */
class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    /** The first release deliberately ships a single, consistent Russian UI. */
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration).apply { setLocale(Locale("ru")) }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    private fun take(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "roomik") vm.openLink(uri.getQueryParameter("plan"), uri.getQueryParameter("room"))
        if (intent.action == ACTION_IMPORT_MODEL || intent.action == Intent.ACTION_VIEW && uri.scheme == "content") runCatching {
            val bytes = contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            val file = com.dawoyan.roomik.ui.fileNameOf(this, uri)
            val name = intent.getStringExtra("name")?.takeIf { it.isNotBlank() } ?: file.substringBeforeLast('.')
            vm.receiveModel(name, file, bytes)
            if (vm.pendingModel != null && vm.screen != Screen.EDITOR)
                android.widget.Toast.makeText(this, R.string.open_plan_first, android.widget.Toast.LENGTH_LONG).show()
        }.onFailure { android.util.Log.e("MariamLand", "model not read", it) }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); take(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) take(intent)
        setContent {
            val scheme = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF8FCBE6), secondary = Color(0xFFFFC857))
            else lightColorScheme(primary = Color(0xFF2F6B8A), secondary = Color(0xFFFFC857))
            MaterialTheme(colorScheme = scheme) {
                BackHandler(vm.screen != Screen.PROJECTS) { vm.back() }
                Column(Modifier.fillMaxSize()) {
                    if (vm.screen == Screen.EDITOR || vm.screen == Screen.VIEW3D) HumanoidCompanion(vm)
                    Box(Modifier.weight(1f)) {
                        when (vm.screen) {
                            Screen.PROJECTS -> ProjectsScreen(vm)
                            Screen.EDITOR -> EditorScreen(vm)
                            Screen.VIEW3D -> View3DScreen(vm)
                            Screen.MATERIALS -> MaterialsScreen(vm)
                        }
                    }
                }
            }
        }
    }
}

const val ACTION_IMPORT_MODEL = "com.dawoyan.roomik.IMPORT_MODEL"
