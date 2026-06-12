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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R
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
    val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"

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

    val dividendItems = portfolioItems.filter { (it.info.dividendYield ?: 0.0) > 0.0 }
    val totalYearlyDividend = dividendItems.sumOf { 
        it.portfolio.quantity * it.info.lastPrice * ((it.info.dividendYield ?: 0.0) / 100.0)
    }

    val targetMonthlyDividend by viewModel.targetMonthlyDividend.collectAsState()
    val targetYearlyDividend = targetMonthlyDividend * 12
    val dividendProgress = if (targetYearlyDividend > 0) (totalYearlyDividend / targetYearlyDividend).toFloat().coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.title_portfolio), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    if (lastSync != "---") {
                        Text(
                            text = stringResource(R.string.label_last_sync, lastSync),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                IconButton(onClick = { viewModel.refreshWatchlistInfo() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.desc_refresh), modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { showBuyDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_buy_stock), modifier = Modifier.size(24.dp))
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

            if (targetYearlyDividend > 0) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Dividend Snowball Goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "฿${String.format(Locale.ENGLISH, "%,.0f", totalYearlyDividend)} / ฿${String.format(Locale.ENGLISH, "%,.0f", targetYearlyDividend)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "${String.format(Locale.ENGLISH, "%.1f", dividendProgress * 100)}%",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { dividendProgress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
            }

            if (totalYearlyDividend > 0) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.section_dividend_estimation),
                        icon = Icons.Default.AutoAwesome,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.label_est_yearly_dividend),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "฿${String.format(Locale.ENGLISH, "%,.2f", totalYearlyDividend)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    val yield = if (stockValue > 0) (totalYearlyDividend / stockValue) * 100 else 0.0
                                    Text(
                                        text = String.format(Locale.ENGLISH, "%.2f%%", yield),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.label_dividend_symbols),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(4.dp))
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                dividendItems.forEach { item ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = item.info.symbol,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.section_active_holdings), 
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            if (portfolioItems.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.label_portfolio_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
            onConfirm = { symbol, cost, qty, target, stopLoss, note, purpose ->
                viewModel.addToWatchlist(symbol, cost, qty, purpose)
                if (target > 0) {
                    viewModel.addToFocusList(symbol, cost, target)
                }
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
        title = stringResource(R.string.title_adjust_cash),
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0, false) }) {
                    Text(stringResource(R.string.action_add_funds))
                }
                Button(
                    onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0, true) }, 
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_set_balance))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.label_current_balance, String.format(Locale.ENGLISH, "%,.2f", currentBalance)), style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.label_amount_thb)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyStockDialog(
    initialStock: StockWatchlistInfo? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, Double, Double, String, String) -> Unit
) {
    var symbol by remember { mutableStateOf(initialStock?.info?.symbol ?: "") }
    var entryPrice by remember { mutableStateOf(initialStock?.portfolio?.cost?.toString() ?: "") }
    var qty by remember { mutableStateOf(initialStock?.portfolio?.quantity?.toString() ?: "") }
    
    var targetPrice by remember { mutableStateOf("") }
    var stopLossPrice by remember { mutableStateOf("") }
    var playbookNote by remember { mutableStateOf("") }
    var tradePurpose by remember { mutableStateOf(initialStock?.portfolio?.tradePurpose ?: "SWING") }

    val entry = entryPrice.toDoubleOrNull() ?: 0.0
    val amount = qty.toIntOrNull() ?: 0
    val target = targetPrice.toDoubleOrNull() ?: 0.0
    val stopLoss = stopLossPrice.toDoubleOrNull() ?: 0.0

    val suggestion = if (entry > 0) {
        Triple(1.10, 0.95, stringResource(R.string.label_suggested_strategy))
    } else null

    val riskPerShare = entry - stopLoss
    val rewardPerShare = target - entry
    val rrRatio = if (riskPerShare > 0) rewardPerShare / riskPerShare else 0.0
    val isValidDividend = tradePurpose == "DIVIDEND" || playbookNote.lowercase().contains("dividend")
    val isFormValid = symbol.isNotBlank() && entry > 0 && amount > 0 && 
                      (isValidDividend || (target > 0 && stopLoss > 0 && rrRatio >= 2.0))

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                if (initialStock == null) stringResource(R.string.title_record_purchase) else stringResource(R.string.title_edit_holding),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase() },
                        label = { Text(stringResource(R.string.label_stock_symbol)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = initialStock == null,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Text("Trade Purpose", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { tradePurpose = "SWING" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tradePurpose == "SWING") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (tradePurpose == "SWING") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Swing Trade")
                        }
                        Button(
                            onClick = { tradePurpose = "DIVIDEND" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tradePurpose == "DIVIDEND") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (tradePurpose == "DIVIDEND") MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Dividend")
                        }
                    }
                    OutlinedTextField(
                        value = entryPrice,
                        onValueChange = { entryPrice = it },
                        label = { Text(stringResource(R.string.label_avg_cost)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("฿ ") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text(stringResource(R.string.label_quantity)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = playbookNote,
                        onValueChange = { playbookNote = it },
                        label = { Text("Playbook / Setup Reasoning") },
                        placeholder = { Text("e.g. Swing Breakout, Dividend") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        minLines = 2
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.label_rr_calculator), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                Text(stringResource(R.string.action_set_suggested), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
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
                        label = { Text(stringResource(R.string.label_target)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        prefix = { Text("฿ ") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = stopLossPrice,
                        onValueChange = { stopLossPrice = it },
                        label = { Text(stringResource(R.string.label_stop_loss)) },
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
                    val totalRisk = riskPerShare * amount
                    val totalReward = rewardPerShare * amount
                    
                    val totalFees = TechnicalAnalysis.calculateFees(entry * amount, false) + TechnicalAnalysis.calculateFees(target * amount, true)
                    val netReward = totalReward - totalFees

                    GlassCard(
                        containerColor = (if (rrRatio >= 2) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary).copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.label_rr_ratio), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    Text(stringResource(R.string.label_potential_gain), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("฿${String.format(Locale.ENGLISH, "%,.2f", netReward)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(stringResource(R.string.label_potential_risk), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("฿${String.format(Locale.ENGLISH, "%,.2f", totalRisk)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = isFormValid,
                    onClick = { 
                        if (isFormValid) {
                            onConfirm(symbol, entry, amount, target, stopLoss, playbookNote, tradePurpose)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (initialStock == null) stringResource(R.string.action_add_to_portfolio) else stringResource(R.string.action_update))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellStockDialog(
    stock: StockWatchlistInfo,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String) -> Unit
) {
    var price by remember { mutableStateOf(stock.info.lastPrice.toString()) }
    var qty by remember { mutableStateOf(stock.portfolio.quantity.toString()) }
    var note by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                stringResource(R.string.title_sell_stock, stock.info.symbol),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.label_current_holdings_count, stock.portfolio.quantity), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(stringResource(R.string.label_sell_price)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("฿ ") },
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it },
                label = { Text(stringResource(R.string.label_quantity_to_sell)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.label_sell_note)) },
                placeholder = { Text(stringResource(R.string.hint_sell_note)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
        
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Spacer(Modifier.width(8.dp))
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
                    Text(stringResource(R.string.action_confirm_sell), color = Color.White)
                }
            }
        }
    }
}
