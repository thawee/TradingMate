package apincer.mobile.tradings.domain

enum class ExitPolicy { LEGACY, FIXED_TARGET }

/** Accepted levels for a position. Actual fills and average cost remain separate. */
data class TradePlan(
    val id: String,
    val version: Int,
    val symbol: String,
    val plannedEntryPrice: Double,
    val initialStopPrice: Double?,
    val targetPrice: Double?,
    val strategy: String,
    val source: String,
    val exitPolicy: ExitPolicy,
    val createdAtMillis: Long
) {
    companion object {
        fun fixed(
            symbol: String,
            entry: Double,
            stop: Double,
            target: Double,
            strategy: String,
            source: String,
            id: String,
            createdAtMillis: Long,
            version: Int = 1
        ): TradePlan {
            require(entry.isFinite() && stop.isFinite() && target.isFinite())
            require(entry > 0.0 && stop > 0.0 && stop < entry && target > entry)
            require(id.isNotBlank() && version > 0)
            return TradePlan(id, version, symbol.uppercase(), entry, stop, target,
                strategy, source, ExitPolicy.FIXED_TARGET, createdAtMillis)
        }

        fun legacy(symbol: String, entry: Double, stop: Double?, strategy: String): TradePlan =
            TradePlan("", 0, symbol.uppercase(), entry, stop?.takeIf { it > 0 }, null,
                strategy, "LEGACY", ExitPolicy.LEGACY, 0L)
    }
}
