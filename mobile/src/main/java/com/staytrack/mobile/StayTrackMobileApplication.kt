package com.staytrack.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.staytrack.core.database.StayTrackDatabase

class StayTrackMobileApplication : Application() {

    lateinit var database: StayTrackDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = StayTrackDatabase.getInstance(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                "StayTrack Monitoreo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoreo inteligente de estancias y geolocalización"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_TRACKING = "staytrack_mobile_tracking_channel"
        lateinit var instance: StayTrackMobileApplication
            private set
    }
}
