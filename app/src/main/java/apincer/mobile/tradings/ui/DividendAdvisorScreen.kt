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
import androidx.compose.foundation.clickable
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

/** One line of a holding alert and how soon it needs the user. */
data class AlertLine(val level: apincer.mobile.tradings.domain.AlertLevel, val text: String)

data class SellAlertData(
    val stock: StockWatchlistInfo,
    val reason: String,
    val lines: List<AlertLine> = listOf(AlertLine(apincer.mobile.tradings.domain.AlertLevel.ACT_NOW, reason))
) {
    /** The most urgent line decides where the holding is listed. */
    val level: apincer.mobile.tradings.domain.AlertLevel get() = lines.minOf { it.level }
}

/** Toggle order follows declaration order: Dividend first (core-first decision, 2026-10-03). */
enum class PlaybookMode(val label: String) {
    DIVIDEND("Dividend"),
    SWING("Swing (context)")
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
    val alertRoutineState by viewModel.alertRoutineState.collectAsState()
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val cashBalance by viewModel.cashBalance.collectAsState()
    val dcaAmount by settingsViewModel.monthlyDcaAmount.collectAsState()
    val dcaDay by settingsViewModel.dcaDayOfMonth.collectAsState()
    val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"

    val showSwingTab by settingsViewModel.showSwingTab.collectAsState()
    val playbookMode = if (showSwingTab) alertRoutineState.playbookMode else PlaybookMode.DIVIDEND
    val swingSellAlerts = alertRoutineState.swingSellAlerts
    val dividendSellAlerts = alertRoutineState.dividendSellAlerts
    val portfolioItems = alertRoutineState.portfolioItems
    val marketRegime = alertRoutineState.marketRegime
    val dividendSymbols = portfolioItems.filter { it.portfolio.tradePurpose == "DIVIDEND" }
        .map { it.info.symbol.uppercase() }.toSet()
    LaunchedEffect(dividendSymbols) { viewModel.refreshDividendCuts(dividendSymbols) }
    val heldSymbols = portfolioItems.map { it.info.symbol.uppercase() }.toSet()
    LaunchedEffect(heldSymbols) { viewModel.refreshAccountingFlags(heldSymbols) }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scrollState = rememberScrollState()
    var researchOpen by rememberSaveable { mutableStateOf(false) }
    val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok"))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Advisor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
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

            // Custom segmented glass control selector, only when the Swing tab is enabled in Settings
            if (showSwingTab) Surface(
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
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Today: what needs the user, most urgent first, across both tabs.
                    val allAlerts = (swingSellAlerts + dividendSellAlerts).map { alert ->
                        val first = alert.lines.filter { it.level == alert.level }.firstOrNull()?.text ?: alert.reason
                        apincer.mobile.tradings.domain.AdvisorToday.HoldingAlert(alert.stock.info.symbol.uppercase(), alert.level, first)
                    }
                    TodayCard(apincer.mobile.tradings.domain.AdvisorToday.items(
                        allAlerts, dividendSymbols.isNotEmpty(), dcaAmount, dcaDay, today))

                    if (playbookMode == PlaybookMode.SWING) {
                        UntestedEdgeNotice()
                        RegimeBanner(marketRegime)
                    }

                    // 2. This tab's holdings, grouped by how soon they need the user.
                    val tabAlerts = when {
                        playbookMode == PlaybookMode.SWING -> swingSellAlerts
                        showSwingTab -> dividendSellAlerts
                        else -> swingSellAlerts + dividendSellAlerts
                    }
                    HoldingAlertsSection(tabAlerts, onOpen = { viewModel.fetchStockData(it) })

                    // 3. The one tested rule: high dividend yield list, forward record and rebalance plan.
                    if (playbookMode == PlaybookMode.DIVIDEND) {
                        val heldSymbols = portfolioItems.map { it.info.symbol.uppercase() }.toSet()
                        HighYieldListCard(viewModel, heldSymbols)
                        HighYieldRebalanceCard(viewModel, settingsViewModel, portfolioItems, watchlist, cashBalance, onRecordBuy)
                        UpcomingDividendsCard(portfolioItems, today)
                    }

                    // 4. Research, collapsed: filters over all tracked stocks.
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { researchOpen = !researchOpen },
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Research", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Filter all tracked stocks. Matches are for study, not buy calls.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(if (researchOpen) "Hide" else "Show", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (researchOpen) {
                        AdvisorFilterExplorer(
                            stocks = watchlist,
                            mode = playbookMode,
                            bearish = marketRegime == TechnicalAnalysis.MarketRegime.BEARISH,
                            onStockClick = { viewModel.fetchStockData(it) }
                        )
                    }

                    Text(
                        text = "For educational purposes only. Not financial advice. Trade at your own risk.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/** Most urgent first; when nothing needs action it says so. */
@Composable
private fun TodayCard(items: List<apincer.mobile.tradings.domain.AdvisorToday.Item>) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(alertIcon(item.level), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(item.text, style = MaterialTheme.typography.bodyMedium, color = alertColor(item.level))
                }
            }
        }
    }
}

private fun alertIcon(level: apincer.mobile.tradings.domain.AlertLevel) = when (level) {
    apincer.mobile.tradings.domain.AlertLevel.ACT_NOW -> "🔴"
    apincer.mobile.tradings.domain.AlertLevel.MONTHLY -> "🟠"
    apincer.mobile.tradings.domain.AlertLevel.REVIEW -> "⚪"
}

/** Red only for act now; limits and plans to review use the monthly-review orange. */
@Composable
internal fun alertColor(level: apincer.mobile.tradings.domain.AlertLevel): Color = when (level) {
    apincer.mobile.tradings.domain.AlertLevel.ACT_NOW -> MaterialTheme.colorScheme.error
    apincer.mobile.tradings.domain.AlertLevel.MONTHLY -> Color(0xFFFFA726)
    apincer.mobile.tradings.domain.AlertLevel.REVIEW -> MaterialTheme.colorScheme.onSurface
}

/** Holding alerts grouped Act now / At the monthly review / Review; each card lists all of its lines. */
@Composable
private fun HoldingAlertsSection(alerts: List<SellAlertData>, onOpen: (String) -> Unit) {
    Text("Your holdings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp))
    if (alerts.isEmpty()) {
        Text("Nothing to act on or review.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val titles = mapOf(
        apincer.mobile.tradings.domain.AlertLevel.ACT_NOW to "Act now",
        apincer.mobile.tradings.domain.AlertLevel.MONTHLY to "At the monthly review",
        apincer.mobile.tradings.domain.AlertLevel.REVIEW to "Review only")
    alerts.groupBy { it.level }.toSortedMap().forEach { (level, group) ->
        Text(titles.getValue(level), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
            color = alertColor(level))
        group.forEach { alert ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpen(alert.stock.info.symbol) },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(alert.stock.info.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        val net = alert.stock.netProfitPercent
                        Text(String.format(Locale.ENGLISH, "฿%.2f · P/L %+.2f%%", alert.stock.info.lastPrice, net),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                    }
                    alert.lines.sortedBy { it.level }.forEach { line ->
                        Text("${alertIcon(line.level)} ${line.text}", style = MaterialTheme.typography.bodySmall,
                            color = alertColor(line.level))
                    }
                }
            }
        }
    }
}

/** SET market regime and the cash buffer it suggests; applies to Swing buys only (core DCA and the rebalance are exempt). */
@Composable
private fun RegimeBanner(marketRegime: TechnicalAnalysis.MarketRegime) {
    val isBull = marketRegime == TechnicalAnalysis.MarketRegime.BULLISH
    val isBear = marketRegime == TechnicalAnalysis.MarketRegime.BEARISH
    val bg = when {
        isBull -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        isBear -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val fg = when {
        isBull -> MaterialTheme.colorScheme.onTertiaryContainer
        isBear -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val sizing = when { isBull -> "normal size (100%)"; isBear -> "defensive size (50%)"; else -> "selective size (75%)" }
    val buffer = when { isBull -> "10-15%"; isBear -> "50%+"; else -> "25-35%" }
    Surface(color = bg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("SET: ${marketRegime.label}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = fg)
            Text("Swing buys only: $sizing, cash buffer $buffer. Core DCA and the dividend rebalance are not affected.",
                style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.85f))
        }
    }
}

/** Announced ex-dates (SET) for holdings; when none is announced, the latest past ones. */
@Composable
private fun UpcomingDividendsCard(holdings: List<StockWatchlistInfo>, today: java.time.LocalDate) {
    val dated = holdings.mapNotNull { h ->
        h.info.dividendDate?.let { runCatching { java.time.LocalDate.parse(it.take(10)) }.getOrNull() }?.let { h to it }
    }
    if (dated.isEmpty()) return
    val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    val upcoming = dated.filter { !it.second.isBefore(today) }.sortedBy { it.second }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Upcoming dividends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (upcoming.isEmpty()) {
                Text("No ex-dates announced for your holdings.", style = MaterialTheme.typography.bodySmall)
                val recent = dated.sortedByDescending { it.second }.take(4)
                Text("Latest: " + recent.joinToString(", ") { "${it.first.info.symbol} ${it.second.format(fmt)}" },
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else upcoming.forEach { (h, date) ->
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
                Text("${h.info.symbol}: ex-date ${date.format(fmt)}" + (if (days == 0L) " (today)" else " (in $days days)") +
                    ", ${h.portfolio.quantity} shares held. Own them before the ex-date to receive it.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Top 10 SET50 stocks by dividend yield (F5 in tools/backtest/fundamental_screens_report.md). */
@Composable
fun HighYieldListCard(viewModel: StockViewModel, heldSymbols: Set<String>) {
    val list by viewModel.highYieldList.collectAsState()
    val loading by viewModel.highYieldLoading.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshHighYieldList() }
    RankedListCard(
        title = "High dividend yield list",
        summary = "Top 10 SET50 by yield, reviewed monthly. Backtest +7.80% a year vs TDEX +2.52% (2015-2025).",
        detail = "Rule: review monthly or quarterly, hold about 10% each, and replace names that drop out. " +
            "The backtest was ahead in both halves of 2015-2025 and in a 2011-2014 holdout, but is inflated by survivorship. " +
            "The real 1DIV high-dividend ETF: +3.86% a year vs TDEX +2.55% over 2015-2025, ahead in 2021-2025, behind in 2015-2020 and 2012-2014. " +
            "Buying 1DIV gets the same tilt without picking stocks.",
        loadingText = "Fetching dividend yields for SET50…",
        rankedNote = "from SET quotes. A very high yield can mean the price fell because a dividend cut is expected.",
        list = list, loading = loading, heldSymbols = heldSymbols,
        onRefresh = { viewModel.refreshHighYieldList(force = true) },
        signed = false
    )
    HighYieldForwardRecord(viewModel)
}

/** Monthly snapshots of the list measured against TDEX after 1, 3 and 12 months (HighYieldTracker). */
@Composable
private fun HighYieldForwardRecord(viewModel: StockViewModel) {
    val track by viewModel.highYieldTrack.collectAsState()
    val summary = apincer.mobile.tradings.domain.HighYieldTracker.summary(track)
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Forward record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            WhyNote("Each month's list against TDEX after 1, 3 and 12 months. Saved: ${track.size} month(s)" +
                (track.firstOrNull()?.let { " since ${it.month}" } ?: "") + ".",
                "Prices are saved with each list and dividends are included. Names are fixed when saved, so a later delisting stays in the record.")
            ForwardRecordLines(summary)
            if (summary.isNotEmpty()) Text("A fair read needs a year or more of months.", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val tr2 by viewModel.tr2Track.collectAsState()
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Without commodity names (comparison)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            WhyNote("Recorded only, never traded. Saved: ${tr2.size} month(s)" +
                (tr2.firstOrNull()?.let { " since ${it.month}" } ?: "") + ".",
                "The same list without " + apincer.mobile.tradings.domain.HighYieldList.COMMODITY.sorted().joinToString(", ") +
                ". It beat the list in the 2015-2025 backtest with a smaller drawdown but trailed in 2021-2025, and the names were chosen with hindsight.")
            ForwardRecordLines(apincer.mobile.tradings.domain.HighYieldTracker.summary(tr2))
        }
    }
}

@Composable
private fun ForwardRecordLines(summary: List<apincer.mobile.tradings.domain.HighYieldTracker.HorizonSummary>) {
    fun pct(v: Double) = String.format(Locale.ENGLISH, "%+.1f%%", v)
    if (summary.isEmpty()) {
        Text("No results yet: the first one is due 30 days after the first saved list.",
            style = MaterialTheme.typography.bodySmall)
    } else summary.forEach { h ->
        val label = when (h.days) { 30 -> "1 month"; 91 -> "3 months"; else -> "12 months" }
        Text("$label: ${h.measured} list(s) · list ${pct(h.avgListPercent)} vs TDEX ${pct(h.avgTdexPercent)} · " +
            "ahead in ${h.ahead} of ${h.measured}", style = MaterialTheme.typography.bodyMedium)
    }
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
            it.info.lastPrice.takeIf { p -> p > 0.0 }, it.portfolio.cost, it.info.sector,
            managed = it.portfolio.tradePurpose == "DIVIDEND")
    }
    val managedValue = holdings.filter { it.managed && !apincer.mobile.tradings.domain.CoreSatellite.isCore(it.symbol) }
        .sumOf { it.shares * (it.price ?: it.costPerShare) }
    var budgetText by rememberSaveable { mutableStateOf("") }
    val budget = budgetText.replace(",", "").toDoubleOrNull()?.takeIf { it >= 0.0 } ?: managedValue
    fun baht(v: Double) = String.format(Locale.ENGLISH, "฿%,.0f", v)

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Monthly rebalance plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            WhyNote("Review only. Place orders at your broker, then Record buy.",
                "Follows the tested rule once a month: sell names that left the list, keep held names as they are, " +
                "and buy each new name with up to a tenth of the budget. It manages holdings recorded with the Dividend purpose; others are never sold.")
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it },
                label = { Text("Satellite budget (baht)") },
                placeholder = { Text(String.format(Locale.ENGLISH, "%,.0f", managedValue)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Text(when {
                    budgetText.isNotBlank() -> "Using ${baht(budget)}."
                    managedValue > 0.0 -> "Using ${baht(budget)}, the value of your Dividend-purpose holdings."
                    else -> "No Dividend-purpose holdings yet: enter the amount to invest."
                },
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
                val (belowOneLot, shown) = plan.rows.partition {
                    it.action == apincer.mobile.tradings.domain.HighYieldRebalance.Action.BUY && it.shares == 0
                }
                shown.forEach { row ->
                    val action = when (row.action) {
                        apincer.mobile.tradings.domain.HighYieldRebalance.Action.BUY -> "Buy ${row.shares}"
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
                                color = if (row.note?.contains("cap") == true) alertColor(apincer.mobile.tradings.domain.AlertLevel.MONTHLY) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val price = row.price
                        if (row.action == apincer.mobile.tradings.domain.HighYieldRebalance.Action.BUY && row.shares > 0 && price != null)
                            TextButton(onClick = { onRecordBuy(row.symbol, price, row.shares) }) { Text("Record buy") }
                    }
                }
                Text("Sells ${baht(plan.sellProceeds)} · Buys ${baht(plan.buyCost)} · Fees ${baht(plan.fees)} · Cash left ${baht(plan.leftoverCash)}",
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                if (belowOneLot.isNotEmpty()) {
                    val full = plan.fullListBudget
                    Text("Not bought, a tenth of the budget is below one lot: ${belowOneLot.joinToString { it.symbol }}." +
                        (if (full != null && budget < full) " All ten need about ${baht(full)}; below that, the 1DIV ETF gives the same tilt in small amounts." else ""),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** One line always visible; the reasoning behind it opens on demand. */
@Composable
private fun WhyNote(summary: String, detail: String) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.Top) {
        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(if (open) "Less" else "Why", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp).clickable { open = !open })
    }
    if (open) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RankedListCard(
    title: String, summary: String, detail: String, loadingText: String, rankedNote: String,
    list: Pair<String, List<apincer.mobile.tradings.domain.MomentumList.Entry>>?, loading: Boolean,
    heldSymbols: Set<String>, onRefresh: () -> Unit, signed: Boolean = true
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            WhyNote(summary, detail)
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
