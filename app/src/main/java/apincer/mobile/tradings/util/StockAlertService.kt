package apincer.mobile.tradings.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import apincer.mobile.tradings.MainActivity
import apincer.mobile.tradings.data.SetScraper
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.MarketStatus
import apincer.mobile.tradings.domain.TechnicalAnalysis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StockAlertService : Service() {

    companion object {
        private const val TAG = "StockAlertService"
        private const val CHANNEL_ID = "stock_alerts_service"
        private const val CHANNEL_NAME = "Stock Alert Service"
        private const val NOTIFICATION_ID = 9999
        private const val CHECK_INTERVAL_MS = 60 * 60 * 1000L // 1 hour

        fun start(context: Context) {
            val intent = Intent(context, StockAlertService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StockAlertService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createServiceChannel()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildForegroundNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, buildForegroundNotification())
        }
        acquireWakeLock()
        startPeriodicCheck()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        releaseWakeLock()
        Log.d(TAG, "Service destroyed")
    }

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background stock alert monitoring"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("TradingMate")
            .setContentText("Monitoring stocks for sell signals...")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "tradingMate:stockAlertWakeLock"
        ).apply {
            acquire(15 * 60 * 1000L) // 15 minutes max per acquire
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun startPeriodicCheck() {
        serviceScope.launch {
            while (isActive) {
                try {
                    if (isMarketOpen()) {
                        checkAndNotifySellSignals()
                    } else {
                        Log.d(TAG, "Market closed, skipping check")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during check: ${e.message}", e)
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private fun isMarketOpen(): Boolean {
        return TechnicalAnalysis.getMarketStatus() != MarketStatus.CLOSED
    }

    private suspend fun checkAndNotifySellSignals() {
        val database = StockDatabase.getDatabase(applicationContext)
        val stockDao = database.stockDao()
        val allStocks = stockDao.getAllStocks().firstOrNull() ?: emptyList()

        if (allStocks.isEmpty()) {
            Log.d(TAG, "No stocks in watchlist")
            return
        }

        Log.d(TAG, "Checking ${allStocks.size} stocks for sell signals")

        allStocks.forEach { entity ->
            if (entity.quantity > 0) {
                try {
                    val scraped = SetScraper.fetchStockInfo(entity.symbol)
                    val indicators = SetScraper.fetchTechnicalIndicators(entity.symbol)
                    val signal = TechnicalAnalysis.getDetailedSignal(
                        rsi = indicators.rsi,
                        macdHist = indicators.histogram,
                        lastPrice = scraped.lastPrice,
                        sma50 = null,
                        sma200 = null,
                        bb = null,
                        isVolumeSurge = false,
                        userCost = entity.cost,
                        isFundamentalGood = false,
                        tradePurpose = entity.tradePurpose,
                        dividendYield = scraped.dividendYield,
                        roe = scraped.roe
                    )

                    if (signal.type == IndicatorSignal.SELL) {
                        Log.d(TAG, "SELL signal for ${entity.symbol}: ${signal.reason}")
                        NotificationHelper.showSellReminderNotification(
                            context = applicationContext,
                            symbol = entity.symbol,
                            reason = signal.reason
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check ${entity.symbol}: ${e.message}")
                }
            }
        }
    }
}
