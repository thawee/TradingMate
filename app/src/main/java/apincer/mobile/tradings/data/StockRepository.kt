package apincer.mobile.tradings.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class StockRepository(
    private val stockDao: StockDao,
    private val tradeDao: TradeDao,
    private val cashDao: CashDao
) {
    val allStocks: Flow<List<StockEntity>> = stockDao.getAllStocks()
    val allTrades: Flow<List<TradeEntity>> = tradeDao.getAllTrades()
    val cashBalance: Flow<CashEntity?> = cashDao.getCash()

    suspend fun updateCash(balance: Double) {
        cashDao.updateCash(CashEntity(balance = balance))
    }

    suspend fun addStock(
        symbol: String, 
        cost: Double = 0.0, 
        quantity: Int = 0, 
        name: String? = null, 
        description: String? = null
    ) {
        val normalizedSymbol = symbol.uppercase()
        val existing = stockDao.getStockBySymbol(normalizedSymbol)
        stockDao.insertStock(
            existing?.copy(
                cost = cost,
                quantity = quantity,
                name = name ?: existing.name,
                businessDescription = description ?: existing.businessDescription
            ) ?: StockEntity(
                symbol = normalizedSymbol,
                name = name,
                businessDescription = description,
                cost = cost,
                quantity = quantity
            )
        )
    }

    suspend fun updateStockCache(stock: StockEntity) {
        stockDao.insertStock(stock)
    }

    suspend fun addStockIfMissing(symbol: String) {
        val normalizedSymbol = symbol.uppercase()
        val existing = stockDao.getStockBySymbol(normalizedSymbol)
        if (existing == null) {
            stockDao.insertStock(StockEntity(symbol = normalizedSymbol))
        }
    }

    suspend fun removeStock(symbol: String) {
        stockDao.deleteStock(StockEntity(symbol.uppercase()))
    }

    suspend fun clearWatchlist() {
        stockDao.deleteWatchlistStocks()
    }

    suspend fun insertTrade(trade: TradeEntity) {
        tradeDao.insertTrade(trade)
    }

    suspend fun clearHistory() {
        tradeDao.clearHistory()
    }
}
