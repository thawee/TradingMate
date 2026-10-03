package apincer.mobile.tradings.domain

import apincer.mobile.tradings.domain.HighYieldRebalance.Action
import apincer.mobile.tradings.domain.HighYieldRebalance.Holding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighYieldRebalanceTest {
    private val names = ('A'..'J').map { "S$it" }
    private val tens = names.associateWith { 10.0 }

    private fun plan(budget: Double, ranked: List<String> = names, holdings: List<Holding> = emptyList(),
                     prices: Map<String, Double> = tens, sectors: Map<String, String> = names.associateWith { "S-$it" },
                     cash: Double = budget, equity: Double = 1_000_000.0, stockCap: Double = 15.0, sectorCap: Double = 30.0) =
        HighYieldRebalance.plan(budget, ranked, holdings, prices, sectors, cash, equity, stockCap, sectorCap, true)

    @Test fun emptyPortfolioBuysTenthsInWholeLotsUntilCashRunsOut() {
        val p = plan(100_000.0)
        val buys = p.rows.filter { it.action == Action.BUY }
        assertEquals(10, buys.size)
        assertEquals(1000, buys.first().shares)
        // Fees on the first nine leave less than a full tenth for the last name.
        assertEquals(900, buys.last().shares)
        assertEquals(100_000.0, p.buyCost + p.leftoverCash, 1e-6)
        assertEquals(buys.sumOf { it.fees }, p.fees, 1e-9)
        assertTrue(p.leftoverCash >= 0.0)
    }

    @Test fun keepsHeldNamesSellsLeaversAndIgnoresCore() {
        val holdings = listOf(
            Holding("SA", 500, 10.0, 9.0, "S-SA"),
            Holding("XOLD", 1000, 20.0, 18.0, "S-X"),
            Holding("TDEX", 1000, 9.0, 8.5, null))
        val p = plan(25_000.0, ranked = listOf("SA", "SB", "SC"), holdings = holdings, cash = 1_000.0)
        assertEquals(25_000.0, p.satelliteValue, 1e-9)
        val byName = p.rows.associateBy { it.symbol }
        assertEquals(Action.KEEP, byName.getValue("SA").action)
        assertEquals(500, byName.getValue("SA").shares)
        assertEquals(Action.SELL, byName.getValue("XOLD").action)
        assertEquals(1000, byName.getValue("XOLD").shares)
        assertEquals(200, byName.getValue("SB").shares)
        assertEquals(200, byName.getValue("SC").shares)
        assertTrue("TDEX" !in byName)
        val sellFee = TechnicalAnalysis.calculateFees(20_000.0, true, true)
        assertEquals(20_000.0 - sellFee, p.sellProceeds, 1e-9)
        // Buys are funded by the sale only: budget equals the current satellite value.
        assertEquals(p.sellProceeds - p.buyCost, p.leftoverCash, 1e-9)
    }

    @Test fun extraBudgetIsCappedByActualCash() {
        val p = plan(100_000.0, cash = 30_000.0)
        assertEquals(30_000.0, p.buyCost + p.leftoverCash, 1e-6)
        // Two full tenths, then 900 shares from what fees leave, then nothing.
        assertEquals(listOf(1000, 1000, 900, 0), p.rows.filter { it.action == Action.BUY }.take(4).map { it.shares })
    }

    @Test fun missingPriceAndTooSmallBudgetAreExplained() {
        val p = plan(20_000.0, ranked = listOf("SA", "SB"), prices = mapOf("SB" to 500.0))
        val byName = p.rows.associateBy { it.symbol }
        assertEquals(Action.UNAVAILABLE, byName.getValue("SA").action)
        assertEquals(0, byName.getValue("SB").shares)
        assertEquals("Budget left is below one lot", byName.getValue("SB").note)
        assertEquals(0.0, p.buyCost, 0.0)
        assertEquals(20_000.0, p.leftoverCash, 0.0)
        // One 500-baht lot per name, two names in a top-10 rule.
        assertEquals((50_000.0 + TechnicalAnalysis.calculateFees(50_000.0, false, true)) * 10, p.fullListBudget!!, 1e-6)
    }

    @Test fun capsWarnWithoutChangingTheQuantity() {
        val p = plan(100_000.0, sectors = names.associateWith { "BANK" }, equity = 100_000.0, stockCap = 5.0)
        val buys = p.rows.filter { it.action == Action.BUY }
        assertEquals(1000, buys.first().shares)
        assertTrue(buys.first().note!!.contains("single-stock cap"))
        assertTrue(!buys[2].note!!.contains("sector cap"))
        assertTrue(buys[3].note!!.contains("BANK above your sector cap"))
        assertNull(plan(100_000.0).rows.first().note)
    }

    @Test fun heldNameWithoutPriceIsValuedAtCostAndNotCountedAsProceeds() {
        val p = plan(5_000.0, ranked = listOf("SA"), holdings = listOf(Holding("XOLD", 100, null, 50.0, null)), cash = 0.0)
        assertEquals(5_000.0, p.satelliteValue, 1e-9)
        val sell = p.rows.single { it.symbol == "XOLD" }
        assertEquals(Action.SELL, sell.action)
        assertEquals(0.0, p.sellProceeds, 0.0)
        assertEquals(0, p.rows.single { it.symbol == "SA" }.shares)
    }
}
