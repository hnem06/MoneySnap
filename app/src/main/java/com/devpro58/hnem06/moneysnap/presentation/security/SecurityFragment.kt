package com.devpro58.hnem06.moneysnap.presentation.security

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.setupTopAppBarNavigation
import com.devpro58.hnem06.moneysnap.presentation.auth.AuthScreen
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint

/**
 * Password change and account deletion.
 *
 * Reached from the Profile "Security" row, which previously rendered a chevron and did nothing.
 * Account deletion is a Google Play requirement for any app that lets users create an account
 * in-app.
 */
@AndroidEntryPoint
class SecurityFragment : Fragment() {

    private lateinit var viewModel: SecurityViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_security, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SecurityViewModel::class.java]

        setupTopAppBarNavigation(view)
        view.findViewById<MaterialToolbar>(R.id.topAppBar).apply {
            title = getString(R.string.security_title)
            setNavigationIcon(R.drawable.ic_arrow_back)
            navigationContentDescription = getString(R.string.cd_back_to_profile)
            setNavigationOnClickListener { findNavController().popBackStack() }
        }

        val passwordForm = view.findViewById<LinearLayout>(R.id.securityPasswordForm)
        val googleNotice = view.findViewById<TextView>(R.id.securityGoogleNotice)
        passwordForm.isVisible = viewModel.canChangePassword
        googleNotice.isVisible = !viewModel.canChangePassword

        val currentPassword = view.findViewById<TextInputEditText>(R.id.securityCurrentPassword)
        val newPassword = view.findViewById<TextInputEditText>(R.id.securityNewPassword)
        val confirmPassword = view.findViewById<TextInputEditText>(R.id.securityConfirmPassword)
        val saveButton = view.findViewById<MaterialButton>(R.id.securitySavePassword)
        val deleteButton = view.findViewById<MaterialButton>(R.id.securityDeleteAccount)
        val progress = view.findViewById<View>(R.id.securityProgress)

        saveButton.setOnClickListener {
            val current = currentPassword.text.toString()
            val next = newPassword.text.toString()
            val confirm = confirmPassword.text.toString()

            // Mirrors the register screen's rules so the two cannot drift apart.
            when {
                current.isBlank() -> toast(R.string.error_password_empty)
                next.length < MIN_PASSWORD_LENGTH -> toast(R.string.error_password_too_short)
                next != confirm -> toast(R.string.error_password_mismatch)
                else -> viewModel.changePassword(current, next)
            }
        }

        deleteButton.setOnClickListener { confirmDeleteAccount() }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progress.isVisible = state.busy
            saveButton.isEnabled = !state.busy
            deleteButton.isEnabled = !state.busy

            state.message?.let { messageRes ->
                Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show()
                viewModel.consumeMessage()
                if (!state.accountDeleted) {
                    currentPassword.text = null
                    newPassword.text = null
                    confirmPassword.text = null
                }
            }

            if (state.accountDeleted) goToAuthScreen()
        }
    }

    /**
     * Two-step confirmation: a typed keyword, plus the password when the account has one.
     * Deletion is irreversible and cascades to the server, so a single tap is not enough.
     */
    private fun confirmDeleteAccount() {
        val keyword = getString(R.string.security_delete_keyword)
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }

        val confirmInput = EditText(requireContext()).apply {
            hint = getString(R.string.security_delete_confirm_hint, keyword)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        container.addView(confirmInput)

        val passwordInput = EditText(requireContext()).apply {
            hint = getString(R.string.security_delete_password_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isVisible = viewModel.canChangePassword
        }
        container.addView(passwordInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.security_delete_title)
            .setMessage(getString(R.string.security_delete_message, keyword))
            .setView(container)
            .setNegativeButton(R.string.security_cancel, null)
            .setPositiveButton(R.string.security_delete_confirm) { _, _ ->
                if (!confirmInput.text.toString().trim().equals(keyword, ignoreCase = true)) {
                    toast(R.string.security_delete_mismatch)
                    return@setPositiveButton
                }
                val password = passwordInput.text.toString()
                    .takeIf { viewModel.canChangePassword && it.isNotBlank() }
                viewModel.deleteAccount(password)
            }
            .show()
    }

    private fun goToAuthScreen() {
        val intent = Intent(requireContext(), AuthScreen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
