package com.devpro58.hnem06.moneysnap.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devpro58.hnem06.moneysnap.core.di.DatabaseModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates the hand-written [DatabaseModule.MIGRATION_1_2], which shipped unverified.
 *
 * `runMigrationsAndValidate(..., validateDroppedTables = true)` compares the resulting database
 * against the committed `app/schemas/.../2.json`, so a typo in the migration SQL (a wrong column
 * type, a missing index) fails here instead of crashing on a user's device at upgrade time.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_createsPaymentMethodsAndMatchesSchema() {
        helper.createDatabase(TEST_DB, 1).close()

        // Throws if the post-migration schema differs from 2.json in any way.
        helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseModule.MIGRATION_1_2)
    }

    @Test
    fun migrate1To2_preservesExistingExpenses() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO expenses (
                    id, userId, amount, currency, title, category, paymentMethod, note,
                    localReceiptPath, remoteReceiptUrl, receiptUploadStatus, syncStatus,
                    spentAtMillis, createdAtMillis, updatedAtMillis
                ) VALUES (
                    'exp-1', 'user-1', 45000, 'VND', 'Cà phê', 'Food', 'Tiền mặt', NULL,
                    NULL, NULL, 'None', 'Synced', 1757462400000, 1757462400000, 1757462400000
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseModule.MIGRATION_1_2)

        db.query("SELECT userId, amount, title FROM expenses WHERE id = 'exp-1'").use { cursor ->
            assertTrue("migration dropped the pre-existing expense row", cursor.moveToFirst())
            assertEquals("user-1", cursor.getString(0))
            assertEquals(45_000L, cursor.getLong(1))
            // Vietnamese text must survive the migration byte-for-byte.
            assertEquals("Cà phê", cursor.getString(2))
        }

        db.query("SELECT COUNT(*) FROM payment_methods").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
