package apincer.mobile.tradings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
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

@OptIn(ExperimentalMaterial3Api::class)
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
        GlassDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = "Clear History",
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearTradeHistory()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete All", color = Color.White)
                }
            },
            dismissButton = {
                
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            
            Text("Are you sure you want to delete all trade history? This action cannot be undone.")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Performance", fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            actions = {
                if (history.isNotEmpty()) {
                    IconButton(onClick = { showConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
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
                                
                                Text("Beginning Cash", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                
                                Text("฿${String.format(Locale.ENGLISH,"%,.2f", beginningCash)}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                
                                Text("Win Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                
                                Text("${String.format(Locale.ENGLISH,"%.1f", winRate)}%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp).alpha(0.1f))
                        
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    
                    Text(text = "Trading Efficiency", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                
                                Text("Avg Win", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                
                                Text("฿${String.format(Locale.ENGLISH,"%,.0f", avgWin)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                
                                Text("Avg Loss", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                
                                Text("฿${String.format(Locale.ENGLISH,"%,.0f", avgLoss)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                
                                Text("Total Fees", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                
                                Text("฿${String.format(Locale.ENGLISH,"%,.0f", totalFees)}", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        
                        if (bestTrade != null) {
                            HorizontalDivider(modifier = Modifier.alpha(0.05f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                
                                Text("Best Performer: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                
                                Text(bestTrade.symbol, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.weight(1f))
                                
                                Text("+฿${String.format(Locale.ENGLISH,"%,.0f", bestTrade.netProfitBaht)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    
                    Text(text = "Recent Trades", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                }
            }

            if (history.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            
                            Text("No trade history yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(history) { trade ->
                    TradeHistoryCard(trade)
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
        
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        
        Text(
            text = "${if (value >= 0) "+" else ""}฿${String.format(Locale.ENGLISH,"%,.0f", value)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = if (value >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun TradeHistoryCard(trade: TradeEntity) {
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
                    
                    Text(text = dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    
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
                    
                    Text(text = "${trade.quantity} Shares", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
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
                        
                        Text(text = "LESSON LEARNED", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        
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
