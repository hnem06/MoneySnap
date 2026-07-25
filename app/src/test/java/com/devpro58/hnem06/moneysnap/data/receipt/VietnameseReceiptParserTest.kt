package com.devpro58.hnem06.moneysnap.data.receipt

import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VietnameseReceiptParserTest {

    @Test
    fun totalKeywordWinsOverCashAndChange() {
        val draft = VietnameseReceiptParser.parse(
            """
            TỔNG CỘNG 85.000
            TIỀN KHÁCH ĐƯA 100.000
            TIỀN THỪA 15.000
            """.trimIndent()
        )

        assertEquals(85_000L, draft.totalAmount)
    }

    @Test
    fun parsesVietnameseDateAndFoodCategory() {
        val draft = VietnameseReceiptParser.parse(
            """
            CÀ PHÊ
            Ngày 16/07/2026 12:30
            TOTAL 50,000
            """.trimIndent(),
            nowMillis = Calendar.getInstance().apply { set(2026, 6, 16) }.timeInMillis
        )

        val date = Calendar.getInstance().apply { timeInMillis = draft.spentAtMillis!! }
        assertEquals(16, date.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.JULY, date.get(Calendar.MONTH))
        assertEquals(ExpenseCategory.Food, draft.suggestedCategory)
    }

    @Test
    fun doesNotInventFieldsWhenTextHasNoReceiptData() {
        val draft = VietnameseReceiptParser.parse("Cảm ơn quý khách")

        assertNull(draft.totalAmount)
        assertNull(draft.spentAtMillis)
        assertNull(draft.suggestedCategory)
    }
}
