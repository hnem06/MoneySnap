package com.devpro58.hnem06.moneysnap.data.repository

import android.content.Context
import com.devpro58.hnem06.moneysnap.data.notification.BudgetAlertNotifier
import com.devpro58.hnem06.moneysnap.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val budgetAlertPreferences =
        context.getSharedPreferences(BudgetAlertNotifier.PREFS_NAME, Context.MODE_PRIVATE)

    override fun isDarkMode(): Boolean =
        preferences.getBoolean(KEY_DARK_MODE, false)

    override fun setDarkMode(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }

    override fun areBudgetAlertsEnabled(): Boolean =
        preferences.getBoolean(KEY_BUDGET_ALERTS_ENABLED, true)

    override fun setBudgetAlertsEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_BUDGET_ALERTS_ENABLED, enabled)
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

    override fun clearUserScopedSettings() {
        preferences.edit()
            .remove(KEY_MONTHLY_BUDGET)
            .remove(KEY_MONTHLY_BUDGET_CONFIGURED)
            .apply()
        // The alert bookkeeping is keyed by user+month, but it lives in a separate prefs file and
        // would otherwise keep suppressing notifications for whoever signs in next on this device.
        budgetAlertPreferences.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "MoneySnapPrefs"
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val KEY_BUDGET_ALERTS_ENABLED = "budget_alerts_enabled"
        const val KEY_MONTHLY_BUDGET = "monthly_budget"
        const val KEY_MONTHLY_BUDGET_CONFIGURED = "monthly_budget_configured"
        const val DEFAULT_MONTHLY_BUDGET = 5_000_000L
    }
}
