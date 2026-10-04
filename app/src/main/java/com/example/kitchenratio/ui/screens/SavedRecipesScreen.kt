package com.example.kitchenratio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kitchenratio.data.SavedRecipesRepository
import com.example.kitchenratio.domain.QuantityFormatter
import com.example.kitchenratio.model.Recipe
import com.example.kitchenratio.ui.KitchenRatioViewModel
import com.example.kitchenratio.ui.components.AppIcons
import com.example.kitchenratio.ui.theme.WordmarkStyle
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun SavedRecipesScreen(
    viewModel: KitchenRatioViewModel,
    recipes: List<Recipe>,
    snackbarHostState: SnackbarHostState,
    onOpen: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all saved recipes?") },
            text = { Text("All ${recipes.size} saved recipes will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearSavedRecipes()
                }) { Text("Clear all") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("SAVED", style = WordmarkStyle, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "${recipes.size} of ${SavedRecipesRepository.MAX_RECIPES} recipes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (recipes.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) { Text("Clear all") }
                    }
                }
            }
            if (recipes.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp)
                    ) {
                        Icon(
                            AppIcons.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No saved recipes yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Tap SAVE on the calculator to keep a scaled recipe here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            items(recipes, key = { it.id }) { recipe ->
                SavedRecipeRow(
                    recipe = recipe,
                    onOpen = { onOpen(recipe) },
                    onDelete = {
                        viewModel.deleteRecipe(recipe)
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = "${recipe.name} deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) viewModel.restoreRecipe(recipe)
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun SavedRecipeRow(recipe: Recipe, onOpen: () -> Unit, onDelete: () -> Unit) {
    val servings = "${QuantityFormatter.formatServingsNumber(recipe.originalServings)} → " +
        QuantityFormatter.formatServings(recipe.newServings)
    val count = recipe.ingredients.size
    val ingredients = "$count ingredient${if (count == 1) "" else "s"}"
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(recipe.createdAt))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open recipe", onClick = onOpen)
            .padding(vertical = 14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${recipe.name}, $servings, $ingredients, saved $date"
            }
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                recipe.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(servings, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "$ingredients · Last updated $date",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete recipe ${recipe.name}")
        }
    }
}
