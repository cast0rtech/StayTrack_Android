package com.staytrack.core.model

data class LocationReading(
    val latitud: Double,
    val longitud: Double,
    val precision: Float,
    val timestamp: Long,
    val origen: DeviceOrigin = DeviceOrigin.PHONE
)
