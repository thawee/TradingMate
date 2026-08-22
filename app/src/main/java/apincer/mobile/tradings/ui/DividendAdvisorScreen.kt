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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoorFront
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
import androidx.compose.material3.OutlinedButton
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


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendAdvisorScreen(
    viewModel: StockViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToAcademy: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
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
            val geminiApiKey by settingsViewModel.geminiApiKey.collectAsState()
            val geminiModelId by settingsViewModel.geminiModel.collectAsState()

            val activeAlerts = alertRoutineState.activeAlerts
            val step1Done = alertRoutineState.step1Done
            
            androidx.compose.runtime.LaunchedEffect(activeAlerts.size, playbookMode) {
                if (activeAlerts.isEmpty() && !step1Done) {
                    viewModel.markAlertRoutineStepDone(1)
                }
            }

            // Market Regime Banner
            val marketRegime = alertRoutineState.marketRegime
            Surface(
                color = if (marketRegime.isBullish) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (marketRegime.isBullish) "📈 SET Regime: ${marketRegime.label}" else "⚠️ SET Regime: ${marketRegime.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (marketRegime.isBullish) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = if (marketRegime.isBullish) "Normal Sizing (100%)" else "Defensive Sizing (50%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (marketRegime.isBullish) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
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
                    maxOpenExposure = maxOpenExposure,
                    maxPortfolioAllocation = maxPortfolioAllocation,
                    apiKey = geminiApiKey,
                    geminiModelId = geminiModelId,
                    cashBalance = cashBalance,
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
                
                if (selectedArchetype == "ALL" && speculativePlays.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Speculative Watch (Low Quality)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("These stocks triggered technical buys but failed the strict Quality filter. Trade with caution.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    speculativePlays.forEach { stock ->
                        AdvisorStockCard(stock, viewModel)
                    }
                }
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
                                        text = stock.signal.type.name,
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
                                    border = if (borderAlpha > 0f) BorderStroke(0.5.dp, tagColor.copy(alpha = borderAlpha)) else null
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
                val isSet50 = apincer.mobile.tradings.domain.TradingConstants.SET50_SYMBOLS.contains(stock.info.symbol.uppercase())
                val stopPrice = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateSuggestedStopLossPrice(
                    lastPrice = stock.info.lastPrice,
                    atr = stock.portfolio.atr,
                    isSet50 = isSet50
                )
                val targetPrice = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateSuggestedTargetPrice(
                    lastPrice = stock.info.lastPrice,
                    stopLossPrice = stopPrice
                )
                val rr = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateRiskRewardRatio(stock.info.lastPrice, targetPrice, stopPrice)
                val stopPercent = ((stopPrice - stock.info.lastPrice) / stock.info.lastPrice) * 100

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stop ฿${String.format(Locale.ENGLISH, "%.2f", stopPrice)} (${String.format(Locale.ENGLISH, "%.1f", stopPercent)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "  •  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1
                            )
                            Text(
                                text = "Target ฿${String.format(Locale.ENGLISH, "%.2f", targetPrice)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
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
                                    text = "R:R ${String.format(Locale.ENGLISH, "%.1f", rr)}:1",
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
    apiKey: String,
    geminiModelId: String = "gemini-3.6-flash",
    cashBalance: Double = 0.0,
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

            Spacer(modifier = Modifier.height(8.dp))

            if (playbookMode == PlaybookMode.SWING) {
                val swingPlaysFilter = watchlist.filter {
                    it.info.lastPrice >= 1.0 && isLiquid(it) && isQual(it) && (isMom(it) || isSup(it)) && StockDna.isFlow(it)
                }.sortedByDescending { it.portfolio.relativeStrength ?: -999.0 }
                val gapUpPlaysFilter = watchlist.filter { it.info.lastPrice >= 1.0 && isLiquid(it) && isGapUp(it) }
                val speculativePromptPlays = speculativePlays.filter { it.info.lastPrice >= 1.0 }
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Data Preview: Sending ${swingPlaysFilter.size} Swing, ${gapUpPlaysFilter.size} Gap Up, and ${speculativePromptPlays.size} Speculative plays to AI.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Swing Trade Prompts
                val buildSwingPrompt = {
                    val lastSyncLocal = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"
                    val cashBalanceFormatted = String.format(Locale.ENGLISH, "%,.2f THB", cashBalance)
                    
                    val swingCandidates = if (swingPlaysFilter.isEmpty()) "None" else swingPlaysFilter.joinToString("\n") {
                        "- ${it.info.symbol}: Price=${it.info.lastPrice}, Vol=${it.info.volume ?: 0L}, P/E=${it.info.pe?.let { pe -> String.format(Locale.ENGLISH, "%.1f", pe) } ?: "N/A"}, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}, MACD Hist=${it.portfolio.macdHist?.let { m -> String.format(Locale.ENGLISH, "%.2f", m) } ?: "N/A"}, Signal=${it.portfolio.signalType ?: "NEUTRAL"} (${it.portfolio.signalReason ?: "N/A"})"
                    }
                    val gapUpCandidates = if (gapUpPlaysFilter.isEmpty()) "None" else gapUpPlaysFilter.joinToString("\n") {
                        "- ${it.info.symbol}: Price=${it.info.lastPrice}, Vol=${it.info.volume ?: 0L}, Chg=${String.format(Locale.ENGLISH, "%.1f", it.info.percentChange)}%, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, NPM=${it.info.netProfitMargin?.let { npm -> String.format(Locale.ENGLISH, "%.1f", npm) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}"
                    }
                    val speculativeCandidates = if (speculativePromptPlays.isEmpty()) "None" else speculativePromptPlays.joinToString("\n") {
                        "- ${it.info.symbol}: Price=${it.info.lastPrice}, Vol=${it.info.volume ?: 0L}, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"}, MACD Hist=${it.portfolio.macdHist?.let { m -> String.format(Locale.ENGLISH, "%.2f", m) } ?: "N/A"}, Signal=${it.portfolio.signalType ?: "NEUTRAL"} (${it.portfolio.signalReason ?: "N/A"})"
                    }
                    
                    """
                        Act as my expert subagents to evaluate the Stock Exchange of Thailand (SET) swing trade, gap up, and speculative candidates.
                        
                        DATA FRESHNESS & CAPITAL:
                        - Metrics last updated/synced on: $lastSyncLocal
                        - Available Cash Balance: $cashBalanceFormatted
                        
                        PLAYBOOK RULES & CONSTRAINTS:
                        1. Swing/Breakout Candidates (VIP Quality):
                           - Holding Period: 2-4 weeks.
                           - Technical Alignment: Entry near key moving average support (ideally price is above the 50-day SMA) or structural breakout levels.
                        2. Earnings Gap Candidates:
                           - Holding Period: Short-term momentum (typically 1-3 weeks).
                           - Technical Alignment: Entry near the gap-up support line or on breakout validation. Prioritize volume surge and strong catalyst.
                        3. Speculative Watch (Low Quality, incl. unconfirmed setups):
                           - High risk trades. Fundamentals are poor, but technicals are flashing oversold or reversal. Some are MACD-confirmed, others are early/unconfirmed (weaker signal). Trade only if the catalyst is extremely strong.
                        4. General Risk Constraints:
                           - Risk/Reward ratio MUST be asymmetric (minimum 2.0:1 R:R, e.g. Target Profit >= +6%, Stop Loss <= -3%). Stop loss must be placed below key technical support.
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
                        1. [market-researcher]: Search for upcoming earnings, news catalysts (last 7 days), and general sentiment for these tickers. Also check current SET index level, sector trends, and interest rates for macro context. (If live web search is unavailable in direct API mode, perform evaluation using the provided metrics, technical indicators, and known market knowledge).
                        2. [regime-manager]: Evaluate market regime (Bullish, Bearish, or Choppy/Sideways based on price relative to SMA 200/50 and MACD). In a Bull market, allow higher profit targets and breakout trailing stops; in a Bear/Choppy market, enforce capital preservation, tighter stop losses, and buying strictly at major technical support.
                        3. [risk-manager]: Select and rank the Top 3 setups across all lists. Prioritize VIP Swing and Gap Up plays over Speculative ones. Verify entry zones (e.g. SMA support or gap support). Define the exact Buy Zone, target profit (min 2.0:1 R:R), and strict Stop Loss for each setup. IMPORTANT: Intelligently split my available cash balance ($cashBalanceFormatted) across these recommended picks (specify recommended capital in THB and estimated share count for each stock, reserving a cash buffer if market risk is elevated). For each ranked pick, assign a Confidence Score (0-100%) and brief justification.
                        
                        EXPLAIN INSTRUCTIONS:
                        - Break down the recommendations step-by-step using clear, accessible logic.
                        - Provide a real-world analogy to describe the setup of the ranked pick.
                        
                        FORMAT REQUIREMENT:
                        Output the final recommendation as a clean Markdown report with the following structure:
                        ### Executive Summary (Market Regime, Macro Environment, Swing vs Gap Candidates)
                        ### Top Ranked Setups (Markdown Table: Ticker, Playbook Type, Buy Zone, Target, Stop Loss, Cash Allocation THB & Est. Shares, Confidence Score)
                        ### Subagent Analysis Details (Bull/Bear regime alignment, catalysts, technical support, risk parameters, confidence score justification)
                        ### Analogous Story (The real-world analogy)
                    """.trimIndent()
                }

                // Primary Highlighted Action: Direct In-App Analysis with Google Gemini
                AiAnalysisButton(
                    label = "Ask! Google Gemini",
                    apiKey = apiKey,
                    geminiModelId = geminiModelId,
                    buildPrompt = buildSwingPrompt,
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
                val dividendPlays = watchlist.filter { isLiquid(it) && isDiv(it) && isQual(it) }
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
                    val cashBalanceFormatted = String.format(Locale.ENGLISH, "%,.2f THB", cashBalance)
                    val dividendCandidates = if (dividendPlays.isEmpty()) "None" else dividendPlays
                        .sortedByDescending { it.info.dividendYield }
                        .joinToString("\n") {
                            "- ${it.info.symbol}: Price=${it.info.lastPrice}, Yield=${it.info.dividendYield?.let { y -> String.format(Locale.ENGLISH, "%.1f", y) } ?: "N/A"}%, ROE=${it.info.roe?.let { r -> String.format(Locale.ENGLISH, "%.1f", r) } ?: "N/A"}%, D/E=${it.info.debtToEquity?.let { de -> String.format(Locale.ENGLISH, "%.2f", de) } ?: "N/A"}, P/E=${it.info.pe?.let { pe -> String.format(Locale.ENGLISH, "%.1f", pe) } ?: "N/A"}, RSI=${it.portfolio.rsi?.let { rsi -> String.format(Locale.ENGLISH, "%.1f", rsi) } ?: "N/A"} (Updated=${it.info.lastUpdated})"
                        }

                    """
                        Act as my expert subagents to evaluate the Stock Exchange of Thailand (SET) dividend candidates.
                        
                        DATA FRESHNESS & CAPITAL:
                        - Metrics last updated/synced on: $lastSyncLocal
                        - Available Cash Balance: $cashBalanceFormatted
                        
                        CONSTRAINTS & PLAYBOOK (Dividend Accumulation):
                        - Holding Period: Long-term (indefinite hold for compound growth).
                        - Yield Threshold: Starting Dividend Yield MUST be >= 5%.
                        - Hard rule: Never average down on a breaking technical trend.
                        - Hold and accumulate/compound indefinitely, unless fundamentals break (ROE < 15%) or yield drops below 3%.
                        - RISK: Max $maxPortfolioAllocation% total portfolio allocation per asset.
                        
                        GUARDRAILS & NEGATIVE CONSTRAINTS:
                        - DO NOT recommend penny stocks (price < 1.0 THB) or highly illiquid assets.
                        - DO NOT recommend leveraged or complex structured products (e.g. DWs, TFEX warrants).
                        - DO NOT formulate response as direct financial advice; frame the analysis as educational research.

                        I am loading the 'Dividend Accumulation Playbook' for the candidates below.
                        Note: All candidates have already passed static baseline filters (ROE > 15%, D/E < 1.5, NPM > 10%).
                        
                        Candidates Yielding > 5%:
                        $dividendCandidates
                        
                        DELEGATED TASKS:
                        1. [market-researcher]: Search for forward-looking dividend safety (check cash flow trend, forward payout ratio, and upcoming earnings outlook) for these SET tickers. Also check current SET index level and general market sentiment. (If live web search is unavailable in direct API mode, perform evaluation using the provided metrics, fundamental indicators, and known market knowledge).
                        2. [regime-manager]: Assess market regime (Bullish vs Bearish/Choppy). In a Bear/Choppy market, prioritize defensive blue-chips with higher yields (>6%) and safe payout ratios; in a Bull market, focus on dividend growth & compounding.
                        3. [risk-manager]: Recommend the Top 3 additions. Calculate the 'Max Buy Price' for each to guarantee a >=5% yield and ensure it fits my overall risk exposure. IMPORTANT: Intelligently split my available cash balance ($cashBalanceFormatted) across these recommended picks (specify recommended capital in THB and estimated share count for each stock). For each ranked pick, assign a Confidence Score (0-100%) and brief justification.
                        
                        EXPLAIN INSTRUCTIONS:
                        - Break down the recommendations step-by-step using clear, accessible logic.
                        - Provide a real-world analogy to explain why the ranked stock is a reliable dividend payer.
                        
                        FORMAT REQUIREMENT:
                        Output the final recommendation as a clean Markdown report with the following structure:
                        ### Executive Summary (Dividend Outlook, Market Regime, Macro Environment)
                        ### Top Ranked Dividend Additions (Markdown Table: Ticker, Yield, Max Buy Price, Cash Allocation THB & Est. Shares, Confidence Score)
                        ### Subagent Safety & Cash Flow Analysis (Regime alignment, payout safety, cash flow metrics, confidence score justification)
                        ### Analogous Story (The real-world analogy)
                    """.trimIndent()
                }

                // Primary Highlighted Action: Direct In-App Analysis with Google Gemini
                AiAnalysisButton(
                    label = "Ask! Google Gemini",
                    apiKey = apiKey,
                    geminiModelId = geminiModelId,
                    buildPrompt = buildDividendPrompt,
                    onDone = onMarkAiDone,
                    showSnackbar = showSnackbar
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
    onDone: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<apincer.mobile.tradings.domain.AiAnalysisResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

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
            coroutineScope.launch {
                val outcome = withContext(Dispatchers.IO) {
                    apincer.mobile.tradings.domain.GeminiClient.analyze(prompt, apiKey, geminiModelId)
                }
                isLoading = false
                outcome.onSuccess {
                    result = it
                    onDone()
                }.onFailure {
                    error = it.message ?: "AI analysis failed."
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
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

    Text(
        text = "No live web/news search — reasons only over the data above. Requires a Gemini API key (Settings).",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 4.dp)
    )

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
            Text(r.executiveSummary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (r.recommendations.isEmpty()) {
            Text("No recommendations returned.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            r.recommendations.sortedByDescending { it.confidenceScore }.forEach { rec ->
                AiRecommendationCard(rec)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AiRecommendationCard(rec: apincer.mobile.tradings.domain.AiRecommendation) {
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
                        text = "Confidence ${rec.confidenceScore}%",
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
            Text(rec.reasoning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
