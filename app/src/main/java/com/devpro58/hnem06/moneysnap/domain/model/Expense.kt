package com.devpro58.hnem06.moneysnap.domain.model

import java.io.Serializable

/**
 * [Serializable] so the model can travel in a Fragment `arguments` Bundle — bottom sheets that
 * received it through a setter lost it whenever the system recreated them and silently dismissed.
 * `java.io.Serializable` is JDK, not Android, so the domain layer stays platform-free.
 */
data class Expense(
    val id: String,
    val userId: String,
    val amount: Long,
    val currency: String,
    val title: String,
    val category: ExpenseCategory,
    val paymentMethod: String?,
    val note: String?,
    val localReceiptPath: String?,
    val remoteReceiptUrl: String?,
    val receiptUploadStatus: ReceiptUploadStatus,
    val syncStatus: ExpenseSyncStatus,
    val spentAtMillis: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val type: TransactionType = TransactionType.Expense,
    /** Non-null exactly when [type] is [TransactionType.Income]. */
    val incomeCategory: IncomeCategory? = null
) : Serializable {

    val isIncome: Boolean get() = type == TransactionType.Income

    /** Negative for spending, positive for income — for balance arithmetic. */
    val signedAmount: Long get() = if (isIncome) amount else -amount
}
