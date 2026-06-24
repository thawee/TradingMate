package apincer.mobile.tradings.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import apincer.mobile.tradings.MainActivity
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.domain.TechnicalAnalysis
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext

class TradingMateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = StockDatabase.getDatabase(context)
        val cashBalance = database.cashDao().getCashSync()?.balance ?: 0.0
        val stocks = database.stockDao().getAllStocksSync()

        val portfolioItems = stocks.filter { it.portfolio.quantity > 0 }
        val stockValue = portfolioItems.sumOf { (it.cache?.lastPrice ?: 0.0) * it.portfolio.quantity }
        val totalAssetValue = stockValue + cashBalance

        val totalCost = portfolioItems.sumOf { it.portfolio.cost * it.portfolio.quantity }
        val buyFees = portfolioItems.sumOf { item ->
            if (item.portfolio.buyFees > 0.0) {
                item.portfolio.buyFees
            } else {
                TechnicalAnalysis.calculateFees(item.portfolio.cost * item.portfolio.quantity, false)
            }
        }
        val sellFees = TechnicalAnalysis.calculateFees(stockValue, true)
        val totalFees = buyFees + sellFees

        val grossProfit = stockValue - totalCost
        val netProfitValue = grossProfit - totalFees
        val totalNetProfitPercent = if (totalCost > 0) (netProfitValue / (totalCost + buyFees)) * 100 else 0.0

        val snapshots = database.portfolioSnapshotDao().getAllSnapshotsSync().sortedBy { it.date }
        val graphBitmap = if (snapshots.size >= 2) {
            val color = if (totalNetProfitPercent >= 0) android.graphics.Color.parseColor("#1DE9B6") else android.graphics.Color.parseColor("#FF5252")
            createTrendGraphBitmap(snapshots, 400, 150, color)
        } else null

        val pref = context.getSharedPreferences("trading_mate_settings", Context.MODE_PRIVATE)
        val trailingStopPercent = pref.getFloat("trailing_stop_percent", 5.0f).toDouble()
        val explicitStopLossMap = portfolioItems.associate { it.portfolio.symbol to it.portfolio.stopLoss }
        
        var alertCount = 0
        portfolioItems.forEach { item ->
            val symbol = item.portfolio.symbol
            val lastPrice = item.cache?.lastPrice ?: 0.0
            val peakPrice = item.portfolio.peakPrice
            val stopLoss = explicitStopLossMap[symbol] ?: 0.0

            val dropFromPeak = if (peakPrice > 0) ((lastPrice - peakPrice) / peakPrice) * 100 else 0.0
            if ((dropFromPeak <= -trailingStopPercent && peakPrice > 0) || (stopLoss > 0 && lastPrice <= stopLoss)) {
                alertCount++
            }
        }

        provideContent {
            val lightColors = androidx.glance.material3.ColorProviders(
                light = androidx.compose.material3.lightColorScheme(
                    background = Color(0xFFF8FAFC),
                    onBackground = Color(0xFF0F172A),
                    surface = Color(0xFFFFFFFF),
                    onSurface = Color(0xFF0F172A),
                    surfaceVariant = Color(0xFFF1F5F9),
                    onSurfaceVariant = Color(0xFF475569)
                ),
                dark = androidx.compose.material3.darkColorScheme(
                    background = Color(0xFF0F172A),
                    onBackground = Color(0xFFF1F5F9),
                    surface = Color(0xFF1E293B),
                    onSurface = Color(0xFFF1F5F9),
                    surfaceVariant = Color(0xFF334155),
                    onSurfaceVariant = Color(0xFFCBD5E1)
                )
            )

            GlanceTheme(colors = lightColors) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(16.dp)
                        .padding(16.dp)
                        .clickable(actionStartActivity(
                            android.content.Intent(context, MainActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("START_SCREEN", "PORTFOLIO")
                            }
                        )),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = "TradingMate",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.height(16.dp))
                    
                    // Balance Section
                    Text(
                        text = "Total Assets",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "฿${String.format(Locale.ENGLISH, "%,.2f", totalAssetValue)}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    val profitColor = if (totalNetProfitPercent >= 0) Color(0xFF1DE9B6) else Color(0xFFFF5252)
                    val profitSign = if (totalNetProfitPercent >= 0) "+" else ""
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$profitSign฿${String.format(Locale.ENGLISH, "%,.2f", netProfitValue)}",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(profitColor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "($profitSign${String.format(Locale.ENGLISH, "%.2f", totalNetProfitPercent)}%)",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(profitColor),
                                fontSize = 14.sp
                            )
                        )
                    }

                    if (graphBitmap != null) {
                        Spacer(modifier = GlanceModifier.height(16.dp))
                        Image(
                            provider = ImageProvider(graphBitmap),
                            contentDescription = "Portfolio Trend",
                            modifier = GlanceModifier.fillMaxWidth().height(40.dp)
                        )
                    }

                    if (alertCount > 0) {
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(androidx.glance.unit.ColorProvider(Color(0xFFFF5252).copy(alpha = 0.15f)))
                                .cornerRadius(8.dp)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ $alertCount Active Sell Alert${if (alertCount > 1) "s" else ""}",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFFFF5252)),
                                    fontSize = 12.sp,
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

private fun createTrendGraphBitmap(snapshots: List<apincer.mobile.tradings.data.PortfolioSnapshotEntity>, width: Int, height: Int, color: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        this.color = color
        this.strokeWidth = 4f
        this.style = Paint.Style.STROKE
        this.isAntiAlias = true
    }
    
    val maxVal = snapshots.maxOf { it.totalValue }.toFloat()
    val minVal = snapshots.minOf { it.totalValue }.toFloat()
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val stepX = width.toFloat() / (snapshots.size - 1).coerceAtLeast(1).toFloat()
    val path = Path()

    snapshots.forEachIndexed { i, snapshot ->
        val x = i * stepX
        val y = height * 0.9f - ((snapshot.totalValue.toFloat() - minVal) / range) * (height * 0.8f)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    canvas.drawPath(path, paint)
    
    val fillPaint = Paint().apply {
        this.style = Paint.Style.FILL
        this.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            color and 0x00FFFFFF or 0x40000000, // 25% alpha
            color and 0x00FFFFFF or 0x00000000, // 0% alpha
            Shader.TileMode.CLAMP
        )
    }
    path.lineTo(width.toFloat(), height.toFloat())
    path.lineTo(0f, height.toFloat())
    path.close()
    canvas.drawPath(path, fillPaint)

    return bitmap
}
