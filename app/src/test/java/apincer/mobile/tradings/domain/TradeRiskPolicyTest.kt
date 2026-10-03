package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeRiskPolicyTest {
    private val limits = TradeRiskLimits(1.0, 15.0, 30.0, 15.0)

    @Test fun exactlyAtPositionLimitPasses() {
        val input = TradeRiskInput(100.0, 95.0, 100, 15.0, 100_000.0, 50_000.0,
            0.0, 0.0, true)
        assertTrue(TradeRiskPolicy.evaluate(input, limits).allowed)
    }

    @Test fun existingHoldingCountsAgainstLimit() {
        val input = TradeRiskInput(100.0, 95.0, 100, 15.0, 100_000.0, 50_000.0,
            10_000.0, 10_000.0, true)
        assertFalse(TradeRiskPolicy.evaluate(input, limits).allowed)
    }

    @Test fun feesAndReserveCanBlock() {
        val input = TradeRiskInput(100.0, 95.0, 100, 15.0, 100_000.0, 15_000.0,
            0.0, 0.0, true)
        assertFalse(TradeRiskPolicy.evaluate(input, limits).allowed)
    }

    @Test fun missingSectorIsUnverified() {
        val input = TradeRiskInput(100.0, 95.0, 100, 15.0, 100_000.0, 50_000.0,
            0.0, null, true)
        assertFalse(TradeRiskPolicy.evaluate(input, limits).allowed)
    }

    @Test fun proposedSwingNeedsNetTwoToOneTarget() {
        val base = TradeRiskInput(100.0, 95.0, 100, 15.0, 100_000.0, 50_000.0,
            0.0, 0.0, true, 110.0, true)
        assertFalse(TradeRiskPolicy.evaluate(base, limits).allowed)
        assertTrue(TradeRiskPolicy.evaluate(base.copy(targetPrice = 112.0), limits).allowed)
        assertFalse(TradeRiskPolicy.evaluate(base.copy(targetPrice = null), limits).allowed)
    }

    @Test fun proposedSwingRespectsConfiguredMinimumNetRewardRisk() {
        val input = TradeRiskInput(100.0, 95.0, 100, 15.0, 100_000.0, 50_000.0,
            0.0, 0.0, true, 112.0, true)
        assertFalse(TradeRiskPolicy.evaluate(input, limits.copy(minNetRewardRiskRatio = 3.0)).allowed)
        assertTrue(TradeRiskPolicy.evaluate(input.copy(targetPrice = 120.0),
            limits.copy(minNetRewardRiskRatio = 3.0)).allowed)
    }

    @Test fun invalidRiskSettingsCannotSilentlyDisableTheRewardCheck() {
        val input = TradeRiskInput(100.0, 95.0, 100, 15.0, 100_000.0, 50_000.0,
            0.0, 0.0, true, 112.0, true)
        assertFalse(TradeRiskPolicy.evaluate(input,
            limits.copy(minNetRewardRiskRatio = Double.NaN)).allowed)
    }

    @Test fun coreBuyIgnoresSatelliteCapsAndBuffer() {
        // ฿90,000 of TDEX on ฿100,000 equity would fail the 15% stock cap and any cash buffer.
        assertTrue(TradeRiskPolicy.evaluateCoreBuy(9.0, 10_000, 135.0, 90_135.0).allowed)
    }

    @Test fun coreBuyNeedsCashAndBoardLot() {
        assertFalse(TradeRiskPolicy.evaluateCoreBuy(9.0, 10_000, 135.0, 90_000.0).allowed)
        assertFalse(TradeRiskPolicy.evaluateCoreBuy(9.0, 150, 0.0, 90_000.0).allowed)
        assertFalse(TradeRiskPolicy.evaluateCoreBuy(0.0, 100, 0.0, 90_000.0).allowed)
    }

    private fun fit(entry: Double = 10.0, stop: Double = 9.0, cash: Double = 50_000.0,
                    stock: Double = 0.0, sector: Double? = 0.0) =
        TradeRiskPolicy.largestFit(entry, stop, 100_000.0, cash, stock, sector, true, limits)

    private fun passes(entry: Double, stop: Double, qty: Int, cash: Double, stock: Double, sector: Double?) =
        TradeRiskPolicy.evaluate(TradeRiskInput(entry, stop, qty, TechnicalAnalysis.calculateFees(entry * qty, false, true),
            100_000.0, cash, stock, sector, true), limits).allowed

    @Test fun feeAwareSizingKeepsTradesTheOldHelperLost() {
        // Gross sizing gives 1,000 shares, whose loss with fees breaks the 1,000-baht budget.
        val old = TechnicalAnalysis.calculateRecommendedPositionSize(100_000.0, 10.0, 9.0, 1.0, 15.0).shares
        assertEquals(1000, old)
        assertFalse(passes(10.0, 9.0, old, 50_000.0, 0.0, 0.0))
        val f = fit()
        assertEquals(900, f.quantity)
        assertEquals("Stop-loss risk exceeds the configured per-trade budget", f.blockingReason)
    }

    @Test fun existingHoldingsShrinkTheQuantity() {
        assertEquals(TradeRiskFit(500, "Combined holding exceeds the single-stock allocation limit"),
            fit(stop = 9.5, stock = 10_000.0, sector = 10_000.0))
        assertEquals(TradeRiskFit(200, "Sector allocation exceeds the configured limit"),
            fit(stop = 9.5, sector = 28_000.0))
    }

    @Test fun cashReserveCapsTheQuantity() {
        val f = fit(stop = 9.5, cash = 20_000.0)
        assertEquals(400, f.quantity)
        assertEquals("Purchase would breach the cash reserve", f.blockingReason)
    }

    @Test fun minimumLotBlockedOrInvalidInputGivesZeroWithReason() {
        assertEquals(TradeRiskFit(0, "Stop-loss risk exceeds the configured per-trade budget"), fit(entry = 200.0, stop = 100.0))
        assertEquals(TradeRiskFit(0, "Sector exposure is unknown"), fit(sector = null))
        assertEquals(0, fit(stop = 10.0).quantity)
        assertEquals(0, fit(cash = Double.NaN).quantity)
        assertEquals(0, fit(stock = Double.POSITIVE_INFINITY).quantity)
    }

    @Test fun largestFitMatchesALinearScan() {
        for (entry in listOf(1.5, 4.2, 10.0, 37.25, 120.0, 480.0))
            for (stopFraction in listOf(0.8, 0.93, 0.98))
                for (cash in listOf(3_000.0, 20_000.0, 60_000.0))
                    for (stock in listOf(0.0, 9_000.0)) {
                        val stop = entry * stopFraction
                        var expected = 0
                        while (passes(entry, stop, expected + 100, cash, stock, stock)) expected += 100
                        val f = TradeRiskPolicy.largestFit(entry, stop, 100_000.0, cash, stock, stock, true, limits)
                        assertEquals("entry=$entry stop=$stop cash=$cash stock=$stock", expected, f.quantity)
                        assertTrue(f.blockingReason != null)
                    }
    }
}
