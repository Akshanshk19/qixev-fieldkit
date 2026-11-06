package com.qixev.qfieldkit.data.backup.model

sealed interface BackupValidationResult {
    data class Success(val backup: FieldKitBackup) : BackupValidationResult
    data class Error(val message: String) : BackupValidationResult
}

