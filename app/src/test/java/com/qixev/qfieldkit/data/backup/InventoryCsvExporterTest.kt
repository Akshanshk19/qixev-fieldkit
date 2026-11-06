package com.qixev.qfieldkit.data.backup

import com.qixev.qfieldkit.data.backup.csv.InventoryCsvExporter
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryCsvExporterTest {

    @Test
    fun exportEmptyInventoryProducesOnlyHeader() {
        val csv = InventoryCsvExporter.export(emptyList())
        val lines = csv.trim().lines()
        assertEquals(1, lines.size)
        assertEquals("ID,Name,SKU,Category,Quantity,MinimumStock,PurchasePrice,SellingPrice", lines[0])
    }

    @Test
    fun exportInventoryWithValuesAndProperEscaping() {
        val items = listOf(
            InventoryItemEntity(
                id = 1L,
                name = "Standard Screen",
                sku = "SCR-001",
                category = "Screens",
                quantity = 15,
                minimumStock = 3,
                purchasePrice = 25.50,
                sellingPrice = 50.00
            ),
            InventoryItemEntity(
                id = 2L,
                name = "Screen, OLED \"Super\", 6.1-inch",
                sku = "SCR-002",
                category = "Screens, Premium",
                quantity = 5,
                minimumStock = 1,
                purchasePrice = 75.00,
                sellingPrice = 120.00
            ),
            InventoryItemEntity(
                id = 3L,
                name = "Multi\nLine\nCable",
                sku = "CBL-001",
                category = "Cables",
                quantity = 20,
                minimumStock = 5,
                purchasePrice = 2.00,
                sellingPrice = 9.99
            )
        )

        val csv = InventoryCsvExporter.export(items)
        val header = "ID,Name,SKU,Category,Quantity,MinimumStock,PurchasePrice,SellingPrice"
        assertTrue(csv.startsWith(header))

        // Check escaping of item 2 (contains commas and quotes)
        assertTrue(csv.contains("\"Screen, OLED \"\"Super\"\", 6.1-inch\""))
        assertTrue(csv.contains("\"Screens, Premium\""))

        // Check escaping of item 3 (contains newlines)
        assertTrue(csv.contains("\"Multi\nLine\nCable\""))
    }
}

