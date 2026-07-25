package com.devpro58.hnem06.moneysnap.presentation.welcome

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        navigateToLanguageSelect()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvWelcomeTitle.text = Html.fromHtml(
            getString(R.string.welcome_title),
            Html.FROM_HTML_MODE_LEGACY
        )

        binding.tvTerms.text = Html.fromHtml(
            getString(R.string.welcome_terms),
            Html.FROM_HTML_MODE_LEGACY
        )

        binding.btnStart.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(android.Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun navigateToLanguageSelect() {
        (activity as? WelcomeActivity)?.switchToLanguageSelect()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
