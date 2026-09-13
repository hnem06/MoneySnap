package com.devpro58.hnem06.moneysnap.data.mapper

import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.devpro58.hnem06.moneysnap.domain.model.IncomeCategory
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptUploadStatus
import com.devpro58.hnem06.moneysnap.domain.model.TransactionType

/**
 * Expense and income share the single `category` column, discriminated by `type`. So for an income
 * row `category` holds an [IncomeCategory] name and the domain's [Expense.category] is set to
 * [ExpenseCategory.Uncategorized] — which keeps it out of the expense-category breakdown, where an
 * income row has no business appearing.
 */
fun ExpenseEntity.toDomain(): Expense {
    val transactionType = enumValueOrDefault(type, TransactionType.Expense)
    val isIncome = transactionType == TransactionType.Income

    return Expense(
        id = id,
        userId = userId,
        amount = amount,
        currency = currency,
        title = title,
        category = if (isIncome) {
            ExpenseCategory.Uncategorized
        } else {
            enumValueOrDefault(category, ExpenseCategory.Uncategorized)
        },
        paymentMethod = paymentMethod,
        note = note,
        localReceiptPath = localReceiptPath,
        remoteReceiptUrl = remoteReceiptUrl,
        receiptUploadStatus = enumValueOrDefault(receiptUploadStatus, ReceiptUploadStatus.None),
        syncStatus = enumValueOrDefault(syncStatus, ExpenseSyncStatus.LocalOnly),
        spentAtMillis = spentAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        type = transactionType,
        incomeCategory = if (isIncome) {
            enumValueOrDefault(category, IncomeCategory.OtherIncome)
        } else {
            null
        }
    )
}

fun Expense.toEntity(): ExpenseEntity =
    ExpenseEntity(
        id = id,
        userId = userId,
        amount = amount,
        currency = currency,
        title = title,
        // Income stores its own category taxonomy in the same column; see toDomain above.
        category = if (isIncome) {
            (incomeCategory ?: IncomeCategory.OtherIncome).name
        } else {
            category.name
        },
        paymentMethod = paymentMethod,
        note = note,
        localReceiptPath = localReceiptPath,
        remoteReceiptUrl = remoteReceiptUrl,
        receiptUploadStatus = receiptUploadStatus.name,
        syncStatus = syncStatus.name,
        spentAtMillis = spentAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        type = type.name
    )

inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)
