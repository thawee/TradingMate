package apincer.mobile.tradings.util

import android.content.Context
import android.util.Log
import apincer.mobile.tradings.domain.MarketLists

/** Keeps [MarketLists] in step with the repository's config/market_lists.json, at most once a day. */
object MarketListsSync {
    private const val PREFS = "trading_mate_alerts"
    private const val KEY_JSON = "market_lists_json"
    private const val KEY_DAY = "market_lists_fetched_day"

    /** Applies the last good copy at startup so offline launches still use the newest lists. */
    fun loadCached(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_JSON, null)?.let { MarketLists.apply(it) }
    }

    /** Network call; run off the main thread. A file that fails validation is not cached. */
    fun refreshIfStale(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).toString()
        if (prefs.getString(KEY_DAY, null) == today) return
        try {
            val body = org.jsoup.Jsoup.connect(MarketLists.REMOTE_URL).ignoreContentType(true)
                .timeout(15_000).execute().body()
            if (MarketLists.apply(body)) prefs.edit().putString(KEY_JSON, body).putString(KEY_DAY, today).apply()
            else Log.w("MarketListsSync", "Remote market lists failed validation; keeping current lists")
        } catch (e: Exception) {
            Log.w("MarketListsSync", "Could not fetch market lists: ${e.message}")
        }
    }
}
