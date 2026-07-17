package apincer.mobile.tradings.ui
import android.util.Log
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TradingConstants

/**
 * The 5-Layer Filter System (Stock DNA).
 * Single source of truth for stock classification, used by the Advisor screen,
 * candidate lists (Swing Plays / Dividend Stars), and DNA tag chips.
 *
 * Pre-filter: isLiquid() gates all candidates to ensure tradeable liquidity.
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

    /** Pre-filter — Liquidity: daily turnover > ฿1,000,000.
     *  Ensures the stock is actively traded enough for stop-loss orders
     *  to execute at the displayed price. */
    fun isLiquid(s: StockWatchlistInfo): Boolean {
        val volume = s.info.volume
        if (volume == null) {
            Log.w("StockDna", "isLiquid: Null volume for ${s.info.symbol}, failing open.")
            return true // Fail-open: allow if volume data hasn't been fetched yet
        }
        val price = s.info.lastPrice
        return price > 0 && volume * price > 1_000_000.0
    }

    /** Layer 2 — Value: P/E 0.1–15.0 and P/BV 0.1–1.0. */
    fun isVal(s: StockWatchlistInfo): Boolean =
        (s.info.pe ?: 0.0) in 0.1..15.0 && (s.info.pbv ?: 0.0) in 0.1..1.0

    /** Layer 3 — Dividend: yield >= 5%. */
    fun isDiv(s: StockWatchlistInfo): Boolean =
        (s.info.dividendYield ?: 0.0) >= TradingConstants.DIVIDEND_YIELD_ENTRY

    /** Layer 4 — Momentum: MACD histogram meaningfully positive (>0.1% of price)
     *  and RSI in 40–64.9 (not overbought — aligned with RSI_OVERBOUGHT). */
    fun isMom(s: StockWatchlistInfo): Boolean {
        val hist = s.portfolio.macdHist ?: 0.0
        val price = s.info.lastPrice
        return price > 0 && hist > price * 0.001 &&
               (s.portfolio.rsi ?: 50.0) in 40.0..TradingConstants.RSI_MOMENTUM_MAX
    }

    /** Layer 5 — Support/Setup: BUY/POTENTIAL signal only.
     *  These signal types already incorporate SMA 200 trend context checks
     *  in getDetailedSignal(), avoiding "falling knife" entries where
     *  RSI < 35 in a structural downtrend (below SMA 200). */
    fun isSup(s: StockWatchlistInfo): Boolean =
        s.signal?.type == IndicatorSignal.BUY ||
        s.signal?.type == IndicatorSignal.POTENTIAL

    /** Earnings gap-up play: +4% day on a profitable stock with high volume.
     *  Gap plays are momentum events, so we relax the strict 3-year growth
     *  requirements of isQual, requiring only basic baseline profitability.
     *  Must have >= 5M THB turnover to filter out low-volume artifacts. */
    fun isGapUp(s: StockWatchlistInfo): Boolean {
        val turnover = (s.info.volume ?: 0L) * s.info.lastPrice
        return s.info.percentChange >= 4.0 && 
               turnover >= 5_000_000.0 &&
               ((s.info.roe ?: 0.0) > 10.0 || (s.info.netProfitMargin ?: 0.0) > 5.0)
    }

    /** DNA tag chips displayed on stock cards. */
    fun tags(s: StockWatchlistInfo): List<String> = buildList {
        if (isQual(s)) add("QUAL")
        if (isVal(s)) add("VAL")
        if (isDiv(s)) add("DIV")
        if (isMom(s)) add("MOM")
        if (isSup(s)) add("SUP")
        if (isGapUp(s)) add("GAP")
        // OS = "extreme oversold" (RSI < 30), stricter than SUP's signal-based entry
        if ((s.portfolio.rsi ?: 50.0) < TradingConstants.RSI_OVERSOLD - 5.0) add("OS")
    }
}
