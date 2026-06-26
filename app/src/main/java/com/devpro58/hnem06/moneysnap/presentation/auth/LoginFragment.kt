package com.devpro58.hnem06.moneysnap.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth

import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.databinding.FragmentLoginBinding
import com.devpro58.hnem06.moneysnap.presentation.main.MainActivity

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(context, getString(R.string.error_google_login, e.localizedMessage), Toast.LENGTH_SHORT).show()
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.tvSwitchToRegister.text = android.text.Html.fromHtml(
            getString(R.string.switch_to_register),
            android.text.Html.FROM_HTML_MODE_LEGACY
        )

        binding.tvSwitchToRegister.setOnClickListener {
            (activity as? AuthScreen)?.switchToRegister()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.edtEmail.error = getString(R.string.error_email_empty)
                binding.edtEmail.requestFocus()
            } else if (password.isEmpty()) {
                binding.edtPassword.error = getString(R.string.error_password_empty)
                binding.edtPassword.requestFocus()
            } else {
                binding.btnLogin.isEnabled = false
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            navigateToMainScreen()
                        } else {
                            binding.btnLogin.isEnabled = true
                            val errorMessage = task.exception?.localizedMessage ?: getString(R.string.error_login_failed)
                            Toast.makeText(context, getString(R.string.error_with_message, errorMessage), Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        binding.btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    navigateToMainScreen()
                } else {
                    val error = task.exception?.localizedMessage ?: getString(R.string.error_firebase_login_failed)
                    Toast.makeText(context, getString(R.string.error_with_message, error), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToMainScreen() {
        val intent = Intent(activity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
