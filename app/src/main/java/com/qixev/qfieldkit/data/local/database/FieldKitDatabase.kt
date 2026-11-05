package com.qixev.qfieldkit.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.qixev.qfieldkit.data.local.dao.InventoryItemDao
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity

@Database(
    entities = [InventoryItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FieldKitDatabase : RoomDatabase() {

    abstract fun inventoryItemDao(): InventoryItemDao

    companion object {
        private const val DATABASE_NAME = "fieldkit_database.db"

        @Volatile
        private var INSTANCE: FieldKitDatabase? = null

        fun getInstance(context: Context): FieldKitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FieldKitDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
