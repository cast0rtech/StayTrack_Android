package com.staytrack.mobile.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staytrack.core.export.CsvExporter
import com.staytrack.core.export.MapImageGenerator
import com.staytrack.core.export.XlsxExporter
import com.staytrack.core.logic.AnalyticsAndExportUtils
import com.staytrack.core.logic.FavoritesManager
import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import com.staytrack.mobile.StayTrackMobileApplication
import com.staytrack.mobile.service.MobileTrackingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel (.xlsx)"),
    MAP_IMAGE("png", "image/png", "Mapa Táctico (.png)"),
    GPX("gpx", "application/gpx+xml", "GPS Waypoints (.gpx)"),
    KML("kml", "application/vnd.google-earth.kml+xml", "Google Earth (.kml)"),
    CSV("csv", "text/csv", "CSV")
}

data class DashboardStats(
    val estanciasHoy: Int = 0,
    val tiempoEstanciaHoyMillis: Long = 0L,
    val lugaresTotal: Int = 0,
    val categoriaPrincipal: PlaceCategory = PlaceCategory.CASA,
    val tiempoCategoriaPrincipalMillis: Long = 0L
)

class MobileMainViewModel : ViewModel() {

    private val db = StayTrackMobileApplication.instance.database

    val serviceState = MobileTrackingService.serviceState

    val allPlaces: StateFlow<List<Place>> = db.placeDao().getAllPlacesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVisits: StateFlow<List<Visit>> = db.visitDao().getAllVisitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exportingState = MutableStateFlow<String?>(null)
    val exportingState: StateFlow<String?> = _exportingState.asStateFlow()

    val dashboardStats: StateFlow<DashboardStats> = combine(allVisits, allPlaces) { visits, places ->
        computeStats(visits, places)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    fun toggleTracking(context: Context) {
        if (serviceState.value.isRunning) {
            MobileTrackingService.stop(context)
        } else {
            MobileTrackingService.start(context)
        }
    }

    fun guardarLugar(
        placeId: String,
        nombre: String?,
        categoria: PlaceCategory,
        radio: Double,
        isFavorite: Boolean,
        nombrePersonalizadoCategoria: String? = null
    ) {
        viewModelScope.launch {
            val existente = db.placeDao().getPlaceById(placeId)
            val updated = if (existente != null) {
                existente.copy(
                    nombre = nombre,
                    categoria = categoria,
                    radio = radio,
                    isFavorite = isFavorite,
                    nombrePersonalizadoCategoria = nombrePersonalizadoCategoria
                )
            } else {
                Place(
                    id = placeId,
                    nombre = nombre,
                    latitud = 0.0,
                    longitud = 0.0,
                    categoria = categoria,
                    radio = radio,
                    isFavorite = isFavorite,
                    nombrePersonalizadoCategoria = nombrePersonalizadoCategoria
                )
            }
            db.placeDao().upsertPlace(updated)
        }
    }

    fun crearFavorito(
        nombre: String,
        latitud: Double,
        longitud: Double,
        categoria: PlaceCategory,
        radio: Double = 75.0
    ) {
        viewModelScope.launch {
            val nuevo = FavoritesManager.crearFavorito(
                nombre = nombre,
                latitud = latitud,
                longitud = longitud,
                categoria = categoria,
                radio = radio
            )
            db.placeDao().upsertPlace(nuevo)
        }
    }

    fun fusionarLugares(origenId: String, destinoId: String) {
        viewModelScope.launch {
            val lugares = db.placeDao().getAllPlaces()
            val visitas = db.visitDao().getAllVisits()

            val resultado = FavoritesManager.fusionarLugares(origenId, destinoId, lugares, visitas)

            db.placeDao().deletePlaceById(origenId)
            db.visitDao().upsertVisits(resultado.visitasActualizadas)
        }
    }

    fun eliminarLugar(placeId: String) {
        viewModelScope.launch {
            db.placeDao().deletePlaceById(placeId)
        }
    }

    fun eliminarVisita(visitId: String) {
        viewModelScope.launch {
            db.visitDao().deleteVisitById(visitId)
        }
    }

    fun exportar(formato: ExportFormat, context: Context) {
        viewModelScope.launch {
            _exportingState.value = "Generando ${formato.displayName}..."
            try {
                val visitas = allVisits.value
                val lugares = allPlaces.value

                val file = withContext(Dispatchers.IO) {
                    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val outFile = File(exportDir, "staytrack_$timestamp.${formato.extension}")

                    FileOutputStream(outFile).use { out ->
                        when (formato) {
                            ExportFormat.CSV -> {
                                val csv = CsvExporter.exportVisits(visitas, lugares)
                                out.write(csv.toByteArray(Charsets.UTF_8))
                            }
                            ExportFormat.XLSX -> {
                                XlsxExporter.exportToStream(visitas, lugares, out)
                            }
                            ExportFormat.MAP_IMAGE -> {
                                MapImageGenerator.exportToStream(
                                    lugares = lugares,
                                    visitas = visitas,
                                    outputStream = out,
                                    format = Bitmap.CompressFormat.PNG
                                )
                            }
                            ExportFormat.GPX -> {
                                val waypoints = AnalyticsAndExportUtils.generarWaypointsParaExportacion(lugares, visitas)
                                val gpx = AnalyticsAndExportUtils.generarXmlGpx(waypoints)
                                out.write(gpx.toByteArray(Charsets.UTF_8))
                            }
                            ExportFormat.KML -> {
                                val waypoints = AnalyticsAndExportUtils.generarWaypointsParaExportacion(lugares, visitas)
                                val kml = AnalyticsAndExportUtils.generarXmlKml(waypoints)
                                out.write(kml.toByteArray(Charsets.UTF_8))
                            }
                        }
                    }
                    outFile
                }

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = formato.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "StayTrack Export: ${formato.displayName}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Compartir ${formato.displayName}").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                _exportingState.value = null
            } catch (e: Exception) {
                _exportingState.value = "Error: ${e.message}"
            }
        }
    }

    private fun computeStats(visitas: List<Visit>, lugares: List<Place>): DashboardStats {
        val inicioHoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val finHoy = inicioHoy + 24 * 3600_000L

        val agg = AnalyticsAndExportUtils.agregarTiempo(visitas, lugares, inicioHoy, finHoy)
        val catPrincipal = agg.tiempoPorCategoria.maxByOrNull { it.value }

        return DashboardStats(
            estanciasHoy = agg.visitasContadas,
            tiempoEstanciaHoyMillis = agg.tiempoTotalMillis,
            lugaresTotal = lugares.size,
            categoriaPrincipal = catPrincipal?.key ?: PlaceCategory.CASA,
            tiempoCategoriaPrincipalMillis = catPrincipal?.value ?: 0L
        )
    }
}
