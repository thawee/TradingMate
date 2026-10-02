package apincer.mobile.tradings.domain

import org.json.JSONObject
import java.time.LocalDate

/**
 * SET50 membership and SET holidays, refreshed from `config/market_lists.json` in the project
 * repository so a new year's calendar or a June/December index review does not need an app release.
 * Built-in lists ([TradingConstants.SET50_SYMBOLS], [SetHolidays]) are the fallback, and a remote
 * file that fails validation is ignored rather than partly applied.
 */
object MarketLists {
    const val REMOTE_URL = "https://raw.githubusercontent.com/thawee/TradingMate/main/config/market_lists.json"

    @Volatile private var set50: Set<String>? = null
    @Volatile private var holidays: Map<Int, Set<LocalDate>> = emptyMap()

    fun isSet50(symbol: String): Boolean = (set50 ?: TradingConstants.SET50_SYMBOLS).contains(symbol.uppercase())

    /** Remote holidays for [year] when published there; otherwise null so the built-in list applies. */
    fun holidaysFor(year: Int): Set<LocalDate>? = holidays[year]

    /** Validates and applies [json]; returns false (and changes nothing) if anything is off. */
    fun apply(json: String): Boolean {
        val parsed = parse(json) ?: return false
        set50 = parsed.first
        holidays = parsed.second
        return true
    }

    internal fun parse(json: String): Pair<Set<String>?, Map<Int, Set<LocalDate>>>? = try {
        val root = JSONObject(json)
        val set50 = root.optJSONObject("set50")?.optJSONArray("symbols")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it).trim().uppercase() }.toSet()
        }
        if (set50 != null && (set50.size !in 45..55 || set50.any { !it.matches(Regex("[A-Z0-9&.-]{1,12}")) })) null
        else {
            val hol = root.optJSONObject("holidays")
            val years = hol?.keys()?.asSequence()?.associate { key ->
                val year = key.toInt()
                val arr = hol.getJSONArray(key)
                val dates = (0 until arr.length()).map { LocalDate.parse(arr.getString(it)) }.toSet()
                require(dates.all { it.year == year } && dates.size in 5..30)
                year to dates
            } ?: emptyMap()
            set50 to years
        }
    } catch (e: Exception) {
        null
    }

    internal fun resetForTest() { set50 = null; holidays = emptyMap() }
}
