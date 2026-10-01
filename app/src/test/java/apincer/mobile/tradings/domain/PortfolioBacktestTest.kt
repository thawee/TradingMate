package apincer.mobile.tradings.domain

import apincer.mobile.tradings.data.ScrapedHistoricalPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin
import kotlin.random.Random

class PortfolioBacktestTest {

    private fun synthetic(days: Int, seed: Long, drift: Double): List<ScrapedHistoricalPrice> {
        val random = Random(seed)
        return (0 until days).map { i ->
            val price = (100.0 + i * drift + 6.0 * sin(i / 12.0 + seed) + random.nextDouble(-1.0, 1.0)).coerceAtLeast(1.0)
            ScrapedHistoricalPrice(
                date = java.time.LocalDate.of(2020, 1, 1).plusDays(i.toLong()).toString(),
                close = price, volume = 1_000_000L,
                high = price * (1 + random.nextDouble(0.0, 0.01)), low = price * (1 - random.nextDouble(0.0, 0.01))
            )
        }
    }

    private val universe = mapOf("AAA" to synthetic(700, 1, 0.03), "BBB" to synthetic(700, 2, -0.01), "CCC" to synthetic(700, 3, 0.05))
    private val config = PortfolioBacktestConfig(startDate = "2020-01-01", endDate = "2021-12-31")

    @Test
    fun respectsCapitalLotsAndPositionLimits() {
        val result = PortfolioBacktest.run(universe, config.copy(maxPositions = 2))
        assertTrue(result.trades.isNotEmpty())
        assertTrue(result.trades.all { it.shares % 100 == 0 && it.shares > 0 })
        assertTrue(result.equityCurve.all { it.second > 0 })
        assertTrue(result.exposurePercent in 0.0..100.0)
        // Equity at the end equals cash plus marked positions, so closed P/L must reconcile when nothing is open.
        if (result.openPositionsAtEnd == 0) {
            assertEquals(config.initialCapital + result.trades.sumOf { it.pnlBaht }, result.stats.finalEquity, 0.01)
        }
    }

    @Test
    fun slippageReducesReturn() {
        val noSlip = PortfolioBacktest.run(universe, config.copy(slippagePercent = 0.0))
        val withSlip = PortfolioBacktest.run(universe, config.copy(slippagePercent = 0.5))
        assertTrue(withSlip.stats.finalEquity < noSlip.stats.finalEquity)
    }

    @Test
    fun buyAndHoldTracksPrice() {
        val series = universe.getValue("AAA")
        val stats = PortfolioBacktest.buyAndHold(series, config.copy(slippagePercent = 0.0))
        val expected = series.last().close / series.first().close
        assertEquals(expected, stats.finalEquity / config.initialCapital, 0.01)
    }
}
