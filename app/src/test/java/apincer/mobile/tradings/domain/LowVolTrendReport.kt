package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.sqrt

/**
 * Low-volatility portfolios and TDEX trend timing, fixed before the first run
 * (tasks/todo.md, 2026-10-02). Skipped unless LOWVOL=1.
 *
 *   LOWVOL=1 ./gradlew testDebugUnitTest --tests '*LowVolTrendReport*'
 *
 * Writes tools/backtest/lowvol_trend_report.md.
 */
class LowVolTrendReport {
    private fun returns(bars: List<ScrapedHistoricalPrice>, i: Int, n: Int): List<Double> =
        (i - n + 1..i).map { bars[it].close / bars[it - 1].close - 1 }

    private fun stdev(x: List<Double>): Double { val m = x.average(); return sqrt(x.sumOf { (it - m) * (it - m) } / (x.size - 1)) }

    @Test
    fun generateReport() {
        assumeTrue(System.getenv("LOWVOL") == "1" && RuleStudy.dataDir.isDirectory)
        val all = RuleStudy.loadAll()
        val tdex = all.getValue("TDEX")
        val universe = all - "TDEX"
        val tdexIdx = tdex.withIndex().associate { it.value.date to it.index }

        fun vol(n: Int): (String, List<ScrapedHistoricalPrice>, Int) -> Double? = { _, bars, i ->
            if (i <= n) null else -stdev(returns(bars, i, n)) // lower volatility ranks higher
        }
        val lowBeta: (String, List<ScrapedHistoricalPrice>, Int) -> Double? = { _, bars, i ->
            val j = tdexIdx[bars[i].date]
            if (i <= 252 || j == null || j <= 252) null else {
                // Align on dates both series traded.
                val pairs = (i - 251..i).mapNotNull { k ->
                    val t = tdexIdx[bars[k].date]; val tp = tdexIdx[bars[k - 1].date]
                    if (t == null || tp == null) null else (bars[k].close / bars[k - 1].close - 1) to (tdex[t].close / tdex[tp].close - 1)
                }
                if (pairs.size < 200) null else {
                    val ms = pairs.map { it.first }.average(); val mt = pairs.map { it.second }.average()
                    val cov = pairs.sumOf { (it.first - ms) * (it.second - mt) }; val varT = pairs.sumOf { (it.second - mt) * (it.second - mt) }
                    if (varT <= 0.0) null else -(cov / varT)
                }
            }
        }
        val variants = listOf(
            "LV1 lowest 63-day volatility, top 10" to vol(63),
            "LV2 lowest 252-day volatility, top 10" to vol(252),
            "LV3 lowest 252-day beta to TDEX, top 10" to lowBeta
        )
        val periods = listOf("Full 2015-2025" to ("2015-01-01" to "2025-12-31"), "2015-2020" to ("2015-01-01" to "2020-12-31"), "2021-2025" to ("2021-01-01" to "2025-12-31"))
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)
        fun f0(v: Double) = String.format(Locale.ENGLISH, "%,.0f", v)
        val sb = StringBuilder("# Low Volatility and Index Trend Timing\n\n")
        sb.appendLine("Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: ${universe.size} SET50 stocks as of H1 2025, dividend-adjusted. " +
            "Portfolios re-rank at each month end and hold the top 10 at a 10% target (same simulator as the momentum portfolio); ฿1M, InnovestX fees (ATS), 0.15% slippage per side, 100-share lots.\n")
        sb.appendLine("## Low-volatility portfolios\n")
        sb.appendLine("| Variant | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Closed positions | Win % | Avg net return / position % | Avg days held | Turnover / yr |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|")
        val gate = StringBuilder("\n### Concentration and evidence gate (full period)\n\n| Variant | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |\n|---|---|---|---|---|---|\n")
        for ((name, score) in variants) {
            val gp = mutableListOf<EvidenceGate.PeriodResult>()
            var full: RuleStudy.Ranked? = null
            for ((label, range) in periods) {
                val r = RuleStudy.rankedPortfolio(universe, tdex, range.first, range.second, score = score)
                val b = PortfolioBacktest.buyAndHold(tdex, PortfolioBacktestConfig(startDate = range.first, endDate = range.second))
                val win = if (r.positions.isEmpty()) 0.0 else r.positions.count { it > 0 } * 100.0 / r.positions.size
                sb.appendLine("| $name | $label | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.cagrPercent - b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | ${f(b.maxDrawdownPercent)} | " +
                    "${r.positions.size} | ${f(win)} | ${f(r.positions.average() * 100)} | ${f(r.avgHeld)} | ${f(r.turnoverPerYear)} |")
                if (label.startsWith("Full")) full = r else gp += EvidenceGate.PeriodResult(label, r.stats.cagrPercent, b.cagrPercent)
            }
            val r = full!!
            val top = r.pnlBySymbol.entries.sortedByDescending { it.value }.take(3)
            val total = r.pnlBySymbol.values.sum()
            val v = EvidenceGate.evaluate(gp, r.pnlBySymbol, r.trades, r.positions.average())
            gate.appendLine("| $name | ${f0(total)} | ${top.joinToString { "${it.key} ${f0(it.value)}" }} | ${f0(total - top.sumOf { it.value })} | " +
                "${if (v.passed) "PASS" else "FAIL"} | ${v.reasons.joinToString("; ").ifEmpty { "-" }} |")
        }
        sb.append(gate)

        sb.appendLine("\n## TDEX trend timing\n")
        sb.appendLine("Cash earns 0% (a conservative assumption; Thai deposits paid about 0.5-2%).\n")
        sb.appendLine("| Variant | Period | CAGR % | TDEX CAGR % | MDD % | TDEX MDD % | Round trips | Time in market % |")
        sb.appendLine("|---|---|---|---|---|---|---|---|")
        for ((name, monthly) in listOf("T1 TDEX above 200-day average" to false, "T2 TDEX above 10-month average (month end)" to true)) {
            for ((label, range) in periods) {
                val r = timing(tdex, range.first, range.second, monthly)
                val b = PortfolioBacktest.buyAndHold(tdex, PortfolioBacktestConfig(startDate = range.first, endDate = range.second))
                sb.appendLine(String.format(Locale.ENGLISH, "| %s | %s | %.2f | %.2f | %.2f | %.2f | %d | %.1f |",
                    name, label, r.first.cagrPercent, b.cagrPercent, r.first.maxDrawdownPercent, b.maxDrawdownPercent, r.second, r.third))
            }
        }
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- Survivorship bias: today's SET50 applied to 2015-2025.")
        sb.appendLine("- The cited low-risk study used long-short portfolios over 2004-2015; these are long-only.")
        sb.appendLine("- \"Expectancy\" in the gate is the average net return per closed position.")
        File(RuleStudy.root, "tools/backtest/lowvol_trend_report.md").writeText(sb.toString())
        println(sb)
    }

    /** TDEX in or out on a moving-average rule; returns stats, round trips and time in market. */
    private fun timing(bars: List<ScrapedHistoricalPrice>, from: String, to: String, monthly: Boolean): Triple<EquityStats, Int, Double> {
        val slip = 0.0015
        val first = bars.indexOfFirst { it.date >= from }; val last = bars.indexOfLast { it.date <= to }
        val monthEndIdx = bars.indices.filter { i -> i + 1 == bars.size || bars[i + 1].date.substring(0, 7) != bars[i].date.substring(0, 7) }
        fun wantIn(i: Int): Boolean? = if (!monthly) {
            if (i < 199) null else bars[i].close > bars.subList(i - 199, i + 1).sumOf { it.close } / 200
        } else {
            val ends = monthEndIdx.filter { it <= i }
            if (ends.size < 10 || ends.last() != i) null else bars[i].close > ends.takeLast(10).sumOf { bars[it].close } / 10
        }
        var cash = 1_000_000.0; var shares = 0; var trips = 0; var inDays = 0
        var desired: Boolean? = null
        val curve = mutableListOf<Pair<String, Double>>()
        for (i in first..last) {
            val px = bars[i].close
            // Act today on yesterday's decision.
            when (desired) {
                true -> if (shares == 0) {
                    val fill = px * (1 + slip)
                    var lots = (cash / (fill * 100)).toInt()
                    while (lots > 0 && lots * 100 * fill + TechnicalAnalysis.calculateFees(lots * 100 * fill, false, true) > cash) lots--
                    if (lots > 0) { val c = lots * 100 * fill; cash -= c + TechnicalAnalysis.calculateFees(c, false, true); shares = lots * 100 }
                }
                false -> if (shares > 0) {
                    val g = shares * px * (1 - slip); cash += g - TechnicalAnalysis.calculateFees(g, true, true); shares = 0; trips++
                }
                null -> Unit
            }
            wantIn(i)?.let { desired = it }
            if (shares > 0) inDays++
            curve += bars[i].date to (cash + shares * px)
        }
        return Triple(EquityStats.from(curve, 1_000_000.0), trips, inDays * 100.0 / (last - first + 1))
    }
}
