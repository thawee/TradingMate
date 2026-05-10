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
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
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
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = MaterialTheme.shapes.extraSmall,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    "${String.format(Locale.ENGLISH,"%.1f", info.dividendYield)}% YIELD",
                                    color = Color(0xFF2E7D32),
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
                        Text(text = it, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }

                    // Add Signal Badge if not NEUTRAL
                    item.signal?.let { signal ->
                        if (signal.type != IndicatorSignal.NEUTRAL) {
                            val signalColor = when (signal.type) {
                                IndicatorSignal.BUY -> Color(0xFF00C853)
                                IndicatorSignal.POTENTIAL -> Color(0xFFC66900)
                                else -> Color.Red
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
                                color = Color.Gray
                            )
                            if (yieldOnCost != null && yieldOnCost != info.dividendYield) {
                                Text(
                                    text = "Yield on Cost: ${String.format(Locale.ENGLISH,"%.2f", yieldOnCost)}%",
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    } else {
                        Text(text = "P/E: ${info.pe ?: "N/A"} | P/BV: ${info.pbv ?: "N/A"}", fontSize = 11.sp, color = Color.Gray)
                    }
                    
                    // Show Signal Reason if not NEUTRAL
                    item.signal?.let { signal ->
                        if (signal.type != IndicatorSignal.NEUTRAL) {
                            val signalColor = when (signal.type) {
                                IndicatorSignal.BUY -> Color(0xFF00C853)
                                IndicatorSignal.POTENTIAL -> Color(0xFFC66900)
                                else -> Color.Red
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
                            Text(text = "Total Value", fontSize = 10.sp, color = Color.Gray)
                            Text(text = "฿${String.format(Locale.ENGLISH,"%,.2f", totalValue)}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                text = "${if (profitValue >= 0) "+" else ""}฿${String.format(Locale.ENGLISH,"%.2f", profitValue)} (${String.format(Locale.ENGLISH,"%.2f", profitPercent)}%)",
                                fontSize = 11.sp,
                                color = if (profitValue >= 0.0) Color(0xFF00C853) else Color.Red
                            )
                        } else {
                            // WATCHLIST VIEW or not in port: Show Price and Daily Change
                            Text(text = "฿${info.lastPrice}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                text = "${if (info.change >= 0) "+" else ""}${String.format(Locale.ENGLISH,"%.2f", info.change)} (${String.format(Locale.ENGLISH,"%.2f", info.percentChange)}%)",
                                fontSize = 11.sp,
                                color = if (info.change >= 0.0) Color(0xFF00C853) else Color.Red
                            )
                            
                            // If in port but on Watchlist page, show personal profit percent subtly
                            if (portfolio.quantity > 0) {
                                Text(
                                    text = "My: ${if (profitPercent >= 0) "+" else ""}${String.format(Locale.ENGLISH,"%.1f", profitPercent)}%",
                                    fontSize = 10.sp,
                                    color = if (profitPercent >= 0.0) Color(0xFF00C853) else Color.Red,
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
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                            }
                        } else if (portfolio.quantity > 0) {
                            if (onSell != null) {
                                IconButton(
                                    onClick = { onSell(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Sell, contentDescription = "Sell", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                            if (onEdit != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                IconButton(
                                    onClick = { onEdit(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
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
fun IndicatorRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.DarkGray)
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}
