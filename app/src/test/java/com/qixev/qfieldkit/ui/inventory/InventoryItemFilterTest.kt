package com.qixev.qfieldkit.ui.inventory

import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryItemFilterTest {

    private val sampleItems = listOf(
        InventoryItemEntity(
            id = 1L,
            name = "iPhone 13 Screen OLED",
            sku = "SCR-IP13",
            category = "Screens",
            quantity = 5,
            minimumStock = 2,
            purchasePrice = 50.0,
            sellingPrice = 100.0
        ),
        InventoryItemEntity(
            id = 2L,
            name = "Samsung S21 Battery",
            sku = "BAT-S21",
            category = "Batteries",
            quantity = 8,
            minimumStock = 3,
            purchasePrice = 15.0,
            sellingPrice = 35.0
        ),
        InventoryItemEntity(
            id = 3L,
            name = "USB-C Charging Flex",
            sku = "FLX-USBC",
            category = "Ports",
            quantity = 1,
            minimumStock = 2,
            purchasePrice = 3.0,
            sellingPrice = 12.0
        ),
        InventoryItemEntity(
            id = 4L,
            name = "iPhone 14 Screen OEM",
            sku = "SCR-IP14",
            category = "Screens",
            quantity = 0,
            minimumStock = 1,
            purchasePrice = 80.0,
            sellingPrice = 150.0
        )
    )

    @Test
    fun filterBySearchQuery_matchesNameCaseInsensitive() {
        val result = InventoryItemFilter.filter(sampleItems, searchQuery = "iphone", selectedCategory = null)
        assertEquals(2, result.size)
        assertTrue(result.any { it.sku == "SCR-IP13" })
        assertTrue(result.any { it.sku == "SCR-IP14" })
    }

    @Test
    fun filterBySearchQuery_matchesSkuCaseInsensitive() {
        val result = InventoryItemFilter.filter(sampleItems, searchQuery = "bat-s21", selectedCategory = null)
        assertEquals(1, result.size)
        assertEquals("BAT-S21", result[0].sku)
    }

    @Test
    fun filterByCategory_returnsOnlyItemsInThatCategory() {
        val result = InventoryItemFilter.filter(sampleItems, searchQuery = "", selectedCategory = "Screens")
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "Screens" })
    }

    @Test
    fun filterBySearchAndCategory_returnsIntersection() {
        val result = InventoryItemFilter.filter(sampleItems, searchQuery = "14", selectedCategory = "Screens")
        assertEquals(1, result.size)
        assertEquals("SCR-IP14", result[0].sku)
    }

    @Test
    fun filterWithEmptyQueryAndNullCategory_returnsAllItems() {
        val result = InventoryItemFilter.filter(sampleItems, searchQuery = "", selectedCategory = null)
        assertEquals(4, result.size)
    }

    @Test
    fun filterWithNonMatchingCriteria_returnsEmptyList() {
        val result = InventoryItemFilter.filter(sampleItems, searchQuery = "nonexistent", selectedCategory = null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun extractCategories_returnsDistinctSortedCategories() {
        val categories = InventoryItemFilter.extractCategories(sampleItems)
        assertEquals(listOf("Batteries", "Ports", "Screens"), categories)
    }
}

