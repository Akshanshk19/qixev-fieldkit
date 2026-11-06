package com.qixev.qfieldkit.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
import com.qixev.qfieldkit.data.local.model.JobPartDetail
import com.qixev.qfieldkit.data.local.model.JobStatusTransitions
import com.qixev.qfieldkit.data.repository.InventoryRepository
import com.qixev.qfieldkit.data.repository.OperationResult
import com.qixev.qfieldkit.data.repository.RepairJobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JobDetailViewModel(
    private val jobId: Long,
    private val repairJobRepository: RepairJobRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _isAddPartDialogOpen = MutableStateFlow(false)
    private val _isCancelConfirmationOpen = MutableStateFlow(false)
    private val _partPendingRemoval = MutableStateFlow<JobPartDetail?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<JobDetailUiState> = combine(
        repairJobRepository.getJobById(jobId),
        repairJobRepository.getJobPartDetails(jobId),
        inventoryRepository.getInventoryItems(),
        _isAddPartDialogOpen,
        _isCancelConfirmationOpen,
        _partPendingRemoval,
        _userMessage
    ) { args: Array<Any?> ->
        val job = args[0] as? RepairJobEntity
        @Suppress("UNCHECKED_CAST")
        val parts = args[1] as? List<JobPartDetail> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val inventory = args[2] as? List<InventoryItemEntity> ?: emptyList()
        val isAddPartDialogOpen = args[3] as Boolean
        val isCancelConfirmationOpen = args[4] as Boolean
        val partPendingRemoval = args[5] as? JobPartDetail
        val userMessage = args[6] as? String

        val isLocked = job?.status == JobStatus.DELIVERED || job?.status == JobStatus.CANCELLED
        val allowedTransitions = if (job != null && !isLocked) {
            JobStatus.entries.filter { target ->
                target != job.status &&
                        target != JobStatus.CANCELLED &&
                        JobStatusTransitions.isValidTransition(job.status, target)
            }
        } else {
            emptyList()
        }

        JobDetailUiState(
            job = job,
            parts = parts,
            availableInventory = inventory,
            allowedTransitions = allowedTransitions,
            isLocked = isLocked,
            isLoading = job == null,
            isAddPartDialogOpen = isAddPartDialogOpen,
            isCancelConfirmationOpen = isCancelConfirmationOpen,
            partPendingRemoval = partPendingRemoval,
            userMessage = userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = JobDetailUiState(isLoading = true)
    )

    fun onOpenAddPartDialog() {
        _isAddPartDialogOpen.value = true
    }

    fun onDismissAddPartDialog() {
        _isAddPartDialogOpen.value = false
    }

    fun onOpenCancelConfirmation() {
        _isCancelConfirmationOpen.value = true
    }

    fun onDismissCancelConfirmation() {
        _isCancelConfirmationOpen.value = false
    }

    fun onRequestRemovePart(part: JobPartDetail) {
        _partPendingRemoval.value = part
    }

    fun onDismissRemovePart() {
        _partPendingRemoval.value = null
    }

    fun onClearUserMessage() {
        _userMessage.value = null
    }

    fun transitionStatus(targetStatus: JobStatus) {
        viewModelScope.launch {
            val result = repairJobRepository.updateJobStatus(jobId, targetStatus)
            if (result is OperationResult.Failure) {
                _userMessage.value = result.error.message
            }
        }
    }

    fun addPart(inventoryItemId: Long, quantity: Int) {
        viewModelScope.launch {
            val result = repairJobRepository.addPartToJob(jobId, inventoryItemId, quantity)
            when (result) {
                is OperationResult.Success -> {
                    _isAddPartDialogOpen.value = false
                }
                is OperationResult.Failure -> {
                    _userMessage.value = result.error.message
                }
            }
        }
    }

    fun confirmRemovePart() {
        val part = _partPendingRemoval.value ?: return
        viewModelScope.launch {
            val result = repairJobRepository.removePartFromJob(jobId, part.partRef.inventoryItemId)
            when (result) {
                is OperationResult.Success -> {
                    _partPendingRemoval.value = null
                }
                is OperationResult.Failure -> {
                    _userMessage.value = result.error.message
                }
            }
        }
    }

    fun confirmCancelJob() {
        viewModelScope.launch {
            val result = repairJobRepository.cancelJob(jobId)
            when (result) {
                is OperationResult.Success -> {
                    _isCancelConfirmationOpen.value = false
                }
                is OperationResult.Failure -> {
                    _userMessage.value = result.error.message
                }
            }
        }
    }

    class Factory(
        private val jobId: Long,
        private val repairJobRepository: RepairJobRepository,
        private val inventoryRepository: InventoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(JobDetailViewModel::class.java)) {
                return JobDetailViewModel(jobId, repairJobRepository, inventoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
