package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreSatelliteTest {

    @Test
    fun onlyTdexIsCore() {
        assertTrue(CoreSatellite.isCore("TDEX"))
        assertTrue(CoreSatellite.isCore("tdex"))
        assertTrue(!CoreSatellite.isCore("PTT"))
    }

    @Test
    fun allocationAndShortfall() {
        val a = CoreSatellite.allocation(listOf("TDEX" to 60_000.0, "PTT" to 30_000.0, "KBANK" to 10_000.0), 80.0)
        assertEquals(60.0, a.corePercent, 1e-9)
        assertEquals(40.0, a.satellitePercent, 1e-9)
        assertEquals(-20.0, a.driftPercent, 1e-9)
        // Need core = 4 × satellite (40k) = 160k, so buy 100k more.
        assertEquals(100_000.0, a.coreShortfallBaht, 1e-6)
    }

    @Test
    fun noShortfallWhenCoreAboveTarget() {
        val a = CoreSatellite.allocation(listOf("TDEX" to 90_000.0, "PTT" to 10_000.0), 80.0)
        assertEquals(0.0, a.coreShortfallBaht, 1e-9)
        assertEquals(10.0, a.driftPercent, 1e-9)
    }

    @Test
    fun emptyPortfolio() {
        val a = CoreSatellite.allocation(emptyList(), 80.0)
        assertEquals(0.0, a.corePercent, 1e-9)
        assertEquals(0.0, a.coreShortfallBaht, 1e-9)
    }

    @Test
    fun dcaRoundsDownToBoardLotsIncludingFees() {
        // 10,000 / (8.00 × 100) = 12.5 lots -> 12 lots; 1,200 sh = ฿9,600 + fees fits.
        val s = CoreSatellite.dcaSuggestion(10_000.0, 8.0)
        assertEquals(1_200, s.shares)
        assertTrue(s.estimatedCost <= 10_000.0 && s.estimatedCost > 9_600.0)
        assertEquals(10_000.0 - s.estimatedCost, s.unusedBaht, 1e-9)
    }

    @Test
    fun dcaDropsLotWhenFeesPushOverBudget() {
        // Exactly 10 lots of ฿10 = ฿10,000, fees make it exceed budget -> 9 lots.
        assertEquals(900, CoreSatellite.dcaSuggestion(10_000.0, 10.0).shares)
    }

    @Test
    fun dcaBudgetBelowOneLot() {
        val s = CoreSatellite.dcaSuggestion(500.0, 8.0)
        assertEquals(0, s.shares)
        assertEquals(500.0, s.unusedBaht, 1e-9)
    }
}
