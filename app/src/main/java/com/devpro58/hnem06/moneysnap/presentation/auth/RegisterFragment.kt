package com.devpro58.hnem06.moneysnap.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.databinding.FragmentRegisterBinding
import com.devpro58.hnem06.moneysnap.presentation.main.MainActivity

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        binding.tvSwitchToLogin.text = android.text.Html.fromHtml(
            getString(R.string.switch_to_login),
            android.text.Html.FROM_HTML_MODE_LEGACY
        )

        binding.tvTerms.text = android.text.Html.fromHtml(
            getString(R.string.terms_and_conditions),
            android.text.Html.FROM_HTML_MODE_LEGACY
        )

        binding.tvSwitchToLogin.setOnClickListener {
            (activity as? AuthScreen)?.switchToLogin()
        }

        binding.tvTerms.setOnClickListener {
            showTermsDialog()
        }

        binding.btnRegister.setOnClickListener {
            performRegistration()
        }
    }

    private fun performRegistration() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val isTermsChecked = binding.cbTerms.isChecked

        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.error_name_empty)
            binding.etName.requestFocus()
            return
        }
        if (email.isEmpty()) {
            binding.etEmail.error = getString(R.string.error_email_empty)
            binding.etEmail.requestFocus()
            return
        }
        if (password.isEmpty() || password.length < 6) {
            binding.etPassword.error = getString(R.string.error_password_too_short)
            binding.etPassword.requestFocus()
            return
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = getString(R.string.error_password_mismatch)
            binding.etConfirmPassword.requestFocus()
            return
        }
        if (!isTermsChecked) {
            Toast.makeText(context, getString(R.string.error_terms_not_checked), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener {
                            navigateToMainScreen()
                        }
                } else {
                    binding.btnRegister.isEnabled = true
                    val errorMessage = task.exception?.localizedMessage ?: getString(R.string.error_register_failed)
                    Toast.makeText(context, getString(R.string.error_with_message, errorMessage), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToMainScreen() {
        val intent = Intent(activity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    private fun showTermsDialog() {
        context?.let { ctx ->
            com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.terms_dialog_title)
                .setMessage(R.string.terms_dialog_message)
                .setPositiveButton(R.string.terms_dialog_btn_ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
