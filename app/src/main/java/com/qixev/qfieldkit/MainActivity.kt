package com.qixev.qfieldkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qixev.qfieldkit.ui.inventory.InventoryViewModel
import com.qixev.qfieldkit.ui.jobs.RepairJobListViewModel
import com.qixev.qfieldkit.ui.navigation.FieldKitNavHost
import com.qixev.qfieldkit.ui.navigation.Screen
import com.qixev.qfieldkit.ui.settings.SettingsViewModel
import com.qixev.qfieldkit.ui.theme.QFieldKitTheme

class MainActivity : ComponentActivity() {

    private val inventoryViewModel: InventoryViewModel by viewModels {
        val app = application as FieldKitApplication
        InventoryViewModel.Factory(app.inventoryRepository)
    }

    private val repairJobListViewModel: RepairJobListViewModel by viewModels {
        val app = application as FieldKitApplication
        RepairJobListViewModel.Factory(app.repairJobRepository)
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as FieldKitApplication
        SettingsViewModel.Factory(app.backupRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_QFieldKit)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QFieldKitTheme {
                val navController = rememberNavController()
                val app = application as FieldKitApplication
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute == Screen.RepairJobs.route ||
                        currentRoute == Screen.Inventory.route ||
                        currentRoute == Screen.Settings.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 3.dp
                            ) {
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == Screen.RepairJobs.route) {
                                                Icons.Filled.Build
                                            } else {
                                                Icons.Outlined.Build
                                            },
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_jobs)) },
                                    selected = currentRoute == Screen.RepairJobs.route,
                                    onClick = {
                                        navController.navigate(Screen.RepairJobs.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == Screen.Inventory.route) {
                                                Icons.AutoMirrored.Filled.List
                                            } else {
                                                Icons.AutoMirrored.Outlined.List
                                            },
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_inventory)) },
                                    selected = currentRoute == Screen.Inventory.route,
                                    onClick = {
                                        navController.navigate(Screen.Inventory.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == Screen.Settings.route) {
                                                Icons.Filled.Settings
                                            } else {
                                                Icons.Outlined.Settings
                                            },
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_settings)) },
                                    selected = currentRoute == Screen.Settings.route,
                                    onClick = {
                                        navController.navigate(Screen.Settings.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    FieldKitNavHost(
                        navController = navController,
                        inventoryRepository = app.inventoryRepository,
                        repairJobRepository = app.repairJobRepository,
                        backupRepository = app.backupRepository,
                        onboardingPreferences = app.onboardingPreferences,
                        inventoryViewModel = inventoryViewModel,
                        repairJobListViewModel = repairJobListViewModel,
                        settingsViewModel = settingsViewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }
        }
    }
}
