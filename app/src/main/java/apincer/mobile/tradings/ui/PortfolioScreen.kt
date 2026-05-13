package apincer.mobile.tradings.ui

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TechnicalAnalysis
import java.util.Locale

@Composable
fun PortfolioScreen(viewModel: StockViewModel, onSelectStock: (String) -> Unit) {
    var showBuyDialog by remember { mutableStateOf(false) }
    var showCashDialog by remember { mutableStateOf(false) }
    var selectedStockForSell by remember { mutableStateOf<StockWatchlistInfo?>(null) }
    var selectedStockForEdit by remember { mutableStateOf<StockWatchlistInfo?>(null) }
    
    val watchlistInfo by viewModel.watchlistInfo.collectAsState()
    val cashBalance by viewModel.cashBalance.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val portfolioItems = watchlistInfo.filter { it.portfolio.quantity > 0 }
    
    val lastUpdated = watchlistInfo.map { it.info.lastUpdated }
        .filter { it.isNotEmpty() }
        .maxOrNull() ?: "N/A"

    // Asset Calculations
    val stockValue = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
    val totalAssetValue = stockValue + cashBalance
    val totalCostRaw = portfolioItems.sumOf { it.portfolio.cost * it.portfolio.quantity }
    
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

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(text = "My Portfolio", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Last update: $lastUpdated", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { 
                                selectedStockForEdit = null
                                showBuyDialog = true 
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Buy Stock", modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.refreshWatchlistInfo() },
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

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
                Text(text = "Holdings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (portfolioItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Your portfolio is empty. Click + to buy stocks!", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                if (isSet) {
                    viewModel.updateCashBalance(amount)
                } else {
                    viewModel.adjustCash(amount)
                }
                showCashDialog = false
            }
        )
    }

    selectedStockForSell?.let { item ->
        SellStockDialog(
            stock = item,
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
    var amountInput by remember { mutableStateOf("") }
    var isSetMode by remember { mutableStateOf(false) } 

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Manage Cash Balance") 
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text("฿${String.format(Locale.ENGLISH, "%,.2f", currentBalance)}", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }

                Column {
                    Text("Select Operation Mode:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    Surface(
                        onClick = { isSetMode = false },
                        color = if (!isSetMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent,
                        border = if (!isSetMode) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !isSetMode, onClick = { isSetMode = false })
                            Column {
                                Text("Deposit / Withdraw", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Add or remove funds from your wallet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        onClick = { isSetMode = true },
                        color = if (isSetMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent,
                        border = if (isSetMode) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSetMode, onClick = { isSetMode = true })
                            Column {
                                Text("Account Reconcile", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Set the exact balance to match your bank.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                TextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text(if (isSetMode) "New Exact Balance" else "Amount to Add/Subtract") },
                    placeholder = { Text("e.g. ${if (isSetMode) "50000" else "1000"}") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("฿ ") }
                )
                
                if (!isSetMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Use positive (+) for deposits and negative (-) for withdrawals.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    amountInput.toDoubleOrNull()?.let { amount ->
                        onConfirm(amount, isSetMode)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSetMode) "Update Balance" else "Confirm Transaction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    )
}

@Composable
fun BuyStockDialog(
    initialStock: StockWatchlistInfo? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String, Double, Int) -> Unit
) {
    var symbol by remember { mutableStateOf(initialStock?.info?.symbol ?: "") }
    var cost by remember { mutableStateOf(initialStock?.portfolio?.cost?.toString() ?: "") }
    var qty by remember { mutableStateOf(initialStock?.portfolio?.quantity?.toString() ?: "") }
    
    var targetPrice by remember { mutableStateOf("") }
    var stopLossPrice by remember { mutableStateOf("") }

    val entry = cost.toDoubleOrNull() ?: 0.0
    val amount = qty.toIntOrNull() ?: 0
    val target = targetPrice.toDoubleOrNull() ?: 0.0
    val stopLoss = stopLossPrice.toDoubleOrNull() ?: 0.0

    // Suggestion Logic
    val suggestion = remember(initialStock, entry) {
        if (entry <= 0) null else {
            val score = initialStock?.let {
                var s = 0
                if (it.info.isFundamentalGood) s += 2
                if ((it.info.dividendYield ?: 0.0) > 4.0) s += 1
                if (it.signal?.type == IndicatorSignal.BUY) s += 1
                s
            } ?: 1

            when {
                score >= 3 -> Triple(1.30, 0.90, "Quality stock with strong upside potential.")
                score >= 1 -> Triple(1.20, 0.90, "Standard swing trade for healthy growth.")
                else -> Triple(1.10, 0.93, "Weak fundamentals. Use a tight profit target.")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialStock == null) "Buy New Stock" else "Update Portfolio") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = symbol,
                            onValueChange = { symbol = it.uppercase() },
                            label = { Text("Stock Symbol") },
                            singleLine = true,
                            enabled = initialStock == null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = cost,
                            onValueChange = { cost = it },
                            label = { Text("Entry Price (Average Cost)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("฿ ") }
                        )
                        TextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = { Text("Quantity") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Risk/Reward Calculator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        suggestion?.let { sug ->
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.extraSmall,
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
                        TextField(
                            value = targetPrice,
                            onValueChange = { targetPrice = it },
                            label = { Text("Target Price") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            prefix = { Text("฿ ") }
                        )
                        TextField(
                            value = stopLossPrice,
                            onValueChange = { stopLossPrice = it },
                            label = { Text("Stop Loss") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            prefix = { Text("฿ ") }
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

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (rrRatio >= 2) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("R:R Ratio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("1 : ${String.format(Locale.ENGLISH, "%.2f", rrRatio)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (rrRatio >= 2) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                                }
                                LinearProgressIndicator(
                                    progress = { (rrRatio / 5f).toFloat().coerceIn(0.1f, 1f) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    color = if (rrRatio >= 2) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
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
                                if (rrRatio < 2) {
                                    Text(
                                        "Tip: Reward should be at least 2x your risk.",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            @Suppress("DEPRECATION")
            Button(
                onClick = { 
                    if (symbol.isNotBlank()) {
                        onConfirm(symbol, entry, amount)
                    }
                }
            ) {
                Text(if (initialStock == null) "Add to Portfolio" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sell ${stock.info.symbol}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current holdings: ${stock.portfolio.quantity} shares", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Sell Price") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("฿ ") }
                )
                TextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Quantity to Sell") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Lessons Learned / Note") },
                    placeholder = { Text("e.g. Sold too early, RSI was 80") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val sellQty = qty.toIntOrNull() ?: 0
                    if (sellQty > 0 && sellQty <= stock.portfolio.quantity) {
                        onConfirm(stock.info.symbol, price.toDoubleOrNull() ?: 0.0, sellQty, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirm Sell", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
