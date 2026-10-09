package ca.carpschool.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF163A36)
val Paper = Color(0xFFF5F7F6)
val Amber = Color(0xFFB9E4D0)
val Moss = Color(0xFF25765D)
val Brick = Color(0xFFBA4238)

private val Light = lightColorScheme(
    primary = Ink, onPrimary = Paper, primaryContainer = Color(0xFFDCECE6), onPrimaryContainer = Ink,
    secondary = Amber, onSecondary = Ink, secondaryContainer = Color(0xFFDCEFE5), onSecondaryContainer = Color(0xFF183D2C),
    tertiary = Moss, onTertiary = Color.White, tertiaryContainer = Color(0xFFCDEBD9), onTertiaryContainer = Color(0xFF0B3321),
    error = Brick, onError = Color.White, errorContainer = Color(0xFFFADBD3), onErrorContainer = Color(0xFF4A1205),
    background = Paper, onBackground = Ink, surface = Paper, onSurface = Ink,
    surfaceVariant = Color(0xFFE8EEEA), onSurfaceVariant = Color(0xFF536660),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFFFFF), surfaceContainer = Color(0xFFEDF2EF),
    surfaceContainerHigh = Color(0xFFE7EEE9), surfaceContainerHighest = Color(0xFFDFE8E2),
    outline = Color(0xFF778A81), outlineVariant = Color(0xFFD9E3DD),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFA6D8C5), onPrimary = Color(0xFF163A36), primaryContainer = Color(0xFF274C42), onPrimaryContainer = Color(0xFFDCECE6),
    secondary = Amber, onSecondary = Ink, secondaryContainer = Color(0xFF354F41), onSecondaryContainer = Color(0xFFDCEFE5),
    tertiary = Color(0xFF8FD3AE), onTertiary = Color(0xFF0B3321), tertiaryContainer = Color(0xFF1F5A3E), onTertiaryContainer = Color(0xFFCDEBD9),
    error = Color(0xFFFFB4A3), onError = Color(0xFF5F1607), errorContainer = Color(0xFF7F2A17), onErrorContainer = Color(0xFFFADBD3),
    background = Color(0xFF101C18), onBackground = Color(0xFFE3EDE7), surface = Color(0xFF101C18), onSurface = Color(0xFFE3EDE7),
    surfaceVariant = Color(0xFF2B3D34), onSurfaceVariant = Color(0xFFB8CBC0),
    surfaceContainerLowest = Color(0xFF0B1511), surfaceContainerLow = Color(0xFF15241D), surfaceContainer = Color(0xFF1A2A22),
    surfaceContainerHigh = Color(0xFF22332A), surfaceContainerHighest = Color(0xFF2A3D32),
    outline = Color(0xFF839B8D), outlineVariant = Color(0xFF374C40),
)

val Mono = FontFamily.Monospace
private val base = Typography()
private val Type = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
val Overline = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold)

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun CarpTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Type, shapes = Shapes, content = content)
}
