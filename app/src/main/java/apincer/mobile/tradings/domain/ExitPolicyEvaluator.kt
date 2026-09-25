package apincer.mobile.tradings.domain

enum class ExitReason { STOP, TARGET, INVALIDATION }

data class ExitDecision(
    val reason: ExitReason,
    val triggerPrice: Double,
    val planId: String,
    val planVersion: Int,
    val description: String
)

/** Pure evaluation of accepted fixed-plan levels. Prices are alert observations, not guaranteed fills. */
object ExitPolicyEvaluator {
    fun evaluate(
        plan: TradePlan,
        currentPrice: Double,
        macdHist: Double? = null,
        sma50: Double? = null,
        isNearXdDate: Boolean = false
    ): ExitDecision? {
        if (plan.exitPolicy != ExitPolicy.FIXED_TARGET || !currentPrice.isFinite() || currentPrice <= 0.0) return null
        val stop = plan.initialStopPrice
        if (stop != null && currentPrice <= stop) {
            return ExitDecision(ExitReason.STOP, stop, plan.id, plan.version,
                "Saved stop reached at ฿$stop")
        }
        val entry = plan.plannedEntryPrice
        if (!isNearXdDate && macdHist != null && sma50 != null &&
            currentPrice <= entry * 0.985 && currentPrice < sma50 && macdHist < 0.0) {
            return ExitDecision(ExitReason.INVALIDATION, currentPrice, plan.id, plan.version,
                "Early breakdown: below SMA 50 with negative MACD while down at least 1.5%")
        }
        val target = plan.targetPrice
        if (target != null && currentPrice >= target) {
            return ExitDecision(ExitReason.TARGET, target, plan.id, plan.version,
                "Saved target reached at ฿$target")
        }
        return null
    }
}
