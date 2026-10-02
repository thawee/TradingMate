package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * 1-4 week momentum rules, fixed before the first run (tasks/todo.md, 2026-10-02):
 * buy a new [entryHigh]-session closing high above SMA50 (optionally only while TDEX is above its
 * SMA50); sell on a new [exitLow]-session closing low, the 2x ATR stop, or after [maxBars] sessions.
 */
class MomentumRule(
    override val name: String,
    private val entryHigh: Int,
    private val exitLow: Int,
    private val maxBars: Int,
    private val marketOk: ((String) -> Boolean)?
) : BacktestRule {
    override fun signal(bars: List<ScrapedHistoricalPrice>, i: Int, held: HeldPosition?, isSet50: Boolean, lookback: Int): TradeSignal {
        val close = bars[i].close
        if (held != null) {
            val w = bars.subList(maxOf(0, i + 1 - lookback), i + 1)
            val atrPct = TechnicalAnalysis.calculateATR(w.map { it.high }, w.map { it.low }, w.map { it.close })?.let { it / close * 100 }
            val stop = held.entryFill * (1 + TechnicalAnalysis.legacyStopLossPercent(atrPct, isSet50) / 100)
            if (close <= stop) return TradeSignal(IndicatorSignal.SELL, "Stop", "")
            if (i >= exitLow && close < (i - exitLow + 1 until i).minOf { bars[it].close })
                return TradeSignal(IndicatorSignal.SELL, "$exitLow-day low", "")
            if (held.barsHeld >= maxBars) return TradeSignal(IndicatorSignal.SELL, "Time ($maxBars days)", "")
            return HOLD
        }
        if (i < 260) return HOLD
        if (close <= (i - entryHigh + 1 until i).maxOf { bars[it].close }) return HOLD
        val sma50 = TechnicalAnalysis.calculateSMA(bars.subList(i - 49, i + 1).map { it.close }, 50) ?: return HOLD
        if (close <= sma50) return HOLD
        if (marketOk != null && !marketOk.invoke(bars[i].date)) return HOLD
        return TradeSignal(IndicatorSignal.BUY, "$entryHigh-day high", "")
    }

    override fun rank(bars: List<ScrapedHistoricalPrice>, i: Int) = if (i >= 126) bars[i].close / bars[i - 126].close - 1.0 else 0.0

    private companion object { val HOLD = TradeSignal(IndicatorSignal.NEUTRAL, "", "") }
}

/**
 * Portfolio replay and evidence gate for the momentum rules. Skipped unless MOMENTUM=1.
 *
 *   MOMENTUM=1 ./gradlew testDebugUnitTest --tests '*MomentumRulesReport*'
 *
 * Writes tools/backtest/momentum_report.md.
 */
class MomentumRulesReport {
    private val root = File(System.getProperty("user.dir")).let { if (File(it, "tools").exists()) it else it.parentFile }
    private val dataDir = File(root, "tools/backtest/data")
    private val frozenSet50 = File(root, "tools/backtest/universe.txt").readLines()
        .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()

    private fun load(file: File): List<ScrapedHistoricalPrice> = file.readLines().drop(1).mapNotNull { line ->
        val c = line.split(",")
        if (c.size < 6) null else ScrapedHistoricalPrice(
            date = c[0], close = c[4].toDouble(), volume = c[5].toDouble().toLong(), high = c[2].toDouble(), low = c[3].toDouble())
    }

    @Test
    fun generateReport() {
        assumeTrue(System.getenv("MOMENTUM") == "1" && dataDir.isDirectory)
        val all = dataDir.listFiles { f -> f.extension == "csv" }!!.associate { it.nameWithoutExtension to load(it) }
        val benchmark = all.getValue("TDEX")
        val universe = all - "TDEX"
        val tdexUp: Map<String, Boolean> = benchmark.indices.filter { it >= 49 }.associate { i ->
            benchmark[i].date to (benchmark[i].close > benchmark.subList(i - 49, i + 1).sumOf { it.close } / 50)
        }
        val market: (String) -> Boolean = { d -> tdexUp[d] ?: false }
        val rules = listOf(
            MomentumRule("M1 20d high / 10d low / 20d max, market filter", 20, 10, 20, market),
            MomentumRule("M2 20d high / 20d low / 40d max, market filter", 20, 20, 40, market),
            MomentumRule("M3 20d high / 10d low / 20d max, no market filter", 20, 10, 20, null)
        )
        val periods = listOf(
            "Full 2015-2025" to ("2015-01-01" to "2025-12-31"),
            "2015-2020" to ("2015-01-01" to "2020-12-31"),
            "2021-2025" to ("2021-01-01" to "2025-12-31")
        )
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)
        fun f0(v: Double) = String.format(Locale.ENGLISH, "%,.0f", v)
        val sb = StringBuilder("# 1-4 Week Momentum Rules\n\n")
        sb.appendLine("Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: ${universe.size} SET50 stocks as of H1 2025 (tools/backtest/universe.txt), dividend-adjusted. " +
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
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- Survivorship bias: today's SET50 applied to 2015-2025 flatters momentum rules most.")
        sb.appendLine("- Daily closes only; gaps fill at the next close. The ATR stop is recomputed from current volatility, as in the app's trend-exit replay.")
        File(root, "tools/backtest/momentum_report.md").writeText(sb.toString())
        println(sb)
    }
}
