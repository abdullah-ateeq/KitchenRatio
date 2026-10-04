package com.example.kitchenratio.domain

import com.example.kitchenratio.model.Ingredient
import com.example.kitchenratio.model.ScaledIngredient
import java.math.BigDecimal
import java.math.MathContext

/**
 * The scaling engine. All arithmetic uses BigDecimal with 34 significant digits;
 * nothing is rounded here. Rounding only happens in [QuantityFormatter] for display.
 */
object KitchenRatioCalculator {

    private val Precision: MathContext = MathContext.DECIMAL128

    /** Scale factor = new servings ÷ original servings. Both must be greater than zero. */
    fun calculateScaleFactor(
        originalServings: BigDecimal,
        newServings: BigDecimal
    ): BigDecimal {
        require(originalServings.signum() > 0) { "Original servings must be greater than zero." }
        require(newServings.signum() > 0) { "New servings must be greater than zero." }
        return newServings.divide(originalServings, Precision)
    }

    /** Scaled quantity = original quantity × scale factor. */
    fun scaleQuantity(
        originalQuantity: BigDecimal,
        scaleFactor: BigDecimal
    ): BigDecimal {
        require(originalQuantity.signum() >= 0) { "Quantity cannot be negative." }
        require(scaleFactor.signum() > 0) { "Scale factor must be greater than zero." }
        return originalQuantity.multiply(scaleFactor, Precision)
    }

    fun scaleIngredients(
        ingredients: List<Ingredient>,
        scaleFactor: BigDecimal
    ): List<ScaledIngredient> = ingredients.map { ingredient ->
        ScaledIngredient(
            ingredient = ingredient,
            scaledQuantity = scaleQuantity(ingredient.quantity, scaleFactor)
        )
    }
}
