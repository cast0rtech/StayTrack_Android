package com.staytrack.mobile.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    var showCreateDialog by remember { mutableStateOf(false) }

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
                Column {
                    Text(
                        text = "LUGARES Y FAVORITOS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Los favoritos tienen prioridad al asignar estancias",
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
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    onDeleteClick = { viewModel.eliminarLugar(place.id) }
                )
            }
        }
    }

    // Modal Edit
    editingPlace?.let { placeToEdit ->
        EditPlaceDialog(
            place = placeToEdit,
            onDismiss = { editingPlace = null },
            onSave = { nombre, categoria, radio, isFavorite, nombrePersonalizado ->
                viewModel.guardarLugar(placeToEdit.id, nombre, categoria, radio, isFavorite, nombrePersonalizado)
            }
        )
    }

    // Modal Create
    if (showCreateDialog) {
        val nuevoPlaceDummy = Place(
            nombre = "",
            latitud = 0.0,
            longitud = 0.0,
            radio = 75.0,
            isFavorite = true,
            categoria = PlaceCategory.CASA
        )
        EditPlaceDialog(
            place = nuevoPlaceDummy,
            onDismiss = { showCreateDialog = false },
            onSave = { nombre, categoria, radio, isFavorite, nombrePersonalizado ->
                viewModel.guardarLugar(
                    nuevoPlaceDummy.id,
                    nombre,
                    categoria,
                    radio,
                    isFavorite,
                    nombrePersonalizado
                )
            }
        )
    }
}

@Composable
fun PlaceCardItem(
    place: Place,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val catColor = getCategoryComposeColor(place.categoria)
    val displayName = place.nombre ?: "Lugar sin nombre"

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
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = NeonCyan
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = CrimsonRed
                        )
                    }
                }
            }
        }
    }
}
