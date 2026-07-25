package com.devpro58.hnem06.moneysnap.domain.usecase.settings

import com.devpro58.hnem06.moneysnap.domain.repository.SettingsRepository
import javax.inject.Inject

class GetDarkModeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Boolean = repository.isDarkMode()
}

class SetDarkModeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(enabled: Boolean) = repository.setDarkMode(enabled)
}

class GetMonthlyBudgetUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Long = repository.getMonthlyBudget()
}

class SetMonthlyBudgetUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(amount: Long) = repository.setMonthlyBudget(amount)
}
