# TradingMate

TradingMate is a personal trading companion app designed to simplify stock market analysis for retail investors, specifically tailored for the Thai stock market (SET). It bridges the gap between complex technical indicators and actionable trading decisions with a focus on discipline and dividend tracking.

## 🚀 Concept & Idea

The core philosophy of TradingMate is **Discipline over Emotion**. By converting standard technical indicators into visual "Zones," the app helps traders identify when a stock is in an accumulation phase (Buying Zone) or a distribution phase (Selling Zone). 

It specifically addresses common beginner challenges:
- **When to Buy/Sell:** Automates entry and exit price targets based on a strict RSI 35/65 strategy.
- **Managing Fees:** Calculates net profit/loss by accounting for InnovestX fee structure (approx. 0.32% round-trip).
- **Dividend Focus:** Tracks "Yield on Cost" (YoC) and alerts you to upcoming XD dates.
- **Emotional Bias:** Provides clear technical signals (BUY, SELL, NEUTRAL) based on data, not feelings.

## ✨ Key Features

- **Automated Trading Zones:** Real-time calculation of "Buy Below" and "Sell Above" price ranges using RSI (35/65 targets).
- **Multi-Source Data Engine:** Scrapes data from Google Finance and SET Nuxt-state for deep metrics like ROE, D/E, and Yield.
- **Curated Collections:** Instantly import "Dividend Stars," "Bluechips," or the full "SET100" into your watchlist.
- **Fundamental Guardrails:** Identifies "Quality Stocks" based on ROE (>10%) and Debt-to-Equity (<1.5) ratios.
- **Personalized Portfolio:** Live net profit tracking with automated cash flow and weighted Yield on Cost.
- **Dividend Alerts:** Notifies you of stocks in your watchlist with upcoming XD dates (within 14 days).

## 📊 Technical Indicators Used

TradingMate uses a suite of indicators to generate high-conviction signals. For a deep dive into the formulas and thresholds, see **[INDICATORS.md](INDICATORS.md)**.

1. **RSI (14 Days) - The Speedometer**
   - **Oversold (< 35):** Target BUY zone.
   - **Overbought (> 65):** Target SELL zone.
2. **MACD (12, 26, 9) - The Momentum Switch**
   - Tracks the shift in trend. A positive histogram confirms early reversals.
3. **SMA 50 & SMA 200 - The Trend Guards**
   - **SMA 50:** Mid-term health indicator.
   - **SMA 200:** Long-term "Line in the Sand." Defines Bull vs. Bear trend.
4. **Bollinger Bands - The Volatility Map**
   - Prices near the **Lower Band (5%)** are statistically undervalued for their current volatility.

## 🧠 The Trading Strategy (Standardized)

TradingMate's `getDetailedSignal` engine prioritizes safety and value:

- **🟢 Buy - Oversold Accumulation:** RSI < 35 while above the long-term SMA 200.
- **🟢 Buy - Early Recovery:** MACD turns positive near support or extreme RSI lows.
- **🟢 Buy - Healthy Momentum:** Positive MACD while trading above SMA 50.
- **🔴 Sell - Take Profit:** Triggers at >10% net profit or when RSI > 65.
- **🔴 Sell - Stop Loss:** Automatically alerts you to cut losses at -5% net.
- **⚠️ SELL PRIORITY:** Selling signals (Overbought/Resistance) ALWAYS override BUY momentum to prevent "Buying High."

## 🛠 Tech Stack

- **UI Framework:** [Jetpack Compose](https://developer.android.com/compose) with Material 3.
- **Language:** Kotlin.
- **Asynchronous Logic:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html).
- **Local Persistence:** [Room Database](https://developer.android.com/training/data-storage/room) (Version 8 with deep fundamentals).
- **Architecture:** MVVM (Model-View-ViewModel).

---
*Disclaimer: TradingMate is an educational tool. All trading involves risk. Consult with a professional financial advisor before making investment decisions.*
