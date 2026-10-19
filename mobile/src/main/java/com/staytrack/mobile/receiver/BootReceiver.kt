package com.staytrack.mobile.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.staytrack.mobile.service.MobileTrackingService
import com.staytrack.mobile.service.TrackingPreferences

/**
 * Reanuda automáticamente el servicio de monitoreo en segundo plano al encender el dispositivo
 * o al actualizarse la aplicación, siempre que el usuario no lo haya detenido manualmente.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val hasLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocation && TrackingPreferences.isTrackingEnabled(context)) {
                MobileTrackingService.start(context)
            }
        }
    }
}
