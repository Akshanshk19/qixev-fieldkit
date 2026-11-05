package com.qixev.qfieldkit.ui.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryValidatorTest {

    @Test
    fun validInputs_returnsSuccess() {
        val result = InventoryValidator.validate(
            name = "Screen Replacement OEM",
            sku = "SCR-OEM-01",
            quantityStr = "10",
            minimumStockStr = "3",
            purchasePriceStr = "45.50",
            sellingPriceStr = "90.00"
        )

        assertTrue(result.isValid)
        assertFalse(result.errors.hasErrors)
        assertEquals(10, result.parsedQuantity)
        assertEquals(3, result.parsedMinimumStock)
        assertEquals(45.50, result.parsedPurchasePrice ?: 0.0, 0.001)
        assertEquals(90.00, result.parsedSellingPrice ?: 0.0, 0.001)
    }

    @Test
    fun blankNameAndSku_returnsErrors() {
        val result = InventoryValidator.validate(
            name = "   ",
            sku = "",
            quantityStr = "5",
            minimumStockStr = "2",
            purchasePriceStr = "10.0",
            sellingPriceStr = "20.0"
        )

        assertFalse(result.isValid)
        assertNotNull(result.errors.nameError)
        assertNotNull(result.errors.skuError)
    }

    @Test
    fun negativeQuantityAndMinimumStock_returnsErrors() {
        val result = InventoryValidator.validate(
            name = "Battery Pack",
            sku = "BAT-001",
            quantityStr = "-1",
            minimumStockStr = "-5",
            purchasePriceStr = "12.0",
            sellingPriceStr = "25.0"
        )

        assertFalse(result.isValid)
        assertNotNull(result.errors.quantityError)
        assertNotNull(result.errors.minimumStockError)
    }

    @Test
    fun negativePrices_returnsErrors() {
        val result = InventoryValidator.validate(
            name = "Charging Port",
            sku = "CP-002",
            quantityStr = "8",
            minimumStockStr = "2",
            purchasePriceStr = "-5.0",
            sellingPriceStr = "-10.0"
        )

        assertFalse(result.isValid)
        assertNotNull(result.errors.purchasePriceError)
        assertNotNull(result.errors.sellingPriceError)
    }

    @Test
    fun nonNumericValues_returnsFormatErrors() {
        val result = InventoryValidator.validate(
            name = "Solder Flux",
            sku = "FLUX-99",
            quantityStr = "abc",
            minimumStockStr = "xyz",
            purchasePriceStr = "free",
            sellingPriceStr = "n/a"
        )

        assertFalse(result.isValid)
        assertNotNull(result.errors.quantityError)
        assertNotNull(result.errors.minimumStockError)
        assertNotNull(result.errors.purchasePriceError)
        assertNotNull(result.errors.sellingPriceError)
    }

    @Test
    fun zeroQuantityAndZeroPrices_areAllowed() {
        val result = InventoryValidator.validate(
            name = "Free Sample",
            sku = "SMPL-00",
            quantityStr = "0",
            minimumStockStr = "0",
            purchasePriceStr = "0.0",
            sellingPriceStr = "0.0"
        )

        assertTrue(result.isValid)
        assertNull(result.errors.quantityError)
        assertNull(result.errors.minimumStockError)
        assertNull(result.errors.purchasePriceError)
        assertNull(result.errors.sellingPriceError)
    }
}

