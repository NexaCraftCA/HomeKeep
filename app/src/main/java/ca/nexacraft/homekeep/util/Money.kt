package ca.nexacraft.homekeep.util

import java.text.NumberFormat
import java.util.Currency

fun formatMoney(value: Double?, currency: String? = "CAD"): String? {
    if (value == null) return null
    val code = currency?.trim()?.takeIf { it.length == 3 } ?: "CAD"
    return runCatching {
        NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(code)
        }.format(value)
    }.getOrElse {
        "$code ${"%.2f".format(value)}"
    }
}

fun Double?.asEditable(): String = this?.let {
    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
} ?: ""

fun String.toDoubleOrNullSafe(): Double? = trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
