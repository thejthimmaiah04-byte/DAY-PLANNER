package com.liana.dayplanner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.data.Priority

val Priority.color: Color
    get() = when (this) {
        Priority.URGENT -> Terracotta
        Priority.HIGH -> Champagne
        Priority.MEDIUM -> Sage
        Priority.LOW -> Slate
    }

private val MeridianColors = darkColorScheme(
    primary = Champagne,
    onPrimary = OnChampagne,
    primaryContainer = ChampagneDeep,
    onPrimaryContainer = Ivory,
    secondary = Sage,
    onSecondary = Ink,
    background = Ink,
    onBackground = Ivory,
    surface = Ink,
    onSurface = Ivory,
    surfaceVariant = Surface2,
    onSurfaceVariant = IvoryDim,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface2,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Terracotta,
    onError = Ink
)

private val MeridianShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MeridianTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MeridianColors,
        typography = MeridianTypography,
        shapes = MeridianShapes,
        content = content
    )
}
