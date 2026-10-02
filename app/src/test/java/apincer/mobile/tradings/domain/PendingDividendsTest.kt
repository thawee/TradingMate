package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PendingDividendsTest {
    private val today = LocalDate.parse("2026-10-02")
    private fun d(s: String) = LocalDate.parse(s)
    private val events = mapOf(
        "PTT" to listOf(d("2026-03-05") to 1.4, d("2026-09-01") to 0.9),
        "MBK" to listOf(d("2026-08-20") to 0.5)
    )

    @Test fun recentUnloggedPayoutIsPendingWithWithholding() {
        val p = PendingDividends.find(mapOf("PTT" to 1000), events, emptyMap(), emptyMap(), emptySet(), today)
        assertEquals(1, p.size) // March is outside the 120-day lookback
        assertEquals(900.0, p[0].gross, 1e-9)
        assertEquals(810.0, p[0].net, 1e-9)
    }

    @Test fun loggedDismissedAndLateBuysAreSkipped() {
        val holdings = mapOf("PTT" to 1000, "MBK" to 200)
        // PTT logged on the payment date; MBK bought on its ex-date, so not entitled.
        val p = PendingDividends.find(holdings, events, mapOf("PTT" to listOf(d("2026-09-25"))),
            mapOf("MBK" to d("2026-08-20")), emptySet(), today)
        assertTrue(p.isEmpty())
        val dismissed = PendingDividends.find(mapOf("MBK" to 200), events, emptyMap(), emptyMap(), setOf("MBK_2026-08-20"), today)
        assertTrue(dismissed.isEmpty())
    }

    @Test fun unknownAcquisitionDateIsIncludedForTheUserToConfirm() {
        val p = PendingDividends.find(mapOf("MBK" to 200), events, emptyMap(), emptyMap(), emptySet(), today)
        assertEquals(listOf("MBK"), p.map { it.symbol })
        assertEquals(90.0, p[0].net, 1e-9)
    }
}
