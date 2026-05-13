package apincer.mobile.tradings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import apincer.mobile.tradings.ui.StockScreen
import apincer.mobile.tradings.ui.theme.TradingMateTheme
import apincer.mobile.tradings.util.NotificationHelper
import apincer.mobile.tradings.util.StockAlertWorker
import java.util.concurrent.TimeUnit

import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )

        // 1. Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this)
        
        // 2. Request Permissions (Android 13+)
        checkNotificationPermission()

        // 3. Schedule Background Alerts
        scheduleStockAlerts()

        setContent {
            TradingMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StockScreen()
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleStockAlerts() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val alertRequest = PeriodicWorkRequestBuilder<StockAlertWorker>(
            1, TimeUnit.HOURS, // Check every hour
            15, TimeUnit.MINUTES // Flexibility period
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "stock_signal_alerts",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            alertRequest
        )
    }
}
