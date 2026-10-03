package apincer.mobile.tradings.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Dividend cut or suspension on a holding, as a review trigger (adapted from the "T1" trigger in
 * tradermonty's kanchi-dividend-review-monitor). Thai companies often pay a smaller interim and a larger
 * final dividend, so the latest payment is compared with the payment about a year earlier, not the one
 * just before it. A review note only; nothing is sold.
 */
object DividendCut {
    const val CUT_THRESHOLD = 0.10
    private const val YEAR_DAYS = 365L
    private const val MATCH_WINDOW_DAYS = 75L
    private const val SUSPENSION_DAYS = 400L

    /** [events] are (ex-date, baht per share); returns a one-line reason or null when nothing to review. */
    fun check(events: List<Pair<LocalDate, Double>>, today: LocalDate): String? {
        val paid = events.filter { it.second.isFinite() && it.second > 0.0 }.sortedBy { it.first }
        val latest = paid.lastOrNull() ?: return null
        val sinceLatest = ChronoUnit.DAYS.between(latest.first, today)
        if (sinceLatest > SUSPENSION_DAYS)
            return "No dividend for over 13 months (last ex-date ${latest.first})"
        val yearAgo = latest.first.minusDays(YEAR_DAYS)
        val comparable = paid.dropLast(1)
            .filter { kotlin.math.abs(ChronoUnit.DAYS.between(it.first, yearAgo)) <= MATCH_WINDOW_DAYS }
            .minByOrNull { kotlin.math.abs(ChronoUnit.DAYS.between(it.first, yearAgo)) } ?: return null
        if (latest.second >= comparable.second * (1 - CUT_THRESHOLD)) return null
        val change = (latest.second / comparable.second - 1) * 100
        return String.format(Locale.ENGLISH, "Dividend cut: ฿%.2f on %s vs ฿%.2f a year earlier (%.0f%%)",
            latest.second, latest.first, comparable.second, change)
    }
}
