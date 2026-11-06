package com.qixev.qfieldkit.ui.jobs

import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity

object RepairJobFilter {

    fun filter(
        jobs: List<RepairJobEntity>,
        searchQuery: String,
        selectedStatus: JobStatus?
    ): List<RepairJobEntity> {
        val trimmed = searchQuery.trim().lowercase()

        return jobs.filter { job ->
            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                job.customerName.lowercase().contains(trimmed) ||
                        job.customerPhone.lowercase().contains(trimmed) ||
                        job.deviceName.lowercase().contains(trimmed) ||
                        job.problemDescription.lowercase().contains(trimmed) ||
                        "#${job.id}".contains(trimmed)
            }

            val matchesStatus = if (selectedStatus == null) {
                true
            } else {
                job.status == selectedStatus
            }

            matchesQuery && matchesStatus
        }
    }
}

