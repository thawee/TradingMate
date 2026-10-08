package apincer.mobile.tradings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R

/** One page: what passed, what failed, and how the app turns that into rules. Figures from docs/ADVISOR_EVALUATION.md. */
private val lessons = listOf(
    Triple("Core first",
        "TDEX is the core, 80% of invested value by default, bought by monthly DCA. Everything else is satellite: at most 15% in one stock and 30% in one sector.",
        Icons.Default.AccountBalance),
    Triple("The one rule that passed",
        "The top 10 SET50 stocks by dividend yield, reviewed monthly and sold when they leave the list: +7.80% a year against TDEX +2.52% over 2015-2025, ahead in both halves. It has no trailing, quality or price exits. A high trailing yield can still be cut.",
        Icons.Default.Savings),
    Triple("What did not beat TDEX",
        "Technical buy signals (RSI, MACD, Bollinger, breakouts), buying after a 5% weekly fall, an SMA 200 trend filter, holding the yield list longer, skipping names down 20%, and market-regime cash rules. Regime rules cut drawdown but also cut return.",
        Icons.Default.Block),
    Triple("Swing trades are your plan",
        "The stop and target on a Swing buy are yours, saved as alerts. Size each trade so a stop loses about 1% of the account, rounded down to 100-share lots, and skip trades under 2:1 net reward to risk.",
        Icons.AutoMirrored.Filled.TrendingUp),
    Triple("Indicators, plainly",
        "RSI: 14-day gains against losses, 0 to 100. MACD: the gap between 12- and 26-day averages and its 9-day signal. SMA 50/200: average closes. Bollinger: 20-day average plus or minus 2 standard deviations. ATR: the average daily range, used for stop distance. They describe price; none was a tested edge here.",
        Icons.Default.QueryStats),
    Triple("Keep score",
        "Record the reason for each exit. History compares the satellite with the same money in TDEX and shows which exit reason lost most each quarter.",
        Icons.Default.History)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingEducationScreen(onBack: () -> Unit) {
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.title_academy), fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "What the evidence says",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                lessons.forEach { (title, content, icon) ->
                    EducationGlassCard(title, content, MaterialTheme.colorScheme.primary, icon)
                }
                Text(
                    text = "Backtests use today's SET50 members, so results are biased upward. Details: docs/ADVISOR_EVALUATION.md.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun EducationGlassCard(title: String, content: String, color: Color, icon: ImageVector) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 17.sp, 
                    color = color,
                    letterSpacing = (-0.5).sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content, 
                fontSize = 14.sp, 
                lineHeight = 22.sp, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
