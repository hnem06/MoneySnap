package com.devpro58.hnem06.moneysnap.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import com.devpro58.hnem06.moneysnap.data.local.dao.PaymentMethodDao
import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import com.devpro58.hnem06.moneysnap.data.local.entity.PaymentMethodEntity

@Database(
    entities = [ExpenseEntity::class, PaymentMethodEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentMethodDao(): PaymentMethodDao
}
