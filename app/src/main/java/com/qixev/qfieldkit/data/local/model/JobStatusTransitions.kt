package com.qixev.qfieldkit.data.local.model

import com.qixev.qfieldkit.data.local.entity.JobStatus

object JobStatusTransitions {

    fun isValidTransition(from: JobStatus, to: JobStatus): Boolean {
        if (from == to) return true

        return when (from) {
            JobStatus.RECEIVED -> to in setOf(
                JobStatus.IN_PROGRESS,
                JobStatus.CANCELLED
            )
            JobStatus.IN_PROGRESS -> to in setOf(
                JobStatus.WAITING_FOR_PARTS,
                JobStatus.COMPLETED,
                JobStatus.CANCELLED
            )
            JobStatus.WAITING_FOR_PARTS -> to in setOf(
                JobStatus.IN_PROGRESS,
                JobStatus.CANCELLED
            )
            JobStatus.COMPLETED -> to in setOf(
                JobStatus.DELIVERED,
                JobStatus.CANCELLED
            )
            JobStatus.DELIVERED -> false // Terminal state
            JobStatus.CANCELLED -> false // Terminal state
        }
    }
}

