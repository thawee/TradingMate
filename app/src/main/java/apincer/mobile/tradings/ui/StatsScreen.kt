package apincer.mobile.tradings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R
import apincer.mobile.tradings.data.TradeEntity
import apincer.mobile.tradings.domain.TechnicalAnalysis
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StockViewModel,
    showSnackbar: (String) -> Unit
) {
    val history by viewModel.tradeHistory.collectAsState()
    val cashBalance by viewModel.cashBalance.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    val totalProfit = history.sumOf { it.netProfitBaht }
    val beginningCash = cashBalance - totalProfit

    // Period Calculations
    val now = Calendar.getInstance()
    val mtdProfit = history.filter { 
        val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
        cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    }.sumOf { it.netProfitBaht }

    val ytdProfit = history.filter { 
        val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    }.sumOf { it.netProfitBaht }

    val winCount = history.count { it.netProfitBaht > 0 }
    val winRate = if (history.isNotEmpty()) (winCount.toDouble() / history.size) * 100 else 0.0

    // Efficiency Metrics
    val wins = history.filter { it.netProfitBaht > 0 }
    val losses = history.filter { it.netProfitBaht < 0 }
    val avgWin = if (wins.isNotEmpty()) wins.sumOf { it.netProfitBaht } / wins.size else 0.0
    val avgLoss = if (losses.isNotEmpty()) losses.sumOf { it.netProfitBaht } / losses.size else 0.0
    val totalFees = history.sumOf { 
        TechnicalAnalysis.calculateFees(it.buyPrice * it.quantity, false) + 
        TechnicalAnalysis.calculateFees(it.sellPrice * it.quantity, true) 
    }

    val bestTrade = history.maxByOrNull { it.netProfitBaht }

    if (showConfirmDialog) {
        GlassDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = stringResource(R.string.title_clear_history),
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearTradeHistory()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_delete_all), color = Color.White)
                }
            },
            dismissButton = {

                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {

            Text(stringResource(R.string.confirm_clear_history))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.title_history), fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                if (history.isNotEmpty()) {
                    IconButton(onClick = { showConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.desc_clear_history),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Performance Summary Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource(R.string.label_beginning_cash), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text("฿${String.format(Locale.ENGLISH,"%,.2f", beginningCash)}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.label_win_rate), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.ENGLISH,"%,.1f", winRate)}%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp).alpha(0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMetric(stringResource(R.string.label_total_profit), totalProfit)
                            StatMetric(stringResource(R.string.label_this_month), mtdProfit)
                            StatMetric(stringResource(R.string.label_this_year), ytdProfit)
                        }
                    }
                }
            }

            // L4 Journal Analyzer AI Copilot
            if (history.isNotEmpty()) {
                item {
                    val promptBuilder = StringBuilder()
                    promptBuilder.appendLine("Act as my Trading Journal Analyzer.")
                    promptBuilder.appendLine("Review my recent trades below. Identify my top 3 recurring mistakes, my best performing setup type (defined as the trade purpose/setup with the highest win rate and average gain), and the one rule I break most often.")
                    promptBuilder.appendLine("GUARDRAILS & NEGATIVE CONSTRAINTS:")
                    promptBuilder.appendLine("- DO NOT recommend penny stocks or leveraged DW products.")
                    promptBuilder.appendLine("- DO NOT provide direct financial advice; frame all recommendations as educational analysis.")
                    promptBuilder.appendLine("")
                    promptBuilder.appendLine("FORMAT REQUIREMENT:")
                    promptBuilder.appendLine("Output the analysis as a structured Markdown report with the following sections:")
                    promptBuilder.appendLine("### Executive Summary")
                    promptBuilder.appendLine("### Top 3 Psychological/Strategic Mistakes (with examples from my trade list)")
                    promptBuilder.appendLine("### Best Performing Setup Type (based on win rate/avg gain)")
                    promptBuilder.appendLine("### Actionable Psychological Guardrail (one rule to implement next)")
                    promptBuilder.appendLine("")
                    history.take(30).forEach { trade ->
                        val isWin = trade.netProfitBaht > 0
                        val resultStr = if (isWin) "WIN" else "LOSS"
                        promptBuilder.appendLine("- ${trade.symbol}: $resultStr (${String.format(Locale.ENGLISH,"%.2f", trade.netProfitPercent)}%). Lessons: ${trade.note.takeIf { it.isNotBlank() } ?: "None"}")
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Journal Analyzer (L4)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Review your trading psychology and identify mistakes from your recent trades.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Spacer(Modifier.height(16.dp))
                            
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            val context = androidx.compose.ui.platform.LocalContext.current
                            
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(promptBuilder.toString()))
                                    showSnackbar("Prompt copied! Paste it into your AI.")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("AI Prompt")
                            }
                        }
                    }
                }
            }

            // Trading Efficiency Section
            item {
                SectionHeader(title = stringResource(R.string.section_trading_efficiency), icon = Icons.AutoMirrored.Filled.TrendingUp)
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {

                                Text(stringResource(R.string.label_avg_win), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                                Text("฿${String.format(Locale.ENGLISH,"%,.0f", avgWin)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                Text(stringResource(R.string.label_avg_loss), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                                Text("฿${String.format(Locale.ENGLISH,"%,.0f", avgLoss)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                            }
                            Column(horizontalAlignment = Alignment.End) {

                                Text(stringResource(R.string.label_total_fees), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                                Text("฿${String.format(Locale.ENGLISH,"%,.0f", totalFees)}", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        if (bestTrade != null) {
                            HorizontalDivider(modifier = Modifier.alpha(0.05f))
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Text(stringResource(R.string.label_best_performer), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)

                                Text(bestTrade.symbol, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.weight(1f))

                                Text("+฿${String.format(Locale.ENGLISH,"%,.0f", bestTrade.netProfitBaht)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.section_recent_trades), icon = Icons.Default.History)
            }

            if (history.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                            Text(stringResource(R.string.label_no_trade_history), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(history) { trade ->
                    TradeHistoryCard(trade)
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StatMetric(label: String, value: Double) {
    Column {

        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

        Text(
            text = "${if (value >= 0) "+" else ""}฿${String.format(Locale.ENGLISH,"%,.0f", value)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = if (value >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun TradeHistoryCard(trade: TradeEntity) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", LocalLocale.current.platformLocale)
    val dateStr = dateFormat.format(Date(trade.dateMillis))
    val isWin = trade.netProfitBaht > 0

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {

                    Text(text = trade.symbol, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-0.5).sp)

                    Text(text = dateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "฿${trade.buyPrice} → ฿${trade.sellPrice}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {

                    Text(
                        text = "${if (isWin) "+" else ""}฿${String.format(Locale.ENGLISH,"%,.2f", trade.netProfitBaht)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = if (isWin) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = "${String.format(Locale.ENGLISH,"%.2f", trade.netProfitPercent)}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isWin) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )

                    Text(text = "${trade.quantity} Shares", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }

            if (trade.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {

                        Text(text = stringResource(R.string.label_lesson_learned), fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = trade.note,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

