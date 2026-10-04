package com.example.kitchenratio.domain

import com.example.kitchenratio.model.Ingredient
import com.example.kitchenratio.model.IngredientUnit
import com.example.kitchenratio.model.QuantityDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class KitchenRatioCalculatorTest {

    private fun bd(value: String) = BigDecimal(value)
    private fun assertNumEquals(expected: String, actual: BigDecimal) =
        assertEquals(0, bd(expected).compareTo(actual))

    private fun scale(original: String, new: String, quantity: String): BigDecimal {
        val factor = KitchenRatioCalculator.calculateScaleFactor(bd(original), bd(new))
        return KitchenRatioCalculator.scaleQuantity(bd(quantity), factor)
    }

    @Test fun scalingUp() {
        assertNumEquals("2", KitchenRatioCalculator.calculateScaleFactor(bd("4"), bd("8")))
        assertNumEquals("4", scale("4", "8", "2"))
        assertNumEquals("5", scale("4", "10", "2"))
    }

    @Test fun scalingDown() {
        assertNumEquals("0.5", KitchenRatioCalculator.calculateScaleFactor(bd("4"), bd("2")))
        assertNumEquals("1", scale("4", "2", "2"))
        assertNumEquals("1", scale("8", "4", "2"))
    }

    @Test fun sameServings() {
        assertNumEquals("1", KitchenRatioCalculator.calculateScaleFactor(bd("6"), bd("6")))
        assertNumEquals("2.5", scale("6", "6", "2.5"))
    }

    @Test fun scaleFactorCalculation() {
        assertNumEquals("2.5", KitchenRatioCalculator.calculateScaleFactor(bd("4"), bd("10")))
        val twoThirds = KitchenRatioCalculator.calculateScaleFactor(bd("6"), bd("4"))
        assertTrue(twoThirds.toPlainString().startsWith("0.666666"))
        assertEquals("2.5×", QuantityFormatter.formatScaleFactor(bd("2.5")))
        assertEquals("2×", QuantityFormatter.formatScaleFactor(bd("2")))
        assertEquals("0.5×", QuantityFormatter.formatScaleFactor(bd("0.5")))
        assertEquals("0.67×", QuantityFormatter.formatScaleFactor(twoThirds))
        assertEquals("+100%", QuantityFormatter.formatPercentChange(bd("2")))
        assertEquals("−50%", QuantityFormatter.formatPercentChange(bd("0.5")))
    }

    @Test fun sixToFourGivesOneAndAThird() {
        val scaled = scale("6", "4", "2")
        assertEquals("1 1/3", QuantityFormatter.formatQuantity(scaled, QuantityDisplayMode.SMART))
    }

    @Test fun decimalServingSizes() {
        assertNumEquals("3", scale("4.5", "6.75", "2"))
        assertNumEquals("1.5", KitchenRatioCalculator.calculateScaleFactor(bd("4.5"), bd("6.75")))
    }

    @Test fun decimalIngredientQuantities() {
        assertNumEquals("3.75", scale("4", "10", "1.5"))
        assertNumEquals("0.625", scale("4", "2", "1.25"))
    }

    @Test fun noIntermediateRounding() {
        // 1/3 factor then ×3 must come back to the original quantity.
        val factor = KitchenRatioCalculator.calculateScaleFactor(bd("3"), bd("1"))
        val back = KitchenRatioCalculator.scaleQuantity(
            KitchenRatioCalculator.scaleQuantity(bd("2"), factor), bd("3")
        )
        assertEquals("2", QuantityFormatter.formatDecimal(back, 3))
    }

    @Test fun multipleIngredients() {
        val ingredients = listOf(
            Ingredient(1, "Flour", bd("2"), IngredientUnit.CUP),
            Ingredient(2, "Milk", bd("1.5"), IngredientUnit.CUP),
            Ingredient(3, "Sugar", bd("2"), IngredientUnit.TABLESPOON),
            Ingredient(4, "Eggs", bd("2"), IngredientUnit.ITEM)
        )
        val scaled = KitchenRatioCalculator.scaleIngredients(ingredients, bd("2"))
        assertEquals(listOf("4", "3", "4", "4"), scaled.map { it.scaledQuantity.stripTrailingZeros().toPlainString() })
        assertEquals(ingredients, scaled.map { it.ingredient })
    }

    @Test fun customUnitsArePreservedExactly() {
        val text = QuantityFormatter.formatAmount(bd("4"), IngredientUnit.CUSTOM, " jars ", QuantityDisplayMode.SMART, 2)
        assertEquals("4 jars", text)
        assertEquals("1 Jar", QuantityFormatter.formatAmount(bd("1"), IngredientUnit.CUSTOM, "Jar", QuantityDisplayMode.SMART, 2))
    }

    @Test fun unitsAreNotConverted() {
        assertEquals(
            "1000 ml",
            QuantityFormatter.formatAmount(scale("4", "8", "500"), IngredientUnit.MILLILITER, "", QuantityDisplayMode.SMART, 2)
        )
        assertEquals(
            "4 cups",
            QuantityFormatter.formatAmount(scale("4", "8", "2"), IngredientUnit.CUP, "", QuantityDisplayMode.SMART, 2)
        )
        assertEquals(
            "1/2 cup",
            QuantityFormatter.formatAmount(bd("0.5"), IngredientUnit.CUP, "", QuantityDisplayMode.SMART, 2)
        )
    }
}
