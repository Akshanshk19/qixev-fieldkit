package com.qixev.qfieldkit.ui.jobs

import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
import com.qixev.qfieldkit.data.local.model.JobPartDetail

data class JobDetailUiState(
    val job: RepairJobEntity? = null,
    val parts: List<JobPartDetail> = emptyList(),
    val availableInventory: List<InventoryItemEntity> = emptyList(),
    val allowedTransitions: List<JobStatus> = emptyList(),
    val isLocked: Boolean = false,
    val isLoading: Boolean = true,
    val isAddPartDialogOpen: Boolean = false,
    val isCancelConfirmationOpen: Boolean = false,
    val partPendingRemoval: JobPartDetail? = null,
    val userMessage: String? = null
)

