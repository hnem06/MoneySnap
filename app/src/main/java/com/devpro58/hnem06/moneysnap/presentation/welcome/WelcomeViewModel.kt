package com.devpro58.hnem06.moneysnap.presentation.welcome

import androidx.lifecycle.ViewModel
import com.devpro58.hnem06.moneysnap.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.onboarding.GetLanguageCodeUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.onboarding.SetLanguageCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val getLanguageCode: GetLanguageCodeUseCase,
    private val setLanguageCode: SetLanguageCodeUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase
) : ViewModel() {

    fun ensureDefaultLanguage(defaultLanguageCode: String = DEFAULT_LANGUAGE_CODE): String {
        val currentLanguageCode = getLanguageCode()
        if (currentLanguageCode != null) return currentLanguageCode

        setLanguageCode(defaultLanguageCode)
        return defaultLanguageCode
    }

    fun getSelectedLanguageCode(): String =
        getLanguageCode() ?: DEFAULT_LANGUAGE_CODE

    fun selectLanguage(languageCode: String) {
        setLanguageCode(languageCode)
    }

    fun finishOnboarding() {
        completeOnboarding()
    }

    private companion object {
        const val DEFAULT_LANGUAGE_CODE = "vi"
    }
}
