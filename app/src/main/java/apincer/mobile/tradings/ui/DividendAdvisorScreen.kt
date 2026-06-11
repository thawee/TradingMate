package apincer.mobile.tradings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.domain.IndicatorSignal
import java.util.Locale

data class SellAlertData(
    val stock: StockWatchlistInfo,
    val reason: String
)

enum class AdvisorFilter(val label: String) {
    SWING("Swing Trades"),
    GAP("Earnings Gap"),
    DIVIDEND("Dividend")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendAdvisorScreen(viewModel: StockViewModel) {
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val portfolioItems = watchlist.filter { it.portfolio.quantity > 0 }

    // Logic helpers to match thai-set-cli definitions
    val isQual = { it: StockWatchlistInfo -> (it.info.roe ?: 0.0) > 15.0 }
    val isVal = { it: StockWatchlistInfo -> (it.info.pe ?: 0.0) in 0.1..15.0 && (it.info.pbv ?: 0.0) in 0.1..1.0 }
    val isDiv = { it: StockWatchlistInfo -> (it.info.dividendYield ?: 0.0) >= 5.0 }
    val isMom = { it: StockWatchlistInfo -> (it.portfolio.macdHist ?: 0.0) > 0.0 }
    val isSup = { it: StockWatchlistInfo -> 
        it.signal?.type == IndicatorSignal.BUY || 
        it.signal?.type == IndicatorSignal.POTENTIAL || 
        (it.portfolio.rsi ?: 50.0) < 35.0 
    }
    val isGapUp = { it: StockWatchlistInfo -> it.info.percentChange >= 4.0 && (isQual(it) || (it.info.netProfitMargin ?: 0.0) > 10.0) }

    val dividendPlays = watchlist.filter { isDiv(it) && isQual(it) }.sortedByDescending { it.info.dividendYield }
    val swingPlays = watchlist.filter { (isQual(it) || isVal(it)) && (isMom(it) || isSup(it)) }
    val gapPlays = watchlist.filter { isGapUp(it) }.sortedByDescending { it.info.percentChange }

    val sellAlerts = mutableListOf<SellAlertData>()
    portfolioItems.forEach { stock ->
        val yield = stock.info.dividendYield ?: 0.0
        val isDivHolding = yield > 3.0 // Treat as dividend holding if yield > 3% (might have dropped from 5%)
        
        if (isDivHolding) {
            val roe = stock.info.roe ?: 0.0
            if (roe < 15.0) {
                sellAlerts.add(SellAlertData(stock, "Fundamentals Break (ROE < 15%)"))
            } else if (yield < 5.0) {
                sellAlerts.add(SellAlertData(stock, "Yield Dropped (< 5%)"))
            }
        } else {
            val netProfit = stock.netProfitPercent
            val rsi = stock.portfolio.rsi ?: 50.0
            
            if (netProfit >= 5.0) {
                sellAlerts.add(SellAlertData(stock, "Take Profit (Gain >= 5%)"))
            } else if (netProfit <= -5.0) {
                sellAlerts.add(SellAlertData(stock, "Stop Loss (Loss <= -5%)"))
            } else if (rsi >= 70.0) {
                sellAlerts.add(SellAlertData(stock, "Overbought (RSI >= 70)"))
            } else if (stock.signal?.type == IndicatorSignal.SELL) {
                sellAlerts.add(SellAlertData(stock, stock.signal.reason))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("AI Advisor Report", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val maxRiskPerTrade by viewModel.maxRiskPerTrade.collectAsState()
            val maxOpenExposure by viewModel.maxOpenExposure.collectAsState()
            val maxPortfolioAllocation by viewModel.maxPortfolioAllocation.collectAsState()

            AiCopilotCard(
                watchlist, 
                portfolioItems, 
                isQual, 
                isVal, 
                isDiv, 
                isMom, 
                isSup, 
                isGapUp, 
                maxRiskPerTrade, 
                maxOpenExposure, 
                maxPortfolioAllocation
            )
            SectionHeader(title = "Sell Alerts (Take Profit / Stop Loss)", icon = Icons.Default.Warning)
            if (sellAlerts.isEmpty()) {
                Text("No sell alerts active.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                sellAlerts.forEach { alert ->
                    AdvisorStockCard(alert.stock, viewModel, isSellAlert = true, sellReason = alert.reason)
                }
            }

            var activeFilter by remember { mutableStateOf(AdvisorFilter.SWING) }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(AdvisorFilter.entries.toTypedArray()) { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { activeFilter = filter },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            when (activeFilter) {
                AdvisorFilter.SWING -> {
                    if (swingPlays.isEmpty()) {
                        Text("No setups found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        swingPlays.forEach { stock ->
                            AdvisorStockCard(stock, viewModel)
                        }
                    }
                }
                AdvisorFilter.GAP -> {
                    if (gapPlays.isEmpty()) {
                        Text("No gap ups found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        gapPlays.forEach { stock ->
                            AdvisorStockCard(stock, viewModel)
                        }
                    }
                }
                AdvisorFilter.DIVIDEND -> {
                    if (dividendPlays.isEmpty()) {
                        Text("No candidates found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        dividendPlays.forEach { stock ->
                            AdvisorStockCard(stock, viewModel)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun AdvisorStockCard(
    stock: StockWatchlistInfo, 
    viewModel: StockViewModel, 
    modifier: Modifier = Modifier.fillMaxWidth(),
    isSellAlert: Boolean = false, 
    sellReason: String? = null
) {
    GlassCard(
        modifier = modifier.padding(vertical = 4.dp),
        onClick = { viewModel.fetchStockData(stock.info.symbol) },
        containerColor = if (isSellAlert) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha=0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stock.info.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    if (stock.info.isFundamentalGood) {
                        Spacer(Modifier.width(4.dp))
                        Text("⭐", fontSize = 10.sp)
                    }
                }
                if (isSellAlert) {
                    Text(sellReason ?: stock.signal?.reason ?: "Take Profit / Stop Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                } else {
                    Text(stock.info.sector ?: "Unknown", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                
                val tags = mutableListOf<String>()
                val isQual = (stock.info.roe ?: 0.0) > 15.0
                if (isQual) tags.add("QUAL")
                if ((stock.info.pe ?: 0.0) in 0.1..15.0 && (stock.info.pbv ?: 0.0) in 0.1..1.0) tags.add("VAL")
                if ((stock.info.dividendYield ?: 0.0) >= 5.0) tags.add("DIV")
                if ((stock.portfolio.macdHist ?: 0.0) > 0.0) tags.add("MOM")
                if (stock.signal?.type == IndicatorSignal.BUY || stock.signal?.type == IndicatorSignal.POTENTIAL || (stock.portfolio.rsi ?: 50.0) < 35.0) tags.add("SUP")
                if (stock.info.percentChange >= 4.0 && (isQual || (stock.info.netProfitMargin ?: 0.0) > 10.0)) tags.add("GAP")
                if ((stock.portfolio.rsi ?: 50.0) < 30.0) tags.add("OS")

                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        tags.forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format(Locale.ENGLISH, "%.2f", stock.info.lastPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isSellAlert) {
                     val net = stock.netProfitPercent
                     Text("P/L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                     Text("${if (net > 0) "+" else ""}${String.format(Locale.ENGLISH, "%.2f", net)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                } else {
                     val yield = stock.info.dividendYield ?: 0.0
                     if (yield > 0) {
                         Text("Yield", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         Text("${String.format(Locale.ENGLISH, "%.2f", yield)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                     } else {
                         Text("P/E", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         Text(stock.info.pe?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "-", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                     }
                }
            }
        }
    }
}

@Composable
fun AiCopilotCard(
    watchlist: List<StockWatchlistInfo>,
    portfolioItems: List<StockWatchlistInfo>,
    isQual: (StockWatchlistInfo) -> Boolean,
    isVal: (StockWatchlistInfo) -> Boolean,
    isDiv: (StockWatchlistInfo) -> Boolean,
    isMom: (StockWatchlistInfo) -> Boolean,
    isSup: (StockWatchlistInfo) -> Boolean,
    isGapUp: (StockWatchlistInfo) -> Boolean,
    maxRiskPerTrade: Double,
    maxOpenExposure: Double,
    maxPortfolioAllocation: Double
) {
    @Suppress("DEPRECATION")
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(
                title = "AI Master Prompts", 
                icon = Icons.Default.AutoAwesome,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Swing Trade Button
            Button(
                onClick = {
                    val swingPlays = watchlist.filter { 
                        (isQual(it) || isVal(it)) && (isMom(it) || isSup(it)) 
                    }
                    val gapUpPlays = watchlist.filter { isGapUp(it) }
                    
                    val prompt = """
                        CONSTRAINTS (Swing & Breakout):
                        - Price MUST be above the 50-day SMA.
                        - Risk/Reward ratio MUST be >= 2.0.
                        - Strict Stop Loss required below immediate support or gap.
                        - RISK: Max Risk Per Trade = $maxRiskPerTrade% of account equity. Max $maxOpenExposure% total open risk.
                        
                        I am loading the 'Swing/Breakout Playbook' and 'Earnings Gap Playbook' for the following candidates:
                        
                        Swing Candidates:
                        ${swingPlays.joinToString(", ") { "${it.info.symbol} (Price: ${it.info.lastPrice}, P/E: ${it.info.pe?.let { pe -> String.format(Locale.ENGLISH, "%.2f", pe) } ?: "N/A"})" }}
                        
                        Gap Up Candidates (Earnings Playbook):
                        ${gapUpPlays.joinToString(", ") { it.info.symbol }}

                        DELEGATED TASKS:
                        1. [market-researcher]: Search the web for upcoming earnings, news catalysts, and market structure for these tickers.
                        2. [risk-manager]: Select the #1 Top Pick setup. Confirm its price is above the 50-day SMA. Define the exact Buy Zone and strict Stop Loss.
                        
                        Output the final decision cleanly based on these subagent roles.
                    """.trimIndent()
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(prompt))
                    android.widget.Toast.makeText(context, "Swing Prompt copied! Paste into Gemini.", android.widget.Toast.LENGTH_LONG).show()
                    uriHandler.openUri("https://gemini.google.com/app")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate Swing Trade Prompt")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dividend Button
            Button(
                onClick = {
                    val dividendPlays = watchlist.filter { isDiv(it) && isQual(it) }
                    val dividendCandidates = dividendPlays
                        .sortedByDescending { it.info.dividendYield }
                        .joinToString(", ") { "${it.info.symbol} (Yield: ${it.info.dividendYield?.let { y -> String.format(Locale.ENGLISH, "%.2f", y) } ?: "N/A"}%)" }

                    val prompt = """
                        CONSTRAINTS (Dividend Accumulation):
                        - Starting Dividend Yield MUST be >= 5%.
                        - Company fundamentals must be strong (avoid yield traps).
                        - Hard rule: Never average down on a breaking technical trend.
                        - RISK: Max $maxPortfolioAllocation% total portfolio allocation per asset.

                        I am loading the 'Dividend Accumulation Playbook'.
                        Candidates Yielding > 5%: $dividendCandidates
                        
                        DELEGATED TASKS:
                        1. [market-researcher]: Search the web to verify their dividend sustainability and ensure fundamentals are intact.
                        2. [risk-manager]: Recommend the single best addition. Calculate my 'Max Buy Price' to guarantee a >5% yield and ensure it fits my overall risk exposure.
                        
                        Output the final decision cleanly based on these subagent roles.
                    """.trimIndent()
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(prompt))
                    android.widget.Toast.makeText(context, "Dividend Prompt copied! Paste into Gemini.", android.widget.Toast.LENGTH_LONG).show()
                    uriHandler.openUri("https://gemini.google.com/app")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate Dividend Prompt")
            }
        }
    }
}
