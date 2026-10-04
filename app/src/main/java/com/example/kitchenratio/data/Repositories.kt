package com.example.kitchenratio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.kitchenratio.model.AppSettings
import com.example.kitchenratio.model.QuantityDisplayMode
import com.example.kitchenratio.model.Recipe
import com.example.kitchenratio.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/** Single app-wide DataStore instance. */
val Context.kitchenRatioDataStore: DataStore<Preferences> by preferencesDataStore(name = "kitchenratio")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val defaultOriginal = stringPreferencesKey("default_original_servings")
        val defaultNew = stringPreferencesKey("default_new_servings")
        val displayMode = stringPreferencesKey("display_mode")
        val decimalPlaces = intPreferencesKey("decimal_places")
        val haptics = booleanPreferencesKey("haptics")
        val showScaleFactor = booleanPreferencesKey("show_scale_factor")
        val showComparison = booleanPreferencesKey("show_comparison")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = enumOrDefault(this[Keys.theme], defaults.themeMode),
            dynamicColor = this[Keys.dynamicColor] ?: defaults.dynamicColor,
            defaultOriginalServings = positiveOrDefault(this[Keys.defaultOriginal], defaults.defaultOriginalServings),
            defaultNewServings = positiveOrDefault(this[Keys.defaultNew], defaults.defaultNewServings),
            displayMode = enumOrDefault(this[Keys.displayMode], defaults.displayMode),
            decimalPlaces = (this[Keys.decimalPlaces] ?: defaults.decimalPlaces)
                .takeIf { it in AppSettings.DecimalPlaceOptions } ?: defaults.decimalPlaces,
            hapticsEnabled = this[Keys.haptics] ?: defaults.hapticsEnabled,
            showScaleFactor = this[Keys.showScaleFactor] ?: defaults.showScaleFactor,
            showComparison = this[Keys.showComparison] ?: defaults.showComparison
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[Keys.theme] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[Keys.dynamicColor] = enabled }
    suspend fun setDefaultOriginalServings(value: BigDecimal) =
        dataStore.edit { it[Keys.defaultOriginal] = value.toPlainString() }
    suspend fun setDefaultNewServings(value: BigDecimal) =
        dataStore.edit { it[Keys.defaultNew] = value.toPlainString() }
    suspend fun setDisplayMode(mode: QuantityDisplayMode) = dataStore.edit { it[Keys.displayMode] = mode.name }
    suspend fun setDecimalPlaces(places: Int) = dataStore.edit { it[Keys.decimalPlaces] = places }
    suspend fun setHaptics(enabled: Boolean) = dataStore.edit { it[Keys.haptics] = enabled }
    suspend fun setShowScaleFactor(enabled: Boolean) = dataStore.edit { it[Keys.showScaleFactor] = enabled }
    suspend fun setShowComparison(enabled: Boolean) = dataStore.edit { it[Keys.showComparison] = enabled }

    suspend fun reset() {
        dataStore.edit { prefs ->
            listOf(
                Keys.theme, Keys.dynamicColor, Keys.defaultOriginal, Keys.defaultNew, Keys.displayMode,
                Keys.decimalPlaces, Keys.haptics, Keys.showScaleFactor, Keys.showComparison
            ).forEach { prefs.remove(it) }
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private fun positiveOrDefault(value: String?, default: BigDecimal): BigDecimal =
        value?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: default
}

class SavedRecipesRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        const val MAX_RECIPES = 50
        private val RecipesKey = stringPreferencesKey("saved_recipes")
    }

    /** Newest first. */
    val recipes: Flow<List<Recipe>> = dataStore.data.map { RecipeSerializer.decode(it[RecipesKey]) }

    /** Saves a recipe; when more than [MAX_RECIPES] exist the oldest are removed. */
    suspend fun save(recipe: Recipe) = update { current -> listOf(recipe) + current }

    suspend fun delete(id: Long) = update { current -> current.filterNot { it.id == id } }

    /** Re-inserts a recipe (used for undo), keeping newest-first order. */
    suspend fun restore(recipe: Recipe) = update { current ->
        (current.filterNot { it.id == recipe.id } + recipe)
    }

    suspend fun clearAll() {
        dataStore.edit { it.remove(RecipesKey) }
    }

    private suspend fun update(transform: (List<Recipe>) -> List<Recipe>) {
        dataStore.edit { prefs ->
            val current = RecipeSerializer.decode(prefs[RecipesKey])
            val updated = transform(current)
                .sortedByDescending { it.createdAt }
                .take(MAX_RECIPES)
            prefs[RecipesKey] = RecipeSerializer.encode(updated)
        }
    }
}
