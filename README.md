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

## 📋 Swing Playbook (Daily Discipline Tracker)

The Swing Playbook is a 3-step daily workflow to keep traders disciplined during market hours. It appears at the bottom of the Smart Advisor screen as a floating step bar.

### The 3 Steps

| Step | Name | What It Does |
|------|------|--------------|
| 1 | 🚨 Check Exits | Review sell alerts — Take Profit (≥10%), Stop Loss (≤-5%), Overbought (RSI ≥65), SELL signal |
| 2 | 🔍 Scan Setups | Review swing/gap candidates filtered by Quality, Momentum, and Support criteria |
| 3 | 🤖 Ask AI | Copy a detailed AI prompt to clipboard for external analysis (ChatGPT/Gemini/Claude) |

- Each step has a checkbox. Tapping **"Next →"** scrolls to the next step.
- When all 3 steps are checked, the bar shows **"✅ All 3 steps done! You're ready to trade."**
- If no sell alerts exist, Step 1 auto-marks as done.

### Reset Schedule

All 3 steps reset **daily after market close (16:30)**:

- **Before 16:30** (trading hours): Steps are still valid for today — no reset.
- **After 16:30** (market closed): Opening the app triggers a fresh reset for the new trading day.

This ensures your checklist stays intact during market hours and starts clean the next day.

> Note: The Dividend Playbook does not have step tracking — only the Swing Playbook follows this discipline workflow. Dividend stocks require multiple reviews throughout the day and across the week (e.g., monitoring yield changes, XD dates, fundamental shifts), so a single daily checklist is not feasible for this strategy.

---

## 🔔 Notification Alerts

TradingMate sends push notifications during specific time windows on weekdays (Asia/Bangkok timezone):

| Time Window | Alert | Description |
|-------------|-------|-------------|
| **10:00–11:00 AM** | 🌅 Morning Swing Exit | Reminds you to check active swing positions for Sell/Stop Loss alerts |
| **15:30–16:30 PM** | 🌆 Afternoon Swing Entry | Reminds you to scan the Advisor for new daily Swing & Gap candidates |
| **Within 7 days of XD date** | 💰 Ex-Dividend Alert | Per-stock alert when an ex-dividend date is approaching |
| **January & June** | 📅 Dividend Season | Reminds you to accumulate dividend-paying stocks before payout season |

**Deduplication:** Each alert fires at most once per day per type using daily SharedPreferences keys.

> Note: Notifications are skipped on weekends and when the market is closed.

## ⚠️ Disclaimer

TradingMate is designed for **educational and informational purposes only**. It does not constitute financial advice, investment recommendation, or a solicitation to buy or sell any securities.

- All trading and investment decisions are made **at your own risk**.
- The creators and developers of TradingMate are **not responsible** for any financial losses, damages, or consequences arising from the use of this application.
- Past performance does not guarantee future results.
- Always consult with a **qualified financial advisor** before making investment decisions.
