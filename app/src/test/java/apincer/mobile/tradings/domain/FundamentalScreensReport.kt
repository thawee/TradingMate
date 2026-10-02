package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Quality and dividend screens on 2015-2025 history using thaifin quarterly fundamentals, fixed
 * before the first run (tasks/todo.md, 2026-10-02). Skipped unless FUNDAMENTALS=1.
 *
 *   .venv/bin/python tools/backtest/fetch_fundamentals.py
 *   FUNDAMENTALS=1 ./gradlew testDebugUnitTest --tests '*FundamentalScreensReport*'
 *
 * Writes tools/backtest/fundamental_screens_report.md.
 */
class FundamentalScreensReport {
    data class Quarter(val end: LocalDate, val available: LocalDate, val netProfit: Double?, val revenue: Double?,
                       val equity: Double?, val de: Double?, val yield: Double?)

    /** Point-in-time view: figures known on [date], or null when four usable quarters are not yet published. */
    data class Snapshot(val roe: Double, val margin: Double, val de: Double?, val yield: Double?, val financial: Boolean)

    private val fundDir = File(RuleStudy.root, "tools/backtest/fundamentals")

    private fun load(file: File): Pair<List<Quarter>, Boolean> {
        val lines = file.readLines()
        val financial = lines.drop(1).firstOrNull()?.substringAfterLast(",")?.startsWith("Financials") == true
        val qs = lines.drop(1).mapNotNull { line ->
            val c = line.split(",")
            val m = Regex("(\\d{4})Q(\\d)").find(c[0]) ?: return@mapNotNull null
            val year = m.groupValues[1].toInt(); val q = m.groupValues[2].toInt()
            val end = LocalDate.of(year, q * 3, 1).plusMonths(1).minusDays(1)
            fun d(k: Int) = c.getOrNull(k)?.toDoubleOrNull()
            Quarter(end, end.plusDays(if (q == 4) 90 else 50), d(1), d(2), d(3), d(6), d(7))
        }.sortedBy { it.end }
        return qs to financial
    }

    private fun snapshot(qs: List<Quarter>, financial: Boolean, date: LocalDate): Snapshot? {
        val known = qs.filter { !it.available.isAfter(date) && it.netProfit != null }
        if (known.size < 4) return null
        val last4 = known.takeLast(4)
        val latest = last4.last()
        val equity = latest.equity ?: return null
        val np = last4.sumOf { it.netProfit ?: return null }
        val rev = last4.sumOf { it.revenue ?: return null }
        if (equity <= 0.0 || rev <= 0.0) return null
        return Snapshot(np / equity * 100, np / rev * 100, latest.de, latest.yield, financial)
    }

    @Test
    fun generateReport() {
        assumeTrue(System.getenv("FUNDAMENTALS") == "1" && fundDir.isDirectory && RuleStudy.dataDir.isDirectory)
        val all = RuleStudy.loadAll()
        val tdex = all.getValue("TDEX")
        val fund = fundDir.listFiles { f -> f.extension == "csv" }!!.associate { it.nameWithoutExtension to load(it) }
        val universe = (all - "TDEX").filterKeys { it in fund }
        val tdexIdx = tdex.withIndex().associate { it.value.date to it.index }
        fun snap(s: String, bars: List<ScrapedHistoricalPrice>, i: Int): Snapshot? =
            fund[s]?.let { (qs, fin) -> snapshot(qs, fin, LocalDate.parse(bars[i].date)) }
        fun qualityOld(x: Snapshot) = x.roe > 15 && x.margin > 10 && (x.de?.let { it < 1.5 } ?: true)
        fun qualitySet(x: Snapshot) = x.roe > 10 && x.margin > 10 && (x.financial || (x.de?.let { it < 1.5 } ?: true))
        fun beta(bars: List<ScrapedHistoricalPrice>, i: Int): Double? {
            if (i <= 252) return null
            val pairs = (i - 251..i).mapNotNull { k ->
                val t = tdexIdx[bars[k].date]; val tp = tdexIdx[bars[k - 1].date]
                if (t == null || tp == null) null else (bars[k].close / bars[k - 1].close - 1) to (tdex[t].close / tdex[tp].close - 1)
            }
            if (pairs.size < 200) return null
            val ms = pairs.map { it.first }.average(); val mt = pairs.map { it.second }.average()
            val varT = pairs.sumOf { (it.second - mt) * (it.second - mt) }
            return if (varT <= 0.0) null else pairs.sumOf { (it.first - ms) * (it.second - mt) } / varT
        }
        val variants: List<Pair<String, (String, List<ScrapedHistoricalPrice>, Int) -> Double?>> = listOf(
            "F1 quality, old rule (ROE > 15), by ROE" to { s, b, i -> snap(s, b, i)?.takeIf { qualityOld(it) }?.roe },
            "F2 quality, SET rule (ROE > 10, banks exempt from D/E), by ROE" to { s, b, i -> snap(s, b, i)?.takeIf { qualitySet(it) }?.roe },
            "F3 Dividend Stars (yield >= 5% + F2 quality), by yield" to { s, b, i -> snap(s, b, i)?.takeIf { qualitySet(it) && (it.yield ?: 0.0) >= 5.0 }?.yield },
            "F4 F2 quality, by lowest beta" to { s, b, i -> if (snap(s, b, i)?.let { qualitySet(it) } == true) beta(b, i)?.let { -it } else null },
            "F5 highest dividend yield, no quality filter" to { s, b, i -> snap(s, b, i)?.yield?.takeIf { it > 0.0 } }
        )
        val periods = listOf("Full 2015-2025" to ("2015-01-01" to "2025-12-31"), "2015-2020" to ("2015-01-01" to "2020-12-31"), "2021-2025" to ("2021-01-01" to "2025-12-31"))
        fun f(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)
        fun f0(v: Double) = String.format(Locale.ENGLISH, "%,.0f", v)
        val sb = StringBuilder("# Fundamental Screens on History\n\n")
        sb.appendLine("Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: ${universe.size} SET50 stocks as of H1 2025 with thaifin quarterly fundamentals (Finnomena public API). " +
            "A quarter is used from 50 days after a Q1-Q3 end and 90 days after Q4; ROE and margin are trailing four quarters. Monthly re-rank, top 10 at a 10% target (empty slots in cash), ฿1M, InnovestX fees (ATS), 0.15% slippage per side.\n")
        sb.appendLine("| Variant | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Closed positions | Win % | Avg net return / position % | Avg days held | Turnover / yr |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|")
        val gate = StringBuilder("\n## Concentration and evidence gate (full period)\n\n| Variant | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |\n|---|---|---|---|---|---|\n")
        for ((name, score) in variants) {
            val gp = mutableListOf<EvidenceGate.PeriodResult>()
            var full: RuleStudy.Ranked? = null
            for ((label, range) in periods) {
                val r = RuleStudy.rankedPortfolio(universe, tdex, range.first, range.second, score = score)
                val b = PortfolioBacktest.buyAndHold(tdex, PortfolioBacktestConfig(startDate = range.first, endDate = range.second))
                val win = if (r.positions.isEmpty()) 0.0 else r.positions.count { it > 0 } * 100.0 / r.positions.size
                sb.appendLine("| $name | $label | ${f(r.stats.cagrPercent)} | ${f(b.cagrPercent)} | ${f(r.stats.cagrPercent - b.cagrPercent)} | ${f(r.stats.maxDrawdownPercent)} | ${f(b.maxDrawdownPercent)} | " +
                    "${r.positions.size} | ${f(win)} | ${f((r.positions.takeIf { it.isNotEmpty() }?.average() ?: 0.0) * 100)} | ${f(r.avgHeld)} | ${f(r.turnoverPerYear)} |")
                if (label.startsWith("Full")) full = r else gp += EvidenceGate.PeriodResult(label, r.stats.cagrPercent, b.cagrPercent)
            }
            val r = full!!
            val top = r.pnlBySymbol.entries.sortedByDescending { it.value }.take(3)
            val total = r.pnlBySymbol.values.sum()
            val v = EvidenceGate.evaluate(gp, r.pnlBySymbol, r.trades, r.positions.takeIf { it.isNotEmpty() }?.average() ?: 0.0)
            gate.appendLine("| $name | ${f0(total)} | ${top.joinToString { "${it.key} ${f0(it.value)}" }} | ${f0(total - top.sumOf { it.value })} | " +
                "${if (v.passed) "PASS" else "FAIL"} | ${v.reasons.joinToString("; ").ifEmpty { "-" }} |")
        }
        sb.append(gate)
        sb.appendLine("\n## Caveats\n")
        sb.appendLine("- Survivorship bias: today's SET50 applied to 2015-2025; companies that shrank or failed are missing.")
        sb.appendLine("- Fundamentals come from a third-party aggregator and may include restatements made after the original release.")
        sb.appendLine("- Publication lags are conservative approximations of SET filing deadlines, not actual release dates.")
        File(RuleStudy.root, "tools/backtest/fundamental_screens_report.md").writeText(sb.toString())
        println(sb)
    }
}
