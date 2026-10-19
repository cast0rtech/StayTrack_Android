package com.staytrack.core.logic

import com.staytrack.core.model.DeviceOrigin
import com.staytrack.core.model.MovementSample
import com.staytrack.core.model.Visit
import java.util.UUID

object VisitReconciler {

    const val MINIMA_DURACION_VALIDA_MILLIS = 2 * 60 * 1000L // 2 minutos

    /**
     * Reconcilia visitas y muestras de movimiento del reloj.
     * Si una visita registrada por el PHONE se solapa con movimiento detectado por el WATCH:
     * - El WATCH tiene prioridad absoluta.
     * - La visita del PHONE se recorta o descarta si los fragmentos restantes no alcanzan 2 minutos.
     * - Las visitas registradas por el WATCH se conservan intactas.
     */
    fun reconciliar(
        visitas: List<Visit>,
        movimientosReloj: List<MovementSample>,
        tiempoActual: Long = System.currentTimeMillis()
    ): List<Visit> {
        val movimientosWatch = movimientosReloj.filter { it.dispositivo == DeviceOrigin.WATCH }

        if (movimientosWatch.isEmpty()) {
            return visitas
        }

        val resultado = mutableListOf<Visit>()

        for (visita in visitas) {
            // El reloj siempre tiene prioridad y no se recorta por movimiento
            if (visita.origen == DeviceOrigin.WATCH) {
                resultado.add(visita)
                continue
            }

            // Visita de origen PHONE: evaluar solapamientos
            val visitaFin = visita.fin ?: tiempoActual
            var segmentos = listOf(Pair(visita.inicio, visitaFin))

            for (movimiento in movimientosWatch) {
                val nuevosSegmentos = mutableListOf<Pair<Long, Long>>()

                for ((segInicio, segFin) in segmentos) {
                    val solapeInicio = maxOf(segInicio, movimiento.inicio)
                    val solapeFin = minOf(segFin, movimiento.fin)

                    if (solapeInicio < solapeFin) {
                        // Existe solapamiento entre el móvil y el movimiento del reloj -> Recortar
                        val tramoIzquierdo = Pair(segInicio, solapeInicio)
                        if (tramoIzquierdo.second - tramoIzquierdo.first >= MINIMA_DURACION_VALIDA_MILLIS) {
                            nuevosSegmentos.add(tramoIzquierdo)
                        }

                        val tramoDerecho = Pair(solapeFin, segFin)
                        if (tramoDerecho.second - tramoDerecho.first >= MINIMA_DURACION_VALIDA_MILLIS) {
                            nuevosSegmentos.add(tramoDerecho)
                        }
                    } else {
                        // Sin solapamiento
                        nuevosSegmentos.add(Pair(segInicio, segFin))
                    }
                }
                segmentos = nuevosSegmentos
            }

            // Si todos los fragmentos fueron descartados (solapamiento total o residuo < 2 min)
            if (segmentos.isEmpty()) {
                continue
            }

            // Si no cambió
            if (segmentos.size == 1 &&
                segmentos[0].first == visita.inicio &&
                segmentos[0].second == visitaFin
            ) {
                resultado.add(visita)
            } else {
                // Reconstruir los fragmentos recortados como visitas válidas
                segmentos.forEachIndexed { index, (inicio, fin) ->
                    val idFragmento = if (index == 0) visita.id else UUID.randomUUID().toString()
                    val finVisita = if (visita.isActiva && index == segmentos.lastIndex && fin == visitaFin) null else fin
                    resultado.add(
                        visita.copy(
                            id = idFragmento,
                            inicio = inicio,
                            fin = finVisita
                        )
                    )
                }
            }
        }

        return resultado
    }
}
