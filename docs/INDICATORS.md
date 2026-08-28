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

### 16. Multi-Timeframe (MTF) Macro Trend Alignment (Weekly 20-EMA)
To avoid counter-trend "knife-catching" in larger macro downtrends:
- **Resampling:** Daily price series are resampled into calendar-week closing candles (`TechnicalAnalysis.resampleToWeeklyCloses`).
- **Weekly 20-EMA:** Computes the 20-period Exponential Moving Average on weekly closes.
- **Macro Alignment Guard:** If the latest weekly close is below the Weekly 20-EMA (`isWeeklyMacroBullish == false`), daily momentum `BUY` signals are downgraded to `POTENTIAL` (*Macro Weekly Bearish*).
- **Confluence Tag (`MTF`):** Stocks that are simultaneously bullish on the daily chart (Price $\ge$ SMA 50 $\ge$ SMA 200) AND weekly chart (Weekly Close $\ge$ Weekly 20-EMA) receive the `MTF` confluence tag.

### 17. 63-Day Rolling Covariance Portfolio Beta ($\beta$)
Measures systemic volatility relative to the SET Index (`^SET.BK`) over 63 trading days (approx. 3 calendar months):
$$\beta = \frac{\text{Cov}(R_{\text{stock}}, R_{\text{SET}})}{\text{Var}(R_{\text{SET}})}$$
$$\text{Portfolio } \beta = \frac{\sum (w_i \times \beta_i)}{\sum w_i}$$
- **Defensive Low-Vol ($\beta < 0.85$):** Insulates portfolio from broader market selloffs.
- **Balanced Index Track ($0.85 \le \beta \le 1.15$):** Moves in tandem with the SET Index.
- **Aggressive High-Beta ($\beta > 1.15$):** Outperforms in bull markets but requires strict risk buffers.

### 18. Historical Value-at-Risk ($\text{VaR}_{95\%}$) & Conditional VaR (CVaR)
Quantitative downside tail-risk modeling on empirical daily returns:
- **1-Day 95% Historical VaR:** The 5th percentile worst daily loss:
  $$\text{VaR}_{95\%} = -\text{Percentile}_{5\%}(R_{\text{daily}}) \times \text{Total Assets}$$
- **Conditional VaR (CVaR / Expected Shortfall):** The expected average loss given that the market drops beyond the 95% VaR threshold:
  $$\text{CVaR}_{95\%} = -\mathbb{E}[R \mid R \le \text{Percentile}_{5\%}(R)] \times \text{Total Assets}$$

### 19. Maximum Drawdown (MDD) & High-Water Mark (HWM)
Tracks cumulative equity trajectory to measure downside capital preservation:
$$\text{HWM}_t = \max_{1 \le i \le t}(\text{Equity}_i)$$
$$\text{Drawdown}_t = \frac{\text{Equity}_t - \text{HWM}_t}{\text{HWM}_t} \times 100\%$$
$$\text{Max Drawdown (MDD)} = \min_t(\text{Drawdown}_t)$$

### 20. Fixed-Fractional Anti-Ruin Position Sizing Engine
Calculates optimal share count based on predefined account risk ($1.0\% - 2.0\%$):
$$\text{Max Risk Baht} = \text{Total Assets} \times \text{Risk\%}$$
$$\text{Raw Shares} = \left\lfloor \frac{\text{Max Risk Baht}}{\text{Entry Price} - \text{Stop Loss Price}} \right\rfloor$$
- **100-Share Board Lot Rounding:** Automatically rounded down to the nearest 100 shares for SET standard board lots.
- **15% Single-Stock Ceiling:** Caps total position capital at max 15% of portfolio equity:
  $$\text{Capped Shares} = \min\left(\text{Raw Shares}, \left\lfloor \frac{\text{Total Assets} \times 15\%}{\text{Entry Price}} \right\rfloor\right)$$

### 21. Thai Dividend Tax Shield (Section 47 bis Reclaim)
Thailand Revenue Code Section 47 bis allows individual tax residents to claim tax credits on dividend income based on the paying company's Corporate Income Tax (CIT) rate (standard 20%):
$$\text{Gross Dividend} = \frac{\text{Net Received}}{1 - \text{WHT (10\%)}}$$
$$\text{Tax Credit} = \text{Gross Dividend} \times \left(\frac{\text{CIT Rate}}{100 - \text{CIT Rate}}\right)$$
$$\text{Net Yield on Cost (YoC}_{\text{net}}\text{)} = \left(\frac{\text{Annual DPS} \times (1 - \text{WHT})}{\text{Average Cost}}\right) \times 100\%$$

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
- **Section 47 bis Tax Shield:** Displays estimated tax credits reclaimable during annual personal income tax filing.

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
- **Signal:** *Nearing Value Zone*, *Support Testing*, *False Breakout Guard*, *Falling Knife Guard*, or *Macro Weekly Bearish*.

### 🔴 Selling Zone (Distribution)
Stocks that are overvalued or have broken their upward trend.
- **Criteria:** RSI > 65 **OR** Price is near Upper Bollinger Band **OR** (MACD is Bearish **AND** Price < SMA 50) **OR** Early Breakdown.
- **Signal:** *Overbought*, *Upper Band Resistance*, *Early Breakdown Warning*, or *Weak Trend*.

### ⚪ Neutral Zone
Stocks with no clear trend or extreme valuation.
- **Signal:** *Wait & Watch*.

---

## 🛡️ Institutional Risk Management (The Golden Rules)

1. **SELL Overrides BUY:** Even if a stock has great momentum, if it hits RSI 65 or the Upper Bollinger Band, the app triggers a **SELL** warning. Never buy at the peak.
2. **Early Breakdown Cutting (with XD Grace Period):** If a trade loses $-1.5\%$ and breaks below SMA 50 with negative MACD, exit early rather than suffering full stop-loss drawdown. *Grace Period:* If the price drop occurs within $\pm 2$ trading days of an Ex-Dividend (XD) date, the sell signal is paused to account for expected cash dividend payouts.
3. **Volatility-Adjusted Stop Loss:** The stop is 2× the stock's daily ATR (clamped -3.5% to -8.0%). If ATR is unavailable, fixed tiers apply: -4.5% (SET50) / -6.5% (Mid/Small-Cap). A breach triggers a mandatory **SELL** signal to preserve capital.
4. **Market Regime Sizing & Dynamic Cash Buffer:** Adapt position sizing and cash reserves based on SET Index health:
   - **Bullish Regime:** 100% full position sizing, 10–15% cash buffer.
   - **Sideways / Chop Regime:** 75% selective sizing, 25–35% cash buffer.
   - **Bearish / Correction Regime:** 50% defensive sizing, 50%+ cash buffer.
5. **15% Single-Stock Allocation Cap:** Hard-cap exposure to any single company at max 15% of total account equity.
6. **30% Sector Allocation Cap:** Automatic warning alert if a single sector exceeds 30% concentration.
7. **Dual-Confirmation Smart Money Flow:** Foreign NVDR net accumulation is only valid when paired with non-negative Relative Strength ($\text{RS} \ge -1.0$) to avoid buying into foreign short-covering rallies on fundamentally broken stocks.
8. **Cyclical Sector Quality Shield:** Commodity and cyclical stocks (Energy, Petrochem, Agribusiness, Shipping, Steel) must prove 3Y profit growth $\ge 8\%$ and margins $\ge 10\%$ to avoid value traps at peak commodity cycles.
9. **Multi-Timeframe Weekly 20-EMA Filter:** Do not buy daily breakouts if the stock is trending below its weekly 20-EMA.
10. **The 10% Rule (Take Profit):** At +10% net profit, the app suggests locking in gains, especially if technicals are reaching the Selling Zone.

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
- **Financial Transaction Tax:** ฿0 (Officially abolished).
- **Minimum Fee:** 50 THB daily (waived when ATS + E-Statement enabled).

> **Formula:** Net Profit = (Selling Price - Sell Fees) - (Buying Price + Buy Fees)

