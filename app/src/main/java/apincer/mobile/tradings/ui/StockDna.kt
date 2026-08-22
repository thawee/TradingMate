package apincer.mobile.tradings.ui

import android.util.Log
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TradingConstants

/**
 * Confluence Grade for Quant Screening (0-100 score).
 */
enum class ConfluenceGrade(val label: String, val colorHex: Long) {
    PRIME_A_PLUS("A+", 0xFF6EE7B7), // Mint Green - Institutional Setup
    STRONG_A("A", 0xFF60A5FA),      // Pro Blue - High Conviction
    WATCH_B("B", 0xFFFCD34D),       // Gold - Setup in Progress
    NEUTRAL_C("C", 0xFF94A3B8)      // Slate Grey - Below Threshold
}

data class ConfluenceScore(
    val totalScore: Int,
    val grade: ConfluenceGrade,
    val qualityScore: Int,
    val valueScore: Int,
    val momentumScore: Int,
    val flowScore: Int,
    val safetyScore: Int,
    val highlights: List<String>
)

/**
 * The Institutional Multi-Layer Filter & Quant Scoring System (Stock DNA).
 * Single source of truth for stock classification, used by the Advisor screen,
 * candidate lists (Swing Plays / Dividend Stars), and DNA tag chips.
 */
object StockDna {

    /**
     * Layer 1 — Quality: management efficiency and profitability.
     * ROE > 15% (mandatory), NPM > 10%, D/E < 1.5, 3Y profit growth > 10%.
     * NPM / D/E / growth are null-tolerant: missing data does not disqualify,
     * only a value that actively fails the threshold does.
     */
    fun isQual(s: StockWatchlistInfo): Boolean {
        val info = s.info
        if ((info.roe ?: 0.0) <= TradingConstants.ROE_MIN_THRESHOLD) return false
        if (info.debtToEquity?.let { it >= 1.5 } == true) return false
        if (info.netProfitMargin?.let { it <= 10.0 } == true) return false
        if (info.profitGrowth3Y?.let { it <= 10.0 } == true) return false
        return true
    }

    /** Pre-filter — Liquidity: daily turnover > ฿5,000,000.
     *  Ensures the stock is actively traded enough for stop-loss orders
     *  to execute at the displayed price without excessive slippage. */
    fun isLiquid(s: StockWatchlistInfo): Boolean {
        val volume = s.info.volume
        if (volume == null) {
            Log.w("StockDna", "isLiquid: Null volume for ${s.info.symbol}, failing open.")
            return true // Fail-open: allow if volume data hasn't been fetched yet
        }
        val price = s.info.lastPrice
        return price > 0 && volume * price > TradingConstants.MIN_LIQUIDITY_TURNOVER_BAHT
    }

    /** Pre-filter — 52-Week Low Trap: reject stocks trading within 5% of their
     *  52-week low. Structural decliners "look cheap" on RSI/P-E but keep making
     *  new lows. Null-tolerant: passes if 52w data hasn't been computed yet. */
    fun isNotNear52wLow(s: StockWatchlistInfo): Boolean {
        val low = s.portfolio.week52Low ?: return true
        val price = s.info.lastPrice
        return price <= 0 || price >= low * 1.05
    }

    /** Combined pre-filter gate applied before all DNA layers. */
    fun preFilter(s: StockWatchlistInfo): Boolean = isLiquid(s) && isNotNear52wLow(s)

    /** Layer 2 — Value: P/E 0.1–15.0 and P/BV 0.1–1.0. */
    fun isVal(s: StockWatchlistInfo): Boolean =
        (s.info.pe ?: 0.0) in 0.1..15.0 && (s.info.pbv ?: 0.0) in 0.1..1.0

    /** Layer 3 — Dividend: yield >= 5%. */
    fun isDiv(s: StockWatchlistInfo): Boolean =
        (s.info.dividendYield ?: 0.0) >= TradingConstants.DIVIDEND_YIELD_ENTRY

    /** Layer 4 — Momentum: MACD histogram meaningfully positive (>0.1% of price),
     *  RSI in 40–64.9 (not overbought — aligned with RSI_OVERBOUGHT), and
     *  3-month Relative Strength vs SET index not negative (don't buy market
     *  laggards). RS is null-tolerant: missing data does not disqualify. */
    fun isMom(s: StockWatchlistInfo): Boolean {
        val hist = s.portfolio.macdHist ?: 0.0
        val price = s.info.lastPrice
        return price > 0 && hist > price * 0.001 &&
               (s.portfolio.rsi ?: 50.0) in 40.0..TradingConstants.RSI_MOMENTUM_MAX &&
               (s.portfolio.relativeStrength ?: 0.0) >= 0.0
    }

    /** Layer 5 — Support/Setup: BUY/POTENTIAL signal only. */
    fun isSup(s: StockWatchlistInfo): Boolean =
        s.signal?.type == IndicatorSignal.BUY ||
        s.signal?.type == IndicatorSignal.POTENTIAL

    /** Layer 6 — Flow: Foreign Fund NVDR net accumulation. */
    fun isFlow(s: StockWatchlistInfo): Boolean {
        val nvdrVol = s.portfolio.nvdrNetVolume
        if (nvdrVol == null) return true // Null-tolerant until data source is fully populated
        return nvdrVol > 0.0
    }

    /** Earnings gap-up play: +4% day on a profitable stock with high volume. */
    fun isGapUp(s: StockWatchlistInfo): Boolean {
        val turnover = (s.info.volume ?: 0L) * s.info.lastPrice
        return s.info.percentChange >= 4.0 && 
               turnover >= TradingConstants.MIN_LIQUIDITY_TURNOVER_BAHT &&
               ((s.info.roe ?: 0.0) > 10.0 || (s.info.netProfitMargin ?: 0.0) > 5.0)
    }

    /** Market Regime Aware Swing Filter */
    fun isSwingCandidate(s: StockWatchlistInfo, isMarketBearish: Boolean = false): Boolean {
        if (!preFilter(s) || !isQual(s)) return false
        if (!isMom(s) && !isSup(s) && !isGapUp(s)) return false
        if (isMarketBearish) {
            val hasFlow = isFlow(s) && s.portfolio.nvdrNetVolume != null
            val hasOutperformed = (s.portfolio.relativeStrength ?: 0.0) > 0.0
            return hasFlow || hasOutperformed
        }
        return true
    }

    // ==========================================
    // Strategy Archetypes (One-Tap Quant Presets)
    // ==========================================

    /** Archetype 1: Compounder Aristocrat (Long-term Moat + Low Debt + Dividend) */
    fun isCompounderAristocrat(s: StockWatchlistInfo): Boolean {
        if (!preFilter(s)) return false
        val info = s.info
        val roe = info.roe ?: 0.0
        val de = info.debtToEquity ?: 1.0
        val divYield = info.dividendYield ?: 0.0
        return roe >= 12.0 && de <= 1.2 && divYield >= 3.0 && (info.netProfitMargin ?: 0.0) >= 8.0
    }

    /** Archetype 2: Minervini VCP / Stage-2 Momentum Breakout */
    fun isVcpBreakout(s: StockWatchlistInfo): Boolean {
        if (!preFilter(s)) return false
        val price = s.info.lastPrice
        val sma50 = s.portfolio.sma50 ?: 0.0
        val sma200 = s.portfolio.sma200 ?: 0.0
        val isAboveTrend = price >= sma50 && (sma200 == 0.0 || sma50 >= sma200 || price >= sma200)
        return isAboveTrend && isMom(s) && ((s.info.roe ?: 0.0) >= 8.0)
    }

    /** Archetype 3: High-Yield Shield (Safe Dividend + Value Protection) */
    fun isHighYieldShield(s: StockWatchlistInfo): Boolean {
        return preFilter(s) && isDiv(s) && isQual(s) && isNotNear52wLow(s)
    }

    /** Archetype 4: Foreign Whale Smart Money Accumulation */
    fun isForeignWhale(s: StockWatchlistInfo): Boolean {
        if (!preFilter(s)) return false
        val nvdr = s.portfolio.nvdrNetVolume ?: 0.0
        val rs = s.portfolio.relativeStrength ?: 0.0
        return nvdr > 0.0 && rs > 0.0
    }

    /** Archetype 5: Oversold Mean-Reversion Spring */
    fun isOversoldRebound(s: StockWatchlistInfo): Boolean {
        if (!preFilter(s)) return false
        val rsi = s.portfolio.rsi ?: 50.0
        return rsi <= 35.0 && ((s.info.roe ?: 0.0) >= 8.0)
    }

    // ==========================================
    // Composite Quant Scoring Engine (0-100 pts)
    // ==========================================

    fun calculateScore(s: StockWatchlistInfo): ConfluenceScore {
        var qualityPts = 0
        var valuePts = 0
        var momentumPts = 0
        var flowPts = 0
        var safetyPts = 0
        val highlights = mutableListOf<String>()

        val info = s.info
        val portfolio = s.portfolio

        // 1. Quality Pillar (Max 25)
        val roe = info.roe ?: 0.0
        if (roe >= 15.0) { qualityPts += 10; highlights.add("Elite ROE") }
        else if (roe >= 10.0) { qualityPts += 6 }
        else if (roe >= 5.0) { qualityPts += 3 }

        val npm = info.netProfitMargin ?: 0.0
        if (npm >= 15.0) { qualityPts += 5 }
        else if (npm >= 8.0) { qualityPts += 3 }

        val de = info.debtToEquity
        if (de != null) {
            if (de <= 1.0) qualityPts += 5
            else if (de <= 1.5) qualityPts += 3
        } else {
            qualityPts += 3 // Neutral credit for missing D/E
        }

        val growth = info.profitGrowth3Y ?: 0.0
        if (growth >= 10.0) qualityPts += 5
        else if (growth > 0.0) qualityPts += 2

        // 2. Value Pillar (Max 20)
        val pe = info.pe ?: 0.0
        if (pe in 0.1..15.0) { valuePts += 10; highlights.add("Low P/E") }
        else if (pe in 15.0..22.0) { valuePts += 5 }

        val pbv = info.pbv ?: 0.0
        if (pbv in 0.1..1.2) { valuePts += 10; highlights.add("Undervalued P/BV") }
        else if (pbv in 1.2..2.0) { valuePts += 5 }

        // 3. Momentum & Trend Pillar (Max 25)
        val price = info.lastPrice
        val sma50 = portfolio.sma50 ?: 0.0
        val sma200 = portfolio.sma200 ?: 0.0
        if (price > 0 && sma50 > 0 && price >= sma50) momentumPts += 8
        if (price > 0 && sma200 > 0 && price >= sma200) momentumPts += 7
        if ((portfolio.macdHist ?: 0.0) > 0) momentumPts += 5
        val rsi = portfolio.rsi ?: 50.0
        if (rsi in 40.0..65.0) momentumPts += 5

        // 4. Flow & Relative Strength Pillar (Max 15)
        val rs = portfolio.relativeStrength ?: 0.0
        if (rs > 0.0) { flowPts += 8; highlights.add("Outperforming SET") }
        val nvdr = portfolio.nvdrNetVolume ?: 0.0
        if (nvdr > 0.0) { flowPts += 7; highlights.add("Foreign Inflow") }

        // 5. Dividend & Safety Pillar (Max 15)
        val yield = info.dividendYield ?: 0.0
        if (yield >= 5.0) { safetyPts += 8; highlights.add("High Yield (≥5%)") }
        else if (yield >= 3.0) { safetyPts += 4 }
        if (isNotNear52wLow(s)) safetyPts += 7

        val total = (qualityPts + valuePts + momentumPts + flowPts + safetyPts).coerceIn(0, 100)

        val grade = when {
            total >= 80 -> ConfluenceGrade.PRIME_A_PLUS
            total >= 65 -> ConfluenceGrade.STRONG_A
            total >= 50 -> ConfluenceGrade.WATCH_B
            else -> ConfluenceGrade.NEUTRAL_C
        }

        return ConfluenceScore(
            totalScore = total,
            grade = grade,
            qualityScore = qualityPts,
            valueScore = valuePts,
            momentumScore = momentumPts,
            flowScore = flowPts,
            safetyScore = safetyPts,
            highlights = highlights
        )
    }

    /** DNA tag chips displayed on stock cards. */
    fun tags(s: StockWatchlistInfo): List<String> = buildList {
        val score = calculateScore(s)
        if (score.grade == ConfluenceGrade.PRIME_A_PLUS) add("A+")
        else if (score.grade == ConfluenceGrade.STRONG_A) add("A")

        if (isVcpBreakout(s)) add("VCP")
        if (isCompounderAristocrat(s)) add("MOAT")
        if (isHighYieldShield(s)) add("SHIELD")
        if (isForeignWhale(s)) add("WHALE")
        if (isOversoldRebound(s)) add("SPRING")

        if (isQual(s)) add("QUAL")
        if (isVal(s)) add("VAL")
        if (isDiv(s)) add("DIV")
        if (isMom(s)) add("MOM")
        if (isSup(s)) add("SUP")
        if (isGapUp(s)) add("GAP")
        if (isFlow(s) && s.portfolio.nvdrNetVolume != null) add("FLOW")
        if ((s.portfolio.relativeStrength ?: 0.0) > 0.0) add("RS")
        if ((s.portfolio.rsi ?: 50.0) < TradingConstants.RSI_OVERSOLD - 5.0) add("OS")
    }
}
