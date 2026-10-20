package com.staytrack.core.logic

import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import java.util.UUID

data class AsignacionResult(
    val lugar: Place,
    val esNuevo: Boolean,
    val distanciaMetros: Double
)

object PlaceAssigner {

    /**
     * Asigna una coordenada al mejor Place existente cuyo radio contiene el punto o está en su
     * zona de influencia por proximidad, priorizando favoritos y evitando crear lugares redundantes.
     * Si no encaja en ninguno, crea un Place sin nombre.
     */
    fun asignarLugar(
        latitud: Double,
        longitud: Double,
        lugaresExistentes: List<Place>,
        radioDefecto: Double = 75.0,
        toleranciaProximidadMetros: Double = 40.0
    ): AsignacionResult {
        // Filtrar lugares con coordenadas válidas (ignorar 0.0, 0.0 sin inicializar)
        val lugaresValidos = lugaresExistentes.filter { it.latitud != 0.0 || it.longitud != 0.0 }

        val evaluados = lugaresValidos.map { lugar ->
            val dist = GeoMath.distanciaMetros(latitud, longitud, lugar.latitud, lugar.longitud)
            Triple(lugar, dist, calcularRadioEfectivo(lugar, toleranciaProximidadMetros))
        }

        // Candidatos dentro del radio efectivo (radio base + tolerancia / zona atracción)
        val candidatos = evaluados.filter { (_, dist, radioEfectivo) ->
            dist <= radioEfectivo
        }

        if (candidatos.isNotEmpty()) {
            val favoritos = candidatos.filter { it.first.isFavorite }

            val mejorCandidato = if (favoritos.isNotEmpty()) {
                // Prioridad 1: Favoritos ordenados por distancia mínima
                favoritos.minByOrNull { it.second }!!
            } else {
                // Prioridad 2: Lugares con nombre propio frente a genéricos
                val conNombre = candidatos.filter { !it.first.nombre.isNullOrBlank() }
                if (conNombre.isNotEmpty()) {
                    conNombre.minByOrNull { it.second }!!
                } else {
                    // Prioridad 3: Lugares más cercanos
                    candidatos.minByOrNull { it.second }!!
                }
            }

            return AsignacionResult(
                lugar = mejorCandidato.first,
                esNuevo = false,
                distanciaMetros = mejorCandidato.second
            )
        }

        // Si no encaja en ninguno, crea un Place sin nombre
        val nuevoLugar = Place(
            id = UUID.randomUUID().toString(),
            nombre = null,
            latitud = latitud,
            longitud = longitud,
            radio = radioDefecto,
            isFavorite = false,
            categoria = PlaceCategory.OTRO,
            nombrePersonalizadoCategoria = null,
            icono = PlaceCategory.OTRO.defaultIcon,
            fechaCreacion = System.currentTimeMillis()
        )

        return AsignacionResult(
            lugar = nuevoLugar,
            esNuevo = true,
            distanciaMetros = 0.0
        )
    }

    /**
     * Calcula el radio de atracción de un lugar. Los favoritos tienen una zona de atracción
     * más amplia (1.5x su radio o +50m) para capturar edificios grandes, aparcamientos y
     * desviaciones de GPS indoor. Los lugares comunes tienen una tolerancia de agrupación
     * para evitar crear lugares redundantes a escasos metros.
     */
    fun calcularRadioEfectivo(lugar: Place, toleranciaProximidadMetros: Double = 40.0): Double {
        return if (lugar.isFavorite) {
            maxOf(lugar.radio * 1.5, lugar.radio + 50.0)
        } else {
            lugar.radio + toleranciaProximidadMetros
        }
    }

    /**
     * Enlaza una estancia Visit al mejor Place, rellenando su placeId.
     */
    fun asignarVisita(
        visit: Visit,
        lugaresExistentes: List<Place>,
        radioDefecto: Double = 75.0,
        toleranciaProximidadMetros: Double = 40.0
    ): Pair<Visit, Place> {
        val resultado = asignarLugar(
            latitud = visit.latitudAncla,
            longitud = visit.longitudAncla,
            lugaresExistentes = lugaresExistentes,
            radioDefecto = radioDefecto,
            toleranciaProximidadMetros = toleranciaProximidadMetros
        )

        val visitaActualizada = visit.copy(placeId = resultado.lugar.id)
        return Pair(visitaActualizada, resultado.lugar)
    }
}

