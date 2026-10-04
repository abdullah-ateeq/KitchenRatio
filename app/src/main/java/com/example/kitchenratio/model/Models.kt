package com.example.kitchenratio.model

import java.math.BigDecimal

enum class QuantityDisplayMode {
    SMART,
    DECIMAL
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Cooking units. Units are never converted into each other: a quantity in cups
 * always scales to a quantity in cups.
 *
 * [plural] is used when the displayed quantity is greater than one. Abbreviations
 * keep the same spelling. [prefersFractions] controls whether Smart formatting may
 * show kitchen fractions (metric units read better as decimals: 312.5 g, not 312 1/2 g).
 */
enum class IngredientUnit(
    val label: String,
    val plural: String,
    val prefersFractions: Boolean = true
) {
    TEASPOON("tsp", "tsp"),
    TABLESPOON("tbsp", "tbsp"),
    CUP("cup", "cups"),
    FLUID_OUNCE("fl oz", "fl oz"),
    OUNCE("oz", "oz"),
    POUND("lb", "lb"),
    GRAM("g", "g", prefersFractions = false),
    KILOGRAM("kg", "kg", prefersFractions = false),
    MILLILITER("ml", "ml", prefersFractions = false),
    LITER("l", "l", prefersFractions = false),
    PINCH("pinch", "pinches"),
    PIECE("piece", "pieces"),
    CLOVE("clove", "cloves"),
    CAN("can", "cans"),
    PACKET("packet", "packets"),
    ITEM("item", "items"),
    CUSTOM("custom", "custom");

    /** Unit text for display; custom units are shown exactly as the user typed them. */
    fun displayName(plural: Boolean, customUnit: String): String = when (this) {
        CUSTOM -> customUnit.trim()
        else -> if (plural) this.plural else label
    }
}

data class Ingredient(
    val id: Long,
    val name: String,
    val quantity: BigDecimal,
    val unit: IngredientUnit,
    val customUnit: String = "",
    /** The quantity as the user typed it (e.g. "1 1/2"), used to restore the input field. */
    val quantityText: String = ""
)

data class Recipe(
    val id: Long,
    val name: String,
    val originalServings: BigDecimal,
    val newServings: BigDecimal,
    val ingredients: List<Ingredient>,
    val notes: String,
    val createdAt: Long
)

data class ScaledIngredient(
    val ingredient: Ingredient,
    val scaledQuantity: BigDecimal
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val defaultOriginalServings: BigDecimal = BigDecimal(4),
    val defaultNewServings: BigDecimal = BigDecimal(4),
    val displayMode: QuantityDisplayMode = QuantityDisplayMode.SMART,
    val decimalPlaces: Int = 2,
    val hapticsEnabled: Boolean = true,
    val showScaleFactor: Boolean = true,
    val showComparison: Boolean = true
) {
    companion object {
        val DecimalPlaceOptions = listOf(1, 2, 3)
    }
}
