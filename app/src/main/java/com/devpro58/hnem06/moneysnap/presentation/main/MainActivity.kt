package com.devpro58.hnem06.moneysnap.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.databinding.ActivityMainBinding
import com.devpro58.hnem06.moneysnap.presentation.auth.AuthScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val navParams = binding.bottomNavigation.layoutParams as
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            navParams.bottomMargin = systemBars.bottom + (16 * resources.displayMetrics.density).toInt()
            binding.bottomNavigation.layoutParams = navParams
            insets
        }

        val navHost = supportFragmentManager
            .findFragmentById(R.id.mainNavHost) as NavHostFragment
        binding.bottomNavigation.setupWithNavController(navHost.navController)
        binding.bottomNavigation.labelVisibilityMode =
            com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED

        viewModel.isOnline.observe(this) { online ->
            binding.offlineBanner.isVisible = !online
        }

        // Budget is a detail screen with its own back arrow and deliberately has no bottom-nav
        // entry. NavigationUI cannot resolve a matching menu item for it, so the previously
        // selected tab stayed highlighted and lied about where the user was.
        navHost.navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible =
                destination.id !in DETAIL_DESTINATIONS
        }

        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_OPEN_BUDGET_SETUP, false)) {
            intent.removeExtra(EXTRA_OPEN_BUDGET_SETUP)
            navHost.navController.navigate(R.id.budgetFragment)
        }

        if (intent.getBooleanExtra(EXTRA_SHOW_SESSION_ERROR, false)) {
            showSessionErrorDialog()
        }
    }

    private fun showSessionErrorDialog() {
        viewModel.clearInvalidSession()

        val dialogView = layoutInflater.inflate(R.layout.dialog_session_error, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set transparent background for the dialog window to show the card view's rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<android.view.View>(R.id.btnDialogConfirm).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, AuthScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    companion object {
        /** Screens reached from Profile that have their own back arrow and no bottom-nav entry. */
        private val DETAIL_DESTINATIONS = setOf(R.id.budgetFragment, R.id.securityFragment)

        const val EXTRA_SHOW_SESSION_ERROR = "SHOW_SESSION_ERROR"
        const val EXTRA_OPEN_BUDGET_SETUP = "OPEN_BUDGET_SETUP"
    }
}
