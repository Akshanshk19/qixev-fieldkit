package com.qixev.qfieldkit.data.local.model

import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairJobValidatorTest {

    @Test
    fun validJob_accepted() {
        val result = RepairJobValidator.validate(
            customerName = "John Doe",
            customerPhone = "+1 (555) 019-2834",
            deviceName = "iPhone 13 Pro",
            problemDescription = "Cracked front glass, OLED intact",
            estimatedCostStr = "120.00",
            finalCostStr = "140.00"
        )

        assertTrue(result.isValid)
        assertFalse(result.errors.hasErrors)
        assertEquals(120.0, result.parsedEstimatedCost ?: 0.0, 0.001)
        assertEquals(140.0, result.parsedFinalCost ?: 0.0, 0.001)
    }

    @Test
    fun blankFields_rejected() {
        val result = RepairJobValidator.validate(
            customerName = "   ",
            customerPhone = "",
            deviceName = "\t",
            problemDescription = "",
            estimatedCostStr = "50",
            finalCostStr = "50"
        )

        assertFalse(result.isValid)
        assertNotNull(result.errors.customerNameError)
        assertNotNull(result.errors.customerPhoneError)
        assertNotNull(result.errors.deviceNameError)
        assertNotNull(result.errors.problemDescriptionError)
    }

    @Test
    fun negativeCosts_rejected() {
        val result = RepairJobValidator.validate(
            customerName = "Jane Smith",
            customerPhone = "123-456-7890",
            deviceName = "Pixel 7",
            problemDescription = "Battery replacement",
            estimatedCostStr = "-10.0",
            finalCostStr = "-20.0"
        )

        assertFalse(result.isValid)
        assertNotNull(result.errors.estimatedCostError)
        assertNotNull(result.errors.finalCostError)
    }

    @Test
    fun nonNumericCosts_rejected() {
        val result = RepairJobValidator.validate(
            customerName = "Jane Smith",
            customerPhone = "123-456-7890",
            deviceName = "Pixel 7",
            problemDescription = "Battery replacement",
            estimatedCostStr = "abc",
            finalCostStr = "pending"
        )

        assertFalse(result.isValid)
        assertNotNull(result.errors.estimatedCostError)
        assertNotNull(result.errors.finalCostError)
    }

    @Test
    fun zeroCosts_accepted() {
        val result = RepairJobValidator.validate(
            customerName = "Jane Smith",
            customerPhone = "123456",
            deviceName = "Pixel 7",
            problemDescription = "Inspection under warranty",
            estimatedCostStr = "0.0",
            finalCostStr = "0"
        )

        assertTrue(result.isValid)
        assertNull(result.errors.estimatedCostError)
        assertNull(result.errors.finalCostError)
        assertEquals(0.0, result.parsedEstimatedCost ?: -1.0, 0.001)
        assertEquals(0.0, result.parsedFinalCost ?: -1.0, 0.001)
    }

    @Test
    fun calculatePartsTotal_multipliesQuantityAndHistoricalPrice() {
        val parts = listOf(
            JobPartCrossRef(jobId = 1L, inventoryItemId = 10L, quantityUsed = 2, unitPriceAtTimeOfUse = 45.0),
            JobPartCrossRef(jobId = 1L, inventoryItemId = 11L, quantityUsed = 1, unitPriceAtTimeOfUse = 15.5),
            JobPartCrossRef(jobId = 1L, inventoryItemId = 12L, quantityUsed = 3, unitPriceAtTimeOfUse = 4.0)
        )

        val total = RepairJobValidator.calculatePartsTotal(parts)
        // 2 * 45.0 (90) + 1 * 15.5 (15.5) + 3 * 4.0 (12) = 117.5
        assertEquals(117.5, total, 0.001)
    }

    @Test
    fun calculatePartsTotal_emptyList_returnsZero() {
        val total = RepairJobValidator.calculatePartsTotal(emptyList())
        assertEquals(0.0, total, 0.001)
    }
}

