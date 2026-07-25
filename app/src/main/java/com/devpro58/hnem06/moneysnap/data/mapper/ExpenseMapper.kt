package com.devpro58.hnem06.moneysnap.data.mapper

import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptUploadStatus

fun ExpenseEntity.toDomain(): Expense =
    Expense(
        id = id,
        userId = userId,
        amount = amount,
        currency = currency,
        title = title,
        category = enumValueOrDefault(category, ExpenseCategory.Uncategorized),
        paymentMethod = paymentMethod,
        note = note,
        localReceiptPath = localReceiptPath,
        remoteReceiptUrl = remoteReceiptUrl,
        receiptUploadStatus = enumValueOrDefault(receiptUploadStatus, ReceiptUploadStatus.None),
        syncStatus = enumValueOrDefault(syncStatus, ExpenseSyncStatus.LocalOnly),
        spentAtMillis = spentAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

fun Expense.toEntity(): ExpenseEntity =
    ExpenseEntity(
        id = id,
        userId = userId,
        amount = amount,
        currency = currency,
        title = title,
        category = category.name,
        paymentMethod = paymentMethod,
        note = note,
        localReceiptPath = localReceiptPath,
        remoteReceiptUrl = remoteReceiptUrl,
        receiptUploadStatus = receiptUploadStatus.name,
        syncStatus = syncStatus.name,
        spentAtMillis = spentAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)
