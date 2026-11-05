package com.qixev.qfieldkit.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qixev.qfieldkit.data.local.dao.InventoryItemDao
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
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
class InventoryItemDaoTest {

    private lateinit var database: FieldKitDatabase
    private lateinit var inventoryItemDao: InventoryItemDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            FieldKitDatabase::class.java
        ).allowMainThreadQueries().build()
        inventoryItemDao = database.inventoryItemDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndQueryInventoryItem() = runBlocking {
        val item = InventoryItemEntity(
            name = "iPhone 13 Display",
            sku = "IP13-DISP",
            category = "Screens",
            quantity = 5,
            minimumStock = 2,
            purchasePrice = 65.0,
            sellingPrice = 130.0
        )

        val insertedId = inventoryItemDao.insertItem(item)
        assertTrue(insertedId > 0)

        val fetchedItem = inventoryItemDao.getInventoryItemById(insertedId)
        assertNotNull(fetchedItem)
        assertEquals("iPhone 13 Display", fetchedItem?.name)
        assertEquals("IP13-DISP", fetchedItem?.sku)
        assertEquals(5, fetchedItem?.quantity)

        val allItems = inventoryItemDao.getAllInventoryItems().first()
        assertEquals(1, allItems.size)
        assertEquals("iPhone 13 Display", allItems[0].name)
    }

    @Test
    fun duplicateSku_throwsConstraintException() = runBlocking {
        val item1 = InventoryItemEntity(
            name = "Original Display",
            sku = "SKU-UNIQUE-1",
            category = "Screens",
            quantity = 5,
            minimumStock = 1,
            purchasePrice = 50.0,
            sellingPrice = 100.0
        )
        val item2 = InventoryItemEntity(
            name = "Aftermarket Display",
            sku = "SKU-UNIQUE-1", // duplicate SKU
            category = "Screens",
            quantity = 2,
            minimumStock = 1,
            purchasePrice = 30.0,
            sellingPrice = 70.0
        )

        inventoryItemDao.insertItem(item1)
        try {
            inventoryItemDao.insertItem(item2)
            fail("Expected SQLiteConstraintException when inserting duplicate SKU")
        } catch (e: SQLiteConstraintException) {
            // Expected
            assertTrue(e.message?.contains("UNIQUE") == true || e.message != null)
        }
    }

    @Test
    fun updateItem_preservesIdAndUpdatesValues() = runBlocking {
        val item = InventoryItemEntity(
            name = "Old Battery",
            sku = "BAT-OLD",
            category = "Batteries",
            quantity = 10,
            minimumStock = 2,
            purchasePrice = 10.0,
            sellingPrice = 25.0
        )
        val id = inventoryItemDao.insertItem(item)
        val inserted = inventoryItemDao.getInventoryItemById(id)
        assertNotNull(inserted)

        val updated = inserted!!.copy(
            name = "New Battery OEM",
            quantity = 15,
            sellingPrice = 30.0
        )
        inventoryItemDao.updateItem(updated)

        val fetched = inventoryItemDao.getInventoryItemById(id)
        assertEquals("New Battery OEM", fetched?.name)
        assertEquals(15, fetched?.quantity)
        assertEquals(30.0, fetched?.sellingPrice ?: 0.0, 0.001)
    }

    @Test
    fun deleteInventoryItem() = runBlocking {
        val item = InventoryItemEntity(
            name = "Thermal Paste",
            sku = "TP-MX4",
            category = "Consumables",
            quantity = 12,
            minimumStock = 3,
            purchasePrice = 4.0,
            sellingPrice = 9.0
        )

        val insertedId = inventoryItemDao.insertItem(item)
        val insertedItem = inventoryItemDao.getInventoryItemById(insertedId)
        assertNotNull(insertedItem)

        inventoryItemDao.deleteItem(insertedItem!!)
        val allItems = inventoryItemDao.getAllInventoryItems().first()
        assertTrue(allItems.isEmpty())
        assertNull(inventoryItemDao.getInventoryItemById(insertedId))
    }

    @Test
    fun queryBySku() = runBlocking {
        val item = InventoryItemEntity(
            name = "Lightning Cable",
            sku = "CAB-LGHT",
            category = "Cables",
            quantity = 20,
            minimumStock = 5,
            purchasePrice = 2.0,
            sellingPrice = 8.0
        )
        inventoryItemDao.insertItem(item)

        val found = inventoryItemDao.getInventoryItemBySku("CAB-LGHT")
        assertNotNull(found)
        assertEquals("Lightning Cable", found?.name)

        val notFound = inventoryItemDao.getInventoryItemBySku("NONEXISTENT")
        assertNull(notFound)
    }
}
