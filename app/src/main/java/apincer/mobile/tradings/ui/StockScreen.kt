package apincer.mobile.tradings.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import apincer.mobile.tradings.data.ScrapedStockInfo
import apincer.mobile.tradings.data.StockEntity
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TechnicalAnalysis
import apincer.mobile.tradings.domain.TradeSignal
import apincer.mobile.tradings.domain.TradingZone
import java.util.Locale
import android.os.Build
import androidx.compose.ui.text.style.TextAlign

enum class Screen(val label: String, val icon: ImageVector, val inBottomBar: Boolean = true) {
    HOME("Home", Icons.Default.Dashboard),
    PORTFOLIO("Portfolio", Icons.Default.AccountBalance),
    WATCHLIST("Watchlist", Icons.Default.QueryStats),
    STATS("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings),
    EDUCATION("Academy", Icons.Default.School, false),
    ABOUT("About", Icons.Default.Info, false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(viewModel: StockViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
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
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label, modifier = Modifier.size(24.dp)) },
                                label = { Text(screen.label, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp) },
                                selected = currentScreen == screen,
                                alwaysShowLabel = true,
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
                                        viewModel.addToFocusList(state.stockInfo.symbol, state.stockInfo.lastPrice, target)
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
                                    title = { Text(state.stockInfo.symbol, fontWeight = FontWeight.Black) },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                                    navigationIcon = {
                                        IconButton(onClick = { viewModel.resetToInitial() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = { 
                                                if (state.isFocused) {
                                                    viewModel.removeFromFocusList(state.stockInfo.symbol)
                                                } else {
                                                    showFocusDialog = true
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (state.isFocused) Icons.Default.Star else Icons.Default.StarBorder, 
                                                contentDescription = "Focus",
                                                tint = if (state.isFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                )
                                StockDashboard(state)
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
                                        Button(onClick = { viewModel.resetToInitial() }, shape = RoundedCornerShape(12.dp)) { Text("Back") }
                                    }
                                }
                            }
                        }
                        is StockUiState.Initial -> {
                            when (currentScreen) {
                                Screen.HOME -> HomeScreen(
                                    viewModel = viewModel, 
                                    onSelectStock = { viewModel.fetchStockData(it) }, 
                                    onOpenEducation = { currentScreen = Screen.EDUCATION },
                                    onOpenAbout = { currentScreen = Screen.ABOUT }
                                )
                                Screen.PORTFOLIO -> PortfolioScreen(viewModel, onSelectStock = { viewModel.fetchStockData(it) })
                                Screen.WATCHLIST -> WatchlistScreen(viewModel, onSelectStock = { viewModel.fetchStockData(it) })
                                Screen.SETTINGS -> SettingsScreen(viewModel)
                                Screen.EDUCATION -> TradingEducationScreen(onBack = { currentScreen = Screen.HOME })
                                Screen.STATS -> StatsScreen(viewModel)
                                Screen.ABOUT -> AboutScreen(onBack = { currentScreen = Screen.HOME })
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
        title = "Focus on $symbol",
        confirmButton = {
            Button(
                onClick = { 
                    val target = targetPrice.toDoubleOrNull() ?: 0.0
                    onConfirm(target)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Focus")
            }
        },
        dismissButton = {
            
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            
            Text("Market Price: ฿$currentPrice", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = targetPrice,
                onValueChange = { targetPrice = it },
                label = { Text("Target Price (THB)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            
            Text(
                "We'll track movement from ฿$currentPrice and alert you on Home screen when price hits target (+/- 10%).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StockDashboard(state: StockUiState.Success) {
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
                    
                    Text("Partial Data: Some fundamental metrics could not be fetched.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (state.isFocused) {
            DashboardSection(title = "Focus Tracking", icon = Icons.Default.Star) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        
                        Text("Start Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        
                        Text("฿${String.format(Locale.ENGLISH, "%.2f", state.stockInfo.lastPrice / (1 + (state.returns[3] ?: 0.0)/100))}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        
                        Text("Target Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        
                        Text(if (state.focusTargetPrice > 0) "฿${state.focusTargetPrice}" else "Not Set", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        DashboardSection {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        
                        Text(text = info.symbol, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                        if (!info.nameTH.isNullOrBlank()) {
                            
                            Text(text = info.nameTH, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (state.isVolumeSurge) {
                        GlassTag(text = "VOLUME SURGE", color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                info.name?.let {
                    
                    Text(text = it, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }

                if (info.sector != null) {
                    
                    Text(
                        text = "${info.sector} • ${info.industry}", 
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
                    
                    Text("View Official Quote ↗", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black)
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
                        
                        Text(text = "฿${info.lastPrice}", fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
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
                    
                    Text(text = "Market Cap", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    
                    Text(
                        text = info.marketCap?.let { "฿${formatLargeNumber(it)}" } ?: "N/A",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    
                    Text(text = "Volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    
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
                text = "Last Sync: ${info.lastUpdated}", 
                fontSize = 10.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), 
                modifier = Modifier.padding(top = 12.dp),
                fontWeight = FontWeight.Medium
            )
        }

        DashboardSection(title = "Market Valuation", icon = Icons.Default.QueryStats) {
            IndicatorRow("P/E Ratio", info.pe?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
            IndicatorRow("P/BV Ratio", info.pbv?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
            IndicatorRow("Book Value (P/BV=1.0)", info.bookValue?.let { String.format(Locale.ENGLISH,"฿%.2f", it) } ?: "N/A")
        }

        DashboardSection(title = "Core Financials", icon = Icons.Default.AccountBalance) {
            if (info.isFundamentalGood) {
                QualityStockBadge()
            }
            IndicatorRow("ROE (%)", info.roe?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "N/A")
            IndicatorRow("Net Profit", info.netProfit?.let { "฿${String.format(Locale.ENGLISH,"%,.0f M", it)}" } ?: "N/A")
            IndicatorRow("Net Margin (%)", info.netProfitMargin?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "N/A")
            IndicatorRow("3Y Growth (%)", info.profitGrowth3Y?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "N/A")
            IndicatorRow("EPS", info.eps?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
            IndicatorRow("D/E Ratio", info.debtToEquity?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
        }

        DashboardSection(title = "Dividends", icon = Icons.Default.AutoAwesome) {
            if ((info.dividendYield ?: 0.0) > 4.0) {
                HighDividendBadge()
            }
            IndicatorRow("Dividend Yield (%)", info.dividendYield?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "No Payout")
            IndicatorRow("Last XD Date", info.dividendDate ?: "N/A")

            if (portfolio != null) {
                val yieldOnCost = if (portfolio.cost > 0 && info.dividendYield != null) {
                    (info.dividendYield * info.lastPrice) / portfolio.cost
                } else null

                if (yieldOnCost != null) {
                    IndicatorRow("My Yield on Cost", "${String.format(Locale.ENGLISH,"%.2f", yieldOnCost)}%")
                }
            }
        }

        DashboardSection(title = "Strategy & Zones", icon = Icons.Default.TrackChanges) {
            
            Text(text = "RSI Zones", fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TargetPriceBadge(
                    label = "Buy Below",
                    price = state.buyingPrice,
                    color = MaterialTheme.colorScheme.tertiary
                )
                TargetPriceBadge(
                    label = "Sell Above",
                    price = state.sellingPrice,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Text(
                text = "Targets based on RSI reversal levels (35 Buy / 65 Sell).",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                fontWeight = FontWeight.Medium
            )

            
            Text(text = "Price Return History", fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
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

        DashboardSection(title = "Technical Indicators", icon = Icons.AutoMirrored.Filled.TrendingUp) {
            TechnicalIndicatorItem(
                label = "RSI (14)",
                value = String.format(Locale.ENGLISH,"%.2f", state.rsi ?: 0.0),
                meaning = "Relative Strength Index. < 30 is Cheap, > 70 is Expensive."
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.05f))

            TechnicalIndicatorItem(
                label = "SMA (50 / 200)",
                value = "${String.format(Locale.ENGLISH,"%.2f", state.sma50 ?: 0.0)} / ${String.format(Locale.ENGLISH,"%.2f", state.sma200 ?: 0.0)}",
                meaning = "Trend lines. Price above SMA indicates a strong uptrend."
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.05f))

            TechnicalIndicatorItem(
                label = "Bollinger Bands",
                value = state.bb?.let { "${String.format(Locale.ENGLISH,"%.2f", it.lower)} - ${String.format(Locale.ENGLISH,"%.2f", it.upper)}" } ?: "N/A",
                meaning = "Volatility tube. Price often bounces between these bands."
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

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(vertical = 8.dp)
            .alpha(0.8f)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (prices.size - 1).coerceAtLeast(1)

        val path = androidx.compose.ui.graphics.Path()
        prices.forEachIndexed { i, price ->
            val x = i * stepX
            val y = height - ((price - minPrice) / range * height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
        )

        // Add a subtle gradient fill
        val fillPath = androidx.compose.ui.graphics.Path().apply {
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
fun DashboardSection(title: String? = null, icon: ImageVector? = null, content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (title != null && icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-0.5).sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            content()
        }
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
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StockPortfolioSummaryCard(portfolio: StockEntity, info: ScrapedStockInfo, netProfitPercent: Double) {
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
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
            
            Text("Returns", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
        modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            
            Spacer(Modifier.width(8.dp))
            
            Text("Premium Financials", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
fun HighDividendBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
        modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth()
    ) {
        
        Text(
            text = "High Dividend Yield Alert!",
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
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
        
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
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
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            
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
                    text = signal.type.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = color,
                    letterSpacing = (-1).sp
                )
                Surface(
                    color = zone.color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, zone.color.copy(alpha = 0.3f))
                ) {
                    
                    Text(
                        text = zone.label,
                        color = zone.color,
                        fontSize = 10.sp,
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
