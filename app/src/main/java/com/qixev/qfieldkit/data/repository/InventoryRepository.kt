package com.qixev.qfieldkit.data.repository

import com.qixev.qfieldkit.data.local.dao.InventoryItemDao
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val inventoryItemDao: InventoryItemDao
) {
    fun getInventoryItems(): Flow<List<InventoryItemEntity>> {
        return inventoryItemDao.getAllInventoryItems()
    }

    suspend fun getInventoryItemById(id: Long): InventoryItemEntity? {
        return inventoryItemDao.getInventoryItemById(id)
    }

    suspend fun getInventoryItemBySku(sku: String): InventoryItemEntity? {
        return inventoryItemDao.getInventoryItemBySku(sku)
    }

    suspend fun addItem(item: InventoryItemEntity): Long {
        return inventoryItemDao.insertItem(item)
    }

    suspend fun updateItem(item: InventoryItemEntity) {
        inventoryItemDao.updateItem(item)
    }

    suspend fun deleteItem(item: InventoryItemEntity) {
        inventoryItemDao.deleteItem(item)
    }
}
