package apincer.mobile.tradings.ui

import android.util.Log
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TradingConstants

enum class CandidateStatus { READY, WATCH, BLOCKED }

data class CandidateAssessment(val status: CandidateStatus, val reasons: List<String>)

/**
 * The Institutional Multi-Layer Filter & Quant Scoring System (Stock DNA).
 * Single source of truth for stock classification, used by the Advisor screen,
 * and candidate lists (Swing Plays / Dividend Stars).
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
        if ((info.roe ?: 0.0) <= TradingConstants.QUALITY_ROE_MIN) return false
        // Banks, finance and insurers run on leverage, so debt-to-equity says nothing about their quality.
        if (!isFinancialSector(info.sector) && info.debtToEquity?.let { it >= 1.5 } == true) return false
        if (info.netProfitMargin?.let { it <= 10.0 } == true) return false
        if (info.profitGrowth3Y?.let { it <= 10.0 } == true) return false
        return true
    }

    /** Pre-filter — Liquidity: daily turnover > ฿5,000,000.
     *  Ensures the stock is actively traded enough for stop-loss orders
     *  to execute at the displayed price without excessive slippage. */
    fun isLiquid(s: StockWatchlistInfo): Boolean {
        val volume = s.info.volume
        if (volume == null) return false
        val price = s.info.lastPrice
        return price > 0 && volume * price > TradingConstants.MIN_LIQUIDITY_TURNOVER_BAHT
    }

    /** Pre-filter — 52-Week Low Trap: reject stocks trading within 5% of their
     *  52-week low. Structural decliners "look cheap" on RSI/P-E but keep making
     *  new lows. Null-tolerant: passes if 52w data hasn't been computed yet. */
    fun isNotNear52wLow(s: StockWatchlistInfo): Boolean {
        val low = s.portfolio.week52Low ?: return false
        val price = s.info.lastPrice
        return price <= 0 || price >= low * 1.05
    }

    /** Require stock and benchmark bars from the same latest known trading session. */
    fun isFresh(s: StockWatchlistInfo, today: java.time.LocalDate =
        java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok"))): Boolean {
        val observed = runCatching { java.time.LocalDate.parse(s.portfolio.observationDate) }.getOrNull()
            ?: return false
        val benchmark = runCatching { java.time.LocalDate.parse(s.portfolio.benchmarkDate) }.getOrNull()
            ?: return false
        return observed == benchmark && !observed.isAfter(today) &&
            !benchmark.isBefore(today.minusDays(7))
    }

    /** Combined pre-filter gate applied before all DNA layers. */
    fun preFilter(s: StockWatchlistInfo): Boolean =
        isLiquid(s) && isNotNear52wLow(s) && isFresh(s)

    fun preFilterReason(s: StockWatchlistInfo): String? = when {
        s.info.volume == null -> "Volume data missing"
        s.portfolio.week52Low == null -> "52-week low data missing"
        s.portfolio.observationDate == null || s.portfolio.benchmarkDate == null ->
            "Price or SET benchmark history missing"
        !isFresh(s) -> "Stock and SET sessions differ or history is too old"
        !isLiquid(s) -> "Turnover below ฿5 million"
        !isNotNear52wLow(s) -> "Within 5% of the 52-week low"
        else -> null
    }

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
        // RS is null-tolerant: stocks lacking 63-bar history are not disqualified — only an
        // actively negative RS (stock lagging the SET index) blocks the momentum gate.
        val rsOk = s.portfolio.relativeStrength?.let { it >= 0.0 } ?: true
        return price > 0 && hist > price * 0.001 &&
               (s.portfolio.rsi ?: 50.0) in 40.0..TradingConstants.RSI_MOMENTUM_MAX &&
               rsOk
    }

    /** Layer 5 — Support/Setup: BUY/POTENTIAL signal only. */
    fun isSup(s: StockWatchlistInfo): Boolean =
        s.signal?.type == IndicatorSignal.BUY ||
        s.signal?.type == IndicatorSignal.POTENTIAL

    /** Layer 6 — Flow: Foreign Fund NVDR net accumulation with Dual Confirmation.
     *  Ensures NVDR buying is not a dead-cat bounce or short-covering in a collapsing stock. */
    fun isFlow(s: StockWatchlistInfo): Boolean {
        val nvdrVol = s.portfolio.nvdrNetVolume
        if (nvdrVol == null) return false // Missing flow cannot confirm accumulation
        val rs = s.portfolio.relativeStrength ?: return false
        return nvdrVol > 0.0 && rs >= -1.0
    }

    /** Banking, finance & securities and insurance, in SET's Thai or English sector names. */
    fun isFinancialSector(sector: String?): Boolean {
        if (sector.isNullOrBlank()) return false
        return FINANCIAL_SECTORS.any { sector.contains(it, ignoreCase = true) }
    }

    private val FINANCIAL_SECTORS = listOf(
        "ธนาคาร", "เงินทุนและหลักทรัพย์", "ประกันภัย",
        "Banking", "Finance & Securities", "Insurance", "Financial Services"
    )

    /** Strong daily move: +4% day on a profitable stock with high volume. */
    fun isGapUp(s: StockWatchlistInfo): Boolean {
        val turnover = (s.info.volume ?: 0L) * s.info.lastPrice
        return preFilter(s) && s.signal?.type != IndicatorSignal.SELL &&
               s.info.percentChange >= 4.0 &&
               turnover >= TradingConstants.MIN_LIQUIDITY_TURNOVER_BAHT &&
               ((s.info.roe ?: 0.0) > 10.0 || (s.info.netProfitMargin ?: 0.0) > 5.0)
    }

    /** Shared entry assessment; only READY may become an actionable swing plan. */
    fun assessSwing(s: StockWatchlistInfo, isMarketBearish: Boolean = false): CandidateAssessment {
        preFilterReason(s)?.let { return CandidateAssessment(CandidateStatus.BLOCKED, listOf(it)) }
        if (s.signal?.type == IndicatorSignal.SELL)
            return CandidateAssessment(CandidateStatus.BLOCKED, listOf("Active sell or invalidation signal"))
        if (!isQual(s))
            return CandidateAssessment(CandidateStatus.BLOCKED, listOf("Quality rules not met"))
        when (s.portfolio.weeklyTrendBullish) {
            false -> return CandidateAssessment(CandidateStatus.BLOCKED, listOf("Completed weekly trend is bearish"))
            null -> return CandidateAssessment(CandidateStatus.WATCH, listOf("Completed weekly trend is unavailable"))
            true -> Unit
        }
        if (!isMom(s) && !isSup(s) && !isGapUp(s))
            return CandidateAssessment(CandidateStatus.WATCH, listOf("No confirmed momentum or support setup"))
        if (isMarketBearish) {
            val hasFlow = isFlow(s)
            val hasOutperformed = (s.portfolio.relativeStrength ?: Double.NEGATIVE_INFINITY) > 0.0
            if (!hasFlow && !hasOutperformed)
                return CandidateAssessment(CandidateStatus.BLOCKED, listOf("No positive flow or relative strength in this market regime"))
        }
        if (s.signal?.type != IndicatorSignal.BUY)
            return CandidateAssessment(CandidateStatus.WATCH, listOf("Entry signal is not confirmed BUY"))
        return CandidateAssessment(CandidateStatus.READY, emptyList())
    }

    fun isSwingCandidate(s: StockWatchlistInfo, isMarketBearish: Boolean = false): Boolean =
        assessSwing(s, isMarketBearish).status == CandidateStatus.READY
}
