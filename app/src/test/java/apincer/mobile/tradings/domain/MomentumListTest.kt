package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MomentumListTest {
    private fun series(start: Double, end: Double, n: Int = 130) = (0 until n).map { start + (end - start) * it / (n - 1) }

    @Test fun ranksBy126SessionReturnAndSkipsShortHistory() {
        val closes = mapOf(
            "AAA" to series(10.0, 15.0),   // strongest
            "BBB" to series(10.0, 9.0),    // falling
            "CCC" to series(10.0, 12.0),
            "NEW" to series(10.0, 30.0, 60) // too little history
        )
        val ranked = MomentumList.rank(closes, topN = 2)
        assertEquals(listOf("AAA", "CCC"), ranked.map { it.symbol })
        // Return is measured from 126 sessions before the last close.
        val a = closes.getValue("AAA"); assertEquals((a.last() / a[a.size - 127] - 1) * 100, ranked[0].returnPercent, 1e-9)
    }

    @Test fun cacheRoundTrips() {
        val entries = listOf(MomentumList.Entry("DELTA", 42.5), MomentumList.Entry("PTT", 3.1))
        assertEquals("2026-10-02" to entries, MomentumList.fromJson(MomentumList.toJson("2026-10-02", entries)))
        assertEquals(null, MomentumList.fromJson("bad"))
    }

    @Test fun highYieldRanksByYieldAndSkipsMissing() {
        val ranked = HighYieldList.rank(mapOf("A" to 3.0, "B" to 7.5, "C" to null, "D" to 0.0, "E" to 5.2), topN = 2)
        assertEquals(listOf("B", "E"), ranked.map { it.symbol })
    }
}
