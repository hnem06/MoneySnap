package com.devpro58.hnem06.moneysnap.presentation.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.domain.usecase.onboarding.GetLanguageCodeUseCase
import com.devpro58.hnem06.moneysnap.presentation.auth.AuthScreen
import com.devpro58.hnem06.moneysnap.presentation.main.MainActivity
import com.devpro58.hnem06.moneysnap.presentation.welcome.WelcomeActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject lateinit var getLanguageCode: GetLanguageCodeUseCase

    private val handler = Handler(Looper.getMainLooper())
    private val navigateRunnable = Runnable { viewModel.resolveDestination() }
    private lateinit var viewModel: SplashViewModel
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyStoredLocale()
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        viewModel = ViewModelProvider(this)[SplashViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel.destination.observe(this) { destination ->
            navigateTo(destination)
        }

        handler.postDelayed(navigateRunnable, 500)
    }

    private fun applyStoredLocale() {
        val languageCode = getLanguageCode() ?: return
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageCode)
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(navigateRunnable)
        super.onDestroy()
    }

    private fun navigateTo(destination: SplashDestination) {
        if (hasNavigated || isFinishing || isDestroyed) return
        hasNavigated = true

        val intent = when (destination) {
            SplashDestination.Auth -> Intent(this, AuthScreen::class.java)
            SplashDestination.Welcome -> Intent(this, WelcomeActivity::class.java)
            is SplashDestination.Main -> Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_SHOW_SESSION_ERROR, destination.showSessionError)
            }
        }

        startActivity(intent)
        finish()
    }
}
