package apincer.mobile.tradings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import apincer.mobile.tradings.ui.StockScreen
import apincer.mobile.tradings.ui.theme.TradingMateTheme
import apincer.mobile.tradings.util.NotificationHelper
import apincer.mobile.tradings.util.StockAlertWorker
import java.util.concurrent.TimeUnit

import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.annotation.RequiresApi

class MainActivity : ComponentActivity() {
    private var openSymbol: String? = null
    private var startScreen: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openSymbol = intent?.getStringExtra("OPEN_SYMBOL")
        startScreen = intent?.getStringExtra("START_SCREEN")

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )

        NotificationHelper.createNotificationChannel(this)
        checkNotificationPermission()
        requestBatteryOptimizationExemption()
        scheduleStockAlertWorker()

        setContent {
            TradingMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StockScreen(openSymbol = openSymbol, startScreen = startScreen)
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                } catch (_: Exception) { }
            }
        }
    }

    private fun scheduleStockAlertWorker() {
        val workRequest = PeriodicWorkRequestBuilder<StockAlertWorker>(
            30, TimeUnit.MINUTES   // 30-min cadence for reliable end-of-day alert window coverage
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "stock_alert_worker",
            ExistingPeriodicWorkPolicy.REPLACE,  // Replace stale worker when interval changes
            workRequest
        )
    }
}
