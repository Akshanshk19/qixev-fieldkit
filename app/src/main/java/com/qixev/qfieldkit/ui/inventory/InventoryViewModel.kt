package com.qixev.qfieldkit.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    private val _isAddDialogOpen = MutableStateFlow(false)
    private val _validationErrors = MutableStateFlow(InventoryFormValidationErrors())
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InventoryUiState> = combine(
        repository.getInventoryItems(),
        _isAddDialogOpen,
        _validationErrors,
        _userMessage
    ) { items, isAddDialogOpen, validationErrors, userMessage ->
        InventoryUiState(
            items = items,
            isLoading = false,
            isAddDialogOpen = isAddDialogOpen,
            validationErrors = validationErrors,
            userMessage = userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = InventoryUiState(isLoading = true)
    )

    fun onOpenAddDialog() {
        _validationErrors.value = InventoryFormValidationErrors()
        _isAddDialogOpen.value = true
    }

    fun onDismissAddDialog() {
        _validationErrors.value = InventoryFormValidationErrors()
        _isAddDialogOpen.value = false
    }

    fun onClearUserMessage() {
        _userMessage.value = null
    }

    fun addItem(
        name: String,
        sku: String,
        category: String,
        quantityStr: String,
        minimumStockStr: String,
        purchasePriceStr: String,
        sellingPriceStr: String
    ): Boolean {
        val validation = InventoryValidator.validate(
            name = name,
            sku = sku,
            quantityStr = quantityStr,
            minimumStockStr = minimumStockStr,
            purchasePriceStr = purchasePriceStr,
            sellingPriceStr = sellingPriceStr
        )

        if (!validation.isValid) {
            _validationErrors.value = validation.errors
            return false
        }

        viewModelScope.launch {
            val item = InventoryItemEntity(
                name = name.trim(),
                sku = sku.trim(),
                category = category.trim().ifBlank { "General" },
                quantity = validation.parsedQuantity ?: 0,
                minimumStock = validation.parsedMinimumStock ?: 0,
                purchasePrice = validation.parsedPurchasePrice ?: 0.0,
                sellingPrice = validation.parsedSellingPrice ?: 0.0
            )
            repository.addItem(item)
            _isAddDialogOpen.value = false
            _validationErrors.value = InventoryFormValidationErrors()
        }
        return true
    }

    class Factory(
        private val repository: InventoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
                return InventoryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

