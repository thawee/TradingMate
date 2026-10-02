# Changelog

All notable changes to the TradingMate project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **Core buys no longer blocked by satellite rules:** a proposed TDEX buy needs only a valid price, a 100-share lot and enough cash. The 15% single-stock cap (which contradicted the 80% core target), sector cap, stop/target and 2:1 Reward:Risk requirement, and the regime cash buffer apply to satellite buys only. The buffer was never backtested and blocked DCA in falling markets. Enforced in both the Buy dialog and `StockRepository.executeBuy`.
- **No technical sell reminders on the core:** TDEX holdings no longer get SELL-signal or trailing-stop reminders; a stop the user saved on it still alerts.
- **Neutral core badge:** TDEX shows a "Core holding" signal (buy on your DCA schedule, hold through technical signals) instead of SETUP / WATCH / EXIT, in the watchlist, stock detail and alert worker.

## [4.0.0] - 2026-10-01

Repositioned from a signal advisor to an index core with a disciplined, measured satellite, after a market-wide backtest showed the technical signals trailing TDEX buy-and-hold (see `tools/backtest/report.md`).

### Changed
- **Honest signal labelling:** signal card reads "Technical Setup" / "On Watch" / "Exit Rule" with a "Context, not a buy call" notice citing the backtest; same notice on the Swing playbook. "Accept AI Plan" is now a secondary "Save as Satellite Plan" action. Signal descriptions no longer claim "high probability" or "institutional buying".
- **Core-satellite (TDEX core):** Settings for target core % (default 80), monthly DCA amount and day. A monthly reminder on the first trading session on/after the DCA day suggests whole TDEX board lots with fees. Portfolio shows a Core vs Satellite card with the core shortfall to reach target.
- **Satellite scorecard:** Stats compares the satellite with the same cash flows replayed into TDEX (dividend-adjusted) since the first journaled fill and over two trailing 12-month windows, with money-weighted annual returns. Suggests lowering the satellite share when it trails in both windows. Holdings whose fills are not fully journaled are excluded and listed.
- **Satellite cap warning:** the Buy dialog warns when a non-TDEX purchase would push the satellite above its cap. Swing caption updated to the 2R take-profit.
- **Trend state chip:** "Buying / Potential / Selling Zone" replaced by descriptive states (Uptrend, Downtrend, Overextended, Near Support, Range). The old "Selling Zone" merged overextended highs with breakdowns at support, so it could contradict an "On Watch: Support Testing" signal.
- **Signal badges:** watchlist and advisor badges, the signal sort bubble and entry notification titles read SETUP / WATCH / EXIT instead of BUY / POTENTIAL / SELL. Stored signal names are unchanged.
- **Entry signal alerts off by default:** new Settings toggle gates BUY notifications and the 15:30 entry-window prompt. Stop and exit alerts are unchanged.
- **R-multiple exits for holdings without a saved plan:** Take-profit moved from a flat +5% / ฿500 to 2R (2 × stop distance), so winners can exceed the loss the stop accepts. The trailing stop now arms once the peak reaches +1R and also exits if price falls back to cost.

### Added
- **Evidence gate for signal rules:** `EvidenceGate` requires beating TDEX in both sub-periods, at least 50% of P/L surviving removal of the top 3 symbols, 100+ trades and positive expectancy. The backtest report prints PASS/FAIL per rule (all current rules fail). Criteria documented in `docs/ADVISOR_EVALUATION.md`.
- **Market-wide portfolio backtest** (`PortfolioBacktest`): shared-capital replay of the signal engine across a universe with fixed-fractional sizing, 15% stock cap, board lots, fees and slippage, plus a buy-and-hold benchmark. `tools/backtest/fetch_history.py` downloads dividend-adjusted history; `BACKTEST=1 ./gradlew testDebugUnitTest --tests '*MarketBacktestReport*'` writes `tools/backtest/report.md`.

### Fixed
- **Batch quotes failing (HTTP 401):** Yahoo's `v7/finance/quote` now requires a crumb, so every refresh marked all symbols failed and fell back to per-stock fetches. `fetchBatchQuotes` now uses `v7/finance/spark` in 20-symbol chunks. It no longer overwrites cached dividend yield with 0 (spark has no fundamentals, so they stay cached).
- **SET index history unusable:** Yahoo returns only today's bar for `^SET.BK`, so the market regime was stuck on Neutral, relative strength and beta were missing, and every swing candidate was blocked ("Price or SET benchmark history missing"), leaving Scan Setups empty. `fetchSetIndexHistory` now falls back to TDEX (SET50 ETF) when fewer than 120 bars come back.
- **Stale indicator cache:** indicators missing an observation or benchmark date are recomputed even inside the cache window.
- **In-app backtest window:** the Backtest screen fetched 1 year of history, leaving only ~35 bars after the 210-bar warm-up. It now fetches 3 years.
- **Overbought exit churn on large positions:** Overbought, MFI distribution and upper-band SELLs triggered once profit exceeded a flat ฿500, selling large positions at a fraction of a percent gain. They now require at least +1R.
- **Duplicated take-profit rules:** `StockViewModel` and `StockAlertWorker` re-derived their own flat take-profit and RSI ≥ 65 alerts with different thresholds; both now use the signal engine's SELL reasons.

## [3.4.0] - 2026-09-28
### Added (V2 Roadmap - The "Discipline & AI" Overhaul)
- **Epic 1: Zero-Friction Trade Logging (Gemini Vision)**: Added image picker in Buy Dialog that uses Gemini Vision API to parse broker screenshots and auto-fill Ticker, Price, and Quantity.
- **Epic 2: "Closed-Loop" AI Journal**: Added an "Accept AI Plan" button to the AI Recommendation Card to instantly save generated plans. Safely handles active holdings without overwriting quantity.
- **Epic 3: Post-Trade Autopsy**: Rebuilt the Sell Dialog to enforce exit categorization (Target Hit, Stop Hit, Mistake, etc.) and mandate a 10-character psychological lesson on losing or mistake trades.
- **Epic 4: Dynamic Trailing Stops**: Integrated Market Regime analysis into the ATR trailing stop calculation (tighter 1.5x in Bearish, looser 3.0x in Bullish).
- **Epic 5: Paper Trading (Sandbox Mode)**: Implemented isolated `stock_database_sandbox` using dynamic Room Database instantiation and a Settings toggle.

## [3.3.1] - 2026-09-26

### Fixed
- **Market regime gating (NEUTRAL treated as Bullish):** `isMarketBearish` was derived from
  `!marketRegime.isBullish`. Because `MarketRegime.NEUTRAL` has `isBullish = true`, a choppy
  or consolidating market silently bypassed the bearish candidate gate (which requires positive
  foreign flow or outperformance). Changed both call sites in `StockViewModel` and
  `DividendAdvisorScreen` to `marketRegime == MarketRegime.BEARISH` so NEUTRAL markets correctly
  enforce stricter candidate gating.
- **`isMom()` hard early-return on null Relative Strength:** The expression
  `(relativeStrength ?: return false)` caused every stock lacking 63-bar RS history to be
  silently excluded from the momentum gate and consequently the swing candidate list. Changed to
  a null-tolerant check — null RS means "data not yet available", not "lagging the market".
  Only an actively negative RS now disqualifies a stock.
- **Snapshot race condition — silent empty AI result:** If a background price refresh fired
  between sending the Gemini request and receiving the response, plan snapshot IDs no longer
  matched and all AI recommendations were discarded without any user-visible explanation. An
  explicit snackbar ("Prices changed during analysis — refresh and try again") is now shown
  whenever the validator discards picks due to stale snapshots.
- **Concentration check used market value instead of cost basis:** `existingStock` and
  `existingSector` values passed to `TradeRiskPolicy` were computed as `lastPrice × quantity`.
  This underestimates exposure for positions bought below current price and overstates it for
  positions in drawdown, making the 15%/30% allocation caps unreliable. Both now use
  `portfolio.cost × quantity` (cost basis) consistently with the intent of the risk policy.
- **Duplicate sell alerts for FIXED_TARGET dividend holdings:** A stock with an active
  `FIXED_TARGET` exit policy could trigger both the `ExitPolicyEvaluator` path (dividend alerts)
  and the swing logic path (swing alerts), appearing in both sell lists simultaneously. Applied
  `.distinctBy { symbol }` to both `swingSellAlerts` and `dividendSellAlerts` before emitting
  `AlertRoutineState`.
- **AI "no live news" disclaimer invisible:** The single-line faded `labelSmall` disclaimer
  ("No live web/news search…") below the AI button was reliably missed. Replaced with a
  prominent `secondaryContainer` info card with an ℹ️ icon and bold text warning users not to
  act on AI reasoning that cites catalysts, earnings, or news not present in the supplied data.
- **No stale-data warning before AI analysis:** Users had no indication that prices and signals
  sent to AI were stale (e.g., overnight or after a weekend). A red warning chip now appears in
  `AiCopilotCard` whenever `lastSync` is more than 12 hours old, prompting a refresh before
  running analysis.

## [3.3.0] - 2026-09-25

### Added
- Saved, versioned swing trade plans with entry, stop, target, source, time, and exit policy. Plan acceptance, clearing, AI ranking, fills, sales, and undo actions are recorded in a local advice journal.
- A shared pre-trade risk check for proposed swing buys: supported target and the configured minimum net reward to risk (default 2:1), per-trade stop risk, combined stock and sector exposure, cash reserve, and 100-share board lots. The buy dialog previews the same checks used when saving.
- Trade-history snapshots of the accepted plan, fees, peak price, and notes. JSON backups now include these records, cash transactions, advice events, dividend history, daily portfolio snapshots, and position plans. Room schema advanced to version 33, with migrations from earlier versions.
- A forward evaluation protocol in [docs/ADVISOR_EVALUATION.md](docs/ADVISOR_EVALUATION.md) for measuring advisor outcomes against recorded decisions and actual fills.

### Changed
- Swing candidate status distinguishes Ready, Watch, and Blocked based on data freshness, liquidity, 52-week-low proximity, quality, completed-week trend, and signal. Relative strength aligns stock and SET observation dates. AI can rank and explain only locally validated swing plans; its assessments are qualitative, and dividend AI output is informational.
- Fixed-plan exit alerts use the accepted stop, confirmed technical invalidation, and target in that order across the app and background worker. Legacy holdings retain their earlier exit behavior until a complete plan is saved. Alerts do not place orders or guarantee execution prices.
- The historical backtest fills signals at the next daily close and includes open positions in daily drawdown. It remains a technical-only replay; closed-trade return excludes unrealized results.
- Market quotes and fundamentals have separate freshness timestamps. Portfolio beta uses historical stock/SET returns; VaR and CVaR require 63 aligned daily observations and show unavailable when the history is incomplete or stale. AI narrative is labeled as unverified interpretation.
- Unverifiable three-year profit growth is displayed as unavailable instead of being inferred from adjacent financial-data rows. Previously cached values are cleared on upgrade; growth-dependent cyclical screening remains conservative.

### Fixed
- Clearing a target clears its fixed plan instead of silently retaining an obsolete target. Additional buys retain the saved plan and combine cost and fees; editing quantity to zero now requires a recorded sale.
- Removing a watchlist item no longer creates a sale at a cached quote or deletes a holding after a failed sale. Concurrent sales validate the remaining quantity inside the database transaction, and undo of a full sale restores the plan and fee state.
- Backup import preserves existing history by assigning new IDs and deduplicating matching content rather than overwriting rows with colliding IDs. Older backups still import, though data they never contained cannot be reconstructed.
- Invalid or failed quote/indicator fetches no longer replace usable cached prices; partial refresh failures identify affected symbols. Sales, purchases, cash changes, and dividends report persistence errors before their forms close. Dividend and cash ledger changes are atomic even on a new account.
- A sale cannot be undone twice. NAV charts and drawdown use chronological snapshots, scheduled alerts retain their cadence across app launches, and risk history is refreshed while the Stats screen is open.

## [3.2.0] - 2026-09-09

### Added
- **Expected Price Boundaries & Volatility Range Modeling**:
  - Added statistical trading channel bounds (`Est. Range: ฿[Lower] - ฿[Upper] (BB ±2σ)`) to candidate and watchlist cards in `DividendAdvisorScreen.kt` and `StockComponents.kt`, with automatic fallback to 52-week price extremes.
  - Added a dedicated **Expected Price Boundaries** card to `StockScreen.kt` displaying technical floors (support / $-2\sigma$), ceilings (resistance / $+2\sigma$), and an interactive 52-week price position range bar.
- **Interactive Archetype Tag Guide**:
  - Implemented `ArchetypeLegendDialog` in `DividendAdvisorScreen.kt` allowing users to tap any quantitative tag chip (`VCP`, `WHALE`, `SPRING`, `MOAT`, `SHIELD`, `MTF`, `QUAL`, etc.) to view its strategic definition and criteria.
- **Trade Purpose Helper Guidance**:
  - Added clear explanatory helper text in `PortfolioScreen.kt` (Buy & Edit Stock Dialogs) clarifying the fundamental rules between **Swing Trade** (enforces daily trailing stops, take-profit, and technical exits) and **Dividend** (long-term compounding, ignores swing trailing stops).

### Changed & Refined
- **Trade Plan Disambiguation**:
  - Replaced misleading `Stop ฿X • Target ฿Y` with action-oriented execution bounds: `Plan: Cut < ฿X (-...%) • Aim > ฿Y` alongside `Reward:Risk 2.0:1` notation. Eliminates the misconception that stop/target levels represent forecasted price guarantees.
- **Alert Sentiment & Visual Differentiation**:
  - Categorized Sell Alerts in `AdvisorStockCard` by sentiment: **Take Profit** alerts now render in positive green (`🎯 Take Profit...`), **Overbought** alerts in amber (`⚡ Overbought...`), and **Stop Loss** in red (`🛑 Stop Loss...`).
  - Clarified initial stop loss vs trailing stop in `StockViewModel.kt` (`Trailing Stop Loss (Drop <= -X% from peak)` vs `Stop Loss (Drop <= -X%)`).
- **Signal & Zone Terminology Polish**:
  - Renamed RSI zone badges from "Buy Below" / "Sell Above" to **"Oversold (RSI ≤ 35)"** and **"Overbought (RSI ≥ 65)"** to prevent knife-catching and premature profit-taking misconceptions.
  - Re-labeled bare "Buy" / "Sell" header in `SignalCard` (`StockScreen.kt`) to **"Bullish Setup"** / **"Bearish / Exit"**.
  - Updated AI recommendation badge from "Confidence 85%" to **"AI Conviction: 85%"**.

## [3.1.0] - 2026-09-08

### Added
- **Regime-Aware Spendable Cash & Mandated Reserve Guards**:
  - Implemented `TechnicalAnalysis.getRecommendedCashBufferPercent` and `calculateSpendableCash` establishing institutional cash reserve targets based on macroeconomic regimes: **15% in Bullish**, **30% in Sideways/Neutral**, and **50% in Bearish** conditions.
  - Injected mandated cash buffer percentages, target reserve balances in THB, and true spendable capital into Gemini AI Advisor Master Prompts (`buildSwingPrompt`, `buildDividendPrompt` in `DividendAdvisorScreen.kt`, and single-stock AI prompt in `StockScreen.kt`). Prevents AI recommendations from breaching safe liquidity reserves.
- **Interactive Live Cash Buffer Guard in Buy Order Dialog**:
  - Integrated a real-time **Cash Buffer & Liquidity** gauge card directly into `BuyStockDialog` (`PortfolioScreen.kt`).
  - Dynamically calculates post-trade cash balance and projected cash buffer percentage as the user modifies purchase share quantity.
  - Features real-time visual alerts: **Healthy (Green)** when preserving target reserve, **Warning (Orange)** when depleting cash below regime target, and **Error (Red)** on insufficient cash balance.
- **63-Day Rolling Weighted Time-Series VaR & Tail Risk (CVaR)**:
  - Added `TechnicalAnalysis.calculatePortfolioHistoricalReturns` to construct weighted portfolio daily return time series ($R_{p,t} = \sum_i w_i R_{i,t} / \sum_i w_i$) across 63 trading days (approx. 1 financial quarter).
  - Updated `InstitutionalRiskCard` in `StatsScreen.kt` to evaluate empirical 1-day 95% Historical Value-at-Risk (VaR) and Expected Shortfall (CVaR) based on true multi-asset time-series returns rather than single-day snapshot proxies.
  - Added asynchronous background fetching and in-memory caching of historical closes in `PortfolioViewModel.kt` (`loadHistoricalClosesForHoldings`).
- **Mark-to-Market (MTM) Daily NAV Performance Trajectory**:
  - Exposed `allSnapshots` StateFlow in `PortfolioViewModel.kt` and added automatic daily snapshot recording upon portfolio inspection.
  - Upgraded `StatsScreen.kt` with a segmented `FilterChip` toggle between **MTM NAV (Daily)** and **Realized PnL (Monthly)**.
  - Linked daily MTM snapshots into `TechnicalAnalysis.calculateMaxDrawdown` to measure real peak-to-trough mark-to-market drawdown including unrealized portfolio price swings.
- **Expanded Verification Suite**:
  - Added comprehensive unit tests in `TechnicalAnalysisTest.kt` for regime cash buffers, spendable cash calculations, 63-day portfolio historical return distributions, and empirical VaR/CVaR (59 unit tests passing total, 100% success rate).

## [3.0.0] - 2026-08-28

### Added
- **Multi-Timeframe (MTF) Macro Trend Alignment Engine**:
  - Implemented `TechnicalAnalysis.resampleToWeeklyCloses` and `isWeeklyMacroBullish` calculating the **Weekly 20-EMA** macro trend directly from price series.
  - Automatically downgrades daily momentum `BUY` signals with a **Macro Weekly Bearish Guard** if the weekly macro trend is broken, preventing whipsaw dip-buying in macro downtrends.
  - Added the **`MTF`** confluence chip to `StockDna.kt` when daily and weekly trends are in full alignment.
- **Thai Dividend Tax Shield (Section 47 bis Reclaim)**:
  - Added `TechnicalAnalysis.calculateThaiDividendTaxCredit` and `calculateNetYieldOnCost` computing reclaimable Corporate Income Tax (CIT 20%) credits and net Yield-on-Cost after 10% Withholding Tax (WHT).
  - Integrated a dedicated `DividendTaxShieldCard` in `PortfolioScreen.kt` displaying reclaimable tax credits and net YoC.
- **Institutional Risk Concentration & Sector Cap Shield**:
  - Enforced `MAX_SECTOR_ALLOCATION_PERCENT = 30.0` and `MAX_SINGLE_STOCK_ALLOCATION_PERCENT = 15.0` in `TradingConstants.kt`.
  - Added full user configurability in `SettingsScreen.kt` & `PreferenceRepository.kt` for Max Sector Allocation (10%–80%), Single-Stock Cap (2%–50%), and Thai Section 47 bis Corporate Income Tax (CIT) Rate (0%–30%) with range clamping and live integration into `SectorBreakdownCard`, `DividendTaxShieldCard`, and `BuyStockDialog`.
  - Upgraded `SectorBreakdownCard` in `PortfolioScreen.kt` with live asset allocation percentage meters and dynamic visual concentration warning alerts.
- **Quantitative Risk Management & CRO Suite**:
  - **1-Day 95% Historical Value-at-Risk (VaR)** & **Conditional VaR (CVaR / Expected Shortfall)**: Implemented empirical quantile loss modeling and tail loss averages (`calculateHistoricalVaR`, `calculateConditionalVaR`) displayed in both Baht (฿) and percentage.
  - **Fixed-Fractional Anti-Ruin Position Sizing Engine**: Mathematical share calculator (`calculateRecommendedPositionSize`) that sizes trades according to exact stop-loss distance and account risk budget, automatically rounding down to 100-share SET board lots and enforcing the 15% single-stock ceiling.
  - **Max Drawdown (MDD) & High-Water Mark (HWM) Tracker**: Tracks peak historical equity curves, maximum historical drawdown, and live recovery status (`calculateMaxDrawdown`).
  - **Unified Quantitative Risk Matrix Dashboard**: Integrated into `StatsScreen.kt` displaying Portfolio Beta ($\beta$), VaR 95%, CVaR, and Max Drawdown.
- **10/10 AAA-Grade Design System & Visual Polish**:
  - **Catmull-Rom $C^1$ Spline Charting**: Implemented `Path.addSmoothCubicCurve` for continuous, smooth vector paths without overshoot across `StockScreen.kt`, `StatsScreen.kt`, and `TradingMateWidget.kt`.
  - **Zero-Jitter Numeric Typography**: OpenType `tnum` (Tabular Numbers) configured across typography to eliminate horizontal decimal jitter during real-time updates.
  - **Adaptive Glassmorphic Borders**: Enhanced `GlassCard` border stroke opacity (`0.08f` on dark / `0.18f` with 1.0dp on light) for sharp card definition in all ambient lighting conditions.
  - **Robinhood-Style Live Tick Pulses**: Integrated 800ms real-time green/coral directional highlight pulses on stock card price updates.
  - **Reusable `MiniSparkline` Component**: High-efficiency gradient-filled vector sparklines for multi-asset summary tables.
- **Comprehensive Verification Suite**: Added 10 new unit tests in `TechnicalAnalysisTest.kt` and `StockDnaTest.kt` (57 unit tests passing total).

### Fixed
- **API 26 Baseline & Dead Code Removal**: Upgraded `minSdk` to 26, eliminated 17 `NewApi` lint errors, removed obsolete `@RequiresApi` annotations across screens, and deleted orphaned `SignalAlertWorker.kt`.
- **Cash Audit Ledger Consistency**: Added missing Room cash transactions (`CashTransactionEntity`) to `executeBuy`, `executeSell`, and `undoSell` operations.
- **Deep Link Navigation**: Implemented `onNewIntent` with reactive state in `MainActivity.kt` for instant deep linking when opened from Glance AppWidgets or system notifications.
- **CSV Delimiter Locale**: Explicitly enforced `Locale.US` in `CsvExporter.kt` to prevent decimal separator corruption in European/Thai comma-decimal locales.

## [2.7.0] - 2026-08-22

### Added
- **Dual-Confirmation Smart Money Flow**: Updated `StockDna.isFlow` and `isForeignWhale` to strictly couple NVDR net buying volume with Relative Strength ($\text{RS} \ge -1.0 / \ge 0.0$). Eliminates false institutional accumulation signals triggered by foreign hedge fund short-covering in downtrends.
- **Cyclical Sector Quality Shield**: Built-in detection for commodity and cyclical sectors (Petrochemicals & Chemicals, Energy & Utilities, Agribusiness, Transportation & Logistics, Steel, Mining). Penalizes volatile 3Y profit growth ($<8\%$) or margin ($<10\%$) with a score dampener, adds the **`CYC`** tag, and shields Compounder Aristocrat archetypes from peak-cycle traps.
- **Ex-Dividend (XD) Price Drop Grace Period**: Added `TechnicalAnalysis.isNearExDividendDate` to detect when a stock is within $\pm 2$ trading days of its XD date. Automatically pauses `Early Breakdown Warning` and `Weak Trend` sell signals on price drops resulting from expected cash dividend payouts.
- **Dynamic Cash Buffer Recommendations**: The Market Regime Banner on `DividendAdvisorScreen.kt` now displays adaptive cash allocation targets: **10–15% in Bull**, **25–35% in Sideways/Chop**, and **50%+ in Bearish** regimes.
- **15% Single-Stock Max Allocation Limit**: Added strict single-stock concentration caps across AI Advisor Master Prompts to protect against macro black swan risk.

### Fixed
- **Responsive Market Regime Banner UX**: Refactored the Regime Banner into a clean 2-column layout with a dedicated rounded pill badge for cash reserves, resolving text wrapping and vertical column crushing on narrow mobile screens.

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
