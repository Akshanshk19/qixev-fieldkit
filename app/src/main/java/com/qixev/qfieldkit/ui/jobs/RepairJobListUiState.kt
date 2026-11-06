package com.qixev.qfieldkit.ui.jobs

import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
import com.qixev.qfieldkit.data.local.model.RepairJobValidationErrors

data class RepairJobListUiState(
    val jobs: List<RepairJobEntity> = emptyList(),
    val filteredJobs: List<RepairJobEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: JobStatus? = null,
    val isLoading: Boolean = true,
    val isCreateDialogOpen: Boolean = false,
    val validationErrors: RepairJobValidationErrors = RepairJobValidationErrors(),
    val userMessage: String? = null
)

