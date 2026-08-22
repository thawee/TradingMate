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
  - **Meaningfully Positive (> 0.1% of last price):** Bullish momentum. The price-relative threshold prevents noise-level crossover flicker around zero.
  - **Negative (< 0):** Bearish momentum. Suggests the trend is moving DOWN.
- **Role:** Acts as a "gear shift" to confirm RSI signals. For DNA tagging (MOM), also requires RSI in 40–70 to exclude overbought stocks.

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

### 5. OBV (On-Balance Volume) - The Lie Detector
OBV cumulatively adds volume on up days and subtracts it on down days, revealing whether volume backs the price move.

- **Rising OBV (above its 10-day average):** Steady accumulation — big money is buying. Confirms "Early Recovery" BUY signals alongside (or instead of) a 2× volume surge.
- **Flat/Falling OBV during a rally:** The move lacks conviction and is likely to fail.

### 6. Relative Strength vs SET Index - The Race
Classic O'Neil-style RS: the stock's 3-month (63 trading days) return minus the SET index return over the same window.

- **RS > 0:** The stock is outperforming the market. Winners tend to keep winning.
- **RS < 0:** A market laggard — even if it looks cheap, money is flowing elsewhere. The MOM DNA layer requires RS ≥ 0 (null-tolerant when index data is unavailable).
- Stocks with positive RS earn the **"RS"** DNA tag.

### 7. 52-Week Low Guard - The Trap Detector
Structural decliners "look cheap" on RSI and P/E while continually making new lows.

- **Rule:** Stocks trading within **5% of their 52-week low** are excluded by the Pre-Filter from all candidate lists (Swing, Dividend, Gap-Up, Speculative).
- **Null-tolerant:** If 52-week data has not been computed yet, the stock passes (fail-open).

### 8. ATR (Average True Range) - The Breathing Room
Wilder-smoothed 14-day average of the daily true range (including gaps). Powers volatility-adjusted exits:

- **Stop Loss:** 2× daily ATR% below cost, clamped to **-3.5% … -8.0%**. Calm large-caps get tight stops; volatile stocks get room so normal wiggles don't shake you out. Priority: user override > ATR-based > fixed tier (-4.5% SET50 / -6.5% Mid/Small).
- **Trailing Stop:** 2.5× ATR% drop from peak (clamped 4–10%), replacing the fixed 5% rule when ATR is available.

### 9. ADX (Average Directional Index) - The Regime Detector
Wilder's 14-day trend-strength gauge (direction-agnostic). Gates BUY signals by market regime:

- **ADX < 20 (Chop):** "Healthy Momentum" BUY signals are suppressed — MACD crossovers in a sideways range are whipsaw noise.
- **ADX ≥ 40 (Violent trend):** "Oversold Accumulation" is downgraded to POTENTIAL (**Falling Knife Guard**) — an extreme selloff still in progress is not a value dip.
- **Null-tolerant:** Without ADX data, signals behave as before.

### 10. Stochastic Oscillator (14,3,3) - The Reversal Timer
Slow stochastic: where the close sits within the recent 14-day high-low range (%K), smoothed (%D).

- **Reversal Gate:** An "Oversold Accumulation" BUY requires %K to have crossed above %D. If %K < 20 and still below %D, the price is oversold but **still falling** — the signal is downgraded to POTENTIAL (*Reversal Not Confirmed*) until the cross-up happens.
- **Null-tolerant:** Without stochastic data, the BUY passes as before.

### 11. MFI (Money Flow Index, 14) - The Smart Money Gauge
A volume-weighted RSI computed on typical price ((H+L+C)/3 × volume).

- **MFI ≥ 80 (Distribution):** Heavy volume flowing OUT at high prices. On a profitable position this triggers a **SELL** (*Distribution Detected*) — smart money is selling into strength.
- **MFI ≤ 20 (Capitulation):** Panic-volume flush. Counts as volume confirmation for "Early Recovery" BUY signals (alongside 2× volume surge and rising OBV).

### 12. Market Regime Gate - SET Index Health
Measures the health of the broader Stock Exchange of Thailand index (`^SET.BK`).

- **Calculation:** Compares SET Index price against its 50-day SMA and evaluates MACD histogram.
  - **Bullish Trend:** SET Index $\ge$ SMA 50 AND MACD Histogram $\ge$ 0. (Full 100% position sizing).
  - **Consolidation / Neutral:** SET Index $\ge$ SMA 50.
  - **Bear / Correction:** SET Index < SMA 50. Activates defensive sizing (50%) and enforces strict NVDR Flow or positive Relative Strength for all Swing entries.

### 13. Pre-Trade Risk:Reward & Invalidation Engine
Before entering any position, TradingMate calculates the exact mathematical trade parameters:

- **Suggested Stop Loss:** Volatility-adjusted stop ($2 \times \text{ATR}\%$, clamped $-3.5\%$ to $-8.0\%$) or fixed tier ($-4.5\%$ SET50 / $-6.5\%$ Mid/Small).
- **Suggested Target Price:** Baseline $+10\%$ take-profit target.
- **Risk:Reward Ratio (R:R):**
  $$\text{R:R} = \frac{\text{Target Price} - \text{Entry Price}}{\text{Entry Price} - \text{Stop Loss Price}}$$
  Displayed prominently on all watchlist and setup cards to enforce positive expectancy before trade execution.

### 14. False Breakout Guard (Institutional Flow & Volume)
Guards against "bull traps" where technical momentum appears positive but lacks institutional backing:

- **NVDR Dumping Guard:** If foreign institutions are heavily net selling (NVDR net selling $> \text{฿}5,000,000$), BUY signals are downgraded to `POTENTIAL` (watch).
- **Volume & RS Confirmation:** If a stock is lagging the SET index (Relative Strength $< -2.0$) and lacks above-average volume or rising OBV, breakout BUY signals are downgraded to `POTENTIAL`.

### 15. Early Breakdown Warning (Active Loss Protection)
Rather than waiting for a full stop loss ($-4.5\%$ to $-8.0\%$), the engine actively monitors open positions:

- **Trigger:** If a holding is in a slight net loss ($\le -1.5\%$) and price breaks below the 50-day SMA while momentum (MACD) turns negative.
- **Action:** Generates an immediate `SELL` (*Early Breakdown Warning*) alert, allowing traders to cut deteriorating positions early with minimal capital loss.

---

## 🧬 The 6-Layer Filter (Stock DNA)

- **Pre-Filter:** Liquidity (daily turnover > ฿5M) **AND** not within 5% of the 52-week low.
- **Layer 1 — QUAL:** ROE > 15%, NPM > 10%, D/E < 1.5, 3Y profit growth > 10%.
- **Layer 2 — VAL:** P/E 0.1–15.0 and P/BV 0.1–1.0.
- **Layer 3 — DIV:** Dividend yield ≥ 5%.
- **Layer 4 — MOM:** MACD histogram > 0.1% of price, RSI 40–64.9, and Relative Strength vs SET ≥ 0.
- **Layer 5 — SUP:** BUY/POTENTIAL signal (incorporates SMA 200 trend context).
- **Layer 6 — FLOW:** Foreign Fund NVDR net accumulation ($> 0$).

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
- **Signal:** *Nearing Value Zone*, *Support Testing*, *False Breakout Guard*, or *Falling Knife Guard*.

### 🔴 Selling Zone (Distribution)
Stocks that are overvalued or have broken their upward trend.
- **Criteria:** RSI > 65 **OR** Price is near Upper Bollinger Band **OR** (MACD is Bearish **AND** Price < SMA 50) **OR** Early Breakdown.
- **Signal:** *Overbought*, *Upper Band Resistance*, *Early Breakdown Warning*, or *Weak Trend*.

### ⚪ Neutral Zone
Stocks with no clear trend or extreme valuation.
- **Signal:** *Wait & Watch*.

---

## 🛡️ Risk Management (The Golden Rules)

1. **SELL Overrides BUY:** Even if a stock has great momentum, if it hits RSI 65 or the Upper Bollinger Band, the app triggers a **SELL** warning. Never buy at the peak.
2. **Early Breakdown Cutting:** If a trade loses $-1.5\%$ and breaks below SMA 50 with negative MACD, exit early rather than suffering full stop-loss drawdown.
3. **Volatility-Adjusted Stop Loss:** The stop is 2× the stock's daily ATR (clamped -3.5% to -8.0%). If ATR is unavailable, fixed tiers apply: -4.5% (SET50) / -6.5% (Mid/Small-Cap). A breach triggers a mandatory **SELL** signal to preserve capital.
4. **Market Regime Sizing:** Reduce position sizing to 50% defensive during Bear/Correction market regimes (SET Index < SMA 50).
5. **The 10% Rule (Take Profit):** At +10% net profit, the app suggests locking in gains, especially if technicals are reaching the Selling Zone.

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
- **Trigger:** If a stock has an upcoming Ex-Dividend (XD) date within the defined window (default 14 days, customizable in Settings), it will appear in the "Dividend Opportunities" section.
- **End of Year Option:** You can also choose to show all upcoming XD dates until the end of the current year.

---

## 💸 Fee Structure (InnovestX)

TradingMate calculates **Net Profit** by accounting for the following fees (approx. 0.32% round-trip):

- **Commission:** 0.15% (Safety estimate).
- **Market Fee:** 0.007% (Trading + Clearing + Regulatory).
- **VAT:** 7% on total commissions.
- **Selling Tax:** 0.11% (applied only on sell orders).
- **Minimum Fee:** 50 THB daily (if applicable).

> **Formula:** Net Profit = (Selling Price - Sell Fees) - (Buying Price + Buy Fees)
