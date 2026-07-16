package apincer.mobile.tradings.ui

import apincer.mobile.tradings.domain.IndicatorSignal

/**
 * The 5-Layer Filter System (Stock DNA).
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
        if ((info.roe ?: 0.0) <= 15.0) return false
        if (info.debtToEquity?.let { it >= 1.5 } == true) return false
        if (info.netProfitMargin?.let { it <= 10.0 } == true) return false
        if (info.profitGrowth3Y?.let { it <= 10.0 } == true) return false
        return true
    }

    /** Layer 2 — Value: P/E 0.1–15.0 and P/BV 0.1–1.0. */
    fun isVal(s: StockWatchlistInfo): Boolean =
        (s.info.pe ?: 0.0) in 0.1..15.0 && (s.info.pbv ?: 0.0) in 0.1..1.0

    /** Layer 3 — Dividend: yield >= 5%. */
    fun isDiv(s: StockWatchlistInfo): Boolean =
        (s.info.dividendYield ?: 0.0) >= 5.0

    /** Layer 4 — Momentum: MACD histogram meaningfully positive (>0.1% of price)
     *  and RSI in 40–70 (not overbought). */
    fun isMom(s: StockWatchlistInfo): Boolean {
        val hist = s.portfolio.macdHist ?: 0.0
        val price = s.info.lastPrice
        return price > 0 && hist > price * 0.001 &&
               (s.portfolio.rsi ?: 50.0) in 40.0..70.0
    }

    /** Layer 5 — Support/Setup: BUY/POTENTIAL signal or RSI oversold (< 35). */
    fun isSup(s: StockWatchlistInfo): Boolean =
        s.signal?.type == IndicatorSignal.BUY ||
        s.signal?.type == IndicatorSignal.POTENTIAL ||
        (s.portfolio.rsi ?: 50.0) < 35.0

    /** Earnings gap-up play: +4% day on a quality or high-margin stock. */
    fun isGapUp(s: StockWatchlistInfo): Boolean =
        s.info.percentChange >= 4.0 && (isQual(s) || (s.info.netProfitMargin ?: 0.0) > 10.0)

    /** DNA tag chips displayed on stock cards. */
    fun tags(s: StockWatchlistInfo): List<String> = buildList {
        if (isQual(s)) add("QUAL")
        if (isVal(s)) add("VAL")
        if (isDiv(s)) add("DIV")
        if (isMom(s)) add("MOM")
        if (isSup(s)) add("SUP")
        if (isGapUp(s)) add("GAP")
        if ((s.portfolio.rsi ?: 50.0) < 30.0) add("OS")
    }
}
