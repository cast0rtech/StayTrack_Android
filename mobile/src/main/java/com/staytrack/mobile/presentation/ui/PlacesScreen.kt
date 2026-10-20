package com.staytrack.mobile.presentation.ui

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
import androidx.compose.material.icons.automirrored.filled.CallMerge
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
import com.staytrack.core.model.Place
import com.staytrack.core.model.PlaceCategory
import com.staytrack.mobile.presentation.MobileMainViewModel
import com.staytrack.mobile.presentation.theme.*

@Composable
fun PlacesScreen(
    viewModel: MobileMainViewModel,
    places: List<Place>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<PlaceCategory?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }

    var editingPlace by remember { mutableStateOf<Place?>(null) }
    var mergingPlace by remember { mutableStateOf<Place?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val serviceState by viewModel.serviceState.collectAsState()
    val consolidacionMensaje by viewModel.consolidacionMensaje.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(consolidacionMensaje) {
        consolidacionMensaje?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.limpiarMensajeConsolidacion()
        }
    }

    val currentLocation = remember(serviceState.lastLatitude, serviceState.lastLongitude) {
        if (serviceState.lastLatitude != 0.0 || serviceState.lastLongitude != 0.0) {
            Pair(serviceState.lastLatitude, serviceState.lastLongitude)
        } else null
    }

    val filteredPlaces = remember(places, searchQuery, selectedCategoryFilter, onlyFavorites) {
        places.filter { place ->
            val matchesSearch = searchQuery.isBlank() ||
                    (place.nombre?.contains(searchQuery, ignoreCase = true) == true) ||
                    place.categoria.displayName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == null || place.categoria == selectedCategoryFilter
            val matchesFavorite = !onlyFavorites || place.isFavorite
            matchesSearch && matchesCategory && matchesFavorite
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "LUGARES Y FAVORITOS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Los favoritos acumulan estancias por proximidad",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nuevo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Banner de Consolidación inteligente si hay más de 1 lugar
            if (places.size > 1) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardGlowBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x3300F0FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Consolidar lugares duplicados",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Agrupa sitios cercanos y acumula todas las estancias",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { viewModel.consolidarLugaresCercanos() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Consolidar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar lugares...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Filter Chips Row
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null && !onlyFavorites,
                            onClick = {
                                selectedCategoryFilter = null
                                onlyFavorites = false
                            },
                            label = { Text("Todos (${places.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = onlyFavorites,
                            onClick = { onlyFavorites = !onlyFavorites },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (onlyFavorites) Color.Black else Color(0xFFFFD700)
                                )
                            },
                            label = { Text("Favoritos") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD700),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    items(PlaceCategory.entries) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryFilter = if (isSelected) null else cat
                            },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getCategoryComposeColor(cat),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // List
            if (filteredPlaces.isEmpty()) {
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
                            Text(
                                text = "No se encontraron lugares",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredPlaces, key = { it.id }) { place ->
                    PlaceCardItem(
                        place = place,
                        onEditClick = { editingPlace = place },
                        onMergeClick = { mergingPlace = place },
                        onDeleteClick = { viewModel.eliminarLugar(place.id) },
                        onFixLocationClick = {
                            if (currentLocation != null) {
                                viewModel.fijarUbicacionLugar(place.id, currentLocation.first, currentLocation.second)
                            }
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // Modal Edit
    editingPlace?.let { placeToEdit ->
        EditPlaceDialog(
            place = placeToEdit,
            currentLocation = currentLocation,
            onDismiss = { editingPlace = null },
            onSave = { nombre, categoria, radio, isFavorite, nombrePersonalizado, lat, lon ->
                viewModel.guardarLugar(
                    placeToEdit.id,
                    nombre,
                    categoria,
                    radio,
                    isFavorite,
                    nombrePersonalizado,
                    latitud = lat,
                    longitud = lon
                )
            }
        )
    }

    // Modal Merge
    mergingPlace?.let { placeToMerge ->
        val candidateDestinations = places.filter { it.id != placeToMerge.id }
        MergePlaceDialog(
            place = placeToMerge,
            candidateDestinations = candidateDestinations,
            onDismiss = { mergingPlace = null },
            onMerge = { destinationId ->
                viewModel.fusionarLugares(placeToMerge.id, destinationId)
                mergingPlace = null
            }
        )
    }

    // Modal Create
    if (showCreateDialog) {
        val nuevoPlaceDummy = Place(
            nombre = "",
            latitud = currentLocation?.first ?: 0.0,
            longitud = currentLocation?.second ?: 0.0,
            radio = 75.0,
            isFavorite = true,
            categoria = PlaceCategory.TRABAJO
        )
        EditPlaceDialog(
            place = nuevoPlaceDummy,
            currentLocation = currentLocation,
            onDismiss = { showCreateDialog = false },
            onSave = { nombre, categoria, radio, isFavorite, nombrePersonalizado, lat, lon ->
                viewModel.guardarLugar(
                    nuevoPlaceDummy.id,
                    nombre,
                    categoria,
                    radio,
                    isFavorite,
                    nombrePersonalizado,
                    latitud = lat,
                    longitud = lon
                )
            }
        )
    }
}

@Composable
fun PlaceCardItem(
    place: Place,
    onEditClick: () -> Unit,
    onMergeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onFixLocationClick: () -> Unit
) {
    val catColor = getCategoryComposeColor(place.categoria)
    val displayName = place.nombre ?: "Lugar sin nombre"
    val hasNoCoords = place.latitud == 0.0 && place.longitud == 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (place.isFavorite) 1.dp else 0.dp,
                color = if (place.isFavorite) Color(0x66FFD700) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                            .size(38.dp)
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
                                text = displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (place.isFavorite) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Favorito",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "${place.categoria.displayName} • Radio: ${place.radio.toInt()}m",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMergeClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallMerge,
                            contentDescription = "Fusionar con otro lugar",
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = CrimsonRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Alerta si no tiene coordenadas GPS válidas
            if (hasNoCoords) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33F59E0B))
                        .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sin GPS fijado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFBBF24)
                        )
                    }

                    Button(
                        onClick = onFixLocationClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFBBF24),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Fijar aquí", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MergePlaceDialog(
    place: Place,
    candidateDestinations: List<Place>,
    onDismiss: () -> Unit,
    onMerge: (destinationId: String) -> Unit
) {
    var selectedTargetId by remember {
        mutableStateOf(candidateDestinations.firstOrNull { it.isFavorite }?.id ?: candidateDestinations.firstOrNull()?.id)
    }

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
                    text = "FUSIONAR LUGAR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Text(
                    text = "Reasigna todas las estancias de '${place.nombre ?: "Lugar sin nombre"}' a otro lugar y elimina este duplicado:",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                if (candidateDestinations.isEmpty()) {
                    Text(
                        text = "No hay otros lugares disponibles para fusionar.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidateDestinations, key = { it.id }) { target ->
                            val isSelected = target.id == selectedTargetId
                            val catColor = getCategoryComposeColor(target.categoria)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTargetId = target.id }
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
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = target.nombre ?: "Lugar sin nombre",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            if (target.isFavorite) {
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
                                            text = target.categoria.displayName,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedTargetId = target.id },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = NeonCyan,
                                            unselectedColor = TextMuted
                                        )
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
                            selectedTargetId?.let { onMerge(it) }
                        },
                        enabled = selectedTargetId != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.CallMerge, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fusionar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
