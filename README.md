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
- **Core-first navigation:** The app opens on Portfolio (core allocation, DCA, scorecard); the Advisor opens on its Dividend tab. Swing signals are labelled "Context, not a buy call" on the Watchlist, the Swing tab and stock details.
- **Monthly DCA Reminder:** On your chosen day (or the next trading session), a notification suggests whole TDEX board lots for your monthly amount, fees included.
- **Satellite Scorecard:** Stats compares the satellite with a shadow TDEX position fed the same journaled buys, sells and dividends: end-value gap and money-weighted annual return since the first fill and over two trailing 12-month windows. Holdings without complete fill records are excluded and listed.
- **Institutional Risk Management & CRO Suite:**
  - **63-Day Rolling Time-Series VaR (95%) & Conditional VaR (CVaR)**: Constructs a true weighted daily return time series across 63 trading days ($R_{p,t} = \sum_i w_i R_{i,t} / \sum_i w_i$) to evaluate empirical downside tail risk and expected shortfall in ฿ and %.
  - **Dynamic Regime-Aware Cash Buffers & Spendable Cash Engine**: Mathematically mandates cash buffer reserves (**15% in Bull, 30% in Sideways, 50% in Bear**). Enforces real-time cash guards in `BuyStockDialog` and injects exact spendable capital limits into Gemini AI prompts.
  - **Mark-to-Market (MTM) Daily NAV Trajectory & Peak Drawdown**: Tracks real daily portfolio equity snapshots with interactive toggle between **MTM NAV (Daily)** and **Realized PnL (Monthly)**, computing true peak-to-trough mark-to-market Max Drawdown (MDD).
  - **Anti-Ruin Fixed Fractional Position Sizing**: Automatically computes exact share size per trade based on account risk budget ($1.0\%-2.0\%$) and stop-loss distance, rounded down to SET 100-share board lots.
  - **Concentration Shields**: Strictly enforces a **15% Single-Stock Allocation Limit** and **30% Sector Allocation Cap** (user-configurable in Settings).
  - **63-Day Rolling Covariance Portfolio Beta ($\beta$):** Tracks systematic volatility vs. SET Index (*Defensive Low-Vol*, *Balanced*, or *Aggressive High-Beta*).
- **Multi-Timeframe (MTF) Macro Trend Alignment:** Daily candle resampling to compute **Weekly 20-EMA** macro trends. Prevents counter-trend daily whipsaw buys with the **Macro Weekly Bearish Guard** and awards the `MTF` confluence tag.
- **Thai Dividend Tax Shield (Section 47 bis):** Calculates reclaimable Corporate Income Tax (CIT 20%) credits ($\text{Gross} \times \frac{20}{80}$) and Net Yield-on-Cost ($\text{YoC}_{\text{net}}$) after 10% Withholding Tax.
- **Dividend Advisor:** A specialized planning dashboard that calculates required capital to reach passive income goals. Suggests high-yield "Dividend Stars" based on strict fundamental and solvency criteria (untested list).
- **High Dividend Yield List and Monthly Rebalance Plan:** The top 10 SET50 stocks by yield, the one stock rule that passed the evidence gate (backtest +7.80% a year vs TDEX +2.52%, inflated by survivorship; the real 1DIV ETF +3.86% vs +2.55%). The rebalance plan turns it into a review-only order list for your Dividend-purpose holdings: sell names that left the list, keep the rest unresized, buy new names with a tenth of the budget in whole lots. It says when the budget is too small to hold all ten and points to 1DIV. A monthly notification reminds you to review it.
- **Market Pulse:** Watchlist monitoring with multi-factor technical context (Technical Setup, On Watch, Exit Rule). Entry signals carry a "Context, not a buy call" notice; entry alerts are off by default.
- **AI Advisor:** Explains and ranks locally validated swing setups as context. The backtest result is shown first, and saving a plan is a secondary action ("Save as Satellite Plan"). Model assessments are qualitative and do not represent win probabilities.
- **Saved Swing Plans:** Records an accepted entry, stop, target, and exit policy. Proposed buys are checked against net reward to risk, stop risk, concentration, cash reserve, and SET board lots. Broker fills can still be recorded when they differ from a proposal.
- **Local Trade Record and Backup:** Saves plan history, trades, fees, cash transactions, dividends, daily portfolio snapshots, and advice events in JSON backups. Older backups import with their available fields.
- **Consolidated Portfolio:** Professional financial dashboard grouping stock holdings, cash balance, net profit, fee tracking, and sector risk meters in one unified view.
- **Trend State Chip:** Each stock shows a descriptive state (Uptrend, Downtrend, Overextended, Near Support, Range) from RSI, MACD, SMA 50 and Bollinger Bands. It describes price and never recommends; observed price levels and accepted plans provide stop and target alerts.
- **Precise Fee Engine:** Accurate net profit/loss tracking using the InnovestX fee structure (Commission 0.15% + Market Fee + VAT). Applies a ฿50 minimum commission unless ATS + E-Statement is enabled.
- **10/10 FinTech Design System:** Ambient radial gradient glows, adaptive glassmorphic contrast borders, OpenType `tnum` tabular numbers for zero decimal jitter, Catmull-Rom $C^1$ smooth spline charts, and live directional price tick pulses.
- **Multi-Source Data Aggregator:** Blends real-time market data from the Stock Exchange of Thailand (SET) with historical coverage and metadata from Yahoo Finance.

## 🧅 The 6-Layer Filter & Quant Confluence Scoring (Stock DNA)

TradingMate evaluates a company using a six-factor screening heuristic and a **0–100 Composite Confluence Score**. These scores are descriptive filters, not calibrated probabilities or evidence of future returns.

0. **Pre-Filter (Gate):** Every candidate must be liquid (daily turnover > ฿5M) and **not within 5% of its 52-week low** (avoids "cheap-looking" structural decliners).
1. **Qual (Quality - Max 25 pts):** Evaluates management efficiency and profitability.
   - *Metrics:* Tiered ROE (>15%, >10%), Net Profit Margin (>15%, >8%), D/E Ratio (≤1.0, ≤1.5). Three-year profit growth is shown as unavailable until the source periods can be verified.
2. **Val (Value - Max 20 pts):** Identifies underpriced or fair-value stocks.
   - *Metrics:* P/E Ratio (0.1 to 15.0) and P/BV (0.1 to 1.2).
3. **Mom (Momentum & Trend - Max 25 pts):** Detects early trend shifts and positive price momentum.
   - *Metrics:* Price above 50-day SMA, Price above 200-day SMA, positive MACD histogram, RSI in healthy zone (40–65).
4. **Flow (Foreign Smart Money - Max 15 pts):** Tracks foreign institution support with **Dual-Confirmation Flow**.
   - *Metrics:* NVDR net accumulation coupled with non-lagging Relative Strength ($\text{RS} \ge -1.0$) to eliminate false smart money signals caused by foreign short-covering in downtrends.
5. **Div & Safety (Yield & Solvency - Max 15 pts):** High passive income and margin of safety.
   - *Metrics:* Dividend Yield ≥ 5.0% and 52-week low trap avoidance.
6. **Cyclical Sector Quality Shield (`CYC`):** Detects commodity/cyclical names (Energy, Petrochem, Agribusiness, Shipping, Steel). Missing verified growth data is treated conservatively rather than inferred from adjacent rows.

### 🎯 Strategy Archetypes (One-Tap Presets)
- **🚀 VCP Breakout (`VCP`):** Stage 2 Uptrend ($Price \ge SMA 50 \ge SMA 200$) with momentum & volume breakout.
- **💎 Compounder Aristocrat (`MOAT`):** High ROE ($\ge 12\%$), low debt ($D/E \le 1.2$), and sustainable dividend track record (excluding cyclical peak traps).
- **🛡️ High-Yield Shield (`SHIELD`):** Safe high-yield ($\ge 5\%$) protected by profitability gates.
- **🐋 Foreign Whale Inflow (`WHALE`):** Heavy NVDR net foreign buying paired with positive Relative Strength ($\text{RS} \ge 0.0$).
- **⚡ Oversold Spring (`SPRING`):** Extreme oversold mean-reversion ($RSI \le 35$) with fundamental quality protection.

**Conviction Grading:**
- **`A+` (80–100):** *Prime Alpha Setup* (Mint Green)
- **`A` (65–79):** *High Conviction* (Pro Blue)
- **`B` (50–64):** *Watchlist / Developing* (Gold)
- **`C` (< 50):** *Neutral / Below Threshold* (Slate)

## 📊 Technical Indicators Used

TradingMate uses a suite of indicators to generate high-conviction signals. For a deep dive into the formulas and thresholds, see **[INDICATORS.md](docs/INDICATORS.md)**.

1. **RSI (14 Days) - The Speedometer**
   - **Oversold (< 35):** Target BUY zone.
   - **Overbought (> 65):** Target SELL zone.
2. **MACD (12, 26, 9) - The Momentum Switch**
   - Tracks trend shifts. A positive histogram confirms early reversals.
3. **SMA 50 & SMA 200 - The Trend Guards**
   - **SMA 200:** Long-term "Line in the Sand" (Bull vs. Bear trend).
4. **Bollinger Bands - The Volatility Map**
   - Helps time entries near the **Lower Band** and exits near the **Upper Band**.
5. **OBV (On-Balance Volume) - The Lie Detector**
   - Rising OBV confirms that volume backs the rally (used to confirm "Early Recovery" buys).
6. **Relative Strength vs SET - The Race**
   - 3-month return vs the SET index; positive RS stocks earn the "RS" DNA tag.
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
12. **Market Regime Gate & Dynamic Cash Buffer Enforcement**
   - Evaluates SET Index vs. SMA 50 and MACD. Enforces 50% defensive sizing in Bear markets and dynamic cash reserve buffers (**15% Bull, 30% Sideways, 50% Bear**). Integrates spendable cash limits into Gemini AI prompts and live liquidity breach warnings into the Buy Order dialog.
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
- **🟢 Buy - Healthy Momentum:** Positive MACD, RSI < 55, price above SMA 50, filtered against false breakouts and NVDR selling.
- **⚪ Context - Trend Weakening:** Below SMA 50 with negative MACD is a note, not a sell; the saved stop is the exit. Selling there was the largest loss source in the 2015-2025 SET50 replay. Near an XD date the dip is labelled as the dividend adjustment.
- **🔴 Sell - Saved Target:** New fixed swing plans alert when the saved target is reached. Legacy holdings retain their earlier profit rules until a plan is completed.
- **🔴 Sell - R-Multiple Target (legacy holdings):** Holdings without a saved plan take profit at 2R (twice the stop distance). Overbought, MFI and upper-band exits apply only after +1R, and the trailing stop arms at +1R and exits before the trade turns into a loss.
- **🔴 Sell - Stop Loss:** Volatility-adjusted (2× ATR) or -4.5% (SET50) / -6.5% (Mid/Small-Cap).
- **💰 Dividend-purpose holdings:** Follow the tested high-yield rule: no price, trailing, quality or yield exits. The Advisor lists leaving the high-yield top 10 as the exit (acted on at the monthly review) and shows ROE below 15%, yield below 3% and a 20% drawdown as review notes. Only a stop you saved sends a notification.
- **⚠️ SELL PRIORITY:** Selling signals (Overbought/Resistance/Breakdown) ALWAYS override BUY momentum.

## 📊 Measuring Performance

The advisor has no established win rate or expected return. The in-app Backtest replays historical technical signals only. It excludes past fundamental/flow observations, AI rankings, saved plans, portfolio limits, spread and slippage, so its results do not measure the complete advisor workflow. Signals fill at the next daily close in the simulation; daily drawdown includes open positions, while reported total return covers closed trades. The local advice journal does not yet link every AI ranking to a later accepted plan. See [Evaluating advisor outcomes](docs/ADVISOR_EVALUATION.md) for a forward measurement protocol and the evidence gate.

A market-wide portfolio replay ([tools/backtest/report.md](tools/backtest/report.md)) runs each rule across 48 current SET50 stocks with shared capital, 1% risk per trade, the 15% stock cap, board lots, fees and 0.15% slippage per side:

| Rule (2015-2025) | CAGR | 2015-2020 | 2021-2025 | Max drawdown | Evidence gate |
|---|---|---|---|---|---|
| App signals | -10.1% | -5.1% | -15.5% | 72.5% | Fail |
| App entries + trend exit | -12.8% | -8.9% | -17.8% | 78.4% | Fail |
| 52-week breakout + trend exit | +11.1% | +23.8% | -1.0% | 38.0% | Fail (99% of P/L from 3 stocks) |
| TDEX buy-and-hold (benchmark) | +2.5% | +1.8% | +3.2% | 36.5% | - |

The universe is today's SET50, so results are flattered by survivorship bias. Reproduce with `python3 tools/backtest/fetch_history.py` then `BACKTEST=1 ./gradlew testDebugUnitTest --tests '*MarketBacktestReport*'`. The in-app Backtest screen replays a single stock over 3 years of history.

Market quotes and fundamentals have separate freshness checks. Failed refreshes retain usable cached values and identify affected symbols; check the last-sync time before acting. The Stats risk panel uses date-aligned history and shows VaR/CVaR or beta as unavailable when the necessary observations are missing or stale. The minimum net reward-to-risk threshold for proposed swing buys is configurable in Settings (default 2:1). Saved stops and targets are alert levels, not broker orders or guaranteed fill prices.

## 📋 Swing Playbook (Daily Discipline Tracker)

The Swing Playbook is a 3-step daily workflow to keep traders disciplined during market hours. It appears at the bottom of the Smart Advisor screen as a floating step bar.

### The 3 Steps

| Step | Name | What It Does |
|------|------|--------------|
| 1 | 🚨 Check Exits | Review saved stop/target alerts, early breakdown warnings and legacy position alerts |
| 2 | 🔍 Scan Setups | Review eligible swing and strong daily move candidates, with missing targets identified |
| 3 | 🤖 Ask AI | Copy an analysis prompt or ask Gemini to explain and rank locally validated plans; its assessment is qualitative |

- Each step has a checkbox. Tapping **"Next →"** scrolls to the next step.
- When all 3 steps are checked, the bar shows **"✅ All 3 steps done! You're ready to trade."**
- If no sell alerts exist, Step 1 auto-marks as done.

### Reset Schedule

All 3 steps reset **daily after market close (16:30)**:

- **Before 16:30** (trading hours): Steps are still valid for today — no reset.
- **After 16:30** (market closed): Opening the app triggers a fresh reset for the new trading day.

This ensures your checklist stays intact during market hours and starts clean the next day.

> Note: The Dividend Playbook does not have step tracking — only the Swing Playbook follows this discipline workflow. Dividend stocks require multiple reviews throughout the day and across the week (e.g., monitoring yield changes, XD dates, fundamental shifts), so a single daily checklist is not feasible for this strategy.

---

## 🔔 Notification Alerts

TradingMate sends push notifications during specific time windows on weekdays (Asia/Bangkok timezone). The background worker runs every **30 minutes**.

| Time / Trigger | Alert | Dedup | Description |
|---|---|---|---|
| **10:00–11:00 AM** | 🌅 Morning Swing Exit | Once/day | Check active swing positions for Sell / Stop Loss |
| **15:30–16:15 PM** | 🏙️ Afternoon Swing Entry | Once/day | Scan Advisor for new Swing & Gap candidates before close. **Off by default** (Settings > Entry signal alerts) |
| **DCA day or next trading session** | 📈 Monthly Core Buy | Once/month | TDEX board lots for your monthly DCA amount, fees included (when an amount is set) |
| **Within 7 days of XD** | 💰 Ex-Dividend Alert | Once/XD date | Per-stock alert when an ex-dividend date is approaching |
| **Jan & Jun, 09:00–17:00** | 📅 Dividend Season | Once/season | Start accumulating for upcoming payout season |
| **Any month** | 💰 Yield Opportunity | Once/week/stock | DIVIDEND stock yield ≥ 5% + ROE ≥ 15% — good accumulation price |
| **First trading session of the month** | 📊 High-Yield Review | Once/month | Review the high dividend yield rebalance plan. Only with untested lists on and a Dividend-purpose holding |
| **Any time (market open)** | 🚨 Sell Reminder | Per stock | Stop, trailing stop, saved-plan exit or SELL signal on a holding (Dividend-purpose holdings: saved stop only) |
| **Any time (market open)** | 🔔 Entry Signal | Per stock | New BUY/POTENTIAL on a non-held quality candidate. **Off by default** |

> **Deduplication rules:**
> - Morning / Afternoon alerts: once per trading day
> - Dividend Season: once per season (January key, June key) — fires on first qualifying business day
> - Yield Opportunity: at most once per ISO week per stock
> - Notifications are skipped on weekends. Stock-scan alerts are also skipped on public holidays (market closed).

## ⚠️ Disclaimer

TradingMate is designed for **educational and informational purposes only**. It does not constitute financial advice, investment recommendation, or a solicitation to buy or sell any securities.

- All trading and investment decisions are made **at your own risk**.
- The creators and developers of TradingMate are **not responsible** for any financial losses, damages, or consequences arising from the use of this application.
- Past performance does not guarantee future results.
- Always consult with a **qualified financial advisor** before making investment decisions.
