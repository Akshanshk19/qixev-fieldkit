package com.qixev.qfieldkit.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
import com.qixev.qfieldkit.data.local.model.RepairJobValidationErrors
import com.qixev.qfieldkit.data.local.model.RepairJobValidator
import com.qixev.qfieldkit.data.repository.OperationResult
import com.qixev.qfieldkit.data.repository.RepairJobRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RepairJobListViewModel(
    private val repository: RepairJobRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<JobStatus?>(null)
    private val _isCreateDialogOpen = MutableStateFlow(false)
    private val _validationErrors = MutableStateFlow(RepairJobValidationErrors())
    private val _userMessage = MutableStateFlow<String?>(null)

    private val _navigateToDetailEvent = MutableSharedFlow<Long>()
    val navigateToDetailEvent: SharedFlow<Long> = _navigateToDetailEvent.asSharedFlow()

    val uiState: StateFlow<RepairJobListUiState> = combine(
        repository.getAllJobs(),
        _searchQuery,
        _selectedStatus,
        _isCreateDialogOpen,
        _validationErrors,
        _userMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val jobs = args[0] as List<RepairJobEntity>
        val searchQuery = args[1] as String
        val selectedStatus = args[2] as JobStatus?
        val isCreateDialogOpen = args[3] as Boolean
        val validationErrors = args[4] as RepairJobValidationErrors
        val userMessage = args[5] as String?

        val filtered = RepairJobFilter.filter(jobs, searchQuery, selectedStatus)

        RepairJobListUiState(
            jobs = jobs,
            filteredJobs = filtered,
            searchQuery = searchQuery,
            selectedStatus = selectedStatus,
            isLoading = false,
            isCreateDialogOpen = isCreateDialogOpen,
            validationErrors = validationErrors,
            userMessage = userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = RepairJobListUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onStatusSelected(status: JobStatus?) {
        _selectedStatus.value = status
    }

    fun onOpenCreateDialog() {
        _validationErrors.value = RepairJobValidationErrors()
        _isCreateDialogOpen.value = true
    }

    fun onDismissCreateDialog() {
        _validationErrors.value = RepairJobValidationErrors()
        _isCreateDialogOpen.value = false
    }

    fun onClearUserMessage() {
        _userMessage.value = null
    }

    fun createJob(
        customerName: String,
        customerPhone: String,
        deviceName: String,
        problemDescription: String,
        estimatedCostStr: String
    ) {
        val validation = RepairJobValidator.validate(
            customerName = customerName,
            customerPhone = customerPhone,
            deviceName = deviceName,
            problemDescription = problemDescription,
            estimatedCostStr = estimatedCostStr.ifBlank { "0.0" },
            finalCostStr = "0.0"
        )

        if (!validation.isValid) {
            _validationErrors.value = validation.errors
            return
        }

        viewModelScope.launch {
            val result = repository.createJob(
                customerName = customerName,
                customerPhone = customerPhone,
                deviceName = deviceName,
                problemDescription = problemDescription,
                estimatedCost = validation.parsedEstimatedCost ?: 0.0,
                finalCost = 0.0
            )

            when (result) {
                is OperationResult.Success -> {
                    _isCreateDialogOpen.value = false
                    _validationErrors.value = RepairJobValidationErrors()
                    _navigateToDetailEvent.emit(result.data)
                }
                is OperationResult.Failure -> {
                    _userMessage.value = result.error.message
                }
            }
        }
    }

    class Factory(
        private val repository: RepairJobRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RepairJobListViewModel::class.java)) {
                return RepairJobListViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

