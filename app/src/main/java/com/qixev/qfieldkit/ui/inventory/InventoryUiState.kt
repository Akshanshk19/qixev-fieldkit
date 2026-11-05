package com.qixev.qfieldkit.ui.inventory

import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity

data class InventoryFormValidationErrors(
    val nameError: String? = null,
    val skuError: String? = null,
    val quantityError: String? = null,
    val minimumStockError: String? = null,
    val purchasePriceError: String? = null,
    val sellingPriceError: String? = null
) {
    val hasErrors: Boolean
        get() = nameError != null ||
                skuError != null ||
                quantityError != null ||
                minimumStockError != null ||
                purchasePriceError != null ||
                sellingPriceError != null
}

data class InventoryUiState(
    val items: List<InventoryItemEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isAddDialogOpen: Boolean = false,
    val validationErrors: InventoryFormValidationErrors = InventoryFormValidationErrors(),
    val userMessage: String? = null
)

