package com.dawoyan.roomik.core

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** One block of a piece of furniture: its outline on the plan (inside on the left), bottom and top in mm, colour. */
class FPart(val corners: List<P>, val z0: Double, val z1: Double, val color: Int,
    /** Laid of these stones: drawn on its two long sides (corners 0→1 and 2→3). */
    val clad: Facing? = null)

/**
 * Furniture as a few blocks that look like what they are: a bed has a frame, a mattress, a headboard and pillows; a
 * table has a top and legs; a car has a body, a cabin, glass and wheels. Each item's front is its c0–c1 edge (the side
 * [Interior.corners] starts with); its back is the far edge. Sizes follow the item, so a resized bed stays a bed.
 */
object Furniture {
    private const val WOOD = 0xB98E63; private const val DARK = 0x6E5440; private const val WHITE = 0xF3F3F0
    private const val METAL = 0x9AA0A6; private const val BLACK = 0x2B2D31; private const val GLASS = 0x8DB6CF
    private const val STONE = 0xC9C4BA

    fun parts(i: Item): List<FPart> {
        val r = Math.toRadians(i.rot.toDouble())
        val u = P(cos(r), sin(r)); val v = u.left()
        val c = P(i.x.toDouble(), i.y.toDouble())
        val w = i.w.toDouble(); val d = i.d.toDouble(); val h = i.h.toDouble()
        val out = ArrayList<FPart>()
        /** A block from x0..x1 across, y0..y1 from the front (both in mm from the item's left-front corner), z0..z1 up. */
        fun b(x0: Double, x1: Double, y0: Double, y1: Double, z0: Double, z1: Double, col: Int) {
            fun at(x: Double, y: Double) = c + u * (x - w / 2) + v * (y - d / 2)
            out += FPart(listOf(at(x0, y0), at(x1, y0), at(x1, y1), at(x0, y1)), z0, z1, col)
        }
        fun legs(t: Double, z1: Double, col: Int, inset: Double = 0.0) {
            b(inset, inset + t, inset, inset + t, 0.0, z1, col); b(w - inset - t, w - inset, inset, inset + t, 0.0, z1, col)
            b(inset, inset + t, d - inset - t, d - inset, 0.0, z1, col); b(w - inset - t, w - inset, d - inset - t, d - inset, 0.0, z1, col)
        }
        val base = Catalog.of(i.kind).color
        when (i.kind) {
            "bed", "bed1" -> {
                b(0.0, w, 0.0, d - 60, 0.0, 300.0, WOOD)                       // frame
                b(30.0, w - 30, 30.0, d - 80, 300.0, 480.0, WHITE)             // mattress
                b(30.0, w - 30, 30.0, d * 0.62, 480.0, 500.0, base)            // blanket
                b(0.0, w, d - 60, d, 0.0, 950.0, DARK)                         // headboard at the back
                val n = if (w >= 1200) 2 else 1
                val pw = (w - 60 - 40.0 * (n + 1)) / n
                for (k in 0 until n) { val x0 = 30 + 40 + k * (pw + 40); b(x0, x0 + pw, d - 480, d - 120, 480.0, 600.0, WHITE) } // pillows
            }
            "sofa", "armchair" -> {
                val arm = min(180.0, w * 0.18)
                b(0.0, w, 0.0, d, 0.0, 120.0, DARK)                            // plinth
                b(arm, w - arm, 0.0, d - 200, 120.0, 430.0, base)              // seat
                b(0.0, w, d - 200, d, 120.0, h, base)                          // back
                b(0.0, arm, 0.0, d - 200, 120.0, 620.0, base)                  // arms
                b(w - arm, w, 0.0, d - 200, 120.0, 620.0, base)
                if (i.kind == "sofa") { val m = w / 2; b(m - 8, m + 8, 30.0, d - 230, 430.0, 440.0, DARK) } // seam between cushions
            }
            "table", "desk" -> {
                b(0.0, w, 0.0, d, h - 35, h, base)
                legs(50.0, h - 35, DARK, 40.0)
                if (i.kind == "desk") b(w - 450, w - 40, 40.0, d - 40, h - 220, h - 35, base) // drawers
            }
            "chair" -> {
                b(0.0, w, 0.0, d, 420.0, 460.0, base)
                legs(35.0, 420.0, DARK)
                b(0.0, w, d - 40, d, 460.0, 900.0, base)                       // back
            }
            "wardrobe" -> {
                b(0.0, w, 0.0, d, 60.0, h, base)
                b(20.0, w - 20, 20.0, d, 0.0, 60.0, DARK)                      // plinth
                b(w / 2 - 5, w / 2 + 5, -6.0, 0.0, 60.0, h - 20, DARK)         // gap between doors
                b(w / 2 - 70, w / 2 - 50, -26.0, -6.0, h * 0.45, h * 0.6, METAL) // handles
                b(w / 2 + 50, w / 2 + 70, -26.0, -6.0, h * 0.45, h * 0.6, METAL)
            }
            "tv" -> {
                b(0.0, w, 0.0, d, 0.0, 450.0, base)                            // stand
                b(w * 0.4, w * 0.6, d * 0.5, d * 0.65, 450.0, 520.0, BLACK)    // foot
                b(w * 0.08, w * 0.92, d * 0.55, d * 0.6, 520.0, 520.0 + w * 0.47, BLACK) // screen
            }
            "cabinet", "sink", "stove" -> {
                b(30.0, w - 30, 50.0, d, 0.0, 100.0, BLACK)                    // plinth
                b(0.0, w, 0.0, d, 100.0, 860.0, if (i.kind == "stove") METAL else base)
                b(0.0, w, -20.0, d, 860.0, 900.0, STONE)                       // worktop
                when (i.kind) {
                    "sink" -> { b(w * 0.18, w * 0.82, d * 0.2, d * 0.75, 900.0, 905.0, METAL); b(w / 2 - 15, w / 2 + 15, d * 0.8, d * 0.85, 900.0, 1150.0, METAL) }
                    "stove" -> {
                        b(30.0, w - 30, 30.0, d - 30, 900.0, 908.0, BLACK)
                        for ((x, y) in listOf(0.3 to 0.3, 0.7 to 0.3, 0.3 to 0.7, 0.7 to 0.7)) b(w * x - 70, w * x + 70, d * y - 70, d * y + 70, 908.0, 914.0, 0x55585E)
                        b(60.0, w - 60, -8.0, 0.0, 200.0, 700.0, BLACK)        // oven door
                    }
                    else -> b(w / 2 - 60, w / 2 + 60, -20.0, -4.0, 780.0, 800.0, METAL)
                }
            }
            "fridge" -> {
                b(0.0, w, 0.0, d, 0.0, h * 0.35 - 5, base); b(0.0, w, 0.0, d, h * 0.35 + 5, h, base)
                b(0.0, w, 10.0, d, h * 0.35 - 5, h * 0.35 + 5, METAL)
                b(w - 80, w - 60, -25.0, 0.0, h * 0.4, h * 0.7, METAL); b(w - 80, w - 60, -25.0, 0.0, h * 0.1, h * 0.3, METAL)
            }
            "washer" -> {
                b(0.0, w, 0.0, d, 0.0, h, base)
                b(w * 0.2, w * 0.8, -12.0, 0.0, h * 0.2, h * 0.2 + w * 0.6, 0x5A6470) // door
                b(30.0, w - 30, -6.0, 0.0, h - 120, h - 30, 0xD5D8DC)          // panel
            }
            "bath" -> {
                b(0.0, w, 0.0, d, 0.0, h, WHITE)
                b(70.0, w - 70, 70.0, d - 70, h, h + 3, 0x9FCDE6)              // water
                b(w - 160, w - 140, d / 2 - 15, d / 2 + 15, h, h + 180, METAL) // tap
            }
            "toilet" -> {
                b(w * 0.25, w * 0.75, d * 0.25, d * 0.85, 0.0, 380.0, WHITE)   // foot
                b(0.0, w, 0.0, d * 0.8, 300.0, 400.0, WHITE)                   // bowl and seat
                b(w * 0.1, w * 0.9, d * 0.8, d, 0.0, 800.0, WHITE)             // tank at the back
            }
            "radiator" -> {
                // sections standing on two brackets, 15 cm off the floor
                val n = maxOf(3, (w / 80).toInt())
                for (k in 0 until n) { val x0 = k * w / n; b(x0 + 5, x0 + w / n - 5, 0.0, d, 150.0, 150.0 + h, base) }
                b(0.0, w, d * 0.3, d * 0.7, 150.0 + h * 0.1, 150.0 + h * 0.18, base); b(0.0, w, d * 0.3, d * 0.7, 150.0 + h * 0.82, 150.0 + h * 0.9, base)
            }
            "fireplace" -> {
                b(-60.0, w + 60, -250.0, d, 0.0, 80.0, 0x8C8C88)                // hearth stone in front
                b(0.0, w, 0.0, d, 80.0, h - 80, base)                          // brick body
                b(w * 0.2, w * 0.8, -5.0, d * 0.6, 160.0, h * 0.62, BLACK)     // firebox
                b(w * 0.3, w * 0.7, -8.0, d * 0.3, 160.0, 220.0, 0xE8692A)     // glow of the fire
                b(-50.0, w + 50, -50.0, d, h - 80, h, DARK)                     // mantel shelf
                b(w * 0.25, w * 0.75, d * 0.2, d, h, h + 1500, base)            // chimney breast up the wall
            }
            "car" -> {
                val wh = 330.0; val wt = 230.0
                for (yy in listOf(d * 0.17, d * 0.8)) {                        // wheels
                    b(-15.0, wt, yy - wh, yy + wh, 0.0, 2 * wh, BLACK); b(w - wt, w + 15, yy - wh, yy + wh, 0.0, 2 * wh, BLACK)
                }
                b(0.0, w, 0.0, d, 250.0, 820.0, base)                          // body
                b(90.0, w - 90, d * 0.3, d * 0.75, 820.0, h - 60, GLASS)       // cabin glass
                b(110.0, w - 110, d * 0.33, d * 0.72, h - 60, h, base)         // roof
                b(150.0, 450.0, -10.0, 0.0, 600.0, 720.0, 0xF4F1D0); b(w - 450, w - 150, -10.0, 0.0, 600.0, 720.0, 0xF4F1D0) // headlights
                b(150.0, 400.0, d, d + 10, 620.0, 720.0, 0xC0392B); b(w - 400, w - 150, d, d + 10, 620.0, 720.0, 0xC0392B)  // rear lights
            }
            "ac_out" -> {
                // the outdoor unit: a white case, the fan's grille in front, feet on a bracket
                b(0.0, w, 0.0, d, 0.0, h, base)
                b(w * 0.08, w * 0.62, -10.0, 0.0, h * 0.1, h * 0.9, 0x3C3F44)
                var zz = h * 0.14
                while (zz < h * 0.88) { b(w * 0.08, w * 0.62, -16.0, -10.0, zz, zz + 12, 0xCFD2D4); zz += h * 0.09 }
                b(w * 0.7, w * 0.95, -6.0, 0.0, h * 0.1, h * 0.35, 0xBFC3C6)        // the pipes' cover
                b(40.0, 80.0, 0.0, d + 120, -60.0, 0.0, METAL); b(w - 80, w - 40, 0.0, d + 120, -60.0, 0.0, METAL) // bracket to the wall
            }
            "ac_in" -> {
                b(0.0, w, 0.0, d, 0.0, h, base)
                b(30.0, w - 30, -6.0, 0.0, 30.0, 70.0, 0x9AA0A6)                  // the louvre
            }
            "chimney" -> {
                // a stone stack with a wider cap slab and a small roof over the flue
                val cap = 70.0
                b(0.0, w, 0.0, d, 0.0, h - cap, base)
                b(-50.0, w + 50, -50.0, d + 50, h - cap, h, 0x8C8C88)
                b(w * 0.3, w * 0.7, d * 0.3, d * 0.7, h, h + 220, BLACK)
                b(w * 0.15, w * 0.85, d * 0.15, d * 0.85, h + 220, h + 260, 0x8C8C88)
            }
            else -> b(0.0, w, 0.0, d, 0.0, h, base)
        }
        if (i.z != 0) return out.map { FPart(it.corners, it.z0 + i.z, it.z1 + i.z, it.color, it.clad) }
        return out
    }
}
