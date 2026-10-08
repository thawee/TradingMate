package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Pyramiding (add only to winners) on F5 and MOM1, fixed before the first run (tasks/todo.md,
 * 2026-10-08). Skipped unless PYRAMID=1.
 *
 *   PYRAMID=1 ./gradlew testDebugUnitTest --tests '*PyramidingReport*'
 *
 * Writes tools/backtest/pyramiding_report.md.
 */
class PyramidingReport {
    private data class Base(val name: String, val needsFundamentals: Boolean, val score: (String, List<ScrapedHistoricalPrice>, Int) -> Double?)

    @Test
    fun generateReport() {
        val holdoutDir = File(RuleStudy.root, "tools/backtest/data_holdout")
        assumeTrue(System.getenv("PYRAMID") == "1" && Fundamentals.dir.isDirectory && holdoutDir.isDirectory)
        val fund = Fundamentals.dir.listFiles { f -> f.extension == "csv" }!!.associate { f -> f.nameWithoutExtension to Fundamentals.load(f) }
        val bases = listOf(
            Base("F5", true) { s, bars, i ->
                fund[s]?.let { (qs, fin) -> Fundamentals.snapshot(qs, fin, LocalDate.parse(bars[i].date)) }?.yield?.takeIf { it > 0.0 }
            },
            Base("MOM1", false) { _, bars, i -> if (i < 126) null else bars[i - 21].close / bars[i - 126].close - 1 }
        )
        val variants = listOf(
            "B0 baseline" to null,
            "P1 5% then top up to 10% once when up 5%" to RuleStudy.HeldRule(topUpTo = 0.10, upPercent = 5.0, entryFraction = 0.5),
            "P2 10% then add up to 15% once when up 10%" to RuleStudy.HeldRule(topUpTo = 0.15, upPercent = 10.0)
        )
        val all = RuleStudy.loadAll()
        val hAll = RuleStudy.loadAll(holdoutDir)
        val periods = listOf("Full 2015-2025" to ("2015-01-01" to "2025-12-31"), "2015-2020" to ("2015-01-01" to "2020-12-31"),
            "2021-2025" to ("2021-01-01" to "2025-12-31"), "Holdout 2011-04 to 2014" to ("2011-04-01" to "2014-12-31"))
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)

        val sb = StringBuilder("# Pyramiding: Add Only to Winners\n\n")
        sb.appendLine("Variants fixed before the first run (tasks/todo.md, 2026-10-08). Bases: F5 (each month the 10 SET50 names with the highest reported dividend yield known at the time) " +
            "and MOM1 (6-1 month momentum, top 10). New names at 10% of equity unless stated, kept names not resized, a name leaves when it drops out of the top 10. " +
            "\"Up\" = month-end close at least the stated percent above average cost per share including fees, checked only at month ends. " +
            "InnovestX fees (ATS), 0.15% slippage per side, 100-share lots, ฿1M start, cash earns nothing.\n")
        sb.appendLine("| Base | Variant | Period | CAGR % | TDEX CAGR % | MDD % | Positions | Adds | Turnover x/yr | Max single-name weight % | Top-3 P/L share % |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|")
        val verdicts = StringBuilder()
        for (base in bases) {
            val universe = (all - "TDEX").let { u -> if (base.needsFundamentals) u.filterKeys { it in fund } else u }
            val hUniverse = (hAll - "TDEX").let { u -> if (base.needsFundamentals) u.filterKeys { it in fund } else u }
            val results = variants.associate { (name, rule) ->
                name to periods.associate { (label, range) ->
                    val holdout = label.startsWith("Holdout")
                    val tdex = (if (holdout) hAll else all).getValue("TDEX")
                    val r = RuleStudy.rankedPortfolio(if (holdout) hUniverse else universe, tdex, range.first, range.second, heldRule = rule, score = base.score)
                    val b = PortfolioBacktest.buyAndHold(tdex, PortfolioBacktestConfig(startDate = range.first, endDate = range.second))
                    val total = r.pnlBySymbol.values.sum()
                    val top3 = r.pnlBySymbol.values.sortedDescending().take(3).sum()
                    sb.appendLine("| ${base.name} | $name | $label | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | ${r.trades} | ${r.resizeOrders} | " +
                        "${f(r.turnoverPerYear)} | ${f(r.maxWeightPercent)} | ${if (total > 0) f(top3 / total * 100) else "-"} |")
                    label to r
                }
            }
            val b0 = results.getValue(variants.first().first)
            for ((name, _) in variants.drop(1)) {
                val r = results.getValue(name)
                val fails = mutableListOf<String>()
                for (p in listOf("2015-2020", "2021-2025", "Holdout 2011-04 to 2014")) {
                    if (r.getValue(p).stats.cagrPercent <= b0.getValue(p).stats.cagrPercent) fails += "not above B0 in $p"
                }
                if (r.getValue("Full 2015-2025").stats.maxDrawdownPercent > b0.getValue("Full 2015-2025").stats.maxDrawdownPercent + 5) fails += "drawdown more than 5 points worse"
                verdicts.appendLine("- ${base.name} $name: ${if (fails.isEmpty()) "PASS" else "FAIL (${fails.joinToString("; ")})"}")
            }
        }
        sb.appendLine("\n## Verdicts\n")
        sb.appendLine("Better than its base only if CAGR beats B0 in 2015-2020, 2021-2025 and the holdout, with full-period max drawdown no worse than B0 + 5 points. A MOM1 pass still needs the TDEX evidence gate.\n")
        sb.append(verdicts)
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- The universe is today's SET50 members with history; delisted stocks are missing.")
        sb.appendLine("- Checks run only at month ends; the interviewed trader adds intraday and uses leverage (block trades, futures), which a cash account cannot replicate.")
        sb.appendLine("- P1 leaves half of each new name's budget in cash until the top-up; that cash earns nothing here.")
        sb.appendLine("- The holdout ranks fewer names (stocks listed later have no early prices).")
        File(RuleStudy.root, "tools/backtest/pyramiding_report.md").writeText(sb.toString())
        println(sb)
    }
}
