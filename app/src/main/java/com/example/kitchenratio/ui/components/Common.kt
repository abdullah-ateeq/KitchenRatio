package com.example.kitchenratio.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Small uppercase label above a section, e.g. "ORIGINAL SERVINGS". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(bottom = 6.dp)
            .semantics { heading() }
    )
}

/** Light, subtle haptic tick that respects the user's setting. Never used while typing. */
class Haptics(private val enabled: Boolean, private val perform: () -> Unit) {
    fun tick() {
        if (enabled) perform()
    }
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(enabled, feedback) {
        Haptics(enabled) { feedback.performHapticFeedback(HapticFeedbackType.ContextClick) }
    }
}
