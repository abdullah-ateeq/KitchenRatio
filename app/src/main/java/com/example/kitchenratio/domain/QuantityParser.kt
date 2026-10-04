package com.example.kitchenratio.domain

import java.math.BigDecimal
import java.math.MathContext

sealed interface ParseResult {
    data object Empty : ParseResult
    data object Invalid : ParseResult
    data object Negative : ParseResult
    data object Zero : ParseResult
    data class Valid(val value: BigDecimal) : ParseResult
}

/**
 * Parses user-entered numbers safely. Accepts:
 * - whole numbers and decimals: "2", "0.5", "1.25", ".5", "2,5"
 * - simple fractions: "1/2", "3/4"
 * - mixed numbers: "1 1/2"
 * - unicode fractions: "½", "1½"
 * Never throws.
 */
object QuantityParser {

    private val decimalRegex = Regex("""^(\d+\.?\d*|\.\d+)$""")
    private val fractionRegex = Regex("""^(\d+)\s*/\s*(\d+)$""")
    private val mixedRegex = Regex("""^(\d+)\s+(\d+)\s*/\s*(\d+)$""")

    private val unicodeFractions = mapOf(
        '½' to "1/2", '⅓' to "1/3", '⅔' to "2/3", '¼' to "1/4", '¾' to "3/4",
        '⅛' to "1/8", '⅜' to "3/8", '⅝' to "5/8", '⅞' to "7/8"
    )

    /** Inputs longer than this are rejected to keep values sensible. */
    const val MAX_LENGTH = 12

    fun parse(input: String): ParseResult {
        var text = input.trim()
        if (text.isEmpty()) return ParseResult.Empty
        if (text.length > MAX_LENGTH + 4) return ParseResult.Invalid

        var negative = false
        if (text.startsWith("-") || text.startsWith("−")) {
            negative = true
            text = text.substring(1).trim()
        }

        text = buildString {
            text.forEach { c ->
                val fraction = unicodeFractions[c]
                if (fraction != null) append(' ').append(fraction) else append(c)
            }
        }.trim().replace(Regex("""\s+"""), " ")

        if (!text.contains('.') && text.count { it == ',' } == 1) {
            text = text.replace(',', '.')
        }

        val value = parseUnsigned(text) ?: return ParseResult.Invalid
        return when {
            negative && value.signum() != 0 -> ParseResult.Negative
            value.signum() == 0 -> ParseResult.Zero
            else -> ParseResult.Valid(value)
        }
    }

    private fun parseUnsigned(text: String): BigDecimal? {
        if (decimalRegex.matches(text)) {
            return text.removeSuffix(".").toBigDecimalOrNull()
        }
        fractionRegex.matchEntire(text)?.let { match ->
            val (numerator, denominator) = match.destructured
            return divide(numerator, denominator)
        }
        mixedRegex.matchEntire(text)?.let { match ->
            val (whole, numerator, denominator) = match.destructured
            val fraction = divide(numerator, denominator) ?: return null
            val wholeValue = whole.toBigDecimalOrNull() ?: return null
            return wholeValue.add(fraction)
        }
        return null
    }

    private fun divide(numerator: String, denominator: String): BigDecimal? {
        val n = numerator.toBigDecimalOrNull() ?: return null
        val d = denominator.toBigDecimalOrNull() ?: return null
        if (d.signum() == 0) return null
        return n.divide(d, MathContext.DECIMAL128)
    }
}
