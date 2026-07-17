package apincer.mobile.tradings.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import apincer.mobile.tradings.data.SetScraper
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.data.StockAggregate
import apincer.mobile.tradings.data.StockRepository
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TechnicalAnalysis
import kotlinx.coroutines.flow.firstOrNull
import androidx.glance.appwidget.updateAll

class StockAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        // Year-round dividend yield opportunity alert thresholds.
        // Fire when a DIVIDEND-purpose stock's yield rises to this level
        // (price fell → yield rose) AND fundamentals remain solid.
        const val YIELD_OPPORTUNITY_THRESHOLD = 5.0   // % — attractive yield for Thai SET
        const val ROE_MIN_THRESHOLD           = 15.0  // % — minimum quality filter
    }

    override suspend fun doWork(): Result {
        Log.d("StockAlertWorker", "Background work started")

        val tz = java.util.TimeZone.getTimeZone("Asia/Bangkok")
        val now = java.util.Calendar.getInstance(tz)
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = now.get(java.util.Calendar.MINUTE)
        val currentTime = hour * 100 + minute

        val prefRepo = apincer.mobile.tradings.data.PreferenceRepository(applicationContext)
        val trailingStopPercent = prefRepo.trailingStopPercent.firstOrNull() ?: 5.0
        val alertPrefs = applicationContext.getSharedPreferences("trading_mate_alerts", Context.MODE_PRIVATE)

        var hasActiveSwingSellAlert = false

        // 1. Only run time-sensitive notifications on trading weekdays
        if (dayOfWeek != java.util.Calendar.SATURDAY && dayOfWeek != java.util.Calendar.SUNDAY) {
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(now.time)
            val marketStatus = TechnicalAnalysis.getMarketStatus()

            // 2. Afternoon Entry Window: 15:30–16:15 Thai time
            //    Cap at 16:15 (not 16:30) so user has ~15 min to act before market closes.
            //    Also guard against public holidays by checking market is not CLOSED.
            if (currentTime in 1530..1615 && marketStatus != apincer.mobile.tradings.domain.MarketStatus.CLOSED) {
                val key = "afternoon_alert_$todayStr"
                if (!alertPrefs.getBoolean(key, false)) {
                    NotificationHelper.showPrimeTimeNotification(applicationContext, isMorning = false)
                    alertPrefs.edit().putBoolean(key, true).apply()
                    // Set flag for Step 2 badge
                    alertPrefs.edit().putBoolean("afternoon_scan_available_$todayStr", true).apply()
                }
            }

            // 3. Dividend Accumulation Season Reminder (January & June)
            //    - January  → accumulate before April/May XD (First-Half payouts)
            //    - June     → accumulate before August/September XD (Second-Half payouts)
            //    Fires ONCE per season (keyed by year-month, not date) during business hours.
            val month = now.get(java.util.Calendar.MONTH)
            val year = now.get(java.util.Calendar.YEAR)
            if ((month == java.util.Calendar.JANUARY || month == java.util.Calendar.JUNE)
                && currentTime in 900..1700) {
                val yearMonth = "$year-${month + 1}"   // e.g. "2026-1" or "2026-6"
                val seasonKey = "dividend_season_$yearMonth"
                if (!alertPrefs.getBoolean(seasonKey, false)) {
                    NotificationHelper.showDividendSeasonNotification(
                        context = applicationContext,
                        isFirstSeason = month == java.util.Calendar.JANUARY
                    )
                    alertPrefs.edit().putBoolean(seasonKey, true).apply()
                }
            }
        }

        // 4. Skip stock-scan alerts when market is fully closed (includes public holidays)
        if (TechnicalAnalysis.getMarketStatus() == apincer.mobile.tradings.domain.MarketStatus.CLOSED) {
            Log.d("StockAlertWorker", "Market is closed. Skipping stock scan alerts.")
            return Result.success()
        }

        val database = StockDatabase.getDatabase(applicationContext)
        val repository = StockRepository(
            database, 
            database.stockDao(), 
            database.tradeDao(), 
            database.cashDao(), 
            database.focusDao(), 
            database.checklistDao(),
            database.dividendDao(),
            database.portfolioSnapshotDao(),
            database.cashTransactionDao()
        )
        val allStocks = repository.allStocks.firstOrNull() ?: emptyList()

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
                    userQuantity = if (entity.quantity > 0) entity.quantity else null,
                    isFundamentalGood = false,
                    tradePurpose = entity.tradePurpose,
                    dividendYield = scraped.dividendYield,
                    roe = scraped.roe
                )

                // 4. Check for state shift (entry opportunities only)
                val oldSignalType = entity.signalType
                val newSignalType = signal.type.name
                val isSignalShift = newSignalType != oldSignalType

                val dummyInfo = apincer.mobile.tradings.ui.StockWatchlistInfo(
                    info = scraped,
                    portfolio = entity,
                    signal = signal
                )

                if (isSignalShift && shouldNotifyEntrySignal(entity.quantity, signal.type, dummyInfo)) {
                    NotificationHelper.showSignalNotification(
                        context = applicationContext,
                        symbol = entity.symbol,
                        signal = signal.type.name,
                        reason = signal.reason
                    )
                }

                var sellReason: String? = null
                if (entity.quantity > 0 && signal.type == IndicatorSignal.SELL) {
                    sellReason = signal.reason
                }

                // 5. Update cache in DB
                val cache = entity.cache ?: apincer.mobile.tradings.data.StockCacheEntity(entity.symbol)
                repository.updateStockCache(
                    cache.copy(
                        lastPrice = scraped.lastPrice,
                        change = scraped.change,
                        percentChange = scraped.percentChange,
                        roe = scraped.roe,
                        debtToEquity = scraped.debtToEquity,
                        dividendYield = scraped.dividendYield,
                        dividendDate = scraped.dividendDate,
                        volume = scraped.volume ?: cache.volume,
                        lastUpdated = scraped.lastUpdated
                    )
                )
                val sig = entity.signal ?: apincer.mobile.tradings.data.StockSignalEntity(entity.symbol)
                repository.updateStockSignal(
                    sig.copy(
                        rsi = indicators.rsi,
                        macdHist = indicators.histogram,
                        signalType = newSignalType,
                        signalReason = signal.reason,
                        signalDescription = signal.description,
                        lastUpdated = scraped.lastUpdated
                    )
                )

                // 5b. Update peak price for trailing stop loss
                if (entity.quantity > 0 && scraped.lastPrice > entity.portfolio.peakPrice) {
                    repository.updatePortfolio(entity.portfolio.copy(peakPrice = scraped.lastPrice))
                }

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
                                val key = "xd_alert_${entity.symbol}_$xdDateStr"
                                if (!alertPrefs.getBoolean(key, false)) {
                                    NotificationHelper.showXdAlertNotification(
                                        context = applicationContext,
                                        symbol = entity.symbol,
                                        dividendDate = xdDateStr
                                    )
                                    alertPrefs.edit().putBoolean(key, true).apply()
                                }
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e("StockAlertWorker", "Failed to parse XD date $xdDateStr for ${entity.symbol}: ${e.message}")
                    }
                }

                // 6b. Year-round yield opportunity alert (any month)
                // Fires when a DIVIDEND-purpose stock's yield spikes ≥ YIELD_OPPORTUNITY_THRESHOLD
                // due to price weakness, AND fundamentals are solid (ROE ≥ ROE_MIN_THRESHOLD).
                // Deduplicates by ISO week — fires at most once per week per stock.
                val dividendYield = scraped.dividendYield ?: 0.0
                val roe = scraped.roe ?: 0.0
                if (entity.tradePurpose == "DIVIDEND"
                    && dividendYield >= YIELD_OPPORTUNITY_THRESHOLD
                    && roe >= ROE_MIN_THRESHOLD) {
                    val weekOfYear = now.get(java.util.Calendar.WEEK_OF_YEAR)
                    val weekYear  = now.get(java.util.Calendar.YEAR)
                    val yieldKey  = "yield_opp_${entity.symbol}_${weekYear}_W${weekOfYear}"
                    if (!alertPrefs.getBoolean(yieldKey, false)) {
                        NotificationHelper.showDividendYieldOpportunityNotification(
                            context = applicationContext,
                            symbol  = entity.symbol,
                            yield   = dividendYield,
                            price   = scraped.lastPrice,
                            roe     = roe
                        )
                        alertPrefs.edit().putBoolean(yieldKey, true).apply()
                    }
                }

                // 7. Check if this stock is currently in a swing exit condition
                val isSwingHold = entity.tradePurpose == "SWING"
                val isDividendTransitionHold = entity.tradePurpose == "DIVIDEND" && (scraped.dividendYield ?: 0.0) < 3.0
                
                if (entity.quantity > 0 && (isSwingHold || isDividendTransitionHold)) {
                    val netProfit = TechnicalAnalysis.calculateNetProfitPercent(entity.cost, scraped.lastPrice)
                    val netProfitBaht = TechnicalAnalysis.calculateNetProfitBaht(entity.cost, scraped.lastPrice, entity.quantity)
                    val rsi = indicators.rsi ?: 50.0
                    val isSell = signal.type == IndicatorSignal.SELL
                    
                    val currentPrice = scraped.lastPrice
                    val cost = entity.cost
                    val peakPrice = entity.portfolio.peakPrice
                    val explicitStopLoss = entity.portfolio.stopLoss
                    val maxPeak = maxOf(cost, peakPrice)
                    val dropFromPeak = if (maxPeak > 0) ((currentPrice - maxPeak) / maxPeak) * 100 else 0.0
                    val trailingBreached = dropFromPeak <= -trailingStopPercent
                    val explicitStopBreached = explicitStopLoss > 0 && currentPrice <= explicitStopLoss

                    // Fix #2: Scale absolute threshold with position size (at least 3% of position, min ₿500)
                    val positionValue = cost * entity.quantity
                    val minTakeProfitBaht = maxOf(500.0, positionValue * 0.03)

                    if (explicitStopBreached) {
                        sellReason = "Stop Loss hit at ฿${String.format(java.util.Locale.ENGLISH, "%.2f", explicitStopLoss)} (current ฿${String.format(java.util.Locale.ENGLISH, "%.2f", currentPrice)})."
                    } else if (trailingBreached) {
                        sellReason = "Trailing stop breached (${String.format(java.util.Locale.ENGLISH, "%.2f", dropFromPeak)}% from peak, limit ${String.format(java.util.Locale.ENGLISH, "%.2f", trailingStopPercent)}%)."
                    } else if (netProfit >= 5.0 || netProfitBaht >= minTakeProfitBaht) {
                        // Fix #4: Send take-profit as a specific sell notification
                        sellReason = "Take Profit: +${String.format(java.util.Locale.ENGLISH, "%.1f", netProfit)}% (฿${String.format(java.util.Locale.ENGLISH, "%,.0f", netProfitBaht)})"
                    }
                    
                    if (netProfit >= 5.0 || netProfitBaht >= minTakeProfitBaht || trailingBreached || explicitStopBreached || rsi >= 65.0 || isSell) {
                        hasActiveSwingSellAlert = true
                    }
                }

                if (entity.quantity > 0 && !sellReason.isNullOrBlank()) {
                    maybeSendSellReminder(
                        prefs = alertPrefs,
                        symbol = entity.symbol,
                        reason = sellReason,
                        now = now
                    )
                }
            } catch (e: Exception) {
                Log.e("StockAlertWorker", "Failed to process ${entity.symbol}: ${e.message}")
            }
        }

        // 5. Morning Swing Exit Alert: 10:00–11:00 AM, only if active sell conditions exist
        if (dayOfWeek != java.util.Calendar.SATURDAY && dayOfWeek != java.util.Calendar.SUNDAY) {
            if (currentTime in 1000..1100 && hasActiveSwingSellAlert) {
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(now.time)
                val key = "morning_alert_$todayStr"
                if (!alertPrefs.getBoolean(key, false)) {
                    NotificationHelper.showPrimeTimeNotification(applicationContext, isMorning = true)
                    alertPrefs.edit().putBoolean(key, true).apply()
                }
            }
        }

        // Calculate portfolio snapshot
        var totalValue = 0.0
        var totalCost = 0.0
        allStocks.forEach { item ->
            val cache = item.cache
            val currentPrice = cache?.lastPrice ?: item.portfolio.cost
            val qty = item.portfolio.quantity
            totalValue += currentPrice * qty
            totalCost += item.portfolio.cost * qty + item.portfolio.buyFees
        }
        val cashBalance = repository.cashBalance.firstOrNull()?.balance ?: 0.0
        
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(now.time)
        val snapshot = apincer.mobile.tradings.data.PortfolioSnapshotEntity(
            date = todayStr,
            totalValue = totalValue,
            totalCost = totalCost,
            cashBalance = cashBalance
        )
        repository.insertSnapshot(snapshot)
        
        // Clean up old snapshots (e.g., older than 30 days)
        val thirtyDaysAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -30) }
        val oldDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(thirtyDaysAgo.time)
        repository.deleteOldSnapshots(oldDateStr)

        try {
            apincer.mobile.tradings.widget.TradingMateWidget().updateAll(applicationContext)
        } catch (e: Exception) {
            Log.e("StockAlertWorker", "Failed to update widget: ${e.message}")
        }

        return Result.success()
    }

    private fun shouldNotifyEntrySignal(quantity: Int, type: IndicatorSignal, s: apincer.mobile.tradings.ui.StockWatchlistInfo): Boolean {
        val isEntrySignal = type == IndicatorSignal.BUY || type == IndicatorSignal.POTENTIAL
        if (quantity > 0 || !isEntrySignal) return false
        
        // STRICT ALERTS: Only notify if it passes the Advisor Screen's VIP gates
        return apincer.mobile.tradings.ui.StockDna.isLiquid(s) && apincer.mobile.tradings.ui.StockDna.isQual(s)
    }

    private fun maybeSendSellReminder(
        prefs: SharedPreferences,
        symbol: String,
        reason: String,
        now: java.util.Calendar
    ) {
        val day = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(now.time)
        val dedupeKey = "sell_alert_${symbol}_${day}"
        if (prefs.getBoolean(dedupeKey, false)) return

        NotificationHelper.showSellReminderNotification(
            context = applicationContext,
            symbol = symbol,
            reason = reason
        )
        prefs.edit().putBoolean(dedupeKey, true).apply()
    }
}
