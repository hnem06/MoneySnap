package com.devpro58.hnem06.moneysnap.data.receipt

import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptDraft
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptParseWarning
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object VietnameseReceiptParser {

    private val totalKeywords = listOf(
        "TỔNG CỘNG", "TONG CONG", "TỔNG TIỀN", "TONG TIEN",
        "THANH TOÁN", "THANH TOAN", "GRAND TOTAL", "TOTAL"
    )
    private val ignoredAmountKeywords = listOf(
        "TIỀN KHÁCH", "TIEN KHACH", "TIỀN THỪA", "TIEN THUA",
        "GIẢM GIÁ", "GIAM GIA", "DISCOUNT", "CASH", "CHANGE"
    )
    private val amountPattern = Regex("(?<!\\d)\\d{1,3}(?:[.,\\s]\\d{3})+(?!\\d)|(?<!\\d)\\d{4,12}(?!\\d)")
    private val datePattern = Regex("(?<!\\d)(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2}|\\d{4})(?!\\d)")

    fun parse(rawText: String, nowMillis: Long = System.currentTimeMillis()): ReceiptDraft {
        val lines = rawText.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        val total = findTotal(lines)
        val date = findDate(lines, nowMillis)
        val category = suggestCategory(rawText)
        val warnings = buildList {
            if (total == null) add(ReceiptParseWarning.TotalNotFound)
            if (date == null) add(ReceiptParseWarning.DateNotFound)
        }
        return ReceiptDraft(
            totalAmount = total,
            spentAtMillis = date,
            suggestedCategory = category,
            rawText = rawText,
            warnings = warnings
        )
    }

    private fun findTotal(lines: List<String>): Long? {
        val preferred = lines.filter { line ->
            val normalized = line.uppercase(Locale.ROOT)
            totalKeywords.any(normalized::contains) && ignoredAmountKeywords.none(normalized::contains)
        }.flatMap(::amountsIn)
        if (preferred.isNotEmpty()) return preferred.maxOrNull()

        return lines
            .filter { line -> ignoredAmountKeywords.none(line.uppercase(Locale.ROOT)::contains) }
            .flatMap(::amountsIn)
            .filter { it in MIN_REASONABLE_AMOUNT..MAX_REASONABLE_AMOUNT }
            .maxOrNull()
    }

    private fun amountsIn(line: String): List<Long> = amountPattern.findAll(line)
        .mapNotNull { match -> match.value.filter(Char::isDigit).toLongOrNull() }
        .filter { it in MIN_REASONABLE_AMOUNT..MAX_REASONABLE_AMOUNT }
        .toList()

    private fun findDate(lines: List<String>, nowMillis: Long): Long? {
        val maxFuture = nowMillis + FUTURE_TOLERANCE_MILLIS
        for (line in lines) {
            for (match in datePattern.findAll(line)) {
                val day = match.groupValues[1].toIntOrNull() ?: continue
                val month = match.groupValues[2].toIntOrNull() ?: continue
                var year = match.groupValues[3].toIntOrNull() ?: continue
                if (year < 100) year += 2000
                val calendar = Calendar.getInstance().apply {
                    isLenient = false
                    clear()
                    set(year, month - 1, day, 12, 0, 0)
                }
                val timestamp = runCatching { calendar.timeInMillis }.getOrNull() ?: continue
                if (timestamp in MIN_DATE_MILLIS..maxFuture) return timestamp
            }
        }
        return null
    }

    private fun suggestCategory(text: String): ExpenseCategory? {
        val value = text.uppercase(Locale.ROOT)
        return when {
            containsAny(value, "CÀ PHÊ", "CAFE", "COFFEE", "NHÀ HÀNG", "RESTAURANT", "CƠM", "PHỞ", "FOOD") -> ExpenseCategory.Food
            containsAny(value, "GRAB", "TAXI", "XĂNG", "PETROL", "PARKING", "BÃI XE") -> ExpenseCategory.Transport
            containsAny(value, "SIÊU THỊ", "SIEU THI", "MART", "SHOP", "STORE") -> ExpenseCategory.Shopping
            containsAny(value, "TIỀN ĐIỆN", "TIEN DIEN", "TIỀN NƯỚC", "INTERNET", "TELECOM") -> ExpenseCategory.Bills
            else -> null
        }
    }

    private fun containsAny(value: String, vararg keywords: String): Boolean =
        keywords.any(value::contains)

    private const val MIN_REASONABLE_AMOUNT = 1_000L
    private const val MAX_REASONABLE_AMOUNT = 1_000_000_000L
    private const val FUTURE_TOLERANCE_MILLIS = 24L * 60L * 60L * 1000L
    private val MIN_DATE_MILLIS = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
        .parse("01/01/2000")!!.time
}
