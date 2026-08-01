package apincer.mobile.tradings.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import apincer.mobile.tradings.R
import apincer.mobile.tradings.appRepository
import apincer.mobile.tradings.domain.IndicatorSignal
import kotlinx.coroutines.flow.first

class SignalAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as android.app.Application
        val repository = app.appRepository
        val stocks = repository.allStocks.first()

        val buySignals = stocks.filter { it.signal?.signalType == "BUY" }
        val sellSignals = stocks.filter { it.signal?.signalType == "SELL" }

        if (buySignals.isNotEmpty() || sellSignals.isNotEmpty()) {
            sendNotification(buySignals.size, sellSignals.size)
        }

        return Result.success()
    }

    private fun sendNotification(buyCount: Int, sellCount: Int) {
        val channelId = "trading_mate_signals"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Trading Signal Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when technical BUY or SELL signals trigger"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val message = buildString {
            if (buyCount > 0) append("$buyCount BUY signal(s) ")
            if (sellCount > 0) append("$sellCount SELL alert(s)")
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Market Signal Update 🚨")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
