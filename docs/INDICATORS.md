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
  - **< 62 (Healthy Momentum Entry Ceiling):** Allows confirmed SET breakouts above SMA 50 + Weekly EMA 20 with ADX ≥ 20 to qualify for `BUY` before becoming overextended.
  - **> 65 (Overbought Zone):** Entry block / overextended zone. For existing Swing holdings at `+1R`, exhaustion `SELL` triggers at **> 72** (or Upper Bollinger Band + RSI > 65) so trend runners can reach `+2R` or trail by ATR.

### 2. MACD (Moving Average Convergence Divergence) - The Momentum Switch
MACD tracks the relationship between two moving averages of a stock’s price.

- **Configuration:** (12, 26, 9).
- **MACD Histogram:**
  - **Meaningfully Positive (> 0.1% of last price):** Bullish momentum. The price-relative threshold prevents noise-level crossover flicker around zero.
  - **Negative (< 0):** Bearish momentum. Suggests the trend is moving DOWN.
- **Role:** Acts as a "gear shift" to confirm RSI signals. The momentum filter also requires RSI in 40–64.9 to exclude overbought stocks.

### 3. SMA (Simple Moving Average) - The Trend Guards
SMAs smooth out price data to identify the direction of the trend.

- **SMA 50 (Mid-term Health):** If the price is above SMA 50, the immediate trend is bullish.
- **SMA 200 (Long-term Trend):** The "Line in the Sand."
  - **Above SMA 200:** Bull Market / Healthy Trend.
  - **Below SMA 200:** Bear Market / Weak Trend.

### 4. Bollinger Bands - The Volatility Map
A "volatility tube" that wraps around the price.

- **Configuration:** 20-day SMA with 2.0 Standard Deviations.
- **Lower Band:** Price is low relative to its recent volatility. This is not support; the app's buy signals, which include it, were backtested and did not beat TDEX (`docs/ADVISOR_EVALUATION.md`).
- **Upper Band:** Price is high relative to its recent volatility. Not a tested sell level.

### 5. OBV (On-Balance Volume) - The Lie Detector
OBV cumulatively adds volume on up days and subtracts it on down days, revealing whether volume backs the price move.

- **Rising OBV (above its 10-day average):** Steady accumulation — big money is buying. Confirms "Early Recovery" BUY signals alongside (or instead of) a 2× volume surge.
- **Flat/Falling OBV during a rally:** The move lacks conviction and is likely to fail.

### 6. Relative Strength vs SET Index - The Race
Relative strength compares the stock's return with the SET index over aligned endpoints from a 63-session benchmark window. Missing or mismatched observation dates do not produce an actionable swing reading.

- **RS > 0:** The stock is outperforming the market. Winners tend to keep winning.
- **RS < 0:** A market laggard — even if it looks cheap, money is flowing elsewhere. An actively negative RS disqualifies a stock from the MOM DNA gate.
- **RS = null (< 63 bars of history):** Treated as "data not yet available" — the stock is **not disqualified** from the MOM gate. Only a confirmed negative RS blocks momentum status.

### 7. 52-Week Low Guard - The Trap Detector
Structural decliners "look cheap" on RSI and P/E while continually making new lows.

- **Rule:** Stocks trading within **5% of their 52-week low** are blocked from actionable swing candidates. Risky signals may still be shown separately for review.
- **Missing data:** A missing 52-week low prevents Ready swing status; it is not treated as evidence that the stock passed the guard.

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
  - **Bullish Trend:** SET Index $\ge$ SMA 50 AND MACD Histogram $\ge$ 0. Full position sizing; breakout and momentum setups proceed without extra flow/RS gate.
  - **Consolidation / Neutral:** SET Index $\ge$ SMA 50 but MACD Histogram $<$ 0. **Also enforces the bearish candidate gate** — swing entries require positive foreign NVDR flow or outperformance vs SET, same as Bear mode.
  - **Bear / Correction:** SET Index $<$ SMA 50. Activates defensive sizing (50%) and strictly requires NVDR Flow or positive Relative Strength for all Swing entries.

> **Note:** The bearish candidate gate (`isMarketBearish`) applies to both NEUTRAL and BEARISH regimes. It does **not** apply in BULLISH only.

### 13. Pre-Trade Risk:Reward & Invalidation Engine
For a proposed swing buy, TradingMate checks its planned levels and account limits:

- **Suggested Stop Loss:** Volatility-adjusted stop ($2 \times \text{ATR}\%$, clamped $-3.5\%$ to $-8.0\%$) or fixed tier ($-4.5\%$ SET50 / $-6.5\%$ Mid/Small).
- **Target:** An observed or saved level is required. The app does not manufacture a $+10\%$ or 2R target to make a proposed plan pass.
- **Net reward to risk:** Estimated profit at the target after buy and sell fees must meet the user's configured minimum (default 2:1) relative to estimated loss at the stop including those fees. A simple price-distance ratio on an advisor card is indicative only; the buy check uses share count and fees.
- **Other checks:** Per-trade loss budget, total open risk cap (`maxOpenExposure`), combined ticker and sector exposure, cash reserve, and 100-share board lots. These limits screen proposals, not actual broker fills recorded after execution.

### 14. False Breakout Guard (Institutional Flow & Volume)
Guards against "bull traps" where technical momentum appears positive but lacks institutional backing:

- **NVDR Dumping Guard:** If foreign institutions are heavily net selling (NVDR net selling $> \text{฿}5,000,000$), BUY signals are downgraded to `POTENTIAL` (watch).
- **Volume & RS Confirmation:** If a stock is lagging the SET index (Relative Strength $< -2.0$) and lacks above-average volume or rising OBV, breakout BUY signals are downgraded to `POTENTIAL`.

### 15. Trend Weakening (context, no early exit)
A holding below its 50-day SMA with negative MACD gets a *Trend Weakening* note (`NEUTRAL`), not a sell; non-holdings get *Downward Trend*. The saved stop is the exit. The earlier early-breakdown exit (sell at $-1.5\%$ on this pattern) was the largest loss source in the 2015-2025 SET50 replay (1,058 exits, -685R; tools/backtest/report.md), so it was removed. Within the XD window the dip is labelled *Ex-Dividend Grace Period*.

### 16. Multi-Timeframe (MTF) Macro Trend Alignment (Weekly 20-EMA)
To avoid counter-trend "knife-catching" in larger macro downtrends:
- **Resampling:** Dated daily history is grouped into completed calendar weeks. The current unfinished week is excluded from the weekly EMA.
- **Weekly 20-EMA:** Computes the 20-period Exponential Moving Average on weekly closes.
- **Macro Alignment Guard:** When the current price is below the EMA of completed weekly closes, daily momentum `BUY` signals are downgraded to `POTENTIAL` (*Macro Weekly Bearish*). Missing weekly history leaves the guard unknown.
- The `MTF` chip was removed on 2026-10-08: untested, and the tested trend filters did not beat TDEX.

### 17-18. Beta, VaR and CVaR (removed)
Removed from the app on 2026-10-08: 63 days of returns is too noisy to act on, and none of them changed a decision. The Stats screen keeps max drawdown (below). The helpers remain in `TechnicalAnalysis` with their unit tests.

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

- **Pre-Filter:** Liquidity (daily turnover > ฿5M), Freshness (matching stock/benchmark trading session within 7 days), **AND** not within 5% of the 52-week low.
- **Layer 1 — QUAL (SET-Calibrated):** ROE > 10%, NPM > 5% (null-tolerant), non-financial D/E < 2.0 (null-tolerant; financial sector exempt), and 3Y profit growth ≥ 0% when available.
- **Layer 2 — VAL:** P/E 0.1–15.0 and P/BV 0.1–1.0.
- **Layer 3 — DIV:** Dividend yield ≥ 5%.
- **Layer 4 — MOM:** MACD histogram > 0.1% of price, RSI 40–64.9, and Relative Strength vs SET ≥ 0.
- **Layer 5 — SUP:** BUY/POTENTIAL signal (incorporates SMA 200 trend context).
- **Layer 6 — FLOW:** Foreign Fund NVDR net accumulation ($> 0$) with Relative Strength $\ge -1.0$.

---

## 🏛️ Fundamental Guardrails (Quality Rules — SET Calibrated)

Before looking at technical signals, TradingMate evaluates the "DNA" of a company. A stock passes the **Quality** screen and receives the **"⭐ Quality"** tag if it meets these SET-calibrated checks:

1. **ROE (Return on Equity) > 10%:**
   - **Why:** Calibrated for the Thai SET (where >15% kept only 13 of 101 watchlist stocks); dividend fundamental review notes still warn below 15%.
2. **Net Profit Margin > 5% (null-tolerant):**
   - **Why:** Includes high-efficiency Thai retail, commerce, and hospitality blue chips (`CPALL`, `HMPRO`, `CRC`) while filtering out razor-thin or loss-making margins.
3. **Profit Growth (3Y) ≥ 0% (null-tolerant):**
   - **Why:** Requires non-contracting earnings when verified 3-year growth data is present; missing growth data does not disqualify a stock.
4. **D/E Ratio (Debt to Equity) < 2.0 (non-financials, null-tolerant):**
   - **Why:** Accommodates capital-intensive Thai telecom, utility, and infrastructure leaders (`ADVANC`, `GULF`, `BEM`) while blocking over-leveraged companies. Banks, finance, and insurance companies are exempt.

> **⭐ Quality Priority:** Stocks that pass these rules are highlighted with a **"⭐ Quality"** tag in all signals and push notifications. In the Advisor's Research explorer, `Quality` and `Weekly uptrend` are opt-in filter chips by default so you can see all liquid, fresh setups first and narrow down on demand.

---

## 💰 Dividend Advisor & Planning

The **Dividend Advisor** screen helps you plan for long-term passive income.

- **Passive Income Goal:** You can set a **Target Monthly Dividend** in the app Settings.
- **Capital Calculation:** The advisor calculates the total capital required to reach your monthly goal (assuming a 5% average yield).
- **Suggested Stocks:** The advisor ranks the **Top 10 SET50 stocks by dividend yield** (the tested `F5` rule) and provides a monthly rebalance plan alongside your core `TDEX` / `1DIV` DCA schedule.
- **Progress Tracking:** The app compares your current portfolio's estimated monthly dividends against your target to show your "Goal Progress."
- **Section 47 bis Tax Shield:** Displays estimated tax credits reclaimable during annual personal income tax filing.

---

## 🧠 Trading Zones & Market Pulse

TradingMate categorizes every stock into one of four "Zones" based on technical logic. These are visualized in the **Market Pulse** section of the Home page.

### 🟢 Buying Zone (Accumulation)
Stocks here represent the best value-to-risk ratio.
- **Criteria:** RSI < 35 **OR** (MACD is Bullish **AND** (Price is near Lower Bollinger Band **OR** (Price > SMA 50 **AND** RSI < 62 **AND** ADX ≥ 20))).
- **Signal:** *Oversold Accumulation*, *Early Recovery*, or *Healthy Momentum*.

### 🟡 Potential Zone (Watchlist)
Stocks that are becoming cheap but haven't confirmed a reversal yet.
- **Criteria:** RSI < 42 **OR** Price is near Lower Bollinger Band.
- **Signal:** *Nearing Value Zone*, *Support Testing*, *False Breakout Guard*, *Falling Knife Guard*, or *Macro Weekly Bearish*.

### 🔴 Selling Zone (Distribution)
Stocks that are overvalued or have broken their upward trend.
- **Criteria:** For holdings at `+1R`, RSI > 72 (exhaustion) **OR** (Price near Upper Bollinger Band **AND** RSI > 65) **OR** MFI ≥ 80. Bearish MACD below SMA 50 is context (*Trend Weakening* / *Downward Trend*), not a sell.
- **Signal:** *Overbought*, *Upper Band Resistance*, *Distribution Detected*.

### ⚪ Neutral Zone
Stocks with no clear trend or extreme valuation.
- **Signal:** *Wait & Watch*.

---

## 🛡️ Institutional Risk Management (The Golden Rules)

1. **Let Winners Run to +2R / Trailing Stop:** Once a Swing position reaches `+1R`, the volatility trailing stop ($2.5 \times \text{ATR}\%$) arms automatically, and overbought exits wait until true exhaustion (`RSI > 72` or Upper Bollinger Band + `RSI > 65`) soSET trend runners can reach `+2R`.
2. **No Early Breakdown Exit:** Losing SMA 50 with negative MACD is a note, not a sell; the saved stop is the exit (the early exit lost money in the 2015-2025 replay). Dividend-purpose holdings follow the tested high-yield rule instead: only a saved stop alerts, and leaving the high-yield top 10 is reviewed monthly.
3. **Volatility-Adjusted Stop Loss:** The stop is 2× the stock's daily ATR (clamped -3.5% to -8.0%). If ATR is unavailable, fixed tiers apply: -4.5% (SET50) / -6.5% (Mid/Small-Cap). A breach triggers a mandatory **SELL** signal to preserve capital.
4. **Market Regime Sizing & Dynamic Cash Buffer:** Adapt position sizing and cash reserves based on SET Index health:
   - **Bullish Regime:** 100% full position sizing, 10–15% cash buffer.
   - **Sideways / Chop Regime:** 75% selective sizing, 25–35% cash buffer.
   - **Bearish / Correction Regime:** 50% defensive sizing, 50%+ cash buffer.
5. **15% Single-Stock Allocation Cap:** Hard-cap exposure to any single company at max 15% of total account equity.
6. **30% Sector Allocation Cap:** Automatic warning alert if a single sector exceeds 30% concentration.
7. **Dual-Confirmation Smart Money Flow:** Foreign NVDR net accumulation is only valid when paired with non-negative Relative Strength ($\text{RS} \ge -1.0$) to avoid buying into foreign short-covering rallies on fundamentally broken stocks.
8. **Cyclical Sector Quality Shield:** Commodity and cyclical stocks (Energy, Petrochem, Agribusiness, Shipping, Steel) do not pass the growth-dependent shield when verified 3Y growth is unavailable.
9. **Multi-Timeframe Weekly 20-EMA Filter:** Do not buy daily breakouts if the stock is trending below its weekly 20-EMA.
10. **Saved Target Rule:** New fixed swing plans alert at their accepted target. Legacy holdings retain older profit rules until their plan is completed.
11. **Odd-Lot Dust Ignore Rule (`< 100` Shares):** Holdings of `1..99` shares (below 1 SET board lot of 100 shares, e.g. leftover shares from a stock dividend) resolve to `ODD_LOT_HOLD` (`NEUTRAL`) in `HoldingSignal.resolve` and are ignored by Advisor sell alerts, the monthly rebalance calculator (`HighYieldRebalance.plan`), widget alert counts, and background sell notifications.

---

## 🔔 Automated Alerts & Notifications

TradingMate monitors saved stocks with in-app alerts and periodic Android notifications. Notification timing depends on WorkManager and market data availability.

### 1. Push Notifications (Background Monitoring)
The app schedules a background worker every 30 minutes to monitor stocks in your **Watchlist**.
- **Market Hours:** Stock scans run while the market is open; time-window reminders are checked separately.
- **Trigger Conditions:** Signal changes and qualifying active exit conditions on full-lot holdings (`≥ 100` shares) can produce a notification. Saved swing plans use their accepted stop and target.

### 2. In-App Dividend Alerts (XD Dates)
On the Advisor screen and in background checks, the app tracks corporate action dates.
- **Trigger:** If a stock has an upcoming Ex-Dividend (XD) date within the defined window (default 14 days, customizable in **Settings**), it triggers an XD notification.
- **End of Year Option:** You can also choose to alert on all upcoming XD dates until the end of the current year.

---

## 💸 Fee Structure (InnovestX)

TradingMate calculates **Net Profit** per position by accounting for the following fees (approx. 0.32% round-trip):

- **Commission:** 0.15% (Safety estimate).
- **Market Fee:** 0.007% (Trading + Clearing + Regulatory).
- **VAT:** 7% on total commissions.
- **Financial Transaction Tax:** ฿0 (Officially abolished).
- **Minimum Fee:** 50 THB daily per full-lot position (waived when ATS + E-Statement is enabled, and not applied to `< 100` share odd-lot positions).

> **Formula:** Net Profit = (Selling Price - Sell Fees) - (Buying Price + Buy Fees)
