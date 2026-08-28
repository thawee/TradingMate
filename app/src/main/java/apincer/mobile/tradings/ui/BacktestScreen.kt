package apincer.mobile.tradings.ui

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.data.SetScraper
import apincer.mobile.tradings.domain.BacktestEngine
import apincer.mobile.tradings.domain.BacktestResult
import apincer.mobile.tradings.domain.TradingConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Replays the app's own BUY/SELL signal engine (getDetailedSignal) over each
 * stock's own daily price history to answer: "historically, would following
 * this app's signals on this stock have made money?"
 *
 * This is intentionally a simple single-position, fully-compounded simulation —
 * see BacktestEngine's kdoc for the full list of simplifying assumptions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktestScreen(
    viewModel: StockViewModel,
    onBack: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val symbols = remember(watchlist) { watchlist.map { it.info.symbol }.distinct().sorted() }

    var selectedSymbol by remember { mutableStateOf<String?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var singleResult by remember { mutableStateOf<BacktestResult?>(null) }
    var batchResults by remember { mutableStateOf<List<BacktestResult>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (selectedSymbol == null && symbols.isNotEmpty()) {
        selectedSymbol = symbols.first()
    }

    fun runSingle() {
        val symbol = selectedSymbol ?: return
        val watchInfo = watchlist.find { it.info.symbol == symbol }
        isRunning = true
        errorMessage = null
        batchResults = emptyList()
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val history = SetScraper.fetchHistoricalPrices(symbol)
                    BacktestEngine.run(
                        symbol = symbol,
                        history = history,
                        isSet50 = TradingConstants.SET50_SYMBOLS.contains(symbol.uppercase()),
                        isFundamentalGood = watchInfo?.info?.isFundamentalGood ?: false,
                        dividendYield = watchInfo?.info?.dividendYield,
                        roe = watchInfo?.info?.roe
                    )
                }
            }
            isRunning = false
            result.onSuccess { r ->
                singleResult = r
                if (r == null) errorMessage = "Not enough price history for $symbol (need 212+ trading days)."
            }.onFailure { e ->
                errorMessage = e.localizedMessage ?: "Failed to fetch history for $symbol"
            }
        }
    }

    fun runAllWatchlist() {
        isRunning = true
        errorMessage = null
        singleResult = null
        coroutineScope.launch {
            val results = withContext(Dispatchers.IO) {
                watchlist.mapNotNull { w ->
                    runCatching {
                        val history = SetScraper.fetchHistoricalPrices(w.info.symbol)
                        BacktestEngine.run(
                            symbol = w.info.symbol,
                            history = history,
                            isSet50 = TradingConstants.SET50_SYMBOLS.contains(w.info.symbol.uppercase()),
                            isFundamentalGood = w.info.isFundamentalGood,
                            dividendYield = w.info.dividendYield,
                            roe = w.info.roe
                        )
                    }.getOrNull()
                }
            }
            isRunning = false
            if (results.isEmpty()) {
                errorMessage = "No watchlist stocks had enough price history to backtest."
            } else {
                batchResults = results.sortedByDescending { it.expectancyPercent }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Backtest (Beta)", fontWeight = FontWeight.Black) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("How this works", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Replays the app's real BUY/SELL signal logic day-by-day over ~1 year of " +
                                "each stock's own price history (no look-ahead). Buys the day after a BUY " +
                                "signal, sells when the same logic used on the Watchlist/Portfolio screens " +
                                "(stop-loss, trailing stop, take-profit, overbought) says SELL. Fees are " +
                                "netted in. This is an estimate, not a guarantee — it doesn't model " +
                                "slippage, partial fills, or the 5-Layer DNA candidate filters.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (symbols.isEmpty()) {
                item {
                    Text(
                        "Add stocks to your Watchlist first to run a backtest.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(symbols) { symbol ->
                            FilterChip(
                                selected = selectedSymbol == symbol,
                                onClick = { selectedSymbol = symbol },
                                label = { Text(symbol, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { runSingle() },
                            enabled = !isRunning && selectedSymbol != null,
                            modifier = Modifier.weight(1f)
                        ) { Text("Run Backtest") }
                        OutlinedButton(
                            onClick = { runAllWatchlist() },
                            enabled = !isRunning,
                            modifier = Modifier.weight(1f)
                        ) { Text("Run Whole Watchlist") }
                    }
                }
            }

            if (isRunning) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            errorMessage?.let { msg ->
                item {
                    Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }

            singleResult?.let { result ->
                item { BacktestSummaryCard(result) }
                items(result.closedTrades.reversed()) { trade ->
                    BacktestTradeRow(trade)
                }
                result.openTrade?.let { open ->
                    item { BacktestTradeRow(open) }
                }
            }

            if (batchResults.isNotEmpty()) {
                item {
                    Text(
                        "Watchlist Backtest Summary (sorted by expectancy)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                items(batchResults) { result ->
                    BacktestSummaryCard(result, compact = true)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BacktestSummaryCard(result: BacktestResult, compact: Boolean = false) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(result.symbol, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    "${result.startDate ?: "?"} → ${result.endDate ?: "?"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (result.totalTrades == 0) {
                Text(
                    "No BUY signal fired in this window, so no simulated trades.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val winColor = if (result.totalReturnPercent >= 0) Color(0xFF00C853) else Color.Red
                StatRow("Trades", "${result.totalTrades}", "Win Rate", String.format(Locale.ENGLISH, "%.0f%%", result.winRatePercent))
                StatRow(
                    "Avg Win", String.format(Locale.ENGLISH, "+%.1f%%", result.avgWinPercent),
                    "Avg Loss", String.format(Locale.ENGLISH, "%.1f%%", result.avgLossPercent)
                )
                StatRow(
                    "Expectancy/Trade", String.format(Locale.ENGLISH, "%.2f%%", result.expectancyPercent),
                    "Max Drawdown", String.format(Locale.ENGLISH, "-%.1f%%", result.maxDrawdownPercent)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Compounded Return: ${String.format(Locale.ENGLISH, "%+.1f%%", result.totalReturnPercent)}",
                    fontWeight = FontWeight.Bold,
                    color = winColor,
                    fontSize = 14.sp
                )
            }
            if (!compact && result.openTrade != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Currently holding a simulated open position (unrealized).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatRow(label1: String, value1: String, label2: String, value2: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(label1, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value1, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(label2, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value2, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun BacktestTradeRow(trade: apincer.mobile.tradings.domain.BacktestTrade) {
    GlassCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${trade.entryDate} → ${trade.exitDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val color = if (trade.netProfitPercent >= 0) Color(0xFF00C853) else Color.Red
                Text(
                    String.format(Locale.ENGLISH, "%+.2f%%", trade.netProfitPercent),
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "฿${String.format(Locale.ENGLISH, "%.2f", trade.entryPrice)} → ฿${String.format(Locale.ENGLISH, "%.2f", trade.exitPrice)} · ${trade.holdingDays}d · ${trade.exitReason}",
                fontSize = 11.sp
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Transparent)
}
