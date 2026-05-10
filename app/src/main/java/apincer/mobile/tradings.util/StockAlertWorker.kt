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
                    isFundamentalGood = scraped.isFundamentalGood
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

            } catch (e: Exception) {
                Log.e("StockAlertWorker", "Failed to process ${entity.symbol}: ${e.message}")
            }
        }

        return Result.success()
    }

    private fun isActionable(type: IndicatorSignal): Boolean {
        return type == IndicatorSignal.BUY || type == IndicatorSignal.POTENTIAL || type == IndicatorSignal.SELL
    }
}
