package com.devpro58.hnem06.moneysnap.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.AuthExceptionHandler
import com.devpro58.hnem06.moneysnap.databinding.FragmentForgotBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotFragment : Fragment() {

    private var _binding: FragmentForgotBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ForgotPasswordViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ForgotPasswordViewModel::class.java]
        observeViewModel()

        binding.tvSwitchToLogin.text = android.text.Html.fromHtml(
            getString(R.string.switch_to_login),
            android.text.Html.FROM_HTML_MODE_LEGACY
        )

        binding.tvSwitchToLogin.setOnClickListener {
            (activity as? AuthScreen)?.switchToLogin()
        }

        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = getString(R.string.error_email_empty)
                binding.etEmail.requestFocus()
            } else {
                binding.btnResetPassword.isEnabled = false
                viewModel.sendResetEmail(email)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.btnResetPassword.isEnabled = state !is AuthUiState.Loading

            when (state) {
                AuthUiState.PasswordResetEmailSent -> {
                    Toast.makeText(
                        context,
                        getString(R.string.forgot_success_message),
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetState()
                    (activity as? AuthScreen)?.switchToLogin()
                }
                is AuthUiState.Error -> {
                    val error = AuthExceptionHandler.getErrorMessage(requireContext(), state.throwable)
                    Toast.makeText(
                        context,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetState()
                }
                AuthUiState.Authenticated,
                AuthUiState.Idle,
                AuthUiState.Loading -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
