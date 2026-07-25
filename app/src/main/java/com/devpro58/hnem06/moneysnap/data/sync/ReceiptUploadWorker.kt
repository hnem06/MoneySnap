package com.devpro58.hnem06.moneysnap.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import com.devpro58.hnem06.moneysnap.data.remote.firestore.FirestoreExpenseSource
import com.devpro58.hnem06.moneysnap.data.remote.storage.FirebaseReceiptStorageSource
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptUploadStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReceiptUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val receiptStorageSource: FirebaseReceiptStorageSource,
    private val firestoreExpenseSource: FirestoreExpenseSource
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val expenseId = inputData.getString(KEY_EXPENSE_ID) ?: return Result.failure()
        val expense = expenseDao.getById(expenseId) ?: return Result.success()

        // Handle deferred deletion (marked PendingDelete when offline)
        if (expense.syncStatus == ExpenseSyncStatus.PendingDelete.name) {
            return try {
                firestoreExpenseSource.deleteExpense(expense.userId, expense.id)
                expenseDao.deleteById(expense.id)
                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }

        return try {
            val now = System.currentTimeMillis()
            var remoteReceiptUrl = expense.remoteReceiptUrl
            var receiptStatus = ReceiptUploadStatus.None.name

            if (!expense.localReceiptPath.isNullOrBlank()) {
                expenseDao.updateUploadStatus(
                    expenseId = expense.id,
                    receiptUploadStatus = ReceiptUploadStatus.Uploading.name,
                    syncStatus = ExpenseSyncStatus.PendingUpload.name,
                    updatedAtMillis = now
                )

                remoteReceiptUrl = receiptStorageSource.uploadReceipt(
                    userId = expense.userId,
                    expenseId = expense.id,
                    localReceiptPath = expense.localReceiptPath
                )
                receiptStatus = ReceiptUploadStatus.Uploaded.name
            }

            val syncedAt = System.currentTimeMillis()
            expenseDao.updateSyncState(
                expenseId = expense.id,
                remoteReceiptUrl = remoteReceiptUrl,
                receiptUploadStatus = receiptStatus,
                syncStatus = ExpenseSyncStatus.Synced.name,
                updatedAtMillis = syncedAt
            )

            val syncedExpense = expenseDao.getById(expense.id)
            if (syncedExpense != null) {
                firestoreExpenseSource.upsertExpense(syncedExpense)
            }

            Result.success()
        } catch (exception: Exception) {
            expenseDao.updateUploadStatus(
                expenseId = expense.id,
                receiptUploadStatus = ReceiptUploadStatus.Failed.name,
                syncStatus = ExpenseSyncStatus.Failed.name,
                updatedAtMillis = System.currentTimeMillis()
            )
            Result.retry()
        }
    }

    companion object {
        const val KEY_EXPENSE_ID = "expense_id"
    }
}
