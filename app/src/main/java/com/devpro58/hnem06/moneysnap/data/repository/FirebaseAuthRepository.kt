package com.devpro58.hnem06.moneysnap.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.devpro58.hnem06.moneysnap.domain.model.AuthDomainException
import com.devpro58.hnem06.moneysnap.domain.model.AuthError
import com.devpro58.hnem06.moneysnap.domain.model.AuthSessionStatus
import com.devpro58.hnem06.moneysnap.domain.model.AuthUser
import com.devpro58.hnem06.moneysnap.domain.repository.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage
) : AuthRepository {

    override fun getCurrentUser(): AuthUser? =
        firebaseAuth.currentUser?.toDomain()

    override suspend fun refreshCurrentSession(): AuthSessionStatus {
        val user = firebaseAuth.currentUser ?: return AuthSessionStatus.Unauthenticated

        return try {
            user.reload().await()
            AuthSessionStatus.Authenticated
        } catch (exception: FirebaseAuthInvalidUserException) {
            AuthSessionStatus.InvalidSession
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthSessionStatus.OfflineAllowed
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthUser =
        runAuthCall {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .await()
                .requireUser()
                .toDomain()
        }

    override suspend fun signInWithGoogle(idToken: String): AuthUser =
        runAuthCall {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .await()
                .requireUser()
                .toDomain()
        }

    override suspend fun registerWithEmail(name: String, email: String, password: String): AuthUser =
        runAuthCall {
            val user = firebaseAuth.createUserWithEmailAndPassword(email, password)
                .await()
                .requireUser()

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()

            user.updateProfile(profileUpdates).await()
            user.toDomain(displayNameOverride = name)
        }

    override suspend fun sendPasswordResetEmail(email: String) {
        runAuthCall {
            firebaseAuth.sendPasswordResetEmail(email).await()
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun updateDisplayName(name: String): AuthUser = runAuthCall {
        val user = firebaseAuth.currentUser ?: throw AuthDomainException(AuthError.InvalidUser)
        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name).build()
        ).await()
        user.toDomain(displayNameOverride = name)
    }

    override suspend fun updateAvatar(localImageUri: String): AuthUser = runAuthCall {
        val user = firebaseAuth.currentUser ?: throw AuthDomainException(AuthError.InvalidUser)

        val compressed = withContext(Dispatchers.IO) { compressAvatar(localImageUri) }
        try {
            val reference = firebaseStorage.reference
                .child("avatars").child(user.uid).child("avatar.jpg")
            val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
            reference.putFile(Uri.fromFile(compressed), metadata).await()
            val downloadUrl = reference.downloadUrl.await()

            user.updateProfile(
                UserProfileChangeRequest.Builder().setPhotoUri(downloadUrl).build()
            ).await()
            user.toDomain().copy(photoUrl = downloadUrl.toString())
        } finally {
            compressed.delete()
        }
    }

    override suspend fun removeAvatar(): AuthUser = runAuthCall {
        val user = firebaseAuth.currentUser ?: throw AuthDomainException(AuthError.InvalidUser)

        try {
            firebaseStorage.reference
                .child("avatars").child(user.uid).child("avatar.jpg")
                .delete().await()
        } catch (exception: StorageException) {
            // Already gone (e.g. removed from another device) — clearing the profile link is enough
            if (exception.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw exception
        }

        user.updateProfile(
            UserProfileChangeRequest.Builder().setPhotoUri(null).build()
        ).await()
        user.toDomain(photoRemoved = true)
    }

    /** Downsamples the picked image to ≤[AVATAR_MAX_DIMENSION]px and JPEG-compresses it into a cache file. */
    private fun compressAvatar(sourceUriString: String): File {
        val sourceUri = Uri.parse(sourceUriString)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw AuthDomainException(AuthError.Unknown, message = "Cannot decode selected image")
        }

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= AVATAR_MAX_DIMENSION &&
            bounds.outHeight / (sampleSize * 2) >= AVATAR_MAX_DIMENSION
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw AuthDomainException(AuthError.Unknown, message = "Cannot open selected image")

        val target = File(context.cacheDir, "avatar_upload.jpg")
        target.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, AVATAR_COMPRESS_QUALITY, output)
        }
        bitmap.recycle()
        return target
    }

    private suspend fun <T> runAuthCall(block: suspend () -> T): T =
        try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw exception.toAuthDomainException()
        }

    private fun AuthResult.requireUser(): FirebaseUser =
        user ?: firebaseAuth.currentUser ?: throw AuthDomainException(AuthError.Unknown)

    private fun FirebaseUser.toDomain(
        displayNameOverride: String? = null,
        photoRemoved: Boolean = false
    ): AuthUser =
        AuthUser(
            id = uid,
            email = email,
            displayName = displayNameOverride ?: displayName,
            photoUrl = if (photoRemoved) null else photoUrl?.toString()
        )

    private companion object {
        const val AVATAR_MAX_DIMENSION = 512
        const val AVATAR_COMPRESS_QUALITY = 85
    }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener

            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: AuthDomainException(AuthError.Unknown)
                )
            }
        }
    }

private fun Throwable.toAuthDomainException(): AuthDomainException {
    if (this is AuthDomainException) return this

    val error = when (this) {
        is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
        is FirebaseAuthInvalidUserException -> AuthError.InvalidUser
        is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
        is FirebaseNetworkException -> AuthError.Network
        is FirebaseAuthException -> when (errorCode) {
            "ERROR_INVALID_EMAIL" -> AuthError.InvalidEmail
            "ERROR_USER_NOT_FOUND" -> AuthError.UserNotFound
            "ERROR_WRONG_PASSWORD" -> AuthError.WrongPassword
            "ERROR_TOO_MANY_REQUESTS" -> AuthError.TooManyRequests
            "ERROR_WEAK_PASSWORD" -> AuthError.WeakPassword
            "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EmailAlreadyInUse
            "ERROR_USER_DISABLED" -> AuthError.UserDisabled
            else -> AuthError.Unknown
        }
        else -> AuthError.Unknown
    }

    return AuthDomainException(error = error, cause = this, message = localizedMessage)
}
