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
            var full: RuleStudy.Ranked? = null
            for ((label, range) in periods) {
                val r = RuleStudy.rankedPortfolio(universe, tdex, range.first, range.second) { _, bars, i ->
                    if (i < v.fromAgo) null
                    else (bars[i - v.toAgo].close / bars[i - v.fromAgo].close - 1).takeIf { !v.positiveOnly || it > 0.0 }
                }
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
}
