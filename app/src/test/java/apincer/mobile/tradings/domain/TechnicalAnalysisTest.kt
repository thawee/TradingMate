package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalAnalysisTest {

    @Test
    fun testSuggestedQuantitySizesRiskCorrectly() {
        // ฿100,000 equity, risk 1% (=฿1,000), risk/share = ฿5 -> 200 shares max.
        val qty = TechnicalAnalysis.calculateSuggestedQuantity(
            accountEquity = 100_000.0,
            riskPercent = 1.0,
            riskPerShare = 5.0
        )
        assertEquals(200, qty)
    }

    @Test
    fun testSuggestedQuantityHandlesInvalidInputs() {
        assertEquals(0, TechnicalAnalysis.calculateSuggestedQuantity(0.0, 1.0, 5.0))
        assertEquals(0, TechnicalAnalysis.calculateSuggestedQuantity(100_000.0, 0.0, 5.0))
        assertEquals(0, TechnicalAnalysis.calculateSuggestedQuantity(100_000.0, 1.0, 0.0))
        assertEquals(0, TechnicalAnalysis.calculateSuggestedQuantity(100_000.0, 1.0, -5.0))
    }

    @Test
    fun testSMA() {
        val prices = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val sma = TechnicalAnalysis.calculateSMA(prices, 3)
        assertEquals(4.0, sma!!, 0.001)
    }

    @Test
    fun testRSI() {
        val prices = listOf(
            44.34, 44.09, 44.15, 43.61, 44.33, 44.83, 45.10, 
            45.42, 45.84, 46.08, 45.89, 46.03, 45.61, 46.28, 46.28
        )
        val rsi = TechnicalAnalysis.calculateRSI(prices, 14)
        // Expected RSI is around 70.4
        assertEquals(70.4, rsi!!, 1.0)
    }

    @Test
    fun testMACD() {
        val prices = List(50) { it.toDouble() }
        val (macd, signal, hist) = TechnicalAnalysis.calculateMACD(prices)
        assert(macd != null)
        assert(signal != null)
        assert(hist != null)
    }

    @Test
    fun testSellSignalWhenProfitAboveFivePercent() {
        val signal = TechnicalAnalysis.getDetailedSignal(
            rsi = 50.0,
            macdHist = 0.5,
            lastPrice = 110.0,
            sma50 = 100.0,
            sma200 = 95.0,
            bb = null,
            isVolumeSurge = false,
            userCost = 100.0,
            userQuantity = 100,
            tradePurpose = "SWING"
        )
        assertEquals(IndicatorSignal.SELL, signal.type)
        assertTrue(signal.reason.contains("Scale Out") || signal.reason.contains("Exit Target"))
    }

    @Test
    fun testSellSignalWhenProfitBahtAboveFiveHundred() {
        val signal = TechnicalAnalysis.getDetailedSignal(
            rsi = 50.0,
            macdHist = 0.5,
            lastPrice = 106.0,
            sma50 = 100.0,
            sma200 = 95.0,
            bb = null,
            isVolumeSurge = false,
            userCost = 100.0,
            userQuantity = 1000,
            tradePurpose = "SWING"
        )
        assertEquals(IndicatorSignal.SELL, signal.type)
        assertTrue(signal.reason.contains("Scale Out") || signal.reason.contains("Exit Target"))
    }

    @Test
    fun testSellSignalWhenTrendTurnsDownward() {
        val signal = TechnicalAnalysis.getDetailedSignal(
            rsi = 50.0,
            macdHist = -0.5,
            lastPrice = 98.0,
            sma50 = 100.0,
            sma200 = 110.0,
            bb = null,
            isVolumeSurge = false,
            userCost = 100.0,
            userQuantity = 100,
            tradePurpose = "SWING"
        )
        assertEquals(IndicatorSignal.SELL, signal.type)
        assertTrue(signal.reason.contains("Early Breakdown Warning") || signal.reason.contains("Weak Trend"))
    }

    @Test
    fun testWeakTrendDoesNotSellWhenLossIsSmall() {
        // Anti-whipsaw: flat position (~-0.3% after fees) in weak trend → NEUTRAL warning, not SELL
        val signal = TechnicalAnalysis.getDetailedSignal(
            rsi = 50.0,
            macdHist = -0.5,
            lastPrice = 100.0,
            sma50 = 102.0,
            sma200 = 110.0,
            bb = null,
            isVolumeSurge = false,
            userCost = 100.0,
            userQuantity = 100,
            tradePurpose = "SWING"
        )
        assertEquals(IndicatorSignal.NEUTRAL, signal.type)
        assertTrue(signal.reason.contains("Trend Weakening"))
    }

    @Test
    fun testEarlyRecoveryRequiresVolumeConfirmation() {
        // MACD bullish + oversold, no volume → POTENTIAL watch only
        val quiet = TechnicalAnalysis.getDetailedSignal(
            rsi = 30.0,
            macdHist = 0.5,
            lastPrice = 90.0,
            sma50 = 100.0,
            sma200 = 95.0,
            bb = null,
            isVolumeSurge = false
        )
        assertEquals(IndicatorSignal.POTENTIAL, quiet.type)

        // Same setup with volume surge → confirmed BUY
        val confirmed = TechnicalAnalysis.getDetailedSignal(
            rsi = 30.0,
            macdHist = 0.5,
            lastPrice = 90.0,
            sma50 = 100.0,
            sma200 = 95.0,
            bb = null,
            isVolumeSurge = true
        )
        assertEquals(IndicatorSignal.BUY, confirmed.type)
    }

    @Test
    fun testHealthyMomentumSkipsExtendedMoves() {
        // MACD bullish above SMA50 but RSI 60 (extended) → no BUY chase
        val signal = TechnicalAnalysis.getDetailedSignal(
            rsi = 60.0,
            macdHist = 0.5,
            lastPrice = 110.0,
            sma50 = 100.0,
            sma200 = 95.0,
            bb = null,
            isVolumeSurge = false
        )
        assertEquals(IndicatorSignal.NEUTRAL, signal.type)
    }

    @Test
    fun testSet50StopLossThreshold() {
        // SET50 stop loss triggers below -4.5%
        val signalSet50 = TechnicalAnalysis.getDetailedSignal(
            rsi = 45.0,
            macdHist = -0.2,
            lastPrice = 95.0, // ~-5.3% net loss
            sma50 = 100.0,
            sma200 = 100.0,
            bb = null,
            isVolumeSurge = false,
            userCost = 100.0,
            userQuantity = 100,
            isSet50 = true
        )
        assertEquals(IndicatorSignal.SELL, signalSet50.type)
        assertTrue(signalSet50.reason.contains("SET50 Large Cap"))
    }

    @Test
    fun testMidSmallCapStopLossThreshold() {
        // Mid/Small-Cap does NOT trigger stop loss at -5.3% (requires -6.5%)
        val signalMidCap = TechnicalAnalysis.getDetailedSignal(
            rsi = 45.0,
            macdHist = 0.1,
            lastPrice = 95.0, // ~-5.3% net loss
            sma50 = 100.0,
            sma200 = 100.0,
            bb = null,
            isVolumeSurge = false,
            userCost = 100.0,
            userQuantity = 100,
            isSet50 = false
        )
        // Since loss is -5.3% (not yet -6.5%), it should NOT trigger Cut Loss SELL
        assertEquals(IndicatorSignal.NEUTRAL, signalMidCap.type)

        // Triggers once net loss exceeds -6.5% (lastPrice = 93.0 -> ~-7.3% net loss)
        val signalMidCapCut = TechnicalAnalysis.getDetailedSignal(
            rsi = 45.0,
            macdHist = -0.2,
            lastPrice = 93.0,
            sma50 = 100.0,
            sma200 = 100.0,
            bb = null,
            isVolumeSurge = false,
            userCost = 100.0,
            userQuantity = 100,
            isSet50 = false
        )
        assertEquals(IndicatorSignal.SELL, signalMidCapCut.type)
        assertTrue(signalMidCapCut.reason.contains("Mid/Small-Cap SET"))
    }

    @Test
    fun testOBVAccumulationAndDistribution() {
        // Up day adds volume, down day subtracts, flat day unchanged
        val prices = listOf(10.0, 11.0, 10.5, 10.5, 11.5)
        val volumes = listOf(100L, 200L, 150L, 120L, 300L)
        val obv = TechnicalAnalysis.calculateOBV(prices, volumes)
        assertEquals(listOf(0.0, 200.0, 50.0, 50.0, 350.0), obv)
    }

    @Test
    fun testObvRisingConfirmsAccumulation() {
        // Steadily rising prices with volume -> OBV above its average = rising
        val prices = List(20) { 10.0 + it * 0.5 }
        val volumes = List(20) { 100L }
        assertTrue(TechnicalAnalysis.isObvRising(prices, volumes))

        // Steadily falling prices -> OBV below its average = not rising
        val falling = List(20) { 20.0 - it * 0.5 }
        assertTrue(!TechnicalAnalysis.isObvRising(falling, volumes))
    }

    @Test
    fun testRelativeStrengthVsIndex() {
        // Stock +20% over 63 days, index +5% -> RS = +15
        val stock = List(64) { i -> 100.0 * (1 + 0.20 * i / 63.0) }
        val index = List(64) { i -> 1000.0 * (1 + 0.05 * i / 63.0) }
        val rs = TechnicalAnalysis.calculateRelativeStrength(stock, index)
        assertEquals(15.0, rs!!, 0.01)

        // Insufficient history -> null
        assertEquals(null, TechnicalAnalysis.calculateRelativeStrength(stock.take(10), index))
    }

    @Test
    fun test52WeekRange() {
        val prices = List(252) { 50.0 } + listOf(30.0, 80.0, 60.0)
        val range = TechnicalAnalysis.calculate52WeekRange(prices)
        // Window is last 252 entries, includes the 30 low and 80 high
        assertEquals(30.0, range!!.first, 0.001)
        assertEquals(80.0, range.second, 0.001)

        // Too little history -> null
        assertEquals(null, TechnicalAnalysis.calculate52WeekRange(List(10) { 50.0 }))
    }

    @Test
    fun testObvRisingConfirmsEarlyRecoveryBuy() {
        // MACD bullish + oversold RSI, quiet volume but rising OBV -> confirmed BUY
        val confirmed = TechnicalAnalysis.getDetailedSignal(
            rsi = 30.0,
            macdHist = 0.5,
            lastPrice = 90.0,
            sma50 = 100.0,
            sma200 = 100.0,
            bb = null,
            isVolumeSurge = false,
            obvRising = true
        )
        assertEquals(IndicatorSignal.BUY, confirmed.type)
        assertTrue(confirmed.description.contains("On-Balance Volume"))
    }

    @Test
    fun testATRConstantRange() {
        // Constant 2-point daily range on a flat close -> ATR = 2.0
        val closes = List(30) { 100.0 }
        val highs = List(30) { 101.0 }
        val lows = List(30) { 99.0 }
        val atr = TechnicalAnalysis.calculateATR(highs, lows, closes)
        assertEquals(2.0, atr!!, 0.001)

        // Insufficient history -> null
        assertEquals(null, TechnicalAnalysis.calculateATR(highs.take(5), lows.take(5), closes.take(5)))
    }

    @Test
    fun testADXTrendVsChop() {
        // Strong steady uptrend -> high ADX
        val n = 60
        val upCloses = List(n) { 100.0 + it }
        val upHighs = List(n) { 101.0 + it }
        val upLows = List(n) { 99.0 + it }
        val trendAdx = TechnicalAnalysis.calculateADX(upHighs, upLows, upCloses)
        assertTrue("Uptrend ADX should be strong, was $trendAdx", trendAdx!! > 25.0)

        // Alternating chop -> weak ADX
        val chopCloses = List(n) { if (it % 2 == 0) 100.0 else 101.0 }
        val chopHighs = chopCloses.map { it + 1.0 }
        val chopLows = chopCloses.map { it - 1.0 }
        val chopAdx = TechnicalAnalysis.calculateADX(chopHighs, chopLows, chopCloses)
        assertTrue("Chop ADX should be weak, was $chopAdx", chopAdx!! < 25.0)
    }

    @Test
    fun testAdxFallingKnifeGuardDowngradesOversoldBuy() {
        // Oversold + above SMA200 normally = BUY, but ADX >= 40 downgrades to POTENTIAL
        val guarded = TechnicalAnalysis.getDetailedSignal(
            rsi = 30.0, macdHist = -0.5, lastPrice = 100.0,
            sma50 = 105.0, sma200 = 95.0, bb = null,
            isVolumeSurge = false, adx = 45.0
        )
        assertEquals(IndicatorSignal.POTENTIAL, guarded.type)
        assertTrue(guarded.reason.contains("Falling Knife"))

        // Same setup with calm ADX stays a BUY
        val allowed = TechnicalAnalysis.getDetailedSignal(
            rsi = 30.0, macdHist = -0.5, lastPrice = 100.0,
            sma50 = 105.0, sma200 = 95.0, bb = null,
            isVolumeSurge = false, adx = 25.0
        )
        assertEquals(IndicatorSignal.BUY, allowed.type)
    }

    @Test
    fun testAdxChopFilterBlocksHealthyMomentum() {
        // MACD bullish above SMA50 normally = Healthy Momentum BUY, but ADX < 20 = chop
        val blocked = TechnicalAnalysis.getDetailedSignal(
            rsi = 50.0, macdHist = 0.5, lastPrice = 110.0,
            sma50 = 100.0, sma200 = 95.0, bb = null,
            isVolumeSurge = false, adx = 15.0
        )
        assertTrue(blocked.type != IndicatorSignal.BUY)

        val allowed = TechnicalAnalysis.getDetailedSignal(
            rsi = 50.0, macdHist = 0.5, lastPrice = 110.0,
            sma50 = 100.0, sma200 = 95.0, bb = null,
            isVolumeSurge = false, adx = 25.0
        )
        assertEquals(IndicatorSignal.BUY, allowed.type)
        assertTrue(allowed.reason.contains("Healthy Momentum"))
    }

    @Test
    fun testAtrBasedStopLoss() {
        // Low-volatility stock (ATR 1%) -> stop = -3.5% (clamped floor); -5% net loss triggers SELL
        val calmStop = TechnicalAnalysis.getDetailedSignal(
            rsi = 45.0, macdHist = 0.2, lastPrice = 95.0,
            sma50 = 100.0, sma200 = 100.0, bb = null,
            isVolumeSurge = false, atrPercent = 1.0,
            userCost = 100.0, userQuantity = 100
        )
        assertEquals(IndicatorSignal.SELL, calmStop.type)
        assertTrue(calmStop.reason.contains("ATR"))

        // High-volatility stock (ATR 3% -> stop -6%): same -5% loss does NOT trigger
        val volatileHold = TechnicalAnalysis.getDetailedSignal(
            rsi = 45.0, macdHist = 0.2, lastPrice = 95.0,
            sma50 = 100.0, sma200 = 100.0, bb = null,
            isVolumeSurge = false, atrPercent = 3.0,
            userCost = 100.0, userQuantity = 100
        )
        assertTrue(volatileHold.type != IndicatorSignal.SELL)
    }

    @Test
    fun testStochasticRangePosition() {
        // Close pinned at the high of the range -> %K near 100; at the low -> near 0
        val n = 20
        val highs = List(n) { 110.0 }
        val lows = List(n) { 90.0 }
        val atHigh = TechnicalAnalysis.calculateStochastic(highs, lows, List(n) { 110.0 })
        assertEquals(100.0, atHigh!!.first, 0.001)
        val atLow = TechnicalAnalysis.calculateStochastic(highs, lows, List(n) { 90.0 })
        assertEquals(0.0, atLow!!.first, 0.001)

        // Insufficient history -> null
        assertEquals(null, TechnicalAnalysis.calculateStochastic(highs.take(5), lows.take(5), List(5) { 100.0 }))
    }

    @Test
    fun testMFIExtremes() {
        val n = 20
        // All up days on volume -> MFI 100 (pure inflow)
        val upCloses = List(n) { 100.0 + it }
        val upHighs = upCloses.map { it + 1 }
        val upLows = upCloses.map { it - 1 }
        val volumes = List(n) { 1000L }
        val mfiUp = TechnicalAnalysis.calculateMFI(upHighs, upLows, upCloses, volumes)
        assertEquals(100.0, mfiUp!!, 0.001)

        // All down days -> MFI 0 (pure outflow)
        val downCloses = List(n) { 200.0 - it }
        val mfiDown = TechnicalAnalysis.calculateMFI(downCloses.map { it + 1 }, downCloses.map { it - 1 }, downCloses, volumes)
        assertEquals(0.0, mfiDown!!, 0.001)
    }

    @Test
    fun testStochasticGateOnOversoldBuy() {
        // Oversold + uptrend, but %K below %D in oversold zone -> still falling, downgrade
        val gated = TechnicalAnalysis.getDetailedSignal(
            rsi = 30.0, macdHist = -0.5, lastPrice = 100.0,
            sma50 = 105.0, sma200 = 95.0, bb = null,
            isVolumeSurge = false, stochK = 10.0, stochD = 15.0
        )
        assertEquals(IndicatorSignal.POTENTIAL, gated.type)
        assertTrue(gated.reason.contains("Reversal Not Confirmed"))

        // %K crossed above %D -> reversal confirmed, BUY allowed
        val confirmed = TechnicalAnalysis.getDetailedSignal(
            rsi = 30.0, macdHist = -0.5, lastPrice = 100.0,
            sma50 = 105.0, sma200 = 95.0, bb = null,
            isVolumeSurge = false, stochK = 18.0, stochD = 14.0
        )
        assertEquals(IndicatorSignal.BUY, confirmed.type)
    }

    @Test
    fun testMfiDistributionSellAndCapitulationConfirm() {
        // Profitable position + MFI >= 80 -> Distribution SELL
        val distribution = TechnicalAnalysis.getDetailedSignal(
            rsi = 55.0, macdHist = 0.5, lastPrice = 110.0,
            sma50 = 100.0, sma200 = 95.0, bb = null,
            isVolumeSurge = false, mfi = 85.0,
            userCost = 100.0, userQuantity = 100, adx = 15.0
        )
        assertEquals(IndicatorSignal.SELL, distribution.type)
        assertTrue(distribution.reason.contains("Distribution") || distribution.reason.contains("Scale Out") || distribution.reason.contains("Exit Target"))

        // MACD bullish near oversold with quiet volume but MFI capitulation -> confirmed BUY
        val capitulation = TechnicalAnalysis.getDetailedSignal(
            rsi = 33.0, macdHist = 0.5, lastPrice = 90.0,
            sma50 = 100.0, sma200 = 100.0, bb = null,
            isVolumeSurge = false, mfi = 15.0
        )
        assertEquals(IndicatorSignal.BUY, capitulation.type)
        assertTrue(capitulation.description.contains("capitulation"))
    }

    @Test
    fun testRiskRewardCalculations() {
        val stop = TechnicalAnalysis.calculateSuggestedStopLossPrice(10.0, atr = 0.3, isSet50 = true)
        // 10.0 with 2*ATR stop (2*3%=6%, clamped 4.5-9%) -> 9.40 (6% loss)
        assertTrue(stop < 10.0 && stop > 9.0)

        val target = TechnicalAnalysis.calculateSuggestedTargetPrice(10.0, stopLossPrice = 9.5, minTargetPercent = 10.0)
        assertEquals(11.0, target, 0.001)

        val rr = TechnicalAnalysis.calculateRiskRewardRatio(entryPrice = 10.0, targetPrice = 11.0, stopLossPrice = 9.5)
        assertEquals(2.0, rr!!, 0.001)
    }

    @Test
    fun testMarketRegime() {
        val bullPrices = List(60) { 100.0 + it * 2 } // Strong uptrend
        assertEquals(TechnicalAnalysis.MarketRegime.BULLISH, TechnicalAnalysis.getMarketRegime(bullPrices))

        val bearPrices = List(60) { 200.0 - it * 2 } // Strong downtrend
        assertEquals(TechnicalAnalysis.MarketRegime.BEARISH, TechnicalAnalysis.getMarketRegime(bearPrices))
    }

    @Test
    fun testEarlyBreakdownWarning() {
        val signal = TechnicalAnalysis.getDetailedSignal(
            rsi = 45.0,
            macdHist = -0.5,
            lastPrice = 98.0,
            sma50 = 100.0,
            sma200 = 90.0,
            bb = null,
            isVolumeSurge = false,
            userCost = 100.0,
            userQuantity = 100
        )
        assertEquals(IndicatorSignal.SELL, signal.type)
        assertTrue(signal.reason.contains("Early Breakdown Warning"))
    }

    @Test
    fun testFalseBreakoutGuardOnNvdrSelling() {
        val signal = TechnicalAnalysis.getDetailedSignal(
            rsi = 50.0,
            macdHist = 0.5,
            lastPrice = 10.0,
            sma50 = 9.5,
            sma200 = 9.0,
            bb = null,
            isVolumeSurge = true,
            adx = 25.0,
            nvdrNetVolume = -500_000.0,
            nvdrNetValue = -10_000_000.0
        )
        assertEquals(IndicatorSignal.POTENTIAL, signal.type)
        assertTrue(signal.reason.contains("False Breakout Guard"))
    }

    @Test
    fun testEarlyBreakdownSuppressedOnExDividendDate() {
        val signal = TechnicalAnalysis.getDetailedSignal(
            rsi = 45.0,
            macdHist = -0.5,
            lastPrice = 98.0,
            sma50 = 100.0,
            sma200 = 90.0,
            bb = null,
            isVolumeSurge = false,
            userCost = 100.0,
            userQuantity = 100,
            isNearXdDate = true
        )
        // With isNearXdDate = true, the early breakdown warning (-2%) is suppressed because price drop is an expected dividend cash payout
        org.junit.Assert.assertNotEquals(IndicatorSignal.SELL, signal.type)
    }

    @Test
    fun testIsNearExDividendDate() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date())
        assertTrue("Today should be near XD date", TechnicalAnalysis.isNearExDividendDate(today))
        org.junit.Assert.assertFalse("Null date should not be near XD", TechnicalAnalysis.isNearExDividendDate(null))
        org.junit.Assert.assertFalse("Past year date should not be near XD", TechnicalAnalysis.isNearExDividendDate("2020-01-01"))
    }
}
