package com.dawoyan.roomik.core

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Things built in the yard, made from a few sizes: canopies (навесы) most of all, and a gazebo, arch, swing, grill, tree. */
enum class YardKind { CANOPY, GAZEBO, ARCH, SWING, BBQ, GREEN_AREA, TREE,
    /** A tall narrow poplar; a cypress or thuja (a slim cone); a power pole with its cross-arm; a rock or boulder. */
    POPLAR, CYPRESS, POLE, ROCK }

/** Things that grow or lie there, not built: nothing to buy for them. */
val YardKind.natural: Boolean get() = this == YardKind.GREEN_AREA || this == YardKind.TREE || this == YardKind.POPLAR || this == YardKind.CYPRESS || this == YardKind.ROCK

/** A canopy's roof: one slope, two slopes (gable), or a curved vault. */
enum class RoofShape { SLOPE, GABLE, ARCH }

/** What carries the roof across: plain rafters, a triangle truss, a truss with zig-zag braces (Warren), an arched truss. */
enum class TrussKind { NONE, TRIANGLE, WARREN, ARCHED }

/** The roof's covering; [color] 0xRRGGBB, [sheet] its usual sheet size in mm (width × length) for counting sheets. */
enum class Cover(val color: Int, val sheetW: Int, val sheetL: Int) {
    POLYCARBONATE(0x9CC9DE, 2100, 6000), PROFILE_SHEET(0x3E6B48, 1150, 6000), METAL_TILE(0x8E3B2F, 1180, 3600),
    SOFT_TILE(0x6B2E2A, 1000, 3000), GLASS(0xBFE3EA, 1000, 2000), NONE(0, 0, 0),
}

/**
 * A yard construction as its sizes: [w] along x, [d] along y (the depth, across which trusses span), [h] the height of
 * the eaves (or the whole thing for a tree, arch, swing), [pitch] of the roof in degrees, posts no more than [step] mm
 * apart, [post] the post's square section, mm.
 */
@Serializable
data class YardSpec(
    val kind: YardKind,
    val w: Int = 6000, val d: Int = 4000, val h: Int = 2500,
    val roof: RoofShape = RoofShape.GABLE, val truss: TrussKind = TrussKind.TRIANGLE, val cover: Cover = Cover.POLYCARBONATE,
    val pitch: Int = 15, val step: Int = 3000, val post: Int = 80,
)

/** Makes a yard construction's 3D parts from its [YardSpec], and what it takes to build it. */
object Yard {
    private class V(val x: Double, val y: Double, val z: Double) {
        operator fun plus(o: V) = V(x + o.x, y + o.y, z + o.z)
        operator fun minus(o: V) = V(x - o.x, y - o.y, z - o.z)
        operator fun times(k: Double) = V(x * k, y * k, z * k)
        fun cross(o: V) = V(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
        fun dot(o: V) = x * o.x + y * o.y + z * o.z
        val len get() = sqrt(dot(this))
        fun norm() = this * (1.0 / len.coerceAtLeast(1e-9))
    }

    /** Triangles of one part, and the length of bars and area of sheets it is made of (for the bill). */
    private class Part(val name: String, val color: Int, val profile: String = "") {
        val xyz = ArrayList<Float>(); val tri = ArrayList<Int>()
        var length = 0.0; var area = 0.0; var count = 0
        fun vert(p: V): Int { xyz += p.x.toFloat(); xyz += p.y.toFloat(); xyz += p.z.toFloat(); return xyz.size / 3 - 1 }
        /** A triangle facing away from [inside]. */
        fun tri(a: V, b: V, c: V, inside: V) {
            val n = (b - a).cross(c - a)
            val mid = (a + b + c) * (1.0 / 3)
            val (p, q) = if (n.dot(mid - inside) >= 0) b to c else c to b
            tri += vert(a); tri += vert(p); tri += vert(q)
        }
        /** A box from its 8 corners: the bottom four (round), then the top four above them. */
        fun box(c: List<V>) {
            val mid = c.fold(V(0.0, 0.0, 0.0)) { s, v -> s + v } * (1.0 / 8)
            val faces = listOf(intArrayOf(0, 1, 2, 3), intArrayOf(4, 5, 6, 7), intArrayOf(0, 1, 5, 4), intArrayOf(1, 2, 6, 5), intArrayOf(2, 3, 7, 6), intArrayOf(3, 0, 4, 7))
            for (f in faces) { tri(c[f[0]], c[f[1]], c[f[2]], mid); tri(c[f[0]], c[f[2]], c[f[3]], mid) }
        }
        /** A bar from [a] to [b], [sw] × [sh] mm in section ([sh] along "up" where it can be). */
        fun bar(a: V, b: V, sw: Double, sh: Double = sw) {
            val u = (b - a).norm()
            val up = V(0.0, 0.0, 1.0)
            var s = u.cross(up); if (s.len < 1e-6) s = V(1.0, 0.0, 0.0)
            s = s.norm(); val t = s.cross(u).norm()
            fun at(p: V, i: Int, j: Int) = p + s * (i * sw / 2) + t * (j * sh / 2)
            box(listOf(at(a, -1, -1), at(a, 1, -1), at(a, 1, 1), at(a, -1, 1), at(b, -1, -1), at(b, 1, -1), at(b, 1, 1), at(b, -1, 1)))
            length += (b - a).len; count++
        }
        /** A thin sheet over the four corners [q] (round), [th] thick, counted as its top area. */
        fun sheet(q: List<V>, th: Double) {
            val n = (q[1] - q[0]).cross(q[3] - q[0]).norm().let { if (it.z < 0) it * -1.0 else it }
            box(q + q.map { it + n * th })
            area += (q[1] - q[0]).cross(q[3] - q[0]).len
        }
        fun part() = ModelPart(name, color, xyz, tri)
    }

    private const val STEEL = 0x4A4F57
    private const val WOOD = 0x9C6B3E

    /** The construction's parts, centred on (0, 0) and standing on z = 0, named in Russian or English. */
    fun parts(s: YardSpec, ru: Boolean): List<ModelPart> = build(s, ru).map { it.part() }.filter { it.tri.isNotEmpty() }

    /** What it takes: bars by part (metres, pieces) and sheets (m², sheets of the usual size). */
    class Line(val name: String, val qty: Double, val unit: String, val note: String = "")

    fun bill(s: YardSpec, ru: Boolean): List<Line> {
        fun t(en: String, r: String) = if (ru) r else en
        val m = t("m", "м"); val pcs = t("pcs", "шт"); val m2 = t("m²", "м²")
        val out = ArrayList<Line>()
        if (s.kind.natural) return out
        for (p in build(s, ru)) {
            if (p.length > 0) out += Line(p.name + (if (p.profile.isNotEmpty()) " " + p.profile else ""), Math.round(p.length / 100.0) / 10.0, m, t("${p.count} pcs", "${p.count} шт"))
            if (p.area > 0) {
                val a = p.area / 1e6
                val sheets = if (s.cover.sheetW > 0) ceil(a * 1.1 / (s.cover.sheetW * s.cover.sheetL / 1e6)).toInt() else 0
                out += Line(p.name, Math.round(a * 10) / 10.0, m2, if (sheets > 0) t("≈ $sheets sheets ${s.cover.sheetW}×${s.cover.sheetL} with 10 %", "≈ $sheets листов ${s.cover.sheetW}×${s.cover.sheetL} с запасом 10 %") else "")
            }
        }
        return out
    }

    private fun build(s: YardSpec, ru: Boolean): List<Part> = when (s.kind) {
        YardKind.CANOPY -> canopy(s, ru, STEEL)
        YardKind.GAZEBO -> gazebo(s, ru)
        YardKind.ARCH -> arch(s, ru)
        YardKind.SWING -> swing(s, ru)
        YardKind.BBQ -> bbq(s, ru)
        YardKind.GREEN_AREA -> greenArea(s, ru)
        YardKind.TREE -> tree(s, ru)
        YardKind.POPLAR -> poplar(s, ru)
        YardKind.CYPRESS -> cypress(s, ru)
        YardKind.POLE -> pole(s, ru)
        YardKind.ROCK -> rock(s, ru)
    }

    /**
     * A round shape about [c]: rings from the top down, each ring's radius [r] at 0..1 from the top (and its height
     * [z] there), every point pushed in or out by up to [jitter] of the radius ([seed] keeps it the same each time).
     */
    private fun blob(part: Part, c: V, rings: Int, segs: Int, r: (Double) -> Double, z: (Double) -> Double, jitter: Double = 0.0, seed: Long = 1) {
        val rnd = java.util.Random(seed)
        val pts = (0..rings).map { i -> val t = i.toDouble() / rings
            (0 until segs).map { j -> val b = 2 * PI * j / segs; val k = 1 + (rnd.nextDouble() - 0.5) * 2 * jitter
                V(c.x + cos(b) * r(t) * k, c.y + sin(b) * r(t) * k, z(t)) } }
        for (i in 0 until rings) for (j in 0 until segs) {
            val a = pts[i][j]; val b = pts[i][(j + 1) % segs]; val p = pts[i + 1][j]; val q = pts[i + 1][(j + 1) % segs]
            val mid = V(c.x, c.y, (a.z + p.z) / 2)
            part.tri(a, b, q, mid); part.tri(a, q, p, mid)
        }
    }

    private fun poplar(s: YardSpec, ru: Boolean): List<Part> {
        val h = s.h.coerceIn(3000, 30000).toDouble(); val r = (s.w.coerceAtLeast(600) / 2).toDouble()
        val trunk = Part(t(ru, "Trunk", "Ствол"), 0x7A6A55)
        val crown = Part(t(ru, "Crown", "Крона"), 0x5E7F3C)
        trunk.bar(V(0.0, 0.0, 0.0), V(0.0, 0.0, h * 0.3), h * 0.02 + 100, h * 0.02 + 100)
        // a narrow spindle from a fifth of the height up to the top
        blob(crown, V(0.0, 0.0, 0.0), 10, 9, { t -> r * sin(PI * t).coerceAtLeast(0.0).let { Math.pow(it, 0.8) } }, { t -> h - (h * 0.8) * t }, 0.12, s.w.toLong() + s.h)
        return listOf(trunk, crown)
    }

    private fun cypress(s: YardSpec, ru: Boolean): List<Part> {
        val h = s.h.coerceIn(800, 25000).toDouble(); val r = (s.w.coerceAtLeast(300) / 2).toDouble()
        val trunk = Part(t(ru, "Trunk", "Ствол"), 0x6B4A2E)
        val crown = Part(t(ru, "Crown", "Крона"), 0x2F5A32)
        trunk.bar(V(0.0, 0.0, 0.0), V(0.0, 0.0, h * 0.12), 120.0, 120.0)
        // a slim cone, round at the bottom, coming to a point on top
        blob(crown, V(0.0, 0.0, 0.0), 9, 10, { t -> r * (if (t > 0.85) (1 - t) / 0.15 else Math.pow(t / 0.85, 0.75)) }, { t -> h - (h * 0.92) * t }, 0.08, s.w.toLong() * 3 + s.h)
        return listOf(trunk, crown)
    }

    private fun pole(s: YardSpec, ru: Boolean): List<Part> {
        val h = s.h.coerceIn(3000, 20000).toDouble()
        val pole = Part(t(ru, "Pole", "Опора"), 0xB5B1A8, "")
        val arm = Part(t(ru, "Cross-arm", "Траверса"), STEEL)
        val ins = Part(t(ru, "Insulators", "Изоляторы"), 0xDCE3E8)
        // a tapering concrete pole: 3 sections, thinner going up
        val b0 = s.post.coerceIn(150, 500).toDouble()
        for (k in 0 until 3) pole.bar(V(0.0, 0.0, h * k / 3), V(0.0, 0.0, h * (k + 1) / 3), b0 * (1 - k * 0.18), b0 * (1 - k * 0.18))
        val aw = s.w.coerceIn(600, 3000).toDouble()
        arm.bar(V(-aw / 2, 0.0, h - 400), V(aw / 2, 0.0, h - 400), 80.0, 80.0)
        for (x in listOf(-aw / 2 + 80, 0.0, aw / 2 - 80)) ins.bar(V(x, 0.0, h - 360), V(x, 0.0, h - 160), 70.0, 70.0)
        // a street lamp on an arm towards the front
        arm.bar(V(0.0, 0.0, h - 1300), V(0.0, -900.0, h - 1100), 50.0, 50.0)
        ins.bar(V(0.0, -900.0, h - 1160), V(0.0, -1150.0, h - 1160), 160.0, 90.0)
        return listOf(pole, arm, ins)
    }

    private fun rock(s: YardSpec, ru: Boolean): List<Part> {
        val rx = (s.w.coerceAtLeast(150) / 2).toDouble(); val h = s.h.coerceAtLeast(100).toDouble()
        val stone = Part(t(ru, "Rock", "Камень"), 0x5E5F62)
        // a lumpy boulder sitting a little into the ground
        blob(stone, V(0.0, 0.0, 0.0), 6, 9, { t -> rx * Math.pow(sin(PI * t), 0.55) }, { t -> h - h * 1.1 * t }, 0.22, s.w.toLong() * 7 + s.d)
        return listOf(stone)
    }

    /** A low, softly raised lawn/planting bed.  It may be sized to any part of the plot. */
    private fun greenArea(s: YardSpec, ru: Boolean): List<Part> {
        val lawn = Part(t(ru, "Green area", "Зелёная зона"), 0x4F8A45)
        val w = s.w.coerceAtLeast(300).toDouble() / 2
        val d = s.d.coerceAtLeast(300).toDouble() / 2
        lawn.box(listOf(V(-w, -d, -35.0), V(w, -d, -35.0), V(w, d, -35.0), V(-w, d, -35.0),
            V(-w, -d, 0.0), V(w, -d, 0.0), V(w, d, 0.0), V(-w, d, 0.0)))
        return listOf(lawn)
    }

    private fun t(ru: Boolean, en: String, r: String) = if (ru) r else en

    /** Positions along [len] (centred on 0) for posts no more than [step] apart, ends included. */
    private fun spots(len: Double, step: Int): List<Double> {
        val n = max(1, ceil(len / step.coerceAtLeast(500)).toInt())
        return (0..n).map { -len / 2 + len * it / n }
    }

    /** The roof's height over the eaves at depth y (−d/2 front … d/2 back). */
    private fun roofZ(s: YardSpec, y: Double): Double {
        val d = s.d.toDouble(); val k = tan(Math.toRadians(s.pitch.toDouble()))
        return when (s.roof) {
            RoofShape.SLOPE -> (y + d / 2) * k
            RoofShape.GABLE -> (d / 2 - abs(y)) * k
            RoofShape.ARCH -> { val rise = d / 2 * k * 1.3; val r = y / (d / 2); rise * (1 - r * r) }
        }
    }

    private fun canopy(s: YardSpec, ru: Boolean, metal: Int): List<Part> {
        val w = s.w.toDouble(); val d = s.d.toDouble(); val h = s.h.toDouble(); val ps = s.post.toDouble()
        val posts = Part(t(ru, "Posts", "Столбы"), metal, "${s.post}×${s.post}")
        val beams = Part(t(ru, "Beams", "Балки"), metal, "100×50")
        val chords = Part(t(ru, "Trusses", "Фермы"), metal, "60×40")
        val braces = Part(t(ru, "Truss braces", "Раскосы ферм"), metal, "40×20")
        val purlins = Part(t(ru, "Purlins", "Обрешётка"), metal, "40×20")
        val cover = Part(t(ru, "Covering", "Покрытие") + " · " + coverName(s.cover, ru), s.cover.color)
        val xs = spots(w, s.step)
        val yEdges = listOf(-d / 2, d / 2)
        // posts on the front and back rows, beams along them
        for (y in yEdges) {
            for (x in xs) posts.bar(V(x, y, 0.0), V(x, y, h + (if (s.roof == RoofShape.SLOPE) roofZ(s, y) else 0.0)), ps)
            val z = h + (if (s.roof == RoofShape.SLOPE) roofZ(s, y) else 0.0)
            beams.bar(V(-w / 2 - 100, y, z + 50), V(w / 2 + 100, y, z + 50), 50.0, 100.0)
        }
        // a truss over every post pair, and more between when posts are far apart (at most 1.5 m)
        val trussXs = spots(w, 1500)
        val base = h + 100
        val ys = (0..16).map { -d / 2 + d * it / 16 }
        for (x in trussXs) {
            fun top(y: Double) = V(x, y, base + roofZ(s, y) + (if (s.roof == RoofShape.SLOPE) 0.0 else 0.0))
            when (s.truss) {
                TrussKind.NONE -> {
                    // rafters along the roof line
                    val line = if (s.roof == RoofShape.ARCH) ys else if (s.roof == RoofShape.GABLE) listOf(-d / 2, 0.0, d / 2) else listOf(-d / 2, d / 2)
                    for (k in 1 until line.size) chords.bar(top(line[k - 1]), top(line[k]), 40.0, 60.0)
                }
                else -> {
                    // top chord on the roof line, bottom chord straight (sloping with a single-slope roof)
                    val line = if (s.roof == RoofShape.ARCH || s.truss == TrussKind.ARCHED) ys else if (s.roof == RoofShape.GABLE) listOf(-d / 2, 0.0, d / 2) else listOf(-d / 2, d / 2)
                    for (k in 1 until line.size) chords.bar(top(line[k - 1]), top(line[k]), 40.0, 60.0)
                    fun bottom(y: Double) = V(x, y, base + if (s.roof == RoofShape.SLOPE) roofZ(s, y) * 0.0 else 0.0)
                    val depth = if (s.roof == RoofShape.SLOPE) 300.0 else 0.0
                    fun low(y: Double) = if (s.roof == RoofShape.SLOPE) V(x, y, base + roofZ(s, y) - depth) else bottom(y)
                    chords.bar(low(-d / 2 + (if (s.roof == RoofShape.SLOPE) 0.0 else 0.0)), low(d / 2), 40.0, 60.0)
                    when (s.truss) {
                        TrussKind.TRIANGLE -> {
                            // a king post and two struts
                            braces.bar(low(0.0), top(0.0), 40.0, 20.0)
                            braces.bar(low(-d / 4), top(0.0), 40.0, 20.0); braces.bar(low(d / 4), top(0.0), 40.0, 20.0)
                        }
                        TrussKind.WARREN, TrussKind.ARCHED -> {
                            val n = max(4, (d / 500).toInt() / 2 * 2)
                            for (k in 0 until n) {
                                val y0 = -d / 2 + d * k / n; val y1 = -d / 2 + d * (k + 1) / n
                                if (k % 2 == 0) braces.bar(low(y0), top(y1), 40.0, 20.0) else braces.bar(top(y0), low(y1), 40.0, 20.0)
                                if (s.truss == TrussKind.ARCHED && k > 0) braces.bar(low(y0), top(y0), 40.0, 20.0)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
        // the bill counts whole trusses, not their pieces
        if (s.truss != TrussKind.NONE) chords.count = trussXs.size
        // purlins across the trusses, about every 600 mm along the roof line
        val roofLen = (1 until ys.size).sumOf { k -> sqrt((ys[k] - ys[k - 1]).let { it * it } + (roofZ(s, ys[k]) - roofZ(s, ys[k - 1])).let { it * it }) }
        val nPurl = max(2, (roofLen / 600).toInt())
        for (k in 0..nPurl) {
            val y = -d / 2 + d * k / nPurl
            val z = base + roofZ(s, y) + 40
            purlins.bar(V(-w / 2 - 150, y, z), V(w / 2 + 150, y, z), 40.0, 20.0)
        }
        // the covering over it all, a little over the edges
        if (s.cover != Cover.NONE) {
            val line = if (s.roof == RoofShape.ARCH) ys else if (s.roof == RoofShape.GABLE) listOf(-d / 2, 0.0, d / 2) else listOf(-d / 2, d / 2)
            val over = 200.0
            for (k in 1 until line.size) {
                val y0 = line[k - 1]; val y1 = line[k]
                val e0 = if (k == 1) -over else 0.0; val e1 = if (k == line.size - 1) over else 0.0
                val z0 = base + roofZ(s, y0) + 60 + (if (k == 1) -e0 * tan(Math.toRadians(s.pitch.toDouble())) * 0 else 0.0)
                val z1 = base + roofZ(s, y1) + 60
                cover.sheet(listOf(V(-w / 2 - over, y0 + e0, z0), V(w / 2 + over, y0 + e0, z0), V(w / 2 + over, y1 + e1, z1), V(-w / 2 - over, y1 + e1, z1)), if (s.cover == Cover.SOFT_TILE) 20.0 else 8.0)
            }
        }
        return listOf(posts, beams, chords, braces, purlins, cover)
    }

    private fun gazebo(s: YardSpec, ru: Boolean): List<Part> {
        val w = s.w.toDouble(); val d = s.d.toDouble(); val h = s.h.toDouble()
        val posts = Part(t(ru, "Posts", "Столбы"), WOOD, "100×100")
        val floor = Part(t(ru, "Deck", "Настил пола"), 0xB08D6A)
        val rails = Part(t(ru, "Railing", "Ограждение"), WOOD, "50×50")
        val rafters = Part(t(ru, "Rafters", "Стропила"), WOOD, "50×100")
        val cover = Part(t(ru, "Roof", "Кровля") + " · " + coverName(s.cover, ru), if (s.cover == Cover.NONE) 0x6B2E2A else s.cover.color)
        floor.sheet(listOf(V(-w / 2, -d / 2, 100.0), V(w / 2, -d / 2, 100.0), V(w / 2, d / 2, 100.0), V(-w / 2, d / 2, 100.0)), 40.0)
        val corners = listOf(V(-w / 2, -d / 2, 0.0), V(w / 2, -d / 2, 0.0), V(w / 2, d / 2, 0.0), V(-w / 2, d / 2, 0.0))
        for (c in corners) posts.bar(c, V(c.x, c.y, h), 100.0)
        for (i in corners.indices) {
            val a = corners[i]; val b = corners[(i + 1) % 4]
            rails.bar(V(a.x, a.y, h), V(b.x, b.y, h), 100.0, 150.0)
            // railing on three sides; the front is the way in
            if (i != 0) { rails.bar(V(a.x, a.y, 900.0), V(b.x, b.y, 900.0), 50.0); rails.bar(V(a.x, a.y, 400.0), V(b.x, b.y, 400.0), 50.0) }
        }
        // a hip roof: four slopes to one top
        val k = tan(Math.toRadians(s.pitch.coerceAtLeast(20).toDouble()))
        val apex = V(0.0, 0.0, h + minOf(w, d) / 2 * k)
        val over = 300.0
        val eaves = listOf(V(-w / 2 - over, -d / 2 - over, h - over * k), V(w / 2 + over, -d / 2 - over, h - over * k), V(w / 2 + over, d / 2 + over, h - over * k), V(-w / 2 - over, d / 2 + over, h - over * k))
        for (e in eaves) rafters.bar(e, apex, 50.0, 100.0)
        for (i in eaves.indices) {
            val a = eaves[i]; val b = eaves[(i + 1) % 4]
            val mid = (a + b) * 0.5
            cover.tri(a, b, apex, V(0.0, 0.0, 0.0)); cover.tri(a, apex, b, V(0.0, 0.0, -1e6))
            cover.area += (b - a).cross(apex - a).len / 2
            rafters.bar(V(mid.x, mid.y, mid.z), apex, 50.0, 100.0)
        }
        return listOf(posts, floor, rails, rafters, cover)
    }

    private fun arch(s: YardSpec, ru: Boolean): List<Part> {
        val w = s.w.coerceAtMost(4000).toDouble(); val d = s.d.coerceAtMost(1500).toDouble(); val h = s.h.toDouble()
        val metal = Part(t(ru, "Arch", "Арка"), 0x2B2D31, "20×20")
        for (y in listOf(-d / 2, d / 2)) {
            metal.bar(V(-w / 2, y, 0.0), V(-w / 2, y, h), 40.0); metal.bar(V(w / 2, y, 0.0), V(w / 2, y, h), 40.0)
            val n = 12
            for (k in 0 until n) {
                val a0 = PI * k / n; val a1 = PI * (k + 1) / n
                metal.bar(V(-cos(a0) * w / 2, y, h + sin(a0) * w / 2), V(-cos(a1) * w / 2, y, h + sin(a1) * w / 2), 30.0)
            }
        }
        // rungs across, up the sides and over the top
        for (k in 0..14) {
            val a = PI * k / 14
            val p = if (k == 0 || k == 14) null else V(-cos(a) * w / 2, 0.0, h + sin(a) * w / 2)
            if (p != null) metal.bar(V(p.x, -d / 2, p.z), V(p.x, d / 2, p.z), 20.0)
        }
        for (z in listOf(500.0, 1100.0, 1700.0)) for (x in listOf(-w / 2, w / 2)) metal.bar(V(x, -d / 2, z), V(x, d / 2, z), 20.0)
        return listOf(metal)
    }

    private fun swing(s: YardSpec, ru: Boolean): List<Part> {
        val w = s.w.coerceIn(1500, 3500).toDouble(); val h = s.h.coerceIn(1800, 3000).toDouble(); val spread = h * 0.5
        val frame = Part(t(ru, "Frame", "Рама"), WOOD, "100×100")
        val seat = Part(t(ru, "Seat", "Сиденье"), 0xC0392B)
        val ropes = Part(t(ru, "Chains", "Цепи"), 0x777777)
        for (x in listOf(-w / 2, w / 2)) { frame.bar(V(x, -spread / 2, 0.0), V(x, 0.0, h), 100.0); frame.bar(V(x, spread / 2, 0.0), V(x, 0.0, h), 100.0) }
        frame.bar(V(-w / 2 - 150, 0.0, h + 50), V(w / 2 + 150, 0.0, h + 50), 100.0, 150.0)
        val sw = minOf(1200.0, w - 400)
        seat.sheet(listOf(V(-sw / 2, -200.0, 450.0), V(sw / 2, -200.0, 450.0), V(sw / 2, 200.0, 450.0), V(-sw / 2, 200.0, 450.0)), 40.0)
        for (x in listOf(-sw / 2 + 50, sw / 2 - 50)) ropes.bar(V(x, 0.0, 490.0), V(x, 0.0, h), 15.0)
        return listOf(frame, seat, ropes)
    }

    private fun bbq(s: YardSpec, ru: Boolean): List<Part> {
        val w = s.w.coerceIn(800, 3000).toDouble(); val d = s.d.coerceIn(500, 1200).toDouble()
        val base = Part(t(ru, "Brick base", "Кирпичное основание"), 0xA0523D)
        val box = Part(t(ru, "Grill", "Мангал"), 0x2B2D31)
        val chimney = Part(t(ru, "Chimney", "Дымоход"), 0x8A4A36)
        base.box(listOf(V(-w / 2, -d / 2, 0.0), V(w / 2, -d / 2, 0.0), V(w / 2, d / 2, 0.0), V(-w / 2, d / 2, 0.0), V(-w / 2, -d / 2, 800.0), V(w / 2, -d / 2, 800.0), V(w / 2, d / 2, 800.0), V(-w / 2, d / 2, 800.0)))
        base.area = (2 * (w + d) * 800) // its faces, for the bricks
        val gw = minOf(1000.0, w * 0.6)
        box.box(listOf(V(-gw / 2, -d / 2 + 50, 800.0), V(gw / 2, -d / 2 + 50, 800.0), V(gw / 2, d / 2 - 50, 800.0), V(-gw / 2, d / 2 - 50, 800.0),
            V(-gw / 2, -d / 2 + 50, 1000.0), V(gw / 2, -d / 2 + 50, 1000.0), V(gw / 2, d / 2 - 50, 1000.0), V(-gw / 2, d / 2 - 50, 1000.0)))
        if (s.h > 1500) chimney.bar(V(0.0, d / 2 - 150, 1000.0), V(0.0, d / 2 - 150, s.h.toDouble()), 300.0)
        return listOf(base, box, chimney)
    }

    private fun tree(s: YardSpec, ru: Boolean): List<Part> {
        val h = s.h.coerceIn(1500, 20000).toDouble(); val r = (s.w.coerceAtLeast(800) / 2).toDouble()
        val trunk = Part(t(ru, "Trunk", "Ствол"), 0x6B4A2E)
        val crown = Part(t(ru, "Crown", "Крона"), 0x4F7A3A)
        trunk.bar(V(0.0, 0.0, 0.0), V(0.0, 0.0, h * 0.55), h * 0.04 + 80, h * 0.04 + 80)
        // a rounded crown: rings of a squashed sphere
        val c = V(0.0, 0.0, h - r * 0.9); val rings = 6; val segs = 10
        val pts = (0..rings).map { i -> val a = PI * i / rings; (0 until segs).map { j -> val b = 2 * PI * j / segs; V(c.x + sin(a) * cos(b) * r, c.y + sin(a) * sin(b) * r, c.z + cos(a) * r * 0.9) } }
        for (i in 0 until rings) for (j in 0 until segs) {
            val a = pts[i][j]; val b = pts[i][(j + 1) % segs]; val p = pts[i + 1][j]; val q = pts[i + 1][(j + 1) % segs]
            crown.tri(a, b, q, c); crown.tri(a, q, p, c)
        }
        return listOf(trunk, crown)
    }

    fun coverName(c: Cover, ru: Boolean) = when (c) {
        Cover.POLYCARBONATE -> if (ru) "поликарбонат" else "polycarbonate"
        Cover.PROFILE_SHEET -> if (ru) "профлист" else "profiled sheet"
        Cover.METAL_TILE -> if (ru) "металлочерепица" else "metal tile"
        Cover.SOFT_TILE -> if (ru) "мягкая кровля" else "soft tile"
        Cover.GLASS -> if (ru) "стекло" else "glass"
        Cover.NONE -> if (ru) "без покрытия" else "no covering"
    }

    /** A good start for each kind. */
    fun default(kind: YardKind): YardSpec = when (kind) {
        YardKind.CANOPY -> YardSpec(kind)
        YardKind.GAZEBO -> YardSpec(kind, 3000, 3000, 2300, cover = Cover.SOFT_TILE, pitch = 30)
        YardKind.ARCH -> YardSpec(kind, 1400, 600, 2000)
        YardKind.SWING -> YardSpec(kind, 2000, 1500, 2200)
        YardKind.BBQ -> YardSpec(kind, 1500, 700, 2200)
        YardKind.GREEN_AREA -> YardSpec(kind, 4000, 3000, 300)
        YardKind.TREE -> YardSpec(kind, 3000, 3000, 5000)
        YardKind.POPLAR -> YardSpec(kind, 1600, 1600, 14000)
        YardKind.CYPRESS -> YardSpec(kind, 900, 900, 2500)
        YardKind.POLE -> YardSpec(kind, 1500, 300, 9000, post = 250)
        YardKind.ROCK -> YardSpec(kind, 1600, 1100, 900)
    }
}
