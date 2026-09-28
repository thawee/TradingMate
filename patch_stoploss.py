import sys
with open('app/src/main/java/apincer/mobile/tradings/domain/TechnicalAnalysis.kt', 'r') as f:
    content = f.read()

old_func = """    fun calculateSuggestedStopLossPrice(
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
    }"""

new_func = """    fun calculateSuggestedStopLossPrice(
        lastPrice: Double, 
        atr: Double? = null, 
        isSet50: Boolean = false, 
        customStopLossPercent: Double? = null,
        marketRegime: MarketRegime = MarketRegime.NEUTRAL
    ): Double {
        if (lastPrice <= 0.0) return 0.0
        
        // Dynamic Trailing Stop Logic based on Market Regime
        val regimeAtrMultiplier = when (marketRegime) {
            MarketRegime.BEARISH -> 1.5 // Tighter stops in bear regimes (less risk tolerance)
            MarketRegime.BULLISH -> 3.0 // Looser stops in bull regimes (let winners run)
            MarketRegime.NEUTRAL -> TradingConstants.ATR_STOP_MULTIPLIER // 2.0 default
        }
        
        val atrStop = atr?.takeIf { it > 0.0 }?.let {
            -(regimeAtrMultiplier * (it / lastPrice * 100))
                .coerceIn(TradingConstants.ATR_STOP_MIN_PERCENT, TradingConstants.ATR_STOP_MAX_PERCENT)
        }
        
        val baselinePercent = if (isSet50) TradingConstants.STOP_LOSS_SET50_PERCENT else TradingConstants.STOP_LOSS_MID_SMALL_PERCENT
        val regimeBaselinePercent = when (marketRegime) {
            MarketRegime.BEARISH -> baselinePercent * 0.75 // Tighter
            MarketRegime.BULLISH -> baselinePercent * 1.25 // Looser
            MarketRegime.NEUTRAL -> baselinePercent
        }
        
        val stopPercent = customStopLossPercent?.takeIf { it < 0.0 }
            ?: atrStop
            ?: regimeBaselinePercent
            
        return lastPrice * (1.0 + stopPercent / 100.0)
    }"""
content = content.replace(old_func, new_func)

with open('app/src/main/java/apincer/mobile/tradings/domain/TechnicalAnalysis.kt', 'w') as f:
    f.write(content)

