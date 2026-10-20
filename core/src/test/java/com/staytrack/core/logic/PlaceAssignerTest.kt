package com.staytrack.core.logic

import com.staytrack.core.model.DeviceOrigin
import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import org.junit.Assert.*
import org.junit.Test

class PlaceAssignerTest {

    private val latBase = 40.416775
    private val lonBase = -3.703790

    @Test
    fun testDentroDelRadioDeUnFavorito() {
        val lugarComun = Place(
            id = "comun-1",
            nombre = "Plaza Mayor",
            latitud = latBase + 0.0001, // ~11m
            longitud = lonBase,
            radio = 75.0,
            isFavorite = false,
            categoria = PlaceCategory.OTRO
        )

        val lugarFavorito = Place(
            id = "fav-trabajo",
            nombre = "Oficina Central",
            latitud = latBase + 0.0003, // ~33m (ligeramente más lejos pero favorito)
            longitud = lonBase,
            radio = 75.0,
            isFavorite = true,
            categoria = PlaceCategory.TRABAJO
        )

        val resultado = PlaceAssigner.asignarLugar(
            latitud = latBase,
            longitud = lonBase,
            lugaresExistentes = listOf(lugarComun, lugarFavorito)
        )

        assertFalse(resultado.esNuevo)
        assertEquals("fav-trabajo", resultado.lugar.id)
        assertEquals("Oficina Central", resultado.lugar.nombre)
        assertTrue(resultado.lugar.isFavorite)
    }

    @Test
    fun testLugarNuevoSiNoEncaja() {
        val lugarLejano = Place(
            id = "lejano-1",
            nombre = "Aeropuerto",
            latitud = latBase + 0.05, // ~5 km
            longitud = lonBase,
            radio = 75.0,
            isFavorite = true,
            categoria = PlaceCategory.OTRO
        )

        val resultado = PlaceAssigner.asignarLugar(
            latitud = latBase,
            longitud = lonBase,
            lugaresExistentes = listOf(lugarLejano)
        )

        assertTrue(resultado.esNuevo)
        assertNull(resultado.lugar.nombre)
        assertEquals(PlaceCategory.OTRO, resultado.lugar.categoria)
        assertEquals(latBase, resultado.lugar.latitud, 0.00001)
        assertEquals(lonBase, resultado.lugar.longitud, 0.00001)
        assertEquals(75.0, resultado.lugar.radio, 0.001)
    }

    @Test
    fun testDosLugaresCercanosEligeElMasCercano() {
        val lugar1 = Place(
            id = "lugar-cercano",
            nombre = "Tienda A",
            latitud = latBase + 0.00015, // ~16m
            longitud = lonBase,
            radio = 75.0,
            isFavorite = false
        )

        val lugar2 = Place(
            id = "lugar-menos-cercano",
            nombre = "Tienda B",
            latitud = latBase + 0.00045, // ~50m
            longitud = lonBase,
            radio = 75.0,
            isFavorite = false
        )

        val resultado = PlaceAssigner.asignarLugar(
            latitud = latBase,
            longitud = lonBase,
            lugaresExistentes = listOf(lugar2, lugar1)
        )

        assertFalse(resultado.esNuevo)
        assertEquals("lugar-cercano", resultado.lugar.id)
    }

    @Test
    fun testAsignarVisitaEnlazaPlaceId() {
        val favorito = Place(
            id = "casa-id",
            nombre = "Mi Casa",
            latitud = latBase,
            longitud = lonBase,
            radio = 75.0,
            isFavorite = true,
            categoria = PlaceCategory.CASA
        )

        val visita = Visit(
            id = "visita-1",
            placeId = null,
            inicio = 1000L,
            fin = 5000L,
            origen = DeviceOrigin.WATCH,
            latitudAncla = latBase,
            longitudAncla = lonBase
        )

        val (visitaAsignada, lugar) = PlaceAssigner.asignarVisita(visita, listOf(favorito))

        assertEquals("casa-id", visitaAsignada.placeId)
        assertEquals("Mi Casa", lugar.nombre)
    }

    @Test
    fun testFavoritoConToleranciaExtendida() {
        // Un favorito con radio 100m debería absorber estancias a 130m (p.ej. edificio grande o parking)
        val trabajo = Place(
            id = "fav-trabajo",
            nombre = "Oficina",
            latitud = latBase,
            longitud = lonBase,
            radio = 100.0,
            isFavorite = true,
            categoria = PlaceCategory.TRABAJO
        )

        // Distancia ~120m (0.00108 grados latitud ~ 120m)
        val latDesviada = latBase + 0.00108

        val resultado = PlaceAssigner.asignarLugar(
            latitud = latDesviada,
            longitud = lonBase,
            lugaresExistentes = listOf(trabajo)
        )

        assertFalse("El favorito debe capturar la estancia en su zona extendida de atracción", resultado.esNuevo)
        assertEquals("fav-trabajo", resultado.lugar.id)
    }

    @Test
    fun testAgrupacionPorProximidadEvitaLugaresRedundantes() {
        // Si ya existe un lugar común con radio 75m, y se detecta una estancia a 95m (desviación GPS de 20m),
        // no debe crearse un lugar nuevo redundante, sino acumularse en el existente
        val lugarExistente = Place(
            id = "lugar-existente",
            nombre = null,
            latitud = latBase,
            longitud = lonBase,
            radio = 75.0,
            isFavorite = false
        )

        // Distancia ~95m (0.00085 grados latitud ~ 95m)
        val latDesviada = latBase + 0.00085

        val resultado = PlaceAssigner.asignarLugar(
            latitud = latDesviada,
            longitud = lonBase,
            lugaresExistentes = listOf(lugarExistente)
        )

        assertFalse("Debe acumularse en el lugar existente por tolerancia de proximidad en vez de crear otro", resultado.esNuevo)
        assertEquals("lugar-existente", resultado.lugar.id)
    }

    @Test
    fun testIgnoraCoordenadasNoInicializadas() {
        val lugarSinCoord = Place(
            id = "invalido",
            nombre = "Sin GPS",
            latitud = 0.0,
            longitud = 0.0,
            radio = 75.0,
            isFavorite = true
        )

        val resultado = PlaceAssigner.asignarLugar(
            latitud = latBase,
            longitud = lonBase,
            lugaresExistentes = listOf(lugarSinCoord)
        )

        // No debe emparejar contra 0.0, 0.0
        assertTrue(resultado.esNuevo)
        assertNotEquals("invalido", resultado.lugar.id)
    }
}

