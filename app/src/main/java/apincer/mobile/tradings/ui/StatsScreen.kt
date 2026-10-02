package apincer.mobile.tradings.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import apincer.mobile.tradings.R
import apincer.mobile.tradings.data.PortfolioSnapshotEntity
import apincer.mobile.tradings.data.TradeEntity
import apincer.mobile.tradings.domain.TechnicalAnalysis
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StockViewModel,
    portfolioViewModel: PortfolioViewModel = viewModel(),
    showSnackbar: (String) -> Unit,
    onNavigateToBacktest: () -> Unit = {}
) {
    val history by portfolioViewModel.tradeHistory.collectAsState()
    val cashBalance by portfolioViewModel.cashBalance.collectAsState()
    val dividendHistory by portfolioViewModel.dividendHistory.collectAsState()
    val snapshots by portfolioViewModel.allSnapshots.collectAsState()
    val portfolioHistoricalCloses by portfolioViewModel.portfolioHistoricalCloses.collectAsState()
    val indexHistory by portfolioViewModel.indexHistory.collectAsState()
    val satelliteScorecard by portfolioViewModel.satelliteScorecard.collectAsState()
    val chronologicalSnapshots = remember(snapshots) { navSnapshotsInDateOrder(snapshots) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val watchlist by viewModel.watchlistInfo.collectAsState()
    var trajectoryMode by remember { mutableStateOf("MTM") }

    val openSymbols = watchlist.filter { it.portfolio.quantity > 0 }.map { it.portfolio.symbol }.sorted()
    LaunchedEffect(openSymbols) {
        if (openSymbols.isNotEmpty()) {
            while (true) {
                portfolioViewModel.loadHistoricalClosesForHoldings(openSymbols)
                kotlinx.coroutines.delay(60L * 60L * 1000L)
            }
        }
    }
    LaunchedEffect(openSymbols, history.size) {
        portfolioViewModel.loadSatelliteScorecard(watchlist.filter { it.portfolio.quantity > 0 })
    }
    LaunchedEffect(watchlist, cashBalance) {
        val openPositions = watchlist.filter { it.portfolio.quantity > 0 }
        if (openPositions.isNotEmpty() || cashBalance != 0.0) {
            portfolioViewModel.takeSnapshot(watchlist)
        }
    }

    val totalProfit = history.sumOf { it.netProfitBaht }
    val totalDividendReceived = dividendHistory.sumOf { it.totalReceived }
    // Beginning cash = reverse-engineer starting capital:
    // currentCash + costOfOpenHoldings - realisedProfit - dividendsReceived
    // (dividends are income that also flowed into cash, so must be subtracted to get original capital)
    val investedCapital = watchlist.filter { it.portfolio.quantity > 0 }.sumOf { it.portfolio.cost * it.portfolio.quantity }
    val beginningCash = cashBalance + investedCapital - totalProfit - totalDividendReceived

    // Period Calculations
    val now = Calendar.getInstance()
    
    val last12Months = remember(history) {
        (11 downTo 0).map { i ->
            val cal = Calendar.getInstance().apply { 
                time = now.time
                add(Calendar.MONTH, -i) 
            }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val monthStr = SimpleDateFormat("MMM", Locale.ENGLISH).format(cal.time)
            
            val profit = history.filter {
                val tCal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                tCal.get(Calendar.YEAR) == year && tCal.get(Calendar.MONTH) == month
            }.sumOf { it.netProfitBaht }
            
            Pair(monthStr, profit)
        }
    }

    val cumulativeProfits = remember(history, last12Months) {
        val startOf12Months = Calendar.getInstance().apply {
            time = now.time
            add(Calendar.MONTH, -11)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var runningTotal = history.filter { it.dateMillis < startOf12Months }.sumOf { it.netProfitBaht }

        last12Months.map { (month, profit) ->
            runningTotal += profit
            Pair(month, runningTotal)
        }
    }

    val mtdProfit = history.filter { 
        val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
        cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    }.sumOf { it.netProfitBaht }

    val ytdProfit = history.filter { 
        val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    }.sumOf { it.netProfitBaht }

    val winCount = history.count { it.netProfitBaht > 0 }
    val winRate = if (history.isNotEmpty()) (winCount.toDouble() / history.size) * 100 else 0.0

    // Efficiency Metrics
    val wins = history.filter { it.netProfitBaht > 0 }
    val losses = history.filter { it.netProfitBaht < 0 }
    val avgWin = if (wins.isNotEmpty()) wins.sumOf { it.netProfitBaht } / wins.size else 0.0
    val avgLoss = if (losses.isNotEmpty()) losses.sumOf { it.netProfitBaht } / losses.size else 0.0
    val totalFees = history.sumOf { 
        TechnicalAnalysis.calculateFees(it.buyPrice * it.quantity, false) + 
        TechnicalAnalysis.calculateFees(it.sellPrice * it.quantity, true) 
    }

    val bestTrade = history.maxByOrNull { it.netProfitBaht }

    if (showConfirmDialog) {
        GlassDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = stringResource(R.string.title_clear_history),
            confirmButton = {
                Button(
                    onClick = {
                        portfolioViewModel.clearTradeHistory()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_delete_all), color = Color.White)
                }
            },
            dismissButton = {

                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {

            Text(stringResource(R.string.confirm_clear_history))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.title_history), fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                IconButton(onClick = onNavigateToBacktest) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = stringResource(R.string.title_backtest),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (history.isNotEmpty()) {
                    IconButton(onClick = { showConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.desc_clear_history),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Performance Summary Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource(R.string.label_beginning_cash), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text("฿${String.format(Locale.ENGLISH,"%,.2f", beginningCash)}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.label_win_rate), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.ENGLISH,"%,.1f", winRate)}%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp).alpha(0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMetric(stringResource(R.string.label_total_profit), totalProfit)
                            StatMetric(stringResource(R.string.label_this_month), mtdProfit)
                            StatMetric(stringResource(R.string.label_this_year), ytdProfit)
                        }
                    }
                }
            }

            item {
                SatelliteScorecardCard(satelliteScorecard)
            }

            // profit graph for 12 month period
            item {
                SectionHeader(
                    title = "12-Month Profit History",
                    icon = Icons.Default.History,
                    subtitle = "Monthly realized profit/loss"
                )
            }
            
            item {
                ProfitHistoryChart(last12Months = last12Months)
            }

            item {
                SectionHeader(
                    title = if (trajectoryMode == "MTM" && snapshots.size >= 2) "Mark-to-Market NAV" else "Cumulative Profit",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    subtitle = if (trajectoryMode == "MTM" && snapshots.size >= 2) "${snapshots.size}-Day Daily NAV Trajectory" else "12-Month Realized PnL Trajectory"
                )
            }

            if (snapshots.size >= 2) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = trajectoryMode == "MTM",
                            onClick = { trajectoryMode = "MTM" },
                            label = { Text("MTM NAV (Daily)", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = trajectoryMode == "REALIZED",
                            onClick = { trajectoryMode = "REALIZED" },
                            label = { Text("Realized PnL (Monthly)", fontSize = 12.sp) }
                        )
                    }
                }
            }

            item {
                val chartData = if (trajectoryMode == "MTM" && snapshots.size >= 2) {
                    chronologicalSnapshots.map { Pair(it.date.takeLast(5), it.totalValue + it.cashBalance) }
                } else {
                    cumulativeProfits
                }
                CumulativeProfitChart(cumulativeProfits = chartData)
            }

            item {
                SectionHeader(
                    title = "Quantitative Risk Matrix",
                    icon = Icons.Default.Insights,
                    subtitle = "VaR 95%, Tail Risk (CVaR) & Max Drawdown"
                )
            }

            item {
                InstitutionalRiskCard(
                    portfolioItems = watchlist.filter { it.portfolio.quantity > 0 },
                    cashBalance = cashBalance,
                    cumulativeProfits = cumulativeProfits,
                    snapshots = chronologicalSnapshots,
                    portfolioHistoricalCloses = portfolioHistoricalCloses,
                    indexHistory = indexHistory
                )
            }

            // Trading Efficiency Section
            /*
            item {
                val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"
                SectionHeader(
                    title = stringResource(R.string.section_trading_efficiency),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    subtitle = "${history.size} trades • Synced $lastSync"
                )
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {

                                Text(stringResource(R.string.label_avg_win), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                                Text("฿${String.format(Locale.ENGLISH,"%,.0f", avgWin)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                Text(stringResource(R.string.label_avg_loss), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                                Text("฿${String.format(Locale.ENGLISH,"%,.0f", avgLoss)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                            }
                            Column(horizontalAlignment = Alignment.End) {

                                Text(stringResource(R.string.label_total_fees), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                                Text("฿${String.format(Locale.ENGLISH,"%,.0f", totalFees)}", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        if (bestTrade != null) {
                            HorizontalDivider(modifier = Modifier.alpha(0.05f))
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Text(stringResource(R.string.label_best_performer), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)

                                Text(bestTrade.symbol, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.weight(1f))

                                Text("+฿${String.format(Locale.ENGLISH,"%,.0f", bestTrade.netProfitBaht)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            } */

            item {
                SectionHeader(
                    title = stringResource(R.string.section_recent_trades),
                    icon = Icons.Default.History,
                    subtitle = "${history.size} records"
                )
            }

            if (history.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                            Text(stringResource(R.string.label_no_trade_history), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(history) { trade ->
                    TradeHistoryCard(trade, onUndo = { portfolioViewModel.undoSell(trade) })
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

/** The DAO emits newest first, but the NAV chart and peak-to-trough drawdown need oldest first. */
internal fun navSnapshotsInDateOrder(snapshots: List<PortfolioSnapshotEntity>): List<PortfolioSnapshotEntity> =
    snapshots.sortedBy { it.date }

@Composable
fun StatMetric(label: String, value: Double) {
    Column {

        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

        Text(
            text = "${if (value >= 0) "+" else ""}฿${String.format(Locale.ENGLISH,"%,.0f", value)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = if (value >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun TradeHistoryCard(trade: TradeEntity, onUndo: (() -> Unit)? = null) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", LocalLocale.current.platformLocale)
    val dateStr = dateFormat.format(Date(trade.dateMillis))
    val isWin = trade.netProfitBaht > 0

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {

                    Text(text = trade.symbol, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-0.5).sp)

                    Text(text = dateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "฿${trade.buyPrice} → ฿${trade.sellPrice}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {

                    Text(
                        text = "${if (isWin) "+" else ""}฿${String.format(Locale.ENGLISH,"%,.2f", trade.netProfitBaht)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = if (isWin) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = "${String.format(Locale.ENGLISH,"%.2f", trade.netProfitPercent)}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isWin) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )

                    Text(text = "${trade.quantity} Shares", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }

            if (onUndo != null) {
                // Undo restores the shares and reverses the cash, so it asks first.
                var confirmUndo by remember { mutableStateOf(false) }
                if (confirmUndo) {
                    GlassDialog(
                        onDismissRequest = { confirmUndo = false },
                        title = stringResource(R.string.action_undo_sell),
                        confirmButton = {
                            Button(
                                onClick = { confirmUndo = false; onUndo() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(stringResource(R.string.action_undo_sell), color = Color.White) }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmUndo = false }) { Text(stringResource(R.string.action_cancel)) }
                        }
                    ) {
                        Text("Put ${trade.quantity} ${trade.symbol} back in your portfolio and reverse the sale proceeds in cash?")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { confirmUndo = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.action_undo_sell), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (trade.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {

                        Text(text = stringResource(R.string.label_lesson_learned), fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = trade.note,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfitHistoryChart(last12Months: List<Pair<String, Double>>) {
    val maxProfit = last12Months.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val minProfit = last12Months.minOfOrNull { it.second }?.coerceAtMost(-1.0) ?: -1.0
    val maxAbsValue = maxOf(Math.abs(maxProfit), Math.abs(minProfit))
    
    val positiveColor = MaterialTheme.colorScheme.tertiary
    val negativeColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    GlassCard(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp)) {
            val width = size.width
            val height = size.height
            val rightMargin = 32.dp.toPx()
            val chartWidth = width - rightMargin
            
            // Draw zero line
            val zeroY = height - (height * (0.0 - (-maxAbsValue)) / (2 * maxAbsValue)).toFloat()
            drawLine(
                color = gridColor,
                start = Offset(0f, zeroY),
                end = Offset(chartWidth, zeroY),
                strokeWidth = 1.dp.toPx()
            )
            
            // Draw Y-axis labels
            val labelPaint = android.graphics.Paint().apply {
                this.color = textColor
                this.textSize = 10.sp.toPx()
                this.textAlign = android.graphics.Paint.Align.LEFT
            }
            
            fun formatCompact(v: Double): String {
                val a = Math.abs(v)
                val s = if (v < 0) "-" else ""
                return when {
                    a >= 1_000_000 -> "$s${String.format(Locale.ENGLISH, "%.1f", a / 1_000_000)}M"
                    a >= 1_000 -> "$s${String.format(Locale.ENGLISH, "%.1f", a / 1_000)}k"
                    else -> "$s${String.format(Locale.ENGLISH, "%.0f", a)}"
                }
            }
            
            drawContext.canvas.nativeCanvas.drawText(formatCompact(maxAbsValue), chartWidth + 4.dp.toPx(), 10.sp.toPx() / 2, labelPaint)
            drawContext.canvas.nativeCanvas.drawText("0", chartWidth + 4.dp.toPx(), zeroY + 10.sp.toPx() / 2, labelPaint)
            drawContext.canvas.nativeCanvas.drawText(formatCompact(-maxAbsValue), chartWidth + 4.dp.toPx(), height + 10.sp.toPx() / 2, labelPaint)
            
            val barWidth = chartWidth / (last12Months.size * 1.5f)
            
            // Draw bars
            last12Months.forEachIndexed { index, (month, profit) ->
                val x = (chartWidth / last12Months.size) * index + (chartWidth / last12Months.size) / 2f - barWidth / 2f
                val profitRatio = Math.abs(profit) / maxAbsValue
                val barHeight = (height / 2f) * profitRatio.toFloat()
                val y = if (profit >= 0) zeroY - barHeight else zeroY
                
                val color = if (profit >= 0) positiveColor else negativeColor
                
                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )
                
                // Draw month label
                val paint = android.graphics.Paint().apply {
                    this.color = textColor
                    this.textSize = 10.sp.toPx()
                    this.textAlign = android.graphics.Paint.Align.CENTER
                }
                
                drawContext.canvas.nativeCanvas.drawText(
                    month,
                    x + barWidth / 2f,
                    height + 20.dp.toPx(),
                    paint
                )
            }
        }
    }
}

@Composable
fun CumulativeProfitChart(cumulativeProfits: List<Pair<String, Double>>) {
    val maxProfit = cumulativeProfits.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val minProfit = cumulativeProfits.minOfOrNull { it.second }?.coerceAtMost(-1.0) ?: -1.0
    val range = (maxProfit - minProfit).coerceAtLeast(1.0)
    
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    GlassCard(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp)) {
            val width = size.width
            val height = size.height
            val rightMargin = 32.dp.toPx()
            val chartWidth = width - rightMargin
            
            val labelPaint = android.graphics.Paint().apply {
                this.color = textColor
                this.textSize = 10.sp.toPx()
                this.textAlign = android.graphics.Paint.Align.LEFT
            }
            
            fun formatCompact(v: Double): String {
                val a = Math.abs(v)
                val s = if (v < 0) "-" else ""
                return when {
                    a >= 1_000_000 -> "$s${String.format(Locale.ENGLISH, "%.1f", a / 1_000_000)}M"
                    a >= 1_000 -> "$s${String.format(Locale.ENGLISH, "%.1f", a / 1_000)}k"
                    else -> "$s${String.format(Locale.ENGLISH, "%.0f", a)}"
                }
            }
            
            // Draw zero line if zero is within the range
            if (minProfit < 0 && maxProfit > 0) {
                val zeroY = height - (height * (0.0 - minProfit) / range).toFloat()
                drawLine(
                    color = gridColor,
                    start = Offset(0f, zeroY),
                    end = Offset(chartWidth, zeroY),
                    strokeWidth = 1.dp.toPx()
                )
                drawContext.canvas.nativeCanvas.drawText("0", chartWidth + 4.dp.toPx(), zeroY + 10.sp.toPx() / 2, labelPaint)
            }
            
            drawContext.canvas.nativeCanvas.drawText(formatCompact(maxProfit), chartWidth + 4.dp.toPx(), 10.sp.toPx() / 2, labelPaint)
            drawContext.canvas.nativeCanvas.drawText(formatCompact(minProfit), chartWidth + 4.dp.toPx(), height + 10.sp.toPx() / 2, labelPaint)
            
            val stepX = chartWidth / (cumulativeProfits.size - 1).coerceAtLeast(1).toFloat()
            val points = cumulativeProfits.mapIndexed { index, (_, totalProfit) ->
                val x = index * stepX
                val y = height - ((totalProfit - minProfit) / range * height).toFloat()
                Offset(x, y)
            }

            val path = Path().apply { addSmoothCubicCurve(points) }
            val fillPath = Path().apply {
                addSmoothCubicCurve(points)
                lineTo(chartWidth, height)
                lineTo(0f, height)
                close()
            }
            
            cumulativeProfits.forEachIndexed { index, (month, _) ->
                val pt = points[index]
                
                // Draw month label
                val paint = android.graphics.Paint().apply {
                    this.color = textColor
                    this.textSize = 10.sp.toPx()
                    this.textAlign = android.graphics.Paint.Align.CENTER
                }
                
                drawContext.canvas.nativeCanvas.drawText(
                    month,
                    pt.x,
                    height + 20.dp.toPx(),
                    paint
                )
                
                // Draw points
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = pt
                )
            }
            
            // Draw fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )
            
            // Draw line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun InstitutionalRiskCard(
    portfolioItems: List<StockWatchlistInfo>,
    cashBalance: Double,
    cumulativeProfits: List<Pair<String, Double>>,
    snapshots: List<PortfolioSnapshotEntity> = emptyList(),
    portfolioHistoricalCloses: Map<String, List<apincer.mobile.tradings.data.ScrapedHistoricalPrice>> = emptyMap(),
    indexHistory: List<apincer.mobile.tradings.data.ScrapedHistoricalPrice> = emptyList()
) {
    val totalStockValue = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
    val totalAssets = totalStockValue + maxOf(cashBalance, 0.0)

    // 1. Beta Calculation
    val indexByDate = indexHistory.associate { it.date to it.close }
    val latestIndexIsRecent = indexHistory.lastOrNull()?.date?.let { date ->
        runCatching { !java.time.LocalDate.parse(date).isBefore(
            java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).minusDays(7)) }.getOrDefault(false)
    } ?: false
    val betaWeights = portfolioItems.mapNotNull { item ->
        val prices = portfolioHistoricalCloses[item.portfolio.symbol.uppercase()].orEmpty()
        val aligned = prices.filter { it.date in indexByDate }.takeLast(64)
        val weight = item.info.lastPrice * item.portfolio.quantity
        val beta = if (aligned.size == 64) TechnicalAnalysis.calculateBeta(
            aligned.map { it.close }, aligned.map { indexByDate.getValue(it.date) }) else null
        if (weight > 0.0 && beta != null) weight to beta else null
    }
    val portfolioBeta = if (latestIndexIsRecent && betaWeights.size == portfolioItems.size &&
        betaWeights.isNotEmpty() && totalAssets > 0.0)
        TechnicalAnalysis.calculatePortfolioBeta(betaWeights) * totalStockValue / totalAssets else null
    val betaProfile = when {
        portfolioBeta == null -> Pair("History unavailable", MaterialTheme.colorScheme.onSurfaceVariant)
        portfolioBeta < 0.85 -> Pair("Defensive Low-Vol", Color(0xFF6EE7B7))
        portfolioBeta <= 1.15 -> Pair("Balanced Index Track", Color(0xFF60A5FA))
        else -> Pair("Aggressive High-Beta", Color(0xFFFCD34D))
    }

    // 2. Max Drawdown from MTM daily snapshots if available, else cumulative profit trajectory
    val equitySeries = if (snapshots.size >= 2) {
        snapshots.map { it.totalValue + it.cashBalance }
    } else if (cumulativeProfits.isNotEmpty()) {
        cumulativeProfits.map { totalAssets + it.second }
    } else {
        listOf(totalAssets)
    }
    val mddResult = TechnicalAnalysis.calculateMaxDrawdown(equitySeries)

    // 3. 63-Day Rolling Time-Series Return Distribution (Historical VaR/CVaR)
    val histories = portfolioItems.map { item -> portfolioHistoricalCloses[item.portfolio.symbol.uppercase()].orEmpty() }
    val commonDates = histories.map { history -> history.map { it.date }.toSet() }
        .reduceOrNull { a, b -> a intersect b }.orEmpty().sorted().takeLast(64)
    val holdingsWithPrices = if (commonDates.size == 64) portfolioItems.mapIndexed { index, item ->
        val pricesByDate = histories[index].associate { it.date to it.close }
        (item.info.lastPrice * item.portfolio.quantity) to commonDates.map { pricesByDate.getValue(it) }
    } else emptyList()
    val empiricalReturns = if (holdingsWithPrices.size == portfolioItems.size && holdingsWithPrices.isNotEmpty())
        TechnicalAnalysis.calculatePortfolioHistoricalReturns(holdingsWithPrices, 63) else emptyList()
    val asOfDate = commonDates.lastOrNull()
    val isRecentHistory = asOfDate?.let { date ->
        runCatching { !java.time.LocalDate.parse(date).isBefore(
            java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).minusDays(7)) }.getOrDefault(false)
    } ?: false
    val isTimeSeriesVaR = empiricalReturns.size == 63 && isRecentHistory
    val var95 = if (empiricalReturns.isNotEmpty()) TechnicalAnalysis.calculateHistoricalVaR(empiricalReturns, 0.95) else 0.0
    val cvar95 = if (empiricalReturns.isNotEmpty()) TechnicalAnalysis.calculateConditionalVaR(empiricalReturns, 0.95) else 0.0
    val varBaht = totalAssets * (var95 / 100.0)
    val cvarBaht = totalAssets * (cvar95 / 100.0)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Risk Matrix & Volatility",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isTimeSeriesVaR) "63 daily observations · as of $asOfDate" else
                            "Price history unavailable or stale${asOfDate?.let { " (last $it)" } ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = betaProfile.second.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (portfolioBeta == null) "Beta unavailable" else "Beta ${String.format(Locale.ENGLISH, "%.2f", portfolioBeta)} · ${betaProfile.first}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = betaProfile.second
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.alpha(0.08f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("1-Day VaR (95%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(if (isTimeSeriesVaR) "฿${String.format(Locale.ENGLISH, "%,.0f", varBaht)}" else "—", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    Text(if (isTimeSeriesVaR) "(-${String.format(Locale.ENGLISH, "%.2f", var95)}%)" else "Need 64 shared closes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tail Risk (CVaR)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(if (isTimeSeriesVaR) "฿${String.format(Locale.ENGLISH, "%,.0f", cvarBaht)}" else "—", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    Text(if (isTimeSeriesVaR) "(-${String.format(Locale.ENGLISH, "%.2f", cvar95)}%)" else "Unavailable", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Max Drawdown (MDD)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text("-${String.format(Locale.ENGLISH, "%.2f", mddResult.maxDrawdownPercent)}%", fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (mddResult.maxDrawdownPercent > 15.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Text(if (snapshots.size >= 2) "MTM Daily NAV" else "Current: -${String.format(Locale.ENGLISH, "%.1f", mddResult.currentDrawdownPercent)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SatelliteScorecardCard(report: apincer.mobile.tradings.domain.SatelliteScorecard.Report?) {
    val core = apincer.mobile.tradings.domain.CoreSatellite.CORE_SYMBOL
    fun pct(v: Double?) = v?.let { String.format(Locale.ENGLISH, "%+.1f%%", it) } ?: "n/a"
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Satellite vs $core", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Same cash flows replayed into $core. Annual money-weighted return.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            val windows = listOfNotNull(
                report?.sinceStart?.let { "Since first fill" to it },
                report?.trailing12m?.let { "Last 12 months" to it },
                report?.trailing12mPriorQuarter?.let { "12 months to last quarter" to it }
            )
            when {
                report == null -> Text("Loading...", style = MaterialTheme.typography.bodySmall)
                windows.isEmpty() -> Text(
                    when {
                        report.firstFillMillis != null -> "First comparison on " +
                            java.time.Instant.ofEpochMilli(report.firstFillMillis)
                                .plus(java.time.Duration.ofDays(apincer.mobile.tradings.domain.SatelliteScorecard.MIN_HISTORY_DAYS.toLong()))
                                .atZone(java.time.ZoneId.of("Asia/Bangkok")).toLocalDate() +
                            ", ${apincer.mobile.tradings.domain.SatelliteScorecard.MIN_HISTORY_DAYS} days after your first journaled satellite fill."
                        report.coverage.excluded.isEmpty() -> "No journaled satellite trades yet. Buys and sells recorded in the app build this scorecard."
                        else -> "Current holdings were added without full fill records, so they cannot be compared yet. New buys and sells recorded in the app build this scorecard."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                else -> windows.forEach { (label, c) ->
                    val ahead = c.excessBaht >= 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Satellite ${pct(c.satelliteXirrPercent)} · $core ${pct(c.shadowXirrPercent)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            String.format(Locale.ENGLISH, "%s฿%,.0f", if (ahead) "+" else "-", kotlin.math.abs(c.excessBaht)),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (ahead) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            if (report?.suggestReducingSatellite == true) {
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        "Your satellite trailed $core in both of the last two 12-month windows. Consider lowering the satellite share in Settings > Core Portfolio.",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            val excluded = report?.coverage?.excluded.orEmpty()
            if (excluded.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Excluded (fills not fully journaled): ${excluded.sorted().joinToString()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
