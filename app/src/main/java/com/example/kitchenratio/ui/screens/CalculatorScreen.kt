package com.example.kitchenratio.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kitchenratio.domain.CalculatorEvaluation
import com.example.kitchenratio.domain.CalculatorInput
import com.example.kitchenratio.domain.IngredientEvaluation
import com.example.kitchenratio.domain.QuantityFormatter
import com.example.kitchenratio.model.AppSettings
import com.example.kitchenratio.model.IngredientUnit
import com.example.kitchenratio.ui.CalculatorUiState
import com.example.kitchenratio.ui.KitchenRatioViewModel
import com.example.kitchenratio.ui.ResultView
import com.example.kitchenratio.ui.ServingField
import com.example.kitchenratio.ui.components.AppIcons
import com.example.kitchenratio.ui.components.Haptics
import com.example.kitchenratio.ui.components.SectionLabel
import com.example.kitchenratio.ui.components.rememberHaptics
import com.example.kitchenratio.ui.theme.WordmarkStyle
import kotlinx.coroutines.launch
import java.math.BigDecimal

const val DISCLAIMER =
    "Scaling results are mathematical estimates. Adjust cooking, seasoning, and preparation " +
        "according to the recipe and ingredients."

private val QuickServings = listOf(1, 2, 4, 6, 8, 10, 12)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen(
    viewModel: KitchenRatioViewModel,
    state: CalculatorUiState,
    settings: AppSettings,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics(settings.hapticsEnabled)
    val resultsRequester = remember { BringIntoViewRequester() }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    fun snack(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    val actions = CalculatorActions(
        onScale = {
            if (viewModel.scale()) {
                haptics.tick()
                scope.launch { resultsRequester.bringIntoView() }
            } else {
                snack("Check the highlighted fields.")
            }
        },
        onAddIngredient = {
            if (viewModel.addIngredient()) haptics.tick()
        },
        onDeleteIngredient = { id, name ->
            val removed = viewModel.removeIngredient(id)
            if (removed != null) {
                haptics.tick()
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val label = name.trim().ifEmpty { "Ingredient" }
                    val result = snackbarHostState.showSnackbar(
                        message = "$label removed",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreIngredient(removed.first, removed.second)
                    }
                }
            }
        },
        onSave = {
            if (viewModel.saveRecipe()) {
                haptics.tick()
                snack("Recipe saved")
            } else {
                snack("Check the highlighted fields before saving.")
            }
        },
        onCopy = {
            val text = viewModel.recipeText()
            if (text != null) {
                copyToClipboard(context, text)
                snack("Recipe copied")
            } else {
                snack("Check the highlighted fields before copying.")
            }
        },
        onShare = {
            val text = viewModel.recipeText()
            if (text != null) {
                shareText(context, text)
            } else {
                snack("Check the highlighted fields before sharing.")
            }
        },
        onReset = { showResetDialog = true }
    )

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset calculator?") },
            text = { Text("This clears the recipe name, ingredients and notes. Saved recipes are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.reset()
                    haptics.tick()
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, top = 16.dp, bottom = 24.dp)
                ) {
                    Header()
                    InputSection(viewModel, state, settings, actions, showSummary = false)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 24.dp, top = 16.dp, bottom = 24.dp)
                ) {
                    if (settings.showScaleFactor) {
                        ScaleSummary(state.evaluation)
                        Spacer(Modifier.height(20.dp))
                    }
                    ResultsSection(
                        viewModel, state, settings, actions,
                        Modifier.bringIntoViewRequester(resultsRequester)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Header()
                InputSection(viewModel, state, settings, actions, showSummary = settings.showScaleFactor)
                Spacer(Modifier.height(24.dp))
                ResultsSection(
                    viewModel, state, settings, actions,
                    Modifier.bringIntoViewRequester(resultsRequester)
                )
            }
        }
    }
}

private class CalculatorActions(
    val onScale: () -> Unit,
    val onAddIngredient: () -> Unit,
    val onDeleteIngredient: (Long, String) -> Unit,
    val onSave: () -> Unit,
    val onCopy: () -> Unit,
    val onShare: () -> Unit,
    val onReset: () -> Unit
)

@Composable
private fun Header() {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(
            text = "KITCHENRATIO",
            style = WordmarkStyle,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Scale recipes up or down.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputSection(
    viewModel: KitchenRatioViewModel,
    state: CalculatorUiState,
    settings: AppSettings,
    actions: CalculatorActions,
    showSummary: Boolean
) {
    val input = state.input
    val evaluation = state.evaluation

    SectionLabel("Recipe name")
    OutlinedTextField(
        value = input.recipeName,
        onValueChange = viewModel::setRecipeName,
        placeholder = { Text(CalculatorInput.UNTITLED) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Recipe name" }
    )

    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ServingInput(
            label = "Original servings",
            value = input.originalServingsText,
            error = evaluation.originalServingsError,
            showAllErrors = state.showAllErrors,
            onValueChange = viewModel::setOriginalServings,
            onFocused = { viewModel.selectServingField(ServingField.ORIGINAL) },
            modifier = Modifier.weight(1f)
        )
        ServingInput(
            label = "New servings",
            value = input.newServingsText,
            error = evaluation.newServingsError,
            showAllErrors = state.showAllErrors,
            onValueChange = viewModel::setNewServings,
            onFocused = { viewModel.selectServingField(ServingField.NEW) },
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(8.dp))
    val target = state.selectedServingField
    val targetText = if (target == ServingField.ORIGINAL) input.originalServingsText else input.newServingsText
    Text(
        text = "Quick set · ${if (target == ServingField.ORIGINAL) "original" else "new"} servings",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickServings.forEach { value ->
            FilterChip(
                selected = targetText.trim() == value.toString(),
                onClick = { viewModel.applyQuickServing(value) },
                label = { Text(value.toString()) },
                modifier = Modifier.semantics {
                    contentDescription = "Set ${if (target == ServingField.ORIGINAL) "original" else "new"} servings to $value"
                }
            )
        }
    }

    if (showSummary) {
        Spacer(Modifier.height(16.dp))
        ScaleSummary(evaluation)
    }

    Spacer(Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("Ingredients", Modifier.weight(1f))
        Text(
            text = "${input.ingredients.size} / ${CalculatorInput.MAX_INGREDIENTS}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    evaluation.ingredients.forEachIndexed { index, item ->
        IngredientRow(
            index = index,
            item = item,
            settings = settings,
            showAllErrors = state.showAllErrors,
            canDelete = state.canDeleteIngredient,
            viewModel = viewModel,
            onDelete = { actions.onDeleteIngredient(item.input.id, item.input.name) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }

    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = actions.onAddIngredient,
        enabled = state.canAddIngredient,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Add ingredient" }
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("ADD INGREDIENT")
    }
    if (!state.canAddIngredient) {
        Text(
            text = "Maximum of ${CalculatorInput.MAX_INGREDIENTS} ingredients.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }

    Spacer(Modifier.height(20.dp))
    SectionLabel("Notes")
    OutlinedTextField(
        value = input.notes,
        onValueChange = viewModel::setNotes,
        placeholder = { Text("Optional, e.g. Add flour gradually.") },
        minLines = 2,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Notes" }
    )

    Spacer(Modifier.height(20.dp))
    Button(
        onClick = actions.onScale,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .semantics { contentDescription = "Scale recipe" }
    ) {
        Text("SCALE RECIPE", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ServingInput(
    label: String,
    value: String,
    error: String?,
    showAllErrors: Boolean,
    onValueChange: (String) -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleError = error.takeIf { showAllErrors || value.isNotBlank() }
    Column(modifier = modifier) {
        SectionLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            isError = visibleError != null,
            supportingText = visibleError?.let { { Text(it) } },
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFocused() }
                .semantics { contentDescription = label }
        )
    }
}

@Composable
private fun IngredientRow(
    index: Int,
    item: IngredientEvaluation,
    settings: AppSettings,
    showAllErrors: Boolean,
    canDelete: Boolean,
    viewModel: KitchenRatioViewModel,
    onDelete: () -> Unit
) {
    val row = item.input
    val nameError = item.nameError.takeIf { showAllErrors }
    val quantityError = item.quantityError.takeIf { showAllErrors || row.quantityText.isNotBlank() }
    val unitError = item.unitError.takeIf { showAllErrors }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .animateContentSize()
    ) {
        Row(verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                value = row.name,
                onValueChange = { viewModel.setIngredientName(row.id, it) },
                label = { Text("Ingredient ${index + 1}") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Ingredient name" }
            )
            IconButton(
                onClick = onDelete,
                enabled = canDelete,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ingredient ${row.name.ifBlank { "${index + 1}" }}"
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                value = row.quantityText,
                onValueChange = { viewModel.setIngredientQuantity(row.id, it) },
                label = { Text("Quantity") },
                singleLine = true,
                isError = quantityError != null,
                supportingText = quantityError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier
                    .weight(0.45f)
                    .semantics { contentDescription = "Ingredient quantity" }
            )
            UnitSelector(
                unit = row.unit,
                onSelect = { viewModel.setIngredientUnit(row.id, it) },
                modifier = Modifier.weight(0.55f)
            )
        }
        AnimatedVisibility(visible = row.unit == IngredientUnit.CUSTOM) {
            OutlinedTextField(
                value = row.customUnit,
                onValueChange = { viewModel.setIngredientCustomUnit(row.id, it) },
                label = { Text("Unit name") },
                placeholder = { Text("e.g. jars") },
                singleLine = true,
                isError = unitError != null,
                supportingText = unitError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .semantics { contentDescription = "Custom unit name" }
            )
        }

        val scaled = item.scaledQuantity
        if (scaled != null) {
            val amount = amountText(scaled, item, settings)
            val name = row.name.ifBlank { "ingredient ${index + 1}" }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Scaled quantity for $name is $amount"
                    }
            ) {
                Text(
                    text = "Scaled:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun UnitSelector(
    unit: IngredientUnit,
    onSelect: (IngredientUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = unit.label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Unit") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay so the whole field opens the menu.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clickable { expanded = true }
                .semantics { contentDescription = "Ingredient unit, ${unit.label}" }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            IngredientUnit.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (option == unit) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ScaleSummary(evaluation: CalculatorEvaluation) {
    val factor = evaluation.scaleFactor
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (factor == null) {
            Text(
                text = "Enter both serving sizes to see the scale factor.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(20.dp)
            )
            return@Surface
        }
        val original = evaluation.originalServings ?: return@Surface
        val new = evaluation.newServings ?: return@Surface
        val direction = when (factor.compareTo(BigDecimal.ONE)) {
            1 -> "Scaling up ↑"
            -1 -> "Scaling down ↓"
            else -> "Same size"
        }
        val factorText = QuantityFormatter.formatScaleFactor(factor)
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SCALE FACTOR",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(text = direction, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                text = factorText,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { contentDescription = "Scale factor $factorText, $direction" }
            )
            Text(
                text = "${QuantityFormatter.formatServingsNumber(original)} → ${QuantityFormatter.formatServings(new)}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryStat("Original", QuantityFormatter.formatServings(original), Modifier.weight(1f))
                SummaryStat("New", QuantityFormatter.formatServings(new), Modifier.weight(1f))
                SummaryStat("Change", QuantityFormatter.formatPercentChange(factor), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ResultsSection(
    viewModel: KitchenRatioViewModel,
    state: CalculatorUiState,
    settings: AppSettings,
    actions: CalculatorActions,
    modifier: Modifier = Modifier
) {
    val evaluation = state.evaluation
    val rows = evaluation.scaledRows
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SCALED RECIPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = state.input.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                evaluation.newServings?.let {
                    Text(
                        text = QuantityFormatter.formatServings(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (settings.showComparison && rows.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            val options = listOf(ResultView.SCALED to "SCALED", ResultView.COMPARE to "COMPARE")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (view, label) ->
                    SegmentedButton(
                        selected = state.resultView == view,
                        onClick = { viewModel.setResultView(view) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) { Text(label) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (rows.isEmpty()) {
            Text(
                text = "Enter serving sizes and ingredient quantities to see the scaled recipe.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            val compare = settings.showComparison && state.resultView == ResultView.COMPARE
            rows.forEachIndexed { index, item ->
                ResultRow(item, index, settings, compare)
                if (index < rows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        if (evaluation.invalidCount > 0 && evaluation.scaleFactor != null) {
            val count = evaluation.invalidCount
            Text(
                text = "$count ingredient${if (count == 1) "" else "s"} need${if (count == 1) "s" else ""} attention.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = actions.onSave,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Save recipe" }
            ) {
                Icon(AppIcons.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("SAVE")
            }
            OutlinedButton(
                onClick = actions.onCopy,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Copy recipe" }
            ) {
                Icon(AppIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("COPY")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = actions.onShare,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Share recipe" }
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("SHARE")
            }
            TextButton(
                onClick = actions.onReset,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Reset calculator" }
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("RESET")
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = DISCLAIMER,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ResultRow(item: IngredientEvaluation, index: Int, settings: AppSettings, compare: Boolean) {
    val scaled = item.scaledQuantity ?: return
    val quantity = item.quantity ?: return
    val name = item.input.name.trim().ifEmpty { "Ingredient ${index + 1}" }
    val scaledText = amountText(scaled, item, settings)
    val originalText = amountText(quantity, item, settings)
    val description = if (compare) {
        "$name, original $originalText, scaled $scaledText"
    } else {
        "Scaled quantity for $name is $scaledText"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(12.dp))
        if (compare) {
            Text(
                text = "$originalText  →  ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = scaledText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun amountText(value: BigDecimal, item: IngredientEvaluation, settings: AppSettings): String =
    QuantityFormatter.formatAmount(
        value = value,
        unit = item.input.unit,
        customUnit = item.input.customUnit.ifBlank { "" },
        mode = settings.displayMode,
        decimalPlaces = settings.decimalPlaces
    )

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Recipe", text))
}

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share recipe"))
}
