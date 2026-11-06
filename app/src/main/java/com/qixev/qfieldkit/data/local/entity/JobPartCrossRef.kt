package com.qixev.qfieldkit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "job_parts",
    primaryKeys = ["jobId", "inventoryItemId"],
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
    val jobId: Long,
    val inventoryItemId: Long,
    val quantityUsed: Int,
    val unitPriceAtTimeOfUse: Double
)

