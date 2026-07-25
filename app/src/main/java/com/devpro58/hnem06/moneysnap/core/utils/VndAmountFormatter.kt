package com.devpro58.hnem06.moneysnap.core.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

object VndAmountFormatter {
    private val locale = Locale.forLanguageTag("vi-VN")

    fun format(amount: Long): String = NumberFormat.getIntegerInstance(locale).format(amount)

    fun parse(value: CharSequence?): Long? = value
        ?.filter(Char::isDigit)
        ?.toString()
        ?.takeIf { it.isNotEmpty() }
        ?.toLongOrNull()
}

fun EditText.enableVndAmountFormatting() {
    var changingText = false
    var previousValue = text?.toString().orEmpty()

    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(editable: Editable?) {
            if (changingText) return
            val value = editable?.toString().orEmpty()
            if (value.isEmpty()) {
                previousValue = ""
                return
            }

            val amount = VndAmountFormatter.parse(value)
            val formatted = amount?.let(VndAmountFormatter::format) ?: previousValue
            if (formatted == value) {
                previousValue = formatted
                return
            }

            changingText = true
            setText(formatted)
            setSelection(formatted.length)
            changingText = false
            previousValue = formatted
        }
    })

    VndAmountFormatter.parse(text)?.let { amount ->
        val formatted = VndAmountFormatter.format(amount)
        setText(formatted)
        setSelection(formatted.length)
        previousValue = formatted
    }
}
