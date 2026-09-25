package apincer.mobile.tradings.data

import apincer.mobile.tradings.domain.ExitPolicy
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradePlanPersistenceTest {
    @Test fun oldBackupDefaultsToLegacyPlan() {
        val oldJson = """{"watchlistSymbols":["PTT"],"portfolioItems":[{"symbol":"PTT","cost":100.0,"quantity":100,"tradePurpose":"SWING"}],"cashBalance":1000.0}"""
        val decoded = Json.decodeFromString(TradingBackup.serializer(), oldJson)
        val restored = decoded.portfolioItems.single()
        assertEquals(0.0, restored.targetPrice, 0.0)
        assertEquals("LEGACY", restored.exitPolicy)
        assertTrue(decoded.cashTransactions.isEmpty())
        assertTrue(decoded.dividendHistory.isEmpty())
        assertTrue(decoded.portfolioSnapshots.isEmpty())
    }

    @Test fun acceptedPlanFieldsRoundTripThroughBackup() {
        val portfolio = SimplePortfolio("PTT", 100.0, 100, "SWING", 15.0, 95.0,
            110.0, 100.0, "plan-1", 1, 123L, "USER", "FIXED_TARGET",
            "Breakout", 111.0)
        val backup = TradingBackup(listOf("PTT"), listOf(portfolio), 1000.0)
        val restored = Json.decodeFromString(TradingBackup.serializer(),
            Json.encodeToString(TradingBackup.serializer(), backup))
        assertEquals(portfolio, restored.portfolioItems.single())
    }

    @Test fun migratedHoldingHasNoInventedTarget() {
        val legacy = PortfolioEntity("PTT", cost = 100.0, quantity = 100, stopLoss = 95.0)
        assertEquals(ExitPolicy.LEGACY, legacy.toTradePlan().exitPolicy)
        assertNull(legacy.toTradePlan().targetPrice)
    }

    @Test fun fullSaleUndoRestoresAcceptedPlanAndFees() {
        val sale = TradeEntity(symbol = "PTT", buyPrice = 100.0, sellPrice = 110.0,
            quantity = 100, netProfitPercent = 8.0, netProfitBaht = 800.0,
            dateMillis = 123L, planId = "plan-1", planVersion = 2,
            plannedEntryPrice = 100.0, stopLoss = 95.0, targetPrice = 110.0,
            planCreatedAtMillis = 100L, planSource = "USER", exitPolicy = "FIXED_TARGET",
            tradePurpose = "SWING", buyFees = 15.0, sellFees = 16.0,
            peakPrice = 112.0, playbookNote = "Breakout")
        val restored = sale.toRestoredPortfolio()
        assertEquals("plan-1", restored.toTradePlan().id)
        assertEquals(2, restored.toTradePlan().version)
        assertEquals(110.0, restored.targetPrice, 0.0)
        assertEquals(15.0, restored.buyFees, 0.0)
        assertEquals(112.0, restored.peakPrice, 0.0)
        assertEquals("Breakout", restored.playbookNote)
        val backup = TradingBackup(emptyList(), emptyList(), 1000.0,
            tradeHistory = listOf(sale), cashTransactions = listOf(
                CashTransactionEntity(amount = -10015.0, type = "BUY PTT", dateMillis = 122L)))
        val decoded = Json.decodeFromString(TradingBackup.serializer(),
            Json.encodeToString(TradingBackup.serializer(), backup))
        assertEquals(sale, decoded.tradeHistory.single())
        assertEquals(backup.cashTransactions, decoded.cashTransactions)
    }

    @Test fun dividendAndNavHistoryRoundTripThroughBackup() {
        val dividend = DividendHistoryEntity(symbol = "PTT", dateMillis = 123L,
            amountPerShare = 2.0, sharesHeld = 100, totalReceived = 180.0, taxDeducted = 20.0)
        val snapshot = PortfolioSnapshotEntity("2026-09-25", 10000.0, 9000.0, 1000.0)
        val backup = TradingBackup(emptyList(), emptyList(), 1180.0,
            dividendHistory = listOf(dividend), portfolioSnapshots = listOf(snapshot))
        val decoded = Json.decodeFromString(TradingBackup.serializer(),
            Json.encodeToString(TradingBackup.serializer(), backup))
        assertEquals(dividend, decoded.dividendHistory.single())
        assertEquals(snapshot, decoded.portfolioSnapshots.single())
    }
}
