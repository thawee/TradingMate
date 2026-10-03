package apincer.mobile.tradings.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartHistoryParserTest {
    // TDEX.BK as Yahoo returned it on 2026-10-03: the 2026-10-02 session has a timestamp but no close.
    private val tdex = JSONObject("""
        {"chart":{"result":[{"timestamp":[1790650800,1790737200,1790823600,1790910000],
          "indicators":{"quote":[{"close":[10.61,10.35,10.33,null],"volume":[1000,2000,1500,null],
            "high":[10.7,10.4,10.4,null],"low":[10.5,10.3,10.3,null]}]}}]}}
    """.trimIndent())

    @Test fun nullCloseIsSkippedByDefault() {
        val bars = SetScraper.parseChartHistory(tdex, dividendAdjusted = false)
        assertEquals(listOf("2026-09-29", "2026-09-30", "2026-10-01"), bars.map { it.date })
    }

    @Test fun indexProxyCarriesTheLastCloseIntoASessionWithoutOne() {
        val bars = SetScraper.parseChartHistory(tdex, dividendAdjusted = false, carryForwardMissingClose = true)
        assertEquals("2026-10-02", bars.last().date)
        assertEquals(10.33, bars.last().close, 0.0)
        assertEquals(0L, bars.last().volume)
        assertEquals(10.33, bars.last().high, 0.0)
        assertEquals(4, bars.size)
    }

    @Test fun leadingNullHasNothingToCarry() {
        val json = JSONObject("""{"chart":{"result":[{"timestamp":[1790650800,1790737200],
            "indicators":{"quote":[{"close":[null,10.35],"volume":[null,2000]}]}}]}}""")
        assertEquals(listOf("2026-09-30"), SetScraper.parseChartHistory(json, false, true).map { it.date })
    }
}
