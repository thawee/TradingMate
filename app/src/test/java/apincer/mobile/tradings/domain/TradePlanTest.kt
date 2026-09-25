package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TradePlanTest {
    @Test fun fixedPlanKeepsAcceptedLevels() {
        val plan = TradePlan.fixed("PTT", 100.0, 95.0, 110.0, "SWING", "USER", "plan-1", 1L)
        assertEquals(110.0, plan.targetPrice!!, 0.0)
        assertEquals(ExitPolicy.FIXED_TARGET, plan.exitPolicy)
        assertEquals("plan-1", plan.id)
    }

    @Test fun legacyPlanDoesNotInventTarget() {
        val plan = TradePlan.legacy("PTT", 100.0, 95.0, "SWING")
        assertNull(plan.targetPrice)
        assertEquals(ExitPolicy.LEGACY, plan.exitPolicy)
    }
}
