package com.devpro58.hnem06.moneysnap.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the `payment_methods` table. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `payment_methods` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `builtInKey` TEXT,
                `createdAtMillis` INTEGER NOT NULL,
                `updatedAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_methods_userId` ON `payment_methods` (`userId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_methods_userId_name` ON `payment_methods` (`userId`, `name`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_methods_userId_builtInKey` ON `payment_methods` (`userId`, `builtInKey`)")
    }
}

/**
 * Adds income support: every expense gains a direction.
 *
 * `DEFAULT 'Expense'` backfills every existing row correctly rather than approximately — the app
 * only recorded spending before this version, so there is no ambiguity. The default must match the
 * `@ColumnInfo(defaultValue = ...)` on [com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity]
 * exactly, or schema validation fails.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'Expense'")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_expenses_userId_type_spentAtMillis` " +
                "ON `expenses` (`userId`, `type`, `spentAtMillis`)"
        )
    }
}
