package com.staytrack.core.model

enum class PlaceCategory(val displayName: String, val defaultIcon: String) {
    CASA("Casa", "home"),
    TRABAJO("Trabajo", "work"),
    ENTRENAMIENTO("Entrenamiento", "fitness_center"),
    MEDICO("Médico", "local_hospital"),
    OTRO("Otro", "place")
}
