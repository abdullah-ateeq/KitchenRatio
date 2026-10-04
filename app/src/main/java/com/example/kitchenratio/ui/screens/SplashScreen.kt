package com.example.kitchenratio.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.kitchenratio.ui.theme.WordmarkStyle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** ~1.4 s: ingredient dots/lines appear, servings count 4 → 8, lines scale, title fades in. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
        delay(300)
        onFinished()
    }
    val p = progress.value
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.outlineVariant
    val servings = 4 + (4 * ((p - 0.3f) / 0.6f).coerceIn(0f, 1f)).roundToInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "KitchenRatio, scale recipes with ease" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = "$servings servings",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                modifier = Modifier.graphicsLayer { alpha = (p * 3f).coerceIn(0f, 1f) }
            )
            Spacer(Modifier.height(16.dp))
            Canvas(modifier = Modifier.size(width = 200.dp, height = 72.dp)) {
                val rows = 3
                val baseWidths = listOf(0.35f, 0.25f, 0.45f)
                val stroke = 8.dp.toPx()
                repeat(rows) { i ->
                    val appear = ((p - i * 0.1f) / 0.25f).coerceIn(0f, 1f)
                    val grow = ((p - 0.3f) / 0.6f).coerceIn(0f, 1f)
                    val y = size.height * (i + 0.5f) / rows
                    val dotX = 8.dp.toPx()
                    drawCircle(primary.copy(alpha = appear), radius = 6.dp.toPx() * appear, center = Offset(dotX, y))
                    val startX = dotX + 18.dp.toPx()
                    val maxLen = size.width - startX
                    drawLine(track.copy(alpha = appear), Offset(startX, y), Offset(size.width, y), stroke, StrokeCap.Round)
                    val fraction = baseWidths[i] * (1f + grow) // quantities visually double
                    drawLine(
                        primary.copy(alpha = appear),
                        Offset(startX, y),
                        Offset(startX + maxLen * fraction * appear, y),
                        stroke,
                        StrokeCap.Round
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            val titleAlpha = ((p - 0.45f) / 0.45f).coerceIn(0f, 1f)
            Text(
                "KITCHENRATIO",
                style = WordmarkStyle.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha }
            )
            Text(
                "Scale recipes with ease",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha }
            )
        }
    }
}
