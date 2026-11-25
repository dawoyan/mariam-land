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

    @Test fun residentCanReachFurnitureThroughAnInteriorDoor() {
        var plan = PlanOps.addRect(Plan("test", "Home"), 0, 0, 4000, 4000)
        plan = PlanOps.addRect(plan, 4000, 0, 8000, 4000)
        plan = assertNotNull(PlanOps.addOpening(plan, OpeningKind.DOOR, P(4000.0, 2000.0), 500.0))
        plan = PlanOps.addItem(plan, "fridge", 6800, 1800)
        plan = PlanOps.addItem(plan, "bed", 1200, 1800)
        val route = ResidentNavigation.route(plan)
        assertTrue(route.size > 1)
        assertTrue(route.zipWithNext().all { (a, b) -> Interior.canWalk(plan, a, b) })
        assertTrue(route.any { it.x < 4000 } && route.any { it.x > 4000 })
    }

    @Test fun bothVacuumsCanBePlacedAndRendered() {
        var plan = PlanOps.addRect(Plan("test", "Home"), 0, 0, 4000, 3000)
        plan = PlanOps.addItem(plan, "vacuum", 1000, 1000)
        plan = PlanOps.addItem(plan, "robot_vacuum", 2000, 1000)
        assertTrue(plan.items.map { it.kind }.containsAll(listOf("vacuum", "robot_vacuum")))
        assertTrue(plan.items.all { Furniture.parts(it).isNotEmpty() })
        assertTrue(Mesh.build(plan).vertexCount > 0)
    }

    @Test fun exteriorDoorProducesAPathFromOutsideIntoTheHouse() {
        var plan = PlanOps.addRect(Plan("test", "Home"), 0, 0, 5000, 4000)
        plan = assertNotNull(PlanOps.addOpening(plan, OpeningKind.DOOR, P(2500.0, 0.0), 500.0))
        plan = PlanOps.addItem(plan, "fridge", 3500, 2500)
        val room = Rooms.detect(plan).single()
        val route = ResidentNavigation.route(plan)
        assertTrue(route.size > 1)
        assertTrue(!Geo.inside(route.first(), room.polygon))
        assertTrue(route.drop(1).any { Geo.inside(it, room.polygon) })
        assertTrue(route.zipWithNext().all { (a, b) -> Interior.canWalk(plan, a, b) })
        assertTrue(ResidentNavigation.diagnose(plan).exteriorEntry)
    }

    @Test fun missingExteriorDoorProducesAnHonestDiagnostic() {
        val plan = PlanOps.addRect(Plan("test", "Home"), 0, 0, 4000, 3000)
        val diagnosis = ResidentNavigation.diagnose(plan)
        assertTrue(!diagnosis.exteriorEntry)
        assertTrue(diagnosis.reason == "no_usable_door")
    }

    @Test fun commandRoutesThroughDoorAndRejectsUnreachableRoom() {
        var plan = PlanOps.addRect(Plan("test", "Home"), 0, 0, 4000, 4000)
        plan = PlanOps.addRect(plan, 4000, 0, 8000, 4000)
        val from = P(1000.0, 2000.0); val target = P(7000.0, 2000.0)
        assertTrue(ResidentNavigation.route(plan, from, target).isEmpty())
        plan = assertNotNull(PlanOps.addOpening(plan, OpeningKind.DOOR, P(4000.0, 2000.0), 500.0))
        val route = ResidentNavigation.route(plan, from, target)
        assertTrue(route.size > 1)
        assertTrue((route.last() - target).len < 500.0)
        assertTrue(route.zipWithNext().all { (a, b) -> Interior.canWalk(plan, a, b) })
    }

    @Test fun residentCanLeaveBedAfterSleeping() {
        var plan = PlanOps.addRect(Plan("test", "Home"), 0, 0, 5000, 4000)
        plan = PlanOps.addItem(plan, "bed", 1500, 2000)
        val bed = plan.items.single()
        val route = ResidentNavigation.route(plan, P(bed.x.toDouble(), bed.y.toDouble()), P(4000.0, 2000.0))
        assertTrue(route.size > 1)
        assertTrue((route.last() - P(4000.0, 2000.0)).len < 500)
    }

    @Test fun reachesHouseFromFarOutsideThroughDoor() {
        var plan = PlanOps.addRect(Plan("test", "Home"), 0, 0, 5000, 4000)
        plan = assertNotNull(PlanOps.addOpening(plan, OpeningKind.DOOR, P(2500.0, 0.0), 500.0))
        val route = ResidentNavigation.route(plan, P(2500.0, -4500.0), P(3000.0, 2500.0))
        assertTrue(route.size > 2)
        assertTrue(route.zipWithNext().all { (a, b) -> Interior.canWalk(plan, a, b) })
        assertTrue((route.last() - P(3000.0, 2500.0)).len < 500)
    }

    @Test fun closedOffTargetIsNotConsideredReachedAcrossWall() {
        var plan = PlanOps.addRect(Plan("test", "Home"), 0, 0, 4000, 4000)
        plan = PlanOps.addRect(plan, 4000, 0, 8000, 4000)
        assertTrue(ResidentNavigation.route(plan, P(3600.0, 2000.0), P(4300.0, 2000.0)).isEmpty())
    }

    @Test fun doorwayWaypointsBridgeAnOffsetDoorInALargeHouse() {
        var plan = PlanOps.addRect(Plan("large", "House"), 0, 0, 4100, 3800)
        plan = PlanOps.addRect(plan, 4100, 0, 8500, 3800)
        plan = PlanOps.addRect(plan, 8500, 0, 14000, 3800)
        plan = assertNotNull(PlanOps.addOpening(plan, OpeningKind.DOOR, P(4100.0, 1730.0), 500.0))
        plan = assertNotNull(PlanOps.addOpening(plan, OpeningKind.DOOR, P(8500.0, 2330.0), 500.0))
        val start = P(1200.0, 2000.0); val target = P(12500.0, 2000.0)
        val route = ResidentNavigation.route(plan, start, target)
        assertTrue(route.isNotEmpty())
        assertTrue((route.last() - target).len < 700)
        assertTrue(route.zipWithNext().all { (a, b) -> Interior.canWalk(plan, a, b) })
    }
}
