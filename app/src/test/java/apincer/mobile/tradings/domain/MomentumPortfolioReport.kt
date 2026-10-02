package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * 6-month cross-sectional momentum (Jegadeesh & Titman), fixed before the first run
 * (tasks/todo.md, 2026-10-02). Skipped unless MOMPORT=1.
 *
 *   MOMPORT=1 ./gradlew testDebugUnitTest --tests '*MomentumPortfolioReport*'
 *
 * Writes tools/backtest/momentum_portfolio_report.md.
 */
class MomentumPortfolioReport {
    data class Variant(val name: String, val fromAgo: Int, val toAgo: Int, val positiveOnly: Boolean)
    data class Result(val stats: EquityStats, val positions: List<Double>, val pnlBySymbol: Map<String, Double>,
                      val trades: Int, val turnoverPerYear: Double, val avgHeld: Double)

    private val topN = 10
    private val slip = 0.0015

    @Test
    fun generateReport() {
        assumeTrue(System.getenv("MOMPORT") == "1" && RuleStudy.dataDir.isDirectory)
        val all = RuleStudy.loadAll()
        val tdex = all.getValue("TDEX")
        val universe = all - "TDEX"
        val variants = listOf(
            Variant("MOM1 6-1 month momentum, top 10", 126, 21, false),
            Variant("MOM2 6-1 month momentum, top 10, positive only", 126, 21, true),
            Variant("MOM3 6-0 month momentum, top 10", 126, 0, false)
        )
        val periods = listOf("Full 2015-2025" to ("2015-01-01" to "2025-12-31"), "2015-2020" to ("2015-01-01" to "2020-12-31"), "2021-2025" to ("2021-01-01" to "2025-12-31"))
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)
        fun f0(v: Double) = String.format(Locale.ENGLISH, "%,.0f", v)
        val sb = StringBuilder("# 6-Month Momentum Portfolio\n\n")
        sb.appendLine("Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: ${universe.size} SET50 stocks as of H1 2025 (tools/backtest/universe.txt), dividend-adjusted. " +
            "Rank at each month's last session, trade at the next close; ฿1M, 10% target per stock, 100-share lots, InnovestX fees (ATS), 0.15% slippage per side. Benchmark: TDEX buy-and-hold.\n")
        sb.appendLine("| Variant | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Closed positions | Win % | Avg net return / position % | Avg days held | Turnover / yr |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|")
        val gate = StringBuilder("\n## Concentration and evidence gate (full period)\n\n| Variant | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |\n|---|---|---|---|---|---|\n")
        for (v in variants) {
            val gatePeriods = mutableListOf<EvidenceGate.PeriodResult>()
            var full: Result? = null
            for ((label, range) in periods) {
                val r = simulate(universe, tdex, v, range.first, range.second)
                val b = PortfolioBacktest.buyAndHold(tdex, PortfolioBacktestConfig(startDate = range.first, endDate = range.second))
                val win = if (r.positions.isEmpty()) 0.0 else r.positions.count { it > 0 } * 100.0 / r.positions.size
                sb.appendLine("| ${v.name} | $label | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.cagrPercent - b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | ${f(b.maxDrawdownPercent)} | " +
                    "${r.positions.size} | ${f(win)} | ${f(r.positions.average() * 100)} | ${f(r.avgHeld)} | ${f(r.turnoverPerYear)} |")
                if (label.startsWith("Full")) full = r else gatePeriods += EvidenceGate.PeriodResult(label, r.stats.cagrPercent, b.cagrPercent)
            }
            val r = full!!
            val top = r.pnlBySymbol.entries.sortedByDescending { it.value }.take(3)
            val total = r.pnlBySymbol.values.sum()
            val verdict = EvidenceGate.evaluate(gatePeriods, r.pnlBySymbol, r.trades, r.positions.average())
            gate.appendLine("| ${v.name} | ${f0(total)} | ${top.joinToString { "${it.key} ${f0(it.value)}" }} | ${f0(total - top.sumOf { it.value })} | " +
                "${if (verdict.passed) "PASS" else "FAIL"} | ${verdict.reasons.joinToString("; ").ifEmpty { "-" }} |")
        }
        sb.append(gate)
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- Survivorship bias: today's SET50 applied to 2015-2025 flatters momentum most, since stocks that collapsed and left the index are missing.")
        sb.appendLine("- \"Expectancy\" in the gate is the average net return per closed position (no stop is used), not an R multiple.")
        sb.appendLine("- Kept names are not resized, so weights drift between rebalances.")
        File(RuleStudy.root, "tools/backtest/momentum_portfolio_report.md").writeText(sb.toString())
        println(sb)
    }

    private fun simulate(universe: Map<String, List<ScrapedHistoricalPrice>>, tdex: List<ScrapedHistoricalPrice>,
                         v: Variant, from: String, to: String): Result {
        val dates = tdex.map { it.date }.filter { it in from..to }
        val idx = universe.mapValues { (_, bars) -> bars.withIndex().associate { it.value.date to it.index } }
        val monthEnds = dates.indices.filter { i -> i + 1 == dates.size || dates[i + 1].substring(0, 7) != dates[i].substring(0, 7) }.toSet()
        var cash = 1_000_000.0
        data class Pos(val shares: Int, val cost: Double, val openDate: String)
        val held = mutableMapOf<String, Pos>()
        val closedReturns = mutableListOf<Double>(); val pnl = mutableMapOf<String, Double>(); val heldDays = mutableListOf<Double>()
        var tradedValue = 0.0
        var pending: List<String>? = null
        val curve = mutableListOf<Pair<String, Double>>()
        fun price(s: String, d: String) = idx.getValue(s)[d]?.let { universe.getValue(s)[it].close }
        for ((di, d) in dates.withIndex()) {
            pending?.let { target ->
                // Sell names that left the list, then buy new names at up to 10% of equity each.
                for (s in held.keys.filter { it !in target }) {
                    val p = price(s, d) ?: continue
                    val pos = held.remove(s)!!
                    val gross = pos.shares * p * (1 - slip)
                    val net = gross - TechnicalAnalysis.calculateFees(gross, true, true)
                    cash += net; tradedValue += gross
                    pnl[s] = (pnl[s] ?: 0.0) + net - pos.cost
                    closedReturns += net / pos.cost - 1
                    heldDays += java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(pos.openDate), java.time.LocalDate.parse(d)).toDouble()
                }
                val equity = cash + held.entries.sumOf { (s, pos) -> pos.shares * (price(s, d) ?: 0.0) }
                for (s in target.filter { it !in held }) {
                    val p = price(s, d) ?: continue
                    val fill = p * (1 + slip)
                    val budget = minOf(equity / topN, cash)
                    var lots = (budget / (fill * 100)).toInt()
                    while (lots > 0 && lots * 100 * fill + TechnicalAnalysis.calculateFees(lots * 100 * fill, false, true) > cash) lots--
                    if (lots <= 0) continue
                    val gross = lots * 100 * fill
                    val cost = gross + TechnicalAnalysis.calculateFees(gross, false, true)
                    cash -= cost; tradedValue += gross
                    held[s] = Pos(lots * 100, cost, d)
                }
                pending = null
            }
            if (di in monthEnds && di + 1 < dates.size) {
                val scores = universe.keys.mapNotNull { s ->
                    val i = idx.getValue(s)[d] ?: return@mapNotNull null
                    if (i < v.fromAgo) return@mapNotNull null
                    val bars = universe.getValue(s)
                    s to (bars[i - v.toAgo].close / bars[i - v.fromAgo].close - 1)
                }.filter { !v.positiveOnly || it.second > 0.0 }
                pending = scores.sortedByDescending { it.second }.take(topN).map { it.first }
            }
            curve += d to (cash + held.entries.sumOf { (s, pos) -> pos.shares * (price(s, d) ?: (pos.cost / pos.shares)) })
        }
        // Mark open positions at the last close for per-symbol P/L.
        val last = dates.last()
        held.forEach { (s, pos) ->
            val p = price(s, last) ?: return@forEach
            pnl[s] = (pnl[s] ?: 0.0) + pos.shares * p - pos.cost
        }
        val years = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(dates.first()), java.time.LocalDate.parse(last)) / 365.25
        val stats = EquityStats.from(curve, 1_000_000.0)
        val avgEquity = curve.map { it.second }.average()
        return Result(stats, closedReturns, pnl, closedReturns.size + held.size,
            tradedValue / avgEquity / years, heldDays.takeIf { it.isNotEmpty() }?.average() ?: 0.0)
    }
}
