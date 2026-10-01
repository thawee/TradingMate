package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/** Shared exit: legacy 2×ATR stop from entry, or close below SMA50 (trend over). */
private fun trendExit(bars: List<ScrapedHistoricalPrice>, i: Int, held: HeldPosition, isSet50: Boolean, lookback: Int): TradeSignal? {
    val w = bars.subList(maxOf(0, i + 1 - lookback), i + 1)
    val closes = w.map { it.close }
    val close = closes.last()
    val atrPct = TechnicalAnalysis.calculateATR(w.map { it.high }, w.map { it.low }, closes)?.let { it / close * 100 }
    val stop = held.entryFill * (1 + TechnicalAnalysis.legacyStopLossPercent(atrPct, isSet50) / 100)
    if (close <= stop) return TradeSignal(IndicatorSignal.SELL, "Stop Loss", "")
    val sma50 = TechnicalAnalysis.calculateSMA(closes, 50)
    if (sma50 != null && close < sma50) return TradeSignal(IndicatorSignal.SELL, "Close < SMA50", "")
    return null
}

private val HOLD = TradeSignal(IndicatorSignal.NEUTRAL, "", "")

private fun momentum(bars: List<ScrapedHistoricalPrice>, i: Int, days: Int = 126): Double =
    if (i >= days) bars[i].close / bars[i - days].close - 1.0 else 0.0

/** New 252-day closing high above SMA200; trend exit. Parameters fixed before the first run. */
object BreakoutRule : BacktestRule {
    override val name = "52w breakout + trend exit"
    override fun signal(bars: List<ScrapedHistoricalPrice>, i: Int, held: HeldPosition?, isSet50: Boolean, lookback: Int): TradeSignal {
        if (held != null) return trendExit(bars, i, held, isSet50, lookback) ?: HOLD
        if (i < 252) return HOLD
        val close = bars[i].close
        val priorHigh = (i - 251 until i).maxOf { bars[it].close }
        val sma200 = TechnicalAnalysis.calculateSMA(bars.subList(i - 199, i + 1).map { it.close }, 200)
        return if (close > priorHigh && sma200 != null && close > sma200) TradeSignal(IndicatorSignal.BUY, "Breakout", "") else HOLD
    }
    override fun rank(bars: List<ScrapedHistoricalPrice>, i: Int) = momentum(bars, i)
}

/** App BUY signals, but exits only on stop or close below SMA50: isolates entry quality. */
object AppEntryTrendExitRule : BacktestRule {
    override val name = "App entries + trend exit"
    override fun signal(bars: List<ScrapedHistoricalPrice>, i: Int, held: HeldPosition?, isSet50: Boolean, lookback: Int): TradeSignal =
        if (held != null) trendExit(bars, i, held, isSet50, lookback) ?: HOLD
        else AppSignalRule.signal(bars, i, null, isSet50, lookback)
}

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
        fun f0(v: Double) = String.format(Locale.ENGLISH, "%,.0f", v)
        val sb = StringBuilder()
        sb.appendLine("# Market-Wide Backtest Report\n")
        sb.appendLine("Universe: ${universe.size} current SET50 stocks (Yahoo, dividend-adjusted). Benchmark: TDEX (SET50 ETF) buy-and-hold, dividend-adjusted.")
        sb.appendLine("Config: ฿1M start, 1% risk per trade, 15% single-stock cap, max 10 positions, 0.15% slippage per side, InnovestX fees (ATS), next-close fills, 260-bar indicator window.\n")
        sb.appendLine("| Rule | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Trades | Trades/yr | Win % | Expectancy R | Exposure % | Skipped |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|---|")
        val rules = listOf(AppSignalRule, AppEntryTrendExitRule, BreakoutRule)
        val reasons = mutableMapOf<String, MutableMap<String, MutableList<Double>>>()
        val bySymbol = mutableMapOf<String, Map<String, Double>>()
        val gatePeriods = mutableMapOf<String, MutableList<EvidenceGate.PeriodResult>>()
        val gateFull = mutableMapOf<String, PortfolioBacktestResult>()
        for (rule in rules) for ((name, range) in periods) {
            val config = PortfolioBacktestConfig(startDate = range.first, endDate = range.second)
            val r = PortfolioBacktest.run(universe, config, rule)
            val b = PortfolioBacktest.buyAndHold(benchmark, config)
            sb.appendLine("| ${rule.name} | $name | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.cagrPercent - b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | ${f(b.maxDrawdownPercent)} | ${r.trades.size} | ${f(r.tradesPerYear)} | ${f(r.winRatePercent)} | ${f(r.expectancyR)} | ${f(r.exposurePercent)} | ${r.skippedSignals} |")
            if (name.startsWith("Full")) gateFull[rule.name] = r
            else gatePeriods.getOrPut(rule.name) { mutableListOf() } += EvidenceGate.PeriodResult(name, r.stats.cagrPercent, b.cagrPercent)
            if (name.startsWith("Full")) bySymbol[rule.name] = r.trades.groupBy { it.symbol }
                .mapValues { (_, ts) -> ts.sumOf { it.pnlBaht } }
            if (name.startsWith("Full")) r.trades.forEach { t ->
                reasons.getOrPut(rule.name) { mutableMapOf() }
                    .getOrPut(t.exitReason.substringBefore(" (").replace("⭐ Quality: ", "")) { mutableListOf() } += t.rMultiple
            }
        }
        for ((ruleName, byReason) in reasons) {
            sb.appendLine("\n## Exit reasons: $ruleName (full period)\n")
            sb.appendLine("| Exit reason | Trades | Avg R | Total R |")
            sb.appendLine("|---|---|---|---|")
            byReason.entries.sortedByDescending { it.value.size }.forEach { (k, v) ->
                sb.appendLine("| $k | ${v.size} | ${f(v.average())} | ${f(v.sum())} |")
            }
        }
        sb.appendLine("\n## Concentration of closed-trade P/L (full period)\n")
        sb.appendLine("| Rule | Total ฿ | Top 3 symbols ฿ | Total excl. top 3 ฿ | Top 3 |")
        sb.appendLine("|---|---|---|---|---|")
        for ((ruleName, pnl) in bySymbol) {
            val top = pnl.entries.sortedByDescending { it.value }.take(3)
            val total = pnl.values.sum()
            val topSum = top.sumOf { it.value }
            sb.appendLine("| $ruleName | ${f0(total)} | ${f0(topSum)} | ${f0(total - topSum)} | ${top.joinToString { "${it.key} ${f0(it.value)}" }} |")
        }
        sb.appendLine("\n## Evidence gate (docs/ADVISOR_EVALUATION.md)\n")
        sb.appendLine("| Rule | Verdict | Reasons |")
        sb.appendLine("|---|---|---|")
        for (rule in rules) {
            val full = gateFull.getValue(rule.name)
            val v = EvidenceGate.evaluate(
                gatePeriods[rule.name].orEmpty(), bySymbol[rule.name].orEmpty(), full.trades.size, full.expectancyR
            )
            sb.appendLine("| ${rule.name} | ${if (v.passed) "PASS" else "FAIL"} | ${v.reasons.joinToString("; ").ifEmpty { "-" }} |")
        }
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- Survivorship bias: universe is today's SET50; delisted and demoted stocks are missing, so results are optimistic.")
        sb.appendLine("- Not replayed: NVDR flow, relative strength, weekly trend, XD grace, market regime cash buffer, sector caps, fundamentals, saved plans, AI ranking.")
        sb.appendLine("- App thresholds were designed with knowledge of this period; neither sub-period is a true out-of-sample test. Alternative rules use textbook parameters fixed before their first run.\n- Simultaneous BUYs: app rules fill in symbol order; the breakout rule fills by 126-day momentum.")
        File(root, "tools/backtest/report.md").writeText(sb.toString())
        println(sb)
    }
}
