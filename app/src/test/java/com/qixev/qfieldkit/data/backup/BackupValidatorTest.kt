package com.qixev.qfieldkit.data.backup

import com.qixev.qfieldkit.data.backup.model.BackupValidationResult
import com.qixev.qfieldkit.data.backup.model.FieldKitBackup
import com.qixev.qfieldkit.data.backup.serializer.FieldKitBackupSerializer
import com.qixev.qfieldkit.data.backup.validator.BackupValidator
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef
import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

    private fun createValidBackup(): FieldKitBackup {
        return FieldKitBackup(
            backupVersion = 1,
            exportedAt = "2026-11-06T09:00:00Z",
            inventoryItems = listOf(
                InventoryItemEntity(
                    id = 1L,
                    name = "iPhone 13 Screen",
                    sku = "SCR-IP13",
                    category = "Screens",
                    quantity = 10,
                    minimumStock = 2,
                    purchasePrice = 45.0,
                    sellingPrice = 85.0
                ),
                InventoryItemEntity(
                    id = 2L,
                    name = "iPhone 13 Battery",
                    sku = "BAT-IP13",
                    category = "Batteries",
                    quantity = 5,
                    minimumStock = 1,
                    purchasePrice = 15.0,
                    sellingPrice = 40.0
                )
            ),
            repairJobs = listOf(
                RepairJobEntity(
                    id = 101L,
                    customerName = "John Doe",
                    customerPhone = "555-1234",
                    deviceName = "iPhone 13",
                    problemDescription = "Broken screen and bad battery",
                    status = JobStatus.IN_PROGRESS,
                    estimatedCost = 150.0,
                    finalCost = 0.0,
                    createdAt = 1000L,
                    updatedAt = 2000L
                )
            ),
            jobParts = listOf(
                JobPartCrossRef(
                    jobId = 101L,
                    inventoryItemId = 1L,
                    quantityUsed = 1,
                    unitPriceAtTimeOfUse = 85.0
                ),
                JobPartCrossRef(
                    jobId = 101L,
                    inventoryItemId = 2L,
                    quantityUsed = 1,
                    unitPriceAtTimeOfUse = 40.0
                )
            )
        )
    }

    @Test
    fun validBackupPassesValidation() {
        val backup = createValidBackup()
        val json = FieldKitBackupSerializer.serialize(backup)
        val result = BackupValidator.validate(json)

        assertTrue(result is BackupValidationResult.Success)
        val validated = (result as BackupValidationResult.Success).backup
        assertEquals(1, validated.backupVersion)
        assertEquals(2, validated.inventoryItems.size)
        assertEquals(1, validated.repairJobs.size)
        assertEquals(2, validated.jobParts.size)
    }

    @Test
    fun validVersion2BackupPassesValidation() {
        val backup = FieldKitBackup(
            backupVersion = 2,
            exportedAt = "2026-11-07T12:00:00Z",
            inventoryItems = listOf(
                InventoryItemEntity(
                    id = 1L,
                    name = "CAT6 Cable",
                    sku = "CAB-CAT6",
                    category = "Networking",
                    quantity = 100,
                    minimumStock = 20,
                    purchasePrice = 0.5,
                    sellingPrice = 1.2
                )
            ),
            repairJobs = listOf(
                RepairJobEntity(
                    id = 10L,
                    title = "Conference Room Setup",
                    description = "Ethernet drops and terminations",
                    status = JobStatus.IN_PROGRESS,
                    estimatedCost = 200.0,
                    createdAt = 1000L,
                    updatedAt = 2000L
                )
            ),
            jobParts = listOf(
                JobPartCrossRef(
                    jobId = 10L,
                    inventoryItemId = 1L,
                    quantityUsed = 25,
                    unitPriceAtTimeOfUse = 1.2,
                    usedAt = 1500L,
                    location = "Floor 2 Room B",
                    purpose = "Table drops"
                )
            )
        )
        val json = FieldKitBackupSerializer.serialize(backup)
        val result = BackupValidator.validate(json)

        assertTrue(result is BackupValidationResult.Success)
        val validated = (result as BackupValidationResult.Success).backup
        assertEquals(2, validated.backupVersion)
        assertEquals(1, validated.inventoryItems.size)
        assertEquals(1, validated.repairJobs.size)
        assertEquals("Conference Room Setup", validated.repairJobs[0].displayTitle)
        assertEquals(1, validated.jobParts.size)
        assertEquals("Floor 2 Room B", validated.jobParts[0].location)
        assertEquals("Table drops", validated.jobParts[0].purpose)
        assertEquals(1500L, validated.jobParts[0].usedAt)
    }

    @Test
    fun version2AllowsMultipleUsagesOfSameItemInSameJob() {
        val backup = FieldKitBackup(
            backupVersion = 2,
            exportedAt = "2026-11-07T12:00:00Z",
            inventoryItems = listOf(
                InventoryItemEntity(
                    id = 1L,
                    name = "Screws",
                    sku = "SCR-01",
                    category = "Fasteners",
                    quantity = 100,
                    minimumStock = 10,
                    purchasePrice = 0.1,
                    sellingPrice = 0.5
                )
            ),
            repairJobs = listOf(
                RepairJobEntity(
                    id = 10L,
                    title = "Bench Assembly",
                    status = JobStatus.IN_PROGRESS
                )
            ),
            jobParts = listOf(
                JobPartCrossRef(
                    id = 1L,
                    jobId = 10L,
                    inventoryItemId = 1L,
                    quantityUsed = 2,
                    unitPriceAtTimeOfUse = 0.5,
                    location = "Bench 1",
                    purpose = "Assembly"
                ),
                JobPartCrossRef(
                    id = 2L,
                    jobId = 10L,
                    inventoryItemId = 1L,
                    quantityUsed = 3,
                    unitPriceAtTimeOfUse = 0.5,
                    location = "Rack 4",
                    purpose = "Installation"
                )
            )
        )
        val json = FieldKitBackupSerializer.serialize(backup)
        val result = BackupValidator.validate(json)

        assertTrue(result is BackupValidationResult.Success)
        val validated = (result as BackupValidationResult.Success).backup
        assertEquals(2, validated.jobParts.size)
        assertEquals(2, validated.jobParts[0].quantityUsed)
        assertEquals(3, validated.jobParts[1].quantityUsed)
    }

    @Test
    fun rejectEmptyFile() {
        val result = BackupValidator.validate("   ")
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("empty"))
    }

    @Test
    fun rejectMalformedJson() {
        val result = BackupValidator.validate("{ malformed json }")
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("Invalid JSON"))
    }

    @Test
    fun rejectUnsupportedBackupVersion() {
        val json = """
            {
                "backupVersion": 99,
                "exportedAt": "2026-11-06T09:00:00Z",
                "inventoryItems": [],
                "repairJobs": [],
                "jobParts": []
            }
        """.trimIndent()
        val result = BackupValidator.validate(json)
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("Unsupported backup version 99"))
    }

    @Test
    fun rejectDuplicateInventoryId() {
        val backup = createValidBackup().copy(
            inventoryItems = listOf(
                InventoryItemEntity(id = 1L, name = "Screen A", sku = "SKU-A", category = "A", quantity = 1, minimumStock = 0, purchasePrice = 1.0, sellingPrice = 2.0),
                InventoryItemEntity(id = 1L, name = "Screen B", sku = "SKU-B", category = "B", quantity = 1, minimumStock = 0, purchasePrice = 1.0, sellingPrice = 2.0)
            )
        )
        val json = FieldKitBackupSerializer.serialize(backup)
        val result = BackupValidator.validate(json)
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("Duplicate inventory item ID"))
    }

    @Test
    fun rejectDuplicateSku() {
        val backup = createValidBackup().copy(
            inventoryItems = listOf(
                InventoryItemEntity(id = 1L, name = "Screen A", sku = "SKU-DUP", category = "A", quantity = 1, minimumStock = 0, purchasePrice = 1.0, sellingPrice = 2.0),
                InventoryItemEntity(id = 2L, name = "Screen B", sku = "SKU-DUP", category = "B", quantity = 1, minimumStock = 0, purchasePrice = 1.0, sellingPrice = 2.0)
            )
        )
        val json = FieldKitBackupSerializer.serialize(backup)
        val result = BackupValidator.validate(json)
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("Duplicate SKU"))
    }

    @Test
    fun rejectDuplicateJobId() {
        val backup = createValidBackup().copy(
            repairJobs = listOf(
                RepairJobEntity(id = 5L, customerName = "A", customerPhone = "1", deviceName = "D1", problemDescription = "P1"),
                RepairJobEntity(id = 5L, customerName = "B", customerPhone = "2", deviceName = "D2", problemDescription = "P2")
            )
        )
        val json = FieldKitBackupSerializer.serialize(backup)
        val result = BackupValidator.validate(json)
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("Duplicate repair job ID"))
    }

    @Test
    fun rejectNegativeQuantity() {
        val backup = createValidBackup().copy(
            inventoryItems = listOf(
                InventoryItemEntity(id = 1L, name = "Screen A", sku = "SKU-A", category = "A", quantity = -5, minimumStock = 0, purchasePrice = 1.0, sellingPrice = 2.0)
            )
        )
        val json = FieldKitBackupSerializer.serialize(backup)
        val result = BackupValidator.validate(json)
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("negative quantity"))
    }

    @Test
    fun rejectPartReferencingUnknownJobId() {
        val backup = createValidBackup().copy(
            jobParts = listOf(
                JobPartCrossRef(jobId = 9999L, inventoryItemId = 1L, quantityUsed = 1, unitPriceAtTimeOfUse = 10.0)
            )
        )
        val json = FieldKitBackupSerializer.serialize(backup)
        val result = BackupValidator.validate(json)
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("unknown repair job ID 9999"))
    }

    @Test
    fun rejectPartReferencingUnknownInventoryItemId() {
        val backup = createValidBackup().copy(
            jobParts = listOf(
                JobPartCrossRef(jobId = 101L, inventoryItemId = 8888L, quantityUsed = 1, unitPriceAtTimeOfUse = 10.0)
            )
        )
        val json = FieldKitBackupSerializer.serialize(backup)
        val result = BackupValidator.validate(json)
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("unknown inventory item ID 8888"))
    }

    @Test
    fun rejectInvalidJobStatus() {
        val json = """
            {
                "backupVersion": 1,
                "exportedAt": "2026-11-06T09:00:00Z",
                "inventoryItems": [],
                "repairJobs": [
                    {
                        "id": 1,
                        "customerName": "Bob",
                        "customerPhone": "123",
                        "deviceName": "Phone",
                        "problemDescription": "Broken",
                        "status": "NON_EXISTENT_STATUS",
                        "estimatedCost": 0.0,
                        "finalCost": 0.0,
                        "createdAt": 100,
                        "updatedAt": 200
                    }
                ],
                "jobParts": []
            }
        """.trimIndent()
        val result = BackupValidator.validate(json)
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("invalid status"))
    }

    @Test
    fun rejectDuplicateCompositeJobPart() {
        val backup = createValidBackup().copy(
            jobParts = listOf(
                JobPartCrossRef(jobId = 101L, inventoryItemId = 1L, quantityUsed = 1, unitPriceAtTimeOfUse = 10.0),
                JobPartCrossRef(jobId = 101L, inventoryItemId = 1L, quantityUsed = 2, unitPriceAtTimeOfUse = 10.0)
            )
        )
        val json = FieldKitBackupSerializer.serialize(backup)
        val result = BackupValidator.validate(json)
        assertTrue(result is BackupValidationResult.Error)
        assertTrue((result as BackupValidationResult.Error).message.contains("Duplicate part record"))
    }
}

