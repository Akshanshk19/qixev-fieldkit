package com.qixev.qfieldkit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.qixev.qfieldkit.data.backup.BackupRepository
import com.qixev.qfieldkit.data.repository.InventoryRepository
import com.qixev.qfieldkit.data.repository.RepairJobRepository
import com.qixev.qfieldkit.ui.inventory.InventoryScreen
import com.qixev.qfieldkit.ui.inventory.InventoryViewModel
import com.qixev.qfieldkit.ui.jobs.JobDetailScreen
import com.qixev.qfieldkit.ui.jobs.JobDetailViewModel
import com.qixev.qfieldkit.ui.jobs.RepairJobListScreen
import com.qixev.qfieldkit.ui.jobs.RepairJobListViewModel
import com.qixev.qfieldkit.ui.settings.SettingsScreen
import com.qixev.qfieldkit.ui.settings.SettingsViewModel

@Composable
fun FieldKitNavHost(
    navController: NavHostController,
    inventoryRepository: InventoryRepository,
    repairJobRepository: RepairJobRepository,
    backupRepository: BackupRepository,
    inventoryViewModel: InventoryViewModel,
    repairJobListViewModel: RepairJobListViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    // Listen for create job navigation event to navigate to detail screen
    LaunchedEffect(Unit) {
        repairJobListViewModel.navigateToDetailEvent.collect { jobId ->
            navController.navigate(Screen.JobDetail.createRoute(jobId))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.RepairJobs.route,
        modifier = modifier
    ) {
        composable(route = Screen.RepairJobs.route) {
            val uiState by repairJobListViewModel.uiState.collectAsStateWithLifecycle()
            RepairJobListScreen(
                uiState = uiState,
                onSearchQueryChanged = repairJobListViewModel::onSearchQueryChanged,
                onStatusSelected = repairJobListViewModel::onStatusSelected,
                onOpenCreateDialog = repairJobListViewModel::onOpenCreateDialog,
                onDismissCreateDialog = repairJobListViewModel::onDismissCreateDialog,
                onCreateJob = repairJobListViewModel::createJob,
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                },
                onClearUserMessage = repairJobListViewModel::onClearUserMessage
            )
        }

        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getLong("jobId") ?: return@composable
            val detailViewModel: JobDetailViewModel = viewModel(
                key = "job_detail_$jobId",
                factory = JobDetailViewModel.Factory(
                    jobId = jobId,
                    repairJobRepository = repairJobRepository,
                    inventoryRepository = inventoryRepository
                )
            )
            val uiState by detailViewModel.uiState.collectAsStateWithLifecycle()

            JobDetailScreen(
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() },
                onTransitionStatus = detailViewModel::transitionStatus,
                onOpenAddPartDialog = detailViewModel::onOpenAddPartDialog,
                onDismissAddPartDialog = detailViewModel::onDismissAddPartDialog,
                onAddPart = detailViewModel::addPart,
                onRequestRemovePart = detailViewModel::onRequestRemovePart,
                onDismissRemovePart = detailViewModel::onDismissRemovePart,
                onConfirmRemovePart = detailViewModel::confirmRemovePart,
                onOpenCancelConfirmation = detailViewModel::onOpenCancelConfirmation,
                onDismissCancelConfirmation = detailViewModel::onDismissCancelConfirmation,
                onConfirmCancelJob = detailViewModel::confirmCancelJob,
                onClearUserMessage = detailViewModel::onClearUserMessage
            )
        }

        composable(route = Screen.Inventory.route) {
            val uiState by inventoryViewModel.uiState.collectAsStateWithLifecycle()
            InventoryScreen(
                uiState = uiState,
                onSearchQueryChanged = inventoryViewModel::onSearchQueryChanged,
                onCategorySelected = inventoryViewModel::onCategorySelected,
                onOpenAddDialog = inventoryViewModel::onOpenAddDialog,
                onOpenEditDialog = inventoryViewModel::onOpenEditDialog,
                onDismissFormDialog = inventoryViewModel::onDismissFormDialog,
                onRequestDelete = inventoryViewModel::onRequestDelete,
                onDismissDeleteDialog = inventoryViewModel::onDismissDeleteDialog,
                onConfirmDelete = inventoryViewModel::onConfirmDelete,
                onSaveItem = inventoryViewModel::saveItem
            )
        }

        composable(route = Screen.Settings.route) {
            val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            SettingsScreen(
                uiState = uiState,
                onExportBackup = settingsViewModel::exportFullBackup,
                onExportCsv = settingsViewModel::exportInventoryCsv,
                onBackupFileSelected = settingsViewModel::onBackupFileSelected,
                onConfirmRestore = settingsViewModel::onConfirmRestore,
                onDismissRestoreConfirmDialog = settingsViewModel::onDismissRestoreConfirmDialog,
                onClearUserMessage = settingsViewModel::onClearUserMessage
            )
        }
    }
}
