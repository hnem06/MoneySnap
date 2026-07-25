package com.devpro58.hnem06.moneysnap.data.remote.firebase

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener

            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase task failed")
                )
            }
        }
    }
