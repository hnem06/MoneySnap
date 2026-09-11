package com.devpro58.hnem06.moneysnap.domain.usecase.auth

import com.devpro58.hnem06.moneysnap.domain.repository.AuthRepository
import com.devpro58.hnem06.moneysnap.domain.repository.ExpenseRepository
import com.devpro58.hnem06.moneysnap.domain.repository.PaymentMethodRepository
import com.devpro58.hnem06.moneysnap.domain.repository.SettingsRepository
import javax.inject.Inject

class CheckAuthSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.refreshCurrentSession()
}

class SignInWithEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) =
        repository.signInWithEmail(email, password)
}

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(idToken: String) = repository.signInWithGoogle(idToken)
}

class RegisterWithEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String) =
        repository.registerWithEmail(name, email, password)
}

class SendPasswordResetUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String) = repository.sendPasswordResetEmail(email)
}

/**
 * Signs out and removes every trace of the account from this device.
 *
 * `firebaseAuth.signOut()` alone left the user's expenses in Room, their receipt images in
 * `filesDir/receipts/{uid}`, and their budget in SharedPreferences — so on a shared device the
 * next person to sign in inherited the previous account's data and notification state.
 *
 * Local cleanup runs before the auth sign-out and each step is isolated: a failure to delete a
 * cached file must never leave the user still signed in.
 */
class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val expenseRepository: ExpenseRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        val userId = repository.getCurrentUser()?.id

        // Each step is isolated: failing to delete a cached file must never leave the user
        // still signed in. Cancelling queued sync work is handled inside clearLocalData, which
        // owns the scheduler — the domain layer must not reach into data/sync directly.
        if (userId != null) {
            runCatching { expenseRepository.clearLocalData(userId) }
            runCatching { paymentMethodRepository.clearLocalData(userId) }
        }
        runCatching { settingsRepository.clearUserScopedSettings() }

        repository.signOut()
    }
}

class HasPasswordProviderUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Boolean = repository.hasPasswordProvider()
}

class ChangePasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String) =
        repository.updatePassword(currentPassword, newPassword)
}

/**
 * Deletes the account remotely and then clears everything this device cached for it.
 *
 * Local cleanup runs after the remote cascade succeeds: wiping Room first would leave the user
 * with an empty app if the remote deletion then failed and they stayed signed in.
 */
class DeleteAccountUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val expenseRepository: ExpenseRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(password: String?) {
        val userId = repository.getCurrentUser()?.id

        repository.deleteAccount(password)

        if (userId != null) {
            runCatching { expenseRepository.clearLocalData(userId) }
            runCatching { paymentMethodRepository.clearLocalData(userId) }
        }
        runCatching { settingsRepository.clearUserScopedSettings() }
    }
}

class GetCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke() = repository.getCurrentUser()
}

class UpdateDisplayNameUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(name: String) = repository.updateDisplayName(name)
}

class UpdateAvatarUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(localImageUri: String) = repository.updateAvatar(localImageUri)
}

class RemoveAvatarUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.removeAvatar()
}
