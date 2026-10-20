package com.staytrack.mobile.service

import android.content.Context

enum class MapProvider(val id: String, val displayName: String, val requiresApiKey: Boolean) {
    DARK_TACTICAL("dark_tactical", "Oscuro Táctico (Libre, Sin Clave)", false),
    SATELLITE("satellite", "Satélite Real (ESRI, Libre)", false),
    OPEN_STREET_MAP("osm", "OpenStreetMap Estándar", false),
    CARTO_CUSTOM("carto_custom", "CartoDB (Con Clave API)", true)
}

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
    private const val KEY_MAP_PROVIDER = "map_provider"
    private const val KEY_MAP_API_KEY = "map_api_key"

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

    fun getMapProvider(context: Context): MapProvider {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_MAP_PROVIDER, MapProvider.DARK_TACTICAL.id)
        return MapProvider.entries.find { it.id == id } ?: MapProvider.DARK_TACTICAL
    }

    fun setMapProvider(context: Context, provider: MapProvider) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MAP_PROVIDER, provider.id)
            .apply()
    }

    fun getMapApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MAP_API_KEY, "") ?: ""
    }

    fun setMapApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MAP_API_KEY, key.trim())
            .apply()
    }
}
