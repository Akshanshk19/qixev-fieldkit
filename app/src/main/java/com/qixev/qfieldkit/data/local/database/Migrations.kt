package com.qixev.qfieldkit.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create repair_jobs table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `repair_jobs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `customerName` TEXT NOT NULL,
                `customerPhone` TEXT NOT NULL,
                `deviceName` TEXT NOT NULL,
                `problemDescription` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `estimatedCost` REAL NOT NULL,
                `finalCost` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 2. Create job_parts table with foreign keys and indices
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `job_parts` (
                `jobId` INTEGER NOT NULL,
                `inventoryItemId` INTEGER NOT NULL,
                `quantityUsed` INTEGER NOT NULL,
                `unitPriceAtTimeOfUse` REAL NOT NULL,
                PRIMARY KEY(`jobId`, `inventoryItemId`),
                FOREIGN KEY(`jobId`) REFERENCES `repair_jobs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`inventoryItemId`) REFERENCES `inventory_items`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_job_parts_jobId` ON `job_parts` (`jobId`)"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_job_parts_inventoryItemId` ON `job_parts` (`inventoryItemId`)"
        )
    }
}

