package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Market-wide backtest report. Skipped unless BACKTEST=1.
 *
 *   python3 tools/backtest/fetch_history.py
 *   BACKTEST=1 ./gradlew testDebugUnitTest --tests '*MarketBacktestReport*'
 *
 * Writes tools/backtest/report.md.
 */
class MarketBacktestReport {
    private val root = File(System.getProperty("user.dir")).let { if (File(it, "tools").exists()) it else it.parentFile }
    private val dataDir = File(root, "tools/backtest/data")

    private fun load(file: File): List<ScrapedHistoricalPrice> = file.readLines().drop(1).mapNotNull { line ->
        val c = line.split(",")
        if (c.size < 6) null else ScrapedHistoricalPrice(
            date = c[0], close = c[4].toDouble(), volume = c[5].toDouble().toLong(), high = c[2].toDouble(), low = c[3].toDouble()
        )
    }

    @Test
    fun generateReport() {
        assumeTrue(System.getenv("BACKTEST") == "1" && dataDir.isDirectory)
        val all = dataDir.listFiles { f -> f.extension == "csv" }!!.associate { it.nameWithoutExtension to load(it) }
        val benchmark = all.getValue("TDEX")
        val universe = all - "TDEX"

        val periods = listOf(
            "Full 2015-2025" to ("2015-01-01" to "2025-12-31"),
            "2015-2020" to ("2015-01-01" to "2020-12-31"),
            "2021-2025" to ("2021-01-01" to "2025-12-31")
        )
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)
        val sb = StringBuilder()
        sb.appendLine("# Market-Wide Backtest Report\n")
        sb.appendLine("Universe: ${universe.size} current SET50 stocks (Yahoo, dividend-adjusted). Benchmark: TDEX (SET50 ETF) buy-and-hold, dividend-adjusted.")
        sb.appendLine("Config: ฿1M start, 1% risk per trade, 15% single-stock cap, max 10 positions, 0.15% slippage per side, InnovestX fees (ATS), next-close fills, 260-bar indicator window.\n")
        sb.appendLine("| Period | Strategy CAGR % | TDEX CAGR % | Gap % | Strategy MDD % | TDEX MDD % | Trades | Trades/yr | Win % | Expectancy R | Exposure % | Skipped |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|")
        val reasons = mutableMapOf<String, MutableList<Double>>()
        for ((name, range) in periods) {
            val config = PortfolioBacktestConfig(startDate = range.first, endDate = range.second)
            val r = PortfolioBacktest.run(universe, config)
            val b = PortfolioBacktest.buyAndHold(benchmark, config)
            sb.appendLine("| $name | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.cagrPercent - b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | ${f(b.maxDrawdownPercent)} | ${r.trades.size} | ${f(r.tradesPerYear)} | ${f(r.winRatePercent)} | ${f(r.expectancyR)} | ${f(r.exposurePercent)} | ${r.skippedSignals} |")
            if (name.startsWith("Full")) r.trades.forEach { t ->
                reasons.getOrPut(t.exitReason.substringBefore(" (").replace("⭐ Quality: ", "")) { mutableListOf() } += t.rMultiple
            }
        }
        sb.appendLine("\n## Exit reasons (full period)\n")
        sb.appendLine("| Exit reason | Trades | Avg R | Total R |")
        sb.appendLine("|---|---|---|---|")
        reasons.entries.sortedByDescending { it.value.size }.forEach { (k, v) ->
            sb.appendLine("| $k | ${v.size} | ${f(v.average())} | ${f(v.sum())} |")
        }
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- Survivorship bias: universe is today's SET50; delisted and demoted stocks are missing, so results are optimistic.")
        sb.appendLine("- Not replayed: NVDR flow, relative strength, weekly trend, XD grace, market regime cash buffer, sector caps, fundamentals, saved plans, AI ranking.")
        sb.appendLine("- Thresholds were designed with knowledge of this period; neither sub-period is a true out-of-sample test.")
        File(root, "tools/backtest/report.md").writeText(sb.toString())
        println(sb)
    }
}
