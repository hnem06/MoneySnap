package com.devpro58.hnem06.moneysnap.core.utils

/** Direction of a month-over-month change, kept separate from its presentation. */
enum class TrendDirection { Up, Down, Flat, Unknown }

/**
 * Month-over-month comparison, shared by Home and Stats.
 *
 * Extracted because both screens need it and neither could test it: the logic lived inline in
 * StatsFragment, and Home simply never computed it (its `monthlyTrendText` view was left
 * permanently gone).
 */
data class MonthlyTrend(
    val direction: TrendDirection,
    /** Absolute percentage change, or null when there is no previous month to compare against. */
    val percent: Int?
) {
    companion object {
        fun of(currentTotal: Long, previousTotal: Long): MonthlyTrend = when {
            // Dividing by a zero baseline yields infinity, and "+∞% vs last month" is noise
            // rather than information — the user's first month has nothing to compare to.
            previousTotal <= 0L -> MonthlyTrend(TrendDirection.Unknown, null)
            currentTotal == previousTotal -> MonthlyTrend(TrendDirection.Flat, 0)
            else -> {
                val delta = currentTotal - previousTotal
                val percent = Math.round(Math.abs(delta) * 100.0 / previousTotal).toInt()
                val direction = if (delta > 0) TrendDirection.Up else TrendDirection.Down
                // A change too small to round to 1% is reported as flat rather than as a
                // direction with a "0%" magnitude, which reads as a contradiction.
                if (percent == 0) MonthlyTrend(TrendDirection.Flat, 0)
                else MonthlyTrend(direction, percent)
            }
        }
    }
}
