package apincer.mobile.tradings.data

import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction

class StockRepository(
    private val database: StockDatabase,
    private val stockDao: StockDao,
    private val tradeDao: TradeDao,
    private val cashDao: CashDao,
    private val focusDao: FocusDao,
    private val checklistDao: ChecklistDao
) {
    val allStocks: Flow<List<StockAggregate>> = stockDao.getAllStocks()
    val allTrades: Flow<List<TradeEntity>> = tradeDao.getAllTrades()
    val cashBalance: Flow<CashEntity?> = cashDao.getCash()
    val allFocusStocks: Flow<List<FocusEntity>> = focusDao.getAllFocusStocks()
    val checklist: Flow<ChecklistEntity?> = checklistDao.getChecklistFlow()

    suspend fun updateChecklist(checklist: ChecklistEntity) {
        checklistDao.insertChecklist(checklist)
    }

    suspend fun updateCash(balance: Double) {
        cashDao.updateCash(CashEntity(balance = balance))
    }

    suspend fun adjustCashBy(amount: Double) {
        cashDao.adjustCashBy(amount)
    }

    suspend fun addToFocusList(symbol: String, startPrice: Double, targetPrice: Double = 0.0) {
        focusDao.insertFocusStock(FocusEntity(symbol.uppercase(), startPrice, targetPrice))
    }

    suspend fun removeFromFocusList(symbol: String) {
        val existing = focusDao.getFocusStockBySymbol(symbol.uppercase())
        if (existing != null) {
            focusDao.deleteFocusStock(existing)
        }
    }

    suspend fun getFocusStock(symbol: String): FocusEntity? {
        return focusDao.getFocusStockBySymbol(symbol.uppercase())
    }

    suspend fun getStockBySymbol(symbol: String): StockAggregate? {
        return stockDao.getStockBySymbol(symbol.uppercase())
    }

    suspend fun addStock(
        symbol: String, 
        cost: Double = 0.0, 
        quantity: Int = 0, 
        tradePurpose: String = "SWING",
        buyFees: Double = 0.0,
        stopLoss: Double = 0.0,
        playbookNote: String = "",
        name: String? = null, 
        description: String? = null
    ) {
        val normalizedSymbol = symbol.uppercase()
        val existing = stockDao.getPortfolioBySymbol(normalizedSymbol)
        stockDao.insertPortfolio(
            existing?.copy(
                cost = cost,
                quantity = quantity,
                tradePurpose = tradePurpose,
                buyFees = buyFees,
                stopLoss = stopLoss,
                playbookNote = playbookNote
            ) ?: PortfolioEntity(
                symbol = normalizedSymbol,
                cost = cost,
                quantity = quantity,
                tradePurpose = tradePurpose,
                buyFees = buyFees,
                stopLoss = stopLoss,
                playbookNote = playbookNote
            )
        )

        if (name != null || description != null) {
            val cache = stockDao.getCacheBySymbol(normalizedSymbol) ?: StockCacheEntity(symbol = normalizedSymbol)
            stockDao.insertCache(cache.copy(
                name = name ?: cache.name,
                businessDescription = description ?: cache.businessDescription
            ))
        }
    }

    suspend fun updateStockCache(cache: StockCacheEntity) {
        stockDao.insertCache(cache)
    }

    suspend fun updateStockSignal(signal: StockSignalEntity) {
        stockDao.insertSignal(signal)
    }

    suspend fun addStockIfMissing(symbol: String) {
        val normalizedSymbol = symbol.uppercase()
        val existing = stockDao.getPortfolioBySymbol(normalizedSymbol)
        if (existing == null) {
            stockDao.insertPortfolio(PortfolioEntity(symbol = normalizedSymbol))
        }
    }

    suspend fun removeStock(symbol: String) {
        stockDao.deletePortfolio(PortfolioEntity(symbol.uppercase()))
    }

    suspend fun clearWatchlist() {
        stockDao.deleteWatchlistStocks()
    }

    suspend fun clearFocusList() {
        focusDao.clearFocusList()
    }

    suspend fun insertTrade(trade: TradeEntity) {
        tradeDao.insertTrade(trade)
    }

    suspend fun clearHistory() {
        tradeDao.clearHistory()
    }

    suspend fun getAllStocksSync(): List<StockAggregate> = stockDao.getAllStocksSync()
    suspend fun getAllTradesSync(): List<TradeEntity> = tradeDao.getAllTradesSync()
    suspend fun getAllFocusStocksSync(): List<FocusEntity> = focusDao.getAllFocusStocksSync()
    suspend fun getCashSync(): CashEntity? = cashDao.getCashSync()

    suspend fun restoreBackup(backup: TradingBackup) {
        database.withTransaction {
            stockDao.insertPortfolios(backup.portfolios)
            stockDao.insertCaches(backup.caches)
            stockDao.insertSignals(backup.signals)
            focusDao.insertFocusStocks(backup.focusList)
            tradeDao.insertTrades(backup.trades)
            cashDao.updateCash(CashEntity(balance = backup.cashBalance))
        }
    }
}
