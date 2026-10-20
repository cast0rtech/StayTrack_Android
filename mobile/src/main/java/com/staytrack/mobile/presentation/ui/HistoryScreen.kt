package com.staytrack.mobile.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.staytrack.core.logic.AnalyticsAndExportUtils
import com.staytrack.core.model.DeviceOrigin
import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.core.model.Visit
import com.staytrack.mobile.presentation.MobileMainViewModel
import com.staytrack.mobile.presentation.theme.*
import com.staytrack.mobile.service.TrackingState
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryDateFilter(val label: String) {
    TODAY("Hoy"),
    YESTERDAY("Ayer"),
    LAST_7_DAYS("Últimos 7 días"),
    THIS_MONTH("Este mes"),
    ALL("Todas")
}

@Composable
fun HistoryScreen(
    viewModel: MobileMainViewModel,
    allVisits: List<Visit>,
    allPlaces: List<Place>,
    serviceState: TrackingState
) {
    var selectedDateFilter by remember { mutableStateOf(HistoryDateFilter.TODAY) }
    var selectedPlaceFilterId by remember { mutableStateOf<String?>(null) }
    var showMapPreview by remember { mutableStateOf(true) }
    var focusedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var reassigningVisit by remember { mutableStateOf<Visit?>(null) }
    var creatingFavFromVisit by remember { mutableStateOf<Visit?>(null) }

    val placeMap = remember(allPlaces) { allPlaces.associateBy { it.id } }

    val filteredVisits = remember(allVisits, selectedDateFilter, selectedPlaceFilterId) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val startOfYesterday = cal.timeInMillis

        val calMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calMonth.timeInMillis

        allVisits.filter { visit ->
            val matchesDate = when (selectedDateFilter) {
                HistoryDateFilter.TODAY -> visit.inicio >= startOfToday
                HistoryDateFilter.YESTERDAY -> visit.inicio in startOfYesterday until startOfToday
                HistoryDateFilter.LAST_7_DAYS -> visit.inicio >= (now - 7 * 24 * 3600_000L)
                HistoryDateFilter.THIS_MONTH -> visit.inicio >= startOfMonth
                HistoryDateFilter.ALL -> true
            }

            val matchesPlace = selectedPlaceFilterId == null || visit.placeId == selectedPlaceFilterId

            matchesDate && matchesPlace
        }
    }

    // Estadísticas del periodo
    val totalTimeMillis = remember(filteredVisits) {
        filteredVisits.sumOf { it.duracionMillis }
    }

    // Agrupación de visitas por fecha
    val groupedVisits = remember(filteredVisits) {
        val dayFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        filteredVisits.groupBy { v ->
            dayFormat.format(Date(v.inicio)).replaceFirstChar { it.uppercase() }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Encabezado
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HISTORIAL DE ESTANCIAS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Consulta estancias anteriores y previsualiza en el mapa",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { showMapPreview = !showMapPreview },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (showMapPreview) NeonCyan else DarkSurfaceBorder)
                ) {
                    Icon(
                        imageVector = if (showMapPreview) Icons.Default.Map else Icons.Default.LayersClear,
                        contentDescription = "Alternar mapa",
                        tint = if (showMapPreview) Color.Black else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Filtros de fecha (Chips)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HistoryDateFilter.entries) { filter ->
                    val isSelected = filter == selectedDateFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDateFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Filtro opcional por Lugar
        if (allPlaces.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedPlaceFilterId == null,
                            onClick = { selectedPlaceFilterId = null },
                            label = { Text("Todos los sitios") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x3300F0FF),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                    items(allPlaces) { place ->
                        val isSelected = place.id == selectedPlaceFilterId
                        val catColor = getCategoryComposeColor(place.categoria)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPlaceFilterId = if (isSelected) null else place.id
                            },
                            label = { Text(place.nombre ?: place.categoria.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // Previsualizador de Mapa Táctico (Muestra las estancias del periodo filtrado)
        if (showMapPreview && filteredVisits.isNotEmpty()) {
            item {
                TacticalLiveMapCard(
                    serviceState = serviceState,
                    places = allPlaces,
                    trajectoryVisits = filteredVisits,
                    selectedLocation = focusedLocation,
                    titleOverlay = "PREVISUALIZACIÓN (${filteredVisits.size} ESTANCIAS)"
                )
            }
        }

        // Resumen KPI del periodo
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Estancias", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${filteredVisits.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Tiempo Total", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AnalyticsAndExportUtils.formatearDuracion(totalTimeMillis),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Promedio", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val avg = if (filteredVisits.isNotEmpty()) totalTimeMillis / filteredVisits.size else 0L
                        Text(
                            text = AnalyticsAndExportUtils.formatearDuracion(avg),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }

        // Listado Cronológico de Estancias
        if (filteredVisits.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay estancias registradas en este periodo",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Prueba seleccionando 'Últimos 7 días' o 'Todas'",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else {
            groupedVisits.forEach { (dia, visitasDelDia) ->
                val tiempoDia = visitasDelDia.sumOf { it.duracionMillis }

                // Cabecera del día
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dia,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${visitasDelDia.size} estancias • ${AnalyticsAndExportUtils.formatearDuracion(tiempoDia)}",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                items(visitasDelDia, key = { it.id }) { visit ->
                    val place = visit.placeId?.let { placeMap[it] }
                    HistoryVisitItem(
                        visit = visit,
                        place = place,
                        onPreviewMapClick = {
                            focusedLocation = Pair(visit.latitudAncla, visit.longitudAncla)
                            showMapPreview = true
                        },
                        onReassignClick = { reassigningVisit = visit },
                        onCreateFavClick = { creatingFavFromVisit = visit },
                        onDeleteClick = { viewModel.eliminarVisita(visit.id) }
                    )
                }
            }
        }
    }

    // Modal para reasignar estancia a otro lugar
    reassigningVisit?.let { visit ->
        ReassignVisitDialog(
            visit = visit,
            places = allPlaces,
            onDismiss = { reassigningVisit = null },
            onReassign = { newPlaceId ->
                viewModel.reasignarVisitaLugar(visit.id, newPlaceId)
                reassigningVisit = null
            }
        )
    }

    // Modal para crear un favorito a partir de la estancia
    creatingFavFromVisit?.let { visit ->
        CreateFavoriteFromVisitDialog(
            visit = visit,
            onDismiss = { creatingFavFromVisit = null },
            onCreate = { nombre, cat, radio ->
                viewModel.crearFavoritoDesdeVisita(visit, nombre, cat, radio)
                creatingFavFromVisit = null
            }
        )
    }
}

@Composable
fun HistoryVisitItem(
    visit: Visit,
    place: Place?,
    onPreviewMapClick: () -> Unit,
    onReassignClick: () -> Unit,
    onCreateFavClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val inicioStr = timeFormat.format(Date(visit.inicio))
    val finStr = visit.fin?.let { timeFormat.format(Date(it)) } ?: "En curso"
    val duracionStr = AnalyticsAndExportUtils.formatearDuracion(visit.duracionMillis)

    val cat = place?.categoria ?: PlaceCategory.OTRO
    val catColor = getCategoryComposeColor(cat)
    val nombre = place?.nombre ?: cat.displayName

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (place?.isFavorite == true) Color(0x44FFD700) else DarkSurfaceBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = nombre,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (place?.isFavorite == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Favorito",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = "$inicioStr - $finStr",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Duración en píldora
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3300F0FF))
                        .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = duracionStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fila inferior con detalles y acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Origen dispositivo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (visit.origen == DeviceOrigin.WATCH) "⌚ Reloj" else "📱 Móvil",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.4f", visit.latitudAncla)}, ${String.format(Locale.US, "%.4f", visit.longitudAncla)}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                // Acciones rápidas
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onPreviewMapClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationSearching,
                            contentDescription = "Ver en mapa",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (place == null || !place.isFavorite) {
                        IconButton(
                            onClick = onCreateFavClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Guardar como favorito",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onReassignClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Reasignar a otro lugar",
                            tint = NeonPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar estancia",
                            tint = CrimsonRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReassignVisitDialog(
    visit: Visit,
    places: List<Place>,
    onDismiss: () -> Unit,
    onReassign: (newPlaceId: String) -> Unit
) {
    var selectedPlaceId by remember { mutableStateOf(visit.placeId ?: places.firstOrNull()?.id) }

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
                Text(
                    text = "REASIGNAR ESTANCIA",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "Vincula esta estancia a uno de tus lugares guardados:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(places, key = { it.id }) { p ->
                        val isSelected = p.id == selectedPlaceId
                        val catColor = getCategoryComposeColor(p.categoria)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlaceId = p.id }
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
                                    onClick = { selectedPlaceId = p.id },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NeonCyan,
                                        unselectedColor = TextMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = p.nombre ?: p.categoria.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        if (p.isFavorite) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${p.categoria.displayName} • Radio: ${p.radio.toInt()}m",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
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
                        onClick = {
                            selectedPlaceId?.let { onReassign(it) }
                        },
                        enabled = selectedPlaceId != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reasignar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFavoriteFromVisitDialog(
    visit: Visit,
    onDismiss: () -> Unit,
    onCreate: (nombre: String, categoria: PlaceCategory, radio: Double) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PlaceCategory.TRABAJO) }
    var selectedRadio by remember { mutableStateOf(75.0) }

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
                Text(
                    text = "GUARDAR COMO FAVORITO",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del lugar") },
                    placeholder = { Text("Ej: Trabajo, Gimnasio, Casa...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Categoría",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlaceCategory.entries.forEach { category ->
                        val isSelected = category == selectedCategory
                        val catColor = getCategoryComposeColor(category)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) catColor.copy(alpha = 0.25f) else DarkSurface)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) catColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.displayName.take(4),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
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
                        onClick = {
                            if (nombre.isNotBlank()) {
                                onCreate(nombre.trim(), selectedCategory, selectedRadio)
                            }
                        },
                        enabled = nombre.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Guardar Favorito", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
