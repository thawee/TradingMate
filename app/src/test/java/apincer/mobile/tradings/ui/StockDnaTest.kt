package apincer.mobile.tradings.ui

import apincer.mobile.tradings.data.PortfolioEntity
import apincer.mobile.tradings.data.ScrapedStockInfo
import apincer.mobile.tradings.data.StockAggregate
import apincer.mobile.tradings.data.StockCacheEntity
import apincer.mobile.tradings.data.StockSignalEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StockDnaTest {

    private fun createStock(nvdrNetVolume: Double?): StockWatchlistInfo {
        val portfolio = PortfolioEntity(
            symbol = "TEST",
            cost = 10.0,
            quantity = 100
        )
        val cache = StockCacheEntity(
            symbol = "TEST",
            lastPrice = 10.0,
            change = 0.5,
            percentChange = 5.0,
            volume = 100_000L,
            roe = 12.0,
            netProfitMargin = 8.0
        )
        val signal = StockSignalEntity(
            symbol = "TEST",
            nvdrNetVolume = nvdrNetVolume,
            relativeStrength = 2.0,
            week52Low = 5.0,
            weeklyTrendBullish = true,
            observationDate = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).toString(),
            benchmarkDate = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).toString()
        )
        val aggregate = StockAggregate(
            portfolio = portfolio,
            cache = cache,
            signal = signal
        )
        val info = ScrapedStockInfo(
            symbol = "TEST",
            lastPrice = 10.0,
            change = 0.5,
            percentChange = 5.0,
            volume = 100_000L,
            roe = 12.0,
            netProfitMargin = 8.0,
            nvdrNetVolume = nvdrNetVolume,
            lastUpdated = "2026-08-14 10:00:00"
        )
        return StockWatchlistInfo(
            portfolio = aggregate,
            info = info,
            signal = null
        )
    }

    @Test
    fun testMissingFlowDoesNotPass() {
        val stock = createStock(nvdrNetVolume = null)
        assertFalse("Missing NVDR cannot confirm positive flow", StockDna.isFlow(stock))
    }

    @Test
    fun testDailyMoverWithSellSignalIsNotActionable() {
        val stock = createStock(nvdrNetVolume = 500_000.0).copy(
            info = createStock(500_000.0).info.copy(volume = 1_000_000L),
            signal = apincer.mobile.tradings.domain.TradeSignal(
                apincer.mobile.tradings.domain.IndicatorSignal.SELL, "Exit", "Exit")
        )
        assertFalse(StockDna.isGapUp(stock))
    }

    @Test
    fun testMissingLiquidityOr52WeekEvidenceBlocksEntry() {
        val stock = createStock(500_000.0).copy(
            info = createStock(500_000.0).info.copy(volume = null),
            portfolio = createStock(500_000.0).portfolio.copy(
                signal = createStock(500_000.0).portfolio.signal!!.copy(week52Low = null))
        )
        assertFalse(StockDna.preFilter(stock))
    }

    @Test
    fun testOldOrMissingMarketObservationBlocksEntry() {
        val stock = createStock(500_000.0)
        val old = stock.copy(portfolio = stock.portfolio.copy(signal =
            stock.portfolio.signal!!.copy(observationDate = "2026-01-01", benchmarkDate = "2026-01-01")))
        assertFalse(StockDna.isFresh(old, java.time.LocalDate.of(2026, 9, 24)))
        val missing = stock.copy(portfolio = stock.portfolio.copy(signal =
            stock.portfolio.signal!!.copy(observationDate = null)))
        assertFalse(StockDna.isFresh(missing, java.time.LocalDate.of(2026, 9, 24)))
        org.junit.Assert.assertEquals("Price or SET benchmark history missing",
            StockDna.preFilterReason(missing.copy(info = missing.info.copy(volume = 1_000_000L))))
        val mismatched = stock.copy(portfolio = stock.portfolio.copy(signal =
            stock.portfolio.signal!!.copy(
                benchmarkDate = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).minusDays(1).toString())))
        assertFalse(StockDna.isFresh(mismatched))
    }

    @Test
    fun testFreshnessAcrossWeekendBoundaryAndFutureDates() {
        val stock = createStock(500_000.0)
        fun on(date: String) = stock.copy(portfolio = stock.portfolio.copy(signal =
            stock.portfolio.signal!!.copy(observationDate = date, benchmarkDate = date)))
        val friday = on("2026-10-02")
        // Saturday and the next Monday morning, before a new session: Friday's matched session is still current.
        assertTrue(StockDna.isFresh(friday, java.time.LocalDate.of(2026, 10, 3)))
        assertTrue(StockDna.isFresh(friday, java.time.LocalDate.of(2026, 10, 5)))
        // Seven days is the limit (covers a long holiday); eight is stale.
        assertTrue(StockDna.isFresh(friday, java.time.LocalDate.of(2026, 10, 9)))
        assertFalse(StockDna.isFresh(friday, java.time.LocalDate.of(2026, 10, 10)))
        // A session dated after today (clock or source error) is rejected.
        assertFalse(StockDna.isFresh(on("2026-10-06"), java.time.LocalDate.of(2026, 10, 5)))
        // The 2026-10-03 failure: stock on Friday, index proxy one session behind.
        val lagging = stock.copy(portfolio = stock.portfolio.copy(signal =
            stock.portfolio.signal!!.copy(observationDate = "2026-10-02", benchmarkDate = "2026-10-01")))
        assertFalse(StockDna.isFresh(lagging, java.time.LocalDate.of(2026, 10, 3)))
    }

    @Test
    fun testIsFlowPositiveNetVolume() {
        val stock = createStock(nvdrNetVolume = 500_000.0)
        assertTrue(StockDna.isFlow(stock))
    }

    @Test
    fun testIsFlowNegativeNetVolume() {
        val stock = createStock(nvdrNetVolume = -100_000.0)
        assertFalse("Negative NVDR volume should return false for isFlow", StockDna.isFlow(stock))
    }

    @Test
    fun testIsSwingCandidateMarketRegime() {
        // Stock passing quality and momentum
        val stock = createStock(nvdrNetVolume = 100_000.0).let { s ->
            val cache = s.portfolio.cache!!.copy(roe = 18.0, debtToEquity = 1.0, netProfitMargin = 15.0, profitGrowth3Y = 15.0, volume = 1_000_000L)
            val signal = s.portfolio.signal!!.copy(
                macdHist = 0.5,
                rsi = 50.0,
                relativeStrength = 2.0,
                signalType = "BUY"
            )
            s.copy(
                info = s.info.copy(roe = 18.0, debtToEquity = 1.0, netProfitMargin = 15.0, profitGrowth3Y = 15.0, volume = 1_000_000L),
                portfolio = s.portfolio.copy(cache = cache, signal = signal),
                signal = apincer.mobile.tradings.domain.TradeSignal(apincer.mobile.tradings.domain.IndicatorSignal.BUY, "BUY", "BUY")
            )
        }

        assertTrue("Bullish regime candidate", StockDna.isSwingCandidate(stock, isMarketBearish = false))
        assertTrue("Bearish regime passes when stock has NVDR flow + RS", StockDna.isSwingCandidate(stock, isMarketBearish = true))

        val laggingStock = stock.let { s ->
            val signal = s.portfolio.signal!!.copy(relativeStrength = -3.0, nvdrNetVolume = null)
            s.copy(
                portfolio = s.portfolio.copy(signal = signal),
                info = s.info.copy(nvdrNetVolume = null)
            )
        }
        assertFalse("Bearish regime filters out lagging stocks with no NVDR flow", StockDna.isSwingCandidate(laggingStock, isMarketBearish = true))
    }

    @Test fun sellSignalBlocksSwingEvenWhenMomentumIsPositive() {
        val candidate = createStock(500_000.0).let { s ->
            val cache = s.portfolio.cache!!.copy(roe = 18.0, debtToEquity = 1.0,
                netProfitMargin = 15.0, profitGrowth3Y = 15.0, volume = 1_000_000L)
            val signal = s.portfolio.signal!!.copy(macdHist = 0.5, rsi = 50.0,
                signalType = "SELL", weeklyTrendBullish = true)
            s.copy(info = s.info.copy(roe = 18.0, debtToEquity = 1.0,
                netProfitMargin = 15.0, profitGrowth3Y = 15.0, volume = 1_000_000L),
                portfolio = s.portfolio.copy(cache = cache, signal = signal),
                signal = apincer.mobile.tradings.domain.TradeSignal(
                    apincer.mobile.tradings.domain.IndicatorSignal.SELL, "SELL", "Invalidated"))
        }
        assertFalse(StockDna.isSwingCandidate(candidate))
        assertEquals(CandidateStatus.BLOCKED, StockDna.assessSwing(candidate).status)
    }

    @Test fun missingOrBearishWeeklyTrendCannotBeReady() {
        val candidate = createStock(500_000.0).let { s ->
            val cache = s.portfolio.cache!!.copy(roe = 18.0, debtToEquity = 1.0,
                netProfitMargin = 15.0, profitGrowth3Y = 15.0, volume = 1_000_000L)
            val signal = s.portfolio.signal!!.copy(macdHist = 0.5, rsi = 50.0,
                signalType = "BUY", weeklyTrendBullish = true)
            s.copy(info = s.info.copy(roe = 18.0, debtToEquity = 1.0,
                netProfitMargin = 15.0, profitGrowth3Y = 15.0, volume = 1_000_000L),
                portfolio = s.portfolio.copy(cache = cache, signal = signal),
                signal = apincer.mobile.tradings.domain.TradeSignal(
                    apincer.mobile.tradings.domain.IndicatorSignal.BUY, "BUY", "Confirmed"))
        }
        assertEquals(CandidateStatus.READY, StockDna.assessSwing(candidate).status)
        assertEquals(CandidateStatus.WATCH, StockDna.assessSwing(candidate.copy(portfolio =
            candidate.portfolio.copy(signal = candidate.portfolio.signal!!.copy(weeklyTrendBullish = null)))).status)
        assertEquals(CandidateStatus.BLOCKED, StockDna.assessSwing(candidate.copy(portfolio =
            candidate.portfolio.copy(signal = candidate.portfolio.signal!!.copy(weeklyTrendBullish = false)))).status)
    }

    @Test
    fun testIsFlowDualConfirmationLaggingSet() {
        // Stock has positive NVDR, but is severely lagging the market (RS = -3.5)
        val deadCatStock = createStock(nvdrNetVolume = 500_000.0).let { s ->
            val signal = s.portfolio.signal!!.copy(relativeStrength = -3.5)
            s.copy(portfolio = s.portfolio.copy(signal = signal))
        }
        assertFalse("Positive NVDR on severely lagging stock should fail isFlow dual confirmation", StockDna.isFlow(deadCatStock))
    }

    @Test
    fun qualityIsCalibratedForSet() {
        val base = createStock(nvdrNetVolume = null)
        fun withInfo(roe: Double, npm: Double, de: Double, sector: String) =
            base.copy(info = base.info.copy(roe = roe, netProfitMargin = npm, debtToEquity = de, sector = sector))
        // ROE 12% now qualifies (15% kept only 13 of 101 SET watchlist stocks).
        assertTrue(StockDna.isQual(withInfo(12.0, 14.0, 0.8, "พาณิชย์")))
        assertFalse(StockDna.isQual(withInfo(9.0, 14.0, 0.8, "พาณิชย์")))
        // Leverage blocks an ordinary company but not a bank.
        assertFalse(StockDna.isQual(withInfo(14.0, 20.0, 6.0, "พาณิชย์")))
        assertTrue(StockDna.isQual(withInfo(14.0, 20.0, 6.0, "ธนาคาร")))
        assertTrue(StockDna.isFinancialSector("ประกันภัยและประกันชีวิต"))
    }
}
