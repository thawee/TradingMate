package apincer.mobile.tradings.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import apincer.mobile.tradings.MainActivity
import apincer.mobile.tradings.data.PortfolioSnapshotEntity
import apincer.mobile.tradings.data.PreferenceRepository
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.domain.TechnicalAnalysis
import apincer.mobile.tradings.domain.TradingConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

private data class WidgetData(
    val totalNetProfitPercent: Double,
    val snapshots: List<PortfolioSnapshotEntity>,
    val alertCount: Int,
    val positionsCount: Int
)

suspend fun notifyWidgetDataChanged(context: Context) {
    try {
        TradingMateWidget().updateAll(context)
    } catch (e: Exception) {
        android.util.Log.e("TradingMateWidget", "Failed to update widget: ${e.message}")
    }
}

class TradingMateWidget : GlanceAppWidget() {
    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = StockDatabase.getDatabase(context)
        val prefRepo = PreferenceRepository(context)

        val widgetData = withContext(Dispatchers.IO) {
            val isAtsEnabled = prefRepo.isAtsEnabled.firstOrNull() ?: true
            prefRepo.coreMix.firstOrNull()?.let { (symbol, pct) ->
                apincer.mobile.tradings.domain.CoreSatellite.configure(symbol, pct)
            }
            val alertPrefs = context.getSharedPreferences("trading_mate_alerts", Context.MODE_PRIVATE)
            val highYieldRanked = apincer.mobile.tradings.domain.MomentumList
                .fromJson(alertPrefs.getString(apincer.mobile.tradings.data.HighYieldRecorder.LIST_KEY, null))
                ?.second?.map { it.symbol.uppercase() }?.toSet()
            val stocks = database.stockDao().getAllStocksSync()

            val portfolioItems = stocks.filter { it.portfolio.quantity > 0 }
            val stockValue = portfolioItems.sumOf { item ->
                val currentPrice = item.cache?.lastPrice?.takeIf { it > 0.0 } ?: item.portfolio.cost
                currentPrice * item.portfolio.quantity
            }

            val totalCost = portfolioItems.sumOf { it.portfolio.cost * it.portfolio.quantity }
            val buyFees = portfolioItems.sumOf { item ->
                if (item.portfolio.buyFees > 0.0) {
                    item.portfolio.buyFees
                } else {
                    val isOddLot = item.portfolio.quantity < apincer.mobile.tradings.domain.HoldingSignal.BOARD_LOT_SHARES
                    TechnicalAnalysis.calculateFees(item.portfolio.cost * item.portfolio.quantity, false, isAtsEnabled || isOddLot)
                }
            }
            val sellFees = portfolioItems.sumOf { item ->
                val currentPrice = item.cache?.lastPrice?.takeIf { it > 0.0 } ?: item.portfolio.cost
                val isOddLot = item.portfolio.quantity < apincer.mobile.tradings.domain.HoldingSignal.BOARD_LOT_SHARES
                TechnicalAnalysis.calculateFees(currentPrice * item.portfolio.quantity, true, isAtsEnabled || isOddLot)
            }
            val totalFees = buyFees + sellFees

            val grossProfit = stockValue - totalCost
            val netProfitValue = grossProfit - totalFees
            val netProfitPercent = if (totalCost > 0) (netProfitValue / (totalCost + buyFees)) * 100 else 0.0

            val snapshots = database.portfolioSnapshotDao().getAllSnapshotsSync().sortedBy { it.date }
            
            var alertCount = 0
            portfolioItems.filter { it.portfolio.quantity >= apincer.mobile.tradings.domain.HoldingSignal.BOARD_LOT_SHARES }.forEach { item ->
                val p = item.portfolio
                val symbol = p.symbol.uppercase()
                val lastPrice = item.cache?.lastPrice?.takeIf { it > 0.0 } ?: p.cost
                val stopLoss = p.stopLoss
                val heldPastStop = stopLoss > 0.0 &&
                    alertPrefs.getString(apincer.mobile.tradings.ui.STOP_ACK_PREFIX + symbol, null)
                        ?.toDoubleOrNull() == stopLoss

                val isStopBreached = stopLoss > 0.0 && lastPrice <= stopLoss && !heldPastStop
                val isCore = apincer.mobile.tradings.domain.CoreSatellite.isCore(symbol)

                val needsReview = when {
                    p.exitPolicy == "FIXED_TARGET" -> {
                        val decision = apincer.mobile.tradings.domain.ExitPolicyEvaluator.evaluate(p.toTradePlan(), lastPrice)
                        decision != null && !(decision.reason == apincer.mobile.tradings.domain.ExitReason.STOP && heldPastStop)
                    }
                    isCore -> isStopBreached
                    p.tradePurpose == "DIVIDEND" -> {
                        val leftRankedList = highYieldRanked != null && symbol !in highYieldRanked
                        isStopBreached || leftRankedList
                    }
                    else -> isStopBreached || (item.signalType == "SELL" && !(item.signalReason == "STOP" && heldPastStop))
                }

                if (needsReview) {
                    alertCount++
                }
            }

            WidgetData(
                totalNetProfitPercent = netProfitPercent,
                snapshots = snapshots,
                alertCount = alertCount,
                positionsCount = portfolioItems.size
            )
        }

        val totalNetProfitPercent = widgetData.totalNetProfitPercent
        val snapshots = widgetData.snapshots
        val alertCount = widgetData.alertCount
        val positionsCount = widgetData.positionsCount

        val isProfit = totalNetProfitPercent >= 0
        val graphColor = if (isProfit) android.graphics.Color.parseColor("#10B981") else android.graphics.Color.parseColor("#F87171")
        val graphBitmap = if (snapshots.size >= 2) {
            createTrendGraphBitmap(snapshots, 500, 160, graphColor)
        } else null

        provideContent {
            val lightColors = androidx.glance.material3.ColorProviders(
                light = androidx.compose.material3.lightColorScheme(
                    background = Color(0xFFF8FAFC),
                    onBackground = Color(0xFF0F172A),
                    surface = Color(0xFFFFFFFF),
                    onSurface = Color(0xFF0F172A),
                    surfaceVariant = Color(0xFFF1F5F9),
                    onSurfaceVariant = Color(0xFF64748B)
                ),
                dark = androidx.compose.material3.darkColorScheme(
                    background = Color(0xFF0F172A),
                    onBackground = Color(0xFFF1F5F9),
                    surface = Color(0xFF1E293B),
                    onSurface = Color(0xFFF1F5F9),
                    surfaceVariant = Color(0xFF334155),
                    onSurfaceVariant = Color(0xFF94A3B8)
                )
            )
            val portfolioAction = actionStartActivity(
                android.content.Intent(context, MainActivity::class.java).apply {
                    action = "apincer.mobile.tradings.widget.OPEN_PORTFOLIO"
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("START_SCREEN", "PORTFOLIO")
                }
            )

            GlanceTheme(colors = lightColors) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(24.dp)
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight().clickable(portfolioAction)
                    ) {
                    // Header Bar with Brand Lockup & Position Pill
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRADINGMATE",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Row(
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.surfaceVariant)
                                .cornerRadius(6.dp)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$positionsCount ${if (positionsCount == 1) "STOCK" else "STOCKS"}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    
                    val profitColor = if (isProfit) Color(0xFF10B981) else Color(0xFFF87171)
                    val profitArrow = if (isProfit) "▲ +" else "▼ -"
                    val formattedPercent = String.format(Locale.ENGLISH, "%.2f", abs(totalNetProfitPercent))
                    
                    // Return Section
                        Text(
                            text = "TOTAL RETURN",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "$profitArrow$formattedPercent%",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(profitColor),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        if (graphBitmap != null) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Image(
                                provider = ImageProvider(graphBitmap),
                                contentDescription = "Portfolio value trend",
                                modifier = GlanceModifier.fillMaxWidth().height(42.dp)
                            )
                        }
                    }

                    // Open the Advisor for holdings with exit signals.
                    if (alertCount > 0) {
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(androidx.glance.unit.ColorProvider(Color(0xFFFF5252).copy(alpha = 0.15f)))
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable(actionStartActivity(
                                    android.content.Intent(context, MainActivity::class.java).apply {
                                        action = "apincer.mobile.tradings.widget.OPEN_ADVISOR"
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        putExtra("START_SCREEN", "ADVISOR")
                                    }
                                )),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ $alertCount holding${if (alertCount == 1) "" else "s"} to review ›",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFFFCA5A5)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(GlanceTheme.colors.surfaceVariant)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable(portfolioAction),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Open Portfolio ›",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

class TradingMateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TradingMateWidget()
}

private fun createTrendGraphBitmap(
    snapshots: List<PortfolioSnapshotEntity>,
    width: Int,
    height: Int,
    color: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val maxVal = snapshots.maxOf { it.totalValue }.toFloat()
    val minVal = snapshots.minOf { it.totalValue }.toFloat()
    val range = (maxVal - minVal).coerceAtLeast(1f)
    val isFlat = maxVal == minVal

    val paddingX = 14f
    val paddingTop = 12f
    val paddingBottom = 16f
    val graphWidth = width - (paddingX * 2)
    val graphHeight = height - paddingTop - paddingBottom

    val points = snapshots.mapIndexed { i, snapshot ->
        val x = paddingX + i * (graphWidth / (snapshots.size - 1).coerceAtLeast(1))
        val y = if (isFlat) {
            height / 2f
        } else {
            (height - paddingBottom) - ((snapshot.totalValue.toFloat() - minVal) / range) * graphHeight
        }
        PointF(x, y)
    }

    if (points.isEmpty()) return bitmap

    val strokePath = Path()
    val fillPath = Path()

    strokePath.moveTo(points[0].x, points[0].y)
    fillPath.moveTo(points[0].x, points[0].y)

    if (points.size == 1) {
        strokePath.lineTo(width - paddingX, points[0].y)
        fillPath.lineTo(width - paddingX, points[0].y)
    } else {
        for (i in 0 until points.size - 1) {
            val p0 = if (i > 0) points[i - 1] else points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i + 2 < points.size) points[i + 2] else p2

            val cp1x = p1.x + (p2.x - p0.x) / 6f
            val cp1y = p1.y + (p2.y - p0.y) / 6f

            val cp2x = p2.x - (p3.x - p1.x) / 6f
            val cp2y = p2.y - (p3.y - p1.y) / 6f

            strokePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
            fillPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        }
    }

    // 1. Fill gradient beneath curve
    fillPath.lineTo(points.last().x, height.toFloat())
    fillPath.lineTo(points.first().x, height.toFloat())
    fillPath.close()

    val fillPaint = Paint().apply {
        this.style = Paint.Style.FILL
        this.isAntiAlias = true
        this.shader = LinearGradient(
            0f, paddingTop, 0f, height.toFloat(),
            color and 0x00FFFFFF or 0x4D000000, // ~30% alpha at top
            color and 0x00FFFFFF or 0x00000000, // 0% alpha at bottom
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawPath(fillPath, fillPaint)

    // 2. Stroke curve with smooth anti-aliased line
    val strokePaint = Paint().apply {
        this.color = color
        this.strokeWidth = 5f
        this.style = Paint.Style.STROKE
        this.strokeCap = Paint.Cap.ROUND
        this.strokeJoin = Paint.Join.ROUND
        this.isAntiAlias = true
    }
    canvas.drawPath(strokePath, strokePaint)

    // 3. Draw glowing beacon dot at latest point
    val lastPoint = points.last()
    val haloPaint = Paint().apply {
        this.color = color and 0x00FFFFFF or 0x33000000 // 20% alpha outer halo
        this.style = Paint.Style.FILL
        this.isAntiAlias = true
    }
    val dotPaint = Paint().apply {
        this.color = color
        this.style = Paint.Style.FILL
        this.isAntiAlias = true
    }
    val whiteCenterPaint = Paint().apply {
        this.color = android.graphics.Color.WHITE
        this.style = Paint.Style.FILL
        this.isAntiAlias = true
    }
    canvas.drawCircle(lastPoint.x, lastPoint.y, 11f, haloPaint)
    canvas.drawCircle(lastPoint.x, lastPoint.y, 5.5f, dotPaint)
    canvas.drawCircle(lastPoint.x, lastPoint.y, 2.5f, whiteCenterPaint)

    return bitmap
}
