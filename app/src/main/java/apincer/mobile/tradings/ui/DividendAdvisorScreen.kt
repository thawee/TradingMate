package apincer.mobile.tradings.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R
import apincer.mobile.tradings.data.ChecklistEntity
import apincer.mobile.tradings.domain.TechnicalAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class SellAlertData(
    val stock: StockWatchlistInfo,
    val reason: String
)

enum class PlaybookMode(val label: String) {
    SWING("Swing Playbook"),
    DIVIDEND("Dividend Playbook")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendAdvisorScreen(
    viewModel: StockViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToAcademy: () -> Unit,
    showSnackbar: (String) -> Unit,
    /** Opens Portfolio's Buy dialog prefilled with symbol, price and shares. */
    onRecordBuy: (String, Double, Int) -> Unit = { _, _, _ -> }
) {
    val haptic = LocalHapticFeedback.current
    val showUntestedLists by settingsViewModel.showUntestedLists.collectAsState()
    val alertRoutineState by viewModel.alertRoutineState.collectAsState()
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val cashBalance by viewModel.cashBalance.collectAsState()
    val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"

    val isQual = StockDna::isQual
    val isVal = StockDna::isVal
    val isDiv = StockDna::isDiv
    val isMom = StockDna::isMom
    val isSup = StockDna::isSup
    val isGapUp = StockDna::isGapUp
    val isLiquid = StockDna::preFilter // liquidity + 52-week-low trap gate

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
    var selectedArchetype by rememberSaveable { mutableStateOf("ALL") }

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
                onRefresh = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.refreshWatchlistInfo() 
                },
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
                    if (playbookMode == PlaybookMode.SWING) {
                        UntestedEdgeNotice(modifier = Modifier.padding(top = 4.dp))
                    }
                    Text(
                        text = "For educational purposes only. Not financial advice. Trade at your own risk.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
            val maxRiskPerTrade by settingsViewModel.maxRiskPerTrade.collectAsState()
            val minRiskRewardRatio by settingsViewModel.minRiskRewardRatio.collectAsState()
            val maxOpenExposure by settingsViewModel.maxOpenExposure.collectAsState()
            val maxPortfolioAllocation by settingsViewModel.maxPortfolioAllocation.collectAsState()
            val maxSectorAllocation by settingsViewModel.maxSectorAllocation.collectAsState()
            val atsEnabled by settingsViewModel.isAtsEnabled.collectAsState()
            val geminiApiKey by settingsViewModel.geminiApiKey.collectAsState()
            val geminiModelId by settingsViewModel.geminiModel.collectAsState()

            val activeAlerts = alertRoutineState.activeAlerts
            val step1Done = alertRoutineState.step1Done
            
            androidx.compose.runtime.LaunchedEffect(activeAlerts.size, playbookMode) {
                if (activeAlerts.isEmpty() && !step1Done) {
                    viewModel.markAlertRoutineStepDone(1)
                }
            }

            // Market Regime Banner with Cash Buffer Recommendation
            val marketRegime = alertRoutineState.marketRegime
            val isBull = marketRegime == apincer.mobile.tradings.domain.TechnicalAnalysis.MarketRegime.BULLISH
            val isBear = marketRegime == apincer.mobile.tradings.domain.TechnicalAnalysis.MarketRegime.BEARISH
            val regimeBg = when {
                isBull -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                isBear -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
            val regimeFg = when {
                isBull -> MaterialTheme.colorScheme.onTertiaryContainer
                isBear -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val sizingText = when {
                isBull -> "Normal Sizing (100%)"
                isBear -> "Defensive Sizing (50%)"
                else -> "Selective Sizing (75%)"
            }
            val cashBufferRec = when {
                isBull -> "Cash Buffer: 10–15%"
                isBear -> "Cash Buffer: 50%+"
                else -> "Cash Buffer: 25–35%"
            }

            Surface(
                color = regimeBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isBull) "📈" else if (isBear) "⚠️" else "⚖️",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "SET: ${marketRegime.label}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = regimeFg,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = sizingText,
                            style = MaterialTheme.typography.labelSmall,
                            color = regimeFg.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = regimeFg.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = cashBufferRec,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = regimeFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            if (showUntestedLists) {
                // AI Master Prompts - Always at top for easy access
                Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                    aiOffset = coordinates.positionInWindow().y.toInt() - 150
                }) {
                    AiCopilotCard(
                        playbookMode = playbookMode,
                        checklist = checklist,
                        onToggleAiDone = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleAlertRoutineStep(3)
                        },
                        onMarkAiDone = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        minRiskRewardRatio = minRiskRewardRatio,
                        maxOpenExposure = maxOpenExposure,
                        maxPortfolioAllocation = maxPortfolioAllocation,
                        maxSectorAllocation = maxSectorAllocation,
                        atsEnabled = atsEnabled,
                        onValidatedAiResult = { result, plans ->
                            viewModel.recordAiRecommendations(result, plans)
                        },
                        apiKey = geminiApiKey,
                        geminiModelId = geminiModelId,
                        cashBalance = cashBalance,
                        marketRegime = marketRegime,
                        showSnackbar = showSnackbar
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

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
                        icon = Icons.Default.DoorFront
                       // icon = Icons.AutoMirrored.Sharp.List
                    )
                    if (playbookMode == PlaybookMode.SWING) {
                        StepCheckbox(
                            isDone = checklist.swingDailyDone,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

            AdvisorFilterExplorer(
                stocks = watchlist,
                mode = playbookMode,
                bearish = marketRegime == TechnicalAnalysis.MarketRegime.BEARISH,
                onStockClick = { viewModel.fetchStockData(it) }
            )

            if (showUntestedLists) {
                val heldSymbols = portfolioItems.map { it.info.symbol.uppercase() }.toSet()
                if (playbookMode == PlaybookMode.SWING) MomentumListCard(viewModel, heldSymbols)
                else {
                    HighYieldListCard(viewModel, heldSymbols)
                    HighYieldRebalanceCard(viewModel, settingsViewModel, portfolioItems, watchlist, cashBalance, onRecordBuy)
                }
                Spacer(Modifier.height(16.dp))
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
                        SectionHeader(
                            modifier = Modifier.weight(1f),
                            title = if (playbookMode == PlaybookMode.SWING) "Scan Setups" else "Find Dividend Stars 💰",
                            subtitle = if (playbookMode == PlaybookMode.SWING) "$candidatesCount setups (Quality + Momentum)" else "$candidatesCount stars (Yield ≥ 5% & Quality)",
                            icon = Icons.Default.QueryStats
                        )
                        if (playbookMode == PlaybookMode.SWING) {
                            StepCheckbox(
                                isDone = checklist.swingWeeklyDone,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.toggleAlertRoutineStep(2)
                                    if (isAfternoonScanAvailable) {
                                        viewModel.clearAfternoonScanFlag()
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                UntestedListsHiddenCard(onShow = { settingsViewModel.updateShowUntestedLists(true) })
            }

            if (playbookMode == PlaybookMode.DIVIDEND) {
                // SET XD Dividend Calendar Timeline
                val upcomingXdList = watchlist.filter { it.info.dividendYield != null && it.info.dividendYield > 0 }
                if (upcomingXdList.isNotEmpty()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Upcoming XD Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            upcomingXdList.take(5).forEach { item ->
                                val yieldStr = "%.2f%%".format(item.info.dividendYield ?: 0.0)
                                val estDps = if (item.info.lastPrice > 0) item.info.lastPrice * ((item.info.dividendYield ?: 0.0) / 100.0) else 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.info.symbol, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        Text("Est. DPS: ฿%.2f".format(estDps), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Yield: $yieldStr",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showUntestedLists) {
                // Strategy Archetype Preset Chips
                val archetypes = if (playbookMode == PlaybookMode.SWING) {
                    listOf(
                        "ALL" to "All Setups (${combinedSwingPlays.size})",
                        "VCP" to "🚀 VCP Breakout",
                        "MOAT" to "💎 Compounder",
                        "WHALE" to "🐋 Whales Inflow",
                        "SPRING" to "⚡ Oversold Spring"
                    )
                } else {
                    listOf(
                        "ALL" to "All Stars (${dividendPlays.size})",
                        "SHIELD" to "🛡️ High Yield Shield",
                        "MOAT" to "💎 Compounder",
                        "WHALE" to "🐋 Foreign Flow"
                    )
                }

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    items(archetypes) { (key, label) ->
                        val isSelected = selectedArchetype == key
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedArchetype = key
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (playbookMode == PlaybookMode.SWING) {
                    val candidateFiltered = when (selectedArchetype) {
                        "VCP" -> combinedSwingPlays.filter { StockDna.isVcpBreakout(it) }
                        "MOAT" -> combinedSwingPlays.filter { StockDna.isCompounderAristocrat(it) }
                        "WHALE" -> combinedSwingPlays.filter { StockDna.isForeignWhale(it) }
                        "SPRING" -> combinedSwingPlays.filter { StockDna.isOversoldRebound(it) }
                        else -> combinedSwingPlays
                    }

                    if (candidateFiltered.isEmpty()) {
                        Text("No setups matching preset.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        candidateFiltered.forEach { stock ->
                            AdvisorStockCard(stock, viewModel)
                        }
                    }
                    
                    // "Speculative Watch" (setups that failed the quality filter) was removed: it pushed the weakest trades.
                } else {
                    val candidateFiltered = when (selectedArchetype) {
                        "SHIELD" -> dividendPlays.filter { StockDna.isHighYieldShield(it) }
                        "MOAT" -> dividendPlays.filter { StockDna.isCompounderAristocrat(it) }
                        "WHALE" -> dividendPlays.filter { StockDna.isForeignWhale(it) }
                        else -> dividendPlays
                    }

                    if (candidateFiltered.isEmpty()) {
                        Text("No candidates matching preset.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        candidateFiltered.forEach { stock ->
                            AdvisorStockCard(stock, viewModel)
                        }
                    }
                }

                val excluded = watchlist.filter {
                    it.portfolio.portfolio.quantity == 0 &&
                        StockDna.assessSwing(it, !marketRegime.isBullish).status != CandidateStatus.READY
                }
                if (excluded.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Watch or blocked from actionable swing plans", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    // Why the list is empty, at a glance: rules are checked in order, so each stock counts once,
                    // under the first rule it fails.
                    val byReason = excluded.groupingBy { StockDna.assessSwing(it, !marketRegime.isBullish).reasons.firstOrNull() ?: "Other" }
                        .eachCount().entries.sortedByDescending { it.value }
                    Text(
                        "${excluded.size} of ${watchlist.count { it.portfolio.portfolio.quantity == 0 }} watchlist stocks stopped at: " +
                            byReason.joinToString(" · ") { "${it.key} ${it.value}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        "In 2016-2025, the price rules alone produced a shown setup on about 8% of trading days for SET50 stocks (roughly twice a month), in bull and bear markets alike. An empty list is normal.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    excluded.take(6).forEach { stock ->
                        val assessment = StockDna.assessSwing(stock, !marketRegime.isBullish)
                        Text("${stock.info.symbol} · ${assessment.status.name.lowercase()}: ${assessment.reasons.joinToString("; ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (excluded.size > 6) Text("${excluded.size - 6} more excluded",
                        style = MaterialTheme.typography.bodySmall)
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

@Composable
fun AdvisorStockCard(
    stock: StockWatchlistInfo, 
    viewModel: StockViewModel, 
    modifier: Modifier = Modifier,
    isSellAlert: Boolean = false, 
    sellReason: String? = null
) {
    val isProfitAlert = isSellAlert && sellReason?.contains("Take Profit", ignoreCase = true) == true
    val isOverboughtAlert = isSellAlert && sellReason?.contains("Overbought", ignoreCase = true) == true
    val cardContainerColor = when {
        !isSellAlert -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        isProfitAlert -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
        isOverboughtAlert -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    }
    val alertTextColor = when {
        isProfitAlert -> MaterialTheme.colorScheme.tertiary
        isOverboughtAlert -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    var selectedLegendTag by remember { mutableStateOf<String?>(null) }

    GlassCard(
        modifier = modifier.padding(vertical = 4.dp),
        onClick = { viewModel.fetchStockData(stock.info.symbol) },
        containerColor = cardContainerColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        if (!isSellAlert && stock.signal?.type != null) {
                            Spacer(Modifier.width(6.dp))
                            val signalColor = when (stock.signal.type) {
                                apincer.mobile.tradings.domain.IndicatorSignal.BUY -> MaterialTheme.colorScheme.tertiary
                                apincer.mobile.tradings.domain.IndicatorSignal.POTENTIAL -> MaterialTheme.colorScheme.secondary
                                apincer.mobile.tradings.domain.IndicatorSignal.SELL -> MaterialTheme.colorScheme.error
                                else -> null
                            }
                            if (signalColor != null) {
                                Surface(
                                    color = signalColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stock.signal.type.badgeLabel,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = signalColor
                                    )
                                }
                            }
                        }
                    }
                    if (isSellAlert) {
                        val prefix = when {
                            isProfitAlert -> "🎯 "
                            isOverboughtAlert -> "⚡ "
                            else -> "🛑 "
                        }
                        Text(prefix + (sellReason ?: stock.signal?.reason ?: "Exit Alert"), style = MaterialTheme.typography.labelSmall, color = alertTextColor, fontWeight = FontWeight.Bold)
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
                                val (bgAlpha, borderAlpha, tagColor) = when (tag) {
                                    "A+" -> Triple(0.18f, 0.4f, Color(0xFF6EE7B7))
                                    "A" -> Triple(0.18f, 0.4f, Color(0xFF60A5FA))
                                    "VCP", "GAP" -> Triple(0.18f, 0.35f, Color(0xFFA78BFA))
                                    "MOAT", "SHIELD" -> Triple(0.18f, 0.35f, Color(0xFF34D399))
                                    "WHALE" -> Triple(0.18f, 0.35f, Color(0xFF38BDF8))
                                    "SPRING", "OS" -> Triple(0.18f, 0.35f, Color(0xFFFBBF24))
                                    else -> Triple(0.10f, 0.0f, MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    color = tagColor.copy(alpha = bgAlpha),
                                    shape = RoundedCornerShape(4.dp),
                                    border = if (borderAlpha > 0f) BorderStroke(0.5.dp, tagColor.copy(alpha = borderAlpha)) else null,
                                    onClick = { selectedLegendTag = tag }
                                ) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (borderAlpha > 0f) tagColor else MaterialTheme.colorScheme.onSecondaryContainer
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

            if (!isSellAlert && stock.info.lastPrice > 0) {
                val isSet50 = apincer.mobile.tradings.domain.MarketLists.isSet50(stock.info.symbol.uppercase())
                val stopPrice = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateSuggestedStopLossPrice(
                    lastPrice = stock.info.lastPrice,
                    atr = stock.portfolio.atr,
                    isSet50 = isSet50
                )
                val targetPrice = stock.portfolio.portfolio.targetPrice.takeIf { it > stock.info.lastPrice }
                val rr = targetPrice?.let {
                    apincer.mobile.tradings.domain.TechnicalAnalysis.calculateRiskRewardRatio(stock.info.lastPrice, it, stopPrice)
                }
                val stopPercent = ((stopPrice - stock.info.lastPrice) / stock.info.lastPrice) * 100

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Plan: Cut < ฿${String.format(Locale.ENGLISH, "%.2f", stopPrice)} (${String.format(Locale.ENGLISH, "%.1f", stopPercent)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                // Only a saved target is shown; a missing one is simply omitted (it used to clip to a bare "Target").
                                if (targetPrice != null) {
                                    Text(
                                        text = "  •  ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Saved target ฿${String.format(Locale.ENGLISH, "%.2f", targetPrice)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (rr != null) {
                                Spacer(Modifier.width(6.dp))
                                val (badgeBg, badgeFg) = when {
                                    rr >= 2.0 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f) to MaterialTheme.colorScheme.onTertiaryContainer
                                    rr >= 1.5 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f) to MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Surface(
                                    color = badgeBg,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Reward:Risk ${String.format(Locale.ENGLISH, "%.1f", rr)}:1",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = badgeFg,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        val bb = stock.portfolio.bb
                        val wLow = stock.portfolio.week52Low
                        val wHigh = stock.portfolio.week52High
                        val rangeText = when {
                            bb != null && bb.lower > 0 && bb.upper > 0 ->
                                "Est. Range: ฿${String.format(Locale.ENGLISH, "%.2f", bb.lower)} - ฿${String.format(Locale.ENGLISH, "%.2f", bb.upper)} (BB ±2σ)"
                            wLow != null && wHigh != null && wLow > 0 && wHigh > 0 ->
                                "52W Range: ฿${String.format(Locale.ENGLISH, "%.2f", wLow)} - ฿${String.format(Locale.ENGLISH, "%.2f", wHigh)}"
                            else -> null
                        }
                        if (rangeText != null) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = rangeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedLegendTag != null) {
        ArchetypeLegendDialog(
            selectedTag = selectedLegendTag,
            onDismiss = { selectedLegendTag = null }
        )
    }
}

@Composable
fun ArchetypeLegendDialog(
    selectedTag: String?,
    onDismiss: () -> Unit
) {
    if (selectedTag == null) return

    val descriptions = mapOf(
        "A+" to "Prime Quant Grade (Score 80–100): Highest-conviction confluence across quality, value, momentum, flow, and safety.",
        "A" to "Strong Quant Grade (Score 65–79): Solid confluence of fundamental quality, trend, and setup support.",
        "VCP" to "Minervini VCP: Volatility Contraction Pattern — tightening price action preceding a potential Stage-2 breakout.",
        "MOAT" to "Compounder Aristocrat: High ROE (≥12%), low debt (D/E ≤1.2), healthy margins, and durable competitive moat.",
        "SHIELD" to "High-Yield Shield: High dividend yield (≥5%) with robust safety buffer well above 52-week low.",
        "WHALE" to "Foreign Smart Money: Institutional accumulation via positive foreign NVDR net buying and relative strength.",
        "SPRING" to "Wyckoff Oversold Spring: Profitable company (ROE ≥8%) experiencing extreme oversold mean-reversion (RSI ≤35).",
        "MTF" to "Multi-Timeframe Trend: Macro trend alignment across SMA 200/50 and positive MACD histogram.",
        "QUAL" to "Quality: ROE above 10%, net margin above 10%, and debt-to-equity below 1.5 (not applied to banks, finance and insurers).",
        "VAL" to "Value Pricing: Undervalued multiples (P/E 0.1–15.0 and P/BV 0.1–1.0).",
        "DIV" to "Dividend: High current dividend yield (≥ 5.0%).",
        "MOM" to "Momentum: Meaningful MACD histogram expansion and active momentum (RSI 40–65).",
        "SUP" to "Setup Confirmation: Active BUY or POTENTIAL technical indicator signal.",
        "MOVE" to "Strong daily move: +4% daily gain on heavy volume with profitable fundamentals.",
        "FLOW" to "Foreign Flow: Confirmed positive foreign NVDR net volume.",
        "CYC" to "Cyclical Sector: Commodity/cyclical industry — requires active profit-taking discipline.",
        "RS" to "Relative Strength: outperformed TDEX over the last 63 trading days (about 3 months).",
        "OS" to "Deeply Oversold: 14-day RSI below 30. Describes the fall; it is not a buy signal."
    )

    val currentDesc = descriptions[selectedTag] ?: "Quantitative screening filter tag."

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = selectedTag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text("Archetype Tag Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = currentDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "All Archetype Tags:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                descriptions.forEach { (tag, desc) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(
                            text = "• $tag: ",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = desc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", fontWeight = FontWeight.Bold)
            }
        }
    )
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
    minRiskRewardRatio: Double,
    maxOpenExposure: Double,
    maxPortfolioAllocation: Double,
    maxSectorAllocation: Double,
    atsEnabled: Boolean,
    onValidatedAiResult: (
        apincer.mobile.tradings.domain.AiAnalysisResult,
        Map<String, apincer.mobile.tradings.domain.AiCandidatePlan>
    ) -> Unit,
    apiKey: String,
    geminiModelId: String = "gemini-3.6-flash",
    cashBalance: Double = 0.0,
    marketRegime: apincer.mobile.tradings.domain.TechnicalAnalysis.MarketRegime = apincer.mobile.tradings.domain.TechnicalAnalysis.MarketRegime.NEUTRAL,
    showSnackbar: (String) -> Unit,
    onAcceptAiPlan: (apincer.mobile.tradings.domain.AiRecommendation) -> Unit = {}
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
                    title = "Gemini AI Advisor",
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
                text = "Run analysis with Google Gemini, or optionally copy the master prompt for external AIs.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                lineHeight = 16.sp
            )

            // Fix 7: Warn when data used by AI is stale (> 12 hours old)
            val isDataStale = remember(lastSync) {
                if (lastSync == "---") true
                else runCatching {
                    val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val dt = java.time.LocalDateTime.parse(lastSync, fmt)
                    val ageMs = System.currentTimeMillis() -
                        dt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    ageMs > 12L * 60L * 60L * 1000L
                }.getOrDefault(true)
            }
            if (isDataStale) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Data may be stale (last sync: $lastSync). Pull to refresh before running AI analysis.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (playbookMode == PlaybookMode.SWING) {
                val swingPlaysFilter = watchlist.filter {
                    it.info.lastPrice >= 1.0 && StockDna.isSwingCandidate(it,
                        marketRegime == apincer.mobile.tradings.domain.TechnicalAnalysis.MarketRegime.BEARISH)
                }.sortedByDescending { it.portfolio.relativeStrength ?: -999.0 }
                val gapUpPlaysFilter = swingPlaysFilter.filter { isGapUp(it) }
                val speculativePromptPlays = emptyList<StockWatchlistInfo>()
                val totalAssets = cashBalance + portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
                // Symbol -> first budget limit that blocks even one lot, shown under the preview.
                val budgetBlocks = mutableMapOf<String, String>()
                val aiPlans = swingPlaysFilter.mapNotNull { stock ->
                    val entry = stock.info.lastPrice
                    val target = stock.portfolio.week52High?.takeIf { it > entry } ?: return@mapNotNull null
                    val sector = stock.info.sector
                    val stop = TechnicalAnalysis.calculateSuggestedStopLossPrice(
                        entry, stock.portfolio.atr,
                        apincer.mobile.tradings.domain.MarketLists.isSet50(stock.info.symbol.uppercase()), marketRegime = marketRegime)
                    // Fix 4: Use cost basis (not market value) for concentration checks — market
                    // value fluctuates and would silently allow exceeding allocation limits on
                    // positions bought at lower prices, or falsely block entries after drawdowns.
                    val existingStock = portfolioItems.filter { it.info.symbol == stock.info.symbol }
                        .sumOf { it.portfolio.cost * it.portfolio.quantity }
                    val existingSector = sector?.let { s -> portfolioItems.filter { it.info.sector == s }
                        .sumOf { it.portfolio.cost * it.portfolio.quantity } }
                    // Largest whole lot that fits every budget limit with fees, rather than a gross
                    // risk size that is dropped when fees or existing holdings push it over a limit.
                    val fit = apincer.mobile.tradings.domain.TradeRiskPolicy.largestFit(
                        entry, stop, totalAssets, cashBalance, existingStock, existingSector, atsEnabled,
                        apincer.mobile.tradings.domain.TradeRiskLimits(
                            maxRiskPerTrade, maxPortfolioAllocation, maxSectorAllocation,
                            TechnicalAnalysis.getRecommendedCashBufferPercent(marketRegime),
                            minRiskRewardRatio))
                    if (fit.quantity <= 0) {
                        budgetBlocks[stock.info.symbol.uppercase()] = fit.blockingReason ?: "No lot fits"
                        return@mapNotNull null
                    }
                    val shares = fit.quantity
                    val buyFees = TechnicalAnalysis.calculateFees(entry * shares, false, atsEnabled)
                    val netReward = (target - entry) * shares - buyFees -
                        TechnicalAnalysis.calculateFees(target * shares, true, atsEnabled)
                    val netRisk = (entry - stop) * shares + buyFees +
                        TechnicalAnalysis.calculateFees(stop * shares, true, atsEnabled)
                    if (netReward < minRiskRewardRatio * netRisk) return@mapNotNull null
                    apincer.mobile.tradings.domain.AiCandidatePlan(
                        stock.info.symbol.uppercase(), entry, stop, target,
                        entry * shares + buyFees, shares,
                        "${stock.info.lastUpdated}:${stock.portfolio.signal?.lastUpdated}:$entry:$target"
                    )
                }.associateBy { it.symbol }
                val actionableSwingFilter = swingPlaysFilter.filter {
                    aiPlans.containsKey(it.info.symbol.uppercase())
                }
                val actionableDailyMoves = gapUpPlaysFilter.filter {
                    aiPlans.containsKey(it.info.symbol.uppercase())
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Data Preview: Sending ${actionableSwingFilter.size} locally validated swing plans, including ${actionableDailyMoves.size} strong daily movers, to AI." +
                            if (budgetBlocks.isEmpty()) "" else "\n${budgetBlocks.size} blocked by budget, not even one lot fits: " +
                                budgetBlocks.entries.joinToString("; ") { "${it.key} (${it.value})" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Swing Trade Prompts
                val buildSwingPrompt = {
                    val lastSyncLocal = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"
                    val totalStockEquity = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
                    val totalPortfolioEquity = cashBalance + totalStockEquity
                    val currentMarketRegime = marketRegime
                    val spendableInfo = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateSpendableCash(
                        totalAssets = totalPortfolioEquity,
                        cashBalance = cashBalance,
                        regime = currentMarketRegime
                    )

                    val cashBalanceFormatted = String.format(Locale.ENGLISH, "%,.2f THB", cashBalance)
                    val totalAssetsFormatted = String.format(Locale.ENGLISH, "%,.2f THB", totalPortfolioEquity)
                    val targetReserveFormatted = String.format(Locale.ENGLISH, "%,.2f THB", spendableInfo.targetReserveBaht)
                    val spendableCashFormatted = String.format(Locale.ENGLISH, "%,.2f THB", spendableInfo.spendableCashBaht)
                    val bufferPercentStr = String.format(Locale.ENGLISH, "%.0f", spendableInfo.recommendedBufferPercent)
                    
                    val swingCandidates = if (actionableSwingFilter.isEmpty()) "None" else actionableSwingFilter.joinToString("\n") {
                        "- ${it.info.symbol}: Price=${it.info.lastPrice}, Vol=${it.info.volume ?: 0L}, P/E=${it.info.pe?.let { pe -> String.format(Locale.ENGLISH, "%.1f", pe) } ?: "N/A"}, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}, MACD Hist=${it.portfolio.macdHist?.let { m -> String.format(Locale.ENGLISH, "%.2f", m) } ?: "N/A"}, Signal=${it.portfolio.signalType ?: "NEUTRAL"} (${it.portfolio.signalReason ?: "N/A"})"
                    }
                    val gapUpCandidates = if (actionableDailyMoves.isEmpty()) "None" else actionableDailyMoves.joinToString("\n") {
                        "- ${it.info.symbol}: Price=${it.info.lastPrice}, Vol=${it.info.volume ?: 0L}, Chg=${String.format(Locale.ENGLISH, "%.1f", it.info.percentChange)}%, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, NPM=${it.info.netProfitMargin?.let { npm -> String.format(Locale.ENGLISH, "%.1f", npm) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}"
                    }
                    val speculativeCandidates = if (speculativePromptPlays.isEmpty()) "None" else speculativePromptPlays.joinToString("\n") {
                        "- ${it.info.symbol}: Price=${it.info.lastPrice}, Vol=${it.info.volume ?: 0L}, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}, MACD Hist=${it.portfolio.macdHist?.let { m -> String.format(Locale.ENGLISH, "%.2f", m) } ?: "N/A"}, Signal=${it.portfolio.signalType ?: "NEUTRAL"} (${it.portfolio.signalReason ?: "N/A"})"
                    }
                    
                    """
                        Evaluate the supplied SET swing candidates. Rank only the locally validated plans below. Return no recommendations if the list is empty. Explain only the supplied data; you have no live news or search tools.

                        LOCALLY VALIDATED PLANS (keep exact levels and allocation; the app will independently restore them):
                        ${if (aiPlans.isEmpty()) "None" else aiPlans.values.joinToString("\n") { "${it.symbol}: planId=${it.snapshotId}, entry=${it.entryPrice}, stop=${it.stopPrice}, target=${it.targetPrice}, allocation=${it.allocationBaht}, shares=${it.shares}" }}
                        
                        DATA FRESHNESS & CAPITAL:
                        - Metrics last updated/synced on: $lastSyncLocal
                        - Total Account Equity: $totalAssetsFormatted (Stock Holdings: ฿${String.format(Locale.ENGLISH, "%,.2f", totalStockEquity)} | Cash: $cashBalanceFormatted)
                        - Current SET Market Regime: ${spendableInfo.regime.label} (${spendableInfo.regime.name})
                        - Regime-Mandated Cash Buffer: $bufferPercentStr% (Target Reserve: $targetReserveFormatted)
                        - Maximum Spendable Capital: $spendableCashFormatted ${if (spendableInfo.isDeficit) "⚠️ DEFICIT: Cash balance is below required buffer! Enforce capital preservation." else ""}
                        
                        PLAYBOOK RULES & CONSTRAINTS:
                        1. Swing/Breakout Candidates (VIP Quality):
                           - Holding Period: 2-4 weeks.
                           - Technical Alignment: Entry near key moving average support (ideally price is above the 50-day SMA) or structural breakout levels.
                        2. Strong Daily Move Candidates:
                           - Holding Period: Short-term momentum (typically 1-3 weeks).
                           - Technical Alignment: Check trend and volume. Do not infer an overnight gap or earnings catalyst from the daily percentage change.
                        3. General Risk Constraints:
                           - Risk/Reward ratio MUST meet the configured minimum $minRiskRewardRatio:1 after fees. Stop loss must be placed below key technical support.
                           - RISK: Max Risk Per Trade = $maxRiskPerTrade% of account equity. Total open risk guideline $maxOpenExposure% (advisory; not enforced by the app). Max $maxPortfolioAllocation% capital allocation in any single stock (cost basis) and $maxSectorAllocation% per sector.
                           - CYCLICAL SHIELD: If recommending a cyclical/commodity stock, require volume catalyst and tighter stop loss.
                            
                        GUARDRAILS & NEGATIVE CONSTRAINTS:
                        - DO NOT recommend penny stocks (price < 1.0 THB) or highly illiquid assets.
                        - DO NOT recommend leveraged or complex structured products (e.g. DWs, TFEX warrants).
                        - DO NOT formulate response as direct financial advice; frame the analysis as educational research.
                        
                        I am loading the candidates below:
                        
                        Swing Candidates (VIP Quality):
                        $swingCandidates
                        
                        Strong Daily Move Candidates:
                        $gapUpCandidates
                        
                        Speculative Candidates (Poor Quality, High Risk):
                        $speculativeCandidates

                        DELEGATED TASKS:
                        1. [market-researcher]: Explain the supplied technical and fundamental metrics. Do not assert unprovided news, earnings catalysts or live macro data.
                        2. [regime-manager]: Evaluate market regime (Bullish, Bearish, or Choppy/Sideways based on price relative to SMA 200/50 and MACD). In a Bull market, allow higher profit targets and breakout trailing stops; in a Bear/Choppy market, enforce capital preservation, tighter stop losses, and buying strictly at major technical support.
                        3. [risk-manager]: Rank at most three locally validated plans above. Do not change their entry, target, stop or allocation.
                        IMPORTANT CASH ALLOCATION & BUFFER RULES:
                        - Spendable Capital Ceiling: $spendableCashFormatted (after strictly preserving the $bufferPercentStr% regime cash reserve of $targetReserveFormatted).
                        - If Spendable Capital is 0.00 THB or account is in a cash deficit, return no picks.
                        - Use the local allocations and board-lot quantities exactly as supplied; do not calculate replacement values.
                        - For each ranked pick, give a qualitative model assessment and brief justification. A score is not a measured win probability.

                        
                        EXPLAIN INSTRUCTIONS:
                        - Break down the recommendations step-by-step using clear, accessible logic.
                        - Provide a real-world analogy to describe the setup of the ranked pick.
                        
                        FORMAT REQUIREMENT:
                        Output the final recommendation as a clean Markdown report with the following structure:
                        ### Executive Summary (Market Regime, Macro Environment, Swing vs Gap Candidates)
                        ### Top Ranked Setups (Markdown Table: Ticker, Playbook Type, Buy Zone, Target, Stop Loss, Cash Allocation THB & Est. Shares, Model Assessment)
                        ### Model Analysis Details (regime alignment, supplied evidence and risk checks)
                        ### Analogous Story (The real-world analogy)
                    """.trimIndent()
                }

                // Primary Highlighted Action: Direct In-App Analysis with Google Gemini
                AiAnalysisButton(
                    label = "Ask! Google Gemini",
                    apiKey = apiKey,
                    geminiModelId = geminiModelId,
                    buildPrompt = buildSwingPrompt,
                    allowedPlans = aiPlans,
                    requirePlans = true,
                    onValidatedResult = { onValidatedAiResult(it, aiPlans) },
                    onDone = onMarkAiDone,
                    showSnackbar = showSnackbar
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Optional Action: Copy Prompt to Clipboard
                OutlinedButton(
                    onClick = {
                        onMarkAiDone()
                        val prompt = buildSwingPrompt()
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(prompt))
                        showSnackbar("Swing Prompt copied! Paste into external AI.")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Master Prompt")
                }
            } else {
                val dividendPlays = watchlist.filter(StockDna::isDividendCandidate)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Data Preview: Sending ${dividendPlays.size} high-yield candidates to AI.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                val buildDividendPrompt = {
                    val lastSyncLocal = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"
                    val totalStockEquity = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
                    val totalPortfolioEquity = cashBalance + totalStockEquity
                    val currentMarketRegime = marketRegime
                    val spendableInfo = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateSpendableCash(
                        totalAssets = totalPortfolioEquity,
                        cashBalance = cashBalance,
                        regime = currentMarketRegime
                    )

                    val cashBalanceFormatted = String.format(Locale.ENGLISH, "%,.2f THB", cashBalance)
                    val totalAssetsFormatted = String.format(Locale.ENGLISH, "%,.2f THB", totalPortfolioEquity)
                    val targetReserveFormatted = String.format(Locale.ENGLISH, "%,.2f THB", spendableInfo.targetReserveBaht)
                    val spendableCashFormatted = String.format(Locale.ENGLISH, "%,.2f THB", spendableInfo.spendableCashBaht)
                    val bufferPercentStr = String.format(Locale.ENGLISH, "%.0f", spendableInfo.recommendedBufferPercent)

                    val dividendCandidates = if (dividendPlays.isEmpty()) "None" else dividendPlays
                        .sortedByDescending { it.info.dividendYield }
                        .joinToString("\n") {
                            "- ${it.info.symbol}: Price=${it.info.lastPrice}, Yield=${it.info.dividendYield?.let { y -> String.format(Locale.ENGLISH, "%.1f", y) } ?: "N/A"}%, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, D/E=${it.info.debtToEquity?.let { de -> String.format(Locale.ENGLISH, "%.2f", de) } ?: "N/A"}, P/E=${it.info.pe?.let { pe -> String.format(Locale.ENGLISH, "%.1f", pe) } ?: "N/A"}, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"} (Updated=${it.info.lastUpdated})"
                        }

                    """
                        Act as my expert subagents to evaluate the Stock Exchange of Thailand (SET) dividend candidates.
                        
                        DATA FRESHNESS & CAPITAL:
                        - Metrics last updated/synced on: $lastSyncLocal
                        - Total Account Equity: $totalAssetsFormatted (Stock Holdings: ฿${String.format(Locale.ENGLISH, "%,.2f", totalStockEquity)} | Cash: $cashBalanceFormatted)
                        - Current SET Market Regime: ${spendableInfo.regime.label} (${spendableInfo.regime.name})
                        - Regime-Mandated Cash Buffer: $bufferPercentStr% (Target Reserve: $targetReserveFormatted)
                        - Maximum Spendable Capital: $spendableCashFormatted ${if (spendableInfo.isDeficit) "⚠️ DEFICIT: Cash balance is below required buffer! Enforce capital preservation." else ""}
                        
                        CONSTRAINTS & PLAYBOOK (Dividend Accumulation):
                        - Holding Period: Long-term (indefinite hold for compound growth).
                        - Yield Threshold: Starting Dividend Yield MUST be >= 5%.
                        - Hard rule: Never average down on a breaking technical trend.
                        - Hold and accumulate/compound indefinitely, unless fundamentals break (ROE < 15%) or yield drops below 3%.
                        - RISK: Max $maxPortfolioAllocation% total portfolio allocation per asset (cost basis, hard-capped).
                        - CYCLICAL DIVIDEND SHIELD: Verify dividend is backed by operational cash flow, not cyclical commodity peaks or one-off asset sales.
                        
                        GUARDRAILS & NEGATIVE CONSTRAINTS:
                        - DO NOT recommend penny stocks (price < 1.0 THB) or highly illiquid assets.
                        - DO NOT recommend leveraged or complex structured products (e.g. DWs, TFEX warrants).
                        - DO NOT formulate response as direct financial advice; frame the analysis as educational research.

                        Explain the supplied dividend candidates qualitatively. Some safety inputs may be missing; treat them as unknown. No locally validated dividend entry, stop, target, or allocation plan is supplied.
                        
                        Candidates Yielding > 5%:
                        $dividendCandidates
                        
                        DELEGATED TASKS:
                        1. Explain the supplied yield and profitability metrics, and name evidence needed to assess dividend safety.
                        2. Discuss the supplied market regime without inventing current news, payout data, or cash-flow figures.
                        3. Return no executable recommendations. Do not calculate buy prices, share counts, or allocations.

                        
                        EXPLAIN INSTRUCTIONS:
                        - Explain uncertainties in clear, accessible language.
                        
                        FORMAT REQUIREMENT:
                        Output the final recommendation as a clean Markdown report with the following structure:
                        ### Executive Summary (Dividend Outlook, Market Regime, Macro Environment)
                        ### Candidate Evidence (supplied metrics and missing safety inputs)
                        ### Dividend Risks (regime alignment and uncertainty)
                    """.trimIndent()
                }

                // Primary Highlighted Action: Direct In-App Analysis with Google Gemini
                AiAnalysisButton(
                    label = "Explain dividend candidates",
                    apiKey = apiKey,
                    geminiModelId = geminiModelId,
                    buildPrompt = buildDividendPrompt,
                    emptyMessage = "Dividend analysis is qualitative until a locally validated trade plan is available.",
                    onDone = onMarkAiDone,
                    showSnackbar = showSnackbar,
                    onAcceptAiPlan = onAcceptAiPlan
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Optional Action: Copy Prompt to Clipboard
                OutlinedButton(
                    onClick = {
                        onMarkAiDone()
                        val prompt = buildDividendPrompt()
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(prompt))
                        showSnackbar("Dividend Prompt copied! Paste into external AI.")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Master Prompt")
                }
            }
        }
    }
}

@Composable
fun AiAnalysisButton(
    label: String,
    apiKey: String,
    geminiModelId: String = "gemini-3.6-flash",
    buildPrompt: () -> String,
    emptyMessage: String = "No locally validated setup available. AI explanations are educational only.",
    allowedPlans: Map<String, apincer.mobile.tradings.domain.AiCandidatePlan> = emptyMap(),
    onValidatedResult: (apincer.mobile.tradings.domain.AiAnalysisResult) -> Unit = {},
    onDone: () -> Unit,
    showSnackbar: (String) -> Unit,
    onAcceptAiPlan: (apincer.mobile.tradings.domain.AiRecommendation) -> Unit = {},
    /** When true the button is disabled with no plans, so no AI request is spent on an empty list. */
    requirePlans: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    val nothingToSend = requirePlans && allowedPlans.isEmpty()
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<apincer.mobile.tradings.domain.AiAnalysisResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentPlans by androidx.compose.runtime.rememberUpdatedState(allowedPlans)

    Button(
        onClick = {
            if (apiKey.isBlank()) {
                showSnackbar("Add your Gemini API key in Settings > AI Integration first.")
                return@Button
            }
            error = null
            result = null
            isLoading = true
            val prompt = buildPrompt()
            val requestedPlans = allowedPlans.toMap()
            coroutineScope.launch {
                val outcome = withContext(Dispatchers.IO) {
                    apincer.mobile.tradings.domain.GeminiClient.analyze(prompt, apiKey, geminiModelId)
                }
                isLoading = false
                outcome.onSuccess {
                    val validated = apincer.mobile.tradings.domain.AiRecommendationValidator.validate(
                        it, requestedPlans, currentPlans)
                    result = validated
                    onValidatedResult(validated)
                    onDone()
                    // Fix 3: Detect silent discard — AI returned picks but all were invalidated
                    // because prices moved between request and response (snapshot race condition).
                    if (validated.recommendations.isEmpty() && it.recommendations.isNotEmpty() &&
                        requestedPlans.isNotEmpty()) {
                        showSnackbar("Prices changed during analysis — plans were invalidated. Refresh and try again.")
                    }
                }.onFailure {
                    error = it.message ?: "AI analysis failed."
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading && !nothingToSend,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Analyzing…")
        } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
    if (nothingToSend) {
        Text(
            text = "No validated setups today, so there is nothing to send.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    // Fix 6: Warning must be visible — replace tiny labelSmall text with a prominent card.
    // The AI reasons only over the data in the prompt; no live prices, news, or earnings.
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ℹ️", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "AI has no live news or real-time prices. It reasons only over the snapshot data sent above. Do not follow reasoning that cites catalysts, earnings, or news not shown here.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 16.sp
            )
        }
    }

    error?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    result?.let { r ->
        Spacer(modifier = Modifier.height(12.dp))
        if (r.executiveSummary.isNotBlank()) {
            Text("Unverified AI interpretation · check all claims against the data above",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(r.executiveSummary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (r.recommendations.isEmpty()) {
            Text(emptyMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            r.recommendations.sortedByDescending { it.confidenceScore }.forEach { rec ->
                AiRecommendationCard(rec, onAccept = { onAcceptAiPlan(rec) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AiRecommendationCard(rec: apincer.mobile.tradings.domain.AiRecommendation, onAccept: () -> Unit) {
    val confidenceColor = when {
        rec.confidenceScore >= 70 -> MaterialTheme.colorScheme.tertiary
        rec.confidenceScore >= 40 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(rec.symbol, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                Surface(
                    color = confidenceColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Model assessment: ${when {
                            rec.confidenceScore >= 70 -> "High"
                            rec.confidenceScore >= 40 -> "Medium"
                            else -> "Low"
                        }}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = confidenceColor
                    )
                }
            }
            Text(rec.playbookType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Buy Zone: ${rec.buyZone}  •  Target: ${rec.targetProfit}  •  Stop: ${rec.stopLoss}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            if (rec.cashAllocation.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "💰 Cash Allocation: ${rec.cashAllocation}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("AI reasoning · unverified; use only the local levels above",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rec.reasoning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            // Secondary action: AI rankings are context, not a validated edge.
            OutlinedButton(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save as Satellite Plan")
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

/** Shown in place of the advisor buy lists while they remain untested; exits and the XD calendar stay. */
@Composable
fun UntestedListsHiddenCard(onShow: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ranked lists are hidden", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Additional ranked lists and the AI ranking are off by default. You can explore tracked stocks with the filters above. " +
                    "Each ranked list includes its test results and limitations. Your exit checks still run.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onShow) { Text("Show them anyway") }
        }
    }
}

/** Top 10 SET50 stocks by 6-month return (MOM3 in tools/backtest/momentum_portfolio_report.md). */
@Composable
fun MomentumListCard(viewModel: StockViewModel, heldSymbols: Set<String>) {
    val list by viewModel.momentumList.collectAsState()
    val loading by viewModel.momentumLoading.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshMomentumList() }
    RankedListCard(
        title = "6-month momentum list",
        rule = "Top 10 SET50 stocks by 6-month gain. Rule: at each month end, hold these at about 10% each inside your satellite, and sell names that drop out.",
        result = "Tested 2015-2025: +12.45% a year vs TDEX +2.52%, but -2.80% a year in 2021-2025 vs TDEX +3.19%, " +
            "worst drop 46%, and almost all profit came from DELTA, JMART and TRUE. It failed the evidence gate: keep it small.",
        loadingText = "Ranking SET50 from 6 months of prices…",
        rankedNote = "from dividend-adjusted closes. The list is meant to be acted on once a month, not daily.",
        list = list, loading = loading, heldSymbols = heldSymbols,
        onRefresh = { viewModel.refreshMomentumList(force = true) }
    )
}

/** Top 10 SET50 stocks by dividend yield (F5 in tools/backtest/fundamental_screens_report.md). */
@Composable
fun HighYieldListCard(viewModel: StockViewModel, heldSymbols: Set<String>) {
    val list by viewModel.highYieldList.collectAsState()
    val loading by viewModel.highYieldLoading.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshHighYieldList() }
    RankedListCard(
        title = "High dividend yield list",
        rule = "Top 10 SET50 stocks by current dividend yield. Rule: review monthly or quarterly, hold about 10% each, and replace names that drop out.",
        result = "Backtest 2015-2025: +7.80% a year vs TDEX +2.52%, ahead in both halves and in a 2011-2014 holdout, but inflated by survivorship. " +
            "The real 1DIV high-dividend ETF: +3.86% a year vs TDEX +2.55% over 2015-2025, ahead in 2021-2025, behind in 2015-2020 and 2012-2014. " +
            "Buying 1DIV gets the same tilt without picking stocks.",
        loadingText = "Fetching dividend yields for SET50…",
        rankedNote = "from SET quotes. A very high yield can mean the price fell because a dividend cut is expected.",
        list = list, loading = loading, heldSymbols = heldSymbols,
        onRefresh = { viewModel.refreshHighYieldList(force = true) },
        signed = false
    )
}

/**
 * Order list for the high-yield rule as tested: sell satellite names that left the list, keep held names
 * without resizing, buy new names at up to a tenth of the budget. Review only; nothing is executed.
 */
@Composable
fun HighYieldRebalanceCard(
    viewModel: StockViewModel, settingsViewModel: SettingsViewModel,
    portfolioItems: List<StockWatchlistInfo>, watchlist: List<StockWatchlistInfo>, cashBalance: Double,
    onRecordBuy: (String, Double, Int) -> Unit
) {
    val list by viewModel.highYieldList.collectAsState()
    val atsEnabled by settingsViewModel.isAtsEnabled.collectAsState()
    val stockCap by settingsViewModel.maxPortfolioAllocation.collectAsState()
    val sectorCap by settingsViewModel.maxSectorAllocation.collectAsState()
    val holdings = portfolioItems.filter { it.portfolio.quantity > 0 }.map {
        apincer.mobile.tradings.domain.HighYieldRebalance.Holding(it.info.symbol.uppercase(), it.portfolio.quantity,
            it.info.lastPrice.takeIf { p -> p > 0.0 }, it.portfolio.cost, it.info.sector)
    }
    val satelliteValue = holdings.filterNot { apincer.mobile.tradings.domain.CoreSatellite.isCore(it.symbol) }
        .sumOf { it.shares * (it.price ?: it.costPerShare) }
    var budgetText by rememberSaveable { mutableStateOf("") }
    val budget = budgetText.replace(",", "").toDoubleOrNull()?.takeIf { it >= 0.0 } ?: satelliteValue
    fun baht(v: Double) = String.format(Locale.ENGLISH, "฿%,.0f", v)

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Monthly rebalance plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Follows the tested rule once a month: sell names that left the list, keep held names as they are, " +
                "and buy each new name with up to a tenth of the budget. Nothing is executed. Place orders at your broker, " +
                "then record each fill (tick \"Record an already executed broker trade\").",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it },
                label = { Text("Satellite budget (baht)") },
                placeholder = { Text(String.format(Locale.ENGLISH, "%,.0f (current satellite value)", satelliteValue)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Text(if (budgetText.isBlank()) "Using ${baht(budget)}, your current satellite value (core funds excluded)." else "Using ${baht(budget)}.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val ranked = list?.second?.map { it.symbol }
            if (ranked.isNullOrEmpty()) {
                Text("Rank the list above first.", style = MaterialTheme.typography.bodySmall)
            } else {
                val plan = apincer.mobile.tradings.domain.HighYieldRebalance.plan(
                    budget, ranked, holdings,
                    watchlist.filter { it.info.lastPrice > 0.0 }.associate { it.info.symbol.uppercase() to it.info.lastPrice },
                    watchlist.mapNotNull { w -> w.info.sector?.let { w.info.symbol.uppercase() to it } }.toMap(),
                    cashBalance, cashBalance + holdings.sumOf { it.shares * (it.price ?: it.costPerShare) },
                    stockCap, sectorCap, atsEnabled)
                plan.rows.forEach { row ->
                    val action = when (row.action) {
                        apincer.mobile.tradings.domain.HighYieldRebalance.Action.BUY -> if (row.shares > 0) "Buy ${row.shares}" else "Buy 0"
                        apincer.mobile.tradings.domain.HighYieldRebalance.Action.KEEP -> "Keep ${row.shares}"
                        apincer.mobile.tradings.domain.HighYieldRebalance.Action.SELL -> "Sell ${row.shares}"
                        apincer.mobile.tradings.domain.HighYieldRebalance.Action.UNAVAILABLE -> "No price"
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${row.symbol}  ·  $action" + (row.price?.let { String.format(Locale.ENGLISH, " @ %.2f", it) } ?: ""),
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text((if (row.baht > 0.0) baht(row.baht) + "  " else "") + (row.note ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (row.note?.contains("cap") == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val price = row.price
                        if (row.action == apincer.mobile.tradings.domain.HighYieldRebalance.Action.BUY && row.shares > 0 && price != null)
                            TextButton(onClick = { onRecordBuy(row.symbol, price, row.shares) }) { Text("Record buy") }
                    }
                }
                Text("Sells ${baht(plan.sellProceeds)} · Buys ${baht(plan.buyCost)} · Fees ${baht(plan.fees)} · Cash left ${baht(plan.leftoverCash)}",
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                val full = plan.fullListBudget
                if (full != null && budget < full && plan.rows.any { it.action == apincer.mobile.tradings.domain.HighYieldRebalance.Action.BUY && it.shares == 0 })
                    Text("A tenth of this budget is below one lot of some names. Holding all ten at a tenth each needs about ${baht(full)}; " +
                        "below that, the 1DIV ETF gives the same tilt in small amounts.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun RankedListCard(
    title: String, rule: String, result: String, loadingText: String, rankedNote: String,
    list: Pair<String, List<apincer.mobile.tradings.domain.MomentumList.Entry>>?, loading: Boolean,
    heldSymbols: Set<String>, onRefresh: () -> Unit, signed: Boolean = true
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(rule, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f), shape = RoundedCornerShape(10.dp)) {
                Text(result, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(10.dp))
            }
            when {
                list == null && loading -> Text(loadingText, style = MaterialTheme.typography.bodySmall)
                list == null -> Text("Could not load data. Try again when online.", style = MaterialTheme.typography.bodySmall)
                else -> {
                    list.second.forEachIndexed { i, e ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${i + 1}. ${e.symbol}" + if (e.symbol in heldSymbols) "  · held" else "",
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(String.format(Locale.ENGLISH, if (signed) "%+.1f%%" else "%.1f%%", e.returnPercent), style = MaterialTheme.typography.bodyMedium,
                                color = if (e.returnPercent >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                        }
                    }
                    Text("Ranked ${list.first} $rankedNote", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onRefresh, enabled = !loading) { Text(if (loading) "Ranking…" else "Re-rank now") }
        }
    }
}
