package com.example.kitchenratio.domain

import com.example.kitchenratio.model.Ingredient
import com.example.kitchenratio.model.IngredientUnit
import com.example.kitchenratio.model.Recipe
import com.example.kitchenratio.model.ScaledIngredient
import java.math.BigDecimal

/** One editable ingredient row, holding raw text exactly as typed. */
data class IngredientInput(
    val id: Long,
    val name: String = "",
    val quantityText: String = "",
    val unit: IngredientUnit = IngredientUnit.CUP,
    val customUnit: String = ""
)

/** Everything the user can edit on the calculator. */
data class CalculatorInput(
    val recipeName: String = "",
    val originalServingsText: String = "4",
    val newServingsText: String = "8",
    val ingredients: List<IngredientInput> = emptyList(),
    val notes: String = ""
) {
    companion object {
        const val MIN_INGREDIENTS = 1
        const val MAX_INGREDIENTS = 30
        const val UNTITLED = "Untitled Recipe"
    }

    val displayName: String get() = recipeName.trim().ifEmpty { UNTITLED }
}

object ValidationMessages {
    const val ENTER_SERVINGS = "Enter serving sizes."
    const val SERVINGS_ABOVE_ZERO = "Servings must be greater than zero."
    const val INVALID_SERVINGS = "Enter a valid serving size."
    const val NEGATIVE = "Value cannot be negative."
    const val ENTER_QUANTITY = "Enter an ingredient quantity."
    const val INVALID_QUANTITY = "Enter a valid quantity."
    const val ENTER_NAME = "Enter an ingredient name."
    const val ENTER_UNIT = "Enter a unit name."
}

data class IngredientEvaluation(
    val input: IngredientInput,
    val quantity: BigDecimal?,
    val scaledQuantity: BigDecimal?,
    val nameError: String?,
    val quantityError: String?,
    val unitError: String?
) {
    val isValid: Boolean get() = nameError == null && quantityError == null && unitError == null
}

data class CalculatorEvaluation(
    val originalServings: BigDecimal?,
    val newServings: BigDecimal?,
    val originalServingsError: String?,
    val newServingsError: String?,
    val scaleFactor: BigDecimal?,
    val ingredients: List<IngredientEvaluation>
) {
    val isValid: Boolean
        get() = scaleFactor != null && ingredients.isNotEmpty() && ingredients.all { it.isValid }

    /** Rows that can be shown in the scaled recipe right now (quantity valid, servings valid). */
    val scaledRows: List<IngredientEvaluation>
        get() = ingredients.filter { it.scaledQuantity != null }

    val invalidCount: Int get() = ingredients.count { !it.isValid }
}

/**
 * Validates the calculator input and computes the scaled values.
 * Pure function: the UI layer only renders the result.
 */
object RecipeEvaluator {

    fun evaluate(input: CalculatorInput): CalculatorEvaluation {
        val original = QuantityParser.parse(input.originalServingsText)
        val new = QuantityParser.parse(input.newServingsText)
        val originalValue = (original as? ParseResult.Valid)?.value
        val newValue = (new as? ParseResult.Valid)?.value
        val factor = if (originalValue != null && newValue != null) {
            KitchenRatioCalculator.calculateScaleFactor(originalValue, newValue)
        } else {
            null
        }

        val ingredients = input.ingredients.map { row ->
            val parsed = QuantityParser.parse(row.quantityText)
            val quantity = (parsed as? ParseResult.Valid)?.value
            IngredientEvaluation(
                input = row,
                quantity = quantity,
                scaledQuantity = if (quantity != null && factor != null) {
                    KitchenRatioCalculator.scaleQuantity(quantity, factor)
                } else {
                    null
                },
                nameError = if (row.name.isBlank()) ValidationMessages.ENTER_NAME else null,
                quantityError = quantityError(parsed),
                unitError = if (row.unit == IngredientUnit.CUSTOM && row.customUnit.isBlank()) {
                    ValidationMessages.ENTER_UNIT
                } else {
                    null
                }
            )
        }

        return CalculatorEvaluation(
            originalServings = originalValue,
            newServings = newValue,
            originalServingsError = servingsError(original),
            newServingsError = servingsError(new),
            scaleFactor = factor,
            ingredients = ingredients
        )
    }

    fun servingsError(result: ParseResult): String? = when (result) {
        ParseResult.Empty -> ValidationMessages.ENTER_SERVINGS
        ParseResult.Negative -> ValidationMessages.NEGATIVE
        ParseResult.Zero -> ValidationMessages.SERVINGS_ABOVE_ZERO
        ParseResult.Invalid -> ValidationMessages.INVALID_SERVINGS
        is ParseResult.Valid -> null
    }

    fun quantityError(result: ParseResult): String? = when (result) {
        ParseResult.Empty -> ValidationMessages.ENTER_QUANTITY
        ParseResult.Negative -> ValidationMessages.NEGATIVE
        ParseResult.Zero, ParseResult.Invalid -> ValidationMessages.INVALID_QUANTITY
        is ParseResult.Valid -> null
    }

    /** Builds a [Recipe] from valid input, or null if anything is invalid. */
    fun toRecipe(input: CalculatorInput, id: Long, createdAt: Long): Recipe? {
        val evaluation = evaluate(input)
        if (!evaluation.isValid) return null
        return Recipe(
            id = id,
            name = input.displayName,
            originalServings = evaluation.originalServings ?: return null,
            newServings = evaluation.newServings ?: return null,
            ingredients = evaluation.ingredients.map { it.toIngredient() ?: return null },
            notes = input.notes.trim(),
            createdAt = createdAt
        )
    }

    /** Restores editable input from a saved recipe. */
    fun fromRecipe(recipe: Recipe): CalculatorInput = CalculatorInput(
        recipeName = if (recipe.name == CalculatorInput.UNTITLED) "" else recipe.name,
        originalServingsText = QuantityFormatter.toInputText(recipe.originalServings),
        newServingsText = QuantityFormatter.toInputText(recipe.newServings),
        ingredients = recipe.ingredients.map { ingredient ->
            IngredientInput(
                id = ingredient.id,
                name = ingredient.name,
                quantityText = ingredient.quantityText.ifBlank {
                    QuantityFormatter.toInputText(ingredient.quantity)
                },
                unit = ingredient.unit,
                customUnit = ingredient.customUnit
            )
        },
        notes = recipe.notes
    )

    /** Scaled ingredients for a fully valid input, using the calculator engine. */
    fun scaledIngredients(input: CalculatorInput): List<ScaledIngredient>? {
        val recipe = toRecipe(input, id = 0, createdAt = 0) ?: return null
        val factor = KitchenRatioCalculator.calculateScaleFactor(
            recipe.originalServings,
            recipe.newServings
        )
        return KitchenRatioCalculator.scaleIngredients(recipe.ingredients, factor)
    }

    private fun IngredientEvaluation.toIngredient(): Ingredient? {
        if (!isValid) return null
        return Ingredient(
            id = input.id,
            name = input.name.trim(),
            quantity = quantity ?: return null,
            unit = input.unit,
            customUnit = input.customUnit.trim(),
            quantityText = input.quantityText.trim()
        )
    }
}
