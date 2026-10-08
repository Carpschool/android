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

val Ink = Color(0xFF13203B)
val Paper = Color(0xFFF5F0E6)
val Amber = Color(0xFFF2A900)
val Moss = Color(0xFF2F7A57)
val Brick = Color(0xFFC4462B)

private val Light = lightColorScheme(
    primary = Ink, onPrimary = Paper, primaryContainer = Color(0xFFDCE2F2), onPrimaryContainer = Ink,
    secondary = Amber, onSecondary = Ink, secondaryContainer = Color(0xFFFFE7A8), onSecondaryContainer = Color(0xFF3A2A00),
    tertiary = Moss, onTertiary = Color.White, tertiaryContainer = Color(0xFFCDEBD9), onTertiaryContainer = Color(0xFF0B3321),
    error = Brick, onError = Color.White, errorContainer = Color(0xFFFADBD3), onErrorContainer = Color(0xFF4A1205),
    background = Paper, onBackground = Ink, surface = Paper, onSurface = Ink,
    surfaceVariant = Color(0xFFE9E2D3), onSurfaceVariant = Color(0xFF4A5268),
    surfaceContainerLowest = Color(0xFFFFFCF6), surfaceContainerLow = Color(0xFFFBF7EE), surfaceContainer = Color(0xFFF1EBDF),
    surfaceContainerHigh = Color(0xFFEBE4D6), surfaceContainerHighest = Color(0xFFE5DDCE),
    outline = Color(0xFF8A8F9C), outlineVariant = Color(0xFFD8D1C2),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFBFCBEA), onPrimary = Color(0xFF13203B), primaryContainer = Color(0xFF2A3A5E), onPrimaryContainer = Color(0xFFDCE2F2),
    secondary = Amber, onSecondary = Ink, secondaryContainer = Color(0xFF5A4300), onSecondaryContainer = Color(0xFFFFE7A8),
    tertiary = Color(0xFF8FD3AE), onTertiary = Color(0xFF0B3321), tertiaryContainer = Color(0xFF1F5A3E), onTertiaryContainer = Color(0xFFCDEBD9),
    error = Color(0xFFFFB4A3), onError = Color(0xFF5F1607), errorContainer = Color(0xFF7F2A17), onErrorContainer = Color(0xFFFADBD3),
    background = Color(0xFF0E1526), onBackground = Color(0xFFEDE7DA), surface = Color(0xFF0E1526), onSurface = Color(0xFFEDE7DA),
    surfaceVariant = Color(0xFF2B3346), onSurfaceVariant = Color(0xFFB9BFCE),
    surfaceContainerLowest = Color(0xFF0A101E), surfaceContainerLow = Color(0xFF141C2F), surfaceContainer = Color(0xFF182136),
    surfaceContainerHigh = Color(0xFF1F2940), surfaceContainerHighest = Color(0xFF26304A),
    outline = Color(0xFF7D8598), outlineVariant = Color(0xFF353E54),
)

val Mono = FontFamily.Monospace
private val base = Typography()
private val Type = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
val Overline = TextStyle(fontFamily = Mono, fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.SemiBold)

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CarpTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Type, shapes = Shapes, content = content)
}
