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

    fun loadAll(dir: File = dataDir): Map<String, List<ScrapedHistoricalPrice>> =
        dir.listFiles { f -> f.extension == "csv" }!!.associate { f ->
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

    data class Ranked(val stats: EquityStats, val positions: List<Double>, val pnlBySymbol: Map<String, Double>,
                      val trades: Int, val turnoverPerYear: Double, val avgHeld: Double,
                      val resizeOrders: Int = 0, val maxWeightPercent: Double = 0.0)

    /**
     * Rule for names kept across a rebalance (averaging-down study, tasks/todo.md 2026-10-03). A name is
     * "down" when its month-end close is at least [downPercent] below its average cost per share including
     * fees. [topUpTo] buys a down name back up to that fraction of equity, at most [maxAdds] times per
     * position; [resizeAll] resizes every kept name to 1/topN of equity; [sellDown] sells down names even
     * if still ranked (not re-bought in the same rebalance).
     */
    data class HeldRule(val downPercent: Double = 15.0, val topUpTo: Double? = null, val maxAdds: Int = 1,
                        val resizeAll: Boolean = false, val sellDown: Boolean = false)

    /**
     * Monthly ranked portfolio: at each month's last session, rank symbols by [score] (higher first,
     * null = not eligible), hold the top [topN] at 1/[topN] target each from the next close. Names that
     * stay are kept without resizing unless [heldRule] says otherwise; names that drop out are sold.
     * Fees (ATS) and slippage per side.
     */
    fun rankedPortfolio(universe: Map<String, List<ScrapedHistoricalPrice>>, tdex: List<ScrapedHistoricalPrice>,
                        from: String, to: String, topN: Int = 10, slip: Double = 0.0015,
                        rebalanceMonths: Set<Int>? = null, heldRule: HeldRule? = null,
                        score: (String, List<ScrapedHistoricalPrice>, Int) -> Double?): Ranked {
        val dates = tdex.map { it.date }.filter { it in from..to }
        val idx = universe.mapValues { (_, bars) -> bars.withIndex().associate { it.value.date to it.index } }
        val monthEnds = dates.indices.filter { i -> i + 1 == dates.size || dates[i + 1].substring(0, 7) != dates[i].substring(0, 7) }.toSet()
        var cash = 1_000_000.0
        data class Pos(val shares: Int, val cost: Double, val openDate: String, val adds: Int = 0)
        val held = mutableMapOf<String, Pos>()
        val closedReturns = mutableListOf<Double>(); val pnl = mutableMapOf<String, Double>(); val heldDays = mutableListOf<Double>()
        var tradedValue = 0.0
        var resizeOrders = 0
        var maxWeight = 0.0
        var pending: List<String>? = null
        var pendingDown: Set<String> = emptySet()
        val curve = mutableListOf<Pair<String, Double>>()
        fun price(s: String, d: String) = idx.getValue(s)[d]?.let { universe.getValue(s)[it].close }
        fun sell(s: String, shares: Int, p: Double, d: String) {
            val pos = held.getValue(s)
            val gross = shares * p * (1 - slip)
            val net = gross - TechnicalAnalysis.calculateFees(gross, true, true)
            cash += net; tradedValue += gross
            if (shares == pos.shares) {
                held.remove(s)
                pnl[s] = (pnl[s] ?: 0.0) + net - pos.cost
                closedReturns += net / pos.cost - 1
                heldDays += java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(pos.openDate), java.time.LocalDate.parse(d)).toDouble()
            } else {
                val costPart = pos.cost * shares / pos.shares
                pnl[s] = (pnl[s] ?: 0.0) + net - costPart
                held[s] = pos.copy(shares = pos.shares - shares, cost = pos.cost - costPart)
            }
        }
        /** Whole lots worth up to [budget] at [fill] whose cost with fees fits in cash. */
        fun lotsFor(budget: Double, fill: Double): Int {
            var lots = (budget / (fill * 100)).toInt()
            while (lots > 0 && lots * 100 * fill + TechnicalAnalysis.calculateFees(lots * 100 * fill, false, true) > cash) lots--
            return lots
        }
        for ((di, d) in dates.withIndex()) {
            pending?.let { target ->
                // Sell names that left the list, then buy new names at up to 10% of equity each.
                for (s in held.keys.filter { it !in target }) {
                    val p = price(s, d) ?: continue
                    sell(s, held.getValue(s).shares, p, d)
                }
                val stopped = mutableSetOf<String>()
                if (heldRule?.sellDown == true) for (s in held.keys.filter { it in pendingDown }) {
                    val p = price(s, d) ?: continue
                    sell(s, held.getValue(s).shares, p, d); stopped += s
                }
                if (heldRule?.resizeAll == true) {
                    val eq = cash + held.entries.sumOf { (s, pos) -> pos.shares * (price(s, d) ?: 0.0) }
                    for (s in held.keys.toList()) {
                        val p = price(s, d) ?: continue
                        val excess = ((held.getValue(s).shares * p - eq / topN) / (p * 100)).toInt()
                        if (excess > 0) { sell(s, excess * 100, p, d); resizeOrders++ }
                    }
                }
                val kept = held.keys.toSet()
                val equity = cash + held.entries.sumOf { (s, pos) -> pos.shares * (price(s, d) ?: 0.0) }
                for (s in target.filter { it !in held && it !in stopped }) {
                    val p = price(s, d) ?: continue
                    val fill = p * (1 + slip)
                    val lots = lotsFor(minOf(equity / topN, cash), fill)
                    if (lots <= 0) continue
                    val gross = lots * 100 * fill
                    val cost = gross + TechnicalAnalysis.calculateFees(gross, false, true)
                    cash -= cost; tradedValue += gross
                    held[s] = Pos(lots * 100, cost, d)
                }
                // Top up kept names (after new names, which keep F5's budget) from the remaining cash.
                if (heldRule != null) for (s in kept) {
                    val pos = held[s] ?: continue
                    val goal = when {
                        heldRule.resizeAll -> equity / topN
                        heldRule.topUpTo != null && s in pendingDown && pos.adds < heldRule.maxAdds -> equity * heldRule.topUpTo
                        else -> continue
                    }
                    val p = price(s, d) ?: continue
                    val fill = p * (1 + slip)
                    val lots = lotsFor(minOf(goal - pos.shares * p, cash), fill)
                    if (lots <= 0) continue
                    val gross = lots * 100 * fill
                    val cost = gross + TechnicalAnalysis.calculateFees(gross, false, true)
                    cash -= cost; tradedValue += gross; resizeOrders++
                    held[s] = pos.copy(shares = pos.shares + lots * 100, cost = pos.cost + cost,
                        adds = if (heldRule.resizeAll) pos.adds else pos.adds + 1)
                }
                pending = null
                pendingDown = emptySet()
            }
            if (di in monthEnds && di + 1 < dates.size &&
                (rebalanceMonths == null || d.substring(5, 7).toInt() in rebalanceMonths)) {
                val scores = universe.keys.mapNotNull { s ->
                    val i = idx.getValue(s)[d] ?: return@mapNotNull null
                    score(s, universe.getValue(s), i)?.let { s to it }
                }
                pending = scores.sortedByDescending { it.second }.take(topN).map { it.first }
                if (heldRule != null) pendingDown = held.filter { (s, pos) ->
                    price(s, d)?.let { it <= pos.cost / pos.shares * (1 - heldRule.downPercent / 100) } == true
                }.keys
            }
            val values = held.entries.map { (s, pos) -> pos.shares * (price(s, d) ?: (pos.cost / pos.shares)) }
            val total = cash + values.sum()
            curve += d to total
            values.maxOrNull()?.let { maxWeight = maxOf(maxWeight, it / total * 100) }
        }
        // Mark open positions at the last close for per-symbol P/L.
        val last = dates.last()
        held.forEach { (s, pos) ->
            val p = price(s, last) ?: return@forEach
            pnl[s] = (pnl[s] ?: 0.0) + pos.shares * p - pos.cost
        }
        val years = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(dates.first()), java.time.LocalDate.parse(last)) / 365.25
        val stats = EquityStats.from(curve, 1_000_000.0)
        val avgEquity = curve.map { it.second }.average()
        return Ranked(stats, closedReturns, pnl, closedReturns.size + held.size,
            tradedValue / avgEquity / years, heldDays.takeIf { it.isNotEmpty() }?.average() ?: 0.0,
            resizeOrders, maxWeight)
    }
}
