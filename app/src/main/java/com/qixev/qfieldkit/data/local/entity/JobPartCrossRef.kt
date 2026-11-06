package com.qixev.qfieldkit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_parts",
    foreignKeys = [
        ForeignKey(
            entity = RepairJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryItemId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["jobId"]),
        Index(value = ["inventoryItemId"])
    ]
)
data class JobPartCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val jobId: Long,
    val inventoryItemId: Long,
    val quantityUsed: Int,
    val unitPriceAtTimeOfUse: Double,
    val usedAt: Long = 0L,
    val location: String? = null,
    val purpose: String? = null
)

