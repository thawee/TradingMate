file_path = '/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/data/StockRepository.kt'
with open(file_path, 'r') as f:
    content = f.read()

undo_sell = """
    suspend fun undoSell(trade: TradeEntity, atsEnabled: Boolean = true) {
        database.withTransaction {
            val sellValueRaw = trade.sellPrice * trade.quantity
            val sellFees = apincer.mobile.tradings.domain.TechnicalAnalysis.calculateFees(sellValueRaw, true, atsEnabled)
            val refundCash = sellValueRaw - sellFees
            adjustCashBy(-refundCash, "Undo Sell")
            
            val existing = stockDao.getPortfolioBySymbol(trade.symbol)
            if (existing != null) {
                val newQty = existing.quantity + trade.quantity
                val additionalCost = trade.buyPrice * trade.quantity
                val oldTotalCost = existing.cost * existing.quantity
                val newCost = (oldTotalCost + additionalCost) / newQty
                stockDao.insertPortfolio(existing.copy(quantity = newQty, cost = newCost))
            } else {
                stockDao.insertPortfolio(PortfolioEntity(
                    symbol = trade.symbol,
                    cost = trade.buyPrice,
                    quantity = trade.quantity
                ))
            }
            tradeDao.deleteTrade(trade)
        }
    }
"""

if "suspend fun undoSell" not in content:
    content = content.replace(
        'suspend fun executeSell(',
        undo_sell + '\n    suspend fun executeSell('
    )
    with open(file_path, 'w') as f:
        f.write(content)
