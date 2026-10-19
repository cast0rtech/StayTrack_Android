package com.staytrack.core.logic

import com.staytrack.core.model.DeviceOrigin
import com.staytrack.core.model.MovementSample
import com.staytrack.core.model.Visit
import org.junit.Assert.*
import org.junit.Test

class VisitReconcilerTest {

    private val hourMillis = 3600_000L
    private val t10_00 = 1700000000000L
    private val t11_00 = t10_00 + 1 * hourMillis
    private val t11_30 = t10_00 + 1 * hourMillis + 30 * 60_000L
    private val t12_00 = t10_00 + 2 * hourMillis
    private val t13_00 = t10_00 + 3 * hourMillis

    @Test
    fun testSolapamientoNuloConservaVisitaPhone() {
        val visitaPhone = Visit(
            id = "phone-v1",
            placeId = "casa",
            inicio = t10_00,
            fin = t11_00,
            origen = DeviceOrigin.PHONE,
            latitudAncla = 40.0,
            longitudAncla = -3.0
        )

        // Movimiento del reloj ocurre más tarde (12:00 a 13:00) -> Solapamiento nulo
        val movimientoWatch = MovementSample(
            id = "mov-1",
            dispositivo = DeviceOrigin.WATCH,
            inicio = t12_00,
            fin = t13_00
        )

        val resultado = VisitReconciler.reconciliar(
            visitas = listOf(visitaPhone),
            movimientosReloj = listOf(movimientoWatch)
        )

        assertEquals(1, resultado.size)
        assertEquals("phone-v1", resultado[0].id)
        assertEquals(t10_00, resultado[0].inicio)
        assertEquals(t11_00, resultado[0].fin)
    }

    @Test
    fun testSolapamientoParcialRecortaVisitaPhone() {
        // Visita PHONE de 10:00 a 13:00 (3 horas = 180 min)
        val visitaPhone = Visit(
            id = "phone-v2",
            placeId = "oficina",
            inicio = t10_00,
            fin = t13_00,
            origen = DeviceOrigin.PHONE,
            latitudAncla = 40.0,
            longitudAncla = -3.0
        )

        // El usuario sale con el reloj entre las 11:00 y las 12:00 (1 hora en el medio)
        val movimientoWatch = MovementSample(
            id = "mov-watch",
            dispositivo = DeviceOrigin.WATCH,
            inicio = t11_00,
            fin = t12_00
        )

        val resultado = VisitReconciler.reconciliar(
            visitas = listOf(visitaPhone),
            movimientosReloj = listOf(movimientoWatch)
        )

        // Debe recortar el tramo del medio y producir 2 fragmentos válidos:
        // [10:00 - 11:00] (60 min >= 2 min) y [12:00 - 13:00] (60 min >= 2 min)
        assertEquals(2, resultado.size)

        val tramo1 = resultado[0]
        assertEquals(t10_00, tramo1.inicio)
        assertEquals(t11_00, tramo1.fin)

        val tramo2 = resultado[1]
        assertEquals(t12_00, tramo2.inicio)
        assertEquals(t13_00, tramo2.fin)
    }

    @Test
    fun testSolapamientoTotalDescartaVisitaPhone() {
        // Visita PHONE de 10:00 a 11:00 (60 minutos)
        val visitaPhone = Visit(
            id = "phone-v3",
            placeId = "oficina",
            inicio = t10_00,
            fin = t11_00,
            origen = DeviceOrigin.PHONE,
            latitudAncla = 40.0,
            longitudAncla = -3.0
        )

        // El reloj estuvo en movimiento durante todo ese tiempo (09:50 a 11:10)
        val movimientoWatchTotal = MovementSample(
            id = "mov-total",
            dispositivo = DeviceOrigin.WATCH,
            inicio = t10_00 - 10 * 60_000L,
            fin = t11_00 + 10 * 60_000L
        )

        val resultado = VisitReconciler.reconciliar(
            visitas = listOf(visitaPhone),
            movimientosReloj = listOf(movimientoWatchTotal)
        )

        // Debe descartarse completamente porque el solapamiento es total
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun testSolapamientoConFragmentoResidualMenorADosMinutosSeDescarta() {
        // Visita PHONE de 10:00 a 10:31 (31 minutos)
        val visitaPhone = Visit(
            id = "phone-v4",
            placeId = "oficina",
            inicio = t10_00,
            fin = t10_00 + 31 * 60_000L,
            origen = DeviceOrigin.PHONE,
            latitudAncla = 40.0,
            longitudAncla = -3.0
        )

        // Movimiento WATCH de 10:00 a 10:30 (deja solo 1 minuto residual de 10:30 a 10:31)
        val movimientoWatch = MovementSample(
            id = "mov-30min",
            dispositivo = DeviceOrigin.WATCH,
            inicio = t10_00,
            fin = t10_00 + 30 * 60_000L
        )

        val resultado = VisitReconciler.reconciliar(
            visitas = listOf(visitaPhone),
            movimientosReloj = listOf(movimientoWatch)
        )

        // El fragmento de 1 minuto es < 2 minutos -> se descarta
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun testVisitaWatchTienePrioridadYNoSeRecortaPorMovimiento() {
        val visitaWatch = Visit(
            id = "watch-v1",
            placeId = "gym",
            inicio = t10_00,
            fin = t11_00,
            origen = DeviceOrigin.WATCH,
            latitudAncla = 40.0,
            longitudAncla = -3.0
        )

        val movimientoWatch = MovementSample(
            id = "mov-any",
            dispositivo = DeviceOrigin.WATCH,
            inicio = t10_00,
            fin = t11_00
        )

        val resultado = VisitReconciler.reconciliar(
            visitas = listOf(visitaWatch),
            movimientosReloj = listOf(movimientoWatch)
        )

        assertEquals(1, resultado.size)
        assertEquals("watch-v1", resultado[0].id)
        assertEquals(DeviceOrigin.WATCH, resultado[0].origen)
    }
}
