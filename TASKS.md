# UI/UX Consistency Roadmap

## Phase 1: Standardize UI Patterns (The "Glass" Core)
- [x] Refactor `AboutScreen.kt` to use `AppBackground` and `GlassCard`.
- [x] Refactor `TradingEducationScreen.kt` to use `AppBackground` and `GlassCard`.
- [x] Update `FocusSettingsDialog` in `StockScreen.kt` to match standard `GlassDialog` spacing.

## Phase 2: Component Extraction & Standardization
- [x] Extract a reusable `SectionHeader` component into `StockComponents.kt`.
- [x] Apply `SectionHeader` to all screens (Home, StockDetails, Portfolio, Stats, Settings, Education, About).

## Phase 3: Navigation & Header Consistency
- [x] Add `CenterAlignedTopAppBar` to `TradingEducationScreen.kt`.
- [x] Standardize icon sizes in all `TopAppBar` actions to 24dp.
- [x] Standardize screen titles (e.g., "About TradingMate" -> "About").

## Phase 4: Maintenance & Best Practices
- [x] Externalize all remaining hardcoded strings into `strings.xml`.
- [x] Review all `LazyColumn` vs `verticalScroll` implementations for consistent padding.


## Phase 5: The "Discipline & AI" Overhaul (V2 Roadmap)
- [x] **Epic 1: Zero-Friction Trade Logging (Gemini Vision)**
  - Implement intent receiver for image sharing/screenshots.
  - Integrate Gemini Vision API to extract Ticker, Price, Qty, Fees from broker screenshots.
  - Auto-fill the `BuyStockDialog` with extracted data.
- [x] **Epic 2: "Closed-Loop" AI Journal**
  - Update Gemini prompt in `DividendAdvisorScreen` to enforce structured JSON output (Entry, Stop, Target).
  - Add "Accept AI Plan" button to auto-fill the Swing Plan.
  - Track `ai_assisted` flag in `TradeEntity` and build AI win-rate scorecard in `StatsScreen`.
- [x] **Epic 3: Post-Trade Autopsy (Psychological Journaling)**
  - Detect when a trade is closed (hits stop/target).
  - Trigger Autopsy Dialog (Did you follow plan? Emotional state 1-10).
  - Save autopsy data to database and chart Discipline vs. PnL in `StatsScreen`.
- [x] **Epic 4: Dynamic Trailing Stops**
  - Monitor active positions against `MarketRegime`.
  - Trigger high-priority local notification if regime shifts to BEAR while holding long positions.
  - Suggest tightening trailing ATR stops automatically.
- [x] **Epic 5: Paper Trading (Sandbox Mode)**
  - Add `isPaperTrading` toggle in Settings.
  - Create a separate database table or flag for fake ฿1,000,000 portfolio.
  - Branch repository logic to read/write to the paper portfolio when enabled.
