package com.devpro58.hnem06.moneysnap.core.utils

import java.text.NumberFormat
import java.util.Locale

object MoneyFormatter {
    private val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

    fun formatVnd(amount: Long): String =
        "${formatter.format(amount)} đ"
}
