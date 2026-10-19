package com.staytrack.core.export

import android.graphics.*
import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object MapImageGenerator {

    private const val DEFAULT_WIDTH = 1280
    private const val DEFAULT_HEIGHT = 960

    fun generateMapBitmap(
        lugares: List<Place>,
        visitas: List<Visit>,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fondo oscuro futurista con degradado
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#080B11"),
                Color.parseColor("#121826"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Bounding box
        val points = mutableListOf<Pair<Double, Double>>()
        lugares.forEach { points.add(Pair(it.latitud, it.longitud)) }
        visitas.forEach { points.add(Pair(it.latitudAncla, it.longitudAncla)) }

        val bbox = computeBoundingBox(points)

        // Rejilla cibernética
        val gridPaint = Paint().apply {
            color = Color.parseColor("#1A2336")
            strokeWidth = 1f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }
        val gridStep = 80f
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridStep
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gridStep
        }

        val margin = 100f
        fun projectX(lon: Double): Float {
            val normalized = (lon - bbox.minLon) / bbox.lonSpan
            return (margin + normalized * (width - 2 * margin)).toFloat()
        }

        fun projectY(lat: Double): Float {
            val normalized = (bbox.maxLat - lat) / bbox.latSpan
            return (margin + normalized * (height - 2 * margin)).toFloat()
        }

        // Trayectoria entre visitas cronológicas
        if (visitas.size >= 2) {
            val pathPaint = Paint().apply {
                color = Color.parseColor("#00F0FF")
                alpha = 120
                strokeWidth = 3f
                style = Paint.Style.STROKE
                isAntiAlias = true
                pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
            }
            val trajPath = Path()
            val ordenadas = visitas.sortedBy { it.inicio }
            ordenadas.forEachIndexed { index, v ->
                val px = projectX(v.longitudAncla)
                val py = projectY(v.latitudAncla)
                if (index == 0) trajPath.moveTo(px, py) else trajPath.lineTo(px, py)
            }
            canvas.drawPath(trajPath, pathPaint)
        }

        // Renderizado de Lugares
        for (lugar in lugares) {
            val px = projectX(lugar.longitud)
            val py = projectY(lugar.latitud)
            val catColor = getCategoryColor(lugar.categoria)

            val zonePx = maxOf(25f, (lugar.radio / 15.0).toFloat())

            // Resplandor exterior
            val glowPaint = Paint().apply {
                color = catColor
                alpha = 40
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(px, py, zonePx, glowPaint)

            // Límite de radio
            val boundaryPaint = Paint().apply {
                color = catColor
                alpha = 180
                strokeWidth = 2f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawCircle(px, py, zonePx, boundaryPaint)

            // Pin central
            val pinPaint = Paint().apply {
                color = catColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(px, py, 10f, pinPaint)

            val corePaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(px, py, 4f, corePaint)

            // Etiqueta
            val label = lugar.nombre ?: lugar.categoria.displayName
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val textWidth = textPaint.measureText(label)

            val cardPaint = Paint().apply {
                color = Color.parseColor("#DD0E1522")
                style = Paint.Style.FILL
            }
            val cardRect = RectF(
                px - textWidth / 2 - 12f,
                py + zonePx + 8f,
                px + textWidth / 2 + 12f,
                py + zonePx + 42f
            )
            canvas.drawRoundRect(cardRect, 8f, 8f, cardPaint)

            val cardBorderPaint = Paint().apply {
                color = catColor
                alpha = 160
                strokeWidth = 1.5f
                style = Paint.Style.STROKE
            }
            canvas.drawRoundRect(cardRect, 8f, 8f, cardBorderPaint)
            canvas.drawText(label, px - textWidth / 2, py + zonePx + 33f, textPaint)
        }

        // HUD Header
        val hudTitlePaint = Paint().apply {
            color = Color.parseColor("#00F0FF")
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("STAYTRACK // TACTICAL MAP INTELLIGENCE", 40f, 50f, hudTitlePaint)

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val hudSubPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("FECHA: $dateStr | LUGARES: ${lugares.size} | ESTANCIAS: ${visitas.size}", 40f, 80f, hudSubPaint)

        return bitmap
    }

    fun exportToStream(
        lugares: List<Place>,
        visitas: List<Visit>,
        outputStream: OutputStream,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 95
    ) {
        val bmp = generateMapBitmap(lugares, visitas)
        bmp.compress(format, quality, outputStream)
    }

    private data class BBox(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double) {
        val latSpan: Double get() = max(0.0001, maxLat - minLat)
        val lonSpan: Double get() = max(0.0001, maxLon - minLon)
    }

    private fun computeBoundingBox(points: List<Pair<Double, Double>>): BBox {
        if (points.isEmpty()) return BBox(40.0, 40.01, -3.0, -2.99)
        var minLat = points[0].first
        var maxLat = points[0].first
        var minLon = points[0].second
        var maxLon = points[0].second

        for (p in points) {
            if (p.first < minLat) minLat = p.first
            if (p.first > maxLat) maxLat = p.first
            if (p.second < minLon) minLon = p.second
            if (p.second > maxLon) maxLon = p.second
        }

        val latPad = max(0.002, (maxLat - minLat) * 0.15)
        val lonPad = max(0.002, (maxLon - minLon) * 0.15)

        return BBox(minLat - latPad, maxLat + latPad, minLon - lonPad, maxLon + lonPad)
    }

    private fun getCategoryColor(category: PlaceCategory): Int {
        return when (category) {
            PlaceCategory.CASA -> Color.parseColor("#3B82F6")
            PlaceCategory.TRABAJO -> Color.parseColor("#00F0FF")
            PlaceCategory.ENTRENAMIENTO -> Color.parseColor("#10B981")
            PlaceCategory.MEDICO -> Color.parseColor("#EF4444")
            PlaceCategory.OTRO -> Color.parseColor("#A855F7")
        }
    }
}
