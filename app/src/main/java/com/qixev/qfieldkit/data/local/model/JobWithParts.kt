package com.qixev.qfieldkit.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity

data class JobWithParts(
    @Embedded
    val job: RepairJobEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "jobId"
    )
    val parts: List<JobPartCrossRef> = emptyList()
)

