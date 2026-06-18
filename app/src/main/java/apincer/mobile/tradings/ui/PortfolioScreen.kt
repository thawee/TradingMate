package apincer.mobile.tradings.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import apincer.mobile.tradings.data.StockAggregate
import apincer.mobile.tradings.domain.TechnicalAnalysis
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: StockViewModel,
    settingsViewModel: SettingsViewModel,
    portfolioViewModel: PortfolioViewModel = viewModel(),
    onSelectStock: (String) -> Unit,
    showSnackbar: (String) -> Unit,
    scrollSymbol: String? = null
) {
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val cashBalance by portfolioViewModel.cashBalance.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isPrivacyMode by settingsViewModel.isPrivacyMode.collectAsState()
    val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"

    var showBuyDialog by remember { mutableStateOf(false) }
    var showCashDialog by remember { mutableStateOf(false) }
    var selectedStockForSell by remember { mutableStateOf<StockWatchlistInfo?>(null) }
    var selectedStockForEdit by remember { mutableStateOf<StockWatchlistInfo?>(null) }

    val portfolioItems = watchlist.filter { it.portfolio.quantity > 0 }
    val stockValue = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
    val totalAssetValue = stockValue + cashBalance
    
    val totalCost = portfolioItems.sumOf { it.portfolio.cost * it.portfolio.quantity }
    val buyFees = portfolioItems.sumOf { item ->
        if (item.portfolio.buyFees > 0.0) {
            item.portfolio.buyFees
        } else {
            TechnicalAnalysis.calculateFees(item.portfolio.cost * item.portfolio.quantity, false)
        }
    }
    val sellFees = TechnicalAnalysis.calculateFees(stockValue, true)
    val totalFees = buyFees + sellFees
    
    val grossProfit = stockValue - totalCost
    val netProfitValue = grossProfit - totalFees
    val totalNetProfitPercent = if (totalCost > 0) (netProfitValue / (totalCost + buyFees)) * 100 else 0.0

    val avgYieldOnCost = if (totalCost > 0) {
        portfolioItems.sumOf { item ->
            val dps = item.portfolio.dividendPerShare ?: if (item.info.dividendYield != null && item.info.lastPrice != 0.0) {
                item.info.lastPrice * (item.info.dividendYield / 100.0)
            } else 0.0
            dps * item.portfolio.quantity
        } / totalCost * 100.0
    } else null

    val dividendItems = portfolioItems.filter { (it.portfolio.dividendPerShare ?: 0.0) > 0.0 || (it.info.dividendYield ?: 0.0) > 0.0 }
    val totalYearlyDividend = dividendItems.sumOf { item ->
        val dps = item.portfolio.dividendPerShare ?: if (item.info.dividendYield != null && item.info.lastPrice != 0.0) {
            item.info.lastPrice * (item.info.dividendYield / 100.0)
        } else 0.0
        item.portfolio.quantity * dps
    }

    val targetMonthlyDividend by settingsViewModel.targetMonthlyDividend.collectAsState()
    val targetYearlyDividend = targetMonthlyDividend * 12
    val dividendProgress = if (targetYearlyDividend > 0) (totalYearlyDividend / targetYearlyDividend).toFloat().coerceIn(0f, 1f) else 0f

    val listState = rememberLazyListState()
    LaunchedEffect(scrollSymbol, portfolioItems) {
        scrollSymbol?.let { symbol ->
            val index = portfolioItems.indexOfFirst { it.info.symbol == symbol }
            if (index >= 0) {
                listState.animateScrollToItem(index + 2)
            }
        }
    }

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
                IconButton(onClick = { showBuyDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_buy_stock), modifier = Modifier.size(24.dp))
                }
            }
        )
        
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshPortfolioOnly() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
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
                    isPrivacyMode = isPrivacyMode,
                    onEditCash = { showCashDialog = true }
                )
            }

            if (portfolioItems.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Portfolio Allocation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            val chartColors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.error,
                                androidx.compose.ui.graphics.Color(0xFFFFA000),
                                androidx.compose.ui.graphics.Color(0xFF00B0FF),
                                androidx.compose.ui.graphics.Color(0xFFE040FB),
                                androidx.compose.ui.graphics.Color(0xFF1DE9B6)
                            )
                            
                            val values = portfolioItems.map { (it.info.lastPrice * it.portfolio.quantity).toFloat() }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Chart on the left
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    DonutChart(
                                        values = values,
                                        colors = chartColors,
                                        centerText = "฿${String.format(Locale.ENGLISH, "%,.0f", stockValue)}",
                                        centerSubText = "Total Equity"
                                    )
                                }
                                
                                Spacer(Modifier.width(16.dp))
                                
                                // Legend on the right
                                Column(modifier = Modifier.weight(1f)) {
                                    portfolioItems.forEachIndexed { index, item ->
                                        val pct = if (stockValue > 0) (item.info.lastPrice * item.portfolio.quantity / stockValue) * 100 else 0.0
                                        if (pct > 0.1) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(
                                                            color = chartColors[index % chartColors.size],
                                                            shape = androidx.compose.foundation.shape.CircleShape
                                                        )
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = item.info.symbol,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = String.format(Locale.ENGLISH, "%.1f%%", pct),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
                    subtitle = "${portfolioItems.size} stocks • Synced $lastSync",
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            if (portfolioItems.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.label_portfolio_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(
                                onClick = { showBuyDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_add_to_portfolio))
                            }
                        }
                    }
                }
            } else {
                items(portfolioItems, key = { it.info.symbol }) { item ->
                    StockItemCard(
                        item = item,
                        onSelect = { onSelectStock(item.info.symbol) },
                        onDelete = { viewModel.removeFromWatchlist(item.info.symbol) },
                        onSell = { selectedStockForSell = item },
                        onEdit = { 
                            selectedStockForEdit = item
                            showBuyDialog = true
                        },
                        isPrivacyMode = isPrivacyMode
                    )
                }
            }
            
            item {
                Spacer(Modifier.height(40.dp))
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
            onConfirm = { symbol, cost, qty, target, stopLoss, note, purpose ->
                viewModel.addToWatchlist(symbol, cost, qty, purpose, stopLoss, note)
                if (purpose == "SWING" && target > 0) {
                    viewModel.addToFocusList(symbol, cost, target)
                } else {
                    viewModel.removeFromFocusList(symbol)
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
                if (isSet) portfolioViewModel.updateCashBalance(amount)
                else portfolioViewModel.adjustCash(amount)
                showCashDialog = false
            }
        )
    }

    selectedStockForSell?.let { stock ->
        SellStockDialog(
            stock = stock,
            onDismiss = { selectedStockForSell = null },
            onConfirm = { symbol, price, qty, note ->
                portfolioViewModel.recordSell(stock, price, qty, note)
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
            val amountVal = amount.toDoubleOrNull() ?: 0.0
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = amountVal > 0.0,
                    onClick = { onConfirm(amountVal, false) }
                ) {
                    Text(stringResource(R.string.action_add_funds))
                }
                Button(
                    enabled = amountVal >= 0.0 && amount.isNotBlank(),
                    onClick = { onConfirm(amountVal, true) }, 
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
    
    var targetPrice by remember { mutableStateOf(initialStock?.focusTargetPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var stopLossPrice by remember { mutableStateOf(initialStock?.portfolio?.stopLoss?.let { if (it > 0) it.toString() else "" } ?: "") }
    var playbookNote by remember { mutableStateOf(initialStock?.portfolio?.playbookNote ?: "") }
    var tradePurpose by remember { mutableStateOf(initialStock?.portfolio?.tradePurpose ?: "SWING") }
    var acceptLowRR by remember { mutableStateOf(false) }

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
                      (isValidDividend || (target > 0 && stopLoss > 0 && (rrRatio >= 2.0 || acceptLowRR)))

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
                                Text(stringResource(R.string.action_set_suggested), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
                
                suggestion?.let { sug ->
                    Text(text = sug.third, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
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
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(stringResource(R.string.label_potential_gain), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("฿${String.format(Locale.ENGLISH, "%,.2f", netReward)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(stringResource(R.string.label_potential_risk), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("฿${String.format(Locale.ENGLISH, "%,.2f", totalRisk)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                if (tradePurpose == "SWING" && rrRatio < 2.0) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = acceptLowRR,
                                onCheckedChange = { acceptLowRR = it }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Override: Accept low Risk/Reward ratio (< 2.0)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
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
    var qty by remember { mutableStateOf("") }
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
                val sellQty = qty.toIntOrNull() ?: 0
                val sellPrice = price.toDoubleOrNull() ?: 0.0
                val isValid = sellQty > 0 && sellQty <= stock.portfolio.quantity && sellPrice > 0.0
                Button(
                    enabled = isValid,
                    onClick = { 
                        if (isValid) {
                            onConfirm(stock.info.symbol, sellPrice, sellQty, note)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_confirm_sell), color = Color.White)
                }
            }
        }
    }
}
