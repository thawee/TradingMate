package apincer.mobile.tradings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TradingEducationScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.School, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Trading Academy",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = "Master the TradingMate workflow for disciplined profit.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, start = 44.dp)
        )

        // --- SECTION 1: THE WORKFLOW ---
        SectionHeader("The 3-Step Success Path",
            Icons.AutoMirrored.Filled.MenuBook, MaterialTheme.colorScheme.primary)

        EducationCard(
            title = "1. Discovery (Quality First)",
            content = "Don't gamble on random stocks. Use the '+' button to import 'Dividend Stars' or 'Bluechips'. Look for the 'Solid Financials' badge—these companies have high ROE (>10%) and low debt.",
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Search
        )

        Spacer(modifier = Modifier.height(12.dp))

        EducationCard(
            title = "2. Monitoring (The Traffic Lights)",
            content = "Move stocks into your Watchlist. The app monitors them 24/7:\n\n" +
                      "• AMBER (Potential): The stock is getting cheap or testing support. Start planning!\n" +
                      "• GREEN (Buy): Confirmed value + confirmed momentum. High-probability entry point.",
            color = MaterialTheme.colorScheme.secondary,
            icon = Icons.Default.Traffic
        )

        Spacer(modifier = Modifier.height(12.dp))

        EducationCard(
            title = "3. Execution (The Plan)",
            content = "When you see a Buy signal, use the 'Risk/Reward Calculator'. Set a target (+10-20%) and a stop loss (-5%). If the math doesn't show at least 2x reward for your risk, wait for a better price.",
            color = MaterialTheme.colorScheme.tertiary,
            icon = Icons.Default.AssignmentTurnedIn
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // --- NEW SECTION: RISK MANAGEMENT ---
        SectionHeader("Risk Management", Icons.Default.Shield, MaterialTheme.colorScheme.error)
        
        EducationCard(
            title = "The Art of the 'Cut Loss'",
            content = "The most important rule in trading is preserving your capital. \n\n" +
                      "• Why: Small losses are easy to recover; big losses are not. A -50% loss requires a +100% gain just to break even.\n" +
                      "• When: If a stock drops -5% from your cost, the app will trigger a SELL signal. This is your cue to exit and protect your remaining cash.",
            color = MaterialTheme.colorScheme.error,
            icon = Icons.Default.ContentCut
        )

        Spacer(modifier = Modifier.height(12.dp))

        EducationCard(
            title = "Yield on Cost (YoC)",
            content = "For dividend stocks, focus on your YoC. If you buy a stock at ฿10 with a ฿1 dividend, your YoC is 10%. Even if the price drops to ฿8, your YoC stays 10%—the app helps you see this 'Real Return'.",
            color = MaterialTheme.colorScheme.tertiary,
            icon = Icons.Default.Analytics
        )

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))

        // --- SECTION 2: THE SENSORS ---
        SectionHeader("Your Weather Sensors", Icons.Default.Lightbulb, MaterialTheme.colorScheme.secondary)
        Text(text = "Understanding the math behind the alerts.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 44.dp, bottom = 16.dp))

        EducationCard(
            title = "RSI (The Speedometer)",
            content = "The Relative Strength Index (14 days). \n\n• Target 35 (Value): The stock is oversold. Prices are 'compressed' and ready to bounce.\n• Target 65 (Caution): The stock is overbought. Risk of a crash is high.",
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Speed
        )

        Spacer(modifier = Modifier.height(12.dp))

        EducationCard(
            title = "MACD (The Momentum Switch)",
            content = "Think of this as the car's gears. Even if a stock is cheap (RSI 35), you wait for the MACD Histogram to turn GREEN. This confirms that the momentum is moving UP.",
            color = MaterialTheme.colorScheme.tertiary,
            icon = Icons.AutoMirrored.Filled.CompareArrows
        )

        Spacer(modifier = Modifier.height(12.dp))

        EducationCard(
            title = "SMA 50 & 200 (The Trend Guards)",
            content = "Simple Moving Averages track the 'Average' price over time.\n\n• SMA 200: The long-term 'Line in the Sand'. It defines if we are in a Bull or Bear market.\n• SMA 50: Mid-term health. If the price is above this, the immediate trend is your friend.",
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )

        Spacer(modifier = Modifier.height(12.dp))

        EducationCard(
            title = "Bollinger Bands (The Volatility Map)",
            content = "A 'Volatility Tube' that wraps around the price. \n\n• Lower Band (Value): Prices here are statistically cheap for their current movement. Good for entry.\n• Upper Band (Resistance): Prices here are stretched too far. High risk of a pullback.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Default.BlurOn
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // --- THE GOLDEN RULE ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("The Golden Rule", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    Text(
                        "SELL signals ALWAYS override BUY signals. If a stock has great momentum but hits RSI 65, the app will warn you to wait. Never buy at the peak!",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("I'm ready to trade", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
    }
}

@Composable
fun EducationCard(title: String, content: String, color: Color, icon: ImageVector) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = color)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content, 
                fontSize = 14.sp, 
                lineHeight = 22.sp, 
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
