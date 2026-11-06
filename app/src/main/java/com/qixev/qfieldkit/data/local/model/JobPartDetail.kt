package com.qixev.qfieldkit.data.local.model

import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef

data class JobPartDetail(
    val partRef: JobPartCrossRef,
    val item: InventoryItemEntity
) {
    val usageId: Long
        get() = partRef.id

    val totalCost: Double
        get() = partRef.quantityUsed * partRef.unitPriceAtTimeOfUse

    val usedAt: Long
        get() = partRef.usedAt

    val location: String?
        get() = partRef.location

    val purpose: String?
        get() = partRef.purpose
}

