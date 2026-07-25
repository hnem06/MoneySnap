package com.devpro58.hnem06.moneysnap.domain.repository

interface SettingsRepository {
    fun isDarkMode(): Boolean
    fun setDarkMode(enabled: Boolean)
    fun getMonthlyBudget(): Long
    fun setMonthlyBudget(amount: Long)
    fun isMonthlyBudgetConfigured(): Boolean
}
