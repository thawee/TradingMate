package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Dividend-trap filters on F5, fixed before the first run (tasks/todo.md, 2026-10-08).
 * Skipped unless DIVTRAP=1.
 *
 *   DIVTRAP=1 ./gradlew testDebugUnitTest --tests '*DividendTrapReport*'
 *
 * Writes tools/backtest/dividend_trap_report.md.
 */
class DividendTrapReport {
    @Test
    fun generateReport() {
        val holdoutDir = File(RuleStudy.root, "tools/backtest/data_holdout")
        assumeTrue(System.getenv("DIVTRAP") == "1" && Fundamentals.dir.isDirectory && holdoutDir.isDirectory)
        val fund = Fundamentals.dir.listFiles { f -> f.extension == "csv" }!!.associate { f -> f.nameWithoutExtension to Fundamentals.load(f) }
        fun yieldAt(s: String, bars: List<ScrapedHistoricalPrice>, i: Int) =
            fund[s]?.let { (qs, fin) -> Fundamentals.snapshot(qs, fin, LocalDate.parse(bars[i].date)) }?.yield?.takeIf { it > 0.0 }
        fun notFallen(bars: List<ScrapedHistoricalPrice>, i: Int) = i >= 252 && bars[i].close / bars[i - 252].close - 1 > -0.20
        val commodity = setOf("PTT", "PTTEP", "BCP", "SPRC", "OR", "IVL", "PTTGC", "SCC")
        val variants = listOf<Pair<String, (String, List<ScrapedHistoricalPrice>, Int) -> Double?>>(
            "B0 F5 baseline" to { s, b, i -> yieldAt(s, b, i) },
            "TR1 skip names down 20% or more in 12 months" to { s, b, i -> yieldAt(s, b, i)?.takeIf { notFallen(b, i) } },
            "TR2 skip commodity names" to { s, b, i -> yieldAt(s, b, i)?.takeIf { s !in commodity } }
        )
        val all = RuleStudy.loadAll()
        val tdex = all.getValue("TDEX")
        val universe = (all - "TDEX").filterKeys { it in fund }
        val hAll = RuleStudy.loadAll(holdoutDir)
        val hTdex = hAll.getValue("TDEX")
        val hUniverse = (hAll - "TDEX").filterKeys { it in fund }
        val periods = listOf("Full 2015-2025" to ("2015-01-01" to "2025-12-31"), "2015-2020" to ("2015-01-01" to "2020-12-31"),
            "2021-2025" to ("2021-01-01" to "2025-12-31"), "Holdout 2011-04 to 2014" to ("2011-04-01" to "2014-12-31"))
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)

        val sb = StringBuilder("# Dividend-Trap Filters on the High Dividend Yield List\n\n")
        sb.appendLine("Variants fixed before the first run (tasks/todo.md, 2026-10-08). Base rule F5: each month hold the 10 SET50 names with the highest reported dividend yield known at the time, " +
            "10% target for new names, kept names not resized, a name leaves when it is no longer eligible and top 10. TR1: ineligible when the month-end close is at least 20% below the close 252 sessions earlier. TR2: PTT, PTTEP, BCP, SPRC, OR, IVL, PTTGC and SCC are never eligible. " +
            "InnovestX fees (ATS), 0.15% slippage per side, 100-share lots, ฿1M start, cash earns nothing.\n")
        sb.appendLine("| Variant | Period | CAGR % | TDEX CAGR % | MDD % | Positions | Avg days held | Turnover x/yr | Max single-name weight % | Top-3 P/L share % |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|")
        val results = variants.associate { (name, score) ->
            name to periods.associate { (label, range) ->
                val holdout = label.startsWith("Holdout")
                val r = RuleStudy.rankedPortfolio(if (holdout) hUniverse else universe, if (holdout) hTdex else tdex, range.first, range.second, score = score)
                val b = PortfolioBacktest.buyAndHold(if (holdout) hTdex else tdex, PortfolioBacktestConfig(startDate = range.first, endDate = range.second))
                val total = r.pnlBySymbol.values.sum()
                val top3 = r.pnlBySymbol.values.sortedDescending().take(3).sum()
                sb.appendLine("| $name | $label | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | ${r.trades} | ${f(r.avgHeld)} | " +
                    "${f(r.turnoverPerYear)} | ${f(r.maxWeightPercent)} | ${if (total > 0) f(top3 / total * 100) else "-"} |")
                label to r
            }
        }
        sb.appendLine("\n## Verdicts\n")
        sb.appendLine("Better than F5 only if CAGR beats B0 in 2015-2020, 2021-2025 and the holdout, with full-period max drawdown no worse than B0 + 5 points.\n")
        val b0 = results.getValue(variants.first().first)
        for ((name, _) in variants.drop(1)) {
            val r = results.getValue(name)
            val fails = mutableListOf<String>()
            for (p in listOf("2015-2020", "2021-2025", "Holdout 2011-04 to 2014")) {
                if (r.getValue(p).stats.cagrPercent <= b0.getValue(p).stats.cagrPercent) fails += "not above B0 in $p"
            }
            if (r.getValue("Full 2015-2025").stats.maxDrawdownPercent > b0.getValue("Full 2015-2025").stats.maxDrawdownPercent + 5) fails += "drawdown more than 5 points worse"
            sb.appendLine("- $name: ${if (fails.isEmpty()) "PASS" else "FAIL (${fails.joinToString("; ")})"}")
        }
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- The universe is today's SET50 members with history; delisted stocks are missing.")
        sb.appendLine("- The interview's main test, next year's expected dividend and the payout ratio, cannot be replayed: there are no analyst forecasts or share counts in the data. TR1 is a proxy for a yield inflated by a falling price.")
        sb.appendLine("- The holdout ranks fewer names (stocks listed later have no early prices).")
        File(RuleStudy.root, "tools/backtest/dividend_trap_report.md").writeText(sb.toString())
        println(sb)
    }
}
