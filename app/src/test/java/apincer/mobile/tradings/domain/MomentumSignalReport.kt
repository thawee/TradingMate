package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * 6-month momentum list with the app's BUY/POTENTIAL signal as entry timing, fixed before the
 * first run (tasks/todo.md, 2026-10-02). Skipped unless MOMSIG=1.
 *
 *   MOMSIG=1 ./gradlew testDebugUnitTest --tests '*MomentumSignalReport*'
 *
 * Writes tools/backtest/momentum_signal_report.md.
 */
class MomentumSignalReport {
    /** [entryOn] null = buy at the first close after the ranking (MOM3); otherwise wait for one of these signals. */
    data class Variant(val name: String, val entryOn: Set<IndicatorSignal>?)

    private val topN = 10
    private val lookback = 126
    private val slip = 0.0015

    @Test
    fun generateReport() {
        assumeTrue(System.getenv("MOMSIG") == "1" && RuleStudy.dataDir.isDirectory)
        val all = RuleStudy.loadAll()
        val tdex = all.getValue("TDEX")
        val universe = all - "TDEX"
        // App signal per symbol and session (flat position), computed once.
        val signals: Map<String, Map<String, IndicatorSignal>> = universe.mapValues { (s, bars) ->
            (260 until bars.size).associate { i -> bars[i].date to AppSignalRule.signal(bars, i, null, s in RuleStudy.frozenSet50, 260).type }
        }
        val variants = listOf(
            Variant("S0 MOM3 baseline (buy at next close)", null),
            Variant("S1 MOM3 + app BUY or POTENTIAL to enter", setOf(IndicatorSignal.BUY, IndicatorSignal.POTENTIAL)),
            Variant("S2 MOM3 + app BUY to enter", setOf(IndicatorSignal.BUY))
        )
        val periods = listOf("Full 2015-2025" to ("2015-01-01" to "2025-12-31"), "2015-2020" to ("2015-01-01" to "2020-12-31"), "2021-2025" to ("2021-01-01" to "2025-12-31"))
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)
        fun f0(v: Double) = String.format(Locale.ENGLISH, "%,.0f", v)
        val sb = StringBuilder("# 6-Month Momentum With App Signal Timing\n\n")
        sb.appendLine("Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: ${universe.size} SET50 stocks as of H1 2025, dividend-adjusted. " +
            "Top 10 by 126-session return, re-ranked at each month end; ฿1M, 10% target per stock, 100-share lots, InnovestX fees (ATS), 0.15% slippage per side. Benchmark: TDEX buy-and-hold.\n")
        sb.appendLine("| Variant | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | Closed positions | Win % | Avg net return / position % | Avg invested % |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|")
        val gate = StringBuilder("\n## Concentration and evidence gate (full period)\n\n| Variant | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |\n|---|---|---|---|---|---|\n")
        for (v in variants) {
            val gp = mutableListOf<EvidenceGate.PeriodResult>()
            var full: Sim? = null
            for ((label, range) in periods) {
                val r = simulate(universe, tdex, signals, v, range.first, range.second)
                val b = PortfolioBacktest.buyAndHold(tdex, PortfolioBacktestConfig(startDate = range.first, endDate = range.second))
                val win = if (r.closed.isEmpty()) 0.0 else r.closed.count { it > 0 } * 100.0 / r.closed.size
                sb.appendLine("| ${v.name} | $label | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.cagrPercent - b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | " +
                    "${r.closed.size} | ${f(win)} | ${f(r.closed.average() * 100)} | ${f(r.investedPercent)} |")
                if (label.startsWith("Full")) full = r else gp += EvidenceGate.PeriodResult(label, r.stats.cagrPercent, b.cagrPercent)
            }
            val r = full!!
            val top = r.pnl.entries.sortedByDescending { it.value }.take(3)
            val total = r.pnl.values.sum()
            val verdict = EvidenceGate.evaluate(gp, r.pnl, r.closed.size + r.openCount, r.closed.average())
            gate.appendLine("| ${v.name} | ${f0(total)} | ${top.joinToString { "${it.key} ${f0(it.value)}" }} | ${f0(total - top.sumOf { it.value })} | " +
                "${if (verdict.passed) "PASS" else "FAIL"} | ${verdict.reasons.joinToString("; ").ifEmpty { "-" }} |")
        }
        sb.append(gate)
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- Survivorship bias: today's SET50 applied to 2015-2025 flatters momentum.")
        sb.appendLine("- The app signal is computed as for a flat position; \"Expectancy\" in the gate is the average net return per closed position.")
        File(RuleStudy.root, "tools/backtest/momentum_signal_report.md").writeText(sb.toString())
        println(sb)
    }

    data class Sim(val stats: EquityStats, val closed: List<Double>, val pnl: Map<String, Double>, val openCount: Int, val investedPercent: Double)

    private fun simulate(universe: Map<String, List<ScrapedHistoricalPrice>>, tdex: List<ScrapedHistoricalPrice>,
                         signals: Map<String, Map<String, IndicatorSignal>>, v: Variant, from: String, to: String): Sim {
        val dates = tdex.map { it.date }.filter { it in from..to }
        val idx = universe.mapValues { (_, bars) -> bars.withIndex().associate { it.value.date to it.index } }
        val monthEnds = dates.indices.filter { i -> i + 1 == dates.size || dates[i + 1].substring(0, 7) != dates[i].substring(0, 7) }.toSet()
        fun price(s: String, d: String) = idx.getValue(s)[d]?.let { universe.getValue(s)[it].close }
        data class Pos(val shares: Int, val cost: Double)
        var cash = 1_000_000.0
        val held = mutableMapOf<String, Pos>()
        val closed = mutableListOf<Double>(); val pnl = mutableMapOf<String, Double>()
        var target: Set<String> = emptySet()
        var rebalancePending = false
        var newTargetFillDay = false
        val curve = mutableListOf<Pair<String, Double>>(); var investedSum = 0.0
        for ((di, d) in dates.withIndex()) {
            if (rebalancePending) {
                for (s in held.keys.filter { it !in target }) {
                    val p = price(s, d) ?: continue
                    val pos = held.remove(s)!!
                    val gross = pos.shares * p * (1 - slip)
                    val net = gross - TechnicalAnalysis.calculateFees(gross, true, true)
                    cash += net; pnl[s] = (pnl[s] ?: 0.0) + net - pos.cost; closed += net / pos.cost - 1
                }
                rebalancePending = false
                newTargetFillDay = true
            }
            // Entries: baseline buys once, right after the ranking; signal variants buy on the close after a qualifying signal.
            val prevDay = if (di > 0) dates[di - 1] else null
            val equity = cash + held.entries.sumOf { (s, pos) -> pos.shares * (price(s, d) ?: 0.0) }
            for (s in target.filter { it !in held }) {
                val allowed = when (val on = v.entryOn) {
                    null -> newTargetFillDay
                    else -> prevDay != null && signals.getValue(s)[prevDay] in on
                }
                if (!allowed) continue
                val p = price(s, d) ?: continue
                val fill = p * (1 + slip)
                var lots = (minOf(equity / topN, cash) / (fill * 100)).toInt()
                while (lots > 0 && lots * 100 * fill + TechnicalAnalysis.calculateFees(lots * 100 * fill, false, true) > cash) lots--
                if (lots <= 0) continue
                val gross = lots * 100 * fill
                val cost = gross + TechnicalAnalysis.calculateFees(gross, false, true)
                cash -= cost; held[s] = Pos(lots * 100, cost)
            }
            newTargetFillDay = false
            if (di in monthEnds && di + 1 < dates.size) {
                target = universe.keys.mapNotNull { s ->
                    val i = idx.getValue(s)[d] ?: return@mapNotNull null
                    if (i < lookback) null else s to (universe.getValue(s)[i].close / universe.getValue(s)[i - lookback].close - 1)
                }.sortedByDescending { it.second }.take(topN).map { it.first }.toSet()
                rebalancePending = true
            }
            val value = cash + held.entries.sumOf { (s, pos) -> pos.shares * (price(s, d) ?: (pos.cost / pos.shares)) }
            investedSum += (value - cash) / value
            curve += d to value
        }
        val last = dates.last()
        held.forEach { (s, pos) -> price(s, last)?.let { pnl[s] = (pnl[s] ?: 0.0) + pos.shares * it - pos.cost } }
        return Sim(EquityStats.from(curve, 1_000_000.0), closed, pnl, held.size, investedSum * 100 / dates.size)
    }
}
