package com.devpro58.hnem06.moneysnap.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Sweeps every expense that still owes the backend work and pushes it.
 *
 * This is the safety net for the per-expense [ReceiptUploadWorker]: that one is enqueued at the
 * moment of the edit, so anything that loses its scheduled job — retries exhausted, WorkManager
 * state cleared, app data restored onto a new device — would otherwise never be retried at all.
 * Runs on app start and on a periodic schedule.
 */
@HiltWorker
class PendingExpenseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val syncEngine: ExpenseSyncEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pendingIds = expenseDao.getPendingSyncIds()
        if (pendingIds.isEmpty()) return Result.success()

        Timber.i("Retrying %d pending expense(s)", pendingIds.size)

        // One failure must not abandon the rest of the queue, so push them all and only then
        // decide whether a retry is warranted.
        val anyFailed = pendingIds
            .map { syncEngine.pushExpense(it) }
            .any { it == ExpenseSyncEngine.Outcome.Retry }

        return if (anyFailed) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_PERIODIC_NAME = "pending-expense-sync-periodic"
        const val UNIQUE_ONE_TIME_NAME = "pending-expense-sync-now"
    }
}
