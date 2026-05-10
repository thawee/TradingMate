package apincer.mobile.tradings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
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

enum class Screen(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Dashboard),
    PORTFOLIO("Portfolio", Icons.Default.AccountBalance),
    WATCHLIST("Watchlist", Icons.Default.QueryStats),
    STATS("History", Icons.Default.History),
    LEARN("Academy", Icons.Default.School)
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
                Screen.entries.forEach { screen ->
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            TextButton(onClick = { viewModel.resetToInitial() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back")
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
                            Text("Error: ${state.message}", color = Color.Red)
                            Button(onClick = { viewModel.resetToInitial() }) { Text("Back") }
                        }
                    }
                    is StockUiState.Initial -> {
                        when (currentScreen) {
                            Screen.HOME -> HomeScreen(viewModel, onSelectStock = { viewModel.fetchStockData(it) })
                            Screen.PORTFOLIO -> PortfolioScreen(viewModel, onSelectStock = { viewModel.fetchStockData(it) })
                            Screen.WATCHLIST -> WatchlistScreen(viewModel, onSelectStock = { viewModel.fetchStockData(it) })
                            Screen.STATS -> StatsScreen(viewModel)
                            Screen.LEARN -> TradingEducationScreen(onBack = { currentScreen = Screen.HOME })
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
        // --- 1. Top Strategy Signal ---
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

        // --- 2. Market Data (Public) ---
        DashboardSection(title = "Public Market Data (SET Thai)", icon = Icons.Default.Info) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = info.symbol, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        if (!info.nameTH.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "(${info.nameTH})", fontSize = 18.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                        if (state.isVolumeSurge) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFFF9800).copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.extraSmall,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800))
                            ) {
                                Text(
                                    "VOLUME SURGE", 
                                    color = Color(0xFFE65100), 
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
                            color = Color.DarkGray, 
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "฿${info.lastPrice}", fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${if (info.change >= 0) "+" else ""}${String.format(Locale.ENGLISH,"%.2f", info.change)} (${String.format(Locale.ENGLISH,"%.2f", info.percentChange)}%)",
                        fontSize = 18.sp,
                        color = if (info.change >= 0.0) Color(0xFF00C853) else Color.Red
                    )
                }
                
                if (portfolio != null && portfolio.quantity > 0 && state.netProfitPercent != null) {
                    PortfolioSummaryCard(portfolio, info, state.netProfitPercent)
                }
            }
            
            Text(text = "Last Updated: ${info.lastUpdated}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Fundamental Section
            Text(text = "Fundamentals", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            if (info.isFundamentalGood) {
                QualityStockBadge()
            }

            IndicatorRow("P/E Ratio", info.pe?.toString() ?: "N/A")
            IndicatorRow("P/BV Ratio", info.pbv?.toString() ?: "N/A")
            IndicatorRow("ROE (%)", info.roe?.let { "${it}%" } ?: "N/A")
            IndicatorRow("Net Profit", info.netProfit?.let { "฿${String.format(Locale.ENGLISH,"%,.0f M", it)}" } ?: "N/A")
            IndicatorRow("Net Profit Margin (%)", info.netProfitMargin?.let { "${it}%" } ?: "N/A")
            IndicatorRow("3Y Profit Growth (%)", info.profitGrowth3Y?.let { "${it}%" } ?: "N/A")
            IndicatorRow("EPS", info.eps?.toString() ?: "N/A")
            IndicatorRow("D/E Ratio", info.debtToEquity?.toString() ?: "N/A")
            IndicatorRow("Equity", info.equity?.let { "฿${String.format(Locale.ENGLISH,"%,.0f M", it)}" } ?: "N/A")
            IndicatorRow("Fair Value (P/BV=1.0)", info.bookValue?.let { String.format(Locale.ENGLISH,"฿%.2f", it) } ?: "N/A")
            
            Spacer(modifier = Modifier.height(12.dp))

            // Dividend Section
            Text(text = "Dividend Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            if ((info.dividendYield ?: 0.0) > 4.0) {
                HighDividendBadge()
            }
            IndicatorRow("Dividend Yield (%)", info.dividendYield?.let { "${it}%" } ?: "No Payout")
            IndicatorRow("Last XD Date", info.dividendDate ?: "N/A")

            if (portfolio != null) {
                val yieldOnCost = if (portfolio.cost > 0 && info.dividendYield != null) {
                    (info.dividendYield * info.lastPrice) / portfolio.cost
                } else null
                
                IndicatorRow("Average Cost", "฿${portfolio.cost}")
                IndicatorRow("Holdings", "${portfolio.quantity} Shares")
                if (yieldOnCost != null) {
                    IndicatorRow("My Yield on Cost", "${String.format(Locale.ENGLISH,"%.2f", yieldOnCost)}%")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Analyst & Strategy (Calculated) ---
        DashboardSection(title = "Analyst & Strategy", icon = Icons.Default.Star) {
            // Zone Price Targets
            Text(text = "Zone Price Targets", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TargetPriceBadge(
                    label = "Buy Below",
                    price = state.buyingPrice,
                    color = Color(0xFF00C853)
                )
                TargetPriceBadge(
                    label = "Sell Above",
                    price = state.sellingPrice,
                    color = Color.Red
                )
            }
            Text(
                text = "These targets are calculated based on RSI reversal levels (35 for Buy, 65 for Sell).",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Performance History Section
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

        // --- 4. Technical Indicators (Calculated) ---
        DashboardSection(title = "Technical Indicators", icon = Icons.AutoMirrored.Filled.TrendingUp) {
            Text(
                text = "Indicators help you understand price momentum and volatility 'under the hood'.",
                fontSize = 12.sp,
                color = Color.Gray,
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
fun DashboardSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
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
            color = Color.Gray,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun PortfolioSummaryCard(portfolio: StockEntity, info: ScrapedStockInfo, netProfitPercent: Double) {
    val totalCostRaw = portfolio.cost * portfolio.quantity
    val currentTotalValue = info.lastPrice * portfolio.quantity
    val stockTotalFees = (totalCostRaw + currentTotalValue) * TechnicalAnalysis.THAI_FEE_RATE
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
                color = if (netProfitValue >= 0) Color(0xFF00C853) else Color.Red
            )
            Text("${String.format(Locale.ENGLISH,"%.2f", netProfitPercent)}%", fontSize = 14.sp, color = if (netProfitValue >= 0) Color(0xFF00C853) else Color.Red)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp).alpha(0.2f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gross:", fontSize = 10.sp, color = Color.Gray)
                Text("฿${String.format(Locale.ENGLISH,"%.2f", grossProfit)}", fontSize = 10.sp, color = if (grossProfit >= 0) Color(0xFF00C853) else Color.Red)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Fees:", fontSize = 10.sp, color = Color.Gray)
                Text("-฿${String.format(Locale.ENGLISH,"%.2f", stockTotalFees)}", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun QualityStockBadge() {
    Surface(
        color = Color(0xFFFFD700).copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700)),
        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Solid Financials", color = Color(0xFFC6A700), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
    }
}

@Composable
fun HighDividendBadge() {
    Surface(
        color = Color(0xFFE8F5E9),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)),
        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
    ) {
        Text(
            text = "High Dividend Yield Target!",
            color = Color(0xFF2E7D32),
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
        value == null -> Color.Gray
        value >= 0 -> Color(0xFF00C853)
        else -> Color.Red
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
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
            Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
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
        IndicatorSignal.BUY -> Color(0xFF00C853)
        IndicatorSignal.POTENTIAL -> Color(0xFFC66900)
        IndicatorSignal.SELL -> Color.Red
        IndicatorSignal.NEUTRAL -> Color.Gray
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
                color = Color.DarkGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
