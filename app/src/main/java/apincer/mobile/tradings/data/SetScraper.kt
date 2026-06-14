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
import java.text.SimpleDateFormat
import java.util.Date
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
    val netProfit: Double? = null,
    val netProfitMargin: Double? = null,
    val profitGrowth3Y: Double? = null,
    val equity: Double? = null,
    val debtToEquity: Double? = null,
    val dividendYield: Double? = null,
    val dividendDate: String? = null,
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
    val volume: Long = 0
)

object SetScraper {
    private const val TAG = "SetScraper"
    private const val SET_BASE_URL = "https://www.set.or.th"
    private const val YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart"
    private const val YAHOO_SEARCH_URL = "https://query1.finance.yahoo.com/v1/finance/search"
    private const val YAHOO_QUOTE_URL = "https://query1.finance.yahoo.com/v7/finance/quote"
    private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    private const val SEC_CH_UA = "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\""

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

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
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
                dividendDate = setInfo.dividendDate ?: info.dividendDate
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

            val nameVal = infoObj?.optString("nameTH") ?: overviewObj?.optString("name")
            val desc = infoObj?.optString("businessDescription") ?: overviewObj?.optString("name")
            
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
            var profitGrowth: Double? = null
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
                
                // Calculate Profit Growth if we have multiple years
                if (highlightRows.length() >= 2) {
                    val prevHighlight = highlightRows.optJSONObject(highlightRows.length() - 2)
                    val currentNP = recentHighlight.optDouble("netProfit", 0.0)
                    val prevNP = prevHighlight.optDouble("netProfit", 0.0)
                    if (prevNP != 0.0 && !prevNP.isNaN() && !currentNP.isNaN()) {
                        profitGrowth = ((currentNP - prevNP) / Math.abs(prevNP)) * 100.0
                    }
                }
            }

            val xdDate = divArray?.optJSONObject(0)?.optString("xdate")?.substringBefore("T")

            ScrapedStockInfo(
                symbol = symbolUpper,
                name = cleanName(nameVal),
                businessDescription = desc,
                lastPrice = price,
                change = change,
                percentChange = percentChange,
                pe = pe,
                pbv = pbv,
                roe = roe,
                eps = eps,
                netProfit = netProfit,
                netProfitMargin = margin,
                profitGrowth3Y = profitGrowth,
                equity = equity,
                debtToEquity = de,
                dividendYield = yield,
                dividendDate = xdDate,
                marketCap = marketCap,
                volume = volume,
                lastUpdated = getCurrentTimestamp()
            )
        } catch (e: Exception) {
            Log.e(TAG, "SET API Fetch Error for $symbol", e)
            null
        }
    }

    private fun fetchJson(url: String, symbol: String, referer: String? = null): Any? {
        return try {
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
                if (!response.isSuccessful || body.isNullOrBlank()) return null
                JSONTokener(body).nextValue()
            }
        } catch (e: Exception) { 
            Log.e(TAG, "fetchJson failed for $url", e)
            null 
        }
    }

    fun fetchBatchQuotes(symbols: List<String>): List<ScrapedStockInfo> {
        return try {
            val yahooSymbols = symbols.joinToString(",") { "${it.uppercase()}.BK" }
            val url = "$YAHOO_QUOTE_URL?symbols=$yahooSymbols"
            Log.v(TAG, "Fetching Batch Quotes: $url")
            val response = Jsoup.connect(url).userAgent(USER_AGENT).ignoreContentType(true).execute().body()
            val json = JSONObject(response)
            val results = json.getJSONObject("quoteResponse").getJSONArray("result")
            
            val infoList = mutableListOf<ScrapedStockInfo>()
            for (i in 0 until results.length()) {
                val quote = results.getJSONObject(i)
                val symbol = quote.getString("symbol").replace(".BK", "")
                infoList.add(ScrapedStockInfo(
                    symbol = symbol,
                    name = quote.optString("longName", quote.optString("shortName", null)),
                    lastPrice = quote.optDouble("regularMarketPrice", 0.0),
                    change = quote.optDouble("regularMarketChange", 0.0),
                    percentChange = quote.optDouble("regularMarketChangePercent", 0.0),
                    pe = quote.optDouble("trailingPE", 0.0).takeIf { it > 0 },
                    pbv = quote.optDouble("priceToBook", 0.0).takeIf { it > 0 },
                    dividendYield = quote.optDouble("trailingAnnualDividendYield", 0.0) * 100, // Yahoo returns decimal
                    lastUpdated = getCurrentTimestamp()
                ))
            }
            infoList
        } catch (e: Exception) {
            Log.e(TAG, "Batch Quote Error", e)
            emptyList()
        }
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
            val url = "https://www.set.or.th/th/market/product/stock/quote/${symbol.uppercase()}/overview"
            val doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(10000).get()
            val name = doc.select(".quote-symbol").first()?.parent()?.select("div")?.first()?.text()
                ?: doc.select("h1").first()?.text()?.substringAfter(" - ")
            cleanName(name)
        } catch (e: Exception) { null }
    }

    fun fetchHistoricalPrices(symbol: String): List<ScrapedHistoricalPrice> {
        val url = "$YAHOO_FINANCE_URL/${symbol.uppercase()}.BK?range=1y&interval=1d"
        try {
            val response = Jsoup.connect(url).userAgent(USER_AGENT).ignoreContentType(true).execute().body()
            val prices = mutableListOf<ScrapedHistoricalPrice>()
            val json = JSONObject(response)
            val result = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
            val timestamps = result.getJSONArray("timestamp")
            val indicators = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
            val closePrices = indicators.getJSONArray("close")
            val volumes = indicators.getJSONArray("volume")

            for (i in 0 until timestamps.length()) {
                val timestamp = timestamps.getLong(i)
                val close = if (closePrices.isNull(i)) null else closePrices.getDouble(i)
                val volume = if (volumes.isNull(i)) 0L else volumes.getLong(i)
                if (close != null) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp * 1000))
                    prices.add(ScrapedHistoricalPrice(date, close, volume))
                }
            }
            return prices
        } catch (e: Exception) { return emptyList() }
    }

    fun fetchTechnicalIndicators(symbol: String): apincer.mobile.tradings.domain.Indicators {
        val history = fetchHistoricalPrices(symbol)
        val prices = history.map { it.close }.reversed()
        val volumes = history.map { it.volume }.reversed()

        val sma50 = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateSMA(prices, 50)
        val sma200 = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateSMA(prices, 200)
        val bb = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateBollingerBands(prices)
        val isVolumeSurge = apincer.mobile.tradings.domain.TechnicalAnalysis.isVolumeSurge(volumes)
        val rsi = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateRSI(prices, 14)
        val macd = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateMACD(prices)

        return apincer.mobile.tradings.domain.Indicators(
            sma50 = sma50,
            sma200 = sma200,
            rsi = rsi,
            macd = macd.first,
            signal = macd.second,
            histogram = macd.third,
            bollingerBands = bb,
            isVolumeSurge = isVolumeSurge
        )
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
        val dividendStars = listOf(
            "ADVANC", "BBL", "CPALL", "EGCO", "INTUCH", "KBANK", "KTB", "LH", "PTT", 
            "PTTEP", "RATCH", "SCB", "SCC", "TISCO", "TOP", "TU", "WHA"
        )
        return when (category.uppercase()) {
            "DIVIDEND" -> dividendStars
            "BLUECHIP" -> listOf(
                "AOT", "BBL", "BDMS", "CPALL", "DELTA", "GULF", "KBANK", "PTT", "PTTEP", "SCB", "SCC"
            )
            "SET50" -> {
                val dynamicList = fetchIndexComposition("SET50")
                if (dynamicList.isNotEmpty()) {
                    dynamicList
                } else {
                    listOf(
                        "ADVANC", "AOT", "AWC", "BANPU", "BBL", "BCP", "BDMS", "BEM", "BGRIM", "BH",
                        "CBG", "CENTEL", "COM7", "CPALL", "CPF", "CPN", "CRC", "DELTA", "EA", "EGCO",
                        "GLOBAL", "GPSC", "GULF", "GUNKUL", "HMPRO", "INTUCH", "IVL", "JMART", "JMT", "KBANK",
                        "KCE", "KKP", "KTB", "KTC", "LH", "MINT", "MTC", "OR", "OSP", "PTT",
                        "PTTEP", "PTTGC", "RATCH", "SAWAD", "SCB", "SCC", "SCGP", "TIDLOR", "TISCO", "TOP",
                        "TRUE", "TTB", "TU", "WHA"
                    )
                }
            }
            "SET100" -> {
                val dynamicList = fetchIndexComposition("SET100")
                if (dynamicList.isNotEmpty()) dynamicList else getCuratedCollection("SET50")
            }
            "SETHD" -> {
                val dynamicList = fetchIndexComposition("SETHD")
                if (dynamicList.isNotEmpty()) dynamicList else dividendStars
            }
            else -> emptyList()
        }
    }
}
