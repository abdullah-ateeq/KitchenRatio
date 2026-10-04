package com.example.kitchenratio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.kitchenratio.data.SavedRecipesRepository
import com.example.kitchenratio.data.SettingsRepository
import com.example.kitchenratio.data.kitchenRatioDataStore
import com.example.kitchenratio.domain.CalculatorEvaluation
import com.example.kitchenratio.domain.CalculatorInput
import com.example.kitchenratio.domain.IngredientInput
import com.example.kitchenratio.domain.QuantityFormatter
import com.example.kitchenratio.domain.QuantityParser
import com.example.kitchenratio.domain.RecipeEvaluator
import com.example.kitchenratio.domain.RecipeTextFormatter
import com.example.kitchenratio.model.AppSettings
import com.example.kitchenratio.model.IngredientUnit
import com.example.kitchenratio.model.QuantityDisplayMode
import com.example.kitchenratio.model.Recipe
import com.example.kitchenratio.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class ResultView { SCALED, COMPARE }

enum class ServingField { ORIGINAL, NEW }

data class CalculatorUiState(
    val input: CalculatorInput,
    val evaluation: CalculatorEvaluation,
    /** Becomes true after Scale/Save is tapped, so empty fields are only flagged on request. */
    val showAllErrors: Boolean = false,
    val resultView: ResultView = ResultView.SCALED,
    val selectedServingField: ServingField = ServingField.NEW
) {
    val canAddIngredient: Boolean
        get() = input.ingredients.size < CalculatorInput.MAX_INGREDIENTS
    val canDeleteIngredient: Boolean
        get() = input.ingredients.size > CalculatorInput.MIN_INGREDIENTS
}

class KitchenRatioViewModel(
    private val settingsRepository: SettingsRepository,
    private val recipesRepository: SavedRecipesRepository
) : ViewModel() {

    private var nextId = 1L

    private val _state = MutableStateFlow(stateFor(sampleRecipe()))
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val savedRecipes: StateFlow<List<Recipe>> = recipesRepository.recipes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun newId(): Long = nextId++

    /** First-launch example so the app is understandable immediately. */
    private fun sampleRecipe() = CalculatorInput(
        recipeName = "Simple Pancakes",
        originalServingsText = "4",
        newServingsText = "8",
        ingredients = listOf(
            IngredientInput(newId(), "Flour", "2", IngredientUnit.CUP),
            IngredientInput(newId(), "Milk", "1.5", IngredientUnit.CUP),
            IngredientInput(newId(), "Sugar", "2", IngredientUnit.TABLESPOON),
            IngredientInput(newId(), "Oil", "1", IngredientUnit.TABLESPOON)
        )
    )

    private fun stateFor(input: CalculatorInput) =
        CalculatorUiState(input = input, evaluation = RecipeEvaluator.evaluate(input))

    private fun updateInput(transform: (CalculatorInput) -> CalculatorInput) {
        _state.update { current ->
            val input = transform(current.input)
            current.copy(input = input, evaluation = RecipeEvaluator.evaluate(input))
        }
    }

    private fun updateIngredient(id: Long, transform: (IngredientInput) -> IngredientInput) =
        updateInput { input ->
            input.copy(ingredients = input.ingredients.map { if (it.id == id) transform(it) else it })
        }

    // region Calculator input

    fun setRecipeName(value: String) = updateInput { it.copy(recipeName = value.take(60)) }
    fun setNotes(value: String) = updateInput { it.copy(notes = value.take(500)) }
    fun setOriginalServings(value: String) =
        updateInput { it.copy(originalServingsText = value.take(QuantityParser.MAX_LENGTH)) }
    fun setNewServings(value: String) =
        updateInput { it.copy(newServingsText = value.take(QuantityParser.MAX_LENGTH)) }

    fun selectServingField(field: ServingField) = _state.update { it.copy(selectedServingField = field) }

    fun applyQuickServing(value: Int) {
        when (_state.value.selectedServingField) {
            ServingField.ORIGINAL -> setOriginalServings(value.toString())
            ServingField.NEW -> setNewServings(value.toString())
        }
    }

    fun setIngredientName(id: Long, value: String) = updateIngredient(id) { it.copy(name = value.take(40)) }
    fun setIngredientQuantity(id: Long, value: String) =
        updateIngredient(id) { it.copy(quantityText = value.take(QuantityParser.MAX_LENGTH)) }
    fun setIngredientUnit(id: Long, unit: IngredientUnit) = updateIngredient(id) { it.copy(unit = unit) }
    fun setIngredientCustomUnit(id: Long, value: String) =
        updateIngredient(id) { it.copy(customUnit = value.take(20)) }

    fun addIngredient(): Boolean {
        if (!_state.value.canAddIngredient) return false
        updateInput { it.copy(ingredients = it.ingredients + IngredientInput(newId())) }
        return true
    }

    /** Removes a row and returns it with its index so the UI can offer Undo. */
    fun removeIngredient(id: Long): Pair<Int, IngredientInput>? {
        val current = _state.value
        if (!current.canDeleteIngredient) return null
        val index = current.input.ingredients.indexOfFirst { it.id == id }
        if (index < 0) return null
        val removed = current.input.ingredients[index]
        updateInput { it.copy(ingredients = it.ingredients.filterNot { row -> row.id == id }) }
        return index to removed
    }

    fun restoreIngredient(index: Int, ingredient: IngredientInput) = updateInput { input ->
        if (input.ingredients.size >= CalculatorInput.MAX_INGREDIENTS) return@updateInput input
        val list = input.ingredients.toMutableList()
        list.add(index.coerceIn(0, list.size), ingredient)
        input.copy(ingredients = list)
    }

    fun setResultView(view: ResultView) = _state.update { it.copy(resultView = view) }

    /** Validates everything and reveals errors. Returns true when the recipe is fully valid. */
    fun scale(): Boolean {
        _state.update { it.copy(showAllErrors = true) }
        return _state.value.evaluation.isValid
    }

    fun reset() {
        val s = settings.value
        _state.value = stateFor(
            CalculatorInput(
                recipeName = "",
                originalServingsText = QuantityFormatter.toInputText(s.defaultOriginalServings),
                newServingsText = QuantityFormatter.toInputText(s.defaultNewServings),
                ingredients = listOf(IngredientInput(newId()), IngredientInput(newId())),
                notes = ""
            )
        )
    }

    fun loadRecipe(recipe: Recipe) {
        val input = RecipeEvaluator.fromRecipe(recipe)
        // Re-number rows so new rows never collide with restored ids.
        val renumbered = input.copy(ingredients = input.ingredients.map { it.copy(id = newId()) })
        _state.value = stateFor(renumbered)
    }

    // endregion

    // region Saving / sharing

    private fun currentRecipe(): Recipe? {
        val now = System.currentTimeMillis()
        return RecipeEvaluator.toRecipe(_state.value.input, id = now, createdAt = now)
    }

    /** Returns false (and reveals errors) when the recipe is not valid. */
    fun saveRecipe(): Boolean {
        _state.update { it.copy(showAllErrors = true) }
        val recipe = currentRecipe() ?: return false
        viewModelScope.launch { recipesRepository.save(recipe) }
        return true
    }

    /** Plain-text summary, or null if the recipe is not valid. */
    fun recipeText(): String? {
        _state.update { it.copy(showAllErrors = true) }
        val recipe = currentRecipe() ?: return null
        val s = settings.value
        return RecipeTextFormatter.format(recipe, s.displayMode, s.decimalPlaces)
    }

    fun deleteRecipe(recipe: Recipe) = viewModelScope.launch { recipesRepository.delete(recipe.id) }
    fun restoreRecipe(recipe: Recipe) = viewModelScope.launch { recipesRepository.restore(recipe) }
    fun clearSavedRecipes() = viewModelScope.launch { recipesRepository.clearAll() }

    // endregion

    // region Settings

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    fun setDefaultOriginalServings(value: BigDecimal) =
        viewModelScope.launch { settingsRepository.setDefaultOriginalServings(value) }
    fun setDefaultNewServings(value: BigDecimal) =
        viewModelScope.launch { settingsRepository.setDefaultNewServings(value) }
    fun setDisplayMode(mode: QuantityDisplayMode) = viewModelScope.launch { settingsRepository.setDisplayMode(mode) }
    fun setDecimalPlaces(places: Int) = viewModelScope.launch { settingsRepository.setDecimalPlaces(places) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { settingsRepository.setHaptics(enabled) }
    fun setShowScaleFactor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setShowScaleFactor(enabled) }
    fun setShowComparison(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setShowComparison(enabled)
        if (!enabled) _state.update { it.copy(resultView = ResultView.SCALED) }
    }
    fun resetSettings() = viewModelScope.launch { settingsRepository.reset() }

    // endregion

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val store = app.kitchenRatioDataStore
                KitchenRatioViewModel(SettingsRepository(store), SavedRecipesRepository(store))
            }
        }
    }
}
