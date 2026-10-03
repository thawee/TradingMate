package apincer.mobile.tradings.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Advisor's "Today" summary: what needs the user, most urgent first. Holdings at a saved exit come
 * first, then the monthly high-yield review, then the core DCA date; when no holding needs action it says so.
 */
object AdvisorToday {
    data class Item(val level: AlertLevel, val text: String)

    /** A holding alert reduced to what the summary needs. */
    data class HoldingAlert(val symbol: String, val level: AlertLevel, val firstLine: String)

    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    fun isTradingDay(date: LocalDate): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY && !SetHolidays.isHoliday(date)

    fun nextTradingDayOnOrAfter(date: LocalDate): LocalDate {
        var d = date
        while (!isTradingDay(d)) d = d.plusDays(1)
        return d
    }

    /** First trading session of [today]'s month if it has not passed, otherwise of next month. */
    fun nextMonthlyReview(today: LocalDate): LocalDate {
        val thisMonth = nextTradingDayOnOrAfter(today.withDayOfMonth(1))
        return if (!today.isAfter(thisMonth)) thisMonth else nextTradingDayOnOrAfter(today.withDayOfMonth(1).plusMonths(1))
    }

    /** The DCA day this month (or next, once passed), moved to the next trading session. */
    fun nextDca(today: LocalDate, dayOfMonth: Int): LocalDate {
        fun inMonth(month: LocalDate) =
            nextTradingDayOnOrAfter(month.withDayOfMonth(dayOfMonth.coerceIn(1, month.lengthOfMonth())))
        val thisMonth = inMonth(today.withDayOfMonth(1))
        return if (!today.isAfter(thisMonth)) thisMonth else inMonth(today.withDayOfMonth(1).plusMonths(1))
    }

    private fun on(date: LocalDate, today: LocalDate) = if (date == today) "today" else "on ${date.format(dayMonth)}"

    fun items(alerts: List<HoldingAlert>, hasDividendHoldings: Boolean, dcaAmount: Double, dcaDay: Int,
              today: LocalDate): List<Item> {
        val items = mutableListOf<Item>()
        val actNow = alerts.filter { it.level == AlertLevel.ACT_NOW }.distinctBy { it.symbol }
        actNow.forEach { items += Item(AlertLevel.ACT_NOW, "${it.symbol}: ${it.firstLine}. Sell, or open it to hold anyway.") }
        if (actNow.isEmpty()) items += Item(AlertLevel.REVIEW, "No holding needs action today.")
        val monthlySells = alerts.filter { it.level == AlertLevel.MONTHLY }.map { it.symbol }.distinct()
        if (hasDividendHoldings || monthlySells.isNotEmpty()) {
            val review = nextMonthlyReview(today)
            items += Item(AlertLevel.MONTHLY, "Monthly high-yield review ${on(review, today)}" +
                (if (monthlySells.isEmpty()) "." else ": sell ${monthlySells.joinToString(", ")} under the tested rule."))
        }
        if (dcaAmount > 0.0) {
            items += Item(AlertLevel.MONTHLY, String.format(Locale.ENGLISH, "Core DCA: ฿%,.0f into %s %s.",
                dcaAmount, CoreSatellite.label(), on(nextDca(today, dcaDay), today)))
        }
        return items
    }
}
