package com.qixev.qfieldkit.data.local.model

import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef

data class JobPartDetail(
    val partRef: JobPartCrossRef,
    val item: InventoryItemEntity
) {
    val totalCost: Double
        get() = partRef.quantityUsed * partRef.unitPriceAtTimeOfUse
}

