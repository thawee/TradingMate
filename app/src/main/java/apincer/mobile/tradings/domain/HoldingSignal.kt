package apincer.mobile.tradings.domain

/**
 * The one signal shown and alerted on a holding, shared by the Advisor, Portfolio and the worker.
 * Order: saved stop, saved fixed plan, core fund, Dividend-purpose hold, then the technical signal.
 */
object HoldingSignal {
    /**
     * Dividend-purpose holdings follow the tested high-yield rule (F5): no price, quality or yield exits;
     * names are replaced at the monthly review when they leave the list. Only a stop the user saved alerts.
     */
    val DIVIDEND_HOLD = TradeSignal(IndicatorSignal.NEUTRAL, "Dividend hold",
        "Held under the high-yield rule: review it at the monthly rebalance. Only a stop you saved sends an alert.")

    fun resolve(symbol: String, quantity: Int, tradePurpose: String?, savedStop: Double, price: Double,
                exitPolicy: String?, plan: () -> TradePlan, technical: TradeSignal?): TradeSignal? {
        if (quantity > 0 && savedStop > 0.0 && price > 0.0 && price <= savedStop)
            return TradeSignal(IndicatorSignal.SELL, "STOP", "Saved stop reached at ฿$savedStop")
        if (quantity > 0 && exitPolicy == ExitPolicy.FIXED_TARGET.name) {
            val decision = ExitPolicyEvaluator.evaluate(plan(), price)
            return if (decision != null) TradeSignal(IndicatorSignal.SELL, decision.reason.name, decision.description)
            else TradeSignal(IndicatorSignal.NEUTRAL, "Saved plan active", "No saved stop or target reached")
        }
        if (CoreSatellite.isCore(symbol)) return CoreSatellite.CORE_SIGNAL
        if (quantity > 0 && tradePurpose == "DIVIDEND") return DIVIDEND_HOLD
        return technical
    }
}

/**
 * Advisor notes for a Dividend-purpose holding under the tested high-yield rule. Only a saved stop and
 * leaving the ranked list are exits; low ROE, low yield and a deep drawdown are review notes, because
 * quality filters and selling after a fall both did worse in the 2015-2025 tests.
 */
object DividendExitPolicy {
    data class Note(val exit: Boolean, val reason: String)

    /** [inHighYieldList] is null when no ranking is available; [savedStop] 0 when none (or a fixed plan owns it). */
    fun notes(price: Double, costPerShare: Double, savedStop: Double, yieldPercent: Double?, roe: Double?,
              inHighYieldList: Boolean?): List<Note> {
        val notes = mutableListOf<Note>()
        if (savedStop > 0.0 && price > 0.0 && price <= savedStop)
            notes += Note(true, "Saved stop reached at ฿$savedStop")
        if (inHighYieldList == false)
            notes += Note(true, "Left the high dividend yield top 10: sell at the monthly review (tested rule)")
        if (roe != null && roe < TradingConstants.ROE_MIN_THRESHOLD)
            notes += Note(false, "Review: ROE below ${TradingConstants.ROE_MIN_THRESHOLD.toInt()}% (not part of the tested rule)")
        if (yieldPercent != null && yieldPercent < TradingConstants.DIVIDEND_YIELD_PROTECTION)
            notes += Note(false, "Review: yield below ${TradingConstants.DIVIDEND_YIELD_PROTECTION.toInt()}%")
        if (costPerShare > 0.0 && price > 0.0) {
            val drawdown = (price - costPerShare) / costPerShare * 100
            if (drawdown <= TradingConstants.DIVIDEND_DEEP_DRAWDOWN_PERCENT)
                notes += Note(false, String.format(java.util.Locale.ENGLISH, "Review: down %.1f%% from cost", drawdown))
        }
        return notes
    }
}
