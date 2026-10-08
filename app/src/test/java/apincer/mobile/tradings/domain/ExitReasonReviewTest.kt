package apincer.mobile.tradings.domain

import apincer.mobile.tradings.domain.ExitReasonReview.Trade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ExitReasonReviewTest {
    private val bangkok = ZoneId.of("Asia/Bangkok")
    private fun at(s: String) = LocalDateTime.parse(s).atZone(bangkok).toInstant().toEpochMilli()
    private fun trade(time: String, note: String, pnl: Double, purpose: String = "SWING") = Trade(at(time), note, pnl, purpose)

    @Test fun readsEachChipLabelAndFallsBackToNotRecorded() {
        assertEquals("Target Hit", ExitReasonReview.reasonOf("[Target Hit 🎯] took profit at plan"))
        assertEquals("Stop Hit", ExitReasonReview.reasonOf("[Stop Hit 🛡️] respected the stop"))
        assertEquals("Trailing Stop", ExitReasonReview.reasonOf("[Trailing Stop 📈] "))
        assertEquals("Boredom", ExitReasonReview.reasonOf("[Boredom 😴] sideways for weeks"))
        assertEquals("Mistake", ExitReasonReview.reasonOf("[Mistake 🤦] bought on a rumour"))
        assertEquals("Fundamental", ExitReasonReview.reasonOf("[Fundamental 📜] dividend cut"))
        assertEquals("Not recorded", ExitReasonReview.reasonOf(""))
        assertEquals("Not recorded", ExitReasonReview.reasonOf("Sold from broker statement"))
        assertEquals("Not recorded", ExitReasonReview.reasonOf("[Something else] note"))
    }

    @Test fun quartersUseBangkokTime() {
        // 23:30 on 30 June in Bangkok is 16:30 UTC: still Q2, whatever the device time zone.
        assertEquals("2026 Q2", ExitReasonReview.quarterLabel(at("2026-06-30T23:30")))
        assertEquals("2026 Q3", ExitReasonReview.quarterLabel(at("2026-07-01T00:10")))
    }

    @Test fun groupsByReasonAndPicksTheMostFrequentLosingReason() {
        val trades = listOf(
            trade("2026-07-03T10:00", "[Stop Hit 🛡️] a", -1_000.0),
            trade("2026-08-10T10:00", "[Stop Hit 🛡️] b", -2_000.0),
            trade("2026-08-20T10:00", "[Mistake 🤦] c", -5_000.0),
            trade("2026-09-01T10:00", "[Target Hit 🎯] d", 4_000.0, "DIVIDEND"),
            trade("2026-10-02T10:00", "[Boredom 😴] e", -300.0))
        val (current, previous) = ExitReasonReview.review(trades, LocalDate.parse("2026-10-08"))
        assertEquals("2026 Q4", current.label)
        assertEquals(listOf("Boredom"), current.rows.map { it.reason })
        assertEquals("2026 Q3", previous.label)
        assertEquals(4, previous.trades)
        assertEquals(mapOf("SWING" to 3, "DIVIDEND" to 1), previous.byPurpose)
        val stop = previous.rows.first { it.reason == "Stop Hit" }
        assertEquals(2, stop.trades); assertEquals(2, stop.losses); assertEquals(-3_000.0, stop.netBaht, 0.0)
        assertEquals("Stop Hit", previous.topLosingReason!!.reason)
    }

    @Test fun tieOnLossCountGoesToTheLargerLoss() {
        val trades = listOf(
            trade("2026-08-01T10:00", "[Stop Hit 🛡️] a", -1_000.0),
            trade("2026-08-02T10:00", "[Mistake 🤦] b", -4_000.0))
        val (_, previous) = ExitReasonReview.review(trades, LocalDate.parse("2026-10-08"))
        assertEquals("Mistake", previous.topLosingReason!!.reason)
    }

    @Test fun quarterWithoutLossesHasNoTopLosingReason() {
        val (current, previous) = ExitReasonReview.review(
            listOf(trade("2026-10-01T10:00", "[Target Hit 🎯] a", 900.0)), LocalDate.parse("2026-10-08"))
        assertNull(current.topLosingReason)
        assertEquals(0, previous.trades)
    }
}
