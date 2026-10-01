package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class PortfolioBacktestConfig(
    /** First date on which signals may fire (inclusive, yyyy-MM-dd). Earlier bars are warm-up only. */
    val startDate: String,
    /** Last simulated date (inclusive, yyyy-MM-dd). */
    val endDate: String,
    val initialCapital: Double = 1_000_000.0,
    val riskPercent: Double = 1.0,
    val maxStockAllocationPercent: Double = TradingConstants.MAX_SINGLE_STOCK_ALLOCATION_PERCENT,
    val maxPositions: Int = 10,
    /** Adverse fill vs close on each side (0.15 = 0.15%). */
    val slippagePercent: Double = 0.15,
    /** Bars fed to the indicators, matching the live app's ~1-year history window. */
    val lookback: Int = 260
)

data class PortfolioTrade(
    val symbol: String,
    val entryDate: String,
    val exitDate: String,
    val shares: Int,
    val entryFill: Double,
    val exitFill: Double,
    val pnlBaht: Double,
    /** Net P/L divided by the baht risked at entry (shares × (fill − stop)). */
    val rMultiple: Double,
    val exitReason: String
)

data class EquityStats(
    val startDate: String,
    val endDate: String,
    val finalEquity: Double,
    val totalReturnPercent: Double,
    val cagrPercent: Double,
    val maxDrawdownPercent: Double
) {
    companion object {
        fun from(curve: List<Pair<String, Double>>, initialCapital: Double): EquityStats {
            require(curve.isNotEmpty()) { "Empty equity curve" }
            val final = curve.last().second
            val years = ChronoUnit.DAYS.between(LocalDate.parse(curve.first().first), LocalDate.parse(curve.last().first)) / 365.25
            val growth = final / initialCapital
            return EquityStats(
                startDate = curve.first().first,
                endDate = curve.last().first,
                finalEquity = final,
                totalReturnPercent = (growth - 1.0) * 100.0,
                cagrPercent = if (years > 0 && growth > 0) (Math.pow(growth, 1.0 / years) - 1.0) * 100.0 else 0.0,
                maxDrawdownPercent = TechnicalAnalysis.calculateMaxDrawdown(curve.map { it.second }).maxDrawdownPercent
            )
        }
    }
}

data class PortfolioBacktestResult(
    val equityCurve: List<Pair<String, Double>>,
    val stats: EquityStats,
    val trades: List<PortfolioTrade>,
    val openPositionsAtEnd: Int,
    /** Average share of equity held in stocks across simulated days. */
    val exposurePercent: Double,
    /** BUY signals not taken because of position count, cash, or lot-size limits. */
    val skippedSignals: Int
) {
    val winRatePercent: Double get() = if (trades.isEmpty()) 0.0 else trades.count { it.pnlBaht > 0 } * 100.0 / trades.size
    val expectancyR: Double get() = if (trades.isEmpty()) 0.0 else trades.map { it.rMultiple }.average()
    val tradesPerYear: Double get() {
        val years = ChronoUnit.DAYS.between(LocalDate.parse(stats.startDate), LocalDate.parse(stats.endDate)) / 365.25
        return if (years > 0) trades.size / years else 0.0
    }
}

/**
 * Portfolio-level replay of the technical signal engine across a universe of stocks
 * with shared capital. Uses the same [BacktestEngine.signalAt] as the in-app backtest.
 *
 * - Signals on day `d` fill at the next bar's close for that symbol, with slippage and fees.
 * - Size: fixed-fractional risk to the legacy stop (2×ATR, clamped) via
 *   [TechnicalAnalysis.calculateRecommendedPositionSize], 100-share lots, single-stock cap,
 *   further limited by available cash and [PortfolioBacktestConfig.maxPositions].
 * - Simultaneous BUYs are taken in symbol order (no ranking model is replayed).
 * - Not replayed: fundamentals, NVDR flow, relative strength, weekly trend, XD dates,
 *   market regime cash buffers, sector caps, saved plans and AI ranking.
 */
object PortfolioBacktest {
    private const val WARMUP_BARS = 210

    private class Position(
        val symbol: String,
        val shares: Int,
        val entryFill: Double,
        val entryCashOut: Double,
        val riskBaht: Double,
        val entryDate: String,
        var peak: Double
    )

    fun run(
        universe: Map<String, List<ScrapedHistoricalPrice>>,
        config: PortfolioBacktestConfig,
        isSet50: (String) -> Boolean = { it.uppercase() in TradingConstants.SET50_SYMBOLS }
    ): PortfolioBacktestResult {
        val symbols = universe.keys.sorted()
        val indexByDate = symbols.associateWith { s -> universe.getValue(s).withIndex().associate { it.value.date to it.index } }
        val dates = universe.values.flatMap { bars -> bars.map { it.date } }
            .filter { it >= config.startDate && it <= config.endDate }
            .toSortedSet().toList()
        val slip = config.slippagePercent / 100.0

        var cash = config.initialCapital
        val positions = linkedMapOf<String, Position>()
        val lastClose = mutableMapOf<String, Double>()
        val pendingBuys = linkedSetOf<String>()
        val pendingSells = linkedMapOf<String, String>()
        val trades = mutableListOf<PortfolioTrade>()
        val curve = mutableListOf<Pair<String, Double>>()
        var exposureSum = 0.0
        var skipped = 0

        fun equity() = cash + positions.values.sumOf { it.shares * (lastClose[it.symbol] ?: it.entryFill) }

        for (date in dates) {
            val trading = symbols.filter { indexByDate.getValue(it).containsKey(date) }
            trading.forEach { s -> lastClose[s] = universe.getValue(s)[indexByDate.getValue(s).getValue(date)].close }

            // 1. Exits signalled yesterday.
            for (s in pendingSells.keys.toList().filter { it in trading }) {
                val pos = positions.remove(s) ?: continue
                val fill = lastClose.getValue(s) * (1 - slip)
                val gross = pos.shares * fill
                val cashIn = gross - TechnicalAnalysis.calculateFees(gross, isSelling = true)
                cash += cashIn
                val pnl = cashIn - pos.entryCashOut
                trades += PortfolioTrade(
                    s, pos.entryDate, date, pos.shares, pos.entryFill, fill, pnl,
                    if (pos.riskBaht > 0) pnl / pos.riskBaht else 0.0, pendingSells.getValue(s)
                )
                pendingSells.remove(s)
            }

            // 2. Entries signalled yesterday.
            for (s in pendingBuys.toList().filter { it in trading }) {
                pendingBuys.remove(s)
                if (positions.size >= config.maxPositions) { skipped++; continue }
                val bars = universe.getValue(s)
                val i = indexByDate.getValue(s).getValue(date)
                val fill = lastClose.getValue(s) * (1 + slip)
                val window = bars.subList(maxOf(0, i + 1 - config.lookback), i + 1)
                val atrPercent = TechnicalAnalysis.calculateATR(
                    window.map { it.high }, window.map { it.low }, window.map { it.close }
                )?.let { it / fill * 100.0 }
                val stopPrice = fill * (1 + TechnicalAnalysis.legacyStopLossPercent(atrPercent, isSet50(s)) / 100.0)
                var shares = TechnicalAnalysis.calculateRecommendedPositionSize(
                    totalAssets = equity(), entryPrice = fill, stopLossPrice = stopPrice,
                    riskPercent = config.riskPercent, maxStockAllocationPercent = config.maxStockAllocationPercent
                ).shares
                fun cashOut(n: Int) = n * fill + TechnicalAnalysis.calculateFees(n * fill, isSelling = false)
                while (shares >= 100 && cashOut(shares) > cash) shares -= 100
                if (shares < 100) { skipped++; continue }
                cash -= cashOut(shares)
                positions[s] = Position(s, shares, fill, cashOut(shares), shares * (fill - stopPrice), date, fill)
            }

            // 3. Today's signals, filled on each symbol's next bar.
            for (s in trading) {
                val i = indexByDate.getValue(s).getValue(date)
                if (i < WARMUP_BARS) continue
                val bars = universe.getValue(s)
                val pos = positions[s]
                if (pos != null) {
                    pos.peak = maxOf(pos.peak, bars[i].close)
                    if (s in pendingSells) continue
                    val signal = BacktestEngine.signalAt(
                        bars, i, entryPrice = pos.entryFill, quantity = pos.shares, peakPrice = pos.peak,
                        isSet50 = isSet50(s), lookback = config.lookback
                    )
                    if (signal.type == IndicatorSignal.SELL) pendingSells[s] = signal.reason
                } else if (s !in pendingBuys) {
                    val signal = BacktestEngine.signalAt(bars, i, isSet50 = isSet50(s), lookback = config.lookback)
                    if (signal.type == IndicatorSignal.BUY) pendingBuys += s
                }
            }

            val eq = equity()
            exposureSum += if (eq > 0) (eq - cash) / eq else 0.0
            curve += date to eq
        }

        return PortfolioBacktestResult(
            equityCurve = curve,
            stats = EquityStats.from(curve, config.initialCapital),
            trades = trades,
            openPositionsAtEnd = positions.size,
            exposurePercent = if (dates.isEmpty()) 0.0 else exposureSum / dates.size * 100.0,
            skippedSignals = skipped
        )
    }

    /** Buy-and-hold of [series] over the same window, all-in at the first close with fees and slippage. */
    fun buyAndHold(series: List<ScrapedHistoricalPrice>, config: PortfolioBacktestConfig): EquityStats {
        val bars = series.filter { it.date >= config.startDate && it.date <= config.endDate }
        require(bars.isNotEmpty()) { "No benchmark bars in window" }
        val fill = bars.first().close * (1 + config.slippagePercent / 100.0)
        val shares = config.initialCapital / (fill * (1 + TechnicalAnalysis.THAI_FEE_RATE))
        val leftover = config.initialCapital - shares * fill - TechnicalAnalysis.calculateFees(shares * fill, isSelling = false)
        return EquityStats.from(bars.map { it.date to leftover + shares * it.close }, config.initialCapital)
    }
}
