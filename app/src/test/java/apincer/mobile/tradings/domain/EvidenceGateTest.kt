package apincer.mobile.tradings.domain

import apincer.mobile.tradings.domain.EvidenceGate.PeriodResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceGateTest {
    private val beatsBoth = listOf(PeriodResult("A", 8.0, 2.0), PeriodResult("B", 6.0, 3.0))
    private val broadPnl = (1..20).associate { "S$it" to 10_000.0 }

    @Test
    fun passesWhenAllCriteriaMet() {
        val v = EvidenceGate.evaluate(beatsBoth, broadPnl, totalTrades = 300, expectancyR = 0.2)
        assertTrue(v.reasons.joinToString(), v.passed)
    }

    @Test
    fun failsWhenOneSubPeriodTrails() {
        val v = EvidenceGate.evaluate(listOf(PeriodResult("A", 23.8, 1.75), PeriodResult("B", -1.0, 3.19)), broadPnl, 300, 0.4)
        assertFalse(v.passed)
        assertEquals(1, v.reasons.size)
        assertTrue(v.reasons.single().contains("B"))
    }

    @Test
    fun failsWhenProfitDependsOnTopThreeSymbols() {
        // Mirrors the breakout result: three names carry the P/L.
        // Remaining names are slightly positive (+13.7k of 1.9M), as in the real report: still a fail.
        val pnl = mapOf("DELTA" to 970_000.0, "KTC" to 468_000.0, "JMART" to 454_000.0, "X" to 20_000.0, "Y" to -6_300.0)
        val v = EvidenceGate.evaluate(beatsBoth, pnl, 300, 0.4)
        assertFalse(v.passed)
        assertTrue(v.reasons.any { it.contains("top 3") })
    }

    @Test
    fun failsOnSmallSampleAndNegativeExpectancy() {
        val v = EvidenceGate.evaluate(beatsBoth, broadPnl, totalTrades = 40, expectancyR = -0.1)
        assertEquals(2, v.reasons.size)
    }
}
