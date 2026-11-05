package com.qixev.qfieldkit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.qixev.qfieldkit.ui.inventory.InventoryScreen
import com.qixev.qfieldkit.ui.inventory.InventoryViewModel

@Composable
fun FieldKitNavHost(
    navController: NavHostController,
    inventoryViewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Inventory.route,
        modifier = modifier
    ) {
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
    }
}
