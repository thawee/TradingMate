package apincer.mobile.tradings.domain

/**
 * Core-satellite allocation. The core is TDEX (SET50 ETF), the benchmark the market-wide
 * backtest could not beat; every other holding is satellite. Percentages are of invested
 * market value, so the regime cash buffer does not distort the split.
 */
object CoreSatellite {
    const val CORE_SYMBOL = "TDEX"
    const val DEFAULT_TARGET_CORE_PERCENT = 80.0
    const val DEFAULT_DCA_DAY = 1

    fun isCore(symbol: String): Boolean = symbol.equals(CORE_SYMBOL, ignoreCase = true)

    /** Shown instead of technical signals on the core, which the backtested signals trail. */
    val CORE_SIGNAL = TradeSignal(IndicatorSignal.NEUTRAL, "Core holding",
        "Index core: buy on your DCA schedule and hold through technical signals.")

    data class Allocation(
        val coreValue: Double,
        val satelliteValue: Double,
        val targetCorePercent: Double
    ) {
        val investedValue: Double get() = coreValue + satelliteValue
        val corePercent: Double get() = if (investedValue > 0) coreValue / investedValue * 100.0 else 0.0
        val satellitePercent: Double get() = if (investedValue > 0) 100.0 - corePercent else 0.0
        /** Positive when core is above target, negative when below. */
        val driftPercent: Double get() = corePercent - targetCorePercent
        /** Core purchase needed to reach target without selling satellite. */
        val coreShortfallBaht: Double get() {
            val target = targetCorePercent / 100.0
            if (target >= 1.0) return 0.0
            return maxOf(0.0, (target * satelliteValue / (1.0 - target)) - coreValue)
        }
    }

    /** [holdings] are (symbol, market value) pairs. */
    fun allocation(holdings: List<Pair<String, Double>>, targetCorePercent: Double): Allocation =
        Allocation(
            coreValue = holdings.filter { isCore(it.first) }.sumOf { it.second },
            satelliteValue = holdings.filterNot { isCore(it.first) }.sumOf { it.second },
            targetCorePercent = targetCorePercent
        )

    /** Allocation if [buyValue] of [symbol] were added to [holdings]. */
    fun allocationAfterBuy(holdings: List<Pair<String, Double>>, symbol: String, buyValue: Double, targetCorePercent: Double): Allocation =
        allocation(holdings + (symbol to maxOf(buyValue, 0.0)), targetCorePercent)

    /** True when a non-core buy would push the satellite above its cap (100% − target core). */
    fun breachesSatelliteCap(holdings: List<Pair<String, Double>>, symbol: String, buyValue: Double, targetCorePercent: Double): Boolean =
        !isCore(symbol) && buyValue > 0.0 &&
            allocationAfterBuy(holdings, symbol, buyValue, targetCorePercent).satellitePercent > 100.0 - targetCorePercent

    /** Below-target drift that earns a reminder; above target is fine (never nudge toward stock picking). */
    const val REBALANCE_DRIFT_PERCENT = 5.0

    /**
     * Reminder text when the core is more than [REBALANCE_DRIFT_PERCENT] points under target and
     * there is a satellite to balance against; null otherwise. Suggests new money, never selling.
     */
    fun rebalanceMessage(allocation: Allocation): String? {
        if (allocation.satelliteValue <= 0.0 || allocation.driftPercent >= -REBALANCE_DRIFT_PERCENT) return null
        return String.format(
            java.util.Locale.ENGLISH,
            "Core %s is %.0f%% of invested money vs your %.0f%% target. About ฿%,.0f more %s gets you back on target; put new money into the core before buying more stocks.",
            CORE_SYMBOL, allocation.corePercent, allocation.targetCorePercent, allocation.coreShortfallBaht, CORE_SYMBOL
        )
    }

    data class DcaSuggestion(val shares: Int, val estimatedCost: Double, val unusedBaht: Double)

    /** Largest whole 100-share board lot of [price] whose cost including buy fees fits [budget]. */
    fun dcaSuggestion(budget: Double, price: Double, atsEnabled: Boolean = true): DcaSuggestion {
        if (budget <= 0.0 || price <= 0.0) return DcaSuggestion(0, 0.0, maxOf(budget, 0.0))
        fun cost(shares: Int) = shares * price + TechnicalAnalysis.calculateFees(shares * price, isSelling = false, atsEnabled = atsEnabled)
        var lots = (budget / (price * 100)).toInt()
        while (lots > 0 && cost(lots * 100) > budget) lots--
        val shares = lots * 100
        val spent = if (shares > 0) cost(shares) else 0.0
        return DcaSuggestion(shares, spent, budget - spent)
    }
}
