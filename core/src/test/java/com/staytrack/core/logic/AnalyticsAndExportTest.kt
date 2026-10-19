package com.staytrack.core.logic

import com.staytrack.core.model.DeviceOrigin
import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import org.junit.Assert.*
import org.junit.Test

class AnalyticsAndExportTest {

    private val hourMillis = 3600_000L
    private val t10_00 = 1700000000000L
    private val t12_00 = t10_00 + 2 * hourMillis
    private val t14_00 = t10_00 + 4 * hourMillis

    @Test
    fun testAgregarTiempoPorLugarYCategoriaEnRango() {
        val lugarCasa = Place(
            id = "casa",
            nombre = "Mi Casa",
            latitud = 40.0,
            longitud = -3.0,
            categoria = PlaceCategory.CASA
        )
        val lugarTrabajo = Place(
            id = "trabajo",
            nombre = "Oficina",
            latitud = 40.1,
            longitud = -3.1,
            categoria = PlaceCategory.TRABAJO
        )

        val visitas = listOf(
            // 10:00 a 12:00 en Casa (2 horas = 7200s)
            Visit(id = "v1", placeId = "casa", inicio = t10_00, fin = t12_00, origen = DeviceOrigin.PHONE, latitudAncla = 40.0, longitudAncla = -3.0),
            // 12:00 a 14:00 en Trabajo (2 horas = 7200s)
            Visit(id = "v2", placeId = "trabajo", inicio = t12_00, fin = t14_00, origen = DeviceOrigin.PHONE, latitudAncla = 40.1, longitudAncla = -3.1)
        )

        // Rango de consulta: 11:00 a 13:00 (solapa 1h con Casa y 1h con Trabajo)
        val rangoInicio = t10_00 + 1 * hourMillis // 11:00
        val rangoFin = t10_00 + 3 * hourMillis    // 13:00

        val resultado = AnalyticsAndExportUtils.agregarTiempo(
            visitas = visitas,
            lugares = listOf(lugarCasa, lugarTrabajo),
            rangoInicio = rangoInicio,
            rangoFin = rangoFin
        )

        assertEquals(2 * hourMillis, resultado.tiempoTotalMillis)
        assertEquals(1 * hourMillis, resultado.tiempoPorLugar["casa"])
        assertEquals(1 * hourMillis, resultado.tiempoPorLugar["trabajo"])
        assertEquals(1 * hourMillis, resultado.tiempoPorCategoria[PlaceCategory.CASA])
        assertEquals(1 * hourMillis, resultado.tiempoPorCategoria[PlaceCategory.TRABAJO])
        assertEquals(2, resultado.visitasContadas)
    }

    @Test
    fun testFormatearDuracion() {
        assertEquals("2h 30m", AnalyticsAndExportUtils.formatearDuracion(150 * 60_000L))
        assertEquals("1h", AnalyticsAndExportUtils.formatearDuracion(60 * 60_000L))
        assertEquals("45m", AnalyticsAndExportUtils.formatearDuracion(45 * 60_000L))
    }

    @Test
    fun testGeneracionWaypointsYExportacionGpxKml() {
        val lugar = Place(
            id = "p1",
            nombre = "Centro Médico",
            latitud = 40.41,
            longitud = -3.70,
            categoria = PlaceCategory.MEDICO
        )
        val visita = Visit(
            id = "v1",
            placeId = "p1",
            inicio = t10_00,
            fin = t12_00,
            origen = DeviceOrigin.WATCH,
            latitudAncla = 40.41,
            longitudAncla = -3.70
        )

        val waypoints = AnalyticsAndExportUtils.generarWaypointsParaExportacion(
            lugares = listOf(lugar),
            visitas = listOf(visita)
        )

        assertEquals(2, waypoints.size)

        // Validar GPX
        val gpx = AnalyticsAndExportUtils.generarXmlGpx(waypoints)
        assertTrue(gpx.startsWith("<?xml"))
        assertTrue(gpx.contains("<gpx version=\"1.1\""))
        assertTrue(gpx.contains("<wpt lat=\"40.410000\" lon=\"-3.700000\">"))
        assertTrue(gpx.contains("<name>Centro Médico</name>"))
        assertTrue(gpx.contains("</gpx>"))

        // Validar KML
        val kml = AnalyticsAndExportUtils.generarXmlKml(waypoints)
        assertTrue(kml.startsWith("<?xml"))
        assertTrue(kml.contains("<kml xmlns=\"http://www.opengis.net/kml/2.2\">"))
        assertTrue(kml.contains("<Placemark>"))
        assertTrue(kml.contains("<coordinates>-3.700000,40.410000,0</coordinates>"))
        assertTrue(kml.contains("</kml>"))
    }
}
