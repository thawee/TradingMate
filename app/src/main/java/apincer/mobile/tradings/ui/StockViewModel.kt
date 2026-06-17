package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.data.ScrapedStockInfo
import apincer.mobile.tradings.data.SetScraper
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.data.StockAggregate
import apincer.mobile.tradings.data.StockRepository
import apincer.mobile.tradings.data.TradingBackup
import apincer.mobile.tradings.data.ChecklistEntity
import apincer.mobile.tradings.domain.BollingerBands
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TechnicalAnalysis
import apincer.mobile.tradings.domain.TradeSignal
import apincer.mobile.tradings.domain.TradingZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

sealed class StockUiState {
    object Initial : StockUiState()
    object Loading : StockUiState()
    data class Success(
        val stockInfo: ScrapedStockInfo,
        val portfolio: StockAggregate?,
        val sma50: Double?,
        val sma200: Double?,
        val bb: BollingerBands?,
        val isVolumeSurge: Boolean,
        val rsi: Double?,
        val macd: Triple<Double?, Double?, Double?>,
        val signal: TradeSignal,
        val netProfitPercent: Double? = null,
        val returns: Map<Int, Double> = emptyMap(),
        val zone: TradingZone,
        val buyingPrice: Double? = null,
        val sellingPrice: Double? = null,
        val isFocused: Boolean = false,
        val focusStartPrice: Double = 0.0,
        val focusTargetPrice: Double = 0.0,
        val historicalPrices: List<Double> = emptyList()
    ) : StockUiState()
    data class Error(val message: String, val symbol: String) : StockUiState()
}

data class StockWatchlistInfo(
    val info: ScrapedStockInfo,
    val portfolio: StockAggregate,
    val netProfitPercent: Double = 0.0,
    val signal: TradeSignal? = null,
    val isFocused: Boolean = false,
    val focusStartPrice: Double? = null,
    val focusTargetPrice: Double? = null,
    val focusMovementPercent: Double? = null,
    val buyPriceTarget: Double? = null,
    val sellPriceTarget: Double? = null
)

data class StockFocusInfo(
    val symbol: String,
    val startPrice: Double,
    val targetPrice: Double,
    val currentPrice: Double,
    val movementPercent: Double,
    val addedAtMillis: Long,
    val info: ScrapedStockInfo? = null
)

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val database = StockDatabase.getDatabase(application)
    private val repository = StockRepository(
        database,
        database.stockDao(), 
        database.tradeDao(), 
        database.cashDao(), 
        database.focusDao(), 
        database.checklistDao()
    )
    private val preferenceRepository = apincer.mobile.tradings.data.PreferenceRepository(application)


    fun exportBackup(
        contentResolver: android.content.ContentResolver,
        uri: android.net.Uri,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stocks = repository.getAllStocksSync()
                val trades = repository.getAllTradesSync()
                val focusList = repository.getAllFocusStocksSync()
                val cashBalanceVal = repository.getCashSync()?.balance ?: 0.0
                
                val backup = TradingBackup(
                    portfolios = stocks.map { it.portfolio },
                    caches = stocks.mapNotNull { it.cache },
                    signals = stocks.mapNotNull { it.signal },
                    focusList = focusList,
                    trades = trades,
                    cashBalance = cashBalanceVal
                )
                
                val jsonString = Json.encodeToString(TradingBackup.serializer(), backup)
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                } ?: throw java.io.IOException("Failed to open output stream")
                
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    fun importBackup(
        contentResolver: android.content.ContentResolver,
        uri: android.net.Uri,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes().decodeToString()
                } ?: throw java.io.IOException("Failed to open input stream")
                
                val backup = Json.decodeFromString(TradingBackup.serializer(), jsonString)
                repository.restoreBackup(backup)
                
                // Trigger refresh to update UI State Flows
                refreshWatchlistInfo()
                
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    private val _uiState = MutableStateFlow<StockUiState>(StockUiState.Initial)
    val uiState: StateFlow<StockUiState> = _uiState

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError

    fun clearRefreshError() {
        _refreshError.value = null
    }

    private val _searchResults = MutableStateFlow<List<ScrapedStockInfo>>(emptyList())
    val searchResults: StateFlow<List<ScrapedStockInfo>> = _searchResults

    val watchlistInfo: StateFlow<List<StockWatchlistInfo>> = 
        combine(repository.allStocks, repository.allFocusStocks) { stocks, focusStocks ->
            stocks.map { stock ->
                val focus = focusStocks.find { it.symbol == stock.symbol }
                val info = stock.toScrapedStockInfo()
                val netProfit = if (stock.cost > 0) {
                    TechnicalAnalysis.calculateNetProfitPercent(stock.cost, stock.lastPrice)
                } else 0.0

                val signal = if (stock.rsi != null && stock.macdHist != null && stock.lastPrice > 0) {
                    TechnicalAnalysis.getDetailedSignal(
                        rsi = stock.rsi,
                        macdHist = stock.macdHist,
                        lastPrice = stock.lastPrice,
                        sma50 = null,
                        sma200 = null,
                        bb = null,
                        isVolumeSurge = false,
                        userCost = if (stock.cost > 0) stock.cost else null,
                        isFundamentalGood = false,
                        tradePurpose = stock.tradePurpose,
                        dividendYield = stock.dividendYield,
                        roe = stock.roe
                    )
                } else if (stock.signalType != null) {
                    TradeSignal(
                        type = runCatching { IndicatorSignal.valueOf(stock.signalType!!) }.getOrDefault(IndicatorSignal.NEUTRAL),
                        reason = stock.signalReason ?: "",
                        description = stock.signalDescription ?: ""
                    )
                } else null

                val focusMovement = if (focus != null && focus.startPrice != 0.0) {
                    ((stock.lastPrice - focus.startPrice) / focus.startPrice) * 100
                } else null

                StockWatchlistInfo(
                    info = info, 
                    portfolio = stock, 
                    netProfitPercent = netProfit, 
                    signal = signal,
                    isFocused = focus != null,
                    focusStartPrice = focus?.startPrice,
                    focusTargetPrice = focus?.targetPrice,
                    focusMovementPercent = focusMovement
                )
            }.sortedWith(
                compareByDescending<StockWatchlistInfo> { it.portfolio.quantity > 0 }
                    .thenByDescending { if (it.portfolio.quantity > 0) it.netProfitPercent else 0.0 }
                    .thenBy { it.info.symbol }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val focusListInfo: StateFlow<List<StockFocusInfo>> = 
        combine(repository.allFocusStocks, repository.allStocks) { focusStocks, stocks ->
            focusStocks.map { focus ->
                val stock = stocks.find { it.symbol == focus.symbol }
                val currentPrice = stock?.lastPrice ?: focus.startPrice
                val movement = if (focus.startPrice != 0.0) ((currentPrice - focus.startPrice) / focus.startPrice) * 100 else 0.0

                StockFocusInfo(
                    symbol = focus.symbol,
                    startPrice = focus.startPrice,
                    targetPrice = focus.targetPrice,
                    currentPrice = currentPrice,
                    movementPercent = movement,
                    addedAtMillis = focus.addedAtMillis,
                    info = stock?.toScrapedStockInfo()
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing



    private val _checklist = MutableStateFlow(ChecklistEntity())
    val checklist: StateFlow<ChecklistEntity> = _checklist

    fun updateChecklistState(update: (ChecklistEntity) -> ChecklistEntity) {
        viewModelScope.launch {
            val current = _checklist.value
            val next = update(current)
            repository.updateChecklist(next)
        }
    }

    private fun checkAndResetChecklist(existing: ChecklistEntity): ChecklistEntity {
        val zoneId = java.time.ZoneId.of("Asia/Bangkok")
        val now = java.time.ZonedDateTime.now(zoneId)
        val disciplineDateTime = if (now.toLocalTime().isBefore(java.time.LocalTime.of(16, 30))) {
            now.minusDays(1)
        } else {
            now
        }
        val todayStr = disciplineDateTime.toLocalDate().toString()
        val currentWeek = disciplineDateTime.get(java.time.temporal.WeekFields.of(java.util.Locale.US).weekOfWeekBasedYear())
        val currentMonth = disciplineDateTime.monthValue

        var updated = existing.copy(
            lastResetDate = todayStr,
            lastResetWeek = currentWeek,
            lastResetMonth = currentMonth
        )

        // Reset daily if date changed
        if (existing.lastResetDate != todayStr) {
            updated = updated.copy(
                swingDailyDone = false,
                swingWeeklyDone = false,
                swingAiDone = false
            )
        }
        // Reset weekly if week changed
        if (existing.lastResetWeek != currentWeek) {
            updated = updated.copy(
                divWeeklyDone = false,
                divWeeklyPricesDone = false
            )
        }
        // Reset monthly if month changed
        if (existing.lastResetMonth != currentMonth) {
            updated = updated.copy(
                divMonthlyDone = false,
                divAiDone = false
            )
        }
        return updated
    }

    init {
        // Observe checklist and handle resets
        viewModelScope.launch {
            repository.checklist.collect { entity ->
                val currentChecklist = entity ?: ChecklistEntity()
                val targetChecklist = checkAndResetChecklist(currentChecklist)
                if (targetChecklist != currentChecklist || entity == null) {
                    repository.updateChecklist(targetChecklist)
                }
                _checklist.value = targetChecklist
            }
        }
        // Trigger background refresh on start
        refreshWatchlistInfo()
    }

    fun refreshWatchlistInfo() {
        viewModelScope.launch {
            if (!_isRefreshing.compareAndSet(false, true)) return@launch
            _refreshError.value = null
            try {
                val isMarketOpen = TechnicalAnalysis.getMarketStatus() != apincer.mobile.tradings.domain.MarketStatus.CLOSED
                val lastSync = watchlistInfo.value.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull()
                if (!isMarketOpen && lastSync != null && !isTechnicalCacheExpired(lastSync)) {
                    android.util.Log.d("StockViewModel", "Market is closed and data is up-to-date. Skipping refresh.")
                    return@launch
                }

                val stocks = watchlistInfo.value.map { it.portfolio }
                if (stocks.isEmpty()) return@launch

                // 1. Ultra-Fast Batch Update (Prices, Changes, basic ratios)
                withContext(Dispatchers.IO) {
                    val batchResults = try {
                        SetScraper.fetchBatchQuotes(stocks.map { it.symbol })
                    } catch (e: Exception) {
                        android.util.Log.e("StockViewModel", "Error fetching batch quotes", e)
                        emptyList()
                    }
                    batchResults.forEach { updated ->
                        stocks.find { it.symbol == updated.symbol }?.let { original ->
                            val cache = original.cache ?: apincer.mobile.tradings.data.StockCacheEntity(original.symbol)
                            repository.updateStockCache(cache.copy(
                                name = updated.name ?: cache.name,
                                lastPrice = updated.lastPrice,
                                change = updated.change,
                                percentChange = updated.percentChange,
                                pe = updated.pe ?: cache.pe,
                                pbv = updated.pbv ?: cache.pbv,
                                dividendYield = updated.dividendYield ?: cache.dividendYield,
                                lastUpdated = updated.lastUpdated
                            ))
                        }
                    }
                }

                // 2. Deep Update (Technical Analysis & Fundamentals from SET)
                val semaphore = Semaphore(3) 
                withContext(Dispatchers.IO) {
                    val jobs = stocks.map { stock ->
                        async {
                            semaphore.withPermit {
                                try {
                                    val latestStock = repository.getStockBySymbol(stock.symbol) ?: stock
                                    val needsDeepFetch = latestStock.roe == null || latestStock.debtToEquity == null || latestStock.sector == null || isCacheExpired(latestStock.lastUpdated)
                                    val needsIndicators = latestStock.rsi == null || latestStock.macdHist == null || isTechnicalCacheExpired(latestStock.lastUpdated)

                                    if (needsDeepFetch || needsIndicators) {
                                        val info = if (needsDeepFetch) {
                                            SetScraper.fetchStockInfo(stock.symbol)
                                        } else {
                                            latestStock.toScrapedStockInfo()
                                        }

                                        val indicators = if (needsIndicators) {
                                            SetScraper.fetchTechnicalIndicators(stock.symbol)
                                        } else {
                                            apincer.mobile.tradings.domain.Indicators(
                                                sma50 = null,
                                                sma200 = null,
                                                rsi = latestStock.rsi,
                                                macd = null,
                                                signal = null,
                                                histogram = latestStock.macdHist,
                                                bollingerBands = null,
                                                isVolumeSurge = false
                                            )
                                        }

                                        val signal = if (needsIndicators) {
                                            TechnicalAnalysis.getDetailedSignal(
                                                rsi = indicators.rsi, 
                                                macdHist = indicators.histogram, 
                                                lastPrice = info.lastPrice, 
                                                sma50 = indicators.sma50,
                                                sma200 = indicators.sma200,
                                                bb = indicators.bollingerBands,
                                                isVolumeSurge = indicators.isVolumeSurge,
                                                userCost = if (stock.cost > 0) stock.cost else null,
                                                isFundamentalGood = info.isFundamentalGood,
                                                tradePurpose = stock.tradePurpose,
                                                dividendYield = info.dividendYield,
                                                roe = info.roe
                                            )
                                        } else {
                                            TradeSignal(
                                                type = runCatching { IndicatorSignal.valueOf(latestStock.signalType ?: "NEUTRAL") }.getOrDefault(IndicatorSignal.NEUTRAL),
                                                reason = latestStock.signalReason ?: "",
                                                description = latestStock.signalDescription ?: ""
                                            )
                                        }

                                        val cache = latestStock.cache ?: apincer.mobile.tradings.data.StockCacheEntity(latestStock.symbol)
                                        repository.updateStockCache(
                                            cache.copy(
                                                name = info.name ?: cache.name,
                                                businessDescription = info.businessDescription ?: cache.businessDescription,
                                                sector = info.sector ?: cache.sector,
                                                industry = info.industry ?: cache.industry,
                                                lastPrice = info.lastPrice,
                                                change = info.change,
                                                percentChange = info.percentChange,
                                                pe = info.pe ?: cache.pe,
                                                pbv = info.pbv ?: cache.pbv,
                                                roe = info.roe,
                                                eps = info.eps,
                                                netProfit = info.netProfit,
                                                netProfitMargin = info.netProfitMargin ?: cache.netProfitMargin,
                                                profitGrowth3Y = info.profitGrowth3Y ?: cache.profitGrowth3Y,
                                                equity = info.equity,
                                                debtToEquity = info.debtToEquity,
                                                dividendYield = info.dividendYield ?: cache.dividendYield,
                                                dividendDate = info.dividendDate,
                                                dividendPerShare = if (info.dividendYield != null && info.lastPrice != 0.0) {
                                                    info.lastPrice * (info.dividendYield / 100.0)
                                                } else cache.dividendPerShare,
                                                lastUpdated = info.lastUpdated.takeIf { it.isNotBlank() } ?: cache.lastUpdated
                                            )
                                        )

                                        val sig = latestStock.signal ?: apincer.mobile.tradings.data.StockSignalEntity(latestStock.symbol)
                                        repository.updateStockSignal(
                                            sig.copy(
                                                rsi = if (needsIndicators) indicators.rsi else sig.rsi,
                                                macdHist = if (needsIndicators) indicators.histogram else sig.macdHist,
                                                signalType = signal.type.name,
                                                signalReason = signal.reason,
                                                signalDescription = signal.description,
                                                lastUpdated = info.lastUpdated.takeIf { it.isNotBlank() } ?: sig.lastUpdated
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("StockViewModel", "Error deep refreshing stock ${stock.symbol}", e)
                                }
                            }
                        }
                    }
                    jobs.awaitAll()
                }
            } catch (e: Exception) {
                android.util.Log.e("StockViewModel", "Error refreshing watchlist info", e)
                _refreshError.value = e.localizedMessage ?: "Failed to refresh watchlist"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun calculateManualPercent(info: ScrapedStockInfo): ScrapedStockInfo {
        return if (info.percentChange == 0.0 && info.lastPrice != 0.0) {
            val prevClose = info.lastPrice - info.change
            val percent = if (prevClose != 0.0) (info.change / prevClose) * 100 else 0.0
            info.copy(percentChange = percent)
        } else info
    }

    fun addToWatchlist(
        symbol: String, 
        cost: Double = 0.0, 
        quantity: Int = 0, 
        tradePurpose: String = "SWING",
        stopLoss: Double = 0.0,
        playbookNote: String = ""
    ) {
        viewModelScope.launch {
            val fees = if (quantity > 0) {
                TechnicalAnalysis.calculateFees(cost * quantity, false)
            } else 0.0
            try {
                repository.executeBuy(symbol, cost, quantity, tradePurpose, fees, stopLoss, playbookNote)
            } catch (e: IllegalStateException) {
                _refreshError.value = e.message
            }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            val item = watchlistInfo.value.find { it.info.symbol == symbol.uppercase() }
            if (item != null && item.portfolio.quantity > 0) {
                try {
                    repository.executeSell(
                        symbol = symbol,
                        sellPrice = item.info.lastPrice,
                        sellQuantity = item.portfolio.quantity,
                        note = "Stock removed from watchlist"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("StockViewModel", "Error selling ${symbol}: ${e.message}", e)
                }
            }
            repository.removeStock(symbol)
        }
    }

    fun addToFocusList(symbol: String, price: Double, targetPrice: Double = 0.0) {
        viewModelScope.launch {
            val normalizedSymbol = symbol.uppercase()
            val existing = repository.getFocusStock(normalizedSymbol)
            val startPrice = existing?.startPrice ?: price
            
            repository.addToFocusList(normalizedSymbol, startPrice, targetPrice)
            repository.addStockIfMissing(normalizedSymbol)
            
            // Update current UI state immediately for reactivity
            val currentState = _uiState.value
            if (currentState is StockUiState.Success && currentState.stockInfo.symbol == normalizedSymbol) {
                _uiState.value = currentState.copy(
                    isFocused = true,
                    focusStartPrice = startPrice,
                    focusTargetPrice = targetPrice
                )
            }
            
            refreshWatchlistInfo()
        }
    }

    fun removeFromFocusList(symbol: String) {
        viewModelScope.launch {
            val normalizedSymbol = symbol.uppercase()
            repository.removeFromFocusList(normalizedSymbol)
            
            // Update current UI state immediately for reactivity
            val currentState = _uiState.value
            if (currentState is StockUiState.Success && currentState.stockInfo.symbol == normalizedSymbol) {
                _uiState.value = currentState.copy(
                    isFocused = false,
                    focusStartPrice = 0.0,
                    focusTargetPrice = 0.0
                )
            }
        }
    }

    fun resetToInitial() {
        _uiState.value = StockUiState.Initial
    }





    fun fetchStockData(symbol: String) {
        if (symbol.isBlank()) {
            resetToInitial()
            return
        }

        viewModelScope.launch {
            _uiState.value = StockUiState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    var info = SetScraper.fetchStockInfo(symbol)

                    // Find if it's in local DB to get cached name/desc
                    val local = watchlistInfo.value.find { it.info.symbol == symbol.uppercase() }?.portfolio

                    val updatedInfo = calculateManualPercent(info.copy(
                        name = info.name ?: local?.name,
                        businessDescription = info.businessDescription ?: local?.businessDescription,
                        sector = info.sector ?: local?.sector,
                        industry = info.industry ?: local?.industry
                    ))

                    val history = SetScraper.fetchHistoricalPrices(symbol)
                    // Prices are in chronological order (oldest→newest) as required by TA functions
                    val prices = history.map { it.close }
                    val volumes = history.map { it.volume }

                    val sma50 = TechnicalAnalysis.calculateSMA(prices, 50)
                    val sma200 = TechnicalAnalysis.calculateSMA(prices, 200)
                    val bb = TechnicalAnalysis.calculateBollingerBands(prices)
                    val isVolumeSurge = TechnicalAnalysis.isVolumeSurge(volumes)

                    // Calculate Returns for different periods (prices are oldest→newest)
                    val returns = mutableMapOf<Int, Double>()
                    val periods = listOf(3, 7, 15, 30)
                    periods.forEach { days ->
                        if (prices.size >= days + 1) {
                            val current = prices.last()
                            val past = prices[prices.size - 1 - days]
                            val ret = ((current - past) / past) * 100
                            returns[days] = Math.round(ret * 100.0) / 100.0
                        }
                    }

                    val rsi = TechnicalAnalysis.calculateRSI(prices, 14)
                    val macd = TechnicalAnalysis.calculateMACD(prices)

                    val netProfit = if (local != null && local.cost > 0) {
                        TechnicalAnalysis.calculateNetProfitPercent(local.cost, updatedInfo.lastPrice)
                    } else null

                    val signal = TechnicalAnalysis.getDetailedSignal(
                        rsi = rsi, 
                        macdHist = macd.third, 
                        lastPrice = updatedInfo.lastPrice, 
                        sma50 = sma50,
                        sma200 = sma200,
                        bb = bb,
                        isVolumeSurge = isVolumeSurge,
                        userCost = local?.cost,
                        isFundamentalGood = updatedInfo.isFundamentalGood,
                        tradePurpose = local?.tradePurpose ?: "SWING",
                        dividendYield = updatedInfo.dividendYield,
                        roe = updatedInfo.roe
                    )

                    val zone = TechnicalAnalysis.getTradingZone(rsi, macd.third, updatedInfo.lastPrice, sma50, sma200, bb)

                    val buyPriceTarget = TechnicalAnalysis.estimatePriceForRSI(prices, 35.0)
                    val sellPriceTarget = TechnicalAnalysis.estimatePriceForRSI(prices, 65.0)

                    val focusEntry = repository.getFocusStock(symbol)
                    val isFocused = focusEntry != null
                    val focusStart = focusEntry?.startPrice ?: 0.0
                    val focusTarget = focusEntry?.targetPrice ?: 0.0

                    // Optional: Update cache for this single stock too
                    if (local != null) {
                        val cache = local.cache ?: apincer.mobile.tradings.data.StockCacheEntity(local.symbol)
                        repository.updateStockCache(cache.copy(
                            name = updatedInfo.name,
                            businessDescription = updatedInfo.businessDescription,
                            sector = updatedInfo.sector ?: cache.sector,
                            industry = updatedInfo.industry ?: cache.industry,
                            lastPrice = updatedInfo.lastPrice,
                            change = updatedInfo.change,
                            percentChange = updatedInfo.percentChange,
                            pe = updatedInfo.pe,
                            pbv = updatedInfo.pbv,
                            netProfitMargin = updatedInfo.netProfitMargin ?: cache.netProfitMargin,
                            profitGrowth3Y = updatedInfo.profitGrowth3Y ?: cache.profitGrowth3Y,
                            lastUpdated = updatedInfo.lastUpdated
                        ))
                        val sig = local.signal ?: apincer.mobile.tradings.data.StockSignalEntity(local.symbol)
                        repository.updateStockSignal(sig.copy(
                            rsi = rsi,
                            macdHist = macd.third,
                            signalType = signal.type.name,
                            signalReason = signal.reason,
                            signalDescription = signal.description,
                            lastUpdated = updatedInfo.lastUpdated
                        ))
                    }

                    StockUiState.Success(
                        updatedInfo, 
                        local, 
                        sma50, 
                        sma200,
                        bb,
                        isVolumeSurge,
                        rsi, 
                        macd, 
                        signal, 
                        netProfit, 
                        returns, 
                        zone,
                        buyPriceTarget,
                        sellPriceTarget,
                        isFocused,
                        focusStart,
                        focusTarget,
                        prices.reversed()
                    )
                }
                _uiState.value = result
            } catch (e: Exception) {
                _uiState.value = StockUiState.Error(e.message ?: "Failed to scrape data", symbol)
            }
        }
    }

    private val cacheDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun isCacheExpired(lastUpdated: String?): Boolean {
        if (lastUpdated.isNullOrBlank()) return true
        return try {
            val dateTime = java.time.LocalDateTime.parse(lastUpdated, cacheDateTimeFormatter)
            val diffMs = System.currentTimeMillis() - java.time.ZoneId.systemDefault().let { dateTime.atZone(it).toInstant().toEpochMilli() }
            diffMs > 24L * 60L * 60L * 1000L // 24 Hours
        } catch (e: Exception) {
            true
        }
    }

    private fun isTechnicalCacheExpired(lastUpdated: String?): Boolean {
        if (lastUpdated.isNullOrBlank()) return true
        return try {
            val dateTime = java.time.LocalDateTime.parse(lastUpdated, cacheDateTimeFormatter)
            val diffMs = System.currentTimeMillis() - java.time.ZoneId.systemDefault().let { dateTime.atZone(it).toInstant().toEpochMilli() }
            val isMarketClosed = TechnicalAnalysis.getMarketStatus() == apincer.mobile.tradings.domain.MarketStatus.CLOSED
            if (isMarketClosed) {
                diffMs > 12L * 60L * 60L * 1000L // 12 Hours
            } else {
                diffMs > 15L * 60L * 1000L // 15 Minutes
            }
        } catch (e: Exception) {
            true
        }
    }
}

