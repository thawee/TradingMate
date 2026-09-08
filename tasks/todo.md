# Option C Implementation Plan: Comprehensive Risk, Cash & Quant Suite

- [x] **Phase 1: Regime-Aware Spendable Cash in AI Prompts** <!-- id: 201 -->
  - [x] Add regime-based cash buffer target calculation (Bull: 15%, Sideways: 30%, Bear: 50%) <!-- id: 202 -->
  - [x] Inject mandated buffer, target cash reserve, and spendable capital into `buildSwingPrompt` and `buildDividendPrompt` in `DividendAdvisorScreen.kt` <!-- id: 203 -->
  - [x] Update single-stock AI prompt in `StockScreen.kt` to include cash and regime context <!-- id: 204 -->
- [x] **Phase 2: Live Cash Buffer Warning in Buy Stock Dialog** <!-- id: 205 -->
  - [x] Pass `marketRegime` into `BuyStockDialog` in `PortfolioScreen.kt` <!-- id: 206 -->
  - [x] Compute projected remaining cash and cash buffer % in real-time as user changes share quantity <!-- id: 207 -->
  - [x] Render dynamic visual cash buffer badge (healthy green vs low-cash warning banner) in `BuyStockDialog` <!-- id: 208 -->
- [x] **Phase 3: 63-Day Rolling Time-Series Portfolio VaR & CVaR** <!-- id: 209 -->
  - [x] Add `calculatePortfolioHistoricalReturns` in `TechnicalAnalysis.kt` to construct weighted daily return time series <!-- id: 210 -->
  - [x] Update `StatsScreen.kt` to use the 63-day time-series return distribution for 1-day 95% VaR & CVaR <!-- id: 211 -->
- [x] **Phase 4: Mark-to-Market Equity Trajectory from Daily Snapshots** <!-- id: 212 -->
  - [x] Expose `allSnapshots` StateFlow in `PortfolioViewModel.kt` <!-- id: 213 -->
  - [x] Integrate snapshot trajectory into `StatsScreen.kt` equity visualization <!-- id: 214 -->
- [x] **Phase 5: Verification & Testing** <!-- id: 215 -->
  - [x] Add unit tests in `TechnicalAnalysisTest.kt` for time-series VaR, cash buffer, and spendable cash calculations <!-- id: 216 -->
  - [x] Run `./gradlew compileDebugKotlin` and `./gradlew testDebugUnitTest` <!-- id: 217 -->

## Release 3.1.0 Packaging & Documentation
- [x] Update `CHANGELOG.md` with version 3.1.0 release notes <!-- id: 301 -->
- [x] Update `README.md` with new features (cash buffers, time-series VaR, MTM NAV trajectory) <!-- id: 302 -->
- [x] Bump version in `app/build.gradle.kts` to `versionCode = 25`, `versionName = "3.1.0"` <!-- id: 303 -->
- [x] Verify build and tests via `./gradlew testDebugUnitTest` <!-- id: 304 -->
- [x] Commit all changes with conventional commit message <!-- id: 305 -->















