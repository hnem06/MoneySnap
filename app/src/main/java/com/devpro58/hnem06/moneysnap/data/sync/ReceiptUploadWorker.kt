package com.devpro58.hnem06.moneysnap.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Pushes one expense to the backend. All ordering and failure handling lives in
 * [ExpenseSyncEngine] so the app-start catch-up pass and this worker cannot drift apart.
 */
@HiltWorker
class ReceiptUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncEngine: ExpenseSyncEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val expenseId = inputData.getString(KEY_EXPENSE_ID) ?: return Result.failure()

        return when (syncEngine.pushExpense(expenseId)) {
            ExpenseSyncEngine.Outcome.Success -> Result.success()
            ExpenseSyncEngine.Outcome.Retry -> Result.retry()
        }
    }

    companion object {
        const val KEY_EXPENSE_ID = "expense_id"
    }
}
