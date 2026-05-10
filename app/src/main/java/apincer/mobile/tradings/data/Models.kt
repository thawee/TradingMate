package apincer.mobile.tradings.data

import kotlinx.serialization.Serializable

@Serializable
data class StockInfoResponse(
    val symbol: String,
    val lastPrice: Double? = null,
    val change: Double? = null,
    val percentChange: Double? = null,
    val marketStatus: String? = null
)

@Serializable
data class HistoricalTradingResponse(
    val symbol: String,
    val historicalTradings: List<HistoricalTrading> = emptyList()
)

@Serializable
data class HistoricalTrading(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)
