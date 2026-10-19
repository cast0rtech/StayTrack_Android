package com.staytrack.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "movement_samples")
data class MovementSample(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val dispositivo: DeviceOrigin,
    val inicio: Long,
    val fin: Long
)
