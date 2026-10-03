package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldingSignalTest {
    private val technicalSell = TradeSignal(IndicatorSignal.SELL, "Fundamentals Broke", "ROE < 15%")
    private val legacyPlan = { TradePlan.legacy("ABC", 10.0, null, "DIVIDEND") }

    private fun resolve(purpose: String = "DIVIDEND", stop: Double = 0.0, price: Double = 10.0,
                        quantity: Int = 100, symbol: String = "ABC", policy: String? = "LEGACY") =
        HoldingSignal.resolve(symbol, quantity, purpose, stop, price, policy, legacyPlan, technicalSell)

    @Test fun dividendHoldingIgnoresTechnicalAndQualitySells() {
        assertSame(HoldingSignal.DIVIDEND_HOLD, resolve())
    }

    @Test fun savedStopStillAlertsOnDividendHolding() {
        val s = resolve(stop = 10.5)
        assertEquals(IndicatorSignal.SELL, s!!.type)
        assertEquals("STOP", s.reason)
    }

    @Test fun swingHoldingKeepsTheTechnicalSignal() {
        assertSame(technicalSell, resolve(purpose = "SWING"))
    }

    @Test fun coreFundAndNonHoldingsAreUnchanged() {
        assertSame(CoreSatellite.CORE_SIGNAL, resolve(symbol = "TDEX"))
        assertSame(technicalSell, resolve(quantity = 0))
    }

    @Test fun fixedPlanComesBeforeDividendHold() {
        val plan = { TradePlan.fixed("ABC", 10.0, 9.0, 12.0, "DIVIDEND", "test", "plan-1", 0L) }
        val s = HoldingSignal.resolve("ABC", 100, "DIVIDEND", 0.0, 12.5, "FIXED_TARGET", plan, technicalSell)
        assertEquals(IndicatorSignal.SELL, s!!.type)
        assertEquals("TARGET", s.reason)
    }

    @Test fun dividendNotesSeparateExitsFromReviews() {
        val notes = DividendExitPolicy.notes(price = 7.5, costPerShare = 10.0, savedStop = 0.0,
            yieldPercent = 2.0, roe = 8.0, inHighYieldList = false)
        assertEquals(listOf(true, false, false, false), notes.map { it.exit })
        assertTrue(notes[0].reason.startsWith("Left the high dividend yield top 10"))
        assertTrue(notes.drop(1).all { it.reason.startsWith("Review:") })
        assertTrue(notes.last().reason.contains("-25.0%"))
    }

    @Test fun healthyListedHoldingHasNoNotes() {
        assertTrue(DividendExitPolicy.notes(10.0, 9.0, 0.0, 6.0, 18.0, true).isEmpty())
        // No ranking yet: no list note.
        assertTrue(DividendExitPolicy.notes(10.0, 9.0, 0.0, null, null, null).isEmpty())
        assertEquals(listOf(true), DividendExitPolicy.notes(8.9, 9.0, 9.0, 6.0, 18.0, true).map { it.exit })
    }
}
