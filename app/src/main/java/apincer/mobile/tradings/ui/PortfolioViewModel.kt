package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.data.StockRepository
import apincer.mobile.tradings.data.TradeEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val database = StockDatabase.getDatabase(application)
    private val repository = StockRepository(
        database,
        database.stockDao(),
        database.tradeDao(),
        database.cashDao(),
        database.focusDao(),
        database.checklistDao()
    )

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

    fun updateCashBalance(amount: Double) {
        viewModelScope.launch {
            repository.updateCash(amount)
        }
    }

    fun adjustCash(amount: Double) {
        viewModelScope.launch {
            repository.adjustCashBy(amount)
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
                        note = note
                    )
                } catch (e: Exception) {
                    android.util.Log.e("PortfolioViewModel", "Error recording sell: ${e.message}", e)
                }
            }
        }
    }

    fun clearTradeHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
