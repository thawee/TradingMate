package apincer.mobile.tradings.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.domain.IndicatorSignal
import java.util.Locale
import android.os.Build

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    val border = androidx.compose.foundation.BorderStroke(
        width = 0.5.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.18f)
            ),
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f)
        )
    )

    val animatedModifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = animatedModifier,
            color = containerColor,
            shape = shape,
            border = border,
            interactionSource = interactionSource
        ) {
            Column(content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            color = containerColor,
            shape = shape,
            border = border
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                title?.let {
                    
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                content()

                if (confirmButton != null || dismissButton != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dismissButton?.invoke()
                        Spacer(modifier = Modifier.width(8.dp))
                        confirmButton?.invoke()
                    }
                }
            }
        }
    }
}

@Composable
fun GlassTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun <T> GlassSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelect: (T) -> Unit,
    labelExtractor: (T) -> String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
        shape = CircleShape,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp)
            .border(
                0.5.dp, 
                Brush.verticalGradient(listOf(Color.White.copy(0.4f), Color.White.copy(0.05f))), 
                CircleShape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            else Color.Transparent
                        )
                        .clickable { onItemSelect(item) },
                    contentAlignment = Alignment.Center
                ) {
                    
                    Text(
                        text = labelExtractor(item),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun StockItemCard(
    item: StockWatchlistInfo,
    onSelect: (StockWatchlistInfo) -> Unit,
    onDelete: (StockWatchlistInfo) -> Unit,
    onSell: ((StockWatchlistInfo) -> Unit)? = null,
    onEdit: ((StockWatchlistInfo) -> Unit)? = null,
    isPrivacyMode: Boolean = false
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        val hasPosition = item.portfolio.quantity > 0
        GlassDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = if (hasPosition) "Sell & Remove Stock?" else "Remove from Watchlist?",
            confirmButton = {
                Button(
                    onClick = { 
                        onDelete(item)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (hasPosition) "Sell & Remove" else "Remove", color = Color.White)
                }
            },
            dismissButton = {
                
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        ) {
            if (hasPosition) {
                Text("⚠️ You have ${item.portfolio.quantity} shares of ${item.info.symbol} at cost ฿${"%.2f".format(item.portfolio.cost)}. Removing will auto-record a sale at the current market price (฿${"%.2f".format(item.info.lastPrice)}). This cannot be undone.")
            } else {
                Text("Are you sure you want to remove ${item.info.symbol} from your watchlist?")
            }
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onSelect(item) }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        
                        Text(
                            text = item.info.symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false,
                            letterSpacing = (-0.5).sp
                        )
                        if (item.isFocused) {
                            Spacer(Modifier.width(8.dp))
                            GlassTag(text = "FOCUS", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    
                    Text(
                        text = item.info.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                    
                    val tags = mutableListOf<String>()
                    val isQual = (item.info.roe ?: 0.0) > 15.0
                    if (isQual) tags.add("QUAL")
                    if ((item.info.pe ?: 0.0) in 0.1..15.0 && (item.info.pbv ?: 0.0) in 0.1..1.0) tags.add("VAL")
                    if ((item.info.dividendYield ?: 0.0) >= 5.0) tags.add("DIV")
                    if ((item.portfolio.macdHist ?: 0.0) > 0.0) tags.add("MOM")
                    if (item.signal?.type == IndicatorSignal.BUY || item.signal?.type == IndicatorSignal.POTENTIAL || (item.portfolio.rsi ?: 50.0) < 35.0) tags.add("SUP")
                    if (item.info.percentChange >= 4.0 && (isQual || (item.info.netProfitMargin ?: 0.0) > 10.0)) tags.add("GAP")
                    if ((item.portfolio.rsi ?: 50.0) < 30.0) tags.add("OS")

                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tags.forEach { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.weight(1.2f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isPrivacyMode) "฿••••" else "฿${item.info.lastPrice}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        
                        val changeColor = if (item.info.change >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (item.info.change >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = changeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            
                            Text(
                                text = "${if (item.info.change >= 0) "+" else ""}${String.format(Locale.ENGLISH, "%.2f", item.info.percentChange)}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = changeColor,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    if (item.portfolio.quantity == 0) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (item.portfolio.quantity > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Cost", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isPrivacyMode) "฿••••" else "฿${item.portfolio.cost}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Net Profit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isPrivacyMode) "••••%" else "${if (item.netProfitPercent >= 0) "+" else ""}${String.format(Locale.ENGLISH, "%.2f", item.netProfitPercent)}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (item.netProfitPercent >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Take Profit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        val takeProfitPrice = item.portfolio.cost * 1.10
                        Text(
                            text = if (isPrivacyMode) "฿••••" else "฿${String.format(Locale.ENGLISH, "%.2f", takeProfitPrice)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else if (item.isFocused && item.focusStartPrice != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Start", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isPrivacyMode) "฿••••" else "฿${item.focusStartPrice}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Move", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        val startDiff = ((item.info.lastPrice - item.focusStartPrice) / item.focusStartPrice) * 100
                        Text(
                            text = "${if (startDiff >= 0) "+" else ""}${String.format(Locale.ENGLISH, "%.1f", startDiff)}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = if (startDiff >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sell Target", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isPrivacyMode) "฿••••" else if ((item.focusTargetPrice ?: 0.0) > 0) "฿${String.format(Locale.ENGLISH, "%.2f", item.focusTargetPrice)}" else "---",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val isPortfolioItem = item.portfolio.quantity > 0
            val isSellSignal = item.signal?.type == IndicatorSignal.SELL
            val isSoftSell = isSellSignal && (item.signal?.reason?.contains("Overbought") == true || item.signal?.reason?.contains("Upper Band") == true)
            val showStatusBox = item.signal != null || isPortfolioItem
            
            if (showStatusBox) {
                
                val signalColor = when {
                    isPortfolioItem && isSellSignal && isSoftSell -> MaterialTheme.colorScheme.tertiary
                    isPortfolioItem && isSellSignal -> MaterialTheme.colorScheme.error
                    isPortfolioItem && !isSellSignal -> MaterialTheme.colorScheme.tertiary
                    item.signal?.type == IndicatorSignal.BUY -> MaterialTheme.colorScheme.tertiary
                    item.signal?.type == IndicatorSignal.SELL -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                }
                
                val signalText = when {
                    isPortfolioItem && isSellSignal && !isSoftSell -> "ACTION: SELL"
                    isPortfolioItem && isSellSignal && isSoftSell -> "WARNING"
                    isPortfolioItem && !isSellSignal -> "ACTION: HOLD"
                    else -> item.signal?.type?.name ?: "MONITOR"
                }
                
                val reasonText = when {
                    isPortfolioItem && !isSellSignal -> "Trend is intact. No exit signals triggered."
                    else -> item.signal?.reason ?: "Monitoring price action."
                }
                
                Surface(
                    color = signalColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, signalColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = signalText,
                            color = signalColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = reasonText,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            if (onSell != null && onEdit != null && item.portfolio.quantity > 0) {
                 Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    TextButton(onClick = { onEdit(item) }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    
                    Button(
                        onClick = { onSell(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSellSignal && !isSoftSell) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSellSignal && !isSoftSell) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isSellSignal && !isSoftSell) "Execute Sell" else "Close Trade", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SectionContent(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null && icon != null) {
            SectionHeader(title = title, icon = icon, color = color)
            Spacer(modifier = Modifier.height(16.dp))
        }
        content()
    }
}

@Composable
fun IndicatorRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
    yieldOnCost: Double?,
    isPrivacyMode: Boolean = false,
    onEditCash: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    
                    Text("Total Assets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    Text(
                        text = if (isPrivacyMode) "฿••••" else "฿${String.format(Locale.ENGLISH, "%,.2f", totalAssetValue)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )
                }
                
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape,
                    onClick = onEditCash,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Cash", modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    
                    Text("Stocks Value", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    
                    Text(
                        text = if (isPrivacyMode) "฿••••" else "฿${String.format(Locale.ENGLISH, "%,.2f", stockValue)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    
                    Text("Cash Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    
                    Text(
                        text = if (isPrivacyMode) "฿••••" else "฿${String.format(Locale.ENGLISH, "%,.2f", cashBalance)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            GlassCard(
                containerColor = (if (netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error).copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        
                        Text("Total Net Profit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            
                            Text(
                                text = if (isPrivacyMode) "฿••••" else "฿${String.format(Locale.ENGLISH, "%,.2f", netProfit)}", 
                                fontSize = 20.sp, 
                                fontWeight = FontWeight.Black,
                                color = if (netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            
                            Text(
                                text = if (isPrivacyMode) "(••••%)" else "(${String.format(Locale.ENGLISH, "%,.2f", netPercent)}%)", 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.Bold,
                                color = if (netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    yieldOnCost?.let {
                        Column(horizontalAlignment = Alignment.End) {
                            
                            Text("Avg Yield", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            
                            Text("${String.format(Locale.ENGLISH, "%.2f", it)}%", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                
                Text("Gross: ฿${String.format(Locale.ENGLISH, "%,.2f", grossProfit)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Text("Fees: ฿${String.format(Locale.ENGLISH, "%,.2f", totalFees)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Dynamic background "blobs" for better glass effect and premium feel
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Sophisticated Teal Glow (Primary Action/Trust)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.primary.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(screenWidth.toPx() * -0.1f, screenHeight.toPx() * -0.05f),
                    radius = screenWidth.toPx() * 1.5f
                )
            )
            // Champagne Gold Glow (Wealth/Achievement)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.secondary.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(screenWidth.toPx() * 1.1f, screenHeight.toPx() * 0.15f),
                    radius = screenWidth.toPx() * 1.2f
                )
            )
            // Success Green Glow (Subtle growth hint)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.tertiary.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(screenWidth.toPx() * -0.05f, screenHeight.toPx() * 1.05f),
                    radius = screenWidth.toPx() * 1.4f
                )
            )
            // Soft Surface Bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.primary.copy(alpha = 0.03f), Color.Transparent),
                    center = Offset(screenWidth.toPx() * 1.05f, screenHeight.toPx() * 0.95f),
                    radius = screenWidth.toPx() * 1.1f
                )
            )
        }
        
        content()
    }
}
