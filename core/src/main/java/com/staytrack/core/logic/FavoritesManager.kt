package com.staytrack.core.logic

import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import java.util.UUID

data class FusionResult(
    val lugaresActualizados: List<Place>,
    val visitasActualizadas: List<Visit>,
    val visitasReasignadasCount: Int
)

object FavoritesManager {

    /**
     * Crea un nuevo Place favorito explícito.
     */
    fun crearFavorito(
        nombre: String,
        latitud: Double,
        longitud: Double,
        categoria: PlaceCategory,
        radio: Double = 75.0,
        nombrePersonalizadoCategoria: String? = null,
        icono: String = categoria.defaultIcon
    ): Place {
        return Place(
            id = UUID.randomUUID().toString(),
            nombre = nombre,
            latitud = latitud,
            longitud = longitud,
            radio = radio,
            isFavorite = true,
            categoria = categoria,
            nombrePersonalizadoCategoria = nombrePersonalizadoCategoria,
            icono = icono,
            fechaCreacion = System.currentTimeMillis()
        )
    }

    /**
     * Crea un favorito a partir de una ubicación (lat, lon).
     */
    fun crearFavoritoDesdeUbicacion(
        nombre: String,
        latitud: Double,
        longitud: Double,
        categoria: PlaceCategory,
        radio: Double = 75.0
    ): Place {
        return crearFavorito(
            nombre = nombre,
            latitud = latitud,
            longitud = longitud,
            categoria = categoria,
            radio = radio
        )
    }

    /**
     * Crea un favorito a partir de una estancia Visit, asociando la visita al nuevo favorito.
     */
    fun crearFavoritoDesdeEstancia(
        visit: Visit,
        nombre: String,
        categoria: PlaceCategory,
        radio: Double = 75.0,
        nombrePersonalizadoCategoria: String? = null
    ): Pair<Place, Visit> {
        val nuevoFavorito = crearFavorito(
            nombre = nombre,
            latitud = visit.latitudAncla,
            longitud = visit.longitudAncla,
            categoria = categoria,
            radio = radio,
            nombrePersonalizadoCategoria = nombrePersonalizadoCategoria
        )
        val visitaActualizada = visit.copy(placeId = nuevoFavorito.id)
        return Pair(nuevoFavorito, visitaActualizada)
    }

    /**
     * Edita los atributos de un lugar favorito.
     */
    fun editarFavorito(
        place: Place,
        nuevoNombre: String?,
        nuevaCategoria: PlaceCategory,
        nuevoRadio: Double = place.radio,
        nuevoIcono: String? = null,
        nuevoNombrePersonalizado: String? = null,
        isFavorite: Boolean = true
    ): Place {
        return place.copy(
            nombre = nuevoNombre,
            categoria = nuevaCategoria,
            radio = nuevoRadio,
            icono = nuevoIcono ?: nuevaCategoria.defaultIcon,
            nombrePersonalizadoCategoria = nuevoNombrePersonalizado,
            isFavorite = isFavorite
        )
    }

    /**
     * Renombra un lugar favorito.
     */
    fun renombrarFavorito(place: Place, nuevoNombre: String): Place {
        return place.copy(nombre = nuevoNombre)
    }

    /**
     * Fusiona dos lugares: reasigna todas las visitas del lugar origen al lugar destino
     * y retira el lugar origen de la lista, actualizando el histórico de visitas.
     */
    fun fusionarLugares(
        lugarOrigenId: String,
        lugarDestinoId: String,
        lugares: List<Place>,
        visitas: List<Visit>
    ): FusionResult {
        require(lugarOrigenId != lugarDestinoId) { "No se puede fusionar un lugar consigo mismo" }

        val lugarDestino = lugares.find { it.id == lugarDestinoId }
            ?: throw IllegalArgumentException("Lugar destino con id $lugarDestinoId no encontrado")

        // Retirar el lugar origen
        val lugaresFiltrados = lugares.filter { it.id != lugarOrigenId }

        // Reasignar visitas del origen al destino
        var reasignadas = 0
        val visitasActualizadas = visitas.map { visit ->
            if (visit.placeId == lugarOrigenId) {
                reasignadas++
                visit.copy(placeId = lugarDestino.id)
            } else {
                visit
            }
        }

        return FusionResult(
            lugaresActualizados = lugaresFiltrados,
            visitasActualizadas = visitasActualizadas,
            visitasReasignadasCount = reasignadas
        )
    }
}
