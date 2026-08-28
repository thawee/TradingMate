# TradingMate Requirements

## 1. Project Overview
TradingMate is a high-performance personal trading companion specifically designed for the Thai Stock Market (SET). The application aims to provide retail investors with simplified technical analysis, automated trading signals, and consolidated portfolio tracking using a modern, discipline-focused approach.

## 2. Functional Requirements

### 2.1 Market Data & Analysis
- **Real-time Quotes:** Must fetch and display real-time price updates for SET stocks (Symbol, Last Price, Change, %Change).
- **Fundamental Metrics:** Must display core financial data including P/E, P/BV, ROE, Net Profit, EPS, and D/E Ratio.
- **Dividend Tracking:** Must track and display Dividend Yield, upcoming XD dates, and Dividend Per Share.
- **Technical Indicator Engine:**
    - **RSI (14):** Speedometer for overbought (>65) and oversold (<35) conditions.
    - **SMA (50/200):** Trend guards for mid-term and long-term market health.
    - **MACD:** Momentum confirmation for early reversals.
    - **Bollinger Bands:** Volatility mapping for undervaluation detection.
- **Smart Signal Logic:** Generate actionable signals (BUY, SELL, POTENTIAL, NEUTRAL) based on combined technical and fundamental criteria.
- **Cache-first Refresh:** Show cached data instantly, refresh from API only if stale (>1 hour). Portfolio screen refreshes only portfolio stocks (quantity > 0).

### 2.2 Five-Layer Filter System (Stock DNA)
A strict 5-layer filter to classify stocks into Swing Plays or Dividend Stars:

0. **Pre-filter (Liquidity):** Daily turnover (last price × volume) > ฿5,000,000. Ensures stop-loss orders can execute at displayed prices.
1. **Qual (Quality):** Evaluates management efficiency and profitability.
   - Indicators: ROE > 15%, Net Profit Margin > 10%, D/E Ratio < 1.5, Profit Growth (3Y) > 10%.
2. **Val (Value):** Identifies underpriced or fair-value stocks.
   - Indicators: P/E Ratio (0.1 to 15.0) and P/BV (0.1 to 1.0).
3. **Div (Dividend):** Highlights strong passive income generators.
   - Indicators: Dividend Yield ≥ 5.0%.
4. **Mom (Momentum):** Detects early trend shifts and positive price momentum.
   - Indicators: MACD Histogram > 0.1% of last price (price-relative threshold to avoid noise-level crossover flicker), RSI in 40–70 (confirms healthy momentum, excludes overbought).
5. **Sup (Support / Setup):** Locates ideal entry zones via signal confirmation.
   - Indicators: BUY or POTENTIAL zone signals (which incorporate SMA 200 trend context checks, avoiding "falling knife" entries in structural downtrends).

**Combination Rules:**
- **Swing Plays:** Must pass `Liquidity AND Quality AND (Momentum OR Support)`. Quality is mandatory — cheap-but-bad stocks (Value-only) are excluded to protect win-rate.
- **Dividend Stars:** Must pass `Liquidity AND Dividend AND Quality`.
- **Gap Plays:** Must pass `Liquidity AND percentChange ≥ 4% AND basic profitability (ROE > 10% or NPM > 5%)`. Decoupled from strict historical Quality to capture turnaround earnings catalysts.
- **Speculative Plays:** Must pass `Liquidity AND (NOT Quality) AND Support`. Higher-risk BUY/POTENTIAL setups on stocks that pass the liquidity gate but fail the Quality layer; sorted with MACD-confirmed setups ranked above unconfirmed ones.
- **Liquidity/Trap Risk:** Any not-yet-owned stock with a live BUY/POTENTIAL signal that fails the Pre-filter (illiquid turnover and/or within 5% of its 52-week low). Surfaced separately with a stronger warning rather than silently hidden, since it's the highest-risk bucket.

### 2.3 Portfolio Management & Institutional Risk Suite
- **Consolidated Equity:** Calculate and display Total Assets by merging Stock Holdings and Cash Balance.
- **Transaction Recording:** Allow users to record buy and sell transactions with entry price and quantity.
- **Fee Engine:** Automatically calculate trading fees using the InnovestX structure (Commission 0.15%, Market Fee, VAT). Applies a ฿50/day minimum commission unless ATS + E-Statement is enabled (waived). Financial Transaction Tax (FTT) is ฿0 — officially abolished.
- **Profit/Loss Tracking:** Display Gross and Net Profit/Loss in both currency (THB) and percentage.
- **Cash Management & Audit Ledger:** Maintain double-entry cash adjustments and cash audit log across buy/sell/undo operations.
- **Quantitative Risk Management Suite:**
  - **1-Day 95% Historical Value-at-Risk (VaR):** Real-time empirical quantile estimation of daily downside risk.
  - **Conditional VaR (CVaR / Expected Shortfall):** Average tail loss estimation during extreme market corrections.
  - **Max Drawdown (MDD) & High-Water Mark (HWM):** All-time peak equity curve tracking and recovery progress meter.
  - **Anti-Ruin Fixed Fractional Position Sizing:** Automatically calculates recommended position sizes according to exact stop-loss distance and account risk budget (1.0%–2.0%), rounded to 100-share SET board lots and clamped by the 15% single-stock ceiling.
  - **Concentration Caps:** Enforce a **15% Single-Stock Allocation Limit** and a **30% Sector Allocation Cap** with warning alerts.
  - **63-Day Rolling Portfolio Beta ($\beta$):** Tracks systematic portfolio volatility vs. the SET Index.
- **Thai Dividend Tax Shield (Section 47 bis):** Computes estimated reclaimable Corporate Income Tax (CIT 20%) credits and Net Yield-on-Cost after 10% Withholding Tax.

### 2.4 Watchlist Management
- **Multi-source Search:** Enable searching for stocks using both SET and Yahoo Finance data.
- **SET Collections Import:** Enable one-click import of curated stock groups (SET50, SET100, SETHD, Dividend Stars, Bluechips).
- **Dynamic Sorting & Filtering:** Sort stocks by Symbol, Change %, or Signal priority (BUY > POTENTIAL > SELL > MONITOR) with direction toggle, search filter, fast scroll index track, and auto-scroll to top on sort change.

### 2.5 Smart Advisor
- **Playbook Modes:** Two modes — Swing Playbook and Dividend Playbook.
- **3-Step Routine (SWING):**
    1. **Ask AI** — Copy AI prompt to clipboard, or tap "Analyze with AI" for an in-app call (auto-marks step as done either way).
    2. **Check Exits** — Display sell alerts based on technical conditions (Take Profit, Stop Loss, Overbought, Yield Drop).
    3. **Scan Setups** — Display candidate stocks (Swing/Dividend, Speculative, and Liquidity/Trap Risk) filtered by Quality, Momentum, Value, and Gap criteria using the Five-Layer Filter System. This is the single screen for "what should I consider buying" — no BUY-signal stock is filtered out of the app entirely; it always lands in one of these three buckets.
- **AI Prompt Generation:** Generate structured prompts for ChatGPT/Gemini with candidate data (including Speculative and Liquidity/Trap Risk categories), risk constraints, and playbook rules. Prompts request a Confidence Score (0–100%) per AI-ranked pick with justification.
- **In-App AI Analysis:** "Analyze with AI" button calls Google Gemini directly (structured JSON output) using a user-supplied API key (Settings > AI Integration), returning an executive summary and ranked recommendations with a color-coded Confidence Score, without needing to copy/paste into an external tool. Unlike the copy/paste prompt, this direct call has no live web/news access — it only reasons over the data in the prompt.
- **Selectable Gemini Model:** Users can pick which free-tier Gemini model powers the in-app analysis (Settings > AI Integration), and refresh the list live from Gemini's ListModels API to pick up newly released or soon-to-be-retired models automatically.
- **Push Notifications:** Morning exit alerts (10:00-11:00 AM) and afternoon entry reminders (15:30-16:30).
- **Afternoon Badge:** Visual indicator on Step 2 when afternoon scan notification has fired.
- **Auto-mark:** Step 1 auto-checks when no sell alerts exist. Step 3 auto-checks when AI prompt is copied.
- **Wizard Step Bar:** Bottom bar showing step progress, alert counts, and candidate counts.

### 2.6 Alert & Notification System
- **Signal Change Alerts:** Push notification when stock signal shifts (BUY → SELL, etc.).
- **Sell Reminder Alerts:** Push notification for portfolio stocks with active SELL signal.
- **XD Date Alerts:** Push notification for upcoming ex-dividend dates (within 7 days).
- **Morning Exit Window:** Push notification at 10:00–11:00 AM if swing exit conditions exist.
- **Afternoon Entry Window:** Push notification at 15:30–16:15 (capped before close) if market is open.
- **Dividend Season Reminder:** Push notification in January and June for accumulation season (once per season, 09:00–17:00 only).
- **Yield Opportunity Alert:** Year-round push notification (any month) when a DIVIDEND-purpose watchlist stock's yield rises ≥ 5% with ROE ≥ 15% — deduplicated per ISO week per stock.
- **In-App Sell Alerts:** Reactive sell alerts displayed in Advisor screen (Take Profit ≥10%, Stop Loss ≤-5%, Overbought RSI ≥65, Yield Drop <3%).

### 2.7 Trading Academy
- **Educational Content:** In-app trading education with structured learning paths.
- **Quick Reference:** Common trading concepts and strategies.

### 2.8 User Experience & Discovery
- **Price Alerts:** Visual indicators for stocks nearing target prices (+/- 10%).
- **Performance History:** Maintain a detailed log of past trades with "Lessons Learned" notes.
- **Analytics:** Calculate Win Rate, Average Win/Loss, and overall trading efficiency metrics.

### 2.9 Backtest Engine
- **Per-Stock Historical Replay:** Replays a stock's historical price/indicator series against the Swing DNA entry/exit rules (BUY/POTENTIAL entries, Take Profit/Stop Loss/Overbought exits).
- **Reported Metrics:** Total trades, win rate %, average win %, average loss %, and expectancy % per trade (derived from win rate and average win/loss).
- **Purpose:** Replaces relying purely on published research-based estimated win-rate ranges with an actual, stock-specific historical measurement.

## 3. Screen & Page Flows

### 3.1 Main Navigation
- **Watchlist:** Active monitoring list with quick filtering (All/Focus/Portfolio), sorting, and Focus management via Filter Chips. Signal-based BUY/SELL alerts are surfaced on the Advisor screen instead of here.
- **Advisor:** Smart Advisor with 3-step routine (SWING) or informational view (DIVIDEND). Single screen aggregating sell alerts, Swing/Dividend/Gap candidates, Speculative Plays, Liquidity/Trap Risk signals, and both copy-paste and in-app AI prompts.
- **Portfolio:** Central hub for viewing current holdings, cash management, and net return summary. Pull-to-refresh updates only portfolio stocks.
- **History (Stats):** Audit trail of completed trades with profitability analytics and lessons learned.
- **Settings:** App configuration, dynamic Risk Management limits, AI Integration (Gemini API key + model selection), and data management.

### 3.2 Secondary Flows
- **Stock Detail (Dashboard):** Triggered from any list item. Shows cached data first, refreshes from API if stale. Provides deep technical drill-down, price trend charts, and Focus toggle.
- **Backtest (from History/Stats):** Accessed via an icon on the History (Stats) screen. Run per-stock or full-watchlist historical replays of the Swing DNA rules to see win rate, avg win/loss, and expectancy.
- **Action Dialogs & Sheets:** 
    - **Record Buy/Sell:** Modal Bottom Sheet sliding up for transaction entry without losing context.
    - **Import SET:** Multi-select dialog for rapid watchlist population.
    - **Adjust Cash:** Instant balance setting from the Portfolio summary card.

## 4. User UX Principles

### 4.1 Visual Hierarchy & Aesthetic
- **Glassmorphism Design:** Use of semi-transparent "Glass" cards over vibrant, edge-to-edge background blobs to create depth and modern appeal.
- **Repositioned Background Blobs:** Dynamic colors kept to screen corners to ensure the central data area remains sharp and distraction-free.
- **High-Contrast Signalling:** Strict use of **Green (Tertiary)** for Buy/Profit and **Red (Error)** for Sell/Loss to communicate market intent at a glance.

### 4.2 Interaction Standards
- **Visibility First Navigation:** Bottom bar items always display bold labels and use primary colors for active states, avoiding "mystery meat" navigation.
- **Gesture Shortcuts:** Support for **Horizontal Swiping** to return from detail views to lists, optimizing for one-handed mobile use.
- **Zero-Latency Feel:** Local-first architecture ensures that UI navigation is instantaneous, with network updates happening gracefully in the background.
- **Actionable Tooltips:** Meaningful descriptions for technical indicators (e.g., "RSI < 30 is Cheap") to aid user decision-making without leaving the app.
- **Pull-to-Refresh:** Available on Watchlist, Portfolio, and Advisor screens. Portfolio refreshes only portfolio stocks for faster updates.

## 5. Non-Functional Requirements

### 5.1 Performance & Technical
- **Background Sync:** WorkManager runs every **30 minutes** on weekdays only. Skips stock scan when market is closed (including public holidays). Time-based alerts (afternoon window, dividend season) are checked first and guarded independently.
- **Data Privacy:** All personal portfolio and watchlist data must be stored locally on the device (Local-First architecture).
- **Backup & Restore:** Export/import watchlist symbols and portfolio essentials (cost, quantity, purpose) + cash balance as JSON. Caches and signals are regenerated on refresh.
- **Resilience:** Fallback mechanism for market data when primary SET sources are unavailable.

## 6. Technical Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Local Database:** Room Persistence Library (version 20)
- **Networking:** OkHttp 4 & Kotlin Serialization
- **Background Jobs:** WorkManager
- **Async Pattern:** Kotlin Coroutines & Flow
- **Image Loading:** Coil 3
