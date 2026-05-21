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

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightBackground,
    primaryContainer = NightPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = NightPrimary,
    
    secondary = NightSecondary,
    onSecondary = NightBackground,
    secondaryContainer = NightSecondary.copy(alpha = 0.15f),
    onSecondaryContainer = NightSecondary,
    
    tertiary = NightSuccess,
    onTertiary = NightBackground,
    
    error = NightError,
    onError = NightBackground,
    
    background = NightBackground,
    onBackground = OffWhite,
    surface = NightSurface,
    onSurface = OffWhite,
    surfaceVariant = NightSurface.copy(alpha = 0.7f),
    onSurfaceVariant = LightGray.copy(alpha = 0.6f),
    outline = GlassBorder
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
        content = content
    )
}
