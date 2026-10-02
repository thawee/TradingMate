package apincer.mobile.tradings.domain

data class TradeRiskLimits(
    val riskPercent: Double,
    val stockAllocationPercent: Double,
    val sectorAllocationPercent: Double,
    val cashReservePercent: Double,
    val minNetRewardRiskRatio: Double = 2.0
)

data class TradeRiskInput(
    val entryPrice: Double,
    val stopPrice: Double,
    val quantity: Int,
    val buyFees: Double,
    val accountEquity: Double,
    val cashBalance: Double,
    val existingStockValue: Double,
    val existingSectorValue: Double?,
    val atsEnabled: Boolean,
    val targetPrice: Double? = null,
    val requireMinimumNetReward: Boolean = false
)

data class TradeRiskResult(val reasons: List<String>) {
    val allowed: Boolean get() = reasons.isEmpty()
}

object TradeRiskPolicy {
    fun evaluate(input: TradeRiskInput, limits: TradeRiskLimits): TradeRiskResult {
        val reasons = mutableListOf<String>()
        if (!limits.riskPercent.isFinite() || limits.riskPercent <= 0.0 ||
            !limits.stockAllocationPercent.isFinite() || limits.stockAllocationPercent <= 0.0 ||
            !limits.sectorAllocationPercent.isFinite() || limits.sectorAllocationPercent <= 0.0 ||
            !limits.cashReservePercent.isFinite() || limits.cashReservePercent < 0.0 ||
            !limits.minNetRewardRiskRatio.isFinite() || limits.minNetRewardRiskRatio < 1.0) {
            return TradeRiskResult(listOf("Risk settings are invalid; review them in Settings"))
        }
        if (!input.entryPrice.isFinite() || !input.stopPrice.isFinite() ||
            input.entryPrice <= 0.0 || input.stopPrice <= 0.0 || input.stopPrice >= input.entryPrice ||
            input.quantity <= 0 || input.quantity % 100 != 0 ||
            input.accountEquity <= 0.0 || !input.accountEquity.isFinite()) {
            return TradeRiskResult(listOf("Enter a valid entry, lower stop, equity and 100-share quantity"))
        }
        val purchase = input.entryPrice * input.quantity + input.buyFees
        val exitFees = TechnicalAnalysis.calculateFees(
            input.stopPrice * input.quantity, true, input.atsEnabled)
        val lossAtStop = (input.entryPrice - input.stopPrice) * input.quantity + input.buyFees + exitFees
        if (input.requireMinimumNetReward) {
            val target = input.targetPrice
            val netReward = if (target != null && target.isFinite() && target > input.entryPrice) {
                (target - input.entryPrice) * input.quantity - input.buyFees -
                    TechnicalAnalysis.calculateFees(target * input.quantity, true, input.atsEnabled)
            } else Double.NEGATIVE_INFINITY
            if (netReward < limits.minNetRewardRiskRatio * lossAtStop - 0.01)
                reasons.add("A proposed swing trade needs a supported target with net Reward:Risk of at least ${limits.minNetRewardRiskRatio}:1")
        }
        if (lossAtStop > input.accountEquity * limits.riskPercent / 100.0 + 0.01)
            reasons.add("Stop-loss risk exceeds the configured per-trade budget")
        if (input.existingStockValue + input.entryPrice * input.quantity >
            input.accountEquity * limits.stockAllocationPercent / 100.0 + 0.01)
            reasons.add("Combined holding exceeds the single-stock allocation limit")
        val sectorValue = input.existingSectorValue
        if (sectorValue == null) reasons.add("Sector exposure is unknown")
        else if (sectorValue + input.entryPrice * input.quantity >
            input.accountEquity * limits.sectorAllocationPercent / 100.0 + 0.01)
            reasons.add("Sector allocation exceeds the configured limit")
        if (input.cashBalance - purchase < input.accountEquity * limits.cashReservePercent / 100.0 - 0.01)
            reasons.add("Purchase would breach the cash reserve")
        return TradeRiskResult(reasons)
    }

    /**
     * Core (TDEX) buys are held, not traded: no stop, target, single-stock, sector or regime
     * cash-buffer gate. The buffer rule was never backtested and would block DCA when prices fall.
     */
    fun evaluateCoreBuy(entryPrice: Double, quantity: Int, buyFees: Double, cashBalance: Double): TradeRiskResult {
        if (!entryPrice.isFinite() || entryPrice <= 0.0 || quantity <= 0 || quantity % 100 != 0)
            return TradeRiskResult(listOf("Enter a valid price and 100-share quantity"))
        return if (entryPrice * quantity + buyFees > cashBalance + 0.01)
            TradeRiskResult(listOf("Purchase exceeds available cash"))
        else TradeRiskResult(emptyList())
    }
}
