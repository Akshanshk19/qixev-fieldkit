package com.qixev.qfieldkit.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.local.database.MIGRATION_2_3
import com.qixev.qfieldkit.data.local.database.MIGRATION_3_4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val testDbName = "migration-test.db"

    @Test
    fun migrate2To3_preservesExistingInventoryAndCreatesNewTables() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase(testDbName)

            // 1. Manually create version 2 schema (as it existed in v0.2)
            val v2Helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
                .create(
                    androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(testDbName)
                        .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    """
                                    CREATE TABLE IF NOT EXISTS `inventory_items` (
                                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                        `name` TEXT NOT NULL,
                                        `sku` TEXT NOT NULL,
                                        `category` TEXT NOT NULL,
                                        `quantity` INTEGER NOT NULL,
                                        `minimumStock` INTEGER NOT NULL,
                                        `purchasePrice` REAL NOT NULL,
                                        `sellingPrice` REAL NOT NULL
                                    )
                                    """.trimIndent()
                                )
                                db.execSQL(
                                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_inventory_items_sku` ON `inventory_items` (`sku`)"
                                )
                            }

                            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                        })
                        .build()
                )

            val v2Db = v2Helper.writableDatabase
            v2Db.execSQL(
                """
                INSERT INTO `inventory_items` (`name`, `sku`, `category`, `quantity`, `minimumStock`, `purchasePrice`, `sellingPrice`)
                VALUES ('Existing Screen', 'SCR-EXISTING-1', 'Screens', 7, 2, 45.0, 95.0)
                """.trimIndent()
            )
            v2Db.close()

            // 2. Open with FieldKitDatabase applying MIGRATION_2_3 and MIGRATION_3_4
            val migratedDb = Room.databaseBuilder(context, FieldKitDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()

            // 3. Verify existing inventory survived migration
            val item = migratedDb.inventoryItemDao().getInventoryItemBySku("SCR-EXISTING-1")
            assertTrue("Inventory item should survive migration 2->3", item != null)
            assertEquals("Existing Screen", item?.name)
            assertEquals(7, item?.quantity)
            assertEquals(95.0, item?.sellingPrice ?: 0.0, 0.001)

            // 4. Verify repair_jobs table exists and can be written to
            val jobId = (migratedDb.repairJobDao().insertJob(
                com.qixev.qfieldkit.data.local.entity.RepairJobEntity(
                    customerName = "Post Migration Customer",
                    customerPhone = "12345",
                    deviceName = "Test Device",
                    problemDescription = "Working"
                )
            ))
            assertTrue(jobId > 0)

            // 5. Verify job_parts table exists and works
            migratedDb.repairJobDao().insertJobPart(
                com.qixev.qfieldkit.data.local.entity.JobPartCrossRef(
                    jobId = jobId,
                    inventoryItemId = item!!.id,
                    quantityUsed = 1,
                    unitPriceAtTimeOfUse = 95.0
                )
            )
            val part = migratedDb.repairJobDao().getJobPart(jobId, item.id)
            assertTrue(part != null)
            assertEquals(1, part?.quantityUsed)

            migratedDb.close()
            context.deleteDatabase(testDbName)
        }
    }

    @Test
    fun migrate3To4_addsTitleDescriptionAndInventoryUsageColumns() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val v3TestDbName = "migration-v3-v4-test.db"
            context.deleteDatabase(v3TestDbName)

            // 1. Manually create version 3 schema
            val v3Helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
                .create(
                    androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(v3TestDbName)
                        .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    """
                                    CREATE TABLE IF NOT EXISTS `inventory_items` (
                                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                        `name` TEXT NOT NULL,
                                        `sku` TEXT NOT NULL,
                                        `category` TEXT NOT NULL,
                                        `quantity` INTEGER NOT NULL,
                                        `minimumStock` INTEGER NOT NULL,
                                        `purchasePrice` REAL NOT NULL,
                                        `sellingPrice` REAL NOT NULL
                                    )
                                    """.trimIndent()
                                )
                                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_inventory_items_sku` ON `inventory_items` (`sku`)")

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
                                db.execSQL("CREATE INDEX IF NOT EXISTS `index_job_parts_jobId` ON `job_parts` (`jobId`)")
                                db.execSQL("CREATE INDEX IF NOT EXISTS `index_job_parts_inventoryItemId` ON `job_parts` (`inventoryItemId`)")
                            }

                            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                        })
                        .build()
                )

            val v3Db = v3Helper.writableDatabase
            v3Db.execSQL(
                """
                INSERT INTO `inventory_items` (`id`, `name`, `sku`, `category`, `quantity`, `minimumStock`, `purchasePrice`, `sellingPrice`)
                VALUES (1, 'CAT6 Cable', 'CAB-CAT6', 'Networking', 50, 10, 0.5, 1.5)
                """.trimIndent()
            )
            v3Db.execSQL(
                """
                INSERT INTO `repair_jobs` (`id`, `customerName`, `customerPhone`, `deviceName`, `problemDescription`, `status`, `estimatedCost`, `finalCost`, `createdAt`, `updatedAt`)
                VALUES (10, 'Acme Corp', '555-9000', 'Office Network Setup', 'Run cables to conference room', 'IN_PROGRESS', 250.0, 0.0, 1700000000000, 1700000500000)
                """.trimIndent()
            )
            v3Db.execSQL(
                """
                INSERT INTO `job_parts` (`jobId`, `inventoryItemId`, `quantityUsed`, `unitPriceAtTimeOfUse`)
                VALUES (10, 1, 20, 1.5)
                """.trimIndent()
            )
            v3Db.close()

            // 2. Open with FieldKitDatabase applying MIGRATION_3_4
            val migratedDb = Room.databaseBuilder(context, FieldKitDatabase::class.java, v3TestDbName)
                .addMigrations(MIGRATION_3_4)
                .build()

            // 3. Verify repair_job has title and description populated from deviceName and problemDescription
            val job = migratedDb.repairJobDao().getJobById(10L)
            assertTrue("Job #10 should exist after migration 3->4", job != null)
            assertEquals("Office Network Setup", job?.title)
            assertEquals("Run cables to conference room", job?.description)
            assertEquals("Office Network Setup", job?.displayTitle)
            assertEquals("Run cables to conference room", job?.displayDescription)

            // 4. Verify job_part has usedAt backfilled from updatedAt (1700000500000) and location/purpose are null
            val part = migratedDb.repairJobDao().getJobPart(10L, 1L)
            assertTrue("Job part (10, 1) should exist after migration 3->4", part != null)
            assertEquals(20, part?.quantityUsed)
            assertEquals(1700000500000L, part?.usedAt)
            assertEquals(null, part?.location)
            assertEquals(null, part?.purpose)

            // 5. Verify inserting new work unit with location and purpose
            val newJobId = migratedDb.repairJobDao().insertJob(
                com.qixev.qfieldkit.data.local.entity.RepairJobEntity(
                    title = "Data Center Migration",
                    description = "Rack mounting servers",
                    status = com.qixev.qfieldkit.data.local.entity.JobStatus.RECEIVED
                )
            )
            val part1Id = migratedDb.repairJobDao().insertJobPart(
                com.qixev.qfieldkit.data.local.entity.JobPartCrossRef(
                    jobId = newJobId,
                    inventoryItemId = 1L,
                    quantityUsed = 5,
                    unitPriceAtTimeOfUse = 1.5,
                    usedAt = 1710000000000L,
                    location = "Rack 4B",
                    purpose = "Top of Rack uplink"
                )
            )
            val part2Id = migratedDb.repairJobDao().insertJobPart(
                com.qixev.qfieldkit.data.local.entity.JobPartCrossRef(
                    jobId = newJobId,
                    inventoryItemId = 1L,
                    quantityUsed = 10,
                    unitPriceAtTimeOfUse = 1.5,
                    usedAt = 1710000050000L,
                    location = "Rack 4C",
                    purpose = "Downlink to switch 2"
                )
            )
            assertTrue(part1Id > 0)
            assertTrue(part2Id > 0)
            assertTrue(part1Id != part2Id)

            val partsForNewJob = migratedDb.repairJobDao().getPartsForJob(newJobId)
            assertEquals(2, partsForNewJob.size)
            assertEquals("Rack 4B", partsForNewJob[0].location)
            assertEquals("Top of Rack uplink", partsForNewJob[0].purpose)
            assertEquals("Rack 4C", partsForNewJob[1].location)
            assertEquals("Downlink to switch 2", partsForNewJob[1].purpose)

            migratedDb.close()
            context.deleteDatabase(v3TestDbName)
        }
    }

    @Test
    fun missingMigrationThrowsIllegalStateException_insteadOfDestructiveDrop() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
        val unmigratedDbName = "unmigrated-test.db"
        context.deleteDatabase(unmigratedDbName)

        // Create an older schema (v1)
        val v1Helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
            .create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(unmigratedDbName)
                    .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `inventory_items` (
                                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    `name` TEXT NOT NULL,
                                    `sku` TEXT NOT NULL,
                                    `category` TEXT NOT NULL,
                                    `quantity` INTEGER NOT NULL,
                                    `minimumStock` INTEGER NOT NULL,
                                    `purchasePrice` REAL NOT NULL,
                                    `sellingPrice` REAL NOT NULL
                                )
                                """.trimIndent()
                            )
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    })
                    .build()
            )

        val v1Db = v1Helper.writableDatabase
        v1Db.execSQL(
            "INSERT INTO `inventory_items` (`name`, `sku`, `category`, `quantity`, `minimumStock`, `purchasePrice`, `sellingPrice`) " +
            "VALUES ('Pre-Migration Item', 'SKU-001', 'General', 10, 1, 5.0, 10.0)"
        )
        v1Db.close()

        // Attempt to open with FieldKitDatabase builder (only has MIGRATION_2_3, no v1->v3 migration)
        // Without fallbackToDestructiveMigration, Room MUST throw an IllegalStateException
        var caughtException: Exception? = null
        try {
            val db = Room.databaseBuilder(context, FieldKitDatabase::class.java, unmigratedDbName)
                .addMigrations(MIGRATION_2_3)
                .build()
            db.inventoryItemDao().getAllInventoryItemsSync()
            db.close()
        } catch (e: Exception) {
            caughtException = e
        }

        assertTrue(
            "Expected IllegalStateException due to missing migration without destructive fallback",
            caughtException is IllegalStateException
        )
        assertTrue(
            "Exception message should mention migration",
            caughtException?.message?.contains("migration", ignoreCase = true) == true
        )

        context.deleteDatabase(unmigratedDbName)
        }
    }
}
