package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Robustness and holdout checks for the high-dividend-yield screen (F5), fixed before the first run
 * (tasks/todo.md, 2026-10-02). Skipped unless DIVROBUST=1.
 *
 *   BACKTEST_OUT=data_holdout BACKTEST_START=2008 python3 tools/backtest/fetch_history.py
 *   DIVROBUST=1 ./gradlew testDebugUnitTest --tests '*DividendYieldRobustnessReport*'
 *
 * Writes tools/backtest/dividend_yield_robustness.md.
 */
class DividendYieldRobustnessReport {
    data class Check(val name: String, val topN: Int, val quarterly: Boolean, val lag: Long?)

    @Test
    fun generateReport() {
        val holdoutDir = File(RuleStudy.root, "tools/backtest/data_holdout")
        assumeTrue(System.getenv("DIVROBUST") == "1" && Fundamentals.dir.isDirectory && holdoutDir.isDirectory)
        val files = Fundamentals.dir.listFiles { f -> f.extension == "csv" }!!
        fun fund(lag: Long?) = files.associate { f ->
            f.nameWithoutExtension to (if (lag == null) Fundamentals.load(f) else Fundamentals.load(f, lag, lag))
        }
        fun yieldScore(fund: Map<String, Pair<List<Fundamentals.Quarter>, Boolean>>): (String, List<ScrapedHistoricalPrice>, Int) -> Double? = { s, bars, i ->
            fund[s]?.let { (qs, fin) -> Fundamentals.snapshot(qs, fin, LocalDate.parse(bars[i].date)) }?.yield?.takeIf { it > 0.0 }
        }
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)
        val sb = StringBuilder("# High Dividend Yield: Robustness and Holdout\n\n")
        sb.appendLine("Checks fixed before the first run (tasks/todo.md, 2026-10-02). Base rule F5: each month hold the 10 stocks with the highest reported dividend yield known at the time " +
            "(50-day lag after Q1-Q3, 90 days after Q4), 10% target each; InnovestX fees (ATS), 0.15% slippage per side, 100-share lots.\n")

        val all = RuleStudy.loadAll()
        val tdex = all.getValue("TDEX")
        val baseFund = fund(null)
        val universe = (all - "TDEX").filterKeys { it in baseFund }
        val checks = listOf(
            Check("F5 base: top 10, monthly", 10, false, null),
            Check("D1 top 5", 5, false, null),
            Check("D2 top 15", 15, false, null),
            Check("D3 quarterly rebalance", 10, true, null),
            Check("D4 120-day lag", 10, false, 120)
        )
        val periods = listOf("Full 2015-2025" to ("2015-01-01" to "2025-12-31"), "2015-2020" to ("2015-01-01" to "2020-12-31"), "2021-2025" to ("2021-01-01" to "2025-12-31"))
        sb.appendLine("## Robustness (2015-2025)\n")
        sb.appendLine("| Check | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Closed positions | Avg net return / position % |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|")
        val verdicts = StringBuilder()
        for (c in checks) {
            val score = yieldScore(if (c.lag == null) baseFund else fund(c.lag))
            var beatsBoth = true
            for ((label, range) in periods) {
                val r = RuleStudy.rankedPortfolio(universe, tdex, range.first, range.second, topN = c.topN,
                    rebalanceMonths = if (c.quarterly) setOf(3, 6, 9, 12) else null, score = score)
                val b = PortfolioBacktest.buyAndHold(tdex, PortfolioBacktestConfig(startDate = range.first, endDate = range.second))
                sb.appendLine("| ${c.name} | $label | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.cagrPercent - b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | ${f(b.maxDrawdownPercent)} | " +
                    "${r.positions.size} | ${f((r.positions.takeIf { it.isNotEmpty() }?.average() ?: 0.0) * 100)} |")
                if (!label.startsWith("Full") && r.stats.cagrPercent <= b.cagrPercent) beatsBoth = false
            }
            verdicts.appendLine("- ${c.name}: ${if (beatsBoth) "beats TDEX in both sub-periods" else "does NOT beat TDEX in both sub-periods"}")
        }
        sb.appendLine().append(verdicts)

        sb.appendLine("\n## Holdout 2011-04-01 to 2014-12-31\n")
        sb.appendLine("Prices from tools/backtest/data_holdout (2008 onward); same rule and universe. This period was not used to design or choose the rule.\n")
        val hAll = RuleStudy.loadAll(holdoutDir)
        val hTdex = hAll.getValue("TDEX")
        val hUniverse = (hAll - "TDEX").filterKeys { it in baseFund }
        val hr = RuleStudy.rankedPortfolio(hUniverse, hTdex, "2011-04-01", "2014-12-31", score = yieldScore(baseFund))
        val hb = PortfolioBacktest.buyAndHold(hTdex, PortfolioBacktestConfig(startDate = "2011-04-01", endDate = "2014-12-31"))
        sb.appendLine("| Rule | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Closed positions | Avg net return / position % |")
        sb.appendLine("|---|---|---|---|---|---|---|---|")
        sb.appendLine("| F5 top 10, monthly | ${f(hr.stats.cagrPercent)} | ${f(hb.cagrPercent)} | ${f(hr.stats.cagrPercent - hb.cagrPercent)} | ${f(hr.stats.maxDrawdownPercent)} | ${f(hb.maxDrawdownPercent)} | " +
            "${hr.positions.size} | ${f((hr.positions.takeIf { it.isNotEmpty() }?.average() ?: 0.0) * 100)} |")
        val top = hr.pnlBySymbol.entries.sortedByDescending { it.value }.take(3)
        sb.appendLine("\nTop three symbols by P/L in the holdout: ${top.joinToString { "${it.key} ${String.format(Locale.ENGLISH, "%,.0f", it.value)}" }} " +
            "of ${String.format(Locale.ENGLISH, "%,.0f", hr.pnlBySymbol.values.sum())} total.")
        sb.appendLine("\nHoldout verdict: ${if (hr.stats.cagrPercent > hb.cagrPercent) "beats" else "does NOT beat"} TDEX buy-and-hold.")
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- Survivorship bias is stronger in the holdout: today's SET50 applied to 2011-2014.")
        sb.appendLine("- Several stocks have no price history before their listing (e.g. BGRIM 2017), so the holdout ranks fewer names.")
        File(RuleStudy.root, "tools/backtest/dividend_yield_robustness.md").writeText(sb.toString())
        println(sb)
    }
}
