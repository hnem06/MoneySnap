package com.devpro58.hnem06.moneysnap.domain.repository

interface SettingsRepository {
    fun isDarkMode(): Boolean
    fun setDarkMode(enabled: Boolean)
    fun getMonthlyBudget(): Long
    fun setMonthlyBudget(amount: Long)
    fun isMonthlyBudgetConfigured(): Boolean

    /**
     * Clears settings that belong to the signed-in account, leaving device preferences alone.
     *
     * The budget is account data and must not follow one user into another's session. Dark mode,
     * language and onboarding are properties of the device and the person holding it, so they
     * survive sign-out — clearing them would reset the UI to English light mode for no reason.
     */
    fun clearUserScopedSettings()
}
