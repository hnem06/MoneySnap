package com.devpro58.hnem06.moneysnap.domain.usecase.onboarding

import com.devpro58.hnem06.moneysnap.domain.repository.OnboardingRepository
import javax.inject.Inject

class IsOnboardingCompletedUseCase @Inject constructor(
    private val repository: OnboardingRepository
) {
    operator fun invoke() = repository.isCompleted()
}

class CompleteOnboardingUseCase @Inject constructor(
    private val repository: OnboardingRepository
) {
    operator fun invoke() = repository.complete()
}

class GetLanguageCodeUseCase @Inject constructor(
    private val repository: OnboardingRepository
) {
    operator fun invoke() = repository.getLanguageCode()
}

class SetLanguageCodeUseCase @Inject constructor(
    private val repository: OnboardingRepository
) {
    operator fun invoke(languageCode: String) = repository.setLanguageCode(languageCode)
}
