package com.qixev.qfieldkit.data.backup

import androidx.room.withTransaction
import com.qixev.qfieldkit.data.backup.csv.InventoryCsvExporter
import com.qixev.qfieldkit.data.backup.model.BackupValidationResult
import com.qixev.qfieldkit.data.backup.model.FieldKitBackup
import com.qixev.qfieldkit.data.backup.serializer.FieldKitBackupSerializer
import com.qixev.qfieldkit.data.backup.validator.BackupValidator
import com.qixev.qfieldkit.data.local.dao.InventoryItemDao
import com.qixev.qfieldkit.data.local.dao.RepairJobDao
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import java.time.Instant

class BackupRepository(
    private val database: FieldKitDatabase,
    private val inventoryItemDao: InventoryItemDao,
    private val repairJobDao: RepairJobDao
) {

    suspend fun createBackup(): FieldKitBackup {
        val items = inventoryItemDao.getAllInventoryItemsSync()
        val jobs = repairJobDao.getAllJobsSync()
        val parts = repairJobDao.getAllJobPartsSync()
        val timestamp = Instant.now().toString()

        return FieldKitBackup(
            backupVersion = FieldKitBackup.CURRENT_VERSION,
            exportedAt = timestamp,
            inventoryItems = items,
            repairJobs = jobs,
            jobParts = parts
        )
    }

    fun serializeBackup(backup: FieldKitBackup): String {
        return FieldKitBackupSerializer.serialize(backup)
    }

    fun validateBackupJson(jsonString: String): BackupValidationResult {
        return BackupValidator.validate(jsonString)
    }

    suspend fun restoreBackup(backup: FieldKitBackup): Result<Unit> {
        return runCatching {
            database.withTransaction {
                // Delete in referential-integrity order
                repairJobDao.deleteAllJobParts()
                repairJobDao.deleteAllJobs()
                inventoryItemDao.deleteAll()

                // Insert in parent-first dependency order
                inventoryItemDao.insertAll(backup.inventoryItems)
                repairJobDao.insertAllJobs(backup.repairJobs)
                repairJobDao.insertAllJobParts(backup.jobParts)
            }
        }
    }

    suspend fun exportInventoryCsv(): String {
        val items = inventoryItemDao.getAllInventoryItemsSync()
        return InventoryCsvExporter.export(items)
    }
}

