# Plan: Fix All Confusing Financial & UI Terminology

## Overview
Address all identified ambiguous, panic-inducing, or misleading UI labels and badges across the application:
1. Reframe RSI "Buy Below" / "Sell Above" into technical zones ("Oversold Zone" / "Overbought Zone").
2. Reframe giant "BUY" / "SELL" banner into "Bullish Setup" / "Bearish / Exit".
3. Differentiate "Sale Alert" colors and titles: green for Take Profit, amber for Overbought, red for Stop Loss.
4. Add archetype explanation legend dialog/sheet for cryptic DNA tags (VCP, WHALE, SPRING, MOAT, SHIELD).
5. Clarify Reward-to-Risk notation from `R:R 2.0:1` to `Reward:Risk 2.0:1`.
6. Clarify AI Copilot confidence from `Confidence 85%` to `AI Conviction: 85%`.
7. Add explicit helper captions explaining the difference between `SWING` (daily stops & take profit) and `DIVIDEND` (long-term, no trailing stops).

---

## Todo Checklist

- [x] **Phase 1: RSI & Technical Signal Clarity (`StockScreen.kt` & `strings.xml`)** <!-- id: 201 -->
  - [x] Update `strings.xml` and `StockScreen.kt` for RSI zones: `Oversold (RSI ≤ 35)` & `Overbought (RSI ≥ 65)` <!-- id: 202 -->
  - [x] Update `SignalCard` in `StockScreen.kt`: replace bare "Buy"/"Sell" with "Bullish Setup" / "Bearish / Exit" <!-- id: 203 -->
- [x] **Phase 2: Alert Color & Sentiment Differentiation (`DividendAdvisorScreen.kt` & `StockViewModel.kt`)** <!-- id: 204 -->
  - [x] Style Take Profit alerts with green/tertiary container & `🎯 Take Profit` title <!-- id: 205 -->
  - [x] Style Overbought alerts with amber container & `⚡ Overbought (RSI ≥ 65)` <!-- id: 206 -->
  - [x] Keep red container strictly for actual risk/loss alerts (Stop Loss, Drawdown, Trailing Stop) <!-- id: 207 -->
  - [x] Distinguish initial stop loss from trailing stop in `StockViewModel.kt` when stock has not reached new peaks <!-- id: 208 -->
- [x] **Phase 3: Candidate Card Notation & Archetype Legend (`DividendAdvisorScreen.kt`, `StockComponents.kt`)** <!-- id: 209 -->
  - [x] Update badge notation to `Reward:Risk 2.0:1` <!-- id: 210 -->
  - [x] Update AI card from `Confidence 85%` to `AI Conviction: 85%` <!-- id: 211 -->
  - [x] Add an Archetype Legend dialog in `DividendAdvisorScreen.kt` explaining tags (`VCP`, `WHALE`, `SPRING`, `MOAT`, etc.) <!-- id: 212 -->
- [x] **Phase 4: Trade Purpose Helper Caption in `PortfolioScreen.kt`** <!-- id: 213 -->
  - [x] Add explanatory helper text for `SWING` vs `DIVIDEND` trade purpose in stock dialogs <!-- id: 214 -->
- [x] **Phase 5: Verification & Testing** <!-- id: 215 -->
  - [x] Compile and run all unit tests <!-- id: 216 -->
  - [x] Document results in `tasks/todo.md` <!-- id: 217 -->

---

## Review & Results
- **RSI & Signal Clarity:**
  - Replaced ambiguous "Buy Below" / "Sell Above" with "Oversold (RSI ≤ 35)" and "Overbought (RSI ≥ 65)".
  - Replaced commanding "Buy" / "Sell" in `SignalCard` with "Bullish Setup" / "Bearish / Exit".
- **Alert Sentiment Differentiation:**
  - In `AdvisorStockCard`, Take Profit alerts now render with a green surface (`🎯 Take Profit...`), Overbought with amber (`⚡ Overbought...`), and Stop Loss with red (`🛑 Stop Loss...`).
  - In `StockViewModel.kt`, clarified "Trailing Stop Loss (Drop <= -5% from peak)" vs initial "Stop Loss".
- **Candidate Notation & Tag Guide:**
  - Changed `R:R 2.0:1` to `Reward:Risk 2.0:1` in candidate cards.
  - Changed `Confidence 85%` to `AI Conviction: 85%` in AI recommendations.
  - Made archetype tags (`VCP`, `MOAT`, `WHALE`, etc.) interactive with an informative `ArchetypeLegendDialog`.
- **Trade Purpose Clarification:**
  - Added clear helper text in `PortfolioScreen.kt` clarifying that Swing enforces trailing stops and take profit, while Dividend mode ignores trailing stops for long-term compounding.
- **Verification:**
  - `./gradlew compileDebugKotlin` and `./gradlew testDebugUnitTest` passed with 0 errors.
