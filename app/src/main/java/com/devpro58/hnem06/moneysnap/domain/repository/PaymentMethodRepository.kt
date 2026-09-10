package com.devpro58.hnem06.moneysnap.domain.repository

import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    fun observePaymentMethods(userId: String): Flow<List<PaymentMethod>>
    suspend fun ensureDefaultPaymentMethods(userId: String)
    suspend fun addPaymentMethod(userId: String, name: String): PaymentMethod
    suspend fun updatePaymentMethod(methodId: String, name: String)
    suspend fun deletePaymentMethod(methodId: String)

    /** Drops this device's cached payment methods for a user. Remote data is untouched. */
    suspend fun clearLocalData(userId: String)
}
