package apincer.mobile.tradings.data

import android.util.Log
import org.jsoup.Jsoup
import java.io.IOException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ScrapedStockInfo(
    val symbol: String,
    val name: String? = null,
    val nameTH: String? = null,
    val businessDescription: String? = null,
    val lastPrice: Double,
    val change: Double,
    val percentChange: Double,
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
                (netProfitMargin ?: 0.0) > 10.0

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
    private const val GOOGLE_FINANCE_URL = "https://www.google.com/finance/quote"
    private const val SET_FACTSHEET_URL = "https://www.set.or.th/th/market/product/stock/quote"
    private const val YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart"
    private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun fetchStockInfo(symbol: String): ScrapedStockInfo {
        Log.d(TAG, "Starting fetchStockInfo for $symbol")
        var info = tryFetchFromGoogle(symbol)
        Log.d(TAG, "Google Source Result [$symbol]: Price=${info.lastPrice}, Change=${info.change}, Percent=${info.percentChange}")
        
        val setInfo = tryFetchFromSet(symbol)
        if (setInfo != null) {
            Log.d(TAG, "SET Source Result [$symbol]: Price=${setInfo.lastPrice}, PE=${setInfo.pe}, PBV=${setInfo.pbv}, ROE=${setInfo.roe}, Name=${setInfo.name}")
            info = info.copy(
                name = if (!setInfo.name.isNullOrBlank()) setInfo.name else info.name,
                businessDescription = if (info.businessDescription.isNullOrBlank()) setInfo.businessDescription else info.businessDescription,
                lastPrice = if (info.lastPrice == 0.0) setInfo.lastPrice else info.lastPrice,
                pe = if (info.pe == null || info.pe == 0.0) setInfo.pe else info.pe,
                pbv = if (info.pbv == null || info.pbv == 0.0) setInfo.pbv else info.pbv,
                roe = if (info.roe == null) setInfo.roe else info.roe,
                eps = if (info.eps == null) setInfo.eps else info.eps,
                netProfit = if (info.netProfit == null) setInfo.netProfit else info.netProfit,
                equity = if (info.equity == null) setInfo.equity else info.equity,
                debtToEquity = if (info.debtToEquity == null) setInfo.debtToEquity else info.debtToEquity,
                dividendYield = if (info.dividendYield == null) setInfo.dividendYield else info.dividendYield,
                dividendDate = if (info.dividendDate == null) setInfo.dividendDate else info.dividendDate
            )
        }

        if (info.name.isNullOrBlank()) {
            fetchStockNameFallback(symbol)?.let {
                info = info.copy(name = it)
            }
        }
        
        Log.d(TAG, "Final Merged Result for $symbol: Price=${info.lastPrice}, PE=${info.pe}, ROE=${info.roe}, Partial=${info.isPartialData}")
        return info
    }

    private fun tryFetchFromGoogle(symbol: String): ScrapedStockInfo {
        var info = ScrapedStockInfo(
            symbol = symbol,
            lastPrice = 0.0,
            change = 0.0,
            percentChange = 0.0,
            lastUpdated = ""
        )
        try {
            info = scrapeGoogle(symbol, "BK")
            if (info.lastPrice == 0.0) {
                info = scrapeGoogle(symbol, "BKK")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Google for $symbol", e)
        }
        return info
    }

    private fun scrapeGoogle(symbol: String, suffix: String): ScrapedStockInfo {
        val url = "$GOOGLE_FINANCE_URL/${symbol.uppercase()}:$suffix"
        Log.v(TAG, "Fetching Google: $url")
        val doc = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .timeout(10000)
            .get()

        val mainContainer = doc.select("main").first() ?: doc.select("#yDmH0d").first() ?: doc

        // Identify the symbol-specific block (card or row)
        val symbolBlock = mainContainer.select(".SxcTic, .KY7fce, .vP79B, .D67Xm, tr, .MD1Z9").find { block ->
             block.text().split(Regex("[^A-Za-z0-9]")).any { it.equals(symbol, ignoreCase = true) }
        }
        
        val searchArea = symbolBlock ?: mainContainer

        // Extract Name
        var companyName: String? = symbolBlock?.select(".ZvmM7, .zz3FFb")?.first()?.text()
            ?: mainContainer.select(".zz3FFb").first()?.text()
            ?: mainContainer.select("h1").find { !it.text().contains("Google", ignoreCase = true) }?.text()
            ?: doc.title().substringBefore(" (")
        companyName = cleanName(companyName)

        // Extract Price
        val priceElement = searchArea.select(".YMlKec.fxKbKc, [jsname='vW7dgc']").first()
            ?: searchArea.select(".YMlKec").find { it.text().contains("฿") }
            ?: mainContainer.select(".YMlKec.fxKbKc, [jsname='vW7dgc']").first()
            
        val priceText = (priceElement?.text() ?: "0.0").replace("฿", "").replace(",", "").trim()
        val lastPrice = priceText.toDoubleOrNull() ?: 0.0
        
        // Extract Change Data
        var changeContainer = searchArea.select("[jsname='Fe7oBc']").first()
            ?: priceElement?.parents()?.take(5)?.find { it.select("[jsname='Fe7oBc']").isNotEmpty() }?.select("[jsname='Fe7oBc']")?.first()
            ?: mainContainer.select("[jsname='Fe7oBc']").first()
            
        val mainLabel = changeContainer?.attr("aria-label") ?: ""
        val changeElement = changeContainer?.select("[jsname='qE749c'], [jsname='m6NnIb']")?.first() 
        val percentElement = changeContainer?.select("[jsname='j9S6ve'], .JwB6zf")?.first()

        val changeText = changeElement?.text() ?: ""
        val percentText = percentElement?.text() ?: ""
        
        // Parse raw values: ignore percentage sign for absolute Baht change
        var changeValue = if (changeText.contains("%")) 0.0 else extractNumeric(changeText)
        var percentValue = extractNumeric(percentText.replace("%", ""))
        
        // Robust extraction from aria-label
        if (mainLabel.isNotEmpty()) {
             val rawAbsText = mainLabel.substringBefore("(")
             val rawPerText = if (mainLabel.contains("(")) mainLabel.substringAfter("(").substringBefore(")") else ""
             
             // Extract absolute if text parsing failed or was just a percentage
             if (changeValue == 0.0 && !rawAbsText.contains("%")) changeValue = extractNumeric(rawAbsText)
             if (percentValue == 0.0) percentValue = extractNumeric(rawPerText)
             
             // If we still have 0.0 but have a percentage, calculate the Baht change
             if (changeValue == 0.0 && rawAbsText.contains("%") && lastPrice != 0.0) {
                 val p = extractNumeric(rawAbsText)
                 changeValue = (lastPrice * (p/100.0))
             }
        }

        Log.v(TAG, "Google Data Raw [$symbol]: Price=$lastPrice, Label=$mainLabel, ChangeValue=$changeValue, PercentValue=$percentValue")

        // Direction
        val combined = "$mainLabel $changeText $percentText".lowercase()
        val isNegative = combined.contains("down") || combined.contains("-") || combined.contains("跌") || combined.contains("decrease")
        
        if (isNegative) {
            changeValue = -Math.abs(changeValue)
            percentValue = -Math.abs(percentValue)
        }

        var peVal: Double? = null
        var pbvVal: Double? = null
        var divYield: Double? = null

        mainContainer.select(".P6uYm").forEach { row ->
            val label = row.text().lowercase()
            val valueText = row.nextElementSibling()?.text() ?: ""
            val value = valueText.replace(",", "").replace("%", "").trim().toDoubleOrNull()
            
            when {
                label.contains("p/e ratio") -> peVal = value
                label.contains("p/b ratio") -> pbvVal = value
                label.contains("dividend yield") -> divYield = value
            }
        }

        return ScrapedStockInfo(
            symbol = symbol.uppercase(),
            name = companyName,
            businessDescription = null,
            lastPrice = lastPrice,
            change = changeValue,
            percentChange = percentValue,
            pe = peVal,
            pbv = pbvVal,
            dividendYield = divYield,
            lastUpdated = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        )
    }

    private fun extractNumeric(text: String): Double {
        val regex = "([0-9]+[,.]?[0-9]*)".toRegex()
        val match = regex.find(text.replace(",", ""))
        return match?.value?.toDoubleOrNull() ?: 0.0
    }

    private fun tryFetchFromSet(symbol: String): ScrapedStockInfo? {
        return try {
            val url = "$SET_FACTSHEET_URL/${symbol.uppercase()}/factsheet"
            Log.v(TAG, "Fetching SET Factsheet: $url")
            val doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(15000).get()

            val scripts = doc.select("script")
            var nuxtInfo: ScrapedStockInfo? = null
            for (script in scripts) {
                val content = script.html()
                if (content.contains("window.__NUXT__")) {
                    Log.v(TAG, "Found window.__NUXT__ script for $symbol")
                    nuxtInfo = parseNuxtJson(content, symbol.uppercase())
                    break
                }
            }

            if (nuxtInfo != null && !nuxtInfo.name.isNullOrBlank()) {
                Log.d(TAG, "SET Nuxt Parse SUCCESS for $symbol: Price=${nuxtInfo.lastPrice}, PE=${nuxtInfo.pe}, PBV=${nuxtInfo.pbv}")
                return nuxtInfo
            }

            Log.v(TAG, "SET Nuxt Parse failed or incomplete for $symbol, trying DOM fallbacks")
            val nameVal = doc.select(".factsheet-title").first()?.text()
                ?: doc.select(".company-name").first()?.text()
                ?: doc.select("h1").first()?.text()
            
            val priceVal = doc.select("label:contains(Price) + span").first()?.text()
                ?: doc.select("label:contains(Price)").first()?.nextElementSibling()?.text()

            val peVal = doc.select("label:contains(P/E (X)) + span").first()?.text()
                ?: doc.select("label:contains(P/E)").first()?.nextElementSibling()?.text()

            val pbvVal = doc.select("label:contains(P/BV (X)) + span").first()?.text()
                ?: doc.select("label:contains(P/BV)").first()?.nextElementSibling()?.text()

            Log.d(TAG, "SET DOM Fallback for $symbol: Name=$nameVal, Price=$priceVal, PE=$peVal, PBV=$pbvVal")

            ScrapedStockInfo(
                symbol = symbol.uppercase(),
                name = cleanName(nameVal),
                lastPrice = priceVal?.replace(",", "")?.toDoubleOrNull() ?: 0.0,
                change = 0.0,
                percentChange = 0.0,
                pe = peVal?.replace(",", "")?.toDoubleOrNull(),
                pbv = pbvVal?.replace(",", "")?.toDoubleOrNull(),
                lastUpdated = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            )
        } catch (e: Exception) {
            Log.e(TAG, "SET Fetch Error for $symbol", e)
            null
        }
    }

    private fun parseNuxtJson(scriptContent: String, symbol: String): ScrapedStockInfo? {
        try {
            val paramMap = extractNuxtParamMap(scriptContent) ?: return null

            // Extract main data blocks (could be objects or arrays)
            val hData = extractNuxtDataBlock(scriptContent, "highlightData")
            val pData = extractNuxtDataBlock(scriptContent, "profile")
            val cData = extractNuxtDataBlock(scriptContent, "compareStat")
            val fData = extractNuxtDataBlock(scriptContent, "financialData")
            
            if (hData != null) {
                // Market Ratios (highlightData)
                val peStr = resolveValue(hData, "peRatio", paramMap)
                val pbvStr = resolveValue(hData, "pbRatio", paramMap)
                val priceStr = resolveValue(hData, "price", paramMap)
                val yieldStr = resolveValue(hData, "dividendYield", paramMap)
                
                // Fundamental Metrics (compareStat and financialData)
                val roeStr = cData?.let { resolveLatestValue(it, "roe", paramMap) }
                val netProfitStr = fData?.let { resolveLatestValue(it, "netProfit", paramMap) }
                val equityStr = fData?.let { resolveLatestValue(it, "totalEquity", paramMap) }
                val deStr = fData?.let { resolveLatestValue(it, "debtToEquityRatio", paramMap) }
                val epsStr = fData?.let { resolveLatestValue(it, "eps", paramMap) }

                val nameRaw = pData?.let { resolveValue(it, "name", paramMap) ?: resolveValue(it, "nameTH", paramMap) }
                
                Log.v(TAG, "Nuxt Resolved for $symbol: PE=$peStr, PBV=$pbvStr, Price=$priceStr, ROE=$roeStr, DE=$deStr, Yield=$yieldStr")

                return ScrapedStockInfo(
                    symbol = symbol,
                    name = cleanName(nameRaw),
                    lastPrice = priceStr?.toDoubleOrNull() ?: 0.0,
                    change = 0.0,
                    percentChange = 0.0,
                    pe = peStr?.toDoubleOrNull(),
                    pbv = pbvStr?.toDoubleOrNull(),
                    roe = roeStr?.toDoubleOrNull(),
                    eps = epsStr?.toDoubleOrNull(),
                    netProfit = netProfitStr?.toDoubleOrNull(),
                    equity = equityStr?.toDoubleOrNull(),
                    debtToEquity = deStr?.toDoubleOrNull(),
                    dividendYield = yieldStr?.toDoubleOrNull(),
                    dividendDate = null,
                    lastUpdated = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseNuxtJson failed for $symbol", e)
        }
        return null
    }

    private fun extractNuxtParamMap(scriptContent: String): Map<String, String>? {
        val functionStart = scriptContent.indexOf("(function(")
        if (functionStart == -1) return null
        
        val paramStart = functionStart + 10
        val paramEnd = scriptContent.indexOf("){", paramStart)
        if (paramEnd == -1) return null
        val paramsPart = scriptContent.substring(paramStart, paramEnd)
        val params = paramsPart.split(",")
        
        val argsEnd = scriptContent.lastIndexOf("));")
        if (argsEnd == -1) return null
        
        var braceLevel = 0
        var argsStart = -1
        for (i in argsEnd - 1 downTo 0) {
            val iChar = scriptContent[i]
            if (iChar == ')') braceLevel++
            else if (iChar == '(') {
                if (braceLevel == 0) {
                    argsStart = i + 1
                    break
                }
                braceLevel--
            }
        }
        
        if (argsStart == -1) return null
        val argsPart = scriptContent.substring(argsStart, argsEnd)
        val args = parseArgs(argsPart)
        
        if (params.size != args.size) {
             Log.w(TAG, "Param/Arg size mismatch: Params=${params.size}, Args=${args.size}")
        }
        
        return params.zip(args).toMap()
    }

    private fun extractNuxtDataBlock(script: String, key: String): String? {
        val pattern = Pattern.compile("${Pattern.quote(key)}:([\\{\\[])")
        val matcher = pattern.matcher(script)
        
        if (matcher.find()) {
             val startPos = matcher.start(1)
             val startChar = matcher.group(1)
             val endChar = if (startChar == "{") "}" else "]"
             var balance = 0
             for (i in startPos until script.length) {
                 val c = script[i].toString()
                 if (c == startChar) balance++
                 else if (c == endChar) {
                     balance--
                     if (balance == 0) {
                         return script.substring(startPos + 1, i)
                     }
                 }
             }
        }
        return null
    }

    private fun parseArgs(argsStr: String): List<String> {
        val args = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var bracketLevel = 0
        var braceLevel = 0
        
        var i = 0
        while (i < argsStr.length) {
            val c = argsStr[i]
            when {
                c == '\"' -> inQuotes = !inQuotes
                !inQuotes && c == '[' -> bracketLevel++
                !inQuotes && c == ']' -> bracketLevel--
                !inQuotes && c == '{' -> braceLevel++
                !inQuotes && c == '}' -> braceLevel--
                !inQuotes && c == ',' && bracketLevel == 0 && braceLevel == 0 -> {
                    args.add(current.toString().trim())
                    current.setLength(0)
                    i++
                    continue
                }
            }
            current.append(c)
            i++
        }
        args.add(current.toString().trim())
        return args
    }

    private fun resolveValue(data: String, key: String, paramMap: Map<String, String>): String? {
        val pattern = Pattern.compile("$key:([^,}\\]]*)")
        val matcher = pattern.matcher(data)
        if (matcher.find()) {
            val v = matcher.group(1).trim()
            if (v.startsWith("\"") && v.endsWith("\"")) return v.substring(1, v.length - 1)
            if (v.toDoubleOrNull() != null) return v
            
            val resolved = paramMap[v]
            if (resolved != null) {
                if (resolved.startsWith("\"") && resolved.endsWith("\"")) return resolved.substring(1, resolved.length - 1)
                return if (resolved == "void 0" || resolved == "null" || resolved == "a") null else resolved
            }
            return v
        }
        return null
    }

    private fun resolveLatestValue(data: String, key: String, paramMap: Map<String, String>): String? {
        // Find the specific key pattern, skipping arrays if necessary
        val pattern = Pattern.compile("$key:\\[*([^\\]}]*)")
        val matcher = pattern.matcher(data)
        var latestValue: String? = null
        while (matcher.find()) {
            val listContent = matcher.group(1).trim()
            val raw = listContent.split(",").last().trim()
            
            if (raw.toDoubleOrNull() != null) {
                latestValue = raw
            } else {
                val resolved = paramMap[raw]
                if (resolved != null && resolved != "void 0" && resolved != "null" && resolved != "a") {
                    latestValue = if (resolved.startsWith("\"") && resolved.endsWith("\"")) resolved.substring(1, resolved.length - 1) else resolved
                }
            }
        }
        return latestValue
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

    private fun cleanName(name: String?): String? {
        if (name == null) return null
        val unwanted = listOf("Back to Google Finance", "Google Finance", "Search", "Sign in")
        var cleaned = name.trim()
        unwanted.forEach { 
            if (cleaned.contains(it, ignoreCase = true)) {
                cleaned = cleaned.replace(it, "", ignoreCase = true).trim()
            }
        }
        return if (cleaned.isEmpty() || cleaned == "Google" || cleaned == "-") null else cleaned
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

    /**
     * Returns a curated list of interesting symbols by category.
     * This acts as a reliable resource fallback when web scraping fails.
     */
    fun getCuratedCollection(category: String): List<String> {
        return when (category.uppercase()) {
            "DIVIDEND" -> listOf(
                "ADVANC", "BBL", "CPALL", "EGCO", "INTUCH", "KBANK", "KTB", "LH", "PTT", 
                "PTTEP", "RATCH", "SCB", "SCC", "TISCO", "TOP", "TU", "WHA"
            )
            "BLUECHIP" -> listOf(
                "AOT", "BBL", "BDMS", "CPALL", "DELTA", "GULF", "KBANK", "PTT", "PTTEP", "SCB", "SCC"
            )
            "SET100" -> listOf(
                "AAV", "ADVANC", "AEONTS", "AMATA", "AOT", "AP", "AURA", "AWC", "BA", "BAM",
                "BANPU", "BBL", "BCH", "BCP", "BCPG", "BDMS", "BEM", "BGRIM", "BH", "BJC",
                "BLA", "BPP", "BTG", "CBG", "CENTEL", "CHG", "CK", "COM7", "CPALL", "CPF",
                "CPN", "CRC", "DELTA", "DOHOME", "EA", "EGCO", "ERW", "FORTH", "GFPT", "GLOBAL",
                "GPSC", "GULF", "GUNKUL", "HANA", "HMPRO", "ICHI", "INTUCH", "IRPC", "ITC", "IVL",
                "JMART", "JMT", "JTS", "KBANK", "KCE", "KKP", "KTB", "KTC", "LH", "M",
                "MASTER", "MEGA", "MINT", "MTC", "OR", "ORI", "OSP", "PLANB", "PR9", "PTG",
                "PTT", "PTTEP", "PTTGC", "QH", "RATCH", "RBF", "SAWAD", "SCB", "SCC", "SCGP",
                "SIRI", "SPALI", "SPRC", "STA", "STECON", "STGT", "TASCO", "TCAP", "THG", "TIDLOR",
                "TIPH", "TISCO", "TLI", "TOA", "TOP", "TPIPL", "TPIPP", "TRUE", "TTB", "TU", "VGI", "WHA"
            )
            else -> emptyList()
        }
    }
}
