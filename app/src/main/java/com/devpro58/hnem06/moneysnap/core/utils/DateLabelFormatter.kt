package com.devpro58.hnem06.moneysnap.core.utils

import android.content.Context
import com.devpro58.hnem06.moneysnap.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateLabelFormatter {
    fun time(timestamp: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp)

    fun historyGroup(context: Context, timestamp: Long): String {
        val target = startOfDay(timestamp)
        val today = startOfDay(System.currentTimeMillis())
        val yesterday = today - ONE_DAY_MILLIS

        return when (target) {
            today -> context.getString(R.string.date_today)
            yesterday -> context.getString(R.string.date_yesterday)
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp)
        }
    }

    private fun startOfDay(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
}
