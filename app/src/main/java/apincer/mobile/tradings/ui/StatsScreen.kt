package apincer.mobile.tradings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.data.TradeEntity
import apincer.mobile.tradings.domain.TechnicalAnalysis
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalLocale

@Composable
fun StatsScreen(viewModel: StockViewModel) {
    val history by viewModel.tradeHistory.collectAsState()
    val cashBalance by viewModel.cashBalance.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    val totalProfit = history.sumOf { it.netProfitBaht }
    val beginningCash = cashBalance - totalProfit
    
    // Period Calculations
    val now = Calendar.getInstance()
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
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to delete all trade history? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearTradeHistory()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Trade Statistics", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Improve your trading discipline", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (history.isNotEmpty()) {
                    IconButton(onClick = { showConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        // Performance Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Beginning Cash", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("฿${String.format(Locale.ENGLISH,"%,.2f", beginningCash)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Win Rate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.ENGLISH,"%.1f", winRate)}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.3f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatMetric("Total Profit", totalProfit)
                        StatMetric("This Month", mtdProfit)
                        StatMetric("This Year", ytdProfit)
                    }
                }
            }
        }

        // Trading Efficiency Section
        item {
            Text(text = "Trading Efficiency", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Avg Win", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("฿${String.format(Locale.ENGLISH,"%.0f", avgWin)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Avg Loss", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("฿${String.format(Locale.ENGLISH,"%.0f", avgLoss)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Fees", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("฿${String.format(Locale.ENGLISH,"%.0f", totalFees)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (bestTrade != null) {
                        HorizontalDivider(modifier = Modifier.alpha(0.1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Best Stock: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(bestTrade.symbol, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.weight(1f))
                            Text("+฿${String.format(Locale.ENGLISH,"%.0f", bestTrade.netProfitBaht)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }

        item {
            Text(text = "Recent Trades", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (history.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("No trade history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(history) { trade ->
                TradeHistoryCard(trade)
            }
        }
    }
}

@Composable
fun StatMetric(label: String, value: Double) {
    Column {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "${if (value >= 0) "+" else ""}฿${String.format(Locale.ENGLISH,"%.0f", value)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (value >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun TradeHistoryCard(trade: TradeEntity) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", LocalLocale.current.platformLocale)
    val dateStr = dateFormat.format(Date(trade.dateMillis))
    val isWin = trade.netProfitBaht > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = trade.symbol, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Bought @ ${trade.buyPrice}, Sold @ ${trade.sellPrice}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isWin) "+" else ""}฿${String.format(Locale.ENGLISH,"%.2f", trade.netProfitBaht)}",
                        fontWeight = FontWeight.Bold,
                        color = if (isWin) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "${String.format(Locale.ENGLISH,"%.2f", trade.netProfitPercent)}%",
                        fontSize = 12.sp,
                        color = if (isWin) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                    Text(text = "Qty: ${trade.quantity}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            if (trade.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "Lesson Learned:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = trade.note,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
