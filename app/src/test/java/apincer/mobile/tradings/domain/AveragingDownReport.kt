package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Averaging down on the high-dividend-yield list (F5), fixed before the first run
 * (tasks/todo.md, 2026-10-03). Skipped unless AVGDOWN=1.
 *
 *   AVGDOWN=1 ./gradlew testDebugUnitTest --tests '*AveragingDownReport*'
 *
 * Writes tools/backtest/averaging_down_report.md.
 */
class AveragingDownReport {
    internal data class Variant(val name: String, val rule: RuleStudy.HeldRule?)

    @Test
    fun generateReport() {
        val holdoutDir = File(RuleStudy.root, "tools/backtest/data_holdout")
        assumeTrue(System.getenv("AVGDOWN") == "1" && Fundamentals.dir.isDirectory && holdoutDir.isDirectory)
        val fund = Fundamentals.dir.listFiles { f -> f.extension == "csv" }!!.associate { f -> f.nameWithoutExtension to Fundamentals.load(f) }
        val score: (String, List<ScrapedHistoricalPrice>, Int) -> Double? = { s, bars, i ->
            fund[s]?.let { (qs, fin) -> Fundamentals.snapshot(qs, fin, LocalDate.parse(bars[i].date)) }?.yield?.takeIf { it > 0.0 }
        }
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)
        val variants = listOf(
            Variant("A0 F5 baseline", null),
            Variant("A1 top up to 10% once when down 15%", RuleStudy.HeldRule(topUpTo = 0.10)),
            Variant("A2 add up to 15% once when down 15%", RuleStudy.HeldRule(topUpTo = 0.15)),
            Variant("A3 resize every kept name to 10% monthly", RuleStudy.HeldRule(resizeAll = true)),
            Variant("A4 sell when down 15% (control)", RuleStudy.HeldRule(sellDown = true))
        )
        val all = RuleStudy.loadAll()
        val tdex = all.getValue("TDEX")
        val universe = (all - "TDEX").filterKeys { it in fund }
        val hAll = RuleStudy.loadAll(holdoutDir)
        val hTdex = hAll.getValue("TDEX")
        val hUniverse = (hAll - "TDEX").filterKeys { it in fund }
        val periods = listOf("Full 2015-2025" to ("2015-01-01" to "2025-12-31"), "2015-2020" to ("2015-01-01" to "2020-12-31"),
            "2021-2025" to ("2021-01-01" to "2025-12-31"), "Holdout 2011-04 to 2014" to ("2011-04-01" to "2014-12-31"))

        val sb = StringBuilder("# Averaging Down on the High Dividend Yield List\n\n")
        sb.appendLine("Variants fixed before the first run (tasks/todo.md, 2026-10-03). Base rule F5: each month hold the 10 stocks with the highest reported dividend yield known at the time, " +
            "10% target for new names, kept names not resized; InnovestX fees (ATS), 0.15% slippage per side, 100-share lots. \"Down\" = month-end close at least 15% below average cost per share including fees. " +
            "New names are bought first with F5's budget; top-ups and resizes use the remaining cash. A name sold by A4 is not re-bought in the same rebalance.\n")
        sb.appendLine("| Variant | Period | CAGR % | TDEX CAGR % | MDD % | Positions | Add/trim orders | Turnover x/yr | Max single-name weight % | Top-3 P/L share % |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|")
        val results = variants.associateWith { v ->
            periods.map { (label, range) ->
                val holdout = label.startsWith("Holdout")
                val r = RuleStudy.rankedPortfolio(if (holdout) hUniverse else universe, if (holdout) hTdex else tdex,
                    range.first, range.second, heldRule = v.rule, score = score)
                val b = PortfolioBacktest.buyAndHold(if (holdout) hTdex else tdex, PortfolioBacktestConfig(startDate = range.first, endDate = range.second))
                val total = r.pnlBySymbol.values.sum()
                val top3 = r.pnlBySymbol.values.sortedDescending().take(3).sum()
                sb.appendLine("| ${v.name} | $label | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | ${r.trades} | ${r.resizeOrders} | " +
                    "${f(r.turnoverPerYear)} | ${f(r.maxWeightPercent)} | ${if (total > 0) f(top3 / total * 100) else "-"} |")
                label to r
            }.toMap()
        }

        sb.appendLine("\n## Verdicts\n")
        sb.appendLine("Better than F5 only if CAGR beats A0 in 2015-2020, 2021-2025 and the holdout, with full-period max drawdown no worse than A0 + 5 points.\n")
        val base = results.getValue(variants.first())
        for (v in variants.drop(1)) {
            val r = results.getValue(v)
            val fails = mutableListOf<String>()
            for (p in listOf("2015-2020", "2021-2025", "Holdout 2011-04 to 2014")) {
                if (r.getValue(p).stats.cagrPercent <= base.getValue(p).stats.cagrPercent) fails += "not above A0 in $p"
            }
            if (r.getValue("Full 2015-2025").stats.maxDrawdownPercent > base.getValue("Full 2015-2025").stats.maxDrawdownPercent + 5) fails += "drawdown more than 5 points worse"
            sb.appendLine("- ${v.name}: ${if (fails.isEmpty()) "PASS" else "FAIL (${fails.joinToString("; ")})"}")
        }
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- The universe is today's SET50 members with history; delisted stocks are missing. Survivorship flatters averaging down most, because losers that never recovered are absent. A pass needs a delisting-inclusive check before it reaches the app.")
        sb.appendLine("- The holdout ranks fewer names (stocks listed later have no early prices).")
        File(RuleStudy.root, "tools/backtest/averaging_down_report.md").writeText(sb.toString())
        println(sb)
    }
}
