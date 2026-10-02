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
    @Test
    fun generateReport() {
        assumeTrue(System.getenv("MOMENTUM") == "1" && RuleStudy.dataDir.isDirectory)
        val all = RuleStudy.loadAll()
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
        val sb = StringBuilder("# 1-4 Week Momentum Rules\n\nRules fixed before the first run (tasks/todo.md, 2026-10-02). ")
        RuleStudy.portfolioSections(rules, universe, benchmark, sb)
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- Survivorship bias: today's SET50 applied to 2015-2025 flatters momentum rules most.")
        sb.appendLine("- Daily closes only; gaps fill at the next close. The ATR stop is recomputed from current volatility, as in the app's trend-exit replay.")
        File(RuleStudy.root, "tools/backtest/momentum_report.md").writeText(sb.toString())
        println(sb)
    }
}
