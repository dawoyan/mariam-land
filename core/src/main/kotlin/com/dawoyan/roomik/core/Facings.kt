package com.dawoyan.roomik.core

import kotlinx.serialization.Serializable

/** What a wall's outside (or a stone fence) is clad with. */
@Serializable
enum class Stone(val blockW: Int, val blockH: Int) {
    PLASTER(0, 0),
    /** Armenian tuf: facing blocks, laid in courses. */
    TUF(500, 250),
    TRAVERTINE(600, 300),
    BASALT(400, 200),
    BRICK(250, 65),
    /** Concrete or cinder blocks. */
    BLOCK(390, 190),
}

/** How the stones are laid: smooth (no joints), even courses, half-brick bond (small stones), or random rubble. */
@Serializable
enum class Pattern { SMOOTH, BLOCKS, BOND, RUBBLE }

/** A cladding: [stone] of colour [color] (0xRRGGBB) laid in [pattern]. */
@Serializable
data class Facing(val stone: Stone, val color: Int, val pattern: Pattern = Pattern.BLOCKS)

/** One stone on a face: from [t0] to [t1] mm along it, from [y0] to [y1] mm up, its colour a little lighter or darker. */
class Cell(val t0: Double, val t1: Double, val y0: Double, val y1: Double, val color: Int)

object Facings {
    /** A stone in a colour people know it by. */
    class Preset(val facing: Facing, val en: String, val ru: String, val hy: String) {
        fun name(lang: String) = when (lang) { "ru" -> ru; "hy" -> hy; else -> en }
    }

    val presets = listOf(
        Preset(Facing(Stone.TUF, 0xC98B85), "Tuf pink", "Туф розовый", "Վարդագույն տուֆ"),
        Preset(Facing(Stone.TUF, 0xA9827A), "Tuf dirty pink", "Туф грязно-розовый", "Մոխրավարդագույն տուֆ"),
        Preset(Facing(Stone.TUF, 0xC4704A), "Tuf orange", "Туф оранжевый", "Նարնջագույն տուֆ"),
        Preset(Facing(Stone.TUF, 0xD6B46E), "Tuf yellow", "Туф жёлтый", "Դեղին տուֆ"),
        Preset(Facing(Stone.TUF, 0x4A4342), "Tuf black", "Туф чёрный", "Սև տուֆ"),
        Preset(Facing(Stone.TRAVERTINE, 0xEDE3CF), "Travertine ivory", "Травертин светлый", "Բաց տրավերտին"),
        Preset(Facing(Stone.TRAVERTINE, 0xD9C29A), "Travertine beige", "Травертин бежевый", "Բեժ տրավերտին"),
        Preset(Facing(Stone.TRAVERTINE, 0xD5AE6A), "Travertine yellow", "Травертин жёлтый", "Դեղին տրավերտին"),
        Preset(Facing(Stone.BASALT, 0x34363A), "Basalt black", "Базальт чёрный", "Սև բազալտ"),
        Preset(Facing(Stone.BASALT, 0x6A6D70), "Basalt grey", "Базальт серый", "Մոխրագույն բազալտ"),
        Preset(Facing(Stone.BRICK, 0xA0523D, Pattern.BOND), "Brick red", "Кирпич красный", "Կարմիր աղյուս"),
        Preset(Facing(Stone.BLOCK, 0x9C9C98, Pattern.BOND), "Concrete block", "Бетонный блок", "Բետոնե բլոկ"),
        Preset(Facing(Stone.PLASTER, 0xF1EEE8, Pattern.SMOOTH), "Plaster", "Штукатурка", "Սվաղ"),
    )

    /** Colours to pick for any stone, beyond the presets. */
    val colors = listOf(0xC98B85, 0xA9827A, 0xC4704A, 0xD6B46E, 0x4A4342, 0xEDE3CF, 0xD9C29A, 0x34363A, 0x6A6D70, 0xA0523D, 0x9C9C98, 0xF1EEE8, 0x8E9E7A, 0x7D5A44)

    fun stoneName(s: Stone, lang: String): String = when (s) {
        Stone.PLASTER -> when (lang) { "ru" -> "Штукатурка"; "hy" -> "Սվաղ"; else -> "Plaster" }
        Stone.TUF -> when (lang) { "ru" -> "Туф"; "hy" -> "Տուֆ"; else -> "Tuf" }
        Stone.TRAVERTINE -> when (lang) { "ru" -> "Травертин"; "hy" -> "Տրավերտին"; else -> "Travertine" }
        Stone.BASALT -> when (lang) { "ru" -> "Базальт"; "hy" -> "Բազալտ"; else -> "Basalt" }
        Stone.BRICK -> when (lang) { "ru" -> "Кирпич"; "hy" -> "Աղյուս"; else -> "Brick" }
        Stone.BLOCK -> when (lang) { "ru" -> "Блок"; "hy" -> "Բլոկ"; else -> "Block" }
    }

    /** The preset name when [f] is one, else the stone's name. */
    fun name(f: Facing, lang: String): String =
        (presets.firstOrNull { it.facing == f } ?: presets.firstOrNull { it.facing.stone == f.stone && it.facing.color == f.color })?.name(lang)
            ?: stoneName(f.stone, lang)

    /** Mortar between the stones: a shade darker (lighter for dark stone). */
    fun mortar(f: Facing): Int {
        val lum = ((f.color shr 16 and 255) * 3 + (f.color shr 8 and 255) * 6 + (f.color and 255)) / 10
        return if (lum < 90) shade(f.color, 1.45) else shade(f.color, 0.72)
    }

    fun shade(c: Int, k: Double): Int {
        fun ch(v: Int) = (v * k).toInt().coerceIn(0, 255)
        return (ch(c shr 16 and 255) shl 16) or (ch(c shr 8 and 255) shl 8) or ch(c and 255)
    }

    const val JOINT = 10.0

    /**
     * The stones on a face [len] mm long and [h] mm high, laid as [f] says; [seed] keeps each face's own look
     * (the same each time it is drawn). Empty for a smooth face.
     */
    fun cells(f: Facing, len: Double, h: Double, seed: Int, y0: Double = 0.0): List<Cell> {
        if (f.pattern == Pattern.SMOOTH || f.stone == Stone.PLASTER || len < 20 || h < 20) return emptyList()
        val rnd = java.util.Random(seed * 7919L + 17)
        val bw = (if (f.stone.blockW > 0) f.stone.blockW else 500).toDouble()
        val bh = (if (f.stone.blockH > 0) f.stone.blockH else 250).toDouble()
        val out = ArrayList<Cell>()
        fun col() = shade(f.color, 0.92 + rnd.nextDouble() * 0.16)
        var y = y0; var row = 0
        while (y < h - 1) {
            val rh = if (f.pattern == Pattern.RUBBLE) bh * (0.6 + rnd.nextDouble() * 0.8) else bh
            val y1 = minOf(h, y + rh)
            var t = when (f.pattern) {
                Pattern.RUBBLE -> -rnd.nextDouble() * bw * 0.8
                else -> if (row % 2 == 1) -bw / 2 else 0.0
            }
            while (t < len - 1) {
                val w = if (f.pattern == Pattern.RUBBLE) bw * (0.5 + rnd.nextDouble() * 1.0) else bw
                val a = maxOf(0.0, t); val b = minOf(len, t + w)
                if (b - a > 5) out += Cell(a, b, y, y1, col())
                t += w
            }
            y = y1; row++
        }
        return out
    }

    /** [cells] with the rectangles [holes] (t0, t1, y0, y1) cut out of them. */
    fun cut(cells: List<Cell>, holes: List<DoubleArray>): List<Cell> {
        var cur = cells
        for (hl in holes) {
            val next = ArrayList<Cell>(cur.size + 8)
            for (c in cur) {
                if (c.t1 <= hl[0] || c.t0 >= hl[1] || c.y1 <= hl[2] || c.y0 >= hl[3]) { next += c; continue }
                if (c.t0 < hl[0]) next += Cell(c.t0, hl[0], c.y0, c.y1, c.color)
                if (c.t1 > hl[1]) next += Cell(hl[1], c.t1, c.y0, c.y1, c.color)
                val a = maxOf(c.t0, hl[0]); val b = minOf(c.t1, hl[1])
                if (c.y0 < hl[2]) next += Cell(a, b, c.y0, hl[2], c.color)
                if (c.y1 > hl[3]) next += Cell(a, b, hl[3], c.y1, c.color)
            }
            cur = next
        }
        return cur
    }

    /**
     * Which of wall [w]'s long sides look outside: left, right (from corner a to b). A side is inside when it is in a
     * room, or walled in all round even where the walls do not close into rooms (a gap, a doorway left open).
     */
    fun outsides(plan: Plan, w: Wall, rooms: List<Room>): Pair<Boolean, Boolean> {
        val a = plan.pos(w.a) ?: return false to false; val b = plan.pos(w.b) ?: return false to false
        if ((b - a).len < 1) return false to false
        val n = (b - a).norm().left() * (w.thickness / 2.0 + 80)
        fun out(sign: Double): Boolean {
            // three spots along the wall: most of them decide
            val inside = listOf(0.3, 0.5, 0.7).count { f ->
                val p = a + (b - a) * f + n * sign
                rooms.any { Geo.inside(p, it.polygon) } || enclosed(plan, p, w.id)
            }
            return inside < 2
        }
        return out(1.0) to out(-1.0)
    }

    /** Walls all round [p]: rays in 8 ways, at least 7 meet a wall (other than [skip]). */
    private fun enclosed(plan: Plan, p: P, skip: Int): Boolean {
        val segs = plan.walls.filter { it.id != skip }.mapNotNull { w -> val a = plan.pos(w.a); val b = plan.pos(w.b); if (a == null || b == null) null else a to b }
        if (segs.size < 3) return false
        var hits = 0
        for (k in 0 until 8) {
            val ang = Math.PI / 4 * k + 0.1
            val far = p + P(Math.cos(ang), Math.sin(ang)) * 80000.0
            if (segs.any { (a, b) -> Geo.intersect(p, far, a, b) != null }) hits++
        }
        return hits >= 7
    }

    /** How many of [w]'s long faces look outside. */
    fun cladSides(plan: Plan, w: Wall, rooms: List<Room>): Int = outsides(plan, w, rooms).let { (l, r) -> (if (l) 1 else 0) + (if (r) 1 else 0) }

    /**
     * What [w]'s left and right faces are clad with: as set side by side, else its [Wall.facing] on the outside faces
     * (both, when it has no outside or two).
     */
    fun sides(plan: Plan, w: Wall, rooms: List<Room>): Pair<Facing?, Facing?> {
        if (w.left != null || w.right != null) return w.left to w.right
        val f = w.facing ?: return null to null
        val (l, r) = outsides(plan, w, rooms)
        // standing alone: both faces; inside the house it has no outside face, so none
        return (if (l) f else null) to (if (r) f else null)
    }

    /** Which faces of a wall a stone is put on. */
    enum class Side { OUTSIDE, INSIDE, LEFT, RIGHT, BOTH }

    /** [w] with [f] put on its faces [side] (null: plain), the other faces kept as they are. */
    fun put(plan: Plan, w: Wall, rooms: List<Room>, side: Side, f: Facing?): Wall {
        if (side == Side.OUTSIDE) return w.copy(facing = f, left = null, right = null)
        val (l, r) = sides(plan, w, rooms)
        val (lo, ro) = outsides(plan, w, rooms)
        val inL = !lo; val inR = !ro
        val nl = when (side) { Side.LEFT, Side.BOTH -> f; Side.INSIDE -> if (inL || (!inL && !inR)) f else l; else -> l }
        val nr = when (side) { Side.RIGHT, Side.BOTH -> f; Side.INSIDE -> if (inR || (!inL && !inR)) f else r; else -> r }
        return w.copy(facing = null, left = nl, right = nr)
    }

    /** What [w] has on its faces [side], to start the choice from. */
    fun current(plan: Plan, w: Wall, rooms: List<Room>, side: Side): Facing? {
        val (l, r) = sides(plan, w, rooms); val (lo, ro) = outsides(plan, w, rooms)
        return when (side) {
            Side.LEFT -> l; Side.RIGHT -> r; Side.BOTH -> l ?: r
            Side.OUTSIDE -> w.facing ?: (if (lo) l else null) ?: (if (ro) r else null)
            Side.INSIDE -> (if (!lo) l else null) ?: (if (!ro) r else null)
        }
    }

    /** m² of each stone over the whole house: clad wall faces less their doors and windows, stone fences both sides. */
    fun areas(root: Plan): Map<Facing, Double> {
        val out = LinkedHashMap<Facing, Double>()
        for (plan in root.allFloors) {
            val rooms = Rooms.detect(plan)
            val cuts = Interior.openings(plan)
            for (w in plan.walls) {
                val (l, r) = sides(plan, w, rooms)
                val holes = cuts[w.id].orEmpty().sumOf { (it.t1 - it.t0) * minOf(it.opening.height, plan.heightOf(w) - it.opening.sill).coerceAtLeast(0) }
                val mm2 = (plan.length(w) * plan.heightOf(w) - holes).coerceAtLeast(0.0)
                for (f in listOfNotNull(l, r)) out[f] = (out[f] ?: 0.0) + mm2 / 1e6
            }
            for (fe in plan.fences) {
                val f = Fences.facingOf(fe) ?: continue
                out[f] = (out[f] ?: 0.0) + Fences.faceArea(fe)
            }
        }
        return out
    }

    /** About how many stones cover [m2] of [f] (10 % to spare); 0 for plaster. */
    fun blocks(f: Facing, m2: Double): Int =
        if (f.stone.blockW == 0) 0 else Math.ceil(m2 * 1e6 * 1.1 / (f.stone.blockW * f.stone.blockH)).toInt()
}
