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
    primary = SoftBlue,
    onPrimary = DeepNavy,
    primaryContainer = DeepNavy,
    onPrimaryContainer = SoftBlue,
    
    secondary = GoldAccent,
    onSecondary = RichBlue,
    
    tertiary = GrowthGreen,
    onTertiary = RichBlue,
    
    background = RichBlue,
    onBackground = OffWhite,
    surface = DeepNavy,
    onSurface = OffWhite,
    surfaceVariant = MutedNavy,
    onSurfaceVariant = LightGray
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
