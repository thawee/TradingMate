package apincer.mobile.tradings.util

import android.app.NotificationChannel
import android.util.Log
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import apincer.mobile.tradings.MainActivity
import apincer.mobile.tradings.R

object NotificationHelper {
    private const val CHANNEL_ID = "stock_alerts"
    private const val CHANNEL_NAME = "Stock Signal Alerts"
    private const val CHANNEL_DESCRIPTION = "Notifications for BUY, POTENTIAL, and SELL signals."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSignalNotification(context: Context, symbol: String, signal: String, reason: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SYMBOL", symbol)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle("$signal: $symbol")
            .setContentText(reason)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(symbol.hashCode(), builder.build())
    }

    fun showPrimeTimeNotification(context: Context, isMorning: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("START_SCREEN", if (isMorning) "PORTFOLIO" else "ADVISOR")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, if (isMorning) 1 else 2, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isMorning) "☀️ Morning Swing Exit Window" else "🔔 Afternoon Swing Entry Window"
        val text = if (isMorning) 
            "Markets are open! Check your active swing positions for Sell or Stop Loss alerts." 
        else 
            "Market close approaching! Scan the Advisor for new daily Swing & Gap candidates."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(if (isMorning) 9991 else 9992, builder.build())
    }

    fun showXdAlertNotification(context: Context, symbol: String, dividendDate: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SYMBOL", symbol)
            putExtra("START_SCREEN", "PORTFOLIO")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, symbol.hashCode(), intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 Upcoming XD: $symbol")
            .setContentText("Ex-Dividend date is $dividendDate. Accumulate before this date if you want the upcoming dividend.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(symbol.hashCode() + 1000, builder.build())
    }

    fun showSellReminderNotification(context: Context, symbol: String, reason: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SYMBOL", symbol)
            putExtra("START_SCREEN", "PORTFOLIO")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, symbol.hashCode() + 2000, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ SELL: $symbol")
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(symbol.hashCode() + 2000, builder.build())
    }

    fun showDividendSeasonNotification(context: Context, isFirstSeason: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("START_SCREEN", "WATCHLIST")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 3, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val seasonName = if (isFirstSeason) "First-Half" else "Second-Half"
        val xdMonths = if (isFirstSeason) "April/May" else "August/September"
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💰 Dividend Accumulation Season")
            .setContentText("Start researching high-yield Dividend Stars now for the $seasonName payouts in $xdMonths.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(9993, builder.build())
    }

    /**
     * Year-round alert: fires any month when a DIVIDEND-purpose watchlist stock's
     * yield spikes above the threshold (price dropped → yield rose) with solid fundamentals.
     * Deduplicates per week so it doesn't spam on every 30-min worker cycle.
     */
    fun showDividendYieldOpportunityNotification(
        context: Context,
        symbol: String,
        yield: Double,
        price: Double,
        roe: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SYMBOL", symbol)
            putExtra("START_SCREEN", "WATCHLIST")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, symbol.hashCode() + 5000, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💰 Yield Opportunity: $symbol")
            .setContentText("Yield %.1f%% at ฿%.2f — strong ROE %.1f%%. Good accumulation point.".format(yield, price, roe))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$symbol dividend yield has risen to %.1f%% at ฿%.2f. ".format(yield, price) +
                    "Fundamentals remain solid (ROE %.1f%%). ".format(roe) +
                    "Price weakness may be a good long-term accumulation opportunity."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(symbol.hashCode() + 5000, builder.build())
    }
}

