package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HighYieldListTest {
    @Test fun commodityFreeListRefillsFromBelowTheTopTen() {
        val yields = mapOf("PTT" to 9.0, "SCC" to 8.5, "TISCO" to 8.0, "KKP" to 7.5, "PTTEP" to 7.0,
            "BBL" to 6.0, "TTB" to 5.5, "IVL" to 5.0, "ADVANC" to 4.5, "CPALL" to 4.0, "BDMS" to 3.5, "HMPRO" to 3.0)
        val all = HighYieldList.rank(yields, topN = Int.MAX_VALUE)
        assertEquals(listOf("TISCO", "KKP", "BBL"), HighYieldList.withoutCommodities(all, topN = 3).map { it.symbol })
        assertEquals(8, HighYieldList.withoutCommodities(all).size)
        assertEquals("PTT", HighYieldList.rank(yields).first().symbol)
    }
}
