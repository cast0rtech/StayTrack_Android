package com.staytrack.mobile.service

import android.content.Context

/**
 * Persiste las preferencias de rastreo continuo y estado de primera apertura.
 * Por defecto, tracking_enabled es TRUE para que el monitoreo en segundo plano
 * se active automáticamente tras la primera apertura y se mantenga siempre activo,
 * salvo que el usuario lo detenga explícitamente.
 */
object TrackingPreferences {
    private const val PREFS_NAME = "staytrack_prefs"
    private const val KEY_TRACKING_ENABLED = "tracking_enabled"
    private const val KEY_FIRST_OPEN_DONE = "first_open_done"

    fun isTrackingEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TRACKING_ENABLED, true)
    }

    fun setTrackingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TRACKING_ENABLED, enabled)
            .apply()
    }

    fun isFirstOpen(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_FIRST_OPEN_DONE, false)
    }

    fun markFirstOpenDone(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_OPEN_DONE, true)
            .apply()
    }
}
