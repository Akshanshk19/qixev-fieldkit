package com.qixev.qfieldkit.ui.inventory

import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity

object InventoryItemFilter {

    fun filter(
        items: List<InventoryItemEntity>,
        searchQuery: String,
        selectedCategory: String?
    ): List<InventoryItemEntity> {
        val trimmedQuery = searchQuery.trim().lowercase()

        return items.filter { item ->
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                item.name.lowercase().contains(trimmedQuery) ||
                        item.sku.lowercase().contains(trimmedQuery)
            }

            val matchesCategory = if (selectedCategory.isNullOrBlank()) {
                true
            } else {
                item.category.equals(selectedCategory, ignoreCase = true)
            }

            matchesQuery && matchesCategory
        }
    }

    fun extractCategories(items: List<InventoryItemEntity>): List<String> {
        return items.map { it.category.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }
}

