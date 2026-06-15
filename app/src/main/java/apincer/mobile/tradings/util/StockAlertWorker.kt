package apincer.mobile.tradings.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import apincer.mobile.tradings.data.SetScraper
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.data.StockEntity
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TechnicalAnalysis
import kotlinx.coroutines.flow.firstOrNull

class StockAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("StockAlertWorker", "Background work started")

        // Check for Prime Time Reminders (Monday to Friday, Asia/Bangkok time)
        val tz = java.util.TimeZone.getTimeZone("Asia/Bangkok")
        val now = java.util.Calendar.getInstance(tz)
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)
        
        var hasActiveSwingSellAlert = false

        if (dayOfWeek != java.util.Calendar.SATURDAY && dayOfWeek != java.util.Calendar.SUNDAY) {
            val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = now.get(java.util.Calendar.MINUTE)
            val currentTime = hour * 100 + minute

            val prefs = applicationContext.getSharedPreferences("trading_mate_alerts", Context.MODE_PRIVATE)
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(now.time)

            // 2. Afternoon Entry Window: 15:30 - 16:30 PM (Thai time)
            if (currentTime in 1530..1630) {
                val key = "afternoon_alert_$todayStr"
                if (!prefs.getBoolean(key, false)) {
                    NotificationHelper.showPrimeTimeNotification(applicationContext, isMorning = false)
                    prefs.edit().putBoolean(key, true).apply()
                }
            }

            // 3. Dividend Accumulation Season Reminder (January & June)
            val month = now.get(java.util.Calendar.MONTH)
            if (month == java.util.Calendar.JANUARY || month == java.util.Calendar.JUNE) {
                val seasonKey = "dividend_season_${month}_$todayStr"
                if (!prefs.getBoolean(seasonKey, false)) {
                    NotificationHelper.showDividendSeasonNotification(
                        context = applicationContext,
                        isFirstSeason = month == java.util.Calendar.JANUARY
                    )
                    prefs.edit().putBoolean(seasonKey, true).apply()
                }
            }
        }
        
        // 1. Only run notifications during market hours to avoid spamming
        if (TechnicalAnalysis.getMarketStatus() == apincer.mobile.tradings.domain.MarketStatus.CLOSED) {
            Log.d("StockAlertWorker", "Market is closed. Skipping alerts.")
            return Result.success()
        }

        val database = StockDatabase.getDatabase(applicationContext)
        val stockDao = database.stockDao()
        val allStocks = stockDao.getAllStocks().firstOrNull() ?: emptyList()

        if (allStocks.isEmpty()) {
            Log.d("StockAlertWorker", "No stocks in watchlist. Skipping.")
            return Result.success()
        }

        allStocks.forEach { entity ->
            try {
                // 2. Fetch latest data
                val scraped = SetScraper.fetchStockInfo(entity.symbol)
                
                // 3. Calculate new signal
                val indicators = SetScraper.fetchTechnicalIndicators(entity.symbol)
                val signal = TechnicalAnalysis.getDetailedSignal(
                    rsi = indicators.rsi,
                    macdHist = indicators.histogram,
                    lastPrice = scraped.lastPrice,
                    sma50 = indicators.sma50,
                    sma200 = indicators.sma200,
                    bb = indicators.bollingerBands,
                    isVolumeSurge = indicators.isVolumeSurge,
                    userCost = if (entity.quantity > 0) entity.cost else null,
                    isFundamentalGood = scraped.isFundamentalGood,
                    tradePurpose = entity.tradePurpose,
                    dividendYield = scraped.dividendYield,
                    roe = scraped.roe
                )

                // 4. Check for state shift
                val oldSignalType = entity.signalType
                val newSignalType = signal.type.name

                if (newSignalType != oldSignalType && isActionable(signal.type)) {
                    // Trigger notification
                    NotificationHelper.showSignalNotification(
                        context = applicationContext,
                        symbol = entity.symbol,
                        signal = signal.type.name,
                        reason = signal.reason
                    )
                }

                // 4b. Send sell reminder for portfolio stocks with active SELL signal
                if (entity.quantity > 0 && signal.type == IndicatorSignal.SELL) {
                    NotificationHelper.showSellReminderNotification(
                        context = applicationContext,
                        symbol = entity.symbol,
                        reason = signal.reason
                    )
                }

                // 5. Update cache in DB
                stockDao.insertStock(
                    entity.copy(
                        lastPrice = scraped.lastPrice,
                        change = scraped.change,
                        percentChange = scraped.percentChange,
                        roe = scraped.roe,
                        debtToEquity = scraped.debtToEquity,
                        dividendYield = scraped.dividendYield,
                        dividendDate = scraped.dividendDate,
                        rsi = indicators.rsi,
                        macdHist = indicators.histogram,
                        signalType = newSignalType,
                        signalReason = signal.reason,
                        signalDescription = signal.description,
                        lastUpdated = scraped.lastUpdated
                    )
                )

                // 6. Check for upcoming Ex-Dividend (XD) date alerts (next 7 days)
                val xdDateStr = scraped.dividendDate
                if (!xdDateStr.isNullOrEmpty()) {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val xdDate = sdf.parse(xdDateStr)
                        if (xdDate != null) {
                            val diffMs = xdDate.time - System.currentTimeMillis()
                            val diffDays = diffMs / (1000 * 60 * 60 * 24)
                            
                            // If XD is in the next 1 to 7 days
                            if (diffDays >= -1 && diffDays <= 7) {
                                val xdPrefs = applicationContext.getSharedPreferences("trading_mate_alerts", Context.MODE_PRIVATE)
                                val key = "xd_alert_${entity.symbol}_$xdDateStr"
                                if (!xdPrefs.getBoolean(key, false)) {
                                    NotificationHelper.showXdAlertNotification(
                                        context = applicationContext,
                                        symbol = entity.symbol,
                                        dividendDate = xdDateStr
                                    )
                                    xdPrefs.edit().putBoolean(key, true).apply()
                                }
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e("StockAlertWorker", "Failed to parse XD date $xdDateStr for ${entity.symbol}: ${e.message}")
                    }
                }

                // 7. Check if this stock is currently in a swing exit condition
                val isSwingHold = entity.tradePurpose == "SWING"
                val isDividendTransitionHold = entity.tradePurpose == "DIVIDEND" && (scraped.dividendYield ?: 0.0) < 3.0
                
                if (entity.quantity > 0 && (isSwingHold || isDividendTransitionHold)) {
                    val netProfit = TechnicalAnalysis.calculateNetProfitPercent(entity.cost, scraped.lastPrice)
                    val rsi = indicators.rsi ?: 50.0
                    val isSell = signal.type == IndicatorSignal.SELL
                    
                    if (netProfit >= 10.0 || netProfit <= -5.0 || rsi >= 65.0 || isSell) {
                        hasActiveSwingSellAlert = true
                    }
                }
            } catch (e: Exception) {
                Log.e("StockAlertWorker", "Failed to process ${entity.symbol}: ${e.message}")
            }
        }

        // Trigger Morning Swing Exit Alert if we have active swing sell alerts during 10:00 - 11:00 AM
        if (dayOfWeek != java.util.Calendar.SATURDAY && dayOfWeek != java.util.Calendar.SUNDAY) {
            val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = now.get(java.util.Calendar.MINUTE)
            val currentTime = hour * 100 + minute

            if (currentTime in 1000..1100 && hasActiveSwingSellAlert) {
                val prefs = applicationContext.getSharedPreferences("trading_mate_alerts", Context.MODE_PRIVATE)
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(now.time)
                val key = "morning_alert_$todayStr"
                if (!prefs.getBoolean(key, false)) {
                    NotificationHelper.showPrimeTimeNotification(applicationContext, isMorning = true)
                    prefs.edit().putBoolean(key, true).apply()
                }
            }
        }

        return Result.success()
    }

    private fun isActionable(type: IndicatorSignal): Boolean {
        return type == IndicatorSignal.BUY || type == IndicatorSignal.POTENTIAL || type == IndicatorSignal.SELL
    }
}
