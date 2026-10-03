package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DividendCutTest {
    private fun d(s: String) = LocalDate.parse(s)
    private val today = d("2026-10-03")

    @Test fun interimBelowFinalIsNotACut() {
        // Final 1.20 in April, interim 0.60 in September, same pattern as the year before.
        val events = listOf(d("2025-04-20") to 1.20, d("2025-09-10") to 0.60, d("2026-04-22") to 1.25, d("2026-09-12") to 0.60)
        assertNull(DividendCut.check(events, today))
    }

    @Test fun lowerThanTheSamePaymentAYearEarlierIsACut() {
        val events = listOf(d("2025-04-20") to 1.20, d("2025-09-10") to 0.60, d("2026-04-22") to 1.25, d("2026-09-12") to 0.40)
        val reason = DividendCut.check(events, today)!!
        assertTrue(reason.startsWith("Dividend cut: ฿0.40 on 2026-09-12 vs ฿0.60"))
        assertTrue(reason.endsWith("(-33%)"))
    }

    @Test fun smallTrimWithinTenPercentIsIgnored() {
        assertNull(DividendCut.check(listOf(d("2025-09-10") to 1.00, d("2026-09-12") to 0.92), today))
        assertTrue(DividendCut.check(listOf(d("2025-09-10") to 1.00, d("2026-09-12") to 0.89), today) != null)
    }

    @Test fun suspensionAfterThirteenMonths() {
        val reason = DividendCut.check(listOf(d("2025-04-20") to 1.20, d("2025-08-25") to 0.60), today)
        assertEquals("No dividend for over 13 months (last ex-date 2025-08-25)", reason)
    }

    @Test fun noComparablePaymentOrNoHistoryGivesNothing() {
        assertNull(DividendCut.check(emptyList(), today))
        // First payment ever, or a payment moved by more than 75 days: no like-for-like comparison.
        assertNull(DividendCut.check(listOf(d("2026-09-12") to 0.40), today))
        assertNull(DividendCut.check(listOf(d("2025-05-01") to 1.00, d("2026-09-12") to 0.40), today))
    }
}
