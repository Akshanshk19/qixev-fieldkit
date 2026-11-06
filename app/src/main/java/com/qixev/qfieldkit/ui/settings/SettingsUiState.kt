package com.qixev.qfieldkit.ui.settings

import com.qixev.qfieldkit.data.backup.model.FieldKitBackup

data class SettingsUiState(
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val isErrorMessage: Boolean = false,
    val pendingRestoreBackup: FieldKitBackup? = null,
    val showRestoreConfirmDialog: Boolean = false
)

