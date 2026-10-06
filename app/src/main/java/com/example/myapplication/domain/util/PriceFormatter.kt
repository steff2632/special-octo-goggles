package com.example.myapplication.domain.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Formats prices supplied in minor units (pence) as GBP, e.g. 1000 -> "£10.00". */
object PriceFormatter {
    private val currency: Currency = Currency.getInstance("GBP")

    fun format(pence: Long?): String? {
        if (pence == null || pence < 0) return null
        val formatter = NumberFormat.getCurrencyInstance(Locale.UK).apply {
            currency = this@PriceFormatter.currency
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return formatter.format(BigDecimal.valueOf(pence).movePointLeft(2))
    }
}
