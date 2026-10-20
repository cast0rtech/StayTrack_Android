package com.staytrack.mobile.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import com.staytrack.mobile.presentation.theme.*

@Composable
fun EditPlaceDialog(
    place: Place,
    currentLocation: Pair<Double, Double>? = null,
    onDismiss: () -> Unit,
    onSave: (
        nombre: String?,
        categoria: PlaceCategory,
        radio: Double,
        isFavorite: Boolean,
        nombrePersonalizado: String?,
        latitud: Double,
        longitud: Double
    ) -> Unit
) {
    var nombre by remember { mutableStateOf(place.nombre ?: "") }
    var selectedCategory by remember { mutableStateOf(place.categoria) }
    var selectedRadio by remember { mutableStateOf(place.radio) }
    var isFavorite by remember { mutableStateOf(place.isFavorite) }
    var nombrePersonalizado by remember { mutableStateOf(place.nombrePersonalizadoCategoria ?: "") }

    var latitud by remember {
        mutableStateOf(
            if (place.latitud != 0.0) place.latitud else (currentLocation?.first ?: 0.0)
        )
    }
    var longitud by remember {
        mutableStateOf(
            if (place.longitud != 0.0) place.longitud else (currentLocation?.second ?: 0.0)
        )
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (place.id.isEmpty() || place.nombre.isNullOrBlank()) "CONFIGURAR LUGAR" else "EDITAR LUGAR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) Color(0xFFFFD700) else TextMuted
                        )
                    }
                }

                // Nombre
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del lugar") },
                    placeholder = { Text("Ej: Casa, Trabajo, Gimnasio") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Categoría
                Text(
                    text = "Categoría",
                    fontSize = 13.sp,
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = category.displayName.take(4),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Radio de zona (50m, 75m, 100m)
                Text(
                    text = "Radio de zona: ${selectedRadio.toInt()}m",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50.0, 75.0, 100.0).forEach { r ->
                        val isSelected = selectedRadio == r
                        Button(
                            onClick = { selectedRadio = r },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonCyan else DarkSurface,
                                contentColor = if (isSelected) Color.Black else TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${r.toInt()}m",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Ubicación GPS fijada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (latitud != 0.0 || longitud != 0.0) "📍 Ubicación GPS fijada" else "⚠️ Sin coordenadas GPS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (latitud != 0.0 || longitud != 0.0) NeonCyan else Color(0xFFFBBF24)
                                )
                                if (latitud != 0.0 || longitud != 0.0) {
                                    Text(
                                        text = "${String.format("%.4f", latitud)}, ${String.format("%.4f", longitud)}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                } else {
                                    Text(
                                        text = "Asigna tu posición actual para acumular estancias aquí",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            if (currentLocation != null && (currentLocation.first != 0.0 || currentLocation.second != 0.0)) {
                                Button(
                                    onClick = {
                                        latitud = currentLocation.first
                                        longitud = currentLocation.second
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkSurfaceBorder,
                                        contentColor = NeonCyan
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fijar aquí", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Acciones
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
                            val finalNombre = if (nombre.isBlank()) null else nombre.trim()
                            val finalPersonalizado = if (nombrePersonalizado.isBlank()) null else nombrePersonalizado.trim()
                            onSave(
                                finalNombre,
                                selectedCategory,
                                selectedRadio,
                                isFavorite,
                                finalPersonalizado,
                                latitud,
                                longitud
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

