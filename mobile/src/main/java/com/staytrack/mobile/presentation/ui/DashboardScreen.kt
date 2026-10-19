package com.staytrack.mobile.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.staytrack.core.logic.AnalyticsAndExportUtils
import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import com.staytrack.mobile.presentation.DashboardStats
import com.staytrack.mobile.presentation.MobileMainViewModel
import com.staytrack.mobile.presentation.theme.*
import com.staytrack.mobile.service.TrackingState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: MobileMainViewModel,
    serviceState: TrackingState,
    allVisits: List<Visit>,
    allPlaces: List<Place>,
    stats: DashboardStats
) {
    val context = LocalContext.current
    val placeMap = remember(allPlaces) { allPlaces.associateBy { it.id } }

    val todayVisits = remember(allVisits) {
        val inicioHoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        allVisits.filter { it.inicio >= inicioHoy }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (serviceState.isRunning) NeonCyan else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "STAYTRACK // MÓVIL AUTÓNOMO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (serviceState.isRunning) "Registrando estancias continuamente sin reloj" else "Monitoreo en pausa",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Live Hero Card
        item {
            LiveStatusHeroCard(
                serviceState = serviceState,
                onToggle = { viewModel.toggleTracking(context) }
            )
        }

        // Quick Stats Row
        item {
            QuickStatsRow(stats = stats)
        }

        // Timeline Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESTANCIAS DE HOY",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${todayVisits.size} registradas",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Visits Timeline
        if (todayVisits.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay estancias registradas hoy aún.\nInicia el monitoreo para detectar paradas automáticamente.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(todayVisits, key = { it.id }) { visit ->
                val place = visit.placeId?.let { placeMap[it] }
                VisitTimelineCard(visit = visit, place = place)
            }
        }
    }
}

@Composable
fun LiveStatusHeroCard(
    serviceState: TrackingState,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardGlowBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(if (serviceState.isRunning) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (!serviceState.isRunning) DarkSurfaceElevated
                        else if (serviceState.isDwelling) Color(0x3300F0FF)
                        else Color(0x33A855F7)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (!serviceState.isRunning) Icons.Default.Pause
                    else if (serviceState.isDwelling) Icons.Default.LocationOn
                    else Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = if (!serviceState.isRunning) TextMuted
                    else if (serviceState.isDwelling) NeonCyan
                    else NeonPurple,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!serviceState.isRunning) {
                Text(
                    text = "MONITOREO DETENIDO",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
                Text(
                    text = "Toca iniciar para comenzar el registro de estancias",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (serviceState.isDwelling) {
                Text(
                    text = "EN ESTANCIA CONFIRMADA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                val nombreLugar = serviceState.activePlace?.nombre
                    ?: serviceState.activePlace?.categoria?.displayName
                    ?: "Lugar sin nombre"
                Text(
                    text = nombreLugar,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${serviceState.stayDurationMinutes} minutos acumulados",
                    fontSize = 14.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "EN MOVIMIENTO",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonPurple
                )
                Text(
                    text = "Detectando zona de estancia (>2 min en radio de 75m)",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (serviceState.isRunning) CrimsonRed else NeonCyan,
                    contentColor = if (serviceState.isRunning) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(
                    imageVector = if (serviceState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (serviceState.isRunning) "Detener Monitoreo" else "Iniciar Monitoreo",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuickStatsRow(stats: DashboardStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Estancias",
            value = "${stats.estanciasHoy}",
            icon = Icons.Default.Schedule,
            color = NeonCyan
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Tiempo",
            value = AnalyticsAndExportUtils.formatearDuracion(stats.tiempoEstanciaHoyMillis),
            icon = Icons.Default.Timer,
            color = CyberBlue
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Lugares",
            value = "${stats.lugaresTotal}",
            icon = Icons.Default.Place,
            color = NeonPurple
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun VisitTimelineCard(visit: Visit, place: Place?) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val inicioStr = timeFormat.format(Date(visit.inicio))
    val finStr = visit.fin?.let { timeFormat.format(Date(it)) } ?: "En curso"
    val duracionStr = AnalyticsAndExportUtils.formatearDuracion(visit.duracionMillis)

    val catColor = getCategoryComposeColor(place?.categoria ?: PlaceCategory.OTRO)
    val nombre = place?.nombre ?: place?.categoria?.displayName ?: "Lugar sin nombre"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = nombre,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$inicioStr - $finStr • $duracionStr",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                color = Color(0x3300F0FF),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = duracionStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

fun getCategoryComposeColor(category: PlaceCategory): Color {
    return when (category) {
        PlaceCategory.CASA -> CyberBlue
        PlaceCategory.TRABAJO -> NeonCyan
        PlaceCategory.ENTRENAMIENTO -> EmeraldGreen
        PlaceCategory.MEDICO -> CrimsonRed
        PlaceCategory.OTRO -> NeonPurple
    }
}
