package apincer.mobile.tradings.domain

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
}
