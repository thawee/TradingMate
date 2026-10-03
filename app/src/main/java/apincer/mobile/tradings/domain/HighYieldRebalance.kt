package apincer.mobile.tradings.domain

/**
 * Monthly order list for the high-dividend-yield rule as tested (F5, RuleStudy.rankedPortfolio):
 * satellite names that left the top list are sold, names still in it are kept without resizing, and
 * each new name gets up to budget / topN in whole lots, funded by sale proceeds plus any budget above
 * the current satellite value. Leftover stays in cash. Review only: nothing is executed or recorded.
 */
object HighYieldRebalance {
    /**
     * A current holding; [price] is the last price, [costPerShare] the average cost including fees.
     * [managed] holdings (the Dividend purpose) are the ones this plan budgets and may sell.
     */
    data class Holding(val symbol: String, val shares: Int, val price: Double?, val costPerShare: Double,
                       val sector: String?, val managed: Boolean = true)

    enum class Action { BUY, KEEP, SELL, UNAVAILABLE }

    data class Row(val symbol: String, val action: Action, val shares: Int, val price: Double?,
                   val baht: Double, val fees: Double, val note: String? = null)

    /** [fullListBudget]: budget at which a tenth covers one lot of the priciest listed name (null without prices). */
    data class Plan(val rows: List<Row>, val managedValue: Double, val sellProceeds: Double,
                    val buyCost: Double, val fees: Double, val leftoverCash: Double, val fullListBudget: Double? = null)

    /**
     * [ranked] in rank order; [prices] and [sectors] cover ranked names not held. Core funds are never
     * sold or bought. Unmanaged holdings are never sold or budgeted; one in the list is shown as kept. Caps are warnings only, measured at cost basis against [accountEquity], so the
     * plan keeps the tested weights.
     */
    fun plan(budget: Double, ranked: List<String>, holdings: List<Holding>, prices: Map<String, Double>,
             sectors: Map<String, String>, cashBalance: Double, accountEquity: Double,
             stockCapPercent: Double, sectorCapPercent: Double, atsEnabled: Boolean,
             topN: Int = HighYieldList.TOP_N): Plan {
        val top = ranked.map { it.uppercase() }.distinct().take(topN)
        val satellite = holdings.filter { it.shares > 0 && !CoreSatellite.isCore(it.symbol) }
        val managed = satellite.filter { it.managed }
        fun value(h: Holding) = h.shares * (h.price?.takeIf { it > 0.0 } ?: h.costPerShare)
        val managedValue = managed.sumOf { value(it) }

        val sells = mutableListOf<Row>()
        var proceeds = 0.0
        var fees = 0.0
        val sold = mutableSetOf<String>()
        for (h in managed.filter { it.symbol.uppercase() !in top }) {
            val p = h.price?.takeIf { it > 0.0 && it.isFinite() }
            if (p == null) {
                sells += Row(h.symbol, Action.SELL, h.shares, null, 0.0, 0.0, "Not in the list; no price, so proceeds are not counted")
                continue
            }
            val gross = h.shares * p
            val fee = TechnicalAnalysis.calculateFees(gross, true, atsEnabled)
            proceeds += gross - fee; fees += fee
            sold += h.symbol.uppercase()
            sells += Row(h.symbol, Action.SELL, h.shares, p, gross - fee, fee, "Not in the list: sell under the tested rule")
        }

        // Sector exposure at cost after the sales, for warnings.
        val sectorCost = holdings.filter { it.shares > 0 && it.symbol.uppercase() !in sold && it.sector != null }
            .groupBy { it.sector!! }.mapValues { (_, hs) -> hs.sumOf { it.shares * it.costPerShare } }.toMutableMap()
        val stockCap = accountEquity * stockCapPercent / 100.0
        val sectorCap = accountEquity * sectorCapPercent / 100.0

        var cash = minOf(maxOf(0.0, budget - managedValue) + proceeds, maxOf(0.0, cashBalance) + proceeds)
        val perName = budget / topN
        val held = satellite.associateBy { it.symbol.uppercase() }
        val ranks = mutableListOf<Row>()
        var buyCost = 0.0
        for (s in top) {
            val h = held[s]
            if (h != null) {
                ranks += Row(s, Action.KEEP, h.shares, h.price, value(h), 0.0,
                    if (h.managed) "Held: keep, no resizing" else "Held for another purpose; not in the budget")
                continue
            }
            val p = prices[s]?.takeIf { it > 0.0 && it.isFinite() }
            if (p == null) {
                ranks += Row(s, Action.UNAVAILABLE, 0, null, 0.0, 0.0, "No price; add it to the watchlist")
                continue
            }
            var lots = (minOf(perName, cash) / (p * 100)).toInt().coerceAtLeast(0)
            while (lots > 0 && lots * 100 * p + TechnicalAnalysis.calculateFees(lots * 100 * p, false, atsEnabled) > cash) lots--
            if (lots == 0) {
                ranks += Row(s, Action.BUY, 0, p, 0.0, 0.0, "Budget left is below one lot")
                continue
            }
            val gross = lots * 100 * p
            val fee = TechnicalAnalysis.calculateFees(gross, false, atsEnabled)
            cash -= gross + fee; buyCost += gross + fee; fees += fee
            val sector = sectors[s]
            val warnings = mutableListOf<String>()
            if (gross > stockCap + 0.01) warnings += "Above your single-stock cap"
            if (sector == null) warnings += "Sector unknown"
            else {
                val after = (sectorCost[sector] ?: 0.0) + gross
                sectorCost[sector] = after
                if (after > sectorCap + 0.01) warnings += "$sector above your sector cap"
            }
            ranks += Row(s, Action.BUY, lots * 100, p, gross + fee, fee, warnings.joinToString("; ").ifEmpty { null })
        }
        val maxLot = top.mapNotNull { s -> (held[s]?.price ?: prices[s])?.takeIf { it > 0.0 && it.isFinite() } }
            .maxOfOrNull { it * 100 + TechnicalAnalysis.calculateFees(it * 100, false, atsEnabled) }
        return Plan(ranks + sells, managedValue, proceeds, buyCost, fees, cash, maxLot?.let { it * topN })
    }
}
