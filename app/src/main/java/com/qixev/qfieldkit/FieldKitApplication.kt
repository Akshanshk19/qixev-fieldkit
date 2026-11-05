package com.qixev.qfieldkit

import android.app.Application
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.repository.InventoryRepository

class FieldKitApplication : Application() {

    val database: FieldKitDatabase by lazy {
        FieldKitDatabase.getInstance(this)
    }

    val inventoryRepository: InventoryRepository by lazy {
        InventoryRepository(database.inventoryItemDao())
    }
}

