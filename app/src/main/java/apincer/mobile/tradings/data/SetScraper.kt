package apincer.mobile.tradings.data

import android.util.Log
import kotlinx.serialization.Serializable
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
data class ScrapedStockInfo(
    val symbol: String,
    val name: String? = null,
    val nameTH: String? = null,
    val businessDescription: String? = null,
    val sector: String? = null,
    val industry: String? = null,
    val lastPrice: Double,
    val change: Double,
    val percentChange: Double,
    val marketCap: Double? = null,
    val volume: Long? = null,
    val pe: Double? = null,
    val pbv: Double? = null,
    val roe: Double? = null,
    val eps: Double? = null,
    val netProfit: Double? = null, // thousand baht, as the SET company-highlight API reports it
    val netProfitMargin: Double? = null,
    val profitGrowth3Y: Double? = null,
    val equity: Double? = null,
    val debtToEquity: Double? = null,
    val dividendYield: Double? = null,
    val dividendDate: String? = null,
    val nvdrNetVolume: Double? = null,
    val nvdrNetValue: Double? = null,
    val lastUpdated: String
) {
    val bookValue: Double?
        get() = if (lastPrice != 0.0 && pbv != null && pbv != 0.0) {
            lastPrice / pbv
        } else null

    val isFundamentalGood: Boolean
        get() = (roe ?: 0.0) > 15.0 && 
                (debtToEquity ?: 100.0) < 1.5 && 
                (netProfitMargin ?: 0.0) > 10.0 &&
                (profitGrowth3Y ?: 0.0) > 10.0

    val isPartialData: Boolean
        get() = (name.isNullOrBlank() && nameTH.isNullOrBlank()) || lastPrice == 0.0 || (pe == null && pbv == null)
}

data class ScrapedHistoricalPrice(
    val date: String,
    val close: Double,
    val volume: Long = 0,
    val high: Double = close,
    val low: Double = close
)

object SetScraper {
    private const val TAG = "SetScraper"
    private const val SET_BASE_URL = "https://www.set.or.th"
    private const val YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart"
    private const val YAHOO_SEARCH_URL = "https://query1.finance.yahoo.com/v1/finance/search"
    private const val YAHOO_SPARK_URL = "https://query1.finance.yahoo.com/v7/finance/spark"
    private const val SPARK_MAX_SYMBOLS = 20
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/Consumer-Agent"
    private const val SEC_CH_UA = "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\""

    val FALLBACK_COLLECTIONS = mapOf(
        "SET50" to listOf(
            "ADVANC", "AOT", "AWC", "BANPU", "BBL", "BCP", "BDMS", "BEM", "BGRIM", "BH",
            "CBG", "CENTEL", "CPALL", "CPF", "CPN", "CRC", "DELTA", "EA", "EGCO", "GLOBAL",
            "GPSC", "GULF", "HMPRO", "INTUCH", "ITC", "IVL", "KBANK", "KCE", "KTB", "KTC",
            "LH", "MINT", "MTC", "OR", "OSP", "PTT", "PTTEP", "PTTGC", "RATCH", "SAWAD",
            "SCB", "SCC", "SCGP", "TIDLOR", "TISCO", "TOP", "TRUE", "TTB", "TU", "WHA"
        ),
        "SET100" to listOf(
            "ADVANC", "AMATA", "AOT", "AP", "AWC", "BA", "BAM", "BANPU", "BBL", "BCP",
            "BCPG", "BDMS", "BEM", "BGRIM", "BH", "BJC", "BLA", "BPP", "BTG", "BYD",
            "CBG", "CENTEL", "CHG", "CK", "CKP", "COM7", "CPALL", "CPF", "CPN", "CRC",
            "DELTA", "DOHOME", "EA", "EGCO", "ERW", "FORTH", "GLOBAL", "GPSC", "GULF", "HANA",
            "HMPRO", "ICHI", "INTUCH", "ITC", "IVL", "JMART", "JMT", "KAMART", "KBANK", "KCE",
            "KEX", "KKP", "KTB", "KTC", "LH", "MEGA", "MINT", "MOSHI", "MTC", "NEX",
            "OR", "ORI", "OSP", "PLANB", "PRM", "PSL", "PTG", "PTT", "PTTEP", "PTTGC",
            "QH", "RATCH", "RCL", "RS", "SABINA", "SAK", "SAPPE", "SAWAD", "SCB", "SCC",
            "SCGP", "SINGER", "SIRI", "SJWD", "SKY", "SPALI", "SPRC", "STA", "STEC", "SUPER",
            "TASCO", "TCAP", "THG", "TIDLOR", "TISCO", "TKN", "TOP", "TRUE", "TTB", "TU", "WHA"
        ),
        "SETHD" to listOf(
            "ADVANC", "AP", "BAM", "BANPU", "BBL", "BCP", "BCPG", "EGCO", "INTUCH", "KBANK",
            "KKP", "KTB", "LH", "ORI", "PSH", "PTG", "PTT", "PTTEP", "QH", "RATCH",
            "SCB", "SCC", "SIRI", "SPALI", "TASCO", "TCAP", "TISCO", "TOP", "TTB", "TU"
        ),
        "DIVIDEND" to listOf(
            "ADVANC", "BBL", "CPALL", "EGCO", "INTUCH", "KBANK", "KTB", "LH", "PTT", 
            "PTTEP", "RATCH", "SCB", "SCC", "TISCO", "TOP", "TU", "WHA"
        ),
        "BLUECHIP" to listOf(
            "AOT", "BBL", "BDMS", "CPALL", "DELTA", "GULF", "KBANK", "PTT", "PTTEP", "SCB", "SCC"
        )
    )

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            private val cookieStore = java.util.concurrent.ConcurrentHashMap<String, MutableList<Cookie>>()
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val host = url.host
                val store = cookieStore.getOrPut(host) { java.util.Collections.synchronizedList(mutableListOf()) }
                synchronized(store) {
                    cookies.forEach { newCookie ->
                        store.removeAll { it.name == newCookie.name }
                        store.add(newCookie)
                    }
                }
                Log.d(TAG, "SET Cookies Saved [${host}]: total ${store.size}")
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val store = cookieStore[url.host] ?: return emptyList()
                return synchronized(store) { store.toList() }
            }
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Retry mechanism for network resilience
    private fun <T> withRetry(maxRetries: Int = 3, initialDelayMs: Long = 1000, block: () -> T): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Network attempt $attempt failed, retrying in ${currentDelay}ms...", e)
                if (attempt < maxRetries) {
                    Thread.sleep(currentDelay)
                    currentDelay *= 2
                }
            }
        }
        throw lastException ?: Exception("Max retries exceeded")
    }

    private fun ensureSession(url: String) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,th;q=0.8")
                .header("Sec-Ch-Ua", SEC_CH_UA)
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"macOS\"")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")
                .build()
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Session Warmup [${url}]: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Session Warmup Failed", e)
        }
    }

    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun getCurrentTimestamp(): String {
        return LocalDateTime.now().format(timestampFormatter)
    }

    fun fetchStockInfo(symbol: String): ScrapedStockInfo {
        Log.d(TAG, "Starting fetchStockInfo for $symbol")
        var info = ScrapedStockInfo(
            symbol = symbol.uppercase(),
            lastPrice = 0.0,
            change = 0.0,
            percentChange = 0.0,
            lastUpdated = getCurrentTimestamp()
        )
        
        // 1. Get Metadata from Yahoo Search
        val yahooSearchList = searchYahoo(symbol)
        if (yahooSearchList.isNotEmpty()) {
            val yInfo = yahooSearchList[0]
            info = info.copy(
                name = yInfo.name,
                sector = yInfo.sector,
                industry = yInfo.industry
            )
        }
        
        // 2. Deep Fundamentals from SET API
        val setInfo = tryFetchFromSetApi(symbol)
        if (setInfo != null) {
            info = info.copy(
                name = setInfo.name ?: info.name,
                businessDescription = setInfo.businessDescription ?: info.businessDescription,
                sector = setInfo.sector ?: info.sector,
                industry = setInfo.industry ?: info.industry,
                lastPrice = if (setInfo.lastPrice != 0.0) setInfo.lastPrice else info.lastPrice,
                change = if (setInfo.lastPrice != 0.0) setInfo.change else info.change,
                percentChange = if (setInfo.lastPrice != 0.0) setInfo.percentChange else info.percentChange,
                pe = setInfo.pe ?: info.pe,
                pbv = setInfo.pbv ?: info.pbv,
                roe = setInfo.roe ?: info.roe,
                eps = setInfo.eps ?: info.eps,
                marketCap = setInfo.marketCap ?: info.marketCap,
                volume = setInfo.volume ?: info.volume,
                netProfit = setInfo.netProfit ?: info.netProfit,
                netProfitMargin = if (setInfo.netProfitMargin != null) setInfo.netProfitMargin else info.netProfitMargin,
                profitGrowth3Y = if (setInfo.profitGrowth3Y != null) setInfo.profitGrowth3Y else info.profitGrowth3Y,
                equity = setInfo.equity ?: info.equity,
                debtToEquity = setInfo.debtToEquity ?: info.debtToEquity,
                dividendYield = setInfo.dividendYield ?: info.dividendYield,
                dividendDate = setInfo.dividendDate ?: info.dividendDate,
                nvdrNetVolume = setInfo.nvdrNetVolume ?: info.nvdrNetVolume,
                nvdrNetValue = setInfo.nvdrNetValue ?: info.nvdrNetValue
            )
        }

        // 3. Final Fallback for Name
        if (info.name.isNullOrBlank()) {
            fetchStockNameFallback(symbol)?.let {
                info = info.copy(name = it)
            }
        }
        
        return info
    }

    private fun tryFetchFromSetApi(symbol: String): ScrapedStockInfo? {
        return try {
            val symbolUpper = symbol.uppercase()
            ensureSession("$SET_BASE_URL/th/market/product/stock/quote/$symbolUpper/overview")
            
            // 1. Fetch Overview (Basic Metadata)
            val overviewObj = fetchJson("$SET_BASE_URL/api/set/stock/$symbolUpper/overview?lang=th", symbolUpper) as? JSONObject
            
            // 2. Fetch Trading Stats (ROE, historical P/E) - Returns Array
            val tradingStatArray = fetchJson("$SET_BASE_URL/api/set/factsheet/$symbolUpper/trading-stat?lang=th", symbolUpper) as? JSONArray
            
            // 3. Fetch Info (Real-time Quote, P/E Ratio, P/BV Ratio, Dividend Yield)
            val infoObj = fetchJson("$SET_BASE_URL/api/set/stock/$symbolUpper/info?lang=th", symbolUpper) as? JSONObject

            // 4. Fetch Dividend History (Latest XD Date) - Returns Array
            val divArray = fetchJson("$SET_BASE_URL/api/set/stock/$symbolUpper/corporate-action/historical?caType=XD&lang=th", symbolUpper) as? JSONArray

            // 5. Fetch Company Highlight / Financial Data (ROE, D/E, Profit Growth)
            val highlightReferer = "$SET_BASE_URL/th/market/product/stock/quote/$symbolUpper/financial-statement/company-highlights"
            val highlightResult = fetchJson("$SET_BASE_URL/api/set/stock/$symbolUpper/company-highlight/financial-data?lang=th", symbolUpper, highlightReferer)
            val highlightRows = when (highlightResult) {
                is JSONArray -> highlightResult
                is JSONObject -> highlightResult.optJSONArray("rows")
                else -> null
            }

            // 6. Fetch NVDR Trading Data (Foreign Fund Flow)
            var nvdrNetVolume: Double? = null
            var nvdrNetValue: Double? = null
            try {
                val nvdrReferer = "$SET_BASE_URL/th/market/statistics/nvdr/trading-by-stock"
                val nvdrResult = fetchJson("$SET_BASE_URL/api/set/nvdr-trade/stock-trading?sortBy=symbol&symbols=$symbolUpper", symbolUpper, nvdrReferer)
                val latestNvdr = when (nvdrResult) {
                    is JSONObject -> nvdrResult.optJSONArray("nvdrTradings")?.optJSONObject(0) ?: nvdrResult
                    is JSONArray -> nvdrResult.optJSONObject(0)
                    else -> null
                }
                if (latestNvdr != null) {
                    nvdrNetVolume = latestNvdr.optDouble("netVolume", Double.NaN).takeIf { !it.isNaN() }
                    nvdrNetValue = latestNvdr.optDouble("netValue", Double.NaN).takeIf { !it.isNaN() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch NVDR for $symbolUpper", e)
            }

            val nameVal = infoObj?.optString("nameTH") ?: overviewObj?.optString("name")
            val desc = infoObj?.optString("businessDescription") ?: overviewObj?.optString("name")
            
            val sector = overviewObj?.optString("sectorName")?.takeIf { it.isNotBlank() } ?: overviewObj?.optString("sector")?.takeIf { it.isNotBlank() }
            val industry = overviewObj?.optString("industryName")?.takeIf { it.isNotBlank() } ?: overviewObj?.optString("industry")?.takeIf { it.isNotBlank() }
            
            val stats = tradingStatArray?.optJSONObject(0)
            
            // Prioritize Real-time infoObj for price and market ratios
            val price = infoObj?.optDouble("last", 0.0)?.takeIf { it != 0.0 && !it.isNaN() } 
                ?: stats?.optDouble("close", 0.0)?.takeIf { it != 0.0 && !it.isNaN() }
                ?: overviewObj?.optDouble("lastPrice", 0.0)?.takeIf { it != 0.0 && !it.isNaN() } ?: 0.0
                
            val change = infoObj?.optDouble("change", 0.0)?.takeIf { !it.isNaN() }
                ?: stats?.optDouble("change", 0.0)?.takeIf { !it.isNaN() } ?: 0.0
                
            val percentChange = infoObj?.optDouble("percentChange", 0.0)?.takeIf { !it.isNaN() }
                ?: stats?.optDouble("percentChange", 0.0)?.takeIf { !it.isNaN() } ?: 0.0
            
            val marketCap = infoObj?.optDouble("marketCap", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                ?: overviewObj?.optDouble("marketCap", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                
            val volume = infoObj?.optLong("totalVolume", 0L)?.takeIf { it != 0L }
                ?: overviewObj?.optLong("totalVolume", 0L)?.takeIf { it != 0L }

            val pe = infoObj?.optDouble("peRatio", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                ?: stats?.optDouble("pe", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                ?: overviewObj?.optDouble("pe", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                
            val pbv = infoObj?.optDouble("pbRatio", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                ?: stats?.optDouble("pbv", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                ?: overviewObj?.optDouble("pbv", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                
            val yield = infoObj?.optDouble("dividendYield", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                ?: stats?.optDouble("dividendYield", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                ?: overviewObj?.optDouble("dividendYield", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }

            // Financial Highlight Extraction
            var roe = stats?.optDouble("roe", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
            var de = infoObj?.optDouble("debtToEquity", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
                ?: overviewObj?.optDouble("debtToEquity", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
            var netProfit = overviewObj?.optDouble("netProfit", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
            var margin: Double? = null
            var eps = overviewObj?.optDouble("eps", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }
            var equity = overviewObj?.optDouble("totalEquity", 0.0)?.takeIf { !it.isNaN() && it != 0.0 }

            if (highlightRows != null && highlightRows.length() > 0) {
                // Typically rows are chronological, last one is the most recent
                val recentHighlight = highlightRows.optJSONObject(highlightRows.length() - 1)
                roe = recentHighlight.optDouble("roe", roe ?: 0.0).takeIf { !it.isNaN() } ?: roe
                de = recentHighlight.optDouble("deRatio", recentHighlight.optDouble("debtToEquity", de ?: 0.0)).takeIf { !it.isNaN() } ?: de
                netProfit = recentHighlight.optDouble("netProfit", netProfit ?: 0.0).takeIf { !it.isNaN() } ?: netProfit
                margin = recentHighlight.optDouble("netProfitMargin", 0.0).takeIf { !it.isNaN() }
                eps = recentHighlight.optDouble("eps", eps ?: 0.0).takeIf { !it.isNaN() } ?: eps
                equity = recentHighlight.optDouble("equity", equity ?: 0.0).takeIf { !it.isNaN() } ?: equity
                
            }

            val xdDate = divArray?.optJSONObject(0)?.optString("xdate")?.substringBefore("T")

            ScrapedStockInfo(
                symbol = symbolUpper,
                name = cleanName(nameVal),
                businessDescription = desc,
                sector = sector,
                industry = industry,
                lastPrice = price,
                change = change,
                percentChange = percentChange,
                pe = pe,
                pbv = pbv,
                roe = roe,
                eps = eps,
                netProfit = netProfit,
                netProfitMargin = margin,
                // Financial-data rows have no verified three-year period mapping here.
                // Do not present adjacent-row growth as a three-year result.
                profitGrowth3Y = null,
                equity = equity,
                debtToEquity = de,
                dividendYield = yield,
                dividendDate = xdDate,
                marketCap = marketCap,
                volume = volume,
                nvdrNetVolume = nvdrNetVolume,
                nvdrNetValue = nvdrNetValue,
                lastUpdated = getCurrentTimestamp()
            )
        } catch (e: Exception) {
            Log.e(TAG, "SET API Fetch Error for $symbol", e)
            null
        }
    }

    private fun fetchJson(url: String, symbol: String, referer: String? = null): Any? {
        return try {
            withRetry {
                val requestReferer = referer ?: "$SET_BASE_URL/th/market/product/stock/quote/$symbol/factsheet"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Host", "www.set.or.th")
                    .header("Origin", SET_BASE_URL)
                    .header("Referer", requestReferer)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "en-US,en;q=0.9,th;q=0.8")
                    .header("Sec-Ch-Ua", SEC_CH_UA)
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"macOS\"")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Dest", "empty")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    Log.d(TAG, "SET API Response [$url]: Code=${response.code}, Length=${body?.length ?: 0}")
                    if (response.code == 404 || (response.code in 400..499 && response.code != 429)) return@use null
                    if (!response.isSuccessful) throw java.io.IOException("HTTP error ${response.code}")
                    if (body.isNullOrBlank()) return@use null
                    JSONTokener(body).nextValue()
                }
            }
        } catch (e: Exception) { 
            Log.e(TAG, "fetchJson failed for $url after retries", e)
            null 
        }
    }

    /**
     * Latest price, change and name for [symbols] via Yahoo's spark endpoint, in chunks of
     * [SPARK_MAX_SYMBOLS]. The v7 quote endpoint now returns 401 without a crumb. Spark has
     * no P/E, P/BV or yield, so those stay null and callers keep their cached values.
     */
    fun fetchBatchQuotes(symbols: List<String>): List<ScrapedStockInfo> =
        symbols.distinct().chunked(SPARK_MAX_SYMBOLS).flatMap { chunk ->
            try {
                withRetry {
                    val yahooSymbols = chunk.joinToString(",") { "${it.uppercase()}.BK" }
                    val url = "$YAHOO_SPARK_URL?symbols=$yahooSymbols&range=1d&interval=1d"
                    Log.v(TAG, "Fetching Batch Quotes: $url")
                    val response = Jsoup.connect(url).userAgent(USER_AGENT).ignoreContentType(true).execute()
                    if (response.statusCode() != 200) throw java.io.IOException("HTTP ${response.statusCode()}")
                    parseSparkQuotes(JSONObject(response.body()), getCurrentTimestamp())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Batch Quote Error after retries for ${chunk.size} symbols", e)
                emptyList()
            }
        }

    /** Cash dividends from Yahoo chart events: ex-date (Bangkok) and baht per share, oldest first. */
    fun fetchDividendEvents(symbol: String, range: String = "2y"): List<Pair<java.time.LocalDate, Double>> =
        try {
            withRetry {
                val url = "$YAHOO_FINANCE_URL/${symbol.uppercase()}.BK?range=$range&interval=1d&events=div"
                val response = Jsoup.connect(url).userAgent(USER_AGENT).ignoreContentType(true).execute()
                if (response.statusCode() != 200) throw java.io.IOException("HTTP ${response.statusCode()}")
                parseDividendEvents(JSONObject(response.body()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dividend events error for $symbol", e)
            emptyList()
        }

    internal fun parseDividendEvents(json: JSONObject): List<Pair<java.time.LocalDate, Double>> {
        val divs = json.optJSONObject("chart")?.optJSONArray("result")?.optJSONObject(0)
            ?.optJSONObject("events")?.optJSONObject("dividends") ?: return emptyList()
        val bangkok = java.time.ZoneId.of("Asia/Bangkok")
        return divs.keys().asSequence().mapNotNull { key ->
            val e = divs.optJSONObject(key) ?: return@mapNotNull null
            val amount = e.optDouble("amount", Double.NaN)
            val epoch = e.optLong("date", key.toLongOrNull() ?: 0L)
            if (!amount.isFinite() || amount <= 0.0 || epoch <= 0L) null
            else java.time.Instant.ofEpochSecond(epoch).atZone(bangkok).toLocalDate() to amount
        }.sortedBy { it.first }.toList()
    }

    internal fun parseSparkQuotes(json: JSONObject, timestamp: String): List<ScrapedStockInfo> {
        val results = json.getJSONObject("spark").optJSONArray("result") ?: return emptyList()
        val infoList = mutableListOf<ScrapedStockInfo>()
        for (i in 0 until results.length()) {
            val meta = results.getJSONObject(i).optJSONArray("response")
                ?.optJSONObject(0)?.optJSONObject("meta") ?: continue
            val price = meta.optDouble("regularMarketPrice", 0.0)
            if (!price.isFinite() || price <= 0.0) continue
            val previousClose = meta.optDouble("chartPreviousClose", Double.NaN)
            val change = meta.optDouble("fulldayChange", Double.NaN).takeIf { it.isFinite() }
                ?: if (previousClose > 0) price - previousClose else 0.0
            val percentChange = meta.optDouble("regularMarketChangePercent", Double.NaN).takeIf { it.isFinite() }
                ?: if (previousClose > 0) change / previousClose * 100.0 else 0.0
            infoList.add(ScrapedStockInfo(
                symbol = meta.getString("symbol").removeSuffix(".BK"),
                name = meta.optString("longName").ifBlank { meta.optString("shortName") }.ifBlank { null },
                lastPrice = price,
                change = change,
                percentChange = percentChange,
                volume = meta.optLong("regularMarketVolume", -1L).takeIf { it >= 0 },
                lastUpdated = timestamp
            ))
        }
        return infoList
    }

    fun searchYahoo(query: String): List<ScrapedStockInfo> {
        return try {
            val url = "$YAHOO_SEARCH_URL?q=${query.uppercase()}${if (!query.contains(".")) ".BK" else ""}"
            Log.v(TAG, "Searching Yahoo: $url")
            val response = Jsoup.connect(url).userAgent(USER_AGENT).ignoreContentType(true).execute().body()
            val json = JSONObject(response)

            val quotes = json.getJSONArray("quotes")
            val results = mutableListOf<ScrapedStockInfo>()
            for (i in 0 until quotes.length()) {
                val quote = quotes.getJSONObject(i)
                results.add(ScrapedStockInfo(
                    symbol = quote.getString("symbol").replace(".BK", ""),
                    name = quote.optString("longname", quote.optString("shortname", null)),
                    sector = quote.optString("sector", null),
                    industry = quote.optString("industry", null),
                    lastPrice = 0.0,
                    change = 0.0,
                    percentChange = 0.0,
                    lastUpdated = ""
                ))
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Yahoo Search Error", e)
            emptyList()
        }
    }

    private fun cleanName(name: String?): String? {
        if (name == null) return null
        val unwanted = listOf("-")
        var cleaned = name.trim()
        unwanted.forEach { 
            if (cleaned.contains(it, ignoreCase = true)) {
                cleaned = cleaned.replace(it, "", ignoreCase = true).trim()
            }
        }
        return if (cleaned.isEmpty() || cleaned == "-") null else cleaned
    }

    private fun fetchStockNameFallback(symbol: String): String? {
        return try {
            withRetry(maxRetries = 2) {
                val url = "https://www.set.or.th/th/market/product/stock/quote/${symbol.uppercase()}/overview"
                val response = Jsoup.connect(url).userAgent(USER_AGENT).timeout(10000).execute()
                if (response.statusCode() != 200) throw java.io.IOException("HTTP ${response.statusCode()}")
                val doc = response.parse()
                
                val nameElement = doc.selectFirst("h1.text-white.mb-1")
                val nameText = nameElement?.text()?.trim()
                if (nameText.isNullOrBlank() || nameText == symbol.uppercase()) {
                    null
                } else {
                    nameText
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchStockNameFallback error for $symbol", e)
            null
        }
    }

    /**
     * Daily history for the last [days] calendar days (default ~1 year for live indicators).
     * [dividendAdjusted] scales OHLC by Yahoo's adjclose, giving a total-return series.
     */
    /**
     * Daily bars from a Yahoo chart response; null closes are skipped unless [carryForwardMissingClose]
     * (index proxy only), which repeats the previous close for that session.
     */
    internal fun parseChartHistory(json: JSONObject, dividendAdjusted: Boolean,
                                   carryForwardMissingClose: Boolean = false): List<ScrapedHistoricalPrice> {
        val result = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)

        if (!result.has("timestamp")) return emptyList()

        val timestamps = result.getJSONArray("timestamp")
        val indicators = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
        val closes = indicators.getJSONArray("close")
        val volumes = indicators.getJSONArray("volume")
        // high/low may be absent in degraded responses — fall back to close
        val highs = indicators.optJSONArray("high")
        val lows = indicators.optJSONArray("low")
        val adjCloses = if (dividendAdjusted) result.getJSONObject("indicators")
            .optJSONArray("adjclose")?.optJSONObject(0)?.optJSONArray("adjclose") else null

        val prices = mutableListOf<ScrapedHistoricalPrice>()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

        for (i in 0 until timestamps.length()) {
            val ts = timestamps.getLong(i) * 1000
            if (closes.isNull(i)) {
                // A session timestamp without a close: no trade (or not yet) in that session. For the index
                // proxy, carry the last close so its date matches stocks that did trade that session.
                val previous = prices.lastOrNull()
                if (carryForwardMissingClose && previous != null) prices.add(previous.copy(
                    date = java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.of("Asia/Bangkok")).toLocalDate().format(dateFormatter),
                    volume = 0L, high = previous.close, low = previous.close))
                continue
            }
            val rawClose = closes.getDouble(i)
            val factor = if (adjCloses != null && !adjCloses.isNull(i) && rawClose > 0) adjCloses.getDouble(i) / rawClose else 1.0
            val close = rawClose * factor
            val volume = if (!volumes.isNull(i)) volumes.getLong(i) else 0L
            val high = (if (highs != null && !highs.isNull(i)) highs.getDouble(i) else rawClose) * factor
            val low = (if (lows != null && !lows.isNull(i)) lows.getDouble(i) else rawClose) * factor

            prices.add(ScrapedHistoricalPrice(
                date = java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.of("Asia/Bangkok")).toLocalDate().format(dateFormatter),
                close = close,
                volume = volume,
                high = high,
                low = low
            ))
        }

        return prices
    }

    fun fetchHistoricalPrices(symbol: String, days: Int = 365, dividendAdjusted: Boolean = false,
                              carryForwardMissingClose: Boolean = false): List<ScrapedHistoricalPrice> {
        return try {
            withRetry {
                val symbolBK = "${symbol.uppercase()}.BK"
                val endDate = System.currentTimeMillis() / 1000
                val startDate = endDate - days * 86_400L
                
                val url = "$YAHOO_FINANCE_URL/$symbolBK?period1=$startDate&period2=$endDate&interval=1d&events=history"
                Log.d(TAG, "Fetching Historical Prices: $url")
                
                val response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .ignoreContentType(true)
                    .timeout(15000)
                    .execute()
                    
                if (response.statusCode() != 200) throw java.io.IOException("HTTP ${response.statusCode()}")
                
                parseChartHistory(JSONObject(response.body()), dividendAdjusted, carryForwardMissingClose)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Historical Price Fetch Error after retries", e)
            emptyList()
        }
    }

    fun fetchTechnicalIndicators(symbol: String): apincer.mobile.tradings.domain.Indicators {
        val history = fetchHistoricalPrices(symbol)
        // Prices are in chronological order (oldest→newest) as required by TA functions
        val prices = history.map { it.close }
        val volumes = history.map { it.volume }

        val sma50 = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateSMA(prices, 50)
        val sma200 = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateSMA(prices, 200)
        val bb = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateBollingerBands(prices)
        val isVolumeSurge = apincer.mobile.tradings.domain.TechnicalAnalysis.isVolumeSurge(volumes)
        val rsi = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateRSI(prices, 14)
        val macd = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateMACD(prices)
        val obvRising = apincer.mobile.tradings.domain.TechnicalAnalysis.isObvRising(prices, volumes)
        val week52 = apincer.mobile.tradings.domain.TechnicalAnalysis.calculate52WeekRange(prices)
        val indexHistory = fetchSetIndexHistory()
        val relativeStrength = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateRelativeStrengthOnDates(
            history.map { it.date to it.close }, indexHistory.map { it.date to it.close })
        val highs = history.map { it.high }
        val lows = history.map { it.low }
        val atr = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateATR(highs, lows, prices)
        val adx = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateADX(highs, lows, prices)
        val stoch = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateStochastic(highs, lows, prices)
        val mfi = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateMFI(highs, lows, prices, volumes)
        val weeklyTrendBullish = apincer.mobile.tradings.domain.TechnicalAnalysis.isWeeklyTrendBullishOnDate(
            history.map { it.date to it.close },
            java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).toString(),
            prices.lastOrNull() ?: 0.0
        )

        return apincer.mobile.tradings.domain.Indicators(
            sma50 = sma50,
            sma200 = sma200,
            rsi = rsi,
            macd = macd.first,
            signal = macd.second,
            histogram = macd.third,
            bollingerBands = bb,
            isVolumeSurge = isVolumeSurge,
            obvRising = obvRising,
            week52Low = week52?.first,
            week52High = week52?.second,
            relativeStrength = relativeStrength,
            atr = atr,
            adx = adx,
            stochK = stoch?.first,
            stochD = stoch?.second,
            mfi = mfi,
            weeklyTrendBullish = weeklyTrendBullish,
            observationDate = history.lastOrNull()?.date,
            benchmarkDate = indexHistory.lastOrNull()?.date
        )
    }

    // SET index history cache — shared across all stocks in a refresh cycle
    private const val SET_INDEX_SYMBOL_RAW = "%5ESET.BK" // URL-encoded ^SET.BK
    // Yahoo returns no daily history for ^SET.BK (only the latest quote). TDEX, the SET50 ETF,
    // tracks the broad market closely enough for return-based uses: regime (SMA/MACD),
    // relative strength and beta. Do not display its price as the SET index level.
    private const val SET_INDEX_PROXY_SYMBOL = "TDEX"
    // ^SET.BK currently returns only today's bar; regime (SMA 50 + MACD), 63-day RS and beta need far more.
    private const val MIN_INDEX_HISTORY_BARS = 120
    private const val INDEX_CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour
    @Volatile private var cachedIndexHistory: List<ScrapedHistoricalPrice> = emptyList()
    @Volatile private var cachedIndexTimestamp: Long = 0L

    /**
     * Fetches ~1 year of SET index daily closes (cached 1h) for regime, Relative Strength and beta.
     * Falls back to [SET_INDEX_PROXY_SYMBOL] when Yahoo serves fewer than [MIN_INDEX_HISTORY_BARS] bars.
     */
    fun fetchSetIndexHistory(): List<ScrapedHistoricalPrice> {
        val now = System.currentTimeMillis()
        if (cachedIndexHistory.isNotEmpty() && now - cachedIndexTimestamp < INDEX_CACHE_TTL_MS) {
            return cachedIndexHistory
        }
        val direct = fetchSetIndexHistoryDirect()
        val history = if (direct.size >= MIN_INDEX_HISTORY_BARS) direct else {
            Log.w(TAG, "^SET.BK returned ${direct.size} bars; using $SET_INDEX_PROXY_SYMBOL as index proxy")
            fetchHistoricalPrices(SET_INDEX_PROXY_SYMBOL, carryForwardMissingClose = true)
        }
        if (history.isEmpty()) return cachedIndexHistory // stale cache is better than nothing
        cachedIndexHistory = history
        cachedIndexTimestamp = now
        return history
    }

    private fun fetchSetIndexHistoryDirect(): List<ScrapedHistoricalPrice> {
        val now = System.currentTimeMillis()
        return try {
            withRetry {
                val endDate = now / 1000
                val startDate = endDate - 31536000
                val url = "$YAHOO_FINANCE_URL/$SET_INDEX_SYMBOL_RAW?period1=$startDate&period2=$endDate&interval=1d&events=history"
                Log.d(TAG, "Fetching SET Index History: $url")

                val response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .ignoreContentType(true)
                    .timeout(15000)
                    .execute()

                if (response.statusCode() != 200) throw java.io.IOException("HTTP ${response.statusCode()}")

                val json = JSONObject(response.body())
                val result = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
                if (!result.has("timestamp")) return@withRetry emptyList()

                val timestamps = result.getJSONArray("timestamp")
                val indicators = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
                val closes = indicators.getJSONArray("close")

                val prices = mutableListOf<ScrapedHistoricalPrice>()
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
                for (i in 0 until timestamps.length()) {
                    if (closes.isNull(i)) continue
                    val ts = timestamps.getLong(i) * 1000
                    prices.add(ScrapedHistoricalPrice(
                        date = java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.of("Asia/Bangkok")).toLocalDate().format(dateFormatter),
                        close = closes.getDouble(i)
                    ))
                }
                prices
            }
        } catch (e: Exception) {
            Log.e(TAG, "SET Index History Fetch Error after retries", e)
            emptyList()
        }
    }

    fun fetchIndexComposition(indexName: String): List<String> {
        return try {
            val indexLower = indexName.lowercase()
            val url = "$SET_BASE_URL/api/set/index/$indexLower/composition?lang=th"
            val referer = "$SET_BASE_URL/th/market/index/$indexLower/overview"
            ensureSession(referer)
            val result = fetchJson(url, indexName, referer)
            val symbols = mutableListOf<String>()
            
            Log.d(TAG, "$indexName Raw Result Type: ${result?.javaClass?.simpleName}")

            val compositionArray = when (result) {
                is JSONObject -> {
                    val comp = result.optJSONObject("composition")
                    result.optJSONArray("composition") 
                        ?: comp?.optJSONArray("stockInfos")
                        ?: result.optJSONArray("rows")
                }
                is JSONArray -> result 
                else -> null
            }
            
            if (compositionArray != null) {
                for (i in 0 until compositionArray.length()) {
                    val item = compositionArray.getJSONObject(i)
                    val symbol = item.optString("symbol")
                    if (!symbol.isNullOrBlank()) {
                        symbols.add(symbol)
                    }
                }
            }
            Log.d(TAG, "Fetched ${symbols.size} symbols for $indexName")
            symbols
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch $indexName composition", e)
            emptyList()
        }
    }

    fun getCuratedCollection(category: String): List<String> {
        return when (category.uppercase()) {
            "DIVIDEND" -> FALLBACK_COLLECTIONS["DIVIDEND"] ?: emptyList()
            "BLUECHIP" -> FALLBACK_COLLECTIONS["BLUECHIP"] ?: emptyList()
            "SET50" -> {
                val dynamicList = fetchIndexComposition("SET50")
                if (dynamicList.isNotEmpty()) dynamicList else FALLBACK_COLLECTIONS["SET50"] ?: emptyList()
            }
            "SET100" -> {
                val dynamicList = fetchIndexComposition("SET100")
                if (dynamicList.isNotEmpty()) dynamicList else FALLBACK_COLLECTIONS["SET100"] ?: emptyList()
            }
            "SETHD" -> {
                val dynamicList = fetchIndexComposition("SETHD")
                if (dynamicList.isNotEmpty()) dynamicList else FALLBACK_COLLECTIONS["SETHD"] ?: emptyList()
            }
            else -> emptyList()
        }
    }
}
