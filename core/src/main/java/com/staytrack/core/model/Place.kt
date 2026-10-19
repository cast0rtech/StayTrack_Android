package com.staytrack.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "places")
data class Place(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nombre: String? = null,
    val latitud: Double,
    val longitud: Double,
    val radio: Double = 75.0,
    val isFavorite: Boolean = false,
    val categoria: PlaceCategory = PlaceCategory.OTRO,
    val nombrePersonalizadoCategoria: String? = null,
    val icono: String = categoria.defaultIcon,
    val fechaCreacion: Long = System.currentTimeMillis()
)
