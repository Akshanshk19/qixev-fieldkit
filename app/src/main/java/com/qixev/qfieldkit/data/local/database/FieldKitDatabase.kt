package com.qixev.qfieldkit.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qixev.qfieldkit.data.local.dao.InventoryItemDao
import com.qixev.qfieldkit.data.local.dao.RepairJobDao
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity

@Database(
    entities = [
        InventoryItemEntity::class,
        RepairJobEntity::class,
        JobPartCrossRef::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(FieldKitConverters::class)
abstract class FieldKitDatabase : RoomDatabase() {

    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun repairJobDao(): RepairJobDao

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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
