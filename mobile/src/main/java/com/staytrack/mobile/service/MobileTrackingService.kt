package com.staytrack.mobile.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.staytrack.core.logic.PlaceAssigner
import com.staytrack.core.logic.StayDetectionResult
import com.staytrack.core.logic.StayDetector
import com.staytrack.core.model.DeviceOrigin
import com.staytrack.core.model.LocationReading
import com.staytrack.core.model.Place
import com.staytrack.core.model.Visit
import com.staytrack.mobile.StayTrackMobileApplication
import com.staytrack.mobile.presentation.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackingState(
    val isRunning: Boolean = false,
    val isDwelling: Boolean = false,
    val activeVisit: Visit? = null,
    val activePlace: Place? = null,
    val stayDurationMinutes: Long = 0L,
    val lastLatitude: Double = 0.0,
    val lastLongitude: Double = 0.0,
    val lastAccuracy: Float = 0.0f
)

class MobileTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val stayDetector = StayDetector(radioMetros = 75.0, minimaDuracionEstanciaMillis = 120_000L)

    private val app by lazy { StayTrackMobileApplication.instance }
    private var durationTickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_TRACKING -> {
                TrackingPreferences.setTrackingEnabled(this, false)
                stopTracking()
                stopSelf()
            }
            else -> {
                TrackingPreferences.setTrackingEnabled(this, true)
                startTrackingForeground()
            }
        }
        return START_STICKY
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                onNewLocation(loc)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTrackingForeground() {
        val notification = buildNotification("StayTrack Activo", "Registrando estancias de forma autónoma")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            30_000L
        ).apply {
            setMinUpdateIntervalMillis(15_000L)
            setMinUpdateDistanceMeters(10.0f)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            _serviceState.value = _serviceState.value.copy(isRunning = true)
            startDurationTicker()
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun onNewLocation(loc: Location) {
        val reading = LocationReading(
            latitud = loc.latitude,
            longitud = loc.longitude,
            precision = loc.accuracy,
            timestamp = loc.time,
            origen = DeviceOrigin.PHONE
        )

        serviceScope.launch {
            val result = stayDetector.procesarLectura(reading)
            handleDetectionResult(result)

            _serviceState.value = _serviceState.value.copy(
                lastLatitude = reading.latitud,
                lastLongitude = reading.longitud,
                lastAccuracy = reading.precision
            )
        }
    }

    private suspend fun handleDetectionResult(result: StayDetectionResult) {
        when (result) {
            is StayDetectionResult.EstanciaIniciada -> {
                val existingPlaces = app.database.placeDao().getAllPlaces()
                val (visitaAsignada, lugar) = PlaceAssigner.asignarVisita(result.visit, existingPlaces)

                // Persistir lugar y visita en Room
                app.database.placeDao().upsertPlace(lugar)
                app.database.visitDao().upsertVisit(visitaAsignada)

                _serviceState.value = _serviceState.value.copy(
                    isDwelling = true,
                    activeVisit = visitaAsignada,
                    activePlace = lugar,
                    stayDurationMinutes = 2L
                )

                val nombre = lugar.nombre ?: lugar.categoria.displayName
                updateNotification("En estancia: $nombre (2 min)")
            }

            is StayDetectionResult.EstanciaActualizada -> {
                val currentVisit = result.visit
                val durationMin = currentVisit.duracionMillis / 60000
                _serviceState.value = _serviceState.value.copy(
                    activeVisit = currentVisit,
                    stayDurationMinutes = durationMin
                )
                val placeName = _serviceState.value.activePlace?.nombre ?: "Estancia"
                updateNotification("En estancia: $placeName (${formatDuration(durationMin)})")
            }

            is StayDetectionResult.EstanciaFinalizada -> {
                app.database.visitDao().upsertVisit(result.visit)

                _serviceState.value = _serviceState.value.copy(
                    isDwelling = false,
                    activeVisit = null,
                    activePlace = null,
                    stayDurationMinutes = 0L
                )

                updateNotification("En movimiento • Buscando estancias")
            }

            else -> {}
        }
    }

    private fun formatDuration(mins: Long): String {
        val h = mins / 60
        val m = mins % 60
        return if (h > 0) "${h}h ${m}m" else "$m min"
    }

    private fun startDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = serviceScope.launch {
            while (isActive) {
                delay(60_000L)
                val current = _serviceState.value.activeVisit
                if (current != null) {
                    val durationMin = current.duracionMillis / 60000
                    _serviceState.value = _serviceState.value.copy(
                        stayDurationMinutes = durationMin
                    )
                    val placeName = _serviceState.value.activePlace?.nombre ?: "Estancia"
                    updateNotification("En estancia: $placeName (${formatDuration(durationMin)})")
                }
            }
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification("StayTrack", text))
    }

    private fun buildNotification(title: String, text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MobileTrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, StayTrackMobileApplication.CHANNEL_ID_TRACKING)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText("Monitoreo continuo en segundo plano")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pausar", stopPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun stopTracking() {
        durationTickerJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.launch {
            val finalizada = stayDetector.forzarCierre()
            if (finalizada != null) {
                app.database.visitDao().upsertVisit(finalizada)
            }
        }
        _serviceState.value = TrackingState(isRunning = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopTracking()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_TRACKING = "com.staytrack.mobile.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.staytrack.mobile.STOP_TRACKING"
        private const val NOTIFICATION_ID = 1001

        private val _serviceState = MutableStateFlow(TrackingState())
        val serviceState: StateFlow<TrackingState> = _serviceState.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, MobileTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MobileTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
}
