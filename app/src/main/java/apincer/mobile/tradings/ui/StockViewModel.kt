package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.data.FocusEntity
import apincer.mobile.tradings.data.ScrapedStockInfo
import apincer.mobile.tradings.data.SetScraper
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.data.StockEntity
import apincer.mobile.tradings.data.StockRepository
import apincer.mobile.tradings.data.TradeEntity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        val portfolio: StockEntity?,
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
    val portfolio: StockEntity,
    val netProfitPercent: Double = 0.0,
    val signal: TradeSignal? = null,
    val isFocused: Boolean = false,
    val focusStartPrice: Double? = null,
    val focusTargetPrice: Double? = null,
    val focusMovementPercent: Double? = null
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
        database.stockDao(), 
        database.tradeDao(), 
        database.cashDao(), 
        database.focusDao(), 
        database.checklistDao()
    )
    private val preferenceRepository = apincer.mobile.tradings.data.PreferenceRepository(application)


    val targetMonthlyDividend: StateFlow<Double> = 
        preferenceRepository.targetMonthlyDividend.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10000.0
        )

    fun updateTargetMonthlyDividend(amount: Double) {
        viewModelScope.launch {
            preferenceRepository.setTargetMonthlyDividend(amount)
        }
    }

    val priceAlertThreshold: StateFlow<Double> = 
        preferenceRepository.priceAlertThreshold.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10.0
        )

    fun updatePriceAlertThreshold(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setPriceAlertThreshold(percent)
        }
    }

    val dividendAlertWindow: StateFlow<Int> = 
        preferenceRepository.dividendAlertWindow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 14
        )

    fun updateDividendAlertWindow(days: Int) {
        viewModelScope.launch {
            preferenceRepository.setDividendAlertWindow(days)
        }
    }

    val isDividendAlertEndYear: StateFlow<Boolean> = 
        preferenceRepository.isDividendAlertEndYear.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleDividendAlertEndYear() {
        viewModelScope.launch {
            preferenceRepository.setDividendAlertEndYear(!isDividendAlertEndYear.value)
        }
    }

    val isPrivacyMode: StateFlow<Boolean> = 
        preferenceRepository.isPrivacyMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun togglePrivacyMode() {
        viewModelScope.launch {
            preferenceRepository.setPrivacyMode(!isPrivacyMode.value)
        }
    }

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
                    stocks = stocks,
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

    val maxRiskPerTrade: StateFlow<Double> = 
        preferenceRepository.maxRiskPerTrade.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0
        )

    fun updateMaxRiskPerTrade(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setMaxRiskPerTrade(percent)
        }
    }

    val maxOpenExposure: StateFlow<Double> = 
        preferenceRepository.maxOpenExposure.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5.0
        )

    fun updateMaxOpenExposure(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setMaxOpenExposure(percent)
        }
    }

    val maxPortfolioAllocation: StateFlow<Double> = 
        preferenceRepository.maxPortfolioAllocation.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10.0
        )

    fun updateMaxPortfolioAllocation(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setMaxPortfolioAllocation(percent)
        }
    }

    val minRiskRewardRatio: StateFlow<Double> = 
        preferenceRepository.minRiskRewardRatio.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 2.0
        )

    fun updateMinRiskRewardRatio(ratio: Double) {
        viewModelScope.launch {
            preferenceRepository.setMinRiskRewardRatio(ratio)
        }
    }

    private val _uiState = MutableStateFlow<StockUiState>(StockUiState.Initial)
    val uiState: StateFlow<StockUiState> = _uiState

    private val _searchResults = MutableStateFlow<List<ScrapedStockInfo>>(emptyList())
    val searchResults: StateFlow<List<ScrapedStockInfo>> = _searchResults

    val watchlistInfo: StateFlow<List<StockWatchlistInfo>> = 
        combine(repository.allStocks, repository.allFocusStocks) { stocks, focusStocks ->
            stocks.map { stock ->
                val focus = focusStocks.find { it.symbol == stock.symbol }
                val info = ScrapedStockInfo(
                    symbol = stock.symbol,
                    name = stock.name,
                    nameTH = stock.nameTH,
                    businessDescription = stock.businessDescription,
                    sector = stock.sector,
                    industry = stock.industry,
                    lastPrice = stock.lastPrice,
                    change = stock.change,
                    percentChange = stock.percentChange,
                    pe = stock.pe,
                    pbv = stock.pbv,
                    roe = stock.roe,
                    eps = stock.eps,
                    netProfit = stock.netProfit,
                    equity = stock.equity,
                    debtToEquity = stock.debtToEquity,
                    dividendYield = stock.dividendYield,
                    dividendDate = stock.dividendDate,
                    lastUpdated = stock.lastUpdated ?: ""
                )
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
                        type = IndicatorSignal.valueOf(stock.signalType),
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
                    info = stock?.let {
                        ScrapedStockInfo(
                            symbol = it.symbol,
                            name = it.name,
                            lastPrice = it.lastPrice,
                            change = it.change,
                            percentChange = it.percentChange,
                            lastUpdated = it.lastUpdated ?: ""
                        )
                    }
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val cashBalance: StateFlow<Double> = 
        repository.cashBalance.map { it?.balance ?: 0.0 }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val tradeHistory: StateFlow<List<TradeEntity>> = 
        repository.allTrades.stateIn(
            scope = viewModelScope, 
            started = SharingStarted.Lazily, 
            initialValue = emptyList()
        )

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
        val disciplineDateTime = if (now.toLocalTime().isBefore(java.time.LocalTime.of(9, 30))) {
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
            updated = updated.copy(swingDailyDone = false)
        }
        // Reset weekly if week changed
        if (existing.lastResetWeek != currentWeek) {
            updated = updated.copy(
                swingWeeklyDone = false,
                swingAiDone = false,
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
            try {
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
                            repository.updateStockCache(original.copy(
                                name = updated.name ?: original.name,
                                lastPrice = updated.lastPrice,
                                change = updated.change,
                                percentChange = updated.percentChange,
                                pe = updated.pe ?: original.pe,
                                pbv = updated.pbv ?: original.pbv,
                                dividendYield = updated.dividendYield ?: original.dividendYield,
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
                                            apincer.mobile.tradings.data.ScrapedStockInfo(
                                                symbol = latestStock.symbol,
                                                name = latestStock.name,
                                                nameTH = latestStock.nameTH,
                                                businessDescription = latestStock.businessDescription,
                                                sector = latestStock.sector,
                                                industry = latestStock.industry,
                                                lastPrice = latestStock.lastPrice,
                                                change = latestStock.change,
                                                percentChange = latestStock.percentChange,
                                                pe = latestStock.pe,
                                                pbv = latestStock.pbv,
                                                roe = latestStock.roe,
                                                eps = latestStock.eps,
                                                netProfit = latestStock.netProfit,
                                                debtToEquity = latestStock.debtToEquity,
                                                dividendYield = latestStock.dividendYield,
                                                dividendDate = latestStock.dividendDate,
                                                lastUpdated = latestStock.lastUpdated ?: ""
                                            )
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
                                                type = IndicatorSignal.valueOf(latestStock.signalType ?: "NEUTRAL"),
                                                reason = latestStock.signalReason ?: "",
                                                description = latestStock.signalDescription ?: ""
                                            )
                                        }

                                        repository.updateStockCache(
                                            latestStock.copy(
                                                name = info.name ?: latestStock.name,
                                                businessDescription = info.businessDescription ?: latestStock.businessDescription,
                                                sector = info.sector ?: latestStock.sector,
                                                industry = info.industry ?: latestStock.industry,
                                                lastPrice = info.lastPrice,
                                                change = info.change,
                                                percentChange = info.percentChange,
                                                pe = info.pe ?: latestStock.pe,
                                                pbv = info.pbv ?: latestStock.pbv,
                                                roe = info.roe,
                                                eps = info.eps,
                                                netProfit = info.netProfit,
                                                equity = info.equity,
                                                debtToEquity = info.debtToEquity,
                                                dividendYield = info.dividendYield ?: latestStock.dividendYield,
                                                dividendDate = info.dividendDate,
                                                dividendPerShare = if (info.dividendYield != null && info.lastPrice != 0.0) {
                                                    info.lastPrice * (info.dividendYield / 100.0)
                                                } else latestStock.dividendPerShare,
                                                rsi = if (needsIndicators) indicators.rsi else latestStock.rsi,
                                                macdHist = if (needsIndicators) indicators.histogram else latestStock.macdHist,
                                                signalType = signal.type.name,
                                                signalReason = signal.reason,
                                                signalDescription = signal.description,
                                                lastUpdated = info.lastUpdated.takeIf { it.isNotBlank() } ?: latestStock.lastUpdated
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

    fun updateCashBalance(amount: Double) {
        viewModelScope.launch {
            repository.updateCash(amount)
        }
    }

    fun adjustCash(amount: Double) {
        viewModelScope.launch {
            val current = cashBalance.value
            repository.updateCash(current + amount)
        }
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
            var fees = 0.0
            // Deduct cash if it's a purchase (quantity > 0)
            if (quantity > 0) {
                val totalCostRaw = cost * quantity
                fees = TechnicalAnalysis.calculateFees(totalCostRaw, false)
                adjustCash(-(totalCostRaw + fees))
            }
            repository.addStock(
                symbol = symbol, 
                cost = cost, 
                quantity = quantity, 
                tradePurpose = tradePurpose, 
                buyFees = fees,
                stopLoss = stopLoss,
                playbookNote = playbookNote
            )
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

    fun recordSell(symbol: String, sellPrice: Double, sellQuantity: Int, note: String = "") {
        viewModelScope.launch {
            val item = watchlistInfo.value.find { it.info.symbol == symbol.uppercase() }
            if (item != null && item.portfolio.quantity >= sellQuantity) {
                val buyPrice = item.portfolio.cost
                val totalCostRaw = buyPrice * sellQuantity
                val sellValueRaw = sellPrice * sellQuantity

                // Buying fees already paid when adding to portfolio, we only pay selling fees now
                val sellFees = TechnicalAnalysis.calculateFees(sellValueRaw, true)
                val buyFees = if (item.portfolio.quantity > 0) {
                    (item.portfolio.buyFees * sellQuantity.toDouble()) / item.portfolio.quantity
                } else 0.0
                val totalFees = buyFees + sellFees

                val netProfitValue = (sellValueRaw - totalCostRaw) - totalFees
                val netProfitPercent = ((sellValueRaw - sellFees - (totalCostRaw + buyFees)) / (totalCostRaw + buyFees)) * 100

                // Add proceeds to cash (after selling fees and tax)
                val proceeds = sellValueRaw - sellFees
                adjustCash(proceeds)

                repository.insertTrade(
                    TradeEntity(
                        symbol = symbol.uppercase(),
                        buyPrice = buyPrice,
                        sellPrice = sellPrice,
                        quantity = sellQuantity,
                        netProfitPercent = netProfitPercent,
                        netProfitBaht = netProfitValue,
                        dateMillis = System.currentTimeMillis(),
                        note = note
                    )
                )

                if (item.portfolio.quantity == sellQuantity) {
                    repository.removeStock(symbol)
                } else {
                    val remainingQty = item.portfolio.quantity - sellQuantity
                    val remainingBuyFees = (item.portfolio.buyFees * remainingQty.toDouble()) / item.portfolio.quantity
                    repository.addStock(
                        symbol = symbol, 
                        cost = buyPrice, 
                        quantity = remainingQty,
                        tradePurpose = item.portfolio.tradePurpose,
                        buyFees = remainingBuyFees
                    )
                }
            }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            val item = watchlistInfo.value.find { it.info.symbol == symbol.uppercase() }
            if (item != null && item.portfolio.quantity > 0) {
                // If it was in portfolio, record the trade history as a "Sale" at current market price
                val totalCostRaw = item.portfolio.cost * item.portfolio.quantity
                val currentTotalValue = item.info.lastPrice * item.portfolio.quantity

                val buyFees = TechnicalAnalysis.calculateFees(totalCostRaw, false)
                val sellFees = TechnicalAnalysis.calculateFees(currentTotalValue, true)
                val stockTotalFees = buyFees + sellFees
                val netProfitValue = (currentTotalValue - totalCostRaw) - stockTotalFees

                // Add proceeds to cash
                val proceeds = currentTotalValue - sellFees
                adjustCash(proceeds)

                repository.insertTrade(
                    TradeEntity(
                        symbol = symbol.uppercase(),
                        buyPrice = item.portfolio.cost,
                        sellPrice = item.info.lastPrice,
                        quantity = item.portfolio.quantity,
                        netProfitPercent = item.netProfitPercent,
                        netProfitBaht = netProfitValue,
                        dateMillis = System.currentTimeMillis(),
                        note = "Stock removed from watchlist"
                    )
                )
            }
            repository.removeStock(symbol)
        }
    }

    fun resetToInitial() {
        _uiState.value = StockUiState.Initial
    }

    fun clearTradeHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun importFromCollection(category: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val symbols = withContext(Dispatchers.IO) {
                SetScraper.getCuratedCollection(category)
            }
            android.util.Log.d("StockViewModel", "Importing ${symbols.size} symbols from $category collection")
            symbols.forEach { symbol ->
                repository.addStockIfMissing(symbol)
            }
            // Trigger refresh after import to get prices for new stocks
            _isRefreshing.value = false
            refreshWatchlistInfo()
        }
    }

    fun resetSearchResults() {
        _searchResults.value = emptyList()
    }

    fun searchStocks(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                SetScraper.searchYahoo(query)
            }
            _searchResults.value = result
        }
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
                    val prices = history.map { it.close }.reversed()
                    val volumes = history.map { it.volume }.reversed()

                    val sma50 = TechnicalAnalysis.calculateSMA(prices, 50)
                    val sma200 = TechnicalAnalysis.calculateSMA(prices, 200)
                    val bb = TechnicalAnalysis.calculateBollingerBands(prices)
                    val isVolumeSurge = TechnicalAnalysis.isVolumeSurge(volumes)

                    // Calculate Returns for different periods
                    val returns = mutableMapOf<Int, Double>()
                    val periods = listOf(3, 7, 15, 30)
                    periods.forEach { days ->
                        if (prices.size >= days + 1) {
                            val current = prices.first()
                            val past = prices[days]
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

                    val buyPriceTarget = TechnicalAnalysis.estimatePriceForRSI(prices.reversed(), 35.0)
                    val sellPriceTarget = TechnicalAnalysis.estimatePriceForRSI(prices.reversed(), 65.0)

                    val focusEntry = repository.getFocusStock(symbol)
                    val isFocused = focusEntry != null
                    val focusStart = focusEntry?.startPrice ?: 0.0
                    val focusTarget = focusEntry?.targetPrice ?: 0.0

                    // Optional: Update cache for this single stock too
                    if (local != null) {
                        repository.updateStockCache(local.copy(
                            name = updatedInfo.name,
                            businessDescription = updatedInfo.businessDescription,
                            sector = updatedInfo.sector ?: local.sector,
                            industry = updatedInfo.industry ?: local.industry,
                            lastPrice = updatedInfo.lastPrice,
                            change = updatedInfo.change,
                            percentChange = updatedInfo.percentChange,
                            pe = updatedInfo.pe,
                            pbv = updatedInfo.pbv,
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

    private fun isCacheExpired(lastUpdated: String?): Boolean {
        if (lastUpdated.isNullOrBlank()) return true
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val date = format.parse(lastUpdated) ?: return true
            val diffMs = System.currentTimeMillis() - date.time
            diffMs > 24L * 60L * 60L * 1000L // 24 Hours
        } catch (e: Exception) {
            true
        }
    }

    private fun isTechnicalCacheExpired(lastUpdated: String?): Boolean {
        if (lastUpdated.isNullOrBlank()) return true
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val date = format.parse(lastUpdated) ?: return true
            val diffMs = System.currentTimeMillis() - date.time
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

