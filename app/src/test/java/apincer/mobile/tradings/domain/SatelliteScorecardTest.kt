package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.AdviceEventEntity
import apincer.mobile.tradings.domain.SatelliteScorecard.Dividend
import apincer.mobile.tradings.domain.SatelliteScorecard.Fill
import apincer.mobile.tradings.domain.SatelliteScorecard.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SatelliteScorecardTest {
    private val day = 86_400_000L
    private val year = (365.25 * day).toLong()
    private val t0 = 1_700_000_000_000L

    private fun buy(symbol: String, t: Long, qty: Int, price: Double) = Fill(symbol, t, Kind.BUY, qty, -qty * price)
    private fun sell(symbol: String, t: Long, qty: Int, price: Double) = Fill(symbol, t, Kind.SELL, qty, qty * price)

    @Test
    fun xirrOfTenPercentOverOneYear() {
        val r = SatelliteScorecard.xirr(listOf(t0 to -100.0, (t0 + year) to 110.0))!!
        assertEquals(0.10, r, 1e-6)
    }

    @Test
    fun xirrNeedsBothSigns() {
        assertNull(SatelliteScorecard.xirr(listOf(t0 to -100.0)))
    }

    @Test
    fun fillsFromEventsSkipsCoreAndEstimatesSellFee() {
        val events = listOf(
            AdviceEventEntity(symbol = "PTT", planId = "", planVersion = 0, kind = "BUY_FILL", timeMillis = t0, fillPrice = 10.0, quantity = 100, fees = 2.0),
            AdviceEventEntity(symbol = "PTT", planId = "", planVersion = 0, kind = "SELL_FILL", timeMillis = t0 + day, fillPrice = 12.0, quantity = 100, fees = 99.0),
            AdviceEventEntity(symbol = "TDEX", planId = "", planVersion = 0, kind = "BUY_FILL", timeMillis = t0, fillPrice = 10.0, quantity = 100),
            AdviceEventEntity(symbol = "PTT", planId = "", planVersion = 0, kind = "AI_RANKED", timeMillis = t0, fillPrice = 10.0, quantity = 100)
        )
        val fills = SatelliteScorecard.fillsFromEvents(events)
        assertEquals(2, fills.size)
        assertEquals(-1_002.0, fills[0].cashFlow, 1e-9)
        // SELL_FILL fees (99, buy+sell combined) are ignored; sell fee comes from the fee engine.
        assertEquals(1_200.0 - TechnicalAnalysis.calculateFees(1_200.0, isSelling = true), fills[1].cashFlow, 1e-9)
    }

    @Test
    fun coverageExcludesInconsistentLedgers() {
        val fills = listOf(
            buy("AAA", t0, 100, 10.0),
            sell("BBB", t0, 100, 10.0),            // sold before any journaled buy
            buy("CCC", t0, 100, 10.0)              // holding edited to 300 outside the journal
        )
        val c = SatelliteScorecard.coverage(fills, mapOf("AAA" to 100, "CCC" to 300, "TDEX" to 500))
        assertEquals(setOf("AAA"), c.included)
        assertEquals(setOf("BBB", "CCC"), c.excluded)
    }

    @Test
    fun satelliteThatDoublesBeatsFlatCore() {
        val fills = listOf(buy("AAA", t0, 1_000, 10.0))
        val c = SatelliteScorecard.compare(
            fills, emptyList(), null, t0 + year,
            priceAt = { _, t -> if (t >= t0 + year) 20.0 else 10.0 },
            corePriceAt = { 5.0 }, coreFeeRate = 0.0
        )!!
        assertEquals(20_000.0, c.satelliteEndValue, 1e-6)
        assertEquals(10_000.0, c.shadowEndValue, 1e-6)
        assertEquals(10_000.0, c.excessBaht, 1e-6)
        assertEquals(100.0, c.satelliteXirrPercent!!, 1e-3)
        assertEquals(0.0, c.shadowXirrPercent!!, 1e-3)
    }

    @Test
    fun sellProceedsAndDividendsLeaveTheShadowToo() {
        // Buy 10k, take 6k out mid-year, receive 500 dividend; core flat. Shadow keeps 10k - 6k - 500.
        val fills = listOf(buy("AAA", t0, 1_000, 10.0), sell("AAA", t0 + year / 2, 500, 12.0))
        val divs = listOf(Dividend("AAA", t0 + year / 2, 500.0))
        val c = SatelliteScorecard.compare(
            fills, divs, null, t0 + year,
            priceAt = { _, _ -> 12.0 }, corePriceAt = { 8.0 }, coreFeeRate = 0.0
        )!!
        assertEquals(6_000.0, c.satelliteEndValue, 1e-6)
        assertEquals(3_500.0, c.shadowEndValue, 1e-6)
    }

    @Test
    fun windowValuesHoldingsAtStartAndFlagsTrailing() {
        // Satellite held flat at 10 throughout; core rises 10% per year.
        val fills = listOf(buy("AAA", t0, 1_000, 10.0))
        val now = t0 + 3 * year
        val core: (Long) -> Double = { t -> 10.0 * Math.pow(1.10, (t - t0).toDouble() / year) }
        val r = SatelliteScorecard.report(
            fills, emptyList(), mapOf("AAA" to 1_000), now,
            priceAt = { _, _ -> 10.0 }, corePriceAt = core
        )
        val trailing = r.trailing12m!!
        assertEquals(now - year, trailing.fromMillis)
        assertEquals(10_000.0, trailing.satelliteEndValue, 1e-6)
        assertEquals(11_000.0, trailing.shadowEndValue, 1.0)
        assertTrue(trailing.excessBaht < 0)
        assertNotNull(r.trailing12mPriorQuarter)
        assertTrue(r.suggestReducingSatellite)
    }

    @Test
    fun missingPriceGivesNoComparison() {
        val fills = listOf(buy("AAA", t0, 100, 10.0))
        assertNull(SatelliteScorecard.compare(fills, emptyList(), null, t0 + year, { _, _ -> null }, { 5.0 }))
    }

    @Test
    fun sinceStartWaitsForMinimumHistory() {
        val fills = listOf(buy("AAA", t0, 100, 10.0))
        val early = SatelliteScorecard.report(fills, emptyList(), mapOf("AAA" to 100), t0 + 5 * day, { _, _ -> 10.0 }, { 5.0 })
        assertNull(early.sinceStart)
        assertNull(early.trailing12m)
        assertEquals(t0, early.firstFillMillis)
        val later = SatelliteScorecard.report(fills, emptyList(), mapOf("AAA" to 100), t0 + 31 * day, { _, _ -> 10.0 }, { 5.0 })
        assertNotNull(later.sinceStart)
    }

    @Test
    fun noSuggestionWithoutTrailingData() {
        val r = SatelliteScorecard.report(emptyList(), emptyList(), emptyMap(), t0, { _, _ -> 1.0 }, { 1.0 })
        assertFalse(r.suggestReducingSatellite)
        assertNull(r.sinceStart)
    }
}
