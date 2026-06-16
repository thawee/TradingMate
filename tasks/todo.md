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
