package com.dawoyan.roomik.core

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D models on the plan: read from 3MF (with each object's colour, as Idea 3D writes it), STL or OBJ; drawn on the
 * plan seen from above and in three views (top, front, side); measured for materials.
 *
 * Idea 3D's axes are X right, Y up, Z toward the viewer in its front view. On the plan that is x = X, y = Z (toward the
 * front, the bottom of the plan) and up = Y.
 */
object Models {
    const val MAX_TRIANGLES = 200_000

    /** Parts of the model in [bytes], in the plan's axes, centred on their middle and standing on z = 0. */
    fun read(fileName: String, bytes: ByteArray): List<ModelPart> {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val raw = when {
            ext == "3mf" || bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() -> threeMf(bytes)
            ext == "obj" -> obj(String(bytes, Charsets.UTF_8))
            binaryStl(bytes) -> listOf(stlBinary(bytes))
            else -> String(bytes, Charsets.UTF_8).let { if (it.trimStart().startsWith("solid")) listOf(stlText(it)) else obj(it) }
        }.filter { it.tri.isNotEmpty() }
        require(raw.isNotEmpty()) { "No triangles in $fileName" }
        require(raw.sumOf { it.tri.size / 3 } <= MAX_TRIANGLES) { "Too many triangles" }
        // their Y up → our z; their Z (front) → our y
        val turned = raw.map { p ->
            val v = p.xyz; val out = ArrayList<Float>(v.size)
            for (i in v.indices step 3) { out += v[i]; out += v[i + 2]; out += v[i + 1] }
            // swapping two axes mirrors the model: each triangle's corners go the other way round so it still faces out
            val t = p.tri.toMutableList()
            for (k in t.indices step 3) { val b = t[k + 1]; t[k + 1] = t[k + 2]; t[k + 2] = b }
            p.copy(xyz = out, tri = t)
        }
        return settle(turned)
    }

    /** [parts] moved so their middle (x, y) is at 0 and their lowest point at z = 0. */
    fun settle(parts: List<ModelPart>): List<ModelPart> {
        val all = parts.flatMap { it.xyz }
        if (all.isEmpty()) return parts
        var x0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var y1 = -Float.MAX_VALUE; var z0 = Float.MAX_VALUE
        for (i in all.indices step 3) { x0 = minOf(x0, all[i]); x1 = maxOf(x1, all[i]); y0 = minOf(y0, all[i + 1]); y1 = maxOf(y1, all[i + 1]); z0 = minOf(z0, all[i + 2]) }
        val cx = (x0 + x1) / 2; val cy = (y0 + y1) / 2
        return parts.map { p -> p.copy(xyz = p.xyz.mapIndexed { i, v -> when (i % 3) { 0 -> v - cx; 1 -> v - cy; else -> v - z0 } }) }
    }

    /** Width (x), depth (y) and height (z) of the model, mm. */
    fun size(m: Model3D): Triple<Double, Double, Double> {
        var x0 = Double.MAX_VALUE; var x1 = -Double.MAX_VALUE; var y0 = Double.MAX_VALUE; var y1 = -Double.MAX_VALUE; var z1 = 0.0
        for (p in m.parts) for (i in p.xyz.indices step 3) {
            x0 = minOf(x0, p.xyz[i].toDouble()); x1 = maxOf(x1, p.xyz[i].toDouble())
            y0 = minOf(y0, p.xyz[i + 1].toDouble()); y1 = maxOf(y1, p.xyz[i + 1].toDouble()); z1 = maxOf(z1, p.xyz[i + 2].toDouble())
        }
        if (x0 > x1) return Triple(0.0, 0.0, 0.0)
        return Triple(x1 - x0, y1 - y0, z1)
    }

    /** A point of the model's own (x, y) on the plan. */
    fun toPlan(m: Model3D, x: Double, y: Double): P {
        val a = Math.toRadians(m.rot.toDouble()); val c = cos(a); val s = sin(a)
        return P(m.x + x * c - y * s, m.y + x * s + y * c)
    }

    /** The model's outline on the plan: its turned bounding box. */
    fun corners(m: Model3D): List<P> {
        val (w, d, _) = size(m)
        return listOf(P(-w / 2, -d / 2), P(w / 2, -d / 2), P(w / 2, d / 2), P(-w / 2, d / 2)).map { toPlan(m, it.x, it.y) }
    }

    fun covers(m: Model3D, w: P): Boolean = Geo.inside(w, corners(m))

    /** What a part takes: its whole surface, the top seen from above (a roof sheet), and its size, all mm / mm². */
    class PartSize(val part: ModelPart, val surface: Double, val top: Double, val w: Double, val d: Double, val h: Double)

    fun measure(part: ModelPart): PartSize {
        val v = part.xyz; var surface = 0.0; var top = 0.0
        for (k in part.tri.indices step 3) {
            val a = part.tri[k] * 3; val b = part.tri[k + 1] * 3; val c = part.tri[k + 2] * 3
            val ux = (v[b] - v[a]).toDouble(); val uy = (v[b + 1] - v[a + 1]).toDouble(); val uz = (v[b + 2] - v[a + 2]).toDouble()
            val wx = (v[c] - v[a]).toDouble(); val wy = (v[c + 1] - v[a + 1]).toDouble(); val wz = (v[c + 2] - v[a + 2]).toDouble()
            val nx = uy * wz - uz * wy; val ny = uz * wx - ux * wz; val nz = ux * wy - uy * wx
            surface += sqrt(nx * nx + ny * ny + nz * nz) / 2
            if (nz > 0) top += nz / 2
        }
        var x0 = Double.MAX_VALUE; var x1 = -Double.MAX_VALUE; var y0 = Double.MAX_VALUE; var y1 = -Double.MAX_VALUE; var z0 = Double.MAX_VALUE; var z1 = -Double.MAX_VALUE
        for (i in v.indices step 3) {
            x0 = minOf(x0, v[i].toDouble()); x1 = maxOf(x1, v[i].toDouble()); y0 = minOf(y0, v[i + 1].toDouble()); y1 = maxOf(y1, v[i + 1].toDouble())
            z0 = minOf(z0, v[i + 2].toDouble()); z1 = maxOf(z1, v[i + 2].toDouble())
        }
        return PartSize(part, surface, top, x1 - x0, y1 - y0, z1 - z0)
    }

    /** Views of a model drawn flat. */
    enum class View { TOP, FRONT, SIDE }

    /**
     * Triangle [k] of [part] seen in [view]: its three corners (horizontal, vertical screen axes in mm, vertical up),
     * how deep it is (bigger is nearer, for drawing far ones first) and how much it faces the viewer (0..1, for shading).
     */
    class Flat(val pts: FloatArray, val depth: Float, val facing: Float, val color: Int)

    fun flat(m: Model3D, view: View): List<Flat> {
        val out = ArrayList<Flat>()
        for (p in m.parts) {
            val v = p.xyz
            for (k in p.tri.indices step 3) {
                val ia = p.tri[k] * 3; val ib = p.tri[k + 1] * 3; val ic = p.tri[k + 2] * 3
                if (ia + 2 >= v.size || ib + 2 >= v.size || ic + 2 >= v.size) continue
                val ux = v[ib] - v[ia]; val uy = v[ib + 1] - v[ia + 1]; val uz = v[ib + 2] - v[ia + 2]
                val wx = v[ic] - v[ia]; val wy = v[ic + 1] - v[ia + 1]; val wz = v[ic + 2] - v[ia + 2]
                var nx = uy * wz - uz * wy; var ny = uz * wx - ux * wz; var nz = ux * wy - uy * wx
                val l = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-9f); nx /= l; ny /= l; nz /= l
                // horizontal, vertical (up) and toward the viewer, per view
                fun h(i: Int) = when (view) { View.TOP -> v[i]; View.FRONT -> v[i]; View.SIDE -> -v[i + 1] }
                fun vv(i: Int) = when (view) { View.TOP -> -v[i + 1]; View.FRONT -> v[i + 2]; View.SIDE -> v[i + 2] }
                fun d(i: Int) = when (view) { View.TOP -> v[i + 2]; View.FRONT -> v[i + 1]; View.SIDE -> v[i] }
                val facing = abs(when (view) { View.TOP -> nz; View.FRONT -> ny; View.SIDE -> nx })
                out += Flat(floatArrayOf(h(ia), vv(ia), h(ib), vv(ib), h(ic), vv(ic)), maxOf(d(ia), d(ib), d(ic)), facing, p.color)
            }
        }
        out.sortBy { it.depth }
        return out
    }

    // ---- writing, for Idea 3D ----

    /**
     * [parts] as a 3MF file with each part's colour, the way 3D printing files stand: X right, Y back, Z up (Idea 3D
     * stands such a file upright as it imports it). A plan's y runs to the front, so it is turned round.
     */
    fun toThreeMf(parts: List<ModelPart>): ByteArray {
        fun n(v: Float) = String.format(java.util.Locale.US, "%.2f", v).trimEnd('0').trimEnd('.').let { if (it == "-0") "0" else it }
        fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<model unit=\"millimeter\" xml:lang=\"en-US\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02\">\n")
        sb.append("  <metadata name=\"Application\">Roomik</metadata>\n  <resources>\n    <basematerials id=\"1\">\n")
        for (p in parts) sb.append("      <base name=\"${esc(p.name)}\" displaycolor=\"#${String.format("%06X", p.color and 0xFFFFFF)}FF\"/>\n")
        sb.append("    </basematerials>\n")
        parts.forEachIndexed { i, p ->
            sb.append("    <object id=\"${i + 2}\" type=\"model\" name=\"${esc(p.name)}\" pid=\"1\" pindex=\"$i\">\n      <mesh>\n        <vertices>\n")
            for (k in p.xyz.indices step 3) sb.append("          <vertex x=\"${n(p.xyz[k])}\" y=\"${n(-p.xyz[k + 1])}\" z=\"${n(p.xyz[k + 2])}\"/>\n")
            sb.append("        </vertices>\n        <triangles>\n")
            // y turned round mirrors the model: corners go the other way round so faces still look out
            for (k in p.tri.indices step 3) sb.append("          <triangle v1=\"${p.tri[k]}\" v2=\"${p.tri[k + 2]}\" v3=\"${p.tri[k + 1]}\"/>\n")
            sb.append("        </triangles>\n      </mesh>\n    </object>\n")
        }
        sb.append("  </resources>\n  <build>\n")
        parts.indices.forEach { sb.append("    <item objectid=\"${it + 2}\"/>\n") }
        sb.append("  </build>\n</model>\n")
        val out = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { z ->
            fun entry(name: String, text: String) { z.putNextEntry(java.util.zip.ZipEntry(name)); z.write(text.toByteArray()); z.closeEntry() }
            entry("[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"model\" ContentType=\"application/vnd.ms-package.3dmanufacturing-3dmodel+xml\"/></Types>")
            entry("_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Target=\"/3D/3dmodel.model\" Id=\"rel0\" Type=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\"/></Relationships>")
            entry("3D/3dmodel.model", sb.toString())
        }
        return out.toByteArray()
    }

    /** A piece of furniture as model parts: its blocks as boxes, in its own mm (middle at 0, standing on 0, not turned). */
    fun ofItem(item: Item): List<ModelPart> {
        val at0 = item.copy(x = 0, y = 0, rot = 0)
        return Furniture.parts(at0).groupBy { it.color }.map { (color, blocks) ->
            val xyz = ArrayList<Float>(); val tri = ArrayList<Int>()
            for (b in blocks) {
                val base = xyz.size / 3
                for (z in listOf(b.z0, b.z1)) for (c in b.corners) { xyz += c.x.toFloat(); xyz += c.y.toFloat(); xyz += z.toFloat() }
                val n = b.corners.size
                val mid = b.corners.fold(P(0.0, 0.0)) { s, c -> s + c } * (1.0 / n)
                val midZ = (b.z0 + b.z1) / 2
                fun face(i0: Int, i1: Int, i2: Int) {
                    // faced away from the block's middle
                    val a = i0; val bb = i1; val c = i2
                    val ax = xyz[(base + a) * 3]; val ay = xyz[(base + a) * 3 + 1]; val az = xyz[(base + a) * 3 + 2]
                    val ux = xyz[(base + bb) * 3] - ax; val uy = xyz[(base + bb) * 3 + 1] - ay; val uz = xyz[(base + bb) * 3 + 2] - az
                    val wx = xyz[(base + c) * 3] - ax; val wy = xyz[(base + c) * 3 + 1] - ay; val wz = xyz[(base + c) * 3 + 2] - az
                    val nx = uy * wz - uz * wy; val ny = uz * wx - ux * wz; val nz = ux * wy - uy * wx
                    val out = nx * (ax - mid.x) + ny * (ay - mid.y) + nz * (az - midZ) >= 0
                    tri += base + a; tri += base + (if (out) bb else c); tri += base + (if (out) c else bb)
                }
                for (k in 1 until n - 1) { face(0, k, k + 1); face(n, n + k, n + k + 1) }
                for (k in 0 until n) { val j = (k + 1) % n; face(k, j, n + j); face(k, n + j, n + k) }
            }
            ModelPart(Catalog.of(item.kind).id, color, xyz, tri)
        }
    }

    // ---- readers ----

    private class Raw(val name: String, val color: Int, val xyz: MutableList<Float> = ArrayList(), val tri: MutableList<Int> = ArrayList()) {
        fun part() = ModelPart(name, color, xyz, tri)
    }

    private fun threeMf(bytes: ByteArray): List<ModelPart> {
        var xml = ""
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (e.name.endsWith(".model")) { xml = String(zip.readBytes(), Charsets.UTF_8); break }
            }
        }
        require(xml.isNotEmpty()) { "No model in the 3MF file" }
        fun attr(tag: String, name: String) = Regex("""\b$name="([^"]*)"""").find(tag)?.groupValues?.get(1)
        // colours of base materials: id → list of #RRGGBB(AA)
        val materials = HashMap<String, List<Int>>()
        for (m in Regex("""<basematerials\b([^>]*)>(.*?)</basematerials>""", RegexOption.DOT_MATCHES_ALL).findAll(xml)) {
            val id = attr(m.groupValues[1], "id") ?: continue
            materials[id] = Regex("""<base\b[^>]*>""").findAll(m.groupValues[2]).map { b -> hex(attr(b.value, "displaycolor")) }.toList()
        }
        val out = ArrayList<ModelPart>()
        for (o in Regex("""<object\b([^>]*)>(.*?)</object>""", RegexOption.DOT_MATCHES_ALL).findAll(xml)) {
            val head = o.groupValues[1]; val body = o.groupValues[2]
            if (!body.contains("<mesh")) continue
            val color = materials[attr(head, "pid")]?.getOrNull(attr(head, "pindex")?.toIntOrNull() ?: 0) ?: 0x9E9E9E
            val r = Raw(attr(head, "name") ?: "Part ${out.size + 1}", color)
            for (vtx in Regex("""<vertex\b[^>]*>""").findAll(body)) {
                r.xyz += (attr(vtx.value, "x")?.toFloatOrNull() ?: 0f); r.xyz += (attr(vtx.value, "y")?.toFloatOrNull() ?: 0f); r.xyz += (attr(vtx.value, "z")?.toFloatOrNull() ?: 0f)
            }
            for (t in Regex("""<triangle\b[^>]*>""").findAll(body)) {
                r.tri += attr(t.value, "v1")!!.toInt(); r.tri += attr(t.value, "v2")!!.toInt(); r.tri += attr(t.value, "v3")!!.toInt()
            }
            out += r.part()
        }
        return out
    }

    private fun hex(s: String?): Int {
        val h = s?.removePrefix("#") ?: return 0x9E9E9E
        return h.take(6).toIntOrNull(16) ?: 0x9E9E9E
    }

    private fun binaryStl(b: ByteArray): Boolean {
        if (b.size < 84) return false
        val n = ByteBuffer.wrap(b, 80, 4).order(ByteOrder.LITTLE_ENDIAN).int
        return n >= 0 && 84L + 50L * n == b.size.toLong()
    }

    private fun stlBinary(b: ByteArray): ModelPart {
        val buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        val n = buf.getInt(80)
        val r = Raw("Model", 0x9E9E9E)
        var at = 84
        repeat(n) {
            at += 12
            repeat(3) { r.tri += r.xyz.size / 3; repeat(3) { r.xyz += buf.getFloat(at); at += 4 } }
            at += 2
        }
        return r.part()
    }

    private fun stlText(t: String): ModelPart {
        val r = Raw("Model", 0x9E9E9E)
        for (m in Regex("""vertex\s+(\S+)\s+(\S+)\s+(\S+)""").findAll(t)) {
            r.tri += r.xyz.size / 3
            r.xyz += m.groupValues[1].toFloat(); r.xyz += m.groupValues[2].toFloat(); r.xyz += m.groupValues[3].toFloat()
        }
        return r.part()
    }

    private fun obj(t: String): List<ModelPart> {
        val verts = ArrayList<Float>()
        val parts = ArrayList<Raw>()
        var cur: Raw? = null
        val map = HashMap<Int, Int>()
        for (line in t.lineSequence()) {
            val s = line.trim()
            when {
                s.startsWith("v ") -> s.substring(2).trim().split(Regex("\\s+")).take(3).forEach { verts += it.toFloat() }
                s.startsWith("o ") || s.startsWith("g ") -> { cur = Raw(s.substring(2).trim().ifBlank { "Part" }, 0x9E9E9E).also { parts += it }; map.clear() }
                s.startsWith("f ") -> {
                    val c = cur ?: Raw("Model", 0x9E9E9E).also { cur = it; parts += it }
                    val idx = s.substring(2).trim().split(Regex("\\s+")).map { it.substringBefore('/').toInt().let { i -> if (i < 0) verts.size / 3 + i else i - 1 } }
                    val local = idx.map { g -> map.getOrPut(g) { c.xyz.add(verts[g * 3]); c.xyz.add(verts[g * 3 + 1]); c.xyz.add(verts[g * 3 + 2]); c.xyz.size / 3 - 1 } }
                    for (k in 1 until local.size - 1) { c.tri += local[0]; c.tri += local[k]; c.tri += local[k + 1] }
                }
            }
        }
        return parts.map { it.part() }
    }
}
