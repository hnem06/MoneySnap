package com.devpro58.hnem06.moneysnap.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlyTrendTest {

    @Test
    fun `reports an increase`() {
        val trend = MonthlyTrend.of(currentTotal = 1_500_000, previousTotal = 1_000_000)
        assertEquals(TrendDirection.Up, trend.direction)
        assertEquals(50, trend.percent)
    }

    @Test
    fun `reports a decrease`() {
        val trend = MonthlyTrend.of(currentTotal = 800_000, previousTotal = 1_000_000)
        assertEquals(TrendDirection.Down, trend.direction)
        assertEquals(20, trend.percent)
    }

    @Test
    fun `has nothing to compare on the first month`() {
        // Dividing by zero would yield infinity; the UI hides the row entirely instead of
        // rendering "+∞%" or a fabricated 100%.
        val trend = MonthlyTrend.of(currentTotal = 1_000_000, previousTotal = 0)
        assertEquals(TrendDirection.Unknown, trend.direction)
        assertEquals(null, trend.percent)
    }

    @Test
    fun `identical totals are flat`() {
        val trend = MonthlyTrend.of(currentTotal = 1_000_000, previousTotal = 1_000_000)
        assertEquals(TrendDirection.Flat, trend.direction)
        assertEquals(0, trend.percent)
    }

    @Test
    fun `a change too small to round to one percent is flat, not a zero-percent direction`() {
        // 1,000,000 -> 1,002,000 is 0.2%. Rendering "Up 0%" reads as a contradiction.
        val trend = MonthlyTrend.of(currentTotal = 1_002_000, previousTotal = 1_000_000)
        assertEquals(TrendDirection.Flat, trend.direction)
    }

    @Test
    fun `rounds to the nearest percent rather than truncating`() {
        // 1,000,000 -> 1,336,000 is 33.6%, which truncation would report as 33%.
        val trend = MonthlyTrend.of(currentTotal = 1_336_000, previousTotal = 1_000_000)
        assertEquals(34, trend.percent)
    }

    @Test
    fun `spending nothing this month is a full decrease`() {
        val trend = MonthlyTrend.of(currentTotal = 0, previousTotal = 2_000_000)
        assertEquals(TrendDirection.Down, trend.direction)
        assertEquals(100, trend.percent)
    }
}
