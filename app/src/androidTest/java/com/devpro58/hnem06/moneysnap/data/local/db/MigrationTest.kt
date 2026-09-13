package com.devpro58.hnem06.moneysnap.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates the hand-written [MIGRATION_1_2], which shipped unverified.
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
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
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

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

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

    @Test
    fun migrate2To3_backfillsExistingRowsAsExpenses() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO expenses (
                    id, userId, amount, currency, title, category, paymentMethod, note,
                    localReceiptPath, remoteReceiptUrl, receiptUploadStatus, syncStatus,
                    spentAtMillis, createdAtMillis, updatedAtMillis
                ) VALUES (
                    'exp-1', 'user-1', 45000, 'VND', 'Cà phê', 'Food', NULL, NULL,
                    NULL, NULL, 'None', 'Synced', 1757462400000, 1757462400000, 1757462400000
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // Every row written before income existed is spending — this is the correct reading, not
        // a lossy default. Getting it wrong would turn a user's entire history into income.
        db.query("SELECT type FROM expenses WHERE id = 'exp-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Expense", cursor.getString(0))
        }
    }

    @Test
    fun migrate2To3_defaultAppliesToRowsInsertedWithoutType() {
        helper.createDatabase(TEST_DB, 2).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // Proves the column default is on the table itself, not just applied during the backfill.
        db.execSQL(
            """
            INSERT INTO expenses (
                id, userId, amount, currency, title, category, paymentMethod, note,
                localReceiptPath, remoteReceiptUrl, receiptUploadStatus, syncStatus,
                spentAtMillis, createdAtMillis, updatedAtMillis
            ) VALUES (
                'exp-2', 'user-1', 1000, 'VND', 'x', 'Food', NULL, NULL,
                NULL, NULL, 'None', 'Synced', 1, 1, 1
            )
            """.trimIndent()
        )
        db.query("SELECT type FROM expenses WHERE id = 'exp-2'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Expense", cursor.getString(0))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
