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
     * Si el lugar destino carecía de coordenadas GPS válidas y el origen sí las tenía,
     * el destino hereda automáticamente las coordenadas del origen.
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

        val lugarOrigen = lugares.find { it.id == lugarOrigenId }

        // Si el destino no tenía coordenadas válidas (0,0) y el origen sí, hereda coordenadas
        val destinoAjustado = if (
            lugarDestino.latitud == 0.0 && lugarDestino.longitud == 0.0 &&
            lugarOrigen != null && (lugarOrigen.latitud != 0.0 || lugarOrigen.longitud != 0.0)
        ) {
            lugarDestino.copy(latitud = lugarOrigen.latitud, longitud = lugarOrigen.longitud)
        } else {
            lugarDestino
        }

        // Retirar el lugar origen y actualizar destino si cambió
        val lugaresFiltrados = lugares
            .filter { it.id != lugarOrigenId }
            .map { if (it.id == lugarDestinoId) destinoAjustado else it }

        // Reasignar visitas del origen al destino
        var reasignadas = 0
        val visitasActualizadas = visitas.map { visit ->
            if (visit.placeId == lugarOrigenId) {
                reasignadas++
                visit.copy(placeId = destinoAjustado.id)
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

    /**
     * Consolida automáticamente lugares duplicados o muy cercanos entre sí.
     * Prioriza favoritos y lugares con nombre, reasignando las visitas al lugar canónico
     * y eliminando lugares redundantes creados por deriva GPS o visitas continuas.
     */
    fun consolidarLugaresCercanos(
        lugares: List<Place>,
        visitas: List<Visit>,
        umbralMetros: Double = 120.0
    ): FusionResult {
        if (lugares.size <= 1) {
            return FusionResult(lugares, visitas, 0)
        }

        var lugaresActuales = lugares.toMutableList()
        var visitasActuales = visitas.toList()
        var totalReasignadas = 0

        // Si hay favoritos sin coordenadas (0,0), vincularlos al grupo de visitas más relevante
        val favoritosSinCoord = lugaresActuales.filter { it.isFavorite && it.latitud == 0.0 && it.longitud == 0.0 }
        val lugaresConCoord = lugaresActuales.filter { it.latitud != 0.0 || it.longitud != 0.0 }

        if (favoritosSinCoord.isNotEmpty() && lugaresConCoord.isNotEmpty()) {
            for (fav in favoritosSinCoord) {
                val origenCandidato = lugaresConCoord.maxByOrNull { l ->
                    visitasActuales.count { it.placeId == l.id }
                }
                if (origenCandidato != null) {
                    val fusion = fusionarLugares(origenCandidato.id, fav.id, lugaresActuales, visitasActuales)
                    lugaresActuales = fusion.lugaresActualizados.toMutableList()
                    visitasActuales = fusion.visitasActualizadas
                    totalReasignadas += fusion.visitasReasignadasCount
                }
            }
        }

        var huboFusion: Boolean
        do {
            huboFusion = false
            val n = lugaresActuales.size
            outerLoop@ for (i in 0 until n) {
                val p1 = lugaresActuales[i]
                if (p1.latitud == 0.0 && p1.longitud == 0.0) continue

                for (j in i + 1 until n) {
                    val p2 = lugaresActuales[j]
                    if (p2.latitud == 0.0 && p2.longitud == 0.0) continue

                    val dist = GeoMath.distanciaMetros(p1.latitud, p1.longitud, p2.latitud, p2.longitud)
                    val radioEfectivo = maxOf(p1.radio, p2.radio, umbralMetros)

                    if (dist <= radioEfectivo) {
                        // Decidir quién es el canónico (destino) y quién el que se fusiona (origen)
                        val (destino, origen) = if (p1.isFavorite && !p2.isFavorite) {
                            Pair(p1, p2)
                        } else if (!p1.isFavorite && p2.isFavorite) {
                            Pair(p2, p1)
                        } else if (!p1.nombre.isNullOrBlank() && p2.nombre.isNullOrBlank()) {
                            Pair(p1, p2)
                        } else if (p1.nombre.isNullOrBlank() && !p2.nombre.isNullOrBlank()) {
                            Pair(p2, p1)
                        } else {
                            if (p1.fechaCreacion <= p2.fechaCreacion) Pair(p1, p2) else Pair(p2, p1)
                        }

                        val fusion = fusionarLugares(origen.id, destino.id, lugaresActuales, visitasActuales)
                        lugaresActuales = fusion.lugaresActualizados.toMutableList()
                        visitasActuales = fusion.visitasActualizadas
                        totalReasignadas += fusion.visitasReasignadasCount
                        huboFusion = true
                        break@outerLoop
                    }
                }
            }
        } while (huboFusion && lugaresActuales.size > 1)

        return FusionResult(
            lugaresActualizados = lugaresActuales,
            visitasActualizadas = visitasActuales,
            visitasReasignadasCount = totalReasignadas
        )
    }
}
