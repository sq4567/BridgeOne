package com.bridgeone.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// BridgeOne 다크 테마 색상 스킴
private val BridgeOneDarkColorScheme = darkColorScheme(
    primary = StateInfo,
    onPrimary = TextPrimary,
    primaryContainer = StateInfo,
    onPrimaryContainer = TextPrimary,
    
    secondary = StateSuccess,
    onSecondary = TextPrimary,
    secondaryContainer = StateSuccess,
    onSecondaryContainer = TextPrimary,
    
    tertiary = StateWarning,
    onTertiary = TextPrimary,
    tertiaryContainer = StateWarning,
    onTertiaryContainer = TextPrimary,
    
    error = StateError,
    onError = TextPrimary,
    errorContainer = StateError,
    onErrorContainer = TextPrimary,
    
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundPrimary,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = TextDisabled,
    
    outline = TextDisabled,
    outlineVariant = TextDisabled,
)

@Composable
fun BridgeOneTheme(
    darkTheme: Boolean = true,  // 항상 다크 테마 사용
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BridgeOneDarkColorScheme,
        typography = BridgeOneTypography,
        content = content
    )
}