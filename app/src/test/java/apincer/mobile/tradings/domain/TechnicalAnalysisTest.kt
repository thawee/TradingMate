package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalAnalysisTest {

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
        assertTrue(signal.reason.contains("Exit Area"))
    }

    @Test
    fun testSellSignalWhenProfitBahtAboveFiveHundred() {
        val signal = TechnicalAnalysis.getDetailedSignal(
            rsi = 50.0,
            macdHist = 0.5,
            lastPrice = 101.0,
            sma50 = 100.0,
            sma200 = 95.0,
            bb = null,
            isVolumeSurge = false,
            userCost = 100.0,
            userQuantity = 1000,
            tradePurpose = "SWING"
        )
        assertEquals(IndicatorSignal.SELL, signal.type)
        assertTrue(signal.reason.contains("Exit Area"))
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
        assertEquals("Weak Trend", signal.reason)
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
}
