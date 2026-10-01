package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.AdviceEventEntity
import kotlin.math.pow

/**
 * Did stock picking beat the core? Replays the satellite's actual cash flows (journaled
 * fills and dividends) into a shadow TDEX position and compares end values and
 * money-weighted returns over the same window.
 */
object SatelliteScorecard {
    private const val DAY_MS = 86_400_000L
    private const val YEAR_DAYS = 365.25
    /** Annualised returns over shorter spans are noise, so comparisons wait this long. */
    const val MIN_HISTORY_DAYS = 30

    enum class Kind { BUY, SELL, UNDO_SELL }

    /** [cashFlow] is from the investor's view: negative = money into the satellite. */
    data class Fill(val symbol: String, val timeMillis: Long, val kind: Kind, val quantity: Int, val cashFlow: Double) {
        val signedQuantity: Int get() = if (kind == Kind.SELL) -quantity else quantity
    }

    data class Dividend(val symbol: String, val timeMillis: Long, val amount: Double)

    data class Coverage(val included: Set<String>, val excluded: Set<String>)

    data class Comparison(
        val fromMillis: Long,
        val toMillis: Long,
        val satelliteEndValue: Double,
        val shadowEndValue: Double,
        val satelliteXirrPercent: Double?,
        val shadowXirrPercent: Double?
    ) {
        /** Positive when the satellite ended with more than the same cash flows in TDEX. */
        val excessBaht: Double get() = satelliteEndValue - shadowEndValue
    }

    data class Report(
        val coverage: Coverage,
        /** First usable satellite fill; comparisons start [MIN_HISTORY_DAYS] after it. */
        val firstFillMillis: Long?,
        val sinceStart: Comparison?,
        val trailing12m: Comparison?,
        val trailing12mPriorQuarter: Comparison?
    ) {
        /** Both trailing windows show the satellite behind the core. */
        val suggestReducingSatellite: Boolean get() =
            (trailing12m?.excessBaht ?: 0.0) < 0.0 && (trailing12mPriorQuarter?.excessBaht ?: 0.0) < 0.0
    }

    /**
     * Satellite fills from the advice journal. SELL_FILL stores buy+sell fees combined,
     * so sell proceeds use the fee engine's sell-side estimate.
     */
    fun fillsFromEvents(events: List<AdviceEventEntity>, atsEnabled: Boolean = true): List<Fill> =
        events.filter { !CoreSatellite.isCore(it.symbol) && it.quantity > 0 && (it.fillPrice ?: 0.0) > 0.0 }
            .mapNotNull { e ->
                val gross = e.quantity * e.fillPrice!!
                val symbol = e.symbol.uppercase()
                when (e.kind) {
                    "BUY_FILL" -> Fill(symbol, e.timeMillis, Kind.BUY, e.quantity, -(gross + e.fees))
                    "SELL_FILL" -> Fill(symbol, e.timeMillis, Kind.SELL, e.quantity,
                        gross - TechnicalAnalysis.calculateFees(gross, isSelling = true, atsEnabled = atsEnabled))
                    "UNDO_SELL" -> Fill(symbol, e.timeMillis, Kind.UNDO_SELL, e.quantity, -(gross - e.fees))
                    else -> null
                }
            }
            .sortedBy { it.timeMillis }

    /** A symbol is usable only if its journaled quantity never goes negative and matches today's holding. */
    fun coverage(fills: List<Fill>, currentQuantities: Map<String, Int>): Coverage {
        val symbols = fills.map { it.symbol }.toSet() +
            currentQuantities.filter { it.value > 0 && !CoreSatellite.isCore(it.key) }.keys.map { it.uppercase() }
        val (included, excluded) = symbols.partition { symbol ->
            var running = 0
            var consistent = true
            fills.filter { it.symbol == symbol }.forEach {
                running += it.signedQuantity
                if (running < 0) consistent = false
            }
            consistent && running == (currentQuantities[symbol] ?: 0)
        }
        return Coverage(included.toSet(), excluded.toSet())
    }

    private fun quantitiesAt(fills: List<Fill>, timeMillis: Long): Map<String, Int> =
        fills.filter { it.timeMillis <= timeMillis }.groupBy { it.symbol }
            .mapValues { (_, fs) -> fs.sumOf { it.signedQuantity } }.filterValues { it > 0 }

    /**
     * Compares the satellite with a shadow TDEX position fed the same cash flows over
     * ([fromMillis], [toMillis]]. With [fromMillis] null the window starts at the first fill.
     * [priceAt] values a satellite symbol at a time; [corePriceAt] must be dividend-adjusted
     * TDEX. Returns null when a needed price is missing.
     */
    fun compare(
        fills: List<Fill>,
        dividends: List<Dividend>,
        fromMillis: Long?,
        toMillis: Long,
        priceAt: (symbol: String, timeMillis: Long) -> Double?,
        corePriceAt: (timeMillis: Long) -> Double?,
        coreFeeRate: Double = TechnicalAnalysis.THAI_FEE_RATE
    ): Comparison? {
        val start = fromMillis ?: fills.firstOrNull()?.timeMillis ?: return null
        if (start >= toMillis) return null

        fun valueAt(time: Long): Double? = quantitiesAt(fills, time).entries.sumOf { (symbol, qty) ->
            qty * (priceAt(symbol, time) ?: return null)
        }

        val startValue = if (fromMillis != null) valueAt(start) ?: return null else 0.0
        // A fixed window's start value already includes fills at `start`; since-start includes the first fill.
        val inWindow: (Long) -> Boolean =
            if (fromMillis == null) { t -> t in start..toMillis } else { t -> t > start && t <= toMillis }
        val flows = mutableListOf<Pair<Long, Double>>()
        if (startValue > 0.0) flows += start to -startValue
        fills.filter { inWindow(it.timeMillis) }.forEach { flows += it.timeMillis to it.cashFlow }
        dividends.filter { inWindow(it.timeMillis) }.forEach { flows += it.timeMillis to it.amount }
        flows.sortBy { it.first }
        if (flows.none { it.second < 0.0 }) return null

        // Shadow: an already-invested start value moves into TDEX fee-free; later flows pay TDEX fees.
        var units = if (startValue > 0.0) startValue / (corePriceAt(start) ?: return null) else 0.0
        for ((time, amount) in flows.drop(if (startValue > 0.0) 1 else 0)) {
            val price = corePriceAt(time) ?: return null
            units += if (amount < 0.0) -amount / (price * (1 + coreFeeRate)) else -amount / (price * (1 - coreFeeRate))
        }
        val satelliteEnd = valueAt(toMillis) ?: return null
        val shadowEnd = units * (corePriceAt(toMillis) ?: return null)

        return Comparison(
            fromMillis = start,
            toMillis = toMillis,
            satelliteEndValue = satelliteEnd,
            shadowEndValue = shadowEnd,
            satelliteXirrPercent = xirr(flows + (toMillis to satelliteEnd))?.times(100),
            shadowXirrPercent = xirr(flows + (toMillis to shadowEnd))?.times(100)
        )
    }

    fun report(
        fills: List<Fill>,
        dividends: List<Dividend>,
        currentQuantities: Map<String, Int>,
        nowMillis: Long,
        priceAt: (String, Long) -> Double?,
        corePriceAt: (Long) -> Double?
    ): Report {
        val coverage = coverage(fills, currentQuantities)
        val usable = fills.filter { it.symbol in coverage.included }
        val usableDividends = dividends.filter { it.symbol.uppercase() in coverage.included }
        val year = (YEAR_DAYS * DAY_MS).toLong()
        val quarter = year / 4
        val firstFill = usable.firstOrNull()?.timeMillis
        val hasEnoughHistory = firstFill != null && nowMillis - firstFill >= MIN_HISTORY_DAYS * DAY_MS
        return Report(
            coverage = coverage,
            firstFillMillis = firstFill,
            sinceStart = if (hasEnoughHistory) compare(usable, usableDividends, null, nowMillis, priceAt, corePriceAt) else null,
            // Trailing windows need satellite history covering the whole window.
            trailing12m = if (firstFill != null && firstFill <= nowMillis - year)
                compare(usable, usableDividends, nowMillis - year, nowMillis, priceAt, corePriceAt) else null,
            trailing12mPriorQuarter = if (firstFill != null && firstFill <= nowMillis - year - quarter)
                compare(usable, usableDividends, nowMillis - year - quarter, nowMillis - quarter, priceAt, corePriceAt) else null
        )
    }

    /** Annualised money-weighted return (decimal) by bisection; null when it has no root. */
    fun xirr(flows: List<Pair<Long, Double>>): Double? {
        if (flows.none { it.second < 0 } || flows.none { it.second > 0 }) return null
        val t0 = flows.minOf { it.first }
        fun npv(rate: Double) = flows.sumOf { (t, cf) -> cf / (1 + rate).pow((t - t0).toDouble() / DAY_MS / YEAR_DAYS) }
        var lo = -0.9999
        var hi = 100.0
        var fLo = npv(lo)
        if (fLo * npv(hi) > 0) return null
        repeat(200) {
            val mid = (lo + hi) / 2
            val fMid = npv(mid)
            if (fLo * fMid <= 0) hi = mid else { lo = mid; fLo = fMid }
        }
        return (lo + hi) / 2
    }
}
