# TradingMate

TradingMate is a personal investing companion for the Thai stock market (SET). It helps retail investors build wealth with a low-cost index core, keep any stock picking small and disciplined, and see honestly whether that stock picking is worth it.

## 🚀 Concept & Idea

The core philosophy is **evidence over excitement**. A 2015-2025 market-wide backtest found that the app's technical entry signals trailed simply holding TDEX (the SET50 ETF), and no tested signal rule passed the [evidence gate](docs/ADVISOR_EVALUATION.md). TradingMate is therefore built around three ideas:

- **Index core:** Most of your invested money sits in TDEX. A monthly DCA reminder tells you how many board lots your budget buys, fees included. Default split: 80% core, 20% satellite.
- **Disciplined satellite:** Individual stocks are a capped satellite. Position sizing, stops, concentration caps, saved plans and post-trade journaling keep each trade small and recorded. Technical signals are shown as context, not buy calls.
- **Honest scorecard:** The satellite is compared with the same cash flows replayed into TDEX. If it trails over two consecutive 12-month windows, the app suggests shrinking it.

It also covers the practical side of Thai investing:
- **Consolidated Tracking:** Merges your stock value and available cash into a single "Total Assets" view.
- **Dividend Focus:** Tracks "Yield on Cost" (YoC), the Section 47 bis tax credit, and upcoming XD dates.
- **Built with AI:** Architected and developed through a deep collaboration with Google's Gemini AI.

## ✨ Key Features

- **Core-Satellite Allocation:** TDEX is the core; everything else is satellite. Portfolio shows core % against your target with the TDEX amount needed to close the gap, and the Buy dialog warns when a stock purchase would push the satellite over its cap.
- **Core-first navigation:** The app opens on Portfolio (core allocation, DCA, scorecard); the Advisor opens on its Dividend view. The Swing tab is off by default (Settings > Swing tab in Advisor); while it is off, Swing holding alerts show on the Dividend view. Swing signals are labelled "Context, not a buy call" on the Watchlist, the Swing tab and stock details.
- **Monthly DCA Reminder:** On your chosen day (or the next trading session), a notification suggests whole TDEX board lots for your monthly amount, fees included.
- **Satellite Scorecard:** Stats compares the satellite with a shadow TDEX position fed the same journaled buys, sells and dividends: end-value gap and money-weighted annual return since the first fill and over two trailing 12-month windows. Holdings without complete fill records are excluded and listed.
- **Risk Tools:**
  - **Suggested Regime Cash Reserve**: Suggests keeping 15% (Bull), 30% (Sideways) or 50% (Bear) in cash. The Buy dialog warns when a satellite buy goes below it but does not block it: the reserve was never backtested, and market-regime filters reduced drawdown without improving returns in the tests (`docs/ADVISOR_EVALUATION.md`). Only buying more than the available cash is blocked.
  - **Mark-to-Market (MTM) Daily NAV Trajectory & Peak Drawdown**: Tracks real daily portfolio equity snapshots with interactive toggle between **MTM NAV (Daily)** and **Realized PnL (Monthly)**, computing true peak-to-trough mark-to-market Max Drawdown (MDD).
  - **Anti-Ruin Fixed Fractional Position Sizing**: Automatically computes exact share size per trade based on account risk budget ($1.0\%-2.0\%$) and stop-loss distance, rounded down to SET 100-share board lots.
  - **Concentration Shields**: Strictly enforces a **15% Single-Stock Allocation Limit** and **30% Sector Allocation Cap** (user-configurable in Settings).
- **Weekly Trend Check:** Daily candles are resampled to a **Weekly 20-EMA**; the **Weekly Bearish Guard** holds back counter-trend swing signals. Untested as an edge.
- **Thai Dividend Tax Shield (Section 47 bis):** Calculates reclaimable Corporate Income Tax (CIT 20%) credits ($\text{Gross} \times \frac{20}{80}$) and Net Yield-on-Cost ($\text{YoC}_{\text{net}}$) after 10% Withholding Tax.
- **Advisor (3-Step Guided Flow):**
  - **Today's Checklist & Step 1 · Check Your Holdings:** Holdings at a saved exit first (`🔴 Sell / Stop Reached`), then monthly dividend switches (`🟠 Monthly Switch`), then `🔵 FYI Notes`, or `"All Good Today"`. Odd-lot dust (`< 100` shares) and Core ETFs (`TDEX`/`1DIV`) do not trigger sell or monthly switch alerts.
  - **Step 2 · Monthly Dividend Plan (Top 10 SET50):** Shows the top 10 SET50 dividend stocks, a collapsible track record vs. TDEX, the **Monthly Buy / Sell Calculator**, and **Upcoming Dividend (XD) Dates**.
  - **Step 3 · Find Stock Ideas (Watchlist Screener):** Open by default with 1-tap **Quick Presets** (`💰 High Yield ≥5%` / `🟢 Ready to Buy`, `⭐ Strict (+Quality)`, `📋 All Watchlist`) and customizable filter chips.
- **High Dividend Yield List and Monthly Rebalance Plan:** The top 10 SET50 stocks by yield, the one stock rule that passed the evidence gate (backtest +7.80% a year vs TDEX +2.52%, inflated by survivorship; the real 1DIV ETF +3.86% vs +2.55%). The rebalance plan turns it into a review-only order list for your full-lot (`≥ 100` share) Dividend-purpose holdings: sell names that left the list, keep the rest unresized, buy new names with a tenth of the budget in whole lots. It says when the budget is too small to hold all ten and points to 1DIV. A monthly notification reminds you to review it. A forward record saves each month's list with its prices and compares it with TDEX after 1, 3 and 12 months, dividends included; names are fixed when saved, so delistings stay in the record (idea from tradermonty's signal-postmortem).
- **Odd-Lot (`< 100` Share) Dust Handling:** Leftover shares below 1 SET board lot (`1..99` shares, e.g., from a stock dividend) are marked `Odd lot (< 100 shares)`, ignored by sell alerts and the monthly rebalance calculator, exempted from the ฿50 non-ATS minimum commission in P/L, and can be cleared in 1 tap via **`Odd Lot / Clear 🧹`** in the Sell dialog.
- **Market Pulse:** Watchlist monitoring with multi-factor technical context (Technical Setup, On Watch, Exit Rule). Entry signals carry a "Context, not a buy call" notice; entry alerts are off by default.
- **Screenshot import:** The Buy dialog can read a broker trade screenshot with Gemini (`gemini-2.5-flash` default, user-supplied API key).
- **Saved Swing Plans:** Records an accepted entry, stop, target, and exit policy. Proposed buys are checked against net reward to risk, per-trade and total open stop risk (`maxOpenExposure`), concentration, cash reserve, and SET board lots. Broker fills can still be recorded when they differ from a proposal.
- **Local Trade Record and Backup:** Saves plan history, trades, fees, cash transactions, dividends, daily portfolio snapshots (365-day retention), and advice events in JSON backups. Older backups import with their available fields.
- **Consolidated Portfolio:** Professional financial dashboard grouping stock holdings, cash balance, net profit, per-position fee tracking, and sector risk meters in one unified view.
- **Trend State Chip:** Each stock shows a descriptive state (Uptrend, Downtrend, Overextended, Near Support, Range) from RSI, MACD, SMA 50 and Bollinger Bands. It describes price and never recommends; observed price levels and accepted plans provide stop and target alerts.
- **Precise Fee Engine:** Accurate net profit/loss tracking using the InnovestX fee structure (Commission 0.15% + Market Fee + VAT). Applies a ฿50 minimum commission per full-lot position unless ATS + E-Statement is enabled.
- **Design:** Ambient radial gradient glows, adaptive glassmorphic contrast borders, OpenType `tnum` tabular numbers for zero decimal jitter, Catmull-Rom $C^1$ smooth spline charts, and live directional price tick pulses.
- **Multi-Source Data Aggregator:** Blends real-time market data from the Stock Exchange of Thailand (SET) with historical coverage and metadata from Yahoo Finance.

## 🧅 Screening Filters (Research and Swing candidates)

The Research explorer and the Swing candidate check use SET-calibrated descriptive filters. None is a tested edge; the 0-100 score, its grades and the archetype chips (MOAT, SHIELD, WHALE and others) were removed on 2026-10-08 because they were never backtested.

- **Pre-filter:** liquid (daily turnover above ฿5M), fresh session matching the benchmark within 7 days, and **not within 5% of the 52-week low**.
- **Quality (SET-calibrated, opt-in chip in Screener):** ROE > 10%, net profit margin > 5% (null-tolerant), non-financial debt-to-equity < 2.0 (null-tolerant; banks, finance and insurers exempt), and 3Y profit growth ≥ 0% when available.
- **Dividend:** yield of at least 5%.
- **Foreign flow:** NVDR net buying with relative strength not badly lagging the SET ($\ge -1.0$).
- **Swing candidate:** passes the pre-filter and quality, has no sell signal, a completed weekly trend that is not bearish, momentum, support or a strong daily move, and a confirmed BUY; in a bearish market it also needs positive foreign flow or relative strength.

## 📊 Technical Indicators Used

TradingMate computes these indicators to describe each stock; in the 2015-2025 backtest its signals built on them trailed holding TDEX. For a deep dive into the formulas and thresholds, see **[INDICATORS.md](docs/INDICATORS.md)**.

1. **RSI (14 Days) - The Speedometer**
   - **Oversold (< 35):** Target BUY zone.
   - **Healthy Momentum (< 62):** Trend continuation entry zone when price > SMA 50, MACD > 0, and ADX ≥ 20.
   - **Overbought (> 65 / > 72 on `+1R` holdings):** Target SELL / exhaustion zone.
2. **MACD (12, 26, 9) - The Momentum Switch**
   - Tracks trend shifts. A positive histogram confirms early reversals.
3. **SMA 50 & SMA 200 - The Trend Guards**
   - **SMA 200:** Long-term "Line in the Sand" (Bull vs. Bear trend).
4. **Bollinger Bands - The Volatility Map**
   - Helps time entries near the **Lower Band** and exits near the **Upper Band**.
5. **OBV (On-Balance Volume) - The Lie Detector**
   - Rising OBV confirms that volume backs the rally (used to confirm "Early Recovery" buys).
6. **Relative Strength vs SET - The Race**
   - 3-month return vs the SET index.
7. **52-Week Low Guard - The Trap Detector**
   - Blocks actionable swing candidates within 5% of their 52-week low; risky signals can still appear separately for review.
8. **ATR (14) - The Breathing Room**
   - Volatility-adjusted stop loss (2× ATR, clamped -3.5% to -8%) and trailing stop (2.5× ATR).
9. **ADX (14) - The Regime Detector**
   - Suppresses momentum buys in chop (ADX < 20) and knife-catching in violent trends (ADX ≥ 40).
10. **Stochastic (14,3,3) - The Reversal Timer**
   - Oversold buys require %K to cross above %D — no entries while price is still falling.
11. **MFI (14) - The Smart Money Gauge**
   - Volume-weighted RSI: ≥ 80 triggers Distribution SELL on profits; ≤ 20 confirms capitulation buys.
12. **Market Regime and Suggested Cash Reserve**
   - Evaluates SET Index vs. SMA 50 and MACD. Suggests smaller Swing sizes in Bear markets and a cash reserve (**15% Bull, 30% Sideways, 50% Bear**), shown as warnings in the Buy dialog, not blocks. Untested: regime filters lowered drawdown but not returns in the backtests.
13. **Pre-Trade Risk:Reward & Invalidation Display**
   - Shows an estimated stop. A target and reward/risk appear only when an observed price level or saved target exists; levels are alert prices, not guaranteed fills.
14. **False Breakout Guard**
   - Downgrades Swing BUYs to POTENTIAL if foreign funds are dumping (NVDR selling > ฿5M) or on dry volume while lagging the SET index.
15. **Ex-Dividend (XD) Grace Period**
   - Pauses Early Breakdown Warning and Weak Trend sell alarms within $\pm 2$ trading days of XD date to prevent panic selling on expected cash dividend payouts.

## 🧠 The Trading Strategy (Standardized)

> These rules drive the satellite's technical context and exit alerts. They have **not** passed the evidence gate: in the portfolio backtest they returned -10.1% a year (2015-2025) against +2.5% for TDEX. Exit rules (stops, trailing stops, saved targets) are risk controls and stay on; entry signals are context only.

- **🟢 Buy - Oversold Accumulation:** RSI < 35 while above the long-term SMA 200 (Stoch %K > %D confirmed).
- **🟢 Buy - Early Recovery:** MACD turns positive near support with volume/MFI confirmation.
- **🟢 Buy - Healthy Momentum:** Positive MACD, RSI < 62, ADX ≥ 20, price above SMA 50, filtered against false breakouts and NVDR selling.
- **⚪ Context - Trend Weakening:** Below SMA 50 with negative MACD is a note, not a sell; the saved stop is the exit. Selling there was the largest loss source in the 2015-2025 SET50 replay. Near an XD date the dip is labelled as the dividend adjustment.
- **🔴 Sell - Saved Target:** New fixed swing plans alert when the saved target is reached. Legacy holdings retain their earlier profit rules until a plan is completed.
- **🔴 Sell - R-Multiple Target (legacy holdings):** Holdings without a saved plan take profit at 2R (twice the stop distance). Overbought (`RSI > 72` or Upper Band + `RSI > 65`) and MFI (`≥ 80`) exits apply only after +1R, and the trailing stop arms at +1R and exits before the trade turns into a loss.
- **🔴 Sell - Stop Loss:** Volatility-adjusted (2× ATR) or -4.5% (SET50) / -6.5% (Mid/Small-Cap).
- **💰 Dividend-purpose holdings:** Follow the tested high-yield rule: no price, trailing, quality or yield exits. The Advisor lists leaving the high-yield top 10 as the exit (acted on at the monthly review) and shows a dividend cut or suspension (the latest payment more than 10% below the one a year earlier, or none for 13 months), ROE below 15%, yield below 3% and a 20% drawdown as review notes. Only a stop you saved sends a notification.
- **🧹 Odd-lot holdings (`< 100` shares):** Leftover shares below 1 SET board lot (`1..99` shares) show `Odd lot (< 100 shares)` and are ignored by sell alerts and the monthly rebalance plan.
- **⏸️ Swing cooldown (advisory):** The Buy dialog warns on a new Swing buy after the last 2 Swing trades closed at a loss (for 24 hours) or when this month's Swing losses reach 8% of equity. Dividend-purpose and core buys are never warned, so the monthly rebalance and DCA are not paused. The thresholds are untested; it warns and never blocks. Adapted from tradermonty's drawdown-circuit-breaker (MIT).
- **⚠️ SELL PRIORITY:** Selling signals (Overbought/Resistance/Breakdown) ALWAYS override BUY momentum.

## 📊 Measuring Performance

The advisor has no established win rate or expected return. The per-stock in-app Backtest screen was removed on 2026-10-08: the market-wide test of the same signals trailed TDEX, and single-stock replays invite picking the lucky names. The local advice journal does not yet link every AI ranking to a later accepted plan. See [Evaluating advisor outcomes](docs/ADVISOR_EVALUATION.md) for a forward measurement protocol and the evidence gate.

A market-wide portfolio replay ([tools/backtest/report.md](tools/backtest/report.md)) runs each rule across 48 current SET50 stocks with shared capital, 1% risk per trade, the 15% stock cap, board lots, fees and 0.15% slippage per side:

| Rule (2015-2025) | CAGR | 2015-2020 | 2021-2025 | Max drawdown | Evidence gate |
|---|---|---|---|---|---|
| App signals | -10.1% | -5.1% | -15.5% | 72.5% | Fail |
| App entries + trend exit | -12.8% | -8.9% | -17.8% | 78.4% | Fail |
| 52-week breakout + trend exit | +11.1% | +23.8% | -1.0% | 38.0% | Fail (99% of P/L from 3 stocks) |
| TDEX buy-and-hold (benchmark) | +2.5% | +1.8% | +3.2% | 36.5% | - |

The universe is today's SET50, so results are flattered by survivorship bias. Reproduce with `python3 tools/backtest/fetch_history.py` then `BACKTEST=1 ./gradlew testDebugUnitTest --tests '*MarketBacktestReport*'`. The in-app Backtest screen replays a single stock over 3 years of history.

Market quotes and fundamentals have separate freshness checks. Failed refreshes retain usable cached values and identify affected symbols; check the last-sync time before acting. The Stats drawdown card uses daily account snapshots when there are two or more, else the realized P/L path. VaR, CVaR and beta were removed (2026-10-08): 63 days of data is too noisy to act on. The minimum net reward-to-risk threshold for proposed swing buys is configurable in Settings (default 2:1). Saved stops and targets are alert levels, not broker orders or guaranteed fill prices.

## 🔔 Notification Alerts

TradingMate sends push notifications during specific time windows on weekdays (Asia/Bangkok timezone). The background worker runs every **30 minutes**.

| Time / Trigger | Alert | Dedup | Description |
|---|---|---|---|
| **10:00–11:00 AM** | 🌅 Morning Swing Exit | Once/day | Check active swing positions for Sell / Stop Loss |
| **15:30–16:15 PM** | 🏙️ Afternoon Swing Entry | Once/day | Scan Advisor for new Swing & Gap candidates before close. **Off by default** (Settings > Entry signal alerts) |
| **DCA day or next trading session** | 📈 Monthly Core Buy | Once/month | TDEX board lots for your monthly DCA amount, fees included (when an amount is set) |
| **Within 7 days of XD** | 💰 Ex-Dividend Alert | Once/XD date | Per-stock alert when an ex-dividend date is approaching |
| **First trading session of the month** | 📊 High-Yield Review | Once/month | Review the high dividend yield rebalance plan. Only with a Dividend-purpose holding |
| **Any time (market open)** | 🚨 Sell Reminder | Per stock | Stop, trailing stop, saved-plan exit or SELL signal on a holding (Dividend-purpose holdings: saved stop only) |
| **Any time (market open)** | 🔔 Entry Signal | Per stock | New BUY/POTENTIAL on a non-held quality candidate. **Off by default** |

> **Deduplication rules:**
> - Morning / Afternoon alerts: once per trading day
> - Notifications are skipped on weekends. Stock-scan alerts are also skipped on public holidays (market closed).

## 🔏 Building a Signed Release

`./gradlew :app:assembleRelease` signs the APK when `local.properties` (not committed) contains:

```properties
KEYSTORE_FILE=/path/to/release.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

Without these keys it builds `app-release-unsigned.apk`. Keystore files (`*.jks`, `*.keystore`) are ignored by git.

## ⚠️ Disclaimer

TradingMate is designed for **educational and informational purposes only**. It does not constitute financial advice, investment recommendation, or a solicitation to buy or sell any securities.

- All trading and investment decisions are made **at your own risk**.
- The creators and developers of TradingMate are **not responsible** for any financial losses, damages, or consequences arising from the use of this application.
- Past performance does not guarantee future results.
- Always consult with a **qualified financial advisor** before making investment decisions.
