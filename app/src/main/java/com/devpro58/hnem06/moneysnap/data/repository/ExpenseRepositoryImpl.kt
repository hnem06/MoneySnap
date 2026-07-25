package com.devpro58.hnem06.moneysnap.data.repository

import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import com.devpro58.hnem06.moneysnap.data.local.file.ReceiptImageLocalDataSource
import com.devpro58.hnem06.moneysnap.data.mapper.toDomain
import com.devpro58.hnem06.moneysnap.data.mapper.toEntity
import com.devpro58.hnem06.moneysnap.data.notification.BudgetAlertNotifier
import com.devpro58.hnem06.moneysnap.data.remote.firestore.FirestoreExpenseSource
import com.devpro58.hnem06.moneysnap.data.sync.ExpenseSyncScheduler
import com.devpro58.hnem06.moneysnap.domain.model.AddExpenseInput
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.devpro58.hnem06.moneysnap.domain.model.HistoryFilter
import com.devpro58.hnem06.moneysnap.domain.model.HomeDashboard
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptUploadStatus
import com.devpro58.hnem06.moneysnap.domain.repository.ExpenseRepository
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val receiptImageLocalDataSource: ReceiptImageLocalDataSource,
    private val firestoreExpenseSource: FirestoreExpenseSource,
    private val syncScheduler: ExpenseSyncScheduler,
    private val budgetAlertNotifier: BudgetAlertNotifier
) : ExpenseRepository {

    override fun observeHomeDashboard(userId: String): Flow<HomeDashboard> =
        expenseDao.observeByUser(userId).map { entities ->
            val expenses = entities.map { it.toDomain() }
            val now = System.currentTimeMillis()
            val monthStart = startOfMonth(now)
            val nextMonthStart = startOfNextMonth(now)
            val todayStart = startOfDay(now)
            val tomorrowStart = todayStart + ONE_DAY_MILLIS

            HomeDashboard(
                monthlyTotal = expenses
                    .filter { it.spentAtMillis in monthStart until nextMonthStart }
                    .sumOf { it.amount },
                todayTotal = expenses
                    .filter { it.spentAtMillis in todayStart until tomorrowStart }
                    .sumOf { it.amount },
                monthlyExpenseCount = expenses.count {
                    it.spentAtMillis in monthStart until nextMonthStart
                },
                recentExpenses = expenses.take(6)
            )
        }

    override fun observeHistory(userId: String, filter: HistoryFilter): Flow<List<Expense>> =
        expenseDao.observeByUser(userId).map { entities ->
            entities.map { it.toDomain() }
                .filter { expense ->
                    filter.category == null || expense.category == filter.category
                }
                .filter { expense ->
                    val query = filter.query.trim()
                    query.isBlank() ||
                        expense.title.contains(query, ignoreCase = true) ||
                        expense.note?.contains(query, ignoreCase = true) == true
                }
                .filter { expense ->
                    (filter.dateRangeStart == null || expense.spentAtMillis >= filter.dateRangeStart) &&
                        (filter.dateRangeEnd == null || expense.spentAtMillis <= filter.dateRangeEnd)
                }
                .filter { expense ->
                    (filter.amountMin == null || expense.amount >= filter.amountMin) &&
                        (filter.amountMax == null || expense.amount <= filter.amountMax)
                }
        }

    override fun observeExpense(expenseId: String): Flow<Expense?> =
        expenseDao.observeById(expenseId).map { it?.toDomain() }

    override suspend fun addExpense(userId: String, input: AddExpenseInput): Expense {
        val expenseId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val localReceiptPath = receiptImageLocalDataSource.copyReceipt(
            userId = userId,
            expenseId = expenseId,
            sourceUriString = input.receiptSourceUri
        )

        val expense = Expense(
            id = expenseId,
            userId = userId,
            amount = input.amount,
            currency = "VND",
            title = input.title,
            category = input.category,
            paymentMethod = input.paymentMethod,
            note = input.note,
            localReceiptPath = localReceiptPath,
            remoteReceiptUrl = null,
            receiptUploadStatus = if (localReceiptPath == null) {
                ReceiptUploadStatus.None
            } else {
                ReceiptUploadStatus.Queued
            },
            syncStatus = ExpenseSyncStatus.PendingUpload,
            spentAtMillis = input.spentAtMillis,
            createdAtMillis = now,
            updatedAtMillis = now
        )

        expenseDao.upsert(expense.toEntity())
        syncScheduler.enqueueExpenseSync(expense.id)
        runCatching {
            budgetAlertNotifier.notifyIfNeeded(userId, alwaysNotifyOverBudget = true)
        }
        return expense
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.upsert(expense.toEntity())
        syncScheduler.enqueueExpenseSync(expense.id)
        runCatching {
            budgetAlertNotifier.notifyIfNeeded(expense.userId, alwaysNotifyOverBudget = true)
        }
    }

    override suspend fun deleteExpense(expenseId: String) {
        val entity = expenseDao.getById(expenseId) ?: return

        // 1. Delete from Firestore first so it won't come back during sync
        try {
            firestoreExpenseSource.deleteExpense(entity.userId, expenseId)
        } catch (_: Exception) {
            // Offline: mark as PendingDelete so the sync worker handles it later
            expenseDao.updateUploadStatus(
                expenseId = expenseId,
                receiptUploadStatus = entity.receiptUploadStatus,
                syncStatus = ExpenseSyncStatus.PendingDelete.name,
                updatedAtMillis = System.currentTimeMillis()
            )
            syncScheduler.enqueueExpenseSync(expenseId)
            return
        }

        // 2. Clean up local receipt file
        receiptImageLocalDataSource.deleteReceipt(entity.localReceiptPath)

        // 3. Remove from local DB
        expenseDao.deleteById(expenseId)
    }

    override suspend fun retryReceiptUpload(expenseId: String) {
        expenseDao.updateUploadStatus(
            expenseId = expenseId,
            receiptUploadStatus = ReceiptUploadStatus.Queued.name,
            syncStatus = ExpenseSyncStatus.PendingUpload.name,
            updatedAtMillis = System.currentTimeMillis()
        )
        syncScheduler.enqueueExpenseSync(expenseId)
    }

    override suspend fun syncRemoteExpenses(userId: String) {
        firestoreExpenseSource.observeExpenses(userId).collect { remoteExpenses ->
            reconcile(userId, remoteExpenses)
        }
    }

    /**
     * Merges a remote snapshot into the local store (last-write-wins by [ExpenseEntity.updatedAtMillis]).
     * Local rows with unsynced changes are never overwritten or deleted, so a pending upload on this
     * device isn't lost just because it isn't on the server yet.
     */
    private suspend fun reconcile(userId: String, remoteExpenses: List<ExpenseEntity>) {
        val localById = expenseDao.getAllByUser(userId).associateBy { it.id }
        val remoteIds = remoteExpenses.mapTo(mutableSetOf()) { it.id }

        for (remote in remoteExpenses) {
            val local = localById[remote.id]
            when {
                local == null -> expenseDao.upsert(remote)
                local.hasPendingLocalChanges() -> Unit
                remote.updatedAtMillis > local.updatedAtMillis ->
                    expenseDao.upsert(remote.copy(localReceiptPath = local.localReceiptPath))
            }
        }

        for (local in localById.values) {
            if (local.id !in remoteIds && !local.hasPendingLocalChanges()) {
                expenseDao.deleteById(local.id)
            }
        }
        runCatching { budgetAlertNotifier.notifyIfNeeded(userId) }
    }

    private fun ExpenseEntity.hasPendingLocalChanges(): Boolean =
        syncStatus == ExpenseSyncStatus.PendingUpload.name ||
            syncStatus == ExpenseSyncStatus.PendingDelete.name ||
            syncStatus == ExpenseSyncStatus.LocalOnly.name ||
            syncStatus == ExpenseSyncStatus.Failed.name

    private fun startOfDay(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun startOfMonth(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun startOfNextMonth(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = startOfMonth(timestamp)
            add(Calendar.MONTH, 1)
        }.timeInMillis

    private companion object {
        const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
