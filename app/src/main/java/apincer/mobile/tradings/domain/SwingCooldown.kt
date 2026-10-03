package apincer.mobile.tradings.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Advisory pause on new Swing buys after a losing streak or a heavy month, adapted from tradermonty's
 * drawdown-circuit-breaker (defaults: 2 losses, 24-hour cooldown, 8% monthly limit). Only Swing-purpose
 * trades count and only Swing buys are warned: the monthly high-yield rebalance and core DCA follow tested
 * rules and must not be paused. The thresholds themselves are untested, so this warns and never blocks.
 */
object SwingCooldown {
    const val LOSING_STREAK = 2
    const val COOLDOWN_HOURS = 24L
    const val MONTHLY_LOSS_PERCENT = 8.0
    private val bangkok: ZoneId = ZoneId.of("Asia/Bangkok")
    private val timeFormat = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH)

    data class ClosedTrade(val symbol: String, val tradePurpose: String, val netProfitBaht: Double, val closedAtMillis: Long)

    /** A one-line warning for a new Swing buy, or null when no pause applies. */
    fun check(trades: List<ClosedTrade>, accountEquity: Double, nowMillis: Long): String? {
        val swing = trades.filter { it.tradePurpose == "SWING" && !CoreSatellite.isCore(it.symbol) &&
            it.netProfitBaht.isFinite() && it.closedAtMillis <= nowMillis }.sortedBy { it.closedAtMillis }
        val recent = swing.takeLast(LOSING_STREAK)
        if (recent.size == LOSING_STREAK && recent.all { it.netProfitBaht < 0.0 }) {
            val until = recent.last().closedAtMillis + COOLDOWN_HOURS * 3_600_000L
            if (nowMillis < until)
                return "Cooldown: your last $LOSING_STREAK swing trades closed at a loss. Consider waiting until " +
                    "${Instant.ofEpochMilli(until).atZone(bangkok).format(timeFormat)} before a new swing buy."
        }
        if (accountEquity > 0.0 && accountEquity.isFinite()) {
            val month = Instant.ofEpochMilli(nowMillis).atZone(bangkok).toLocalDate().withDayOfMonth(1)
            val monthResult = swing.filter {
                Instant.ofEpochMilli(it.closedAtMillis).atZone(bangkok).toLocalDate().withDayOfMonth(1) == month
            }.sumOf { it.netProfitBaht }
            val lossPercent = -monthResult / accountEquity * 100
            if (lossPercent >= MONTHLY_LOSS_PERCENT)
                return String.format(Locale.ENGLISH,
                    "Monthly limit: swing trades lost ฿%,.0f this month (%.1f%% of equity, limit %.0f%%). Consider pausing new swing buys until %s.",
                    -monthResult, lossPercent, MONTHLY_LOSS_PERCENT, nextMonth(month))
        }
        return null
    }

    private fun nextMonth(month: LocalDate): String =
        month.plusMonths(1).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
}
