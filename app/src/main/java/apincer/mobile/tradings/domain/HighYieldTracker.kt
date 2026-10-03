package apincer.mobile.tradings.domain

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Forward record of the high dividend yield list (adapted from tradermonty's signal-postmortem idea).
 * Once a month the top 10 and their prices are saved with TDEX's price; after 30, 91 and 365 days the
 * list's equal-weight total return (price plus cash dividends with an ex-date in the window) is compared
 * with TDEX's. Names are fixed when saved, so a later delisting cannot drop a loser from the record,
 * which the survivorship-biased backtest could not avoid. Names without a current price are counted.
 */
object HighYieldTracker {
    val HORIZONS = listOf(30, 91, 365)

    data class Checkpoint(val date: String, val listReturnPercent: Double, val tdexReturnPercent: Double,
                          val priced: Int, val total: Int)

    data class Snapshot(val month: String, val date: String, val prices: Map<String, Double>, val tdex: Double,
                        val checkpoints: Map<Int, Checkpoint> = emptyMap())

    data class HorizonSummary(val days: Int, val measured: Int, val avgListPercent: Double,
                              val avgTdexPercent: Double, val ahead: Int)

    fun needsSnapshot(snapshots: List<Snapshot>, today: LocalDate): Boolean =
        snapshots.none { it.month == today.toString().substring(0, 7) }

    fun snapshot(today: LocalDate, prices: Map<String, Double>, tdex: Double): Snapshot? {
        val valid = prices.filterValues { it.isFinite() && it > 0.0 }
        if (valid.isEmpty() || !tdex.isFinite() || tdex <= 0.0) return null
        return Snapshot(today.toString().substring(0, 7), today.toString(), valid, tdex)
    }

    fun dueHorizons(s: Snapshot, today: LocalDate): List<Int> {
        val age = ChronoUnit.DAYS.between(LocalDate.parse(s.date), today)
        return HORIZONS.filter { it !in s.checkpoints && age >= it }
    }

    /** Total return of [entry] to [now] in percent, adding dividends with an ex-date after [from] up to [to]. */
    private fun totalReturn(entry: Double, now: Double, dividends: List<Pair<LocalDate, Double>>,
                            from: LocalDate, to: LocalDate): Double =
        ((now + dividends.filter { it.first.isAfter(from) && !it.first.isAfter(to) }.sumOf { it.second }) / entry - 1) * 100

    /**
     * Measures [s] on [today]. [prices] and [dividends] cover the snapshot's names; a name without a positive
     * current price is excluded from the average but still counted in [Checkpoint.total]. Null if none is priced.
     */
    fun checkpoint(s: Snapshot, today: LocalDate, prices: Map<String, Double>,
                   dividends: Map<String, List<Pair<LocalDate, Double>>>, tdexNow: Double,
                   tdexDividends: List<Pair<LocalDate, Double>>): Checkpoint? {
        if (!tdexNow.isFinite() || tdexNow <= 0.0) return null
        val from = LocalDate.parse(s.date)
        val returns = s.prices.mapNotNull { (symbol, entry) ->
            prices[symbol]?.takeIf { it.isFinite() && it > 0.0 }?.let {
                totalReturn(entry, it, dividends[symbol].orEmpty(), from, today)
            }
        }
        if (returns.isEmpty()) return null
        return Checkpoint(today.toString(), returns.average(),
            totalReturn(s.tdex, tdexNow, tdexDividends, from, today), returns.size, s.prices.size)
    }

    /** Checkpoints measured more than this many days after their horizon (app not opened) are left out. */
    const val ON_TIME_DAYS = 15

    fun summary(snapshots: List<Snapshot>): List<HorizonSummary> = HORIZONS.mapNotNull { days ->
        val points = snapshots.mapNotNull { s ->
            s.checkpoints[days]?.takeIf {
                ChronoUnit.DAYS.between(LocalDate.parse(s.date), LocalDate.parse(it.date)) <= days + ON_TIME_DAYS
            }
        }
        if (points.isEmpty()) null
        else HorizonSummary(days, points.size, points.map { it.listReturnPercent }.average(),
            points.map { it.tdexReturnPercent }.average(), points.count { it.listReturnPercent > it.tdexReturnPercent })
    }

    fun toJson(snapshots: List<Snapshot>): String = JSONArray().apply {
        snapshots.forEach { s ->
            put(JSONObject().apply {
                put("month", s.month); put("date", s.date); put("tdex", s.tdex)
                put("prices", JSONObject().apply { s.prices.forEach { (k, v) -> put(k, v) } })
                put("checkpoints", JSONObject().apply {
                    s.checkpoints.forEach { (days, c) ->
                        put(days.toString(), JSONObject().apply {
                            put("date", c.date); put("list", c.listReturnPercent); put("tdex", c.tdexReturnPercent)
                            put("priced", c.priced); put("total", c.total)
                        })
                    }
                })
            })
        }
    }.toString()

    fun fromJson(json: String?): List<Snapshot> = runCatching {
        if (json.isNullOrBlank()) return emptyList()
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val prices = o.getJSONObject("prices").let { p -> p.keys().asSequence().associateWith { p.getDouble(it) } }
            val cps = o.optJSONObject("checkpoints")?.let { c ->
                c.keys().asSequence().associate { k ->
                    val x = c.getJSONObject(k)
                    k.toInt() to Checkpoint(x.getString("date"), x.getDouble("list"), x.getDouble("tdex"),
                        x.getInt("priced"), x.getInt("total"))
                }
            }.orEmpty()
            Snapshot(o.getString("month"), o.getString("date"), prices, o.getDouble("tdex"), cps)
        }
    }.getOrDefault(emptyList())
}
