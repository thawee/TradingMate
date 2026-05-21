package apincer.mobile.tradings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R
import apincer.mobile.tradings.data.ScrapedStockInfo
import apincer.mobile.tradings.data.SetScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendAdvisorScreen(viewModel: StockViewModel) {
    val targetMonthlyIncome by viewModel.targetMonthlyDividend.collectAsState()
    var estYield by remember { mutableStateOf(5.0) } // Default 5%

    val monthly = targetMonthlyIncome
    val yearlyGoal = monthly * 12
    val requiredCapital = if (estYield > 0) yearlyGoal / (estYield / 100.0) else 0.0

    var dividendStocks by remember { mutableStateOf<List<ScrapedStockInfo>>(emptyList()) }
    val watchlist by viewModel.watchlistInfo.collectAsState()
    
    val portfolioItems = watchlist.filter { it.portfolio.quantity > 0 }
    val currentYearlyDividend = portfolioItems.sumOf { 
        it.portfolio.quantity * it.info.lastPrice * ((it.info.dividendYield ?: 0.0) / 100.0)
    }
    val currentMonthlyDividend = currentYearlyDividend / 12.0
    val progress = if (monthly > 0) (currentMonthlyDividend / monthly).toFloat().coerceIn(0f, 1.2f) else 0f
    
    val remainingMonthlyGoal = (monthly - currentMonthlyDividend).coerceAtLeast(0.0)
    val remainingRequiredCapital = if (estYield > 0) (remainingMonthlyGoal * 12) / (estYield / 100.0) else 0.0

    LaunchedEffect(Unit) {
        val symbols = SetScraper.getCuratedCollection("DIVIDEND")
        val stocks = withContext(Dispatchers.IO) {
            symbols.map { SetScraper.fetchStockInfo(it) }
                .filter { (it.dividendYield ?: 0.0) > 0 }
                .sortedByDescending { it.dividendYield }
        }
        dividendStocks = stocks
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.title_advisor), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = stringResource(R.string.section_plan_details), icon = Icons.Default.Calculate)

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(
                            R.string.label_plan_summary,
                            String.format(Locale.ENGLISH, "%,.0f", monthly),
                            String.format(Locale.ENGLISH, "%,.0f", requiredCapital)
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_goal_progress),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.ENGLISH, "%.1f%%", progress * 100),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    LinearProgressIndicator(
                        progress = { progress.coerceAtMost(1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.label_current_monthly_dividend), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("฿${String.format(Locale.ENGLISH, "%,.0f", currentMonthlyDividend)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.label_remaining_capital), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("฿${String.format(Locale.ENGLISH, "%,.0f", remainingRequiredCapital)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.alpha(0.1f))
                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.label_required_capital), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("฿${String.format(Locale.ENGLISH, "%,.0f", requiredCapital)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.label_est_avg_yield), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.ENGLISH, "%.1f", estYield)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (dividendStocks.isNotEmpty()) {
                SectionHeader(title = "Diversification", icon = Icons.Default.PieChart)
                DiversificationComparison(
                    currentStocks = portfolioItems.map { it.info },
                    recommendedStocks = dividendStocks
                )
            }

            SectionHeader(title = stringResource(R.string.section_suggested_stocks), icon = Icons.Default.AutoAwesome)

            if (dividendStocks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                dividendStocks.forEach { stock ->
                    SuggestedStockCard(stock, remainingRequiredCapital / dividendStocks.size, viewModel)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiversificationComparison(currentStocks: List<ScrapedStockInfo>, recommendedStocks: List<ScrapedStockInfo>) {
    val sectorColors = remember {
        mapOf(
            "Financial Services" to Color(0xFF2196F3),
            "Financials" to Color(0xFF2196F3),
            "Communication Services" to Color(0xFF00BCD4),
            "Energy" to Color(0xFFFF9800),
            "Consumer Defensive" to Color(0xFF4CAF50),
            "Real Estate" to Color(0xFF795548),
            "Basic Materials" to Color(0xFF8BC34A),
            "Utilities" to Color(0xFF3F51B5),
            "Industrials" to Color(0xFF607D8B),
            "Consumer Cyclical" to Color(0xFFE91E63),
            "Healthcare" to Color(0xFFF44336)
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // 1. Current Portfolio Distribution
            if (currentStocks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("My Portfolio", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    DistributionBar(stocks = currentStocks, sectorColors = sectorColors)
                }
            }

            // 2. Recommended Distribution
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Recommended Diversification", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                DistributionBar(stocks = recommendedStocks, sectorColors = sectorColors)
            }

            // Shared Legend
            val allStocks = currentStocks + recommendedStocks
            val uniqueSectors = allStocks.map { it.sector ?: "Other" }.distinct()
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uniqueSectors.forEach { sector ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(sectorColors[sector] ?: Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sector,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DistributionBar(stocks: List<ScrapedStockInfo>, sectorColors: Map<String, Color>) {
    val distribution = remember(stocks) {
        stocks.groupBy { it.sector ?: "Other" }
            .mapValues { it.value.size.toFloat() / stocks.size }
            .toList()
            .sortedByDescending { it.second }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        distribution.forEach { (sector, percent) ->
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(percent)
                    .background(sectorColors[sector] ?: Color.Gray)
            )
        }
    }
}

@Composable
fun SuggestedStockCard(stock: ScrapedStockInfo, suggestedCapital: Double, viewModel: StockViewModel) {
    val yield = stock.dividendYield ?: 0.0
    val suggestedShares = if (stock.lastPrice > 0) (suggestedCapital / stock.lastPrice).toInt() else 0

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = { viewModel.fetchStockData(stock.symbol) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stock.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                    if (stock.isFundamentalGood) {
                        Spacer(Modifier.width(4.dp))
                        Text("⭐", fontSize = 10.sp)
                    }
                }
                Text(stock.sector ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(stock.name ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.label_dividend_yield), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text("${String.format(Locale.ENGLISH, "%.2f", yield)}%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
            }

            Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.label_suggested_invest), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text("${String.format(Locale.ENGLISH, "%,d", suggestedShares)} shares", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("฿${String.format(Locale.ENGLISH, "%,.0f", suggestedCapital)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
