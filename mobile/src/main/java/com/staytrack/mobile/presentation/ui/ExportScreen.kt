package com.staytrack.mobile.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.staytrack.mobile.presentation.ExportFormat
import com.staytrack.mobile.presentation.MobileMainViewModel
import com.staytrack.mobile.presentation.theme.*

@Composable
fun ExportScreen(
    viewModel: MobileMainViewModel,
    exportingState: String?
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "EXPORTACIÓN MULTI-FORMATO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Genera y comparte informes, hojas de cálculo, mapas y capas GIS",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        if (exportingState != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = exportingState,
                            fontSize = 13.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Excel XLSX
        item {
            ExportCardItem(
                title = "Libro Excel Real (.xlsx)",
                badge = "OpenXML Nativo",
                badgeColor = EmeraldGreen,
                description = "Libro de cálculo genuino en formato OpenXML con cabeceras con estilo, formatos numéricos y resumen de estancias.",
                icon = Icons.Default.TableChart,
                iconColor = EmeraldGreen,
                onExport = { viewModel.exportar(ExportFormat.XLSX, context) }
            )
        }

        // Tactical Map Image PNG
        item {
            ExportCardItem(
                title = "Imagen de Mapa Táctico (.png)",
                badge = "Render Bitmap HD",
                badgeColor = NeonCyan,
                description = "Captura cartográfica en alta resolución con cuadrícula cyberpunk, trayectorias neón, zonas de radio y HUD de telemetría.",
                icon = Icons.Default.Map,
                iconColor = NeonCyan,
                onExport = { viewModel.exportar(ExportFormat.MAP_IMAGE, context) }
            )
        }

        // GPX Waypoints
        item {
            ExportCardItem(
                title = "Waypoints GPS (.gpx)",
                badge = "GPX 1.1 Estándar",
                badgeColor = CyberBlue,
                description = "Archivo GPX universal con lugares y estancias como waypoints <wpt>, compatible con Garmin, Strava y GPS.",
                icon = Icons.Default.Navigation,
                iconColor = CyberBlue,
                onExport = { viewModel.exportar(ExportFormat.GPX, context) }
            )
        }

        // KML Google Earth
        item {
            ExportCardItem(
                title = "Google Earth (.kml)",
                badge = "OpenGIS KML 2.2",
                badgeColor = NeonPurple,
                description = "Documento KML con estilos y pines de color según la categoría (Casa, Trabajo, Gym, Médico) listo para Google Earth y QGIS.",
                icon = Icons.Default.Public,
                iconColor = NeonPurple,
                onExport = { viewModel.exportar(ExportFormat.KML, context) }
            )
        }

        // CSV
        item {
            ExportCardItem(
                title = "Hoja de Cálculo CSV (.csv)",
                badge = "RFC 4180",
                badgeColor = TextSecondary,
                description = "Archivo CSV estándar universal con datos en crudo para análisis en Python, Pandas o Excel.",
                icon = Icons.Default.Description,
                iconColor = TextSecondary,
                onExport = { viewModel.exportar(ExportFormat.CSV, context) }
            )
        }
    }
}

@Composable
fun ExportCardItem(
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardGlowBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceElevated,
                    contentColor = iconColor
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, iconColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generar y Compartir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
