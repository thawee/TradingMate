package apincer.mobile.tradings.domain

/**
 * Minimum evidence before a signal rule may drive BUY/SELL wording or alerts.
 * Criteria are documented in docs/ADVISOR_EVALUATION.md and fixed before testing a rule.
 */
object EvidenceGate {
    const val MIN_TRADES = 100
    const val TOP_SYMBOLS_REMOVED = 3
    /** Share of total closed P/L that must remain after removing the top symbols. */
    const val MIN_SHARE_WITHOUT_TOP = 0.5

    data class PeriodResult(val name: String, val strategyCagrPercent: Double, val benchmarkCagrPercent: Double)

    data class Verdict(val passed: Boolean, val reasons: List<String>)

    /**
     * @param periods each sub-period (e.g. 2015-2020, 2021-2025) with strategy and benchmark CAGR.
     * @param pnlBySymbol closed-trade P/L per symbol over the full period.
     * @param expectancyR average R per closed trade over the full period.
     */
    fun evaluate(
        periods: List<PeriodResult>,
        pnlBySymbol: Map<String, Double>,
        totalTrades: Int,
        expectancyR: Double
    ): Verdict {
        val failures = mutableListOf<String>()
        periods.filter { it.strategyCagrPercent <= it.benchmarkCagrPercent }.forEach {
            failures += "Trails benchmark in ${it.name} (${fmt(it.strategyCagrPercent)}% vs ${fmt(it.benchmarkCagrPercent)}%)"
        }
        if (periods.isEmpty()) failures += "No sub-period results"
        val total = pnlBySymbol.values.sum()
        val withoutTop = pnlBySymbol.values.sortedDescending().drop(TOP_SYMBOLS_REMOVED).sum()
        if (withoutTop <= 0.0 || withoutTop < MIN_SHARE_WITHOUT_TOP * total) {
            failures += "Without its top $TOP_SYMBOLS_REMOVED symbols, P/L is ฿${String.format(java.util.Locale.ENGLISH, "%,.0f", withoutTop)} " +
                "of ฿${String.format(java.util.Locale.ENGLISH, "%,.0f", total)} (need positive and ≥ ${(MIN_SHARE_WITHOUT_TOP * 100).toInt()}%)"
        }
        if (totalTrades < MIN_TRADES) failures += "Only $totalTrades trades (need $MIN_TRADES)"
        if (expectancyR <= 0.0) failures += "Expectancy ${fmt(expectancyR)}R is not positive"
        return Verdict(failures.isEmpty(), failures)
    }

    private fun fmt(v: Double) = String.format(java.util.Locale.ENGLISH, "%.2f", v)
}
