package apincer.mobile.tradings.domain

enum class ExitReason { STOP, TARGET }

data class ExitDecision(
    val reason: ExitReason,
    val triggerPrice: Double,
    val planId: String,
    val planVersion: Int,
    val description: String
)

/** Pure evaluation of accepted fixed-plan levels. Prices are alert observations, not guaranteed fills. */
object ExitPolicyEvaluator {
    /**
     * Saved stop, then saved target. No early-breakdown invalidation: the same rule in the signal
     * engine was the largest loss source in the 2015-2025 SET50 replay (tools/backtest/report.md).
     */
    fun evaluate(plan: TradePlan, currentPrice: Double): ExitDecision? {
        if (plan.exitPolicy != ExitPolicy.FIXED_TARGET || !currentPrice.isFinite() || currentPrice <= 0.0) return null
        val stop = plan.initialStopPrice
        if (stop != null && currentPrice <= stop) {
            return ExitDecision(ExitReason.STOP, stop, plan.id, plan.version,
                "Saved stop reached at ฿$stop")
        }
        val target = plan.targetPrice
        if (target != null && currentPrice >= target) {
            return ExitDecision(ExitReason.TARGET, target, plan.id, plan.version,
                "Saved target reached at ฿$target")
        }
        return null
    }
}
