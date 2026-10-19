package com.staytrack.core.logic

import com.staytrack.core.model.LocationReading
import com.staytrack.core.model.Visit
import java.util.UUID

sealed interface StayDetectionResult {
    data class EstanciaIniciada(val visit: Visit) : StayDetectionResult
    data class EstanciaActualizada(val visit: Visit) : StayDetectionResult
    data class EstanciaFinalizada(val visit: Visit) : StayDetectionResult
    data class EnProcesoCandidata(val elapsedMillis: Long) : StayDetectionResult
    data class PosibleDerivaGPS(val lecturasFuera: Int, val umbralRequerido: Int) : StayDetectionResult
    data class CandidataReiniciada(val distanciaMetros: Double) : StayDetectionResult
    data class PrecisionIgnorada(val precision: Float, val umbral: Float) : StayDetectionResult
}

class StayDetector(
    val radioMetros: Double = 75.0,
    val minimaDuracionEstanciaMillis: Long = 120_000L, // 2 minutos
    val maximaPrecisionValida: Float = 40.0f,
    val lecturasSalidaSostenida: Int = 3
) {
    private var ancla: LocationReading? = null
    private var inicioCandidata: Long = 0L
    private var ultimaLecturaDentro: LocationReading? = null
    private var contadorSalida: Int = 0
    private var visitActiva: Visit? = null

    val isEstanciaActiva: Boolean get() = visitActiva != null
    val currentVisit: Visit? get() = visitActiva
    val currentAnchor: LocationReading? get() = ancla

    fun procesarLectura(lectura: LocationReading): StayDetectionResult {
        // Ignora lecturas con mala precisión
        if (lectura.precision > maximaPrecisionValida) {
            return StayDetectionResult.PrecisionIgnorada(lectura.precision, maximaPrecisionValida)
        }

        val anchor = ancla
        if (anchor == null) {
            ancla = lectura
            inicioCandidata = lectura.timestamp
            ultimaLecturaDentro = lectura
            contadorSalida = 0
            return StayDetectionResult.EnProcesoCandidata(0L)
        }

        val distancia = GeoMath.distanciaMetros(
            anchor.latitud, anchor.longitud,
            lectura.latitud, lectura.longitud
        )

        return if (distancia <= radioMetros) {
            contadorSalida = 0
            ultimaLecturaDentro = lectura

            if (visitActiva == null) {
                val tiempoTranscurrido = lectura.timestamp - inicioCandidata
                if (tiempoTranscurrido >= minimaDuracionEstanciaMillis) {
                    val nuevaEstancia = Visit(
                        id = UUID.randomUUID().toString(),
                        placeId = null,
                        inicio = inicioCandidata,
                        fin = null,
                        origen = lectura.origen,
                        latitudAncla = anchor.latitud,
                        longitudAncla = anchor.longitud,
                        precision = anchor.precision
                    )
                    visitActiva = nuevaEstancia
                    StayDetectionResult.EstanciaIniciada(nuevaEstancia)
                } else {
                    StayDetectionResult.EnProcesoCandidata(tiempoTranscurrido)
                }
            } else {
                StayDetectionResult.EstanciaActualizada(visitActiva!!)
            }
        } else {
            // Distancia > radio
            if (visitActiva == null) {
                // Menos de 2 minutos antes de salir: se resetea la candidata
                ancla = lectura
                inicioCandidata = lectura.timestamp
                ultimaLecturaDentro = lectura
                contadorSalida = 0
                StayDetectionResult.CandidataReiniciada(distancia)
            } else {
                // Hay estancia activa confirmada: verificar si la salida es sostenida
                contadorSalida++
                if (contadorSalida >= lecturasSalidaSostenida) {
                    val finEstancia = ultimaLecturaDentro?.timestamp ?: lectura.timestamp
                    val estanciaFinalizada = visitActiva!!.copy(fin = finEstancia)
                    visitActiva = null

                    // La nueva lectura se convierte en candidata
                    ancla = lectura
                    inicioCandidata = lectura.timestamp
                    ultimaLecturaDentro = lectura
                    contadorSalida = 0

                    StayDetectionResult.EstanciaFinalizada(estanciaFinalizada)
                } else {
                    // Posible deriva GPS o salida no sostenida todavía
                    StayDetectionResult.PosibleDerivaGPS(contadorSalida, lecturasSalidaSostenida)
                }
            }
        }
    }

    fun forzarCierre(timestamp: Long = System.currentTimeMillis()): Visit? {
        val activa = visitActiva ?: return null
        val fin = ultimaLecturaDentro?.timestamp ?: timestamp
        val finalizada = activa.copy(fin = fin)
        visitActiva = null
        ancla = null
        contadorSalida = 0
        return finalizada
    }
}
