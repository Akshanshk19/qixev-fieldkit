package com.qixev.qfieldkit.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.local.database.MIGRATION_2_3
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

            // 2. Open with FieldKitDatabase applying MIGRATION_2_3
            val migratedDb = Room.databaseBuilder(context, FieldKitDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_2_3)
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
