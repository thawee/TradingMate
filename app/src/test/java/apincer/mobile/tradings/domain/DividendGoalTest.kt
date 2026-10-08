package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DividendGoalTest {
    @Test fun neededCapitalIsTheTargetDividedByTheHoldingsYield() {
        // ฿10,000 a month = ฿120,000 a year; ฿25,000 a year on ฿500,000 is a 5% yield.
        val c = DividendGoal.capital(targetYearly = 120_000.0, yearlyDividend = 25_000.0, marketValue = 500_000.0)!!
        assertEquals(5.0, c.yieldPercent, 1e-9)
        assertEquals(2_400_000.0, c.needed, 1e-6)
        assertEquals(1_900_000.0, c.shortfall, 1e-6)
    }

    @Test fun noShortfallOnceTheTargetIsCovered() {
        val c = DividendGoal.capital(targetYearly = 20_000.0, yearlyDividend = 25_000.0, marketValue = 500_000.0)!!
        assertEquals(400_000.0, c.needed, 1e-6)
        assertEquals(0.0, c.shortfall, 0.0)
    }

    @Test fun nothingToShowWithoutATargetOrAYield() {
        assertNull(DividendGoal.capital(0.0, 25_000.0, 500_000.0))
        assertNull(DividendGoal.capital(120_000.0, 0.0, 500_000.0))
        assertNull(DividendGoal.capital(120_000.0, 25_000.0, 0.0))
    }
}
