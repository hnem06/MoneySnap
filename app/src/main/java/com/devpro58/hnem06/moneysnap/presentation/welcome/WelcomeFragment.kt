package com.devpro58.hnem06.moneysnap.presentation.welcome

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.showTermsDialog
import com.devpro58.hnem06.moneysnap.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

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
        // The text rendered as a link but had no listener, so tapping it did nothing.
        binding.tvTerms.setOnClickListener { showTermsDialog() }

        // No permissions are requested here any more. The old up-front prompt discarded its
        // result, was skipped entirely for returning users, and asked for storage permissions the
        // app never needed. CAMERA is now requested in AddExpenseFragment at the moment the user
        // taps "Take photo", which is both correct and the placement Play guidance expects.
        binding.btnStart.setOnClickListener {
            navigateToLanguageSelect()
        }
    }

    private fun navigateToLanguageSelect() {
        (activity as? WelcomeActivity)?.switchToLanguageSelect()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
