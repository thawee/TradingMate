package apincer.mobile.tradings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Assignment
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

enum class PlaybookMode(val label: String) {
    SWING("Swing Playbook"),
    DIVIDEND("Dividend Playbook")
}

enum class AdvisorFilter(val label: String) {
    SWING("Swing Trades"),
    GAP("Earnings Gap"),
    DIVIDEND("Dividend")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendAdvisorScreen(
    viewModel: StockViewModel,
    onNavigateToAcademy: () -> Unit
) {
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

    val dividendPlays = watchlist.filter { isDiv(it) && isQual(it) }
        .sortedWith(
            compareBy<StockWatchlistInfo> {
                when (it.signal?.type) {
                    IndicatorSignal.BUY -> 0
                    IndicatorSignal.POTENTIAL -> 1
                    IndicatorSignal.NEUTRAL -> 2
                    else -> 3
                }
            }.thenByDescending {
                it.info.dividendYield ?: 0.0
            }
        )
    val swingPlays = watchlist.filter { (isQual(it) || isVal(it)) && (isMom(it) || isSup(it)) }
        .sortedWith(
            compareBy<StockWatchlistInfo> {
                when (it.signal?.type) {
                    IndicatorSignal.BUY -> 0
                    IndicatorSignal.POTENTIAL -> 1
                    else -> 2
                }
            }.thenBy {
                it.portfolio.rsi ?: 100.0
            }.thenByDescending {
                isQual(it)
            }
        )
    val gapPlays = watchlist.filter { isGapUp(it) }.sortedByDescending { it.info.percentChange }
    val combinedSwingPlays = (swingPlays + gapPlays).distinctBy { it.info.symbol }
        .sortedWith(
            compareBy<StockWatchlistInfo> {
                when (it.signal?.type) {
                    IndicatorSignal.BUY -> 0
                    IndicatorSignal.POTENTIAL -> 1
                    else -> 2
                }
            }.thenByDescending {
                it.info.percentChange
            }.thenBy {
                it.portfolio.rsi ?: 100.0
            }
        )

    val swingSellAlerts = mutableListOf<SellAlertData>()
    val dividendSellAlerts = mutableListOf<SellAlertData>()

    portfolioItems.forEach { stock ->
        val tradePurpose = stock.portfolio.tradePurpose
        
        var applySwingLogic = true
        
        // Dividend checks
        if (tradePurpose == "DIVIDEND") {
            val yield = stock.info.dividendYield ?: 0.0
            val roe = stock.info.roe ?: 0.0
            
            if (roe < 15.0) {
                dividendSellAlerts.add(SellAlertData(stock, "Fundamentals Break (ROE < 15%)"))
            } 
            
            if (yield >= 3.0) {
                applySwingLogic = false
            } else {
                swingSellAlerts.add(SellAlertData(stock, "Yield Dropped (< 3%) (Transition to Swing)"))
                applySwingLogic = true
            }
        }
        
        // Swing checks
        if (applySwingLogic) {
            val netProfit = stock.netProfitPercent
            val rsi = stock.portfolio.rsi ?: 50.0
            
            val targetAlerts = swingSellAlerts
            
            if (netProfit >= 10.0) {
                targetAlerts.add(SellAlertData(stock, "Take Profit (Gain >= 10%)"))
            } else if (netProfit <= -5.0) {
                targetAlerts.add(SellAlertData(stock, "Stop Loss (Loss <= -5%)"))
            } else if (rsi >= 65.0) { // Using 65.0 to match TechnicalAnalysis OVERBOUGHT
                targetAlerts.add(SellAlertData(stock, "Overbought (RSI >= 65)"))
            } else if (stock.signal?.type == IndicatorSignal.SELL) {
                targetAlerts.add(SellAlertData(stock, stock.signal.reason))
            }
        }
    }

    var swingDailyDone by rememberSaveable { mutableStateOf(false) }
    var swingWeeklyDone by rememberSaveable { mutableStateOf(false) }
    var swingAiDone by rememberSaveable { mutableStateOf(false) }
    
    var divDailyDone by rememberSaveable { mutableStateOf(false) }
    var divWeeklyDone by rememberSaveable { mutableStateOf(false) }
    var divMonthlyDone by rememberSaveable { mutableStateOf(false) }
    var divAiDone by rememberSaveable { mutableStateOf(false) }

    var playbookMode by remember { mutableStateOf(PlaybookMode.SWING) }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Smart Advisor Report", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
            actions = {
                IconButton(onClick = onNavigateToAcademy) {
                    Icon(Icons.Default.School, contentDescription = "Academy")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        // Custom segmented glass control selector
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
            shape = CircleShape,
            border = BorderStroke(
                width = 0.5.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlaybookMode.entries.forEach { mode ->
                    val isSelected = playbookMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        onClick = { playbookMode = mode },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
                        shape = CircleShape,
                        border = if (isSelected) BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (mode == PlaybookMode.SWING) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

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
                playbookMode = playbookMode,
                watchlist = watchlist,
                portfolioItems = portfolioItems,
                isQual = isQual,
                isVal = isVal,
                isDiv = isDiv,
                isMom = isMom,
                isSup = isSup,
                isGapUp = isGapUp,
                maxRiskPerTrade = maxRiskPerTrade,
                maxOpenExposure = maxOpenExposure,
                maxPortfolioAllocation = maxPortfolioAllocation
            )

            RoutineChecklistCard(
                playbookMode = playbookMode,
                swingDaily = swingDailyDone,
                onSwingDailyChange = { swingDailyDone = it },
                swingWeekly = swingWeeklyDone,
                onSwingWeeklyChange = { swingWeeklyDone = it },
                swingAi = swingAiDone,
                onSwingAiChange = { swingAiDone = it },
                divDaily = divDailyDone,
                onDivDailyChange = { divDailyDone = it },
                divWeekly = divWeeklyDone,
                onDivWeeklyChange = { divWeeklyDone = it },
                divMonthly = divMonthlyDone,
                onDivMonthlyChange = { divMonthlyDone = it },
                divAi = divAiDone,
                onDivAiChange = { divAiDone = it }
            )

            SectionHeader(title = "Sell Alerts", icon = Icons.Default.Warning)
            val activeAlerts = if (playbookMode == PlaybookMode.SWING) swingSellAlerts else dividendSellAlerts
            if (activeAlerts.isEmpty()) {
                Text(
                    text = if (playbookMode == PlaybookMode.SWING) "No active swing exit alerts." else "No fundamental quality alerts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                activeAlerts.forEach { alert ->
                    AdvisorStockCard(alert.stock, viewModel, isSellAlert = true, sellReason = alert.reason)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (playbookMode == PlaybookMode.SWING) {
                SectionHeader(title = "Swing & Gap Candidates", icon = Icons.AutoMirrored.Filled.TrendingUp)
                if (combinedSwingPlays.isEmpty()) {
                    Text("No swing setups or gap ups found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    combinedSwingPlays.forEach { stock ->
                        AdvisorStockCard(stock, viewModel)
                    }
                }
            } else {
                SectionHeader(title = "High-Yield Dividend Stars", icon = Icons.Default.Savings)
                if (dividendPlays.isEmpty()) {
                    Text("No candidates found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    dividendPlays.forEach { stock ->
                        AdvisorStockCard(stock, viewModel)
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
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.ENGLISH, "%.2f", stock.info.lastPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.height(4.dp))

                if (isSellAlert) {
                    val net = stock.netProfitPercent
                    Text(
                        text = "P/L: ${if (net > 0) "+" else ""}${String.format(Locale.ENGLISH, "%.2f", net)}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                } else {
                    val yield = stock.info.dividendYield ?: 0.0
                    if (yield > 0) {
                        Text(
                            text = "Yield: ${String.format(Locale.ENGLISH, "%.2f", yield)}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        Text(
                            text = "P/E: ${stock.info.pe?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "-"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiCopilotCard(
    playbookMode: PlaybookMode,
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

            if (playbookMode == PlaybookMode.SWING) {
                // Swing Trade Button
                Button(
                    onClick = {
                        val swingPlays = watchlist.filter { 
                            (isQual(it) || isVal(it)) && (isMom(it) || isSup(it)) 
                        }
                        val gapUpPlays = watchlist.filter { isGapUp(it) }
                        
                        val swingCandidates = if (swingPlays.isEmpty()) "None" else swingPlays.joinToString("\n") {
                            "- ${it.info.symbol}: Price=${it.info.lastPrice}, P/E=${it.info.pe?.let { pe -> String.format(Locale.ENGLISH, "%.1f", pe) } ?: "N/A"}, P/BV=${it.info.pbv?.let { pbv -> String.format(Locale.ENGLISH, "%.1f", pbv) } ?: "N/A"}, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}, MACD Hist=${it.portfolio.macdHist?.let { m -> String.format(Locale.ENGLISH, "%.2f", m) } ?: "N/A"}, Signal=${it.portfolio.signalType ?: "NEUTRAL"} (${it.portfolio.signalReason ?: "N/A"})"
                        }
                        val gapUpCandidates = if (gapUpPlays.isEmpty()) "None" else gapUpPlays.joinToString("\n") {
                            "- ${it.info.symbol}: Price=${it.info.lastPrice}, Chg=${String.format(Locale.ENGLISH, "%.1f", it.info.percentChange)}%, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, NPM=${it.info.netProfitMargin?.let { npm -> String.format(Locale.ENGLISH, "%.1f", npm) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}"
                        }
                        
                        val prompt = """
                            Act as my expert subagents to evaluate the Stock Exchange of Thailand (SET) swing trade and gap up candidates.
                            
                            PLAYBOOK RULES & CONSTRAINTS:
                            - Swing/Breakout Candidates: Price should ideally be above the 50-day SMA, showing active trend support.
                            - Earnings Gap Candidates: Look for volume surge and strong catalyst support; entry is typically near the gap-up support line or on a breakout validation (even if catch-up indicators like 50-day SMA are lagging).
                            - General Constraints: Risk/Reward ratio MUST be >= 2.0. Strict Stop Loss required.
                            - RISK: Max Risk Per Trade = $maxRiskPerTrade% of account equity. Max $maxOpenExposure% total open risk.
                            
                            I am loading the 'Swing/Breakout Playbook' and 'Earnings Gap Playbook' for the following candidates:
                            
                            Swing Candidates:
                            $swingCandidates
                            
                            Gap Up Candidates (Earnings Playbook):
                            $gapUpCandidates

                            DELEGATED TASKS:
                            1. [market-researcher]: Perform a live web search for upcoming earnings, recent news catalysts (last 7 days), and market structure/sentiment for these SET tickers.
                            2. [risk-manager]: Select and rank the Top 3 setups from either candidate list. For Swing plays, verify 50-day SMA support. For Gap Up plays, verify volume validation and entry zones (e.g. gap support or breakout levels). Define the exact Buy Zone, target profit, and strict Stop Loss for each setup.
                            
                            EXPLAIN INSTRUCTIONS:
                            - Break down the recommendations step-by-step, referencing the math/technical metrics provided.
                            - Use ELI10 style (Explain Like I'm 10) so it's super simple.
                            - Provide a real-world analogy to describe the setup of the ranked pick.
                            
                            Output the final decision cleanly based on these subagent roles. Keep it short and easy to read.
                        """.trimIndent()
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(prompt))
                        android.widget.Toast.makeText(context, "Swing Prompt copied! Paste into your AI.", android.widget.Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Swing Trade AI Prompt")
                }
            } else {
                // Dividend Button
                Button(
                    onClick = {
                        val dividendPlays = watchlist.filter { isDiv(it) && isQual(it) }
                        val dividendCandidates = if (dividendPlays.isEmpty()) "None" else dividendPlays
                            .sortedByDescending { it.info.dividendYield }
                            .joinToString("\n") {
                                "- ${it.info.symbol}: Price=${it.info.lastPrice}, Yield=${it.info.dividendYield?.let { y -> String.format(Locale.ENGLISH, "%.1f", y) } ?: "N/A"}%, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, D/E=${it.info.debtToEquity?.let { de -> String.format(Locale.ENGLISH, "%.2f", de) } ?: "N/A"}, P/E=${it.info.pe?.let { pe -> String.format(Locale.ENGLISH, "%.1f", pe) } ?: "N/A"}, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"} (Updated=${it.info.lastUpdated})"
                            }

                        val prompt = """
                            Act as my expert subagents to evaluate the Stock Exchange of Thailand (SET) dividend candidates.
                            
                            CONSTRAINTS (Dividend Accumulation):
                            - Starting Dividend Yield MUST be >= 5%.
                            - Company fundamentals must be strong (avoid yield traps).
                            - Hard rule: Never average down on a breaking technical trend.
                            - Hold and accumulate/compound indefinitely, unless fundamentals break (ROE < 15%) or yield drops below 3%.
                            - RISK: Max $maxPortfolioAllocation% total portfolio allocation per asset.

                            I am loading the 'Dividend Accumulation Playbook'.
                            Candidates Yielding > 5%:
                            $dividendCandidates
                            
                            DELEGATED TASKS:
                            1. [market-researcher]: Perform a live web search to verify the dividend sustainability (check payout ratio trend, recent earnings reports, and cash flow safety) for these SET tickers.
                            2. [risk-manager]: Recommend the Top 3 additions. Calculate the 'Max Buy Price' for each to guarantee a >=5% yield and ensure it fits my overall risk exposure.
                            
                            EXPLAIN INSTRUCTIONS:
                            - Break down the recommendations step-by-step, referencing the math/financial metrics provided.
                            - Use ELI10 style (Explain Like I'm 10) so it's super simple.
                            - Provide a real-world analogy to explain why the ranked stock is a reliable dividend payer.
                            
                            Output the final decision cleanly based on these subagent roles. Keep it short and easy to read.
                        """.trimIndent()
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(prompt))
                        android.widget.Toast.makeText(context, "Dividend Prompt copied! Paste into your AI.", android.widget.Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Dividend AI Prompt")
                }
            }
        }
    }
}

@Composable
fun RoutineChecklistCard(
    playbookMode: PlaybookMode,
    swingDaily: Boolean,
    onSwingDailyChange: (Boolean) -> Unit,
    swingWeekly: Boolean,
    onSwingWeeklyChange: (Boolean) -> Unit,
    swingAi: Boolean,
    onSwingAiChange: (Boolean) -> Unit,
    divDaily: Boolean,
    onDivDailyChange: (Boolean) -> Unit,
    divWeekly: Boolean,
    onDivWeeklyChange: (Boolean) -> Unit,
    divMonthly: Boolean,
    onDivMonthlyChange: (Boolean) -> Unit,
    divAi: Boolean,
    onDivAiChange: (Boolean) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (playbookMode == PlaybookMode.SWING) "Swing Routine Checklist" else "Dividend Routine Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Discipline First",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (playbookMode == PlaybookMode.SWING) {
                RoutineItem(
                    checked = swingDaily,
                    onCheckedChange = onSwingDailyChange,
                    title = "Daily: Check Exit Signals",
                    description = "Review 'Sell Alerts' at the top for Take Profit (+10%) or Stop Loss (-5%)."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                RoutineItem(
                    checked = swingWeekly,
                    onCheckedChange = onSwingWeeklyChange,
                    title = "Daily: Scan Candidates",
                    description = "Browse the 'Swing & Gap Candidates' list below for fresh entries."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                RoutineItem(
                    checked = swingAi,
                    onCheckedChange = onSwingAiChange,
                    title = "Daily: Run AI Copilot Prompt",
                    description = "Copy the Swing Trade AI prompt below to analyze news & catalysts."
                )
            } else {
                RoutineItem(
                    checked = divDaily,
                    onCheckedChange = onDivDailyChange,
                    title = "Weekly: Audit Core Quality",
                    description = "Check if any active holding has broken quality standards (ROE < 15%)."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                RoutineItem(
                    checked = divWeekly,
                    onCheckedChange = onDivWeeklyChange,
                    title = "Weekly: Scan Cheap Entry Prices",
                    description = "Check Dividend Stars below for low price tags (low RSI or near support) to buy cheap."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                RoutineItem(
                    checked = divMonthly,
                    onCheckedChange = onDivMonthlyChange,
                    title = "Monthly: Plan Reinvestments",
                    description = "Deploy fresh cash or payouts into 'High-Yield Dividend Stars'."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                RoutineItem(
                    checked = divAi,
                    onCheckedChange = onDivAiChange,
                    title = "Monthly: Run AI Dividend Prompt",
                    description = "Copy the Dividend AI prompt to audit payout sustainability."
                )
            }
        }
    }
}

@Composable
fun RoutineItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.offset(y = (-4).dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
