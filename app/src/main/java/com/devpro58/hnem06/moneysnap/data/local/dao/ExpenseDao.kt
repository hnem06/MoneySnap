package com.devpro58.hnem06.moneysnap.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY spentAtMillis DESC, createdAtMillis DESC")
    fun observeByUser(userId: String): Flow<List<ExpenseEntity>>

    /**
     * Same as [observeByUser] but hides rows awaiting a deferred remote delete, so an expense
     * deleted while offline disappears from the UI immediately instead of lingering until the
     * sync worker runs. Mirrors the `PendingDelete` exclusion already in [getTotalForPeriod] —
     * without this, a pending-delete row stays visible while being excluded from the totals.
     */
    @Query(
        """
        SELECT * FROM expenses
        WHERE userId = :userId AND syncStatus != 'PendingDelete'
        ORDER BY spentAtMillis DESC, createdAtMillis DESC
        """
    )
    fun observeVisibleByUser(userId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    fun observeById(expenseId: String): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getById(expenseId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getAllByUser(userId: String): List<ExpenseEntity>

    /**
     * Every row still owing work to the backend, across all users on this device.
     *
     * Nothing used to re-drive these: WorkManager's own database can be cleared by a backup
     * restore or "clear data" on the WorkManager storage, and a request that exhausts its retries
     * is simply dropped. A row could therefore sit `PendingUpload` forever with no scheduled job
     * pointing at it, which looks to the user like an expense that silently failed to save.
     */
    @Query("SELECT id FROM expenses WHERE syncStatus != 'Synced'")
    suspend fun getPendingSyncIds(): List<String>

    @Query("DELETE FROM expenses WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)

    /**
     * Spending in a period. `type = 'Expense'` is essential, not cosmetic: this feeds the budget
     * evaluation, and counting a salary as spending would fire an over-budget alert on payday.
     */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE userId = :userId
          AND type = 'Expense'
          AND spentAtMillis >= :startMillis
          AND spentAtMillis < :endMillis
          AND syncStatus != 'PendingDelete'
        """
    )
    suspend fun getTotalForPeriod(userId: String, startMillis: Long, endMillis: Long): Long

    /** Counts transactions of either direction in a period — logging income also counts as use. */
    @Query(
        """
        SELECT COUNT(*) FROM expenses
        WHERE userId = :userId
          AND spentAtMillis >= :startMillis
          AND spentAtMillis < :endMillis
          AND syncStatus != 'PendingDelete'
        """
    )
    suspend fun countInPeriod(userId: String, startMillis: Long, endMillis: Long): Int

    @Upsert
    suspend fun upsert(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: String)

    /**
     * Marks a row as pushed. Two deliberate differences from [updateUploadStatus]:
     *
     * 1. **[updatedAtMillis] is not touched.** Syncing is not a user edit; bumping the timestamp
     *    made a merely-pushed row look newer than a genuine concurrent edit on another device,
     *    so last-write-wins in `reconcile()` resolved in favour of the device that synced last
     *    rather than the device that edited last.
     * 2. **Guarded by [expectedUpdatedAtMillis].** If the user edited the expense while the
     *    upload was in flight, the row has already been re-marked `PendingUpload` with a newer
     *    timestamp; the guard makes this a no-op instead of silently marking the newer edit
     *    `Synced` and losing it.
     */
    @Query(
        """
        UPDATE expenses
        SET remoteReceiptUrl = :remoteReceiptUrl,
            receiptUploadStatus = :receiptUploadStatus,
            syncStatus = 'Synced'
        WHERE id = :expenseId AND updatedAtMillis = :expectedUpdatedAtMillis
        """
    )
    suspend fun markSynced(
        expenseId: String,
        remoteReceiptUrl: String?,
        receiptUploadStatus: String,
        expectedUpdatedAtMillis: Long
    )

    @Query(
        """
        UPDATE expenses
        SET receiptUploadStatus = :receiptUploadStatus,
            syncStatus = :syncStatus,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :expenseId
        """
    )
    suspend fun updateUploadStatus(
        expenseId: String,
        receiptUploadStatus: String,
        syncStatus: String,
        updatedAtMillis: Long
    )
}
