package com.example.kitchenratio.data

import com.example.kitchenratio.model.Ingredient
import com.example.kitchenratio.model.IngredientUnit
import com.example.kitchenratio.model.Recipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON serialization for saved recipes. BigDecimal values are stored as plain strings
 * so no precision is lost. Corrupt entries are skipped instead of crashing.
 */
object RecipeSerializer {

    @Serializable
    private data class IngredientDto(
        val id: Long,
        val name: String,
        val quantity: String,
        val quantityText: String = "",
        val unit: String,
        val customUnit: String = ""
    )

    @Serializable
    private data class RecipeDto(
        val id: Long,
        val name: String,
        val originalServings: String,
        val newServings: String,
        val ingredients: List<IngredientDto>,
        val notes: String = "",
        val createdAt: Long
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(recipes: List<Recipe>): String = json.encodeToString(recipes.map { it.toDto() })

    fun decode(text: String?): List<Recipe> {
        if (text.isNullOrBlank()) return emptyList()
        val dtos = runCatching { json.decodeFromString<List<RecipeDto>>(text) }.getOrNull()
            ?: return emptyList()
        return dtos.mapNotNull { dto -> runCatching { dto.toModel() }.getOrNull() }
    }

    private fun Recipe.toDto() = RecipeDto(
        id = id,
        name = name,
        originalServings = originalServings.toPlainString(),
        newServings = newServings.toPlainString(),
        ingredients = ingredients.map {
            IngredientDto(
                id = it.id,
                name = it.name,
                quantity = it.quantity.toPlainString(),
                quantityText = it.quantityText,
                unit = it.unit.name,
                customUnit = it.customUnit
            )
        },
        notes = notes,
        createdAt = createdAt
    )

    private fun RecipeDto.toModel(): Recipe {
        val original = originalServings.toBigDecimal()
        val new = newServings.toBigDecimal()
        require(original.signum() > 0 && new.signum() > 0)
        return Recipe(
            id = id,
            name = name,
            originalServings = original,
            newServings = new,
            ingredients = ingredients.map {
                Ingredient(
                    id = it.id,
                    name = it.name,
                    quantity = it.quantity.toBigDecimal(),
                    unit = IngredientUnit.entries.firstOrNull { u -> u.name == it.unit }
                        ?: IngredientUnit.ITEM,
                    customUnit = it.customUnit,
                    quantityText = it.quantityText
                )
            },
            notes = notes,
            createdAt = createdAt
        )
    }
}
