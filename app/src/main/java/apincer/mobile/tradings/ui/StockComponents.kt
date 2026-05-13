package apincer.mobile.tradings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.domain.IndicatorSignal
import java.util.Locale

@Composable
fun StockItemCard(
    item: StockWatchlistInfo,
    onSelect: (StockWatchlistInfo) -> Unit,
    onDelete: (StockWatchlistInfo) -> Unit,
    onSell: ((StockWatchlistInfo) -> Unit)? = null,
    onEdit: ((StockWatchlistInfo) -> Unit)? = null
) {
    val info = item.info
    val portfolio = item.portfolio
    var showDeleteConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val totalValue = info.lastPrice * portfolio.quantity
    val profitPercent = item.netProfitPercent
    val profitValue = (profitPercent / 100) * (portfolio.cost * portfolio.quantity)
    
    // Yield on Cost Calculation
    val yieldOnCost = if (portfolio.cost > 0 && info.dividendYield != null) {
        (info.dividendYield * info.lastPrice) / portfolio.cost
    } else null

    val isWatchlistMode = onSell == null && onEdit == null

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove from Watchlist") },
            text = { Text("Are you sure you want to remove ${info.symbol}? This will also delete its portfolio and trade records if any.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        onDelete(item)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(item) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Main Content Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Side: Stock Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = info.symbol, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (portfolio.quantity > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    "IN PORT", 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Dividend Badge
                        if ((info.dividendYield ?: 0.0) > 0.0) {
                            val badgeColor = MaterialTheme.colorScheme.tertiary
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = badgeColor.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.extraSmall,
                                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    "${String.format(Locale.ENGLISH,"%.2f", info.dividendYield)}% YIELD",
                                    color = badgeColor,
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        if (info.isPartialData) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.ErrorOutline, 
                                contentDescription = "Partial Data", 
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    info.name?.let {
                        Text(
                            text = it, 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            maxLines = 1, 
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    // Add Signal Badge if not NEUTRAL
                    item.signal?.let { signal ->
                        if (signal.type != IndicatorSignal.NEUTRAL) {
                            val signalColor = when (signal.type) {
                                IndicatorSignal.BUY -> MaterialTheme.colorScheme.tertiary
                                IndicatorSignal.POTENTIAL -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            }
                            Surface(
                                color = signalColor.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.extraSmall,
                                border = androidx.compose.foundation.BorderStroke(1.dp, signalColor.copy(alpha = 0.5f)),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "SIGNAL: ${signal.type.name}", 
                                    color = signalColor, 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (portfolio.quantity > 0) {
                        Column {
                            Text(
                                text = "${portfolio.quantity} @ ฿${portfolio.cost} | Market: ฿${info.lastPrice}", 
                                fontSize = 11.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (yieldOnCost != null && yieldOnCost != info.dividendYield) {
                                Text(
                                    text = "Yield on Cost: ${String.format(Locale.ENGLISH,"%.2f", yieldOnCost)}%",
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    } else {
                        val peStr = info.pe?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "N/A"
                        val pbvStr = info.pbv?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "N/A"
                        Text(
                            text = "P/E: $peStr | P/BV: $pbvStr", 
                            fontSize = 11.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Show Signal Reason if not NEUTRAL
                    item.signal?.let { signal ->
                        if (signal.type != IndicatorSignal.NEUTRAL) {
                            val signalColor = when (signal.type) {
                                IndicatorSignal.BUY -> MaterialTheme.colorScheme.tertiary
                                IndicatorSignal.POTENTIAL -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            }
                            Text(
                                text = signal.reason, 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = signalColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                // Right Side: Price, Profit, and Actions
                Row(verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (!isWatchlistMode && portfolio.quantity > 0) {
                            // PORTFOLIO VIEW: Show Total Value and Profit
                            Text(text = "Total Value", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "฿${String.format(Locale.ENGLISH,"%,.2f", totalValue)}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                text = "${if (profitValue >= 0) "+" else ""}฿${String.format(Locale.ENGLISH,"%.2f", profitValue)} (${String.format(Locale.ENGLISH,"%.2f", profitPercent)}%)",
                                fontSize = 11.sp,
                                color = if (profitValue >= 0.0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        } else {
                            // WATCHLIST VIEW or not in port: Show Price and Daily Change
                            Text(text = "฿${info.lastPrice}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                text = "${if (info.change >= 0) "+" else ""}${String.format(Locale.ENGLISH,"%.2f", info.change)} (${String.format(Locale.ENGLISH,"%.2f", info.percentChange)}%)",
                                fontSize = 11.sp,
                                color = if (info.change >= 0.0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                            
                            // If in port but on Watchlist page, show personal profit percent subtly
                            if (portfolio.quantity > 0) {
                                Text(
                                    text = "My: ${if (profitPercent >= 0) "+" else ""}${String.format(Locale.ENGLISH,"%.1f", profitPercent)}%",
                                    fontSize = 10.sp,
                                    color = if (profitPercent >= 0.0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    // Trailing Actions (Top Right)
                    Column(horizontalAlignment = Alignment.End) {
                        if (isWatchlistMode) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Delete", 
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), 
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (portfolio.quantity > 0) {
                            if (onSell != null) {
                                IconButton(
                                    onClick = { onSell(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Sell, 
                                        contentDescription = "Sell", 
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), 
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (onEdit != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                IconButton(
                                    onClick = { onEdit(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit, 
                                        contentDescription = "Edit", 
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), 
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioSummaryCard(
    totalAssetValue: Double,
    stockValue: Double,
    cashBalance: Double,
    grossProfit: Double, 
    totalFees: Double, 
    netProfit: Double, 
    netPercent: Double,
    yieldOnCost: Double,
    onEditCash: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Total Assets (Stock + Cash)", fontSize = 14.sp)
            Text(
                text = "฿${String.format(Locale.ENGLISH,"%,.2f", totalAssetValue)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            
            SummaryDetailRow("Stock Value", "฿${String.format(Locale.ENGLISH,"%,.2f", stockValue)}", MaterialTheme.colorScheme.onPrimaryContainer)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Available Cash", fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "฿${String.format(Locale.ENGLISH,"%,.2f", cashBalance)}", 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (onEditCash != null) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = onEditCash,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = "Edit Cash", 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            val profitColor = if (grossProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            SummaryDetailRow("Gross Profit", "฿${String.format(Locale.ENGLISH,"%,.2f", grossProfit)}", profitColor)
            SummaryDetailRow("Total Thai Fees (0.32%)", "-฿${String.format(Locale.ENGLISH,"%,.2f", totalFees)}", MaterialTheme.colorScheme.onSurfaceVariant)
            SummaryDetailRow("Avg Yield on Cost", "${String.format(Locale.ENGLISH,"%.2f", yieldOnCost)}%", MaterialTheme.colorScheme.tertiary)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                val netColor = if (netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                Column(modifier = Modifier.weight(1f)) {
                    Text("Net Realized (Post-Fee)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${if (netProfit >= 0) "+" else ""}฿${String.format(Locale.ENGLISH,"%,.2f", netProfit)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = netColor
                    )
                }
                Text(
                    text = "${String.format(Locale.ENGLISH,"%.2f", netPercent)}%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = netColor
                )
            }
        }
    }
}

@Composable
fun SummaryDetailRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun IndicatorRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
