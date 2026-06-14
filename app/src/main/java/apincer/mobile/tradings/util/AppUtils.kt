package apincer.mobile.tradings.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object AppUtils {
    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            "Trading Mate v${packageInfo.versionName ?: "1.3.0"}"
        } catch (e: Exception) {
            "Trading Mate v1.3.0"
        }
    }
}
