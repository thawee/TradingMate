package apincer.mobile.tradings.ui

import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import apincer.mobile.tradings.R
import apincer.mobile.tradings.data.ScrapedStockInfo
import apincer.mobile.tradings.data.StockAggregate
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TechnicalAnalysis
import apincer.mobile.tradings.domain.TradeSignal
import apincer.mobile.tradings.domain.TradingZone
import java.util.Locale

enum class Screen(val labelResId: Int, val icon: ImageVector, val inBottomBar: Boolean = true) {
    WATCHLIST(R.string.title_watchlist, Icons.Default.QueryStats),
    ADVISOR(R.string.title_advisor, Icons.Default.Savings),
    PORTFOLIO(R.string.title_portfolio, Icons.Default.AccountBalance),
    STATS(R.string.title_history, Icons.Default.History),
    SETTINGS(R.string.title_settings, Icons.Default.Settings),
    EDUCATION(R.string.title_academy, Icons.Default.School, false),
    ABOUT(R.string.title_about, Icons.Default.Info, false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: StockViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    openSymbol: String? = null
) {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.WATCHLIST) }
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val minRR by settingsViewModel.minRiskRewardRatio.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    val refreshError by viewModel.refreshError.collectAsState()
    LaunchedEffect(refreshError) {
        refreshError?.let { msg ->
            snackbarHostState.showSnackbar("Refresh failed: $msg")
            viewModel.clearRefreshError()
        }
    }

    LaunchedEffect(openSymbol) {
        openSymbol?.let {
            currentScreen = Screen.PORTFOLIO
        }
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (uiState is StockUiState.Success) {
                    val state = uiState as StockUiState.Success
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    val context = androidx.compose.ui.platform.LocalContext.current

                    ExtendedFloatingActionButton(
                        onClick = { 
                            val symbol = state.stockInfo.symbol
                            val pe = state.stockInfo.pe ?: 0.0
                            val yield = state.stockInfo.dividendYield ?: 0.0
                            val rsi = state.rsi?.let { String.format(Locale.ENGLISH, "%.1f", it) } ?: "N/A"
                            val macdHist = state.macd.third?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "N/A"
                            val sma50 = state.sma50?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "N/A"
                            val sma200 = state.sma200?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "N/A"
                            val zone = state.zone.name
                            val signalType = state.signal.type.name
                            val signalReason = state.signal.reason
                            val sector = state.stockInfo.sector ?: "N/A"
                            val marketCap = state.stockInfo.marketCap?.let { String.format(Locale.ENGLISH, "%,.0fM THB", it / 1_000_000.0) } ?: "N/A"
                            val roe = state.stockInfo.roe?.let { String.format(Locale.ENGLISH, "%.1f%%", it) } ?: "N/A"
                            val de = state.stockInfo.debtToEquity?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "N/A"
                            val npm = state.stockInfo.netProfitMargin?.let { String.format(Locale.ENGLISH, "%.1f%%", it) } ?: "N/A"
                            val lastUpdated = state.stockInfo.lastUpdated

                            val prompt = """
                                Act as my expert subagents to evaluate the Stock Exchange of Thailand (SET) ticker $symbol.
                                
                                DATA SHEET (Updated: $lastUpdated):
                                - Sector: $sector | Market Cap: $marketCap
                                - Fundamentals: P/E: $pe | Yield: $yield% | ROE: $roe | D/E: $de | Net Margin: $npm
                                - Technicals: RSI: $rsi | MACD Hist: $macdHist | SMA 50: $sma50 | SMA 200: $sma200
                                - Current Signal: $signalType ($signalReason)
                                - Technical Zone: $zone
                                
                                PLAYBOOK RULES & CONSTRAINTS:
                                - Holding Period: 2-4 weeks (Swing) or Long-term (Dividend).
                                - Technical Alignment: Focus on technical support and indicator confirmations.
                                - RISK: Risk/Reward ratio MUST be >= 2.0. Strict Stop Loss required.
                                
                                GUARDRAILS & NEGATIVE CONSTRAINTS:
                                - DO NOT recommend if liquidity is dangerously low.
                                - DO NOT recommend penny stocks or leveraged DW products.
                                - DO NOT provide direct financial advice; frame all recommendations as educational analysis.
                                
                                DELEGATED TASKS:
                                1. [market-researcher]: Perform a live web search for recent news (last 7 days), upcoming earnings events, and catalysts on $symbol. Is their business moat strong? Also perform a quick query for current SET index level and sector trends to verify macro context.
                                2. [risk-manager]: Is this setup safe? Identify major downside scenarios. Determine an exact Buy Zone and strict Stop Loss.
                                
                                EXPLAIN INSTRUCTIONS:
                                - Break down the final decision step-by-step.
                                - Use ELI10 style (Explain Like I'm 10) so it's super simple.
                                - Provide a real-world analogy to describe the recommended action/situation.
                                
                                FORMAT REQUIREMENT:
                                Output the final analysis as a clean Markdown report with the following structure:
                                ### Executive Summary (Ticker, Moat, Action)
                                ### Technical & Fundamental Setup (Markdown table of entry, target, and stop loss)
                                ### Subagent Moat & Downside Analysis (Detail news, catalysts, and risk factor)
                                ### Analogous Story (The real-world analogy)
                            """.trimIndent()
                            
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(prompt))
                            showSnackbar("Prompt copied! Paste it into your AI.")
                        },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Ask AI") },
                        text = { Text("AI Prompt") },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        Screen.entries.filter { it.inBottomBar }.forEach { screen ->
                            val label = stringResource(screen.labelResId)
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = label, modifier = Modifier.size(24.dp)) },
                                label = { Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) },
                                selected = currentScreen == screen,
                                alwaysShowLabel = false,
                                onClick = { 
                                    currentScreen = screen 
                                    viewModel.resetToInitial()
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (val state = uiState) {
                        is StockUiState.Success -> {
                            var showFocusDialog by remember { mutableStateOf(false) }

                            if (showFocusDialog) {
                                FocusSettingsDialog(
                                    symbol = state.stockInfo.symbol,
                                    initialTargetPrice = state.focusTargetPrice,
                                    currentPrice = state.stockInfo.lastPrice,
                                    onDismiss = { showFocusDialog = false },
                                    onConfirm = { target ->
                                        val startPriceToUse = if (state.isFocused && state.focusStartPrice > 0) state.focusStartPrice else state.stockInfo.lastPrice
                                        viewModel.addToFocusList(state.stockInfo.symbol, startPriceToUse, target)
                                        showSnackbar("${state.stockInfo.symbol} added to Focus List at ฿${String.format(Locale.ENGLISH, "%.2f", target)}")
                                        showFocusDialog = false
                                    }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        var totalDrag = 0f
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (totalDrag > 150f) {
                                                    viewModel.resetToInitial()
                                                }
                                                totalDrag = 0f
                                            },
                                            onDragCancel = { totalDrag = 0f },
                                            onHorizontalDrag = { change, dragAmount ->
                                                totalDrag += dragAmount
                                            }
                                        )
                                    }
                            ) {
                                CenterAlignedTopAppBar(
                                    title = { 
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(state.stockInfo.symbol, fontWeight = FontWeight.Black)
                                            if (state.stockInfo.lastUpdated.isNotBlank()) {
                                                Text(
                                                    text = stringResource(R.string.label_last_sync, state.stockInfo.lastUpdated),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                                    navigationIcon = {
                                        IconButton(onClick = { viewModel.resetToInitial() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back), modifier = Modifier.size(24.dp))
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = { 
                                                if (state.isFocused) {
                                                    viewModel.removeFromFocusList(state.stockInfo.symbol)
                                                    showSnackbar("${state.stockInfo.symbol} removed from Focus List")
                                                } else {
                                                    showFocusDialog = true
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (state.isFocused) Icons.Default.Star else Icons.Default.StarBorder, 
                                                contentDescription = stringResource(R.string.desc_focus),
                                                tint = if (state.isFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                )
                                StockDashboard(state, onEditFocus = { showFocusDialog = true })
                            }
                        }
                        is StockUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is StockUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                GlassCard(modifier = Modifier.padding(32.dp)) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(16.dp))
                                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                                        Spacer(Modifier.height(24.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            TextButton(onClick = { viewModel.resetToInitial() }) { 
                                                Text(stringResource(R.string.desc_back)) 
                                            }
                                            Button(
                                                onClick = { viewModel.fetchStockData(state.symbol) },
                                                shape = RoundedCornerShape(12.dp)
                                            ) { 
                                                Text("Retry") 
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is StockUiState.Initial -> {
                            when (currentScreen) {
                                Screen.PORTFOLIO -> PortfolioScreen(viewModel, settingsViewModel, onSelectStock = { viewModel.fetchStockData(it) }, showSnackbar = showSnackbar, scrollSymbol = openSymbol)
                                Screen.WATCHLIST -> WatchlistScreen(viewModel, settingsViewModel, onSelectStock = { viewModel.fetchStockData(it) }, showSnackbar = showSnackbar)
                                Screen.ADVISOR -> DividendAdvisorScreen(
                                    viewModel = viewModel,
                                    settingsViewModel = settingsViewModel,
                                    onNavigateToAcademy = { currentScreen = Screen.EDUCATION },
                                    showSnackbar = showSnackbar
                                )
                                Screen.STATS -> StatsScreen(viewModel, showSnackbar = showSnackbar)
                                Screen.SETTINGS -> SettingsScreen(viewModel, settingsViewModel, showSnackbar = showSnackbar)
                                Screen.EDUCATION -> TradingEducationScreen(onBack = { currentScreen = Screen.ADVISOR })
                                Screen.ABOUT -> AboutScreen(onBack = { currentScreen = Screen.ADVISOR })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusSettingsDialog(
    symbol: String,
    initialTargetPrice: Double,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var targetPrice by remember { mutableStateOf(if (initialTargetPrice > 0) initialTargetPrice.toString() else "") }

    GlassDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.title_focus_on, symbol),
        confirmButton = {
            Button(
                onClick = {
                    val target = targetPrice.toDoubleOrNull() ?: 0.0
                    onConfirm(target)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_confirm_focus))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_current_market_price), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("฿${String.format(Locale.ENGLISH, "%.2f", currentPrice)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }

            OutlinedTextField(
                value = targetPrice,
                onValueChange = { targetPrice = it },
                label = { Text(stringResource(R.string.label_target_price_thb)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                prefix = { Text("฿ ") },
                shape = RoundedCornerShape(14.dp)
            )

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_focus_tracker_info, String.format(Locale.ENGLISH, "%.2f", currentPrice)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StockDashboard(state: StockUiState.Success, onEditFocus: () -> Unit) {
    val info = state.stockInfo
    val portfolio = state.portfolio
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SignalCard(state.signal, state.zone)

        if (info.isPartialData) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    
                    Text(stringResource(R.string.label_partial_data_warning), fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (state.isFocused) {
            SectionContent(
                modifier = Modifier.clickable { onEditFocus() },
                title = stringResource(R.string.section_focus_tracking), 
                icon = Icons.Default.Star
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        
                        Text(stringResource(R.string.label_start_price), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        
                        Text("฿${String.format(Locale.ENGLISH, "%.2f", state.focusStartPrice)}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        
                        Text(stringResource(R.string.label_target), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (state.focusTargetPrice > 0) "฿${state.focusTargetPrice}" else stringResource(R.string.label_not_set), fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        SectionContent {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        
                        Text(text = info.symbol, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                        if (!info.nameTH.isNullOrBlank()) {
                            
                            Text(text = info.nameTH, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (state.isVolumeSurge) {
                        GlassTag(text = stringResource(R.string.label_volume_surge), color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                info.name?.let {
                    
                    Text(text = it, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }

                if (info.sector != null) {
                    val sectorIndustry = if (info.industry != null) "${info.sector} • ${info.industry}" else info.sector
                    Text(
                        text = sectorIndustry,
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(
                    onClick = { uriHandler.openUri("https://www.set.or.th/th/market/product/stock/quote/${info.symbol}/price") },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(stringResource(R.string.label_view_quote), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                info.businessDescription?.let {
                    
                    Text(
                        text = it, 
                        fontSize = 13.sp, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        
                        Text(text = "฿${String.format(Locale.ENGLISH, "%.2f", info.lastPrice)}", fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                        val changeColor = if (info.change >= 0.0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        
                        Text(
                            text = "${if (info.change >= 0) "+" else ""}${String.format(Locale.ENGLISH,"%.2f", info.change)} (${String.format(Locale.ENGLISH,"%.2f", info.percentChange)}%)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = changeColor
                        )
                    }

                    if (portfolio != null && portfolio.quantity > 0 && state.netProfitPercent != null) {
                        StockPortfolioSummaryCard(portfolio, info, state.netProfitPercent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    
                    Text(text = stringResource(R.string.label_market_cap), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    
                    Text(
                        text = info.marketCap?.let { "฿${formatLargeNumber(it)}" } ?: "N/A",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    
                    Text(text = stringResource(R.string.label_volume), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    
                    Text(
                        text = info.volume?.let { formatLargeNumber(it.toDouble()) } ?: "N/A",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (state.historicalPrices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                PriceTrendChart(prices = state.historicalPrices, isPositive = info.change >= 0)
            }

            
            Text(
                text = stringResource(R.string.label_last_sync, info.lastUpdated), 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), 
                modifier = Modifier.padding(top = 12.dp),
                fontWeight = FontWeight.Medium
            )
        }

        SectionContent(title = stringResource(R.string.section_market_valuation), icon = Icons.Default.QueryStats) {
            IndicatorRow(stringResource(R.string.label_pe_ratio), info.pe?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
            IndicatorRow(stringResource(R.string.label_pbv_ratio), info.pbv?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
            IndicatorRow(stringResource(R.string.label_book_value), info.bookValue?.let { String.format(Locale.ENGLISH,"฿%.2f", it) } ?: "N/A")
        }

        SectionContent(title = stringResource(R.string.section_core_financials), icon = Icons.Default.AccountBalance) {
            if (info.isFundamentalGood) {
                QualityStockBadge()
            }
            IndicatorRow(stringResource(R.string.label_roe), info.roe?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "N/A")
            IndicatorRow(stringResource(R.string.label_net_profit), info.netProfit?.let { "฿${String.format(Locale.ENGLISH,"%,.0f M", it)}" } ?: "N/A")
            IndicatorRow(stringResource(R.string.label_net_margin), info.netProfitMargin?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "N/A")
            IndicatorRow(stringResource(R.string.label_3y_growth), info.profitGrowth3Y?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "N/A")
            IndicatorRow(stringResource(R.string.label_eps), info.eps?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
            IndicatorRow(stringResource(R.string.label_de_ratio), info.debtToEquity?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
        }

        SectionContent(title = stringResource(R.string.section_dividends), icon = Icons.Default.AutoAwesome) {
            if ((info.dividendYield ?: 0.0) > 4.0) {
                HighDividendBadge()
            }
            IndicatorRow(stringResource(R.string.label_dividend_yield), info.dividendYield?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "No Payout")
            IndicatorRow(stringResource(R.string.label_last_xd), info.dividendDate ?: "N/A")

            if (portfolio != null) {
                val yieldOnCost = if (portfolio.cost > 0 && info.dividendYield != null) {
                    (info.dividendYield * info.lastPrice) / portfolio.cost
                } else null

                if (yieldOnCost != null) {
                    IndicatorRow(stringResource(R.string.label_yoc), "${String.format(Locale.ENGLISH,"%.2f", yieldOnCost)}%")
                }
            }
        }

        SectionContent(title = stringResource(R.string.section_strategy_zones), icon = Icons.Default.TrackChanges) {
            
            Text(text = stringResource(R.string.label_rsi_zones_title), fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TargetPriceBadge(
                    label = stringResource(R.string.label_buy_below),
                    price = state.buyingPrice,
                    color = MaterialTheme.colorScheme.tertiary
                )
                TargetPriceBadge(
                    label = stringResource(R.string.label_sell_above),
                    price = state.sellingPrice,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Text(
                text = stringResource(R.string.label_rsi_zone_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                fontWeight = FontWeight.Medium
            )

            
            Text(text = stringResource(R.string.label_price_return_history_title), fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PeriodReturnBadge("3D", state.returns[3])
                PeriodReturnBadge("7D", state.returns[7])
                PeriodReturnBadge("15D", state.returns[15])
                PeriodReturnBadge("30D", state.returns[30])
            }
        }

        SectionContent(title = stringResource(R.string.section_technical_indicators), icon = Icons.AutoMirrored.Filled.TrendingUp) {
            TechnicalIndicatorItem(
                label = stringResource(R.string.label_rsi_14),
                value = String.format(Locale.ENGLISH,"%.2f", state.rsi ?: 0.0),
                meaning = stringResource(R.string.label_rsi_meaning)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.05f))

            TechnicalIndicatorItem(
                label = stringResource(R.string.label_sma),
                value = "${String.format(Locale.ENGLISH,"%.2f", state.sma50 ?: 0.0)} / ${String.format(Locale.ENGLISH,"%.2f", state.sma200 ?: 0.0)}",
                meaning = stringResource(R.string.label_sma_meaning)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.05f))

            TechnicalIndicatorItem(
                label = stringResource(R.string.label_bollinger_bands),
                value = state.bb?.let { "${String.format(Locale.ENGLISH,"%.2f", it.lower)} - ${String.format(Locale.ENGLISH,"%.2f", it.upper)}" } ?: "N/A",
                meaning = "Volatility bands. Price near the lower band indicates oversold conditions, while near the upper band indicates overbought conditions."
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.05f))

            val macdLineVal = state.macd.first?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "N/A"
            val macdSignalVal = state.macd.second?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "N/A"
            val macdHistVal = state.macd.third?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "N/A"
            TechnicalIndicatorItem(
                label = "MACD (12, 26, 9)",
                value = "$macdLineVal / $macdSignalVal (Hist: $macdHistVal)",
                meaning = "Trend strength and momentum indicator. MACD line crossing above signal line suggests bullish momentum."
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PriceTrendChart(prices: List<Double>, isPositive: Boolean) {
    val color = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 0.0
    val range = (maxPrice - minPrice).coerceAtLeast(0.01)

    Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
                .alpha(0.8f)
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (prices.size - 1).coerceAtLeast(1)

            val path = Path()
            prices.forEachIndexed { i, price ->
                val x = i * stepX
                val y = height - ((price - minPrice) / range * height).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Add a subtle gradient fill
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.2f), Color.Transparent)
                )
            )
        }

        // Overlay Max Price at top
        Text(
            text = "Max: ฿${String.format(Locale.ENGLISH, "%.2f", maxPrice)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp)
        )

        // Overlay Min Price at bottom
        Text(
            text = "Min: ฿${String.format(Locale.ENGLISH, "%.2f", minPrice)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 4.dp)
        )
    }
}

fun formatLargeNumber(number: Double): String {
    return when {
        number >= 1_000_000_000_000 -> String.format(Locale.ENGLISH, "%.1fT", number / 1_000_000_000_000)
        number >= 1_000_000_000 -> String.format(Locale.ENGLISH, "%.1fB", number / 1_000_000_000)
        number >= 1_000_000 -> String.format(Locale.ENGLISH, "%.1fM", number / 1_000_000)
        else -> String.format(Locale.ENGLISH, "%,.0f", number)
    }
}

@Composable
fun TechnicalIndicatorItem(label: String, value: String, meaning: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            Text(text = value, fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        }
        
        Text(
            text = meaning,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StockPortfolioSummaryCard(portfolio: StockAggregate, info: ScrapedStockInfo, netProfitPercent: Double) {
    val totalCostRaw = portfolio.cost * portfolio.quantity
    val currentTotalValue = info.lastPrice * portfolio.quantity
    val buyFees = TechnicalAnalysis.calculateFees(totalCostRaw, false)
    val sellFees = TechnicalAnalysis.calculateFees(currentTotalValue, true)
    val stockTotalFees = buyFees + sellFees
    val grossProfit = currentTotalValue - totalCostRaw
    val netProfitValue = grossProfit - stockTotalFees

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.width(150.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
            
            Text(stringResource(R.string.label_returns), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            val profitColor = if (netProfitValue >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            
            Text(
                text = "฿${String.format(Locale.ENGLISH,"%,.0f", netProfitValue)}",
                fontWeight = FontWeight.Black, 
                fontSize = 18.sp,
                color = profitColor
            )
            
            Text("${String.format(Locale.ENGLISH,"%.1f", netProfitPercent)}%", fontSize = 14.sp, color = profitColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QualityStockBadge() {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
        modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            
            Spacer(Modifier.width(8.dp))
            
            Text(stringResource(R.string.label_premium_financials), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
fun HighDividendBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
        modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth()
    ) {
        
        Text(
            text = stringResource(R.string.label_high_dividend_alert),
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}


@Composable
fun PeriodReturnBadge(label: String, value: Double?) {
    val color = when {
        value == null -> MaterialTheme.colorScheme.onSurfaceVariant
        value >= 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
        ) {
            
            Text(
                text = if (value != null) "${if (value >= 0) "+" else ""}${String.format(Locale.ENGLISH, "%.0f", value)}%" else "---",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun RowScope.TargetPriceBadge(label: String, price: Double?, color: Color) {
    Surface(
        modifier = Modifier.weight(1f),
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            
            Text(
                text = if (price != null) "฿${String.format(Locale.ENGLISH,"%.1f", price)}" else "---",
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun SignalCard(signal: TradeSignal, zone: TradingZone) {
    val color = when (signal.type) {
        IndicatorSignal.BUY -> MaterialTheme.colorScheme.tertiary
        IndicatorSignal.POTENTIAL -> MaterialTheme.colorScheme.secondary
        IndicatorSignal.SELL -> MaterialTheme.colorScheme.error
        IndicatorSignal.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = when (signal.type) {
                        IndicatorSignal.BUY -> "Buy"
                        IndicatorSignal.POTENTIAL -> "Potential"
                        IndicatorSignal.SELL -> "Sell"
                        IndicatorSignal.NEUTRAL -> "Neutral"
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = color,
                    letterSpacing = (-1).sp
                )
                Surface(
                    color = zone.color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, zone.color.copy(alpha = 0.3f))
                ) {
                    
                    Text(
                        text = zone.label,
                        color = zone.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Text(
                text = signal.reason,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = color.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = signal.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
