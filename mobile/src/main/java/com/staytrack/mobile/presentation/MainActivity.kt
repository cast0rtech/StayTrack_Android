package com.staytrack.mobile.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.staytrack.mobile.presentation.theme.*
import com.staytrack.mobile.presentation.ui.DashboardScreen
import com.staytrack.mobile.presentation.ui.ExportScreen
import com.staytrack.mobile.presentation.ui.PlacesScreen

enum class MobileScreen(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    PLACES("Lugares", Icons.Default.Place),
    EXPORT("Exportar", Icons.Default.Share)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MobileMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StayTrackMobileTheme {
                var currentScreen by remember { mutableStateOf(MobileScreen.DASHBOARD) }

                // Runtime Permissions Launcher
                var hasLocationPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                }

                LaunchedEffect(Unit) {
                    val neededPermissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(neededPermissions.toTypedArray())
                }

                val serviceState by viewModel.serviceState.collectAsState()
                val allVisits by viewModel.allVisits.collectAsState()
                val allPlaces by viewModel.allPlaces.collectAsState()
                val stats by viewModel.dashboardStats.collectAsState()
                val exportingState by viewModel.exportingState.collectAsState()

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurfaceElevated,
                            contentColor = TextPrimary,
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = DarkSurfaceBorder
                            )
                        ) {
                            MobileScreen.entries.forEach { screen ->
                                val isSelected = currentScreen == screen
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.label
                                        )
                                    },
                                    label = { Text(screen.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = NeonCyan,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(DarkBackground)
                    ) {
                        when (currentScreen) {
                            MobileScreen.DASHBOARD -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    serviceState = serviceState,
                                    allVisits = allVisits,
                                    allPlaces = allPlaces,
                                    stats = stats
                                )
                            }
                            MobileScreen.PLACES -> {
                                PlacesScreen(
                                    viewModel = viewModel,
                                    places = allPlaces
                                )
                            }
                            MobileScreen.EXPORT -> {
                                ExportScreen(
                                    viewModel = viewModel,
                                    exportingState = exportingState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
