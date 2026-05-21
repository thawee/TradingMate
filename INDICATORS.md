# Technical Indicators & Trading Strategy

This document provides a detailed breakdown of the technical indicators, trading zones, and decision-making logic used in **TradingMate**.

---

## 📊 Technical Indicators

TradingMate combines momentum, trend, and volatility indicators to generate high-conviction signals.

### 1. RSI (Relative Strength Index) - The Speedometer
RSI measures the speed and change of price movements. It ranges from 0 to 100.

- **Calculation:** 14-day period.
- **Thresholds:**
  - **< 35 (Oversold):** Target BUY zone. The stock is statistically "cheap" and compressed.
  - **< 42 (Potential):** Nearing value zone. High alert for a reversal.
  - **> 65 (Overbought):** Target SELL zone. The stock is overextended and at risk of a pullback.

### 2. MACD (Moving Average Convergence Divergence) - The Momentum Switch
MACD tracks the relationship between two moving averages of a stock’s price.

- **Configuration:** (12, 26, 9).
- **MACD Histogram:**
  - **Positive (> 0):** Bullish momentum. Confirms the trend is moving UP.
  - **Negative (< 0):** Bearish momentum. Suggests the trend is moving DOWN.
- **Role:** Acts as a "gear shift" to confirm RSI signals.

### 3. SMA (Simple Moving Average) - The Trend Guards
SMAs smooth out price data to identify the direction of the trend.

- **SMA 50 (Mid-term Health):** If the price is above SMA 50, the immediate trend is bullish.
- **SMA 200 (Long-term Trend):** The "Line in the Sand."
  - **Above SMA 200:** Bull Market / Healthy Trend.
  - **Below SMA 200:** Bear Market / Weak Trend.

### 4. Bollinger Bands - The Volatility Map
A "volatility tube" that wraps around the price.

- **Configuration:** 20-day SMA with 2.0 Standard Deviations.
- **Lower Band (Value):** Prices hitting this band are statistically undervalued for their current volatility. Good for entry.
- **Upper Band (Resistance):** Prices hitting this band are stretched too far. High risk of a pullback.

---

## 🏛️ Fundamental Guardrails (Quality Rules)

Before looking at technical signals, TradingMate evaluates the "DNA" of a company. A stock is marked with the **"Solid Financials"** badge or suggested in the **Dividend Advisor** only if it passes these strict safety checks:

1. **ROE (Return on Equity) > 15%:**
   - **Why:** We want highly efficient companies that generate superior returns on shareholders' capital.
2. **Net Profit Margin > 10%:**
   - **Why:** Ensures the company keeps a healthy portion of its revenue as profit after all expenses.
3. **Profit Growth (3Y) > 10%:**
   - **Why:** Confirms the company is growing its bottom line consistently over the medium term.
4. **D/E Ratio (Debt to Equity) < 1.5:**
   - **Why:** Prevents exposure to companies with excessive debt that could be risky during high-interest periods.

> **⭐ Quality Priority:** Stocks that pass these rules are highlighted with a **"⭐ Quality"** tag in all signals and push notifications. These are your "Must Watch" opportunities when they enter a Buy or Potential zone.

---

## 💰 Dividend Advisor & Planning

The **Dividend Advisor** screen helps you plan for long-term passive income.

- **Passive Income Goal:** You can set a **Target Monthly Dividend** in the app Settings.
- **Capital Calculation:** The advisor calculates the total capital required to reach your monthly goal (assuming a 5% average yield).
- **Suggested Stocks:** The advisor suggests stocks from the "Dividend Stars" collection. These are filtered to ensure they have a **positive dividend yield** and pass the **Fundamental Guardrails** mentioned above.
- **Progress Tracking:** The app compares your current portfolio's estimated monthly dividends against your target to show your "Goal Progress."

---

## 🧠 Trading Zones & Market Pulse

TradingMate categorizes every stock into one of four "Zones" based on technical logic. These are visualized in the **Market Pulse** section of the Home page.

### 🟢 Buying Zone (Accumulation)
Stocks here represent the best value-to-risk ratio.
- **Criteria:** RSI < 35 **OR** (MACD is Bullish **AND** (Price is near Lower Bollinger Band **OR** Price > SMA 50)).
- **Signal:** *Oversold Accumulation* or *Early Recovery*.

### 🟡 Potential Zone (Watchlist)
Stocks that are becoming cheap but haven't confirmed a reversal yet.
- **Criteria:** RSI < 42 **OR** Price is near Lower Bollinger Band.
- **Signal:** *Nearing Value Zone* or *Support Testing*.

### 🔴 Selling Zone (Distribution)
Stocks that are overvalued or have broken their upward trend.
- **Criteria:** RSI > 65 **OR** Price is near Upper Bollinger Band **OR** (MACD is Bearish **AND** Price < SMA 50).
- **Signal:** *Overbought*, *Upper Band Resistance*, or *Weak Trend*.

### ⚪ Neutral Zone
Stocks with no clear trend or extreme valuation.
- **Signal:** *Wait & Watch*.

---

## 🛡️ Risk Management (The Golden Rules)

1. **SELL Overrides BUY:** Even if a stock has great momentum, if it hits RSI 65 or the Upper Bollinger Band, the app triggers a **SELL** warning. Never buy at the peak.
2. **The 5% Rule (Stop Loss):** If a stock drops -5% from your average cost, TradingMate triggers a mandatory **SELL** signal to preserve capital.
3. **The 10% Rule (Take Profit):** At +10% net profit, the app suggests locking in gains, especially if technicals are reaching the Selling Zone.

---

## 🔔 Automated Alerts & Notifications

TradingMate actively monitors your saved stocks and delivers real-time intelligence via Android Push Notifications and in-app alerts.

### 1. Push Notifications (Background Monitoring)
The app runs a background worker (every hour) to monitor stocks currently in your **Watchlist**. 
- **Market Hours Only:** Notifications are only processed during SET market hours.
- **Trigger Conditions:** A push notification is sent whenever a stock's technical data triggers a `BUY`, `POTENTIAL`, or `SELL` signal.
- **Price Alerts:** Visual indicators for stocks nearing target prices. The proximity threshold can be customized in **Settings**.

### 2. In-App Dividend Alerts (XD Dates)
On the Home screen, the app tracks corporate action dates.
- **Trigger:** If a stock has an upcoming Ex-Dividend (XD) date within the **next 14 days**, it will appear in the "Dividend Opportunities" section.

---

## 💸 Fee Structure (InnovestX)

TradingMate calculates **Net Profit** by accounting for the following fees (approx. 0.32% round-trip):

- **Commission:** 0.15% (Safety estimate).
- **Market Fee:** 0.007% (Trading + Clearing + Regulatory).
- **VAT:** 7% on total commissions.
- **Selling Tax:** 0.11% (applied only on sell orders).
- **Minimum Fee:** 50 THB daily (if applicable).

> **Formula:** Net Profit = (Selling Price - Sell Fees) - (Buying Price + Buy Fees)
