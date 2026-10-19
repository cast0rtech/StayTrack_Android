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
     * Asigna una coordenada al mejor Place existente cuyo radio contiene el punto,
     * priorizando lugares favoritos frente a lugares sin nombre o no favoritos.
     * Si no encaja en ninguno, crea un Place sin nombre.
     */
    fun asignarLugar(
        latitud: Double,
        longitud: Double,
        lugaresExistentes: List<Place>,
        radioDefecto: Double = 75.0
    ): AsignacionResult {
        // Encontrar lugares cuyo radio contiene la coordenada
        val candidatos = lugaresExistentes.map { lugar ->
            val dist = GeoMath.distanciaMetros(latitud, longitud, lugar.latitud, lugar.longitud)
            Pair(lugar, dist)
        }.filter { (lugar, dist) ->
            dist <= lugar.radio
        }

        if (candidatos.isNotEmpty()) {
            val favoritos = candidatos.filter { it.first.isFavorite }

            val mejorCandidato = if (favoritos.isNotEmpty()) {
                // Prioridad 1: Favoritos ordenados por distancia mínima
                favoritos.minByOrNull { it.second }!!
            } else {
                // Prioridad 2: No favoritos ordenados por distancia mínima
                candidatos.minByOrNull { it.second }!!
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
     * Enlaza una estancia Visit al mejor Place, rellenando su placeId.
     */
    fun asignarVisita(
        visit: Visit,
        lugaresExistentes: List<Place>,
        radioDefecto: Double = 75.0
    ): Pair<Visit, Place> {
        val resultado = asignarLugar(
            latitud = visit.latitudAncla,
            longitud = visit.longitudAncla,
            lugaresExistentes = lugaresExistentes,
            radioDefecto = radioDefecto
        )

        val visitaActualizada = visit.copy(placeId = resultado.lugar.id)
        return Pair(visitaActualizada, resultado.lugar)
    }
}
