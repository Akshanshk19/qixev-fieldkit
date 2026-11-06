package com.qixev.qfieldkit.data.backup.model

import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity

data class FieldKitBackup(
    val backupVersion: Int = CURRENT_VERSION,
    val exportedAt: String,
    val inventoryItems: List<InventoryItemEntity>,
    val repairJobs: List<RepairJobEntity>,
    val jobParts: List<JobPartCrossRef>
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

