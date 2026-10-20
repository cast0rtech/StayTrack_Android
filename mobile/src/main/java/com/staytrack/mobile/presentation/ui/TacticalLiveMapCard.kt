package com.staytrack.mobile.presentation.ui

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import com.staytrack.mobile.presentation.theme.*
import com.staytrack.mobile.service.MapProvider
import com.staytrack.mobile.service.TrackingPreferences
import com.staytrack.mobile.service.TrackingState
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun TacticalLiveMapCard(
    serviceState: TrackingState,
    places: List<Place>,
    modifier: Modifier = Modifier,
    trajectoryVisits: List<Visit> = emptyList(),
    selectedLocation: Pair<Double, Double>? = null,
    titleOverlay: String? = null
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showMapSettings by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var currentMapProvider by remember { mutableStateOf(TrackingPreferences.getMapProvider(context)) }
    var currentApiKey by remember { mutableStateOf(TrackingPreferences.getMapApiKey(context)) }

    val hasValidLocation = (serviceState.lastLatitude != 0.0 || serviceState.lastLongitude != 0.0) ||
            (selectedLocation != null && selectedLocation.first != 0.0) ||
            trajectoryVisits.isNotEmpty()

    val initialLat = selectedLocation?.first
        ?: (if (serviceState.lastLatitude != 0.0) serviceState.lastLatitude else trajectoryVisits.firstOrNull()?.latitudAncla ?: 0.0)
    val initialLon = selectedLocation?.second
        ?: (if (serviceState.lastLongitude != 0.0) serviceState.lastLongitude else trajectoryVisits.firstOrNull()?.longitudAncla ?: 0.0)

    val mapHeight by animateDpAsState(
        targetValue = if (isExpanded) 360.dp else 220.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mapHeight"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardGlowBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
        ) {
            if (hasValidLocation) {
                // Interactive Leaflet Map View
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                            setBackgroundColor(AndroidColor.parseColor("#080B11"))

                            // Evitar que el scroll del LazyColumn intercepte los toques en el mapa
                            setOnTouchListener { v, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                        v.parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        v.parent?.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                                false
                            }

                            val placesJson = formatPlacesJson(places)
                            val trajectoryJson = formatTrajectoryJson(trajectoryVisits)
                            val anchorLat = serviceState.activeVisit?.latitudAncla ?: initialLat
                            val anchorLon = serviceState.activeVisit?.longitudAncla ?: initialLon
                            val radius = serviceState.activePlace?.radio ?: 75.0

                            val html = buildLeafletHtml(
                                lat = initialLat,
                                lon = initialLon,
                                accuracy = serviceState.lastAccuracy,
                                isDwelling = serviceState.isDwelling,
                                anchorLat = anchorLat,
                                anchorLon = anchorLon,
                                radius = radius,
                                placesJson = placesJson,
                                trajectoryJson = trajectoryJson,
                                provider = currentMapProvider,
                                apiKey = currentApiKey
                            )
                            loadDataWithBaseURL("https://staytrack.app", html, "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    update = {
                        webViewRef = it
                    }
                )

                // Actualizar ubicación en tiempo real cuando cambian las coordenadas
                LaunchedEffect(serviceState.lastLatitude, serviceState.lastLongitude, serviceState.isDwelling) {
                    if (serviceState.lastLatitude != 0.0 || serviceState.lastLongitude != 0.0) {
                        val anchorLat = serviceState.activeVisit?.latitudAncla ?: serviceState.lastLatitude
                        val anchorLon = serviceState.activeVisit?.longitudAncla ?: serviceState.lastLongitude
                        val radius = serviceState.activePlace?.radio ?: 75.0
                        val js = "if (window.updateUserLocation) { window.updateUserLocation(${serviceState.lastLatitude}, ${serviceState.lastLongitude}, ${serviceState.lastAccuracy}, ${serviceState.isDwelling}, $anchorLat, $anchorLon, $radius); }"
                        webViewRef?.evaluateJavascript(js, null)
                    }
                }

                // Centrar en ubicación seleccionada (por ejemplo al tocar una estancia en el historial)
                LaunchedEffect(selectedLocation) {
                    selectedLocation?.let { (lat, lon) ->
                        if (lat != 0.0 || lon != 0.0) {
                            val js = "if (window.focusLocation) { window.focusLocation($lat, $lon, 16); }"
                            webViewRef?.evaluateJavascript(js, null)
                        }
                    }
                }

                // Actualizar trayectoria
                LaunchedEffect(trajectoryVisits) {
                    val trajJson = formatTrajectoryJson(trajectoryVisits)
                    val js = "if (window.setTrajectory) { window.setTrajectory($trajJson); }"
                    webViewRef?.evaluateJavascript(js, null)
                }

                // Actualizar lugares cuando cambie la lista
                LaunchedEffect(places) {
                    val placesJson = formatPlacesJson(places)
                    val js = "if (window.setPlaces) { window.setPlaces($placesJson); }"
                    webViewRef?.evaluateJavascript(js, null)
                }

                // Overlay HUD Superior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge Estado / Título
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xDD0F172A))
                            .border(1.dp, CardGlowBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (serviceState.isDwelling) NeonCyan else if (serviceState.isRunning) NeonPurple else CrimsonRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val statusText = titleOverlay ?: if (serviceState.isDwelling) {
                            "ESTANCIA: ${serviceState.activePlace?.nombre ?: "Otro"}"
                        } else if (serviceState.isRunning) {
                            "EN MOVIMIENTO"
                        } else {
                            "PAUSADO"
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    // Badge Proveedor de Mapa
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (serviceState.lastAccuracy > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xCC0F172A))
                                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "±${serviceState.lastAccuracy.toInt()}m",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = { showMapSettings = true },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC0F172A))
                                .border(1.dp, DarkSurfaceBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Cambiar capa de mapa / API Key",
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Overlay HUD Inferior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coordenadas GPS
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC0F172A))
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val displayLat = if (serviceState.lastLatitude != 0.0) serviceState.lastLatitude else initialLat
                        val displayLon = if (serviceState.lastLongitude != 0.0) serviceState.lastLongitude else initialLon
                        Text(
                            text = "${String.format(Locale.US, "%.4f", displayLat)}, ${String.format(Locale.US, "%.4f", displayLon)}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }

                    // Controles flotantes (Recentrar y Expandir)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                webViewRef?.evaluateJavascript("if (window.recenter) { window.recenter(); }", null)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xEE0F172A))
                                .border(1.dp, CardGlowBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Recentrar mapa",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xEE0F172A))
                                .border(1.dp, CardGlowBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isExpanded) "Reducir mapa" else "Expandir mapa",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Radar animado mientras no haya coordenadas válidas aún
                GpsSearchingRadar(isRunning = serviceState.isRunning)
            }
        }
    }

    // Diálogo de Selección de Proveedor de Mapa y Clave API
    if (showMapSettings) {
        MapSettingsDialog(
            currentProvider = currentMapProvider,
            currentApiKey = currentApiKey,
            onDismiss = { showMapSettings = false },
            onSave = { newProvider, newKey ->
                currentMapProvider = newProvider
                currentApiKey = newKey
                TrackingPreferences.setMapProvider(context, newProvider)
                TrackingPreferences.setMapApiKey(context, newKey)

                // Recargar mapa con el nuevo proveedor
                val placesJson = formatPlacesJson(places)
                val trajectoryJson = formatTrajectoryJson(trajectoryVisits)
                val anchorLat = serviceState.activeVisit?.latitudAncla ?: initialLat
                val anchorLon = serviceState.activeVisit?.longitudAncla ?: initialLon
                val radius = serviceState.activePlace?.radio ?: 75.0

                val html = buildLeafletHtml(
                    lat = initialLat,
                    lon = initialLon,
                    accuracy = serviceState.lastAccuracy,
                    isDwelling = serviceState.isDwelling,
                    anchorLat = anchorLat,
                    anchorLon = anchorLon,
                    radius = radius,
                    placesJson = placesJson,
                    trajectoryJson = trajectoryJson,
                    provider = newProvider,
                    apiKey = newKey
                )
                webViewRef?.loadDataWithBaseURL("https://staytrack.app", html, "text/html", "UTF-8", null)
                showMapSettings = false
            }
        )
    }
}

@Composable
fun MapSettingsDialog(
    currentProvider: MapProvider,
    currentApiKey: String,
    onDismiss: () -> Unit,
    onSave: (MapProvider, String) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardGlowBorder, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAPA DE MAPA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }

                Text(
                    text = "Selecciona el estilo de mapa o introduce tu clave API si dispones de ella:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MapProvider.entries.forEach { provider ->
                        val isSelected = provider == selectedProvider
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProvider = provider }
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) NeonCyan else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DarkSurfaceBorder else DarkSurface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedProvider = provider },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NeonCyan,
                                        unselectedColor = TextMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = provider.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.White
                                    )
                                    val desc = when (provider) {
                                        MapProvider.DARK_TACTICAL -> "Libre, sin clave y sin marcas de agua (Recomendado)"
                                        MapProvider.SATELLITE -> "Imágenes satelitales reales de alta resolución (ESRI)"
                                        MapProvider.OPEN_STREET_MAP -> "Mapa estándar claro de OpenStreetMap"
                                        MapProvider.CARTO_CUSTOM -> "Requiere clave de cartocdn.com/basemaps/apikey"
                                    }
                                    Text(
                                        text = desc,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedProvider.requiresApiKey) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Clave API de CartoDB / Mapbox") },
                        placeholder = { Text("Pega tu api_key aquí") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(selectedProvider, apiKeyInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Aplicar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GpsSearchingRadar(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2

            // Círculos concéntricos
            for (i in 1..3) {
                drawCircle(
                    color = Color(0x2200F0FF),
                    radius = maxRadius * (i / 3f),
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // Pulso expansivo
            drawCircle(
                color = Color(0x4400F0FF).copy(alpha = (1f - radarPulse) * 0.5f),
                radius = maxRadius * radarPulse,
                center = center,
                style = Stroke(width = 2f)
            )

            // Líneas de cuadrícula
            drawLine(
                color = Color(0x2200F0FF),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0x2200F0FF),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x3300F0FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isRunning) "BUSCANDO SEÑAL GPS..." else "RASTREO PAUSADO",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isRunning) NeonCyan else TextSecondary,
                letterSpacing = 1.sp
            )
            Text(
                text = if (isRunning) "Esperando posición de satélite para mostrar el mapa" else "Inicia el monitoreo para ver el mapa táctico",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun formatPlacesJson(places: List<Place>): String {
    val items = places.filter { it.latitud != 0.0 || it.longitud != 0.0 }.map { p ->
        val color = when (p.categoria) {
            PlaceCategory.CASA -> "#00F0FF"
            PlaceCategory.TRABAJO -> "#A855F7"
            PlaceCategory.ENTRENAMIENTO -> "#10B981"
            PlaceCategory.MEDICO -> "#EF4444"
            PlaceCategory.OTRO -> "#6366F1"
        }
        val safeName = (p.nombre ?: p.categoria.displayName).replace("\"", "\\\"").replace("'", "\\'")
        """{"lat":${p.latitud},"lon":${p.longitud},"name":"$safeName","category":"${p.categoria.displayName}","radius":${p.radio},"color":"$color","isFav":${p.isFavorite}}"""
    }
    return "[${items.joinToString(",")}]"
}

private fun formatTrajectoryJson(visits: List<Visit>): String {
    val items = visits.filter { it.latitudAncla != 0.0 || it.longitudAncla != 0.0 }
        .sortedBy { it.inicio }
        .map { v ->
            """{"lat":${v.latitudAncla},"lon":${v.longitudAncla},"start":${v.inicio},"duration":${v.duracionMillis}}"""
        }
    return "[${items.joinToString(",")}]"
}

private fun buildLeafletHtml(
    lat: Double,
    lon: Double,
    accuracy: Float,
    isDwelling: Boolean,
    anchorLat: Double,
    anchorLon: Double,
    radius: Double,
    placesJson: String,
    trajectoryJson: String = "[]",
    provider: MapProvider = MapProvider.DARK_TACTICAL,
    apiKey: String = ""
): String {
    // Configurar URL de mosaicos y clases CSS según proveedor
    val tileUrl: String
    val tileOptions: String
    val tileCssFilter: String

    when (provider) {
        MapProvider.DARK_TACTICAL -> {
            tileUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
            tileOptions = "{ maxZoom: 19, className: 'tactical-dark-tile' }"
            tileCssFilter = ".tactical-dark-tile { filter: brightness(0.6) invert(1) contrast(3) hue-rotate(200deg) saturate(0.3) brightness(0.7) !important; }"
        }
        MapProvider.SATELLITE -> {
            tileUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            tileOptions = "{ maxZoom: 19 }"
            tileCssFilter = ""
        }
        MapProvider.OPEN_STREET_MAP -> {
            tileUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
            tileOptions = "{ maxZoom: 19 }"
            tileCssFilter = ""
        }
        MapProvider.CARTO_CUSTOM -> {
            tileUrl = if (apiKey.isNotBlank()) {
                "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png?api_key=$apiKey"
            } else {
                "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
            }
            tileOptions = "{ subdomains: 'abcd', maxZoom: 19 }"
            tileCssFilter = ""
        }
    }

    return """
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <style>
        html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            background-color: #080B11;
            overflow: hidden;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
        }
        #map {
            width: 100%;
            height: 100%;
            background: #080B11;
        }
        .leaflet-container {
            background-color: #080B11 !important;
        }
        $tileCssFilter
        .user-dot {
            width: 14px;
            height: 14px;
            background: #00F0FF;
            border: 2.5px solid #FFFFFF;
            border-radius: 50%;
            box-shadow: 0 0 10px #00F0FF, 0 0 20px #00F0FF;
            position: relative;
        }
        .user-pulse {
            position: absolute;
            top: -11px;
            left: -11px;
            width: 36px;
            height: 36px;
            border-radius: 50%;
            border: 2px solid #00F0FF;
            animation: pulse-ring 2s infinite cubic-bezier(0.215, 0.61, 0.355, 1);
        }
        @keyframes pulse-ring {
            0% { transform: scale(0.5); opacity: 1; }
            100% { transform: scale(1.6); opacity: 0; }
        }
        .place-marker {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 26px;
            height: 26px;
            border-radius: 50%;
            color: #000;
            font-weight: bold;
            font-size: 13px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.6);
            border: 1.5px solid rgba(255,255,255,0.8);
        }
        .trajectory-dot {
            width: 10px;
            height: 10px;
            background: #A855F7;
            border-radius: 50%;
            border: 2px solid #FFFFFF;
            box-shadow: 0 0 6px #A855F7;
        }
        .leaflet-popup-content-wrapper {
            background: #0F172A !important;
            color: #F8FAFC !important;
            border: 1px solid #00F0FF !important;
            border-radius: 10px !important;
            box-shadow: 0 4px 20px rgba(0, 240, 255, 0.25) !important;
        }
        .leaflet-popup-tip {
            background: #0F172A !important;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        var map, userMarker, accuracyCircle, stayCircle;
        var placesLayer = null;
        var trajectoryLayer = null;
        var currentLat = $lat, currentLon = $lon;

        function init() {
            try {
                map = L.map('map', {
                    center: [currentLat, currentLon],
                    zoom: 16,
                    zoomControl: false,
                    attributionControl: false
                });

                L.tileLayer('$tileUrl', $tileOptions).addTo(map);

                placesLayer = L.layerGroup().addTo(map);
                trajectoryLayer = L.layerGroup().addTo(map);

                var userIcon = L.divIcon({
                    className: '',
                    html: '<div class="user-dot"><div class="user-pulse"></div></div>',
                    iconSize: [14, 14],
                    iconAnchor: [7, 7]
                });

                userMarker = L.marker([currentLat, currentLon], { icon: userIcon, zIndexOffset: 1000 }).addTo(map);
                userMarker.bindPopup('<b>Mi posición actual</b><br><span style="color:#94A3B8;font-size:11px">Precisión: ±' + Math.round($accuracy) + 'm</span>');

                if ($accuracy && $accuracy > 0) {
                    accuracyCircle = L.circle([currentLat, currentLon], {
                        radius: $accuracy,
                        color: '#00F0FF',
                        weight: 1,
                        fillColor: '#00F0FF',
                        fillOpacity: 0.12
                    }).addTo(map);
                }

                if ($isDwelling && $anchorLat && $anchorLon) {
                    stayCircle = L.circle([$anchorLat, $anchorLon], {
                        radius: $radius,
                        color: '#A855F7',
                        weight: 2,
                        dashArray: '6, 6',
                        fillColor: '#A855F7',
                        fillOpacity: 0.2
                    }).addTo(map);
                }

                setPlaces($placesJson);
                setTrajectory($trajectoryJson);
            } catch (e) {
                console.error("Error al inicializar mapa", e);
            }
        }

        function updateUserLocation(lat, lon, accuracy, isDwelling, anchorLat, anchorLon, radius) {
            currentLat = lat;
            currentLon = lon;
            if (!map) return;

            if (userMarker) {
                userMarker.setLatLng([lat, lon]);
                userMarker.setPopupContent('<b>Mi posición actual</b><br><span style="color:#94A3B8;font-size:11px">Precisión: ±' + Math.round(accuracy) + 'm</span>');
            }

            if (accuracy && accuracy > 0) {
                if (accuracyCircle) {
                    accuracyCircle.setLatLng([lat, lon]);
                    accuracyCircle.setRadius(accuracy);
                } else {
                    accuracyCircle = L.circle([lat, lon], {
                        radius: accuracy,
                        color: '#00F0FF',
                        weight: 1,
                        fillColor: '#00F0FF',
                        fillOpacity: 0.12
                    }).addTo(map);
                }
            }

            if (isDwelling && anchorLat && anchorLon) {
                var rad = radius || 75;
                if (stayCircle) {
                    stayCircle.setLatLng([anchorLat, anchorLon]);
                    stayCircle.setRadius(rad);
                } else {
                    stayCircle = L.circle([anchorLat, anchorLon], {
                        radius: rad,
                        color: '#A855F7',
                        weight: 2,
                        dashArray: '6, 6',
                        fillColor: '#A855F7',
                        fillOpacity: 0.2
                    }).addTo(map);
                }
            } else if (stayCircle) {
                map.removeLayer(stayCircle);
                stayCircle = null;
            }
        }

        function setPlaces(placesList) {
            if (!map || !placesLayer) return;
            placesLayer.clearLayers();
            if (!placesList || !Array.isArray(placesList)) return;

            placesList.forEach(function(p) {
                if (p.lat && p.lon && (p.lat !== 0 || p.lon !== 0)) {
                    var color = p.color || '#00F0FF';
                    var iconHtml = '<div class="place-marker" style="background:' + color + ';">' + (p.isFav ? '★' : '📍') + '</div>';
                    var icon = L.divIcon({
                        className: '',
                        html: iconHtml,
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });

                    var marker = L.marker([p.lat, p.lon], { icon: icon });
                    marker.bindPopup('<b style="color:' + color + '">' + p.name + '</b><br><span style="color:#94A3B8;font-size:11px">' + p.category + ' • Radio: ' + p.radius + 'm</span>');
                    placesLayer.addLayer(marker);

                    var zoneCircle = L.circle([p.lat, p.lon], {
                        radius: p.radius || 75,
                        color: color,
                        weight: 1,
                        dashArray: '4, 4',
                        fillColor: color,
                        fillOpacity: 0.08
                    });
                    placesLayer.addLayer(zoneCircle);
                }
            });
        }

        function setTrajectory(trajectoryList) {
            if (!map || !trajectoryLayer) return;
            trajectoryLayer.clearLayers();
            if (!trajectoryList || !Array.isArray(trajectoryList) || trajectoryList.length === 0) return;

            var latlngs = [];
            trajectoryList.forEach(function(item, idx) {
                if (item.lat && item.lon) {
                    var pt = [item.lat, item.lon];
                    latlngs.push(pt);

                    var dotIcon = L.divIcon({
                        className: '',
                        html: '<div class="trajectory-dot"></div>',
                        iconSize: [10, 10],
                        iconAnchor: [5, 5]
                    });
                    var m = L.marker(pt, { icon: dotIcon });
                    var mins = Math.round((item.duration || 0) / 60000);
                    m.bindPopup('<b>Estancia #' + (idx + 1) + '</b><br><span style="color:#94A3B8;font-size:11px">Duración: ' + mins + ' min</span>');
                    trajectoryLayer.addLayer(m);
                }
            });

            if (latlngs.length >= 2) {
                var polyline = L.polyline(latlngs, {
                    color: '#00F0FF',
                    weight: 3,
                    opacity: 0.7,
                    dashArray: '8, 8'
                });
                trajectoryLayer.addLayer(polyline);
            }
        }

        function focusLocation(lat, lon, zoom) {
            if (map && lat && lon) {
                map.flyTo([lat, lon], zoom || 16, { animate: true, duration: 0.8 });
            }
        }

        function recenter() {
            if (map && currentLat && currentLon) {
                map.flyTo([currentLat, currentLon], 16, { animate: true, duration: 0.8 });
            }
        }

        window.onload = init;
    </script>
</body>
</html>
""".trimIndent()
}
