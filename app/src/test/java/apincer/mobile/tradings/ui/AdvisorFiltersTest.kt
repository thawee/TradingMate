package apincer.mobile.tradings.ui

import apincer.mobile.tradings.data.PortfolioEntity
import apincer.mobile.tradings.data.ScrapedStockInfo
import apincer.mobile.tradings.data.StockAggregate
import apincer.mobile.tradings.data.StockSignalEntity
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TradeSignal
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AdvisorFiltersTest {
    private fun stock(
        type: IndicatorSignal = IndicatorSignal.BUY,
        weekly: Boolean? = true,
        roe: Double? = 12.0,
        rs: Double? = 2.0
    ): StockWatchlistInfo {
        val today = LocalDate.now(ZoneId.of("Asia/Bangkok")).toString()
        return StockWatchlistInfo(
            info = ScrapedStockInfo(symbol = "TEST", lastPrice = 10.0, change = 0.0,
                percentChange = 0.0, volume = 1_000_000L, roe = roe,
                dividendYield = 6.0, lastUpdated = today),
            portfolio = StockAggregate(
                portfolio = PortfolioEntity(symbol = "TEST", cost = 0.0, quantity = 0),
                cache = null,
                signal = StockSignalEntity(symbol = "TEST", week52Low = 5.0,
                    weeklyTrendBullish = weekly, observationDate = today,
                    benchmarkDate = today, relativeStrength = rs)
            ),
            signal = TradeSignal(type, "Test", "Test")
        )
    }

    @Test fun swingDefaultsMatchExistingEligibilityAcrossSignalsAndRegimes() {
        for (type in IndicatorSignal.entries) for (weekly in listOf(true, false, null)) {
            for (roe in listOf(12.0, 10.0, null)) for (rs in listOf(2.0, -2.0, null)) {
                val s = stock(type, weekly, roe, rs)
                for (bearish in listOf(true, false)) {
                    val result = filterAdvisorStocks(listOf(s), PlaybookMode.SWING,
                        AdvisorFilter.defaultMask(PlaybookMode.SWING), bearish)
                    assertEquals(StockDna.isSwingCandidate(s, bearish), result.stocks.isNotEmpty())
                }
            }
        }
    }

    @Test fun dividendDefaultsMatchExistingEligibilityWithoutSwingTiming() {
        for (type in IndicatorSignal.entries) for (weekly in listOf(true, false, null)) {
            for (roe in listOf(12.0, 10.0, null)) {
                val s = stock(type, weekly, roe)
                val result = filterAdvisorStocks(listOf(s), PlaybookMode.DIVIDEND,
                    AdvisorFilter.defaultMask(PlaybookMode.DIVIDEND), true)
                assertEquals(StockDna.isDividendCandidate(s), result.stocks.isNotEmpty())
            }
        }
    }

    @Test fun disablingBuyRevealsPotentialWithoutMakingItEligible() {
        val s = stock(IndicatorSignal.POTENTIAL)
        val mask = AdvisorFilter.defaultMask(PlaybookMode.SWING) xor AdvisorFilter.BUY.bit
        assertEquals(listOf(s), filterAdvisorStocks(listOf(s), PlaybookMode.SWING, mask, false).stocks)
        assertFalse(StockDna.isSwingCandidate(s))
    }

    @Test fun countsAreCumulativeAndDisabledStagesDoNotRemoveStocks() {
        val stocks = listOf(stock(), stock(roe = 5.0), stock(weekly = false))
        val result = filterAdvisorStocks(stocks, PlaybookMode.SWING,
            AdvisorFilter.QUALITY.bit or AdvisorFilter.WEEKLY.bit, false)
        assertEquals(2, result.stages.first { it.filter == AdvisorFilter.QUALITY }.count)
        assertEquals(1, result.stages.first { it.filter == AdvisorFilter.WEEKLY }.count)
        assertEquals(1, result.stocks.size)
        val withoutQuality = filterAdvisorStocks(stocks, PlaybookMode.SWING, AdvisorFilter.WEEKLY.bit, false)
        assertEquals(2, withoutQuality.stages.first { it.filter == AdvisorFilter.QUALITY }.count)
        assertFalse(withoutQuality.stages.first { it.filter == AdvisorFilter.QUALITY }.enabled)
        assertEquals(2, withoutQuality.stocks.size)
    }

    @Test fun clearShowsAllTrackedStocksIncludingMissingData() {
        val s = stock(roe = null).let { it.copy(info = it.info.copy(volume = null),
            portfolio = it.portfolio.copy(signal = null)) }
        assertEquals(listOf(s), filterAdvisorStocks(listOf(s), PlaybookMode.SWING, 0, false).stocks)
        assertTrue(filterAdvisorStocks(listOf(s), PlaybookMode.SWING,
            AdvisorFilter.defaultMask(PlaybookMode.SWING), false).stocks.isEmpty())
    }

    @Test fun dividendIgnoresSwingOnlyBitsAndEmptyInputHasZeroCounts() {
        val s = stock(IndicatorSignal.POTENTIAL, weekly = false)
        assertEquals(listOf(s), filterAdvisorStocks(listOf(s), PlaybookMode.DIVIDEND,
            AdvisorFilter.BUY.bit or AdvisorFilter.WEEKLY.bit, true).stocks)
        val empty = filterAdvisorStocks(emptyList(), PlaybookMode.SWING, 0, false)
        assertTrue(empty.stocks.isEmpty())
        assertTrue(empty.stages.all { it.count == 0 })
    }
}
