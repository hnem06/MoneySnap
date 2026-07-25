package com.devpro58.hnem06.moneysnap.data.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetAlertThresholdTest {

    @Test
    fun belowFirstThresholdDoesNotAlert() {
        assertNull(highestUnnotifiedBudgetThreshold(49.9, emptySet()))
    }

    @Test
    fun sequentialSpendingReturnsNextThreshold() {
        assertEquals(65, highestUnnotifiedBudgetThreshold(66.0, setOf(50)))
    }

    @Test
    fun largeJumpReturnsHighestReachedThreshold() {
        assertEquals(80, highestUnnotifiedBudgetThreshold(92.0, emptySet()))
    }

    @Test
    fun alreadyNotifiedThresholdsAreNotRepeated() {
        assertNull(highestUnnotifiedBudgetThreshold(95.0, setOf(50, 65, 80)))
    }

    @Test
    fun totalEqualToBudgetIsNotOverBudget() {
        assertEquals(false, isOverBudget(5_000_000L, 5_000_000L))
    }

    @Test
    fun totalAboveBudgetAlwaysCountsAsOverBudget() {
        assertEquals(true, isOverBudget(5_000_001L, 5_000_000L))
    }
}
