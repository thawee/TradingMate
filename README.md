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

- **Dividend Advisor:** A specialized planning dashboard that calculates required capital to reach passive income goals. It suggests high-yield "Dividend Stars" based on strict fundamental criteria.
- **Market Pulse:** Real-time monitoring of your watchlist with automated technical signals (BUY, SELL, POTENTIAL).
- **AI Advisor:** A centralized AI discovery hub that evaluates Swing, Gap, and Dividend opportunities and generates direct Master Prompts for deep AI analysis.
- **Consolidated Portfolio:** A professional-grade financial dashboard grouping stock holdings, cash balance, net profit, and fee tracking in one unified view.
- **Automated Trading Zones:** Real-time calculation of "Buy Below" and "Sell Above" price ranges using RSI (35/65 targets).
- **Precise Fee Engine:** Accurate net profit/loss tracking using a detailed InnovestX fee structure (Commission + Market Fee + VAT + Selling Tax).
- **Multi-Source Data Aggregator:** Blends real-time market data from the Stock Exchange of Thailand (SET) with historical coverage and metadata from Yahoo Finance.

## 🧅 The 5-Layer Filter System (Stock DNA)

TradingMate doesn't just look at price; it evaluates the "DNA" of a company using a strict 5-Layer filter to classify stocks into Swing Plays or Dividend Stars.

1. **Qual (Quality):** Evaluates management efficiency and profitability.
   - *Indicators used:* ROE > 15%, Net Profit Margin > 10%, D/E Ratio < 1.5, Profit Growth (3Y) > 10%.
2. **Val (Value):** Identifies underpriced or fair-value stocks.
   - *Indicators used:* P/E Ratio (0.1 to 15.0) and P/BV (0.1 to 1.0).
3. **Div (Dividend):** Highlights strong passive income generators.
   - *Indicators used:* Dividend Yield ≥ 5.0%.
4. **Mom (Momentum):** Detects early trend shifts and positive price momentum.
   - *Indicators used:* MACD Histogram > 0.0.
5. **Sup (Support / Setup):** Locates ideal entry zones or extreme discounts.
   - *Indicators used:* RSI < 35 (Oversold) or proprietary BUY/POTENTIAL zone signals.

**How they combine:**
- **Swing Plays:** Must pass `(Quality OR Value) AND (Momentum OR Support)`.
- **Dividend Stars:** Must pass `Dividend AND Quality`.

## 📊 Technical Indicators Used

TradingMate uses a suite of indicators to generate high-conviction signals. For a deep dive into the formulas and thresholds, see **[INDICATORS.md](INDICATORS.md)**.

1. **RSI (14 Days) - The Speedometer**
   - **Oversold (< 35):** Target BUY zone.
   - **Overbought (> 65):** Target SELL zone.
2. **MACD (12, 26, 9) - The Momentum Switch**
   - Tracks trend shifts. A positive histogram confirms early reversals.
3. **SMA 50 & SMA 200 - The Trend Guards**
   - **SMA 200:** Long-term "Line in the Sand" (Bull vs. Bear trend).
4. **Bollinger Bands - The Volatility Map**
   - Helps time entries near the **Lower Band** and exits near the **Upper Band**.

## 🧠 The Trading Strategy (Standardized)

- **🟢 Buy - Oversold Accumulation:** RSI < 35 while above the long-term SMA 200.
- **🟢 Buy - Early Recovery:** MACD turns positive near support or extreme RSI lows.
- **🔴 Sell - Take Profit:** Triggers at >10% net profit or when RSI > 65.
- **🔴 Sell - Stop Loss:** Automatically alerts you to cut losses at -5% net.
- **⚠️ SELL PRIORITY:** Selling signals (Overbought/Resistance) ALWAYS override BUY momentum.

---

## ⚠️ Disclaimer

TradingMate is designed for **educational and informational purposes only**. It does not constitute financial advice, investment recommendation, or a solicitation to buy or sell any securities.

- All trading and investment decisions are made **at your own risk**.
- The creators and developers of TradingMate are **not responsible** for any financial losses, damages, or consequences arising from the use of this application.
- Past performance does not guarantee future results.
- Always consult with a **qualified financial advisor** before making investment decisions.
