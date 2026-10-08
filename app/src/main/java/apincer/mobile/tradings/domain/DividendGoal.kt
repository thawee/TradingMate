package apincer.mobile.tradings.domain

/** Capital a yearly dividend target needs at the holdings' current yield (target ÷ yield). */
object DividendGoal {
    data class Capital(val needed: Double, val invested: Double, val yieldPercent: Double) {
        val shortfall: Double get() = (needed - invested).coerceAtLeast(0.0)
    }

    /** Null when the target, the expected yearly dividend or the market value is not positive. */
    fun capital(targetYearly: Double, yearlyDividend: Double, marketValue: Double): Capital? {
        if (targetYearly <= 0.0 || yearlyDividend <= 0.0 || marketValue <= 0.0) return null
        val yieldPercent = yearlyDividend / marketValue * 100.0
        return Capital(targetYearly / (yieldPercent / 100.0), marketValue, yieldPercent)
    }
}
