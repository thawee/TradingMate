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
    data class Error(val message: String) : StockUiState()
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

@Serializable
data class TradingBackup(
    val stocks: List<StockEntity>,
    val focusList: List<FocusEntity>,
    val trades: List<TradeEntity>,
    val cashBalance: Double
)

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val database = StockDatabase.getDatabase(application)
    private val repository = StockRepository(database.stockDao(), database.tradeDao(), database.cashDao(), database.focusDao())
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

                val signal = if (stock.signalType != null) {
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

    init {
        // Trigger background refresh on start
        refreshWatchlistInfo()
    }

    fun refreshWatchlistInfo() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            _isRefreshing.value = true

            val stocks = watchlistInfo.value.map { it.portfolio }
            if (stocks.isEmpty()) {
                _isRefreshing.value = false
                return@launch
            }

            // 1. Ultra-Fast Batch Update (Prices, Changes, basic ratios)
            withContext(Dispatchers.IO) {
                val batchResults = SetScraper.fetchBatchQuotes(stocks.map { it.symbol })
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
                                val info = SetScraper.fetchStockInfo(stock.symbol)
                                val indicators = SetScraper.fetchTechnicalIndicators(stock.symbol)

                                val signal = TechnicalAnalysis.getDetailedSignal(
                                    rsi = indicators.rsi, 
                                    macdHist = indicators.histogram, 
                                    lastPrice = info.lastPrice, 
                                    sma50 = indicators.sma50,
                                    sma200 = indicators.sma200,
                                    bb = indicators.bollingerBands,
                                    isVolumeSurge = indicators.isVolumeSurge,
                                    userCost = if (stock.cost > 0) stock.cost else null,
                                    isFundamentalGood = info.isFundamentalGood
                                )

                                repository.updateStockCache(
                                    stock.copy(
                                        name = info.name ?: stock.name,
                                        businessDescription = info.businessDescription ?: stock.businessDescription,
                                        sector = info.sector ?: stock.sector,
                                        industry = info.industry ?: stock.industry,
                                        lastPrice = info.lastPrice,
                                        change = info.change,
                                        percentChange = info.percentChange,
                                        pe = info.pe ?: stock.pe,
                                        pbv = info.pbv ?: stock.pbv,
                                        roe = info.roe,
                                        eps = info.eps,
                                        netProfit = info.netProfit,
                                        equity = info.equity,
                                        debtToEquity = info.debtToEquity,
                                        dividendYield = info.dividendYield ?: stock.dividendYield,
                                        dividendDate = info.dividendDate,
                                        rsi = indicators.rsi,
                                        macdHist = indicators.histogram,
                                        signalType = signal.type.name,
                                        signalReason = signal.reason,
                                        signalDescription = signal.description,
                                        lastUpdated = info.lastUpdated
                                    )
                                )
                            } catch (e: Exception) { }
                        }
                    }
                }
                jobs.awaitAll()
            }
            _isRefreshing.value = false
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

    fun addToWatchlist(symbol: String, cost: Double = 0.0, quantity: Int = 0) {
        viewModelScope.launch {
            // Deduct cash if it's a purchase (quantity > 0)
            if (quantity > 0) {
                val totalCostRaw = cost * quantity
                val fees = TechnicalAnalysis.calculateFees(totalCostRaw, false)
                adjustCash(-(totalCostRaw + fees))
            }
            repository.addStock(symbol, cost, quantity)
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
                val buyFees = TechnicalAnalysis.calculateFees(totalCostRaw, false)
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
                    repository.addStock(
                        symbol = symbol, 
                        cost = buyPrice, 
                        quantity = item.portfolio.quantity - sellQuantity
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

    fun clearWatchlist() {
        viewModelScope.launch {
            repository.clearWatchlist()
            repository.clearFocusList()
        }
    }

    fun exportBackup(contentResolver: android.content.ContentResolver, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val stocks = repository.allStocks.first()
                val focusList = repository.allFocusStocks.first()
                val trades = repository.allTrades.first()
                val cash = repository.cashBalance.first()?.balance ?: 0.0

                val backup = TradingBackup(stocks, focusList, trades, cash)

                val json = Json { 
                    prettyPrint = true
                    ignoreUnknownKeys = true
                }
                val content = json.encodeToString(backup)
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                }
            } catch (e: Exception) {
                _uiState.value = StockUiState.Error("Failed to export: ${e.message}")
            }
        }
    }

    fun importBackup(contentResolver: android.content.ContentResolver, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: ""
                }
                val json = Json { ignoreUnknownKeys = true }
                val backup = json.decodeFromString<TradingBackup>(content)

                // Clear existing data
                repository.clearWatchlist()
                repository.clearHistory()

                backup.stocks.forEach { repository.updateStockCache(it) }
                backup.focusList.forEach { repository.addToFocusList(it.symbol, it.startPrice, it.targetPrice) }
                backup.trades.forEach { repository.insertTrade(it) }
                repository.updateCash(backup.cashBalance)

                refreshWatchlistInfo()
            } catch (e: Exception) {
                _uiState.value = StockUiState.Error("Failed to import backup: ${e.message}")
            }
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
                        isFundamentalGood = updatedInfo.isFundamentalGood
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
                _uiState.value = StockUiState.Error(e.message ?: "Failed to scrape data")
            }
        }
    }
}
