package apincer.mobile.tradings.domain

import java.time.LocalDate

/**
 * Dividends a holding should have earned but that are not in the dividend log yet, so the user
 * confirms them instead of typing them. Uses today's share count, so a position resized after
 * the ex-date needs the amount corrected in the dialog.
 */
object PendingDividends {
    data class Pending(
        val symbol: String,
        val exDate: LocalDate,
        val perShare: Double,
        val shares: Int
    ) {
        val gross: Double get() = perShare * shares
        val withholding: Double get() = gross * TradingConstants.THAI_DIVIDEND_WHT_RATE / 100.0
        val net: Double get() = gross - withholding
        val key: String get() = "${symbol}_$exDate"
    }

    /**
     * [events] per symbol are (ex-date, baht per share). [recordedDates] per symbol are dates already
     * logged; one from 30 days before an ex-date to the end of the lookback counts as that payout.
     * [acquiredOn] is the first journaled buy, if known: shares bought on or after the ex-date
     * do not qualify. [dismissed] holds [Pending.key]s the user waved away.
     */
    fun find(
        holdings: Map<String, Int>,
        events: Map<String, List<Pair<LocalDate, Double>>>,
        recordedDates: Map<String, List<LocalDate>>,
        acquiredOn: Map<String, LocalDate>,
        dismissed: Set<String>,
        today: LocalDate,
        lookbackDays: Long = 120
    ): List<Pending> = holdings.filter { it.value > 0 }.flatMap { (rawSymbol, shares) ->
        val symbol = rawSymbol.uppercase()
        events[symbol].orEmpty()
            .filter { (ex, amount) -> amount > 0.0 && !ex.isAfter(today) && ex.isAfter(today.minusDays(lookbackDays)) }
            .filter { (ex, _) -> acquiredOn[symbol]?.isBefore(ex) ?: true }
            .filter { (ex, _) ->
                recordedDates[symbol].orEmpty().none { !it.isBefore(ex.minusDays(30)) && !it.isAfter(ex.plusDays(lookbackDays)) }
            }
            .map { (ex, amount) -> Pending(symbol, ex, amount, shares) }
            .filter { it.key !in dismissed }
    }.sortedBy { it.exDate }
}
