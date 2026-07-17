package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.appRepository
import apincer.mobile.tradings.data.PreferenceRepository
import apincer.mobile.tradings.data.TradeEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        }
    }

    fun updateCashBalance(amount: Double, reason: String = "Set Balance") {
        viewModelScope.launch {
            repository.updateCash(amount, reason)
        }
    }

    fun adjustCash(amount: Double, reason: String = "Adjustment") {
        viewModelScope.launch {
            repository.adjustCashBy(amount, reason)
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
            } catch (e: Exception) {
                android.util.Log.e("PortfolioViewModel", "Error undoing sell: ${e.message}", e)
            }
        }
    }

    fun clearTradeHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
