# Plan - Make Morning Swing Exit Alert Conditional

Modify the daily Morning Swing Exit alert logic so it only notifies the user if they have active swing positions that currently require action (Take Profit, Stop Loss, or Sell signal).

## Tasks

- [x] **Phase 1: Update StockAlertWorker.kt Alert Flag**
  - [x] Add `hasActiveSwingSellAlert` boolean flag at the start of the `StockAlertWorker` run.
  - [x] Inside the stock scraper loop of [StockAlertWorker.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/util/StockAlertWorker.kt), verify if a stock is currently held (`quantity > 0`).
  - [x] Check if the stock is a swing position or a dividend stock in transition (yield < 3%).
  - [x] Check if it meets the exit criteria (Gain >= 10%, Loss <= -5%, RSI >= 65, or IndicatorSignal.SELL).
  - [x] Set `hasActiveSwingSellAlert = true` if any match.

- [x] **Phase 2: Relocate Morning Alert Logic**
  - [x] Remove the morning alert trigger from the top of `doWork()`.
  - [x] Append the morning alert check at the end of the stock loop, adding the conditional check for `hasActiveSwingSellAlert`.

- [x] **Phase 3: Verification**
  - [x] Compile the project using `./gradlew compileDebugKotlin` to ensure no errors.

- [x] **Phase 4: AI Prompt Updates**
  - [x] Update Swing Trade AI Prompt in [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt) to list candidates with mathematical metrics (Price, P/E, P/BV, ROE, RSI, MACD Histogram, Signal/Reason) and instruct it to rank the Top 3 setups.
  - [x] Update Dividend AI Prompt in [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt) to list candidates with mathematical metrics (Price, Yield, ROE, D/E, P/E, RSI) and instruct it to select the Top 3 additions with their Max Buy Price.
  - [x] Verify the compilation of the project.

- [x] **Phase 5: Clarify Playbook Rules in AI Prompt**
  - [x] Split constraints into separate Swing/Breakout vs. Earnings Gap rules.
  - [x] Adjust risk-manager delegated task to evaluate Swing candidates with 50-day SMA and Gap Up candidates with volume/gap support.
  - [x] Compile and verify.

- [x] **Phase 6: Dynamic App Version**
  - [x] Create [AppUtils.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/util/AppUtils.kt) to retrieve package manager versionName dynamically.
  - [x] Refactor [SettingsScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/SettingsScreen.kt) to use dynamic app version.
  - [x] Refactor [AboutScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/AboutScreen.kt) to use dynamic app version.
  - [x] Compile and verify.

## Review Section
- **Implementation:** 
  - Defined `hasActiveSwingSellAlert = false` initially.
  - While iterating through all watchlist stocks in the background worker, if a swing position (or a dividend stock in transition with dividend yield < 3%) is held with `quantity > 0` and meets any exit condition (net profit >= 10%, stop loss <= -5%, RSI >= 65, or a technical SELL signal), we flag `hasActiveSwingSellAlert = true`.
  - Moved the morning window trigger (10:00 - 11:00 AM) to the end of the stock analysis loop, requiring `hasActiveSwingSellAlert == true` before triggering the system notification.
  - Enhanced both the **Swing Trade AI Prompt** and **Dividend AI Prompt** in [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt):
    - Lists of Swing, Gap Up, and Dividend candidates now embed explicit quantitative metrics directly into the prompt (e.g., Last Price, P/E, P/BV, ROE%, RSI, MACD Histogram, current Signal type & reason, Dividend Yield%, D/E ratio) to ground the AI model and prevent hallucinations.
    - Updated instruction tasks to explicitly request identifying and ranking the **Top 3 setups/additions** with concrete entry, target profit, and stop loss rules/Max Buy prices for each.
  - Split the constraints in the Swing AI Prompt:
    - Separate playbook rules are defined for Swing/Breakout candidates (requiring 50-day SMA trend alignment) and Earnings Gap candidates (relying on volume validation and gap-up support line entries instead of lagging moving averages).
    - Prevents AI confusion or unnecessary selection restrictions on Gap Up setups.
  - Refactored version display logic to load dynamically from `PackageManager` via [AppUtils.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/util/AppUtils.kt) in both [SettingsScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/SettingsScreen.kt) and [AboutScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/AboutScreen.kt).
- **Verification:** Verified by compiling the project using `./gradlew compileDebugKotlin`, which completed successfully.
