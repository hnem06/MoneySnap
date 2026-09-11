package com.devpro58.hnem06.moneysnap.core.utils

import android.content.Context
import androidx.annotation.StringRes
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.domain.model.AuthDomainException
import com.devpro58.hnem06.moneysnap.domain.model.AuthError

object AuthExceptionHandler {

    fun getErrorMessage(context: Context, exception: Throwable?): String =
        context.getString(messageRes(exception))

    /**
     * Resource-id form, so a ViewModel can pick the message without holding a Context.
     * Resolution stays in the view layer where the configuration (and thus the locale) lives.
     */
    @StringRes
    fun messageRes(exception: Throwable?): Int = when (exception) {
        null -> R.string.error_default
        is AuthDomainException -> exception.error.messageRes()
        else -> R.string.error_default
    }

    @StringRes
    private fun AuthError.messageRes(): Int = when (this) {
        AuthError.InvalidCredentials -> R.string.error_auth_invalid_credentials
        AuthError.InvalidUser -> R.string.error_auth_invalid_user
        AuthError.EmailAlreadyInUse -> R.string.error_auth_email_already_in_use
        AuthError.Network -> R.string.error_auth_network
        AuthError.InvalidEmail -> R.string.error_auth_invalid_email
        AuthError.UserNotFound -> R.string.error_auth_user_not_found
        AuthError.WrongPassword -> R.string.error_auth_wrong_password
        AuthError.TooManyRequests -> R.string.error_auth_too_many_requests
        AuthError.WeakPassword -> R.string.error_auth_weak_password
        AuthError.UserDisabled -> R.string.error_auth_user_disabled
        AuthError.Unknown -> R.string.error_auth_unknown
    }
}
