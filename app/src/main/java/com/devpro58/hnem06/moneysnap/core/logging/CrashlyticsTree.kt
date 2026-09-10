package com.devpro58.hnem06.moneysnap.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Release-build Timber tree: forwards warnings and errors to Crashlytics so a user report of
 * "saving failed" is actually diagnosable.
 *
 * VERBOSE/DEBUG/INFO are dropped on purpose — they are development noise, and Crashlytics keeps
 * only the last 64 KB of log per session, so flooding it would evict the entries that matter.
 *
 * Never log receipt OCR text, amounts, notes, or e-mail addresses through this tree: Crashlytics
 * payloads leave the device.
 */
class CrashlyticsTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean =
        priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log(if (tag != null) "$tag: $message" else message)
        // Only ERROR carries a throwable worth a non-fatal report; a WARN breadcrumb is enough
        // context on its own and would otherwise drown the non-fatal issue list.
        if (priority >= Log.ERROR && t != null) {
            crashlytics.recordException(t)
        }
    }
}
