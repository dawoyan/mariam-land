package com.dawoyan.roomik.core

import java.util.Locale

/**
 * The house as a Wavefront OBJ file in millimetres, Y up (x right, z towards the viewer, as idea-3d uses): every floor,
 * walls with their doors and windows, furniture, stairs, electrics and ceilings; no ground. Each vertex also carries its
 * colour (r g b after x y z), which most programs read and the others ignore.
 */
object ObjExport {
    fun write(root: Plan): String {
        val mesh = Mesh.build(root, root.floors.size)
        val d = mesh.data
        val sb = StringBuilder(d.size * 8)
        sb.append("# Roomik: ").append(root.name.replace('\n', ' ')).append("\n# units: millimetres, Y up\no ")
            .append(root.name.replace(Regex("\\s+"), "_").ifEmpty { "plan" }).append('\n')
        val first = mesh.groundCount // the ground is left out
        val n = mesh.vertexCount
        for (i in first until n) {
            val o = i * Mesh.STRIDE
            sb.append(String.format(Locale.US, "v %.1f %.1f %.1f %.3f %.3f %.3f\n",
                d[o] * 1000, d[o + 1] * 1000, d[o + 2] * 1000, d[o + 6], d[o + 7], d[o + 8]))
        }
        var k = 1
        for (t in 0 until (n - first) / 3) { sb.append("f ").append(k).append(' ').append(k + 1).append(' ').append(k + 2).append('\n'); k += 3 }
        return sb.toString()
    }
}
