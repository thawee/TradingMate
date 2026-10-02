package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale

private val HOLD = TradeSignal(IndicatorSignal.NEUTRAL, "", "")

private fun atrStop(bars: List<ScrapedHistoricalPrice>, i: Int, held: HeldPosition, isSet50: Boolean, lookback: Int): Double {
    val w = bars.subList(maxOf(0, i + 1 - lookback), i + 1)
    val close = bars[i].close
    val atrPct = TechnicalAnalysis.calculateATR(w.map { it.high }, w.map { it.low }, w.map { it.close })?.let { it / close * 100 }
    return held.entryFill * (1 + TechnicalAnalysis.legacyStopLossPercent(atrPct, isSet50) / 100)
}

private fun isLastSessionOfWeek(bars: List<ScrapedHistoricalPrice>, i: Int): Boolean =
    i + 1 >= bars.size || java.time.LocalDate.parse(bars[i + 1].date).with(java.time.DayOfWeek.MONDAY) !=
        java.time.LocalDate.parse(bars[i].date).with(java.time.DayOfWeek.MONDAY)

/** R1: weekly short-term reversal. Buy last week's big losers at the week's last session, hold 5 sessions. */
object WeeklyReversalRule : BacktestRule {
    override val name = "R1 weekly reversal (5-day return <= -5%, hold 5 days)"
    override fun signal(bars: List<ScrapedHistoricalPrice>, i: Int, held: HeldPosition?, isSet50: Boolean, lookback: Int): TradeSignal {
        if (held != null) {
            if (bars[i].close <= atrStop(bars, i, held, isSet50, lookback)) return TradeSignal(IndicatorSignal.SELL, "Stop", "")
            return if (held.barsHeld >= 5) TradeSignal(IndicatorSignal.SELL, "Time (5 days)", "") else HOLD
        }
        if (i < 260 || !isLastSessionOfWeek(bars, i)) return HOLD
        return if (bars[i].close / bars[i - 5].close - 1 <= -0.05) TradeSignal(IndicatorSignal.BUY, "Weekly loser", "") else HOLD
    }
    /** Worst 5-day return fills first. */
    override fun rank(bars: List<ScrapedHistoricalPrice>, i: Int) = -(bars[i].close / bars[i - 5].close - 1)
}

/** R2/R3: buy a sharp 2-day dip (RSI(2)) inside a long uptrend; sell on the bounce above SMA5. */
class DipInUptrendRule(override val name: String, private val rsiMax: Double, private val requireSma50: Boolean) : BacktestRule {
    override fun signal(bars: List<ScrapedHistoricalPrice>, i: Int, held: HeldPosition?, isSet50: Boolean, lookback: Int): TradeSignal {
        val close = bars[i].close
        if (held != null) {
            if (close <= atrStop(bars, i, held, isSet50, lookback)) return TradeSignal(IndicatorSignal.SELL, "Stop", "")
            val sma5 = bars.subList(i - 4, i + 1).sumOf { it.close } / 5
            if (close > sma5) return TradeSignal(IndicatorSignal.SELL, "Close > SMA5", "")
            return if (held.barsHeld >= 10) TradeSignal(IndicatorSignal.SELL, "Time (10 days)", "") else HOLD
        }
        if (i < 260) return HOLD
        val closes = bars.subList(i - 199, i + 1).map { it.close }
        if (close <= closes.average()) return HOLD
        if (requireSma50 && close <= closes.takeLast(50).average()) return HOLD
        val rsi2 = TechnicalAnalysis.calculateRSI(bars.subList(i - 30, i + 1).map { it.close }, 2) ?: return HOLD
        return if (rsi2 < rsiMax) TradeSignal(IndicatorSignal.BUY, "RSI(2) dip", "") else HOLD
    }
    /** Deepest dip fills first. */
    override fun rank(bars: List<ScrapedHistoricalPrice>, i: Int) =
        -(TechnicalAnalysis.calculateRSI(bars.subList(maxOf(0, i - 30), i + 1).map { it.close }, 2) ?: 50.0)
}

/**
 * Short-term rules from the literature, fixed before the first run (tasks/todo.md, 2026-10-02).
 * Skipped unless SHORTTERM=1.
 *
 *   SHORTTERM=1 ./gradlew testDebugUnitTest --tests '*ShortTermRulesReport*'
 *
 * Writes tools/backtest/shortterm_report.md.
 */
class ShortTermRulesReport {
    @Test
    fun generateReport() {
        assumeTrue(System.getenv("SHORTTERM") == "1" && RuleStudy.dataDir.isDirectory)
        val all = RuleStudy.loadAll()
        val benchmark = all.getValue("TDEX")
        val universe = all - "TDEX"
        val rules = listOf(
            WeeklyReversalRule,
            DipInUptrendRule("R2 dip in uptrend (RSI(2) < 10, > SMA200, exit > SMA5)", 10.0, false),
            DipInUptrendRule("R3 deeper dip (RSI(2) < 5, > SMA200 and SMA50, exit > SMA5)", 5.0, true)
        )
        val sb = StringBuilder("# Short-Term Rules From the Literature\n\nRules fixed before the first run (tasks/todo.md, 2026-10-02). ")
        RuleStudy.portfolioSections(rules, universe, benchmark, sb)
        sb.appendLine("\n## R4 Turn of month on TDEX\n")
        sb.appendLine("Hold TDEX from the close of the 4th-last session of each month to the close of the 3rd session of the next; cash (0%) otherwise. " +
            "Each round trip pays InnovestX fees (ATS) and 0.15% slippage per side.\n")
        sb.appendLine("| Period | Turn-of-month CAGR % | TDEX buy-and-hold CAGR % | Turn-of-month MDD % | TDEX MDD % | Round trips | Time in market % |")
        sb.appendLine("|---|---|---|---|---|---|---|")
        for ((label, range) in listOf("Full 2015-2025" to ("2015-01-01" to "2025-12-31"), "2015-2020" to ("2015-01-01" to "2020-12-31"), "2021-2025" to ("2021-01-01" to "2025-12-31"))) {
            val r = turnOfMonth(benchmark, range.first, range.second)
            val b = PortfolioBacktest.buyAndHold(benchmark, PortfolioBacktestConfig(startDate = range.first, endDate = range.second))
            sb.appendLine(String.format(Locale.ENGLISH, "| %s | %.2f | %.2f | %.2f | %.2f | %d | %.1f |", label, r.first.cagrPercent, b.cagrPercent,
                r.first.maxDrawdownPercent, b.maxDrawdownPercent, r.second, r.third))
        }
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- Survivorship bias: today's SET50 applied to 2015-2025.")
        sb.appendLine("- The literature on short-term reversal used older data (Asian markets including Thailand, 1990s-2000s) and often ignored costs; these tests include Thai costs.")
        sb.appendLine("- Daily closes only; fills at the next close.")
        File(RuleStudy.root, "tools/backtest/shortterm_report.md").writeText(sb.toString())
        println(sb)
    }

    /** Returns equity stats, round trips and time in market for the turn-of-month TDEX rule. */
    private fun turnOfMonth(bars: List<ScrapedHistoricalPrice>, from: String, to: String): Triple<EquityStats, Int, Double> {
        val window = bars.filter { it.date in from..to }
        val byMonth = window.indices.groupBy { window[it].date.substring(0, 7) }
        val months = byMonth.keys.sorted()
        val entryIdx = mutableSetOf<Int>(); val exitIdx = mutableSetOf<Int>()
        for ((m, idx) in byMonth) {
            if (idx.size >= 4) entryIdx += idx[idx.size - 4]
            val prev = months.indexOf(m) - 1
            if (prev >= 0 && idx.size >= 3) exitIdx += idx[2]
        }
        val slip = 0.0015
        var cash = 1_000_000.0; var shares = 0; var trips = 0; var inMarket = 0
        val curve = mutableListOf<Pair<String, Double>>()
        for (i in window.indices) {
            val px = window[i].close
            if (shares > 0 && i in exitIdx) {
                val gross = shares * px * (1 - slip)
                cash += gross - TechnicalAnalysis.calculateFees(gross, true, true); shares = 0; trips++
            }
            if (shares == 0 && i in entryIdx) {
                val fill = px * (1 + slip)
                var lots = (cash / (fill * 100)).toInt()
                while (lots > 0 && lots * 100 * fill + TechnicalAnalysis.calculateFees(lots * 100 * fill, false, true) > cash) lots--
                if (lots > 0) { val cost = lots * 100 * fill; cash -= cost + TechnicalAnalysis.calculateFees(cost, false, true); shares = lots * 100 }
            }
            if (shares > 0) inMarket++
            curve += window[i].date to (cash + shares * px)
        }
        return Triple(EquityStats.from(curve, 1_000_000.0), trips, inMarket * 100.0 / window.size)
    }
}
