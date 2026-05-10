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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@Composable
fun HomeScreen(viewModel: StockViewModel, onSelectStock: (String) -> Unit) {
    val watchlistInfo by viewModel.watchlistInfo.collectAsState()
    val cashBalance by viewModel.cashBalance.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    val portfolioItems = watchlistInfo.filter { it.portfolio.quantity > 0 }
    
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
            val xdDate = sdf.parse(divDate)
            val today = Calendar.getInstance()
            val xd = Calendar.getInstance().apply { time = xdDate }

            // Show alert if XD is within next 14 days
            val diff = (xd.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)
            diff in 0..14
        } catch (e: Exception) { false }
    }
    val stockValue = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
    val totalAssetValue = stockValue + cashBalance
    val totalCostRaw = portfolioItems.sumOf { it.portfolio.cost * it.portfolio.quantity }
    
    // Weighted Yield on Cost
    val totalDividendBaht = portfolioItems.sumOf { 
        val yield = it.info.dividendYield ?: 0.0
        val priceAtDiv = it.info.lastPrice
        (yield / 100.0) * priceAtDiv * it.portfolio.quantity
    }
    val avgYieldOnCost = if (totalCostRaw > 0) (totalDividendBaht / totalCostRaw) * 100 else 0.0

    val totalBuyFees = portfolioItems.map { TechnicalAnalysis.calculateFees(it.portfolio.cost * it.portfolio.quantity, false) }.sum()
    val totalSellFees = portfolioItems.map { TechnicalAnalysis.calculateFees(it.info.lastPrice * it.portfolio.quantity, true) }.sum()
    val totalFees = totalBuyFees + totalSellFees

    val grossProfit = stockValue - totalCostRaw
    val netProfitValue = grossProfit - totalFees
    val totalNetProfitPercent = if (totalCostRaw > 0) (netProfitValue / totalCostRaw) * 100 else 0.0
    val marketStatus = TechnicalAnalysis.getMarketStatus()
    
    val lastUpdated = watchlistInfo.map { it.info.lastUpdated }
        .filter { it.isNotEmpty() }
        .maxOrNull() ?: "N/A"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(text = "Summary", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Last update: $lastUpdated", fontSize = 12.sp, color = Color.Gray)
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
            Spacer(modifier = Modifier.height(12.dp))
            PortfolioSummaryCard(
                totalAssetValue, 
                stockValue, 
                cashBalance, 
                grossProfit, 
                totalFees, 
                netProfitValue, 
                totalNetProfitPercent,
                avgYieldOnCost
            )
        }

        if (dividendAlerts.isNotEmpty()) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Dividend Opportunities", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Stocks with upcoming XD dates in next 14 days", fontSize = 12.sp, color = Color.Gray)
                    
                    Spacer(Modifier.height(12.dp))
                    
                    dividendAlerts.forEach { item ->
                        Surface(
                            color = Color(0xFFE8F5E9).copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelectStock(item.info.symbol) }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.info.symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("XD Date: ${item.info.dividendDate}", fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${item.info.dividendYield}%", fontWeight = FontWeight.Black, color = Color(0xFF1B5E20), fontSize = 16.sp)
                                    Text("Yield", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4).copy(alpha = 0.3f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBC02D).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning, 
                        contentDescription = null, 
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Risk Warning & Disclaimer", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 13.sp, 
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "Trading signals and alerts are calculated by algorithms for learning purposes. They are NOT financial advice. Always perform your own research. All investments carry risk of loss.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        if (focusedStocks.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Attention Required", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            items(focusedStocks) { item ->
                FocusedStockCard(item, onClick = { onSelectStock(item.info.symbol) })
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
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Text("No active alerts. Your portfolio is stable.", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun RefreshActionPill(lastUpdated: String, isRefreshing: Boolean, onRefresh: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.medium,
        onClick = onRefresh,
        enabled = !isRefreshing
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh, 
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "UPDATED", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(text = lastUpdated, fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
fun PortfolioSummaryCard(
    totalAssetValue: Double,
    stockValue: Double,
    cashBalance: Double,
    grossProfit: Double, 
    totalFees: Double, 
    netProfit: Double, 
    netPercent: Double,
    yieldOnCost: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Total Assets (Stock + Cash)", fontSize = 14.sp)
            Text(
                text = "฿${String.format(Locale.ENGLISH,"%,.2f", totalAssetValue)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            
            SummaryDetailRow("Stock Value", "฿${String.format(Locale.ENGLISH,"%,.2f", stockValue)}", MaterialTheme.colorScheme.onPrimaryContainer)
            SummaryDetailRow("Cash Balance", "฿${String.format(Locale.ENGLISH,"%,.2f", cashBalance)}", MaterialTheme.colorScheme.onPrimaryContainer)
            
            Spacer(modifier = Modifier.height(12.dp))

            SummaryDetailRow("Gross Profit", "฿${String.format(Locale.ENGLISH,"%,.2f", grossProfit)}", if (grossProfit >= 0) Color(0xFF00C853) else Color.Red)
            SummaryDetailRow("Total Thai Fees (0.32%)", "-฿${String.format(Locale.ENGLISH,"%,.2f", totalFees)}", Color.Gray)
            SummaryDetailRow("Avg Yield on Cost", "${String.format(Locale.ENGLISH,"%.2f", yieldOnCost)}%", Color(0xFF2E7D32))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Net Realized (Post-Fee)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${if (netProfit >= 0) "+" else ""}฿${String.format(Locale.ENGLISH,"%,.2f", netProfit)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netProfit >= 0) Color(0xFF00C853) else Color.Red
                    )
                }
                Text(
                    text = "${String.format(Locale.ENGLISH,"%.2f", netPercent)}%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = if (netProfit >= 0) Color(0xFF00C853) else Color.Red
                )
            }
        }
    }
}

@Composable
fun SummaryDetailRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun FocusedStockCard(item: StockWatchlistInfo, onClick: () -> Unit) {
    val signal = item.signal!!
    val color = when (signal.type) {
        IndicatorSignal.BUY -> Color(0xFF00C853)
        IndicatorSignal.POTENTIAL -> Color(0xFFFFB300)
        IndicatorSignal.SELL -> Color.Red
        else -> Color.Gray
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
                        Text(text = it, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "฿${item.info.lastPrice}", fontWeight = FontWeight.Bold)
                    if (item.portfolio.quantity > 0) {
                        Text(
                            text = "${if (item.netProfitPercent >= 0) "+" else ""}${String.format(Locale.ENGLISH,"%.2f", item.netProfitPercent)}%",
                            color = if (item.netProfitPercent >= 0) Color(0xFF00C853) else Color.Red,
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
                    Text(text = signal.description, fontSize = 12.sp, color = Color.DarkGray, lineHeight = 18.sp)
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
                Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            Text(text = meaning, fontSize = 9.sp, color = Color.Gray)
        }
    }
}
