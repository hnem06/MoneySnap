package com.devpro58.hnem06.moneysnap.domain.repository

import com.devpro58.hnem06.moneysnap.domain.model.AddExpenseInput
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.HistoryFilter
import com.devpro58.hnem06.moneysnap.domain.model.HomeDashboard
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeHomeDashboard(userId: String): Flow<HomeDashboard>
    fun observeHistory(userId: String, filter: HistoryFilter): Flow<List<Expense>>
    fun observeExpense(expenseId: String): Flow<Expense?>
    suspend fun addExpense(userId: String, input: AddExpenseInput): Expense
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expenseId: String)
    suspend fun retryReceiptUpload(expenseId: String)

    /** Listens to remote expenses for [userId] and reconciles them into the local store.
     *  Suspends indefinitely (realtime listener); cancel the coroutine to stop syncing. */
    suspend fun syncRemoteExpenses(userId: String)

    /** Drops this device's cached copy of a user's expenses and receipt images. Remote data is
     *  untouched — everything is restored on the next sign-in. */
    suspend fun clearLocalData(userId: String)
}
