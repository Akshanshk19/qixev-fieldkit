package com.qixev.qfieldkit.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qixev.qfieldkit.data.local.dao.InventoryItemDao
import com.qixev.qfieldkit.data.local.dao.RepairJobDao
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef
import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
import com.qixev.qfieldkit.data.repository.OperationResult
import com.qixev.qfieldkit.data.repository.RepairJobError
import com.qixev.qfieldkit.data.repository.RepairJobRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RepairJobRepositoryTest {

    private lateinit var database: FieldKitDatabase
    private lateinit var repairJobDao: RepairJobDao
    private lateinit var inventoryItemDao: InventoryItemDao
    private lateinit var repository: RepairJobRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            FieldKitDatabase::class.java
        ).allowMainThreadQueries().build()

        repairJobDao = database.repairJobDao()
        inventoryItemDao = database.inventoryItemDao()
        repository = RepairJobRepository(database, repairJobDao, inventoryItemDao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun createAndRetrieveJob() = runBlocking {
        val result = repository.createJob(
            customerName = "Alice Brown",
            customerPhone = "+1 555-0123",
            deviceName = "MacBook Air M1",
            problemDescription = "No display backlight",
            estimatedCost = 150.0
        )

        assertTrue(result.isSuccess)
        val jobId = (result as OperationResult.Success).data
        assertTrue(jobId > 0)

        val retrieved = repairJobDao.getJobById(jobId)
        assertNotNull(retrieved)
        assertEquals("Alice Brown", retrieved?.customerName)
        assertEquals(JobStatus.RECEIVED, retrieved?.status)
        assertEquals(150.0, retrieved?.estimatedCost ?: 0.0, 0.001)
    }

    @Test
    fun addPart_decrementsStockAndCreatesJobPartCrossRef() = runBlocking {
        // Setup inventory: Screen stock = 10, price = 75.0
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "MacBook Display",
                sku = "MB-DISP-M1",
                category = "Screens",
                quantity = 10,
                minimumStock = 2,
                purchasePrice = 40.0,
                sellingPrice = 75.0
            )
        )

        val jobId = (repository.createJob(
            customerName = "Bob Smith",
            customerPhone = "555-4321",
            deviceName = "MacBook Air",
            problemDescription = "Display broken"
        ) as OperationResult.Success).data

        // Add 3 parts
        val addResult = repository.addPartToJob(jobId, itemId, quantity = 3)
        assertTrue(addResult.isSuccess)

        // Verify inventory decreased to 7
        val updatedItem = inventoryItemDao.getInventoryItemById(itemId)
        assertEquals(7, updatedItem?.quantity)

        // Verify job part record
        val jobPart = repairJobDao.getJobPart(jobId, itemId)
        assertNotNull(jobPart)
        assertEquals(3, jobPart?.quantityUsed)
        assertEquals(75.0, jobPart?.unitPriceAtTimeOfUse ?: 0.0, 0.001)
    }

    @Test
    fun addPart_duplicateItem_incrementsQuantityAndPreservesHistoricalPrice() = runBlocking {
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Thermal Pad",
                sku = "TP-01",
                category = "Consumables",
                quantity = 20,
                minimumStock = 5,
                purchasePrice = 1.0,
                sellingPrice = 5.0
            )
        )

        val jobId = (repository.createJob(
            customerName = "Charlie",
            customerPhone = "555-7890",
            deviceName = "Dell XPS",
            problemDescription = "Overheating"
        ) as OperationResult.Success).data

        // First addition: 2 units at $5.0
        repository.addPartToJob(jobId, itemId, quantity = 2)

        // Change inventory catalog price to $8.0 (simulating price change next week)
        val currentItem = inventoryItemDao.getInventoryItemById(itemId)!!
        inventoryItemDao.updateItem(currentItem.copy(sellingPrice = 8.0))

        // Second addition of same part: 3 more units
        val secondAdd = repository.addPartToJob(jobId, itemId, quantity = 3)
        assertTrue(secondAdd.isSuccess)

        // Inventory should decrease from 20 -> 18 -> 15
        val finalItem = inventoryItemDao.getInventoryItemById(itemId)
        assertEquals(15, finalItem?.quantity)

        // Job part quantity should be 2 + 3 = 5, price must remain initial historical $5.0
        val jobPart = repairJobDao.getJobPart(jobId, itemId)
        assertEquals(5, jobPart?.quantityUsed)
        assertEquals(5.0, jobPart?.unitPriceAtTimeOfUse ?: 0.0, 0.001)
    }

    @Test
    fun addPart_insufficientStock_failsAndRollsBack() = runBlocking {
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Rare Chipset",
                sku = "CHIP-RARE",
                category = "IC",
                quantity = 2,
                minimumStock = 1,
                purchasePrice = 50.0,
                sellingPrice = 120.0
            )
        )

        val jobId = (repository.createJob(
            customerName = "David",
            customerPhone = "555-9999",
            deviceName = "Motherboard",
            problemDescription = "Short circuit"
        ) as OperationResult.Success).data

        // Attempt to allocate 3 when only 2 exist
        val result = repository.addPartToJob(jobId, itemId, quantity = 3)
        assertTrue(result.isFailure)
        assertTrue((result as OperationResult.Failure).error is RepairJobError.InsufficientStock)

        // Stock must remain intact at 2
        val item = inventoryItemDao.getInventoryItemById(itemId)
        assertEquals(2, item?.quantity)

        // Job parts must be empty
        val jobPart = repairJobDao.getJobPart(jobId, itemId)
        assertNull(jobPart)
    }

    @Test
    fun removePart_restoresEntireQuantityToInventory() = runBlocking {
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Battery",
                sku = "BAT-IP12",
                category = "Batteries",
                quantity = 10,
                minimumStock = 2,
                purchasePrice = 15.0,
                sellingPrice = 40.0
            )
        )

        val jobId = (repository.createJob(
            customerName = "Eve",
            customerPhone = "555-1111",
            deviceName = "iPhone 12",
            problemDescription = "Degraded battery"
        ) as OperationResult.Success).data

        // Add 3 parts: inventory becomes 7
        repository.addPartToJob(jobId, itemId, quantity = 3)
        assertEquals(7, inventoryItemDao.getInventoryItemById(itemId)?.quantity)

        // Remove part from job
        val removeResult = repository.removePartFromJob(jobId, itemId)
        assertTrue(removeResult.isSuccess)

        // Stock restored back to 10
        assertEquals(10, inventoryItemDao.getInventoryItemById(itemId)?.quantity)

        // Job part deleted
        assertNull(repairJobDao.getJobPart(jobId, itemId))
    }

    @Test
    fun cancelJob_restoresAllPartsAndSetsStatusToCancelled() = runBlocking {
        val item1 = inventoryItemDao.insertItem(
            InventoryItemEntity(name = "Part A", sku = "SKU-A", category = "Gen", quantity = 10, minimumStock = 1, purchasePrice = 5.0, sellingPrice = 10.0)
        )
        val item2 = inventoryItemDao.insertItem(
            InventoryItemEntity(name = "Part B", sku = "SKU-B", category = "Gen", quantity = 5, minimumStock = 1, purchasePrice = 8.0, sellingPrice = 20.0)
        )

        val jobId = (repository.createJob(
            customerName = "Frank",
            customerPhone = "555-2222",
            deviceName = "Laptop",
            problemDescription = "Water damage"
        ) as OperationResult.Success).data

        // Allocate 3 of A (remains 7) and 2 of B (remains 3)
        repository.addPartToJob(jobId, item1, quantity = 3)
        repository.addPartToJob(jobId, item2, quantity = 2)

        assertEquals(7, inventoryItemDao.getInventoryItemById(item1)?.quantity)
        assertEquals(3, inventoryItemDao.getInventoryItemById(item2)?.quantity)

        // Cancel the job
        val cancelResult = repository.cancelJob(jobId)
        assertTrue(cancelResult.isSuccess)

        // Stock restored: A back to 10, B back to 5
        assertEquals(10, inventoryItemDao.getInventoryItemById(item1)?.quantity)
        assertEquals(5, inventoryItemDao.getInventoryItemById(item2)?.quantity)

        // Job parts removed
        assertTrue(repairJobDao.getPartsForJob(jobId).isEmpty())

        // Job status is CANCELLED
        val cancelledJob = repairJobDao.getJobById(jobId)
        assertEquals(JobStatus.CANCELLED, cancelledJob?.status)
    }

    @Test
    fun deliveredJob_isLocked_cannotAddOrRemovePartsOrCancel() = runBlocking {
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(name = "Screen", sku = "SCR-001", category = "Screen", quantity = 5, minimumStock = 1, purchasePrice = 20.0, sellingPrice = 50.0)
        )

        val jobId = (repository.createJob(
            customerName = "Grace",
            customerPhone = "555-3333",
            deviceName = "iPad",
            problemDescription = "Glass broken"
        ) as OperationResult.Success).data

        // Add 1 part while open
        repository.addPartToJob(jobId, itemId, quantity = 1)

        // Progress lifecycle: RECEIVED -> IN_PROGRESS -> COMPLETED -> DELIVERED
        repository.updateJobStatus(jobId, JobStatus.IN_PROGRESS)
        repository.updateJobStatus(jobId, JobStatus.COMPLETED)
        val deliverResult = repository.updateJobStatus(jobId, JobStatus.DELIVERED)
        assertTrue(deliverResult.isSuccess)

        // Attempt to add another part: MUST FAIL
        val addFail = repository.addPartToJob(jobId, itemId, quantity = 1)
        assertTrue(addFail.isFailure)
        assertTrue((addFail as OperationResult.Failure).error is RepairJobError.JobLocked)

        // Attempt to remove part: MUST FAIL
        val removeFail = repository.removePartFromJob(jobId, itemId)
        assertTrue(removeFail.isFailure)
        assertTrue((removeFail as OperationResult.Failure).error is RepairJobError.JobLocked)

        // Attempt to cancel: MUST FAIL
        val cancelFail = repository.cancelJob(jobId)
        assertTrue(cancelFail.isFailure)

        // Stock remains 4 (was 5, 1 used)
        assertEquals(4, inventoryItemDao.getInventoryItemById(itemId)?.quantity)
    }

    @Test
    fun foreignKey_deletingInventoryItemUsedInJob_throwsConstraintException() = runBlocking {
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(name = "Connector", sku = "CONN-01", category = "Parts", quantity = 10, minimumStock = 2, purchasePrice = 2.0, sellingPrice = 5.0)
        )

        val jobId = (repository.createJob(
            customerName = "Helen",
            customerPhone = "555-4444",
            deviceName = "Tablet",
            problemDescription = "Port damaged"
        ) as OperationResult.Success).data

        repository.addPartToJob(jobId, itemId, quantity = 1)

        val item = inventoryItemDao.getInventoryItemById(itemId)!!
        try {
            inventoryItemDao.deleteItem(item)
            fail("Expected SQLiteConstraintException due to RESTRICT foreign key")
        } catch (e: SQLiteConstraintException) {
            // Expected RESTRICT foreign key violation
            assertTrue(e.message?.contains("FOREIGN KEY") == true || e.message != null)
        }
    }

    @Test
    fun foreignKey_deletingJob_cascadesJobParts() = runBlocking {
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(name = "Speaker", sku = "SPK-01", category = "Audio", quantity = 5, minimumStock = 1, purchasePrice = 5.0, sellingPrice = 15.0)
        )

        val jobId = (repository.createJob(
            customerName = "Ian",
            customerPhone = "555-5555",
            deviceName = "Phone",
            problemDescription = "Muffled sound"
        ) as OperationResult.Success).data

        repository.addPartToJob(jobId, itemId, quantity = 1)
        assertEquals(1, repairJobDao.getPartsForJob(jobId).size)

        // Delete job directly via DAO
        val job = repairJobDao.getJobById(jobId)!!
        repairJobDao.deleteJob(job)

        // Job parts must be automatically cascade-deleted
        assertTrue(repairJobDao.getPartsForJob(jobId).isEmpty())
    }
}

