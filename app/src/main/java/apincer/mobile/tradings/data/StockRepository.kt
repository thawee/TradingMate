package apincer.mobile.tradings.data

import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction

class StockRepository(
    private val database: StockDatabase,
    private val stockDao: StockDao,
    private val tradeDao: TradeDao,
    private val cashDao: CashDao,
    private val focusDao: FocusDao,
    private val checklistDao: ChecklistDao,
    private val dividendDao: DividendDao,
    private val portfolioSnapshotDao: PortfolioSnapshotDao
) {
    val allStocks: Flow<List<StockAggregate>> = stockDao.getAllStocks()
    val allTrades: Flow<List<TradeEntity>> = tradeDao.getAllTrades()
    val cashBalance: Flow<CashEntity?> = cashDao.getCash()
    val allFocusStocks: Flow<List<FocusEntity>> = focusDao.getAllFocusStocks()
    val checklist: Flow<ChecklistEntity?> = checklistDao.getChecklistFlow()
    val allDividends: Flow<List<DividendHistoryEntity>> = dividendDao.getAllDividends()
    val allSnapshots: Flow<List<PortfolioSnapshotEntity>> = portfolioSnapshotDao.getAllSnapshots()

    suspend fun insertSnapshot(snapshot: PortfolioSnapshotEntity) {
        portfolioSnapshotDao.insertSnapshot(snapshot)
    }

    suspend fun deleteOldSnapshots(beforeDate: String) {
        portfolioSnapshotDao.deleteOldSnapshots(beforeDate)
    }

    suspend fun insertDividend(dividend: DividendHistoryEntity) {
        dividendDao.insertDividend(dividend)
    }

    suspend fun deleteDividend(dividend: DividendHistoryEntity) {
        dividendDao.deleteDividend(dividend)
    }

    suspend fun updateChecklist(checklist: ChecklistEntity) {
        checklistDao.insertChecklist(checklist)
    }

    suspend fun updateCash(balance: Double) {
        cashDao.updateCash(CashEntity(balance = balance))
    }

    suspend fun adjustCashBy(amount: Double) {
        if (amount < 0) {
            val current = cashDao.getCashSync()
            if (current != null && current.balance + amount < 0) {
                throw IllegalStateException("Insufficient balance: has ${current.balance}, needs ${-amount}")
            }
        }
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

    suspend fun updatePortfolio(portfolio: PortfolioEntity) {
        stockDao.insertPortfolio(portfolio)
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
            // Add watchlist symbols (no position)
            backup.watchlistSymbols.forEach { symbol ->
                addStockIfMissing(symbol)
            }
            // Add/update portfolio items with position data
            backup.portfolioItems.forEach { item ->
                addStock(
                    symbol = item.symbol,
                    cost = item.cost,
                    quantity = item.quantity,
                    tradePurpose = item.tradePurpose
                )
            }
            // Restore cash balance
            cashDao.updateCash(CashEntity(balance = backup.cashBalance))
        }
    }

    suspend fun executeBuy(
        symbol: String,
        cost: Double,
        quantity: Int,
        tradePurpose: String,
        buyFees: Double,
        stopLoss: Double,
        playbookNote: String
    ) {
        database.withTransaction {
            // Use repository-level adjustCashBy so the negative-balance guard is enforced
            val deduction = cost * quantity + buyFees
            val current = cashDao.getCashSync()
            if (current != null && current.balance - deduction < 0) {
                throw IllegalStateException("Insufficient balance: has ${current.balance}, needs $deduction")
            }
            cashDao.adjustCashBy(-deduction)
            addStock(symbol, cost, quantity, tradePurpose, buyFees, stopLoss, playbookNote)
        }
    }

    suspend fun executeSell(
        symbol: String,
        sellPrice: Double,
        sellQuantity: Int,
        note: String = "",
        atsEnabled: Boolean = true
    ) {
        val aggregate = stockDao.getStockBySymbol(symbol.uppercase()) ?: return
        val portfolio = aggregate.portfolio

        database.withTransaction {
            val totalCostRaw = portfolio.cost * sellQuantity
            val sellValueRaw = sellPrice * sellQuantity
            val sellFees = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateFees(sellValueRaw, true, atsEnabled)
            val buyFees = if (portfolio.quantity > 0) {
                (portfolio.buyFees * sellQuantity.toDouble()) / portfolio.quantity
            } else 0.0
            val totalFees = buyFees + sellFees
            val netProfitValue = (sellValueRaw - sellFees) - (totalCostRaw + buyFees)
            val netProfitPercent = if (totalCostRaw + buyFees != 0.0) {
                (netProfitValue / (totalCostRaw + buyFees)) * 100
            } else 0.0

            cashDao.adjustCashBy(sellValueRaw - sellFees)

            tradeDao.insertTrade(
                TradeEntity(
                    symbol = symbol.uppercase(),
                    buyPrice = portfolio.cost,
                    sellPrice = sellPrice,
                    quantity = sellQuantity,
                    netProfitPercent = netProfitPercent,
                    netProfitBaht = netProfitValue,
                    dateMillis = System.currentTimeMillis(),
                    note = note
                )
            )

            if (portfolio.quantity == sellQuantity) {
                stockDao.deletePortfolio(PortfolioEntity(symbol.uppercase()))
            } else {
                val remainingQty = portfolio.quantity - sellQuantity
                val remainingBuyFees = (portfolio.buyFees * remainingQty.toDouble()) / portfolio.quantity
                stockDao.insertPortfolio(
                    portfolio.copy(quantity = remainingQty, buyFees = remainingBuyFees)
                )
            }
        }
    }
}
