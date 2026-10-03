# TradingMate Screen Flows

This document outlines the user journey and navigation architecture of the TradingMate application. The app follows a **Material 3 Navigation** structure with a centralized dashboard and a success-state-driven detail view.

---

## 🧭 Primary Navigation (Bottom Bar)

The bottom navigation bar provides instant access to the five main functional areas of the app. The app is core-first (decision 2026-10-03): it opens on Portfolio, where the index core, DCA and allocation live.

1.  **💼 Portfolio (Financial Hub, start screen)**
    *   **Focus:** Consolidated asset tracking & Risk Management.
    *   **Features:** Total Asset summary (Stock + Cash), inline cash management & ledger audit, Sector Breakdown with 30% concentration caps, Section 47 bis Dividend Tax Shield card, and 1-Click Anti-Ruin Position Sizing in trade entry dialogs.
2.  **🧠 Advisor**
    *   **Focus:** Dividend list, rebalance plan and holding checks first (opens on the Dividend tab); swing setups as technical context, marked "Context, not a buy call".
    *   **Features:** The Advisor opens with a Today card (holdings at a saved exit first, then the monthly high-yield review date and names to sell, then the core DCA date, or "No holding needs action today"). Holdings are grouped Act now / At the monthly review / Review only. The Dividend tab shows the high dividend yield list, its forward record, the monthly rebalance plan and upcoming ex-dates for holdings; the Swing tab shows the context notice, the regime banner (Swing buys only) and swing holding exits. Research (the filter explorer) is collapsed at the bottom.
    *   **High dividend yield list and monthly rebalance plan (Dividend tab, always shown):** the top 10 SET50 names by yield, and an order list for the tested rule: sell satellite names that left the list, keep held names without resizing, buy each new name with up to a tenth of the budget. It manages only Dividend-purpose holdings (default budget: their value); other holdings are never sold. Cap breaches are warnings. Place the order at the broker first; "Record buy" then opens the Portfolio Buy dialog prefilled, with the Dividend purpose and "Record an already executed broker trade" preset. A monthly notification reminds users who follow the plan (untested lists on and a Dividend-purpose holding). When a tenth of the budget is below one lot, the card shows the budget needed to hold all ten and points to 1DIV.
3.  **📊 Watchlist (Data Center)**
    *   **Focus:** Broad market monitoring.
    *   **Features:** A comprehensive list of saved stocks with technical signals (shown as context under the same notice as the Swing tab), fundamental overview, search filter, and dynamic sorting.
4.  **📈 History & Stats (CRO Dashboard)**
    *   **Focus:** Performance review & Quantitative Risk Matrix.
    *   **Features:** Closed trade records, cumulative profit trajectory with Catmull-Rom splines, 1-Day 95% Historical Value-at-Risk (VaR), Conditional VaR (Expected Shortfall), Max Drawdown (MDD), and 63-day rolling Portfolio Beta ($\beta$).
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
    *   **Strategy:** Technical context, observed price levels, and any accepted plan's saved stop and target.
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
*   **Process:** Opens a dialog to enter symbol, cost, quantity, trade purpose, and any planned stop and target. An accepted swing plan is saved with its source and version.
*   **Intelligent Assist:** The size calculator rounds to SET 100-share board lots. Proposed swing buys are checked for a supported target and the configured minimum net reward to risk (default 2:1) after estimated fees, stop risk, combined holding and sector caps, and cash reserve. A recorded broker fill can exceed proposal limits, so review its cash reconciliation notice.
*   **Editing:** Adding shares retains the existing plan and accumulates purchase cost and fees. To reduce a holding to zero, record a sale; removing it from the watchlist does not create a sale.
*   **Persistence feedback:** The purchase, edit, cash, dividend, and sale forms remain open until the database reports success. A failed write displays its error and retains the entered values.

### 2. The Cash Management Flow
*   **Path:** `Portfolio` -> `Summary Card` -> `Edit Icon (next to Cash)`.
*   **Modes:**
    *   **Deposit/Withdraw:** Add or subtract from existing balance (logged in cash audit ledger).
    *   **Account Reconcile:** Set a hard balance to match bank records.

### 3. The Sell/Exit Flow
*   **Path:** `Portfolio` -> `Holdings` -> `Sell Icon`.
*   **Process:** Enter sell price and quantity. 
*   **Outcome:** The sale is recorded in **History** with a snapshot of its plan and fees, and cash is updated with net proceeds and an audit entry. A full-sale Undo restores the holding, plan, and fees; the same sale cannot be undone twice.

---

## 🔔 Background Monitoring Flow

The app maintains a silent lifecycle to keep you informed without active usage:

1.  **WorkManager Activation:** Scheduled every 30 minutes without restarting the schedule on each app launch; actual execution is approximate. Stock scanning runs during market hours.
2.  **Analysis:** Scans all stocks in the **Watchlist**.
3.  **Notification:** Signal changes and qualifying exit alerts may generate notifications. Saved swing plans use their accepted stop and target; an alert is not an order or guaranteed fill.
4.  **Re-Entry:** Tapping the notification launches the app directly into that stock's **Deep Dive** dashboard.
