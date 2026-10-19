package com.staytrack.core.database

import androidx.room.*
import com.staytrack.core.model.Visit
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Query("SELECT * FROM visits ORDER BY inicio DESC")
    fun getAllVisitsFlow(): Flow<List<Visit>>

    @Query("SELECT * FROM visits ORDER BY inicio DESC")
    suspend fun getAllVisits(): List<Visit>

    @Query("SELECT * FROM visits WHERE fin IS NULL ORDER BY inicio DESC LIMIT 1")
    fun getActiveVisitFlow(): Flow<Visit?>

    @Query("SELECT * FROM visits WHERE fin IS NULL ORDER BY inicio DESC LIMIT 1")
    suspend fun getActiveVisit(): Visit?

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    suspend fun getVisitById(id: String): Visit?

    @Query("SELECT * FROM visits WHERE inicio >= :desde AND inicio <= :hasta ORDER BY inicio DESC")
    suspend fun getVisitsBetween(desde: Long, hasta: Long): List<Visit>

    @Query("SELECT * FROM visits WHERE placeId = :placeId ORDER BY inicio DESC")
    suspend fun getVisitsForPlace(placeId: String): List<Visit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVisit(visit: Visit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVisits(visits: List<Visit>)

    @Update
    suspend fun updateVisit(visit: Visit)

    @Delete
    suspend fun deleteVisit(visit: Visit)

    @Query("DELETE FROM visits WHERE id = :id")
    suspend fun deleteVisitById(id: String)
}
