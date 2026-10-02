package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Event study: what happens 1-4 weeks after the advisor's price-replayable filters fire.
 * Returns are net of round-trip fees and 0.15% slippage per side, entered at the next close.
 * Fundamentals, NVDR flow and AI ranking are not replayed. Skipped unless EVENT_STUDY=1.
 *
 *   EVENT_STUDY=1 ./gradlew testDebugUnitTest --tests '*AdvisorEventStudy*'
 *
 * Writes tools/backtest/advisor_event_study.md.
 */
class AdvisorEventStudy {
    private val root = File(System.getProperty("user.dir")).let { if (File(it, "tools").exists()) it else it.parentFile }
    private val dataDir = File(root, "tools/backtest/data")
    private val roundTripCost = 2 * (TechnicalAnalysis.THAI_FEE_RATE + 0.0015)

    private fun load(file: File) = file.readLines().drop(1).mapNotNull { line ->
        val c = line.split(",")
        if (c.size < 6) null else ScrapedHistoricalPrice(date = c[0], close = c[4].toDouble(),
            volume = c[5].toDouble().toLong(), high = c[2].toDouble(), low = c[3].toDouble())
    }

    data class Event(val date: String, val r5: Double, val r10: Double, val r20: Double, val x20: Double,
                     val outcome: String, val planNet: Double, val targetDist: Double, val rr: Double)

    @Test
    fun run() {
        assumeTrue(System.getenv("EVENT_STUDY") == "1" && dataDir.isDirectory)
        val all = dataDir.listFiles { f -> f.extension == "csv" }!!.associate { it.nameWithoutExtension to load(it) }
        val tdex = all.getValue("TDEX").associate { it.date to it.close }
        val tdexBars = all.getValue("TDEX").map { it.date to it.close }
        val variants = linkedMapOf<String, MutableList<Event>>(
            "All stock-days (baseline)" to mutableListOf(),
            "App BUY signal" to mutableListOf(),
            "BUY + weekly trend up + target above" to mutableListOf(),
            "Advisor replay (+ RS > 0)" to mutableListOf(),
            "Advisor replay + R:R >= 2 (shown plans)" to mutableListOf()
        )
        for ((symbol, bars) in all - "TDEX") {
            val isSet50 = TradingConstants.SET50_SYMBOLS.contains(symbol)
            val dated = bars.map { it.date to it.close }
            for (i in 260 until bars.size - 21) {
                val entryBar = bars[i + 1]
                val entry = entryBar.close
                fun fwd(d: Int) = bars[i + 1 + d].close / entry - 1 - roundTripCost
                val t0 = tdex[entryBar.date]; val t20 = tdex[bars[i + 21].date]
                val x20 = if (t0 != null && t20 != null) fwd(20) - (t20 / t0 - 1) else Double.NaN
                val w = bars.subList(i + 1 - 260, i + 1)
                val target = w.subList(w.size - 252, w.size).maxOf { it.high }
                val atr = TechnicalAnalysis.calculateATR(w.map { it.high }, w.map { it.low }, w.map { it.close })
                val stop = TechnicalAnalysis.calculateSuggestedStopLossPrice(entry, atr, isSet50)
                // First touch inside 20 sessions; stop checked first on a bar that spans both.
                var outcome = "time"; var exit = bars[i + 21].close
                for (d in 1..20) {
                    val b = bars[i + 1 + d]
                    if (b.low <= stop) { outcome = "stop"; exit = stop; break }
                    if (b.high >= target && target > entry) { outcome = "target"; exit = target; break }
                }
                val ev = Event(bars[i].date, fwd(5), fwd(10), fwd(20), x20, outcome, exit / entry - 1 - roundTripCost,
                    target / entry - 1, if (entry > stop) (target - entry) / (entry - stop) else Double.NaN)
                variants.getValue("All stock-days (baseline)").add(ev)

                if (AppSignalRule.signal(bars, i, null, isSet50, 260).type != IndicatorSignal.BUY) continue
                variants.getValue("App BUY signal").add(ev)
                val weekly = TechnicalAnalysis.isWeeklyTrendBullishOnDate(dated.subList(0, i + 1), bars[i].date, bars[i].close)
                if (weekly != true || target <= entry) continue
                variants.getValue("BUY + weekly trend up + target above").add(ev)
                val rs = TechnicalAnalysis.calculateRelativeStrengthOnDates(dated.subList(0, i + 1),
                    tdexBars.filter { it.first <= bars[i].date })
                if (rs == null || rs <= 0) continue
                variants.getValue("Advisor replay (+ RS > 0)").add(ev)
                if (ev.rr >= 2.0) variants.getValue("Advisor replay + R:R >= 2 (shown plans)").add(ev)
            }
        }
        fun pct(v: Double) = String.format(Locale.ENGLISH, "%+.2f%%", v * 100)
        fun med(l: List<Double>) = l.sorted().let { if (it.isEmpty()) Double.NaN else it[it.size / 2] }
        val sb = StringBuilder("| Variant | Period | Events | Mean 1w | Mean 2w | Mean 4w | Median 4w | Win 4w | Mean 4w vs TDEX | Target hit | Stop hit | Plan exit mean | Median target dist | Median R:R |\n|---|---|---|---|---|---|---|---|---|---|---|---|---|---|\n")
        for ((name, evs) in variants) for ((label, range) in listOf("2015-2025" to ("2015" to "2026"), "2015-2020" to ("2015" to "2021"), "2021-2025" to ("2021" to "2026"))) {
            val e = evs.filter { it.date >= range.first && it.date < range.second }
            if (e.isEmpty()) continue
            val x = e.map { it.x20 }.filter { !it.isNaN() }
            sb.appendLine("| $name | $label | ${e.size} | ${pct(e.map { it.r5 }.average())} | ${pct(e.map { it.r10 }.average())} | " +
                "${pct(e.map { it.r20 }.average())} | ${pct(med(e.map { it.r20 }))} | " +
                "${String.format(Locale.ENGLISH, "%.1f%%", e.count { it.r20 > 0 } * 100.0 / e.size)} | ${pct(x.average())} | " +
                "${String.format(Locale.ENGLISH, "%.1f%%", e.count { it.outcome == "target" } * 100.0 / e.size)} | " +
                "${String.format(Locale.ENGLISH, "%.1f%%", e.count { it.outcome == "stop" } * 100.0 / e.size)} | " +
                "${pct(e.map { it.planNet }.average())} | ${pct(med(e.map { it.targetDist }))} | " +
                "${String.format(Locale.ENGLISH, "%.2f", med(e.map { it.rr }.filter { !it.isNaN() }))} |")
        }
        val header = "# Advisor Event Study (1-4 weeks)\n\nUniverse: current SET50 (survivorship-biased), 2015-2025. Entry at next close; returns net of " +
            "InnovestX fees and 0.15% slippage per side. Target = 52-week high, stop = suggested ATR stop; first touch within " +
            "20 sessions, stop assumed first when one bar spans both. Not replayed: fundamentals, NVDR flow, AI ranking.\n\n"
        File(root, "tools/backtest/advisor_event_study.md").writeText(header + sb)
        println(sb)
    }
}
