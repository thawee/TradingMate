package apincer.mobile.tradings.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockRepositoryIntegrityTest {
    private fun openDatabase(): StockDatabase = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        StockDatabase::class.java).build()

    private fun repository(db: StockDatabase) = StockRepository(db, db.stockDao(), db.tradeDao(),
        db.cashDao(), db.focusDao(), db.checklistDao(), db.dividendDao(),
        db.portfolioSnapshotDao(), db.cashTransactionDao())

    @Test fun removingWatchlistSymbolCannotDeleteHeldShares() = runBlocking {
        val db = openDatabase()
        try {
            val repo = repository(db)
            db.stockDao().insertPortfolio(PortfolioEntity(symbol = "PTT", cost = 100.0, quantity = 100))
            assertEquals(true, runCatching { repo.removeStock("PTT") }.isFailure)
            assertNotNull(db.stockDao().getPortfolioBySymbol("PTT"))
            assertEquals(0, repo.getAllTradesSync().size)
        } finally { db.close() }
    }

    @Test fun duplicateSaleRequestsRecordOnlyOneFill() = runBlocking {
        val db = openDatabase()
        try {
            val repo = repository(db)
            db.stockDao().insertPortfolio(PortfolioEntity(symbol = "PTT", cost = 100.0, quantity = 100))
            db.cashDao().updateCash(CashEntity(balance = 0.0))
            val results = coroutineScope {
                listOf(async { runCatching { repo.executeSell("PTT", 110.0, 100) } },
                    async { runCatching { repo.executeSell("PTT", 110.0, 100) } }).awaitAll()
            }
            assertEquals(1, results.count { it.isSuccess })
            assertEquals(1, repo.getAllTradesSync().size)
            assertNull(db.stockDao().getPortfolioBySymbol("PTT"))
        } finally { db.close() }
    }

    @Test fun undoFullSaleRestoresAcceptedPlan() = runBlocking {
        val db = openDatabase()
        try {
            val repo = repository(db)
            db.stockDao().insertPortfolio(PortfolioEntity(symbol = "PTT", cost = 100.0,
                quantity = 100, buyFees = 15.0, stopLoss = 95.0, targetPrice = 110.0,
                plannedEntryPrice = 100.0, planId = "plan-1", planVersion = 2,
                planCreatedAtMillis = 123L, planSource = "USER", exitPolicy = "FIXED_TARGET"))
            db.cashDao().updateCash(CashEntity(balance = 0.0))
            repo.executeSell("PTT", 110.0, 100)
            val sale = repo.getAllTradesSync().single()
            assertNull(db.stockDao().getPortfolioBySymbol("PTT"))
            repo.undoSell(sale)
            val restored = db.stockDao().getPortfolioBySymbol("PTT")
            assertNotNull(restored)
            assertEquals("plan-1", restored!!.toTradePlan().id)
            assertEquals(110.0, restored.targetPrice, 0.0)
            assertEquals(15.0, restored.buyFees, 0.0)
            assertEquals(0.0, db.cashDao().getCashSync()!!.balance, 0.01)
        } finally { db.close() }
    }

    @Test fun undoingTheSameSaleTwiceDoesNotReverseCashOrSharesTwice() = runBlocking {
        val db = openDatabase()
        try {
            val repo = repository(db)
            db.stockDao().insertPortfolio(PortfolioEntity(symbol = "PTT", cost = 100.0, quantity = 100))
            db.cashDao().updateCash(CashEntity(balance = 0.0))
            repo.executeSell("PTT", 110.0, 100)
            val sale = repo.getAllTradesSync().single()
            repo.undoSell(sale)
            val balanceAfterUndo = repo.getCashSync()!!.balance
            assertEquals(true, runCatching { repo.undoSell(sale) }.isFailure)
            assertEquals(balanceAfterUndo, repo.getCashSync()!!.balance, 0.01)
            assertEquals(100, db.stockDao().getPortfolioBySymbol("PTT")!!.quantity)
            assertEquals(1, repo.getAllCashTransactionsSync().count { it.type.startsWith("UNDO SELL") })
        } finally { db.close() }
    }

    @Test fun backupImportKeepsLocalRecordsWithCollidingIds() = runBlocking {
        val db = openDatabase()
        try {
            val repo = repository(db)
            val local = TradeEntity(id = 1, symbol = "PTT", buyPrice = 100.0,
                sellPrice = 110.0, quantity = 100, netProfitPercent = 9.0,
                netProfitBaht = 900.0, dateMillis = 1L)
            val imported = local.copy(symbol = "ADVANC", dateMillis = 2L)
            db.tradeDao().insertTrade(local)
            db.cashTransactionDao().insertTransaction(CashTransactionEntity(
                id = 1, amount = 1000.0, type = "DEPOSIT", dateMillis = 1L))
            repo.restoreBackup(TradingBackup(emptyList(), emptyList(), 1000.0,
                tradeHistory = listOf(imported), cashTransactions = listOf(
                    CashTransactionEntity(id = 1, amount = -100.0, type = "WITHDRAWAL", dateMillis = 2L))))
            assertEquals(2, repo.getAllTradesSync().size)
            assertEquals(2, repo.getAllCashTransactionsSync().size)
            assertEquals(setOf("PTT", "ADVANC"), repo.getAllTradesSync().map { it.symbol }.toSet())
        } finally { db.close() }
    }

    @Test fun dividendRecordingInitializesCashAndRestoresOnce() = runBlocking {
        val db = openDatabase()
        try {
            val repo = repository(db)
            val dividend = DividendHistoryEntity(symbol = "PTT", dateMillis = 123L,
                amountPerShare = 2.0, sharesHeld = 100, totalReceived = 180.0, taxDeducted = 20.0)
            repo.recordDividend(dividend)
            assertEquals(180.0, repo.getCashSync()!!.balance, 0.01)
            val snapshot = PortfolioSnapshotEntity("2026-09-25", 10000.0, 9000.0, 180.0)
            val backup = TradingBackup(emptyList(), emptyList(), 180.0,
                dividendHistory = repo.getAllDividendsSync(), portfolioSnapshots = listOf(snapshot))
            repo.restoreBackup(backup)
            repo.restoreBackup(backup)
            assertEquals(1, repo.getAllDividendsSync().size)
            assertEquals(listOf(snapshot), repo.getAllSnapshotsSync())
            assertEquals(180.0, repo.getCashSync()!!.balance, 0.01)
        } finally { db.close() }
    }

    @Test fun invalidDividendDoesNotCreateLedgerEntries() = runBlocking {
        val db = openDatabase()
        try {
            val repo = repository(db)
            val invalid = DividendHistoryEntity(symbol = "PTT", dateMillis = 123L,
                amountPerShare = 2.0, sharesHeld = 100, totalReceived = -20.0, taxDeducted = 220.0)
            assertEquals(true, runCatching { repo.recordDividend(invalid) }.isFailure)
            assertEquals(0, repo.getAllDividendsSync().size)
            assertEquals(0, repo.getAllCashTransactionsSync().size)
            assertNull(repo.getCashSync())
        } finally { db.close() }
    }

    private val limits = apincer.mobile.tradings.domain.TradeRiskLimits(1.0, 15.0, 30.0, 15.0, 2.0)

    private suspend fun fundedWithPtt(db: StockDatabase) {
        db.cashDao().updateCash(CashEntity(balance = 100_000.0))
        db.stockDao().insertCache(StockCacheEntity(symbol = "PTT", sector = "Energy", lastPrice = 100.0))
    }

    @Test fun overLimitProposalIsBlockedAndLeavesLedgerUnchanged() = runBlocking {
        val db = openDatabase()
        try {
            val repo = repository(db)
            fundedWithPtt(db)
            val error = runCatching {
                repo.executeBuy("PTT", 100.0, 300, "SWING", 30.0, 95.0, "", 120.0, limits, true)
            }.exceptionOrNull()
            assertTrue(error?.message.orEmpty().contains("per-trade budget"))
            assertEquals(100_000.0, db.cashDao().getCashSync()!!.balance, 0.0)
            assertNull(db.stockDao().getPortfolioBySymbol("PTT"))
            assertEquals(0, db.adviceEventDao().getAllSync().size)
        } finally { db.close() }
    }

    @Test fun secondProposalIsRecheckedAgainstTheHoldingTheFirstCreated() = runBlocking {
        val db = openDatabase()
        try {
            val repo = repository(db)
            fundedWithPtt(db)
            repo.executeBuy("PTT", 100.0, 100, "SWING", 15.0, 95.0, "", 120.0, limits, true)
            val error = runCatching {
                repo.executeBuy("PTT", 100.0, 100, "SWING", 15.0, 95.0, "", 120.0, limits, true)
            }.exceptionOrNull()
            assertTrue(error?.message.orEmpty().contains("single-stock allocation"))
            assertEquals(100, db.stockDao().getPortfolioBySymbol("PTT")!!.quantity)
            assertEquals(100_000.0 - 10_015.0, db.cashDao().getCashSync()!!.balance, 1e-9)
        } finally { db.close() }
    }

    @Test fun executedFillIsRecordedTruthfullyWithItsBreaches() = runBlocking {
        val db = openDatabase()
        try {
            val repo = repository(db)
            fundedWithPtt(db)
            repo.executeBuy("PTT", 100.0, 300, "SWING", 30.0, 95.0, "", 120.0, limits, true,
                recordExecutedFill = true)
            assertEquals(300, db.stockDao().getPortfolioBySymbol("PTT")!!.quantity)
            assertEquals(100_000.0 - 30_030.0, db.cashDao().getCashSync()!!.balance, 1e-9)
            val event = db.adviceEventDao().getAllSync().single { it.kind == "BUY_FILL" }
            assertEquals("BROKER_RECORD", event.source)
            assertTrue(event.note.contains("per-trade budget"))
            assertTrue(event.note.contains("single-stock allocation"))
        } finally { db.close() }
    }
}
