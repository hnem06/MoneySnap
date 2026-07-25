package com.devpro58.hnem06.moneysnap.domain.model

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
    val updatedAtMillis: Long
)
