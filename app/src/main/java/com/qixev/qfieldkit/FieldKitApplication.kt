package com.qixev.qfieldkit

import android.app.Application
import com.qixev.qfieldkit.data.backup.BackupRepository
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.preferences.OnboardingPreferences
import com.qixev.qfieldkit.data.preferences.SharedPreferencesOnboardingPreferences
import com.qixev.qfieldkit.data.repository.InventoryRepository
import com.qixev.qfieldkit.data.repository.RepairJobRepository

class FieldKitApplication : Application() {

    val database: FieldKitDatabase by lazy {
        FieldKitDatabase.getInstance(this)
    }

    val onboardingPreferences: OnboardingPreferences by lazy {
        SharedPreferencesOnboardingPreferences(this)
    }

    val inventoryRepository: InventoryRepository by lazy {
        InventoryRepository(database.inventoryItemDao())
    }

    val repairJobRepository: RepairJobRepository by lazy {
        RepairJobRepository(
            database = database,
            repairJobDao = database.repairJobDao(),
            inventoryItemDao = database.inventoryItemDao()
        )
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            database = database,
            inventoryItemDao = database.inventoryItemDao(),
            repairJobDao = database.repairJobDao()
        )
    }
}
