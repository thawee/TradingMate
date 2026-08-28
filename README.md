# TradingMate

TradingMate is a personal trading companion app designed to simplify stock market analysis for retail investors, specifically tailored for the Thai stock market (SET). It bridges the gap between complex technical indicators and actionable trading decisions with a focus on discipline, dividend tracking, and human-AI collaboration.

## 🚀 Concept & Idea

The core philosophy of TradingMate is **Discipline over Emotion**. By converting standard technical indicators into visual "Zones," the app helps traders identify when a stock is in an accumulation phase (Buying Zone) or a distribution phase (Selling Zone). 

It specifically addresses common beginner challenges:
- **When to Buy/Sell:** Automates entry and exit price targets based on a strict RSI 35/65 strategy.
- **Consolidated Tracking:** Merges your stock value and available cash into a single "Total Assets" view.
- **Dividend Focus:** Tracks "Yield on Cost" (YoC) and alerts you to upcoming XD dates.
- **Built with AI:** Architected and developed through a deep collaboration with Google's Gemini AI.

## ✨ Key Features

- **Institutional Risk Management & CRO Suite:**
  - **1-Day 95% Historical Value-at-Risk (VaR)** and **Conditional VaR (CVaR / Expected Shortfall)** to model extreme downside tail risk in ฿ and %.
  - **Anti-Ruin Fixed Fractional Position Sizing:** Automatically computes exact share size per trade based on account risk budget ($1.0\%-2.0\%$) and stop-loss distance, rounded down to SET 100-share board lots.
  - **Concentration Shields:** Strictly enforces a **15% Single-Stock Allocation Limit** and **30% Sector Allocation Cap**.
  - **Max Drawdown (MDD) & High-Water Mark Tracker:** Visualizes all-time peak equity and drawdown recovery status in the Stats dashboard.
  - **63-Day Rolling Covariance Portfolio Beta ($\beta$):** Tracks systematic volatility vs. SET Index (*Defensive Low-Vol*, *Balanced*, or *Aggressive High-Beta*).
- **Multi-Timeframe (MTF) Macro Trend Alignment:** Daily candle resampling to compute **Weekly 20-EMA** macro trends. Prevents counter-trend daily whipsaw buys with the **Macro Weekly Bearish Guard** and awards the `MTF` confluence tag.
- **Thai Dividend Tax Shield (Section 47 bis):** Calculates reclaimable Corporate Income Tax (CIT 20%) credits ($\text{Gross} \times \frac{20}{80}$) and Net Yield-on-Cost ($\text{YoC}_{\text{net}}$) after 10% Withholding Tax.
- **Dividend Advisor:** A specialized planning dashboard that calculates required capital to reach passive income goals. Suggests high-yield "Dividend Stars" based on strict fundamental and solvency criteria.
- **Market Pulse:** Real-time monitoring of your watchlist with automated multi-factor technical signals (BUY, SELL, POTENTIAL).
- **AI Advisor:** Centralized AI discovery hub evaluating Swing, Gap, and Dividend opportunities. Integrates Google Gemini directly for structured recommendations with confidence scores.
- **Consolidated Portfolio:** Professional financial dashboard grouping stock holdings, cash balance, net profit, fee tracking, and sector risk meters in one unified view.
- **Automated Trading Zones:** Real-time calculation of "Buy Below" and "Sell Above" price ranges using RSI (35/65 targets).
- **Precise Fee Engine:** Accurate net profit/loss tracking using the InnovestX fee structure (Commission 0.15% + Market Fee + VAT). Applies a ฿50 minimum commission unless ATS + E-Statement is enabled.
- **10/10 FinTech Design System:** Ambient radial gradient glows, adaptive glassmorphic contrast borders, OpenType `tnum` tabular numbers for zero decimal jitter, Catmull-Rom $C^1$ smooth spline charts, and live directional price tick pulses.
- **Multi-Source Data Aggregator:** Blends real-time market data from the Stock Exchange of Thailand (SET) with historical coverage and metadata from Yahoo Finance.

## 🧅 The 6-Layer Filter & Quant Confluence Scoring (Stock DNA)

TradingMate doesn't just look at price; it evaluates the "DNA" of a company using an institutional 6-Layer multi-factor filter combined with a **0–100 Composite Confluence Scoring Engine**.

0. **Pre-Filter (Gate):** Every candidate must be liquid (daily turnover > ฿5M) and **not within 5% of its 52-week low** (avoids "cheap-looking" structural decliners).
1. **Qual (Quality - Max 25 pts):** Evaluates management efficiency and profitability.
   - *Metrics:* Tiered ROE (>15%, >10%), Net Profit Margin (>15%, >8%), D/E Ratio (≤1.0, ≤1.5), 3Y Profit Growth (>10%).
2. **Val (Value - Max 20 pts):** Identifies underpriced or fair-value stocks.
   - *Metrics:* P/E Ratio (0.1 to 15.0) and P/BV (0.1 to 1.2).
3. **Mom (Momentum & Trend - Max 25 pts):** Detects early trend shifts and positive price momentum.
   - *Metrics:* Price above 50-day SMA, Price above 200-day SMA, positive MACD histogram, RSI in healthy zone (40–65).
4. **Flow (Foreign Smart Money - Max 15 pts):** Tracks foreign institution support with **Dual-Confirmation Flow**.
   - *Metrics:* NVDR net accumulation coupled with non-lagging Relative Strength ($\text{RS} \ge -1.0$) to eliminate false smart money signals caused by foreign short-covering in downtrends.
5. **Div & Safety (Yield & Solvency - Max 15 pts):** High passive income and margin of safety.
   - *Metrics:* Dividend Yield ≥ 5.0% and 52-week low trap avoidance.
6. **Cyclical Sector Quality Shield (`CYC`):** Detects commodity/cyclical names (Energy, Petrochem, Agribusiness, Shipping, Steel) and applies a quality dampener if 3Y profit growth is volatile ($<8\%$) or margins are weak ($<10\%$) to prevent buying at peak commodity cycles.

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
   - Excludes stocks within 5% of their 52-week low from all candidate lists.
8. **ATR (14) - The Breathing Room**
   - Volatility-adjusted stop loss (2× ATR, clamped -3.5% to -8%) and trailing stop (2.5× ATR).
9. **ADX (14) - The Regime Detector**
   - Suppresses momentum buys in chop (ADX < 20) and knife-catching in violent trends (ADX ≥ 40).
10. **Stochastic (14,3,3) - The Reversal Timer**
   - Oversold buys require %K to cross above %D — no entries while price is still falling.
11. **MFI (14) - The Smart Money Gauge**
   - Volume-weighted RSI: ≥ 80 triggers Distribution SELL on profits; ≤ 20 confirms capitulation buys.
12. **Market Regime Gate & Cash Buffer Guidance**
   - Evaluates SET Index vs. SMA 50 and MACD. Enforces 50% defensive sizing in Bear markets and dynamic cash reserve buffers (10–15% Bull, 25–35% Sideways, 50%+ Bear).
13. **Pre-Trade Risk:Reward & Invalidation Display**
   - Calculates exact suggested Stop Loss (฿/-%), Target (฿/+%), and R:R ratio on every card before entry.
14. **False Breakout Guard**
   - Downgrades Swing BUYs to POTENTIAL if foreign funds are dumping (NVDR selling > ฿5M) or on dry volume while lagging the SET index.
15. **Ex-Dividend (XD) Grace Period**
   - Pauses Early Breakdown Warning and Weak Trend sell alarms within $\pm 2$ trading days of XD date to prevent panic selling on expected cash dividend payouts.

## 🧠 The Trading Strategy (Standardized)

- **🟢 Buy - Oversold Accumulation:** RSI < 35 while above the long-term SMA 200 (Stoch %K > %D confirmed).
- **🟢 Buy - Early Recovery:** MACD turns positive near support with volume/MFI confirmation.
- **🟢 Buy - Healthy Momentum:** Positive MACD, RSI < 55, price above SMA 50, filtered against false breakouts and NVDR selling.
- **🔴 Sell - Early Breakdown:** Triggers early exit if a position drops $\le -1.5\%$ and loses SMA 50 with negative MACD (suppressed near XD date).
- **🔴 Sell - Take Profit:** Triggers at >10% net profit or when RSI > 65 / MFI $\ge$ 80.
- **🔴 Sell - Stop Loss:** Volatility-adjusted (2× ATR) or -4.5% (SET50) / -6.5% (Mid/Small-Cap).
- **⚠️ SELL PRIORITY:** Selling signals (Overbought/Resistance/Breakdown) ALWAYS override BUY momentum.

## 🎯 Expected Performance

TradingMate is a **mean-reversion + trend-quality hybrid**: it buys oversold dips (RSI/Stochastic) only in healthy trends (SMA 200, ADX guards) on quality names (5-Layer DNA), and exits with fee-aware take-profits and ATR-sized stops.

| Signal Path | Guards Applied | Est. Win Rate* |
|---|---|---|
| Healthy Momentum BUY | MACD+, RSI 40–65, ADX ≥ 20, RS ≥ 0 | ~55–62% |
| Oversold Accumulation | RSI < 35 + above SMA 200 + Stoch %K > %D + ADX < 40 knife guard | ~58–65% |
| Early Recovery | Volume / MFI capitulation confirmation | ~50–58% |
| Full 6-Layer DNA pass | QUAL + VAL + DIV + MOM + SUP + FLOW + pre-filters | ~65–72% (rare) |

*Ranges based on published research for RSI mean-reversion with trend filters on equities. Unfiltered RSI dip-buying alone runs only ~45–52% — the ADX chop filter, 52-week-low trap guard, and Relative Strength gate are what push the odds above breakeven.

**Expectancy math:** average win ≈ +4.6% net of fees vs. average loss ≈ -5.4% (ATR stop + fees) → breakeven win rate ≈ **54%**. At a realistic 55–63% win rate, expectancy is roughly **+0.3 to +0.7% per trade**. Letting the ATR trailing stop run winners beyond +5% is where the real profit comes from.

> The ranges above are general research-backed estimates for the strategy family. For a **stock-specific, historical measurement**, use the in-app **Backtest** screen — it replays each stock's own price/indicator history against these exact DNA rules and reports actual win rate, average win/loss, and expectancy for that ticker.

## 📋 Swing Playbook (Daily Discipline Tracker)

The Swing Playbook is a 3-step daily workflow to keep traders disciplined during market hours. It appears at the bottom of the Smart Advisor screen as a floating step bar.

### The 3 Steps

| Step | Name | What It Does |
|------|------|--------------|
| 1 | 🚨 Check Exits | Review sell alerts — Take Profit (≥10%), Stop Loss (≤-5%), Overbought (RSI ≥65), SELL signal |
| 2 | 🔍 Scan Setups | Review swing/gap candidates filtered by Quality, Momentum, and Support criteria |
| 3 | 🤖 Ask AI | Copy an AI prompt to clipboard for external analysis (ChatGPT/Gemini/Claude), or tap "Analyze with AI" for an in-app Gemini call returning ranked picks with a Confidence Score (0–100%) |

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
| **15:30–16:15 PM** | 🏙️ Afternoon Swing Entry | Once/day | Scan Advisor for new Swing & Gap candidates before close |
| **Within 7 days of XD** | 💰 Ex-Dividend Alert | Once/XD date | Per-stock alert when an ex-dividend date is approaching |
| **Jan & Jun, 09:00–17:00** | 📅 Dividend Season | Once/season | Start accumulating for upcoming payout season |
| **Any month** | 💰 Yield Opportunity | Once/week/stock | DIVIDEND stock yield ≥ 5% + ROE ≥ 15% — good accumulation price |
| **Any time (market open)** | 🚨 Signal Change / Sell Reminder | Per stock | BUY→SELL or active SELL signal detected |

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
