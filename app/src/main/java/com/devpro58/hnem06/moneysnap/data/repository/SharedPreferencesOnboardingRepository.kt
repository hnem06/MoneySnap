package com.devpro58.hnem06.moneysnap.data.repository

import android.content.Context
import com.devpro58.hnem06.moneysnap.domain.repository.OnboardingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesOnboardingRepository @Inject constructor(
    @ApplicationContext context: Context
) : OnboardingRepository {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isCompleted(): Boolean =
        preferences.getBoolean(KEY_LANGUAGE_SELECT_COMPLETED, false)

    override fun complete() {
        preferences.edit()
            .putBoolean(KEY_LANGUAGE_SELECT_COMPLETED, true)
            .apply()
    }

    override fun getLanguageCode(): String? =
        preferences.getString(KEY_LANGUAGE_CODE, null)

    override fun setLanguageCode(languageCode: String) {
        preferences.edit()
            .putString(KEY_LANGUAGE_CODE, languageCode)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "MoneySnapPrefs"
        const val KEY_LANGUAGE_SELECT_COMPLETED = "language_select_completed"
        const val KEY_LANGUAGE_CODE = "language_code"
    }
}
