package apincer.mobile.tradings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R
import apincer.mobile.tradings.domain.IndicatorSignal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StockViewModel, 
    onSelectStock: (String) -> Unit,
    onOpenEducation: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val cashBalance by viewModel.cashBalance.collectAsState()
    val priceAlertThreshold by viewModel.priceAlertThreshold.collectAsState()
    val isPrivacyMode by viewModel.isPrivacyMode.collectAsState()
    val tradeHistory by viewModel.tradeHistory.collectAsState()
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Surveillance: Stocks near target price (+/- threshold%)
    val targetWatchList = watchlist.filter { 
        it.isFocused && it.focusTargetPrice != null && it.focusTargetPrice > 0.0 &&
        Math.abs(it.info.lastPrice - it.focusTargetPrice) <= (it.focusTargetPrice * (priceAlertThreshold / 100.0))
    }

    val buySignals = watchlist.filter { it.signal?.type == IndicatorSignal.BUY }
    val sellSignals = watchlist.filter { it.signal?.type == IndicatorSignal.SELL && it.portfolio.quantity > 0 }
    val potentialSignals = watchlist.filter { it.signal?.type == IndicatorSignal.POTENTIAL }
    
    val portfolioItems = watchlist.filter { it.portfolio.quantity > 0 }
    val totalStockValue = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
    val totalCost = portfolioItems.sumOf { it.portfolio.cost * it.portfolio.quantity }
    
    val totalEquity = totalStockValue + cashBalance
    val unrealizedProfit = totalStockValue - totalCost
    val realizedProfit = tradeHistory.sumOf { it.netProfitBaht }
    val totalNetProfit = unrealizedProfit + realizedProfit
    val totalNetProfitPercent = if (totalCost > 0) (totalNetProfit / totalCost) * 100 else 0.0

    val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"

    // Dividend Alerts (XD within next 14 days)
    val dividendAlerts = remember(watchlist) {
        val today = Calendar.getInstance()
        val next14Days = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 14) }
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        
        watchlist.filter { 
            it.info.dividendDate?.isNotBlank() == true && try {
                val xdDate = Calendar.getInstance().apply { time = sdf.parse(it.info.dividendDate)!! }
                xdDate.after(today) && xdDate.before(next14Days)
            } catch (e: Exception) { false }
        }
    }

    // Scroll mapping logic using dynamic index calculation matching Pulse order: BUY -> ATTN -> XD -> SELL
    fun scrollToSection(sectionId: String) {
        scope.launch {
            try {
                var targetIndex = 3 // Items before signals: SUMMARY (0), PULSE (1), NAV (2)
                
                if (sectionId == "BUY") {
                    if (buySignals.isNotEmpty()) listState.animateScrollToItem(targetIndex)
                    return@launch
                }
                if (buySignals.isNotEmpty()) targetIndex++
                
                if (sectionId == "ATTN") {
                    if (potentialSignals.isNotEmpty()) listState.animateScrollToItem(targetIndex)
                    return@launch
                }
                if (potentialSignals.isNotEmpty()) targetIndex++

                if (sectionId == "XD") {
                    listState.animateScrollToItem(targetIndex) // XD is always present
                    return@launch
                }
                targetIndex++ 
                
                if (sectionId == "SELL") {
                    if (sellSignals.isNotEmpty()) listState.animateScrollToItem(targetIndex)
                    return@launch
                }
            } catch (e: Exception) { }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "SUMMARY") {
                PortfolioSummaryCard(
                    totalEquity = totalEquity, 
                    cashBalance = cashBalance,
                    stockValue = totalStockValue,
                    netProfit = totalNetProfit,
                    netProfitPercent = totalNetProfitPercent,
                    isPrivacyMode = isPrivacyMode,
                    onTogglePrivacy = { viewModel.togglePrivacyMode() }
                )
            }

            item(key = "PULSE") {
                MarketPulseCard(
                    buyCount = buySignals.size,
                    sellCount = sellSignals.size,
                    potentialCount = potentialSignals.size,
                    dividendAlertCount = dividendAlerts.size,
                    totalCount = watchlist.size,
                    onStatClick = { scrollToSection(it) }
                )
            }

            item(key = "NAV") {
                DiscoveryNavigationRows(onOpenAbout, onOpenEducation)
            }

            // ORDER: BUY -> ATTN -> XD -> SELL -> PRICE
            if (buySignals.isNotEmpty()) {
                item(key = "BUY") {
                    SectionHeader(title = stringResource(R.string.section_buy_signals), icon = Icons.AutoMirrored.Filled.TrendingUp, color = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(buySignals) { item ->
                            SignalQuickCard(item, color = MaterialTheme.colorScheme.tertiary, onClick = { onSelectStock(item.info.symbol) })
                        }
                    }
                }
            }

            if (potentialSignals.isNotEmpty()) {
                item(key = "ATTN") {
                    SectionHeader(title = stringResource(R.string.section_attentions), icon = Icons.Default.Lightbulb, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(potentialSignals) { item ->
                            SignalQuickCard(item, color = MaterialTheme.colorScheme.secondary, onClick = { onSelectStock(item.info.symbol) })
                        }
                    }
                }
            }

            item(key = "XD") {
                SectionHeader(title = stringResource(R.string.label_dividend_alerts), icon = Icons.Default.EventNote, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                if (dividendAlerts.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(dividendAlerts) { item ->
                            DividendQuickCard(item, onClick = { onSelectStock(item.info.symbol) })
                        }
                    }
                } else {
                    EmptyAlertCard(message = stringResource(R.string.label_no_upcoming_dividends), icon = Icons.Default.EventBusy)
                }
            }

            if (sellSignals.isNotEmpty()) {
                item(key = "SELL") {
                    SectionHeader(title = stringResource(R.string.section_exit_alerts), icon = Icons.AutoMirrored.Filled.TrendingDown, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(sellSignals) { item ->
                            SignalQuickCard(item, color = MaterialTheme.colorScheme.error, onClick = { onSelectStock(item.info.symbol) })
                        }
                    }
                }
            }

            if (targetWatchList.isNotEmpty()) {
                item(key = "PRICE") {
                    SectionHeader(title = stringResource(R.string.section_price_alerts), icon = Icons.Default.Star, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(targetWatchList) { item ->
                            TargetAlertCard(item, onClick = { onSelectStock(item.info.symbol) })
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PortfolioSummaryCard(
    totalEquity: Double, 
    cashBalance: Double,
    stockValue: Double,
    netProfit: Double,
    netProfitPercent: Double,
    isPrivacyMode: Boolean,
    onTogglePrivacy: () -> Unit
) {
    val displayTotal = if (isPrivacyMode) "฿ ••••••" else "฿${String.format(Locale.ENGLISH, "%,.0f", totalEquity)}"
    val profitColor = if (netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(text = stringResource(R.string.label_total_equity), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = displayTotal, 
                        fontSize = 36.sp, 
                        fontWeight = FontWeight.Black, 
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )
                }
                
                IconButton(onClick = onTogglePrivacy) {
                    Icon(
                        imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Privacy",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Overall Profit Badge
            Surface(
                color = profitColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, profitColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (netProfit >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = profitColor
                    )
                    Spacer(Modifier.width(6.dp))
                    val profitText = if (isPrivacyMode) "••••" else "฿${String.format(Locale.ENGLISH, "%,.0f", Math.abs(netProfit))}"
                    Text(
                        text = "$profitText (${String.format(Locale.ENGLISH, "%.2f%%", netProfitPercent)})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = profitColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Asset Allocation Bar (Cash vs Stocks)
            if (totalEquity > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    val stockWeight = (stockValue / totalEquity).toFloat()
                    val cashWeight = (cashBalance / totalEquity).toFloat()
                    
                    if (stockValue > 0) Box(modifier = Modifier.fillMaxHeight().weight(stockWeight).background(MaterialTheme.colorScheme.primary))
                    if (cashBalance > 0) Box(modifier = Modifier.fillMaxHeight().weight(cashWeight).background(MaterialTheme.colorScheme.secondary))
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.label_stocks_value), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_cash_balance), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
fun MarketPulseCard(
    buyCount: Int,
    sellCount: Int,
    potentialCount: Int,
    dividendAlertCount: Int,
    totalCount: Int,
    onStatClick: (String) -> Unit
) {
    val sentiment = when {
        buyCount > sellCount -> stringResource(R.string.sentiment_bullish)
        sellCount > buyCount -> stringResource(R.string.sentiment_bearish)
        else -> stringResource(R.string.sentiment_balanced)
    }
    
    val sentimentColor = when {
        buyCount > sellCount -> MaterialTheme.colorScheme.tertiary
        sellCount > buyCount -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = stringResource(R.string.section_market_pulse), 
                    icon = Icons.Default.QueryStats
                )
                
                Surface(
                    color = sentimentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, sentimentColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = sentiment,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = sentimentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            // Breadth Bar
            if (totalCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    val neutralCount = (totalCount - buyCount - sellCount - potentialCount).coerceAtLeast(0)
                    
                    if (buyCount > 0) Box(modifier = Modifier.fillMaxHeight().weight(buyCount.toFloat()).background(MaterialTheme.colorScheme.tertiary))
                    if (potentialCount > 0) Box(modifier = Modifier.fillMaxHeight().weight(potentialCount.toFloat()).background(MaterialTheme.colorScheme.secondary))
                    if (neutralCount > 0) Box(modifier = Modifier.fillMaxHeight().weight(neutralCount.toFloat()).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                    if (sellCount > 0) Box(modifier = Modifier.fillMaxHeight().weight(sellCount.toFloat()).background(MaterialTheme.colorScheme.error))
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStat(stringResource(R.string.section_buy_signals), buyCount, MaterialTheme.colorScheme.tertiary, Icons.AutoMirrored.Filled.TrendingUp, onClick = { onStatClick("BUY") })
                SummaryStat(stringResource(R.string.section_attentions), potentialCount, MaterialTheme.colorScheme.secondary, Icons.Default.Lightbulb, onClick = { onStatClick("ATTN") })
                SummaryStat(stringResource(R.string.label_dividend_alerts), dividendAlertCount, MaterialTheme.colorScheme.primary, Icons.Default.EventNote, onClick = { onStatClick("XD") })
                SummaryStat(stringResource(R.string.section_exit_alerts), sellCount, MaterialTheme.colorScheme.error, Icons.AutoMirrored.Filled.TrendingDown, onClick = { onStatClick("SELL") })
            }
        }
    }
}

@Composable
fun DiscoveryNavigationRows(onOpenAbout: () -> Unit, onOpenEducation: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassCard(
            modifier = Modifier.weight(1f).clickable { onOpenAbout() },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.label_our_story), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp
                )
            }
        }

        GlassCard(
            modifier = Modifier.weight(1f).clickable { onOpenEducation() },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.label_academy), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, count: Int, color: Color, icon: ImageVector, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DividendQuickCard(item: StockWatchlistInfo, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.width(160.dp).clickable { onClick() },
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.info.symbol, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(8.dp))
            Text(text = "฿${item.info.lastPrice}", fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "XD: ${item.info.dividendDate}", 
                fontSize = 11.sp, 
                lineHeight = 14.sp, 
                maxLines = 1, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Yield: ${String.format(Locale.ENGLISH, "%.2f%%", item.info.dividendYield ?: 0.0)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyAlertCard(message: String, icon: ImageVector) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TargetAlertCard(item: StockWatchlistInfo, onClick: () -> Unit) {
    val progress = if (item.focusTargetPrice != null && item.focusTargetPrice > 0) {
        val diff = item.focusTargetPrice - item.info.lastPrice
        val pct = (diff / item.focusTargetPrice) * 100
        pct
    } else 0.0

    GlassCard(
        modifier = Modifier.width(180.dp).clickable { onClick() },
        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = item.info.symbol, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.5).sp)
            Text(text = "฿${item.info.lastPrice}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Target: ฿${item.focusTargetPrice}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            
            val moveColor = if (progress >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Text(
                text = "${if (progress >= 0) "Gap" else "Over"}: ${String.format(Locale.ENGLISH, "%.1f", Math.abs(progress))}%", 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Black,
                color = moveColor
            )
        }
    }
}

@Composable
fun SignalQuickCard(item: StockWatchlistInfo, color: Color, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.width(160.dp).clickable { onClick() },
        containerColor = color.copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.info.symbol, fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = color.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(8.dp))
            Text(text = "฿${item.info.lastPrice}", fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.signal?.reason ?: "", 
                fontSize = 11.sp, 
                lineHeight = 14.sp, 
                maxLines = 2, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
