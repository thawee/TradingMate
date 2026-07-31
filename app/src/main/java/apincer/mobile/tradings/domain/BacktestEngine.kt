package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice

/** One closed (or still-open) simulated position. */
data class BacktestTrade(
    val entryDate: String,
    val entryPrice: Double,
    val exitDate: String,
    val exitPrice: Double,
    val exitReason: String,
    val netProfitPercent: Double,
    val holdingDays: Int,
    val isOpen: Boolean = false
)

/**
 * Aggregate result of replaying [TechnicalAnalysis.getDetailedSignal] day-by-day
 * over a stock's own price history — i.e. "what would have happened if a user
 * bought every time the app said BUY and sold every time it said SELL."
 */
data class BacktestResult(
    val symbol: String,
    val startDate: String?,
    val endDate: String?,
    val closedTrades: List<BacktestTrade>,
    val openTrade: BacktestTrade?,
    /** Compounded return across all CLOSED trades only (open position is unrealized). */
    val totalReturnPercent: Double,
    val maxDrawdownPercent: Double
) {
    val totalTrades: Int get() = closedTrades.size
    val wins: Int get() = closedTrades.count { it.netProfitPercent > 0 }
    val losses: Int get() = closedTrades.count { it.netProfitPercent <= 0 }
    val winRatePercent: Double get() = if (totalTrades == 0) 0.0 else wins * 100.0 / totalTrades
    val avgWinPercent: Double get() = closedTrades.filter { it.netProfitPercent > 0 }
        .map { it.netProfitPercent }.average().let { if (it.isNaN()) 0.0 else it }
    val avgLossPercent: Double get() = closedTrades.filter { it.netProfitPercent <= 0 }
        .map { it.netProfitPercent }.average().let { if (it.isNaN()) 0.0 else it }
    /** Expected net profit % per trade: winRate*avgWin + lossRate*avgLoss (avgLoss is already negative). */
    val expectancyPercent: Double get() =
        (winRatePercent / 100.0) * avgWinPercent + (1 - winRatePercent / 100.0) * avgLossPercent
}

/**
 * Replays the app's actual BUY/SELL signal engine over historical daily prices.
 *
 * Methodology (kept deliberately simple/transparent, not a professional
 * portfolio-level backtester):
 * - Indicators at day `i` are computed ONLY from data[0..i] (no look-ahead).
 * - A BUY signal on day `i` fills at day `i+1`'s close (avoids same-bar fill bias).
 * - While a position is open, the same [TechnicalAnalysis.getDetailedSignal] used
 *   by the Watchlist/Portfolio screens (stop-loss, trailing stop, take-profit,
 *   overbought exits) decides when to sell — exactly what a user following the
 *   app's signals would have experienced.
 * - Fees are already netted in via [TechnicalAnalysis.calculateNetProfitPercent].
 * - One position at a time (no pyramiding/partial scale-outs), fully re-invested
 *   between trades (compounded), and does not model bid/ask spread or slippage
 *   beyond the liquidity pre-filter applied upstream by the caller.
 * - Only tests the core signal engine — the 5-Layer DNA filters (QUAL/VAL/DIV)
 *   are a separate candidate-selection layer, not replayed here.
 */
object BacktestEngine {
    // Needs SMA200 (200) + a buffer so the very first evaluated day has stable indicators.
    private const val WARMUP_DAYS = 210

    private data class OpenPosition(val entryIndex: Int, val entryPrice: Double, var peakPrice: Double)

    fun run(
        symbol: String,
        history: List<ScrapedHistoricalPrice>,
        isSet50: Boolean = false,
        isFundamentalGood: Boolean = false,
        dividendYield: Double? = null,
        roe: Double? = null
    ): BacktestResult? {
        if (history.size < WARMUP_DAYS + 2) return null

        val closes = history.map { it.close }
        val highs = history.map { it.high }
        val lows = history.map { it.low }
        val volumes = history.map { it.volume }

        val trades = mutableListOf<BacktestTrade>()
        var position: OpenPosition? = null
        var pendingEntryPrice: Double? = null

        var equity = 1.0
        var peakEquity = 1.0
        var maxDrawdown = 0.0

        for (i in WARMUP_DAYS until history.size) {
            // A BUY signal fired yesterday — fill today at close, skip evaluation
            // this bar (no same-day entry+exit).
            if (position == null && pendingEntryPrice != null) {
                position = OpenPosition(entryIndex = i, entryPrice = closes[i], peakPrice = closes[i])
                pendingEntryPrice = null
                continue
            }

            val pricesToDate = closes.subList(0, i + 1)
            val highsToDate = highs.subList(0, i + 1)
            val lowsToDate = lows.subList(0, i + 1)
            val volumesToDate = volumes.subList(0, i + 1)
            val lastPrice = closes[i]

            val sma50 = TechnicalAnalysis.calculateSMA(pricesToDate, 50)
            val sma200 = TechnicalAnalysis.calculateSMA(pricesToDate, 200)
            val bb = TechnicalAnalysis.calculateBollingerBands(pricesToDate)
            val rsi = TechnicalAnalysis.calculateRSI(pricesToDate)
            val macd = TechnicalAnalysis.calculateMACD(pricesToDate)
            val isVolSurge = TechnicalAnalysis.isVolumeSurge(volumesToDate)
            val obvRising = TechnicalAnalysis.isObvRising(pricesToDate, volumesToDate)
            val atr = TechnicalAnalysis.calculateATR(highsToDate, lowsToDate, pricesToDate)
            val adx = TechnicalAnalysis.calculateADX(highsToDate, lowsToDate, pricesToDate)
            val stoch = TechnicalAnalysis.calculateStochastic(highsToDate, lowsToDate, pricesToDate)
            val mfi = TechnicalAnalysis.calculateMFI(highsToDate, lowsToDate, pricesToDate, volumesToDate)
            val atrPercent = atr?.takeIf { lastPrice > 0 }?.let { it / lastPrice * 100.0 }

            val openPos = position
            if (openPos != null) {
                openPos.peakPrice = maxOf(openPos.peakPrice, lastPrice)
                val signal = TechnicalAnalysis.getDetailedSignal(
                    rsi = rsi, macdHist = macd.third, lastPrice = lastPrice,
                    sma50 = sma50, sma200 = sma200, bb = bb,
                    isVolumeSurge = isVolSurge, obvRising = obvRising,
                    atrPercent = atrPercent, adx = adx,
                    stochK = stoch?.first, stochD = stoch?.second, mfi = mfi,
                    userCost = openPos.entryPrice, userQuantity = 1,
                    isFundamentalGood = isFundamentalGood, tradePurpose = "SWING",
                    dividendYield = dividendYield, roe = roe,
                    peakPrice = openPos.peakPrice, isSet50 = isSet50
                )
                if (signal.type == IndicatorSignal.SELL) {
                    val netProfit = TechnicalAnalysis.calculateNetProfitPercent(openPos.entryPrice, lastPrice)
                    trades.add(
                        BacktestTrade(
                            entryDate = history[openPos.entryIndex].date,
                            entryPrice = openPos.entryPrice,
                            exitDate = history[i].date,
                            exitPrice = lastPrice,
                            exitReason = signal.reason,
                            netProfitPercent = netProfit,
                            holdingDays = i - openPos.entryIndex
                        )
                    )
                    equity *= (1 + netProfit / 100.0)
                    peakEquity = maxOf(peakEquity, equity)
                    maxDrawdown = maxOf(maxDrawdown, (peakEquity - equity) / peakEquity * 100.0)
                    position = null
                }
            } else {
                val signal = TechnicalAnalysis.getDetailedSignal(
                    rsi = rsi, macdHist = macd.third, lastPrice = lastPrice,
                    sma50 = sma50, sma200 = sma200, bb = bb,
                    isVolumeSurge = isVolSurge, obvRising = obvRising,
                    atrPercent = atrPercent, adx = adx,
                    stochK = stoch?.first, stochD = stoch?.second, mfi = mfi,
                    userCost = null, userQuantity = null,
                    isFundamentalGood = isFundamentalGood, tradePurpose = "SWING",
                    dividendYield = dividendYield, roe = roe,
                    peakPrice = null, isSet50 = isSet50
                )
                if (signal.type == IndicatorSignal.BUY && i + 1 < history.size) {
                    pendingEntryPrice = lastPrice // fill next bar
                }
            }
        }

        val finalPosition = position
        val openTrade = finalPosition?.let {
            val netProfit = TechnicalAnalysis.calculateNetProfitPercent(it.entryPrice, closes.last())
            BacktestTrade(
                entryDate = history[it.entryIndex].date,
                entryPrice = it.entryPrice,
                exitDate = history.last().date,
                exitPrice = closes.last(),
                exitReason = "Open (Unrealized)",
                netProfitPercent = netProfit,
                holdingDays = closes.size - 1 - it.entryIndex,
                isOpen = true
            )
        }

        return BacktestResult(
            symbol = symbol,
            startDate = history.getOrNull(WARMUP_DAYS)?.date,
            endDate = history.lastOrNull()?.date,
            closedTrades = trades,
            openTrade = openTrade,
            totalReturnPercent = (equity - 1.0) * 100.0,
            maxDrawdownPercent = maxDrawdown
        )
    }
}
