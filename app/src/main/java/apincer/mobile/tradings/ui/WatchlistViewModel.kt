package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.data.SetScraper
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.data.StockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import apincer.mobile.tradings.data.ScrapedStockInfo

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {
    private val database = StockDatabase.getDatabase(application)
    private val repository = StockRepository(
        database,
        database.stockDao(),
        database.tradeDao(),
        database.cashDao(),
        database.focusDao(),
        database.checklistDao()
    )

    private val _searchResults = MutableStateFlow<List<ScrapedStockInfo>>(emptyList())
    val searchResults: StateFlow<List<ScrapedStockInfo>> = _searchResults

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

    fun resetSearchResults() {
        _searchResults.value = emptyList()
    }

    fun importFromCollection(category: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val symbols = withContext(Dispatchers.IO) {
                SetScraper.getCuratedCollection(category)
            }
            android.util.Log.d("WatchlistViewModel", "Importing ${symbols.size} symbols from $category collection")
            symbols.forEach { symbol ->
                repository.addStockIfMissing(symbol)
            }
            onComplete()
        }
    }

    fun addToFocusList(symbol: String, price: Double, targetPrice: Double = 0.0) {
        viewModelScope.launch {
            val normalizedSymbol = symbol.uppercase()
            val existing = repository.getFocusStock(normalizedSymbol)
            val startPrice = existing?.startPrice ?: price
            
            repository.addToFocusList(normalizedSymbol, startPrice, targetPrice)
            repository.addStockIfMissing(normalizedSymbol)
        }
    }

    fun removeFromFocusList(symbol: String) {
        viewModelScope.launch {
            val normalizedSymbol = symbol.uppercase()
            repository.removeFromFocusList(normalizedSymbol)
        }
    }


}
