package apincer.mobile.tradings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AdvisorFilterExplorer(
    stocks: List<StockWatchlistInfo>, mode: PlaybookMode, bearish: Boolean,
    onStockClick: (String) -> Unit
) {
    var swingMask by rememberSaveable { mutableIntStateOf(AdvisorFilter.defaultMask(PlaybookMode.SWING)) }
    var dividendMask by rememberSaveable { mutableIntStateOf(AdvisorFilter.defaultMask(PlaybookMode.DIVIDEND)) }
    var detail by rememberSaveable(mode) { mutableStateOf<String?>(null) }
    var visibleCount by rememberSaveable(mode) { mutableIntStateOf(10) }
    val mask = if (mode == PlaybookMode.SWING) swingMask else dividendMask
    val result = filterAdvisorStocks(stocks, mode, mask, bearish)
    val updateMask: (Int) -> Unit = { next ->
        if (mode == PlaybookMode.SWING) swingMask = next else dividendMask = next
        visibleCount = 10
    }

    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Explore filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${result.stocks.size} of ${stocks.size} tracked stocks match",
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text("Tap badges to combine rules. Counts run left to right, row by row. Selected = remaining; off = count if added at that stage.",
                style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                result.stages.forEach { stage ->
                    FilterChip(
                        selected = stage.enabled,
                        onClick = {
                            updateMask(mask xor stage.filter.bit)
                            detail = stage.filter.explanation
                        },
                        label = { Text("${stage.filter.label} · ${if (stage.enabled) stage.count.toString() else "off (${stage.count})"}") }
                    )
                }
            }
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { updateMask(AdvisorFilter.defaultMask(mode)); detail = null }) { Text("Reset rules") }
                TextButton(onClick = { updateMask(0); detail = null }) { Text("Clear filters") }
            }
            Text("Research matches only. Entry checks still apply when creating a trade plan.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (result.stocks.isEmpty()) {
                Text("No matches. Turn off a selected badge or clear filters to explore your tracked stocks.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            result.stocks.take(visibleCount).forEach { stock ->
                val signal = stock.signal?.type?.badgeLabel ?: "Signal unavailable"
                val yield = stock.info.dividendYield?.let { " · Yield %.1f%%".format(it) }.orEmpty()
                TextButton(onClick = { onStockClick(stock.info.symbol) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(stock.info.symbol, fontWeight = FontWeight.Bold)
                        Text("$signal$yield", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (result.stocks.size > visibleCount) {
                TextButton(onClick = { visibleCount += 20 }) {
                    Text("Show more (${result.stocks.size - visibleCount} remaining)")
                }
            }
        }
    }
}
