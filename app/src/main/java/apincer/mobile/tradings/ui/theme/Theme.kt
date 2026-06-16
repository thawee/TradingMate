package apincer.mobile.tradings.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = White,
    primaryContainer = SoftBlue,
    onPrimaryContainer = DeepNavy,
    
    secondary = GoldAccent,
    onSecondary = White,
    secondaryContainer = SoftGold.copy(alpha = 0.3f),
    onSecondaryContainer = RichBlue,
    
    tertiary = GrowthGreen,
    onTertiary = White,
    tertiaryContainer = GrowthGreen.copy(alpha = 0.1f),
    onTertiaryContainer = GrowthGreen,
    
    error = LossRed,
    onError = White,
    errorContainer = LossRed.copy(alpha = 0.1f),
    onErrorContainer = LossRed,
    
    background = OffWhite,
    onBackground = RichBlue,
    surface = White,
    onSurface = RichBlue,
    surfaceVariant = LightGray,
    onSurfaceVariant = MutedNavy,
    outline = Gray.copy(alpha = 0.5f)
)

// Premium Glassy Obsidian Dark Mode
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),          // Cyan/Neon Teal for accents (Tech, Data-driven)
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF004D40), // Deep teal container
    onPrimaryContainer = Color(0xFF84FFFF),
    
    secondary = Color(0xFFB388FF),        // Soft Neon Purple
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF311B92),
    onSecondaryContainer = Color(0xFFD1C4E9),
    
    tertiary = Color(0xFF00E676),         // Neon Green (Profit)
    onTertiary = Color(0xFF000000),
    
    error = Color(0xFFFF1744),            // Neon Red (Loss)
    onError = Color(0xFF000000),
    
    background = Color(0xFF050505),       // Pitch Black for OLED
    onBackground = Color(0xFFFBFBFB),
    surface = Color(0xFF121212),          // Slightly elevated surface
    onSurface = Color(0xFFFBFBFB),
    surfaceVariant = Color(0xFF1E1E1E),   // For cards
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF333333)           // Subtle borders
)

@Composable
fun TradingMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
