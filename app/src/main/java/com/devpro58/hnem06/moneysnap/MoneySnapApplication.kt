package com.devpro58.hnem06.moneysnap

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.devpro58.hnem06.moneysnap.core.logging.CrashlyticsTree
import com.devpro58.hnem06.moneysnap.domain.repository.SettingsRepository
import com.devpro58.hnem06.moneysnap.data.notification.BudgetAlertNotifier
import com.devpro58.hnem06.moneysnap.data.sync.ExpenseSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class MoneySnapApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var budgetAlertNotifier: BudgetAlertNotifier

    @Inject
    lateinit var expenseSyncScheduler: ExpenseSyncScheduler

    override fun onCreate() {
        super.onCreate()
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashlyticsTree())
        AppCompatDelegate.setDefaultNightMode(
            if (settingsRepository.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        budgetAlertNotifier.ensureChannel()

        // Recover anything the per-expense workers lost track of, and keep sweeping in the
        // background so a device that is rarely opened still converges with the backend.
        // Both are idempotent, so running them on every process start is safe.
        expenseSyncScheduler.enqueuePendingSync()
        expenseSyncScheduler.ensurePeriodicSync()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Coil singleton config. [respectCacheHeaders] = false makes Coil keep and reuse the
     * downloaded receipt from its persistent disk cache instead of re-fetching on every app
     * launch — Firebase Storage responses carry no-cache headers that would otherwise defeat it.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .respectCacheHeaders(false)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("receipt_image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .build()
}
