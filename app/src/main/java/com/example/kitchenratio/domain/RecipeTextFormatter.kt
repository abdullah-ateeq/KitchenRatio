package com.example.kitchenratio.domain

import com.example.kitchenratio.model.QuantityDisplayMode
import com.example.kitchenratio.model.Recipe

/** Plain-text recipe summary used by Copy and Share. */
object RecipeTextFormatter {

    fun format(
        recipe: Recipe,
        mode: QuantityDisplayMode,
        decimalPlaces: Int
    ): String {
        val factor = KitchenRatioCalculator.calculateScaleFactor(
            recipe.originalServings,
            recipe.newServings
        )
        val scaled = KitchenRatioCalculator.scaleIngredients(recipe.ingredients, factor)
        return buildString {
            appendLine(recipe.name)
            append(QuantityFormatter.formatServings(recipe.originalServings))
            append(" → ")
            appendLine(QuantityFormatter.formatServings(recipe.newServings))
            appendLine("Scale factor: ${QuantityFormatter.formatScaleFactor(factor)}")
            appendLine()
            scaled.forEach { item ->
                val amount = QuantityFormatter.formatAmount(
                    item.scaledQuantity,
                    item.ingredient.unit,
                    item.ingredient.customUnit,
                    mode,
                    decimalPlaces
                )
                appendLine("${item.ingredient.name}: $amount")
            }
            if (recipe.notes.isNotBlank()) {
                appendLine()
                appendLine("Notes: ${recipe.notes.trim()}")
            }
        }.trimEnd()
    }
}
