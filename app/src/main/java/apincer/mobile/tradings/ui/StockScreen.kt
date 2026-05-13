package apincer.mobile.tradings.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

enum class Screen(val label: String, val icon: ImageVector, val inBottomBar: Boolean = true) {
    HOME("Home", Icons.Default.Dashboard),
    PORTFOLIO("Portfolio", Icons.Default.AccountBalance),
    FOCUS("Focus", Icons.Default.Star),
    WATCHLIST("Watchlist", Icons.Default.QueryStats),
    STATS("History", Icons.Default.History),
    EDUCATION("Academy", Icons.Default.School, false),
    ABOUT("About", Icons.Default.Info, false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(viewModel: StockViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.entries.filter { it.inBottomBar }.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentScreen == screen,
                        onClick = { 
                            currentScreen = screen 
                            viewModel.resetToInitial()
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is StockUiState.Success -> {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { viewModel.resetToInitial() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Back")
                                }
                                
                                TextButton(
                                    onClick = { 
                                        if (state.isFocused) {
                                            viewModel.removeFromFocusList(state.stockInfo.symbol)
                                        } else {
                                            viewModel.addToFocusList(state.stockInfo.symbol, state.stockInfo.lastPrice)
                                        }
                                        viewModel.fetchStockData(state.stockInfo.symbol)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Star, 
                                        contentDescription = "Focus",
                                        tint = if (state.isFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (state.isFocused) "Focused" else "Focus", color = if (state.isFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            StockDashboard(state)
                        }
                    }
                    is StockUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is StockUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.resetToInitial() }) { Text("Back") }
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
                            Screen.FOCUS -> FocusListScreen(viewModel, onSelectStock = { viewModel.fetchStockData(it) })
                            Screen.WATCHLIST -> WatchlistScreen(viewModel, onSelectStock = { viewModel.fetchStockData(it) })
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

@Composable
fun StockDashboard(state: StockUiState.Success) {
    val info = state.stockInfo
    val portfolio = state.portfolio
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        SignalCard(state.signal, state.zone)
        
        if (info.isPartialData) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Partial Data: Some fundamental metrics could not be fetched.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        DashboardSection {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = info.symbol, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        if (!info.nameTH.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "(${info.nameTH})", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                        if (state.isVolumeSurge) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.extraSmall,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(
                                    "VOLUME SURGE", 
                                    color = MaterialTheme.colorScheme.secondary, 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    info.name?.let {
                        Text(text = it, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }

                    if (info.sector != null) {
                        Text(
                            text = "${info.sector} • ${info.industry}", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    TextButton(
                        onClick = { uriHandler.openUri("https://www.set.or.th/th/market/product/stock/quote/${info.symbol}/price") },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("View on SET.or.th ↗", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    info.businessDescription?.let {
                        Text(
                            text = it, 
                            fontSize = 13.sp, 
                            color = MaterialTheme.colorScheme.onSurface, 
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "฿${info.lastPrice}", fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${if (info.change >= 0) "+" else ""}${String.format(Locale.ENGLISH,"%.2f", info.change)} (${String.format(Locale.ENGLISH,"%.2f", info.percentChange)}%)",
                        fontSize = 18.sp,
                        color = if (info.change >= 0.0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
                
                if (portfolio != null && portfolio.quantity > 0 && state.netProfitPercent != null) {
                    StockPortfolioSummaryCard(portfolio, info, state.netProfitPercent)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Market Cap", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(
                        text = info.marketCap?.let { "฿${formatLargeNumber(it)}" } ?: "N/A",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(
                        text = info.volume?.let { formatLargeNumber(it.toDouble()) } ?: "N/A",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (state.historicalPrices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                PriceTrendChart(prices = state.historicalPrices, isPositive = info.change >= 0)
            }
            
            Text(text = "Last Updated: ${info.lastUpdated}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        DashboardSection(title = "Market Valuation", icon = Icons.Default.QueryStats) {
            IndicatorRow("P/E Ratio", info.pe?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
            IndicatorRow("P/BV Ratio", info.pbv?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
            IndicatorRow("Fair Value (P/BV=1.0)", info.bookValue?.let { String.format(Locale.ENGLISH,"฿%.2f", it) } ?: "N/A")
        }

        Spacer(modifier = Modifier.height(16.dp))

        DashboardSection(title = "Business Financials", icon = Icons.Default.AccountBalance) {
            if (info.isFundamentalGood) {
                QualityStockBadge()
            }
            IndicatorRow("ROE (%)", info.roe?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "N/A")
            IndicatorRow("Net Profit", info.netProfit?.let { "฿${String.format(Locale.ENGLISH,"%,.0f M", it)}" } ?: "N/A")
            IndicatorRow("Net Profit Margin (%)", info.netProfitMargin?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "N/A")
            IndicatorRow("3Y Profit Growth (%)", info.profitGrowth3Y?.let { String.format(Locale.ENGLISH,"%.2f%%", it) } ?: "N/A")
            IndicatorRow("EPS", info.eps?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
            IndicatorRow("D/E Ratio", info.debtToEquity?.let { String.format(Locale.ENGLISH,"%.2f", it) } ?: "N/A")
            IndicatorRow("Equity", info.equity?.let { "฿${String.format(Locale.ENGLISH,"%,.0f M", it)}" } ?: "N/A")
        }

        Spacer(modifier = Modifier.height(16.dp))

        DashboardSection(title = "Dividends", icon = Icons.Default.Star) {
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

        Spacer(modifier = Modifier.height(16.dp))

        DashboardSection(title = "Analyst & Strategy", icon = Icons.Default.Star) {
            Text(text = "Zone Price Targets", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                text = "These targets are calculated based on RSI reversal levels (35 for Buy, 65 for Sell).",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Text(text = "Performance History (Price Return)", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PeriodReturnBadge("3D", state.returns[3])
                PeriodReturnBadge("7D", state.returns[7])
                PeriodReturnBadge("15D", state.returns[15])
                PeriodReturnBadge("30D", state.returns[30])
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        DashboardSection(title = "Technical Indicators", icon = Icons.AutoMirrored.Filled.TrendingUp) {
            Text(
                text = "Indicators help you understand price momentum and volatility 'under the hood'.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TechnicalIndicatorItem(
                label = "RSI (14)",
                value = String.format(Locale.ENGLISH,"%.2f", state.rsi ?: 0.0),
                meaning = "Relative Strength Index. Below 30 is 'Cheap' (Oversold), above 70 is 'Expensive' (Overbought)."
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.1f))

            TechnicalIndicatorItem(
                label = "SMA (50 & 200)",
                value = "50: ${String.format(Locale.ENGLISH,"%.2f", state.sma50 ?: 0.0)} | 200: ${String.format(Locale.ENGLISH,"%.2f", state.sma200 ?: 0.0)}",
                meaning = "Moving averages. Price above SMA shows an uptrend. SMA 200 is the 'main trend' for big investors."
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.1f))

            TechnicalIndicatorItem(
                label = "Bollinger Bands",
                value = state.bb?.let { "L: ${String.format(Locale.ENGLISH,"%.2f", it.lower)} | U: ${String.format(Locale.ENGLISH,"%.2f", it.upper)}" } ?: "N/A",
                meaning = "Volatility tube. Price hitting the Lower band often bounces back; hitting the Upper band often pulls back."
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.1f))

            TechnicalIndicatorItem(
                label = "MACD Momentum",
                value = "Hist: ${String.format(Locale.ENGLISH,"%.4f", state.macd.third ?: 0.0)}",
                meaning = "Shows if momentum is increasing (positive) or decreasing (negative). It acts like a gear shift for price."
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
            .height(60.dp)
            .padding(vertical = 4.dp)
            .alpha(0.7f)
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
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

fun formatLargeNumber(number: Double): String {
    return when {
        number >= 1_000_000_000_000 -> String.format(Locale.ENGLISH, "%.2fT", number / 1_000_000_000_000)
        number >= 1_000_000_000 -> String.format(Locale.ENGLISH, "%.2fB", number / 1_000_000_000)
        number >= 1_000_000 -> String.format(Locale.ENGLISH, "%.2fM", number / 1_000_000)
        else -> String.format(Locale.ENGLISH, "%,.0f", number)
    }
}

@Composable
fun DashboardSection(title: String? = null, icon: ImageVector? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null && icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    @Suppress("DEPRECATION")
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = meaning,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 2.dp)
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

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.padding(start = 12.dp).width(160.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.End) {
            Text("My Return", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "฿${String.format(Locale.ENGLISH,"%,.2f", netProfitValue)}",
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 18.sp,
                color = if (netProfitValue >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
            Text("${String.format(Locale.ENGLISH,"%.2f", netProfitPercent)}%", fontSize = 14.sp, color = if (netProfitValue >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp).alpha(0.2f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                @Suppress("DEPRECATION")
                Text("Gross:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("฿${String.format(Locale.ENGLISH,"%,.2f", grossProfit)}", fontSize = 10.sp, color = if (grossProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                @Suppress("DEPRECATION")
                Text("Fees:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("-฿${String.format(Locale.ENGLISH,"%,.2f", stockTotalFees)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun QualityStockBadge() {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Solid Financials", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
    }
}

@Composable
fun HighDividendBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
    ) {
        Text(
            text = "High Dividend Yield Target!",
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(4.dp)
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
        @Suppress("DEPRECATION")
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.small,
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Text(
                text = if (value != null) "${if (value >= 0) "+" else ""}${value}%" else "---",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun RowScope.TargetPriceBadge(label: String, price: Double?, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            @Suppress("DEPRECATION")
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(
                text = if (price != null) "฿${String.format(Locale.ENGLISH,"%.2f", price)}" else "---",
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
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

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.3f)))
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
                    fontSize = 24.sp,
                    color = color
                )
                Surface(
                    color = zone.color.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                    border = androidx.compose.foundation.BorderStroke(1.dp, zone.color.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = zone.label,
                        color = zone.color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = signal.reason,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = signal.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
