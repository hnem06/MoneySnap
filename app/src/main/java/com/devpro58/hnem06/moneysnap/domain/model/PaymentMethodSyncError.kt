package com.devpro58.hnem06.moneysnap.domain.model

enum class PaymentMethodSyncError {
    PermissionDenied,
    Network,
    Unknown
}

class PaymentMethodSyncException(
    val error: PaymentMethodSyncError,
    cause: Throwable? = null
) : Exception(cause)
