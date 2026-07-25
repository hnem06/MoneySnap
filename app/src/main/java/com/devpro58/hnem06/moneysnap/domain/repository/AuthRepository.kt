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
}
