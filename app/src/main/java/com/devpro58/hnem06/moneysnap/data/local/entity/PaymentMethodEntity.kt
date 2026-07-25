package com.devpro58.hnem06.moneysnap.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_methods",
    indices = [
        Index("userId"),
        Index(value = ["userId", "name"], unique = true),
        Index(value = ["userId", "builtInKey"], unique = true)
    ]
)
data class PaymentMethodEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val builtInKey: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
