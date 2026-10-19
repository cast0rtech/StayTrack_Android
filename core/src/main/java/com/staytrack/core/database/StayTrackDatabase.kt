package com.staytrack.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.staytrack.core.model.MovementSample
import com.staytrack.core.model.Place
import com.staytrack.core.model.Visit

@Database(
    entities = [
        Place::class,
        Visit::class,
        MovementSample::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StayTrackDatabase : RoomDatabase() {

    abstract fun placeDao(): PlaceDao
    abstract fun visitDao(): VisitDao
    abstract fun movementSampleDao(): MovementSampleDao

    companion object {
        @Volatile
        private var INSTANCE: StayTrackDatabase? = null

        fun getInstance(context: Context): StayTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StayTrackDatabase::class.java,
                    "staytrack.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
