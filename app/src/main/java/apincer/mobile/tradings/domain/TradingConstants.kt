package apincer.mobile.tradings.domain

object TradingConstants {
    // Take Profit Targets
    const val TAKE_PROFIT_PERCENT = 5.0
    const val TAKE_PROFIT_MIN_BAHT = 500.0
    const val TAKE_PROFIT_R_MULTIPLE = 2.0     // legacy target = 2 × stop distance (1R)
    
    // Stop Loss / Drawdown Limits
    const val STOP_LOSS_PERCENT = -5.0
    const val STOP_LOSS_SET50_PERCENT = -4.5
    const val STOP_LOSS_MID_SMALL_PERCENT = -6.5
    const val DIVIDEND_DEEP_DRAWDOWN_PERCENT = -20.0
    
    // RSI Thresholds
    const val RSI_OVERSOLD = 35.0
    const val RSI_OVERBOUGHT = 65.0
    const val RSI_MOMENTUM_MAX = 64.9
    const val RSI_POTENTIAL = 42.0

    // ATR-based Risk (volatility-adjusted stops)
    const val ATR_STOP_MULTIPLIER = 2.0        // stop loss = 2× daily ATR%
    const val ATR_STOP_MIN_PERCENT = 3.5       // clamp: never tighter than -3.5%
    const val ATR_STOP_MAX_PERCENT = 8.0       // clamp: never wider than -8.0%
    const val ATR_TRAILING_MULTIPLIER = 2.5    // trailing stop = 2.5× daily ATR%
    const val ATR_TRAILING_MIN_PERCENT = 4.0
    const val ATR_TRAILING_MAX_PERCENT = 10.0

    // ADX Trend-Strength Regime Thresholds
    const val ADX_TREND_CONFIRM = 20.0         // below = chop, no trend to follow
    const val ADX_STRONG_TREND = 40.0          // above = violent move, don't knife-catch

    // Stochastic Thresholds (14,3,3)
    const val STOCH_OVERSOLD = 20.0            // %K below = oversold zone

    // MFI (Money Flow Index) Thresholds
    const val MFI_DISTRIBUTION = 80.0          // above = overbought with real selling volume
    const val MFI_CAPITULATION = 20.0          // below = oversold with heavy volume
    
    // Fundamental Thresholds
    const val ROE_MIN_THRESHOLD = 15.0
    const val DIVIDEND_YIELD_ENTRY = 5.0
    const val DIVIDEND_YIELD_PROTECTION = 3.0

    // Liquidity Thresholds (daily turnover, THB)
    // Pre-filter gate for all candidate lists (Swing, Dividend, Gap-Up, Speculative).
    // Raised from ฿1M: at that level, thin small-caps can pass while still having
    // bid-ask spreads too wide for a stop-loss/take-profit order to fill at the
    // displayed price. Aligned with the Gap-Up play's existing ฿5M bar.
    const val MIN_LIQUIDITY_TURNOVER_BAHT = 5_000_000.0

    // Portfolio Risk & Concentration Limits
    const val MAX_SINGLE_STOCK_ALLOCATION_PERCENT = 15.0 // Maximum portfolio exposure in any single ticker
    const val MAX_SECTOR_ALLOCATION_PERCENT = 30.0       // Maximum portfolio exposure in any single industry sector

    // Multi-Timeframe Macro Trend Parameters
    const val WEEKLY_EMA_PERIOD = 20                     // 20-week EMA line in the sand for macro bull/bear regime

    // Thai Dividend Tax Shield Constants (Section 47 bis)
    const val DEFAULT_CIT_TAX_RATE = 20.0                // Thai standard Corporate Income Tax rate (20%)
    const val THAI_DIVIDEND_WHT_RATE = 10.0              // Standard Thai dividend withholding tax rate (10%)

    // SET50 Benchmark Components for Market Cap Tiering
    // SET50 constituents for July 1 - December 31, 2026: SET's H1 2026 list (SET50_100_H1_2026.pdf,
    // updated 2025-12-15) with the 2H 2026 review applied (out: BTS, CBG, CENTEL, SAWAD; in: BCP,
    // MRDIYT, TFG, THAI). Refresh each June and December. Backtests use tools/backtest/universe.txt.
    val SET50_SYMBOLS = setOf(
        "ADVANC", "AOT", "AWC", "BANPU", "BBL", "BCP", "BDMS", "BEM", "BH", "BJC",
        "CCET", "COM7", "CPALL", "CPF", "CPN", "CRC", "DELTA", "EGCO", "GPSC", "GULF",
        "HMPRO", "IVL", "KBANK", "KKP", "KTB", "KTC", "LH", "MINT", "MRDIYT", "MTC",
        "OR", "OSP", "PTT", "PTTEP", "PTTGC", "RATCH", "SCB", "SCC", "SCGP", "TCAP",
        "TFG", "THAI", "TIDLOR", "TISCO", "TLI", "TOP", "TRUE", "TTB", "TU", "WHA"
    )
}
