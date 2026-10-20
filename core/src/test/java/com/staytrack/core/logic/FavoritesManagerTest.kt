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

    @Test
    fun testFusionarDestinoHeredaCoordenadasSiEranCero() {
        val lugarConCoords = Place(id = "origen", nombre = "Lugar sin nombre", latitud = 40.416, longitud = -3.703)
        val favoritoSinCoords = Place(id = "fav", nombre = "Trabajo", latitud = 0.0, longitud = 0.0, isFavorite = true)

        val visitas = listOf(
            Visit(id = "v1", placeId = "origen", inicio = 1000L, fin = 2000L, origen = DeviceOrigin.PHONE, latitudAncla = 40.416, longitudAncla = -3.703)
        )

        val resultado = FavoritesManager.fusionarLugares(
            lugarOrigenId = "origen",
            lugarDestinoId = "fav",
            lugares = listOf(lugarConCoords, favoritoSinCoords),
            visitas = visitas
        )

        assertEquals(1, resultado.lugaresActualizados.size)
        val favFinal = resultado.lugaresActualizados[0]
        assertEquals("fav", favFinal.id)
        assertEquals(40.416, favFinal.latitud, 0.001)
        assertEquals(-3.703, favFinal.longitud, 0.001)
        assertEquals("fav", resultado.visitasActualizadas[0].placeId)
    }

    @Test
    fun testConsolidarLugaresCercanosAgrupaMultiplesLugares() {
        val baseLat = 40.416775
        val baseLon = -3.703790

        val favTrabajo = Place(
            id = "fav-trabajo",
            nombre = "Trabajo",
            latitud = baseLat,
            longitud = baseLon,
            isFavorite = true,
            radio = 100.0
        )

        // 3 lugares sin nombre creados en días distintos dentro de un radio de 80m de trabajo
        val lugar1 = Place(id = "p1", nombre = null, latitud = baseLat + 0.0003, longitud = baseLon, radio = 75.0) // ~33m
        val lugar2 = Place(id = "p2", nombre = null, latitud = baseLat + 0.0006, longitud = baseLon, radio = 75.0) // ~66m
        val lugar3 = Place(id = "p3", nombre = null, latitud = baseLat + 0.0008, longitud = baseLon, radio = 75.0) // ~88m

        // Otro lugar lejano (a 5 km) que no debe fusionarse
        val lugarLejano = Place(id = "p-lejos", nombre = "Gimnasio", latitud = baseLat + 0.05, longitud = baseLon)

        val visitas = listOf(
            Visit(id = "v1", placeId = "p1", inicio = 1000L, fin = 2000L, origen = DeviceOrigin.PHONE, latitudAncla = baseLat, longitudAncla = baseLon),
            Visit(id = "v2", placeId = "p2", inicio = 3000L, fin = 4000L, origen = DeviceOrigin.PHONE, latitudAncla = baseLat, longitudAncla = baseLon),
            Visit(id = "v3", placeId = "p3", inicio = 5000L, fin = 6000L, origen = DeviceOrigin.PHONE, latitudAncla = baseLat, longitudAncla = baseLon),
            Visit(id = "v4", placeId = "p-lejos", inicio = 7000L, fin = 8000L, origen = DeviceOrigin.PHONE, latitudAncla = baseLat + 0.05, longitudAncla = baseLon)
        )

        val resultado = FavoritesManager.consolidarLugaresCercanos(
            lugares = listOf(favTrabajo, lugar1, lugar2, lugar3, lugarLejano),
            visitas = visitas,
            umbralMetros = 120.0
        )

        // Los 3 lugares cercanos se fusionaron en favTrabajo, lugarLejano se mantiene
        assertEquals(2, resultado.lugaresActualizados.size)
        val idsRestantes = resultado.lugaresActualizados.map { it.id }.toSet()
        assertTrue(idsRestantes.contains("fav-trabajo"))
        assertTrue(idsRestantes.contains("p-lejos"))

        // Las visitas de p1, p2, p3 ahora pertenecen a fav-trabajo
        val visitasTrabajo = resultado.visitasActualizadas.filter { it.placeId == "fav-trabajo" }
        assertEquals(3, visitasTrabajo.size)

        val visitaLejana = resultado.visitasActualizadas.find { it.id == "v4" }
        assertEquals("p-lejos", visitaLejana?.placeId)
    }
}

