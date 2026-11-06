package com.qixev.qfieldkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repair_jobs")
data class RepairJobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deviceName: String = "",
    val problemDescription: String = "",
    val status: JobStatus = JobStatus.RECEIVED,
    val estimatedCost: Double = 0.0,
    val finalCost: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayTitle: String
        get() = title.ifBlank { deviceName.ifBlank { "Job #$id" } }

    val displayDescription: String
        get() = description.ifBlank { problemDescription }
}
