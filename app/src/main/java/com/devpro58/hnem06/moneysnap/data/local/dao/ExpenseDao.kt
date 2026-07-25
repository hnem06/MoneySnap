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

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    fun observeById(expenseId: String): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getById(expenseId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getAllByUser(userId: String): List<ExpenseEntity>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE userId = :userId
          AND spentAtMillis >= :startMillis
          AND spentAtMillis < :endMillis
          AND syncStatus != 'PendingDelete'
        """
    )
    suspend fun getTotalForPeriod(userId: String, startMillis: Long, endMillis: Long): Long

    @Upsert
    suspend fun upsert(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: String)

    @Query(
        """
        UPDATE expenses
        SET remoteReceiptUrl = :remoteReceiptUrl,
            receiptUploadStatus = :receiptUploadStatus,
            syncStatus = :syncStatus,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :expenseId
        """
    )
    suspend fun updateSyncState(
        expenseId: String,
        remoteReceiptUrl: String?,
        receiptUploadStatus: String,
        syncStatus: String,
        updatedAtMillis: Long
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
