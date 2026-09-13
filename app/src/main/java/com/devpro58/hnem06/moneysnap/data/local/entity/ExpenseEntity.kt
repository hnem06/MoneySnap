package com.devpro58.hnem06.moneysnap.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devpro58.hnem06.moneysnap.domain.model.TransactionType

@Entity(
    tableName = "expenses",
    indices = [
        Index("userId"),
        Index("spentAtMillis"),
        Index("syncStatus"),
        Index("receiptUploadStatus"),
        // Composite index for the hot path: per-user totals within a period, split by direction.
        Index("userId", "type", "spentAtMillis")
    ]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Long,
    val currency: String,
    val title: String,
    val category: String,
    val paymentMethod: String?,
    val note: String?,
    val localReceiptPath: String?,
    val remoteReceiptUrl: String?,
    val receiptUploadStatus: String,
    val syncStatus: String,
    val spentAtMillis: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /**
     * [com.devpro58.hnem06.moneysnap.domain.model.TransactionType] name.
     *
     * The default is declared here AND in the migration SQL on purpose: Room only compares column
     * defaults when the entity declares one, so a mismatch is silently tolerated now but becomes a
     * hard IllegalStateException the first time anyone adds the annotation later.
     */
    @ColumnInfo(defaultValue = "Expense")
    val type: String = TransactionType.Expense.name
)
