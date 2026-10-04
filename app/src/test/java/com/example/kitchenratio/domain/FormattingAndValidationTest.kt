package com.example.kitchenratio.domain

import com.example.kitchenratio.model.IngredientUnit
import com.example.kitchenratio.model.QuantityDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FormattingAndValidationTest {

    private fun smart(value: String) = QuantityFormatter.formatQuantity(BigDecimal(value), QuantityDisplayMode.SMART)
    private fun decimal(value: String, places: Int = 2) =
        QuantityFormatter.formatQuantity(BigDecimal(value), QuantityDisplayMode.DECIMAL, places)

    @Test fun smartFractionFormatting() {
        assertEquals("1", smart("1.00"))
        assertEquals("3", smart("3.00"))
        assertEquals("1/2", smart("0.50"))
        assertEquals("1/4", smart("0.25"))
        assertEquals("3/4", smart("0.75"))
        assertEquals("1/8", smart("0.125"))
        assertEquals("1/3", smart("0.333"))
        assertEquals("3/8", smart("0.375"))
        assertEquals("5/8", smart("0.625"))
        assertEquals("2/3", smart("0.666"))
        assertEquals("7/8", smart("0.875"))
        assertEquals("1 1/2", smart("1.5"))
        assertEquals("2 1/4", smart("2.25"))
        assertEquals("2 3/4", smart("2.75"))
        // Not near a kitchen fraction → sensible decimal.
        assertEquals("1.1", smart("1.1"))
        // Tiny amounts must never become "0".
        assertEquals("0.01", smart("0.01"))
    }

    @Test fun smartModeUsesDecimalsForMetricUnits() {
        assertEquals(
            "312.5 g",
            QuantityFormatter.formatAmount(BigDecimal("312.5"), IngredientUnit.GRAM, "", QuantityDisplayMode.SMART, 2)
        )
    }

    @Test fun decimalFormatting() {
        assertEquals("1.33", decimal("1.33333"))
        assertEquals("1.3", decimal("1.33333", 1))
        assertEquals("1.333", decimal("1.33333", 3))
        assertEquals("2.5", decimal("2.50"))
        assertEquals("4", decimal("4.000"))
        assertEquals("100", decimal("100"))
        assertEquals("0.001", decimal("0.001", 1))
    }

    @Test fun parserAcceptsDecimalsAndFractions() {
        fun value(text: String) = (QuantityParser.parse(text) as ParseResult.Valid).value
        assertEquals(0, BigDecimal("0.5").compareTo(value("1/2")))
        assertEquals(0, BigDecimal("0.75").compareTo(value("3/4")))
        assertEquals(0, BigDecimal("1.5").compareTo(value("1 1/2")))
        assertEquals(0, BigDecimal("1.5").compareTo(value("1½")))
        assertEquals(0, BigDecimal("2.5").compareTo(value("2,5")))
        assertEquals(0, BigDecimal("0.5").compareTo(value(".5")))
        assertEquals(0, BigDecimal("4.5").compareTo(value(" 4.5 ")))
    }

    @Test fun zeroValidation() {
        assertEquals(ParseResult.Zero, QuantityParser.parse("0"))
        assertEquals(ParseResult.Zero, QuantityParser.parse("0/4"))
        assertEquals(ValidationMessages.INVALID_QUANTITY, RecipeEvaluator.quantityError(QuantityParser.parse("0")))
        assertEquals(ValidationMessages.SERVINGS_ABOVE_ZERO, RecipeEvaluator.servingsError(QuantityParser.parse("0")))
        val eval = RecipeEvaluator.evaluate(input(original = "0"))
        assertNull(eval.scaleFactor)
        assertFalse(eval.isValid)
    }

    @Test fun negativeValidation() {
        assertEquals(ParseResult.Negative, QuantityParser.parse("-2"))
        assertEquals(ValidationMessages.NEGATIVE, RecipeEvaluator.servingsError(QuantityParser.parse("-4")))
        val eval = RecipeEvaluator.evaluate(input(quantity = "-1"))
        assertEquals(ValidationMessages.NEGATIVE, eval.ingredients.single().quantityError)
        assertFalse(eval.isValid)
    }

    @Test fun emptyAndInvalidInput() {
        assertEquals(ParseResult.Invalid, QuantityParser.parse("abc"))
        assertEquals(ParseResult.Invalid, QuantityParser.parse("1/0"))
        val eval = RecipeEvaluator.evaluate(input(original = "", name = " ", quantity = ""))
        assertEquals(ValidationMessages.ENTER_SERVINGS, eval.originalServingsError)
        assertEquals(ValidationMessages.ENTER_NAME, eval.ingredients.single().nameError)
        assertEquals(ValidationMessages.ENTER_QUANTITY, eval.ingredients.single().quantityError)
    }

    @Test fun customUnitRequiresName() {
        val eval = RecipeEvaluator.evaluate(input(unit = IngredientUnit.CUSTOM))
        assertEquals(ValidationMessages.ENTER_UNIT, eval.ingredients.single().unitError)
    }

    @Test fun liveEvaluationScalesValidRows() {
        val eval = RecipeEvaluator.evaluate(input(original = "4", new = "10", quantity = "2"))
        assertTrue(eval.isValid)
        assertEquals(0, BigDecimal("5").compareTo(eval.ingredients.single().scaledQuantity))
    }

    @Test fun copyTextIsClean() {
        val recipe = RecipeEvaluator.toRecipe(
            CalculatorInput(
                recipeName = "Simple Pancakes",
                originalServingsText = "4",
                newServingsText = "8",
                ingredients = listOf(
                    IngredientInput(1, "Flour", "2", IngredientUnit.CUP),
                    IngredientInput(2, "Milk", "1.5", IngredientUnit.CUP),
                    IngredientInput(3, "Sugar", "2", IngredientUnit.TABLESPOON),
                    IngredientInput(4, "Oil", "1", IngredientUnit.TABLESPOON)
                )
            ),
            id = 1, createdAt = 0
        )!!
        val expected = """
            Simple Pancakes
            4 servings → 8 servings
            Scale factor: 2×

            Flour: 4 cups
            Milk: 3 cups
            Sugar: 4 tbsp
            Oil: 2 tbsp
        """.trimIndent()
        assertEquals(expected, RecipeTextFormatter.format(recipe, QuantityDisplayMode.SMART, 2))
    }

    private fun input(
        original: String = "4",
        new: String = "8",
        name: String = "Flour",
        quantity: String = "2",
        unit: IngredientUnit = IngredientUnit.CUP
    ) = CalculatorInput(
        originalServingsText = original,
        newServingsText = new,
        ingredients = listOf(IngredientInput(1, name, quantity, unit))
    )
}
