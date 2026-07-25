package com.devpro58.hnem06.moneysnap.domain.usecase.expense

import com.devpro58.hnem06.moneysnap.domain.model.AddExpenseInput
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.HistoryFilter
import com.devpro58.hnem06.moneysnap.domain.repository.AuthRepository
import com.devpro58.hnem06.moneysnap.domain.repository.ExpenseRepository
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(input: AddExpenseInput): Expense {
        val userId = authRepository.getCurrentUser()?.id ?: error("User is not authenticated")
        return expenseRepository.addExpense(userId, input)
    }
}

class GetHomeDashboardUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke() =
        expenseRepository.observeHomeDashboard(
            authRepository.getCurrentUser()?.id ?: ""
        )
}

class GetExpenseHistoryUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(filter: HistoryFilter) =
        expenseRepository.observeHistory(
            userId = authRepository.getCurrentUser()?.id ?: "",
            filter = filter
        )
}

class GetExpenseDetailUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(expenseId: String) = expenseRepository.observeExpense(expenseId)
}

class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expenseId: String) = expenseRepository.deleteExpense(expenseId)
}

class UpdateExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense) = expenseRepository.updateExpense(expense)
}

class RetryReceiptUploadUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expenseId: String) = expenseRepository.retryReceiptUpload(expenseId)
}

class SyncExpensesUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val expenseRepository: ExpenseRepository
) {
    /** Streams remote expenses into the local store for the signed-in user.
     *  No-op if no one is signed in. Suspends until the coroutine is cancelled. */
    suspend operator fun invoke() {
        val userId = authRepository.getCurrentUser()?.id ?: return
        expenseRepository.syncRemoteExpenses(userId)
    }
}
