@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.malekroki.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malekroki.app.domain.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDE8DF),
    onPrimaryContainer = Color(0xFF0B2A23),
    secondary = Color(0xFF56645E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE6E1),
    onSecondaryContainer = Color(0xFF18211D),
    tertiary = Color(0xFF8A5A12),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF2C1800),
    background = Color(0xFFFAF8F4),
    onBackground = Color(0xFF1B1C1A),
    surface = Color(0xFFFAF8F4),
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFE2E3DD),
    onSurfaceVariant = Color(0xFF474B48),
    outline = Color(0xFF777B78),
    outlineVariant = Color(0xFFC8CBC6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F2EE),
    surfaceContainer = Color(0xFFEFEDE8),
    surfaceContainerHigh = Color(0xFFE9E7E2),
    surfaceContainerHighest = Color(0xFFE3E1DC),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF96D3C3),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF145145),
    onPrimaryContainer = Color(0xFFB6EFDF),
    secondary = Color(0xFFBFCAC4),
    onSecondary = Color(0xFF29332F),
    secondaryContainer = Color(0xFF3F4A45),
    onSecondaryContainer = Color(0xFFDBE6E0),
    tertiary = Color(0xFFF2BD6E),
    onTertiary = Color(0xFF482A00),
    tertiaryContainer = Color(0xFF6A4300),
    onTertiaryContainer = Color(0xFFFFDDB0),
    background = Color(0xFF121413),
    onBackground = Color(0xFFE3E3DF),
    surface = Color(0xFF121413),
    onSurface = Color(0xFFE3E3DF),
    surfaceVariant = Color(0xFF424844),
    onSurfaceVariant = Color(0xFFC1C8C3),
    outline = Color(0xFF8B928D),
    outlineVariant = Color(0xFF414844),
    surfaceContainerLowest = Color(0xFF0D0F0E),
    surfaceContainerLow = Color(0xFF1A1C1B),
    surfaceContainer = Color(0xFF1E201F),
    surfaceContainerHigh = Color(0xFF282B29),
    surfaceContainerHighest = Color(0xFF333634),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/** Day-state colours for the calendar. Missed days are quiet grey, never alarming red. */
@Immutable
data class StatusColors(
    val completed: Color,
    val onCompleted: Color,
    val protectedDay: Color,
    val onProtectedDay: Color,
    val missed: Color,
    val streak: Color,
)

private val LightStatus = StatusColors(
    completed = Color(0xFF2E6B5E),
    onCompleted = Color(0xFFFFFFFF),
    protectedDay = Color(0xFFDCE1FF),
    onProtectedDay = Color(0xFF2B3A78),
    missed = Color(0xFFE3E1DC),
    streak = Color(0xFFC77A12),
)

private val DarkStatus = StatusColors(
    completed = Color(0xFF7FC2B1),
    onCompleted = Color(0xFF00382E),
    protectedDay = Color(0xFF34417A),
    onProtectedDay = Color(0xFFDCE1FF),
    missed = Color(0xFF2C2F2D),
    streak = Color(0xFFF2BD6E),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun MaleKrokiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    val status = if (dark) DarkStatus else LightStatus
    val statusWithDynamic = if (dynamicColor) status.copy(completed = colors.primary, onCompleted = colors.onPrimary) else status

    CompositionLocalProvider(LocalStatusColors provides statusWithDynamic) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
