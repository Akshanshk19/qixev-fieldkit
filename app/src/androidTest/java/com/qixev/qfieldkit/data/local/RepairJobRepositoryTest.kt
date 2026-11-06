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
    fun sameItemUsedTwiceOnSameJob_createsDistinctUsageRecords() = runBlocking {
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

        // First usage: 2 units at $5.0 on Bench 1
        val firstAdd = repository.addPartToJob(jobId, itemId, quantity = 2, location = "Bench 1", purpose = "CPU repaste")
        assertTrue(firstAdd.isSuccess)

        // Change inventory catalog price to $8.0
        val currentItem = inventoryItemDao.getInventoryItemById(itemId)!!
        inventoryItemDao.updateItem(currentItem.copy(sellingPrice = 8.0))

        // Second usage: 3 more units on Bench 2
        val secondAdd = repository.addPartToJob(jobId, itemId, quantity = 3, location = "Bench 2", purpose = "GPU repaste")
        assertTrue(secondAdd.isSuccess)

        // Inventory must decrease from 20 -> 18 -> 15
        val finalItem = inventoryItemDao.getInventoryItemById(itemId)
        assertEquals(15, finalItem?.quantity)

        // Must be two distinct usage records in job_parts
        val parts = repairJobDao.getPartsForJob(jobId)
        assertEquals(2, parts.size)

        val usage1 = parts[0]
        assertEquals(2, usage1.quantityUsed)
        assertEquals(5.0, usage1.unitPriceAtTimeOfUse, 0.001)
        assertEquals("Bench 1", usage1.location)
        assertEquals("CPU repaste", usage1.purpose)

        val usage2 = parts[1]
        assertEquals(3, usage2.quantityUsed)
        assertEquals(8.0, usage2.unitPriceAtTimeOfUse, 0.001)
        assertEquals("Bench 2", usage2.location)
        assertEquals("GPU repaste", usage2.purpose)
        assertTrue(usage1.id != usage2.id)
    }

    @Test
    fun removingOneUsageRecord_restoresOnlyThatUsageQuantity() = runBlocking {
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Screws M3x6",
                sku = "SCR-M3",
                category = "Fasteners",
                quantity = 50,
                minimumStock = 10,
                purchasePrice = 0.1,
                sellingPrice = 0.5
            )
        )

        val jobId = (repository.createJob(
            title = "Chassis Assembly",
            description = "Assemble mainframe"
        ) as OperationResult.Success).data

        // Usage 1: 4 screws
        repository.addPartToJob(jobId, itemId, quantity = 4, location = "Front panel", purpose = "Bezel mounting")
        // Usage 2: 6 screws
        repository.addPartToJob(jobId, itemId, quantity = 6, location = "Back panel", purpose = "PSU bracket")

        // Stock: 50 - 4 - 6 = 40
        assertEquals(40, inventoryItemDao.getInventoryItemById(itemId)?.quantity)

        val parts = repairJobDao.getPartsForJob(jobId)
        assertEquals(2, parts.size)
        val usageToRemove = parts.first { it.location == "Front panel" }

        // Remove only usage 1 (4 screws)
        val removeResult = repository.removeUsageRecord(usageToRemove.id)
        assertTrue(removeResult.isSuccess)

        // Stock restored only by 4: 40 + 4 = 44
        assertEquals(44, inventoryItemDao.getInventoryItemById(itemId)?.quantity)

        // Only 1 usage record remains
        val remainingParts = repairJobDao.getPartsForJob(jobId)
        assertEquals(1, remainingParts.size)
        assertEquals("Back panel", remainingParts[0].location)
        assertEquals(6, remainingParts[0].quantityUsed)
    }

    @Test
    fun cancellingWorkUnit_restoresTotalQuantityExactlyOnce() = runBlocking {
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Stand-off M3",
                sku = "STND-M3",
                category = "Fasteners",
                quantity = 30,
                minimumStock = 5,
                purchasePrice = 0.2,
                sellingPrice = 0.8
            )
        )

        val jobId = (repository.createJob(
            title = "Motherboard Mount Project",
            description = "Install stand-offs"
        ) as OperationResult.Success).data

        // Add 5, then add 7
        repository.addPartToJob(jobId, itemId, quantity = 5)
        repository.addPartToJob(jobId, itemId, quantity = 7)

        // Stock: 30 - 5 - 7 = 18
        assertEquals(18, inventoryItemDao.getInventoryItemById(itemId)?.quantity)

        // Cancel job
        val cancelResult = repository.cancelJob(jobId)
        assertTrue(cancelResult.isSuccess)

        // Total quantity restored exactly once: 18 + 5 + 7 = 30
        assertEquals(30, inventoryItemDao.getInventoryItemById(itemId)?.quantity)
        assertTrue(repairJobDao.getPartsForJob(jobId).isEmpty())

        // Calling cancelJob again (idempotent) must NOT re-credit inventory
        val secondCancel = repository.cancelJob(jobId)
        assertTrue(secondCancel.isSuccess)
        assertEquals(30, inventoryItemDao.getInventoryItemById(itemId)?.quantity)
    }

    @Test
    fun stockCannotBecomeNegative_insufficientStockRejectedAndRolledBack() = runBlocking {
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Power Supply 500W",
                sku = "PSU-500",
                category = "Power",
                quantity = 2,
                minimumStock = 1,
                purchasePrice = 30.0,
                sellingPrice = 65.0
            )
        )

        val jobId = (repository.createJob(
            title = "Workstation Build",
            description = "Build desktop"
        ) as OperationResult.Success).data

        // Request 5 units when only 2 available
        val result = repository.addPartToJob(jobId, itemId, quantity = 5)
        assertTrue(result.isFailure)
        val error = (result as OperationResult.Failure).error
        assertTrue(error is RepairJobError.InsufficientStock)
        assertEquals(2, (error as RepairJobError.InsufficientStock).available)
        assertEquals(5, error.requested)

        // Inventory stock must remain strictly 2, not negative
        val item = inventoryItemDao.getInventoryItemById(itemId)
        assertEquals(2, item?.quantity)
        assertTrue(repairJobDao.getPartsForJob(jobId).isEmpty())
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

    @Test
    fun createJob_withTitleAndDescription_persistsGenericWorkUnit() = runBlocking {
        val result = repository.createJob(
            title = "HVAC Quarterly Maintenance",
            description = "Replace air filters and inspect ducting"
        )
        assertTrue(result.isSuccess)
        val jobId = (result as OperationResult.Success).data

        val retrieved = repairJobDao.getJobById(jobId)
        assertNotNull(retrieved)
        assertEquals("HVAC Quarterly Maintenance", retrieved?.displayTitle)
        assertEquals("Replace air filters and inspect ducting", retrieved?.displayDescription)
    }

    @Test
    fun addPart_withLocationAndPurpose_recordsUsageMetadata() = runBlocking {
        val itemId = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Air Filter 20x20",
                sku = "FLT-2020",
                category = "Filters",
                quantity = 15,
                minimumStock = 3,
                purchasePrice = 8.0,
                sellingPrice = 18.0
            )
        )

        val jobId = (repository.createJob(
            title = "Building A HVAC Overhaul",
            description = "Quarterly system maintenance"
        ) as OperationResult.Success).data

        val beforeTime = System.currentTimeMillis()
        val addResult = repository.addPartToJob(
            jobId = jobId,
            inventoryItemId = itemId,
            quantity = 4,
            location = "Air Handler 3 (Roof)",
            purpose = "Intake filter swap"
        )
        assertTrue(addResult.isSuccess)

        // Check stock decremented
        val item = inventoryItemDao.getInventoryItemById(itemId)
        assertEquals(11, item?.quantity)

        // Check cross ref contains location, purpose, and valid timestamp
        val part = repairJobDao.getJobPart(jobId, itemId)
        assertNotNull(part)
        assertEquals(4, part?.quantityUsed)
        assertEquals(18.0, part?.unitPriceAtTimeOfUse ?: 0.0, 0.001)
        assertEquals("Air Handler 3 (Roof)", part?.location)
        assertEquals("Intake filter swap", part?.purpose)
        assertTrue("usedAt timestamp must be recorded", (part?.usedAt ?: 0L) >= beforeTime)
    }

    @Test
    fun addMultipleParts_toWorkUnit_preservesUsageIndependently() = runBlocking {
        val item1Id = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Cat6 Jack RJ45",
                sku = "JK-RJ45",
                category = "Cabling",
                quantity = 50,
                minimumStock = 10,
                purchasePrice = 1.0,
                sellingPrice = 3.5
            )
        )
        val item2Id = inventoryItemDao.insertItem(
            InventoryItemEntity(
                name = "Faceplate 2-Port",
                sku = "FP-2P",
                category = "Cabling",
                quantity = 25,
                minimumStock = 5,
                purchasePrice = 1.5,
                sellingPrice = 4.0
            )
        )

        val jobId = (repository.createJob(
            title = "Conference Room Network Drop",
            description = "Add 2 RJ45 network jacks"
        ) as OperationResult.Success).data

        repository.addPartToJob(
            jobId = jobId,
            inventoryItemId = item1Id,
            quantity = 2,
            location = "Wall Box East",
            purpose = "Port 1 and Port 2 termination"
        )

        repository.addPartToJob(
            jobId = jobId,
            inventoryItemId = item2Id,
            quantity = 1,
            location = "Wall Box East",
            purpose = "Dual port wall plate"
        )

        val parts = repairJobDao.getPartsForJob(jobId)
        assertEquals(2, parts.size)

        val jackPart = parts.first { it.inventoryItemId == item1Id }
        assertEquals(2, jackPart.quantityUsed)
        assertEquals("Port 1 and Port 2 termination", jackPart.purpose)

        val platePart = parts.first { it.inventoryItemId == item2Id }
        assertEquals(1, platePart.quantityUsed)
        assertEquals("Dual port wall plate", platePart.purpose)

        // Verify inventory balances
        assertEquals(48, inventoryItemDao.getInventoryItemById(item1Id)?.quantity)
        assertEquals(24, inventoryItemDao.getInventoryItemById(item2Id)?.quantity)
    }
}

