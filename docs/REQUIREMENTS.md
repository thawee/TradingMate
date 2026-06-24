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

1. **Qual (Quality):** Evaluates management efficiency and profitability.
   - Indicators: ROE > 15%, Net Profit Margin > 10%, D/E Ratio < 1.5, Profit Growth (3Y) > 10%.
2. **Val (Value):** Identifies underpriced or fair-value stocks.
   - Indicators: P/E Ratio (0.1 to 15.0) and P/BV (0.1 to 1.0).
3. **Div (Dividend):** Highlights strong passive income generators.
   - Indicators: Dividend Yield ≥ 5.0%.
4. **Mom (Momentum):** Detects early trend shifts and positive price momentum.
   - Indicators: MACD Histogram > 0.0.
5. **Sup (Support / Setup):** Locates ideal entry zones or extreme discounts.
   - Indicators: RSI < 35 (Oversold) or BUY/POTENTIAL zone signals.

**Combination Rules:**
- **Swing Plays:** Must pass `(Quality OR Value) AND (Momentum OR Support)`.
- **Dividend Stars:** Must pass `Dividend AND Quality`.

### 2.3 Portfolio Management
- **Consolidated Equity:** Calculate and display Total Assets by merging Stock Holdings and Cash Balance.
- **Transaction Recording:** Allow users to record buy and sell transactions with entry price and quantity.
- **Fee Engine:** Automatically calculate trading fees using the InnovestX structure (Commission 0.15%, Market Fee, VAT). Applies a ฿50/day minimum commission unless ATS + E-Statement is enabled (waived). Financial Transaction Tax (FTT) is ฿0 — officially abolished.
- **Profit/Loss tracking:** Display Gross and Net Profit/Loss in both currency (THB) and percentage.
- **Cash Management:** Provide a quick way to adjust or set the current cash balance.
- **Yield-on-Cost:** Calculate and display dividend yield relative to purchase price.

### 2.4 Watchlist & Focus Tracking
- **Multi-source Search:** Enable searching for stocks using both SET and Yahoo Finance data.
- **Focus List:** Allow users to "Star" specific stocks to track them in a dedicated "Focus" tab.
- **SET Collections Import:** Enable one-click import of curated stock groups (SET50, SET100, SETHD, Dividend Stars, Bluechips).
- **Dynamic Sorting:** Sort stocks by Symbol, Change %, Profit %, or Signal strength.
- **Advanced Filtering:** Filter list by Buy signals, Positive movement, or active Holdings.

### 2.5 Smart Advisor
- **Playbook Modes:** Two modes — Swing Playbook and Dividend Playbook.
- **3-Step Routine (SWING):**
    1. **Ask AI** — Copy AI prompt to clipboard (auto-marks step as done).
    2. **Check Exits** — Display sell alerts based on technical conditions (Take Profit, Stop Loss, Overbought, Yield Drop).
    3. **Scan Setups** — Display candidate stocks filtered by Quality, Momentum, Value, and Gap criteria using the Five-Layer Filter System.
- **AI Prompt Generation:** Generate structured prompts for ChatGPT/Gemini with candidate data, risk constraints, and playbook rules.
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

## 3. Screen & Page Flows

### 3.1 Main Navigation
- **Watchlist:** Active monitoring list with quick filtering, sorting, and Focus management via Filter Chips.
- **Advisor:** Smart Advisor with 3-step routine (SWING) or informational view (DIVIDEND). Includes sell alerts, candidates, and AI prompts.
- **Portfolio:** Central hub for viewing current holdings, cash management, and net return summary. Pull-to-refresh updates only portfolio stocks.
- **History (Stats):** Audit trail of completed trades with profitability analytics and lessons learned.
- **Settings:** App configuration, dynamic Risk Management limits, and data management.

### 3.2 Secondary Flows
- **Stock Detail (Dashboard):** Triggered from any list item. Shows cached data first, refreshes from API if stale. Provides deep technical drill-down, price trend charts, and Focus toggle.
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
