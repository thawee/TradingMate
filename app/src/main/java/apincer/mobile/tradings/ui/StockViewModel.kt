package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

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
        val sellingPrice: Double? = null
    ) : StockUiState()
    data class Error(val message: String) : StockUiState()
}

data class StockWatchlistInfo(
    val info: ScrapedStockInfo,
    val portfolio: StockEntity,
    val netProfitPercent: Double = 0.0,
    val signal: TradeSignal? = null
)

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val database = StockDatabase.getDatabase(application)
    private val repository = StockRepository(database.stockDao(), database.tradeDao(), database.cashDao())
    
    private val _uiState = MutableStateFlow<StockUiState>(StockUiState.Initial)
    val uiState: StateFlow<StockUiState> = _uiState

    val watchlistInfo: StateFlow<List<StockWatchlistInfo>> = 
        repository.allStocks.map { stocks ->
            stocks.map { stock ->
                val info = ScrapedStockInfo(
                    symbol = stock.symbol,
                    name = stock.name,
                    nameTH = stock.nameTH,
                    businessDescription = stock.businessDescription,
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
                
                StockWatchlistInfo(info, stock, netProfit, signal)
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
            val semaphore = Semaphore(3) // Limit parallel requests
            
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

                                // Update individual stock in DB to trigger UI updates incrementally
                                repository.updateStockCache(
                                    stock.copy(
                                        name = info.name ?: stock.name,
                                        businessDescription = info.businessDescription ?: stock.businessDescription,
                                        lastPrice = info.lastPrice,
                                        change = info.change,
                                        percentChange = info.percentChange,
                                        pe = info.pe,
                                        pbv = info.pbv,
                                        roe = info.roe,
                                        eps = info.eps,
                                        netProfit = info.netProfit,
                                        equity = info.equity,
                                        debtToEquity = info.debtToEquity,
                                        dividendYield = info.dividendYield,
                                        dividendDate = info.dividendDate,
                                        rsi = indicators.rsi,
                                        macdHist = indicators.histogram,
                                        signalType = signal.type.name,
                                        signalReason = signal.reason,
                                        signalDescription = signal.description,
                                        lastUpdated = info.lastUpdated
                                    )
                                )
                            } catch (e: Exception) {
                                // Skip individual failures
                            }
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
                // If it was in portfolio, record the trade history as a \"Sale\" at current market price
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
                        businessDescription = info.businessDescription ?: local?.businessDescription
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

                    // Optional: Update cache for this single stock too
                    if (local != null) {
                        repository.updateStockCache(local.copy(
                            name = updatedInfo.name,
                            businessDescription = updatedInfo.businessDescription,
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
                        sellPriceTarget
                    )
                }
                _uiState.value = result
            } catch (e: Exception) {
                _uiState.value = StockUiState.Error(e.message ?: "Failed to scrape data")
            }
        }
    }
}
