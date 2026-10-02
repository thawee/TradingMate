package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExitPolicyTest {
    private val plan = TradePlan.fixed("PTT", 100.0, 95.0, 110.0, "SWING", "USER", "p1", 1L)

    @Test fun fixedPlanDoesNotTakeProfitEarly() {
        assertNull(ExitPolicyEvaluator.evaluate(plan, 103.0))
        assertNull(ExitPolicyEvaluator.evaluate(plan, 105.0))
        assertEquals(ExitReason.TARGET, ExitPolicyEvaluator.evaluate(plan, 110.0)?.reason)
    }

    @Test fun stopWorksWithoutIndicators() {
        assertEquals(ExitReason.STOP, ExitPolicyEvaluator.evaluate(plan, 94.0)?.reason)
    }

    @Test fun dipAboveStopHoldsThePlan() {
        // Below SMA 50 with negative MACD used to invalidate here; the plan now holds to its stop.
        assertNull(ExitPolicyEvaluator.evaluate(plan, 97.0))
    }
}
