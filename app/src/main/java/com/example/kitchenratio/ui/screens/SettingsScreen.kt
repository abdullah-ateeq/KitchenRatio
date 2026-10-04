package com.example.kitchenratio.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kitchenratio.domain.ParseResult
import com.example.kitchenratio.domain.QuantityFormatter
import com.example.kitchenratio.domain.QuantityParser
import com.example.kitchenratio.domain.RecipeEvaluator
import com.example.kitchenratio.model.AppSettings
import com.example.kitchenratio.model.QuantityDisplayMode
import com.example.kitchenratio.model.ThemeMode
import com.example.kitchenratio.ui.KitchenRatioViewModel
import com.example.kitchenratio.ui.components.SectionLabel
import com.example.kitchenratio.ui.theme.WordmarkStyle
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun SettingsScreen(
    viewModel: KitchenRatioViewModel,
    settings: AppSettings,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }

    when (confirm) {
        "settings" -> ConfirmDialog(
            title = "Reset settings?",
            text = "All settings return to their defaults. Saved recipes are kept.",
            confirmLabel = "Reset",
            onConfirm = {
                viewModel.resetSettings()
                scope.launch { snackbarHostState.showSnackbar("Settings reset") }
            },
            onDismiss = { confirm = null }
        )
        "recipes" -> ConfirmDialog(
            title = "Clear saved recipes?",
            text = "All saved recipes will be permanently deleted.",
            confirmLabel = "Clear",
            onConfirm = {
                viewModel.clearSavedRecipes()
                scope.launch { snackbarHostState.showSnackbar("Saved recipes cleared") }
            },
            onDismiss = { confirm = null }
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("SETTINGS", style = WordmarkStyle, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(20.dp))

            SectionLabel("Theme")
            Segmented(
                options = listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.SYSTEM to "System"),
                selected = settings.themeMode,
                onSelect = { viewModel.setThemeMode(it) }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow("Dynamic color", "Use colors from your wallpaper", settings.dynamicColor) {
                    viewModel.setDynamicColor(it)
                }
            }
            Divider()

            SectionLabel("Default servings")
            Text(
                "Used when the calculator is reset.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DefaultServingField(
                    label = "Original",
                    value = settings.defaultOriginalServings,
                    onValid = { viewModel.setDefaultOriginalServings(it) },
                    modifier = Modifier.weight(1f)
                )
                DefaultServingField(
                    label = "New",
                    value = settings.defaultNewServings,
                    onValid = { viewModel.setDefaultNewServings(it) },
                    modifier = Modifier.weight(1f)
                )
            }
            Divider()

            SectionLabel("Quantity display")
            Segmented(
                options = listOf(QuantityDisplayMode.SMART to "Smart fractions", QuantityDisplayMode.DECIMAL to "Decimals"),
                selected = settings.displayMode,
                onSelect = { viewModel.setDisplayMode(it) }
            )
            Text(
                text = "Smart: 1.5 → 1 1/2 · Decimals: 1.5",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(16.dp))
            SectionLabel("Decimal places")
            Segmented(
                options = AppSettings.DecimalPlaceOptions.map { it to it.toString() },
                selected = settings.decimalPlaces,
                onSelect = { viewModel.setDecimalPlaces(it) }
            )
            Divider()

            SwitchRow("Haptic feedback", "Subtle feedback on main actions", settings.hapticsEnabled) {
                viewModel.setHaptics(it)
            }
            SwitchRow("Show scale factor", "Display the scale factor summary", settings.showScaleFactor) {
                viewModel.setShowScaleFactor(it)
            }
            SwitchRow("Show comparison", "Allow original vs scaled comparison", settings.showComparison) {
                viewModel.setShowComparison(it)
            }
            Divider()

            OutlinedButton(onClick = { confirm = "settings" }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset settings")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { confirm = "recipes" }, modifier = Modifier.fillMaxWidth()) {
                Text("Clear saved recipes", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 20.dp)
    )
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) { Text(label, maxLines = 1) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun DefaultServingField(
    label: String,
    value: BigDecimal,
    onValid: (BigDecimal) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable { mutableStateOf(QuantityFormatter.toInputText(value)) }
    // Follow external changes (e.g. Reset settings) when the field is not mid-edit.
    LaunchedEffect(value) {
        val parsed = (QuantityParser.parse(text) as? ParseResult.Valid)?.value
        if (parsed == null || parsed.compareTo(value) != 0) text = QuantityFormatter.toInputText(value)
    }
    val result = QuantityParser.parse(text)
    val error = RecipeEvaluator.servingsError(result)
    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText.take(QuantityParser.MAX_LENGTH)
            (QuantityParser.parse(text) as? ParseResult.Valid)?.let { onValid(it.value) }
        },
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.semantics { contentDescription = "Default $label servings" }
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
