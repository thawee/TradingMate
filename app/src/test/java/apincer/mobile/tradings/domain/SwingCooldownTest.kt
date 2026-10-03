package apincer.mobile.tradings.domain

import apincer.mobile.tradings.domain.SwingCooldown.ClosedTrade
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SwingCooldownTest {
    private fun at(s: String) = LocalDateTime.parse(s).atZone(ZoneId.of("Asia/Bangkok")).toInstant().toEpochMilli()
    private val now = at("2026-10-03T12:00")
    private fun swing(symbol: String, profit: Double, closed: String) = ClosedTrade(symbol, "SWING", profit, at(closed))

    @Test fun twoRecentSwingLossesStartATwentyFourHourCooldown() {
        val trades = listOf(swing("AAA", -500.0, "2026-10-02T10:00"), swing("BBB", -300.0, "2026-10-03T09:30"))
        val warning = SwingCooldown.check(trades, 100_000.0, now)!!
        assertTrue(warning.startsWith("Cooldown: your last 2 swing trades closed at a loss"))
        assertTrue(warning.contains("4 Oct 09:30"))
        // A day later the cooldown has passed.
        assertNull(SwingCooldown.check(trades, 100_000.0, at("2026-10-04T09:31")))
    }

    @Test fun aWinBetweenLossesBreaksTheStreak() {
        val trades = listOf(swing("AAA", -500.0, "2026-10-02T10:00"), swing("BBB", 200.0, "2026-10-02T15:00"),
            swing("CCC", -300.0, "2026-10-03T09:30"))
        assertNull(SwingCooldown.check(trades, 100_000.0, now))
    }

    @Test fun dividendAndCoreTradesNeverCount() {
        val trades = listOf(ClosedTrade("JMT", "DIVIDEND", -3_500.0, at("2026-10-02T10:00")),
            ClosedTrade("TDEX", "SWING", -900.0, at("2026-10-03T09:00")), swing("BBB", -300.0, "2026-10-03T09:30"))
        assertNull(SwingCooldown.check(trades, 10_000.0, now))
    }

    @Test fun monthlySwingLossAtEightPercentWarnsUntilNextMonth() {
        val trades = listOf(swing("AAA", -5_000.0, "2026-10-01T10:00"), swing("BBB", 1_000.0, "2026-10-01T14:00"),
            swing("CCC", -4_000.0, "2026-09-30T10:00"))
        // October: -5,000 + 1,000 = -4,000 = 8% of 50,000. September's loss is a different month.
        val warning = SwingCooldown.check(trades, 50_000.0, now)!!
        assertTrue(warning.startsWith("Monthly limit: swing trades lost ฿4,000 this month (8.0% of equity"))
        assertTrue(warning.endsWith("until 1 Nov."))
        assertNull(SwingCooldown.check(trades, 60_000.0, now))
    }
}
