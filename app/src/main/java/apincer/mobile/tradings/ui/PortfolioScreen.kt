package apincer.mobile.tradings.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import apincer.mobile.tradings.domain.GeminiClient
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.CircularProgressIndicator
import apincer.mobile.tradings.data.PreferenceRepository
import kotlinx.coroutines.flow.first

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import apincer.mobile.tradings.R
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
    scrollSymbol: String? = null,
    /** From the monthly DCA notification: open the Buy dialog prefilled with the core purchase. */
    dcaBuyRequest: Triple<String, Double, Int>? = null,
    onDcaBuyRequestConsumed: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val cashBalance by portfolioViewModel.cashBalance.collectAsState()
    val cashTransactions by portfolioViewModel.allCashTransactions.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isAtsEnabled by settingsViewModel.isAtsEnabled.collectAsState()
    val maxRiskPerTrade by settingsViewModel.maxRiskPerTrade.collectAsState()
    val minRiskRewardRatio by settingsViewModel.minRiskRewardRatio.collectAsState()
    val maxPortfolioAllocation by settingsViewModel.maxPortfolioAllocation.collectAsState()
    val maxSectorAllocation by settingsViewModel.maxSectorAllocation.collectAsState()
    val citTaxRate by settingsViewModel.citTaxRate.collectAsState()
    val personalTaxRate by settingsViewModel.personalTaxRate.collectAsState()
    val targetCorePercent by settingsViewModel.targetCorePercent.collectAsState()
    val marketRegime by viewModel.marketRegime.collectAsState()
    val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"

    var showBuyDialog by remember { mutableStateOf(false) }
    var buyPrefill by remember { mutableStateOf<Triple<String, Double, Int>?>(null) }
    val corePrice by portfolioViewModel.corePrice.collectAsState()
    val stopAcks by viewModel.stopAcks.collectAsState()
    val monthlyDcaAmount by settingsViewModel.monthlyDcaAmount.collectAsState()
    val dcaDay by settingsViewModel.dcaDayOfMonth.collectAsState()
    LaunchedEffect(Unit) { portfolioViewModel.refreshCorePrice() }
    var showCashDialog by remember { mutableStateOf(false) }
    var showDividendDialog by remember { mutableStateOf(false) }
    var pendingToRecord by remember { mutableStateOf<apincer.mobile.tradings.domain.PendingDividends.Pending?>(null) }
    var selectedStockForSell by remember { mutableStateOf<StockWatchlistInfo?>(null) }
    var selectedStockForEdit by remember { mutableStateOf<StockWatchlistInfo?>(null) }
    LaunchedEffect(dcaBuyRequest) {
        if (dcaBuyRequest != null) {
            selectedStockForEdit = null
            buyPrefill = dcaBuyRequest
            showBuyDialog = true
            onDcaBuyRequestConsumed() // so returning to Portfolio later does not reopen it
        }
    }
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedPlaybook by remember { mutableStateOf("SWING") }

    val dividendHistory by portfolioViewModel.dividendHistory.collectAsState()
    val totalDividendEarned = dividendHistory.sumOf { it.totalReceived }

    val allPortfolioItems = watchlist.filter { it.portfolio.quantity > 0 }
    val pendingDividends by portfolioViewModel.pendingDividends.collectAsState()
    val heldKey = allPortfolioItems.map { it.info.symbol to it.portfolio.quantity }.sortedBy { it.first }
    LaunchedEffect(heldKey) { if (heldKey.isNotEmpty()) portfolioViewModel.refreshPendingDividends(allPortfolioItems) }
    // Account equity for position sizing = cash on hand + current market value of holdings.
    val accountEquity = cashBalance + allPortfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
    val portfolioItems = when (selectedPlaybook) {
       // "SWING" -> allPortfolioItems.filter { it.portfolio.tradePurpose == "SWING" }
        "DIVIDEND" -> allPortfolioItems.filter { it.portfolio.tradePurpose == "DIVIDEND" }
        else -> allPortfolioItems
    }

    // Always compute total asset value from ALL holdings — not the filtered tab view
    val totalStockValue = allPortfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
    val totalAssetValue = totalStockValue + cashBalance

    val netPrincipal = cashTransactions.filter { it.type != "Fee" && it.type != "Dividend" }.sumOf { it.amount }
   // val lifetimeReturn = if (netPrincipal != 0.0) totalAssetValue - netPrincipal else 0.0
   // val lifetimeReturnPercent = if (netPrincipal > 0) (lifetimeReturn / netPrincipal) * 100 else 0.0

    // Per-tab breakdown (profit/fees/yield shown for the selected filter)
    val stockValue = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }

    val totalCost = portfolioItems.sumOf { it.portfolio.cost * it.portfolio.quantity }
    val buyFees = portfolioItems.sumOf { item ->
        if (item.portfolio.buyFees > 0.0) {
            item.portfolio.buyFees
        } else {
            TechnicalAnalysis.calculateFees(item.portfolio.cost * item.portfolio.quantity, false, isAtsEnabled)
        }
    }
    val sellFees = TechnicalAnalysis.calculateFees(stockValue, true, isAtsEnabled)
    val totalFees = buyFees + sellFees

    val grossProfit = stockValue - totalCost
    val netProfitValue = grossProfit - totalFees
    val totalNetProfitPercent = if (totalCost > 0) (netProfitValue / (totalCost + buyFees)) * 100 else 0.0

    // Only show yield on DIVIDEND tab — meaningless average across swing stocks
    val avgYieldOnCost = if (selectedPlaybook == "DIVIDEND" && totalCost > 0) {
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
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(onClick = {
                    apincer.mobile.tradings.utils.CsvExporter.exportHoldingsToCsv(context, allPortfolioItems)
                }) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Export CSV",
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { showBuyDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_buy_stock), modifier = Modifier.size(24.dp))
                }
            }
        )
        if (lastSync != "---") {
            Text(
                text = stringResource(R.string.label_last_sync, lastSync),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        GlassSegmentedControl(
            items = listOf("SWING", "DIVIDEND"),
            selectedItem = selectedPlaybook,
            onItemSelect = { selectedPlaybook = it },
            labelExtractor = { it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.refreshPortfolioOnly() 
            },
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
                    totalDividendEarned = totalDividendEarned,
                    profitScopeLabel = if (selectedPlaybook == "SWING") null else selectedPlaybook,
                    onEditCash = { showCashDialog = true },
                    onLogDividend = { showDividendDialog = true }
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
                                text = "Portfolio Snapshot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            val chartColors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.error,
                                androidx.compose.ui.graphics.Color(0xFFFFA000),
                                androidx.compose.ui.graphics.Color(0xFF00B0FF),
                                androidx.compose.ui.graphics.Color(0xFFE040FB),
                                androidx.compose.ui.graphics.Color(0xFF1DE9B6)
                            )
                            val isMacro = selectedPlaybook == "SWING"
                            val chartValues = if (isMacro) {
                                val swingVal = allPortfolioItems.filter { it.portfolio.tradePurpose == "SWING" }.sumOf { it.info.lastPrice * it.portfolio.quantity }.toFloat()
                                val divVal = allPortfolioItems.filter { it.portfolio.tradePurpose == "DIVIDEND" }.sumOf { it.info.lastPrice * it.portfolio.quantity }.toFloat()
                                listOf(swingVal, divVal, cashBalance.toFloat())
                            } else {
                                portfolioItems.map { (it.info.lastPrice * it.portfolio.quantity).toFloat() }
                            }
                            val centerAmount = if (isMacro) totalAssetValue else stockValue
                            val centerTextStr = "฿${String.format(java.util.Locale.ENGLISH, "%,.0f", centerAmount)}"

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                // Donut chart
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    DonutChart(
                                        values = chartValues,
                                        colors = chartColors,
                                        centerText = centerTextStr,
                                        centerSubText = if (isMacro) "Total Assets" else "Portfolio"
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                // Legend + key metrics
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (isMacro) {
                                        val total = chartValues.sum()
                                        listOf("Swing", "Dividend", "Cash").forEachIndexed { i, label ->
                                            val pct = if (total > 0f) (chartValues[i] / total) * 100.0 else 0.0
                                            if (pct > 0.1) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(chartColors[i], CircleShape)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = String.format(java.util.Locale.ENGLISH, "%.1f%%", pct),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        portfolioItems.forEachIndexed { i, item ->
                                            val pct = if (stockValue > 0) (item.info.lastPrice * item.portfolio.quantity / stockValue) * 100 else 0.0
                                            if (pct > 0.1) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(chartColors[i % chartColors.size], CircleShape)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(item.info.symbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = String.format(java.util.Locale.ENGLISH, "%.1f%%", pct),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (targetYearlyDividend > 0 && selectedPlaybook != "SWING") {
                                Spacer(Modifier.height(24.dp))
                                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                                Spacer(Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Dividend Snowball Goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "฿${String.format(java.util.Locale.ENGLISH, "%,.0f", totalYearlyDividend)} / ฿${String.format(java.util.Locale.ENGLISH, "%,.0f", targetYearlyDividend)}",
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
                                            "${String.format(java.util.Locale.ENGLISH, "%.1f", dividendProgress * 100)}%",
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

                if (pendingDividends.isNotEmpty()) item {
                    PendingDividendsCard(
                        pending = pendingDividends,
                        onRecord = { pendingToRecord = it; showDividendDialog = true },
                        onDismiss = { portfolioViewModel.dismissPendingDividend(it) }
                    )
                }

                item {
                    HoldingsSummaryTable(
                        items = portfolioItems
                    )
                }

                item {
                    CoreSatelliteCard(
                        portfolioItems = allPortfolioItems,
                        targetCorePercent = targetCorePercent,
                        monthlyDcaAmount = monthlyDcaAmount,
                        dcaDay = dcaDay,
                        corePrice = corePrice,
                        atsEnabled = isAtsEnabled,
                        onSetupCore = { amount, day ->
                            settingsViewModel.updateMonthlyDcaAmount(amount)
                            settingsViewModel.updateDcaDayOfMonth(day)
                            showSnackbar("Monthly core DCA set: ฿${String.format(java.util.Locale.ENGLISH, "%,.0f", amount)} on day $day")
                        },
                        onBuyCore = { price, shares ->
                            selectedStockForEdit = null
                            buyPrefill = Triple(apincer.mobile.tradings.domain.CoreSatellite.CORE_SYMBOL, price, shares)
                            showBuyDialog = true
                        }
                    )
                }

                item {
                    SectorBreakdownCard(
                        portfolioItems = allPortfolioItems,
                        cashBalance = cashBalance,
                        maxSectorPercent = maxSectorAllocation
                    )
                }

                if (selectedPlaybook == "DIVIDEND" && dividendHistory.isNotEmpty()) {
                    item {
                        DividendTaxShieldCard(
                            dividendHistory = dividendHistory,
                            avgYieldOnCost = avgYieldOnCost,
                            citRate = citTaxRate,
                            personalTaxRate = personalTaxRate
                        )
                    }
                }
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
                        stopAckLevel = stopAcks[item.info.symbol.uppercase()],
                        onStopDecision = { stock, move, newStop, reason ->
                            viewModel.recordStopDecision(stock, move, newStop, reason) { result ->
                                result.onSuccess { showSnackbar(it) }
                                    .onFailure { showSnackbar(it.message ?: "Could not save the decision") }
                            }
                        }
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
        val isEditing = selectedStockForEdit != null
        BuyStockDialog(
            initialStock = selectedStockForEdit,
            accountEquity = accountEquity,
            cashBalance = cashBalance,
            marketRegime = marketRegime,
            maxRiskPerTradePercent = maxRiskPerTrade,
            minRiskRewardRatio = minRiskRewardRatio,
            maxStockAllocationPercent = maxPortfolioAllocation,
            maxSectorAllocationPercent = maxSectorAllocation,
            holdings = watchlist,
            targetCorePercent = targetCorePercent,
            atsEnabled = isAtsEnabled,
            isSaving = isSubmitting,
            prefill = buyPrefill,
            onDismiss = {
                if (!isSubmitting) {
                    showBuyDialog = false
                    selectedStockForEdit = null
                    buyPrefill = null
                }
            },
            onConfirm = { symbol, cost, qty, target, stopLoss, note, purpose, recordExecutedFill ->
                if (!isSubmitting) {
                    isSubmitting = true
                    viewModel.addToWatchlist(symbol, cost, qty, purpose, stopLoss, note,
                        isEdit = isEditing, targetPrice = target,
                        recordExecutedFill = recordExecutedFill, onResult = { result ->
                            isSubmitting = false
                            result.onSuccess {
                                showBuyDialog = false
                                selectedStockForEdit = null
                                buyPrefill = null
                            }.onFailure { showSnackbar(it.message ?: "Could not save holding") }
                        })
                }
            }
        )
    }

    if (showCashDialog) {
        AdjustCashDialog(
            currentBalance = cashBalance,
            isSaving = isSubmitting,
            onDismiss = { if (!isSubmitting) showCashDialog = false },
            onConfirm = { amount, isSet, reason ->
                if (!isSubmitting) {
                    isSubmitting = true
                    val onResult: (Result<Unit>) -> Unit = { result ->
                        isSubmitting = false
                        result.onSuccess {
                            showCashDialog = false
                            showSnackbar(if (isSet) "Cash set to ฿${String.format(Locale.ENGLISH, "%,.2f", amount)} · $reason"
                                else "Cash ${if (amount >= 0) "+" else ""}฿${String.format(Locale.ENGLISH, "%,.2f", amount)} · $reason")
                        }.onFailure { showSnackbar(it.message ?: "Could not update cash") }
                    }
                    if (isSet) portfolioViewModel.updateCashBalance(amount, reason, onResult)
                    else portfolioViewModel.adjustCash(amount, reason, onResult)
                }
            }
        )
    }

    if (showDividendDialog) {
        LogDividendDialog(
            isSaving = isSubmitting,
            initial = pendingToRecord,
            onDismiss = { if (!isSubmitting) { showDividendDialog = false; pendingToRecord = null } },
            onConfirm = { symbol, dateMillis, dps, shares, tax ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!isSubmitting) {
                    isSubmitting = true
                    portfolioViewModel.logDividend(symbol, dateMillis, dps, shares, tax) { result ->
                        isSubmitting = false
                        result.onSuccess {
                            showDividendDialog = false
                            pendingToRecord = null
                            portfolioViewModel.refreshPendingDividends(watchlist)
                            showSnackbar("Logged dividend for $symbol")
                        }.onFailure { showSnackbar(it.message ?: "Could not log dividend") }
                    }
                }
            }
        )
    }

    selectedStockForSell?.let { stock ->
        SellStockDialog(
            stock = stock,
            isSaving = isSubmitting,
            onDismiss = { if (!isSubmitting) selectedStockForSell = null },
            onConfirm = { symbol, price, qty, note ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!isSubmitting) {
                    isSubmitting = true
                    portfolioViewModel.recordSell(stock, price, qty, note) { result ->
                        isSubmitting = false
                        result.onSuccess {
                            selectedStockForSell = null
                            showSnackbar("Recorded sale of $symbol")
                        }.onFailure { showSnackbar(it.message ?: "Could not record sale") }
                    }
                }
            }
        )
    }
}

@Composable
fun LogDividendDialog(
    initialSymbol: String = "",
    isSaving: Boolean = false,
    /** Prefill from a pending payout: baht per share, shares, withholding tax. */
    initial: apincer.mobile.tradings.domain.PendingDividends.Pending? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Double, Int, Double) -> Unit
) {
    var symbol by remember { mutableStateOf(initial?.symbol ?: initialSymbol) }
    var dps by remember { mutableStateOf(initial?.perShare?.toString() ?: "") }
    var shares by remember { mutableStateOf(initial?.shares?.toString() ?: "") }
    var taxDeducted by remember { mutableStateOf(initial?.let { String.format(java.util.Locale.ENGLISH, "%.2f", it.withholding) } ?: "0.0") }
    
    val dpsVal = dps.toDoubleOrNull() ?: 0.0
    val sharesVal = shares.toIntOrNull() ?: 0
    val taxVal = taxDeducted.toDoubleOrNull() ?: 0.0
    
    val totalAmount = (dpsVal * sharesVal) - taxVal

    val isValid = symbol.isNotBlank() && dpsVal.isFinite() && dpsVal > 0.0 && sharesVal > 0 &&
        taxDeducted.toDoubleOrNull() != null && taxVal.isFinite() && taxVal >= 0.0 &&
        totalAmount.isFinite() && totalAmount >= 0.0

    GlassDialog(
        onDismissRequest = onDismiss,
        title = "Log Dividend Payment",
        confirmButton = {
            Button(
                enabled = isValid && !isSaving,
                onClick = { onConfirm(symbol, System.currentTimeMillis(), dpsVal, sharesVal, taxVal) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it.uppercase() },
                label = { Text("Symbol") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = dps,
                onValueChange = { dps = it },
                label = { Text("Dividend Per Share (฿)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = shares,
                onValueChange = { shares = it },
                label = { Text("Shares Held") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = taxDeducted,
                onValueChange = { taxDeducted = it },
                label = { Text("Tax Deducted (฿)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(14.dp)
            )
            if (isValid) {
                Text(
                    text = "Net Received: ฿${String.format(Locale.ENGLISH, "%,.2f", totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "This amount will be added to your cash balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdjustCashDialog(
    currentBalance: Double,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean, String) -> Unit
) {
    // Mode: false = Adjust (±), true = Set Exact
    var isSetMode by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("") }
    var customReason by remember { mutableStateOf("") }

    val presetReasons = listOf("Deposit", "Withdrawal", "Correction", "Fee")
    val effectiveReason = customReason.takeIf { it.isNotBlank() } ?: selectedReason
    val amountVal = amount.toDoubleOrNull() ?: 0.0
    val isValid = amount.toDoubleOrNull() != null && amountVal.isFinite() && effectiveReason.isNotBlank() &&
        (if (isSetMode) amountVal >= 0.0 else amountVal != 0.0 && currentBalance + amountVal >= 0.0)

    GlassDialog(
        onDismissRequest = onDismiss,
        title = "Cash Management",
        confirmButton = {
            Button(
                enabled = isValid && !isSaving,
                onClick = { onConfirm(amountVal, isSetMode, effectiveReason) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isSetMode) "Set Balance" else "Adjust Cash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Current balance display
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "฿${String.format(Locale.ENGLISH, "%,.2f", currentBalance)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Mode toggle: Adjust ± vs Set Exact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(false to "Adjust ±", true to "Set Exact").forEach { (mode, label) ->
                    Button(
                        onClick = { isSetMode = mode },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSetMode == mode)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSetMode == mode)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Helper text under mode toggle
            Text(
                text = if (isSetMode)
                    "Set cash to an exact amount (e.g. after reconciling with broker)"
                else
                    "Add or remove from current balance (positive = add)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Amount field
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(if (isSetMode) "New Balance (฿)" else "Amount (฿)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                prefix = { Text("฿ ") },
                supportingText = if (isSetMode && amountVal > 0.0) {{
                    val diff = amountVal - currentBalance
                    val sign = if (diff >= 0) "+" else ""
                    Text(
                        "Change: $sign฿${String.format(Locale.ENGLISH, "%,.2f", diff)}",
                        color = if (diff >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }} else null
            )

            // Reason label
            Text("Reason", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

            // Preset reason chips
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetReasons.forEach { reason ->
                    val isSelected = selectedReason == reason && customReason.isBlank()
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedReason = if (isSelected) "" else reason
                            customReason = ""
                        },
                        label = { Text(reason, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Custom reason field
            OutlinedTextField(
                value = customReason,
                onValueChange = {
                    customReason = it
                    if (it.isNotBlank()) selectedReason = ""
                },
                label = { Text("Other reason…") },
                modifier = Modifier.fillMaxWidth(),
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
    accountEquity: Double = 0.0,
    cashBalance: Double = 0.0,
    marketRegime: TechnicalAnalysis.MarketRegime = TechnicalAnalysis.MarketRegime.NEUTRAL,
    maxRiskPerTradePercent: Double = 1.0,
    minRiskRewardRatio: Double = 2.0,
    maxStockAllocationPercent: Double = apincer.mobile.tradings.domain.TradingConstants.MAX_SINGLE_STOCK_ALLOCATION_PERCENT,
    maxSectorAllocationPercent: Double = apincer.mobile.tradings.domain.TradingConstants.MAX_SECTOR_ALLOCATION_PERCENT,
    holdings: List<StockWatchlistInfo> = emptyList(),
    targetCorePercent: Double = apincer.mobile.tradings.domain.CoreSatellite.DEFAULT_TARGET_CORE_PERCENT,
    atsEnabled: Boolean = true,
    isSaving: Boolean = false,
    /** New-buy prefill, e.g. a DCA purchase of the core: symbol, price, shares. */
    prefill: Triple<String, Double, Int>? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, Double, Double, String, String, Boolean) -> Unit
) {
    var symbol by remember { mutableStateOf(initialStock?.info?.symbol ?: prefill?.first ?: "") }
    var entryPrice by remember { mutableStateOf(initialStock?.portfolio?.cost?.toString() ?: prefill?.second?.toString() ?: "") }
    var qty by remember { mutableStateOf(initialStock?.portfolio?.quantity?.toString() ?: prefill?.third?.takeIf { it > 0 }?.toString() ?: "") }
    
    var targetPrice by remember { mutableStateOf(initialStock?.portfolio?.portfolio?.targetPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var stopLossPrice by remember { mutableStateOf(initialStock?.portfolio?.stopLoss?.let { if (it > 0) it.toString() else "" } ?: "") }
    var playbookNote by remember { mutableStateOf(initialStock?.portfolio?.playbookNote ?: "") }
    var tradePurpose by remember { mutableStateOf(initialStock?.portfolio?.tradePurpose ?: "SWING") }
    var recordExecutedFill by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExtracting by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            isExtracting = true
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    
                    withContext(Dispatchers.IO) {
                        val db = apincer.mobile.tradings.data.StockDatabase.getDatabase(context)
                        val prefRepo = PreferenceRepository(context)
                        val apiKey = prefRepo.geminiApiKey.first()
                        val result = GeminiClient.extractTradeScreenshot(base64, apiKey)
                        
                        withContext(Dispatchers.Main) {
                            if (result.isSuccess) {
                                val data = result.getOrNull()
                                if (data != null) {
                                    symbol = data.symbol
                                    if (data.price > 0) entryPrice = data.price.toString()
                                    if (data.quantity > 0) qty = data.quantity.toString()
                                    recordExecutedFill = true
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Vision failed: " + result.exceptionOrNull()?.message, android.widget.Toast.LENGTH_LONG).show()
                            }
                            isExtracting = false
                        }
                    }
                } catch (e: Exception) {
                    isExtracting = false
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Error reading image", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    val entry = entryPrice.toDoubleOrNull() ?: 0.0
    val amount = qty.toIntOrNull() ?: 0
    // Orders only fill on SET price steps: stops snap up (no extra risk), targets snap down.
    val enteredTarget = targetPrice.toDoubleOrNull() ?: 0.0
    val enteredStop = stopLossPrice.toDoubleOrNull() ?: 0.0
    val target = if (enteredTarget > 0.0) apincer.mobile.tradings.domain.SetTick.floor(enteredTarget) else 0.0
    val stopLoss = if (enteredStop > 0.0) apincer.mobile.tradings.domain.SetTick.ceil(enteredStop) else 0.0
    val existingHolding = if (initialStock == null) holdings.find {
        it.info.symbol.equals(symbol, ignoreCase = true) && it.portfolio.portfolio.quantity > 0
    } else null
    val planTarget = existingHolding?.portfolio?.portfolio?.targetPrice?.takeIf { it > 0.0 } ?: target
    val planStop = existingHolding?.portfolio?.portfolio?.stopLoss?.takeIf { it > 0.0 } ?: stopLoss
    val planPurpose = existingHolding?.portfolio?.portfolio?.tradePurpose ?: tradePurpose

    val riskPerShare = entry - planStop
    val rewardPerShare = planTarget - entry
    val buyFeeEstimate = TechnicalAnalysis.calculateFees(entry * amount, false, atsEnabled)
    val targetSellFeeEstimate = TechnicalAnalysis.calculateFees(planTarget * amount, true, atsEnabled)
    val stopSellFeeEstimate = TechnicalAnalysis.calculateFees(planStop * amount, true, atsEnabled)
    val netReward = rewardPerShare * amount - buyFeeEstimate - targetSellFeeEstimate
    val netRisk = riskPerShare * amount + buyFeeEstimate + stopSellFeeEstimate
    val rrRatio = if (netRisk > 0.0) netReward / netRisk else 0.0
    val positionRecommendation = if (entry > 0 && planStop > 0 && accountEquity > 0) {
        TechnicalAnalysis.calculateRecommendedPositionSize(
            totalAssets = accountEquity,
            entryPrice = entry,
            stopLossPrice = planStop,
            riskPercent = maxRiskPerTradePercent,
            maxStockAllocationPercent = maxStockAllocationPercent
        )
    } else null
    val suggestedQty = positionRecommendation?.shares ?: if (riskPerShare > 0) {
        TechnicalAnalysis.calculateSuggestedQuantity(accountEquity, maxRiskPerTradePercent, riskPerShare)
    } else 0
    val currentTradeRiskBaht = riskPerShare.coerceAtLeast(0.0) * amount
    val currentTradeRiskPercent = if (accountEquity > 0) currentTradeRiskBaht / accountEquity * 100.0 else 0.0
    val isValidDividend = planPurpose == "DIVIDEND" || playbookNote.lowercase().contains("dividend")
    val selectedSector = holdings.find { it.info.symbol.equals(symbol, ignoreCase = true) }?.info?.sector
    // Concentration limits use cost basis, as the advisor and executeBuy do (tasks/lessons.md #4).
    val existingStockValue = holdings.filter { it.info.symbol.equals(symbol, ignoreCase = true) && it.portfolio.quantity > 0 }
        .sumOf { it.portfolio.cost * it.portfolio.quantity }
    val existingSectorValue = selectedSector?.let { sector ->
        holdings.filter { it.info.sector == sector && it.portfolio.quantity > 0 }
            .sumOf { it.portfolio.cost * it.portfolio.quantity }
    }
    val isCoreBuy = apincer.mobile.tradings.domain.CoreSatellite.isCore(symbol)
    val showTradePlan = !(isCoreBuy && initialStock == null)
    val riskResult = if (isCoreBuy) apincer.mobile.tradings.domain.TradeRiskPolicy.evaluateCoreBuy(
        entry, amount, buyFeeEstimate, cashBalance
    ) else apincer.mobile.tradings.domain.TradeRiskPolicy.evaluate(
        apincer.mobile.tradings.domain.TradeRiskInput(
            entry, planStop, amount, TechnicalAnalysis.calculateFees(entry * amount, false, atsEnabled),
            accountEquity, cashBalance, existingStockValue, existingSectorValue, atsEnabled,
            planTarget, planPurpose == "SWING"
        ),
        apincer.mobile.tradings.domain.TradeRiskLimits(
            maxRiskPerTradePercent, maxStockAllocationPercent, maxSectorAllocationPercent,
            TechnicalAnalysis.getRecommendedCashBufferPercent(marketRegime), minRiskRewardRatio
        )
    )
    val isFormValid = if (initialStock != null) {
        symbol.isNotBlank() && entry > 0.0 &&
            amount == initialStock.portfolio.portfolio.quantity &&
            (target <= 0.0 || (stopLoss > 0.0 && stopLoss < entry && target > entry))
    } else {
        symbol.isNotBlank() && entry > 0 && amount > 0 && 
        (recordExecutedFill || isCoreBuy || (isValidDividend && planStop > 0 && planStop < entry) ||
            (planTarget > entry && planStop > 0 && planStop < entry && rrRatio >= minRiskRewardRatio)) &&
        (recordExecutedFill || riskResult.allowed)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (initialStock == null) stringResource(R.string.title_record_purchase) else stringResource(R.string.title_edit_holding),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                if (initialStock == null) {
                    if (isExtracting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.ImageSearch, contentDescription = "Import Screenshot", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
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
                    // A core position is held, not swing- or dividend-managed.
                    if (showTradePlan) {
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
                        Text(
                            text = if (tradePurpose == "SWING") {
                                "⚡ Swing: Active trade management. Enforces daily trailing stops, take-profit at 2× the stop distance, and technical exits."
                            } else {
                                "💰 Dividend: Long-term compounding. Bypasses daily trailing stops; alerts only on fundamental breaks (ROE < 15%) or deep drawdown (> 20%)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
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

            if (!showTradePlan) item {
                Text("${apincer.mobile.tradings.domain.CoreSatellite.CORE_SYMBOL} is your core: bought and held, so no stop, target or position caps apply. Only available cash is checked.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }

            if (showTradePlan) item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.label_rr_calculator), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    
                }
                Text(if (existingHolding != null) "This additional buy keeps the holding's saved stop and target. Net ratio uses those levels and estimated fees."
                    else "Enter your own target and stop. Net ratio includes estimated broker fees at both exits.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (existingHolding != null) planTarget.takeIf { it > 0.0 }?.toString() ?: "" else targetPrice,
                        onValueChange = { targetPrice = it },
                        enabled = existingHolding == null,
                        label = { Text(stringResource(R.string.label_target)) },
                        supportingText = if (existingHolding == null && target > 0.0 && target != enteredTarget) {
                            { Text("SET price: ฿${String.format(Locale.ENGLISH, "%.2f", target)}") }
                        } else null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        prefix = { Text("฿ ") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = if (existingHolding != null) planStop.takeIf { it > 0.0 }?.toString() ?: "" else stopLossPrice,
                        onValueChange = { stopLossPrice = it },
                        enabled = existingHolding == null,
                        label = { Text(stringResource(R.string.label_stop_loss)) },
                        supportingText = if (existingHolding == null && stopLoss > 0.0 && stopLoss != enteredStop) {
                            { Text("SET price: ฿${String.format(Locale.ENGLISH, "%.2f", stopLoss)}") }
                        } else null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        prefix = { Text("฿ ") },
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            if (showTradePlan && entry > 0 && riskPerShare > 0 && accountEquity > 0) {
                item {
                    GlassCard(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Position Size Calculator", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { qty = suggestedQty.toString() }
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Use $suggestedQty shares", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }
                            Text(
                                "Risking ${String.format(Locale.ENGLISH, "%.1f", maxRiskPerTradePercent)}% of ฿${String.format(Locale.ENGLISH, "%,.0f", accountEquity)} equity (risk/share ฿${String.format(Locale.ENGLISH, "%.2f", riskPerShare)}) caps this trade at $suggestedQty shares (100-share board lots)" +
                                    (if (positionRecommendation?.isCappedByMaxStockLimit == true) " · Capped by 15% single-stock ceiling." else "."),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (amount > 0) {
                                val overBudget = currentTradeRiskPercent > maxRiskPerTradePercent + 0.01
                                Text(
                                    "Your entered qty ($amount) risks ฿${String.format(Locale.ENGLISH, "%,.0f", currentTradeRiskBaht)} " +
                                        "(${String.format(Locale.ENGLISH, "%.1f", currentTradeRiskPercent)}% of equity)" +
                                        if (overBudget) " — above your ${String.format(Locale.ENGLISH, "%.1f", maxRiskPerTradePercent)}% limit." else ".",
                                    fontSize = 11.sp,
                                    fontWeight = if (overBudget) FontWeight.Bold else FontWeight.Normal,
                                    color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            val satelliteBuyValue = (entry * amount -
                (initialStock?.portfolio?.cost ?: 0.0) * (initialStock?.portfolio?.quantity ?: 0)).coerceAtLeast(0.0)
            val currentHoldings = holdings.filter { it.portfolio.quantity > 0 }
                .map { it.info.symbol to it.info.lastPrice * it.portfolio.quantity }
            if (symbol.isNotBlank() && apincer.mobile.tradings.domain.CoreSatellite.breachesSatelliteCap(
                    currentHoldings, symbol, satelliteBuyValue, targetCorePercent)) {
                item {
                    val after = apincer.mobile.tradings.domain.CoreSatellite.allocationAfterBuy(
                        currentHoldings, symbol, satelliteBuyValue, targetCorePercent)
                    Surface(
                        color = Color(0xFFFFA726).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            String.format(
                                java.util.Locale.ENGLISH,
                                "Satellite would be %.0f%% of invested value (cap %.0f%%). Consider funding the %s core first.",
                                after.satellitePercent, 100.0 - targetCorePercent,
                                apincer.mobile.tradings.domain.CoreSatellite.CORE_SYMBOL
                            ),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (accountEquity > 0 && (entry > 0 || cashBalance > 0)) {
                item {
                    val recommendedBufferPercent = TechnicalAnalysis.getRecommendedCashBufferPercent(marketRegime)
                    val targetCashReserve = accountEquity * (recommendedBufferPercent / 100.0)
                    val previousCost = (initialStock?.portfolio?.cost ?: 0.0) * (initialStock?.portfolio?.quantity ?: 0)
                    val newCost = entry * amount
                    val incrementalCost = if (initialStock != null) (newCost - previousCost).coerceAtLeast(0.0) else newCost
                    val projectedCashRemaining = cashBalance - incrementalCost
                    val projectedBufferPercent = if (accountEquity > 0) (projectedCashRemaining / accountEquity * 100.0).coerceAtLeast(0.0) else 0.0
                    val isInsufficientCash = incrementalCost > cashBalance
                    // The regime buffer gates satellite buys only; core DCA should continue when prices fall.
                    val isBufferDeficit = !isCoreBuy && projectedCashRemaining < targetCashReserve

                    val statusColor = when {
                        isInsufficientCash -> MaterialTheme.colorScheme.error
                        isBufferDeficit -> Color(0xFFFFA726)
                        else -> MaterialTheme.colorScheme.tertiary
                    }

                    GlassCard(
                        containerColor = statusColor.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isBufferDeficit || isInsufficientCash) Icons.Default.Warning else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (isCoreBuy) "Cash After Purchase" else "Cash Buffer & Liquidity",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                                if (!isCoreBuy) Surface(
                                    color = statusColor.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${marketRegime.name} TARGET: ${recommendedBufferPercent.toInt()}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Post-Trade Cash", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "฿${String.format(Locale.ENGLISH, "%,.0f", projectedCashRemaining.coerceAtLeast(0.0))}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                    Text(
                                        "${String.format(Locale.ENGLISH, "%.1f", projectedBufferPercent)}% of Equity",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!isCoreBuy) Column(horizontalAlignment = Alignment.End) {
                                    Text("Mandated Buffer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "฿${String.format(Locale.ENGLISH, "%,.0f", targetCashReserve)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${recommendedBufferPercent.toInt()}% Reserve",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!isCoreBuy) LinearProgressIndicator(
                                progress = { ((projectedBufferPercent / 50.0).toFloat()).coerceIn(0.02f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = statusColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            val statusMsg = when {
                                recordExecutedFill && isInsufficientCash -> "The recorded fill will leave negative cash. Reconcile deposits or earlier trades in the ledger."
                                isInsufficientCash -> "Insufficient cash: Trade cost (฿${String.format(Locale.ENGLISH, "%,.0f", incrementalCost)}) exceeds available cash (฿${String.format(Locale.ENGLISH, "%,.0f", cashBalance)})."
                                isCoreBuy -> "Core buy: no regime cash buffer applies, only available cash."
                                isBufferDeficit -> "Buffer Breach: Post-trade cash (${String.format(Locale.ENGLISH, "%.1f", projectedBufferPercent)}%) falls below ${recommendedBufferPercent.toInt()}% regime target. Deficit: ฿${String.format(Locale.ENGLISH, "%,.0f", targetCashReserve - projectedCashRemaining)}."
                                else -> "Healthy liquidity: Preserves ${recommendedBufferPercent.toInt()}% cash buffer (฿${String.format(Locale.ENGLISH, "%,.0f", targetCashReserve)}) required for ${marketRegime.name.lowercase().replaceFirstChar { it.uppercase() }} conditions."
                            }
                            Text(
                                text = statusMsg,
                                fontSize = 11.sp,
                                fontWeight = if (isBufferDeficit || isInsufficientCash) FontWeight.Medium else FontWeight.Normal,
                                color = if (isInsufficientCash) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (showTradePlan && entry > 0 && amount > 0 && planTarget > 0 && planStop > 0) {
                item {
                    GlassCard(
                        containerColor = (if (rrRatio >= minRiskRewardRatio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary).copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.label_rr_ratio), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("1 : ${String.format(Locale.ENGLISH, "%.2f", rrRatio)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (rrRatio >= minRiskRewardRatio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                            }
                            LinearProgressIndicator(
                                progress = { (rrRatio / 5f).toFloat().coerceIn(0.1f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                color = if (rrRatio >= minRiskRewardRatio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
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
                                    Text("฿${String.format(Locale.ENGLISH, "%,.2f", netRisk)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                if (tradePurpose == "SWING" && rrRatio < minRiskRewardRatio) item {
                    Text("Net Reward:Risk is below ${String.format(Locale.ENGLISH, "%.1f", minRiskRewardRatio)}:1 for a proposed swing trade.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
            Spacer(Modifier.height(24.dp))
            if (initialStock == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recordExecutedFill, onCheckedChange = { recordExecutedFill = it })
                    Text("Record an already executed broker trade", style = MaterialTheme.typography.bodySmall)
                }
                if (!riskResult.allowed) {
                    Text(riskResult.reasons.joinToString("; "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            if (initialStock != null && amount == 0) {
                Text("Record a sale to remove held shares", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = isFormValid && !isSaving,
                    onClick = { 
                        if (isFormValid) {
                            // A new core position carries no stop or target, even if the fields hold stale text.
                            val newCore = isCoreBuy && existingHolding == null
                            val acceptedTarget = if (newCore || recordExecutedFill &&
                                (planStop <= 0.0 || planStop >= entry || planTarget <= entry)) 0.0 else planTarget
                            onConfirm(symbol, entry, amount, acceptedTarget, if (newCore) 0.0 else planStop, playbookNote,
                                planPurpose, recordExecutedFill)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        if (initialStock == null) stringResource(R.string.action_add_to_portfolio)
                        else stringResource(R.string.action_update)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellStockDialog(
    stock: StockWatchlistInfo,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String) -> Unit
) {
    var price by remember(stock.info.symbol) { mutableStateOf(String.format(Locale.ENGLISH, "%.2f", stock.info.lastPrice)) }
    var qty by remember(stock.info.symbol) { mutableStateOf(stock.portfolio.quantity.toString()) }
    var lesson by remember(stock.info.symbol) { mutableStateOf("") }
    var selectedReason by remember(stock.info.symbol) { mutableStateOf("") }

    val exitReasons = listOf("Target Hit 🎯", "Stop Hit 🛡️", "Trailing Stop 📈", "Boredom 😴", "Mistake 🤦", "Fundamental 📜")

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                "Trade Autopsy: ${stock.info.symbol}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_current_holdings_count, stock.portfolio.quantity), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    TextButton(
                        onClick = { qty = stock.portfolio.quantity.toString() },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Sell All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text(stringResource(R.string.label_sell_price)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        prefix = { Text("฿ ") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text(stringResource(R.string.label_quantity_to_sell)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Text("Exit Reason", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(-4.dp)
                ) {
                    exitReasons.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = lesson,
                    onValueChange = { lesson = it },
                    label = { Text("Lesson Learned (Psychological Journal)") },
                    placeholder = { Text("What did you do well? What went wrong?") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 4
                )
            }
        
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Spacer(Modifier.width(8.dp))
                val sellQty = qty.toIntOrNull() ?: 0
                val sellPrice = price.toDoubleOrNull() ?: 0.0
                
                val isNegativeExit = selectedReason.contains("Stop Hit") || selectedReason.contains("Mistake") || selectedReason.contains("Boredom") || sellPrice < stock.portfolio.cost
                val isLessonSufficient = !isNegativeExit || lesson.trim().length >= 10
                
                val isValid = sellQty > 0 && sellQty <= stock.portfolio.quantity && sellPrice.isFinite() && sellPrice > 0.0 && selectedReason.isNotBlank() && isLessonSufficient
                
                Button(
                    enabled = isValid && !isSaving,
                    onClick = { 
                        if (isValid) {
                            val finalNote = "[$selectedReason] $lesson"
                            onConfirm(stock.info.symbol, sellPrice, sellQty, finalNote)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isLessonSufficient) stringResource(R.string.action_confirm_sell) else "Write Lesson First", color = Color.White)
                }
            }
        }
    }
}

/** Payouts the app expects on current holdings that are not logged yet: confirm instead of typing. */
@Composable
fun PendingDividendsCard(
    pending: List<apincer.mobile.tradings.domain.PendingDividends.Pending>,
    onRecord: (apincer.mobile.tradings.domain.PendingDividends.Pending) -> Unit,
    onDismiss: (apincer.mobile.tradings.domain.PendingDividends.Pending) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Dividends to record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Recent payouts on your holdings that are not in the dividend log. Check the amount against your broker statement, then record it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            pending.forEach { p ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.symbol, fontWeight = FontWeight.Black)
                        Text(
                            String.format(java.util.Locale.ENGLISH, "XD %s · ฿%.2f × %,d = ฿%,.2f, about ฿%,.2f after 10%% tax",
                                p.exDate, p.perShare, p.shares, p.gross, p.net),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onDismiss(p) }) { Text("Dismiss") }
                    Button(onClick = { onRecord(p) }, shape = RoundedCornerShape(10.dp)) { Text("Record") }
                }
            }
        }
    }
}

@Composable
fun CoreSatelliteCard(
    portfolioItems: List<StockWatchlistInfo>,
    targetCorePercent: Double,
    monthlyDcaAmount: Double = 0.0,
    dcaDay: Int = apincer.mobile.tradings.domain.CoreSatellite.DEFAULT_DCA_DAY,
    corePrice: Double? = null,
    atsEnabled: Boolean = true,
    onSetupCore: (amount: Double, day: Int) -> Unit = { _, _ -> },
    onBuyCore: (price: Double, shares: Int) -> Unit = { _, _ -> }
) {
    val allocation = apincer.mobile.tradings.domain.CoreSatellite.allocation(
        portfolioItems.map { it.info.symbol to it.info.lastPrice * it.portfolio.quantity },
        targetCorePercent
    )
    val coreSymbol = apincer.mobile.tradings.domain.CoreSatellite.CORE_SYMBOL
    val isBelowTarget = allocation.investedValue > 0.0 && allocation.driftPercent < -5.0
    val barColor = if (isBelowTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    fun pct(v: Double) = String.format(java.util.Locale.ENGLISH, "%.0f%%", v)
    fun baht(v: Double) = String.format(java.util.Locale.ENGLISH, "฿%,.0f", v)
    fun lotsText(budget: Double): String? = corePrice?.let { price ->
        val s = apincer.mobile.tradings.domain.CoreSatellite.dcaSuggestion(budget, price, atsEnabled)
        if (s.shares > 0) "buys ${String.format(java.util.Locale.ENGLISH, "%,d", s.shares)} $coreSymbol at ฿${String.format(java.util.Locale.ENGLISH, "%.2f", price)} " +
            "(${baht(s.estimatedCost)} with fees, ${baht(s.unusedBaht)} left)"
        else "is less than one 100-share lot at ฿${String.format(java.util.Locale.ENGLISH, "%.2f", price)}"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Core vs Satellite",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (allocation.investedValue > 0.0) {
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Core $coreSymbol ${pct(allocation.corePercent)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = barColor)
                    Text("Target ${pct(targetCorePercent)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(
                    progress = { (allocation.corePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Satellite (individual stocks) ${pct(allocation.satellitePercent)} · cap ${pct(100.0 - targetCorePercent)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (allocation.coreValue > 0.0 && isBelowTarget) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "About ${baht(allocation.coreShortfallBaht)} more $coreSymbol reaches your target without selling. Direct new money to the core first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            if (monthlyDcaAmount <= 0.0) {
                // First-run setup: the core is the plan the backtest supports, so it is set up here, not in Settings.
                var amountText by remember { mutableStateOf("") }
                var dayText by remember { mutableStateOf(dcaDay.toString()) }
                val amount = amountText.toDoubleOrNull()?.takeIf { it > 0.0 }
                val day = dayText.toIntOrNull()?.takeIf { it in 1..28 }
                Text(
                    "Start your index core: put a fixed amount into $coreSymbol (SET50 ETF) every month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        label = { Text("Monthly amount") }, prefix = { Text("฿ ") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(2f), shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = dayText, onValueChange = { dayText = it },
                        label = { Text("Day") }, singleLine = true, isError = dayText.isNotBlank() && day == null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)
                    )
                }
                amount?.let { a -> lotsText(a)?.let {
                    Text("${baht(a)} $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                } }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { if (amount != null && day != null) onSetupCore(amount, day) },
                    enabled = amount != null && day != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save monthly plan") }
            } else {
                Text(
                    "Monthly plan: ${baht(monthlyDcaAmount)} on day $dcaDay" + (lotsText(monthlyDcaAmount)?.let { " $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val suggestion = corePrice?.let { apincer.mobile.tradings.domain.CoreSatellite.dcaSuggestion(monthlyDcaAmount, it, atsEnabled) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { if (corePrice != null && suggestion != null) onBuyCore(corePrice, suggestion.shares) },
                    enabled = corePrice != null && (suggestion?.shares ?: 0) > 0,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (corePrice == null) "Loading $coreSymbol price…" else "Record $coreSymbol buy") }
            }
        }
    }
}

@Composable
fun SectorBreakdownCard(
    portfolioItems: List<StockWatchlistInfo>,
    cashBalance: Double = 0.0,
    maxSectorPercent: Double = apincer.mobile.tradings.domain.TradingConstants.MAX_SECTOR_ALLOCATION_PERCENT
) {
    if (portfolioItems.isEmpty()) return
    
    val totalValue = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
    if (totalValue == 0.0) return

    val totalAssets = totalValue + maxOf(cashBalance, 0.0)
    val sectorExposures = TechnicalAnalysis.calculateSectorExposures(
        portfolioItems.map { Pair(it.info.sector ?: "Other", it.info.lastPrice * it.portfolio.quantity) },
        cashBalance,
        maxSectorPercent = maxSectorPercent
    )

    val hasWarning = sectorExposures.any { it.isOverexposed }

    // Beta calculation
    val betaWeights = portfolioItems.mapNotNull { item ->
        val weight = (item.info.lastPrice * item.portfolio.quantity)
        val beta = (item.portfolio.relativeStrength ?: 0.0).let { rs ->
            // Beta estimation: 1.0 + (RS / 100) clamped 0.4 to 2.0
            (1.0 + rs / 100.0).coerceIn(0.4, 2.0)
        }
        if (weight > 0) Pair(weight, beta) else null
    }
    val portfolioBeta = if (betaWeights.isNotEmpty()) TechnicalAnalysis.calculatePortfolioBeta(betaWeights) else 1.0
    val betaProfile = when {
        portfolioBeta < 0.85 -> Pair("Defensive Low-Vol", Color(0xFF6EE7B7))
        portfolioBeta <= 1.15 -> Pair("Balanced Index Track", Color(0xFF60A5FA))
        else -> Pair("Aggressive High-Beta", Color(0xFFFCD34D))
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Risk Matrix & Sectors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = betaProfile.second.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Beta ${String.format(java.util.Locale.ENGLISH, "%.2f", portfolioBeta)} · ${betaProfile.first}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = betaProfile.second
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))

            if (hasWarning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha=0.5f), RoundedCornerShape(8.dp)).padding(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sector Cap Warning: Positions exceed ${String.format(java.util.Locale.ENGLISH, "%.0f", maxSectorPercent)}% concentration limit in a single sector.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            sectorExposures.forEach { exposure ->
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(exposure.sector, style = MaterialTheme.typography.labelMedium)
                        Text(
                            String.format(java.util.Locale.ENGLISH, "%.1f%%", exposure.portfolioPercent),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (exposure.isOverexposed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (exposure.portfolioPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 4.dp),
                        color = if (exposure.isOverexposed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun DividendTaxShieldCard(
    dividendHistory: List<apincer.mobile.tradings.data.DividendHistoryEntity>,
    avgYieldOnCost: Double?,
    citRate: Double = apincer.mobile.tradings.domain.TradingConstants.DEFAULT_CIT_TAX_RATE,
    personalTaxRate: Double? = null
) {
    // Sec. 47 bis is elected per tax year (calendar year) and covers only dividends from Thai companies:
    // fund distributions such as TDEX carry no credit, so they are left out.
    val taxYear = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).year
    val thisYear = dividendHistory.filter {
        java.time.Instant.ofEpochMilli(it.dateMillis).atZone(java.time.ZoneId.of("Asia/Bangkok")).year == taxYear
    }
    val (fundPayouts, companyPayouts) = thisYear.partition { apincer.mobile.tradings.domain.CoreSatellite.isCore(it.symbol) }
    fun gross(d: apincer.mobile.tradings.data.DividendHistoryEntity) =
        if (d.taxDeducted > 0.0) d.totalReceived + d.taxDeducted
        else d.totalReceived / (1.0 - apincer.mobile.tradings.domain.TradingConstants.THAI_DIVIDEND_WHT_RATE / 100.0)
    val grossCompany = companyPayouts.sumOf { gross(it) }
    val totalTaxWithheld = thisYear.sumOf { it.taxDeducted }
    val totalTaxCredit = grossCompany * citRate / (100.0 - citRate).coerceAtLeast(1.0)
    val electionBenefit = personalTaxRate?.let { TechnicalAnalysis.dividendElectionBenefit(grossCompany, it, citRate) }
    val breakEven = TechnicalAnalysis.dividendElectionBreakEvenRate(citRate)
    val netYoC = avgYieldOnCost?.let { it * (1.0 - apincer.mobile.tradings.domain.TradingConstants.THAI_DIVIDEND_WHT_RATE / 100.0) }

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Dividend Tax Credit $taxYear (Sec. 47 bis)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.ENGLISH, "%.0f", citRate)}% CIT Credit",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(if (electionBenefit != null) "Net If You Claim" else "Gross Tax Credit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val shown = electionBenefit ?: totalTaxCredit
                    Text(
                        "${if (shown < 0) "-" else ""}฿${String.format(java.util.Locale.ENGLISH, "%,.2f", kotlin.math.abs(shown))}",
                        fontSize = 15.sp, fontWeight = FontWeight.Black,
                        color = if (shown >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("10% WHT Withheld", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("฿${String.format(java.util.Locale.ENGLISH, "%,.2f", totalTaxWithheld)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Net Yield-on-Cost", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = netYoC?.let { "${String.format(java.util.Locale.ENGLISH, "%.2f", it)}%" } ?: "---",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            val advice = when {
                electionBenefit == null -> "Set your top income-tax bracket in Settings to see the net effect. Claiming pays only below a ${String.format(java.util.Locale.ENGLISH, "%.0f", breakEven)}% bracket."
                electionBenefit > 0.0 -> "At your ${String.format(java.util.Locale.ENGLISH, "%.0f", personalTaxRate)}% bracket, adding these dividends to your tax return gains about this much versus keeping the 10% withholding final."
                else -> "At your ${String.format(java.util.Locale.ENGLISH, "%.0f", personalTaxRate)}% bracket, keep the 10% withholding final: claiming would cost this much."
            }
            Text(
                text = "$advice The choice covers all dividends in the tax year. No credit on fund payouts" +
                    (if (fundPayouts.isNotEmpty()) " (TDEX ฿${String.format(java.util.Locale.ENGLISH, "%,.0f", fundPayouts.sumOf { it.totalReceived })} excluded)" else "") +
                    " or on dividends paid from BOI tax-exempt profits; check each company's notice.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
