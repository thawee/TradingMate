# TradingMate Requirements

## 1. Project Overview
TradingMate is a high-performance personal trading companion specifically designed for the Thai Stock Market (SET). The application aims to provide retail investors with simplified technical analysis, automated trading signals, and consolidated portfolio tracking using a modern, discipline-focused approach.

## 2. Functional Requirements

### 2.1 Market Data & Analysis
- **Real-time Quotes:** Must fetch and display real-time price updates for SET stocks (Symbol, Last Price, Change, %Change).
- **Fundamental Metrics:** Must display core financial data including P/E, P/BV, ROE, Net Profit, EPS, and D/E Ratio.
- **Dividend Tracking:** Must track and display Dividend Yield and upcoming XD dates.
- **Technical Indicator Engine:**
    - **RSI (14):** Speedometer for overbought (>65) and oversold (<35) conditions.
    - **SMA (50/200):** Trend guards for mid-term and long-term market health.
    - **MACD:** Momentum confirmation for early reversals.
    - **Bollinger Bands:** Volatility mapping for undervaluation detection.
- **Smart Signal Logic:** Generate actionable signals (BUY, SELL, POTENTIAL, NEUTRAL) based on combined technical and fundamental criteria.

### 2.2 Portfolio Management
- **Consolidated Equity:** Calculate and display Total Assets by merging Stock Holdings and Cash Balance.
- **Transaction Recording:** Allow users to record buy and sell transactions with entry price and quantity.
- **Fee Engine:** Automatically calculate trading fees (Commission, Market Fee, VAT, and Selling Tax) based on the InnovestX structure.
- **Profit/Loss tracking:** Display Gross and Net Profit/Loss in both currency (THB) and percentage.
- **Cash Management:** Provide a quick way to adjust or set the current cash balance.

### 2.3 Watchlist & Focus Tracking
- **Multi-source Search:** Enable searching for stocks using both SET and Yahoo Finance data.
- **Focus List:** Allow users to "Star" specific stocks to track them in a dedicated "Focus" tab.
- **SET Collections Import:** Enable one-click import of curated stock groups (SET50, SET100, SETHD, Dividend Stars, Bluechips).
- **Dynamic Sorting:** Sort stocks by Symbol, Change %, Profit %, or Signal strength.
- **Advanced Filtering:** Filter list by Buy signals, Positive movement, or active Holdings.

### 2.4 User Experience & Discovery
- **Home Hub:** Integrated dashboard featuring market highlights, story concepts, and a "Trading Academy" for educational content.
- **Price Alerts:** Visual indicators for stocks nearing target prices (+/- 10%).
- **Performance History:** Maintain a detailed log of past trades with "Lessons Learned" notes.
- **Analytics:** Calculate Win Rate, Average Win/Loss, and overall trading efficiency metrics.

## 3. Screen & Page Flows

### 3.1 Main Navigation
- **Watchlist:** Active monitoring list with quick filtering, sorting, and Focus management via Filter Chips.
- **Advisor:** AI Discovery Hub with top-level Sell Alerts, tabbed Setup lists (Swing, Gap, Dividend), and automatic prompt generation.
- **Portfolio:** Central hub for viewing current holdings, cash management, and net return summary.
- **History (Stats):** Audit trail of completed trades with profitability analytics and lessons learned.
- **Settings:** App configuration, dynamic Risk Management limits, and data management.

### 3.2 Secondary Flows
- **Stock Detail (Dashboard):** Triggered from any list item (Advisor/Portfolio/Watchlist). Provides deep technical drill-down, price trend charts, and Focus toggle.
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

## 5. Non-Functional Requirements

### 5.1 Performance & Technical
- **Background Sync:** Periodically update market data and price alerts in the background (15m to 120m intervals).
- **Data Privacy:** All personal portfolio and watchlist data must be stored locally on the device (Local-First architecture).
- **Backup & Restore:** Provide functionality to export/import all local data as a JSON file for data portability.
- **Resilience:** Fallback mechanism for market data when primary SET sources are unavailable.

## 6. Technical Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Local Database:** Room Persistence Library
- **Networking:** OkHttp 4 & Kotlin Serialization
- **Background Jobs:** WorkManager
- **Async Pattern:** Kotlin Coroutines & Flow
- **Image Loading:** Coil 3
