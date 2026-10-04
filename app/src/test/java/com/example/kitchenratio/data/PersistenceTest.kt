package com.example.kitchenratio.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.kitchenratio.domain.CalculatorInput
import com.example.kitchenratio.domain.IngredientInput
import com.example.kitchenratio.domain.RecipeEvaluator
import com.example.kitchenratio.model.AppSettings
import com.example.kitchenratio.model.Ingredient
import com.example.kitchenratio.model.IngredientUnit
import com.example.kitchenratio.model.QuantityDisplayMode
import com.example.kitchenratio.model.Recipe
import com.example.kitchenratio.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal

class PersistenceTest {

    @get:Rule val tmp = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var file: File

    @Before fun setUp() {
        file = File(tmp.root, "test.preferences_pb")
    }

    @After fun tearDown() {
        if (::scope.isInitialized) scope.cancel()
    }

    /** Creating a fresh store over the same file simulates an app restart. */
    private fun newStore(): DataStore<Preferences> {
        if (::scope.isInitialized) scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    private fun recipe(id: Long, name: String = "Recipe $id") = Recipe(
        id = id,
        name = name,
        originalServings = BigDecimal("4"),
        newServings = BigDecimal("6.5"),
        ingredients = listOf(
            Ingredient(1, "Flour", BigDecimal("0.5"), IngredientUnit.CUP, quantityText = "1/2"),
            Ingredient(2, "Jam", BigDecimal("2"), IngredientUnit.CUSTOM, customUnit = "jars")
        ),
        notes = "Add flour gradually.",
        createdAt = id
    )

    @Test fun recipeSerializationRoundTrip() {
        val recipes = listOf(recipe(1), recipe(2, "Chicken Curry"))
        val decoded = RecipeSerializer.decode(RecipeSerializer.encode(recipes))
        assertEquals(recipes, decoded)
    }

    @Test fun corruptDataDoesNotCrash() {
        assertEquals(emptyList<Recipe>(), RecipeSerializer.decode("{not json"))
        assertEquals(emptyList<Recipe>(), RecipeSerializer.decode(null))
    }

    @Test fun savedRecipeRestore() {
        val input = RecipeEvaluator.fromRecipe(recipe(1))
        assertEquals("4", input.originalServingsText)
        assertEquals("6.5", input.newServingsText)
        assertEquals("1/2", input.ingredients[0].quantityText)
        assertEquals(IngredientUnit.CUSTOM, input.ingredients[1].unit)
        assertEquals("jars", input.ingredients[1].customUnit)
        assertEquals("Add flour gradually.", input.notes)
        val evaluation = RecipeEvaluator.evaluate(input)
        assertTrue(evaluation.isValid)
        assertEquals(0, BigDecimal("0.8125").compareTo(evaluation.ingredients[0].scaledQuantity))
    }

    @Test fun untitledRecipeName() {
        val recipe = RecipeEvaluator.toRecipe(
            CalculatorInput(ingredients = listOf(IngredientInput(1, "Salt", "1", IngredientUnit.PINCH))),
            id = 1, createdAt = 1
        )!!
        assertEquals(CalculatorInput.UNTITLED, recipe.name)
    }

    @Test fun savedRecipesPersistAndCapAtFifty() = runBlocking {
        val repo = SavedRecipesRepository(newStore())
        (1L..51L).forEach { repo.save(recipe(it)) }
        val restarted = SavedRecipesRepository(newStore()).recipes.first()
        assertEquals(SavedRecipesRepository.MAX_RECIPES, restarted.size)
        assertEquals(51L, restarted.first().id)
        assertTrue(restarted.none { it.id == 1L }) // oldest removed
    }

    @Test fun settingsPersistence() = runBlocking {
        val repo = SettingsRepository(newStore())
        assertEquals(AppSettings(), repo.settings.first())
        repo.setThemeMode(ThemeMode.DARK)
        repo.setDisplayMode(QuantityDisplayMode.DECIMAL)
        repo.setDecimalPlaces(3)
        repo.setDefaultNewServings(BigDecimal("8"))
        repo.setHaptics(false)

        val restarted = SettingsRepository(newStore()).settings.first()
        assertEquals(ThemeMode.DARK, restarted.themeMode)
        assertEquals(QuantityDisplayMode.DECIMAL, restarted.displayMode)
        assertEquals(3, restarted.decimalPlaces)
        assertEquals(0, BigDecimal("8").compareTo(restarted.defaultNewServings))
        assertEquals(false, restarted.hapticsEnabled)

        SettingsRepository(newStore()).apply { reset() }
        assertEquals(AppSettings(), SettingsRepository(newStore()).settings.first())
    }
}
