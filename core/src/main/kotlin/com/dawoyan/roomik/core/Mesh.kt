package com.dawoyan.roomik.core

import com.dawoyan.elektronik.core.RoomType
import kotlin.math.abs

/** Floor colours, shared by the plan and the 3D view. 0xRRGGBB. */
object Palette {
    private val colors = intArrayOf(0xE8D5B5, 0xCFE3D4, 0xD6DDF0, 0xF0D9D2, 0xE6E0C4, 0xD3E6EA, 0xE4D6EA, 0xDCE8C8)
    fun floor(room: Room, index: Int): Int = colors[(room.info?.type?.ordinal ?: index) % colors.size]
    fun of(type: RoomType?): Int = colors[(type?.ordinal ?: 0) % colors.size]
}

/** A lamp as a light: position in metres (x, y up, z), the room it lights, power and its controlling switch. */
class Light(val id: Int, val x: Float, val y: Float, val z: Float, val room: Int, val watts: Int, val switchId: Int?)

/**
 * Triangles of the plan in 3D, in metres: x is plan x, z is plan y, y is up. Each vertex is [STRIDE] floats:
 * position, normal, colour, then the middle of the wall it belongs to (x, z), its [kind] and the room it is in (-1
 * outside). Walls ([KIND_WALL]) can be lowered by the renderer when they stand between the camera and the plan.
 */
class Mesh(
    val data: FloatArray, val minX: Float, val minZ: Float, val maxX: Float, val maxZ: Float, val height: Float,
    /** Vertices before the ceilings: what the open-top outside view draws. */
    val openCount: Int,
    val lights: List<Light>,
    /** Vertices of the ground at the start (none when looking at a basement from outside). */
    val groundCount: Int = 6,
    /** Height of the ground, metres. */
    val groundY: Float = 0f,
    val residentParts: List<IntRange> = emptyList(),
    val residentRoute: List<P> = emptyList(),
) {
    val vertexCount: Int get() = data.size / STRIDE

    companion object {
        const val STRIDE = 13
        const val KIND_PLAIN = 0f
        const val KIND_WALL = 1f
        const val KIND_GLOW = 2f
        /** Roofs and ceilings: what the X-ray lens sees through, along with the walls. */
        const val KIND_COVER = 3f
        /** Walls with the outside on one side: they can be shown faint, to look into the house. */
        const val KIND_OUTER = 4f

        /** Two safe points in the largest room, traversed slowly and repeatedly. Coordinates are millimetres. */
        fun residentPosition(plan: Plan, step: Int): P? {
            val room = Rooms.detect(plan).maxByOrNull { it.area } ?: return null
            val poly = room.polygon
            val xs = poly.map { it.x }; val ys = poly.map { it.y }
            val candidates = mutableListOf<P>()
            val spacing = maxOf(400.0, (xs.max() - xs.min()) / 40.0, (ys.max() - ys.min()) / 40.0)
            var x = xs.min() + 300.0
            while (x < xs.max() - 300.0) {
                var y = ys.min() + 300.0
                while (y < ys.max() - 300.0) {
                    val p = P(x, y)
                    if (Geo.inside(p, poly) && plan.walls.all { w ->
                        val a = plan.pos(w.a); val b = plan.pos(w.b)
                        a == null || b == null || Geo.distToSegment(p, a, b) > 300.0
                    }) candidates += p
                    y += spacing
                }
                x += spacing
            }
            val start = candidates.minByOrNull { (it - room.anchor).len } ?: room.anchor
            val end = candidates.filter { Interior.canWalk(plan, start, it) }.maxByOrNull { (it - start).len } ?: start
            val t = (1.0 - kotlin.math.cos(step * Math.PI / 40.0)) * 0.5
            return start + (end - start) * t
        }

        /** Room numbers in the vertex data: unique over all floors, -1 outside. */
        fun roomNo(floor: Int, index: Int) = if (index < 0) -1 else (floor + 10) * 100 + index

        /**
         * Floors 0..[upTo] of [root] stacked; the top one shown is open (no ceiling in the outside view). [kind] in the
         * vertex data also carries the floor's base height: kind + 10 × base in decimetres.
         */
        /** Floors [from]..[upTo] of [root]; by default from the lowest basement. */
        fun build(root: Plan, upTo: Int = 0, from: Int = Int.MIN_VALUE, markClashes: Boolean = false, hidden: Set<Layer> = emptySet(), residentFloor: Int? = null, residentStep: Int = 0): Mesh {
            val out = ArrayList<Float>(8192)
            val ceilings = ArrayList<Float>(1024)
            var target = out
            val top = upTo.coerceIn(-root.basements.size, root.floors.size)
            val low = from.coerceIn(-root.basements.size, top)
            val shown = low..top
            val lights = ArrayList<Light>()
            val residentParts = ArrayList<IntRange>()
            var residentRoute = emptyList<P>()
            var midX = 0f; var midZ = 0f; var kind = KIND_PLAIN; var room = -1; var base = 0f; var baseDm = 0
            fun v(p: P, y: Float, nx: Float, ny: Float, nz: Float, c: Int) {
                target += (p.x / 1000).toFloat(); target += y + base; target += (p.y / 1000).toFloat()
                target += nx; target += ny; target += nz
                target += (c shr 16 and 255) / 255f; target += (c shr 8 and 255) / 255f; target += (c and 255) / 255f
                target += midX; target += midZ; target += kind + 10f * baseDm; target += room.toFloat()
            }
            // the fences and the ground round the house count too, when the ground is shown
            val groundShown = top >= root.groundFloor && low <= root.groundFloor
            val outdoor = if (!groundShown) emptyList() else
                (root.floorRange + listOfNotNull(root.site?.let { SITE })).flatMap { j -> root.floor(j).fences.flatMap { it.points } } +
                    (root.site?.let { it.nodes + it.items.map { i -> Node(0, i.x, i.y) } + it.models.flatMap { m -> Models.corners(m).map { c -> Node(0, c.x.toInt(), c.y.toInt()) } } } ?: emptyList()) +
                    shown.flatMap { fi -> root.floor(fi).models.flatMap { m -> Models.corners(m).map { c -> Node(0, c.x.toInt(), c.y.toInt()) } } }
            val xs = shown.flatMap { fi -> root.floor(fi).nodes.map { it.x / 1000f } } + outdoor.map { it.x / 1000f }
            val zs = shown.flatMap { fi -> root.floor(fi).nodes.map { it.y / 1000f } } + outdoor.map { it.y / 1000f }
            val minX = xs.minOrNull() ?: -2f; val maxX = xs.maxOrNull() ?: 2f
            val minZ = zs.minOrNull() ?: -2f; val maxZ = zs.maxOrNull() ?: 2f

            // Ground.
            val pad = 30000.0
            val g = listOf(P(minX * 1000 - pad, minZ * 1000 - pad), P(maxX * 1000 + pad, minZ * 1000 - pad),
                P(maxX * 1000 + pad, maxZ * 1000 + pad), P(minX * 1000 - pad, maxZ * 1000 + pad))
            // The ground hides what is below it, so it is left out when a basement is the floor looked at.
            // the ground shows when the ground floor or the floors around it are shown, not under a lone upper floor
            val ground = if (groundShown) 6 else 0
            val groundY = root.groundMm / 1000f - 0.01f
            if (ground > 0) for (i in intArrayOf(0, 2, 1, 0, 3, 2)) v(g[i], groundY, 0f, 1f, 0f, 0x9DB58E)
            // which way is north: a red arrow on the ground beside the plan, a dark blue tail to the south
            if (ground > 0) {
                val s = Math.toRadians(root.south.toDouble())
                val nx = Math.sin(s); val ny = -Math.cos(s)
                val c = P((minX + maxX) * 500.0, (minZ + maxZ) * 500.0)
                val rr = maxOf(maxX - minX, maxZ - minZ) * 500.0 + 1500
                val tip = c + P(nx, ny) * (rr + 1800); val b0 = c + P(nx, ny) * rr
                val side = P(-ny, nx) * 500.0
                val y = groundY + 0.01f
                v(tip, y, 0f, 1f, 0f, 0xC0392B); v(b0 + side, y, 0f, 1f, 0f, 0xC0392B); v(b0 - side, y, 0f, 1f, 0f, 0xC0392B)
                val t0 = c - P(nx, ny) * rr; val t1 = c - P(nx, ny) * (rr + 1200)
                v(t0 + side * 0.4, y, 0f, 1f, 0f, 0x2F4F7F); v(t1 + side * 0.4, y, 0f, 1f, 0f, 0x2F4F7F); v(t1 - side * 0.4, y, 0f, 1f, 0f, 0x2F4F7F)
                v(t0 + side * 0.4, y, 0f, 1f, 0f, 0x2F4F7F); v(t1 - side * 0.4, y, 0f, 1f, 0f, 0x2F4F7F); v(t0 - side * 0.4, y, 0f, 1f, 0f, 0x2F4F7F)
            }

            // roads and paths, drawn on any level, lie on the ground: strips with round joins
            if (ground > 0) {
                var lift = 0.05f
                for (j in root.levels) for (rd in root.floor(j).roads) {
                    val pts = rd.points.map { P(it.x.toDouble(), it.y.toDouble()) }
                    val hw = rd.width / 2.0; val col = rd.surface.color; val y = groundY + lift
                    if (rd.closed && pts.size >= 3) {
                        // Landscape areas are flat polygons: grass, gravel and paving sit directly on the site.
                        for (k in 1 until pts.lastIndex) { v(pts[0], y, 0f, 1f, 0f, col); v(pts[k], y, 0f, 1f, 0f, col); v(pts[k + 1], y, 0f, 1f, 0f, col) }
                        lift += 0.003f
                        continue
                    }
                    for (k in 1 until pts.size) {
                        val a = pts[k - 1]; val b = pts[k]; if ((b - a).len < 1) continue
                        val n = (b - a).norm().left() * hw
                        val q = listOf(a + n, b + n, b - n, a - n)
                        for (t in intArrayOf(0, 2, 1, 0, 3, 2)) v(q[t], y, 0f, 1f, 0f, col)
                    }
                    for (k in 1 until pts.size - 1) {
                        val c = pts[k]
                        for (m in 0 until 16) {
                            val a0 = 2 * Math.PI * m / 16; val a1 = 2 * Math.PI * (m + 1) / 16
                            v(c, y, 0f, 1f, 0f, col); v(c + P(Math.cos(a1), Math.sin(a1)) * hw, y, 0f, 1f, 0f, col); v(c + P(Math.cos(a0), Math.sin(a0)) * hw, y, 0f, 1f, 0f, col)
                        }
                    }
                    // a kerb of a darker shade along paved ones
                    if (rd.surface == Surface.ASPHALT || rd.surface == Surface.PAVERS || rd.surface == Surface.CONCRETE) {
                        val kc = Facings.shade(col, if (rd.surface == Surface.ASPHALT) 1.9 else 0.8)
                        for (k in 1 until pts.size) {
                            val a = pts[k - 1]; val b = pts[k]; if ((b - a).len < 1) continue
                            val dd = (b - a).norm(); val n = dd.left()
                            for (sd in listOf(1.0, -1.0)) {
                                val e0 = a + n * (sd * hw); val e1 = b + n * (sd * hw); val i0 = a + n * (sd * (hw - 100)); val i1 = b + n * (sd * (hw - 100))
                                val q = listOf(e0, e1, i1, i0)
                                for (t in intArrayOf(0, 2, 1, 0, 3, 2)) v(q[t], y + 0.002f, 0f, 1f, 0f, kc)
                            }
                        }
                    }
                    lift += 0.003f
                }
            }

            // the ground round the house goes with the ground
            val levels = shown.toList() + if (ground > 0 && root.site != null) listOf(SITE) else emptyList()
            for (fi in levels) {
            val plan = root.floor(fi)
            val onSite = fi == SITE
            base = root.elevation(fi) / 1000f
            baseDm = Math.round(base * 10)
            val h = plan.wallHeight / 1000f
            val rooms = Rooms.detect(plan)
            fun roomAt(p: P): Int = roomNo(fi, rooms.indexOfFirst { Geo.inside(p, it.polygon) })
            /** A vertical quad; its face looks to the right of p→q. Wall faces take the room they look into. */
            fun quad(p: P, q: P, y0: Float, y1: Float, c: Int, lookRoom: Boolean = false) {
                val n = (q - p).norm().left() * -1.0
                if (lookRoom) room = roomAt((p + q) * 0.5 + n * 60.0)
                val nx = n.x.toFloat(); val nz = n.y.toFloat()
                v(p, y0, nx, 0f, nz, c); v(q, y0, nx, 0f, nz, c); v(q, y1, nx, 0f, nz, c)
                v(p, y0, nx, 0f, nz, c); v(q, y1, nx, 0f, nz, c); v(p, y1, nx, 0f, nz, c)
            }
            /** A vertical quad whose bottom and top may slope: heights at p, then at q. */
            fun quadS(p: P, q: P, y0p: Float, y1p: Float, y0q: Float, y1q: Float, c: Int, lookRoom: Boolean = false) {
                if (y1p - y0p < 0.0005f && y1q - y0q < 0.0005f) return
                val n = (q - p).norm().left() * -1.0
                if (lookRoom) room = roomAt((p + q) * 0.5 + n * 60.0)
                val nx = n.x.toFloat(); val nz = n.y.toFloat()
                v(p, y0p, nx, 0f, nz, c); v(q, y0q, nx, 0f, nz, c); v(q, y1q, nx, 0f, nz, c)
                v(p, y0p, nx, 0f, nz, c); v(q, y1q, nx, 0f, nz, c); v(p, y1p, nx, 0f, nz, c)
            }
            /** A flat-ish lid whose corners each have their own height. */
            fun hpolyS(pts: List<P>, ys: List<Float>, up: Boolean, c: Int) {
                val m = pts.fold(P(0.0, 0.0)) { acc, p -> acc + p } * (1.0 / pts.size)
                val ym = ys.average().toFloat()
                val ny = if (up) 1f else -1f
                for (i in pts.indices) { val j = (i + 1) % pts.size; v(m, ym, 0f, ny, 0f, c); v(pts[i], ys[i], 0f, ny, 0f, c); v(pts[j], ys[j], 0f, ny, 0f, c) }
            }
            fun hpoly(pts: List<P>, y: Float, up: Boolean, c: Int) {
                val m = pts.fold(P(0.0, 0.0)) { acc, p -> acc + p } * (1.0 / pts.size)
                val ny = if (up) 1f else -1f
                for (i in pts.indices) { v(m, y, 0f, ny, 0f, c); v(pts[i], y, 0f, ny, 0f, c); v(pts[(i + 1) % pts.size], y, 0f, ny, 0f, c) }
            }
            /** A box standing on the plan outline [c] (inside on the left of each edge), from [y0] to [y1] metres. */
            fun box(c: List<P>, y0: Float, y1: Float, color: Int) {
                for (i in c.indices) quad(c[i], c[(i + 1) % c.size], y0, y1, color)
                hpoly(c, y1, true, color)
                if (abs(y0) > 0.01f) hpoly(c.reversed(), y0, false, color)
            }
            /** A triangle in 3D (heights in mm above this floor), facing up or outwards. */
            fun tri3(a: RP, b: RP, c: RP, col: Int, down: Boolean = false) {
                val ax = a.p.x / 1000; val ay = a.h / 1000; val az = a.p.y / 1000
                val ux = b.p.x / 1000 - ax; val uy = b.h / 1000 - ay; val uz = b.p.y / 1000 - az
                val wx = c.p.x / 1000 - ax; val wy = c.h / 1000 - ay; val wz = c.p.y / 1000 - az
                var nx = uy * wz - uz * wy; var ny = uz * wx - ux * wz; var nz = ux * wy - uy * wx
                val l = Math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-9)
                nx /= l; ny /= l; nz /= l
                if (ny < -0.01) { nx = -nx; ny = -ny; nz = -nz }
                if (down) { nx = -nx; ny = -ny; nz = -nz }
                for (q in listOf(a, b, c)) v(q.p, (q.h / 1000).toFloat(), nx.toFloat(), ny.toFloat(), nz.toFloat(), col)
            }
            fun rect(center: P, u: P, hw: Double, hd: Double): List<P> {
                val w = u.left()
                return listOf(center - u * hw - w * hd, center + u * hw - w * hd, center + u * hw + w * hd, center - u * hw + w * hd)
            }

            // Floors; above the ground floor a slab under them too.
            kind = KIND_PLAIN
            rooms.forEachIndexed { index, r ->
                room = roomNo(fi, index)
                // the stairwell from the floor below is left open
                val fill = Stairs.fillAround(r.polygon, if (!onSite && fi > -root.basements.size) Stairs.wells(root.floor(fi - 1)) else emptyList())
                if (!onSite && (fi != -root.basements.size || low > fi)) for ((a, b, cc) in fill) { v(cc, -SLAB / 1000f, 0f, -1f, 0f, 0xFAF8F4); v(b, -SLAB / 1000f, 0f, -1f, 0f, 0xFAF8F4); v(a, -SLAB / 1000f, 0f, -1f, 0f, 0xFAF8F4) }
                val c = Palette.floor(r, index)
                for ((a, b, cc) in fill) { v(a, 0f, 0f, 1f, 0f, c); v(b, 0f, 0f, 1f, 0f, c); v(cc, 0f, 0f, 1f, 0f, c) }
            }

            // A simple in-scene resident, on the selected floor rather than in the saved house model.
            if (fi == residentFloor && rooms.isNotEmpty()) {
                residentRoute = ResidentNavigation.route(plan)
                val p = residentRoute.firstOrNull() ?: rooms.first().anchor
                val stride = kotlin.math.sin(residentStep * Math.PI / 4) * 105.0
                val bob = (kotlin.math.abs(kotlin.math.sin(residentStep * Math.PI / 4)) * 0.025).toFloat()
                kind = KIND_PLAIN; room = -1
                fun part(dx: Double, dy: Double, w: Double, d: Double, y0: Float, y1: Float, color: Int) {
                    val start = out.size / STRIDE
                    box(rect(p + P(dx, dy), P(1.0, 0.0), w / 2, d / 2), y0, y1, color)
                    residentParts += start until (out.size / STRIDE)
                }
                fun oval(dx: Double, dz: Double, cy: Float, rx: Double, rz: Double, ry: Double, color: Int) {
                    val start = out.size / STRIDE
                    val rings = 10; val segments = 16
                    fun vertex(i: Int, j: Int) {
                        val lat = Math.PI * i / rings; val lon = 2.0 * Math.PI * j / segments
                        val nx = kotlin.math.sin(lat) * kotlin.math.cos(lon); val nz = kotlin.math.sin(lat) * kotlin.math.sin(lon); val ny = kotlin.math.cos(lat)
                        v(p + P(dx + nx * rx, dz + nz * rz), cy + bob + (ny * ry).toFloat(), nx.toFloat(), ny.toFloat(), nz.toFloat(), color)
                    }
                    for (i in 0 until rings) for (j in 0 until segments) {
                        vertex(i, j); vertex(i + 1, j); vertex(i + 1, j + 1)
                        vertex(i, j); vertex(i + 1, j + 1); vertex(i, j + 1)
                    }
                    residentParts += start until (out.size / STRIDE)
                }
                part(-90.0, stride, 130.0, 180.0, 0f, .75f, 0x344866)
                part(90.0, -stride, 130.0, 180.0, 0f, .75f, 0x344866)
                part(0.0, 0.0, 340.0, 200.0, .75f, 1.35f + bob, 0xD96375)
                part(-230.0, -stride * .6, 100.0, 130.0, .85f, 1.3f + bob, 0xE6B392)
                part(230.0, stride * .6, 100.0, 130.0, .85f, 1.3f + bob, 0xE6B392)
                oval(0.0, 0.0, 1.505f, 118.0, 103.0, .155, 0xE6B392)
                oval(0.0, -23.0, 1.66f, 125.0, 95.0, .08, 0x44352E)
                part(0.0, -95.0, 255.0, 70.0, 1.39f + bob, 1.68f + bob, 0x44352E)
                oval(-43.0, 95.0, 1.535f, 19.0, 10.0, .018, 0xF6F0E8)
                oval(43.0, 95.0, 1.535f, 19.0, 10.0, .018, 0xF6F0E8)
                oval(-43.0, 104.0, 1.535f, 9.0, 5.0, .012, 0x45302A)
                oval(43.0, 104.0, 1.535f, 9.0, 5.0, .012, 0x45302A)
                part(-2.0, 107.0, 14.0, 12.0, 1.49f + bob, 1.51f + bob, 0xCF9478)
                part(0.0, 105.0, 46.0, 10.0, 1.445f + bob, 1.453f + bob, 0xAD555A)
                part(-28.0, 102.0, 12.0, 8.0, 1.452f + bob, 1.46f + bob, 0xAD555A)
                part(28.0, 102.0, 12.0, 8.0, 1.452f + bob, 1.46f + bob, 0xAD555A)
            }

            // A plinth under the ground floor's walls, down to the ground when the house stands above it.
            if (fi == root.groundFloor && root.groundLevel > 0) {
                kind = KIND_PLAIN; room = -1
                for (ws in WallShapes.build(plan)) box(ws.outline, -root.groundLevel / 1000f, 0f, 0xB9B4AA)
            }

            // Walls: both faces, end caps, top; doors and windows cut the wall into solid stretches with a lintel above
            // and, for a window, a sill below.
            val side = 0xF1EEE8
            val outside = 0xE4DED3
            val top = 0x5A5F6B
            val cuts = Interior.openings(plan)
            val wallMid = HashMap<Int, P>()
            class Sec(val l: P, val c: P, val r: P)
            kind = KIND_WALL
            // walls stairs run into are red, so they can be found (and lowered by hand)
            val clashing = if (markClashes) Stairs.allClashes(plan).keys else emptySet()
            val crops = Stairs.crops(plan)
            // Roofs cut the walls standing up into them: no wall is higher than the roof over it.
            // a roof of the floor above set low (base 0) comes down into this floor's wall tops too
            val above = if (fi + 1 <= root.floors.size) root.floor(fi + 1) else null
            val rise = if (above != null) root.elevation(fi + 1) - root.elevation(fi) else 0
            val roofCaps = plan.roofs.map { rf -> val b = Roofs.baseOf(plan, rf); Triple(rf, b, Roofs.planes(rf, b)) } +
                (above?.roofs.orEmpty().map { rf -> val b = Roofs.baseOf(above!!, rf) + rise; Triple(rf, b, Roofs.planes(rf, b)) })
            fun cap(p: P): Float {
                var c = Float.MAX_VALUE
                for ((rf, b, pl) in roofCaps) Roofs.surfaceAt(rf, b, p, pl)?.let { c = minOf(c, ((it - 30) / 1000).toFloat()) }
                return c
            }
            val outerWalls = HashSet<Int>()
            for (s in WallShapes.build(plan)) {
                val red = s.wall.id in clashing
                // a full-height wall under another floor goes on up through the slab, so floors meet without a gap
                val h = (plan.heightOf(s.wall) + if (fi < root.floors.size && s.wall.height == null) SLAB else 0) / 1000f
                val m = s.mid
                wallMid[s.wall.id] = m
                midX = (m.x / 1000).toFloat(); midZ = (m.y / 1000).toFloat()
                val d = (s.b - s.a).norm()
                val n = d.left()
                val ht = s.wall.thickness / 2.0
                val outer = roomAt(m + n * (ht + 80)) < 0 || roomAt(m - n * (ht + 80)) < 0
                if (outer) outerWalls += s.wall.id
                kind = if (outer) KIND_OUTER else KIND_WALL
                // does a roof come down lower than this wall's top somewhere along it?
                val capped = roofCaps.isNotEmpty() && (0..20).any { k -> cap(s.a + (s.b - s.a) * (k / 20.0)) < h }
                // clad with stone: its outside faces (both, when it has none: a stone wall inside, or standing alone)
                val (fL, fR) = Facings.sides(plan, s.wall, rooms)
                fun baseCol(f: Facing) = if (f.pattern == Pattern.SMOOTH || f.stone == Stone.PLASTER) f.color else Facings.mortar(f)
                fun face(p: P, q: P, y0: Float, y1: Float) {
                    val look = roomAt((p + q) * 0.5 + (q - p).norm().left() * -60.0)
                    // a long face takes its side's stone; an end or a reveal the outside one's
                    val along = dot((q - p).norm().left() * -1.0, n)
                    val f = when { along > 0.7 -> fL; along < -0.7 -> fR; look < 0 -> fL ?: fR; else -> null }
                    val col = if (red) 0xE0565B else if (f != null) baseCol(f) else if (look < 0) outside else side
                    if (!capped) { quad(p, q, y0, y1, col, lookRoom = true); return }
                    val tp = minOf(y1, cap(p)); val tq = minOf(y1, cap(q))
                    quadS(p, q, minOf(y0, tp), tp, minOf(y0, tq), tq, col, lookRoom = true)
                }
                fun sec(t: Double) = Sec(s.a + d * t + n * ht, s.a + d * t, s.a + d * t - n * ht)
                fun lerp(a: P, b: P, f: Double) = a + (b - a) * f
                fun solid(p: Sec, q: Sec, y0: Float, y1: Float, lid: Int) {
                    // under a roof the stretch is cut in short pieces, so its top can follow the slopes
                    val pieces = if (capped) maxOf(1, Math.ceil((q.c - p.c).len / 100).toInt()) else 1
                    for (k in 0 until pieces) {
                        val f0 = k.toDouble() / pieces; val f1 = (k + 1).toDouble() / pieces
                        val a = Sec(lerp(p.l, q.l, f0), lerp(p.c, q.c, f0), lerp(p.r, q.r, f0))
                        val b = Sec(lerp(p.l, q.l, f1), lerp(p.c, q.c, f1), lerp(p.r, q.r, f1))
                        face(b.l, a.l, y0, y1)
                        face(a.r, b.r, y0, y1)
                        room = -1
                        val pts = listOf(a.c, a.l, b.l, b.c, b.r, a.r)
                        if (capped) hpolyS(pts, pts.map { maxOf(minOf(y0, cap(it)), minOf(y1, cap(it))) }, true, lid)
                        else hpoly(pts, y1, true, lid)
                    }
                }
                face(s.aL, s.a, 0f, h); face(s.a, s.aR, 0f, h)
                face(s.bR, s.b, 0f, h); face(s.b, s.bL, 0f, h)
                var cur = Sec(s.aL, s.a, s.aR)
                // doors and windows, and stretches cut away above the stairs passing through ([Stairs.crops])
                val cropCuts = crops[s.wall.id].orEmpty().map { (t0, t1, under) ->
                    PlacedOpening(Opening(-1, OpeningKind.PASSAGE, 0, 0, width = (t1 - t0).toInt(), height = 100000, sill = under.toInt().coerceAtLeast(0)), s.wall, t0, t1)
                }
                val real = cuts[s.wall.id].orEmpty()
                val all = (real + cropCuts.filter { c -> real.none { r -> c.t0 < r.t1 && c.t1 > r.t0 } }).sortedBy { it.t0 }
                for (cut in all) {
                    val o = cut.opening
                    val p = sec(cut.t0); val q = sec(cut.t1)
                    solid(cur, p, 0f, h, top)
                    val y0 = (o.sill / 1000f).coerceIn(0f, h)
                    val y1 = ((o.sill + o.height) / 1000f).coerceIn(y0, h)
                    if (y1 < h - 0.001f && (!capped || cap(p.c) > y1 || cap(q.c) > y1)) {
                        solid(p, q, y1, h, top); room = -1
                        if (capped) { val pts = listOf(p.l, q.l, q.r, p.r); hpolyS(pts, pts.map { minOf(y1, cap(it)) }, false, side) }
                        else hpoly(listOf(p.l, q.l, q.r, p.r), y1, false, side)
                    }
                    if (y0 > 0.001f) solid(p, q, 0f, y0, side)
                    face(p.r, p.l, y0, y1)
                    face(q.l, q.r, y0, y1)
                    cur = q
                }
                solid(cur, Sec(s.bL, s.b, s.bR), 0f, h, top)
                // the stones on the clad faces, 3 mm proud of them, with the doors, windows and crops left open
                if ((fL != null || fR != null) && !red) {
                    val holes = all.map { c -> doubleArrayOf(c.t0, c.t1, c.opening.sill.toDouble(), (c.opening.sill + c.opening.height).toDouble()) }
                    for (left in listOf(true, false)) {
                        val facing = (if (left) fL else fR) ?: continue
                        val e0 = if (left) s.aL else s.aR; val e1 = if (left) s.bL else s.bR
                        val tA = dot(e0 - s.a, d); val tB = dot(e1 - s.a, d)
                        val off = (if (left) n else n * -1.0) * (ht + 3)
                        val cells = Facings.cut(Facings.cells(facing, tB - tA, h * 1000.0, s.wall.id * 2 + if (left) 0 else 1)
                            .map { Cell(it.t0 + tA, it.t1 + tA, it.y0, it.y1, it.color) }, holes)
                        room = -1
                        val j = Facings.JOINT / 2
                        for (c in cells) {
                            if (c.t1 - c.t0 <= 2 * j + 1 || c.y1 - c.y0 <= 2 * j + 1) continue
                            val p0 = s.a + d * (c.t0 + j) + off; val p1 = s.a + d * (c.t1 - j) + off
                            val ya = ((c.y0 + j) / 1000).toFloat(); var yb = ((c.y1 - j) / 1000).toFloat()
                            val (pp, qq) = if (left) p1 to p0 else p0 to p1
                            if (!capped) { quad(pp, qq, ya, yb, c.color); continue }
                            val tp = minOf(yb, cap(pp)); val tq = minOf(yb, cap(qq))
                            if (tp <= ya && tq <= ya) continue
                            quadS(pp, qq, ya, maxOf(ya, tp), ya, maxOf(ya, tq), c.color)
                        }
                    }
                }
            }

            // Windows get a pane, doors a leaf standing open.
            for ((wid, list) in cuts) for (cut in list) {
                val w = plan.wall(wid) ?: continue
                val a = plan.pos(w.a) ?: continue
                val b = plan.pos(w.b) ?: continue
                wallMid[wid]?.let { midX = (it.x / 1000).toFloat(); midZ = (it.y / 1000).toFloat() }
                kind = if (wid in outerWalls) KIND_OUTER else KIND_WALL
                val h = plan.heightOf(w) / 1000f
                val d = (b - a).norm()
                val o = cut.opening
                val y0 = o.sill / 1000f
                val y1 = ((o.sill + o.height) / 1000f).coerceAtMost(h)
                room = -1
                if (o.kind == OpeningKind.WINDOW) {
                    val mid = a + d * ((cut.t0 + cut.t1) / 2)
                    val y1c = minOf(y1, cap(mid))
                    if (y1c > y0) box(rect(mid, d, (cut.t1 - cut.t0) / 2, 12.0), y0, y1c, 0xA9D4EE)
                } else if (o.kind == OpeningKind.GATE) {
                    // a gate closed in its opening: leaves, a garage door or a shutter
                    for (part in Fences.gate(a + d * cut.t0, a + d * cut.t1, o.gate, (y1 - y0) * 1000.0))
                        box(part.corners, y0 + (part.z0 / 1000).toFloat(), y0 + (part.z1 / 1000).toFloat(), part.color)
                } else if (o.kind == OpeningKind.DOOR) {
                    val no = P(o.nx, o.ny)
                    // The leaf stands open about 75°, turned on its hinge towards the room it opens into.
                    val along = if (o.hingeEnd) d * -1.0 else d
                    val hinge = a + d * (if (o.hingeEnd) cut.t1 else cut.t0) + no * (w.thickness / 2.0)
                    val leaf = (along * 0.26 + no * 0.966).norm()
                    val c = hinge + leaf * ((cut.t1 - cut.t0) / 2)
                    room = roomAt(c)
                    box(rect(c, leaf, (cut.t1 - cut.t0) / 2, 20.0), 0f, y1, 0x9C6B3F)
                }
            }

            kind = KIND_WALL
            // Stairs: each step a solid tread, so they can pass over low walls and furniture; landings as slabs.
            // when a floor is shown on its own, the stairs coming up from the floor below show in its stairwell too
            val upFromBelow = if (!onSite && fi == shown.first && fi - 1 >= -root.basements.size && shown.first == shown.last) root.floor(fi - 1).let { b ->
                b.stairs.filter { !it.fromGround && Stairs.rise(b, it) > b.wallHeight }.map { it to b } } else emptyList()
            for ((st, sp) in plan.stairs.map { it to plan } + upFromBelow) {
                val below = sp !== plan
                if (below) { base = root.elevation(fi - 1) / 1000f; baseDm = Math.round(base * 10) }
                val shp = Stairs.shape(sp, st)
                val low = (Stairs.base(sp, st) / 1000).toFloat()
                for (f in shp.flights) {
                    val c = f.corners
                    room = roomAt((c[0] + c[2]) * 0.5)
                    for (k in 0 until f.steps) {
                        val f0 = k.toDouble() / f.steps; val f1 = (k + 1).toDouble() / f.steps
                        val top = ((f.h0 + (f.h1 - f.h0) * f1) / 1000).toFloat()
                        val q = listOf(c[0] + (c[3] - c[0]) * f0, c[1] + (c[2] - c[1]) * f0, c[1] + (c[2] - c[1]) * f1, c[0] + (c[3] - c[0]) * f1)
                        box(q, (top - 0.22f).coerceAtLeast(low), top, if (k % 2 == 0) 0xB08D6A else 0xA27F5D)
                    }
                }
                for (l in shp.landings) {
                    room = roomAt((l.corners[0] + l.corners[2]) * 0.5)
                    val top = (l.h / 1000).toFloat()
                    box(l.corners, (top - 0.18f).coerceAtLeast(low), top, 0xA9876A)
                }
                if (below) { base = root.elevation(fi) / 1000f; baseDm = Math.round(base * 10) }
            }

            // Warm floor: a warm-coloured layer on the floor of the rooms that have it.
            if (Layer.WARM_FLOOR !in hidden) {
                kind = KIND_PLAIN
                val wells = if (!onSite && fi > -root.basements.size) Stairs.wells(root.floor(fi - 1)) else emptyList()
                rooms.forEachIndexed { index, r ->
                    if (r.info?.warmFloor != true) return@forEachIndexed
                    room = roomNo(fi, index)
                    for ((a, b, cc) in Stairs.fillAround(r.inner, wells)) { v(a, 0.004f, 0f, 1f, 0f, 0xF2B07E); v(b, 0.004f, 0f, 1f, 0f, 0xF2B07E); v(cc, 0.004f, 0f, 1f, 0f, 0xF2B07E) }
                }
            }

            // Roofs: slopes in roof-tile colour, gable ends like the walls.
            if (Layer.ROOF !in hidden) {
                kind = KIND_COVER; room = -1
                for (rf in plan.roofs) for (face in Roofs.faces(rf, Roofs.baseOf(plan, rf))) {
                    val pts = face.pts
                    val col = if (face.gable) 0xE4DED3 else rf.shownColor
                    for (k in 1 until pts.size - 1) tri3(pts[0], pts[k], pts[k + 1], col)
                    // the covering's ribs: a ridge every so often down the slope (and the steps of metal tile across it)
                    if (!face.gable) {
                        val lit = Facings.shade(col, 1.25); val dark = Facings.shade(col, 0.7)
                        fun rib(a: RP, b: RP, half: Double, rise: Double) {
                            val dd = (b.p - a.p).norm(); val sd = dd.left() * half
                            val la = RP(a.p + sd, a.h + 3); val lb = RP(b.p + sd, b.h + 3); val ra = RP(a.p - sd, a.h + 3); val rb = RP(b.p - sd, b.h + 3)
                            val ta = RP(a.p, a.h + rise); val tb = RP(b.p, b.h + rise)
                            tri3(la, lb, tb, lit); tri3(la, tb, ta, lit); tri3(ra, tb, rb, dark); tri3(ra, ta, tb, dark)
                        }
                        when (rf.cover) {
                            RoofCover.PROFILE_SHEET -> for ((a, b) in Roofs.ribs(face, 200.0)) rib(a, b, 25.0, 22.0)
                            RoofCover.METAL_TILE -> {
                                for ((a, b) in Roofs.ribs(face, 185.0)) rib(a, b, 40.0, 14.0)
                                for ((a, b) in Roofs.ribs(face, 350.0, across = true)) rib(a, b, 20.0, 18.0)
                            }
                            RoofCover.TILES -> for ((a, b) in Roofs.ribs(face, 300.0, across = true)) rib(a, b, 25.0, 25.0)
                            RoofCover.SOFT_TILE, RoofCover.SLATE -> for ((a, b) in Roofs.ribs(face, 330.0, across = true)) rib(a, b, 8.0, 6.0)
                        }
                    }
                    // the underside, a little lower: a light ceiling seen from inside an attic, not the tiles
                    if (!face.gable) { val u = pts.map { RP(it.p, it.h - 25) }; for (k in 1 until u.size - 1) tri3(u[0], u[k], u[k + 1], 0xEDE6DA, down = true) }
                }
            }

            // Platforms: balconies, terraces, decks at their height, railings on the open sides.
            kind = KIND_PLAIN
            for (pf in plan.platforms) {
                room = -1
                val groundZ = (root.groundMm - root.elevation(fi)).toDouble()
                for (part in Platforms.parts(plan, pf, groundZ)) box(part.corners, (part.z0 / 1000).toFloat(), (part.z1 / 1000).toFloat(), part.color)
            }

            // Pillars: concrete posts, up through the slab when a floor stands on them, like full walls.
            kind = KIND_PLAIN
            for (pl in plan.pillars) {
                if (Pillars.inWall(plan, pl)) continue
                room = -1
                val ph = (Pillars.height(plan, pl) + if (fi < root.floors.size && pl.height == null) SLAB else 0) / 1000f
                box(Pillars.corners(pl), 0f, ph, 0xC4BFB5)
            }

            // Fences: posts and an infill that looks like the kind.
            // Fences on the ground: all of them, once, whenever the ground is shown, whichever level they were drawn on.
            // Fences higher up (round a terrace, a flat roof, a deck) stand there, with their floor.
            if (Layer.FENCES !in hidden) {
                kind = KIND_PLAIN; room = -1
                val here = ArrayList<Pair<Fence, Double>>()
                if (ground > 0 && fi == levels.first()) {
                    val g = (root.groundMm - root.elevation(fi)).toDouble()
                    for (j in root.floorRange + listOfNotNull(root.site?.let { SITE })) for (f in root.floor(j).fences) if (Fences.grounded(root, j, f)) here += f to g
                }
                if (fi != SITE) for (f in plan.fences) if (!Fences.grounded(root, fi, f)) here += f to Fences.standsAt(root, fi, f)
                for ((f, g) in here) for ((pi, part) in Fences.parts(f).withIndex()) {
                    box(part.corners, ((part.z0 + g) / 1000).toFloat(), ((part.z1 + g) / 1000).toFloat(), part.color)
                    val clad = part.clad ?: continue
                    val c = part.corners
                    for ((side, pq) in listOf(c[1] to c[0], c[3] to c[2]).withIndex()) {
                        // the stones stand 3 mm proud of the face, looking out of it
                        val (p0, p1) = pq
                        val dd = (p1 - p0).norm(); val out = dd.left() * -3.0
                        val len = (p1 - p0).len
                        for (cell in Facings.cells(clad, len, part.z1 - part.z0, f.id * 131 + pi * 7 + side)) {
                            val j = Facings.JOINT / 2
                            if (cell.t1 - cell.t0 <= 2 * j || cell.y1 - cell.y0 <= 2 * j) continue
                            quad(p0 + dd * (cell.t0 + j) + out, p0 + dd * (cell.t1 - j) + out,
                                ((part.z0 + g + cell.y0 + j) / 1000).toFloat(), ((part.z0 + g + cell.y1 - j) / 1000).toFloat(), cell.color)
                        }
                    }
                }
            }

            // House systems: pipes and cables as thin bars at their height.
            kind = KIND_PLAIN
            for (pipe in plan.pipes) {
                if (pipe.system.layer in hidden) continue
                val t = (if (pipe.system == SystemKind.NETWORK) pipe.system.thickness else pipe.sizeOrDefault()) / 2.0
                val y = pipe.height / 1000f
                for (k in 1 until pipe.points.size) {
                    val a = P(pipe.points[k - 1].x.toDouble(), pipe.points[k - 1].y.toDouble())
                    val b = P(pipe.points[k].x.toDouble(), pipe.points[k].y.toDouble())
                    if ((b - a).len < 1) continue
                    room = roomAt((a + b) * 0.5)
                    box(rect((a + b) * 0.5, (b - a).norm(), (b - a).len / 2 + t, t), y - (t / 1000).toFloat(), y + (t / 1000).toFloat(), pipe.system.color)
                }
            }

            // Furniture.
            kind = KIND_PLAIN
            for (i in if (Layer.FURNITURE in hidden) emptyList() else plan.items) {
                room = roomAt(P(i.x.toDouble(), i.y.toDouble()))
                val c = Interior.corners(i)
                if (i.kind != STAIRS) { for (part in Furniture.parts(i)) box(part.corners, (part.z0 / 1000).toFloat(), (part.z1 / 1000).toFloat(), part.color); continue }
                val rise = h + SLAB / 1000f
                val n = maxOf(2, Math.round(rise / 0.175f))
                for (k in 0 until n) {
                    val f0 = k.toDouble() / n; val f1 = (k + 1).toDouble() / n
                    val s0 = c[0] + (c[3] - c[0]) * f0; val s1 = c[1] + (c[2] - c[1]) * f0
                    val e1 = c[1] + (c[2] - c[1]) * f1; val e0 = c[0] + (c[3] - c[0]) * f1
                    box(listOf(s0, s1, e1, e0), 0f, rise * (k + 1) / n, if (k % 2 == 0) 0xB08D6A else 0xA27F5D)
                }
            }

            // 3D models (from Idea 3D): their triangles as they are, turned and placed on this level
            if (Layer.FURNITURE !in hidden) for (m in plan.models) {
                room = roomAt(P(m.x.toDouble(), m.y.toDouble()))
                val a = Math.toRadians(m.rot.toDouble()); val ca = Math.cos(a).toFloat(); val sa = Math.sin(a).toFloat()
                for (part in m.parts) {
                    val q = part.xyz
                    for (k in part.tri.indices step 3) {
                        val i0 = part.tri[k] * 3; val i1 = part.tri[k + 1] * 3; val i2 = part.tri[k + 2] * 3
                        if (maxOf(i0, i1, i2) + 2 >= q.size) continue
                        // turned on the plan: x, y round the model's middle; z is up
                        fun px(i: Int) = q[i] * ca - q[i + 1] * sa
                        fun py(i: Int) = q[i] * sa + q[i + 1] * ca
                        val ux = px(i1) - px(i0); val uy = py(i1) - py(i0); val uz = q[i1 + 2] - q[i0 + 2]
                        val wx = px(i2) - px(i0); val wy = py(i2) - py(i0); val wz = q[i2 + 2] - q[i0 + 2]
                        // normal in plan (x, y) and up (z); the mesh's axes are x, up, plan y
                        var nx = uy * wz - uz * wy; var ny = uz * wx - ux * wz; var nz = ux * wy - uy * wx
                        val l = Math.sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat().coerceAtLeast(1e-9f)
                        nx /= l; ny /= l; nz /= l
                        for (i in intArrayOf(i0, i1, i2))
                            v(P(m.x + px(i).toDouble(), m.y + py(i).toDouble()), (q[i + 2] + m.z) / 1000f, nx, nz, ny, part.color)
                    }
                }
            }

            // Electrical points: plates on the wall at their height (they go down with a lowered wall), lamps hanging.
            for (p in Interior.place(plan).filter { Layer.ELECTRIC !in hidden || it.point.kind == PointKind.LAMP }) {
                if (p.point.kind == PointKind.LAMP) {
                    kind = KIND_PLAIN
                    room = roomAt(p.face)
                    box(rect(p.face, P(1.0, 0.0), 12.0, 12.0), h - 0.4f, h, 0x555555)
                    kind = KIND_GLOW
                    // The renderer uses this otherwise unused wall-centre field to identify this bulb for its fade.
                    midX = p.point.id.toFloat(); midZ = 0f
                    box(rect(p.face, P(1.0, 0.0), 120.0, 120.0), h - 0.58f, h - 0.4f, 0xFFE7A0)
                    lights += Light(p.point.id, (p.face.x / 1000).toFloat(), base + h - 0.65f, (p.face.y / 1000).toFloat(), room, p.point.watts, p.point.switchId)
                    continue
                }
                kind = KIND_WALL
                val m = plan.walls.firstOrNull { w ->
                    val a = plan.pos(w.a); val b = plan.pos(w.b)
                    a != null && b != null && Geo.distToSegment(P(p.point.x.toDouble(), p.point.y.toDouble()), a, b) <= 5
                }?.let { wallMid[it.id] }
                if (m != null) { midX = (m.x / 1000).toFloat(); midZ = (m.y / 1000).toFloat() }
                room = roomAt(p.face + p.normal * 60.0)
                val along = P(p.normal.y, -p.normal.x)
                val y = p.point.height / 1000f
                when (p.point.kind) {
                    PointKind.BOARD -> box(rect(p.face + p.normal * 40.0, along, 150.0, 40.0), y - 0.2f, y + 0.2f, 0x6F7783)
                    // floor to the floor above, through the slab: on every floor they line up into one channel
                    PointKind.JUNCTION -> { kind = KIND_PLAIN; box(rect(p.face, P(1.0, 0.0), 45.0, 45.0), y - 0.03f, y + 0.03f, 0x6F7783); kind = KIND_WALL }
                    PointKind.RISER -> box(rect(p.face + p.normal * 30.0, along, 50.0, 30.0), 0f, h + SLAB / 1000f, 0x8E6CC9)
                    PointKind.APPLIANCE -> box(rect(p.face + p.normal * 12.0, along, 50.0, 12.0), y - 0.05f, y + 0.05f, 0xC0392B)
                    else -> {
                        val color = if (p.point.kind == PointKind.SOCKET) 0xF08A24 else 0x2F7FD6
                        box(rect(p.face + p.normal * 9.0, along, 42.0 * p.point.count, 9.0), y - 0.042f, y + 0.042f, color)
                    }
                }
            }

            // Drawn cables: thin red bars along the wall and the ceiling (or floor).
            kind = KIND_PLAIN
            for (r in if (Layer.ELECTRIC in hidden) emptyList() else plan.routes) {
                val path = Cables.path(plan, r) ?: continue
                for (k in 1 until path.size) {
                    val (x0, y0, z0) = path[k - 1]; val (x1, y1, z1) = path[k]
                    val a = P(x0, z0); val b = P(x1, z1)
                    room = roomAt((a + b) * 0.5)
                    if ((b - a).len < 1) { // vertical run
                        box(rect(a, P(1.0, 0.0), 10.0, 10.0), (minOf(y0, y1) / 1000).toFloat(), (maxOf(y0, y1) / 1000).toFloat(), 0xD0342C)
                    } else {
                        val y = (y0 / 1000).toFloat()
                        box(rect((a + b) * 0.5, (b - a).norm(), (b - a).len / 2 + 10, 10.0), y - 0.01f, y + 0.01f, 0xD0342C)
                    }
                }
            }

            val wells = Stairs.wells(plan)
            // Ceilings go in a list of their own, added last, so the outside view can leave them out.
            kind = KIND_COVER
            target = ceilings
            rooms.forEachIndexed { index, r ->
                room = roomNo(fi, index)
                // an attic (мансарда) room has the sloping roof as its ceiling
                if (plan.roofs.any { it.attic && Roofs.covers(it, r.anchor) }) return@forEachIndexed
                // nor a room the roof comes down into, lower than its ceiling
                if (cap(r.anchor) < h) return@forEachIndexed
                for ((a, b, cc) in Stairs.fillAround(r.polygon, wells)) { v(a, h - 0.001f, 0f, -1f, 0f, 0xFAF8F4); v(b, h - 0.001f, 0f, -1f, 0f, 0xFAF8F4); v(cc, h - 0.001f, 0f, -1f, 0f, 0xFAF8F4) }
            }
            target = out
            }
            val open = out.size / STRIDE
            out.addAll(ceilings)
            return Mesh(out.toFloatArray(), minX, minZ, maxX, maxZ, root.elevation(top) / 1000f + root.floor(top).wallHeight / 1000f, open, lights, ground, groundY, residentParts, residentRoute)
        }
    }
}
