package com.qixev.qfieldkit.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qixev.qfieldkit.data.backup.BackupRepository
import com.qixev.qfieldkit.data.local.dao.InventoryItemDao
import com.qixev.qfieldkit.data.local.dao.RepairJobDao
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.repository.InventoryRepository
import com.qixev.qfieldkit.data.repository.OperationResult
import com.qixev.qfieldkit.data.repository.RepairJobRepository
import kotlinx.coroutines.flow.first
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
class RepeatedUsageSmokeTest {

    private lateinit var database: FieldKitDatabase
    private lateinit var inventoryItemDao: InventoryItemDao
    private lateinit var repairJobDao: RepairJobDao
    private lateinit var inventoryRepository: InventoryRepository
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
        inventoryRepository = InventoryRepository(inventoryItemDao)
        repairJobRepository = RepairJobRepository(database, repairJobDao, inventoryItemDao)
        backupRepository = BackupRepository(database, inventoryItemDao, repairJobDao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun manualSmokeTest_repeatedUsageOfSameItem_lifecycleVerification() = runBlocking {
        // Step 1: Create inventory item with initial stock of 50
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Hex Bolt M6",
                sku = "BLT-M6",
                category = "Fasteners",
                quantity = 50,
                minimumStock = 10,
                purchasePrice = 0.5,
                sellingPrice = 2.0
            )
        )

        // Step 2: Create work unit / job
        val createJobResult = repairJobRepository.createJob(
            title = "Emergency Generator Mounting",
            description = "Mount diesel generator to chassis frame",
            status = JobStatus.IN_PROGRESS
        )
        assertTrue(createJobResult is OperationResult.Success)
        val jobId = (createJobResult as OperationResult.Success).data

        // Step 3: Add SAME item twice with different quantity, location, and purpose
        // Usage Event 1: 4 bolts at "Base Frame North" for "Primary mount"
        val add1 = repairJobRepository.addPartToJob(
            jobId = jobId,
            inventoryItemId = itemId,
            quantity = 4,
            location = "Base Frame North",
            purpose = "Primary mount"
        )
        assertTrue("First usage addition must succeed", add1.isSuccess)

        // Usage Event 2: 6 bolts at "Base Frame South" for "Secondary mount"
        val add2 = repairJobRepository.addPartToJob(
            jobId = jobId,
            inventoryItemId = itemId,
            quantity = 6,
            location = "Base Frame South",
            purpose = "Secondary mount"
        )
        assertTrue("Second usage addition must succeed", add2.isSuccess)

        // Step 4: Verify TWO distinct UI records appear in getJobPartDetails
        val uiDetails = repairJobRepository.getJobPartDetails(jobId).first()
        assertEquals("Must display exactly 2 distinct usage records in UI", 2, uiDetails.size)

        val record1 = uiDetails.first { it.location == "Base Frame North" }
        assertEquals(4, record1.partRef.quantityUsed)
        assertEquals("Primary mount", record1.purpose)
        assertEquals(2.0, record1.partRef.unitPriceAtTimeOfUse, 0.001)
        assertEquals(8.0, record1.totalCost, 0.001)
        assertTrue("Timestamp must be recorded", record1.usedAt > 0L)

        val record2 = uiDetails.first { it.location == "Base Frame South" }
        assertEquals(6, record2.partRef.quantityUsed)
        assertEquals("Secondary mount", record2.purpose)
        assertEquals(2.0, record2.partRef.unitPriceAtTimeOfUse, 0.001)
        assertEquals(12.0, record2.totalCost, 0.001)
        assertTrue("Timestamp must be recorded", record2.usedAt > 0L)

        assertTrue("Each record must have a distinct unique ID", record1.usageId != record2.usageId)

        // Verify stock is decremented by 4 + 6 = 10 (50 - 10 = 40)
        val itemAfterBothUsages = inventoryItemDao.getInventoryItemById(itemId)
        assertNotNull(itemAfterBothUsages)
        assertEquals(40, itemAfterBothUsages!!.quantity)

        // Step 5: Delete ONE record (record 1: 4 bolts)
        val removeResult = repairJobRepository.removeUsageRecord(record1.usageId)
        assertTrue("Removing usage record 1 must succeed", removeResult.isSuccess)

        // Step 6: Verify ONLY its quantity is restored (40 + 4 = 44)
        val itemAfterRemoval = inventoryItemDao.getInventoryItemById(itemId)
        assertEquals("Stock must restore only the deleted usage quantity (+4)", 44, itemAfterRemoval?.quantity)

        // Step 7: Verify the OTHER record remains untouched
        val uiDetailsAfterRemoval = repairJobRepository.getJobPartDetails(jobId).first()
        assertEquals("Only 1 usage record should remain", 1, uiDetailsAfterRemoval.size)
        val remainingRecord = uiDetailsAfterRemoval[0]
        assertEquals(record2.usageId, remainingRecord.usageId)
        assertEquals(6, remainingRecord.partRef.quantityUsed)
        assertEquals("Base Frame South", remainingRecord.location)
        assertEquals("Secondary mount", remainingRecord.purpose)

        // Step 8: Add record 1 back to test backup & restore with multiple discrete usages
        val add1Again = repairJobRepository.addPartToJob(
            jobId = jobId,
            inventoryItemId = itemId,
            quantity = 4,
            location = "Base Frame North",
            purpose = "Primary mount re-added"
        )
        assertTrue(add1Again.isSuccess)
        assertEquals(40, inventoryItemDao.getInventoryItemById(itemId)?.quantity)

        // Step 9: Backup / Restore and verify BOTH remain
        val backup = backupRepository.createBackup()
        assertEquals(2, backup.jobParts.size)

        // Wipe current database
        repairJobDao.deleteAllJobParts()
        repairJobDao.deleteAllJobs()
        inventoryItemDao.deleteAll()
        assertTrue(repairJobDao.getAllJobPartsSync().isEmpty())

        // Restore backup
        val restoreResult = backupRepository.restoreBackup(backup)
        assertTrue("Backup restore must succeed", restoreResult.isSuccess)

        // Verify BOTH records remain after restore with their discrete metadata
        val restoredDetails = repairJobRepository.getJobPartDetails(jobId).first()
        assertEquals("Both usage records must remain after backup/restore", 2, restoredDetails.size)

        val restoredNorth = restoredDetails.first { it.location == "Base Frame North" }
        assertEquals(4, restoredNorth.partRef.quantityUsed)
        assertEquals("Primary mount re-added", restoredNorth.purpose)

        val restoredSouth = restoredDetails.first { it.location == "Base Frame South" }
        assertEquals(6, restoredSouth.partRef.quantityUsed)
        assertEquals("Secondary mount", restoredSouth.purpose)

        // Verify inventory stock balance is preserved accurately at 40
        assertEquals(40, inventoryItemDao.getInventoryItemById(itemId)?.quantity)
    }
}
