# Changelog

All notable changes to the TradingMate project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.6.0] - 2026-08-22

### Added
- **Quantitative Confluence Scoring Engine (0–100 pts)**: Integrated a multi-pillar scoring algorithm in `StockDna.kt` calculating weighted sub-scores for Quality (25), Value (20), Momentum/Trend (25), Flow/RS (15), and Dividend/Safety (15). Classifies stocks into conviction tiers: `A+` ($\ge 80$), `A` ($65–79$), `B` ($50–64$), and `C` ($< 50$).
- **5 One-Tap Strategy Archetypes**: Added quant strategy filters in `StockDna.kt`:
  - `isCompounderAristocrat` (`MOAT`): High ROE $\ge 12\%$, D/E $\le 1.2$, stable margins, consistent yield.
  - `isVcpBreakout` (`VCP`): Stage 2 Uptrend (Price $\ge$ SMA 50 $\ge$ SMA 200) with momentum & volume.
  - `isHighYieldShield` (`SHIELD`): Dividend yield $\ge 5\%$ with strict quality and low-trap protection.
  - `isForeignWhale` (`WHALE`): Foreign NVDR net accumulation with positive Relative Strength vs SET.
  - `isOversoldRebound` (`SPRING`): Extreme oversold ($RSI \le 35$) with baseline profitability.
- **Strategy Preset Filter Bar**: Added horizontal scrollable archetype chips to `DividendAdvisorScreen.kt` for instant preset filtering.
- **10/10 AAA-Grade Fintech UI/UX Elevation**:
  - **OpenType Tabular Figures (`tnum`)**: Configured across all typography in `Type.kt` for zero-jitter numeric alignments during live updates.
  - **Shimmer Placeholder Engine**: Implemented `Modifier.shimmerPlaceholder()`, `StockCardSkeleton`, and `StockDetailSkeleton` in `StockComponents.kt`.
  - **Interactive Touch-Scrubbing Chart**: Upgraded `PriceTrendChart` with touch gestures, vertical guideline, Bézier curves, glowing pulse beacon, and floating glass tooltip pill (`฿ Price • Change% • Point #`).
  - **Sensory Tactile Haptics**: Added subtle haptic ticks (`TextHandleMove`) on pull-to-refresh, card taps, chart scrubbing, and checklist toggles, plus confirmation pulses (`LongPress`) on trade executions.
  - **Directional Fluid Motion**: Implemented horizontal slide & fade navigation transitions across screens via `AnimatedContent`.
  - **Dynamic Tag Highlighting**: Color-coded badges by conviction type (Mint `A+`, Blue `A`, Violet `VCP`, Emerald `MOAT`/`SHIELD`, Cyan `WHALE`, Amber `SPRING`/`OS`).

### Fixed
- **Thread Safety**: Fixed `NetworkOnMainThreadException` by moving synchronous SET Index scraping off `alertRoutineState` Flow combination into an asynchronous `Dispatchers.IO` state flow.

## [2.5.0] - 2026-08-14

### Added
- **Pre-Trade Risk:Reward & Invalidation Display**: Every candidate and watchlist card displays calculated Stop Loss (฿ and -%), Target (฿ and +%), and Risk:Reward ratio badge (e.g., `R:R 2.2:1`) prior to entering trades (`calculateSuggestedStopLossPrice`, `calculateSuggestedTargetPrice`, `calculateRiskRewardRatio`).
- **Market Regime Gate & Banner**: Real-time evaluation of SET Index trend relative to 50-day SMA and MACD histogram (`TechnicalAnalysis.getMarketRegime`). Displays a dynamic Regime Header Banner (`📈 Bullish Trend` vs `⚠️ Bear / Correction`) on the Advisor screen with automatic recommended position sizing (`100%` vs `50% Defensive`).
- **Strict False Breakout Guards**: Automatically downgrades Swing BUY momentum signals to `POTENTIAL` (watch) when foreign institutions are heavily dumping (NVDR net selling > ฿5,000,000) or when breakouts lack volume while lagging the SET index (Relative Strength < -2.0).
- **Early Breakdown Exit Alerts**: Proactively flags positions in slight drawdown ($\le -1.5\%$) that lose SMA 50 with negative MACD, triggering an `Early Breakdown Warning` `SELL` signal to cut losses before hitting a full stop loss.

### Changed
- **Bear Market Swing Filter**: In `StockDna.isSwingCandidate`, Swing setups during Bear/Correction market regimes strictly require positive Foreign NVDR Flow or positive Relative Strength to prevent entering market laggards.
- **SET NVDR API Client**: Updated `SetScraper.kt` to target `https://www.set.or.th/api/set/nvdr-trade/stock-trading?sortBy=symbol&symbols={SYMBOL}` with proper referer headers and instant 404 handling.
- **StockAlertWorker Background Engine**: Propagated peak price, SET50 tiering, explicit stop-loss, Relative Strength, and NVDR net volume/value to ensure background notifications exactly match real-time ViewModel calculations.

## [2.4.0] - 2026-08-07

### Added
- **NVDR Flow Tracking (6th DNA Layer)**: Track Foreign Fund Flow by scraping NVDR net buying volume and value from the SET API (`nvdrNetVolume`, `nvdrNetValue`). The DNA Engine now features a "FLOW" layer to boost the win rate of Swing strategies when foreign institutions accumulate a stock.
- **Sector Rotation & Concentration Visualization**: The `PortfolioScreen` now includes a `SectorBreakdownCard` that breaks down portfolio allocations by sector/industry. It automatically alerts the user (Yellow Warning) if a single sector exceeds a 30% concentration threshold to maintain portfolio diversification.

### Changed
- **Swing Play Filter**: The `DividendAdvisorScreen` and AI Advisors now enforce the `isFlow()` requirement to verify NVDR net accumulation on recommended Swing plays.
- **Database Schema**: Upgraded Room Database to v30 to persistently cache NVDR volume and value data across the portfolio and screener.

## [2.3.0] - 2026-08-06

### Added
- **AI Bull & Bear Market Subagent (`[regime-manager]`)**: Integrated automatic market regime evaluation into all AI prompts (Stock Detail, Swing Advisor, Dividend Advisor) to adapt trade parameters based on prevailing market trends (Price vs. SMA 50/200 & MACD).
- **Available Cash Balance Integration**: AI prompts now read live portfolio cash balance (`cashBalance`) and instruct the `[risk-manager]` subagent to split available cash across top-ranked setups.
- **In-App Cash Allocation Badge**: Extended `GeminiClient.kt` JSON schema and updated `AiRecommendationCard` composable in `DividendAdvisorScreen.kt` to display exact position size in THB and estimated share counts directly in the app.

### Changed
- **AI Prompt Optimization**:
  - Added missing `Price: $lastPrice THB` to `StockScreen.kt` data sheet.
  - Standardized asymmetric **min 2.0:1 Risk/Reward ratio** across all AI prompts.
  - Added direct API fallback guidance for live web search when running in-app without web search grounding.
- **Version Bump**: Updated `versionCode` to `19` and `versionName` to `2.3.0` in `app/build.gradle.kts`.

### Fixed
- Fixed in-app AI recommendation card formatting where cash allocation and position sizing details were previously not displayed in direct API mode.

## [2.2.0] - 2026-08-05

### Added
- Asset Allocation Pie Chart in `PortfolioScreen.kt` with glass legend.
- CSV Exporter for portfolio holdings and trade history (`CsvExporter.kt`).
- SET XD Dividend Calendar timeline in `DividendAdvisorScreen.kt`.
- Background Signal & Price Alert Monitor (`SignalAlertWorker.kt` with `WorkManager`).

## [2.1.0] - 2026-08-01

### Added
- 5-Layer Stock DNA filtering engine (Qual, Val, Div, Mom, Sup).
- Direct in-app Google Gemini API client integration (`GeminiClient.kt`).
- Swing Playbook 3-step daily discipline tracker with floating step bar.
