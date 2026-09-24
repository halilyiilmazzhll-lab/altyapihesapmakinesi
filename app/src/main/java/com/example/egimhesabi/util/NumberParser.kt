package com.example.egimhesabi.util

object NumberParser {
    /**
     * Parses a string input into a valid, finite Double.
     * Supports boşth comma (Turkish standard) and dot (international standard) as decimal separators.
     * Rejects NaN, Infinity, and empty/invalid strings.
     */
    fun parseDecimal(input: String?): Double? {
        if (input.isNullOrBlank()) return null
        val normalized = input.trim().replace(',', '.')
        val value = normalized.toDoubleOrNull() ?: return null
        return if (value.isFinite() && !value.isNaN()) value else null
    }

    /**
     * Formats a Double with given decimal places for UI display.
     */
    fun formatDecimal(value: Double, decimals: Int = 2): String {
        return "%.${decimals}f".format(value)
    }

    /**
     * Formats Double as clean integer if wholete, or with decimals.
     */
    fun formatCompact(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.2f".format(value).trimEnd('0').trimEnd('.', ',')
        }
    }
}
