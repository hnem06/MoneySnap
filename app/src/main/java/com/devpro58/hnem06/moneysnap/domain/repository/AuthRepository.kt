package com.devpro58.hnem06.moneysnap.domain.repository

import com.devpro58.hnem06.moneysnap.domain.model.AuthSessionStatus
import com.devpro58.hnem06.moneysnap.domain.model.AuthUser

interface AuthRepository {
    fun getCurrentUser(): AuthUser?
    suspend fun refreshCurrentSession(): AuthSessionStatus
    suspend fun signInWithEmail(email: String, password: String): AuthUser
    suspend fun signInWithGoogle(idToken: String): AuthUser
    suspend fun registerWithEmail(name: String, email: String, password: String): AuthUser
    suspend fun sendPasswordResetEmail(email: String)
    fun signOut()

    /** Updates the signed-in user's display name and returns the refreshed profile. */
    suspend fun updateDisplayName(name: String): AuthUser

    /** Uploads [localImageUri] as the account avatar and returns the refreshed profile. */
    suspend fun updateAvatar(localImageUri: String): AuthUser

    /** Removes the account avatar (storage object + profile link); returns the refreshed profile. */
    suspend fun removeAvatar(): AuthUser

    /** True when the account can sign in with a password, i.e. not Google-only. */
    fun hasPasswordProvider(): Boolean

    /**
     * Proves the user is who they say they are, which Firebase requires before a password change
     * or account deletion. Throws [com.devpro58.hnem06.moneysnap.domain.model.AuthDomainException]
     * with [com.devpro58.hnem06.moneysnap.domain.model.AuthError.InvalidCredentials] on a wrong
     * password.
     */
    suspend fun reauthenticate(password: String)

    /** Changes the password after re-authenticating with [currentPassword]. */
    suspend fun updatePassword(currentPassword: String, newPassword: String)

    /**
     * Deletes the account and everything belonging to it.
     *
     * Required by Google Play's User Data policy for any app offering in-app account creation.
     * [password] is required for password accounts and ignored for Google-only ones.
     */
    suspend fun deleteAccount(password: String?)
}
