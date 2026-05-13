package apincer.mobile.tradings.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TechnicalAnalysis
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: StockViewModel, 
    onSelectStock: (String) -> Unit, 
    onOpenEducation: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val watchlistInfo by viewModel.watchlistInfo.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    val focusedStocks = watchlistInfo.filter { item ->
        val signal = item.signal
        if (signal == null || signal.type == IndicatorSignal.NEUTRAL) return@filter false
        
        // Show if we own it (Portfolio) OR if it has a BUY/POTENTIAL signal (Watchlist Awareness)
        if (item.portfolio.quantity > 0) {
            true
        } else {
            signal.type == IndicatorSignal.BUY || signal.type == IndicatorSignal.POTENTIAL
        }
    }

    // Dividend Alerts: XD Date soon
    val currentPlatformLocale = LocalLocale.current.platformLocale
    val dividendAlerts = watchlistInfo.filter {
        val divDate = it.info.dividendDate
        if (divDate.isNullOrBlank()) return@filter false
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", currentPlatformLocale)
            val xdDate = sdf.parse(divDate) ?: return@filter false
            val today = Calendar.getInstance()
            val xd = Calendar.getInstance().apply { time = xdDate }

            // Show alert if XD is within next 14 days
            val diff = (xd.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)
            diff in 0..14
        } catch (e: Exception) { false }
    }
    
    val marketStatus = TechnicalAnalysis.getMarketStatus()
    
    val lastUpdated = watchlistInfo.map { it.info.lastUpdated }
        .filter { it.isNotEmpty() }
        .maxOrNull() ?: "N/A"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TradingMate", fontWeight = FontWeight.Black) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. The TradingMate Story Area
                        Card(
                            onClick = onOpenAbout,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HistoryEdu, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Our Story", 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 14.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Idea & Concept", 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        // 2. Trading Academy Area
                        Card(
                            onClick = onOpenEducation,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Academy", 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 14.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Learn Workflow", 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(text = "Market Pulse", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Last update: $lastUpdated", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = marketStatus.color.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.extraSmall,
                            border = androidx.compose.foundation.BorderStroke(1.dp, marketStatus.color.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = marketStatus.label,
                                color = marketStatus.color,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { viewModel.refreshWatchlistInfo() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Refresh, 
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (dividendAlerts.isNotEmpty()) {
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Dividend Opportunities", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Stocks with upcoming XD dates in next 14 days", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(Modifier.height(12.dp))
                        
                        dividendAlerts.forEach { item ->
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelectStock(item.info.symbol) }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.info.symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("XD Date: ${item.info.dividendDate}", fontSize = 12.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${String.format(Locale.ENGLISH, "%.2f", item.info.dividendYield)}%", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary, fontSize = 16.sp)
                                        Text("Yield", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Risk Warning & Disclaimer", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 13.sp, 
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Trading signals and alerts are calculated by algorithms for learning purposes. They are NOT financial advice. Always perform your own research. All investments carry risk of loss.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            val sellStocks = focusedStocks.filter { it.signal?.type == IndicatorSignal.SELL }
            val buyStocks = focusedStocks.filter { it.signal?.type == IndicatorSignal.BUY }
            val potentialStocks = focusedStocks.filter { it.signal?.type == IndicatorSignal.POTENTIAL }

            if (focusedStocks.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Attention Required", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (sellStocks.isNotEmpty()) {
                    item {
                        Text(text = "Sell Signals", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(sellStocks) { item ->
                        FocusedStockCard(item, onClick = { onSelectStock(item.info.symbol) })
                    }
                }

                if (buyStocks.isNotEmpty()) {
                    item {
                        Text(text = "Buy Signals", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(buyStocks) { item ->
                        FocusedStockCard(item, onClick = { onSelectStock(item.info.symbol) })
                    }
                }

                if (potentialStocks.isNotEmpty()) {
                    item {
                        Text(text = "Potential Signals", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(potentialStocks) { item ->
                        FocusedStockCard(item, onClick = { onSelectStock(item.info.symbol) })
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Text("No active alerts. Your portfolio is stable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusedStockCard(item: StockWatchlistInfo, onClick: () -> Unit) {
    val signal = item.signal!!
    val color = when (signal.type) {
        IndicatorSignal.BUY -> MaterialTheme.colorScheme.tertiary
        IndicatorSignal.POTENTIAL -> MaterialTheme.colorScheme.secondary
        IndicatorSignal.SELL -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.5f)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.info.symbol, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = color) {
                            Text(signal.type.name, color = Color.White)
                        }
                    }
                    item.info.name?.let {
                        Text(text = it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "฿${item.info.lastPrice}", fontWeight = FontWeight.Bold)
                    if (item.portfolio.quantity > 0) {
                        Text(
                            text = "${if (item.netProfitPercent >= 0) "+" else ""}${String.format(Locale.ENGLISH,"%.2f", item.netProfitPercent)}%",
                            color = if (item.netProfitPercent >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                color = color.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = signal.reason, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                    Text(text = signal.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IndicatorMiniBox(
                    label = "RSI", 
                    value = String.format(Locale.ENGLISH,"%.1f", item.portfolio.rsi ?: 0.0),
                    meaning = when {
                        (item.portfolio.rsi ?: 0.0) < 35.0 -> "Cheap (Oversold)"
                        (item.portfolio.rsi ?: 0.0) > 65.0 -> "Expensive (Overbought)"
                        else -> "Neutral"
                    },
                    modifier = Modifier.weight(1f)
                )
                IndicatorMiniBox(
                    label = "Momentum", 
                    value = if ((item.portfolio.macdHist ?: 0.0) > 0) "Bullish" else "Bearish",
                    meaning = "MACD Trend",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun IndicatorMiniBox(label: String, value: String, meaning: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            Text(text = meaning, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
