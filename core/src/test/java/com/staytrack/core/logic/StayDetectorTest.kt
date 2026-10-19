package com.staytrack.core.logic

import com.staytrack.core.model.DeviceOrigin
import com.staytrack.core.model.LocationReading
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StayDetectorTest {

    private lateinit var detector: StayDetector
    private val latBase = 40.416775
    private val lonBase = -3.703790
    private val t0 = 1700000000000L

    @Before
    fun setUp() {
        detector = StayDetector(
            radioMetros = 75.0,
            minimaDuracionEstanciaMillis = 120_000L, // 2 minutos
            maximaPrecisionValida = 40.0f,
            lecturasSalidaSostenida = 3
        )
    }

    @Test
    fun testMenosDeDosMinutosNoGeneraEstancia() {
        // Lectura en t=0
        detector.procesarLectura(LocationReading(latBase, lonBase, 10f, t0))
        assertFalse(detector.isEstanciaActiva)

        // Lectura en t=60s dentro del radio
        val res60 = detector.procesarLectura(LocationReading(latBase, lonBase, 10f, t0 + 60_000L))
        assertTrue(res60 is StayDetectionResult.EnProcesoCandidata)
        assertFalse(detector.isEstanciaActiva)

        // Lectura en t=119s (todavía falta 1 segundo para los 2 minutos)
        val res119 = detector.procesarLectura(LocationReading(latBase, lonBase, 10f, t0 + 119_000L))
        assertTrue(res119 is StayDetectionResult.EnProcesoCandidata)
        assertFalse(detector.isEstanciaActiva)

        // El usuario se desplaza a 250m de distancia antes de cumplir los 2 minutos
        val resMover = detector.procesarLectura(LocationReading(latBase + 0.003, lonBase, 10f, t0 + 130_000L))
        assertTrue(resMover is StayDetectionResult.CandidataReiniciada)
        assertFalse(detector.isEstanciaActiva)
        assertNull(detector.currentVisit)
    }

    @Test
    fun testParadaValidaGeneraEstancia() {
        // Lectura en t=0
        detector.procesarLectura(LocationReading(latBase, lonBase, 10f, t0))

        // Lectura en t=60s
        detector.procesarLectura(LocationReading(latBase + 0.00005, lonBase, 10f, t0 + 60_000L))

        // Lectura en t=120s (cumple 2 minutos dentro del radio de 75m)
        val res120 = detector.procesarLectura(LocationReading(latBase + 0.00008, lonBase, 10f, t0 + 120_000L))

        assertTrue(res120 is StayDetectionResult.EstanciaIniciada)
        val visit = (res120 as StayDetectionResult.EstanciaIniciada).visit

        assertEquals(t0, visit.inicio)
        assertNull(visit.fin)
        assertTrue(visit.isActiva)
        assertEquals(latBase, visit.latitudAncla, 0.00001)
        assertTrue(detector.isEstanciaActiva)
    }

    @Test
    fun testSalidaFalsaPorDerivaGpsNoCierraEstancia() {
        // Establecer estancia confirmada
        detector.procesarLectura(LocationReading(latBase, lonBase, 8f, t0))
        detector.procesarLectura(LocationReading(latBase, lonBase, 8f, t0 + 120_000L))
        assertTrue(detector.isEstanciaActiva)

        // Llega 1 lectura espuria a 300m por deriva GPS (glitch)
        val resGlitch = detector.procesarLectura(LocationReading(latBase + 0.003, lonBase, 10f, t0 + 130_000L))
        assertTrue(resGlitch is StayDetectionResult.PosibleDerivaGPS)
        assertEquals(1, (resGlitch as StayDetectionResult.PosibleDerivaGPS).lecturasFuera)
        // La estancia DEBE seguir activa
        assertTrue(detector.isEstanciaActiva)

        // La siguiente lectura vuelve dentro del radio
        val resVuelta = detector.procesarLectura(LocationReading(latBase, lonBase, 8f, t0 + 140_000L))
        assertTrue(resVuelta is StayDetectionResult.EstanciaActualizada)
        assertTrue(detector.isEstanciaActiva)
    }

    @Test
    fun testSalidaRealCierraEstancia() {
        // Establecer estancia confirmada
        detector.procesarLectura(LocationReading(latBase, lonBase, 8f, t0))
        detector.procesarLectura(LocationReading(latBase, lonBase, 8f, t0 + 120_000L))
        detector.procesarLectura(LocationReading(latBase, lonBase, 8f, t0 + 150_000L))
        assertTrue(detector.isEstanciaActiva)

        // Salida real: 3 lecturas consecutivas fuera del radio
        detector.procesarLectura(LocationReading(latBase + 0.004, lonBase, 10f, t0 + 160_000L))
        detector.procesarLectura(LocationReading(latBase + 0.005, lonBase, 10f, t0 + 170_000L))
        val resSalida = detector.procesarLectura(LocationReading(latBase + 0.006, lonBase, 10f, t0 + 180_000L))

        assertTrue(resSalida is StayDetectionResult.EstanciaFinalizada)
        val finalizada = (resSalida as StayDetectionResult.EstanciaFinalizada).visit

        assertNotNull(finalizada.fin)
        assertEquals(t0 + 150_000L, finalizada.fin) // Se cierra con la última lectura válida dentro
        assertFalse(detector.isEstanciaActiva)
    }

    @Test
    fun testIgnoraLecturasConMalaPrecision() {
        // Lectura inicial válida
        detector.procesarLectura(LocationReading(latBase, lonBase, 15f, t0))

        // Lectura con precisión 65m (> 40m umbral)
        val resMala = detector.procesarLectura(LocationReading(latBase, lonBase, 65f, t0 + 60_000L))
        assertTrue(resMala is StayDetectionResult.PrecisionIgnorada)

        // No debe contar para dwell time
        val resSiguiente = detector.procesarLectura(LocationReading(latBase, lonBase, 15f, t0 + 70_000L))
        assertTrue(resSiguiente is StayDetectionResult.EnProcesoCandidata)
        assertEquals(70_000L, (resSiguiente as StayDetectionResult.EnProcesoCandidata).elapsedMillis)
    }
}
