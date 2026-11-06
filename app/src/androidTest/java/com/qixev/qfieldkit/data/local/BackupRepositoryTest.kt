package com.qixev.qfieldkit.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qixev.qfieldkit.data.backup.BackupRepository
import com.qixev.qfieldkit.data.backup.model.FieldKitBackup
import com.qixev.qfieldkit.data.local.dao.InventoryItemDao
import com.qixev.qfieldkit.data.local.dao.RepairJobDao
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef
import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
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
class BackupRepositoryTest {

    private lateinit var database: FieldKitDatabase
    private lateinit var inventoryItemDao: InventoryItemDao
    private lateinit var repairJobDao: RepairJobDao
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
        backupRepository = BackupRepository(database, inventoryItemDao, repairJobDao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun exportBackupContainsAllDataWithPreservedFields() = runBlocking {
        // Insert sample inventory
        val item1Id = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Screen iPhone 13",
                sku = "SCR-13",
                category = "Screens",
                quantity = 15,
                minimumStock = 2,
                purchasePrice = 30.0,
                sellingPrice = 75.0
            )
        )
        val item2Id = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Battery iPhone 13",
                sku = "BAT-13",
                category = "Batteries",
                quantity = 8,
                minimumStock = 1,
                purchasePrice = 12.0,
                sellingPrice = 35.0
            )
        )

        // Insert sample repair job
        val jobId = repairJobDao.insertJob(
            RepairJobEntity(
                customerName = "Bob Smith",
                customerPhone = "555-4321",
                deviceName = "iPhone 13 Pro",
                problemDescription = "Cracked glass and dead battery",
                status = JobStatus.IN_PROGRESS,
                estimatedCost = 150.0,
                finalCost = 0.0,
                createdAt = 1700000000000L,
                updatedAt = 1700000050000L
            )
        )

        // Insert job parts with historical unit prices
        repairJobDao.insertJobPart(
            JobPartCrossRef(
                jobId = jobId,
                inventoryItemId = item1Id,
                quantityUsed = 1,
                unitPriceAtTimeOfUse = 70.0 // historical discounted price
            )
        )
        repairJobDao.insertJobPart(
            JobPartCrossRef(
                jobId = jobId,
                inventoryItemId = item2Id,
                quantityUsed = 1,
                unitPriceAtTimeOfUse = 35.0
            )
        )

        val backup = backupRepository.createBackup()

        assertEquals(1, backup.backupVersion)
        assertNotNull(backup.exportedAt)
        assertEquals(2, backup.inventoryItems.size)
        assertEquals(1, backup.repairJobs.size)
        assertEquals(2, backup.jobParts.size)

        val exportedJob = backup.repairJobs[0]
        assertEquals(jobId, exportedJob.id)
        assertEquals("Bob Smith", exportedJob.customerName)
        assertEquals(JobStatus.IN_PROGRESS, exportedJob.status)
        assertEquals(1700000000000L, exportedJob.createdAt)
        assertEquals(1700000050000L, exportedJob.updatedAt)

        val part1 = backup.jobParts.first { it.inventoryItemId == item1Id }
        assertEquals(1, part1.quantityUsed)
        assertEquals(70.0, part1.unitPriceAtTimeOfUse, 0.001)
    }

    @Test
    fun restoreBackupReplacesExistingDataCorrectly() = runBlocking {
        // Populate initial database data
        val oldItemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Old Part",
                sku = "OLD-01",
                category = "Legacy",
                quantity = 50,
                minimumStock = 5,
                purchasePrice = 1.0,
                sellingPrice = 2.0
            )
        )
        val oldJobId = repairJobDao.insertJob(
            RepairJobEntity(
                customerName = "Old Customer",
                customerPhone = "111",
                deviceName = "Old Device",
                problemDescription = "Old Problem",
                status = JobStatus.COMPLETED
            )
        )
        repairJobDao.insertJobPart(
            JobPartCrossRef(
                jobId = oldJobId,
                inventoryItemId = oldItemId,
                quantityUsed = 2,
                unitPriceAtTimeOfUse = 2.0
            )
        )

        // Construct a new backup dataset to restore
        val newBackup = FieldKitBackup(
            backupVersion = 1,
            exportedAt = "2026-11-06T10:00:00Z",
            inventoryItems = listOf(
                InventoryItemEntity(
                    id = 10L,
                    name = "Restored Screen",
                    sku = "REST-SCR",
                    category = "Screens",
                    quantity = 25,
                    minimumStock = 4,
                    purchasePrice = 40.0,
                    sellingPrice = 90.0
                )
            ),
            repairJobs = listOf(
                RepairJobEntity(
                    id = 20L,
                    customerName = "Restored Customer",
                    customerPhone = "999-8888",
                    deviceName = "Pixel 8",
                    problemDescription = "OLED Replacement",
                    status = JobStatus.WAITING_FOR_PARTS,
                    estimatedCost = 130.0,
                    finalCost = 0.0,
                    createdAt = 5000L,
                    updatedAt = 6000L
                )
            ),
            jobParts = listOf(
                JobPartCrossRef(
                    jobId = 20L,
                    inventoryItemId = 10L,
                    quantityUsed = 1,
                    unitPriceAtTimeOfUse = 90.0
                )
            )
        )

        val result = backupRepository.restoreBackup(newBackup)
        assertTrue(result.isSuccess)

        // Verify old records are gone
        val allItems = inventoryItemDao.getAllInventoryItemsSync()
        val allJobs = repairJobDao.getAllJobsSync()
        val allParts = repairJobDao.getAllJobPartsSync()

        assertEquals(1, allItems.size)
        assertEquals(10L, allItems[0].id)
        assertEquals("Restored Screen", allItems[0].name)
        assertEquals("REST-SCR", allItems[0].sku)
        assertEquals(25, allItems[0].quantity)

        assertEquals(1, allJobs.size)
        assertEquals(20L, allJobs[0].id)
        assertEquals("Restored Customer", allJobs[0].customerName)
        assertEquals(JobStatus.WAITING_FOR_PARTS, allJobs[0].status)

        assertEquals(1, allParts.size)
        assertEquals(20L, allParts[0].jobId)
        assertEquals(10L, allParts[0].inventoryItemId)
        assertEquals(90.0, allParts[0].unitPriceAtTimeOfUse, 0.001)
    }

    @Test
    fun restoreBackupAtomicityRollsBackOnFailure() = runBlocking {
        // Initial database state
        val originalItemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Initial Item",
                sku = "INIT-01",
                category = "General",
                quantity = 100,
                minimumStock = 10,
                purchasePrice = 5.0,
                sellingPrice = 10.0
            )
        )
        val originalJobId = repairJobDao.insertJob(
            RepairJobEntity(
                customerName = "Initial Customer",
                customerPhone = "555-0000",
                deviceName = "Original Phone",
                problemDescription = "Needs checkup",
                status = JobStatus.RECEIVED
            )
        )
        repairJobDao.insertJobPart(
            JobPartCrossRef(
                jobId = originalJobId,
                inventoryItemId = originalItemId,
                quantityUsed = 5,
                unitPriceAtTimeOfUse = 10.0
            )
        )

        // Create a flawed backup:
        // inventoryItems has duplicate primary keys or duplicate SKUs
        // which triggers an SQLiteConstraintException during insertAll
        val failingBackup = FieldKitBackup(
            backupVersion = 1,
            exportedAt = "2026-11-06T10:00:00Z",
            inventoryItems = listOf(
                InventoryItemEntity(id = 50L, name = "Item 1", sku = "DUP-SKU", category = "A", quantity = 1, minimumStock = 0, purchasePrice = 1.0, sellingPrice = 2.0),
                InventoryItemEntity(id = 51L, name = "Item 2", sku = "DUP-SKU", category = "B", quantity = 1, minimumStock = 0, purchasePrice = 1.0, sellingPrice = 2.0)
            ),
            repairJobs = listOf(
                RepairJobEntity(id = 60L, customerName = "New Guy", customerPhone = "123", deviceName = "D", problemDescription = "P")
            ),
            jobParts = emptyList()
        )

        val result = backupRepository.restoreBackup(failingBackup)
        assertTrue(result.isFailure)

        // Verify that the database was ROLLED BACK and remains 100% UNCHANGED
        val itemsAfterFailedRestore = inventoryItemDao.getAllInventoryItemsSync()
        val jobsAfterFailedRestore = repairJobDao.getAllJobsSync()
        val partsAfterFailedRestore = repairJobDao.getAllJobPartsSync()

        assertEquals("Original item count must remain unchanged", 1, itemsAfterFailedRestore.size)
        assertEquals("Original item ID must be preserved", originalItemId, itemsAfterFailedRestore[0].id)
        assertEquals("Initial Item", itemsAfterFailedRestore[0].name)
        assertEquals(100, itemsAfterFailedRestore[0].quantity)

        assertEquals("Original job count must remain unchanged", 1, jobsAfterFailedRestore.size)
        assertEquals("Original job ID must be preserved", originalJobId, jobsAfterFailedRestore[0].id)
        assertEquals("Initial Customer", jobsAfterFailedRestore[0].customerName)

        assertEquals("Original part count must remain unchanged", 1, partsAfterFailedRestore.size)
        assertEquals(originalJobId, partsAfterFailedRestore[0].jobId)
        assertEquals(originalItemId, partsAfterFailedRestore[0].inventoryItemId)
    }
}

