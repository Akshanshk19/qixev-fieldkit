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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add title and description to repair_jobs table and backfill from deviceName / problemDescription
        db.execSQL("ALTER TABLE `repair_jobs` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `repair_jobs` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE `repair_jobs` SET `title` = `deviceName`, `description` = `problemDescription` WHERE `title` = ''")

        // 2. Re-create job_parts table with auto-increment 'id' primary key
        // to support multiple distinct usage records of the same item on a work unit.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `job_parts_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `jobId` INTEGER NOT NULL,
                `inventoryItemId` INTEGER NOT NULL,
                `quantityUsed` INTEGER NOT NULL,
                `unitPriceAtTimeOfUse` REAL NOT NULL,
                `usedAt` INTEGER NOT NULL DEFAULT 0,
                `location` TEXT DEFAULT NULL,
                `purpose` TEXT DEFAULT NULL,
                FOREIGN KEY(`jobId`) REFERENCES `repair_jobs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`inventoryItemId`) REFERENCES `inventory_items`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )

        // Historical usedAt is approximated from the job's updatedAt timestamp for legacy pre-v4 data
        db.execSQL(
            """
            INSERT INTO `job_parts_new` (`jobId`, `inventoryItemId`, `quantityUsed`, `unitPriceAtTimeOfUse`, `usedAt`, `location`, `purpose`)
            SELECT `jobId`, `inventoryItemId`, `quantityUsed`, `unitPriceAtTimeOfUse`,
                   COALESCE((SELECT `updatedAt` FROM `repair_jobs` WHERE `repair_jobs`.`id` = `job_parts`.`jobId`), 0),
                   NULL, NULL
            FROM `job_parts`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `job_parts`")
        db.execSQL("ALTER TABLE `job_parts_new` RENAME TO `job_parts`")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_job_parts_jobId` ON `job_parts` (`jobId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_job_parts_inventoryItemId` ON `job_parts` (`inventoryItemId`)")
    }
}

