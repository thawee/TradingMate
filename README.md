# TradingMate

TradingMate is a personal trading companion app designed to simplify stock market analysis for retail investors, specifically tailored for the Thai stock market (SET). It bridges the gap between complex technical indicators and actionable trading decisions with a focus on discipline, dividend tracking, and human-AI collaboration.

## 🚀 Concept & Idea

The core philosophy of TradingMate is **Discipline over Emotion**. By converting standard technical indicators into visual "Zones," the app helps traders identify when a stock is in an accumulation phase (Buying Zone) or a distribution phase (Selling Zone). 

It specifically addresses common beginner challenges:
- **When to Buy/Sell:** Automates entry and exit price targets based on a strict RSI 35/65 strategy.
- **Consolidated Tracking:** Merges your stock value and available cash into a single "Total Assets" view.
- **Dividend Focus:** Tracks "Yield on Cost" (YoC) and alerts you to upcoming XD dates.
- **Built with AI:** Architected and developed through a deep collaboration with Google's Gemini AI.

## ✨ Key Features

- **Integrated Discovery Hub:** A reimagined Home screen featuring "The TradingMate Story" and "Trading Academy" side-by-side for rapid learning and concept discovery.
- **Market Pulse:** Real-time monitoring of your watchlist with automated technical signals (BUY, SELL, POTENTIAL).
- **Consolidated Portfolio:** A professional-grade financial dashboard grouping stock holdings, cash balance, net profit, and fee tracking in one unified view.
- **Smart Cash Management:** Inline cash balance editing directly within your total asset summary.
- **Automated Trading Zones:** Real-time calculation of "Buy Below" and "Sell Above" price ranges using RSI (35/65 targets).
- **Precise Fee Engine:** Accurate net profit/loss tracking using a detailed InnovestX fee structure (Commission + Market Fee + VAT + Selling Tax).
- **Multi-Source Data Aggregator:** Blends real-time market data from the Stock Exchange of Thailand (SET) with historical coverage and metadata from Yahoo Finance.
- **Smart Navigation:** Optimized 5-item bottom bar (Home, Portfolio, Focus, Watchlist, Stats) for a spacious and intuitive mobile experience with Edge-to-Edge display support.

## 📊 Technical Indicators Used

TradingMate uses a suite of indicators to generate high-conviction signals. For a deep dive into the formulas and thresholds, see **[INDICATORS.md](INDICATORS.md)**.

1. **RSI (14 Days) - The Speedometer**
   - **Oversold (< 35):** Target BUY zone.
   - **Overbought (> 65):** Target SELL zone.
2. **MACD (12, 26, 9) - The Momentum Switch**
   - Tracks trend shifts. A positive histogram confirms early reversals.
3. **SMA 50 & SMA 200 - The Trend Guards**
   - **SMA 50:** Mid-term health indicator.
   - **SMA 200:** Long-term "Line in the Sand" (Bull vs. Bear trend).
4. **Bollinger Bands - The Volatility Map**
   - Prices near the **Lower Band (5%)** are statistically undervalued for their current volatility.

## 🧠 The Trading Strategy (Standardized)

TradingMate's detailed signal engine prioritizes safety and value:

- **🟢 Buy - Oversold Accumulation:** RSI < 35 while above the long-term SMA 200.
- **🟢 Buy - Early Recovery:** MACD turns positive near support or extreme RSI lows.
- **🟢 Buy - Healthy Momentum:** Positive MACD while trading above SMA 50.
- **🔴 Sell - Take Profit:** Triggers at >10% net profit or when RSI > 65.
- **🔴 Sell - Stop Loss:** Automatically alerts you to cut losses at -5% net.
- **⚠️ SELL PRIORITY:** Selling signals (Overbought/Resistance) ALWAYS override BUY momentum to prevent "Buying High."

## 📡 Data Architecture & Sources

### 1. Stock Exchange of Thailand (Official SET API)
- **Role:** Source of truth for real-time quotes, financials, and dividend records.
- **Metrics:** Last Price, Change, ROE, P/E, P/BV, EPS, Net Profit, Equity, D/E Ratio, Dividend Yield, and XD Dates.

### 2. Yahoo Finance (Historical & Smart Metadata)
- **Role:** Historical price data, technical indicators, and rich metadata for search/news.
- **Metrics:** 1-year historical Close prices, Volume, Sector, Industry, and News Feed.

## 🛠 Tech Stack

- **UI Framework:** Jetpack Compose with Material 3 (Edge-to-Edge).
- **Language:** Kotlin.
- **Image Loading:** Coil 3 for news thumbnails.
- **Asynchronous Logic:** Kotlin Coroutines & Flow.
- **Local Persistence:** Room Database.
- **Architecture:** MVVM (Model-View-ViewModel).

---
*Disclaimer: TradingMate is an educational tool. All trading involves risk. Consult with a professional financial advisor before making investment decisions.*
