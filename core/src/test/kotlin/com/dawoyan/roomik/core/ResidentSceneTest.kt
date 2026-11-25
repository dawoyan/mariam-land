package com.dawoyan.roomik.core

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResidentSceneTest {
    @Test fun residentRouteAndMeshStayInsideRoom() {
        val plan = PlanOps.addRect(Plan("test", "Home"), 0, 0, 6000, 4000)
        val room = Rooms.detect(plan).single()
        val start = assertNotNull(Mesh.residentPosition(plan, 0))
        val end = assertNotNull(Mesh.residentPosition(plan, 40))
        assertTrue((end - start).len > 1000.0)
        for (step in 0..40) assertTrue(Geo.inside(assertNotNull(Mesh.residentPosition(plan, step)), room.polygon))
        val mesh = Mesh.build(plan, residentFloor = 0)
        assertTrue(mesh.residentParts.isNotEmpty())
        assertTrue(mesh.residentParts.all { it.first >= mesh.groundCount && it.last < mesh.openCount })
    }

    @Test fun simpleViewKeepsLampsWhenWiringIsHidden() {
        var plan = PlanOps.addRect(Plan("test", "Home"), 0, 0, 4000, 3000)
        plan = assertNotNull(PlanOps.addPoint(plan, PointKind.LAMP, P(2000.0, 1500.0), 1000.0))
        assertTrue(Mesh.build(plan, hidden = setOf(Layer.ELECTRIC)).lights.isNotEmpty())
    }
}
