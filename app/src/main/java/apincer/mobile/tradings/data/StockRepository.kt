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
    private val portfolioSnapshotDao: PortfolioSnapshotDao,
    private val cashTransactionDao: CashTransactionDao
) {
    val allStocks: Flow<List<StockAggregate>> = stockDao.getAllStocks()
    val allTrades: Flow<List<TradeEntity>> = tradeDao.getAllTrades()
    val cashBalance: Flow<CashEntity?> = cashDao.getCash()
    val allFocusStocks: Flow<List<FocusEntity>> = focusDao.getAllFocusStocks()
    val checklist: Flow<ChecklistEntity?> = checklistDao.getChecklistFlow()
    val allDividends: Flow<List<DividendHistoryEntity>> = dividendDao.getAllDividends()
    val allSnapshots: Flow<List<PortfolioSnapshotEntity>> = portfolioSnapshotDao.getAllSnapshots()
    val allCashTransactions: Flow<List<CashTransactionEntity>> = cashTransactionDao.getAllTransactions()

    suspend fun insertSnapshot(snapshot: PortfolioSnapshotEntity) {
        portfolioSnapshotDao.insertSnapshot(snapshot)
    }

    suspend fun deleteOldSnapshots(beforeDate: String) {
        portfolioSnapshotDao.deleteOldSnapshots(beforeDate)
    }

    suspend fun insertDividend(dividend: DividendHistoryEntity) {
        dividendDao.insertDividend(dividend)
    }

    suspend fun recordDividend(dividend: DividendHistoryEntity) {
        if (dividend.symbol.isBlank() || !dividend.amountPerShare.isFinite() ||
            dividend.amountPerShare <= 0.0 || dividend.sharesHeld <= 0 ||
            !dividend.taxDeducted.isFinite() || dividend.taxDeducted < 0.0 ||
            !dividend.totalReceived.isFinite() || dividend.totalReceived < 0.0) {
            throw IllegalStateException("Enter a valid dividend amount and tax")
        }
        database.withTransaction {
            if (cashDao.getCashSync() == null) cashDao.updateCash(CashEntity())
            dividendDao.insertDividend(dividend)
            cashTransactionDao.insertTransaction(CashTransactionEntity(
                amount = dividend.totalReceived, type = "Dividend", note = dividend.symbol))
            cashDao.adjustCashBy(dividend.totalReceived)
        }
    }

    suspend fun deleteDividend(dividend: DividendHistoryEntity) {
        dividendDao.deleteDividend(dividend)
    }

    suspend fun updateChecklist(checklist: ChecklistEntity) {
        checklistDao.insertChecklist(checklist)
    }

    suspend fun updateCash(balance: Double, reason: String = "Set Balance") {
        if (!balance.isFinite() || balance < 0.0) throw IllegalStateException("Enter a valid non-negative cash balance")
        database.withTransaction {
            val current = cashDao.getCashSync()?.balance ?: 0.0
            val diff = balance - current
            if (diff != 0.0) {
                cashTransactionDao.insertTransaction(CashTransactionEntity(amount = diff, type = reason))
            }
            cashDao.updateCash(CashEntity(balance = balance))
        }
    }

    suspend fun adjustCashBy(amount: Double, reason: String = "Adjustment") {
        database.withTransaction {
            if (!amount.isFinite()) throw IllegalStateException("Enter a valid cash amount")
            if (cashDao.getCashSync() == null) cashDao.updateCash(CashEntity())
            if (amount < 0) {
                val current = cashDao.getCashSync()
                if (current != null && current.balance + amount < 0) {
                    throw IllegalStateException("Insufficient balance: has ${current.balance}, needs ${-amount}")
                }
            }
            cashTransactionDao.insertTransaction(CashTransactionEntity(amount = amount, type = reason))
            cashDao.adjustCashBy(amount)
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
        description: String? = null,
        targetPrice: Double? = null,
        recordPlanEvent: Boolean = true,
        revisePlanForEntry: Boolean = false
    ) {
        val normalizedSymbol = symbol.uppercase()
        val existing = stockDao.getPortfolioBySymbol(normalizedSymbol)
        val acceptedTarget = targetPrice ?: existing?.targetPrice ?: 0.0
        if (!acceptedTarget.isFinite() || acceptedTarget < 0.0)
            throw IllegalStateException("Enter a valid target price")
        val clearedPlan = targetPrice == 0.0 && existing?.exitPolicy == "FIXED_TARGET"
        val changedPlan = acceptedTarget > 0.0 && (
            existing == null || existing.targetPrice != acceptedTarget || existing.stopLoss != stopLoss ||
                (revisePlanForEntry && existing.plannedEntryPrice != cost) ||
                existing.tradePurpose != tradePurpose || existing.exitPolicy != "FIXED_TARGET"
        )
        if (changedPlan && (cost <= 0.0 || stopLoss <= 0.0 || stopLoss >= cost || acceptedTarget <= cost)) {
            throw IllegalStateException("A fixed trade plan needs stop < entry < target")
        }
        val planId = if (changedPlan && existing?.planId.isNullOrBlank()) java.util.UUID.randomUUID().toString()
            else existing?.planId ?: if (changedPlan) java.util.UUID.randomUUID().toString() else ""
        stockDao.insertPortfolio(
            existing?.copy(
                cost = cost,
                quantity = quantity,
                tradePurpose = tradePurpose,
                buyFees = if (buyFees > 0.0) buyFees else existing.buyFees,
                stopLoss = stopLoss,
                playbookNote = playbookNote,
                targetPrice = acceptedTarget,
                plannedEntryPrice = if (clearedPlan) 0.0 else if (changedPlan) cost else existing.plannedEntryPrice,
                planId = if (clearedPlan) "" else planId,
                planVersion = if (clearedPlan) 0 else if (changedPlan) existing.planVersion + 1 else existing.planVersion,
                planCreatedAtMillis = if (clearedPlan) 0L else if (changedPlan) System.currentTimeMillis() else existing.planCreatedAtMillis,
                planSource = if (clearedPlan) "LEGACY" else if (changedPlan) "USER" else existing.planSource,
                exitPolicy = if (clearedPlan) "LEGACY" else if (changedPlan) "FIXED_TARGET" else existing.exitPolicy
            ) ?: PortfolioEntity(
                symbol = normalizedSymbol,
                cost = cost,
                quantity = quantity,
                tradePurpose = tradePurpose,
                buyFees = buyFees,
                stopLoss = stopLoss,
                playbookNote = playbookNote,
                targetPrice = acceptedTarget,
                plannedEntryPrice = if (acceptedTarget > 0.0) cost else 0.0,
                planId = planId,
                planVersion = if (acceptedTarget > 0.0) 1 else 0,
                planCreatedAtMillis = if (acceptedTarget > 0.0) System.currentTimeMillis() else 0L,
                planSource = if (acceptedTarget > 0.0) "USER" else "LEGACY",
                exitPolicy = if (acceptedTarget > 0.0) "FIXED_TARGET" else "LEGACY"
            )
        )
        if (changedPlan && recordPlanEvent) {
            database.adviceEventDao().insert(AdviceEventEntity(
                symbol = normalizedSymbol, planId = planId,
                planVersion = (existing?.planVersion ?: 0) + 1,
                kind = "PLAN_ACCEPTED", timeMillis = System.currentTimeMillis(),
                entryPrice = cost, stopPrice = stopLoss, targetPrice = acceptedTarget,
                quantity = quantity, source = "USER"
            ))
        }
        if (clearedPlan && recordPlanEvent) {
            database.adviceEventDao().insert(AdviceEventEntity(
                symbol = normalizedSymbol, planId = existing!!.planId,
                planVersion = existing.planVersion + 1,
                kind = "PLAN_CLEARED", timeMillis = System.currentTimeMillis(),
                entryPrice = cost, stopPrice = stopLoss.takeIf { it > 0.0 },
                quantity = quantity, source = "USER"
            ))
        }

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
        database.withTransaction {
            val normalizedSymbol = symbol.uppercase()
            if ((stockDao.getPortfolioBySymbol(normalizedSymbol)?.quantity ?: 0) > 0)
                throw IllegalStateException("Record the broker sale before removing $normalizedSymbol")
            stockDao.deletePortfolio(PortfolioEntity(normalizedSymbol))
        }
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

    suspend fun getAllStocksSync(): List<StockAggregate> = stockDao.getAllStocksSync()
    suspend fun getAllTradesSync(): List<TradeEntity> = tradeDao.getAllTradesSync()
    suspend fun getAllAdviceEventsSync(): List<AdviceEventEntity> = database.adviceEventDao().getAllSync()
    suspend fun getAllCashTransactionsSync(): List<CashTransactionEntity> = cashTransactionDao.getAllTransactionsSync()
    suspend fun getAllDividendsSync(): List<DividendHistoryEntity> = dividendDao.getAllDividendsSync()
    suspend fun getAllSnapshotsSync(): List<PortfolioSnapshotEntity> = portfolioSnapshotDao.getAllSnapshotsSync()
    suspend fun recordAdviceEvent(event: AdviceEventEntity) = database.adviceEventDao().insert(event)
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
                val symbol = item.symbol.uppercase()
                val existing = stockDao.getPortfolioBySymbol(symbol)
                val validFixedPlan = item.exitPolicy == "FIXED_TARGET" && item.planId.isNotBlank() &&
                    item.planVersion > 0 && item.plannedEntryPrice.isFinite() &&
                    item.stopLoss.isFinite() && item.targetPrice.isFinite() &&
                    item.stopLoss > 0.0 && item.stopLoss < item.plannedEntryPrice &&
                    item.targetPrice > item.plannedEntryPrice
                stockDao.insertPortfolio((existing ?: PortfolioEntity(symbol = symbol)).copy(
                    cost = item.cost, quantity = item.quantity, tradePurpose = item.tradePurpose,
                    buyFees = item.buyFees, stopLoss = item.stopLoss,
                    playbookNote = item.playbookNote ?: existing?.playbookNote ?: "",
                    peakPrice = item.peakPrice ?: existing?.peakPrice ?: 0.0,
                    targetPrice = if (validFixedPlan) item.targetPrice else 0.0,
                    plannedEntryPrice = if (validFixedPlan) item.plannedEntryPrice else 0.0,
                    planId = if (validFixedPlan) item.planId else "",
                    planVersion = if (validFixedPlan) item.planVersion else 0,
                    planCreatedAtMillis = if (validFixedPlan) item.planCreatedAtMillis else 0L,
                    planSource = if (validFixedPlan) item.planSource else "LEGACY",
                    exitPolicy = if (validFixedPlan) "FIXED_TARGET" else "LEGACY"
                ))
            }
            val knownTrades = tradeDao.getAllTradesSync().map { it.copy(id = 0) }.toMutableSet()
            backup.tradeHistory.forEach { imported ->
                val row = imported.copy(id = 0)
                if (knownTrades.add(row)) tradeDao.insertTrade(row)
            }
            val knownAdvice = database.adviceEventDao().getAllSync().map { it.copy(id = 0) }.toMutableSet()
            backup.adviceEvents.forEach { imported ->
                val row = imported.copy(id = 0)
                if (knownAdvice.add(row)) database.adviceEventDao().insert(row)
            }
            val knownCash = cashTransactionDao.getAllTransactionsSync().map { it.copy(id = 0) }.toMutableSet()
            backup.cashTransactions.forEach { imported ->
                val row = imported.copy(id = 0)
                if (knownCash.add(row)) cashTransactionDao.insertTransaction(row)
            }
            val knownDividends = dividendDao.getAllDividendsSync().map { it.copy(id = 0) }.toMutableSet()
            backup.dividendHistory.forEach { imported ->
                val row = imported.copy(id = 0)
                if (knownDividends.add(row)) dividendDao.insertDividend(row)
            }
            val knownDates = portfolioSnapshotDao.getAllSnapshotsSync().map { it.date }.toMutableSet()
            backup.portfolioSnapshots.forEach { imported ->
                if (knownDates.add(imported.date)) portfolioSnapshotDao.insertSnapshot(imported)
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
        playbookNote: String,
        targetPrice: Double = 0.0,
        riskLimits: apincer.mobile.tradings.domain.TradeRiskLimits? = null,
        atsEnabled: Boolean = true,
        recordExecutedFill: Boolean = false
    ) {
        database.withTransaction {
            if (!cost.isFinite() || cost <= 0.0 || quantity <= 0 || !buyFees.isFinite() || buyFees < 0.0)
                throw IllegalStateException("Enter a valid positive price, quantity and fees")
            val deduction = cost * quantity + buyFees
            val current = cashDao.getCashSync()
            val existing = stockDao.getPortfolioBySymbol(symbol.uppercase())
            val riskResult = if (apincer.mobile.tradings.domain.CoreSatellite.isCore(symbol)) {
                apincer.mobile.tradings.domain.TradeRiskPolicy.evaluateCoreBuy(
                    cost, quantity, buyFees, current?.balance ?: 0.0)
            } else if (riskLimits != null) {
                val holdings = stockDao.getAllStocksSync().filter { it.quantity > 0 }
                val holdingsValue = holdings.sumOf { it.quantity * (it.lastPrice.takeIf { p -> p > 0.0 } ?: it.cost) }
                val sector = stockDao.getCacheBySymbol(symbol.uppercase())?.sector
                val sectorValue = sector?.let { name ->
                    holdings.filter { it.sector == name }.sumOf {
                        it.quantity * (it.lastPrice.takeIf { p -> p > 0.0 } ?: it.cost)
                    }
                }
                val existingStockValue = holdings.filter { it.symbol == symbol.uppercase() }.sumOf {
                    it.quantity * (it.lastPrice.takeIf { p -> p > 0.0 } ?: it.cost)
                }
                val effectiveTarget = existing?.targetPrice?.takeIf { it > 0.0 } ?: targetPrice
                val effectiveStop = existing?.stopLoss?.takeIf { it > 0.0 } ?: stopLoss
                val effectivePurpose = existing?.tradePurpose ?: tradePurpose
                apincer.mobile.tradings.domain.TradeRiskPolicy.evaluate(
                    apincer.mobile.tradings.domain.TradeRiskInput(
                        cost, effectiveStop, quantity, buyFees, (current?.balance ?: 0.0) + holdingsValue,
                        current?.balance ?: 0.0, existingStockValue, sectorValue, atsEnabled,
                        effectiveTarget, effectivePurpose == "SWING"
                    ), riskLimits
                )
            } else null
            if (!recordExecutedFill && riskResult != null && !riskResult.allowed)
                throw IllegalStateException(riskResult.reasons.joinToString("; "))
            if (current == null && !recordExecutedFill)
                throw IllegalStateException("Set the cash balance before proposing a buy")
            if (!recordExecutedFill && (current?.balance ?: 0.0) - deduction < 0) {
                throw IllegalStateException("Insufficient balance: has ${current?.balance ?: 0.0}, needs $deduction")
            }
            if (current == null) cashDao.updateCash(CashEntity(balance = 0.0))
            cashTransactionDao.insertTransaction(
                CashTransactionEntity(
                    amount = -deduction,
                    type = "BUY ${symbol.uppercase()}",
                    note = "Bought $quantity shares @ ฿$cost"
                )
            )
            cashDao.adjustCashBy(-deduction)
            if (existing != null && existing.quantity > 0) {
                val combinedQuantity = existing.quantity + quantity
                val averageCost = (existing.cost * existing.quantity + cost * quantity) / combinedQuantity
                addStock(symbol, averageCost, combinedQuantity, existing.tradePurpose,
                    existing.buyFees + buyFees, existing.stopLoss, existing.playbookNote,
                    targetPrice = existing.targetPrice)
            } else {
                addStock(symbol, cost, quantity, tradePurpose, buyFees, stopLoss, playbookNote,
                    targetPrice = targetPrice)
            }
            val saved = stockDao.getPortfolioBySymbol(symbol.uppercase())
            database.adviceEventDao().insert(AdviceEventEntity(
                symbol = symbol.uppercase(), planId = saved?.planId ?: "",
                planVersion = saved?.planVersion ?: 0,
                kind = "BUY_FILL", timeMillis = System.currentTimeMillis(),
                entryPrice = saved?.plannedEntryPrice?.takeIf { it > 0.0 },
                stopPrice = saved?.stopLoss?.takeIf { it > 0.0 },
                targetPrice = saved?.targetPrice?.takeIf { it > 0.0 },
                fillPrice = cost, quantity = quantity, fees = buyFees,
                source = if (recordExecutedFill) "BROKER_RECORD" else "APP_PROPOSAL",
                note = if (recordExecutedFill) (riskResult?.reasons.orEmpty() +
                    listOfNotNull("Cash balance needs reconciliation".takeIf {
                        (current?.balance ?: 0.0) < deduction
                    })).joinToString("; ") else ""
            ))
        }
    }

    
    suspend fun undoSell(trade: TradeEntity, atsEnabled: Boolean = true) {
        database.withTransaction {
            if (trade.id <= 0 || tradeDao.getTradeById(trade.id) != trade ||
                tradeDao.deleteTradeById(trade.id) != 1) {
                throw IllegalStateException("This sale was already undone or its history has changed")
            }
            val sellValueRaw = trade.sellPrice * trade.quantity
            val sellFees = trade.sellFees.takeIf { it > 0.0 } ?:
                apincer.mobile.tradings.domain.TechnicalAnalysis.calculateFees(sellValueRaw, true, atsEnabled)
            val refundCash = sellValueRaw - sellFees
            cashTransactionDao.insertTransaction(
                CashTransactionEntity(
                    amount = -refundCash,
                    type = "UNDO SELL ${trade.symbol.uppercase()}",
                    note = "Reversed sale of ${trade.quantity} shares"
                )
            )
            cashDao.adjustCashBy(-refundCash)
            
            val existing = stockDao.getPortfolioBySymbol(trade.symbol)
            if (existing != null) {
                val newQty = existing.quantity + trade.quantity
                val additionalCost = trade.buyPrice * trade.quantity
                val oldTotalCost = existing.cost * existing.quantity
                val newCost = (oldTotalCost + additionalCost) / newQty
                stockDao.insertPortfolio(existing.copy(quantity = newQty, cost = newCost,
                    buyFees = existing.buyFees + trade.buyFees))
            } else {
                stockDao.insertPortfolio(trade.toRestoredPortfolio())
            }
            database.adviceEventDao().insert(AdviceEventEntity(
                symbol = trade.symbol.uppercase(), planId = existing?.planId ?: trade.planId,
                planVersion = existing?.planVersion ?: trade.planVersion,
                kind = "UNDO_SELL", timeMillis = System.currentTimeMillis(),
                fillPrice = trade.sellPrice, quantity = trade.quantity,
                fees = sellFees, source = "USER", note = "Reversed trade history ID ${trade.id}"
            ))
        }
    }

    suspend fun executeSell(
        symbol: String,
        sellPrice: Double,
        sellQuantity: Int,
        note: String = "",
        atsEnabled: Boolean = true
    ) {
        database.withTransaction {
            val portfolio = stockDao.getPortfolioBySymbol(symbol.uppercase())
                ?: throw IllegalStateException("Holding not found")
            if (!sellPrice.isFinite() || sellPrice <= 0.0 || sellQuantity <= 0 || sellQuantity > portfolio.quantity)
                throw IllegalStateException("Enter a valid sale price and quantity within the holding")
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

            val netCashReceived = sellValueRaw - sellFees
            if (cashDao.getCashSync() == null) cashDao.updateCash(CashEntity())
            cashTransactionDao.insertTransaction(
                CashTransactionEntity(
                    amount = netCashReceived,
                    type = "SELL ${symbol.uppercase()}",
                    note = "Sold $sellQuantity shares @ ฿$sellPrice"
                )
            )
            cashDao.adjustCashBy(netCashReceived)

            tradeDao.insertTrade(
                TradeEntity(
                    symbol = symbol.uppercase(),
                    buyPrice = portfolio.cost,
                    sellPrice = sellPrice,
                    quantity = sellQuantity,
                    netProfitPercent = netProfitPercent,
                    netProfitBaht = netProfitValue,
                    dateMillis = System.currentTimeMillis(),
                    note = note,
                    planId = portfolio.planId,
                    planVersion = portfolio.planVersion,
                    plannedEntryPrice = portfolio.plannedEntryPrice,
                    stopLoss = portfolio.stopLoss,
                    targetPrice = portfolio.targetPrice,
                    planCreatedAtMillis = portfolio.planCreatedAtMillis,
                    planSource = portfolio.planSource,
                    exitPolicy = portfolio.exitPolicy,
                    tradePurpose = portfolio.tradePurpose,
                    buyFees = buyFees,
                    sellFees = sellFees,
                    peakPrice = portfolio.peakPrice,
                    playbookNote = portfolio.playbookNote
                )
            )
            database.adviceEventDao().insert(AdviceEventEntity(
                symbol = symbol.uppercase(), planId = portfolio.planId,
                planVersion = portfolio.planVersion,
                kind = "SELL_FILL", timeMillis = System.currentTimeMillis(),
                entryPrice = portfolio.plannedEntryPrice.takeIf { it > 0.0 },
                stopPrice = portfolio.stopLoss.takeIf { it > 0.0 },
                targetPrice = portfolio.targetPrice.takeIf { it > 0.0 },
                fillPrice = sellPrice, quantity = sellQuantity,
                fees = buyFees + sellFees, source = "BROKER_RECORD", note = note
            ))

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
