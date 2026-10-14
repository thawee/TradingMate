package apincer.mobile.tradings.domain

import apincer.mobile.tradings.domain.AdvisorToday.HoldingAlert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AdvisorTodayTest {
    private fun d(s: String) = LocalDate.parse(s)
    private val saturday = d("2026-10-03")

    @Test fun monthlyReviewIsTheFirstTradingSessionOfTheMonth() {
        // October's first session (Thu 1 Oct) has passed; 1 Nov is a Sunday, so the review is Mon 2 Nov.
        assertEquals(d("2026-11-02"), AdvisorToday.nextMonthlyReview(saturday))
        assertEquals(d("2026-10-01"), AdvisorToday.nextMonthlyReview(d("2026-10-01")))
    }

    @Test fun dcaMovesToTheNextSessionAndToNextMonthOncePassed() {
        assertEquals(d("2026-10-05"), AdvisorToday.nextDca(saturday, 4))   // Sun 4 Oct -> Mon 5 Oct
        assertEquals(d("2026-10-14"), AdvisorToday.nextDca(saturday, 13))  // 13 Oct is a SET holiday
        assertEquals(d("2026-11-02"), AdvisorToday.nextDca(saturday, 1))   // 1 Oct passed; 1 Nov is a Sunday
        // Day 31 in February clamps to the 28th (a Sunday in 2027), then moves to Monday 1 March.
        assertEquals(d("2027-03-01"), AdvisorToday.nextDca(d("2027-02-01"), 31))
    }

    @Test fun itemsOrderActNowThenMonthlyThenCore() {
        val alerts = listOf(
            HoldingAlert("JMT", AlertLevel.ACT_NOW),
            HoldingAlert("CPALL", AlertLevel.ACT_NOW),
            HoldingAlert("MBK", AlertLevel.MONTHLY),
            HoldingAlert("ICHI", AlertLevel.REVIEW))
        val items = AdvisorToday.items(alerts, hasDividendHoldings = true, dcaAmount = 5_000.0, dcaDay = 4, today = saturday)
        assertEquals(listOf(AlertLevel.ACT_NOW, AlertLevel.ACT_NOW, AlertLevel.MONTHLY, AlertLevel.MONTHLY), items.map { it.level })
        assertEquals("JMT: review exit now.", items[0].text)
        assertEquals("CPALL: review exit now.", items[1].text)
        assertEquals("MBK: monthly review on 2 Nov.", items[2].text)
        assertEquals("Core DCA: ฿5,000 into TDEX on 5 Oct.", items[3].text)
        assertEquals(listOf("JMT", "CPALL", "MBK", null), items.map { it.holdingSymbol })
    }

    @Test fun eachMonthlyHoldingHasItsOwnActionAndDuplicateAlertsAreCollapsed() {
        val items = AdvisorToday.items(listOf(
            HoldingAlert("MBK", AlertLevel.MONTHLY),
            HoldingAlert("ICHI", AlertLevel.MONTHLY),
            HoldingAlert("MBK", AlertLevel.MONTHLY),
            HoldingAlert("JMT", AlertLevel.ACT_NOW),
            HoldingAlert("JMT", AlertLevel.ACT_NOW)
        ), hasDividendHoldings = true, dcaAmount = 0.0, dcaDay = 1, today = d("2026-11-02"))

        assertEquals(listOf("JMT", "MBK", "ICHI"), items.map { it.holdingSymbol })
        assertEquals(listOf(AlertLevel.ACT_NOW, AlertLevel.MONTHLY, AlertLevel.MONTHLY), items.map { it.level })
        assertEquals(listOf("JMT: review exit now.", "MBK: monthly review today.", "ICHI: monthly review today."),
            items.map { it.text })
    }

    @Test fun quietDaySaysSoAndSkipsWhatDoesNotApply() {
        val items = AdvisorToday.items(emptyList(), hasDividendHoldings = false, dcaAmount = 0.0, dcaDay = 1, today = saturday)
        assertEquals(listOf(AdvisorToday.Item(AlertLevel.REVIEW, "No holding needs action today.")), items)
        val reviewDay = AdvisorToday.items(emptyList(), true, 0.0, 1, d("2026-11-02"))
        assertEquals("Monthly high-yield review today.", reviewDay[1].text)
    }
}
