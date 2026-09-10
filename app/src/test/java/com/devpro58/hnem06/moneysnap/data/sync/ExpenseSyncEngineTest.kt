package com.devpro58.hnem06.moneysnap.data.sync

import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import com.devpro58.hnem06.moneysnap.data.local.file.ReceiptImageLocalDataSource
import com.devpro58.hnem06.moneysnap.data.remote.firestore.FirestoreExpenseSource
import com.devpro58.hnem06.moneysnap.data.remote.storage.FirebaseReceiptStorageSource
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptUploadStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the ordering contract in [ExpenseSyncEngine]. Each test here corresponds to a real
 * data-loss bug that shipped in the previous inline implementation.
 */
class ExpenseSyncEngineTest {

    private val expenseDao: ExpenseDao = mockk(relaxed = true)
    private val storage: FirebaseReceiptStorageSource = mockk(relaxed = true)
    private val localReceipts: ReceiptImageLocalDataSource = mockk(relaxed = true)
    private val firestore: FirestoreExpenseSource = mockk(relaxed = true)

    private val engine = ExpenseSyncEngine(expenseDao, storage, localReceipts, firestore)

    private fun expense(
        id: String = EXPENSE_ID,
        syncStatus: ExpenseSyncStatus = ExpenseSyncStatus.PendingUpload,
        localReceiptPath: String? = null,
        remoteReceiptUrl: String? = null,
        receiptUploadStatus: ReceiptUploadStatus = ReceiptUploadStatus.None,
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
        localReceiptPath = localReceiptPath,
        remoteReceiptUrl = remoteReceiptUrl,
        receiptUploadStatus = receiptUploadStatus.name,
        syncStatus = syncStatus.name,
        spentAtMillis = 1_757_462_400_000L,
        createdAtMillis = updatedAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    @Test
    fun `writes to firestore before marking the row synced`() = runTest {
        coEvery { expenseDao.getById(EXPENSE_ID) } returns expense()

        val outcome = engine.pushExpense(EXPENSE_ID)

        assertEquals(ExpenseSyncEngine.Outcome.Success, outcome)
        // The reverse order left rows Synced locally but absent remotely when the process died
        // in between; the next reconcile() then deleted them as stale.
        coVerifyOrder {
            firestore.upsertExpense(any())
            expenseDao.markSynced(EXPENSE_ID, any(), any(), any())
        }
    }

    @Test
    fun `does not mark synced when firestore write fails`() = runTest {
        coEvery { expenseDao.getById(EXPENSE_ID) } returns expense()
        coEvery { firestore.upsertExpense(any()) } throws IllegalStateException("offline")

        val outcome = engine.pushExpense(EXPENSE_ID)

        assertEquals(ExpenseSyncEngine.Outcome.Retry, outcome)
        coVerify(exactly = 0) { expenseDao.markSynced(any(), any(), any(), any()) }
        coVerify {
            expenseDao.updateUploadStatus(
                EXPENSE_ID, any(), ExpenseSyncStatus.Failed.name, any()
            )
        }
    }

    @Test
    fun `marking synced is guarded by the timestamp the push started from`() = runTest {
        coEvery { expenseDao.getById(EXPENSE_ID) } returns expense(updatedAtMillis = 7_777L)

        engine.pushExpense(EXPENSE_ID)

        // A concurrent local edit bumps updatedAtMillis, so this UPDATE matches no rows and the
        // edit stays PendingUpload instead of being silently marked Synced and lost.
        coVerify { expenseDao.markSynced(EXPENSE_ID, any(), any(), 7_777L) }
    }

    @Test
    fun `does not clobber upload status on a device that never held the local file`() = runTest {
        // A row received via sync: it has a remote URL but no local file.
        coEvery { expenseDao.getById(EXPENSE_ID) } returns expense(
            localReceiptPath = null,
            remoteReceiptUrl = "https://example.test/r.jpg",
            receiptUploadStatus = ReceiptUploadStatus.Uploaded
        )

        engine.pushExpense(EXPENSE_ID)

        coVerify {
            expenseDao.markSynced(
                EXPENSE_ID,
                "https://example.test/r.jpg",
                ReceiptUploadStatus.Uploaded.name,
                any()
            )
        }
        coVerify(exactly = 0) { storage.uploadReceipt(any(), any(), any()) }
    }

    @Test
    fun `pending delete cascades remote, storage and local file before dropping the row`() = runTest {
        coEvery { expenseDao.getById(EXPENSE_ID) } returns expense(
            syncStatus = ExpenseSyncStatus.PendingDelete,
            localReceiptPath = "/data/receipts/$EXPENSE_ID.jpg"
        )

        val outcome = engine.pushExpense(EXPENSE_ID)

        assertEquals(ExpenseSyncEngine.Outcome.Success, outcome)
        coVerifyOrder {
            firestore.deleteExpense(USER_ID, EXPENSE_ID)
            storage.deleteReceipt(USER_ID, EXPENSE_ID)
            localReceipts.deleteReceipt("/data/receipts/$EXPENSE_ID.jpg")
            expenseDao.deleteById(EXPENSE_ID)
        }
    }

    @Test
    fun `keeps the row when a deferred delete fails so it can be retried`() = runTest {
        coEvery { expenseDao.getById(EXPENSE_ID) } returns
            expense(syncStatus = ExpenseSyncStatus.PendingDelete)
        coEvery { firestore.deleteExpense(any(), any()) } throws IllegalStateException("offline")

        val outcome = engine.pushExpense(EXPENSE_ID)

        assertEquals(ExpenseSyncEngine.Outcome.Retry, outcome)
        coVerify(exactly = 0) { expenseDao.deleteById(any()) }
    }

    @Test
    fun `uploads the receipt then stores the returned url`() = runTest {
        coEvery { expenseDao.getById(EXPENSE_ID) } returns expense(
            localReceiptPath = "/data/receipts/$EXPENSE_ID.jpg",
            receiptUploadStatus = ReceiptUploadStatus.Queued
        )
        coEvery { storage.uploadReceipt(USER_ID, EXPENSE_ID, any()) } returns REMOTE_URL

        engine.pushExpense(EXPENSE_ID)

        coVerifyOrder {
            storage.uploadReceipt(USER_ID, EXPENSE_ID, "/data/receipts/$EXPENSE_ID.jpg")
            firestore.upsertExpense(any())
            expenseDao.markSynced(EXPENSE_ID, REMOTE_URL, ReceiptUploadStatus.Uploaded.name, any())
        }
    }

    @Test
    fun `treats an already deleted expense as success`() = runTest {
        coEvery { expenseDao.getById(EXPENSE_ID) } returns null

        val outcome = engine.pushExpense(EXPENSE_ID)

        assertEquals(ExpenseSyncEngine.Outcome.Success, outcome)
        coVerify(exactly = 0) { firestore.upsertExpense(any()) }
    }

    private companion object {
        const val EXPENSE_ID = "exp-1"
        const val USER_ID = "user-1"
        const val REMOTE_URL = "https://example.test/receipt.jpg"
    }
}
