package com.qixev.qfieldkit

import android.app.Application
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.repository.InventoryRepository
import com.qixev.qfieldkit.data.repository.RepairJobRepository

class FieldKitApplication : Application() {

    val database: FieldKitDatabase by lazy {
        FieldKitDatabase.getInstance(this)
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
}
