package com.devpro58.hnem06.moneysnap.core.di

import com.devpro58.hnem06.moneysnap.data.repository.ConnectivityRepositoryImpl
import com.devpro58.hnem06.moneysnap.data.repository.ExpenseRepositoryImpl
import com.devpro58.hnem06.moneysnap.data.repository.FirebaseAuthRepository
import com.devpro58.hnem06.moneysnap.data.repository.PaymentMethodRepositoryImpl
import com.devpro58.hnem06.moneysnap.data.repository.SharedPreferencesOnboardingRepository
import com.devpro58.hnem06.moneysnap.data.repository.SharedPreferencesSettingsRepository
import com.devpro58.hnem06.moneysnap.data.receipt.MlKitReceiptRecognitionRepository
import com.devpro58.hnem06.moneysnap.data.sync.ExpenseSyncScheduler
import com.devpro58.hnem06.moneysnap.data.sync.WorkManagerExpenseSyncScheduler
import com.devpro58.hnem06.moneysnap.domain.repository.AuthRepository
import com.devpro58.hnem06.moneysnap.domain.repository.ConnectivityRepository
import com.devpro58.hnem06.moneysnap.domain.repository.ExpenseRepository
import com.devpro58.hnem06.moneysnap.domain.repository.OnboardingRepository
import com.devpro58.hnem06.moneysnap.domain.repository.PaymentMethodRepository
import com.devpro58.hnem06.moneysnap.domain.repository.SettingsRepository
import com.devpro58.hnem06.moneysnap.domain.repository.ReceiptRecognitionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        repository: FirebaseAuthRepository
    ): AuthRepository

    @Binds
    abstract fun bindOnboardingRepository(
        repository: SharedPreferencesOnboardingRepository
    ): OnboardingRepository

    @Binds
    abstract fun bindExpenseRepository(
        repository: ExpenseRepositoryImpl
    ): ExpenseRepository

    @Binds
    abstract fun bindPaymentMethodRepository(
        repository: PaymentMethodRepositoryImpl
    ): PaymentMethodRepository

    @Binds
    abstract fun bindExpenseSyncScheduler(
        scheduler: WorkManagerExpenseSyncScheduler
    ): ExpenseSyncScheduler

    @Binds
    abstract fun bindSettingsRepository(
        repository: SharedPreferencesSettingsRepository
    ): SettingsRepository

    @Binds
    abstract fun bindReceiptRecognitionRepository(
        repository: MlKitReceiptRecognitionRepository
    ): ReceiptRecognitionRepository

    @Binds
    abstract fun bindConnectivityRepository(
        repository: ConnectivityRepositoryImpl
    ): ConnectivityRepository
}
