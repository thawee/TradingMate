package apincer.mobile.tradings.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
    showSnackbar: (String) -> Unit
) {
    val history by portfolioViewModel.tradeHistory.collectAsState()
    val cashBalance by portfolioViewModel.cashBalance.collectAsState()
    val dividendHistory by portfolioViewModel.dividendHistory.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    val watchlist by viewModel.watchlistInfo.collectAsState()

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
                    title = "Cumulative Profit",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    subtitle = "12-Month Trajectory"
                )
            }

            item {
                CumulativeProfitChart(cumulativeProfits = cumulativeProfits)
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
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onUndo,
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
            val path = Path()
            val fillPath = Path()
            
            cumulativeProfits.forEachIndexed { index, (month, totalProfit) ->
                val x = index * stepX
                val y = height - ((totalProfit - minProfit) / range * height).toFloat()
                
                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                
                // Draw month label
                val paint = android.graphics.Paint().apply {
                    this.color = textColor
                    this.textSize = 10.sp.toPx()
                    this.textAlign = android.graphics.Paint.Align.CENTER
                }
                
                drawContext.canvas.nativeCanvas.drawText(
                    month,
                    x,
                    height + 20.dp.toPx(),
                    paint
                )
                
                // Draw points
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            // Finish fill path
            fillPath.lineTo(chartWidth, height)
            fillPath.close()
            
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
