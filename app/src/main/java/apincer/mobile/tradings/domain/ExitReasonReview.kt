package apincer.mobile.tradings.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Closed trades grouped by the exit reason chosen in the Sell dialog, per calendar quarter (Bangkok), so
 * the most frequent cause of losing trades can be worked on next quarter (tasks/todo.md, 2026-10-08).
 * The reason is the bracketed prefix the dialog writes into the trade note: "[Stop Hit 🛡️] lesson".
 */
object ExitReasonReview {
    val REASONS = listOf("Target Hit", "Stop Hit", "Trailing Stop", "Boredom", "Mistake", "Fundamental")
    const val NOT_RECORDED = "Not recorded"
    private val bangkok = ZoneId.of("Asia/Bangkok")

    data class Trade(val dateMillis: Long, val note: String, val netProfitBaht: Double, val purpose: String)
    data class Row(val reason: String, val trades: Int, val losses: Int, val netBaht: Double, val lossBaht: Double)
    data class Quarter(val label: String, val rows: List<Row>, val byPurpose: Map<String, Int>) {
        val trades: Int get() = rows.sumOf { it.trades }
        /** Most losing trades; a tie goes to the larger total loss. */
        val topLosingReason: Row? get() = rows.filter { it.losses > 0 }
            .sortedWith(compareByDescending<Row> { it.losses }.thenBy { it.lossBaht }).firstOrNull()
    }

    fun reasonOf(note: String): String {
        val tag = Regex("^\\[([^]]*)]").find(note.trim())?.groupValues?.get(1) ?: return NOT_RECORDED
        return REASONS.firstOrNull { tag.startsWith(it) } ?: NOT_RECORDED
    }

    fun quarterLabel(millis: Long): String = label(Instant.ofEpochMilli(millis).atZone(bangkok).toLocalDate())

    private fun label(d: LocalDate) = "${d.year} Q${(d.monthValue - 1) / 3 + 1}"

    /** The current and the previous quarter as of [today]. */
    fun review(trades: List<Trade>, today: LocalDate): Pair<Quarter, Quarter> {
        val currentLabel = label(today)
        val previousLabel = label(today.withDayOfMonth(1).minusMonths(3))
        fun quarter(lbl: String): Quarter {
            val inQ = trades.filter { quarterLabel(it.dateMillis) == lbl }
            val rows = inQ.groupBy { reasonOf(it.note) }.map { (reason, ts) ->
                val losing = ts.filter { it.netProfitBaht < 0 }
                Row(reason, ts.size, losing.size, ts.sumOf { it.netProfitBaht }, losing.sumOf { it.netProfitBaht })
            }.sortedWith(compareByDescending<Row> { it.trades }.thenBy { it.reason })
            return Quarter(lbl, rows, inQ.groupingBy { it.purpose }.eachCount())
        }
        return quarter(currentLabel) to quarter(previousLabel)
    }
}
