# AI Prompt Review & Optimization Plan

- [x] **Task 1: Comprehensive Review of App AI Prompts** <!-- id: 17 -->
  - [x] Analyze prompt structures across `DividendAdvisorScreen.kt` and `StockScreen.kt`. <!-- id: 18 -->
  - [x] Identify conflicts between API JSON Schema (`GeminiClient`) and text prompt instructions. <!-- id: 19 -->
  - [x] Audit data completeness (missing fields like Last Price in `StockScreen.kt`). <!-- id: 20 -->
  - [x] Review risk management rules and consistency (R:R ratios, ELI10 vs analytical clarity). <!-- id: 21 -->

- [x] **Task 2: Refine Prompts & Fix Code Issues** <!-- id: 22 -->
  - [x] Fix missing `Price` parameter in `StockScreen.kt` AI prompt. <!-- id: 23 -->
  - [x] Harmonize in-app Gemini API prompt instructions with JSON output mode. <!-- id: 24 -->
  - [x] Standardize Risk/Reward constraints across prompts. <!-- id: 25 -->
  - [x] Run `./gradlew test` and verify code builds cleanly. <!-- id: 26 -->

- [x] **Task 3: Implement [regime-manager] (Bull/Bear Subagent) in Prompts** <!-- id: 27 -->
  - [x] Add `[regime-manager]` role to `buildSwingPrompt` and `buildDividendPrompt` in `DividendAdvisorScreen.kt`. <!-- id: 28 -->
  - [x] Add `[regime-manager]` role to `StockScreen.kt` prompt. <!-- id: 29 -->
  - [x] Run `./gradlew testDebugUnitTest` to verify end-to-end. <!-- id: 30 -->

- [x] **Task 4: Cash Balance & Portfolio Capital Splitting in Prompts** <!-- id: 31 -->
  - [x] Expose `cashBalance` StateFlow in `StockViewModel.kt`. <!-- id: 32 -->
  - [x] Include `AVAILABLE CASH BALANCE` in `DividendAdvisorScreen.kt` prompts (`buildSwingPrompt` & `buildDividendPrompt`). <!-- id: 33 -->
  - [x] Instruct `[risk-manager]` subagent to split cash balance across top recommended tickers (position size in THB and shares). <!-- id: 34 -->
  - [x] Update Markdown output format to show **Position Size (THB & Est. Shares)**. <!-- id: 35 -->
  - [x] Run `./gradlew testDebugUnitTest` to verify end-to-end. <!-- id: 36 -->

- [x] **Task 5: In-App Gemini API Schema & UI Card Alignment** <!-- id: 37 -->
  - [x] Add `cashAllocation` property to `AiRecommendation` data class and JSON schema in `GeminiClient.kt`. <!-- id: 38 -->
  - [x] Update `AiRecommendationCard` in `DividendAdvisorScreen.kt` to render Cash Allocation badge & bold price levels. <!-- id: 39 -->
  - [x] Run `./gradlew testDebugUnitTest` to verify end-to-end. <!-- id: 40 -->

