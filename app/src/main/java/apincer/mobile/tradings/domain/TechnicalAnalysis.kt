package apincer.mobile.tradings.domain

import androidx.compose.ui.graphics.Color
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

data class Indicators(
    val sma50: Double?,
    val sma200: Double?,
    val rsi: Double?,
    val macd: Double?,
    val signal: Double?,
    val histogram: Double?,
    val bollingerBands: BollingerBands?,
    val isVolumeSurge: Boolean = false
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
    // InnovestX Fee Structure:
    // 1. Commission: 0.10% - 0.15% (we use 0.15% for safety)
    // 2. Market Fee (Trading Fee + Clearing Fee + Regulatory Fee): 0.007%
    // 3. VAT: 7% on total commissions (Commission + Market Fee)
    // 4. Selling Tax (Financial Transaction Tax): 0.11% (currently 0.10% tax + 0.01% local tax)
    
    const val COMMISSION_RATE = 0.0015
    const val MARKET_FEE_RATE = 0.00007
    const val VAT_RATE = 0.07
    const val SELLING_TAX_RATE = 0.0011

    // Alignment with snapshot Strategy: Focus on RSI (35/65 targets)
    const val RSI_OVERSOLD_THRESHOLD = 35.0
    const val RSI_OVERBOUGHT_THRESHOLD = 65.0
    const val RSI_POTENTIAL_THRESHOLD = 42.0

    fun calculateFees(amount: Double, isSelling: Boolean): Double {
        val commission = amount * COMMISSION_RATE
        val marketFee = amount * MARKET_FEE_RATE
        val vat = (commission + marketFee) * VAT_RATE
        var totalFees = commission + marketFee + vat
        
        if (isSelling) {
            totalFees += (amount * SELLING_TAX_RATE)
        }
        
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
        
        val isRsiOversold = rsi < RSI_OVERSOLD_THRESHOLD
        val isRsiPotential = rsi < RSI_POTENTIAL_THRESHOLD
        val isRsiOverbought = rsi > RSI_OVERBOUGHT_THRESHOLD
        val isMacdBullish = macdHist > 0.0
        val isPriceAboveSma50 = if (sma50 != null) lastPrice > sma50 else false // Unknown: conservative default
        val isPriceAboveSma200 = if (sma200 != null) lastPrice > sma200 else true
        val isNearLowerBB = if (bb != null) lastPrice <= bb.lower * 1.05 else false // Within 5% of lower band
        val isNearUpperBB = if (bb != null) lastPrice >= bb.upper * 0.95 else false // Within 5% of upper band
        
        // Selling Zone: Overbought OR Near Upper Resistance
        if (isRsiOverbought || isNearUpperBB || (!isMacdBullish && !isPriceAboveSma50)) {
            return TradingZone.SELLING_ZONE
        }

        // Buying Zone: Oversold (Extreme Value) OR (Positive Momentum near support) - but NOT if overbought
        if (isRsiOversold || (isMacdBullish && (isNearLowerBB || isPriceAboveSma50))) {
            return TradingZone.BUYING_ZONE
        }

        // Potential Zone: Nearing Buy thresholds
        if (isRsiPotential || isNearLowerBB) {
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
            currentTime in 1000..1230 -> MarketStatus.OPEN
            currentTime in 1231..1429 -> MarketStatus.LUNCH
            currentTime in 1430..1630 -> MarketStatus.OPEN
            else -> MarketStatus.CLOSED
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
        userCost: Double? = null,
        isFundamentalGood: Boolean = false,
        tradePurpose: String = "SWING",
        dividendYield: Double? = null,
        roe: Double? = null
    ): TradeSignal {
        if (rsi == null || macdHist == null) return TradeSignal(IndicatorSignal.NEUTRAL, "Waiting for data", "We need more historical data to generate a signal.")
        
        val isRsiOversold = rsi < RSI_OVERSOLD_THRESHOLD
        val isRsiNearOversold = rsi < RSI_POTENTIAL_THRESHOLD
        val isRsiOverbought = rsi > RSI_OVERBOUGHT_THRESHOLD
        val isMacdBullish = macdHist > 0.0
        val isPriceAboveSma50 = if (lastPrice != null && sma50 != null) lastPrice > sma50 else true
        val isPriceAboveSma200 = if (lastPrice != null && sma200 != null) lastPrice > sma200 else true
        val isNearLowerBB = if (bb != null && lastPrice != null) lastPrice <= bb.lower * 1.05 else false // Within 5%
        val isNearUpperBB = if (bb != null && lastPrice != null) lastPrice >= bb.upper * 0.95 else false

        val qualityPrefix = if (isFundamentalGood) "⭐ Quality: " else ""

        // 1. SELL PRIORITY: Check for "Take Profit" or "Stop Loss"
        if (userCost != null && userCost > 0 && lastPrice != null) {
            val netProfitPercent = calculateNetProfitPercent(userCost, lastPrice)
            
            var applySwingLogic = true
            
            if (tradePurpose == "DIVIDEND") {
                val yield = dividendYield ?: 0.0
                val isRoeBad = roe != null && roe < 15.0
                
                if (isRoeBad) {
                    return TradeSignal(
                        IndicatorSignal.SELL,
                        "${qualityPrefix}Fundamentals Broke",
                        "Dividend rule broken. Company fundamentals declining (ROE < 15%). Re-evaluate holding."
                    )
                }
                
                if (yield >= 3.0) {
                    // Yield is good (>= 3%), keep it! Do not apply swing logic.
                    applySwingLogic = false
                } else {
                    // Yield dropped below 3%. It loses its 'hold forever' status and we use Swing logic to exit.
                    applySwingLogic = true
                }
            }
            
            if (applySwingLogic) {
                // CUT LOSS PRIORITY: Stop loss is more urgent than take profit
                if (netProfitPercent < -5.0) {
                    val technicalWarning = when {
                        !isPriceAboveSma200 -> "the price has crashed below the long-term trend (SMA 200)"
                        !isPriceAboveSma50 -> "the price has broken below its 50-day average (SMA 50)"
                        else -> "momentum is weakening significantly"
                    }
                    return TradeSignal(
                        IndicatorSignal.SELL,
                        "${qualityPrefix}Stop Loss (Cut Loss)",
                        "Warning: Your net loss is ${String.format(Locale.ENGLISH,"%.2f", netProfitPercent)}%. Technically, $technicalWarning. Cutting loss prevents a small loss from becoming a big one."
                    )
                }

                // SWING PLAYBOOK: Take Profit if target reached
                if (netProfitPercent > 10.0) {
                    return TradeSignal(
                        IndicatorSignal.SELL,
                        "${qualityPrefix}Exit Area (Target Reached)",
                        "Your profit is ${String.format(Locale.ENGLISH,"%.2f", netProfitPercent)}%. Good area to lock in gains."
                    )
                }
            }
        }

        // 2. SELL PRIORITY: Technical Overbought
        val isProtectedDividend = tradePurpose == "DIVIDEND" && (dividendYield ?: 0.0) >= 3.0
        if (!isProtectedDividend && isRsiOverbought) {
            return TradeSignal(
                IndicatorSignal.SELL,
                "${qualityPrefix}Overbought",
                "RSI is ${String.format(Locale.ENGLISH,"%.1f", rsi)} (above 65). The stock is overextended and likely to pull back. Consider selling to lock in gains."
            )
        }
        if (!isProtectedDividend && isNearUpperBB) {
            return TradeSignal(
                IndicatorSignal.SELL,
                "${qualityPrefix}Upper Band Resistance",
                "Price is near the upper Bollinger Band — a resistance zone. Stocks often pull back from this level. Consider selling or tightening your stop-loss."
            )
        }

        // 3. BUY SIGNALS (Capturing Value and Early Reversal)
        
        // High Conviction: Oversold in Uptrend
        if (isRsiOversold && isPriceAboveSma200) {
            return TradeSignal(
                IndicatorSignal.BUY,
                "${qualityPrefix}Oversold Accumulation",
                "RSI is oversold (${String.format(Locale.ENGLISH,"%.1f", rsi)}) while the long-term uptrend (SMA 200) is still intact. High probability value dip."
            )
        }

        // Recovery: MACD turns positive near support
        if (isMacdBullish && (isNearLowerBB || isRsiOversold)) {
            return TradeSignal(
                IndicatorSignal.BUY,
                "${qualityPrefix}Early Recovery",
                "Momentum (MACD) has turned positive while the price is at a major support level or recovering from extreme lows."
            )
        }

        // Uptrend Entry
        if (isMacdBullish && isPriceAboveSma50) {
            return TradeSignal(
                IndicatorSignal.BUY,
                "${qualityPrefix}Healthy Momentum",
                "Positive momentum confirmed. The stock is trading above its short-term average, suggesting a sustained uptrend."
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
        if (!isMacdBullish && !isPriceAboveSma50) {
             return if (userCost != null && userCost > 0) {
                TradeSignal(
                    IndicatorSignal.SELL,
                    "${qualityPrefix}Weak Trend",
                    "The stock is losing momentum and trading below its average. Likely to continue falling."
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
