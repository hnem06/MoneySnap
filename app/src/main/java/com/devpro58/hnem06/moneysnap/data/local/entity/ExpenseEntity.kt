package com.devpro58.hnem06.moneysnap.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index("userId"),
        Index("spentAtMillis"),
        Index("syncStatus"),
        Index("receiptUploadStatus")
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
    val updatedAtMillis: Long
)
