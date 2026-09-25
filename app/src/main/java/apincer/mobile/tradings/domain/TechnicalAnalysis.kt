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
    val mfi: Double? = null,
    val nvdrNetVolume: Double? = null,
    val nvdrNetValue: Double? = null,
    val weeklyTrendBullish: Boolean? = null,
    val observationDate: String? = null,
    val benchmarkDate: String? = null
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
        userStopLoss: Double? = null,
        relativeStrength: Double? = null,
        nvdrNetVolume: Double? = null,
        nvdrNetValue: Double? = null,
        isNearXdDate: Boolean = false,
        isWeeklyTrendBullish: Boolean? = null,
        userBuyFees: Double? = null,
        atsEnabled: Boolean = true
    ): TradeSignal {
        fun positionProfitPercent(cost: Double, price: Double): Double =
            if (userQuantity != null && userQuantity > 0 && userBuyFees != null)
                calculatePositionNetProfitPercent(cost, price, userQuantity, userBuyFees, atsEnabled)
            else calculateNetProfitPercent(cost, price)
        fun positionProfitBaht(cost: Double, price: Double, quantity: Int): Double =
            if (userBuyFees != null)
                calculatePositionNetProfitBaht(cost, price, quantity, userBuyFees, atsEnabled)
            else calculateNetProfitBaht(cost, price, quantity)
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
            val netProfitPercent = positionProfitPercent(userCost, lastPrice)
            val quantity = userQuantity ?: 0
            val positionValue = userCost * quantity
            val netProfitBaht = if (quantity > 0) {
                positionProfitBaht(userCost, lastPrice, quantity)
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

                // EARLY BREAKDOWN WARNING: Position in loss (-1.5% to dynamic stop) and momentum breaks below SMA50 with negative MACD
                // Suppressed if within 2 days of XD date (price drop is cash dividend adjustment)
                if (!isNearXdDate && netProfitPercent <= -1.5 && !isMacdBullish && !isPriceAboveSma50) {
                    return TradeSignal(
                        IndicatorSignal.SELL,
                        "${qualityPrefix}Early Breakdown Warning (${String.format(Locale.ENGLISH, "%.1f", netProfitPercent)}%)",
                        "Price broke below SMA 50 and momentum collapsed while position is down ${String.format(Locale.ENGLISH, "%.2f", netProfitPercent)}%. Consider cutting early to prevent full stop-loss."
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
            val netProfitPercent = positionProfitPercent(userCost, lastPrice)
            val quantity = userQuantity ?: 0
            val netProfitBaht = if (quantity > 0) positionProfitBaht(userCost, lastPrice, quantity) else 0.0
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
            
            // FALSE BREAKOUT GUARD: Foreign funds heavily dumping (NVDR net selling > ฿5M)
            if (nvdrNetVolume != null && nvdrNetVolume < 0 && (nvdrNetValue ?: 0.0) < -5_000_000.0) {
                return TradeSignal(
                    IndicatorSignal.POTENTIAL,
                    "${qualityPrefix}False Breakout Guard",
                    "Momentum is positive, but foreign funds are heavily net selling (NVDR ฿${String.format(Locale.ENGLISH, "%,.0f", nvdrNetValue)}). Wait for institutional selling pressure to clear before buying."
                )
            }

            // VOLUME & RELATIVE STRENGTH CONFIRMATION: If lagging broad market and volume is dry, downgrade to watch
            if (!isVolumeSurge && !obvRising && relativeStrength != null && relativeStrength < -2.0) {
                return TradeSignal(
                    IndicatorSignal.POTENTIAL,
                    "${qualityPrefix}Breakout Volume Guard",
                    "Momentum is positive but lacks volume confirmation while lagging the SET index. Wait for a volume surge to confirm institutional participation."
                )
            }

            // MULTI-TIMEFRAME (MTF) MACRO GUARD:
            if (isWeeklyTrendBullish == false) {
                return TradeSignal(
                    IndicatorSignal.POTENTIAL,
                    "${qualityPrefix}Macro Weekly Bearish Guard",
                    "Daily momentum is positive, but the macro weekly trend is bearish (Price < Weekly EMA 20). Counter-trend entries carry higher whipsaw risk."
                )
            }

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
                positionProfitPercent(userCost, lastPrice)
            } else null
            return if (!isNearXdDate && holdingNetProfit != null && holdingNetProfit < -2.0) {
                TradeSignal(
                    IndicatorSignal.SELL,
                    "${qualityPrefix}Weak Trend",
                    "The stock is losing momentum, trading below its average, and your position is down ${String.format(Locale.ENGLISH, "%.2f", holdingNetProfit)}%. Likely to continue falling."
                )
            } else if (isNearXdDate) {
                TradeSignal(
                    IndicatorSignal.NEUTRAL,
                    "${qualityPrefix}Ex-Dividend Grace Period",
                    "Price dip reflects XD cash dividend adjustment. Trend breakdown sell signals are paused around XD date."
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

    /**
     * Position sizing: given account equity, the % of that equity you're willing
     * to risk on this ONE trade, and the per-share risk (entry - stop-loss), returns
     * the quantity of shares to buy so a stop-out loses at most that fixed % of equity —
     * regardless of how volatile/expensive the stock is. This is what turns a %-based
     * stop-loss into an actual, comparable dollar-risk across different trades.
     */
    fun calculateSuggestedQuantity(
        accountEquity: Double,
        riskPercent: Double,
        riskPerShare: Double
    ): Int {
        if (accountEquity <= 0 || riskPercent <= 0 || riskPerShare <= 0) return 0
        val riskBudget = accountEquity * (riskPercent / 100.0)
        return (riskBudget / riskPerShare).toInt().coerceAtLeast(0)
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

    fun calculatePositionNetProfitBaht(
        cost: Double,
        currentPrice: Double,
        quantity: Int,
        actualBuyFees: Double,
        atsEnabled: Boolean
    ): Double {
        if (quantity <= 0 || cost <= 0.0 || currentPrice <= 0.0) return 0.0
        val saleValue = currentPrice * quantity
        return saleValue - calculateFees(saleValue, true, atsEnabled) -
            (cost * quantity + actualBuyFees)
    }

    fun calculatePositionNetProfitPercent(
        cost: Double,
        currentPrice: Double,
        quantity: Int,
        actualBuyFees: Double,
        atsEnabled: Boolean
    ): Double {
        val invested = cost * quantity + actualBuyFees
        if (invested <= 0.0) return 0.0
        return calculatePositionNetProfitBaht(cost, currentPrice, quantity,
            actualBuyFees, atsEnabled) / invested * 100.0
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

    /** Compare returns over the same SET sessions; missing endpoint bars leave RS unknown. */
    fun calculateRelativeStrengthOnDates(
        stockBars: List<Pair<String, Double>>,
        indexBars: List<Pair<String, Double>>,
        days: Int = 63
    ): Double? {
        if (days <= 0) return null
        val benchmark = indexBars.mapNotNull { (date, price) ->
            val parsed = runCatching { java.time.LocalDate.parse(date) }.getOrNull()
            if (parsed == null || !price.isFinite() || price <= 0.0) null else parsed to price
        }.sortedBy { it.first }
        if (benchmark.size < days + 1) return null
        val stocksByDate = stockBars.mapNotNull { (date, price) ->
            val parsed = runCatching { java.time.LocalDate.parse(date) }.getOrNull()
            if (parsed == null || !price.isFinite() || price <= 0.0) null else parsed to price
        }.toMap()
        val (latestDate, latestIndex) = benchmark.last()
        val (pastDate, pastIndex) = benchmark[benchmark.size - 1 - days]
        val latestStock = stocksByDate[latestDate] ?: return null
        val pastStock = stocksByDate[pastDate] ?: return null
        return ((latestStock - pastStock) / pastStock -
            (latestIndex - pastIndex) / pastIndex) * 100.0
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

    enum class MarketRegime(val label: String, val isBullish: Boolean) {
        BULLISH("Bullish Trend", true),
        NEUTRAL("Consolidation", true),
        BEARISH("Bear / Correction", false)
    }

    fun calculateSuggestedStopLossPrice(
        lastPrice: Double, 
        atr: Double? = null, 
        isSet50: Boolean = false, 
        customStopLossPercent: Double? = null
    ): Double {
        if (lastPrice <= 0.0) return 0.0
        val atrStop = atr?.takeIf { it > 0.0 }?.let {
            -(TradingConstants.ATR_STOP_MULTIPLIER * (it / lastPrice * 100))
                .coerceIn(TradingConstants.ATR_STOP_MIN_PERCENT, TradingConstants.ATR_STOP_MAX_PERCENT)
        }
        val stopPercent = customStopLossPercent?.takeIf { it < 0.0 }
            ?: atrStop
            ?: if (isSet50) TradingConstants.STOP_LOSS_SET50_PERCENT else TradingConstants.STOP_LOSS_MID_SMALL_PERCENT
        return lastPrice * (1.0 + stopPercent / 100.0)
    }

    fun calculateSuggestedTargetPrice(
        lastPrice: Double, 
        stopLossPrice: Double? = null,
        minTargetPercent: Double = 10.0
    ): Double {
        if (lastPrice <= 0.0) return 0.0
        if (stopLossPrice != null && stopLossPrice < lastPrice) {
            val risk = lastPrice - stopLossPrice
            val minReward = lastPrice * (minTargetPercent / 100.0)
            val reward = maxOf(minReward, risk * 2.0)
            return lastPrice + reward
        }
        return lastPrice * (1.0 + minTargetPercent / 100.0)
    }

    fun calculateRiskRewardRatio(
        entryPrice: Double, 
        targetPrice: Double, 
        stopLossPrice: Double
    ): Double? {
        val reward = targetPrice - entryPrice
        val risk = entryPrice - stopLossPrice
        if (risk <= 0.0 || reward <= 0.0) return null
        return reward / risk
    }

    fun getMarketRegime(setIndexCloses: List<Double>): MarketRegime {
        if (setIndexCloses.size < 50) return MarketRegime.NEUTRAL
        val sma50 = calculateSMA(setIndexCloses, 50) ?: return MarketRegime.NEUTRAL
        val current = setIndexCloses.lastOrNull() ?: return MarketRegime.NEUTRAL
        val macdData = calculateMACD(setIndexCloses)
        val macdHist = macdData.third ?: 0.0
        return when {
            current >= sma50 && macdHist >= 0.0 -> MarketRegime.BULLISH
            current >= sma50 -> MarketRegime.NEUTRAL
            else -> MarketRegime.BEARISH
        }
    }

    data class SpendableCashBreakdown(
        val totalAssets: Double,
        val cashBalance: Double,
        val regime: MarketRegime,
        val recommendedBufferPercent: Double,
        val targetReserveBaht: Double,
        val spendableCashBaht: Double,
        val isDeficit: Boolean
    )

    fun getRecommendedCashBufferPercent(regime: MarketRegime): Double = when (regime) {
        MarketRegime.BULLISH -> 15.0
        MarketRegime.NEUTRAL -> 30.0
        MarketRegime.BEARISH -> 50.0
    }

    fun calculateSpendableCash(
        totalAssets: Double,
        cashBalance: Double,
        regime: MarketRegime
    ): SpendableCashBreakdown {
        val bufferPercent = getRecommendedCashBufferPercent(regime)
        val baseCapital = maxOf(totalAssets, cashBalance)
        val targetReserve = baseCapital * (bufferPercent / 100.0)
        val spendable = maxOf(0.0, cashBalance - targetReserve)
        val isDeficit = cashBalance < targetReserve
        return SpendableCashBreakdown(
            totalAssets = totalAssets,
            cashBalance = cashBalance,
            regime = regime,
            recommendedBufferPercent = bufferPercent,
            targetReserveBaht = targetReserve,
            spendableCashBaht = spendable,
            isDeficit = isDeficit
        )
    }


    fun isNearExDividendDate(dividendDateStr: String?): Boolean {
        if (dividendDateStr.isNullOrBlank()) return false
        return try {
            val formats = listOf(
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH),
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ENGLISH),
                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH)
            )
            val parsedDate = formats.firstNotNullOfOrNull { fmt ->
                try { fmt.parse(dividendDateStr.trim()) } catch (_: Exception) { null }
            } ?: return false
            val now = java.util.Calendar.getInstance().timeInMillis
            val diffDays = java.lang.Math.abs(now - parsedDate.time) / (1000 * 60 * 60 * 24)
            diffDays <= 2
        } catch (_: Exception) {
            false
        }
    }

    // ==========================================
    // Multi-Timeframe (MTF) Macro Resampling
    // ==========================================

    fun resampleToWeeklyCloses(dailyPrices: List<Double>): List<Double> {
        if (dailyPrices.isEmpty()) return emptyList()
        // Chunk daily closes into weekly bars (5 trading days ≈ 1 week)
        return dailyPrices.chunked(5) { it.last() }
    }

    fun isWeeklyMacroBullish(dailyCloses: List<Double>, weeklyPeriod: Int = TradingConstants.WEEKLY_EMA_PERIOD): Boolean? {
        val weeklyCloses = resampleToWeeklyCloses(dailyCloses)
        if (weeklyCloses.size < weeklyPeriod) return null
        val weeklyEma = calculateEMA(weeklyCloses, weeklyPeriod).lastOrNull() ?: return null
        val currentPrice = dailyCloses.lastOrNull() ?: return null
        return currentPrice >= weeklyEma
    }

    /** Weekly EMA based on completed ISO calendar weeks, including holiday-shortened weeks. */
    fun isWeeklyTrendBullishOnDate(
        datedCloses: List<Pair<String, Double>>,
        asOfDate: String,
        currentPrice: Double,
        weeklyPeriod: Int = TradingConstants.WEEKLY_EMA_PERIOD
    ): Boolean? {
        if (currentPrice <= 0.0 || weeklyPeriod <= 0) return null
        val asOf = runCatching { java.time.LocalDate.parse(asOfDate) }.getOrNull() ?: return null
        val weekStart = asOf.with(java.time.DayOfWeek.MONDAY)
        val completed = datedCloses.mapNotNull { (date, close) ->
            val day = runCatching { java.time.LocalDate.parse(date) }.getOrNull()
            if (day == null || !day.isBefore(weekStart) || close <= 0.0) null else day to close
        }.sortedBy { it.first }
            .groupBy { it.first.with(java.time.DayOfWeek.MONDAY) }
            .values.map { week -> week.last().second }
        if (completed.size < weeklyPeriod) return null
        val ema = calculateEMA(completed, weeklyPeriod).lastOrNull() ?: return null
        return currentPrice >= ema
    }

    // ==========================================
    // Portfolio Risk Concentration & Sector Cap
    // ==========================================

    data class SectorExposure(
        val sector: String,
        val marketValue: Double,
        val portfolioPercent: Double,
        val isOverexposed: Boolean
    )

    fun calculateSectorExposures(
        holdings: List<Pair<String, Double>>, // Pair(sector, marketValue)
        cashBalance: Double,
        maxSectorPercent: Double = TradingConstants.MAX_SECTOR_ALLOCATION_PERCENT
    ): List<SectorExposure> {
        val totalAssets = holdings.sumOf { it.second } + maxOf(cashBalance, 0.0)
        if (totalAssets <= 0.0) return emptyList()

        val grouped = holdings.groupBy({ it.first.ifBlank { "Other" } }, { it.second })
        return grouped.map { (sector, values) ->
            val sectorValue = values.sum()
            val percent = (sectorValue / totalAssets) * 100.0
            SectorExposure(
                sector = sector,
                marketValue = sectorValue,
                portfolioPercent = percent,
                isOverexposed = percent > maxSectorPercent
            )
        }.sortedByDescending { it.marketValue }
    }

    // ==========================================
    // Thai Dividend Tax Shield & Net YoC
    // ==========================================

    /**
     * Thai Dividend Tax Credit (Section 47 bis, Revenue Code):
     * Tax Credit = Gross Dividend * (CIT_rate / (100 - CIT_rate))
     * Where Gross Dividend = Net Dividend Received / (1 - WHT_rate)
     */
    fun calculateThaiDividendTaxCredit(
        netDividendReceived: Double,
        citRate: Double = TradingConstants.DEFAULT_CIT_TAX_RATE,
        whtRate: Double = TradingConstants.THAI_DIVIDEND_WHT_RATE
    ): Double {
        if (netDividendReceived <= 0.0 || citRate <= 0.0 || citRate >= 100.0) return 0.0
        val grossDividend = netDividendReceived / (1.0 - (whtRate / 100.0))
        val creditMultiplier = citRate / (100.0 - citRate)
        return grossDividend * creditMultiplier
    }

    /**
     * Net Yield-on-Cost (YoC) after standard 10% withholding tax.
     */
    fun calculateNetYieldOnCost(
        annualDps: Double,
        avgCost: Double,
        whtRate: Double = TradingConstants.THAI_DIVIDEND_WHT_RATE
    ): Double {
        if (avgCost <= 0.0 || annualDps <= 0.0) return 0.0
        val netDps = annualDps * (1.0 - (whtRate / 100.0))
        return (netDps / avgCost) * 100.0
    }

    // ==========================================
    // Portfolio & Single Stock Beta vs SET Index
    // ==========================================

    /**
     * Beta of stock vs benchmark index over N trading days:
     * Beta = Cov(R_stock, R_index) / Var(R_index)
     */
    fun calculateBeta(
        stockPrices: List<Double>,
        indexPrices: List<Double>,
        days: Int = 63
    ): Double? {
        if (stockPrices.size < days + 1 || indexPrices.size < days + 1) return null
        val stockSub = stockPrices.takeLast(days + 1)
        val indexSub = indexPrices.takeLast(days + 1)

        val stockReturns = stockSub.zipWithNext { a, b -> if (a > 0.0) (b - a) / a else 0.0 }
        val indexReturns = indexSub.zipWithNext { a, b -> if (a > 0.0) (b - a) / a else 0.0 }

        val meanStock = stockReturns.average()
        val meanIndex = indexReturns.average()

        var covariance = 0.0
        var varianceIndex = 0.0
        for (i in stockReturns.indices) {
            val diffStock = stockReturns[i] - meanStock
            val diffIndex = indexReturns[i] - meanIndex
            covariance += diffStock * diffIndex
            varianceIndex += diffIndex * diffIndex
        }

        if (varianceIndex <= 1e-12) return null
        return covariance / varianceIndex
    }

    /**
     * Weighted Portfolio Beta: sum(weight_i * beta_i) / sum(weight_i)
     */
    fun calculatePortfolioBeta(weightedBetas: List<Pair<Double, Double>>): Double {
        val totalWeight = weightedBetas.sumOf { it.first }
        if (totalWeight <= 0.0) return 1.0
        val weightedSum = weightedBetas.sumOf { (weight, beta) -> weight * beta }
        return weightedSum / totalWeight
    }

    // ==========================================
    // Quantitative Risk Management & CRO Models
    // ==========================================

    data class PositionSizeRecommendation(
        val shares: Int,
        val totalCapital: Double,
        val totalRiskBaht: Double,
        val riskPercent: Double,
        val isCappedByMaxStockLimit: Boolean
    )

    /**
     * Fixed-fractional anti-ruin position sizing:
     * Max Risk Baht = Total Assets * Risk%
     * Per Share Risk = Entry Price - Stop Loss Price
     * Raw Shares = Max Risk Baht / Per Share Risk
     * Rounded down to nearest 100 SET board lot and capped by max single-stock allocation limit (15%).
     */
    fun calculateRecommendedPositionSize(
        totalAssets: Double,
        entryPrice: Double,
        stopLossPrice: Double,
        riskPercent: Double = 1.5,
        maxStockAllocationPercent: Double = TradingConstants.MAX_SINGLE_STOCK_ALLOCATION_PERCENT
    ): PositionSizeRecommendation {
        if (totalAssets <= 0.0 || entryPrice <= 0.0 || stopLossPrice >= entryPrice || stopLossPrice <= 0.0) {
            return PositionSizeRecommendation(
                shares = 0,
                totalCapital = 0.0,
                totalRiskBaht = 0.0,
                riskPercent = 0.0,
                isCappedByMaxStockLimit = false
            )
        }

        val maxRiskBaht = totalAssets * (riskPercent / 100.0)
        val perShareRisk = entryPrice - stopLossPrice
        val rawShares = (maxRiskBaht / perShareRisk).toInt()

        // Single stock capital ceiling
        val maxStockCapital = totalAssets * (maxStockAllocationPercent / 100.0)
        val maxStockShares = (maxStockCapital / entryPrice).toInt()

        val isCapped = rawShares > maxStockShares
        val finalSharesUnrounded = if (isCapped) maxStockShares else rawShares

        // Round down to SET board lot (100 shares)
        val boardLotShares = (finalSharesUnrounded / 100) * 100
        val finalShares = boardLotShares.coerceAtLeast(0)

        val totalCapital = finalShares * entryPrice
        val totalRiskBaht = finalShares * perShareRisk
        val effectiveRiskPercent = if (totalAssets > 0.0) (totalRiskBaht / totalAssets) * 100.0 else 0.0

        return PositionSizeRecommendation(
            shares = finalShares,
            totalCapital = totalCapital,
            totalRiskBaht = totalRiskBaht,
            riskPercent = effectiveRiskPercent,
            isCappedByMaxStockLimit = isCapped
        )
    }

    data class DrawdownResult(
        val maxDrawdownPercent: Double,
        val currentDrawdownPercent: Double,
        val highWaterMark: Double
    )

    /**
     * Max Drawdown (MDD) and current drawdown from High Water Mark (HWM)
     */
    fun calculateMaxDrawdown(equitySeries: List<Double>): DrawdownResult {
        if (equitySeries.isEmpty()) {
            return DrawdownResult(0.0, 0.0, 0.0)
        }

        var hwm = equitySeries.first()
        var maxDd = 0.0
        var currentDd = 0.0

        for (equity in equitySeries) {
            if (equity > hwm) {
                hwm = equity
            }
            val dd = if (hwm > 0.0) ((hwm - equity) / hwm) * 100.0 else 0.0
            if (dd > maxDd) {
                maxDd = dd
            }
            currentDd = dd
        }

        return DrawdownResult(
            maxDrawdownPercent = maxDd,
            currentDrawdownPercent = currentDd,
            highWaterMark = hwm
        )
    }

    /**
     * Constructs a rolling daily return time series for the portfolio across N trading days (default 63).
     * For each trading day t, the portfolio return is the weighted average of individual holding daily returns:
     * R_p,t = sum_i (w_i * R_i,t) / sum_i(w_i)
     * This forms the empirical return distribution for Historical Value at Risk (VaR).
     */
    fun calculatePortfolioHistoricalReturns(
        holdingsWithPrices: List<Pair<Double, List<Double>>>, // Pair(marketValueBaht, historicalClosePrices)
        days: Int = 63
    ): List<Double> {
        if (holdingsWithPrices.isEmpty()) return emptyList()
        val validHoldings = holdingsWithPrices.filter { it.first > 0.0 && it.second.size >= 2 }
        if (validHoldings.isEmpty()) return emptyList()

        val totalWeight = validHoldings.sumOf { it.first }
        if (totalWeight <= 0.0) return emptyList()

        val availableReturnLengths = validHoldings.map { it.second.size - 1 }
        val effectiveDays = minOf(days, availableReturnLengths.minOrNull() ?: 0)
        if (effectiveDays <= 0) return emptyList()

        val portfolioReturns = mutableListOf<Double>()
        for (step in 0 until effectiveDays) {
            var weightedReturnSum = 0.0
            for ((weight, prices) in validHoldings) {
                val endIdx = prices.size - effectiveDays + step
                val startIdx = endIdx - 1
                if (startIdx >= 0 && endIdx < prices.size) {
                    val prevPrice = prices[startIdx]
                    val currPrice = prices[endIdx]
                    if (prevPrice > 0.0) {
                        val stockReturn = (currPrice - prevPrice) / prevPrice
                        weightedReturnSum += weight * stockReturn
                    }
                }
            }
            portfolioReturns.add(weightedReturnSum / totalWeight)
        }
        return portfolioReturns
    }

    /**
     * Historical 1-day Value at Risk (VaR):
     * Return at the (1 - confidenceLevel) percentile of the empirical return distribution.
     * Returned as a positive percentage loss (e.g. 2.45%).
     */
    fun calculateHistoricalVaR(returns: List<Double>, confidenceLevel: Double = 0.95): Double {
        if (returns.isEmpty()) return 0.0
        val sorted = returns.sorted()
        val index = ((1.0 - confidenceLevel) * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        val varReturn = sorted[index]
        return if (varReturn < 0.0) -varReturn * 100.0 else 0.0
    }

    /**
     * Conditional Value at Risk (CVaR / Expected Shortfall):
     * The average loss in the worst (1 - confidenceLevel) tail beyond the VaR threshold.
     */
    fun calculateConditionalVaR(returns: List<Double>, confidenceLevel: Double = 0.95): Double {
        if (returns.isEmpty()) return 0.0
        val sorted = returns.sorted()
        val cutoffIndex = ((1.0 - confidenceLevel) * sorted.size).toInt().coerceIn(1, sorted.size)
        val tailReturns = sorted.subList(0, cutoffIndex)
        val avgTailReturn = tailReturns.average()
        return if (avgTailReturn < 0.0) -avgTailReturn * 100.0 else 0.0
    }
}
