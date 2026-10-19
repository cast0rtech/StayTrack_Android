package com.staytrack.core.export

import com.staytrack.core.model.Place
import com.staytrack.core.model.Visit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CsvExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun exportVisits(visitas: List<Visit>, lugares: List<Place>): String {
        val placeMap = lugares.associateBy { it.id }
        val sb = StringBuilder()

        sb.append("VisitId,PlaceName,Category,Latitude,Longitude,StartTime,EndTime,DurationMinutes,Origin,Precision\n")

        for (v in visitas) {
            val place = v.placeId?.let { placeMap[it] }
            val placeName = place?.nombre ?: "Sin nombre"
            val category = place?.categoria?.displayName ?: "Otro"
            val startIso = isoFormat.format(Date(v.inicio))
            val endIso = v.fin?.let { isoFormat.format(Date(it)) } ?: "ACTIVA"
            val durationMin = String.format(Locale.US, "%.1f", v.duracionMillis / 60000.0)

            sb.append(escape(v.id)).append(",")
            sb.append(escape(placeName)).append(",")
            sb.append(escape(category)).append(",")
            sb.append(String.format(Locale.US, "%.6f", v.latitudAncla)).append(",")
            sb.append(String.format(Locale.US, "%.6f", v.longitudAncla)).append(",")
            sb.append(escape(startIso)).append(",")
            sb.append(escape(endIso)).append(",")
            sb.append(durationMin).append(",")
            sb.append(escape(v.origen.name)).append(",")
            sb.append(String.format(Locale.US, "%.1f", v.precision)).append("\n")
        }

        return sb.toString()
    }

    private fun escape(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
