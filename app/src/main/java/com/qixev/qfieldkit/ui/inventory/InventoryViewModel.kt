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
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _isFormDialogOpen = MutableStateFlow(false)
    private val _editingItem = MutableStateFlow<InventoryItemEntity?>(null)
    private val _itemPendingDelete = MutableStateFlow<InventoryItemEntity?>(null)
    private val _validationErrors = MutableStateFlow(InventoryFormValidationErrors())
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InventoryUiState> = combine(
        repository.getInventoryItems(),
        _searchQuery,
        _selectedCategory,
        _isFormDialogOpen,
        _editingItem,
        _itemPendingDelete,
        _validationErrors,
        _userMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val items = args[0] as List<InventoryItemEntity>
        val searchQuery = args[1] as String
        val selectedCategory = args[2] as String?
        val isFormDialogOpen = args[3] as Boolean
        val editingItem = args[4] as InventoryItemEntity?
        val itemPendingDelete = args[5] as InventoryItemEntity?
        val validationErrors = args[6] as InventoryFormValidationErrors
        val userMessage = args[7] as String?

        val availableCategories = InventoryItemFilter.extractCategories(items)
        val filtered = InventoryItemFilter.filter(items, searchQuery, selectedCategory)

        InventoryUiState(
            items = items,
            filteredItems = filtered,
            availableCategories = availableCategories,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            isLoading = false,
            isFormDialogOpen = isFormDialogOpen,
            editingItem = editingItem,
            itemPendingDelete = itemPendingDelete,
            validationErrors = validationErrors,
            userMessage = userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = InventoryUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun onOpenAddDialog() {
        _editingItem.value = null
        _validationErrors.value = InventoryFormValidationErrors()
        _isFormDialogOpen.value = true
    }

    fun onOpenEditDialog(item: InventoryItemEntity) {
        _editingItem.value = item
        _validationErrors.value = InventoryFormValidationErrors()
        _isFormDialogOpen.value = true
    }

    fun onDismissFormDialog() {
        _editingItem.value = null
        _validationErrors.value = InventoryFormValidationErrors()
        _isFormDialogOpen.value = false
    }

    fun onRequestDelete(item: InventoryItemEntity) {
        _itemPendingDelete.value = item
    }

    fun onDismissDeleteDialog() {
        _itemPendingDelete.value = null
    }

    fun onConfirmDelete() {
        val item = _itemPendingDelete.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                _itemPendingDelete.value = null
            } catch (e: Exception) {
                _itemPendingDelete.value = null
                val isForeignKeyViolation = e is android.database.sqlite.SQLiteConstraintException ||
                        e.message?.contains("FOREIGN KEY", ignoreCase = true) == true
                _userMessage.value = if (isForeignKeyViolation) {
                    "This item cannot be deleted because it is used by a repair job."
                } else {
                    "Unable to delete item: database constraint or storage error."
                }
            }
        }
    }

    fun onClearUserMessage() {
        _userMessage.value = null
    }

    fun saveItem(
        name: String,
        sku: String,
        category: String,
        quantityStr: String,
        minimumStockStr: String,
        purchasePriceStr: String,
        sellingPriceStr: String
    ) {
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
            return
        }

        val trimmedSku = sku.trim()
        val currentEditing = _editingItem.value

        viewModelScope.launch {
            // Check for duplicate SKU
            val existingItem = repository.getInventoryItemBySku(trimmedSku)
            if (existingItem != null && existingItem.id != currentEditing?.id) {
                _validationErrors.value = validation.errors.copy(
                    skuError = "SKU already exists. Each item must have a unique SKU."
                )
                return@launch
            }

            try {
                if (currentEditing != null) {
                    val updated = currentEditing.copy(
                        name = name.trim(),
                        sku = trimmedSku,
                        category = category.trim().ifBlank { "General" },
                        quantity = validation.parsedQuantity ?: 0,
                        minimumStock = validation.parsedMinimumStock ?: 0,
                        purchasePrice = validation.parsedPurchasePrice ?: 0.0,
                        sellingPrice = validation.parsedSellingPrice ?: 0.0
                    )
                    repository.updateItem(updated)
                } else {
                    val newItem = InventoryItemEntity(
                        name = name.trim(),
                        sku = trimmedSku,
                        category = category.trim().ifBlank { "General" },
                        quantity = validation.parsedQuantity ?: 0,
                        minimumStock = validation.parsedMinimumStock ?: 0,
                        purchasePrice = validation.parsedPurchasePrice ?: 0.0,
                        sellingPrice = validation.parsedSellingPrice ?: 0.0
                    )
                    repository.addItem(newItem)
                }

                _isFormDialogOpen.value = false
                _editingItem.value = null
                _validationErrors.value = InventoryFormValidationErrors()
            } catch (e: Exception) {
                // Catches SQLiteConstraintException or any other DB constraint violations
                _validationErrors.value = validation.errors.copy(
                    skuError = "SKU already exists or database constraint failed."
                )
            }
        }
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
