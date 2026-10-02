package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.appRepository
import apincer.mobile.tradings.data.PreferenceRepository
import apincer.mobile.tradings.data.TradeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository get() = getApplication<apincer.mobile.tradings.TradingMateApp>().repository
    private val preferenceRepository = PreferenceRepository(application)
    private val isAtsEnabled: StateFlow<Boolean> = preferenceRepository.isAtsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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
    val allCashTransactions: StateFlow<List<apincer.mobile.tradings.data.CashTransactionEntity>> =
        repository.allCashTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val allSnapshots: StateFlow<List<apincer.mobile.tradings.data.PortfolioSnapshotEntity>> =
        repository.allSnapshots.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    private val historicalClosesCache = ConcurrentHashMap<String, List<apincer.mobile.tradings.data.ScrapedHistoricalPrice>>()
    private val historyFetchMutex = Mutex()
    private var lastHistoryFetchedAt = 0L
    private val historyRefreshIntervalMillis = 60L * 60L * 1000L
    private val _portfolioHistoricalCloses = MutableStateFlow<Map<String, List<apincer.mobile.tradings.data.ScrapedHistoricalPrice>>>(emptyMap())
    val portfolioHistoricalCloses: StateFlow<Map<String, List<apincer.mobile.tradings.data.ScrapedHistoricalPrice>>> = _portfolioHistoricalCloses.asStateFlow()
    private val _corePrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    /** Latest prices of the core funds for the core card and one-tap DCA; empty until fetched or if offline. */
    val corePrices: StateFlow<Map<String, Double>> = _corePrices.asStateFlow()

    fun refreshCorePrice() {
        viewModelScope.launch(Dispatchers.IO) {
            val quotes = apincer.mobile.tradings.data.SetScraper.fetchBatchQuotes(
                apincer.mobile.tradings.domain.CoreSatellite.funds.map { it.first }
            ).filter { it.lastPrice > 0.0 }.associate { it.symbol.uppercase() to it.lastPrice }
            if (quotes.isNotEmpty()) _corePrices.value = _corePrices.value + quotes
        }
    }

    private val _indexHistory = MutableStateFlow<List<apincer.mobile.tradings.data.ScrapedHistoricalPrice>>(emptyList())
    val indexHistory: StateFlow<List<apincer.mobile.tradings.data.ScrapedHistoricalPrice>> = _indexHistory.asStateFlow()

    fun loadHistoricalClosesForHoldings(symbols: List<String>) {
        if (symbols.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            historyFetchMutex.withLock {
                val now = System.currentTimeMillis()
                val expired = now - lastHistoryFetchedAt >= historyRefreshIntervalMillis
                val resultMap = HashMap<String, List<apincer.mobile.tradings.data.ScrapedHistoricalPrice>>(historicalClosesCache)
                for (sym in symbols) {
                    val upper = sym.uppercase()
                    if (expired || !resultMap.containsKey(upper)) {
                        val prices = apincer.mobile.tradings.data.SetScraper.fetchHistoricalPrices(upper)
                        if (prices.isNotEmpty()) {
                            resultMap[upper] = prices
                            historicalClosesCache[upper] = prices
                        }
                    }
                }
                _portfolioHistoricalCloses.value = resultMap
                if (expired || _indexHistory.value.isEmpty()) {
                    _indexHistory.value = apincer.mobile.tradings.data.SetScraper.fetchSetIndexHistory()
                }
                if (_indexHistory.value.isNotEmpty() && symbols.all { resultMap.containsKey(it.uppercase()) }) {
                    lastHistoryFetchedAt = now
                }
            }
        }
    }

    private val _satelliteScorecard = MutableStateFlow<apincer.mobile.tradings.domain.SatelliteScorecard.Report?>(null)
    val satelliteScorecard: StateFlow<apincer.mobile.tradings.domain.SatelliteScorecard.Report?> = _satelliteScorecard.asStateFlow()

    /**
     * Satellite vs shadow-TDEX scorecard from the fill journal. Satellite symbols use raw
     * closes (their dividends are counted from the dividend log, net of withholding tax);
     * TDEX uses dividend-adjusted closes (gross), a small bias in the core's favour.
     */
    /**
     * "Start tracking from today": journals the untracked shares of each excluded holding as a
     * BASELINE_FILL at today's price, so the scorecard can compare the satellite from now on.
     * Returns the number of holdings brought in.
     */
    fun startScorecardTracking(holdings: List<StockWatchlistInfo>, onDone: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val scorecard = apincer.mobile.tradings.domain.SatelliteScorecard
            val fills = scorecard.fillsFromEvents(repository.getAllAdviceEventsSync(), isAtsEnabled.value)
            val bySymbol = holdings.associateBy { it.info.symbol.uppercase() }
            val now = System.currentTimeMillis()
            val topUps = scorecard.baselineTopUps(fills, holdings.associate { it.info.symbol.uppercase() to it.portfolio.quantity })
                .filter { (bySymbol[it.key]?.info?.lastPrice ?: 0.0) > 0.0 }
            topUps.forEach { (symbol, qty) ->
                val price = bySymbol.getValue(symbol).info.lastPrice
                repository.recordAdviceEvent(apincer.mobile.tradings.data.AdviceEventEntity(
                    symbol = symbol, planId = "", planVersion = 0, kind = "BASELINE_FILL", timeMillis = now,
                    fillPrice = price, quantity = qty, source = "BASELINE",
                    note = "Scorecard tracking started at today's price"))
            }
            loadSatelliteScorecard(holdings)
            kotlinx.coroutines.withContext(Dispatchers.Main) { onDone(topUps.size) }
        }
    }

    fun loadSatelliteScorecard(holdings: List<StockWatchlistInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val scorecard = apincer.mobile.tradings.domain.SatelliteScorecard
            val fills = scorecard.fillsFromEvents(repository.getAllAdviceEventsSync(), isAtsEnabled.value)
            val now = System.currentTimeMillis()
            val currentQuantities = holdings.associate { it.info.symbol.uppercase() to it.portfolio.quantity }
            if (fills.isEmpty()) {
                _satelliteScorecard.value = scorecard.report(emptyList(), emptyList(), currentQuantities, now, { _, _ -> null }, { null })
                return@launch
            }
            val dayMillis = 86_400_000L
            val windowDays = 470 // trailing 12 months ending a quarter ago, plus margin
            val coreDays = maxOf(windowDays, ((now - fills.first().timeMillis) / dayMillis).toInt() + 10)
            val coreHistory = apincer.mobile.tradings.data.SetScraper.fetchHistoricalPrices(
                apincer.mobile.tradings.domain.CoreSatellite.CORE_SYMBOL, days = coreDays, dividendAdjusted = true)
            val included = scorecard.coverage(fills, currentQuantities).included
            val histories = included.associateWith {
                apincer.mobile.tradings.data.SetScraper.fetchHistoricalPrices(it, days = windowDays)
            }
            val currentPrices = holdings.associate { it.info.symbol.uppercase() to it.info.lastPrice }
            val bangkok = java.time.ZoneId.of("Asia/Bangkok")
            fun closeOnOrBefore(history: List<apincer.mobile.tradings.data.ScrapedHistoricalPrice>, t: Long): Double? {
                val date = java.time.Instant.ofEpochMilli(t).atZone(bangkok).toLocalDate().toString()
                return history.lastOrNull { it.date <= date }?.close
            }
            val dividends = repository.getAllDividendsSync().map {
                apincer.mobile.tradings.domain.SatelliteScorecard.Dividend(it.symbol.uppercase(), it.dateMillis, it.totalReceived)
            }
            _satelliteScorecard.value = scorecard.report(
                fills, dividends, currentQuantities, now,
                priceAt = { symbol, t ->
                    if (now - t < dayMillis) currentPrices[symbol]?.takeIf { it > 0.0 } ?: closeOnOrBefore(histories[symbol].orEmpty(), t)
                    else closeOnOrBefore(histories[symbol].orEmpty(), t)
                },
                corePriceAt = { t -> closeOnOrBefore(coreHistory, t) }
            )
        }
    }

    fun takeSnapshot(holdings: List<StockWatchlistInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val openHoldings = holdings.filter { it.portfolio.quantity > 0 }
            val totalValue = openHoldings.sumOf { item ->
                (item.info.lastPrice.takeIf { it > 0.0 } ?: item.portfolio.cost) * item.portfolio.quantity
            }
            val totalCost = openHoldings.sumOf { it.portfolio.cost * it.portfolio.quantity + it.portfolio.buyFees }
            val currentCash = repository.getCashSync()?.balance ?: 0.0
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val snapshot = apincer.mobile.tradings.data.PortfolioSnapshotEntity(
                date = todayStr,
                totalValue = totalValue,
                totalCost = totalCost,
                cashBalance = currentCash
            )
            repository.insertSnapshot(snapshot)
        }
    }

    val dividendHistory: StateFlow<List<apincer.mobile.tradings.data.DividendHistoryEntity>> = 
        repository.allDividends.stateIn(
            scope = viewModelScope, 
            started = SharingStarted.Lazily, 
            initialValue = emptyList()
        )

    private val dividendPrefs = application.getSharedPreferences("trading_mate_alerts", android.content.Context.MODE_PRIVATE)
    private val _pendingDividends = MutableStateFlow<List<apincer.mobile.tradings.domain.PendingDividends.Pending>>(emptyList())
    /** Recent payouts on current holdings that are not in the dividend log yet. */
    val pendingDividends: StateFlow<List<apincer.mobile.tradings.domain.PendingDividends.Pending>> = _pendingDividends.asStateFlow()

    fun refreshPendingDividends(holdings: List<StockWatchlistInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val held = holdings.filter { it.portfolio.quantity > 0 }
                .associate { it.info.symbol.uppercase() to it.portfolio.quantity }
            if (held.isEmpty()) { _pendingDividends.value = emptyList(); return@launch }
            val bangkok = java.time.ZoneId.of("Asia/Bangkok")
            fun day(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(bangkok).toLocalDate()
            val events = held.keys.associateWith { apincer.mobile.tradings.data.SetScraper.fetchDividendEvents(it, "6mo") }
            val recorded = repository.getAllDividendsSync().groupBy({ it.symbol.uppercase() }, { day(it.dateMillis) })
            // First journaled buy only; a scorecard baseline is not a purchase date.
            val acquired = repository.getAllAdviceEventsSync().filter { it.kind == "BUY_FILL" }
                .groupBy { it.symbol.uppercase() }.mapValues { (_, e) -> day(e.minOf { it.timeMillis }) }
            val dismissed = dividendPrefs.getStringSet(DIVIDEND_DISMISSED_KEY, emptySet()).orEmpty()
            _pendingDividends.value = apincer.mobile.tradings.domain.PendingDividends.find(
                held, events, recorded, acquired, dismissed, java.time.LocalDate.now(bangkok))
        }
    }

    fun dismissPendingDividend(pending: apincer.mobile.tradings.domain.PendingDividends.Pending) {
        val set = dividendPrefs.getStringSet(DIVIDEND_DISMISSED_KEY, emptySet()).orEmpty() + pending.key
        dividendPrefs.edit().putStringSet(DIVIDEND_DISMISSED_KEY, set).apply()
        _pendingDividends.value = _pendingDividends.value.filterNot { it.key == pending.key }
    }

    fun logDividend(symbol: String, dateMillis: Long, amountPerShare: Double, sharesHeld: Int, taxDeducted: Double,
                    onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val totalReceived = (amountPerShare * sharesHeld) - taxDeducted
            val dividend = apincer.mobile.tradings.data.DividendHistoryEntity(
                symbol = symbol.uppercase(),
                dateMillis = dateMillis,
                amountPerShare = amountPerShare,
                sharesHeld = sharesHeld,
                totalReceived = totalReceived,
                taxDeducted = taxDeducted
            )
            try {
                repository.recordDividend(dividend)
                apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun updateCashBalance(amount: Double, reason: String = "Set Balance", onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateCash(amount, reason)
                apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
                onResult(Result.success(Unit))
            } catch (e: Exception) { onResult(Result.failure(e)) }
        }
    }

    fun adjustCash(amount: Double, reason: String = "Adjustment", onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.adjustCashBy(amount, reason)
                apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
                onResult(Result.success(Unit))
            } catch (e: Exception) { onResult(Result.failure(e)) }
        }
    }

    fun recordSell(item: StockWatchlistInfo, sellPrice: Double, sellQuantity: Int, note: String = "",
                   onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                    repository.executeSell(
                        symbol = item.portfolio.symbol,
                        sellPrice = sellPrice,
                        sellQuantity = sellQuantity,
                        note = note,
                        atsEnabled = isAtsEnabled.value
                    )
                    apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
                    onResult(Result.success(Unit))
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Error recording sell: ${e.message}", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun undoSell(trade: TradeEntity) {
        viewModelScope.launch {
            try {
                repository.undoSell(trade, atsEnabled = isAtsEnabled.value)
                apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Error undoing sell: ${e.message}", e)
            }
        }
    }

    fun clearTradeHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
        }
    }
}

private const val DIVIDEND_DISMISSED_KEY = "pending_dividends_dismissed"
