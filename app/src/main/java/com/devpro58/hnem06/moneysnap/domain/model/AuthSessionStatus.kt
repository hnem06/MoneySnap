package com.devpro58.hnem06.moneysnap.domain.model

sealed interface AuthSessionStatus {
    data object Authenticated : AuthSessionStatus
    data object Unauthenticated : AuthSessionStatus
    data object InvalidSession : AuthSessionStatus
    data object OfflineAllowed : AuthSessionStatus
}
