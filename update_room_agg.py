import re

with open("app/src/main/java/apincer/mobile/tradings/data/RoomModels.kt", "r") as f:
    content = f.read()

agg_replacement = """data class StockAggregate(
    @Embedded val portfolio: PortfolioEntity,
    @Relation(
        parentColumn = "symbol",
        entityColumn = "symbol"
    )
    val cache: StockCacheEntity?,
    @Relation(
        parentColumn = "symbol",
        entityColumn = "symbol"
    )
    val signal: StockSignalEntity?
) {
    val symbol: String get() = portfolio.symbol
    val name: String? get() = cache?.name
    val nameTH: String? get() = cache?.nameTH
    val businessDescription: String? get() = cache?.businessDescription
    val sector: String? get() = cache?.sector
    val industry: String? get() = cache?.industry
    val cost: Double get() = portfolio.cost
    val quantity: Int get() = portfolio.quantity
    val tradePurpose: String get() = portfolio.tradePurpose
    val buyFees: Double get() = portfolio.buyFees
    val dividendPerShare: Double? get() = cache?.dividendPerShare
    val lastPrice: Double get() = cache?.lastPrice ?: 0.0
    val change: Double get() = cache?.change ?: 0.0
    val percentChange: Double get() = cache?.percentChange ?: 0.0
    val pe: Double? get() = cache?.pe
    val pbv: Double? get() = cache?.pbv
    val roe: Double? get() = cache?.roe
    val eps: Double? get() = cache?.eps
    val netProfit: Double? get() = cache?.netProfit
    val equity: Double? get() = cache?.equity
    val debtToEquity: Double? get() = cache?.debtToEquity
    val dividendYield: Double? get() = cache?.dividendYield
    val dividendDate: String? get() = cache?.dividendDate
    val rsi: Double? get() = signal?.rsi
    val macdHist: Double? get() = signal?.macdHist
    val signalType: String? get() = signal?.signalType
    val signalReason: String? get() = signal?.signalReason
    val signalDescription: String? get() = signal?.signalDescription
    val lastUpdated: String? get() = cache?.lastUpdated ?: signal?.lastUpdated
    val stopLoss: Double get() = portfolio.stopLoss
    val playbookNote: String get() = portfolio.playbookNote
    val netProfitMargin: Double? get() = cache?.netProfitMargin
    val profitGrowth3Y: Double? get() = cache?.profitGrowth3Y
}"""

content = re.sub(r'data class StockAggregate\(.*?\n\)', agg_replacement, content, flags=re.DOTALL)

with open("app/src/main/java/apincer/mobile/tradings/data/RoomModels.kt", "w") as f:
    f.write(content)
