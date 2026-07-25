package com.devpro58.hnem06.moneysnap.presentation.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.databinding.FragmentLanguageSelectBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageSelectFragment : Fragment() {

    private var _binding: FragmentLanguageSelectBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WelcomeViewModel
    private var selectedLanguageCode = "vi"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[WelcomeViewModel::class.java]

        selectedLanguageCode = viewModel.getSelectedLanguageCode()

        updateLanguageUi()

        binding.layoutLangVi.setOnClickListener { changeLanguage("vi") }
        binding.layoutLangEn.setOnClickListener { changeLanguage("en") }

        binding.btnContinue.setOnClickListener {
            (activity as? WelcomeActivity)?.finishOnboarding()
        }
    }

    private fun changeLanguage(langCode: String) {
        val currentLangCode = viewModel.getSelectedLanguageCode()

        if (langCode != currentLangCode) {
            viewModel.selectLanguage(langCode)
            selectedLanguageCode = langCode
            updateLanguageUi()

            val appLocale = LocaleListCompat.forLanguageTags(langCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    private fun updateLanguageUi() {
        binding.layoutLangVi.setBackgroundResource(R.drawable.bg_lang_unselected)
        binding.ivCheckVi.visibility = View.INVISIBLE
        binding.layoutLangEn.setBackgroundResource(R.drawable.bg_lang_unselected)
        binding.ivCheckEn.visibility = View.INVISIBLE

        when (selectedLanguageCode) {
            "vi" -> {
                binding.layoutLangVi.setBackgroundResource(R.drawable.bg_lang_selected)
                binding.ivCheckVi.visibility = View.VISIBLE
            }
            "en" -> {
                binding.layoutLangEn.setBackgroundResource(R.drawable.bg_lang_selected)
                binding.ivCheckEn.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
