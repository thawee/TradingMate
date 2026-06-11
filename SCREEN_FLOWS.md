# TradingMate Screen Flows

This document outlines the user journey and navigation architecture of the TradingMate application. The app follows a **Material 3 Navigation** structure with a centralized dashboard and a success-state-driven detail view.

---

## 🧭 Primary Navigation (Bottom Bar)

The bottom navigation bar provides instant access to the five main functional areas of the app:

1.  **📊 Watchlist (Data Center)**
    *   **Focus:** Broad market monitoring.
    *   **Features:** A comprehensive list of saved stocks with technical signals, fundamental overview, and Filter Chips to isolate "Focus" or "All" stocks.
2.  **🧠 Advisor (AI Discovery Hub)**
    *   **Focus:** Actionable trade setups and Risk Management.
    *   **Features:** Permanent top-level Sell Alerts, tabbed browsing for Swing Trades, Earnings Gaps, and Dividend plays. Generates AI Master Prompts directly to clipboard.
3.  **💼 Portfolio (Financial Hub)**
    *   **Focus:** Consolidated asset tracking.
    *   **Features:** Total Asset summary (Stock + Cash), inline cash management, detailed holdings list, and inline Modal Bottom Sheet for trades.
4.  **📈 History (Performance Review)**
    *   **Focus:** Learning from past trades.
    *   **Features:** Closed trade records, win/loss ratios, and total realized profit tracking.
5.  **⚙️ Settings (App Configuration)**
    *   **Focus:** Core application preferences.
    *   **Features:** Real-time Risk Management limits (Max Risk Per Trade, Exposure, Portfolio Allocation) driving the AI prompts.

---

## 🔄 The "Deep Dive" Flow (Stock Details)

TradingMate uses a **Success-State Drill-Down** pattern. Whenever a stock is selected from any list, the UI transitions into the `StockDashboard`:

*   **Trigger:** Tap on any stock card in Home, Portfolio, Focus, or Watchlist.
*   **The View:** A full-screen dashboard showing:
    *   **Header:** Signal status (e.g., BUY) and Volume Surge alerts.
    *   **Business Info:** Sector, industry, and description.
    *   **Financials:** Detailed ROE, Net Profit, and Dividend Yield badges.
    *   **Technicals:** RSI, MACD, SMA 50/200, and Bollinger Band charts.
    *   **Strategy:** Automated "Buy Below" and "Sell Above" price targets.
*   **Exit:** Swipe right or tap the "Back" button to return to the previous navigation tab.

---

## 🎓 Discovery & Education Flow

Access to the app's philosophy and workflow is integrated into the Home screen:

1.  **The TradingMate Story (`AboutScreen`)**
    *   **Path:** `Home` -> `Our Story` card.
    *   **Content:** The "Why" behind the app, the concept of discipline, and the creator/AI collaboration details.
2.  **Trading Academy (`EducationScreen`)**
    *   **Path:** `Home` -> `Academy` card (or via `Home` -> `Open Education` callback).
    *   **Content:** A structured 3-step success path, risk management techniques (Cut Loss), and indicator tutorials.

---

## 💰 Portfolio Management Flows

The Portfolio screen manages the full financial lifecycle of an investment:

### 1. The Buy/Update Flow
*   **Path:** `Portfolio` -> `+ (Add Button)` or `Holdings` -> `Edit Icon`.
*   **Process:** Opens a dialog to enter Symbol, Avg Cost, and Quantity.
*   **Intelligent Assist:** Features a **Risk/Reward Calculator** that automatically suggests Target and Stop Loss prices based on the stock's quality.

### 2. The Cash Management Flow
*   **Path:** `Portfolio` -> `Summary Card` -> `Edit Icon (next to Cash)`.
*   **Modes:**
    *   **Deposit/Withdraw:** Add or subtract from existing balance.
    *   **Account Reconcile:** Set a hard balance to match bank records.

### 3. The Sell/Exit Flow
*   **Path:** `Portfolio` -> `Holdings` -> `Sell Icon`.
*   **Process:** Enter sell price and quantity. 
*   **Outcome:** The trade is moved to the **History** tab, and the cash balance is automatically updated with the net proceeds (minus Thai fees).

---

## 🔔 Background Monitoring Flow

The app maintains a silent lifecycle to keep you informed without active usage:

1.  **WorkManager Activation:** Scheduled every 1 hour (Market Hours Only).
2.  **Analysis:** Scans all stocks in the **Watchlist**.
3.  **Notification:** If technical criteria for a `BUY`, `SELL`, or `POTENTIAL` signal are met, a system-level notification is sent to the user.
4.  **Re-Entry:** Tapping the notification launches the app directly into that stock's **Deep Dive** dashboard.
