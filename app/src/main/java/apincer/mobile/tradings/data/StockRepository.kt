package apincer.mobile.tradings.data

import kotlinx.coroutines.flow.Flow

class StockRepository(
    private val stockDao: StockDao,
    private val tradeDao: TradeDao,
    private val cashDao: CashDao,
    private val focusDao: FocusDao,
    private val checklistDao: ChecklistDao
) {
    val allStocks: Flow<List<StockEntity>> = stockDao.getAllStocks()
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

    suspend fun getStockBySymbol(symbol: String): StockEntity? {
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
        val existing = stockDao.getStockBySymbol(normalizedSymbol)
        stockDao.insertStock(
            existing?.copy(
                cost = cost,
                quantity = quantity,
                tradePurpose = tradePurpose,
                buyFees = buyFees,
                stopLoss = stopLoss,
                playbookNote = playbookNote,
                name = name ?: existing.name,
                businessDescription = description ?: existing.businessDescription
            ) ?: StockEntity(
                symbol = normalizedSymbol,
                name = name,
                businessDescription = description,
                cost = cost,
                quantity = quantity,
                tradePurpose = tradePurpose,
                buyFees = buyFees,
                stopLoss = stopLoss,
                playbookNote = playbookNote
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

    suspend fun clearFocusList() {
        focusDao.clearFocusList()
    }

    suspend fun insertTrade(trade: TradeEntity) {
        tradeDao.insertTrade(trade)
    }

    suspend fun clearHistory() {
        tradeDao.clearHistory()
    }

    suspend fun getAllStocksSync(): List<StockEntity> = stockDao.getAllStocksSync()
    suspend fun getAllTradesSync(): List<TradeEntity> = tradeDao.getAllTradesSync()
    suspend fun getAllFocusStocksSync(): List<FocusEntity> = focusDao.getAllFocusStocksSync()
    suspend fun getCashSync(): CashEntity? = cashDao.getCashSync()

    suspend fun restoreBackup(backup: TradingBackup) {
        stockDao.insertStocks(backup.stocks)
        focusDao.insertFocusStocks(backup.focusList)
        tradeDao.insertTrades(backup.trades)
        cashDao.updateCash(CashEntity(balance = backup.cashBalance))
    }
}
