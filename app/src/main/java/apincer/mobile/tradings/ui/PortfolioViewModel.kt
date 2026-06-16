package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.data.StockRepository
import apincer.mobile.tradings.data.TradeEntity
import apincer.mobile.tradings.domain.TechnicalAnalysis
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
                val buyPrice = item.portfolio.cost
                val totalCostRaw = buyPrice * sellQuantity
                val sellValueRaw = sellPrice * sellQuantity

                val sellFees = TechnicalAnalysis.calculateFees(sellValueRaw, true)
                val buyFees = if (item.portfolio.quantity > 0) {
                    (item.portfolio.buyFees * sellQuantity.toDouble()) / item.portfolio.quantity
                } else 0.0
                val totalFees = buyFees + sellFees

                val netProfitValue = (sellValueRaw - totalCostRaw) - totalFees
                val netProfitPercent = ((sellValueRaw - sellFees - (totalCostRaw + buyFees)) / (totalCostRaw + buyFees)) * 100

                val proceeds = sellValueRaw - sellFees
                adjustCash(proceeds)

                repository.insertTrade(
                    TradeEntity(
                        symbol = item.portfolio.symbol.uppercase(),
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
                    repository.removeStock(item.portfolio.symbol)
                } else {
                    val remainingQty = item.portfolio.quantity - sellQuantity
                    val remainingBuyFees = (item.portfolio.buyFees * remainingQty.toDouble()) / item.portfolio.quantity
                    repository.addStock(
                        symbol = item.portfolio.symbol, 
                        cost = buyPrice, 
                        quantity = remainingQty,
                        tradePurpose = item.portfolio.tradePurpose,
                        buyFees = remainingBuyFees
                    )
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
