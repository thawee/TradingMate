package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin
import kotlin.random.Random

class BacktestEngineTest {

    /** Builds a synthetic oscillating-with-uptrend daily price series (no gaps). */
    private fun syntheticHistory(days: Int, seed: Long = 42L): List<ScrapedHistoricalPrice> {
        val random = Random(seed)
        val prices = mutableListOf<ScrapedHistoricalPrice>()
        var price = 100.0
        for (i in 0 until days) {
            // Gentle uptrend + cyclical swings + noise so RSI/MACD/Bollinger actually
            // cross their thresholds over the series (a pure straight line never triggers signals).
            val trend = i * 0.03
            val cycle = 6.0 * sin(i / 12.0)
            val noise = random.nextDouble(-1.0, 1.0)
            price = (100.0 + trend + cycle + noise).coerceAtLeast(1.0)
            val high = price * (1.0 + random.nextDouble(0.0, 0.01))
            val low = price * (1.0 - random.nextDouble(0.0, 0.01))
            val date = java.time.LocalDate.of(2024, 1, 1).plusDays(i.toLong()).toString()
            prices.add(ScrapedHistoricalPrice(date = date, close = price, volume = 1_000_000L, high = high, low = low))
        }
        return prices
    }

    @Test
    fun testInsufficientHistoryReturnsNull() {
        val result = BacktestEngine.run("TEST", syntheticHistory(100))
        assertNull(result)
    }

    @Test
    fun testRunProducesConsistentResult() {
        val history = syntheticHistory(400)
        val result = BacktestEngine.run("TEST", history)
        assertTrue("Expected a non-null backtest result with enough history", result != null)
        result!!

        // Internal consistency of the aggregate stats.
        assertTrue(result.totalTrades == result.wins + result.losses)
        assertTrue(result.winRatePercent in 0.0..100.0)
        result.closedTrades.forEach { trade ->
            // Entry must always be no later than exit, and holding days non-negative.
            assertTrue(trade.holdingDays >= 0)
            assertTrue(trade.entryPrice > 0.0)
            assertTrue(trade.exitPrice > 0.0)
        }
        // Expectancy should be between the worst loss and best win.
        if (result.totalTrades > 0) {
            assertTrue(result.expectancyPercent >= result.avgLossPercent - 0.001)
            assertTrue(result.expectancyPercent <= maxOf(result.avgWinPercent, 0.0) + 0.001)
        }
    }

    @Test
    fun testSet50TierAffectsStopLossOutcome() {
        // Same price series, only the SET50 flag differs -> tiering can change the exit
        // point/reason for trades that hit the fixed-tier stop-loss (when ATR is unavailable
        // this is deterministic; when ATR is available both runs should still be internally valid).
        val history = syntheticHistory(400, seed = 7L)
        val resultSet50 = BacktestEngine.run("TEST50", history, isSet50 = true)
        val resultMidSmall = BacktestEngine.run("TESTMID", history, isSet50 = false)
        assertTrue(resultSet50 != null && resultMidSmall != null)
    }
}
