package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import java.io.File
import java.util.Locale

/** Shared data loading and portfolio-replay report sections for rule studies. */
internal object RuleStudy {
    val root: File = File(System.getProperty("user.dir")).let { if (File(it, "tools").exists()) it else it.parentFile }
    val dataDir = File(root, "tools/backtest/data")
    val frozenSet50: Set<String> by lazy {
        File(root, "tools/backtest/universe.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()
    }

    fun loadAll(): Map<String, List<ScrapedHistoricalPrice>> =
        dataDir.listFiles { f -> f.extension == "csv" }!!.associate { f ->
            f.nameWithoutExtension to f.readLines().drop(1).mapNotNull { line ->
                val c = line.split(",")
                if (c.size < 6) null else ScrapedHistoricalPrice(
                    date = c[0], close = c[4].toDouble(), volume = c[5].toDouble().toLong(), high = c[2].toDouble(), low = c[3].toDouble())
            }
        }

    /** Results table, exit reasons, concentration and evidence gate for [rules]. */
    fun portfolioSections(rules: List<BacktestRule>, universe: Map<String, List<ScrapedHistoricalPrice>>,
                          benchmark: List<ScrapedHistoricalPrice>, sb: StringBuilder) {
        val periods = listOf(
            "Full 2015-2025" to ("2015-01-01" to "2025-12-31"),
            "2015-2020" to ("2015-01-01" to "2020-12-31"),
            "2021-2025" to ("2021-01-01" to "2025-12-31")
        )
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)
        fun f0(v: Double) = String.format(Locale.ENGLISH, "%,.0f", v)
        sb.appendLine("Universe: ${universe.size} SET50 stocks as of H1 2025 (tools/backtest/universe.txt), dividend-adjusted. " +
            "Config: ฿1M start, 1% risk per trade, 15% single-stock cap, max 10 positions, 0.15% slippage per side, InnovestX fees (ATS), next-close fills. Benchmark: TDEX buy-and-hold.\n")
        sb.appendLine("| Rule | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Trades | Trades/yr | Win % | Expectancy R | Avg calendar days held | Exposure % |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|---|")
        val gatePeriods = mutableMapOf<String, MutableList<EvidenceGate.PeriodResult>>()
        val gateFull = mutableMapOf<String, PortfolioBacktestResult>()
        val reasons = mutableMapOf<String, MutableMap<String, MutableList<Double>>>()
        for (rule in rules) for ((name, range) in periods) {
            val config = PortfolioBacktestConfig(startDate = range.first, endDate = range.second)
            val r = PortfolioBacktest.run(universe, config, rule) { it.uppercase() in frozenSet50 }
            val b = PortfolioBacktest.buyAndHold(benchmark, config)
            val avgDays = r.trades.map { java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(it.entryDate), java.time.LocalDate.parse(it.exitDate)).toDouble() }
                .takeIf { it.isNotEmpty() }?.average() ?: 0.0
            sb.appendLine("| ${rule.name} | $name | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.cagrPercent - b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | ${f(b.maxDrawdownPercent)} | ${r.trades.size} | ${f(r.tradesPerYear)} | ${f(r.winRatePercent)} | ${f(r.expectancyR)} | ${f(avgDays)} | ${f(r.exposurePercent)} |")
            if (name.startsWith("Full")) {
                gateFull[rule.name] = r
                r.trades.forEach { t -> reasons.getOrPut(rule.name) { mutableMapOf() }.getOrPut(t.exitReason) { mutableListOf() } += t.rMultiple }
            } else gatePeriods.getOrPut(rule.name) { mutableListOf() } += EvidenceGate.PeriodResult(name, r.stats.cagrPercent, b.cagrPercent)
        }
        for ((ruleName, byReason) in reasons) {
            sb.appendLine("\n## Exit reasons: $ruleName (full period)\n")
            sb.appendLine("| Exit reason | Trades | Avg R | Total R |\n|---|---|---|---|")
            byReason.entries.sortedByDescending { it.value.size }.forEach { (k, v) -> sb.appendLine("| $k | ${v.size} | ${f(v.average())} | ${f(v.sum())} |") }
        }
        sb.appendLine("\n## Concentration and evidence gate (full period)\n")
        sb.appendLine("| Rule | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |\n|---|---|---|---|---|---|")
        for (rule in rules) {
            val full = gateFull.getValue(rule.name)
            val pnl = full.trades.groupBy { it.symbol }.mapValues { (_, ts) -> ts.sumOf { it.pnlBaht } }
            val top = pnl.entries.sortedByDescending { it.value }.take(3)
            val total = pnl.values.sum()
            val v = EvidenceGate.evaluate(gatePeriods[rule.name].orEmpty(), pnl, full.trades.size, full.expectancyR)
            sb.appendLine("| ${rule.name} | ${f0(total)} | ${top.joinToString { "${it.key} ${f0(it.value)}" }} | ${f0(total - top.sumOf { it.value })} | " +
                "${if (v.passed) "PASS" else "FAIL"} | ${v.reasons.joinToString("; ").ifEmpty { "-" }} |")
        }
    }
}
