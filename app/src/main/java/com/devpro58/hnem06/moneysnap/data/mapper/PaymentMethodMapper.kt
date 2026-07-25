package com.devpro58.hnem06.moneysnap.data.mapper

import com.devpro58.hnem06.moneysnap.data.local.entity.PaymentMethodEntity
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethod

fun PaymentMethodEntity.toDomain(): PaymentMethod =
    PaymentMethod(
        id = id,
        userId = userId,
        name = name,
        builtInKey = builtInKey,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

fun PaymentMethod.toEntity(): PaymentMethodEntity =
    PaymentMethodEntity(
        id = id,
        userId = userId,
        name = name,
        builtInKey = builtInKey,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
