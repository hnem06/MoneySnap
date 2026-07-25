package com.devpro58.hnem06.moneysnap.core.utils

import android.content.Context
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.domain.model.AuthDomainException
import com.devpro58.hnem06.moneysnap.domain.model.AuthError

object AuthExceptionHandler {

    fun getErrorMessage(context: Context, exception: Throwable?): String {
        if (exception == null) return context.getString(R.string.error_default)

        return when (exception) {
            is AuthDomainException -> exception.toMessage(context)
            else -> context.getString(R.string.error_default)
        }
    }

    private fun AuthDomainException.toMessage(context: Context): String =
        when (error) {
            AuthError.InvalidCredentials -> context.getString(R.string.error_auth_invalid_credentials)
            AuthError.InvalidUser -> context.getString(R.string.error_auth_invalid_user)
            AuthError.EmailAlreadyInUse -> context.getString(R.string.error_auth_email_already_in_use)
            AuthError.Network -> context.getString(R.string.error_auth_network)
            AuthError.InvalidEmail -> context.getString(R.string.error_auth_invalid_email)
            AuthError.UserNotFound -> context.getString(R.string.error_auth_user_not_found)
            AuthError.WrongPassword -> context.getString(R.string.error_auth_wrong_password)
            AuthError.TooManyRequests -> context.getString(R.string.error_auth_too_many_requests)
            AuthError.WeakPassword -> context.getString(R.string.error_auth_weak_password)
            AuthError.UserDisabled -> context.getString(R.string.error_auth_user_disabled)
            AuthError.Unknown -> context.getString(R.string.error_auth_unknown)
        }
}
