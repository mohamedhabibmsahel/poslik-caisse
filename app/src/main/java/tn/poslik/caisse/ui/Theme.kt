package tn.poslik.caisse.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF0B6E5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEDE6),
    onPrimaryContainer = Color(0xFF00201B),
    secondaryContainer = Color(0xFFE6ECEA),
    background = Color(0xFFF6F8F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF2F0),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FD6C4),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005045),
    onPrimaryContainer = Color(0xFFCDEDE6),
    background = Color(0xFF0F1513),
    surface = Color(0xFF151C1A),
    surfaceVariant = Color(0xFF1E2724),
)

object StatusColors {
    val pending = Color(0xFFB26A00)
    val printed = Color(0xFF1B7F3B)
    val failed = Color(0xFFC62828)
}

@Composable
fun CaisseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
