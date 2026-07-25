package com.devpro58.hnem06.moneysnap.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VndAmountFormatterTest {
    @Test
    fun format_usesVietnameseThousandsSeparators() {
        assertEquals("1.000.000", VndAmountFormatter.format(1_000_000L))
    }

    @Test
    fun parse_removesGroupingCharacters() {
        assertEquals(2_165_000L, VndAmountFormatter.parse("2.165.000"))
    }

    @Test
    fun parse_returnsNullForEmptyOrOverflowingValue() {
        assertNull(VndAmountFormatter.parse(""))
        assertNull(VndAmountFormatter.parse("999999999999999999999999"))
    }
}
