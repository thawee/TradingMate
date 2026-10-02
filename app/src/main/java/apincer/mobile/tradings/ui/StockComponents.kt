package apincer.mobile.tradings.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import apincer.mobile.tradings.domain.IndicatorSignal
import java.util.Locale

@Composable
fun Modifier.shimmerPlaceholder(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val highlightColor = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    return this
        .clip(shape)
        .background(brush)
}

@Composable
fun StockCardSkeleton(modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(width = 64.dp, height = 20.dp).shimmerPlaceholder(RoundedCornerShape(6.dp)))
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(width = 44.dp, height = 16.dp).shimmerPlaceholder(RoundedCornerShape(4.dp)))
                }
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.size(width = 110.dp, height = 14.dp).shimmerPlaceholder(RoundedCornerShape(4.dp)))
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(modifier = Modifier.size(width = 72.dp, height = 20.dp).shimmerPlaceholder(RoundedCornerShape(6.dp)))
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.size(width = 52.dp, height = 16.dp).shimmerPlaceholder(RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable
fun StockDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Skeleton
        GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(width = 100.dp, height = 28.dp).shimmerPlaceholder(RoundedCornerShape(8.dp)))
                    Box(modifier = Modifier.size(width = 90.dp, height = 28.dp).shimmerPlaceholder(RoundedCornerShape(8.dp)))
                }
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).shimmerPlaceholder(RoundedCornerShape(16.dp)))
            }
        }

        // Metrics Grid Skeleton
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f).height(90.dp).shimmerPlaceholder(RoundedCornerShape(20.dp)))
            Box(modifier = Modifier.weight(1f).height(90.dp).shimmerPlaceholder(RoundedCornerShape(20.dp)))
        }

        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).shimmerPlaceholder(RoundedCornerShape(24.dp)))
    }
}

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
    val haptic = LocalHapticFeedback.current

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) Color.White else Color(0xFF0F172A)
    
    val border = androidx.compose.foundation.BorderStroke(
        width = if (isDark) 0.5.dp else 1.0.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                borderColor.copy(alpha = if (isDark) 0.25f else 0.18f),
                borderColor.copy(alpha = if (isDark) 0.08f else 0.08f),
                borderColor.copy(alpha = if (isDark) 0.08f else 0.08f),
                borderColor.copy(alpha = if (isDark) 0.18f else 0.14f)
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
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
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
fun HoldingsSummaryTable(
    items: List<StockWatchlistInfo>
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Symbol",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Avg Cost",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1.1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Text(
                    text = "Mkt Price",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1.1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Text(
                    text = "Unr. P/L",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1.2f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(4.dp))

            items.forEach { item ->
                val avgCost = item.portfolio.cost
                val mktPrice = item.info.lastPrice
                val qty = item.portfolio.quantity
                val costValue = avgCost * qty
                val mktValue = mktPrice * qty
                val unrealizedPL = mktValue - costValue
                val isProfit = unrealizedPL >= 0
                val plColor = if (isProfit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Symbol + Qty badge
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = item.info.symbol,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "(${qty})",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Avg Cost
                    Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.ENGLISH, "%.2f", avgCost),

                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = String.format(Locale.ENGLISH, "(%,.0f)", costValue),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Market Price
                    Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.ENGLISH, "%.2f", mktPrice),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = String.format(Locale.ENGLISH, "(%,.0f)", mktValue),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Unrealized P/L
                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                        val plPct = if (costValue > 0) (unrealizedPL / costValue) * 100.0 else 0.0
                        Text(
                            text = "${if (isProfit) "+" else ""}${String.format(Locale.ENGLISH, "%.1f", plPct)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = plColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${if (isProfit) "+" else ""}${String.format(Locale.ENGLISH, "%,.0f", unrealizedPL)}",
                            fontSize = 10.sp,
                            color = plColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }

            // Totals row
            if (items.isNotEmpty()) {
                val totalCostVal = items.sumOf { it.portfolio.cost * it.portfolio.quantity }
                val totalMktVal = items.sumOf { it.info.lastPrice * it.portfolio.quantity }
                val totalPL = totalMktVal - totalCostVal
                val totalPLPct = if (totalCostVal > 0) (totalPL / totalCostVal) * 100.0 else 0.0
                val isTotalProfit = totalPL >= 0
                val totalColor = if (isTotalProfit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                Spacer(Modifier.height(6.dp))

                /*
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1.1f)
                    )
                    //Spacer(Modifier.weight(2.2f))
                    Text(
                        text = if (isPrivacyMode) "••••" else String.format(Locale.ENGLISH, "%,.0f", totalCostVal),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1.2f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                    Text(
                        text = if (isPrivacyMode) "••••" else String.format(Locale.ENGLISH, "%,.0f", totalMktVal),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1.2f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isPrivacyMode) "••••" else "${if (isTotalProfit) "+" else ""}${String.format(Locale.ENGLISH, "%,.0f", totalPL)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = totalColor
                        )
                        Text(
                            text = if (isPrivacyMode) "••%" else "${if (isTotalProfit) "+" else ""}${String.format(Locale.ENGLISH, "%.1f", totalPLPct)}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = totalColor.copy(alpha = 0.8f)
                        )
                    }
                }*/
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
    showSignalBadge: Boolean = true
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var lastKnownPrice by remember(item.info.symbol) { mutableDoubleStateOf(item.info.lastPrice) }
    var priceTickDirection by remember(item.info.symbol) { mutableIntStateOf(0) }

    LaunchedEffect(item.info.lastPrice) {
        if (item.info.lastPrice > lastKnownPrice && lastKnownPrice > 0.0) {
            priceTickDirection = 1
        } else if (item.info.lastPrice < lastKnownPrice && lastKnownPrice > 0.0) {
            priceTickDirection = -1
        }
        lastKnownPrice = item.info.lastPrice
        if (priceTickDirection != 0) {
            kotlinx.coroutines.delay(1000)
            priceTickDirection = 0
        }
    }

    val tickBgColor by animateColorAsState(
        targetValue = when (priceTickDirection) {
            1 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
            -1 -> MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 350),
        label = "priceTickFlash"
    )

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
                        if (!showSignalBadge && item.signal != null) {
                            Spacer(Modifier.width(6.dp))
                            val signalColor = when (item.signal.type) {
                                IndicatorSignal.BUY -> MaterialTheme.colorScheme.tertiary
                                IndicatorSignal.POTENTIAL -> MaterialTheme.colorScheme.secondary
                                IndicatorSignal.SELL -> MaterialTheme.colorScheme.error
                                else -> null
                            }
                            if (signalColor != null) {
                                Surface(
                                    color = signalColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.signal.type.badgeLabel,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = signalColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = item.info.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                    
                    val score = StockDna.calculateScore(item)
                    val tags = StockDna.tags(item)
                    // Tapping a tag explains it; the short codes were unexplained on cards.
                    var legendTag by remember { mutableStateOf<String?>(null) }
                    ArchetypeLegendDialog(selectedTag = legendTag, onDismiss = { legendTag = null })

                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tags.forEach { tag ->
                                val (bgAlpha, borderAlpha, tagColor) = when (tag) {
                                    "A+" -> Triple(0.18f, 0.4f, Color(0xFF6EE7B7))
                                    "A" -> Triple(0.18f, 0.4f, Color(0xFF60A5FA))
                                    "VCP", "GAP" -> Triple(0.18f, 0.35f, Color(0xFFA78BFA))
                                    "MOAT", "SHIELD" -> Triple(0.18f, 0.35f, Color(0xFF34D399))
                                    "WHALE" -> Triple(0.18f, 0.35f, Color(0xFF38BDF8))
                                    "SPRING", "OS" -> Triple(0.18f, 0.35f, Color(0xFFFBBF24))
                                    else -> Triple(0.10f, 0.0f, MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    onClick = { legendTag = tag },
                                    color = tagColor.copy(alpha = bgAlpha),
                                    shape = RoundedCornerShape(4.dp),
                                    border = if (borderAlpha > 0f) BorderStroke(0.5.dp, tagColor.copy(alpha = borderAlpha)) else null
                                ) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (borderAlpha > 0f) tagColor else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.weight(1.0f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .background(tickBgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "฿${String.format(Locale.ENGLISH, "%.2f", item.info.lastPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        
                        // An unchanged price is neutral, not a gain.
                        val isFlat = kotlin.math.abs(item.info.percentChange) < 0.005
                        val changeColor = when {
                            isFlat -> MaterialTheme.colorScheme.onSurfaceVariant
                            item.info.change > 0 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isFlat) {
                                Icon(
                                    imageVector = if (item.info.change > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = changeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                            }

                            Text(
                                text = "${if (!isFlat && item.info.change > 0) "+" else ""}${String.format(Locale.ENGLISH, "%.2f", if (isFlat) 0.0 else item.info.percentChange)}%",
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

            if (item.portfolio.quantity == 0 && item.info.lastPrice > 0) {
                val isSet50 = apincer.mobile.tradings.domain.TradingConstants.SET50_SYMBOLS.contains(item.info.symbol.uppercase())
                val stopPrice = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateSuggestedStopLossPrice(
                    lastPrice = item.info.lastPrice,
                    atr = item.portfolio.atr,
                    isSet50 = isSet50
                )
                val targetPrice = item.portfolio.portfolio.targetPrice.takeIf { it > item.info.lastPrice }
                val rr = targetPrice?.let {
                    apincer.mobile.tradings.domain.TechnicalAnalysis.calculateRiskRewardRatio(item.info.lastPrice, it, stopPrice)
                }
                val stopPercent = ((stopPrice - item.info.lastPrice) / item.info.lastPrice) * 100

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Plan: Cut < ฿${String.format(Locale.ENGLISH, "%.2f", stopPrice)} (${String.format(Locale.ENGLISH, "%.1f", stopPercent)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                // Only a saved target is shown; a missing one is simply omitted (it used to clip to a bare "Target").
                                if (targetPrice != null) {
                                    Text(
                                        text = "  •  ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Saved target ฿${String.format(Locale.ENGLISH, "%.2f", targetPrice)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (rr != null) {
                                Spacer(Modifier.width(6.dp))
                                val (badgeBg, badgeFg) = when {
                                    rr >= 2.0 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f) to MaterialTheme.colorScheme.onTertiaryContainer
                                    rr >= 1.5 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f) to MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Surface(
                                    color = badgeBg,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Reward:Risk ${String.format(Locale.ENGLISH, "%.1f", rr)}:1",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = badgeFg,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        val bb = item.portfolio.bb
                        val wLow = item.portfolio.week52Low
                        val wHigh = item.portfolio.week52High
                        val rangeText = when {
                            bb != null && bb.lower > 0 && bb.upper > 0 ->
                                "Est. Range: ฿${String.format(Locale.ENGLISH, "%.2f", bb.lower)} - ฿${String.format(Locale.ENGLISH, "%.2f", bb.upper)} (BB ±2σ)"
                            wLow != null && wHigh != null && wLow > 0 && wHigh > 0 ->
                                "52W Range: ฿${String.format(Locale.ENGLISH, "%.2f", wLow)} - ฿${String.format(Locale.ENGLISH, "%.2f", wHigh)}"
                            else -> null
                        }
                        if (rangeText != null) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = rangeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (item.portfolio.quantity > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                val totalCost = item.portfolio.cost * item.portfolio.quantity + item.portfolio.buyFees
                val breakEven = totalCost / (item.portfolio.quantity * (1 - apincer.mobile.tradings.domain.TechnicalAnalysis.THAI_FEE_RATE))
                // The saved plan target; no invented default, so a holding without one says so.
                val takeProfitPrice = item.portfolio.portfolio.targetPrice.takeIf { it > 0.0 }
                val expectedProfit = takeProfitPrice?.let {
                    (it * item.portfolio.quantity * (1 - apincer.mobile.tradings.domain.TechnicalAnalysis.THAI_FEE_RATE)) - totalCost
                } ?: 0.0
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Break-Even", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(
                            text = "฿${String.format(Locale.ENGLISH, "%.2f", breakEven)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Net Profit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${if (item.netProfitPercent >= 0) "+" else ""}${String.format(Locale.ENGLISH, "%.2f", item.netProfitPercent)}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (item.netProfitPercent >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Take Profit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = takeProfitPrice?.let { "฿${String.format(Locale.ENGLISH, "%.2f", it)}" } ?: "No target",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (takeProfitPrice != null) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (expectedProfit > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(+฿${String.format(Locale.ENGLISH, "%,.0f", expectedProfit)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                        }
                    }
                }
                
                if (item.portfolio.stopLoss > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val stopLoss = item.portfolio.stopLoss
                    val expectedLoss = totalCost - (stopLoss * item.portfolio.quantity * (1 - apincer.mobile.tradings.domain.TechnicalAnalysis.THAI_FEE_RATE))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Stop Loss", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "฿${String.format(Locale.ENGLISH, "%.2f", stopLoss)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.error
                                )
                                // Saved before tick rounding: orders cannot sit at this price. Editing the holding snaps it.
                                if (!apincer.mobile.tradings.domain.SetTick.isValid(stopLoss)) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SET ฿${String.format(Locale.ENGLISH, "%.2f", apincer.mobile.tradings.domain.SetTick.ceil(stopLoss))}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(-฿${String.format(Locale.ENGLISH, "%,.0f", expectedLoss)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                            // Once price is through the stop, the planned loss is history: show what selling now costs.
                            val lastPrice = item.info.lastPrice
                            if (lastPrice > 0.0 && lastPrice <= stopLoss) {
                                val lossNow = totalCost - (lastPrice * item.portfolio.quantity * (1 - apincer.mobile.tradings.domain.TechnicalAnalysis.THAI_FEE_RATE))
                                Text(
                                    text = "Stop passed: price ฿${String.format(Locale.ENGLISH, "%.2f", lastPrice)}, selling now loses ฿${String.format(Locale.ENGLISH, "%,.0f", lossNow)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            val isPortfolioItem = item.portfolio.quantity > 0
            val signal = item.signal
            val isSellSignal = signal?.type == IndicatorSignal.SELL
            val isSoftSell = isSellSignal && (signal.reason.contains("Overbought") || signal.reason.contains("Upper Band"))
            val showStatusBox = showSignalBadge && (signal != null || isPortfolioItem)
            
            if (showStatusBox) {
                
                val signalColor = when {
                    isPortfolioItem && isSellSignal && isSoftSell -> MaterialTheme.colorScheme.tertiary
                    isPortfolioItem && isSellSignal -> MaterialTheme.colorScheme.error
                    isPortfolioItem && !isSellSignal -> MaterialTheme.colorScheme.tertiary
                    signal?.type == IndicatorSignal.BUY -> MaterialTheme.colorScheme.tertiary
                    signal?.type == IndicatorSignal.SELL -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                }
                
                val signalText = when {
                    isPortfolioItem && isSellSignal && !isSoftSell -> "ACTION: SELL"
                    isPortfolioItem && isSellSignal && isSoftSell -> "WARNING"
                    isPortfolioItem && !isSellSignal -> "ACTION: HOLD"
                    else -> item.signal?.type?.badgeLabel ?: "MONITOR"
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
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (title != null && icon != null) {
                SectionHeader(title = title, icon = icon, color = color)
                Spacer(modifier = Modifier.height(16.dp))
            }
            content()
        }
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
    totalDividendEarned: Double = 0.0,
    lifetimeReturn: Double = 0.0,
    lifetimeReturnPercent: Double = 0.0,
    profitScopeLabel: String? = null,
    onEditCash: () -> Unit,
    onLogDividend: () -> Unit = {}
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
                        text = "฿${String.format(Locale.ENGLISH, "%,.2f", totalAssetValue)}",
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
                        text = "฿${String.format(Locale.ENGLISH, "%,.2f", stockValue)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    
                    Text("Cash Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    
                    Text(
                        text = "฿${String.format(Locale.ENGLISH, "%,.2f", cashBalance)}",
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Total Net Profit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            profitScopeLabel?.let {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${it} only",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "฿${String.format(Locale.ENGLISH, "%,.2f", netProfit)}", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Black,
                            color = if (netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                        
                        Text(
                            text = "${String.format(Locale.ENGLISH, "%+.2f", netPercent)}%", 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold,
                            color = if (netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    yieldOnCost?.let {
                        Column(horizontalAlignment = Alignment.End) {
                            
                            Text("Avg Yield", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            
                            Text("${String.format(Locale.ENGLISH, "%.2f", it)}%", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            /*
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Lifetime Return", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (isPrivacyMode) "฿••••" else "฿${String.format(java.util.Locale.ENGLISH, "%,.2f", lifetimeReturn)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lifetimeReturn >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPrivacyMode) "(••••%)" else "${String.format(java.util.Locale.ENGLISH, "%+.2f", lifetimeReturnPercent)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lifetimeReturn >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            } */

            /*
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gross: ฿${String.format(Locale.ENGLISH, "%,.2f", grossProfit)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Fees: ฿${String.format(Locale.ENGLISH, "%,.2f", totalFees)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } */

            /*
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dividend Earned: ฿${String.format(Locale.ENGLISH, "%,.2f", totalDividendEarned)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onLogDividend, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("Log Dividend", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } */
        }
    }
}

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Dynamic background "blobs" for better glass effect and premium feel
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            // Sophisticated Teal Glow (Primary Action/Trust)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.primary.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(canvasW * -0.1f, canvasH * -0.05f),
                    radius = canvasW * 1.5f
                )
            )
            // Champagne Gold Glow (Wealth/Achievement)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.secondary.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(canvasW * 1.1f, canvasH * 0.15f),
                    radius = canvasW * 1.2f
                )
            )
            // Success Green Glow (Subtle growth hint)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.tertiary.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(canvasW * -0.05f, canvasH * 1.05f),
                    radius = canvasW * 1.4f
                )
            )
            // Soft Surface Bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.primary.copy(alpha = 0.03f), Color.Transparent),
                    center = Offset(canvasW * 1.05f, canvasH * 0.95f),
                    radius = canvasW * 1.1f
                )
            )
        }
        
        content()
    }
}

data class ChartSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun AllocationDonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    centerTitle: String = "Total Assets",
    centerSubtitle: String = ""
) {
    if (slices.isEmpty()) return
    val total = slices.sumOf { it.value }.coerceAtLeast(0.0001)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 28.dp.toPx()
                var startAngle = -90f

                slices.forEach { slice ->
                    val sweepAngle = ((slice.value / total) * 360f).toFloat()
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerTitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                if (centerSubtitle.isNotBlank()) {
                    Text(
                        text = centerSubtitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Legend grid
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEach { slice ->
                val pct = (slice.value / total) * 100.0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(slice.color, shape = CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${slice.label}: ${"%.1f".format(pct)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Catmull-Rom to Cubic Bezier spline interpolation.
 * Generates smooth C1 continuous curves without sharp elbows or overshoot.
 */
fun Path.addSmoothCubicCurve(points: List<Offset>) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        moveTo(points[0].x, points[0].y)
        return
    }
    moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else p2

        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f

        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f

        cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
}

@Composable
fun MiniSparkline(
    prices: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier.size(width = 64.dp, height = 24.dp)
) {
    if (prices.size < 2) return
    val color = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val minP = prices.minOrNull() ?: 0.0
    val maxP = prices.maxOrNull() ?: 1.0
    val range = (maxP - minP).coerceAtLeast(0.01)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stepX = w / (prices.size - 1).coerceAtLeast(1)
        val points = prices.mapIndexed { i, p ->
            Offset(i * stepX, h - ((p - minP) / range * (h - 4.dp.toPx())).toFloat() - 2.dp.toPx())
        }
        val path = Path().apply { addSmoothCubicCurve(points) }
        val fillPath = Path().apply {
            addSmoothCubicCurve(points)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Evidence notice for entry signals and AI rankings. A distinct coloured card, not faded
 * small text (see tasks/lessons.md #5). Source: tools/backtest/report.md.
 */
@Composable
fun UntestedEdgeNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Info, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Context, not a buy call",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "In a 2015-2025 backtest across SET50 stocks, these technical entry signals " +
                        "trailed simply holding TDEX. Use them to study a stock; build wealth with your index core.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
