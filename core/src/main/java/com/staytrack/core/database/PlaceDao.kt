package com.staytrack.core.database

import androidx.room.*
import com.staytrack.core.model.Place
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {

    @Query("SELECT * FROM places ORDER BY isFavorite DESC, nombre ASC")
    fun getAllPlacesFlow(): Flow<List<Place>>

    @Query("SELECT * FROM places ORDER BY isFavorite DESC, nombre ASC")
    suspend fun getAllPlaces(): List<Place>

    @Query("SELECT * FROM places WHERE isFavorite = 1 ORDER BY nombre ASC")
    fun getFavoritePlacesFlow(): Flow<List<Place>>

    @Query("SELECT * FROM places WHERE id = :id LIMIT 1")
    suspend fun getPlaceById(id: String): Place?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlace(place: Place)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaces(places: List<Place>)

    @Update
    suspend fun updatePlace(place: Place)

    @Delete
    suspend fun deletePlace(place: Place)

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deletePlaceById(id: String)
}
