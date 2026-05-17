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

