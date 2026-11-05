package com.qixev.qfieldkit.data.local

import android.content.Context
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
import org.junit.Assert.assertTrue
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
    }
}

