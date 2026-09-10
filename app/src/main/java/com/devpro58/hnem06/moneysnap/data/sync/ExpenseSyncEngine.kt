package com.devpro58.hnem06.moneysnap.data.sync

import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import com.devpro58.hnem06.moneysnap.data.local.file.ReceiptImageLocalDataSource
import com.devpro58.hnem06.moneysnap.data.remote.firestore.FirestoreExpenseSource
import com.devpro58.hnem06.moneysnap.data.remote.storage.FirebaseReceiptStorageSource
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptUploadStatus
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * The single implementation of "push one local expense to the backend".
 *
 * It exists so the ordering rules below live in exactly one place — they were previously inlined
 * in the worker and got them wrong in three separate ways.
 *
 * **Ordering contract:**
 * - The remote write always happens **before** the local row is marked `Synced`. The reverse
 *   order left a window where process death produced a row that was `Synced` locally but absent
 *   remotely, which the next `reconcile()` pass then deleted as a stale local row.
 * - A delete cascades remote-first (Firestore, then Storage, then the local file) and only
 *   removes the Room row last, so an interrupted delete is retried rather than orphaned.
 */
@Singleton
class ExpenseSyncEngine @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val receiptStorageSource: FirebaseReceiptStorageSource,
    private val receiptImageLocalDataSource: ReceiptImageLocalDataSource,
    private val firestoreExpenseSource: FirestoreExpenseSource
) {

    /** Outcome of a push, mapped by callers onto their own retry semantics. */
    enum class Outcome { Success, Retry }

    suspend fun pushExpense(expenseId: String): Outcome {
        // Already deleted locally, or belongs to a signed-out user: nothing to push.
        val expense = expenseDao.getById(expenseId) ?: return Outcome.Success

        if (expense.syncStatus == ExpenseSyncStatus.PendingDelete.name) {
            return runCatching {
                firestoreExpenseSource.deleteExpense(expense.userId, expense.id)
                receiptStorageSource.deleteReceipt(expense.userId, expense.id)
                receiptImageLocalDataSource.deleteReceipt(expense.localReceiptPath)
                expenseDao.deleteById(expense.id)
            }.fold(
                onSuccess = { Outcome.Success },
                onFailure = { error ->
                    Timber.w(error, "Deferred delete failed for %s, will retry", expense.id)
                    Outcome.Retry
                }
            )
        }

        return runCatching {
            var remoteReceiptUrl = expense.remoteReceiptUrl
            // Preserve whatever the row already claims. Defaulting to `None` here used to wipe
            // `Uploaded` on any device that received the expense from sync, because such a row
            // has a remote URL but no local file.
            var receiptStatus = expense.receiptUploadStatus

            val needsUpload = !expense.localReceiptPath.isNullOrBlank() &&
                expense.receiptUploadStatus != ReceiptUploadStatus.Uploaded.name

            if (needsUpload) {
                expenseDao.updateUploadStatus(
                    expenseId = expense.id,
                    receiptUploadStatus = ReceiptUploadStatus.Uploading.name,
                    syncStatus = ExpenseSyncStatus.PendingUpload.name,
                    updatedAtMillis = expense.updatedAtMillis
                )
                remoteReceiptUrl = receiptStorageSource.uploadReceipt(
                    userId = expense.userId,
                    expenseId = expense.id,
                    localReceiptPath = expense.localReceiptPath!!
                )
                receiptStatus = ReceiptUploadStatus.Uploaded.name
            }

            // Remote first — see the ordering contract above.
            firestoreExpenseSource.upsertExpense(
                expense.copy(
                    remoteReceiptUrl = remoteReceiptUrl,
                    receiptUploadStatus = receiptStatus
                )
            )

            // No-op if the user edited the row while the upload was in flight; that edit carries
            // a newer updatedAtMillis and stays PendingUpload for the next run.
            expenseDao.markSynced(
                expenseId = expense.id,
                remoteReceiptUrl = remoteReceiptUrl,
                receiptUploadStatus = receiptStatus,
                expectedUpdatedAtMillis = expense.updatedAtMillis
            )
        }.fold(
            onSuccess = { Outcome.Success },
            onFailure = { error ->
                Timber.w(error, "Push failed for %s, will retry", expense.id)
                expenseDao.updateUploadStatus(
                    expenseId = expense.id,
                    receiptUploadStatus = if (expense.localReceiptPath.isNullOrBlank()) {
                        expense.receiptUploadStatus
                    } else {
                        ReceiptUploadStatus.Failed.name
                    },
                    syncStatus = ExpenseSyncStatus.Failed.name,
                    updatedAtMillis = expense.updatedAtMillis
                )
                Outcome.Retry
            }
        )
    }
}
