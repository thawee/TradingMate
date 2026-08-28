package apincer.mobile.tradings.ui

import apincer.mobile.tradings.data.PortfolioEntity
import apincer.mobile.tradings.data.ScrapedStockInfo
import apincer.mobile.tradings.data.StockAggregate
import apincer.mobile.tradings.data.StockCacheEntity
import apincer.mobile.tradings.data.StockSignalEntity
import org.junit.Assert.assertFalse
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
            nvdrNetVolume = nvdrNetVolume
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
    fun testIsFlowNullTolerance() {
        val stock = createStock(nvdrNetVolume = null)
        assertTrue("Null NVDR volume should be tolerant and return true", StockDna.isFlow(stock))
        assertFalse("FLOW tag should not be added when NVDR is null", StockDna.tags(stock).contains("FLOW"))
    }

    @Test
    fun testIsFlowPositiveNetVolume() {
        val stock = createStock(nvdrNetVolume = 500_000.0)
        assertTrue(StockDna.isFlow(stock))
        assertTrue("FLOW tag should be present on net buying", StockDna.tags(stock).contains("FLOW"))
    }

    @Test
    fun testIsFlowNegativeNetVolume() {
        val stock = createStock(nvdrNetVolume = -100_000.0)
        assertFalse("Negative NVDR volume should return false for isFlow", StockDna.isFlow(stock))
        assertFalse("FLOW tag should not be present on net selling", StockDna.tags(stock).contains("FLOW"))
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

    @Test
    fun testCalculateScoreAndGrade() {
        val eliteStock = createStock(nvdrNetVolume = 1_000_000.0).let { s ->
            val cache = s.portfolio.cache!!.copy(
                roe = 20.0,
                debtToEquity = 0.8,
                netProfitMargin = 18.0,
                profitGrowth3Y = 15.0,
                pe = 12.0,
                pbv = 1.0,
                dividendYield = 5.5,
                volume = 2_000_000L
            )
            val signal = s.portfolio.signal!!.copy(
                macdHist = 1.2,
                rsi = 52.0,
                relativeStrength = 4.5,
                sma50 = 9.0,
                sma200 = 8.0,
                week52Low = 7.0,
                week52High = 12.0
            )
            s.copy(
                info = s.info.copy(
                    roe = 20.0,
                    debtToEquity = 0.8,
                    netProfitMargin = 18.0,
                    profitGrowth3Y = 15.0,
                    pe = 12.0,
                    pbv = 1.0,
                    dividendYield = 5.5,
                    volume = 2_000_000L
                ),
                portfolio = s.portfolio.copy(cache = cache, signal = signal)
            )
        }

        val score = StockDna.calculateScore(eliteStock)
        assertTrue("Elite stock should score >= 80, got ${score.totalScore}", score.totalScore >= 80)
        org.junit.Assert.assertEquals(ConfluenceGrade.PRIME_A_PLUS, score.grade)
        assertTrue("Tags should contain A+", StockDna.tags(eliteStock).contains("A+"))
    }

    @Test
    fun testStrategyArchetypes() {
        val stock = createStock(nvdrNetVolume = 500_000.0).let { s ->
            val cache = s.portfolio.cache!!.copy(
                roe = 16.0,
                debtToEquity = 0.9,
                netProfitMargin = 14.0,
                profitGrowth3Y = 12.0,
                dividendYield = 5.2,
                volume = 1_000_000L
            )
            val signal = s.portfolio.signal!!.copy(
                macdHist = 0.5,
                rsi = 55.0,
                relativeStrength = 3.0,
                sma50 = 8.5,
                sma200 = 8.0,
                week52Low = 6.0,
                week52High = 11.0
            )
            s.copy(
                info = s.info.copy(
                    roe = 16.0,
                    debtToEquity = 0.9,
                    netProfitMargin = 14.0,
                    profitGrowth3Y = 12.0,
                    dividendYield = 5.2,
                    volume = 1_000_000L
                ),
                portfolio = s.portfolio.copy(cache = cache, signal = signal)
            )
        }

        assertTrue("Should be Compounder Aristocrat", StockDna.isCompounderAristocrat(stock))
        assertTrue("Should be VCP Breakout", StockDna.isVcpBreakout(stock))
        assertTrue("Should be High Yield Shield", StockDna.isHighYieldShield(stock))
        assertTrue("Should be Foreign Whale Inflow", StockDna.isForeignWhale(stock))
        assertFalse("Should not be Oversold Rebound (RSI=55)", StockDna.isOversoldRebound(stock))

        val oversoldStock = stock.let { s ->
            val signal = s.portfolio.signal!!.copy(rsi = 28.0)
            s.copy(portfolio = s.portfolio.copy(signal = signal))
        }
        assertTrue("Should be Oversold Rebound (RSI=28)", StockDna.isOversoldRebound(oversoldStock))
    }

    @Test
    fun testIsFlowDualConfirmationLaggingSet() {
        // Stock has positive NVDR, but is severely lagging the market (RS = -3.5)
        val deadCatStock = createStock(nvdrNetVolume = 500_000.0).let { s ->
            val signal = s.portfolio.signal!!.copy(relativeStrength = -3.5)
            s.copy(portfolio = s.portfolio.copy(signal = signal))
        }
        assertFalse("Positive NVDR on severely lagging stock should fail isFlow dual confirmation", StockDna.isFlow(deadCatStock))
        assertFalse("Foreign whale requires RS >= 0", StockDna.isForeignWhale(deadCatStock))
    }

    @Test
    fun testCyclicalSectorShieldPenaltyAndTag() {
        val cyclicalStock = createStock(nvdrNetVolume = 100_000.0).let { s ->
            val cache = s.portfolio.cache!!.copy(
                sector = "Petrochemicals & Chemicals",
                roe = 16.0,
                debtToEquity = 0.9,
                profitGrowth3Y = 4.0, // Low profit growth
                netProfitMargin = 6.0 // Low net profit margin
            )
            s.copy(
                info = s.info.copy(
                    sector = "Petrochemicals & Chemicals",
                    roe = 16.0,
                    debtToEquity = 0.9,
                    profitGrowth3Y = 4.0,
                    netProfitMargin = 6.0
                ),
                portfolio = s.portfolio.copy(cache = cache)
            )
        }

        assertTrue("Should identify cyclical sector", StockDna.isCyclical(cyclicalStock.info.sector))
        assertTrue("Tags should contain CYC", StockDna.tags(cyclicalStock).contains("CYC"))
        assertFalse("Cyclical stock with low profit growth should not be a compounder aristocrat", StockDna.isCompounderAristocrat(cyclicalStock))
        val score = StockDna.calculateScore(cyclicalStock)
        assertTrue("Highlights should warn of cyclical volatility", score.highlights.contains("Cyclical Volatility"))
    }

    @Test
    fun testIsMtfAlignedAndMtfTag() {
        val mtfStock = createStock(nvdrNetVolume = 100_000.0).let { s ->
            val signal = s.portfolio.signal!!.copy(sma50 = 9.0, sma200 = 8.0, macdHist = 0.4)
            s.copy(
                info = s.info.copy(lastPrice = 10.0),
                portfolio = s.portfolio.copy(signal = signal)
            )
        }
        assertTrue("Stock above SMA50/200 with positive MACD hist is MTF aligned", StockDna.isMtfAligned(mtfStock))
        assertTrue("Tags should contain MTF chip", StockDna.tags(mtfStock).contains("MTF"))
    }
}
