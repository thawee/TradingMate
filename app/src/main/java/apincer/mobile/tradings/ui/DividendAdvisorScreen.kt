package apincer.mobile.tradings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var targetMonthlyIncome by remember { mutableStateOf("10000") }
    var yearsToRetirement by remember { mutableStateOf("10") }
    var estYield by remember { mutableStateOf(5.0) } // Default 5%

    val monthly = targetMonthlyIncome.toDoubleOrNull() ?: 0.0
    val years = yearsToRetirement.toIntOrNull() ?: 0
    val yearlyGoal = monthly * 12
    val requiredCapital = if (estYield > 0) yearlyGoal / (estYield / 100.0) else 0.0

    var dividendStocks by remember { mutableStateOf<List<ScrapedStockInfo>>(emptyList()) }
    val watchlist by viewModel.watchlistInfo.collectAsState()

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
            SectionHeader(title = stringResource(R.string.section_dividend_goal), icon = Icons.Default.Flag)

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = targetMonthlyIncome,
                        onValueChange = { targetMonthlyIncome = it },
                        label = { Text(stringResource(R.string.label_target_monthly_dividend)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text("฿ ") },
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = yearsToRetirement,
                        onValueChange = { yearsToRetirement = it },
                        label = { Text(stringResource(R.string.label_years_to_retirement)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text(" Years") },
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

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
                            years,
                            String.format(Locale.ENGLISH, "%,.0f", requiredCapital)
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.label_required_capital), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("฿${String.format(Locale.ENGLISH, "%,.0f", requiredCapital)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.label_est_avg_yield), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.ENGLISH, "%.1f", estYield)}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.section_suggested_stocks), icon = Icons.Default.AutoAwesome)

            if (dividendStocks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                dividendStocks.forEach { stock ->
                    SuggestedStockCard(stock, requiredCapital / dividendStocks.size, viewModel)
                }
            }

            Spacer(Modifier.height(40.dp))
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
                Text(stock.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
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
