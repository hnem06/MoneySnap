package com.devpro58.hnem06.moneysnap.domain.model

enum class AuthError {
    InvalidCredentials,
    InvalidUser,
    EmailAlreadyInUse,
    InvalidEmail,
    UserNotFound,
    WrongPassword,
    TooManyRequests,
    WeakPassword,
    UserDisabled,
    Network,
    Unknown
}

class AuthDomainException(
    val error: AuthError,
    cause: Throwable? = null,
    message: String? = null
) : Exception(message, cause)
