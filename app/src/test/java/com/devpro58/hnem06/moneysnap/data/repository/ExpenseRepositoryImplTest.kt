package com.devpro58.hnem06.moneysnap.data.repository

import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import com.devpro58.hnem06.moneysnap.data.local.file.ReceiptImageLocalDataSource
import com.devpro58.hnem06.moneysnap.data.mapper.toDomain
import com.devpro58.hnem06.moneysnap.data.notification.BudgetAlertNotifier
import com.devpro58.hnem06.moneysnap.data.remote.firestore.FirestoreExpenseSource
import com.devpro58.hnem06.moneysnap.data.sync.ExpenseSyncScheduler
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptUploadStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the two write paths whose sync bookkeeping was wrong, and the guard that stops
 * `reconcile()` from destroying local work.
 */
class ExpenseRepositoryImplTest {

    private val expenseDao: ExpenseDao = mockk(relaxed = true)
    private val localReceipts: ReceiptImageLocalDataSource = mockk(relaxed = true)
    private val firestore: FirestoreExpenseSource = mockk(relaxed = true)
    private val syncScheduler: ExpenseSyncScheduler = mockk(relaxed = true)
    private val budgetAlertNotifier: BudgetAlertNotifier = mockk(relaxed = true)

    private val repository = ExpenseRepositoryImpl(
        expenseDao, localReceipts, firestore, syncScheduler, budgetAlertNotifier
    )

    private fun entity(
        id: String = EXPENSE_ID,
        syncStatus: ExpenseSyncStatus = ExpenseSyncStatus.Synced,
        updatedAtMillis: Long = 1_000L
    ) = ExpenseEntity(
        id = id,
        userId = USER_ID,
        amount = 45_000L,
        currency = "VND",
        title = "Cà phê",
        category = "Food",
        paymentMethod = "Tiền mặt",
        note = null,
        localReceiptPath = null,
        remoteReceiptUrl = null,
        receiptUploadStatus = ReceiptUploadStatus.None.name,
        syncStatus = syncStatus.name,
        spentAtMillis = 1_757_462_400_000L,
        createdAtMillis = updatedAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    @Test
    fun `updateExpense re-flags the row as pending upload`() = runTest {
        // Callers hand back a Synced expense they just edited; if the repository trusted that,
        // hasPendingLocalChanges() would report false and reconcile() could overwrite or delete
        // the edit before it was ever pushed.
        val edited = entity(syncStatus = ExpenseSyncStatus.Synced).toDomain()
        val saved = slot<ExpenseEntity>()
        coEvery { expenseDao.upsert(capture(saved)) } returns Unit

        repository.updateExpense(edited)

        assertEquals(ExpenseSyncStatus.PendingUpload.name, saved.captured.syncStatus)
        assertTrue(
            "updatedAtMillis must advance so last-write-wins favours this edit",
            saved.captured.updatedAtMillis > 1_000L
        )
        coVerify { syncScheduler.enqueueExpenseSync(EXPENSE_ID) }
    }

    @Test
    fun `deleteExpense returns without waiting on firestore`() = runTest {
        coEvery { expenseDao.getById(EXPENSE_ID) } returns entity()

        repository.deleteExpense(EXPENSE_ID)

        // The old implementation awaited a Firestore delete Task, which offline never completes
        // (and never throws), so the call suspended forever and the row stayed on screen.
        coVerify(exactly = 0) { firestore.deleteExpense(any(), any()) }
        coVerify {
            expenseDao.updateUploadStatus(
                EXPENSE_ID, any(), ExpenseSyncStatus.PendingDelete.name, any()
            )
        }
        coVerify { syncScheduler.enqueueExpenseSync(EXPENSE_ID) }
        // The row must survive until the worker has confirmed the remote delete.
        coVerify(exactly = 0) { expenseDao.deleteById(any()) }
    }

    @Test
    fun `deleteExpense is a no-op for an unknown id`() = runTest {
        coEvery { expenseDao.getById(EXPENSE_ID) } returns null

        repository.deleteExpense(EXPENSE_ID)

        coVerify(exactly = 0) { expenseDao.updateUploadStatus(any(), any(), any(), any()) }
        coVerify(exactly = 0) { syncScheduler.enqueueExpenseSync(any()) }
    }

    @Test
    fun `dashboards read the query that hides pending deletes`() = runTest {
        // Guards against a future change reverting to observeByUser: a PendingDelete row would
        // then show on Home and History while getTotalForPeriod already excludes it.
        repository.observeHomeDashboard(USER_ID)
        repository.observeHistory(USER_ID, com.devpro58.hnem06.moneysnap.domain.model.HistoryFilter())

        coVerify(exactly = 2) { expenseDao.observeVisibleByUser(USER_ID) }
        coVerify(exactly = 0) { expenseDao.observeByUser(any()) }
    }

    private companion object {
        const val EXPENSE_ID = "exp-1"
        const val USER_ID = "user-1"
    }
}
