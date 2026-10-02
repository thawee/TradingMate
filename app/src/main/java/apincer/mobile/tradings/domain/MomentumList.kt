package apincer.mobile.tradings.domain

import org.json.JSONArray
import org.json.JSONObject

/**
 * The "6-month momentum list": SET50 stocks ranked by their 126-session (about 6-month) return,
 * top 10, as tested in tools/backtest/momentum_portfolio_report.md (variant MOM3). It beat TDEX
 * over 2015-2025 but lost to it in 2021-2025 and its profit came mostly from three stocks, so it
 * failed the evidence gate and is shown only as an untested list.
 */
object MomentumList {
    const val LOOKBACK_SESSIONS = 126
    const val TOP_N = 10

    /** A ranked symbol and its score in percent (6-month return here; dividend yield in [HighYieldList]). */
    data class Entry(val symbol: String, val returnPercent: Double)

    /** [closes] per symbol, oldest first and dividend-adjusted. Symbols without enough history are skipped. */
    fun rank(closes: Map<String, List<Double>>, topN: Int = TOP_N): List<Entry> =
        closes.mapNotNull { (symbol, c) ->
            if (c.size <= LOOKBACK_SESSIONS) return@mapNotNull null
            val past = c[c.size - 1 - LOOKBACK_SESSIONS]
            val now = c.last()
            if (past <= 0.0 || now <= 0.0) null else Entry(symbol.uppercase(), (now / past - 1) * 100)
        }.sortedByDescending { it.returnPercent }.take(topN)

    fun toJson(date: String, entries: List<Entry>): String = JSONObject().put("date", date).put("entries",
        JSONArray().apply { entries.forEach { put(JSONObject().put("symbol", it.symbol).put("ret", it.returnPercent)) } }).toString()

    fun fromJson(json: String?): Pair<String, List<Entry>>? = try {
        val o = JSONObject(json ?: return null)
        val arr = o.getJSONArray("entries")
        o.getString("date") to (0 until arr.length()).map { i -> arr.getJSONObject(i).let { Entry(it.getString("symbol"), it.getDouble("ret")) } }
    } catch (e: Exception) {
        null
    }
}
