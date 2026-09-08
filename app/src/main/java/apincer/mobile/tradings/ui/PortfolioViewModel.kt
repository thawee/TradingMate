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
import java.util.concurrent.ConcurrentHashMap

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = application.appRepository
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

    private val historicalClosesCache = ConcurrentHashMap<String, List<Double>>()
    private val _portfolioHistoricalCloses = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val portfolioHistoricalCloses: StateFlow<Map<String, List<Double>>> = _portfolioHistoricalCloses.asStateFlow()

    fun loadHistoricalClosesForHoldings(symbols: List<String>) {
        if (symbols.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val resultMap = HashMap<String, List<Double>>(historicalClosesCache)
            var hasNew = false
            for (sym in symbols) {
                val upper = sym.uppercase()
                if (!resultMap.containsKey(upper)) {
                    val prices = apincer.mobile.tradings.data.SetScraper.fetchHistoricalPrices(upper).map { it.close }
                    if (prices.isNotEmpty()) {
                        resultMap[upper] = prices
                        historicalClosesCache[upper] = prices
                        hasNew = true
                    }
                }
            }
            if (hasNew || _portfolioHistoricalCloses.value.isEmpty()) {
                _portfolioHistoricalCloses.value = resultMap
            }
        }
    }

    fun takeSnapshot(holdings: List<StockWatchlistInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val openHoldings = holdings.filter { it.portfolio.quantity > 0 }
            val totalValue = openHoldings.sumOf { it.info.lastPrice * it.portfolio.quantity }
            val totalCost = openHoldings.sumOf { it.portfolio.cost * it.portfolio.quantity + it.portfolio.buyFees }
            val currentCash = cashBalance.value
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

    fun logDividend(symbol: String, dateMillis: Long, amountPerShare: Double, sharesHeld: Int, taxDeducted: Double) {
        viewModelScope.launch {
            val totalReceived = (amountPerShare * sharesHeld) - taxDeducted
            repository.insertDividend(apincer.mobile.tradings.data.DividendHistoryEntity(
                symbol = symbol.uppercase(),
                dateMillis = dateMillis,
                amountPerShare = amountPerShare,
                sharesHeld = sharesHeld,
                totalReceived = totalReceived,
                taxDeducted = taxDeducted
            ))
            // Also adjust cash balance up by totalReceived
            repository.adjustCashBy(totalReceived, "Dividend")
            apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
        }
    }

    fun updateCashBalance(amount: Double, reason: String = "Set Balance") {
        viewModelScope.launch {
            repository.updateCash(amount, reason)
            apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
        }
    }

    fun adjustCash(amount: Double, reason: String = "Adjustment") {
        viewModelScope.launch {
            repository.adjustCashBy(amount, reason)
            apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
        }
    }

    fun recordSell(item: StockWatchlistInfo, sellPrice: Double, sellQuantity: Int, note: String = "") {
        viewModelScope.launch {
            if (item.portfolio.quantity >= sellQuantity) {
                try {
                    repository.executeSell(
                        symbol = item.portfolio.symbol,
                        sellPrice = sellPrice,
                        sellQuantity = sellQuantity,
                        note = note,
                        atsEnabled = isAtsEnabled.value
                    )
                    apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
                } catch (e: Exception) {
                    android.util.Log.e("PortfolioViewModel", "Error recording sell: ${e.message}", e)
                }
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
