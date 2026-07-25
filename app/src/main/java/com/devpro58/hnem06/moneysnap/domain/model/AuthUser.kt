package com.devpro58.hnem06.moneysnap.domain.model

data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String? = null
)
