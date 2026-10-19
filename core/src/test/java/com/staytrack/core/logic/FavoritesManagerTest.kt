package com.staytrack.core.logic

import com.staytrack.core.model.DeviceOrigin
import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import org.junit.Assert.*
import org.junit.Test

class FavoritesManagerTest {

    @Test
    fun testCrearFavoritoYCrearDesdeEstancia() {
        val favDirecto = FavoritesManager.crearFavorito(
            nombre = "Mi Trabajo",
            latitud = 40.416,
            longitud = -3.703,
            categoria = PlaceCategory.TRABAJO,
            radio = 100.0
        )

        assertEquals("Mi Trabajo", favDirecto.nombre)
        assertTrue(favDirecto.isFavorite)
        assertEquals(PlaceCategory.TRABAJO, favDirecto.categoria)
        assertEquals(100.0, favDirecto.radio, 0.01)

        val visita = Visit(
            id = "visita-1",
            placeId = null,
            inicio = 1000L,
            fin = 5000L,
            origen = DeviceOrigin.WATCH,
            latitudAncla = 40.420,
            longitudAncla = -3.710
        )

        val (nuevoFav, visitaActualizada) = FavoritesManager.crearFavoritoDesdeEstancia(
            visit = visita,
            nombre = "Gimnasio Central",
            categoria = PlaceCategory.ENTRENAMIENTO
        )

        assertEquals("Gimnasio Central", nuevoFav.nombre)
        assertTrue(nuevoFav.isFavorite)
        assertEquals(nuevoFav.id, visitaActualizada.placeId)
    }

    @Test
    fun testEditarYRenombrarFavorito() {
        val placeOriginal = Place(
            id = "place-1",
            nombre = "Oficina Antigua",
            latitud = 40.0,
            longitud = -3.0,
            categoria = PlaceCategory.TRABAJO,
            isFavorite = true
        )

        val renombrado = FavoritesManager.renombrarFavorito(placeOriginal, "Sede Principal")
        assertEquals("Sede Principal", renombrado.nombre)

        val editado = FavoritesManager.editarFavorito(
            place = renombrado,
            nuevoNombre = "Sede Principal Madrid",
            nuevaCategoria = PlaceCategory.TRABAJO,
            nuevoRadio = 100.0,
            nuevoNombrePersonalizado = "HQ",
            isFavorite = true
        )

        assertEquals("Sede Principal Madrid", editado.nombre)
        assertEquals(100.0, editado.radio, 0.01)
        assertEquals("HQ", editado.nombrePersonalizadoCategoria)
    }

    @Test
    fun testFusionarLugaresActualizaHistoricoDeVisitas() {
        val lugarA = Place(id = "lugar-a", nombre = "Casa Vieja", latitud = 40.0, longitud = -3.0)
        val lugarB = Place(id = "lugar-b", nombre = "Casa Nueva", latitud = 40.0001, longitud = -3.0, isFavorite = true)

        val visitas = listOf(
            Visit(id = "v1", placeId = "lugar-a", inicio = 1000L, fin = 2000L, origen = DeviceOrigin.PHONE, latitudAncla = 40.0, longitudAncla = -3.0),
            Visit(id = "v2", placeId = "lugar-a", inicio = 3000L, fin = 4000L, origen = DeviceOrigin.PHONE, latitudAncla = 40.0, longitudAncla = -3.0),
            Visit(id = "v3", placeId = "lugar-b", inicio = 5000L, fin = 6000L, origen = DeviceOrigin.PHONE, latitudAncla = 40.0001, longitudAncla = -3.0)
        )

        val resultado = FavoritesManager.fusionarLugares(
            lugarOrigenId = "lugar-a",
            lugarDestinoId = "lugar-b",
            lugares = listOf(lugarA, lugarB),
            visitas = visitas
        )

        // El lugar origen debe haber sido eliminado de la lista
        assertEquals(1, resultado.lugaresActualizados.size)
        assertEquals("lugar-b", resultado.lugaresActualizados[0].id)

        // Las visitas asociadas al origen deben haberse reasignado al destino
        assertEquals(2, resultado.visitasReasignadasCount)
        assertTrue(resultado.visitasActualizadas.all { it.placeId == "lugar-b" })
    }
}
