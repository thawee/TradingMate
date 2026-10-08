package apincer.mobile.tradings.domain

/**
 * "High dividend yield list": the current SET50 members with the highest reported dividend yield
 * (variant F5 in tools/backtest/fundamental_screens_report.md). It passed every backtest check, but
 * the backtest is inflated by survivorship; the real 1DIV ETF beat TDEX over 2015-2025 by about
 * 1.3 points a year, not in every period. Shown only as an untested list.
 */
object HighYieldList {
    const val TOP_N = 10

    /** [yields] in percent per symbol; missing or non-positive yields are skipped. */
    fun rank(yields: Map<String, Double?>, topN: Int = TOP_N): List<MomentumList.Entry> =
        yields.mapNotNull { (s, y) -> y?.takeIf { it > 0.0 && it.isFinite() }?.let { MomentumList.Entry(s.uppercase(), it) } }
            .sortedByDescending { it.returnPercent }.take(topN)

    /**
     * Oil and gas, petrochemical and cement names left out by variant TR2 (tools/backtest/dividend_trap_report.md):
     * ahead of F5 over 2015-2025 with a lower drawdown, but behind in 2021-2025 and chosen with hindsight, so it
     * only gets a forward record and never changes what the app suggests.
     */
    val COMMODITY = setOf("PTT", "PTTEP", "BCP", "SPRC", "OR", "IVL", "PTTGC", "SCC")

    /** The top [topN] of a full [ranked] list once [COMMODITY] names are removed. */
    fun withoutCommodities(ranked: List<MomentumList.Entry>, topN: Int = TOP_N): List<MomentumList.Entry> =
        ranked.filter { it.symbol.uppercase() !in COMMODITY }.take(topN)
}
