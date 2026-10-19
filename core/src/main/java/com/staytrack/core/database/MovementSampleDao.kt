package com.staytrack.core.database

import androidx.room.*
import com.staytrack.core.model.DeviceOrigin
import com.staytrack.core.model.MovementSample
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementSampleDao {

    @Query("SELECT * FROM movement_samples ORDER BY inicio DESC")
    fun getAllMovementSamplesFlow(): Flow<List<MovementSample>>

    @Query("SELECT * FROM movement_samples WHERE dispositivo = :dispositivo AND inicio >= :desde ORDER BY inicio ASC")
    suspend fun getSamplesForDeviceSince(dispositivo: DeviceOrigin, desde: Long): List<MovementSample>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: MovementSample)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<MovementSample>)

    @Query("DELETE FROM movement_samples WHERE fin < :olderThan")
    suspend fun deleteSamplesOlderThan(olderThan: Long)
}
