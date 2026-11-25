package com.dawoyan.roomik

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawoyan.roomik.core.Plan
import com.dawoyan.roomik.core.PlanJson
import com.dawoyan.roomik.core.PlanOps
import com.dawoyan.roomik.core.Surface
import com.dawoyan.roomik.core.P
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedHouseStartupTest {
    @Test fun savedHouseCanBeRestoredAfterConstructionWithoutDeletingGrass() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("roomik", 0)
        val oldLast = prefs.getString("lastProject", null)
        val id = "startup-test-${UUID.randomUUID()}"
        val dir = File(app.filesDir, "plans").apply { mkdirs() }
        val file = File(dir, "$id.json")
        val p = PlanOps.addRect(Plan(id, "Startup test"), 0, 0, 4000, 3000)
        val grass = PlanOps.addRoad(p, listOf(P(0.0, 0.0), P(1000.0, 0.0), P(1000.0, 1000.0)), Surface.GRASS, 1000, closed = true).copy(schemaVersion = 2)
        file.writeText(PlanJson.encode(grass))
        prefs.edit().putString("lastProject", id).commit()
        try {
            val vm = AppViewModel(app)
            assertNull(vm.root)
            vm.restoreLastProject()
            assertEquals(id, vm.root?.id)
            assertEquals(Screen.VIEW3D, vm.screen)
            assertTrue(vm.root!!.roads.any { it.surface == Surface.GRASS })
            assertFalse(vm.canUndo)
            vm.restoreLastProject()
            assertEquals(id, vm.root?.id)
            assertTrue(PlanJson.decode(file.readText()).roads.any { it.surface == Surface.GRASS })
            // Recreate and relaunch the actual activity with a remembered house, as after an app upgrade.
            repeat(2) {
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    scenario.onActivity { activity ->
                        val active = ViewModelProvider(activity)[AppViewModel::class.java]
                        assertEquals(id, active.root?.id)
                        assertEquals(Screen.VIEW3D, active.screen)
                    }
                    scenario.recreate()
                    scenario.onActivity { activity ->
                        assertEquals(id, ViewModelProvider(activity)[AppViewModel::class.java].root?.id)
                    }
                }
            }
        } finally {
            val edit = prefs.edit().remove("lastFloor:$id")
            if (oldLast == null) edit.remove("lastProject") else edit.putString("lastProject", oldLast)
            edit.commit()
            file.delete()
        }
    }
}
