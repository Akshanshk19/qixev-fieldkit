package com.qixev.qfieldkit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qixev.qfieldkit.data.backup.BackupRepository
import com.qixev.qfieldkit.data.backup.model.BackupValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class SettingsViewModel(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun exportFullBackup(outputStream: OutputStream) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    val backup = backupRepository.createBackup()
                    val json = backupRepository.serializeBackup(backup)
                    outputStream.use { stream ->
                        stream.write(json.toByteArray(StandardCharsets.UTF_8))
                        stream.flush()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "Backup exported successfully (${backup.inventoryItems.size} items, ${backup.repairJobs.size} jobs).",
                            isErrorMessage = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Export failed: ${e.message ?: "Unknown I/O error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun exportInventoryCsv(outputStream: OutputStream) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    val csv = backupRepository.exportInventoryCsv()
                    outputStream.use { stream ->
                        stream.write(csv.toByteArray(StandardCharsets.UTF_8))
                        stream.flush()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "Inventory CSV exported successfully.",
                            isErrorMessage = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "CSV export failed: ${e.message ?: "Unknown I/O error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun onBackupFileSelected(inputStream: InputStream) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    inputStream.use { stream ->
                        stream.bufferedReader(StandardCharsets.UTF_8).readText()
                    }
                }

                when (val result = backupRepository.validateBackupJson(jsonString)) {
                    is BackupValidationResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingRestoreBackup = result.backup,
                                showRestoreConfirmDialog = true
                            )
                        }
                    }
                    is BackupValidationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userMessage = "Backup validation failed: ${result.message}",
                                isErrorMessage = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Failed to read backup file: ${e.message ?: "Unknown read error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun onConfirmRestore() {
        val backup = _uiState.value.pendingRestoreBackup ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showRestoreConfirmDialog = false) }
            val result = withContext(Dispatchers.IO) {
                backupRepository.restoreBackup(backup)
            }
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingRestoreBackup = null,
                        userMessage = "Backup restored successfully (${backup.inventoryItems.size} items, ${backup.repairJobs.size} jobs, ${backup.jobParts.size} parts).",
                        isErrorMessage = false
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Database transaction error"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingRestoreBackup = null,
                        userMessage = "Restore failed: $error",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun onDismissRestoreConfirmDialog() {
        _uiState.update {
            it.copy(
                pendingRestoreBackup = null,
                showRestoreConfirmDialog = false
            )
        }
    }

    fun onClearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    class Factory(
        private val backupRepository: BackupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(backupRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
        }
    }
}

