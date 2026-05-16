package apincer.mobile.tradings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.data.StockEntity
import apincer.mobile.tradings.domain.TechnicalAnalysis
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: StockViewModel,
    onSelectStock: (String) -> Unit
) {
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val cashBalance by viewModel.cashBalance.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showBuyDialog by remember { mutableStateOf(false) }
    var showCashDialog by remember { mutableStateOf(false) }
    var selectedStockForSell by remember { mutableStateOf<StockWatchlistInfo?>(null) }
    var selectedStockForEdit by remember { mutableStateOf<StockWatchlistInfo?>(null) }

    val portfolioItems = watchlist.filter { it.portfolio.quantity > 0 }
    val stockValue = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
    val totalAssetValue = stockValue + cashBalance
    
    val totalCost = portfolioItems.sumOf { it.portfolio.cost * it.portfolio.quantity }
    val buyFees = TechnicalAnalysis.calculateFees(totalCost, false)
    val sellFees = TechnicalAnalysis.calculateFees(stockValue, true)
    val totalFees = buyFees + sellFees
    
    val grossProfit = stockValue - totalCost
    val netProfitValue = grossProfit - totalFees
    val totalNetProfitPercent = if (totalCost > 0) (netProfitValue / (totalCost + buyFees)) * 100 else 0.0

    val avgYieldOnCost = if (totalCost > 0) {
        portfolioItems.sumOf { (it.info.dividendYield ?: 0.0) * (it.info.lastPrice * it.portfolio.quantity) } / totalCost
    } else null

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Portfolio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            actions = {
                IconButton(onClick = { viewModel.refreshWatchlistInfo() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showBuyDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Buy Stock", modifier = Modifier.size(20.dp))
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PortfolioSummaryCard(
                    totalAssetValue = totalAssetValue,
                    stockValue = stockValue,
                    cashBalance = cashBalance,
                    grossProfit = grossProfit,
                    totalFees = totalFees,
                    netProfit = netProfitValue,
                    netPercent = totalNetProfitPercent,
                    yieldOnCost = avgYieldOnCost,
                    onEditCash = { showCashDialog = true }
                )
            }

            item {
                Row(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(text = "Active Holdings", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                }
            }

            if (portfolioItems.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Portfolio is empty", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(portfolioItems) { item ->
                    StockItemCard(
                        item = item,
                        onSelect = { onSelectStock(item.info.symbol) },
                        onDelete = { viewModel.removeFromWatchlist(item.info.symbol) },
                        onSell = { selectedStockForSell = item },
                        onEdit = { 
                            selectedStockForEdit = item
                            showBuyDialog = true
                        }
                    )
                }
            }
            
            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showBuyDialog) {
        BuyStockDialog(
            initialStock = selectedStockForEdit,
            onDismiss = { 
                showBuyDialog = false
                selectedStockForEdit = null
            },
            onConfirm = { symbol, cost, qty ->
                viewModel.addToWatchlist(symbol, cost, qty)
                showBuyDialog = false
                selectedStockForEdit = null
            }
        )
    }

    if (showCashDialog) {
        AdjustCashDialog(
            currentBalance = cashBalance,
            onDismiss = { showCashDialog = false },
            onConfirm = { amount, isSet ->
                if (isSet) viewModel.updateCashBalance(amount)
                else viewModel.adjustCash(amount)
                showCashDialog = false
            }
        )
    }

    selectedStockForSell?.let { stock ->
        SellStockDialog(
            stock = stock,
            onDismiss = { selectedStockForSell = null },
            onConfirm = { symbol, price, qty, note ->
                viewModel.recordSell(symbol, price, qty, note)
                selectedStockForSell = null
            }
        )
    }
}

@Composable
fun AdjustCashDialog(
    currentBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    
    GlassDialog(
        onDismissRequest = onDismiss,
        title = "Adjust Cash Balance",
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0, false) }) {
                    Text("Add Funds")
                }
                Button(
                    onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0, true) }, 
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Set Balance")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Current: ฿${String.format(Locale.ENGLISH, "%,.2f", currentBalance)}", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (THB)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
fun BuyStockDialog(
    initialStock: StockWatchlistInfo? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int) -> Unit
) {
    var symbol by remember { mutableStateOf(initialStock?.info?.symbol ?: "") }
    var entryPrice by remember { mutableStateOf(initialStock?.portfolio?.cost?.toString() ?: "") }
    var qty by remember { mutableStateOf(initialStock?.portfolio?.quantity?.toString() ?: "") }
    
    var targetPrice by remember { mutableStateOf("") }
    var stopLossPrice by remember { mutableStateOf("") }

    val entry = entryPrice.toDoubleOrNull() ?: 0.0
    val amount = qty.toIntOrNull() ?: 0
    val target = targetPrice.toDoubleOrNull() ?: 0.0
    val stopLoss = stopLossPrice.toDoubleOrNull() ?: 0.0

    val suggestion = if (entry > 0) {
        Triple(1.10, 0.95, "Suggested: 10% Target / 5% Stop (2.0 R:R)")
    } else null

    GlassDialog(
        onDismissRequest = onDismiss,
        title = if (initialStock == null) "Record Purchase" else "Edit Holding",
        confirmButton = {
            Button(
                onClick = { 
                    if (symbol.isNotBlank()) {
                        onConfirm(symbol, entry, amount)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (initialStock == null) "Add to Portfolio" else "Update")
            }
        },
        dismissButton = {
            
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase() },
                        label = { Text("Stock Symbol") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = initialStock == null,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = entryPrice,
                        onValueChange = { entryPrice = it },
                        label = { Text("Avg Cost per Share") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("฿ ") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Quantity") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Risk/Reward Calculator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    
                    suggestion?.let { sug ->
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                targetPrice = String.format(Locale.ENGLISH, "%.2f", entry * sug.first)
                                stopLossPrice = String.format(Locale.ENGLISH, "%.2f", entry * sug.second)
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.width(4.dp))
                                Text("Set Suggested", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
                
                suggestion?.let { sug ->
                    Text(text = sug.third, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetPrice,
                        onValueChange = { targetPrice = it },
                        label = { Text("Target") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        prefix = { Text("฿ ") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = stopLossPrice,
                        onValueChange = { stopLossPrice = it },
                        label = { Text("Stop Loss") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        prefix = { Text("฿ ") },
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            if (entry > 0 && amount > 0 && target > 0 && stopLoss > 0) {
                item {
                    val riskPerShare = entry - stopLoss
                    val rewardPerShare = target - entry
                    val totalRisk = riskPerShare * amount
                    val totalReward = rewardPerShare * amount
                    
                    val totalFees = TechnicalAnalysis.calculateFees(entry * amount, false) + TechnicalAnalysis.calculateFees(target * amount, true)
                    val netReward = totalReward - totalFees
                    val rrRatio = if (riskPerShare > 0) rewardPerShare / riskPerShare else 0.0

                    GlassCard(
                        containerColor = (if (rrRatio >= 2) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary).copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("R:R Ratio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("1 : ${String.format(Locale.ENGLISH, "%.2f", rrRatio)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (rrRatio >= 2) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                            }
                            LinearProgressIndicator(
                                progress = { (rrRatio / 5f).toFloat().coerceIn(0.1f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                color = if (rrRatio >= 2) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Potential Gain (Net)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("฿${String.format(Locale.ENGLISH, "%,.2f", netReward)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Potential Risk", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("฿${String.format(Locale.ENGLISH, "%,.2f", totalRisk)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellStockDialog(
    stock: StockWatchlistInfo,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String) -> Unit
) {
    var price by remember { mutableStateOf(stock.info.lastPrice.toString()) }
    var qty by remember { mutableStateOf(stock.portfolio.quantity.toString()) }
    var note by remember { mutableStateOf("") }

    GlassDialog(
        onDismissRequest = onDismiss,
        title = "Sell ${stock.info.symbol}",
        confirmButton = {
            Button(
                onClick = { 
                    val sellQty = qty.toIntOrNull() ?: 0
                    if (sellQty > 0 && sellQty <= stock.portfolio.quantity) {
                        onConfirm(stock.info.symbol, price.toDoubleOrNull() ?: 0.0, sellQty, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Sell", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Current holdings: ${stock.portfolio.quantity} shares", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Sell Price") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("฿ ") },
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it },
                label = { Text("Quantity to Sell") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Lessons Learned / Note") },
                placeholder = { Text("e.g. Sold too early, RSI was 80") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}
