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

// Midnight Blue Dark Mode - Easy on eyes at night
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7DD3FC),          // Soft Sky Blue (calmer than neon cyan)
    onPrimary = Color(0xFF001F33),
    primaryContainer = Color(0xFF003D5C), // Deep blue container
    onPrimaryContainer = Color(0xFFBAE6FD),
    
    secondary = Color(0xFFC4B5FD),        // Soft Lavender
    onSecondary = Color(0xFF1E1033),
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFDDD6FE),
    
    tertiary = Color(0xFF6EE7B7),         // Soft Mint (Profit)
    onTertiary = Color(0xFF003320),
    
    error = Color(0xFFFCA5A5),            // Soft Coral (Loss)
    onError = Color(0xFF3B0000),
    
    background = Color(0xFF0F172A),       // Midnight Blue (softer than pitch black)
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),          // Slate surface
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),   // Lighter slate for cards
    onSurfaceVariant = Color(0xFFCBD5E1), // High contrast muted text
    outline = Color(0xFF475569)           // Visible borders
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
