package com.devpro58.hnem06.moneysnap.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.MoneyFormatter
import com.devpro58.hnem06.moneysnap.data.local.dao.ExpenseDao
import com.devpro58.hnem06.moneysnap.domain.repository.SettingsRepository
import com.devpro58.hnem06.moneysnap.presentation.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetAlertNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val expenseDao: ExpenseDao,
    private val settingsRepository: SettingsRepository
) {

    private val alertPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.budget_alert_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.budget_alert_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    suspend fun notifyIfNeeded(userId: String, alwaysNotifyOverBudget: Boolean = false) {
        if (!settingsRepository.isMonthlyBudgetConfigured()) return
        if (!canPostNotifications()) return

        val budget = settingsRepository.getMonthlyBudget().takeIf { it > 0L } ?: return
        val now = Calendar.getInstance()
        val monthStart = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMonth = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val total = expenseDao.getTotalForPeriod(
            userId = userId,
            startMillis = monthStart.timeInMillis,
            endMillis = nextMonth.timeInMillis
        )
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.ROOT).format(monthStart.time)
        if (alwaysNotifyOverBudget && isOverBudget(total, budget)) {
            markThresholdsNotified(userId, monthKey, BUDGET_THRESHOLDS.last())
            postOverBudgetNotification(userId, monthKey, total, budget)
            return
        }

        val percentage = total * 100.0 / budget
        val notifiedThresholds = BUDGET_THRESHOLDS
            .filterTo(mutableSetOf()) { wasNotified(userId, monthKey, it) }
        val reachedThreshold = highestUnnotifiedBudgetThreshold(
            percentage = percentage,
            notifiedThresholds = notifiedThresholds
        ) ?: return

        postNotification(userId, monthKey, reachedThreshold, total, budget)
        markThresholdsNotified(userId, monthKey, reachedThreshold)
    }

    private fun markThresholdsNotified(userId: String, monthKey: String, upTo: Int) {
        alertPreferences.edit().apply {
            BUDGET_THRESHOLDS.filter { it <= upTo }.forEach { threshold ->
                putBoolean(alertKey(userId, monthKey, threshold), true)
            }
        }.apply()
    }

    private fun postOverBudgetNotification(
        userId: String,
        monthKey: String,
        total: Long,
        budget: Long
    ) {
        postNotification(
            userId = userId,
            monthKey = monthKey,
            threshold = OVER_BUDGET_NOTIFICATION_ID,
            total = total,
            budget = budget,
            title = context.getString(R.string.budget_over_notification_title),
            content = context.getString(
                R.string.budget_over_notification_content,
                MoneyFormatter.formatVnd(total - budget),
                MoneyFormatter.formatVnd(total),
                MoneyFormatter.formatVnd(budget)
            )
        )
    }

    private fun postNotification(
        userId: String,
        monthKey: String,
        threshold: Int,
        total: Long,
        budget: Long,
        title: String = context.getString(R.string.budget_alert_notification_title, threshold),
        content: String = context.getString(
            R.string.budget_alert_notification_content,
            MoneyFormatter.formatVnd(total),
            MoneyFormatter.formatVnd(budget)
        )
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            threshold,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(content)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(
            (userId + monthKey + threshold).hashCode(),
            notification
        )
    }

    private fun canPostNotifications(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED)

    private fun wasNotified(userId: String, monthKey: String, threshold: Int): Boolean =
        alertPreferences.getBoolean(alertKey(userId, monthKey, threshold), false)

    private fun alertKey(userId: String, monthKey: String, threshold: Int) =
        "$userId:$monthKey:$threshold"

    private companion object {
        const val CHANNEL_ID = "monthly_budget_alerts"
        const val PREFS_NAME = "MoneySnapBudgetAlerts"
        const val OVER_BUDGET_NOTIFICATION_ID = 100
    }
}

internal val BUDGET_THRESHOLDS = listOf(50, 65, 80)

internal fun highestUnnotifiedBudgetThreshold(
    percentage: Double,
    notifiedThresholds: Set<Int>
): Int? = BUDGET_THRESHOLDS.lastOrNull { threshold ->
    percentage >= threshold && threshold !in notifiedThresholds
}

internal fun isOverBudget(total: Long, budget: Long): Boolean =
    budget > 0L && total > budget
