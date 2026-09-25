package apincer.mobile.tradings.domain

import java.util.Locale

/** Locally checked trade levels available to the model for explanation and ranking. */
data class AiCandidatePlan(
    val symbol: String,
    val entryPrice: Double,
    val stopPrice: Double,
    val targetPrice: Double,
    val allocationBaht: Double,
    val shares: Int,
    val snapshotId: String
)

object AiRecommendationValidator {
    fun validate(
        result: AiAnalysisResult,
        requested: Map<String, AiCandidatePlan>,
        current: Map<String, AiCandidatePlan>
    ): AiAnalysisResult {
        val accepted = result.recommendations.mapNotNull { rec ->
            val symbol = rec.symbol.trim().uppercase(Locale.ENGLISH)
            val plan = requested[symbol] ?: return@mapNotNull null
            if (current[symbol] != plan || !valid(plan)) return@mapNotNull null
            rec.copy(
                symbol = symbol,
                buyZone = String.format(Locale.ENGLISH, "฿%.2f (local observed price)", plan.entryPrice),
                targetProfit = String.format(Locale.ENGLISH, "฿%.2f (observed 52-week high)", plan.targetPrice),
                stopLoss = String.format(Locale.ENGLISH, "฿%.2f (local ATR estimate)", plan.stopPrice),
                cashAllocation = String.format(Locale.ENGLISH, "฿%,.2f / %d shares (local limit)",
                    plan.allocationBaht, plan.shares)
            )
        }.distinctBy { it.symbol }
        return result.copy(
            executiveSummary = result.executiveSummary.takeIf { accepted.isNotEmpty() }.orEmpty(),
            recommendations = accepted
        )
    }

    private fun valid(plan: AiCandidatePlan): Boolean =
        plan.symbol.isNotBlank() && plan.snapshotId.isNotBlank() &&
            plan.entryPrice.isFinite() && plan.stopPrice.isFinite() && plan.targetPrice.isFinite() &&
            plan.entryPrice > 0.0 && plan.stopPrice > 0.0 &&
            plan.stopPrice < plan.entryPrice && plan.targetPrice > plan.entryPrice &&
            plan.shares > 0 && plan.shares % 100 == 0 &&
            plan.allocationBaht.isFinite() && plan.allocationBaht > 0.0
}
