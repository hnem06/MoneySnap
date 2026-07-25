package com.devpro58.hnem06.moneysnap.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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

    override fun enqueueExpenseSync(expenseId: String) {
        val request = OneTimeWorkRequestBuilder<ReceiptUploadWorker>()
            .setInputData(workDataOf(ReceiptUploadWorker.KEY_EXPENSE_ID to expenseId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "receipt-upload-$expenseId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
