package com.dawoyan.roomik.core

import com.dawoyan.elektronik.core.RoomType
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreTest {
    private val empty = Plan("t", "t")

    @Test fun rectangleIsOneRoomWithClearArea() {
        val p = PlanOps.addRect(empty, 0, 0, 4000, 3000, 200)
        assertEquals(4, p.nodes.size); assertEquals(4, p.walls.size)
        val rooms = Rooms.detect(p)
        assertEquals(1, rooms.size)
        assertEquals(3.8 * 2.8, rooms[0].area, 1e-6)
        assertEquals(2 * (3.8 + 2.8), rooms[0].perimeter, 1e-6)
    }

    @Test fun wallAcrossSplitsRoomAndWalls() {
        var p = PlanOps.addRect(empty, 0, 0, 6000, 3000, 200)
        p = PlanOps.addWall(p, 2000, 0, 2000, 3000, 100)
        assertEquals(6, p.nodes.size); assertEquals(7, p.walls.size)
        val areas = Rooms.detect(p).map { it.area }.sorted()
        assertEquals(2, areas.size)
        assertEquals((2.0 - 0.15) * 2.8, areas[0], 1e-6)
        assertEquals((4.0 - 0.15) * 2.8, areas[1], 1e-6)
    }

    @Test fun crossingWallsSplitEachOther() {
        var p = PlanOps.addWall(empty, 0, 1000, 4000, 1000)
        p = PlanOps.addWall(p, 2000, 0, 2000, 3000)
        assertEquals(5, p.nodes.size); assertEquals(4, p.walls.size)
        assertEquals(0, Rooms.detect(p).size)
    }

    @Test fun twoRoomsShareAWallAndDeadEndsAreIgnored() {
        var p = PlanOps.addRect(empty, 0, 0, 3000, 3000)
        p = PlanOps.addRect(p, 3000, 0, 7000, 3000)
        assertEquals(6, p.nodes.size); assertEquals(7, p.walls.size)
        p = PlanOps.addWall(p, 1000, 0, 1000, 1500) // partition
        assertEquals(2, Rooms.detect(p).size)
    }

    @Test fun roomInfoFollowsItsRoom() {
        var p = PlanOps.addRect(empty, 0, 0, 3000, 3000)
        p = PlanOps.addRect(p, 3000, 0, 7000, 3000)
        val big = Rooms.detect(p).maxBy { it.area }
        p = PlanOps.setRoomInfo(p, big, "Кухня", RoomType.KITCHEN)
        val shared = p.walls.first { w -> listOf(w.a, w.b).all { p.node(it)!!.x == 3000 } }
        p = PlanOps.finish(PlanOps.moveWall(p, shared.id, 500, 0))
        val rooms = Rooms.detect(p)
        assertEquals(2, rooms.size)
        val kitchen = rooms.single { it.info?.type != null }
        assertEquals(RoomType.KITCHEN, kitchen.info!!.type)
        assertEquals((3.5 - 0.2) * 2.8, kitchen.area, 1e-6)
        val back = PlanJson.decode(PlanJson.encode(p))
        assertEquals(p, back)
    }

    @Test fun socketsRideWithTheirWallAndItemsMesh() {
        var p = PlanOps.addRect(empty, 0, 0, 4000, 3000, 200)
        p = PlanOps.addPoint(p, PointKind.SOCKET, P(3950.0, 1500.0), 500.0)!!
        p = PlanOps.addPoint(p, PointKind.LAMP, P(2000.0, 1500.0), 500.0)!!
        assertEquals(null, PlanOps.addPoint(p, PointKind.SWITCH, P(2000.0, 1500.0), 300.0))
        val s = p.points[0]
        assertEquals(4000, s.x); assertEquals(-1.0, s.nx, 1e-9) // on the right wall, facing into the room
        assertEquals(3900.0, Interior.place(p)[0].face.x, 1e-6)
        val right = p.walls.first { w -> listOf(w.a, w.b).all { p.node(it)!!.x == 4000 } }
        p = PlanOps.finish(PlanOps.moveWall(p, right.id, 1000, 0))
        assertEquals(5000, p.points[0].x); assertEquals(-1.0, p.points[0].nx, 1e-9)
        val before = Mesh.build(p).vertexCount
        p = PlanOps.addItem(p, "bed", 1500, 1500)
        assertTrue(Mesh.build(p).vertexCount > before + 36 * 4) // a bed is several blocks
        assertEquals(p, PlanJson.decode(PlanJson.encode(p)))
    }

    @Test fun doorCutsWallAndLetsYouThrough() {
        var p = PlanOps.addRect(empty, 0, 0, 4000, 3000, 200)
        val solid = Mesh.build(p).vertexCount
        assertTrue(!Interior.canWalk(p, P(2000.0, 1500.0), P(2000.0, -1500.0)))
        p = PlanOps.addOpening(p, OpeningKind.DOOR, P(2000.0, 30.0), 500.0)!!
        p = PlanOps.addOpening(p, OpeningKind.WINDOW, P(3950.0, 1500.0), 500.0)!!
        assertEquals(2, Interior.openings(p).values.sumOf { it.size })
        assertTrue(Mesh.build(p).vertexCount > solid)
        assertTrue(Interior.canWalk(p, P(2000.0, 1500.0), P(2000.0, -1500.0)))
        assertTrue(!Interior.canWalk(p, P(2000.0, 1500.0), P(3000.0, -1500.0)))
        assertTrue(!Interior.canWalk(p, P(2000.0, 1500.0), P(6000.0, 1500.0))) // a window is no way out
        assertEquals(p, PlanJson.decode(PlanJson.encode(p)))
    }

    @Test fun materialsFollowElektronikLines() {
        var p = PlanOps.addRect(empty, 0, 0, 4000, 3000, 200)
        p = PlanOps.addPoint(p, PointKind.BOARD, P(50.0, 500.0), 500.0)!!
        for (y in listOf(1000.0, 2000.0)) p = PlanOps.addPoint(p, PointKind.SOCKET, P(3950.0, y), 500.0)!!
        p = PlanOps.updatePoint(p, p.points.last().copy(count = 3))
        p = PlanOps.addPoint(p, PointKind.SWITCH, P(1000.0, 50.0), 500.0)!!
        val sw = p.points.last().id
        p = PlanOps.addPoint(p, PointKind.LAMP, P(2000.0, 1500.0), 500.0)!!
        p = PlanOps.toggleLink(p, sw, p.points.last().id)
        val bom = Materials.build(p, false)
        assertTrue(bom.hasBoard); assertEquals(0, bom.unlinkedLamps)
        assertEquals(2, bom.lines.size)
        val light = bom.lines.first { it.mm2 == 1.5 }; val sockets = bom.lines.first { it.mm2 == 2.5 }
        assertEquals("C10", light.rating); assertEquals("C16", sockets.rating)
        // sockets: up 1.05 + 1 slack; along walls 0.5+4+1 to the first, 1 to the second; down-up 2×2.25, down 2.25; 4×0.15
        assertEquals(1.05 + 1.0 + 5.5 + 1.0 + 3 * 2.25 + 0.6, sockets.meters, 0.01)
        // lights: up 1.05 + 1; board → switch 0.5+1; down 1.65 + 0.15; up 1.65; switch → lamp 1+1.5; 0.15
        assertEquals(1.05 + 1.0 + 1.5 + 1.65 + 0.15 + 1.65 + 2.5 + 0.15, light.meters, 0.01)
        assertEquals(5, bom.items.first { it.key == BomKey.BACK_BOX }.qty)
        assertEquals(4, bom.items.first { it.key == BomKey.SOCKET }.qty)
        assertEquals(listOf(1 to 2, 3 to 1), bom.items.filter { it.key == BomKey.FRAME }.map { it.detail to it.qty })
        assertTrue(bom.board.isNotEmpty())
        assertTrue(com.dawoyan.elektronik.core.BoardBundle.read(Materials.elektronikFile(p, false))!!.boards[0].rooms.size == 1)
    }

    @Test fun lampFollowsItsSwitchRoom() {
        var p = PlanOps.addRect(empty, 0, 0, 3000, 3000)
        p = PlanOps.addRect(p, 3000, 0, 6000, 3000)
        p = PlanOps.addPoint(p, PointKind.SWITCH, P(1000.0, 50.0), 500.0)!!
        p = PlanOps.addPoint(p, PointKind.LAMP, P(4500.0, 1500.0), 500.0)!!
        p = PlanOps.toggleLink(p, p.points[0].id, p.points[1].id)
        assertEquals(1, Materials.build(p, false).lines.size)
    }

    @Test fun emptyRoomsAreSentToo() {
        var p = PlanOps.addRect(empty, 0, 0, 3000, 3000)
        p = PlanOps.addRect(p, 3000, 0, 6000, 3000)
        val rooms = com.dawoyan.elektronik.core.BoardBundle.read(Materials.elektronikFile(p, false))!!.boards[0].rooms
        assertEquals(2, rooms.size)
        assertTrue(rooms.all { it.id.startsWith("rk_t_") })
    }

    @Test fun furnitureStaysOffWalls() {
        var p = PlanOps.addRect(empty, 0, 0, 4000, 3000, 200)
        p = PlanOps.addItem(p, "armchair", 3900, 1500) // dropped on the right wall
        val a = p.items[0]
        assertEquals(4000.0 - 100 - 425, a.x.toDouble(), 2.0) // flush with the wall's inner face
        p = PlanOps.updateItem(p, a.copy(x = 4500, y = -300)) // dragged into the corner
        val b = p.items[0]
        assertTrue(b.x <= 4000 - 100 - 425 + 2 && b.y >= 100 + 425 - 2)
    }

    @Test fun openWallsAndPlacesGoToElektronik() {
        var p = PlanOps.addWall(empty, 0, 0, 6000, 0)
        p = PlanOps.addWall(p, 0, 0, 0, 4000) // a garage drawn as two lines
        p = PlanOps.addPoint(p, PointKind.SOCKET, P(3000.0, 150.0), 500.0)!!
        var rooms = com.dawoyan.elektronik.core.BoardBundle.read(Materials.elektronikFile(p, false))!!.boards[0].rooms
        assertEquals(1, rooms.size); assertEquals("t", rooms[0].name)
        p = PlanOps.addPlace(p, 2000, 2000)
        p = PlanOps.updatePlace(p, p.places[0].copy(name = "Garage", type = RoomType.GARAGE))
        rooms = com.dawoyan.elektronik.core.BoardBundle.read(Materials.elektronikFile(p, false))!!.boards[0].rooms
        assertEquals(listOf("Garage"), rooms.map { it.name })
        assertEquals(RoomType.GARAGE, rooms[0].type); assertEquals("rk_t_p1", rooms[0].id)
    }

    @Test fun passageAndHingeSide() {
        var p = PlanOps.addRect(empty, 0, 0, 4000, 3000, 200)
        p = PlanOps.addOpening(p, OpeningKind.DOOR, P(2000.0, 30.0), 500.0)!!
        val door = Mesh.build(p).vertexCount
        p = PlanOps.updateOpening(p, p.openings[0].copy(kind = OpeningKind.PASSAGE))
        assertTrue(Mesh.build(p).vertexCount < door)
        assertTrue(Interior.canWalk(p, P(2000.0, 1500.0), P(2000.0, -1500.0)))
        p = PlanOps.updateOpening(p, p.openings[0].copy(kind = OpeningKind.DOOR, hingeEnd = true))
        assertEquals(door, Mesh.build(p).vertexCount)
    }

    @Test fun floorsStackAndExport() {
        var p = PlanOps.addRect(empty, 0, 0, 4000, 3000, 200)
        p = p.copy(items = p.items + Item(1, STAIRS, 1000, 1700, 1000, 3000, 2900))
        p = Stairs.migrate(p)
        assertEquals(0, p.items.size); assertEquals(1, p.stairs.size)
        for (k in StairKind.entries) {
            val sh = Stairs.shape(p, p.stairs[0].copy(kind = k))
            assertEquals(2900.0, sh.flights.last().h1, 1e-9)
            assertEquals(if (k == StairKind.STRAIGHT) 0 else 1, sh.landings.size)
            assertEquals(if (k == StairKind.STRAIGHT) 17 else 8, sh.flights[0].steps)
        }
        val rev = Stairs.shape(p, p.stairs[0].copy(kind = StairKind.L_LEFT, reversed = true))
        assertEquals(0.0, rev.flights[0].h0, 1e-9); assertEquals(2900.0, rev.flights.last().h1, 1e-9)
        // porch: from the ground 60 cm below up to the ground floor
        val porch = Stairs.shape(p.copy(groundLevel = 600), p.stairs[0].copy(fromGround = true))
        assertEquals(-600.0, porch.flights[0].h0, 1e-9); assertEquals(0.0, porch.flights.last().h1, 1e-9)
        // a stair is not pushed off a wall it passes over
        p = PlanOps.updateStair(p, p.stairs[0].copy(x = 3990))
        assertEquals(3990, p.stairs[0].x)
        // running into the right wall (x = 4000): it has to come down below the steps over it
        val clash = Stairs.clashes(p, p.stairs[0])
        assertTrue(clash.isNotEmpty() && clash.all { it.fitHeight < 2700 })
        // crop the wall by the stairs: no clash, the wall gets a cut; moving the stairs makes the wall whole again
        val cropped = PlanOps.cropWalls(p, p.stairs[0].id)
        assertTrue(Stairs.clashes(cropped, cropped.stairs[0]).isEmpty())
        assertTrue(Stairs.crops(cropped).isNotEmpty())
        assertTrue(Mesh.build(cropped).vertexCount > Mesh.build(p).vertexCount)
        val moved = PlanOps.updateStair(cropped, cropped.stairs[0].copy(y = cropped.stairs[0].y + 50))
        assertTrue(moved.stairs[0].cropped.isEmpty() && Stairs.crops(moved).isEmpty())
        assertEquals("fireplace", Catalog.forRoom(RoomType.LIVING)[3].id)
        p = PlanOps.updateStair(p, p.stairs[0].copy(x = 1000, length = 2500)) // inside the room now
        assertTrue(Stairs.clashes(p, p.stairs[0]).isEmpty())
        // a low wall
        val lowWall = p.walls[0].id
        val before = Mesh.build(p).data.maxOf { it }
        p = PlanOps.setWallHeight(p, lowWall, 1000)
        assertEquals(1000, p.heightOf(p.wall(lowWall)!!))
        p = PlanOps.finish(PlanOps.addWall(p, 2000, -500, 2000, 500)) // splits it: both halves stay low
        assertEquals(2, p.walls.count { it.height == 1000 })
        p = PlanOps.addPoint(p, PointKind.BOARD, P(50.0, 500.0), 500.0)!!
        p = PlanOps.addFloor(p)
        assertEquals(1, p.floors.size)
        assertTrue(p.floor(1).walls.size in 1 until p.walls.size) // the shell only: no inner or low walls
        assertEquals(2900, p.elevation(1))
        val up = PlanOps.addPoint(p.floor(1), PointKind.SOCKET, P(3950.0, 1500.0), 500.0)!!
        p = p.withFloor(1, up)
        assertEquals(1, p.floor(1).points.size); assertEquals(1, p.points.size)
        // the stairs below cut a stairwell into floor 2's floor: less floor area there
        val upRoom = Rooms.detect(p).maxBy { it.area } // same outline as upstairs
        val whole = Stairs.fillAround(upRoom.polygon, emptyList()).sumOf { (a, b, c) -> kotlin.math.abs(cross(b - a, c - a)) / 2 }
        val holed = Stairs.fillAround(upRoom.polygon, Stairs.wells(p)).sumOf { (a, b, c) -> kotlin.math.abs(cross(b - a, c - a)) / 2 }
        val wellArea = Stairs.wells(p).sumOf { kotlin.math.abs(Geo.area(it)) }
        assertEquals(whole - wellArea, holed, wellArea * 0.08)
        // floor 2 on its own still shows the stairs coming up through its stairwell
        val alone = Mesh.build(p, 1, 1)
        assertTrue((0 until alone.vertexCount).any { kotlin.math.abs(alone.data[it * Mesh.STRIDE + 6] - 0xB0 / 255f) < 0.005f })
        val one = Mesh.build(p, 0); val two = Mesh.build(p, 1)
        assertTrue(two.vertexCount > one.vertexCount)
        assertEquals(2.9f + 2.7f, two.height, 1e-4f)
        val rooms = com.dawoyan.elektronik.core.BoardBundle.read(Materials.elektronikFile(p, false))!!.boards[0].rooms
        assertEquals(listOf("Floor 1", "Floor 2"), rooms.map { it.zone })
        assertTrue(rooms[1].id.startsWith("rk_t_f1-"))
        // the socket upstairs is reached from the board downstairs: up through the floor
        val socket = Materials.build(p, false).lines.single { it.mm2 == 2.5 }
        assertTrue(socket.meters > 2.9)
        // a basement, and a riser through all floors: the socket upstairs is now fed up the riser
        p = PlanOps.addBasement(p)
        assertEquals(-1..1, p.floorRange); assertEquals(-2700, p.elevation(-1))
        val viaBoard = Materials.build(p, false).lines.single { it.mm2 == 2.5 }.meters
        p = PlanOps.addRiser(p, P(3950.0, 1700.0), 500.0) // next to the socket upstairs
        assertEquals(3, p.allFloors.count { f -> f.points.any { it.kind == PointKind.RISER } })
        val viaRiser = Materials.build(p, false).lines.single { it.mm2 == 2.5 }.meters
        assertTrue(viaRiser < viaBoard) // upstairs the shell has no wall where the low one was: the riser is the short way
        val down = PlanOps.addPoint(p.floor(-1), PointKind.SOCKET, P(50.0, 1500.0), 500.0)!!
        p = p.withFloor(-1, down)
        val rooms2 = com.dawoyan.elektronik.core.BoardBundle.read(Materials.elektronikFile(p, false))!!.boards[0].rooms
        assertEquals(listOf("Floor -1", "Floor 1", "Floor 2"), rooms2.map { it.zone })
        assertTrue(rooms2[0].id.startsWith("rk_t_b1-"))
        assertTrue(Mesh.build(p, -1).groundCount == 0 && Mesh.build(p, 0).groundCount == 6)
        val obj = ObjExport.write(p)
        assertTrue(obj.lines().count { it.startsWith("f ") } > 100)
        assertEquals(p, PlanJson.decode(PlanJson.encode(p)))
    }

    @Test fun systemsAndLayers() {
        var p = PlanOps.addRect(empty, 0, 0, 4000, 3000, 200)
        p = PlanOps.addPipe(p, SystemKind.WATER_HOT, listOf(P(200.0, 200.0), P(3800.0, 200.0), P(3800.0, 2800.0)))
        p = PlanOps.toggleWarmFloor(p, Rooms.detect(p)[0])
        assertTrue(Rooms.detect(p)[0].info!!.warmFloor)
        val all = Mesh.build(p).vertexCount
        val noWater = Mesh.build(p, hidden = setOf(Layer.WATER)).vertexCount
        val noWarm = Mesh.build(p, hidden = setOf(Layer.WATER, Layer.WARM_FLOOR)).vertexCount
        assertTrue(all > noWater && noWater > noWarm)
        assertEquals(p, PlanJson.decode(PlanJson.encode(p)))
    }

    @Test fun roofsAtticAndFences() {
        var p = PlanOps.addRect(empty, 0, 0, 8000, 6000, 200)
        p = PlanOps.addRoof(p, 0, 0, 8000, 6000)
        assertTrue(p.roofs[0].alongX)
        val gable = Roofs.faces(p.roofs[0], 2700)
        assertEquals(4, gable.size)
        // ridge: 3 m to the middle × tan 30° above the 2.7 m walls
        assertEquals(2700 + 3000 * Math.tan(Math.toRadians(30.0)), gable.flatMap { it.pts }.maxOf { it.h }, 1e-6)
        for (k in RoofKind.entries) assertTrue(Roofs.faces(p.roofs[0].copy(kind = k), 2700).isNotEmpty())
        val ceilings = Mesh.build(p).let { it.vertexCount - it.openCount }
        p = PlanOps.updateRoof(p, p.roofs[0].copy(attic = true))
        assertEquals(0, Mesh.build(p).let { it.vertexCount - it.openCount }) // attic: no flat ceiling
        assertTrue(ceilings > 0)
        for (k in FenceKind.entries) {
            val f = PlanOps.addFence(p, k, listOf(P(-2000.0, -2000.0), P(10000.0, -2000.0)))
            assertTrue(Fences.parts(f.fences.last()).isNotEmpty())
        }
        assertEquals(p, PlanJson.decode(PlanJson.encode(p)))
    }

    @Test fun lengthAndDelete() {
        var p = PlanOps.addWall(empty, 0, 0, 3000, 0)
        p = PlanOps.setWallLength(p, p.walls[0].id, 4200)
        assertEquals(4200.0, p.length(p.walls[0]), 0.5)
        p = PlanOps.deleteWall(p, p.walls[0].id)
        assertTrue(p.nodes.isEmpty() && p.walls.isEmpty())
    }

    @Test fun lShapeTriangulatesAndMitres() {
        var p = empty
        val pts = listOf(0 to 0, 5000 to 0, 5000 to 2000, 2000 to 2000, 2000 to 4000, 0 to 4000)
        for (i in pts.indices) {
            val (x1, y1) = pts[i]; val (x2, y2) = pts[(i + 1) % pts.size]
            p = PlanOps.addWall(p, x1, y1, x2, y2, 200)
        }
        val room = Rooms.detect(p).single()
        val tri = Geo.triangulate(room.polygon).sumOf { abs(cross(room.polygon[it[1]] - room.polygon[it[0]], room.polygon[it[2]] - room.polygon[it[0]])) / 2 }
        assertEquals(14e6, tri, 1.0)
        assertEquals(4.8 * 1.8 + 1.8 * 2.0, room.area, 1e-6)
        // every corner is a square mitre: wall outline corners are 100 mm from the centre line in both axes
        for (s in WallShapes.build(p)) for (c in listOf(s.aL to s.a, s.aR to s.a, s.bL to s.b, s.bR to s.b)) {
            assertEquals(100.0, abs(c.first.x - c.second.x), 1e-6)
            assertEquals(100.0, abs(c.first.y - c.second.y), 1e-6)
        }
        val mesh = Mesh.build(p)
        assertTrue(mesh.vertexCount > 100 && mesh.data.size % Mesh.STRIDE == 0)
    }

    @Test fun roofFlowAndResize() {
        // gable along x over y 0..4000: water runs to -y on one side and +y on the other
        val g = Roof(1, 0, 0, 6000, 4000, RoofKind.GABLE, 30, true, false, 500, false)
        val f = Roofs.flows(g)
        assertEquals(2, f.size)
        assertTrue(f.any { it.second.y < -0.99 } && f.any { it.second.y > 0.99 })
        // shed falls toward the low side (v0 = y1 unless flipped)
        val s = g.copy(kind = RoofKind.SHED)
        assertTrue(Roofs.flows(s).single().second.y < -0.99)
        assertTrue(Roofs.flows(s.copy(flip = true)).single().second.y > 0.99)
        assertTrue(Roofs.flows(s.copy(alongX = false)).single().second.x < -0.99)
        assertEquals(4, Roofs.flows(g.copy(kind = RoofKind.HIP)).size)
        assertTrue(Roofs.flows(g.copy(kind = RoofKind.FLAT)).isEmpty())
        // resizing
        val r = Roofs.drag(g, 2, P(7000.0, 5000.0))
        assertEquals(7000, r.x2); assertEquals(5000, r.y2); assertEquals(0, r.x1)
        assertEquals(500, Roofs.drag(g, 7, P(9000.0, 0.0)).let { it.x2 - it.x1 })
        assertEquals(4100, Roofs.drag(g, 4, P(0.0, -100.0)).let { it.y2 - it.y1 })
        assertEquals(100, Roofs.drag(g, 8, P(0.0, 0.0), 100, 0).x1)
    }

    @Test fun roofBaseAndCut() {
        var p = PlanOps.addRect(Plan(id = "t", name = "t"), 0, 0, 6000, 4000)
        p = p.copy(walls = p.walls.map { it.copy(height = 1200) })
        p = PlanOps.addRoof(p, 0, 0, 6000, 4000)
        val r = p.roofs[0]
        // auto base: the top of the highest wall under it, not the floor's 270
        assertEquals(1200, Roofs.baseOf(p, r))
        assertEquals(0, Roofs.baseOf(p, r.copy(base = 0)))
        // gable along x: at the eaves line the surface is the base, at the ridge it is base + half width × tan(pitch)
        val g = r.copy(base = 0, kind = RoofKind.GABLE, alongX = true, pitch = 45)
        assertEquals(0.0, Roofs.surfaceAt(g, 0, P(3000.0, 0.0))!!, 1.0)
        assertEquals(2000.0, Roofs.surfaceAt(g, 0, P(3000.0, 2000.0))!!, 1.0)
        assertEquals(null, Roofs.surfaceAt(g, 0, P(9000.0, 2000.0)))
        // walls under a roof on the floor are cut: no wall vertex above the roof
        val cut = p.copy(roofs = listOf(g))
        val mesh = Mesh.build(cut)
        var maxY = 0f
        for (i in 0 until mesh.vertexCount) if (mesh.data[i * Mesh.STRIDE + 10] % 10f == Mesh.KIND_WALL) maxY = maxOf(maxY, mesh.data[i * Mesh.STRIDE + 1])
        assertTrue(maxY <= 2.0f, "walls cut by the roof, top $maxY")
    }

    @Test fun platforms() {
        var p = PlanOps.addRect(Plan(id = "t", name = "t"), 0, 0, 4000, 4000)
        // a balcony on the outside of the wall y = 4000: three sides free, the wall side has no railing
        p = PlanOps.addPlatform(p, 1000, 4000, 3000, 5200, 0)
        val b = p.platforms.single()
        assertEquals(3, Platforms.openSides(p, b).size)
        assertTrue(Platforms.parts(p, b, -3000.0).isNotEmpty())
        val withCols = Platforms.parts(p, b.copy(columns = true, height = 2900), -3000.0)
        assertTrue(withCols.count { it.z0 == -3000.0 } == 4)
        assertEquals(2.4, Platforms.area(b), 1e-9)
        assertTrue(Mesh.build(p).vertexCount > 0)
    }

    @Test fun fenceEditing() {
        val f = Fence(1, FenceKind.PICKET, listOf(Node(0, 0, 0), Node(1, 4000, 0)))
        assertEquals(3, Fences.handles(f).size)
        val added = Fences.drag(f, 2, P(2000.0, 1000.0), 0, 0)
        assertEquals(listOf(0, 2000, 4000), added.points.map { it.x })
        val moved = Fences.drag(added, -1, P(0.0, 0.0), 500, 100)
        assertEquals(500, moved.points[0].x); assertEquals(100, moved.points[0].y)
        val merged = Fences.drag(added, 1, P(4050.0, 20.0), 0, 0)
        assertEquals(2, merged.points.size)
    }

    @Test fun handDrawnLineIsSquare() {
        // a wobbly L: right then down
        val stroke = (0..20).map { P(it * 200.0, (it % 2) * 30.0) } + (1..15).map { P(4000.0 + (it % 2) * 25, it * 200.0) }
        val pts = Lines.orthogonal(stroke, 150.0)
        for (k in 1 until pts.size) assertTrue(pts[k].x == pts[k - 1].x || pts[k].y == pts[k - 1].y)
        assertTrue(pts.size <= 4)
        assertEquals(stroke.last(), pts.last())
    }

    @Test fun manyBoards() {
        var plan = PlanOps.addRect(Plan("p", "P"), 0, 0, 8000, 4000)
        plan = PlanOps.addPoint(plan, PointKind.BOARD, P(0.0, 1000.0), 500.0)!!
        plan = PlanOps.addPoint(plan, PointKind.BOARD, P(8000.0, 1000.0), 500.0)!!
        plan = plan.copy(points = plan.points.mapIndexed { i, p -> p.copy(name = "B$i") })
        plan = PlanOps.addPoint(plan, PointKind.SOCKET, P(500.0, 4000.0), 500.0)!!
        plan = PlanOps.addPoint(plan, PointKind.SOCKET, P(7500.0, 4000.0), 500.0)!!
        val boards = Boards.all(plan)
        assertEquals(2, boards.size)
        val near = Boards.of(plan, boards, 0, plan.points[2])!!
        assertEquals("B0", near.point.name)
        val projects = Materials.elektronikProjects(plan, false)
        assertEquals(listOf("B0", "B1"), projects.map { it.name })
        // choosing the far board moves the socket there
        val chosen = plan.copy(points = plan.points.map { if (it.id == plan.points[2].id) it.copy(board = boards[1].key) else it })
        assertEquals(2, Boards.only(chosen, boards[1]).points.count { it.kind == PointKind.SOCKET })
    }

    @Test fun editDrawnLine() {
        // an L cable from a device at (0,0) to one at (2000,1000)
        val pts = listOf(P(0.0, 0.0), P(2000.0, 0.0), P(2000.0, 1000.0))
        val hs = Lines.handles(pts, fixedEnds = true)
        assertEquals(1 + 2, hs.size)
        // the turn moves
        assertEquals(P(1500.0, 0.0), Lines.drag(pts, 0, P(1500.0, 0.0), true)[1])
        // the first run moved down: its device end stays, a new turn is made by it
        val moved = Lines.drag(pts, 1, P(1000.0, 400.0), true)
        assertEquals(listOf(P(0.0, 0.0), P(0.0, 400.0), P(2000.0, 400.0), P(2000.0, 1000.0)), moved)
        // a pipe's ends move too
        assertEquals(P(0.0, 300.0), Lines.drag(pts, 0, P(0.0, 300.0), false)[0])
        // the boards: the nearest on the point's own floor
        var root = PlanOps.addRect(empty, 0, 0, 6000, 4000, 200)
        root = PlanOps.addPoint(root, PointKind.BOARD, P(0.0, 2000.0), 1500.0)!!
        val up = PlanOps.addPoint(PlanOps.addRect(empty, 0, 0, 6000, 4000, 200), PointKind.BOARD, P(5800.0, 2000.0), 1500.0)!!
        root = root.copy(floors = listOf(up))
        val sock = ElPoint(99, PointKind.SOCKET, 5800, 2100, height = 300)
        assertEquals(0, Boards.of(root, Boards.all(root), 0, sock)!!.floor)
    }

    @Test fun outerWallsMarked() {
        val p = PlanOps.addRect(empty, 0, 0, 4000, 3000, 200)
        for (m in listOf(Mesh.build(p), Mesh.build(p, 0, 0))) {
            var outer = 0
            for (v in 0 until m.vertexCount) { val k = m.data[v * Mesh.STRIDE + 11]; if (Math.abs(k - Mesh.KIND_OUTER) < 0.01f) outer++ }
            assertTrue(outer > 0, "outer vertices: $outer")
        }
    }

    @Test fun pillarsAndOuterWalls() {
        // two rooms side by side: 7 walls, 6 of them outside, the one between inside
        var p = PlanOps.addRect(empty, 0, 0, 4000, 3000, 300)
        p = PlanOps.addRect(p, 4000, 0, 8000, 3000, 300)
        val outer = Outline.outerWalls(p)
        assertEquals(p.walls.size - 1, outer.size)
        // copied onto an empty floor: the outline only, one room
        val up = PlanOps.copyWalls(empty, p, outer)
        assertEquals(1, Rooms.detect(up).size)
        assertTrue(up.walls.all { it.thickness == 300 })
        // pillars: their centre lines make the grid, with near ones merged
        var q = PlanOps.addPillar(empty, 0, 0)
        q = PlanOps.addPillar(q, 6000, 10, q.pillars[0])
        q = PlanOps.addPillar(q, 6000, 4000)
        assertEquals(listOf(0, 6000) to listOf(0, 4000), Pillars.axes(q.pillars))
        assertTrue(Pillars.covers(q.pillars[0], P(150.0, -150.0)))
        // saved and read back; drawn in 3D
        assertEquals(q.pillars, PlanJson.decode(PlanJson.encode(q)).pillars)
        assertTrue(Mesh.build(q).vertexCount > 0)
    }

    @Test fun junctionsAndLines() {
        var p = PlanOps.addRect(empty, 0, 0, 6000, 4000, 200)
        p = PlanOps.addPoint(p, PointKind.BOARD, P(0.0, 2000.0), 1500.0)!!
        p = PlanOps.addPoint(p, PointKind.SOCKET, P(3000.0, 0.0), 1500.0)!!
        p = PlanOps.addPoint(p, PointKind.SOCKET, P(3000.0, 4000.0), 1500.0)!!
        p = PlanOps.addPoint(p, PointKind.LAMP, P(3000.0, 2000.0), 1500.0)!!
        val (board, s1, s2, lamp) = p.points.map { it.id }
        p = PlanOps.addRoute(p, board, s1, listOf(P(0.0, 100.0)), RouteLevel.CEILING)
        val whole = Cables.length(p, p.routes[0])
        // the second socket joins the first one's cable: a box splits it, three cables meet there
        val (q, box) = Circuits.split(p, p.routes[0].id, P(1500.0, 120.0))!!
        assertEquals(PointKind.JUNCTION, q.points.first { it.id == box }.kind)
        assertEquals(whole, q.routes.sumOf { Cables.length(q, it) }, 1.0)
        assertTrue(!Circuits.mixes(q, box, s2))
        // a lamp may not join a sockets line
        assertTrue(Circuits.mixes(q, box, lamp))
        var j = PlanOps.addRoute(q, s2, box, emptyList(), RouteLevel.CEILING)
        val lines = Circuits.lines(j, board)
        assertEquals(1, lines.size)
        assertEquals(setOf(s1, s2), lines[0].devices.map { it.id }.toSet())
        assertEquals(j.routes.sumOf { Cables.length(j, it) }, lines[0].mm, 1.0)
        // the branch taken away: the box goes and the cable is whole again
        j = PlanOps.deleteRoute(j, j.routes.first { it.from == s2 }.id)
        assertTrue(j.points.none { it.kind == PointKind.JUNCTION })
        assertEquals(1, j.routes.size)
        assertEquals(whole, Cables.length(j, j.routes[0]), 1.0)
        // a socket the cable goes on from loops through its terminals: no box there, only the drawn one
        val chain = PlanOps.addRoute(PlanOps.addRoute(q, s2, box, emptyList(), RouteLevel.CEILING), s1, s2, emptyList(), RouteLevel.CEILING)
        assertTrue(!Circuits.tapped(chain, chain.points.first { it.id == s1 }))
        assertTrue(!Circuits.tapped(chain, chain.points.first { it.id == s2 }))
        assertEquals(1, Circuits.boxes(chain))
        // drawn lengths reach elektronik's lines
        val sent = Materials.elektronikProjects(PlanOps.addRoute(q, s2, box, emptyList(), RouteLevel.CEILING), false)
        assertTrue(sent.flatMap { it.devices }.any { (it.cableM ?: 0.0) > 0 })
    }

    @Test fun arrangeAndSplit() {
        // pillars in a wobbly row lined up and spaced evenly
        var q = empty
        for ((x, y) in listOf(0 to 0, 2900 to 40, 6000 to -30, 9000 to 10)) q = PlanOps.addPillar(q, x, y)
        val ids = Arrange.pillarsIn(q, P(-100.0, -100.0), P(9100.0, 100.0))
        assertEquals(4, ids.size)
        val top = Arrange.alignPillars(q, ids, Arrange.Align.MIN)
        assertTrue(top.pillars.all { it.y == -30 })
        assertEquals(listOf(0, 3000, 6000, 9000), Arrange.spacePillars(top, ids).pillars.map { it.x }.sorted())
        // a wall split in two, then the new corner simplified away again
        val p = PlanOps.addRect(empty, 0, 0, 6000, 4000, 200)
        val top2 = p.walls.first { w -> p.pos(w.a)!!.y == 0.0 && p.pos(w.b)!!.y == 0.0 }
        val (s, corner) = Arrange.split(p, top2.id, P(2500.0, 30.0))!!
        assertEquals(p.walls.size + 1, s.walls.size)
        assertEquals(2500, s.node(corner)!!.x)
        val (back, gone) = Arrange.simplify(s, s.nodes.map { it.id }.toSet())
        assertEquals(1, gone)
        assertEquals(p.walls.size, back.walls.size)
        assertEquals(1, Rooms.detect(back).size)
        // two corners moved together: the room grows
        val right = p.nodes.filter { it.x == 6000 }.map { it.id }.toSet()
        val wide = Arrange.move(p, right, emptySet(), 1000, 0)
        assertEquals(7000, wide.nodes.maxOf { it.x })
        assertEquals(1, Rooms.detect(wide).size)
    }

    @Test fun cableFollowsAnother() {
        // an existing cable turning at (3000, 1000); a new one drawn 80 mm off lies onto it and turns at the same corner
        val old = listOf(P(0.0, 1000.0), P(3000.0, 1000.0), P(3000.0, 4000.0))
        val drawn = listOf(P(0.0, 2000.0), P(0.0, 1080.0), P(2920.0, 1080.0), P(2920.0, 3000.0), P(4000.0, 3000.0))
        val f = Lines.follow(drawn, listOf(old), 120.0)
        assertEquals(listOf(P(0.0, 2000.0), P(0.0, 1000.0), P(3000.0, 1000.0), P(3000.0, 3000.0), P(4000.0, 3000.0)), f)
        // far away: left as drawn
        assertEquals(drawn, Lines.follow(drawn, listOf(old.map { P(it.x + 5000, it.y + 5000) }), 120.0))
    }

    @Test fun deviceToBoard() {
        // board 1 → socket 2 → junction 3 → socket 4; a lamp 5 on its own cable to the board
        val pts = listOf(ElPoint(1, PointKind.BOARD, 0, 0), ElPoint(2, PointKind.SOCKET, 1000, 0), ElPoint(3, PointKind.JUNCTION, 2000, 0),
            ElPoint(4, PointKind.SOCKET, 3000, 0), ElPoint(5, PointKind.LAMP, 0, 2000))
        val routes = listOf(Route(1, 1, 2), Route(2, 2, 3), Route(3, 4, 3), Route(4, 5, 1))
        val p = Plan("t", "t", points = pts, routes = routes)
        assertEquals(listOf(3, 2, 1), Circuits.toBoard(p, 4).map { it.id })
        assertEquals(listOf(4), Circuits.toBoard(p, 5).map { it.id })
        assertEquals(emptyList<Int>(), Circuits.toBoard(p.copy(routes = routes.take(1)), 4).map { it.id })
    }

    @Test fun newCableSize() {
        // board 1, lamp 2 with a box 3 on its cable, socket 4, a 7.5 kW hob 5
        val pts = listOf(ElPoint(1, PointKind.BOARD, 0, 0), ElPoint(2, PointKind.LAMP, 3000, 0), ElPoint(3, PointKind.JUNCTION, 1500, 0),
            ElPoint(4, PointKind.SOCKET, 0, 3000), ElPoint(5, PointKind.APPLIANCE, 3000, 3000, kw = 7.5))
        var p = Plan("t", "t", points = pts)
        p = PlanOps.addRoute(p, 2, 1, emptyList(), RouteLevel.CEILING)
        assertEquals(1.5, p.routes.last().mm2, 0.0)          // light to board
        p = Plan("t", "t", points = pts, routes = listOf(Route(1, 2, 3, mm2 = 1.5)))
        p = PlanOps.addRoute(p, 1, 3, emptyList(), RouteLevel.CEILING)
        assertEquals(1.5, p.routes.last().mm2, 0.0)          // board to the light line's box: the line's size
        p = PlanOps.addRoute(p, 1, 4, emptyList(), RouteLevel.CEILING)
        assertEquals(2.5, p.routes.last().mm2, 0.0)
        p = PlanOps.addRoute(p, 5, 1, emptyList(), RouteLevel.CEILING)
        assertEquals(10.0, p.routes.last().mm2, 0.0)         // the hob by its power
    }

    @Test fun autorouteGoesThroughTheDoor() {
        // two rooms side by side, wall at x = 4000 with a door at y = 4500; board in the left room, socket in the right
        var p = Plan("t", "t")
        p = PlanOps.addWall(p, 0, 0, 8000, 0); p = PlanOps.addWall(p, 8000, 0, 8000, 6000)
        p = PlanOps.addWall(p, 8000, 6000, 0, 6000); p = PlanOps.addWall(p, 0, 6000, 0, 0)
        p = PlanOps.addWall(p, 4000, 0, 4000, 6000)
        p = p.copy(openings = listOf(Opening(1, OpeningKind.DOOR, 4000, 2000, width = 900, height = 2100)),
            points = listOf(ElPoint(1, PointKind.BOARD, 1000, 1000), ElPoint(2, PointKind.SOCKET, 6000, 1000), ElPoint(3, PointKind.SOCKET, 7000, 1500),
                ElPoint(4, PointKind.LAMP, 2000, 3000), ElPoint(5, PointKind.LAMP, 6000, 3000), ElPoint(6, PointKind.SWITCH, 3500, 4000)))
        p = p.copy(points = p.points.map { if (it.kind == PointKind.LAMP) it.copy(switchId = 6) else it })
        val (after, n) = Autoroute.join(p, listOf(2, 3, 4, 5), RouteLevel.FLOOR)
        assertEquals(5, n)   // board→socket, socket→socket, board→switch, switch→lamp ×2
        val wallA = P(4000.0, 0.0); val wallB = P(4000.0, 6000.0)
        for (r in after.routes) {
            val pts = Cables.plan(after, r)!!
            for (k in 1 until pts.size) {
                val a = pts[k - 1]; val b = pts[k]
                assertTrue(abs(a.x - b.x) < 1 || abs(a.y - b.y) < 1, "runs along x and y")
                if ((a.x - 4000) * (b.x - 4000) < 0) assertTrue(abs(a.y - 2000) < 450, "crosses at the door: ${a.y}")
            }
        }
        assertTrue(Circuits.toBoard(after, 5).isNotEmpty() && Circuits.toBoard(after, 3).isNotEmpty())
        assertEquals(1.5, after.routes.first { it.to == 6 || it.from == 6 }.mm2, 0.0)
    }

    /** Two rooms; boards 1 (left room, near) and 2 (far right, outside); sockets 3, 4 in the right room joined to board 2... */
    private fun twoRooms(): Plan {
        var p = Plan("t", "t")
        p = PlanOps.addWall(p, 0, 0, 8000, 0); p = PlanOps.addWall(p, 8000, 0, 8000, 6000)
        p = PlanOps.addWall(p, 8000, 6000, 0, 6000); p = PlanOps.addWall(p, 0, 6000, 0, 0)
        p = PlanOps.addWall(p, 4000, 0, 4000, 6000)
        return p.copy(openings = listOf(Opening(1, OpeningKind.DOOR, 4000, 2000, width = 900, height = 2100)),
            points = listOf(ElPoint(1, PointKind.BOARD, 1000, 1000, uid = "b1"), ElPoint(2, PointKind.BOARD, 9000, 3000, uid = "b2")))
    }

    @Test fun socketJoinsItsRoomsCable() {
        var p = twoRooms()
        // a socket on the right room's top wall, wired to board 1 by choice
        p = p.copy(points = p.points + ElPoint(3, PointKind.SOCKET, 5000, 0, nx = 0.0, ny = 1.0, board = "b1"))
        p = Autoroute.join(p, listOf(3), RouteLevel.FLOOR).first
        assertEquals(1, p.routes.size); assertTrue(p.routes[0].from == 1 || p.routes[0].to == 1)
        // a new socket in that room, nearer board 2, no board asked: it joins socket 3, not a board
        p = p.copy(points = p.points + ElPoint(4, PointKind.SOCKET, 8000, 4000, nx = -1.0, ny = 0.0))
        p = Autoroute.join(p, listOf(4), RouteLevel.FLOOR).first
        val r = p.routes.last()
        assertEquals(setOf(3, 4), setOf(r.from, r.to))
        // a 110 V socket in the room: a cable of its own from the nearest board, green
        p = p.copy(points = p.points + ElPoint(5, PointKind.SOCKET, 7000, 6000, nx = 0.0, ny = -1.0, volts = 110))
        p = Autoroute.join(p, listOf(5), RouteLevel.FLOOR).first
        val r5 = p.routes.last()
        assertTrue(5 in setOf(r5.from, r5.to) && (2 in setOf(r5.from, r5.to) || 1 in setOf(r5.from, r5.to)))
        assertEquals(110, r5.volts)
    }

    @Test fun socketCableKeepsBesideWallsAndGoesByTheDoor() {
        var p = twoRooms()
        p = p.copy(points = p.points + ElPoint(3, PointKind.SOCKET, 6000, 0, nx = 0.0, ny = 1.0, board = "b1") +
            ElPoint(4, PointKind.SOCKET, 7000, 0, nx = 0.0, ny = 1.0))
        p = Autoroute.join(p, listOf(3, 4), RouteLevel.FLOOR).first
        for (r in p.routes) {
            val pts = Cables.plan(p, r)!!
            var crossed = false
            for (k in 1 until pts.size) {
                val a = pts[k - 1]; val b = pts[k]; val n = ((b - a).len / 20).toInt().coerceAtLeast(1)
                for (i in 0..n) {
                    val q = a + (b - a) * (i.toDouble() / n)
                    if ((q - pts.first()).len < 200 || (q - pts.last()).len < 200) continue
                    for (w in p.walls) {
                        val wa = p.pos(w.a)!!; val wb = p.pos(w.b)!!
                        if (Geo.distToSegment(q, wa, wb) > w.thickness / 2.0) continue
                        // only across the partition, and only at its door
                        assertEquals(4000.0, wa.x, 1.0); assertEquals(4000.0, wb.x, 1.0)
                        assertTrue(q.y in 1550.0..2450.0, "crosses at y=${q.y}"); crossed = true
                    }
                }
            }
            if (setOf(r.from, r.to) == setOf(1, 3)) assertTrue(crossed)
        }
        // the second socket joins the first, not the board
        assertTrue(p.routes.any { setOf(it.from, it.to) == setOf(3, 4) })
    }

    @Test fun groundRoundTheHouse() {
        var p = PlanOps.addRect(Plan("t", "t"), 0, 0, 4000, 3000, 200)
        p = PlanOps.addBasement(p).copy(groundLevel = 300)
        val fence = Fence(1, FenceKind.BOARD, listOf(Node(0, 6000, 0), Node(1, 6000, 5000)))
        // a fence and a car on the ground level, a fence drawn on the basement
        val site = PlanOps.addFence(p.floor(SITE), FenceKind.BOARD, listOf(P(6000.0, 0.0), P(6000.0, 5000.0)))
        p = p.withFloor(SITE, PlanOps.addItem(site, "car", 9000, 2000))
        p = p.withFloor(-1, p.floor(-1).copy(fences = listOf(fence.copy(points = fence.points.map { it.copy(x = 7000) }))))
        assertEquals(-300, p.elevation(SITE))
        // editing the first floor keeps the ground
        p = p.withFloor(0, PlanOps.addWall(p.floor(0), 0, 0, 100, 0))
        assertEquals(1, p.site!!.fences.size); assertEquals(1, p.site!!.items.size)
        assertEquals(p, PlanJson.decode(PlanJson.encode(p)))
        fun ys(m: Mesh, x0: Float, x1: Float) = (0 until m.vertexCount).filter { m.data[it * 13] in x0..x1 }.map { m.data[it * 13 + 1] }
        for ((lo, hi) in listOf(-1 to 0, 0 to 0)) {
            val m = Mesh.build(p, hi, lo)
            // both fences on the ground, whichever level they were drawn on, the car too
            assertEquals(-0.3f, ys(m, 5.8f, 6.2f).min(), 1e-3f); assertEquals(-0.3f, ys(m, 6.8f, 7.2f).min(), 1e-3f)
            assertEquals(-0.3f, ys(m, 8.0f, 10.0f).min(), 0.05f)
        }
    }

    @Test fun fenceUpstairsStandsOnItsFloorOrRoof() {
        var p = PlanOps.addRect(Plan("t", "t"), 0, 0, 4000, 3000, 200)
        p = PlanOps.addFloor(p)
        val fence = Fence(1, FenceKind.BARS, listOf(Node(0, 0, 0), Node(1, 4000, 0), Node(2, 4000, 3000)))
        // a railing round a terrace on the 2nd floor: on that floor
        val up = p.withFloor(1, p.floor(1).copy(fences = listOf(fence)))
        assertEquals(0.0, Fences.standsAt(up, 1, fence), 1e-6)
        // drawn on the 1st floor round its flat roof: on top of the roof
        val roofed = p.withFloor(0, p.floor(0).copy(fences = listOf(fence), roofs = listOf(Roof(1, 0, 0, 4000, 3000, RoofKind.FLAT, pitch = 0))))
        assertTrue(Fences.standsAt(roofed, 0, fence) >= p.wallHeight - 1)   // round the 1st floor's flat roof: up on it
        assertEquals(0.0, Fences.standsAt(p.withFloor(0, p.copy(fences = listOf(fence.copy(points = fence.points.map { it.copy(x = it.x + 2000) })))), 0, fence.copy(points = fence.points.map { it.copy(x = it.x + 2000) })), 1e-6)
        val roofedUp = p.withFloor(1, p.floor(1).copy(fences = listOf(fence), roofs = listOf(Roof(1, 0, 0, 4000, 3000, RoofKind.FLAT, pitch = 0))))
        assertTrue(Fences.standsAt(roofedUp, 1, fence) >= p.floor(1).wallHeight - 1)
        val m = Mesh.build(up, 1)
        val ys = (0 until m.vertexCount).filter { m.data[it * 13 + 3] == 0f && m.data[it * 13] in 3.9f..4.1f && m.data[it * 13 + 2] in 1.0f..2.0f }.map { m.data[it * 13 + 1] }
        assertTrue(ys.isNotEmpty() && ys.max() > up.elevation(1) / 1000f + 1.0f)
    }

    @Test fun gateKeepsToItsSide() {
        val f = Fence(1, FenceKind.BARS, listOf(Node(0, 5800, -500), Node(1, 5800, 7100), Node(2, -50, 7100)), gates = listOf(Gate(7700, 3500)))
        val (g0, g1) = Fences.openings(f).single()
        assertEquals(7600.0, g0, 1.0); assertEquals(11100.0, g1, 1.0)   // moved off the corner onto the bottom side
        assertEquals(2, Fences.pieces(f).size)
        assertTrue(Fences.parts(f).isNotEmpty())
    }

    @Test fun socketGivenAnotherBoardIsRewired() {
        var p = twoRooms()
        p = p.copy(points = p.points + ElPoint(3, PointKind.SOCKET, 7000, 0, nx = 0.0, ny = 1.0))
        p = Autoroute.join(p, listOf(3), RouteLevel.FLOOR).first
        assertTrue(p.routes.single().let { it.from == 2 || it.to == 2 })   // nearest: board 2
        p = PlanOps.updatePoint(p, p.points.first { it.id == 3 }.copy(board = "b1"))
        p = Autoroute.rewire(p, 3, RouteLevel.FLOOR)
        assertTrue(p.routes.single().let { it.from == 1 || it.to == 1 })
    }

    @Test fun autorouteIsQuick() {
        var p = Plan("t", "t")
        for (i in 0..6) { p = PlanOps.addWall(p, i * 3000, 0, i * 3000, 18000); p = PlanOps.addWall(p, 0, i * 3000, 18000, i * 3000) }
        p = p.copy(points = listOf(ElPoint(1, PointKind.BOARD, 100, 100)) + (2..20).map { ElPoint(it, PointKind.SOCKET, 1500 + (it % 6) * 3000, 1500 + (it / 6) * 3000) })
        val t0 = System.nanoTime()
        val (_, n) = Autoroute.join(p, (2..20).toList(), RouteLevel.FLOOR)
        println("autoroute: $n cables in ${(System.nanoTime() - t0) / 1_000_000} ms")
    }

    @Test fun movedLampKeepsCableSquare() {
        var p = Plan("t", "t", points = listOf(ElPoint(1, PointKind.LAMP, 0, 0), ElPoint(2, PointKind.LAMP, 3000, 2000)),
            routes = listOf(Route(1, 1, 2, via = listOf(Node(0, 3000, 0)), mm2 = 1.5)))
        p = Arrange.alignPoints(p, setOf(1, 2), Arrange.Align.MIN)   // a row: both to y = 0
        p = PlanOps.movePoint(p, 1, P(0.0, 500.0), 300.0)
        val pts = Cables.plan(p, p.routes[0])!!
        for (k in 1 until pts.size) assertTrue(abs(pts[k - 1].x - pts[k].x) < 1 || abs(pts[k - 1].y - pts[k].y) < 1, "square: $pts")
    }

    @Test fun inLinePointsAreTakenOut() {
        val p = Plan("t", "t", nodes = listOf(Node(1, 0, 0), Node(2, 2000, 5), Node(3, 4000, 0), Node(4, 4000, 3000), Node(5, 2000, 3000)),
            walls = listOf(Wall(10, 1, 2), Wall(11, 2, 3), Wall(12, 3, 4), Wall(13, 4, 5), Wall(14, 2, 5)),
            fences = listOf(Fence(1, FenceKind.BARS, listOf(Node(0, 0, -5000), Node(0, 3000, -5000), Node(0, 6000, -5000), Node(0, 6000, -8000)))))
        val t = Tidy.straighten(p)
        // corner 2 starts another wall (14): it stays
        assertEquals(5, t.nodes.size)
        assertEquals(3, t.fences[0].points.size)
        val q = Tidy.straighten(p.copy(walls = p.walls.filter { it.id != 14 }))
        assertEquals(4, q.nodes.size); assertEquals(3, q.walls.size)
    }

    @Test fun pillarOffTheOneBelowIsFound() {
        val below = listOf(Pillar(1, 0, 0), Pillar(2, 6000, 0))
        val up = listOf(Pillar(1, 10, 0), Pillar(2, 6300, 0))
        assertEquals(setOf(2), Pillars.offBelow(up, below))
        assertEquals(emptySet<Int>(), Pillars.offBelow(up, emptyList()))
    }

    @Test fun onlyPillarsInsideAWallAreHidden() {
        val p = Plan("t", "t", nodes = listOf(Node(1, 0, 0), Node(2, 4000, 0)), walls = listOf(Wall(10, 1, 2, thickness = 400)))
        assertTrue(Pillars.inWall(p, Pillar(1, 2000, 0, 300, 300)))
        assertTrue(!Pillars.inWall(p, Pillar(2, 2000, 0, 500, 500)))
        assertTrue(!Pillars.inWall(p, Pillar(3, 4000, 0, 300, 300)))
    }

    @Test fun boardFedFromAnotherFloorGoesAcrossThenUp() {
        val up = Plan("u", "u", points = listOf(ElPoint(1, PointKind.BOARD, 3000, 4000, height = 1500, uid = "b2", source = "b1")))
        val root = Plan("t", "t", points = listOf(ElPoint(1, PointKind.BOARD, 0, 0, height = 1500, uid = "b1")), floors = listOf(up))
        val f = Feeds.all(root).single()
        assertEquals(1, f.dir)
        assertEquals(7000.0, f.across, 1.0)
        assertEquals(2900.0, f.vertical, 1.0)
        assertEquals(3, f.path.size)
        // a board cannot be fed from one it feeds
        val b1 = Boards.all(root).first { it.key == "b1" }
        assertTrue(Feeds.candidates(root, b1).isEmpty())
        val bom = Materials.build(root, false)
        assertTrue(bom.cables.any { it.mm2 == 6.0 && it.net >= 9.8 })
    }

    @Test fun boardsOnTheGroundAndTwinsCanFeed() {
        val site = Plan("s", "s", points = listOf(ElPoint(1, PointKind.BOARD, 9000, 0, height = 1200, uid = "gate")))
        val up = Plan("u", "u", points = listOf(ElPoint(1, PointKind.BOARD, 3000, 4000, uid = "same")))
        val root = Plan("t", "t", points = listOf(ElPoint(1, PointKind.BOARD, 0, 0, uid = "same"), ElPoint(2, PointKind.BOARD, 500, 0)), floors = listOf(up), site = site)
        val boards = Boards.all(root)
        assertEquals(4, boards.size)
        val upper = boards.first { it.floor == 1 }
        // the ground's board and both on the first floor, even the one with the same id
        assertEquals(3, Feeds.candidates(root, upper).size)
        // fed from the ground's board: it comes up to the first floor
        val fed = root.copy(floors = listOf(up.copy(points = up.points.map { it.copy(source = "gate") })))
        assertEquals(1, Feeds.all(fed).single().dir)
        val fixed = Boards.fixUids(root)
        assertEquals(4, Boards.all(fixed).map { it.key }.distinct().size)
    }

    @Test fun pillarsPutOverTheOnesBelow() {
        val below = listOf(Pillar(1, 0, 0), Pillar(2, 6000, 0))
        val up = listOf(Pillar(1, 40, -30), Pillar(2, 6300, 120), Pillar(3, 20000, 0))
        val (ps, n) = Pillars.alignToBelow(up, below, setOf(1, 2, 3))
        assertEquals(2, n)
        assertEquals(listOf(0 to 0, 6000 to 0, 20000 to 0), ps.map { it.x to it.y })
        assertTrue(Pillars.offBelow(ps, below) == setOf(3))
    }

    @Test fun pillarOffItsAxisGoesOnIt() {
        val here = listOf(Pillar(1, 0, 0), Pillar(2, 4000, 0), Pillar(3, 8060, 40), Pillar(4, 8000, 5000))
        val other = listOf(Pillar(10, 8000, 0), Pillar(11, 4000, 5000))
        assertEquals(8000 to 0, Pillars.axisTarget(here[2], here - here[2] + other))
        assertEquals(null to null, Pillars.axisTarget(here[1], here - here[1] + other))
        val (ps, n) = Pillars.centerOnAxes(here, other, setOf(1, 2, 3, 4))
        assertEquals(1, n)
        assertEquals(8000 to 0, ps[2].x to ps[2].y)
    }

    @Test fun pillarsLinedUpOnEveryFloor() {
        val up = Plan("u", "u", pillars = listOf(Pillar(1, 0, 60), Pillar(2, 4000, -40)))
        val root = Plan("t", "t", pillars = listOf(Pillar(1, 0, 0), Pillar(2, 4000, 20)), floors = listOf(up))
        val picks = mapOf(0 to (emptySet<Int>() to setOf(1, 2)), 1 to (emptySet<Int>() to setOf(1, 2)))
        assertTrue(Arrange.isRowAcross(root, picks))
        val r = Arrange.alignAcross(root, picks, Arrange.Align.MIN)
        assertEquals(listOf(-40, -40, -40, -40), (r.pillars + r.floor(1).pillars).map { it.y })
        val m = Arrange.alignAcross(root, picks, Arrange.Align.MID)
        assertEquals(setOf(10), (m.pillars + m.floor(1).pillars).map { it.y }.toSet())
    }

    @Test fun pillarsAndCornersOnAxesOnEveryFloor() {
        val up = Plan("u", "u", pillars = listOf(Pillar(1, 4090, 0)), nodes = listOf(Node(1, 30, 5000), Node(2, 4000, 5000)), walls = listOf(Wall(1, 1, 2)))
        val root = Plan("t", "t", pillars = listOf(Pillar(1, 0, 0), Pillar(2, 4000, 0), Pillar(3, 0, 5000)), floors = listOf(up))
        val (r, n) = Pillars.centerOnAxesAcross(root, mapOf(1 to setOf(1)))
        assertEquals(1, n); assertEquals(4000, r.floor(1).pillars[0].x)
        val (r2, m) = Arrange.nodesToAxes(r, mapOf(1 to setOf(1, 2)))
        assertEquals(1, m)
        assertTrue(r2.floor(1).nodes.any { it.x == 0 && it.y == 5000 })
        val down = Plan("t", "t", pillars = listOf(Pillar(1, 0, 0)), floors = listOf(Plan("u", "u", pillars = listOf(Pillar(1, 100, 50)))))
        assertEquals(0 to 0, Pillars.alignToBelowAcross(down, mapOf(1 to setOf(1))).first.floor(1).pillars[0].let { it.x to it.y })
    }

    @Test fun feedCableTurnsCanBeChanged() {
        val up = Plan("u", "u", points = listOf(ElPoint(1, PointKind.BOARD, 3000, 4000, uid = "b2", source = "b1")))
        val root = Plan("t", "t", points = listOf(ElPoint(1, PointKind.BOARD, 0, 0, uid = "b1")), floors = listOf(up))
        val f = Feeds.of(root, 1, 1)!!
        val r2 = Feeds.setBoard(root, 1, 1) { it.copy(feedVia = listOf(Node(0, 0, 4000))) }
        val g = Feeds.of(r2, 1, 1)!!
        assertEquals(listOf(P(0.0, 0.0), P(0.0, 4000.0), P(3000.0, 4000.0)), g.path)
        assertEquals(f.across, g.across, 1.0)
        val dragged = Lines.drag(g.path, 0, P(1000.0, 4000.0), fixedEnds = true)
        assertEquals(listOf(Node(0, 1000, 4000)), Feeds.via(dragged).take(1))
    }

    @Test fun alignByTheOldest() {
        val p = Plan("t", "t", pillars = listOf(Pillar(5, 0, 120), Pillar(2, 3000, 40), Pillar(9, 6000, 0)))
        val r = Arrange.alignPillars(p, setOf(5, 2, 9), Arrange.Align.OLDEST)
        assertEquals(listOf(40, 40, 40), r.pillars.map { it.y })
    }

    @Test fun socketsOutsideRoomsGoOnFromTheOneBefore() {
        // a garage drawn open: one wall, a board, sockets put one after another along it
        var p = Plan("t", "t", nodes = listOf(Node(1, 0, 0), Node(2, 10000, 0)), walls = listOf(Wall(1, 1, 2)),
            points = listOf(ElPoint(1, PointKind.BOARD, 9500, 100, nx = 0.0, ny = 1.0, uid = "b")))
        for ((i, x) in listOf(8000, 6000, 3000, 1000).withIndex()) {
            p = p.copy(points = p.points + ElPoint(10 + i, PointKind.SOCKET, x, 100, nx = 0.0, ny = 1.0))
            p = Autoroute.join(p, listOf(10 + i), RouteLevel.FLOOR).first
        }
        assertEquals(4, p.routes.size)
        // only the first from the board, then each from the one before
        assertEquals(1, p.routes.count { it.from == 1 || it.to == 1 })
        for (i in 1..3) assertTrue(p.routes.any { setOf(it.from, it.to) == setOf(9 + i, 10 + i) })
        // two switches there: the second goes on from the first
        p = p.copy(points = p.points + ElPoint(20, PointKind.SWITCH, 7000, 100, nx = 0.0, ny = 1.0) + ElPoint(21, PointKind.SWITCH, 2000, 100, nx = 0.0, ny = 1.0))
        p = Autoroute.join(p, listOf(20, 21), RouteLevel.FLOOR).first
        assertTrue(p.routes.any { setOf(it.from, it.to) == setOf(20, 21) })
        assertEquals(1, p.routes.count { (it.from == 1 || it.to == 1) && (20 in setOf(it.from, it.to) || 21 in setOf(it.from, it.to)) })
        // a socket marked "own line": straight from the board
        p = p.copy(points = p.points + ElPoint(30, PointKind.SOCKET, 4000, 100, nx = 0.0, ny = 1.0, ownLine = true))
        p = Autoroute.join(p, listOf(30), RouteLevel.FLOOR).first
        assertEquals(setOf(1, 30), p.routes.last().let { setOf(it.from, it.to) })
    }

    @Test fun socketsEachFromTheBoardAreRelaidAsOneCable() {
        var p = Plan("t", "t", nodes = listOf(Node(1, 0, 0), Node(2, 10000, 0)), walls = listOf(Wall(1, 1, 2)),
            points = listOf(ElPoint(1, PointKind.BOARD, 9500, 100, nx = 0.0, ny = 1.0, uid = "b")) +
                listOf(8000, 6000, 3000, 1000).mapIndexed { i, x -> ElPoint(10 + i, PointKind.SOCKET, x, 100, nx = 0.0, ny = 1.0) })
        // as an old version laid them: each its own cable from the board
        for (i in 0..3) p = PlanOps.addRoute(p, 1, 10 + i, emptyList(), RouteLevel.FLOOR)
        p = Autoroute.rewire(p, listOf(10, 11, 12, 13), RouteLevel.FLOOR)
        assertEquals(4, p.routes.size)
        assertEquals(1, p.routes.count { it.from == 1 || it.to == 1 })
    }

    @Test fun oneBoardGoesToElektronikWithAllItsLoads() {
        var p = twoRooms()
        // a socket in each room, each given to board 2, and one lamp left to board 1
        p = p.copy(points = p.points + ElPoint(3, PointKind.SOCKET, 1000, 100, nx = 0.0, ny = 1.0, board = "b2") +
            ElPoint(4, PointKind.SOCKET, 6000, 100, nx = 0.0, ny = 1.0, board = "b2") + ElPoint(5, PointKind.LAMP, 2000, 3000, board = "b1"))
        val b2 = Boards.all(p).first { it.key == "b2" }
        val text = Materials.elektronikBoardFile(p, b2, false)
        val board = com.dawoyan.elektronik.core.Roomik.singleBoard(text)!!
        assertEquals("t:b2", board.roomikBoard)
        // its sockets and their back boxes to buy, for elektronik's list
        assertTrue(board.materials.any { it.name.startsWith("Socket") && it.qty == 2.0 })
        assertTrue(board.materials.any { it.name.startsWith("Back box") })
        // both rooms' sockets on it, wherever they are; no lights (they are board 1's)
        assertEquals(2, board.rooms.sumOf { r -> r.loads.filter { it.kind == com.dawoyan.elektronik.core.LoadKind.SOCKETS }.sumOf { it.count } })
        assertTrue(board.rooms.none { r -> r.loads.any { it.kind == com.dawoyan.elektronik.core.LoadKind.LIGHTS } })
        // the whole plan is not a single board
        assertEquals(null, com.dawoyan.elektronik.core.Roomik.singleBoard(Materials.elektronikFile(p, false)))
    }

    @Test fun boardExportKeepsLampWithItsSwitchEvenWhenAnotherBoardIsNearer() {
        // A light may hang nearer to board b1, while its controlling switch is deliberately fed by b2.
        // It must appear in b2's Elektronik export: the switch, not the lamp's coordinates, owns that circuit.
        var p = Plan("t", "t", nodes = listOf(Node(1, 0, 0), Node(2, 10000, 0)), walls = listOf(Wall(1, 1, 2)),
            points = listOf(
                ElPoint(1, PointKind.BOARD, 0, 0, nx = 0.0, ny = 1.0, uid = "b1"),
                ElPoint(2, PointKind.BOARD, 10000, 0, nx = 0.0, ny = 1.0, uid = "b2"),
                ElPoint(3, PointKind.SWITCH, 8500, 0, nx = 0.0, ny = 1.0, board = "b2"),
                ElPoint(4, PointKind.LAMP, 1000, 1000, switchId = 3)))
        val b2 = Boards.all(p).first { it.key == "b2" }
        val board = com.dawoyan.elektronik.core.Roomik.singleBoard(Materials.elektronikBoardFile(p, b2, false))!!
        assertEquals(1, board.rooms.flatMap { it.loads }.filter { it.kind == com.dawoyan.elektronik.core.LoadKind.LIGHTS }.sumOf { it.count })
    }

    @Test fun boardExportRequiresExplicitChoiceInMultiBoardPlan() {
        // A room/place is only a name. With more than one board, a device with no chosen board is not silently
        // claimed by the nearest board or by a shared feeder cable.
        var p = Plan("t", "t", nodes = listOf(Node(1, 0, 0), Node(2, 10000, 0)), walls = listOf(Wall(1, 1, 2)),
            points = listOf(
                ElPoint(1, PointKind.BOARD, 0, 0, nx = 0.0, ny = 1.0, uid = "b1"),
                ElPoint(2, PointKind.BOARD, 10000, 0, nx = 0.0, ny = 1.0, uid = "b2"),
                ElPoint(3, PointKind.SOCKET, 8500, 0, nx = 0.0, ny = 1.0)))
        p = PlanOps.addRoute(p, 1, 3, emptyList(), RouteLevel.FLOOR)
        val b1 = Boards.all(p).first { it.key == "b1" }
        val b2 = Boards.all(p).first { it.key == "b2" }
        fun outlets(b: Boards.Ref) = com.dawoyan.elektronik.core.Roomik.singleBoard(Materials.elektronikBoardFile(p, b, false))!!
            .rooms.flatMap { it.loads }.filter { it.kind == com.dawoyan.elektronik.core.LoadKind.SOCKETS }.sumOf { it.count }
        assertEquals(0, outlets(b1))
        assertEquals(0, outlets(b2))
    }

    @Test fun boardExportUsesExplicitBoardEvenWhenCableElsewhere() {
        var p = Plan("t", "t", nodes = listOf(Node(1, 0, 0), Node(2, 10000, 0)), walls = listOf(Wall(1, 1, 2)),
            points = listOf(
                ElPoint(1, PointKind.BOARD, 0, 0, nx = 0.0, ny = 1.0, uid = "house"),
                ElPoint(2, PointKind.BOARD, 10000, 0, nx = 0.0, ny = 1.0, uid = "garage"),
                // The load is explicitly assigned to garage; its room and an old cable route do not change that.
                ElPoint(3, PointKind.SOCKET, 8500, 0, nx = 0.0, ny = 1.0, board = "garage")))
        p = PlanOps.addRoute(p, 1, 3, emptyList(), RouteLevel.FLOOR)
        val garage = Boards.all(p).first { it.key == "garage" }
        val sent = com.dawoyan.elektronik.core.Roomik.singleBoard(Materials.elektronikBoardFile(p, garage, false))!!
        assertEquals(1, sent.rooms.flatMap { it.loads }.filter { it.kind == com.dawoyan.elektronik.core.LoadKind.SOCKETS }.sumOf { it.count })
    }

    @Test fun linesAreNumberedOnTheirBoard() {
        var p = Plan("t", "t", nodes = listOf(Node(1, 0, 0), Node(2, 10000, 0)), walls = listOf(Wall(1, 1, 2)),
            points = listOf(ElPoint(1, PointKind.BOARD, 9500, 100, nx = 0.0, ny = 1.0, uid = "b"),
                ElPoint(2, PointKind.SOCKET, 8000, 100, nx = 0.0, ny = 1.0), ElPoint(3, PointKind.SOCKET, 6000, 100, nx = 0.0, ny = 1.0),
                ElPoint(4, PointKind.SOCKET, 3000, 100, nx = 0.0, ny = 1.0, ownLine = true)))
        p = Autoroute.join(p, listOf(2, 3, 4), RouteLevel.FLOOR).first
        val n = Circuits.numbers(p)
        // sockets 2 and 3 are one line, 4 has its own
        val a = n[p.routes.first { 2 in setOf(it.from, it.to) && 1 in setOf(it.from, it.to) }.id]
        assertEquals(a, n[p.routes.first { setOf(it.from, it.to) == setOf(2, 3) }.id])
        assertEquals(setOf(1, 2), n.values.toSet())
        // a number given to a line is kept, the other takes the next free one
        val own = p.routes.first { 4 in setOf(it.from, it.to) }
        p = p.copy(routes = p.routes.map { if (it.id == own.id) it.copy(num = 1) else it })
        assertEquals(1, Circuits.numbers(p)[own.id]); assertEquals(2, Circuits.numbers(p)[p.routes.first { setOf(it.from, it.to) == setOf(2, 3) }.id])
    }

    @Test fun lampTakesTheNearestSwitchAndTwoWayPairsAreWired() {
        var p = Plan("t", "t", nodes = listOf(Node(1, 0, 0), Node(2, 10000, 0)), walls = listOf(Wall(1, 1, 2)),
            points = listOf(ElPoint(1, PointKind.BOARD, 9500, 100, nx = 0.0, ny = 1.0, uid = "b"),
                ElPoint(2, PointKind.SWITCH, 8000, 100, nx = 0.0, ny = 1.0), ElPoint(3, PointKind.SWITCH, 1000, 100, nx = 0.0, ny = 1.0),
                ElPoint(4, PointKind.LAMP, 2000, 2000)))
        p = PlanOps.linkNearestSwitch(p, 4)
        assertEquals(3, p.points.first { it.id == 4 }.switchId)
        assertTrue(Circuits.hasLamps(p, p.points.first { it.id == 3 }))
        assertTrue(!Circuits.hasLamps(p, p.points.first { it.id == 2 }))
        // 2 and 3 a two-way pair: both "have" the lamp; wired board → 2 → 3 → lamp
        p = PlanOps.pairSwitches(p, 2, 3)
        assertTrue(Circuits.hasLamps(p, p.points.first { it.id == 2 }))
        p = Autoroute.join(p, listOf(4), RouteLevel.CEILING).first
        assertTrue(p.routes.any { setOf(it.from, it.to) == setOf(2, 3) })
        assertTrue(p.routes.any { setOf(it.from, it.to) == setOf(3, 4) })
        // the travellers between the pair: 4 wires; the rest of the line keeps 3
        assertEquals(4, p.routes.first { setOf(it.from, it.to) == setOf(2, 3) }.cores)
        assertEquals(3, p.routes.first { setOf(it.from, it.to) == setOf(3, 4) }.cores)
        assertEquals(3, p.routes.first { 1 in setOf(it.from, it.to) }.cores)
        assertEquals(1, p.routes.count { 1 in setOf(it.from, it.to) })
        // unpaired again
        p = PlanOps.pairSwitches(p, 2, 2)
        assertTrue(p.points.none { it.pairId != null })
    }

    @Test fun aBoardOnTheGroundSendsItsLoadsAndFeeds() {
        // a garage on the ground: one wall, its board fed from the house board, sockets and lamps
        val site = Plan("s", "s", nodes = listOf(Node(1, 0, 0), Node(2, 10000, 0)), walls = listOf(Wall(1, 1, 2)),
            points = listOf(ElPoint(1, PointKind.BOARD, 9500, 100, nx = 0.0, ny = 1.0, uid = "gar", name = "гараж", source = "house"),
                ElPoint(2, PointKind.SOCKET, 8000, 100, nx = 0.0, ny = 1.0, board = "gar"), ElPoint(3, PointKind.SOCKET, 6000, 100, nx = 0.0, ny = 1.0, board = "gar"),
                ElPoint(4, PointKind.LAMP, 5000, 2000, board = "gar")))
        val root = Plan("t", "t", points = listOf(ElPoint(1, PointKind.BOARD, 0, 0, uid = "house", name = "дом")), site = site)
        val gar = Boards.all(root).first { it.key == "gar" }
        val board = com.dawoyan.elektronik.core.Roomik.singleBoard(Materials.elektronikBoardFile(root, gar, true))!!
        val loads = board.rooms.flatMap { it.loads }
        assertEquals(2, loads.filter { it.kind == com.dawoyan.elektronik.core.LoadKind.SOCKETS }.sumOf { it.count })
        assertEquals(1, loads.filter { it.kind == com.dawoyan.elektronik.core.LoadKind.LIGHTS }.sumOf { it.count })
        assertTrue(board.devices.isNotEmpty())
        // the house board has a line of its own to the garage board
        val house = Boards.all(root).first { it.key == "house" }
        val hb = com.dawoyan.elektronik.core.Roomik.singleBoard(Materials.elektronikBoardFile(root, house, true))!!
        assertTrue(hb.rooms.flatMap { it.loads }.any { it.id == "feed-gar" && it.ownLine })
        assertTrue(hb.materials.any { it.nameRu.startsWith("Питающий") })
    }

    @Test fun aSocketLineDrawnToTheBoardHasItsOwnBreaker() {
        var p = Plan("t", "t", nodes = listOf(Node(1, 0, 0), Node(2, 10000, 0)), walls = listOf(Wall(1, 1, 2)),
            points = listOf(ElPoint(1, PointKind.BOARD, 9500, 100, nx = 0.0, ny = 1.0, uid = "b"),
                ElPoint(2, PointKind.SOCKET, 8000, 100, nx = 0.0, ny = 1.0), ElPoint(3, PointKind.SOCKET, 6000, 100, nx = 0.0, ny = 1.0),
                ElPoint(4, PointKind.SOCKET, 3000, 100, nx = 0.0, ny = 1.0, ownLine = true)))
        fun socketBreakers(plan: Plan): Int {
            val board = com.dawoyan.elektronik.core.Roomik.singleBoard(Materials.elektronikBoardFile(plan, Boards.all(plan).first(), false))!!
            return board.devices.count { d -> d.loads.any { it.kind == com.dawoyan.elektronik.core.LoadKind.SOCKETS } }
        }
        // not drawn yet: the three sockets share one line
        assertEquals(1, socketBreakers(p))
        // drawn: 2 and 3 on one cable, 4 on a cable of its own from the board → two breakers
        p = Autoroute.join(p, listOf(2, 3, 4), RouteLevel.FLOOR).first
        assertEquals(2, socketBreakers(p))
    }

    @Test fun aModelFromIdea3dComesInWithItsColoursAndAxes() {
        // a 3MF as Idea 3D writes it: a flat roof sheet 2000 (X) × 1000 (Z, depth) at height Y = 2400, 50 thick, red
        fun box(x0: Int, y0: Int, z0: Int, x1: Int, y1: Int, z1: Int): Pair<String, String> {
            val v = listOf(x0 to y0 to z0, x1 to y0 to z0, x1 to y1 to z0, x0 to y1 to z0, x0 to y0 to z1, x1 to y0 to z1, x1 to y1 to z1, x0 to y1 to z1)
                .joinToString("") { (xy, z) -> "<vertex x=\"${xy.first}\" y=\"${xy.second}\" z=\"$z\"/>" }
            val t = listOf(0,2,1, 0,3,2, 4,5,6, 4,6,7, 0,1,5, 0,5,4, 1,2,6, 1,6,5, 2,3,7, 2,7,6, 3,0,4, 3,4,7).chunked(3)
                .joinToString("") { "<triangle v1=\"${it[0]}\" v2=\"${it[1]}\" v3=\"${it[2]}\"/>" }
            return v to t
        }
        val (rv, rt) = box(0, 2400, 0, 2000, 2450, 1000)
        val (pv, pt) = box(0, 0, 0, 100, 2400, 100)
        val xml = """<?xml version="1.0" encoding="UTF-8"?><model unit="millimeter"><resources><basematerials id="1">
            <base name="Roof" displaycolor="#C0392BFF"/><base name="Post" displaycolor="#555555FF"/></basematerials>
            <object id="2" type="model" name="Roof" pid="1" pindex="0"><mesh><vertices>$rv</vertices><triangles>$rt</triangles></mesh></object>
            <object id="3" type="model" name="Post" pid="1" pindex="1"><mesh><vertices>$pv</vertices><triangles>$pt</triangles></mesh></object>
            </resources><build/></model>"""
        val bytes = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z -> z.putNextEntry(java.util.zip.ZipEntry("3D/3dmodel.model")); z.write(xml.toByteArray()); z.closeEntry() }
        }.toByteArray()
        val parts = Models.read("Shed.3mf", bytes)
        assertEquals(listOf("Roof", "Post"), parts.map { it.name })
        assertEquals(0xC0392B, parts[0].color)
        val m = Model3D(1, "Shed", 5000, 3000, parts = parts)
        val (w, d, h) = Models.size(m)
        // their Z is our depth (y), their Y our height
        assertEquals(2000.0, w, 1.0); assertEquals(1000.0, d, 1.0); assertEquals(2450.0, h, 1.0)
        val roof = Models.measure(parts[0])
        assertEquals(2.0, roof.top / 1e6, 0.01)
        assertEquals(2000.0, roof.w, 1.0); assertEquals(50.0, roof.h, 1.0)
        // the roof's top faces up (outward) in our axes
        assertTrue(Models.flat(m, Models.View.TOP).any { it.color == 0xC0392B && it.facing > 0.99f })
        // sent again under the same name: same place, new parts
        var p = Plan("t", "t")
        val (p1, id) = PlanOps.putModel(p, "Shed", parts, P(5000.0, 3000.0))
        p = PlanOps.updateModel(p1, p1.models.first().copy(x = 7000, rot = 90))
        val (p2, id2) = PlanOps.putModel(p, "Shed", parts.take(1), P(0.0, 0.0))
        assertEquals(id, id2); assertEquals(1, p2.models.size)
        assertEquals(7000, p2.models[0].x); assertEquals(90, p2.models[0].rot); assertEquals(1, p2.models[0].parts.size)
    }

    @Test fun canopiesAreBuiltWithTrussesAndCovering() {
        for (roof in RoofShape.entries) for (truss in TrussKind.entries) for (cover in Cover.entries) {
            val spec = YardSpec(YardKind.CANOPY, 6000, 4000, 2500, roof, truss, cover)
            val parts = Yard.parts(spec, false)
            assertTrue(parts.all { p -> p.tri.all { it * 3 + 2 < p.xyz.size } })
            val m = Model3D(1, "c", 0, 0, parts = parts)
            val (w, d, h) = Models.size(m)
            assertTrue(w >= 6000 && d >= 4000 && h > 2500, "$roof $truss $cover: $w $d $h")
            val bill = Yard.bill(spec, false)
            // 3 posts a row at 3 m steps over 6 m: 6 posts of 2.5 m
            if (roof != RoofShape.SLOPE) assertEquals(15.0, bill.first { it.name.startsWith("Posts") }.qty, 0.2)
            if (cover != Cover.NONE) {
                val c = bill.first { it.name.startsWith("Covering") }
                // more than the plan area (6.4 × 4.4), less than twice
                assertTrue(c.qty > 6.0 * 4.0 && c.qty < 2 * 6.4 * 4.4 * 1.5, "$roof ${c.qty}")
            }
        }
        for (k in YardKind.entries) assertTrue(Yard.parts(Yard.default(k), true).isNotEmpty())
        // the roof sheet faces up: its top seen from above is about its area
        val cov = Yard.parts(YardSpec(YardKind.CANOPY, roof = RoofShape.SLOPE, pitch = 10), false).first { it.name.startsWith("Covering") }
        assertTrue(Models.measure(cov).top / 1e6 > 6.4 * 4.4 * 0.9)
    }

    @Test fun aModelGoesToIdea3dAndComesBackTheSame() {
        val spec = YardSpec(YardKind.CANOPY, 6000, 4000, 2500, RoofShape.SLOPE, TrussKind.WARREN, Cover.PROFILE_SHEET)
        val parts = Yard.parts(spec, false)
        val bytes = Models.toThreeMf(parts)
        // as Idea 3D would read it: Z up; Roomik reads Idea's export (Y up), so check through a Y-up rewrite
        val xml = java.util.zip.ZipInputStream(bytes.inputStream()).use { z -> generateSequence { z.nextEntry }.first { it.name.endsWith(".model") }; String(z.readBytes()) }
        assertTrue(xml.contains("displaycolor=\"#3E6B48FF\""))
        // Idea 3D stands it up (−90° about X): (x, y, z) → (x, z, −y); its export then comes back to Roomik
        val upright = xml.replace(Regex("""<vertex x="([^"]*)" y="([^"]*)" z="([^"]*)"/>""")) { m ->
            val y = m.groupValues[2].toDouble(); """<vertex x="${m.groupValues[1]}" y="${m.groupValues[3]}" z="${-y}"/>""" }
        val back = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z -> z.putNextEntry(java.util.zip.ZipEntry("3D/3dmodel.model")); z.write(upright.toByteArray()); z.closeEntry() }
        }.toByteArray()
        val again = Models.read("x.3mf", back)
        assertEquals(parts.map { it.name }, again.map { it.name })
        val a = Models.size(Model3D(1, "a", 0, 0, parts = parts)); val b = Models.size(Model3D(1, "b", 0, 0, parts = again))
        assertEquals(a.first, b.first, 2.0); assertEquals(a.second, b.second, 2.0); assertEquals(a.third, b.third, 2.0)
        // the slope rises to the back (+y) both times: the highest covering point is at the back
        fun backHigh(ps: List<ModelPart>): Boolean { val c = ps.first { it.name.startsWith("Covering") }.xyz
            val ys = (c.indices step 3).map { c[it + 1] to c[it + 2] }; return ys.maxBy { it.second }.first > 0 }
        assertEquals(backHigh(parts), backHigh(again))
        // furniture: a bed's blocks as parts
        val bed = Models.ofItem(Item(1, "bed", 1000, 2000, 1600, 2000, 500))
        assertTrue(bed.isNotEmpty())
        val sz = Models.size(Model3D(2, "bed", 0, 0, parts = bed))
        assertEquals(1600.0, sz.first, 50.0)
    }
}
