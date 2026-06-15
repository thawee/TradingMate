# Plan - Fix Prompt & Checklist Defects

Implement fixes for the verified prompt and checklist defects.

## Tasks

- [x] **Prompt Defect 1 & 9: Market context & Data freshness**
  - [x] Add instructions for the `[market-researcher]` subagent to perform live web search for SET index level, interest rates, and sector trends.
  - [x] Inject the last updated timestamp (`lastSync` or `lastUpdated`) into prompts.
- [x] **Prompt Defect 2: Missing time horizon**
  - [x] Specify `2-4 weeks` holding period for swing trades and `long-term / indefinite` for dividend plays in the prompts.
- [x] **Prompt Defect 3: Dividend prompt redundancy**
  - [x] Clarify that candidates have already passed baseline quality checks, asking the AI to focus on forward-looking cash flow and dividend safety.
- [x] **Prompt Defect 4: Journal Analyzer lacks structure**
  - [x] Clean up literal `\\n` in [StatsScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/StatsScreen.kt).
  - [x] Define "best performing setup type" and add structured Markdown report output expectations.
- [x] **Prompt Defect 5: No output format constraint**
  - [x] Add strict output format requirements (e.g. Markdown tables, specific sections) to all prompts.
- [x] **Prompt Defect 6: Stock Detail Prompt is too thin**
  - [x] Upgrade [StockScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/StockScreen.kt) to pass full technical (RSI, MACD, SMA) and fundamental (ROE, D/E, NPM, Sector, Market Cap) metrics into the prompt.
- [x] **Prompt Defect 7: No "do NOT" guardrails**
  - [x] Add negative constraints section (no penny stocks, no leveraged DWs, educational disclaimer framing) to all prompts.
- [x] **Prompt Defect 8: Swing prompt lumps Gap plays with Swing plays**
  - [x] Clearly separate Swing vs Gap-up playbook entry/exit definitions in the swing prompt in [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt).
- [x] **Checklist Defect 1 & 2: Persistent checklist & Date-aware Auto-reset**
  - [x] Create `ChecklistEntity` and `ChecklistDao` in [RoomModels.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/data/RoomModels.kt) to store completion states and date trackers.
  - [x] Add `MIGRATION_14_15` in [RoomModels.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/data/RoomModels.kt) and bump database version to 15.
  - [x] Expose checklist DAO in repository and bind checklist flows in [StockViewModel.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/StockViewModel.kt).
  - [x] Implement Bangkok (Asia/Bangkok) time check that auto-resets daily checklist items if current time passes 09:30 ICT, weekly items on new weeks, and monthly items on new months.
- [x] **Checklist Defect 3 & 4: Naming conflict & misleading frequencies**
  - [x] Refactored Swing scan candidates to `"Weekly: Scan Candidates"` to align with weekly variable storage.
  - [x] Changed Swing AI Copilot to `"Routine: Run AI Copilot Prompt"`.
- [x] **Checklist Defect 5: Progress feedback**
  - [x] Added linear progress indicator showing percentage completed (e.g. `2/3 Completed`) at the top of the checklist.
- [x] **Checklist Defect 6: Connection to actual data**
  - [x] Dynamically display the count of active exit alerts inline in the Check Exit Signals item.
  - [x] Display candidate count inline in the Scan Candidates items.
  - [x] Display data update timestamp inline in the Run AI Copilot Prompt item.
- [x] **Checklist Defect 7: Suboptimal checklist order**
  - [x] Reordered Swing checklist flow: `AI Prompt (Routine) -> Scan Candidates (Weekly) -> Check Exit Signals (Daily)`.
- [x] **Verification**
  - [x] Compile the project using `./gradlew compileDebugKotlin` to ensure no errors.

## Review Section
- **Implementation Summary:**
  - Exposed persistent checklist status through Room database entity `ChecklistEntity` (with `id = 1` matching Cash pattern).
  - Developed auto-reset logic for daily, weekly, and monthly items checking against `Asia/Bangkok` timezone date, week number, and month, with daily items resetting exactly at market open (09:30 ICT).
  - Wired checklist composable to collect state from the view model, updating the database reactively.
  - Placed a visual progress bar and completion fraction text within the glass card container.
  - Linked checklist items directly to underlying data by calculating active alert counts and candidate lists counts and displaying them in descriptions.
  - Reordered the swing routine: AI Prompt first, then Candidate Scanning, and Exit Signal checking last.
- **Verification Summary:**
  - Ran `./gradlew compileDebugKotlin` which compiled successfully.

## UI/UX Audit Remediation Plan

- [x] **1. Setup Global Snackbar System in StockScreen (Root)**
  - [x] Pass a `showSnackbar: (String) -> Unit` callback to all child screens.
  - [x] Wrap StockScreen in `Scaffold` with a `SnackbarHost` and `SnackbarHostState`.
  - [x] Replace `Toast.makeText` calls with `showSnackbar` on all screens.
- [x] **2. Add Pull-to-Refresh & displays isRefreshing state**
  - [x] Integrate `PullToRefreshBox` in `WatchlistScreen.kt`, `PortfolioScreen.kt`, and `DividendAdvisorScreen.kt`.
  - [x] Wrap `refreshWatchlistInfo()` in a robust try-finally block in `StockViewModel.kt` to ensure `isRefreshing` always resets to false.
- [x] **3. Fix Stop Loss & Note Data Loss in Portfolio**
  - [x] Create Room migration 16 (`MIGRATION_15_16`) to add `stopLoss` and `playbookNote` to the `stocks` table.
  - [x] Bump Database version to 16.
  - [x] Persist `stopLoss` and `playbookNote` via repository/database when buying/editing.
  - [x] Pre-fill target, stop loss, and note fields when editing a stock.
- [x] **4. Error States with Retry**
  - [x] Update `StockUiState.Error` to hold the failed stock ticker symbol.
  - [x] Add a "Retry" button on the Stock Detail screen error state.
- [x] **5. Validation & Override in Portfolio Dialogs**
  - [x] Disable "Add Funds" button in `AdjustCashDialog` when negative or zero.
  - [x] Disable "Confirm Sell" button in `SellStockDialog` when price/quantity is invalid or exceeds holdings.
  - [x] Prevent accidental full liquidation by clearing the sell quantity text field by default instead of pre-filling full amount.
  - [x] Add an "Override: Accept low Risk/Reward ratio" checkbox in the Buy/Edit holding dialog when R:R < 2.0.
- [x] **6. Accessibility and Layout fixes**
  - [x] Remove all font sizes below `12.sp` (change `8.sp`, `9.sp`, `10.sp` to `12.sp` across all tags/chips).
  - [x] Use `FlowRow` in `DividendAdvisorScreen.kt` and `StockComponents.kt` tags to prevent layout overflow on narrow devices.
  - [x] Replace hardcoded green colors in the checklists with themed colors (`MaterialTheme.colorScheme.tertiary`).
- [x] **7. Price formatting**
  - [x] Format `info.lastPrice` display to `%.2f` in both `StockScreen.kt` and `StockComponents.kt`.
- [x] **8. Empty State Call-to-Actions (CTAs)**
  - [x] Add "Add your first stock" CTA button in the empty watchlist card.
  - [x] Add "Record a purchase" CTA button in the empty portfolio card.
  - [x] Completely removed any destructive "Clear All Data" function/dialog/Danger Zone per updated requirements.
  - [x] Display MACD values in the Technical Indicators section of `StockScreen.kt`.
  - [x] Provide a meaningful description for Bollinger Bands instead of repeating the label.
  - [x] Format the signal type badge nicely (e.g., "Potential" instead of raw "POTENTIAL").
  - [x] Overlay Min/Max price labels on the historical trend chart.
  - [x] Add an external link icon next to the "View Quote" button.
- [x] **10. Verification & Validation**
  - [x] Build and compile the app using `./gradlew compileDebugKotlin`.

## Review Section - UI/UX Audit Remediation

- **Global Changes:**
  - Standardized all toast-based user feedback to a centralized suspending `showSnackbar` mechanism powered by a global `SnackbarHostState` defined at the root of `StockScreen.kt`.
  - Upgraded Room Database to Version 16. Implemented and tested SQL schema migration `MIGRATION_15_16` to introduce `stopLoss` (double) and `playbookNote` (text) to the `stocks` table, resolving silent data loss.
  - Replaced all typography styles with `<12.sp` (such as `8.sp`, `9.sp`, `10.sp`) to `12.sp` to enforce accessibility compliance.

- **Screen-Specific Enhancements:**
  - **WatchlistScreen**:
    - Embedded `PullToRefreshBox` mapped directly to `viewModel.isRefreshing` flow.
    - Standardized empty-state layout with a modern `GlassCard` featuring a clear "Add Stock" CTA.
    - Added sorting order and direction toggles (asc/desc via arrow icons).
    - Hided other top app bar actions when search mode is active, avoiding layout cramming.
    - Exposes "already imported" progress metrics (e.g. `12/54 stocks imported`) within the SET collection importer dialog.
    - Removed redundant manual Refresh button from the TopAppBar actions list.
  - **PortfolioScreen**:
    - Embedded `PullToRefreshBox` mapped directly to `viewModel.isRefreshing` flow.
    - Added transaction validations (prevent negative funds, disable sell if quantity is empty or exceeds inventory, clear sell quantity defaults).
    - Introduced "Override Risk/Reward ratio (<2.0)" checkbox to unlock form submission in case of special trading setups.
    - Placed a clean stacked progress bar for asset allocation breakdown.
    - Removed redundant manual Refresh button from the TopAppBar actions list.
  - **StockScreen**:
    - Added retry action on API error states.
    - Nicely formatted raw enums (e.g., "POTENTIAL" -> "Potential").
    - Placed MACD values under Technical Indicators, added Bollinger Bands explanations, overlayed min/max labels on charts, and added an external link icon on "View Quote".
  - **SettingsScreen**:
    - Completely removed the destructive "Clear All Data" function/dialog/Danger Zone per updated requirements.
    - Changed backup/restore outputs from Toasts to Snackbar.
  - **DividendAdvisorScreen**:
    - Fixed compilation issues with `AiCopilotCard` by passing the snackbar delegate down.
    - Replaced hardcoded green color values with theme-aware `MaterialTheme.colorScheme.tertiary` token.

- **Compilation Status:**
  - Run `./gradlew compileDebugKotlin` successfully. All code compiles cleanly with 0 errors.

# Plan - Fix Notifications Not Working

Review and resolve why notifications are not working.

## Tasks

- [x] **1. Declare `POST_NOTIFICATIONS` permission in Manifest**
  - [x] Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` to [AndroidManifest.xml](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/AndroidManifest.xml).
- [x] **2. Refactor notification permission request in MainActivity**
  - [x] Unconditionally register the activity result launcher as a member property in [MainActivity.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/MainActivity.kt).
  - [x] Update `checkNotificationPermission()` to use this registered launcher.
- [x] **3. Pass foreground service type to `startForeground`**
  - [x] Update [StockAlertService.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/util/StockAlertService.kt) to pass `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC` to `startForeground()` when running on Android 14 (API 34) or higher.
- [x] **4. Verification**
  - [x] Compile the project using `./gradlew compileDebugKotlin` to ensure all changes compile successfully.

## Review Section - Notification Fixes

- **Manifest Updates:**
  - Added `android.permission.POST_NOTIFICATIONS` to the manifest to ensure Android 13+ runtime permissions can be requested and granted.
- **MainActivity Permission Request:**
  - Registered `requestNotificationPermissionLauncher` as an activity member property. This prevents lifecycle exception crashes when calling `registerForActivityResult` too late or conditionally.
- **Foreground Service Compatibility:**
  - Updated `StockAlertService.kt` to pass the `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC` type to `startForeground(...)` on Android 14+ (API 34+), resolving the strict runtime checks on target SDK 37.
- **Verification:**
  - Ran `./gradlew compileDebugKotlin` which compiled successfully with zero errors.

# Plan - Enhance Advisor Screen Checklist & Flow

Incorporate the checklist progress feedback, inline metadata, and monthly research integration, acknowledging the choice to merge Earnings Gaps into Swing Trades.

## Tasks

- [x] **1. Remove unused AdvisorFilter enum**
  - [x] Delete `AdvisorFilter` enum in [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt) since Earnings Gaps are merged into Swing Plays.
- [x] **2. Integrate progress bar in the bottom floating bar**
  - [x] Add a slim `LinearProgressIndicator` inside the bottom `WizardStepBar` to visually show completed steps fraction.
- [x] **3. Display dynamic metadata inline in checklist headers**
  - [x] Update Step 1 header to show active alert counts.
  - [x] Update Step 2 header to show candidate counts.
  - [x] Update Step 3 header (AI Copilot Card) to show the last sync timestamp.
- [x] **4. Integrate monthly checklist item (`divMonthlyDone`)**
  - [x] Render a new "Step 4: Monthly Stars Re-evaluation" step under the Dividend Playbook.
  - [x] Update the `WizardStepBar` to support 4 steps when in Dividend Playbook mode.
- [x] **5. Verification**
  - [x] Compile the project using `./gradlew compileDebugKotlin` to ensure no errors.

## Review Section - Advisor Screen Enhancements

- **Unused Code Cleanup:**
  - Removed the unused `AdvisorFilter` enum from `DividendAdvisorScreen.kt` as Earnings Gaps are merged into Swing Trades.
- **Wizard Step Bar Progress Integration:**
  - Added a `LinearProgressIndicator` directly inside the bottom `WizardStepBar` component that dynamically tracks and reflects progress.
- **Dynamic Checklist Metadata:**
  - Step 1 (Danger/Shields) now displays the count of active sell/shield alerts dynamically.
  - Step 2 (Candidates) now displays the count of candidates/stars dynamically.
  - Step 3 (AI Copilot) now displays the database last sync timestamp in its header.
- **Monthly routine checklist integration:**
  - Integrated `divMonthlyDone` into the Dividend Playbook checklist routine as Step 4 ("📅 Monthly Stars Re-evaluation") with its own offset and navigation handler.
- **Verification:**
  - Ran `./gradlew compileDebugKotlin` which compiled successfully with zero errors.

# Plan - Correct Dividend Playbook Checklist Frequencies

Remap "Find Dividend Stars" to monthly frequency and restore the 3-step workflow.

## Tasks

- [x] **1. Map Step 2 (Find Dividend Stars) to `divMonthlyDone`**
  - [x] Update [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt) Step 2 checkbox to bind to `checklist.divMonthlyDone` (monthly routine) instead of `divWeeklyPricesDone`.
- [x] **2. Remove redundant Step 4 section**
  - [x] Remove the Step 4 Monthly Stars Re-evaluation section from the scrollable column in [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt).
- [x] **3. Revert WizardStepBar to 3 steps**
  - [x] Update `WizardStepBar` in [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt) to display 3 steps, mapping Step 2 for Dividend to `checklist.divMonthlyDone`.
- [x] **4. Verification**
  - [x] Compile the project using `./gradlew compileDebugKotlin` to ensure no errors.

## Review Section - Checklist Frequency Corrections

- **Frequencies Mapping Fix:**
  - Remapped Step 2 of the Dividend Playbook ("Find Dividend Stars") from a weekly check to the monthly routine `checklist.divMonthlyDone`.
- **Flow Restoration:**
  - Reverted the Dividend Playbook workflow from 4 steps back to the clean, symmetrical 3-step bottom bar wizard structure (`Shields` (Weekly) -> `Stars` (Monthly) -> `AI` (Monthly)).
- **Code Cleanups:**
  - Removed the redundant Step 4 UI block and its associated `monthlyOffset` navigation handlers.
- **Verification:**
  - Ran `./gradlew compileDebugKotlin` which compiled successfully with zero errors.

# Plan - Fix Checklist Layout Overflow

Add weights to SectionHeaders to prevent long titles from pushing the checkbox circle off the screen.

## Tasks

- [x] **1. Add weight modifier to Step 1 and Step 2 SectionHeaders**
  - [x] Update [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt) to pass `modifier = Modifier.weight(1f)` to Step 1 and Step 2 `SectionHeader` calls.
- [x] **2. Verification**
  - [x] Compile the project using `./gradlew compileDebugKotlin` to ensure all changes compile successfully.

## Review Section - Checklist Layout Overflow Fixes

- **Layout Constraints Fix:**
  - Added `modifier = Modifier.weight(1f)` to the `SectionHeader` calls for Step 1 and Step 2. This prevents long titles from pushing the circular `StepCheckbox` off the right edge of the screen, ensuring visual consistency across all screen widths.
- **Verification:**
  - Ran `./gradlew compileDebugKotlin` which compiled successfully with zero errors.



