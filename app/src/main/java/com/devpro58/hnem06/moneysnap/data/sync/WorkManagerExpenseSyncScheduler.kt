package com.devpro58.hnem06.moneysnap.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerExpenseSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ExpenseSyncScheduler {

    private val workManager get() = WorkManager.getInstance(context)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun enqueueExpenseSync(expenseId: String) {
        val request = OneTimeWorkRequestBuilder<ReceiptUploadWorker>()
            .setInputData(workDataOf(ReceiptUploadWorker.KEY_EXPENSE_ID to expenseId))
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_EXPENSE_SYNC)
            .build()

        workManager.enqueueUniqueWork(
            "receipt-upload-$expenseId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun enqueuePendingSync() {
        val request = OneTimeWorkRequestBuilder<PendingExpenseSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_EXPENSE_SYNC)
            .build()

        // KEEP, not REPLACE: this is called on every app start, and REPLACE would cancel a sweep
        // already in flight each time the user reopened the app.
        workManager.enqueueUniqueWork(
            PendingExpenseSyncWorker.UNIQUE_ONE_TIME_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun ensurePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<PendingExpenseSyncWorker>(
            PERIODIC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_EXPENSE_SYNC)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PendingExpenseSyncWorker.UNIQUE_PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun cancelAllSync() {
        workManager.cancelAllWorkByTag(TAG_EXPENSE_SYNC)
    }

    private companion object {
        const val TAG_EXPENSE_SYNC = "expense-sync"
        const val PERIODIC_INTERVAL_HOURS = 6L
    }
}
