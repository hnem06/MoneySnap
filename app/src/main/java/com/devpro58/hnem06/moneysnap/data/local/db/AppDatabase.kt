package com.devpro58.hnem06.moneysnap.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import com.devpro58.hnem06.moneysnap.data.local.dao.PaymentMethodDao
import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import com.devpro58.hnem06.moneysnap.data.local.entity.PaymentMethodEntity

/**
 * Schemas are exported to `app/schemas` (see the `room.schemaLocation` ksp arg) and committed,
 * so every migration is verifiable by MigrationTestHelper. Never turn [exportSchema] off again:
 * without the JSON baseline a hand-written migration cannot be validated.
 */
@Database(
    entities = [ExpenseEntity::class, PaymentMethodEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentMethodDao(): PaymentMethodDao
}
