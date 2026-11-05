package com.qixev.qfieldkit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [Index(value = ["sku"], unique = true)]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val sku: String,
    val category: String,
    val quantity: Int,
    val minimumStock: Int,
    val purchasePrice: Double,
    val sellingPrice: Double
)
