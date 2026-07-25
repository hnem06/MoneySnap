package com.devpro58.hnem06.moneysnap.domain.usecase.auth

import com.devpro58.hnem06.moneysnap.domain.repository.AuthRepository
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

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke() = repository.signOut()
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
