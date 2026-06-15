package apincer.mobile.tradings.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.sharp.List
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.sharp.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.data.ChecklistEntity
import apincer.mobile.tradings.domain.IndicatorSignal
import kotlinx.coroutines.launch
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
    onNavigateToAcademy: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val portfolioItems = watchlist.filter { it.portfolio.quantity > 0 }
    val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"

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

    val checklist by viewModel.checklist.collectAsState()

    var playbookMode by remember { mutableStateOf(PlaybookMode.SWING) }

    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Section offsets for floating bar navigation
    var sellAlertsOffset by remember { mutableIntStateOf(0) }
    var candidatesOffset by remember { mutableIntStateOf(0) }
    var aiOffset by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
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

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshWatchlistInfo() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 80.dp), // Space for floating bar
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            val maxRiskPerTrade by viewModel.maxRiskPerTrade.collectAsState()
            val maxOpenExposure by viewModel.maxOpenExposure.collectAsState()
            val maxPortfolioAllocation by viewModel.maxPortfolioAllocation.collectAsState()

            val activeAlerts = if (playbookMode == PlaybookMode.SWING) swingSellAlerts else dividendSellAlerts

            // Step 1: Sell Alerts
            Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                sellAlertsOffset = coordinates.positionInWindow().y.toInt()
            }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    //SectionHeader(title = if (playbookMode == PlaybookMode.SWING) "🚨 Check for Danger" else "🛡️ Check My Shields", icon = Icons.Default.Warning)
                    SectionHeader(title = if (playbookMode == PlaybookMode.SWING) "🚨 Check for Danger" else "🛡️ Check My Shields", icon = Icons.AutoMirrored.Sharp.List)
                    StepCheckbox(
                        isDone = (playbookMode == PlaybookMode.SWING && checklist.swingDailyDone) || (playbookMode == PlaybookMode.DIVIDEND && checklist.divWeeklyDone),
                        onClick = {
                            if (playbookMode == PlaybookMode.SWING) {
                                viewModel.updateChecklistState { it.copy(swingDailyDone = !checklist.swingDailyDone) }
                            } else {
                                viewModel.updateChecklistState { it.copy(divWeeklyDone = !checklist.divWeeklyDone) }
                            }
                        }
                    )
                }
            }
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

            // Step 2: Candidates
            Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                candidatesOffset = coordinates.positionInWindow().y.toInt()
            }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (playbookMode == PlaybookMode.SWING) {
                        SectionHeader(title = "🔍 Find Candidates", icon = Icons.AutoMirrored.Filled.List)
                    } else {
                        SectionHeader(title = "💰 Find Dividend Stars", icon = Icons.AutoMirrored.Filled.List)
                    }
                    StepCheckbox(
                        isDone = (playbookMode == PlaybookMode.SWING && checklist.swingWeeklyDone) || (playbookMode == PlaybookMode.DIVIDEND && checklist.divWeeklyPricesDone),
                        onClick = {
                            if (playbookMode == PlaybookMode.SWING) {
                                viewModel.updateChecklistState { it.copy(swingWeeklyDone = !checklist.swingWeeklyDone) }
                            } else {
                                viewModel.updateChecklistState { it.copy(divWeeklyPricesDone = !checklist.divWeeklyPricesDone) }
                            }
                        }
                    )
                }
            }
            if (playbookMode == PlaybookMode.SWING) {
                if (combinedSwingPlays.isEmpty()) {
                    Text("No swing setups or gap ups found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    combinedSwingPlays.forEach { stock ->
                        AdvisorStockCard(stock, viewModel)
                    }
                }
            } else {
                if (dividendPlays.isEmpty()) {
                    Text("No candidates found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    dividendPlays.forEach { stock ->
                        AdvisorStockCard(stock, viewModel)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Step 3: AI Master Prompts
            Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                aiOffset = coordinates.positionInWindow().y.toInt()
            }) {
                AiCopilotCard(
                    playbookMode = playbookMode,
                    checklist = checklist,
                    onToggleAiDone = {
                        if (playbookMode == PlaybookMode.SWING) {
                            viewModel.updateChecklistState { it.copy(swingAiDone = !checklist.swingAiDone) }
                        } else {
                            viewModel.updateChecklistState { it.copy(divAiDone = !checklist.divAiDone) }
                        }
                    },
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
                    maxPortfolioAllocation = maxPortfolioAllocation,
                    showSnackbar = showSnackbar
                )
            }

            Spacer(Modifier.height(40.dp))
        }
        }
        } // close outer Column

        // Wizard Step Bar (navigation, direct child of Box)
        WizardStepBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            playbookMode = playbookMode,
            checklist = checklist,
            onStepClick = { step ->
                coroutineScope.launch {
                    val targetOffset = when (step) {
                        1 -> sellAlertsOffset
                        2 -> candidatesOffset
                        3 -> aiOffset
                        else -> 0
                    }
                    scrollState.animateScrollTo(targetOffset)
                }
            }
        )
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
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 12.sp,
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
    checklist: ChecklistEntity,
    onToggleAiDone: () -> Unit,
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
    maxPortfolioAllocation: Double,
    showSnackbar: (String) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader(
                    modifier = Modifier.weight(1f),
                    title = "🤖 AI Master Prompts",
                   // icon = Icons.Default.AutoAwesome,
                    icon = Icons.AutoMirrored.Filled.List,
                    color = MaterialTheme.colorScheme.tertiary
                )
                StepCheckbox(
                    isDone = (playbookMode == PlaybookMode.SWING && checklist.swingAiDone) || (playbookMode == PlaybookMode.DIVIDEND && checklist.divAiDone),
                    onClick = onToggleAiDone
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (playbookMode == PlaybookMode.SWING) {
                // Swing Trade Button
                Button(
                    onClick = {
                        val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"
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
                            
                            DATA FRESHNESS:
                            - Metrics last updated/synced on: $lastSync
                            
                            PLAYBOOK RULES & CONSTRAINTS:
                            1. Swing/Breakout Candidates:
                               - Holding Period: 2-4 weeks.
                               - Technical Alignment: Entry near key moving average support (ideally price is above the 50-day SMA) or structural breakout levels.
                            2. Earnings Gap Candidates:
                               - Holding Period: Short-term momentum (typically 1-3 weeks).
                               - Technical Alignment: Entry near the gap-up support line or on breakout validation. Prioritize volume surge and strong catalyst over lagging indicators like the 50-day SMA.
                            3. General Risk Constraints:
                               - Risk/Reward ratio MUST be >= 2.0. Strict Stop Loss required.
                               - RISK: Max Risk Per Trade = $maxRiskPerTrade% of account equity. Max $maxOpenExposure% total open risk.
                               
                            GUARDRAILS & NEGATIVE CONSTRAINTS:
                            - DO NOT recommend penny stocks (price < 1.0 THB) or highly illiquid assets.
                            - DO NOT recommend leveraged or complex structured products (e.g. DWs, TFEX warrants).
                            - DO NOT formulate response as direct financial advice; frame the analysis as educational research.
                            
                            I am loading the 'Swing/Breakout Playbook' and 'Earnings Gap Playbook' for the candidates below:
                            
                            Swing Candidates:
                            $swingCandidates
                            
                            Gap Up Candidates (Earnings Playbook):
                            $gapUpCandidates

                            DELEGATED TASKS:
                            1. [market-researcher]: Perform a live web search for upcoming earnings, news catalysts (last 7 days), and general sentiment for these tickers. Also perform a query for current SET index level, sector trends, and interest rates to establish macro context.
                            2. [risk-manager]: Select and rank the Top 3 setups from either candidate list. For Swing plays, verify 50-day SMA support. For Gap Up plays, verify volume validation and entry zones (e.g. gap support or breakout levels). Define the exact Buy Zone, target profit, and strict Stop Loss for each setup.
                            
                            EXPLAIN INSTRUCTIONS:
                            - Break down the recommendations step-by-step, referencing the math/technical metrics provided.
                            - Use ELI10 style (Explain Like I'm 10) so it's super simple.
                            - Provide a real-world analogy to describe the setup of the ranked pick.
                            
                            FORMAT REQUIREMENT:
                            Output the final recommendation as a clean Markdown report with the following structure:
                            ### Executive Summary (Swing vs Gap Candidates, Macro Environment)
                            ### Top Ranked Setups (Markdown Table: Ticker, Playbook Type, Buy Zone, Target, Stop Loss)
                            ### Subagent Analysis Details (catalysts, technical support, risk parameters)
                            ### Analogous Story (The real-world analogy)
                        """.trimIndent()
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(prompt))
                        showSnackbar("Swing Prompt copied! Paste into your AI.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Swing Trade AI Prompt")
                }
            } else {
                Button(
                    onClick = {
                        val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"
                        val dividendPlays = watchlist.filter { isDiv(it) && isQual(it) }
                        val dividendCandidates = if (dividendPlays.isEmpty()) "None" else dividendPlays
                            .sortedByDescending { it.info.dividendYield }
                            .joinToString("\n") {
                                "- ${it.info.symbol}: Price=${it.info.lastPrice}, Yield=${it.info.dividendYield?.let { y -> String.format(Locale.ENGLISH, "%.1f", y) } ?: "N/A"}%, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, D/E=${it.info.debtToEquity?.let { de -> String.format(Locale.ENGLISH, "%.2f", de) } ?: "N/A"}, P/E=${it.info.pe?.let { pe -> String.format(Locale.ENGLISH, "%.1f", pe) } ?: "N/A"}, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"} (Updated=${it.info.lastUpdated})"
                            }

                        val prompt = """
                            Act as my expert subagents to evaluate the Stock Exchange of Thailand (SET) dividend candidates.
                            
                            DATA FRESHNESS:
                            - Metrics last updated/synced on: $lastSync
                            
                            CONSTRAINTS & PLAYBOOK (Dividend Accumulation):
                            - Holding Period: Long-term (indefinite hold for compound growth).
                            - Yield Threshold: Starting Dividend Yield MUST be >= 5%.
                            - Hard rule: Never average down on a breaking technical trend.
                            - Hold and accumulate/compound indefinitely, unless fundamentals break (ROE < 15%) or yield drops below 3%.
                            - RISK: Max $maxPortfolioAllocation% total portfolio allocation per asset.
                            
                            GUARDRAILS & NEGATIVE CONSTRAINTS:
                            - DO NOT recommend penny stocks or highly illiquid assets.
                            - DO NOT recommend leveraged or complex structured products (e.g. DWs, TFEX warrants).
                            - DO NOT formulate response as direct financial advice; frame the analysis as educational research.

                            I am loading the 'Dividend Accumulation Playbook' for the candidates below.
                            Note: All candidates have already passed static baseline filters (ROE > 15%, D/E < 1.5, NPM > 10%).
                            
                            Candidates Yielding > 5%:
                            $dividendCandidates
                            
                            DELEGATED TASKS:
                            1. [market-researcher]: Perform a live web search for forward-looking dividend safety (check cash flow trend, forward payout ratio, and upcoming earnings outlook) for these SET tickers. Also perform a query for current SET index level and general market sentiment.
                            2. [risk-manager]: Recommend the Top 3 additions. Calculate the 'Max Buy Price' for each to guarantee a >=5% yield and ensure it fits my overall risk exposure.
                            
                            EXPLAIN INSTRUCTIONS:
                            - Break down the recommendations step-by-step, referencing the math/financial metrics provided.
                            - Use ELI10 style (Explain Like I'm 10) so it's super simple.
                            - Provide a real-world analogy to explain why the ranked stock is a reliable dividend payer.
                            
                            FORMAT REQUIREMENT:
                            Output the final recommendation as a clean Markdown report with the following structure:
                            ### Executive Summary (Dividend Outlook, Macro Environment)
                            ### Top Ranked Dividend Additions (Markdown Table: Ticker, Yield, Max Buy Price, Target Allocation)
                            ### Subagent Safety & Cash Flow Analysis (Payout safety, cash flow metrics)
                            ### Analogous Story (The real-world analogy)
                        """.trimIndent()
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(prompt))
                        showSnackbar("Dividend Prompt copied! Paste into your AI.")
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
fun StepCheckbox(
    isDone: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = if (isDone)
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            1.5.dp,
            if (isDone)
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (isDone) "✓" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun NavigationStepButton(
    emoji: String,
    label: String,
    isDone: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isDone)
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            1.dp,
            if (isDone)
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDone)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (isDone) {
                Text("✅", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WizardStepBar(
    modifier: Modifier = Modifier,
    playbookMode: PlaybookMode,
    checklist: ChecklistEntity,
    onStepClick: (Int) -> Unit
) {
    val step1Done = if (playbookMode == PlaybookMode.SWING) checklist.swingDailyDone else checklist.divWeeklyDone
    val step2Done = if (playbookMode == PlaybookMode.SWING) checklist.swingWeeklyDone else checklist.divWeeklyPricesDone
    val step3Done = if (playbookMode == PlaybookMode.SWING) checklist.swingAiDone else checklist.divAiDone

    val steps = listOf(
        Triple(1, if (playbookMode == PlaybookMode.SWING) "🚨 Danger" else "🛡️ Shields", step1Done),
        Triple(2, if (playbookMode == PlaybookMode.SWING) "🔍 Find" else "💰 Stars", step2Done),
        Triple(3, "🤖 AI", step3Done)
    )

    val currentStep = steps.indexOfFirst { !it.third }.coerceAtLeast(0)
    val allDone = steps.all { it.third }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (allDone)
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        border = BorderStroke(
            1.dp,
            if (allDone)
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: current step info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (allDone) "✅ All 3 steps done! You're ready to trade." else "Step ${currentStep + 1} of 3",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (allDone)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (!allDone) {
                    Text(
                        text = steps[currentStep].second,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: next button or checkmark
            if (allDone) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            } else {
                Surface(
                    onClick = { onStepClick(currentStep + 1) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Next",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("→", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
