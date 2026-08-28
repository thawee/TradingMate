# Configurable Settings & User Information Plan

- [x] **Settings Pillar 1: DataStore & ViewModel Expansion** <!-- id: 951 -->
  - [x] Add `maxSectorAllocation` (default 30.0%) and `citTaxRate` (default 20.0%) to `PreferenceRepository.kt` <!-- id: 952 -->
  - [x] Update `maxPortfolioAllocation` default to 15.0% in `PreferenceRepository.kt` <!-- id: 953 -->
  - [x] Implement range clamping and sanity bounds in `SettingsViewModel.kt` <!-- id: 954 -->
- [x] **Settings Pillar 2: Rich Guidance UI in SettingsScreen.kt** <!-- id: 955 -->
  - [x] Add comprehensive institutional explanatory helper text and standard benchmark ranges for every setting <!-- id: 956 -->
  - [x] Add input fields for Single-Stock Cap, Sector Cap, and Thai Section 47 bis CIT Rate <!-- id: 957 -->
- [x] **Settings Pillar 3: Wire Settings to Portfolio & Dialogs** <!-- id: 958 -->
  - [x] Connect `maxSectorAllocation` to `SectorBreakdownCard` in `PortfolioScreen.kt` <!-- id: 959 -->
  - [x] Connect `citTaxRate` to `DividendTaxShieldCard` in `PortfolioScreen.kt` <!-- id: 960 -->
  - [x] Pass `maxPortfolioAllocation` into `BuyStockDialog` position sizing calculation <!-- id: 961 -->
- [x] **Settings Pillar 4: Verification & Test Suite** <!-- id: 962 -->
  - [x] Run `./gradlew compileDebugKotlin` & `./gradlew testDebugUnitTest` (57 tests passed) <!-- id: 963 -->
  - [x] Run `./gradlew lintDebug` (0 errors) <!-- id: 964 -->

## Review & Verification
- **Full Settings Configurability:**
  - Added user configurable **Max Sector Concentration Cap** (10%–80%, default 30%) with live warning thresholds in `SectorBreakdownCard`.
  - Added user configurable **Thai Dividend CIT Tax Rate** (0%–30%, default 20%) for Section 47 bis tax credit calculations in `DividendTaxShieldCard`.
  - Upgraded **Max Single-Stock Allocation Cap** (2%–50%, default 15%) to dynamically throttle maximum capital in `calculateRecommendedPositionSize` across all buy dialogs.
- **Input Sanitization & Range Clamping:**
  - Enforced defensive `.coerceIn()` / `.coerceAtLeast()` bounds in `SettingsViewModel.kt` to prevent runtime crashes, zero divisions, or corrupted calculation states.
- **Rich Guidance & Tooltips:**
  - Provided clear institutional-grade guidance and standard industry benchmark ranges (e.g. 1.0%–2.0% risk per trade, 10%–15% single-stock cap, 25%–30% sector cap) below every input field.
- **Verification:**
  - Added `testConfigurableSectorAndTaxCreditCalculations` in `TechnicalAnalysisTest.kt`.
  - `./gradlew testDebugUnitTest` passed (57/57 tests).
  - `./gradlew lintDebug` passed with 0 errors.












