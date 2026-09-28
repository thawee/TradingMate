package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.appRepository
import apincer.mobile.tradings.data.ScrapedStockInfo
import apincer.mobile.tradings.data.SetScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {
    private val repository get() = getApplication<apincer.mobile.tradings.TradingMateApp>().repository

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




}
