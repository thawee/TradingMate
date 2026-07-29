package apincer.mobile.tradings.domain

import androidx.compose.ui.graphics.Color
import java.util.Locale
import kotlin.math.abs
import apincer.mobile.tradings.domain.TradingConstants
import kotlin.math.sqrt

data class Indicators(
    val sma50: Double?,
    val sma200: Double?,
    val rsi: Double?,
    val macd: Double?,
    val signal: Double?,
    val histogram: Double?,
    val bollingerBands: BollingerBands?,
    val isVolumeSurge: Boolean = false,
    val obvRising: Boolean = false,
    val week52Low: Double? = null,
    val week52High: Double? = null,
    val relativeStrength: Double? = null,
    val atr: Double? = null,
    val adx: Double? = null,
    val stochK: Double? = null,
    val stochD: Double? = null,
    val mfi: Double? = null
)

data class BollingerBands(
    val upper: Double,
    val middle: Double,
    val lower: Double
)

enum class IndicatorSignal {
    BUY, POTENTIAL, SELL, NEUTRAL
}

data class TradeSignal(
    val type: IndicatorSignal,
    val reason: String,
    val description: String
)

enum class MarketStatus(val label: String, val color: Color) {
    OPEN("MARKET OPEN", Color(0xFF00C853)),
    LUNCH("LUNCH BREAK", Color(0xFFFF9800)),
    CLOSED("MARKET CLOSED", Color(0xFF717478))
}

enum class TradingZone(val label: String, val color: Color) {
    BUYING_ZONE("Buying Zone", Color(0xFF00C853)),
    POTENTIAL_ZONE("Potential Zone", Color(0xFFC66900)),
    SELLING_ZONE("Selling Zone", Color.Red),
    NEUTRAL("Neutral Zone", Color(0xFF717478))
}

object TechnicalAnalysis {
    // InnovestX Fee Structure (Internet Trading, Cash Account):
    // 1. Commission: 0.15% for daily volume ≤ ฿5M (conservative worst-case flat rate)
    // 2. Market Fee: SET Trading Fee (0.005%) + TSD Clearing Fee (0.001%) + Regulatory Fee (0.001%) = 0.007%
    // 3. VAT: 7% on (Commission + Market Fee)
    // 4. Minimum Commission: ฿50/day — WAIVED if ATS + E-Statement are registered
    // 5. Selling Tax (Financial Transaction Tax): officially ABOLISHED, rate set to 0

    const val COMMISSION_RATE = 0.0015
    const val MARKET_FEE_RATE = 0.00007
    const val VAT_RATE = 0.07
    const val SELLING_TAX_RATE = 0.0        // FTT abolished by Thai Ministry of Finance
    const val MIN_COMMISSION_BAHT = 50.0    // Minimum commission per day (without ATS)
    const val MIN_COMMISSION_ATS = 0.0      // Minimum commission when ATS + E-Statement registered (waived)

    fun calculateFees(amount: Double, isSelling: Boolean, atsEnabled: Boolean = true): Double {
        val minCommission = if (atsEnabled) MIN_COMMISSION_ATS else MIN_COMMISSION_BAHT
        val commission = maxOf(amount * COMMISSION_RATE, minCommission)
        val marketFee = amount * MARKET_FEE_RATE
        val vat = (commission + marketFee) * VAT_RATE
        val totalFees = commission + marketFee + vat
        // SELLING_TAX_RATE is 0.0 — Financial Transaction Tax was officially abolished
        return totalFees
    }

    // Since we don't track total daily volume easily across all screens, 
    // we'll keep the single transaction fee calculation but highlight the 50 THB min if needed.
    // For most UI purposes, we'll use a combined rate for simpler display if requested.
    const val THAI_FEE_RATE = (COMMISSION_RATE + MARKET_FEE_RATE) * (1 + VAT_RATE)

    fun getTradingZone(
        rsi: Double?, 
        macdHist: Double?, 
        lastPrice: Double?, 
        sma50: Double?,
        sma200: Double?,
        bb: BollingerBands?
    ): TradingZone {
        if (rsi == null || macdHist == null || lastPrice == null) return TradingZone.NEUTRAL
        
        val isRsiOversold = rsi < TradingConstants.RSI_OVERSOLD
        val isRsiPotential = rsi < TradingConstants.RSI_POTENTIAL
        val isRsiOverbought = rsi > TradingConstants.RSI_OVERBOUGHT
        val isMacdBullish = macdHist > 0.0
        val isPriceAboveSma50 = if (sma50 != null) lastPrice > sma50 else false
        val isPriceAboveSma200 = if (sma200 != null) lastPrice > sma200 else true
        val isNearLowerBB = if (bb != null) lastPrice <= bb.lower * 1.05 else false
        val isNearUpperBB = if (bb != null) lastPrice >= bb.upper * 0.95 else false
        
        // Selling Zone: Overbought OR Near Upper Resistance OR Momentum collapse below SMA50
        if (isRsiOverbought || isNearUpperBB || (!isMacdBullish && !isPriceAboveSma50)) {
            return TradingZone.SELLING_ZONE
        }

        // Buying Zone: Oversold AND Long-term Uptrend (Aligned with getDetailedSignal high-conviction buy)
        // OR (Positive Momentum near support/above SMA50)
        if ((isRsiOversold && isPriceAboveSma200) || (isMacdBullish && (isNearLowerBB || isPriceAboveSma50))) {
            return TradingZone.BUYING_ZONE
        }

        // Potential Zone: Nearing Buy thresholds or Contrarian Watch (Oversold below SMA200)
        if (isRsiPotential || isNearLowerBB || isRsiOversold) {
            return TradingZone.POTENTIAL_ZONE
        }
        
        return TradingZone.NEUTRAL
    }

    fun getMarketStatus(): MarketStatus {
        val tz = java.util.TimeZone.getTimeZone("Asia/Bangkok")
        val now = java.util.Calendar.getInstance(tz)
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = now.get(java.util.Calendar.MINUTE)
        val currentTime = hour * 100 + minute

        if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) {
            return MarketStatus.CLOSED
        }

        return when {
            currentTime in 1000..1229 -> MarketStatus.OPEN      // Morning session: 10:00–12:29
            currentTime in 1230..1430 -> MarketStatus.LUNCH     // Lunch break:    12:30–14:30
            currentTime in 1431..1630 -> MarketStatus.OPEN      // Afternoon session: 14:31–16:30
            else                      -> MarketStatus.CLOSED
        }
    }

    fun getDetailedSignal(
        rsi: Double?, 
        macdHist: Double?, 
        lastPrice: Double?, 
        sma50: Double?,
        sma200: Double?,
        bb: BollingerBands?,
        isVolumeSurge: Boolean,
        obvRising: Boolean = false,
        atrPercent: Double? = null,
        adx: Double? = null,
        stochK: Double? = null,
        stochD: Double? = null,
        mfi: Double? = null,
        userCost: Double? = null,
        userQuantity: Int? = null,
        isFundamentalGood: Boolean = false,
        tradePurpose: String = "SWING",
        dividendYield: Double? = null,
        roe: Double? = null,
        peakPrice: Double? = null,
        isSet50: Boolean = false,
        userStopLoss: Double? = null
    ): TradeSignal {
        if (rsi == null || macdHist == null) return TradeSignal(IndicatorSignal.NEUTRAL, "Waiting for data", "We need more historical data to generate a signal.")
        
        val isRsiOversold = rsi < TradingConstants.RSI_OVERSOLD
        val isRsiNearOversold = rsi < TradingConstants.RSI_POTENTIAL
        val isRsiOverbought = rsi > TradingConstants.RSI_OVERBOUGHT
        val isMacdBullish = macdHist > 0.0
        val isPriceAboveSma50 = if (lastPrice != null && sma50 != null) lastPrice > sma50 else true
        val isPriceAboveSma200 = if (lastPrice != null && sma200 != null) lastPrice > sma200 else true
        val isNearLowerBB = if (bb != null && lastPrice != null) lastPrice <= bb.lower * 1.05 else false
        val isNearUpperBB = if (bb != null && lastPrice != null) lastPrice >= bb.upper * 0.95 else false

        val qualityPrefix = if (isFundamentalGood) "⭐ Quality: " else ""

        // 1. SELL PRIORITY: Position Risk & Profit Management
        if (userCost != null && userCost > 0 && lastPrice != null) {
            val netProfitPercent = calculateNetProfitPercent(userCost, lastPrice)
            val quantity = userQuantity ?: 0
            val positionValue = userCost * quantity
            val netProfitBaht = if (quantity > 0) {
                calculateNetProfitBaht(userCost, lastPrice, quantity)
            } else 0.0
            
            var applySwingLogic = true
            
            if (tradePurpose == "DIVIDEND") {
                val yield = dividendYield ?: 0.0
                val isRoeBad = roe != null && roe < TradingConstants.ROE_MIN_THRESHOLD
                
                if (isRoeBad) {
                    return TradeSignal(
                        IndicatorSignal.SELL,
                        "${qualityPrefix}Fundamentals Broke",
                        "Dividend rule broken. Company fundamentals declining (ROE < ${TradingConstants.ROE_MIN_THRESHOLD}%). Re-evaluate holding."
                    )
                }
                
                if (yield >= TradingConstants.DIVIDEND_YIELD_PROTECTION) {
                    // Yield is good, keep it! Do not apply swing logic.
                    applySwingLogic = false
                } else {
                    // Yield dropped below protection threshold.
                    applySwingLogic = true
                }
            }
            
            if (applySwingLogic) {
                // CUT LOSS PRIORITY: Dynamic stop loss.
                // Priority: user override > ATR volatility-adjusted (2×ATR%, clamped) > Market-Cap tier
                val atrStop = atrPercent?.takeIf { it > 0.0 }?.let {
                    -(TradingConstants.ATR_STOP_MULTIPLIER * it)
                        .coerceIn(TradingConstants.ATR_STOP_MIN_PERCENT, TradingConstants.ATR_STOP_MAX_PERCENT)
                }
                val dynamicStopLoss = when {
                    userStopLoss != null && userStopLoss < 0.0 -> userStopLoss // Explicit user percentage override (e.g. -6.0)
                    atrStop != null -> atrStop                                  // Volatility-adjusted (2× daily ATR)
                    isSet50 -> TradingConstants.STOP_LOSS_SET50_PERCENT         // SET50 Large Cap (-4.5%)
                    else -> TradingConstants.STOP_LOSS_MID_SMALL_PERCENT       // Mid/Small-Cap SET (-6.5%)
                }

                if (netProfitPercent < dynamicStopLoss) {
                    val tierLabel = when {
                        userStopLoss != null && userStopLoss < 0.0 -> "Custom"
                        atrStop != null -> "ATR Volatility-Based"
                        isSet50 -> "SET50 Large Cap"
                        else -> "Mid/Small-Cap SET"
                    }
                    val technicalWarning = when {
                        !isPriceAboveSma200 -> "the price has crashed below the long-term trend (SMA 200)"
                        !isPriceAboveSma50 -> "the price has broken below its 50-day average (SMA 50)"
                        else -> "momentum is weakening significantly"
                    }
                    return TradeSignal(
                        IndicatorSignal.SELL,
                        "${qualityPrefix}Stop Loss ($tierLabel ${String.format(Locale.ENGLISH, "%.1f", dynamicStopLoss)}%)",
                        "Warning: Your net loss is ${String.format(Locale.ENGLISH,"%.2f", netProfitPercent)}% (below target ${String.format(Locale.ENGLISH, "%.1f", dynamicStopLoss)}%). Technically, $technicalWarning. Cutting loss prevents further capital erosion."
                    )
                }

                // DYNAMIC TRAILING STOP: Protect profits if price pulled back significantly from peak.
                // Threshold adapts to volatility: 2.5×ATR% (clamped 4–10%), falling back to 5% fixed.
                val highestPeak = maxOf(userCost, peakPrice ?: userCost)
                if (highestPeak > userCost && netProfitPercent > 3.0) {
                    val trailingThreshold = atrPercent?.takeIf { it > 0.0 }?.let {
                        (TradingConstants.ATR_TRAILING_MULTIPLIER * it)
                            .coerceIn(TradingConstants.ATR_TRAILING_MIN_PERCENT, TradingConstants.ATR_TRAILING_MAX_PERCENT)
                    } ?: 5.0
                    val dropFromPeakPercent = ((highestPeak - lastPrice) / highestPeak) * 100
                    if (dropFromPeakPercent >= trailingThreshold) {
                        return TradeSignal(
                            IndicatorSignal.SELL,
                            "${qualityPrefix}Trailing Stop Triggered",
                            "Price dropped ${String.format(Locale.ENGLISH,"%.2f", dropFromPeakPercent)}% from high of ฿${String.format(Locale.ENGLISH,"%.2f", highestPeak)}. Protect gains while profit remains."
                        )
                    }
                }

                // SWING PLAYBOOK: Take Profit / Scale Out
                // Scale threshold relative to position size (minimum 5% or 5% of total position value)
                val minBahtThreshold = if (positionValue > 0) maxOf(TradingConstants.TAKE_PROFIT_MIN_BAHT, positionValue * 0.05) else TradingConstants.TAKE_PROFIT_MIN_BAHT
                val meetsProfitTarget = netProfitPercent > TradingConstants.TAKE_PROFIT_PERCENT || netProfitBaht >= minBahtThreshold

                if (meetsProfitTarget) {
                    // If trend is still strongly bullish (MACD positive and RSI < 70), recommend scaling out / partial profit taking
                    if (isMacdBullish && rsi < 70.0) {
                        return TradeSignal(
                            IndicatorSignal.SELL,
                            "${qualityPrefix}Scale Out Target (+${String.format(Locale.ENGLISH,"%.1f", netProfitPercent)}%)",
                            "Position is up ${String.format(Locale.ENGLISH,"%.2f", netProfitPercent)}% (฿${String.format(Locale.ENGLISH,"%,.2f", netProfitBaht)}). Trend is strong; consider taking partial profits (50%) and trailing the rest."
                        )
                    } else {
                        return TradeSignal(
                            IndicatorSignal.SELL,
                            "${qualityPrefix}Exit Target (Profit Secured)",
                            "Your profit is ${String.format(Locale.ENGLISH,"%.2f", netProfitPercent)}% (฿${String.format(Locale.ENGLISH,"%,.2f", netProfitBaht)}). Momentum is weakening; good level to lock in gains."
                        )
                    }
                }
            }
        }

        // 2. SELL PRIORITY: Technical Overbought (only if already profitable)
        val isProtectedDividend = tradePurpose == "DIVIDEND" && (dividendYield ?: 0.0) >= TradingConstants.DIVIDEND_YIELD_PROTECTION
        val hasProfit = if (userCost != null && userCost > 0 && lastPrice != null) {
            val netProfitPercent = calculateNetProfitPercent(userCost, lastPrice)
            val quantity = userQuantity ?: 0
            val netProfitBaht = if (quantity > 0) calculateNetProfitBaht(userCost, lastPrice, quantity) else 0.0
            netProfitPercent >= TradingConstants.TAKE_PROFIT_PERCENT || netProfitBaht >= TradingConstants.TAKE_PROFIT_MIN_BAHT
        } else {
            false
        }
        if (!isProtectedDividend && hasProfit && isRsiOverbought) {
            return TradeSignal(
                IndicatorSignal.SELL,
                "${qualityPrefix}Overbought",
                "RSI is ${String.format(Locale.ENGLISH,"%.1f", rsi)} (above 65). The stock is overextended and likely to pull back. Consider selling to lock in gains."
            )
        }
        if (!isProtectedDividend && hasProfit && (mfi ?: 0.0) >= TradingConstants.MFI_DISTRIBUTION) {
            return TradeSignal(
                IndicatorSignal.SELL,
                "${qualityPrefix}Distribution Detected",
                "Money Flow Index is ${String.format(Locale.ENGLISH,"%.0f", mfi)} (above 80) — heavy volume is flowing OUT at these prices. Smart money is selling into strength; consider locking in gains."
            )
        }
        if (!isProtectedDividend && hasProfit && isNearUpperBB) {
            return TradeSignal(
                IndicatorSignal.SELL,
                "${qualityPrefix}Upper Band Resistance",
                "Price is near the upper Bollinger Band — a resistance zone. Stocks often pull back from this level. Consider selling or tightening your stop-loss."
            )
        }

        // 3. BUY SIGNALS (Capturing Value and Early Reversal)
        
        // High Conviction: Oversold in Uptrend
        // ADX Knife Guard: ADX ≥ 40 means a violent directional move is in progress —
        // an oversold reading there is a falling knife, not a value dip. Downgrade to watch.
        if (isRsiOversold && isPriceAboveSma200) {
            if (adx != null && adx >= TradingConstants.ADX_STRONG_TREND) {
                return TradeSignal(
                    IndicatorSignal.POTENTIAL,
                    "${qualityPrefix}Falling Knife Guard",
                    "RSI is oversold (${String.format(Locale.ENGLISH,"%.1f", rsi)}) but trend strength is extreme (ADX ${String.format(Locale.ENGLISH,"%.0f", adx)}). The selloff is still violent — wait for it to exhaust before buying."
                )
            }
            // Stochastic Reversal Gate: %K still below %D in the oversold zone means
            // the price is oversold but STILL FALLING. Wait for the %K cross-up.
            if (stochK != null && stochD != null &&
                stochK < TradingConstants.STOCH_OVERSOLD && stochK < stochD) {
                return TradeSignal(
                    IndicatorSignal.POTENTIAL,
                    "${qualityPrefix}Reversal Not Confirmed",
                    "RSI is oversold (${String.format(Locale.ENGLISH,"%.1f", rsi)}) but the Stochastic (%K ${String.format(Locale.ENGLISH,"%.0f", stochK)}) is still falling. Wait for %K to cross above %D before entering."
                )
            }
            val stochConfirm = if (stochK != null && stochD != null && stochK > stochD) {
                " Stochastic has turned up — reversal confirmed."
            } else ""
            return TradeSignal(
                IndicatorSignal.BUY,
                "${qualityPrefix}Oversold Accumulation",
                "RSI is oversold (${String.format(Locale.ENGLISH,"%.1f", rsi)}) while the long-term uptrend (SMA 200) is still intact. High probability value dip.$stochConfirm"
            )
        }

        // Recovery: MACD turns positive near support — volume surge, rising OBV,
        // or MFI capitulation flush confirms real buying interest
        if (isMacdBullish && (isNearLowerBB || isRsiOversold)) {
            val isMfiCapitulation = mfi != null && mfi <= TradingConstants.MFI_CAPITULATION
            return if (isVolumeSurge || obvRising || isMfiCapitulation) {
                val volumeEvidence = when {
                    isVolumeSurge -> "a volume surge"
                    obvRising -> "rising On-Balance Volume (steady accumulation)"
                    else -> "a Money Flow capitulation flush (MFI ≤ 20)"
                }
                TradeSignal(
                    IndicatorSignal.BUY,
                    "${qualityPrefix}Early Recovery (Volume Confirmed)",
                    "Momentum (MACD) has turned positive at a major support level, confirmed by $volumeEvidence — strong sign of institutional buying."
                )
            } else {
                TradeSignal(
                    IndicatorSignal.POTENTIAL,
                    "${qualityPrefix}Early Recovery Watch",
                    "Momentum (MACD) has turned positive near support, but volume is quiet. Wait for a volume surge to confirm the reversal."
                )
            }
        }

        // Uptrend Entry: RSI < 55 gate avoids chasing extended moves late in the trend.
        // ADX Chop Filter: below 20 there is no trend to follow — momentum crossovers
        // in a sideways range are whipsaw noise, so require ADX ≥ 20 (null-tolerant).
        if (isMacdBullish && isPriceAboveSma50 && rsi < 55.0 &&
            (adx == null || adx >= TradingConstants.ADX_TREND_CONFIRM)) {
            return TradeSignal(
                IndicatorSignal.BUY,
                "${qualityPrefix}Healthy Momentum",
                "Positive momentum confirmed early. The stock is above its short-term average and not yet overextended (RSI < 55)."
            )
        }

        // 4. POTENTIAL SIGNALS (Awareness Alerts)
        
        // Potential: Near Oversold
        if (isRsiNearOversold) {
            return TradeSignal(
                IndicatorSignal.POTENTIAL,
                "${qualityPrefix}Nearing Value Zone",
                "Stock is becoming cheap (RSI < 42). Keep a close watch for a momentum shift (MACD Green) or lower support bounce."
            )
        }

        // Potential: Support Testing
        if (isNearLowerBB) {
            return TradeSignal(
                IndicatorSignal.POTENTIAL,
                "${qualityPrefix}Support Testing",
                "Price is testing the Lower Bollinger Band. If support holds and momentum improves, this could be an early entry point."
            )
        }

        // Potential: Contrarian / Bearish Oversold
        if (isRsiOversold) {
            return TradeSignal(
                IndicatorSignal.POTENTIAL,
                "${qualityPrefix}Contrarian Watch",
                "Stock is very cheap (${String.format(Locale.ENGLISH,"%.1f", rsi)}) but currently in a bear trend (Below SMA 200). Watch for a short-term bounce, but be careful."
            )
        }

        // 5. NEUTRAL/WEAK TREND (Fallback)
        // Anti-whipsaw: only SELL a holding on weak trend once real damage shows (net loss > 2%).
        // A flat or slightly-profitable position gets a NEUTRAL warning instead of an exit signal.
        if (!isMacdBullish && !isPriceAboveSma50) {
            val holdingNetProfit = if (userCost != null && userCost > 0 && lastPrice != null) {
                calculateNetProfitPercent(userCost, lastPrice)
            } else null
            return if (holdingNetProfit != null && holdingNetProfit < -2.0) {
                TradeSignal(
                    IndicatorSignal.SELL,
                    "${qualityPrefix}Weak Trend",
                    "The stock is losing momentum, trading below its average, and your position is down ${String.format(Locale.ENGLISH, "%.2f", holdingNetProfit)}%. Likely to continue falling."
                )
            } else if (holdingNetProfit != null) {
                TradeSignal(
                    IndicatorSignal.NEUTRAL,
                    "${qualityPrefix}Trend Weakening",
                    "Momentum is fading but your position is holding up. Watch closely — a net loss beyond 2% will trigger an exit signal."
                )
            } else {
                TradeSignal(
                    IndicatorSignal.NEUTRAL,
                    "Downward Trend",
                    "This stock is trending downward with low momentum. Not a good time to enter."
                )
            }
        }
        
        return TradeSignal(IndicatorSignal.NEUTRAL, "Wait & Watch", "No strong signals right now. It's safer to wait for a clearer entry point.")
    }

    fun calculateNetProfitPercent(cost: Double, currentPrice: Double): Double {
        val buyFee = calculateFees(cost, false)
        val sellFee = calculateFees(currentPrice, true)
        val netCost = cost + buyFee
        val netSell = currentPrice - sellFee
        return ((netSell - netCost) / netCost) * 100
    }

    fun calculateNetProfitBaht(cost: Double, currentPrice: Double, quantity: Int): Double {
        if (quantity <= 0) return 0.0
        val totalCostRaw = cost * quantity
        val sellValueRaw = currentPrice * quantity
        val buyFee = calculateFees(totalCostRaw, false)
        val sellFee = calculateFees(sellValueRaw, true)
        return (sellValueRaw - sellFee) - (totalCostRaw + buyFee)
    }

    fun calculateSMA(prices: List<Double>, period: Int): Double? {
        if (prices.size < period) return null
        return prices.takeLast(period).average()
    }

    fun calculateBollingerBands(prices: List<Double>, period: Int = 20, stdDevMultiplier: Double = 2.0): BollingerBands? {
        if (prices.size < period) return null
        
        val lastPrices = prices.takeLast(period)
        val sma = lastPrices.average()
        
        val variance = lastPrices.map { (it - sma) * (it - sma) }.average()
        val stdDev = sqrt(variance)
        
        return BollingerBands(
            upper = sma + (stdDevMultiplier * stdDev),
            middle = sma,
            lower = sma - (stdDevMultiplier * stdDev)
        )
    }

    /**
     * OBV (On-Balance Volume): cumulative volume flow. Adds volume on up days,
     * subtracts on down days. A rising OBV confirms that volume is backing the
     * price move (accumulation); a flat/falling OBV flags weak rallies.
     */
    fun calculateOBV(prices: List<Double>, volumes: List<Long>): List<Double> {
        if (prices.size < 2 || prices.size != volumes.size) return emptyList()
        val obv = mutableListOf(0.0)
        for (i in 1 until prices.size) {
            val prev = obv.last()
            obv.add(
                when {
                    prices[i] > prices[i - 1] -> prev + volumes[i]
                    prices[i] < prices[i - 1] -> prev - volumes[i]
                    else -> prev
                }
            )
        }
        return obv
    }

    /** OBV trend check: current OBV above its [period]-day average = accumulation. */
    fun isObvRising(prices: List<Double>, volumes: List<Long>, period: Int = 10): Boolean {
        val obv = calculateOBV(prices, volumes)
        if (obv.size < period + 1) return false
        val avg = obv.dropLast(1).takeLast(period).average()
        return obv.last() > avg
    }

    /** 52-week low/high from up to ~252 trading days of history. */
    fun calculate52WeekRange(prices: List<Double>): Pair<Double, Double>? {
        if (prices.size < 60) return null // need a meaningful window
        val window = prices.takeLast(252)
        return Pair(window.min(), window.max())
    }

    /**
     * Relative Strength vs benchmark index (classic O'Neil-style RS):
     * stock return minus index return over [days] trading days (default 63 ≈ 3 months).
     * Positive = outperforming the market.
     */
    fun calculateRelativeStrength(
        stockPrices: List<Double>,
        indexPrices: List<Double>,
        days: Int = 63
    ): Double? {
        if (stockPrices.size < days + 1 || indexPrices.size < days + 1) return null
        val stockPast = stockPrices[stockPrices.size - 1 - days]
        val indexPast = indexPrices[indexPrices.size - 1 - days]
        if (stockPast <= 0.0 || indexPast <= 0.0) return null
        val stockReturn = (stockPrices.last() - stockPast) / stockPast * 100
        val indexReturn = (indexPrices.last() - indexPast) / indexPast * 100
        return stockReturn - indexReturn
    }

    /**
     * ATR (Average True Range, Wilder smoothing): average daily price range
     * including gaps. Used for volatility-adjusted stop-loss and trailing stops —
     * a 2×ATR stop gives volatile stocks room to breathe while keeping tight
     * stops on calm large-caps.
     */
    fun calculateATR(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        period: Int = 14
    ): Double? {
        val n = closes.size
        if (n < period + 1 || highs.size != n || lows.size != n) return null
        var atr = 0.0
        // Seed with simple average of the first `period` true ranges
        for (i in 1..period) {
            atr += trueRange(highs[i], lows[i], closes[i - 1])
        }
        atr /= period
        // Wilder smoothing for the rest
        for (i in period + 1 until n) {
            atr = (atr * (period - 1) + trueRange(highs[i], lows[i], closes[i - 1])) / period
        }
        return atr
    }

    private fun trueRange(high: Double, low: Double, prevClose: Double): Double =
        maxOf(high - low, abs(high - prevClose), abs(low - prevClose))

    /**
     * ADX (Average Directional Index, Wilder): trend-strength regime filter.
     * < 20 = choppy range, > 25 = trending, > 40 = very strong (often violent) trend.
     */
    fun calculateADX(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        period: Int = 14
    ): Double? {
        val n = closes.size
        if (n < period * 2 + 1 || highs.size != n || lows.size != n) return null

        var smTr = 0.0
        var smPlusDm = 0.0
        var smMinusDm = 0.0
        val dxValues = mutableListOf<Double>()

        for (i in 1 until n) {
            val upMove = highs[i] - highs[i - 1]
            val downMove = lows[i - 1] - lows[i]
            val plusDm = if (upMove > downMove && upMove > 0) upMove else 0.0
            val minusDm = if (downMove > upMove && downMove > 0) downMove else 0.0
            val tr = trueRange(highs[i], lows[i], closes[i - 1])

            if (i <= period) {
                smTr += tr
                smPlusDm += plusDm
                smMinusDm += minusDm
                if (i < period) continue
            } else {
                // Wilder smoothing
                smTr = smTr - smTr / period + tr
                smPlusDm = smPlusDm - smPlusDm / period + plusDm
                smMinusDm = smMinusDm - smMinusDm / period + minusDm
            }

            if (smTr == 0.0) { dxValues.add(0.0); continue }
            val plusDi = 100.0 * smPlusDm / smTr
            val minusDi = 100.0 * smMinusDm / smTr
            val diSum = plusDi + minusDi
            dxValues.add(if (diSum == 0.0) 0.0 else 100.0 * abs(plusDi - minusDi) / diSum)
        }

        if (dxValues.size < period) return null
        var adx = dxValues.take(period).average()
        for (i in period until dxValues.size) {
            adx = (adx * (period - 1) + dxValues[i]) / period
        }
        return adx
    }

    /**
     * Slow Stochastic Oscillator (14,3,3): where the close sits within the recent
     * high-low range. Returns Pair(%K, %D). %K crossing above %D from below 20
     * confirms an oversold reversal has actually started — cuts falling-knife entries.
     */
    fun calculateStochastic(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        kPeriod: Int = 14,
        kSmooth: Int = 3,
        dPeriod: Int = 3
    ): Pair<Double, Double>? {
        val n = closes.size
        val required = kPeriod + kSmooth + dPeriod - 2
        if (n < required || highs.size != n || lows.size != n) return null

        val fastK = mutableListOf<Double>()
        for (i in kPeriod - 1 until n) {
            var hh = highs[i]
            var ll = lows[i]
            for (j in i - kPeriod + 1..i) {
                if (highs[j] > hh) hh = highs[j]
                if (lows[j] < ll) ll = lows[j]
            }
            fastK.add(if (hh == ll) 50.0 else 100.0 * (closes[i] - ll) / (hh - ll))
        }

        // Slow %K = SMA(kSmooth) of fast %K; %D = SMA(dPeriod) of slow %K
        val slowK = fastK.windowed(kSmooth) { it.average() }
        if (slowK.size < dPeriod) return null
        val d = slowK.takeLast(dPeriod).average()
        return Pair(slowK.last(), d)
    }

    /**
     * MFI (Money Flow Index, 14): volume-weighted RSI on typical price.
     * > 80 = distribution (overbought with real selling volume),
     * < 20 = capitulation (oversold with heavy volume).
     */
    fun calculateMFI(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        volumes: List<Long>,
        period: Int = 14
    ): Double? {
        val n = closes.size
        if (n < period + 1 || highs.size != n || lows.size != n || volumes.size != n) return null

        var positiveFlow = 0.0
        var negativeFlow = 0.0
        var prevTp = (highs[n - period - 1] + lows[n - period - 1] + closes[n - period - 1]) / 3.0
        for (i in n - period until n) {
            val tp = (highs[i] + lows[i] + closes[i]) / 3.0
            val flow = tp * volumes[i]
            if (tp > prevTp) positiveFlow += flow
            else if (tp < prevTp) negativeFlow += flow
            prevTp = tp
        }

        if (negativeFlow == 0.0) return 100.0
        val ratio = positiveFlow / negativeFlow
        return 100.0 - (100.0 / (1.0 + ratio))
    }

    fun isVolumeSurge(volumes: List<Long>, period: Int = 10): Boolean {
        if (volumes.size < period + 1) return false
        val currentVolume = volumes.last()
        val avgVolume = volumes.dropLast(1).takeLast(period).average()
        return currentVolume > (avgVolume * 2.0) // 2x Average
    }

    fun calculateRSI(prices: List<Double>, period: Int = 14): Double? {
        if (prices.size < period + 1) return null
        
        val changes = prices.zipWithNext { a, b -> b - a }
        var avgGain = changes.take(period).filter { it > 0 }.sum() / period
        var avgLoss = abs(changes.take(period).filter { it < 0 }.sum()) / period

        for (i in period until changes.size) {
            val change = changes[i]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0
            
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    fun estimatePriceForRSI(prices: List<Double>, targetRsi: Double, period: Int = 14): Double? {
        if (prices.size < period) return null
        
        val changes = prices.zipWithNext { a, b -> b - a }
        var avgGain = changes.take(period).filter { it > 0 }.sum() / period
        var avgLoss = abs(changes.take(period).filter { it < 0 }.sum()) / period

        for (i in period until changes.size) {
            val change = changes[i]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0
            
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        val targetRS = if (targetRsi >= 100.0) 999.0 else targetRsi / (100.0 - targetRsi)
        val lastPrice = prices.last()

        val gainNeeded = (targetRS * avgLoss * (period - 1)) - (avgGain * (period - 1))
        if (gainNeeded > 0) return lastPrice + gainNeeded

        val lossNeeded = (avgGain * (period - 1) / targetRS) - (avgLoss * (period - 1))
        if (lossNeeded > 0) return lastPrice - lossNeeded

        return lastPrice
    }

    fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
        if (prices.isEmpty()) return emptyList()
        val ema = mutableListOf<Double>()
        val multiplier = 2.0 / (period + 1)
        
        var currentEma = prices.take(period).average()
        ema.add(currentEma)

        for (i in period until prices.size) {
            currentEma = (prices[i] - currentEma) * multiplier + currentEma
            ema.add(currentEma)
        }
        return ema
    }

    fun calculateMACD(
        prices: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Triple<Double?, Double?, Double?> {
        if (prices.size < slowPeriod + signalPeriod) return Triple(null, null, null)

        val fastEma = calculateEMA(prices, fastPeriod)
        val slowEma = calculateEMA(prices, slowPeriod)

        val offset = slowPeriod - fastPeriod
        val macdLine = fastEma.drop(offset).zip(slowEma) { f, s -> f - s }
        
        val signalLine = calculateEMA(macdLine, signalPeriod)
        
        val currentMacd = macdLine.lastOrNull()
        val currentSignal = signalLine.lastOrNull()
        val currentHist = if (currentMacd != null && currentSignal != null) {
            currentMacd - currentSignal
        } else null

        return Triple(currentMacd, currentSignal, currentHist)
    }
}
