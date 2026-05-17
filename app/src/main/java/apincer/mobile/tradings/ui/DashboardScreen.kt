package apincer.mobile.tradings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R
import apincer.mobile.tradings.domain.IndicatorSignal
import java.util.Locale

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

    // Surveillance: Stocks near target price (+/- 10%)
    val targetWatchList = watchlist.filter { 
        it.isFocused && it.focusTargetPrice != null && it.focusTargetPrice > 0.0 &&
        Math.abs(it.info.lastPrice - it.focusTargetPrice) <= (it.focusTargetPrice * 0.10)
    }

    val buySignals = watchlist.filter { it.signal?.type == IndicatorSignal.BUY }
    val sellSignals = watchlist.filter { it.signal?.type == IndicatorSignal.SELL && it.portfolio.quantity > 0 }
    val potentialSignals = watchlist.filter { it.signal?.type == IndicatorSignal.POTENTIAL }
    
    val portfolioItems = watchlist.filter { it.portfolio.quantity > 0 }
    val totalStockValue = portfolioItems.sumOf { it.info.lastPrice * it.portfolio.quantity }
    val totalEquity = totalStockValue + cashBalance

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = stringResource(R.string.label_total_equity), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "฿${String.format(Locale.ENGLISH, "%,.0f", totalEquity)}", 
                                fontSize = 36.sp, 
                                fontWeight = FontWeight.Black, 
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.label_active_holdings_count, portfolioItems.size),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).alpha(0.2f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        SectionHeader(
                            title = stringResource(R.string.section_market_summary), 
                            icon = Icons.Default.QueryStats,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryStat(stringResource(R.string.label_total_symbols), watchlist.size, MaterialTheme.colorScheme.primary)
                            SummaryStat(stringResource(R.string.label_to_sell), sellSignals.size, MaterialTheme.colorScheme.error)
                            SummaryStat(stringResource(R.string.label_to_buy), buySignals.size, MaterialTheme.colorScheme.tertiary)
                            SummaryStat(stringResource(R.string.label_attentions), potentialSignals.size, MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. The TradingMate Story Area
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

                    // 2. Trading Academy Area
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

            if (sellSignals.isNotEmpty()) {
                item {
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

            if (buySignals.isNotEmpty()) {
                item {
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
                item {
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

            if (targetWatchList.isNotEmpty()) {
                item {
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
fun SummaryStat(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
