# Strategic Safeguards & Edge-Decay Protection Plan

- [x] **Phase 1: Dual-Confirmation Smart Money Flow (NVDR + RS) in `StockDna.kt`** <!-- id: 500 -->
  - [x] Require Relative Strength $\ge 0$ in `StockDna.isFlow` and `isForeignWhale` to eliminate dead-cat bounce fake smart money spikes <!-- id: 501 -->
- [x] **Phase 2: Cyclical Sector Quality Shield in `StockDna.kt`** <!-- id: 502 -->
  - [x] Detect cyclical sectors (Petrochem, Energy, Shipping, Agri) and enforce 3Y margin consistency <!-- id: 503 -->
  - [x] Add `CYC` tag and prevent deceptive `A+` grades at peak commodity cycles <!-- id: 504 -->
- [x] **Phase 3: Ex-Dividend (XD) Price Drop Grace Period** <!-- id: 505 -->
  - [x] Add `isNearXdDate` check in `TechnicalAnalysis.kt` and `StockViewModel.kt` to suppress false early breakdown sell alerts around XD dates <!-- id: 506 -->
- [x] **Phase 4: Dynamic Cash Buffer & 15% Position Exposure Cap** <!-- id: 507 -->
  - [x] Display recommended cash buffer (10% Bull / 25% Neutral / 50% Bear) in `DividendAdvisorScreen.kt` and Regime Banners <!-- id: 508 -->
  - [x] Enforce 15% single-stock max capital allocation in AI Advisor prompts <!-- id: 509 -->
- [x] **Phase 5: Unit Testing & Verification** <!-- id: 510 -->
  - [x] Add tests in `StockDnaTest.kt` and `TechnicalAnalysisTest.kt` for all new guardrails <!-- id: 511 -->
  - [x] Run `./gradlew compileDebugKotlin` and `./gradlew testDebugUnitTest` (all passed) <!-- id: 512 -->

## Review & Verification
- All 47 unit tests passed across `TechnicalAnalysisTest` and `StockDnaTest`.
- Dual-confirmation NVDR flow prevents false signals on severe SET-lagging dead-cat bounces.
- Cyclical sector quality shield properly penalizes peak cyclical traps and tags them with `CYC`.
- Ex-Dividend grace period suppresses false Early Breakdown / Weak Trend sell alarms within 2 trading days of XD date.
- Dynamic Cash buffer guidelines (10% Bull / 25–35% Sideways / 50%+ Bear) and 15% max single-stock exposure cap integrated into Market Regime banner and AI Copilot prompts.







