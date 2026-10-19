package com.staytrack.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val placeId: String? = null,
    val inicio: Long,
    val fin: Long? = null,
    val origen: DeviceOrigin,
    val latitudAncla: Double,
    val longitudAncla: Double,
    val precision: Float = 0f
) {
    val isActiva: Boolean get() = fin == null

    val duracionMillis: Long
        get() = (fin ?: System.currentTimeMillis()) - inicio
}
