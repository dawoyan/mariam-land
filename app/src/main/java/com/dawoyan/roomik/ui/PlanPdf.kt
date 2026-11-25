package com.dawoyan.roomik.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.dawoyan.roomik.R
import com.dawoyan.roomik.core.BomKey
import com.dawoyan.roomik.core.Catalog
import com.dawoyan.roomik.core.Interior
import com.dawoyan.roomik.core.Materials
import com.dawoyan.roomik.core.OpeningKind
import com.dawoyan.roomik.core.P
import com.dawoyan.roomik.core.Palette
import com.dawoyan.roomik.core.Plan
import com.dawoyan.roomik.core.PointKind
import com.dawoyan.roomik.core.RouteLevel
import com.dawoyan.roomik.core.Rooms
import com.dawoyan.roomik.core.WallShapes
import com.dawoyan.roomik.core.allFloors
import com.dawoyan.roomik.core.floorRange
import com.dawoyan.roomik.core.floor
import com.dawoyan.roomik.core.STAIRS
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date
import kotlin.math.cos
import kotlin.math.sin

/**
 * The plan to scale on A4 (1:50, 1:100… whichever fits), with dimensions, rooms, doors and windows, furniture, the
 * electrics and drawn cables; then the materials list. Units in the PDF are points (1/72 inch).
 */
object PlanPdf {
    private const val PT_PER_MM = 72f / 25.4f
    private const val W = 595; private const val H = 842 // A4 portrait
    private const val M = 36f

    fun write(context: Context, plan: Plan, out: OutputStream) {
        val ru = context.resources.configuration.locales[0].language == "ru"
        val doc = PdfDocument()
        // One page per floor, then the materials list for the whole house.
        for ((page, i) in plan.floorRange.reversed().withIndex()) {
            val f = plan.floor(i)
            val landscape = f.nodes.isNotEmpty() &&
                (f.nodes.maxOf { it.x } - f.nodes.minOf { it.x }) > (f.nodes.maxOf { it.y } - f.nodes.minOf { it.y })
            val pw = if (landscape) H else W; val ph = if (landscape) W else H
            val pg = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, page + 1).create())
            val title = plan.name + if (plan.floors.isNotEmpty() || plan.basements.isNotEmpty()) " — " + context.getString(when { i == 0 -> R.string.floor_ground; i > 0 -> R.string.floor_plus; i == -1 -> R.string.floor_basement; else -> R.string.floor_basement_n }, kotlin.math.abs(i)) else ""
            // stairs from the floor below that come up through this one: drawn here too, as on the editor
            val lower = (i - 1).takeIf { it in plan.floorRange }?.let { plan.floor(it) }
            val arriving = lower?.stairs?.filter { !it.fromGround && com.dawoyan.roomik.core.Stairs.rise(lower, it) > lower.wallHeight }?.map { lower to it }.orEmpty()
            drawPlan(context, f.copy(name = title), pg.canvas, pw.toFloat(), ph.toFloat(), arriving)
            doc.finishPage(pg)
        }
        listPages(context, plan, ru, doc)
        doc.writeTo(out)
        doc.close()
    }

    private fun paint(color: Int, size: Float = 8f, bold: Boolean = false, stroke: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        if (stroke > 0) { style = Paint.Style.STROKE; strokeWidth = stroke }
    }

    private fun drawPlan(context: Context, plan: Plan, c: Canvas, pw: Float, ph: Float, arriving: List<Pair<Plan, com.dawoyan.roomik.core.Stair>> = emptyList()) {
        val s = context.resources
        val title = paint(Color.BLACK, 14f, bold = true)
        c.drawText(plan.name, M, M, title)
        if (plan.nodes.isEmpty()) return
        val minX = plan.nodes.minOf { it.x } - 400f; val maxX = plan.nodes.maxOf { it.x } + 400f
        val minY = plan.nodes.minOf { it.y } - 400f; val maxY = plan.nodes.maxOf { it.y } + 400f
        val areaW = pw - 2 * M; val areaH = ph - 2 * M - 70f
        // Largest standard scale at which the plan fits.
        val ratio = listOf(20, 25, 50, 75, 100, 150, 200, 250, 500, 1000).firstOrNull { r ->
            (maxX - minX) / r * PT_PER_MM <= areaW && (maxY - minY) / r * PT_PER_MM <= areaH
        } ?: 1000
        val k = PT_PER_MM / ratio // points per plan mm
        val ox = M + (areaW - (maxX - minX) * k) / 2 - minX * k
        val oy = M + 24f + (areaH - (maxY - minY) * k) / 2 - minY * k
        fun x(v: Double) = (v * k + ox).toFloat()
        fun y(v: Double) = (v * k + oy).toFloat()
        fun path(pts: List<P>) = Path().apply { pts.forEachIndexed { i, q -> if (i == 0) moveTo(x(q.x), y(q.y)) else lineTo(x(q.x), y(q.y)) }; close() }

        c.drawText(s.getString(R.string.scale) + " 1:$ratio · " + DateFormat.getDateInstance().format(Date()), M, M + 14f, paint(Color.DKGRAY, 9f))

        val rooms = Rooms.detect(plan)
        rooms.forEachIndexed { i, r -> c.drawPath(path(r.polygon), paint(0xFF000000.toInt() or Palette.floor(r, i))) }
        for (i in plan.items) {
            val cs = Interior.corners(i)
            for (part in com.dawoyan.roomik.core.Furniture.parts(i).sortedBy { it.z1 }) {
                c.drawPath(path(part.corners), paint(0xFF000000.toInt() or part.color))
                c.drawPath(path(part.corners), paint(0x55000000, stroke = 0.3f))
            }
            if (i.kind == STAIRS) {
                val n = maxOf(2, Math.round(i.h / 175f))
                for (k in 1 until n) { val f = k.toDouble() / n; val a = cs[0] + (cs[3] - cs[0]) * f; val b = cs[1] + (cs[2] - cs[1]) * f
                    c.drawLine(x(a.x), y(a.y), x(b.x), y(b.y), paint(0xFF705030.toInt(), stroke = 0.4f)) }
            }
        }
        val wallInk = paint(0xFF3A3F4B.toInt())
        val lowInk = paint(0xFF8A90A0.toInt())
        for (w in WallShapes.build(plan)) c.drawPath(path(w.outline), if (plan.heightOf(w.wall) < plan.wallHeight) lowInk else wallInk)
        // Openings: gap, then the window's double line or the door's leaf and swing.
        val thin = paint(0xFF3A3F4B.toInt(), stroke = 0.7f)
        for ((wid, list) in Interior.openings(plan)) {
            val wall = plan.wall(wid) ?: continue
            val a = plan.pos(wall.a) ?: continue
            val d = ((plan.pos(wall.b) ?: continue) - a).norm(); val n = d.left(); val ht = wall.thickness / 2.0
            for (cut in list) {
                val p0 = a + d * cut.t0; val p1 = a + d * cut.t1
                val gap = path(listOf(p0 + n * ht, p1 + n * ht, p1 - n * ht, p0 - n * ht))
                c.drawPath(gap, paint(Color.WHITE))
                if (cut.opening.kind == OpeningKind.WINDOW) {
                    c.drawPath(gap, thin)
                    c.drawLine(x(p0.x), y(p0.y), x(p1.x), y(p1.y), thin)
                } else if (cut.opening.kind == OpeningKind.PASSAGE) {
                    c.drawLine(x((p0 + n * ht).x), y((p0 + n * ht).y), x((p0 - n * ht).x), y((p0 - n * ht).y), thin)
                    c.drawLine(x((p1 + n * ht).x), y((p1 + n * ht).y), x((p1 - n * ht).x), y((p1 - n * ht).y), thin)
                } else {
                    val no = P(cut.opening.nx, cut.opening.ny); val len = cut.t1 - cut.t0
                    val hinge = (if (cut.opening.hingeEnd) p1 else p0) + no * ht
                    val dd = if (cut.opening.hingeEnd) d * -1.0 else d
                    val leaf = hinge + no * len
                    c.drawLine(x(hinge.x), y(hinge.y), x(leaf.x), y(leaf.y), paint(0xFF3A3F4B.toInt(), stroke = 1.2f))
                    val arc = Path()
                    for (j in 0..12) {
                        val ang = Math.PI / 2 * j / 12
                        val q = hinge + no * (len * cos(ang)) + dd * (len * sin(ang))
                        if (j == 0) arc.moveTo(x(q.x), y(q.y)) else arc.lineTo(x(q.x), y(q.y))
                    }
                    c.drawPath(arc, paint(0xFF3A3F4B.toInt(), stroke = 0.5f).apply { pathEffect = DashPathEffect(floatArrayOf(2f, 2f), 0f) })
                }
            }
        }
        // Stairs over everything below them.
        val stepInk = paint(0xFF705030.toInt(), stroke = 0.5f)
        for (st in plan.stairs) {
            val shp = com.dawoyan.roomik.core.Stairs.shape(plan, st)
            for (pc in shp.pieces) { c.drawPath(path(pc), paint(0xFFE6D6BE.toInt())); c.drawPath(path(pc), stepInk) }
            for (f in shp.flights) for (k in 1 until f.steps) {
                val t = k.toDouble() / f.steps; val q = f.corners
                val a = q[0] + (q[3] - q[0]) * t; val b = q[1] + (q[2] - q[1]) * t
                c.drawLine(x(a.x), y(a.y), x(b.x), y(b.y), stepInk)
            }
        }
        // Stairs coming up from the floor below: dashed, with where they come from.
        val upInk = paint(0xFF705030.toInt(), stroke = 0.5f).apply { pathEffect = DashPathEffect(floatArrayOf(2f, 1.5f), 0f) }
        for ((lp, st) in arriving) {
            val shp = com.dawoyan.roomik.core.Stairs.shape(lp, st)
            for (pc in shp.pieces) { c.drawPath(path(pc), paint(0x55E6D6BE)); c.drawPath(path(pc), upInk) }
            for (f in shp.flights) for (k in 1 until f.steps) {
                val t = k.toDouble() / f.steps; val q = f.corners
                val a = q[0] + (q[3] - q[0]) * t; val b = q[1] + (q[2] - q[1]) * t
                c.drawLine(x(a.x), y(a.y), x(b.x), y(b.y), upInk)
            }
            shp.pieces.flatten().takeIf { it.isNotEmpty() }?.let { pts ->
                val cx = pts.sumOf { it.x } / pts.size; val cy = pts.sumOf { it.y } / pts.size
                c.drawText("↑ " + s.getString(R.string.stairs), x(cx), y(cy), paint(0xFF5A3D20.toInt(), 6.5f, bold = true).apply { textAlign = Paint.Align.CENTER })
            }
        }
        // Wall lengths, cm.
        val dim = paint(0xFF20242C.toInt(), 6.5f).apply { textAlign = Paint.Align.CENTER }
        for (w in WallShapes.build(plan)) {
            val len = (w.b - w.a).len
            if (len * k < 28) continue
            val n = (w.b - w.a).norm().left(); val off = w.wall.thickness / 2.0 + 9 / k
            val m = w.mid + n * off
            c.save()
            var ang = Math.toDegrees(Math.atan2(w.b.y - w.a.y, w.b.x - w.a.x)).toFloat()
            if (ang > 90 || ang < -90) ang += 180f
            c.rotate(ang, x(m.x), y(m.y))
            c.drawText(cm(len), x(m.x), y(m.y) + 2.5f, dim)
            c.restore()
        }
        // Room names and areas.
        val lang = context.resources.configuration.locales[0].language
        val name = paint(Color.BLACK, 8f, bold = true).apply { textAlign = Paint.Align.CENTER }
        val small = paint(Color.DKGRAY, 7f).apply { textAlign = Paint.Align.CENTER }
        for (r in rooms) {
            val t = r.info?.name?.takeIf { it.isNotEmpty() } ?: r.info?.type?.label(lang)
            val ax = x(r.anchor.x); val ay = y(r.anchor.y)
            if (t != null) c.drawText(t, ax, ay - 2f, name)
            c.drawText(num(r.area) + " " + s.getString(R.string.m2), ax, ay + 8f, small)
        }
        // Drawn cables.
        for (rt in plan.routes) {
            val pts = routePlan(plan, rt) ?: continue
            val p = Path().apply { pts.forEachIndexed { i, q -> if (i == 0) moveTo(x(q.x), y(q.y)) else lineTo(x(q.x), y(q.y)) } }
            c.drawPath(p, paint(0xFFD0342C.toInt(), stroke = 1f).apply { if (rt.level == RouteLevel.FLOOR) pathEffect = DashPathEffect(floatArrayOf(4f, 2f), 0f) })
        }
        // Switch → lamp links.
        val placed = Interior.place(plan)
        fun at(pp: com.dawoyan.roomik.core.PlacedPoint) = if (pp.point.kind == PointKind.LAMP) pp.face else pp.face + pp.normal * 130.0
        val link = paint(0xFF2F7FD6.toInt(), stroke = 0.5f).apply { pathEffect = DashPathEffect(floatArrayOf(3f, 2f), 0f) }
        for (pp in placed) {
            val sw = pp.point.switchId?.let { id -> placed.firstOrNull { it.point.id == id } } ?: continue
            val a = at(sw); val b = pp.face
            c.drawLine(x(a.x), y(a.y), x(b.x), y(b.y), link)
        }
        // Points with their heights.
        val rad = 4.5f
        for (pp in placed) {
            val o = at(pp); val cx = x(o.x); val cy = y(o.y)
            symbol(c, pp.point.kind, cx, cy, rad)
            val note = when (pp.point.kind) {
                PointKind.LAMP -> "${pp.point.watts}W"
                PointKind.APPLIANCE -> num(pp.point.kw, 1) + "kW h" + cm(pp.point.height.toDouble())
                else -> (if (pp.point.count > 1) "×${pp.point.count} " else "") + "h" + cm(pp.point.height.toDouble())
            }
            c.drawText(note, cx + rad + 1.5f, cy - rad, paint(Color.DKGRAY, 5.5f))
        }
        // Legend.
        var lx = M; val ly = ph - M
        c.drawText(s.getString(R.string.legend) + ":", lx, ly, paint(Color.BLACK, 7f, bold = true)); lx += 52f
        for (kind in listOf(PointKind.SOCKET, PointKind.SWITCH, PointKind.LAMP, PointKind.APPLIANCE, PointKind.BOARD)) {
            symbol(c, kind, lx, ly - 2.5f, rad)
            val label = s.getString(when (kind) { PointKind.SOCKET -> R.string.socket; PointKind.SWITCH -> R.string.switch_; PointKind.LAMP -> R.string.lamp; PointKind.APPLIANCE -> R.string.appliance; else -> R.string.board })
            c.drawText(label, lx + 7f, ly, paint(Color.BLACK, 7f))
            lx += 14f + paint(Color.BLACK, 7f).measureText(label) + 8f
        }
        c.drawLine(lx, ly - 2.5f, lx + 14f, ly - 2.5f, paint(0xFFD0342C.toInt(), stroke = 1f))
        c.drawText(s.getString(R.string.cable) + " · h = cm", lx + 18f, ly, paint(Color.BLACK, 7f))
    }

    private fun symbol(c: Canvas, kind: PointKind, cx: Float, cy: Float, r: Float) {
        val fill = when (kind) {
            PointKind.SOCKET -> 0xFFF08A24; PointKind.SWITCH -> 0xFF2F7FD6; PointKind.LAMP -> 0xFFFFD23F
            PointKind.APPLIANCE -> 0xFFC0392B; PointKind.BOARD -> 0xFF8D97A5; PointKind.LOW_CURRENT_BOARD -> 0xFF325A7A; PointKind.LOW_CURRENT_BOX -> 0xFF2E9E5B
            PointKind.RISER -> 0xFF8E6CC9; PointKind.JUNCTION -> 0xFFD0342C
        }.toInt()
        c.drawCircle(cx, cy, r, paint(fill))
        val ink = paint(0xFF30343C.toInt(), stroke = 0.6f)
        c.drawCircle(cx, cy, r, ink)
        when (kind) {
            PointKind.SOCKET -> { c.drawCircle(cx - r * 0.38f, cy, r * 0.16f, paint(0xFF30343C.toInt())); c.drawCircle(cx + r * 0.38f, cy, r * 0.16f, paint(0xFF30343C.toInt())) }
            PointKind.SWITCH -> c.drawLine(cx - r * 0.35f, cy + r * 0.45f, cx + r * 0.35f, cy - r * 0.45f, ink)
            PointKind.LAMP -> { val q = r * 0.5f; c.drawLine(cx - q, cy - q, cx + q, cy + q, ink); c.drawLine(cx - q, cy + q, cx + q, cy - q, ink) }
            PointKind.BOARD, PointKind.LOW_CURRENT_BOARD -> c.drawRect(RectF(cx - r * 0.45f, cy - r * 0.5f, cx + r * 0.45f, cy + r * 0.5f), ink)
            PointKind.LOW_CURRENT_BOX -> c.drawCircle(cx, cy, r * 0.4f, paint(Color.WHITE))
            PointKind.APPLIANCE -> c.drawLine(cx, cy - r * 0.6f, cx, cy + r * 0.6f, paint(Color.WHITE, stroke = 1f))
            PointKind.RISER -> { c.drawLine(cx, cy - r * 0.6f, cx, cy + r * 0.6f, paint(Color.WHITE, stroke = 1f)); c.drawLine(cx - r * 0.4f, cy, cx + r * 0.4f, cy, paint(Color.WHITE, stroke = 1f)) }
            PointKind.JUNCTION -> c.drawCircle(cx, cy, r * 0.4f, paint(Color.WHITE))
        }
    }

    /** The materials list, on as many pages as it takes. */
    private fun listPages(context: Context, plan: Plan, ru: Boolean, doc: PdfDocument) {
        val s = context.resources
        val bom = Materials.build(plan, ru)
        val rows = ArrayList<Triple<String, String, Boolean>>() // text, quantity, heading
        fun head(t: String) { rows += Triple(t, "", true) }
        val m = s.getString(R.string.m); val pcs = s.getString(R.string.pcs)
        if (bom.cables.isNotEmpty()) {
            head(s.getString(R.string.sec_cable))
            for (cb in bom.cables) rows += Triple(s.getString(R.string.cable_name, cb.cores.toString(), size(cb.mm2)) + "  (" + s.getString(R.string.cable_note, num(cb.net, 1)) + ")", "${cb.meters} $m", false)
        }
        if (bom.items.isNotEmpty()) {
            head(s.getString(R.string.sec_parts))
            for (it in bom.items) rows += Triple(bomName(context, it.key, it.detail), "${it.qty} " + if (it.key == BomKey.CONDUIT) m else pcs, false)
        }
        if (bom.board.isNotEmpty()) {
            head(s.getString(R.string.sec_board))
            for (b in bom.board) rows += Triple((b.title + " " + b.rating).trim(), "${b.qty} $pcs", false)
        }
        if (bom.lines.isNotEmpty()) {
            head(s.getString(R.string.sec_lines))
            for (l in bom.lines) rows += Triple(l.name + "  —  " + l.rating + " · ${l.cores}×${size(l.mm2)}" + if (l.drawn) " · " + s.getString(R.string.drawn_note) else "",
                if (l.meters > 0) num(l.meters, 1) + " $m" else "", false)
        }
        if (rows.isEmpty()) return
        var i = 0
        var pageNo = 2
        while (i < rows.size) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo++).create())
            val c = page.canvas
            var yy = M + 4f
            c.drawText(plan.name + " — " + s.getString(R.string.materials), M, yy, paint(Color.BLACK, 14f, bold = true)); yy += 22f
            val qty = paint(Color.BLACK, 9.5f, bold = true).apply { textAlign = Paint.Align.RIGHT }
            while (i < rows.size && yy < H - M) {
                val (t, q, h) = rows[i]
                if (h) { yy += 6f; c.drawText(t, M, yy, paint(0xFF2F6B8A.toInt(), 11f, bold = true)); yy += 15f }
                else {
                    c.drawText(t, M, yy, paint(Color.BLACK, 9.5f))
                    c.drawText(q, W - M, yy, qty)
                    c.drawLine(M, yy + 4f, W - M, yy + 4f, paint(0xFFDDDDDD.toInt(), stroke = 0.5f))
                    yy += 15f
                }
                i++
            }
            if (i >= rows.size) {
                val note = paint(Color.DKGRAY, 7.5f)
                yy += 6f
                for (line in s.getString(R.string.estimate).chunked(110)) { c.drawText(line, M, yy, note); yy += 10f }
            }
            doc.finishPage(page)
        }
    }
}

fun size(mm2: Double) = if (mm2 % 1.0 == 0.0) mm2.toInt().toString() else mm2.toString()

fun bomName(context: Context, key: BomKey, detail: Int): String = context.getString(when (key) {
    BomKey.BACK_BOX -> R.string.bom_back_box
    BomKey.SOCKET -> R.string.bom_socket
    BomKey.SOCKET_IP44 -> R.string.bom_socket_ip44
    BomKey.POWER_OUTLET -> R.string.bom_power_outlet
    BomKey.SWITCH -> R.string.bom_switch
    BomKey.FRAME -> R.string.bom_frame
    BomKey.LAMP -> R.string.bom_lamp
    BomKey.CONNECTOR -> R.string.bom_connector
    BomKey.CONDUIT -> R.string.bom_conduit
    BomKey.CLIP -> R.string.bom_clip
    BomKey.JUNCTION_BOX -> R.string.junction
}, detail)
