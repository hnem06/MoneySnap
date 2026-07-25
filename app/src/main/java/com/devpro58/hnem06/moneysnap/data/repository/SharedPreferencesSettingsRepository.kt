package com.devpro58.hnem06.moneysnap.data.repository

import android.content.Context
import com.devpro58.hnem06.moneysnap.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isDarkMode(): Boolean =
        preferences.getBoolean(KEY_DARK_MODE, false)

    override fun setDarkMode(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }

    override fun getMonthlyBudget(): Long =
        preferences.getLong(KEY_MONTHLY_BUDGET, DEFAULT_MONTHLY_BUDGET)

    override fun setMonthlyBudget(amount: Long) {
        preferences.edit()
            .putLong(KEY_MONTHLY_BUDGET, amount)
            .putBoolean(KEY_MONTHLY_BUDGET_CONFIGURED, true)
            .apply()
    }

    override fun isMonthlyBudgetConfigured(): Boolean =
        preferences.getBoolean(KEY_MONTHLY_BUDGET_CONFIGURED, false)

    private companion object {
        const val PREFS_NAME = "MoneySnapPrefs"
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val KEY_MONTHLY_BUDGET = "monthly_budget"
        const val KEY_MONTHLY_BUDGET_CONFIGURED = "monthly_budget_configured"
        const val DEFAULT_MONTHLY_BUDGET = 5_000_000L
    }
}
