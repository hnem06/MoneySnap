package com.devpro58.hnem06.moneysnap.presentation.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.presentation.auth.AuthScreen
import com.devpro58.hnem06.moneysnap.presentation.main.MainActivity
import com.devpro58.hnem06.moneysnap.presentation.welcome.WelcomeActivity

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val navigateRunnable = Runnable { navigateToNextScreen() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        handler.postDelayed(navigateRunnable, 1500)
    }

    override fun onDestroy() {
        handler.removeCallbacks(navigateRunnable)
        super.onDestroy()
    }

    private fun navigateToNextScreen() {
        if (isFinishing || isDestroyed) return

        val currentUser = FirebaseAuth.getInstance().currentUser
        val prefs = getSharedPreferences("MoneySnapPrefs", Context.MODE_PRIVATE)
        val isOnboardingDone = prefs.getBoolean("language_select_completed", false)

        val intent = when {
            currentUser != null -> Intent(this, MainActivity::class.java)
            isOnboardingDone    -> Intent(this, AuthScreen::class.java)
            else                -> Intent(this, WelcomeActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}
