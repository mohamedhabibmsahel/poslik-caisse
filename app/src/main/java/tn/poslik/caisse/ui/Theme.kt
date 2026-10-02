package tn.poslik.caisse.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tn.poslik.caisse.domain.Category

// Surfaces neutres explicites : sans elles, Material 3 retombe sur ses teintes violettes par défaut.
private val Light = lightColorScheme(
    primary = Color(0xFF0B6E5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EFE8),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4A635D),
    secondaryContainer = Color(0xFFDCE8E4),
    onSecondaryContainer = Color(0xFF06201B),
    background = Color(0xFFF4F6F5),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF4F6F5),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFE3E9E6),
    onSurfaceVariant = Color(0xFF59625F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFECF0EE),
    surfaceContainerHighest = Color(0xFFE3E9E6),
    outline = Color(0xFFC3CBC8),
    outlineVariant = Color(0xFFE0E6E3),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FD6C4),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005045),
    onPrimaryContainer = Color(0xFFD3EFE8),
    secondaryContainer = Color(0xFF334B45),
    onSecondaryContainer = Color(0xFFDCE8E4),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFDDE4E1),
    surface = Color(0xFF0F1513),
    onSurface = Color(0xFFDDE4E1),
    surfaceVariant = Color(0xFF26302D),
    onSurfaceVariant = Color(0xFFA9B3AF),
    surfaceContainerLowest = Color(0xFF0A0F0E),
    surfaceContainerLow = Color(0xFF171E1C),
    surfaceContainer = Color(0xFF1A2220),
    surfaceContainerHigh = Color(0xFF222B28),
    surfaceContainerHighest = Color(0xFF2B3532),
    outline = Color(0xFF3F4946),
    outlineVariant = Color(0xFF2B3532),
)

object StatusColors {
    val pending = Color(0xFFB26A00)
    val printed = Color(0xFF1B7F3B)
    val failed = Color(0xFFC62828)
}

/** Une teinte par catégorie : repérage instantané sur la grille. */
@Composable
@ReadOnlyComposable
fun Category.tint(): Color = when (this) {
    Category.HOT -> Color(0xFFB5652B)
    Category.COLD -> Color(0xFF2F7FB8)
    Category.BAKERY -> Color(0xFFC0901C)
    Category.SNACK -> Color(0xFF4F8A3A)
}

private val CaisseTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    )
}

@Composable
fun CaisseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = CaisseTypography,
        content = content,
    )
}
