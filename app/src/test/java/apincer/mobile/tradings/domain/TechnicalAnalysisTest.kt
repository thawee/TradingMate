package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
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
}
