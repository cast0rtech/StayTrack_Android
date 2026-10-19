package com.staytrack.core.database

import androidx.room.TypeConverter
import com.staytrack.core.model.DeviceOrigin
import com.staytrack.core.model.PlaceCategory

class Converters {

    @TypeConverter
    fun fromPlaceCategory(value: PlaceCategory): String = value.name

    @TypeConverter
    fun toPlaceCategory(value: String): PlaceCategory = try {
        PlaceCategory.valueOf(value)
    } catch (e: Exception) {
        PlaceCategory.OTRO
    }

    @TypeConverter
    fun fromDeviceOrigin(value: DeviceOrigin): String = value.name

    @TypeConverter
    fun toDeviceOrigin(value: String): DeviceOrigin = try {
        DeviceOrigin.valueOf(value)
    } catch (e: Exception) {
        DeviceOrigin.PHONE
    }
}
