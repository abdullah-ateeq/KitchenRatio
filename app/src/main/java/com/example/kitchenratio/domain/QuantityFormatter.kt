package com.example.kitchenratio.domain

import com.example.kitchenratio.model.IngredientUnit
import com.example.kitchenratio.model.QuantityDisplayMode
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * All display formatting for quantities, servings and scale factors lives here.
 * Values are only rounded for display; the underlying BigDecimal is never changed.
 */
object QuantityFormatter {

    private data class KitchenFraction(val value: Double, val text: String)

    private val fractions = listOf(
        KitchenFraction(1.0 / 8, "1/8"),
        KitchenFraction(1.0 / 4, "1/4"),
        KitchenFraction(1.0 / 3, "1/3"),
        KitchenFraction(3.0 / 8, "3/8"),
        KitchenFraction(1.0 / 2, "1/2"),
        KitchenFraction(5.0 / 8, "5/8"),
        KitchenFraction(2.0 / 3, "2/3"),
        KitchenFraction(3.0 / 4, "3/4"),
        KitchenFraction(7.0 / 8, "7/8")
    )

    /** How close a value must be to a kitchen fraction to be shown as that fraction. */
    const val FRACTION_TOLERANCE = 0.02

    /** Decimal places never go beyond this, even for very small amounts. */
    private const val MAX_DECIMAL_PLACES = 4

    /**
     * Formats a quantity for display.
     *
     * SMART: 1.5 → "1 1/2", 0.25 → "1/4", 3.0 → "3", 1.3333 → "1 1/3", 1.1 → "1.1".
     * DECIMAL: rounded to [decimalPlaces] with trailing zeros removed (2.50 → "2.5").
     */
    fun formatQuantity(
        value: BigDecimal,
        mode: QuantityDisplayMode = QuantityDisplayMode.SMART,
        decimalPlaces: Int = 2,
        allowFractions: Boolean = true
    ): String {
        if (mode == QuantityDisplayMode.SMART && allowFractions) {
            smartFraction(value)?.let { return it }
        }
        return formatDecimal(value, decimalPlaces)
    }

    /** Quantity with its unit, e.g. "4 cups", "1/2 cup", "2 tbsp", "3 jars". */
    fun formatAmount(
        value: BigDecimal,
        unit: IngredientUnit,
        customUnit: String,
        mode: QuantityDisplayMode,
        decimalPlaces: Int
    ): String {
        val number = formatQuantity(value, mode, decimalPlaces, unit.prefersFractions)
        val plural = value > BigDecimal.ONE && number != "1"
        val unitText = unit.displayName(plural, customUnit)
        return if (unitText.isEmpty()) number else "$number $unitText"
    }

    /** Decimal with trailing zeros removed. Small non-zero values gain precision instead of showing "0". */
    fun formatDecimal(value: BigDecimal, decimalPlaces: Int): String {
        var places = decimalPlaces.coerceIn(0, MAX_DECIMAL_PLACES)
        var rounded = value.setScale(places, RoundingMode.HALF_UP)
        while (rounded.signum() == 0 && value.signum() != 0 && places < MAX_DECIMAL_PLACES) {
            places++
            rounded = value.setScale(places, RoundingMode.HALF_UP)
        }
        if (rounded.signum() == 0) return "0"
        return rounded.stripTrailingZeros().toPlainString()
    }

    private fun smartFraction(value: BigDecimal): String? {
        if (value.signum() <= 0) return null
        val whole = value.setScale(0, RoundingMode.FLOOR)
        val remainder = value.subtract(whole).toDouble()

        if (remainder <= FRACTION_TOLERANCE) {
            // Very small amounts (e.g. 0.01) must not collapse to "0".
            return if (whole.signum() == 0) null else whole.toPlainString()
        }
        if (remainder >= 1.0 - FRACTION_TOLERANCE) {
            return whole.add(BigDecimal.ONE).toPlainString()
        }
        val match = fractions.minBy { abs(it.value - remainder) }
        if (abs(match.value - remainder) > FRACTION_TOLERANCE) return null
        return if (whole.signum() == 0) match.text else "${whole.toPlainString()} ${match.text}"
    }

    /** "2×", "2.5×", "0.67×". */
    fun formatScaleFactor(factor: BigDecimal): String = formatDecimal(factor, 2) + "×"

    /** "+100%", "−50%", "0%". Uses a true minus sign so the sign is not lost visually. */
    fun formatPercentChange(factor: BigDecimal): String {
        val change = factor.subtract(BigDecimal.ONE).multiply(BigDecimal(100))
        val text = formatDecimal(change.abs(), 1)
        return when {
            text == "0" -> "0%"
            change.signum() > 0 -> "+$text%"
            else -> "−$text%"
        }
    }

    /** "4" or "4.5". */
    fun formatServingsNumber(value: BigDecimal): String = formatDecimal(value, 2)

    /** "1 serving", "4 servings", "4.5 servings". */
    fun formatServings(value: BigDecimal): String {
        val number = formatServingsNumber(value)
        return if (number == "1") "1 serving" else "$number servings"
    }

    /** Plain text for an input field: no exponent notation, no trailing zeros. */
    fun toInputText(value: BigDecimal): String = formatDecimal(value, MAX_DECIMAL_PLACES)
}
