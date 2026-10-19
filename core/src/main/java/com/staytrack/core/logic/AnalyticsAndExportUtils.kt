package com.staytrack.core.logic

import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class TimeAggregationResult(
    val tiempoPorLugar: Map<String, Long>,
    val tiempoPorCategoria: Map<PlaceCategory, Long>,
    val tiempoTotalMillis: Long,
    val visitasContadas: Int
)

data class WaypointExportData(
    val id: String,
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val categoria: PlaceCategory,
    val descripcion: String,
    val timestamp: Long
)

object AnalyticsAndExportUtils {

    /**
     * Agrega el tiempo de estancias por lugar y categoría dentro de un rango temporal [rangoInicio, rangoFin].
     */
    fun agregarTiempo(
        visitas: List<Visit>,
        lugares: List<Place>,
        rangoInicio: Long,
        rangoFin: Long
    ): TimeAggregationResult {
        require(rangoInicio <= rangoFin) { "rangoInicio no puede ser posterior a rangoFin" }

        val placeMap = lugares.associateBy { it.id }
        val tiempoPorLugar = mutableMapOf<String, Long>()
        val tiempoPorCategoria = mutableMapOf<PlaceCategory, Long>()
        var tiempoTotal = 0L
        var visitasContadas = 0

        for (visita in visitas) {
            val finVisita = visita.fin ?: System.currentTimeMillis()

            // Comprobar si hay intersección con el rango de fechas
            val inicioEfectivo = maxOf(visita.inicio, rangoInicio)
            val finEfectivo = minOf(finVisita, rangoFin)

            if (finEfectivo > inicioEfectivo) {
                val duracionInterseccion = finEfectivo - inicioEfectivo
                val placeId = visita.placeId ?: "desconocido"
                val categoria = visita.placeId?.let { placeMap[it]?.categoria } ?: PlaceCategory.OTRO

                tiempoPorLugar[placeId] = (tiempoPorLugar[placeId] ?: 0L) + duracionInterseccion
                tiempoPorCategoria[categoria] = (tiempoPorCategoria[categoria] ?: 0L) + duracionInterseccion
                tiempoTotal += duracionInterseccion
                visitasContadas++
            }
        }

        return TimeAggregationResult(
            tiempoPorLugar = tiempoPorLugar,
            tiempoPorCategoria = tiempoPorCategoria,
            tiempoTotalMillis = tiempoTotal,
            visitasContadas = visitasContadas
        )
    }

    /**
     * Utilidad de formateo de duración a formato legible (ej. "2h 15m", "45m").
     */
    fun formatearDuracion(duracionMillis: Long): String {
        val totalMinutos = duracionMillis / 60000
        val horas = totalMinutos / 60
        val minutos = totalMinutos % 60

        return when {
            horas > 0 && minutos > 0 -> "${horas}h ${minutos}m"
            horas > 0 -> "${horas}h"
            else -> "${minutos}m"
        }
    }

    /**
     * Formatea un timestamp a formato ISO 8601 UTC.
     */
    fun formatearFechaIso(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(timestamp))
    }

    /**
     * Genera una lista unificada de waypoints con nombre, coordenadas y categoría para exportación.
     */
    fun generarWaypointsParaExportacion(
        lugares: List<Place>,
        visitas: List<Visit>
    ): List<WaypointExportData> {
        val placeMap = lugares.associateBy { it.id }
        val resultado = mutableListOf<WaypointExportData>()

        // Waypoints para Lugares (Places)
        for (lugar in lugares) {
            val nombre = lugar.nombre ?: "Lugar sin nombre"
            val desc = "Lugar (${lugar.categoria.displayName}) • Radio: ${lugar.radio.toInt()}m"
            resultado.add(
                WaypointExportData(
                    id = lugar.id,
                    nombre = nombre,
                    latitud = lugar.latitud,
                    longitud = lugar.longitud,
                    categoria = lugar.categoria,
                    descripcion = desc,
                    timestamp = lugar.fechaCreacion
                )
            )
        }

        // Waypoints para Visitas
        for (visita in visitas) {
            val lugar = visita.placeId?.let { placeMap[it] }
            val nombreVisita = lugar?.nombre ?: "Estancia (${visita.origen})"
            val duracionStr = formatearDuracion(visita.duracionMillis)
            val desc = "Estancia de $duracionStr • Origen: ${visita.origen}"
            val cat = lugar?.categoria ?: PlaceCategory.OTRO

            resultado.add(
                WaypointExportData(
                    id = visita.id,
                    nombre = "$nombreVisita [$duracionStr]",
                    latitud = visita.latitudAncla,
                    longitud = visita.longitudAncla,
                    categoria = cat,
                    descripcion = desc,
                    timestamp = visita.inicio
                )
            )
        }

        return resultado
    }

    /**
     * Genera documento GPX 1.1 estándar a partir de waypoints.
     */
    fun generarXmlGpx(waypoints: List<WaypointExportData>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<gpx version="1.1" creator="StayTrack Android" xmlns="http://www.topografix.com/GPX/1/1">""").append("\n")
        sb.append("  <metadata>\n")
        sb.append("    <name>StayTrack Waypoints</name>\n")
        sb.append("    <time>${formatearFechaIso(System.currentTimeMillis())}</time>\n")
        sb.append("  </metadata>\n")

        for (wpt in waypoints) {
            sb.append("""  <wpt lat="${String.format(Locale.US, "%.6f", wpt.latitud)}" lon="${String.format(Locale.US, "%.6f", wpt.longitud)}">""").append("\n")
            sb.append("    <name>${escapeXml(wpt.nombre)}</name>\n")
            sb.append("    <desc>${escapeXml(wpt.descripcion)}</desc>\n")
            sb.append("    <time>${formatearFechaIso(wpt.timestamp)}</time>\n")
            sb.append("    <type>${escapeXml(wpt.categoria.name)}</type>\n")
            sb.append("  </wpt>\n")
        }

        sb.append("</gpx>")
        return sb.toString()
    }

    /**
     * Genera documento KML 2.2 estándar a partir de waypoints.
     */
    fun generarXmlKml(waypoints: List<WaypointExportData>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<kml xmlns="http://www.opengis.net/kml/2.2">""").append("\n")
        sb.append("  <Document>\n")
        sb.append("    <name>StayTrack GIS Export</name>\n")

        for (wpt in waypoints) {
            sb.append("    <Placemark>\n")
            sb.append("      <name>${escapeXml(wpt.nombre)}</name>\n")
            sb.append("      <description>${escapeXml(wpt.descripcion)}</description>\n")
            sb.append("      <Point>\n")
            sb.append("""        <coordinates>${String.format(Locale.US, "%.6f,%.6f,0", wpt.longitud, wpt.latitud)}</coordinates>""").append("\n")
            sb.append("      </Point>\n")
            sb.append("    </Placemark>\n")
        }

        sb.append("  </Document>\n")
        sb.append("</kml>")
        return sb.toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
