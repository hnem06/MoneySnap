package com.devpro58.hnem06.moneysnap.presentation.welcome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.presentation.auth.AuthScreen

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("MoneySnapPrefs", Context.MODE_PRIVATE)

        if (!sharedPrefs.contains("language_code")) {
            sharedPrefs.edit()
                .putString("language_code", "vi")
                .apply()

            val appLocale = LocaleListCompat.forLanguageTags("vi")
            AppCompatDelegate.setApplicationLocales(appLocale)

        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WelcomeFragment())
                .commit()
        }
    }

    fun switchToLanguageSelect() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, LanguageSelectFragment())
            .addToBackStack(null)
            .commit()
    }

    fun finishOnboarding() {
        getSharedPreferences("MoneySnapPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("language_select_completed", true)
            .apply()
        startAuthScreen()
    }

    private fun startAuthScreen() {
        val intent = Intent(this, AuthScreen::class.java)
        startActivity(intent)
        finish()
    }
}
