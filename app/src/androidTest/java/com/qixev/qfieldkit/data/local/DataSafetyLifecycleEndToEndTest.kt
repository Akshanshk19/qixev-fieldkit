package com.qixev.qfieldkit.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qixev.qfieldkit.data.backup.BackupRepository
import com.qixev.qfieldkit.data.backup.model.BackupValidationResult
import com.qixev.qfieldkit.data.local.dao.InventoryItemDao
import com.qixev.qfieldkit.data.local.dao.RepairJobDao
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
import com.qixev.qfieldkit.data.repository.OperationResult
import com.qixev.qfieldkit.data.repository.RepairJobRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DataSafetyLifecycleEndToEndTest {

    private lateinit var database: FieldKitDatabase
    private lateinit var inventoryItemDao: InventoryItemDao
    private lateinit var repairJobDao: RepairJobDao
    private lateinit var repairJobRepository: RepairJobRepository
    private lateinit var backupRepository: BackupRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            FieldKitDatabase::class.java
        ).allowMainThreadQueries().build()

        inventoryItemDao = database.inventoryItemDao()
        repairJobDao = database.repairJobDao()
        repairJobRepository = RepairJobRepository(database, repairJobDao, inventoryItemDao)
        backupRepository = BackupRepository(database, inventoryItemDao, repairJobDao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun completeDataSafetyAndExportLifecycle() = runBlocking {
        // Step 1: Create inventory item
        val initialStock = 20
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "OLED Screen Galaxy S22",
                sku = "SCR-GS22-01",
                category = "Screens",
                quantity = initialStock,
                minimumStock = 3,
                purchasePrice = 60.0,
                sellingPrice = 135.0
            )
        )
        assertTrue(itemId > 0)

        // Step 2: Create repair job
        val createJobResult = repairJobRepository.createJob(
            customerName = "Marcus Brody",
            customerPhone = "+1-555-8392",
            deviceName = "Samsung Galaxy S22",
            problemDescription = "Flickering display after drop",
            estimatedCost = 150.0
        )
        assertTrue(createJobResult is OperationResult.Success)
        val jobId = (createJobResult as OperationResult.Success).data
        assertTrue(jobId > 0)

        // Step 3: Add inventory part to job (allocate 2 units)
        val partQtyToUse = 2
        val addPartResult = repairJobRepository.addPartToJob(jobId, itemId, partQtyToUse)
        assertTrue(addPartResult is OperationResult.Success)

        // Step 4: Verify stock changes (20 - 2 = 18)
        val itemAfterAllocation = inventoryItemDao.getInventoryItemById(itemId)
        assertNotNull(itemAfterAllocation)
        assertEquals(18, itemAfterAllocation!!.quantity)

        // Step 5: Export full backup snapshot
        val backup = backupRepository.createBackup()
        assertEquals(1, backup.inventoryItems.size)
        assertEquals(1, backup.repairJobs.size)
        assertEquals(1, backup.jobParts.size)
        val backupJson = backupRepository.serializeBackup(backup)
        assertTrue(backupJson.contains("SCR-GS22-01"))
        assertTrue(backupJson.contains("Marcus Brody"))

        // Step 6: Change/delete data as appropriate
        // Delete the repair job, modify inventory quantity to 999, and add an unrelated item
        repairJobDao.deleteAllPartsForJob(jobId)
        val jobEntity = repairJobDao.getJobById(jobId)
        if (jobEntity != null) {
            repairJobDao.deleteJob(jobEntity)
        }
        inventoryItemDao.updateItem(itemAfterAllocation.copy(quantity = 999))
        val junkItemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Temporary Junk",
                sku = "JUNK-99",
                category = "Misc",
                quantity = 1,
                minimumStock = 0,
                purchasePrice = 0.5,
                sellingPrice = 1.0
            )
        )
        assertTrue(junkItemId > 0)
        assertEquals(2, inventoryItemDao.getAllInventoryItemsSync().size)
        assertEquals(0, repairJobDao.getAllJobsSync().size)

        // Step 7: Validate and Restore backup
        val validationResult = backupRepository.validateBackupJson(backupJson)
        assertTrue("Backup must pass validation", validationResult is BackupValidationResult.Success)
        val validBackupToRestore = (validationResult as BackupValidationResult.Success).backup
        val restoreResult = backupRepository.restoreBackup(validBackupToRestore)
        assertTrue("Restore must succeed", restoreResult.isSuccess)

        // Step 8: Verify inventory returns correctly
        val restoredItems = inventoryItemDao.getAllInventoryItemsSync()
        assertEquals("Junk item must be wiped, only restored item exists", 1, restoredItems.size)
        val restoredItem = restoredItems[0]
        assertEquals(itemId, restoredItem.id)
        assertEquals("OLED Screen Galaxy S22", restoredItem.name)
        assertEquals("SCR-GS22-01", restoredItem.sku)
        assertEquals(18, restoredItem.quantity)

        // Step 9: Verify repair job returns correctly
        val restoredJobs = repairJobDao.getAllJobsSync()
        assertEquals(1, restoredJobs.size)
        val restoredJob = restoredJobs[0]
        assertEquals(jobId, restoredJob.id)
        assertEquals("Marcus Brody", restoredJob.customerName)
        assertEquals("Samsung Galaxy S22", restoredJob.deviceName)
        assertEquals(JobStatus.RECEIVED, restoredJob.status)

        // Step 10: Verify allocated parts and historical prices return correctly
        val restoredParts = repairJobDao.getAllJobPartsSync()
        assertEquals(1, restoredParts.size)
        val restoredPart = restoredParts[0]
        assertEquals(jobId, restoredPart.jobId)
        assertEquals(itemId, restoredPart.inventoryItemId)
        assertEquals(2, restoredPart.quantityUsed)
        assertEquals(135.0, restoredPart.unitPriceAtTimeOfUse, 0.001)

        // Step 11: Export inventory CSV
        val csv = backupRepository.exportInventoryCsv()

        // Step 12: Open CSV and verify contents
        val csvLines = csv.trim().lines()
        assertEquals(2, csvLines.size) // header + 1 item row
        assertEquals("ID,Name,SKU,Category,Quantity,MinimumStock,PurchasePrice,SellingPrice", csvLines[0])
        val expectedRow = "$itemId,OLED Screen Galaxy S22,SCR-GS22-01,Screens,18,3,60.00,135.00"
        assertEquals(expectedRow, csvLines[1])

        // Step 13: Attempt an invalid backup and verify live data remains untouched
        val invalidJson = """{"backupVersion": 1, "inventoryItems": "corrupted"}"""
        val invalidValidationResult = backupRepository.validateBackupJson(invalidJson)
        assertTrue("Malformed backup must be rejected", invalidValidationResult is BackupValidationResult.Error)

        // Verify live data was not touched at all
        val itemsAfterInvalidAttempt = inventoryItemDao.getAllInventoryItemsSync()
        val jobsAfterInvalidAttempt = repairJobDao.getAllJobsSync()
        val partsAfterInvalidAttempt = repairJobDao.getAllJobPartsSync()

        assertEquals(1, itemsAfterInvalidAttempt.size)
        assertEquals(18, itemsAfterInvalidAttempt[0].quantity)
        assertEquals(1, jobsAfterInvalidAttempt.size)
        assertEquals("Marcus Brody", jobsAfterInvalidAttempt[0].customerName)
        assertEquals(1, partsAfterInvalidAttempt.size)
    }
}

