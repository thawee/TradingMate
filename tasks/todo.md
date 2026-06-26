# TradingMate — Critical Fixes Progress

## ✅ Phase 1: Data Correctness (Complete)

- [x] **Fix 1: Historical prices reversed** — Removed `.reversed()` in `SetScraper.fetchTechnicalIndicators()` and `StockViewModel.fetchStockData()`. TA functions now receive correct chronological data.
- [x] **Fix 2: netProfitMargin & profitGrowth3Y not persisted** — Added columns to `StockEntity`, DB migration 16→17, persist in both refresh paths, added to watchlist mapping.
- [x] **Fix 3: restoreBackup() not atomic** — Wrapped in `database.withTransaction { }`.
- [x] **Fix 4: recordSell() non-atomic** — `adjustCash()` now uses atomic SQL.
- [x] **Fix 5: adjustCash() race condition** — New `adjustCashBy` query uses `UPDATE cash SET balance = balance + ?`.
- [x] **Fix 6: getTradingZone() unreachable paths** — `isPriceAboveSma50` defaults to `false` when unknown.

## ✅ Phase 2: Data Safety (Complete)

- [x] **Fix 7: Import backup confirmation dialog** — Added `GlassDialog` before import. Warns user that all data will be replaced.
- [x] **Fix 8: Portfolio delete warning** — Enhanced `StockItemCard` delete dialog to show "Sell & Remove Stock?" with position details when stock has holdings.
- [x] **Fix 9: beginningCash calculation** — Now accounts for current portfolio's invested capital: `cash + investedCapital - totalProfit`.
- [x] **Fix 10: @Index on trade_history.dateMillis** — Added index + DB migration 17→18 for faster trade history queries.

## ✅ Phase 3: State & UX (Complete)
- [x] Replace `remember` with `rememberSaveable` for key UI state
- [x] Move advisor computation to ViewModel / `derivedStateOf` (Moved to remember block keyed on watchlist)
- [x] Add search debouncing
- [x] Fix SET50/SET100/SETHD collection data
- [x] Add error state visibility for failed refreshes

## ✅ Phase 4: Architecture (Complete)
- [x] Split `StockEntity` into Portfolio + Cache + Signal entities (Completed via subagent)
- [x] Split `StockViewModel` into feature-scoped ViewModels (Settings, Portfolio, and Watchlist ViewModels extracted)
- [x] Route workers through Repository (StockAlertWorker updated)
- [x] Add retry/backoff for HTTP calls (Implemented in SetScraper)
- [x] Remove dead code (`SetApi.kt`, `NetworkModule.kt` removed)

## ✅ Phase 5: UI Polishing & Premium Design (Complete)
- [x] Integrate modern typography (`Inter` font from Google Fonts)
- [x] Refine Dark Mode into "Glassy Obsidian" aesthetic
- [x] Add subtle gradient glows (Radial gradients in background)
- [x] Add micro-animations (Scaling on card press)
- [x] Create custom Animated Donut Chart component for Portfolio Allocation

## 🏗️ Phase 6: Core Feature Additions
- [x] **Feature 2: Android Home Screen Widget**
  - [x] Create a Glance/AppWidget showing Total Assets, Today's P/L, and active Sell alerts.
  - [x] Setup background update worker for the widget.
- [x] **Feature 3: Actual Dividend Tracking**
  - [x] Add `DividendHistoryEntity` (symbol, date, amount per share, total received, tax deducted).
  - [x] Add Room DAOs and relations.
  - [x] Create UI for manually logging received dividends.
  - [x] Show total dividend earned in Portfolio screen.
  - Add a UI flow to record a received dividend payment.
- [x] **Feature 4: Trailing Stop-Loss**
  - Add `peakPrice` to `StockPortfolioEntity` to track highest price since purchase.
  - [x] Implement trailing stop logic (sell if price drops 5% below `peakPrice`).
  - [x] Add UI configuration for the trailing stop percentage.

## ✅ Phase 7: Widget & Navigation Enhancements (Complete)
- [x] Verify Intent Navigation: Widget clicks and notifications now route precisely to Portfolio, Watchlist, or Advisor screens.
- [x] Widget UI Polish: Redesigned widget using `androidx.glance` with rounded corners, custom themes (Dark/Light), and a premium aesthetic.
- [x] Portfolio Trend Graph: Widget now displays a dynamic sparkline graph of portfolio historical value tracking (snapshots saved daily via `StockAlertWorker`).

## ✅ Phase 8: Defect Review & UI Fixes (Complete)
- [x] **C1**: Fix edit holding double-deducting cash
- [x] **C2**: Fix double-removal on watchlist removal
- [x] **H1**: Fix cash adjust snackbar sign
- [x] **H3**: Fix buy fee fallback missing ATS flag
- [x] **H4 & M5**: Fix raw double formatting on price display across Sell Dialog and Stock Cards
- [x] **M1**: Add scope badge to PortfolioSummaryCard
- [x] **M2**: Only show Yield on Cost for DIVIDEND tab
- [x] **M3**: Standardize netProfitBaht formula with netProfitPercent
- [x] **M4**: Log silent deep-refresh failures
- [x] **L1**: Extract `StockRepository` singleton to custom Application class
- [x] **L2 & L3**: Fix daily/weekly checklist resets to trigger on ISO week boundaries
