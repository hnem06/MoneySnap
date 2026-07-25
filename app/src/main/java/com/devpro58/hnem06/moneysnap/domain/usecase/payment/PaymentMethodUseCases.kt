package com.devpro58.hnem06.moneysnap.domain.usecase.payment

import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethod
import com.devpro58.hnem06.moneysnap.domain.repository.AuthRepository
import com.devpro58.hnem06.moneysnap.domain.repository.PaymentMethodRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ObservePaymentMethodsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val paymentMethodRepository: PaymentMethodRepository
) {
    operator fun invoke(): Flow<List<PaymentMethod>> =
        flow {
            val userId = authRepository.getCurrentUser()?.id
            if (userId.isNullOrBlank()) {
                emit(emptyList())
                return@flow
            }

            paymentMethodRepository.ensureDefaultPaymentMethods(userId)
            emitAll(paymentMethodRepository.observePaymentMethods(userId))
        }
}

class AddPaymentMethodUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val paymentMethodRepository: PaymentMethodRepository
) {
    suspend operator fun invoke(name: String): PaymentMethod {
        val userId = authRepository.getCurrentUser()?.id ?: error("User is not authenticated")
        paymentMethodRepository.ensureDefaultPaymentMethods(userId)
        return paymentMethodRepository.addPaymentMethod(userId, name)
    }
}

class UpdatePaymentMethodUseCase @Inject constructor(
    private val paymentMethodRepository: PaymentMethodRepository
) {
    suspend operator fun invoke(methodId: String, name: String) =
        paymentMethodRepository.updatePaymentMethod(methodId, name)
}

class DeletePaymentMethodUseCase @Inject constructor(
    private val paymentMethodRepository: PaymentMethodRepository
) {
    suspend operator fun invoke(methodId: String) =
        paymentMethodRepository.deletePaymentMethod(methodId)
}
