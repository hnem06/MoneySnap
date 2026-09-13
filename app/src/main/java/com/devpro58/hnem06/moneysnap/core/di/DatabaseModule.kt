package com.devpro58.hnem06.moneysnap.core.di

import android.content.Context
import androidx.room.Room
import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import com.devpro58.hnem06.moneysnap.data.local.dao.PaymentMethodDao
import com.devpro58.hnem06.moneysnap.data.local.db.AppDatabase
import com.devpro58.hnem06.moneysnap.data.local.db.MIGRATION_1_2
import com.devpro58.hnem06.moneysnap.data.local.db.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "moneysnap.db"
        )
            // Never add fallbackToDestructiveMigration here: it would silently delete a user's
            // expense history on any schema change we forgot to write a migration for.
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao =
        database.expenseDao()

    @Provides
    fun providePaymentMethodDao(database: AppDatabase): PaymentMethodDao =
        database.paymentMethodDao()
}
