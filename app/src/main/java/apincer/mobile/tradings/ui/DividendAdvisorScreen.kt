package apincer.mobile.tradings.ui

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R
import apincer.mobile.tradings.data.ChecklistEntity
import java.util.Locale

data class SellAlertData(
    val stock: StockWatchlistInfo,
    val reason: String
)

enum class PlaybookMode(val label: String) {
    SWING("Swing Playbook"),
    DIVIDEND("Dividend Playbook")
}


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendAdvisorScreen(
    viewModel: StockViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToAcademy: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    val alertRoutineState by viewModel.alertRoutineState.collectAsState()
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"

    val isQual = StockDna::isQual
    val isVal = StockDna::isVal
    val isDiv = StockDna::isDiv
    val isMom = StockDna::isMom
    val isSup = StockDna::isSup
    val isGapUp = StockDna::isGapUp
    val isLiquid = StockDna::isLiquid

    val playbookMode = alertRoutineState.playbookMode
    val checklist = alertRoutineState.checklist
    val swingSellAlerts = alertRoutineState.swingSellAlerts
    val dividendSellAlerts = alertRoutineState.dividendSellAlerts
    val combinedSwingPlays = alertRoutineState.combinedSwingPlays
    val speculativePlays = alertRoutineState.speculativePlays
    val dividendPlays = alertRoutineState.dividendPlays
    val portfolioItems = alertRoutineState.portfolioItems

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isAfternoonScanAvailable by viewModel.isAfternoonScanAvailable.collectAsState()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Section offsets for floating bar navigation
    var sellAlertsOffset by remember { mutableIntStateOf(0) }
    var candidatesOffset by remember { mutableIntStateOf(0) }
    var aiOffset by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Smart Advisors", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = onNavigateToAcademy) {
                        Icon(Icons.Default.School, contentDescription = "Academy")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
            if (lastSync != "---") {
                Text(
                    text = stringResource(R.string.label_last_sync, lastSync),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

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
                            onClick = { viewModel.setPlaybookMode(mode) },
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
                    Text(
                        text = "For educational purposes only. Not financial advice. Trade at your own risk.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
            val maxRiskPerTrade by settingsViewModel.maxRiskPerTrade.collectAsState()
            val maxOpenExposure by settingsViewModel.maxOpenExposure.collectAsState()
            val maxPortfolioAllocation by settingsViewModel.maxPortfolioAllocation.collectAsState()

            val activeAlerts = alertRoutineState.activeAlerts
            val step1Done = alertRoutineState.step1Done
            
            androidx.compose.runtime.LaunchedEffect(activeAlerts.size, playbookMode) {
                if (activeAlerts.isEmpty() && !step1Done) {
                    viewModel.markAlertRoutineStepDone(1)
                }
            }

            // AI Master Prompts - Always at top for easy access
            Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                aiOffset = coordinates.positionInWindow().y.toInt() - 150
            }) {
                AiCopilotCard(
                    playbookMode = playbookMode,
                    checklist = checklist,
                    onToggleAiDone = {
                        viewModel.toggleAlertRoutineStep(3)
                    },
                    onMarkAiDone = {
                        viewModel.markAlertRoutineStepDone(3)
                    },
                    watchlist = watchlist,
                    portfolioItems = portfolioItems,
                    speculativePlays = speculativePlays,
                    isQual = isQual,
                    isVal = isVal,
                    isDiv = isDiv,
                    isMom = isMom,
                    isSup = isSup,
                    isGapUp = isGapUp,
                    isLiquid = isLiquid,
                    maxRiskPerTrade = maxRiskPerTrade,
                    maxOpenExposure = maxOpenExposure,
                    maxPortfolioAllocation = maxPortfolioAllocation,
                    showSnackbar = showSnackbar
                )
            }
            Spacer(Modifier.height(16.dp))

            // Step 1: Sell Alerts
            Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                sellAlertsOffset = coordinates.positionInWindow().y.toInt()
            }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val alertsCount = activeAlerts.size
                    SectionHeader(
                        modifier = Modifier.weight(1f),
                        title = if (playbookMode == PlaybookMode.SWING) {
                            "Check Exits 🚨"
                        } else {
                            "Check My Shields 🛡️"
                        },
                        subtitle = "$alertsCount alerts",
                        icon = Icons.Default.CurrencyExchange
                       // icon = Icons.AutoMirrored.Sharp.List
                    )
                    if (playbookMode == PlaybookMode.SWING) {
                        StepCheckbox(
                            isDone = checklist.swingDailyDone,
                            onClick = {
                                viewModel.toggleAlertRoutineStep(1)
                            }
                        )
                    }
                }
            }
            if (activeAlerts.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (playbookMode == PlaybookMode.SWING) "All Clear! No swing exits required today." else "All Clear! Portfolio fundamentals are intact.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                activeAlerts.forEach { alert ->
                    AdvisorStockCard(alert.stock, viewModel, isSellAlert = true, sellReason = alert.reason)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Step 2: Candidates
            Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                candidatesOffset = coordinates.positionInWindow().y.toInt() - 150
            }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val candidatesCount = if (playbookMode == PlaybookMode.SWING) combinedSwingPlays.size else dividendPlays.size
                    if (playbookMode == PlaybookMode.SWING) {
                        SectionHeader(
                            modifier = Modifier.weight(1f),
                            //title = "🔍 Scan Setups" + if (isAfternoonScanAvailable) " 📢" else "",
                            title = "Scan Setups" + if (isAfternoonScanAvailable) " 📢" else "",
                            subtitle = "$candidatesCount setups (Quality + Momentum)" + if (isAfternoonScanAvailable) " — Afternoon scan ready" else "",
                           // icon = Icons.AutoMirrored.Filled.List
                            icon = Icons.Default.QueryStats
                        )
                    } else {
                        SectionHeader(
                            modifier = Modifier.weight(1f),
                            title = "Find Dividend Stars 💰",
                            subtitle = "$candidatesCount stars (Yield ≥ 5% & Quality)",
                            //icon = Icons.AutoMirrored.Filled.List
                            icon = Icons.Default.QueryStats
                        )
                    }
                    if (playbookMode == PlaybookMode.SWING) {
                        StepCheckbox(
                            isDone = checklist.swingWeeklyDone,
                            onClick = {
                                viewModel.toggleAlertRoutineStep(2)
                                if (isAfternoonScanAvailable) {
                                    viewModel.clearAfternoonScanFlag()
                                }
                            }
                        )
                    }
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
                
                if (speculativePlays.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Speculative Watch (Low Quality)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("These stocks triggered technical buys but failed the strict Quality filter. Trade with caution.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    speculativePlays.forEach { stock ->
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

            Spacer(Modifier.height(40.dp))
        }
        }
        } // close outer Column

        // Wizard Step Bar or Floating Button (navigation, direct child of Box)
       /* if (playbookMode == PlaybookMode.SWING) {
            WizardStepBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                playbookMode = playbookMode,
                checklist = checklist,
                alertsCount = alertRoutineState.exitAlertsCount,
                candidatesCount = alertRoutineState.activeCandidatesCount,
                isAfternoonScanAvailable = isAfternoonScanAvailable,
                onStepClick = { step ->
                    coroutineScope.launch {
                        val targetOffset = when (step) {
                            1 -> aiOffset
                            2 -> sellAlertsOffset
                            3 -> candidatesOffset
                            else -> 0
                        }
                        scrollState.animateScrollTo(targetOffset)
                    }
                }
            )

        } */
    }
}

@RequiresApi(Build.VERSION_CODES.O)
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
                        Text("⭐", fontSize = 12.sp)
                    }
                }
                if (isSellAlert) {
                    Text(sellReason ?: stock.signal?.reason ?: "Take Profit / Stop Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                } else {
                    Text(stock.info.sector ?: "Unknown", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                
                val tags = StockDna.tags(stock)

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
    onMarkAiDone: () -> Unit,
    watchlist: List<StockWatchlistInfo>,
    portfolioItems: List<StockWatchlistInfo>,
    speculativePlays: List<StockWatchlistInfo>,
    isQual: (StockWatchlistInfo) -> Boolean,
    isVal: (StockWatchlistInfo) -> Boolean,
    isDiv: (StockWatchlistInfo) -> Boolean,
    isMom: (StockWatchlistInfo) -> Boolean,
    isSup: (StockWatchlistInfo) -> Boolean,
    isGapUp: (StockWatchlistInfo) -> Boolean,
    isLiquid: (StockWatchlistInfo) -> Boolean,
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
        val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader(
                    modifier = Modifier.weight(1f),
                   // title = "🤖 AI Master Prompts",
                    title = "AI Master Prompts",
                    //subtitle = "Synced: $lastSync",
                   // icon = Icons.Default.AutoAwesome,
                   // icon = Icons.AutoMirrored.Filled.List,
                    icon = Icons.Default.AutoAwesome,
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (playbookMode == PlaybookMode.SWING) {
                    StepCheckbox(
                        isDone = checklist.swingAiDone,
                        onClick = onToggleAiDone
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap the button below to copy a detailed prompt to your clipboard. Then paste it into your favorite AI (ChatGPT, Gemini, Claude) for deep analysis.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (playbookMode == PlaybookMode.SWING) {
                val swingPlaysFilter = watchlist.filter { 
                    it.info.lastPrice >= 1.0 && isLiquid(it) && isQual(it) && (isMom(it) || isSup(it)) 
                }
                val gapUpPlaysFilter = watchlist.filter { it.info.lastPrice >= 1.0 && isLiquid(it) && isGapUp(it) }
                val speculativePromptPlays = speculativePlays.filter { it.info.lastPrice >= 1.0 }
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Data Preview: Sending ${swingPlaysFilter.size} Swing, ${gapUpPlaysFilter.size} Gap Up, and ${speculativePromptPlays.size} Speculative plays for analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Swing Trade Button
                Button(
                    onClick = {
                        onMarkAiDone()
                        val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"
                        
                        val swingCandidates = if (swingPlaysFilter.isEmpty()) "None" else swingPlaysFilter.joinToString("\n") {
                            "- ${it.info.symbol}: Price=${it.info.lastPrice}, Vol=${it.info.volume ?: 0L}, P/E=${it.info.pe?.let { pe -> String.format(Locale.ENGLISH, "%.1f", pe) } ?: "N/A"}, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}, MACD Hist=${it.portfolio.macdHist?.let { m -> String.format(Locale.ENGLISH, "%.2f", m) } ?: "N/A"}, Signal=${it.portfolio.signalType ?: "NEUTRAL"} (${it.portfolio.signalReason ?: "N/A"})"
                        }
                        val gapUpCandidates = if (gapUpPlaysFilter.isEmpty()) "None" else gapUpPlaysFilter.joinToString("\n") {
                            "- ${it.info.symbol}: Price=${it.info.lastPrice}, Vol=${it.info.volume ?: 0L}, Chg=${String.format(Locale.ENGLISH, "%.1f", it.info.percentChange)}%, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, NPM=${it.info.netProfitMargin?.let { npm -> String.format(Locale.ENGLISH, "%.1f", npm) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}"
                        }
                        val speculativeCandidates = if (speculativePromptPlays.isEmpty()) "None" else speculativePromptPlays.joinToString("\n") {
                            "- ${it.info.symbol}: Price=${it.info.lastPrice}, Vol=${it.info.volume ?: 0L}, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}, MACD Hist=${it.portfolio.macdHist?.let { m -> String.format(Locale.ENGLISH, "%.2f", m) } ?: "N/A"}, Signal=${it.portfolio.signalType ?: "NEUTRAL"} (${it.portfolio.signalReason ?: "N/A"})"
                        }
                        
                        val prompt = """
                            Act as my expert subagents to evaluate the Stock Exchange of Thailand (SET) swing trade, gap up, and speculative candidates.
                            
                            DATA FRESHNESS:
                            - Metrics last updated/synced on: $lastSync
                            
                            PLAYBOOK RULES & CONSTRAINTS:
                            1. Swing/Breakout Candidates (VIP Quality):
                               - Holding Period: 2-4 weeks.
                               - Technical Alignment: Entry near key moving average support (ideally price is above the 50-day SMA) or structural breakout levels.
                            2. Earnings Gap Candidates:
                               - Holding Period: Short-term momentum (typically 1-3 weeks).
                               - Technical Alignment: Entry near the gap-up support line or on breakout validation. Prioritize volume surge and strong catalyst.
                            3. Speculative Watch (Low Quality):
                               - High risk trades. Fundamentals are poor, but technicals are flashing oversold or reversal. Trade only if the catalyst is extremely strong.
                            4. General Risk Constraints:
                               - Risk/Reward ratio MUST be asymmetric: Target +5% Profit, Stop Loss -3%.
                               - RISK: Max Risk Per Trade = $maxRiskPerTrade% of account equity. Max $maxOpenExposure% total open risk.
                               
                            GUARDRAILS & NEGATIVE CONSTRAINTS:
                            - DO NOT recommend penny stocks (price < 1.0 THB) or highly illiquid assets.
                            - DO NOT recommend leveraged or complex structured products (e.g. DWs, TFEX warrants).
                            - DO NOT formulate response as direct financial advice; frame the analysis as educational research.
                            
                            I am loading the candidates below:
                            
                            Swing Candidates (VIP Quality):
                            $swingCandidates
                            
                            Gap Up Candidates:
                            $gapUpCandidates
                            
                            Speculative Candidates (Poor Quality, High Risk):
                            $speculativeCandidates

                            DELEGATED TASKS:
                            1. [market-researcher]: Perform a live web search for upcoming earnings, news catalysts (last 7 days), and general sentiment for these tickers. Also perform a query for current SET index level, sector trends, and interest rates to establish macro context.
                            2. [risk-manager]: Select and rank the Top 3 setups across all lists. Prioritize VIP Swing and Gap Up plays over Speculative ones. Verify entry zones (e.g. SMA support or gap support). Define the exact Buy Zone, target profit (+5%), and strict Stop Loss (-3%) for each setup.
                            
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
                val dividendPlays = watchlist.filter { isLiquid(it) && isDiv(it) && isQual(it) }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Data Preview: Sending ${dividendPlays.size} high-yield candidates for analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onMarkAiDone()
                        val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
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
    alertsCount: Int,
    candidatesCount: Int,
    isAfternoonScanAvailable: Boolean = false,
    onStepClick: (Int) -> Unit
) {
    val step1Done = checklist.swingAiDone
    val step2Done = checklist.swingDailyDone
    val step3Done = checklist.swingWeeklyDone

    val step1Label = "🤖 Ask AI"
    val step2Name = if (playbookMode == PlaybookMode.SWING) "🚨 Exits" else "🛡️ Shields"
    val step2Label = if (alertsCount > 0) "$step2Name ($alertsCount)" else step2Name
    val step3Name = if (playbookMode == PlaybookMode.SWING) "🔍 Setups" else "💰 Stars"
    val step3Badge = if (isAfternoonScanAvailable) " 📢" else ""
    val step3Label = if (candidatesCount > 0) "$step3Name ($candidatesCount)$step3Badge" else "$step3Name$step3Badge"

    val steps = listOf(
        Triple(1, step1Label, step1Done),
        Triple(2, step2Label, step2Done),
        Triple(3, step3Label, step3Done)
    )

    val currentStep = steps.indexOfFirst { !it.third }.coerceAtLeast(0)
    val allDone = steps.all { it.third }
    val totalSteps = steps.size
    val completedCount = steps.count { it.third }
    val progressPercent = completedCount.toFloat() / totalSteps

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
                    text = if (allDone) "✅ All $totalSteps steps done! You're ready to trade." else "Step ${currentStep + 1} of $totalSteps",
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
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (allDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
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
                            text = "Next → ${steps.getOrNull(currentStep + 1)?.second?.substringAfter(" ") ?: ""}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
