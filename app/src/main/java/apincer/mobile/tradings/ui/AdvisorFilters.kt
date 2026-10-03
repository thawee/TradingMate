package apincer.mobile.tradings.ui

import apincer.mobile.tradings.domain.IndicatorSignal

/** Research filters only. These never modify entry, sizing, exit or AI eligibility. */
enum class AdvisorFilter(val label: String, val explanation: String) {
    LIQUIDITY("Liquidity", "Daily turnover above ฿5 million."),
    FRESHNESS("Fresh history", "Stock and benchmark dates match and are no more than seven days old."),
    ABOVE_LOW("Above 52w low", "Price at least 5% above its 52-week low; missing history fails."),
    NO_SELL("No SELL signal", "Exclude active SELL signals."),
    QUALITY("Quality", "ROE above 10%; margin above 10%, growth above 10% and nonfinancial D/E below 1.5 when available."),
    YIELD("Yield ≥5%", "Reported dividend yield at least 5%."),
    WEEKLY("Weekly uptrend", "Completed weekly trend must be bullish."),
    REGIME("Market context", "In a bearish market, require positive relative strength or qualifying foreign flow."),
    BUY("Confirmed BUY", "Require the app's confirmed BUY signal; POTENTIAL does not pass.");

    val bit: Int get() = 1 shl ordinal

    fun matches(stock: StockWatchlistInfo, bearish: Boolean): Boolean = when (this) {
        LIQUIDITY -> StockDna.isLiquid(stock)
        FRESHNESS -> StockDna.isFresh(stock)
        ABOVE_LOW -> StockDna.isNotNear52wLow(stock)
        NO_SELL -> stock.signal?.type != IndicatorSignal.SELL
        QUALITY -> StockDna.isQual(stock)
        YIELD -> StockDna.isDiv(stock)
        WEEKLY -> stock.portfolio.weeklyTrendBullish == true
        REGIME -> !bearish || StockDna.isFlow(stock) ||
            (stock.portfolio.relativeStrength ?: Double.NEGATIVE_INFINITY) > 0.0
        BUY -> stock.signal?.type == IndicatorSignal.BUY
    }

    companion object {
        fun stages(mode: PlaybookMode): List<AdvisorFilter> = when (mode) {
            PlaybookMode.SWING -> entries.filter { it != YIELD }
            PlaybookMode.DIVIDEND -> listOf(LIQUIDITY, FRESHNESS, ABOVE_LOW, NO_SELL, QUALITY, YIELD)
        }

        fun defaultMask(mode: PlaybookMode): Int = stages(mode).fold(0) { mask, stage -> mask or stage.bit }
    }
}

data class AdvisorFilterCount(val filter: AdvisorFilter, val enabled: Boolean, val count: Int)
data class AdvisorFilterResult(val stages: List<AdvisorFilterCount>, val stocks: List<StockWatchlistInfo>)

fun filterAdvisorStocks(
    stocks: List<StockWatchlistInfo>, mode: PlaybookMode, mask: Int, bearish: Boolean
): AdvisorFilterResult {
    var remaining = stocks
    val counts = AdvisorFilter.stages(mode).map { filter ->
        val passing = remaining.filter { filter.matches(it, bearish) }
        val enabled = mask and filter.bit != 0
        if (enabled) remaining = passing
        // Disabled stages preview the count if enabled here; later stages still use only selected filters.
        AdvisorFilterCount(filter, enabled, passing.size)
    }
    return AdvisorFilterResult(counts, remaining)
}
