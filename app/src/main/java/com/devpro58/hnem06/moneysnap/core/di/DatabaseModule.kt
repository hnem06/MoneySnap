package com.devpro58.hnem06.moneysnap.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import com.devpro58.hnem06.moneysnap.data.local.dao.PaymentMethodDao
import com.devpro58.hnem06.moneysnap.data.local.db.AppDatabase
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
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao =
        database.expenseDao()

    @Provides
    fun providePaymentMethodDao(database: AppDatabase): PaymentMethodDao =
        database.paymentMethodDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
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
}
