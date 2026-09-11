package com.devpro58.hnem06.moneysnap.core.utils

import androidx.fragment.app.Fragment
import com.devpro58.hnem06.moneysnap.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Shared terms & privacy dialog.
 *
 * Extracted from RegisterFragment so the Welcome screen's terms link and the Profile privacy row
 * open the same content instead of each shipping their own copy — or, as was the case for both,
 * nothing at all.
 *
 * This is interim: Play requires a hosted privacy-policy URL, and once that page exists the
 * Profile row should open it rather than this dialog.
 */
fun Fragment.showTermsDialog() {
    val ctx = context ?: return
    MaterialAlertDialogBuilder(ctx)
        .setTitle(R.string.terms_dialog_title)
        .setMessage(R.string.terms_dialog_message)
        .setPositiveButton(R.string.terms_dialog_btn_ok, null)
        .show()
}
