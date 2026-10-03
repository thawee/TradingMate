# Plan: Fix All Confusing Financial & UI Terminology

## Overview
Address all identified ambiguous, panic-inducing, or misleading UI labels and badges across the application:
1. Reframe RSI "Buy Below" / "Sell Above" into technical zones ("Oversold Zone" / "Overbought Zone").
2. Reframe giant "BUY" / "SELL" banner into "Bullish Setup" / "Bearish / Exit".
3. Differentiate "Sale Alert" colors and titles: green for Take Profit, amber for Overbought, red for Stop Loss.
4. Add archetype explanation legend dialog/sheet for cryptic DNA tags (VCP, WHALE, SPRING, MOAT, SHIELD).
5. Clarify Reward-to-Risk notation from `R:R 2.0:1` to `Reward:Risk 2.0:1`.
6. Clarify AI Copilot confidence from `Confidence 85%` to `AI Conviction: 85%`.
7. Add explicit helper captions explaining the difference between `SWING` (daily stops & take profit) and `DIVIDEND` (long-term, no trailing stops).

---

## Todo Checklist

- [x] **Phase 1: RSI & Technical Signal Clarity (`StockScreen.kt` & `strings.xml`)** <!-- id: 201 -->
  - [x] Update `strings.xml` and `StockScreen.kt` for RSI zones: `Oversold (RSI ≤ 35)` & `Overbought (RSI ≥ 65)` <!-- id: 202 -->
  - [x] Update `SignalCard` in `StockScreen.kt`: replace bare "Buy"/"Sell" with "Bullish Setup" / "Bearish / Exit" <!-- id: 203 -->
- [x] **Phase 2: Alert Color & Sentiment Differentiation (`DividendAdvisorScreen.kt` & `StockViewModel.kt`)** <!-- id: 204 -->
  - [x] Style Take Profit alerts with green/tertiary container & `🎯 Take Profit` title <!-- id: 205 -->
  - [x] Style Overbought alerts with amber container & `⚡ Overbought (RSI ≥ 65)` <!-- id: 206 -->
  - [x] Keep red container strictly for actual risk/loss alerts (Stop Loss, Drawdown, Trailing Stop) <!-- id: 207 -->
  - [x] Distinguish initial stop loss from trailing stop in `StockViewModel.kt` when stock has not reached new peaks <!-- id: 208 -->
- [x] **Phase 3: Candidate Card Notation & Archetype Legend (`DividendAdvisorScreen.kt`, `StockComponents.kt`)** <!-- id: 209 -->
  - [x] Update badge notation to `Reward:Risk 2.0:1` <!-- id: 210 -->
  - [x] Update AI card from `Confidence 85%` to `AI Conviction: 85%` <!-- id: 211 -->
  - [x] Add an Archetype Legend dialog in `DividendAdvisorScreen.kt` explaining tags (`VCP`, `WHALE`, `SPRING`, `MOAT`, etc.) <!-- id: 212 -->
- [x] **Phase 4: Trade Purpose Helper Caption in `PortfolioScreen.kt`** <!-- id: 213 -->
  - [x] Add explanatory helper text for `SWING` vs `DIVIDEND` trade purpose in stock dialogs <!-- id: 214 -->
- [x] **Phase 5: Verification & Testing** <!-- id: 215 -->
  - [x] Compile and run all unit tests <!-- id: 216 -->
  - [x] Document results in `tasks/todo.md` <!-- id: 217 -->

---

## Review & Results
- **RSI & Signal Clarity:**
  - Replaced ambiguous "Buy Below" / "Sell Above" with "Oversold (RSI ≤ 35)" and "Overbought (RSI ≥ 65)".
  - Replaced commanding "Buy" / "Sell" in `SignalCard` with "Bullish Setup" / "Bearish / Exit".
- **Alert Sentiment Differentiation:**
  - In `AdvisorStockCard`, Take Profit alerts now render with a green surface (`🎯 Take Profit...`), Overbought with amber (`⚡ Overbought...`), and Stop Loss with red (`🛑 Stop Loss...`).
  - In `StockViewModel.kt`, clarified "Trailing Stop Loss (Drop <= -5% from peak)" vs initial "Stop Loss".
- **Candidate Notation & Tag Guide:**
  - Changed `R:R 2.0:1` to `Reward:Risk 2.0:1` in candidate cards.
  - Changed `Confidence 85%` to `AI Conviction: 85%` in AI recommendations.
  - Made archetype tags (`VCP`, `MOAT`, `WHALE`, etc.) interactive with an informative `ArchetypeLegendDialog`.
- **Trade Purpose Clarification:**
  - Added clear helper text in `PortfolioScreen.kt` clarifying that Swing enforces trailing stops and take profit, while Dividend mode ignores trailing stops for long-term compounding.
- **Verification:**
  - `./gradlew compileDebugKotlin` and `./gradlew testDebugUnitTest` passed with 0 errors.

---

# Advisor reliability repair

**Audit 2026-10-03 (Keep tasks).** Ticked items cite their evidence. Still open: Task 6 freshness parity tests; Task 13 docs and release review. Found and fixed during the audit: the TDEX index proxy dropped sessions with a null close, so the benchmark lagged stocks by one session and the freshness gate blocked nearly every list.

**Re-scoped 2026-10-03 (core-first decision, [plan](plan.md#core-first-repositioning-plan)).** Kept: work that protects money in any flow (saved plans, exits and stop alerts on holdings, data freshness, cash/cap checks on every buy, accurate recording of executed fills, honest claims and release review). Deferred: work that only improves swing trade proposals (candidate eligibility, target provenance, Gemini swing plans, swing backtest repair, advice capture). Deferred tasks stay unchecked; reopen them only if swing entries show an edge in a pre-registered test. Unchecked items were not re-verified against current code.

Implementation plan: [plan.md](plan.md). The completed terminology work above is retained.
The checklist below is an acceptance checklist. Items stay open until all parts of the item are verified, including device checks where needed.

## Follow-up code review fixes, 2026-09-25

- Removing a held symbol now requires a recorded sale; it no longer creates a sale at the cached quote or deletes shares after an error. The edit form also rejects zeroing held quantity.
- Sales read and validate the latest quantity inside the database transaction. Sale history now retains the accepted plan, fees and position details so Undo can restore a fully sold holding.
- Clearing a target explicitly clears the fixed plan and records the revision. Backup import preserves local rows when IDs collide, includes cash transactions, and round-trips position notes and peaks.
- The stock detail screen applies the saved exit policy and quantity-aware net profit. Background notifications use the saved exit description. Dividend AI requests qualitative analysis until locally validated dividend plans exist.
- Relative strength uses matching stock and SET dates; actionable entry freshness requires the same latest session.
- JVM tests, debug build, lint and instrumentation-test compilation passed. The migration and repository integrity instrumentation tests still need a connected Android device to execute.

## Implementation status, 2026-09-24

- Implemented: versioned saved targets/stops with migration and backup defaults; fixed-target exit evaluator in foreground and worker; swing Ready/Watch/Blocked assessment and completed-week trend; fee-aware portfolio P/L and proposed-trade reward/risk; risk checks at preview and repository confirmation; separate recorded-fill intent; local AI plan validation; technical-only replay labels and next-close exits; advice event journal and export.
- Verified: `./gradlew :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passed, followed by `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` after the final code changes. Lint reported 0 errors and 122 warnings. `git diff --check` passed.
- Device check: a migration instrumentation test was added and compiled, but `adb devices` showed no connected device. Migration execution, app restart, notification behavior, and end-to-end user flow remain unverified.
- Open product work: apply the Ready/Watch/Blocked assessment consistently to dividend and every archetype; full source/session freshness and market-holiday handling; identical screen/worker fixture tests; complete net reward/risk in both advisor cards; explicit AI plan-ID acceptance/link to a later fill; full advisor replay with recorded historical inputs or forward snapshots; spread/slippage, portfolio sizing and board-lot accounting in replay; old/new decision comparison. See [evaluation protocol](../docs/ADVISOR_EVALUATION.md).

## Task 1: Persist a versioned trade plan (Keep)

- [x] Add a typed plan and additive persistence for entry, stop, target, source, strategy, exit policy, timestamps and revision; preserve existing portfolio data. Audit 2026-10-03: `TradePlan` (version, entry, stop, target, strategy, source, exit policy, createdAt); persisted on the portfolio row.
- [x] Existing holdings migrate as incomplete legacy plans without fabricated targets. Repository reads/writes retain plans across updates. Audit 2026-10-03: `migratedHoldingHasNoInventedTarget`, device `migrationRetainsLegacyHoldingAndAddsEmptyPlan`.
- [x] Verify migration from current schema version 30 and plan serialization with focused persistence tests; assemble debug. Audit 2026-10-03: device instrumentation run: TradePlanMigrationTest (30->31->32, 32->33) and StockRepositoryIntegrityTest, 9/9 passed; DB now v33.

**Dependencies:** None. **Scope:** Medium.
**Likely files:** new `domain/TradePlan.kt`, `data/RoomModels.kt`, `data/StockRepository.kt`, new persistence/migration tests; test dependencies if required.

## Task 2: Round-trip the user's plan through forms and backup (Keep)

- [x] Pass the entered target through the view model and repository; editing/reopening/restarting displays the accepted plan, independently of focus targets. Audit 2026-10-03: `addToWatchlist(targetPrice)` -> `StockRepository` accepted target; edits keep the accepted plan.
- [x] Export/import plan fields with old-backup defaults. Plan revision, partial sale and additional same-symbol buy behavior is explicit and preserves recorded holdings/fees. Audit 2026-10-03: `oldBackupDefaultsToLegacyPlan`, `acceptedPlanFieldsRoundTripThroughBackup`, `fullSaleUndoRestoresAcceptedPlanAndFees`, device undo tests.
- [x] Verify save/reload and old/new backup round-trips; manually enter 100/95/110 and confirm the target survives restart and export/import. 2026-10-03 on the phone, with the user's approval: database backed up; recorded PTT 100 @ 41.50, stop 39.50, target 45.50 (same 1:2 shape as 100/95/110, around the real price so no alert fired) as an executed fill; saved as FIXED_TARGET v1; after force-stop and relaunch the holding card showed Take Profit 45.50 / Stop Loss 39.50 and Edit Holding reloaded 45.5 / 39.5. Database restored: integrity ok, cash, holdings and every table back to the pre-test counts (the test's 1 cash transaction and 2 advice events gone). Export/import is covered by `acceptedPlanFieldsRoundTripThroughBackup`, not repeated manually.

**Dependencies:** 1. **Scope:** Medium.
**Likely files:** `ui/PortfolioScreen.kt`, `ui/StockViewModel.kt`, `data/RoomModels.kt`, `data/StockRepository.kt`, plan round-trip tests.

## Checkpoint A: Plan survives the complete user flow

- [x] Migration and round-trip checks pass; `./gradlew :app:testDebugUnitTest :app:assembleDebug` passes. Audit 2026-10-03: unit and device tests above; build passes.
- [x] Existing holdings, cash and completed trade history remain intact. Audit 2026-10-03: device integrity tests pass; the live database migrated to v33 with holdings and cash present.

## Task 3: Implement one exit policy (Keep)

- [x] Add a pure structured exit evaluator using saved plans; price-stop checks work with missing indicators, stops never silently widen, and risk exits take precedence. Audit 2026-10-03: `ExitPolicyEvaluator` for fixed plans: saved stop before target, needs no indicators, never widens the stop. Legacy/dividend holdings still use other paths (next item).
- [x] Define legacy/dividend behavior and early invalidation reasons. Planned trades do not use unrelated 3%/5% profit overrides; fees honor configured settings and actual buy fees when available. 2026-10-03 (user decision: follow the tested rule): Dividend-purpose holdings exit only on a saved stop or leaving the high-yield top 10 (monthly); ROE, yield and drawdown are review notes (`HoldingSignal`, `DividendExitPolicy`). Swing legacy holdings keep the R-based engine exits; fixed plans keep the evaluator, without the 3%/5% overrides. Early breakdown was removed earlier (largest loss source in the replay).
- [x] Verify the 100/95/110 fixture, trailing behavior, missing indicators, XD context and dividend explicit stops using focused unit tests. 2026-10-03: `ExitPolicyTest` (100/95/110 plan, stop without indicators), `testTrailingStopProtectsTradeThatReachedOneR` (given back, not armed), new `testTrailingStopExitsOnPullbackWhileStillInProfit`, XD tests (`testEarlyBreakdownSuppressedOnExDividendDate`, `testIsNearExDividendDate`), `HoldingSignalTest.savedStopStillAlertsOnDividendHolding`.

**Dependencies:** 1. **Scope:** Medium.
**Likely files:** new `domain/ExitPolicy.kt`, `domain/TechnicalAnalysis.kt`, `domain/TradePlan.kt`, new exit policy tests, `domain/TechnicalAnalysisTest.kt`.

## Task 4: Connect every live exit consumer (Keep)

- [x] Advisor, portfolio signals and worker consume the same exit result and plan revision; remove duplicated target/trailing conditions. 2026-10-03: `HoldingSignal.resolve` replaces the three copies in `StockViewModel` (two) and `StockAlertWorker`; the Advisor's dividend alerts use `DividendExitPolicy`.
- [ ] Alerts distinguish target reached, stop breached, technical invalidation and review-only warnings; all consumers use consistent XD/strategy context.
- [ ] Verify identical decisions for identical input snapshots, plus a manual screen/notification check for stop and target events.

**Dependencies:** 2–3. **Scope:** Medium.
**Likely files:** `ui/StockViewModel.kt`, `util/StockAlertWorker.kt`, `ui/DividendAdvisorScreen.kt`, `domain/TechnicalAnalysis.kt`, consumer parity tests.

## Checkpoint B: Exit advice agrees

- [x] Regression fixtures and consumer parity pass; unit tests and debug build pass. 2026-10-03: one `HoldingSignal.resolve` for Advisor, Portfolio and worker, so parity holds by construction; `HoldingSignalTest`, `ExitPolicyTest`; 194 unit tests pass, build passes.
- [x] Changing current price cannot change an accepted target or disable an explicit stop merely because indicators are missing. 2026-10-03: `ExitPolicyEvaluator` reads only saved levels; `stopWorksWithoutIndicators`, `fixedPlanDoesNotTakeProfitEarly`.

## Task 5: Centralize candidate eligibility (Deferred)

- [ ] Introduce Ready/Watch/Blocked results with reasons. Apply common guards across swing, daily movers, speculative and dividend candidates; SELL states cannot qualify as actionable long entries.
- [ ] Replace unsupported gap/earnings labels with Strong daily move; null required evidence does not count as a pass. Preserve strategy-specific predicates explicitly.
- [ ] Verify bearish-market bypass, SELL plus +4% move, POTENTIAL status and missing-data examples with fixed inputs.

**Dependencies:** 3. **Scope:** Medium.
**Likely files:** new `domain/CandidatePolicy.kt`, `ui/StockDna.kt`, `ui/StockViewModel.kt`, new candidate policy tests, `ui/StockDnaTest.kt`.

## Task 6: Supply weekly trend and freshness evidence (Keep freshness; weekly-trend parts deferred)

- [ ] Carry per-symbol observation timestamps and completed-bar provenance; calculate completed-calendar-week trend from dates rather than five-row chunks and persist/pass it to the policy.
- [ ] Foreground and worker use the same evidence rules, including stale/missing data states and last trading session handling.
- [ ] Verify weekly bearish downgrades, incomplete-week exclusion, old quote with recent fetch time, weekend freshness and worker/foreground parity.

**Dependencies:** 5. **Scope:** Medium; split data plumbing from consumer wiring if it grows beyond one session.
**Likely files:** `data/SetScraper.kt`, `data/RoomModels.kt`, `ui/StockViewModel.kt`, `util/StockAlertWorker.kt`, evidence tests.

## Checkpoint C: Entries use consistent evidence

- [ ] All actionable lists honor common guards; unit tests and debug build pass.
- [ ] Screen shows a concrete reason when a candidate is Watch or Blocked.

## Task 7: Make target provenance and net reward/risk explicit (Deferred)

- [ ] Replace automatic favorable 2R targets with a target source: user-entered, documented historical level, or hypothetical. Missing supported target cannot automatically qualify a setup.
- [ ] Compute reward/risk after quantity-aware fees and disclosed fill assumptions; both stock-card implementations show the same plan and source.
- [ ] Verify 2R is not manufactured by moving the target, net ratio can fall below gross ratio, and missing evidence produces Watch/Target unavailable.

**Dependencies:** 2, 5–6. **Scope:** Medium.
**Likely files:** new `domain/TradePlanBuilder.kt`, `domain/TechnicalAnalysis.kt`, `ui/DividendAdvisorScreen.kt`, `ui/StockComponents.kt`, plan builder tests.

## Task 8: Implement proposal risk validation (Keep; `TradeRiskPolicy.evaluate`/`largestFit` cover part, not re-verified)

- [x] Add one validator for post-fee cash reserve, per-trade risk, combined existing/new ticker exposure, sector exposure, valid prices and board lots; use current configured budgets. Audit 2026-10-03: `TradeRiskPolicy.evaluate` covers reserve after fees, risk with buy and sell fees, combined stock and sector exposure at cost, valid prices and board lots, current settings.
- [x] Missing exposure/sector evidence returns an explicit unverified result. Separate proposal validation from recording an already executed fill. Audit 2026-10-03: unknown sector returns "Sector exposure is unknown"; executed fills bypass proposal checks via the dialog checkbox.
- [x] Verify exact boundaries, one-lot excess, existing holdings, minimum-fee settings and stale/changed account inputs with unit tests. 2026-10-03: `exactlyAtPositionLimitPasses`, `largestFitMatchesALinearScan` (next lot fails), `existingHoldingsShrinkTheQuantity`, new `minimumCommissionWithoutAtsCanBreakASmallBudget`; changed account state at confirmation: device test `secondProposalIsRecheckedAgainstTheHoldingTheFirstCreated`.

**Dependencies:** 1, 7. **Scope:** Medium.
**Likely files:** new `domain/TradeRiskPolicy.kt`, `domain/TradePlan.kt`, `domain/TechnicalAnalysis.kt`, new risk policy tests.

## Task 9: Enforce risk at confirmation while preserving the ledger (Keep)

- [x] Proposal preview and confirmation share validation; revalidate current holdings/settings at the persistence boundary and show specific failure reasons. 2026-10-03: the Buy dialog and `StockRepository.executeBuy` both use `TradeRiskPolicy.evaluate`; `executeBuy` re-reads cash, holdings (cost basis), sector and the settings passed from `addToWatchlist` inside one transaction and throws the specific reasons, shown in the snackbar.
- [x] Recording/importing a broker fill remains possible with truthful amounts and its rule breaches recorded; remove generic overrides that turn an invalid proposal into Ready. 2026-10-03: `recordExecutedFill` records the actual price, quantity and fees; breached rules and any cash shortfall go into the BUY_FILL advice event note with source BROKER_RECORD. No override that turns an invalid proposal into Ready remains (the old AI-accept override is gone).
- [x] Verify over-limit proposals are blocked, changed account state is rechecked, and actual-fill recording/reconciliation remains accurate. 2026-10-03: device tests `overLimitProposalIsBlockedAndLeavesLedgerUnchanged`, `secondProposalIsRecheckedAgainstTheHoldingTheFirstCreated`, `executedFillIsRecordedTruthfullyWithItsBreaches`; 12/12 instrumentation tests pass via `am instrument`.

**Dependencies:** 2, 8. **Scope:** Medium.
**Likely files:** `ui/PortfolioScreen.kt`, `ui/StockViewModel.kt`, `data/StockRepository.kt`, new proposal service if needed, confirmation tests.

## Checkpoint D: Trade preparation is internally consistent

- [ ] Saved plan, card levels, fees, accepted quantity and confirmation result agree; unit tests and debug build pass.
- [x] The app accurately records an executed trade even if it violates a proposed-trade rule. 2026-10-03: `executedFillIsRecordedTruthfullyWithItsBreaches` (exact cash deduction, quantity, breach note).

## Task 10: Constrain AI to validated plans (Deferred)

- [ ] Send the shared eligible list and typed plan IDs/levels/evidence to Gemini; replace duplicate selection and free-form executable recommendations with validated plan references.
- [ ] Reject unknown/stale plans and invalid numbers/allocations. Display qualitative model assessment and support No valid setup without inventing a pick.
- [ ] Use stub responses to verify wrong symbol, bad stop, excessive size, stale revision, empty results and valid-plan rendering. No live model call is required for acceptance tests.

**Dependencies:** 5–9. **Scope:** Medium.
**Likely files:** `domain/GeminiClient.kt`, new `domain/AiRecommendationValidator.kt`, `ui/DividendAdvisorScreen.kt`, `domain/GeminiClientTest.kt`, validator tests.

## Task 11: Repair backtest execution and accounting (Deferred)

- [ ] Reuse plan/exit policy; apply the same disclosed next-bar fill timing for entry and signal exit, quantity-aware costs, and explicit stop/slippage assumptions.
- [ ] Mark equity to market every bar including open positions; expose realized/unrealized results, daily drawdown and historical data coverage. Missing historical fundamentals/flow stays technical-only replay.
- [ ] Verify open loss, recovered deep drawdown, next-bar fills, cost-induced loss and unavailable-history fixtures; verify summary labels match the computed measures.

**Dependencies:** 3–9. **Scope:** Medium; split fill/accounting and UI coverage if needed.
**Likely files:** `domain/BacktestEngine.kt`, `ui/BacktestScreen.kt`, `data/SetScraper.kt` (also defines historical bars), `domain/BacktestEngineTest.kt`.

## Checkpoint E: AI and replay respect actual rules

- [ ] AI cannot reintroduce rejected candidates; backtest fixture outcomes match hand-calculated accounting.
- [ ] Unit tests and debug build pass; unsupported full-advisor performance claims are absent from result screens.

## Task 12: Capture advice and subsequent outcomes (Deferred)

- [ ] Persist a local versioned advice snapshot with input timestamps, eligibility reasons, accepted plan, and rule version; link actual fills, revisions and overrides without storing API credentials.
- [ ] Provide exportable evidence for planned-versus-realized reward/risk, fees and exit reasons; replay complete recorded snapshots through the same policies.
- [ ] Verify one complete advise → accept → fill → revise → exit chain survives restart/export, and missing historical advice remains explicitly unknown.

**Dependencies:** 2, 4, 9–11. **Scope:** Medium; persistence and export wiring may be separate commits.
**Likely files:** `data/RoomModels.kt`, new `data/AdviceJournalRepository.kt`, `ui/StockViewModel.kt`, `data/StockRepository.kt`, journal/replay tests.

## Task 13: Correct claims and complete release review (Keep)

- [ ] Update README, indicator/alert docs and affected labels to match the final strategy, qualitative AI assessment, stop-alert behavior and replay limitations; remove unsupported win-rate/expectancy claims.
- [ ] Review a deterministic old/new decision comparison and a forward paper-evaluation protocol with strategy version, sample size, period, costs, data coverage and a holdout selected before tuning.
- [ ] Run final unit/build/lint gates, migration checks and device walkthrough. Record actual results and remaining limitations here before user release review.

**Dependencies:** 1–12. **Scope:** Medium.
**Likely files:** `README.md`, `docs/INDICATORS.md`, `docs/ALERT_FLOWS.md`, affected string resources, this checklist.

## Checkpoint F: Ready for release review

- [x] `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passes.
- [x] Migration/instrumentation checks pass on an available device; failures or environment blockers are documented. Audit 2026-10-03: 9/9 on the connected phone, run with `am instrument` (not connectedAndroidTest, which uninstalls the app and its data).
- [ ] User flow checks cover legacy holdings, saved target, exit notification, blocked proposal, executed-fill recording, AI empty/invalid result and backtest open loss.
- [ ] The user reviews the completed behavior and evidence before release. Profitability is evaluated separately from software acceptance.

---

# Advisor Quality Fixes — "Why user still doesn't win" (2026-09-26)

## Root Causes Identified & Fixes

- [x] **Fix 1** NEUTRAL market gating: `isMarketBearish = !isBullish` lets NEUTRAL pass — change to explicit `== BEARISH`
- [x] **Fix 2** `isMom()` hard `return false` for null RS silently blocks valid candidates — make null-tolerant
- [x] **Fix 3** Snapshot race condition silently discards AI result — show snackbar when stale discard happens
- [x] **Fix 4** `existingStock` uses market value not cost basis → concentration limits breached silently — use cost
- [x] **Fix 5** Duplicate sell alerts: FIXED_TARGET DIVIDEND stocks appear in both lists — deduplicate by symbol
- [x] **Fix 6** AI "no live news" warning is tiny `labelSmall` text — replace with amber warning card
- [x] **Fix 7** No stale-data banner before AI analysis — show warning chip when lastSync > 12h

## Verification
- [x] `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passes

---

# Plan: Profitability Fixes (Legacy Exit Path)

## Overview
Legacy (no saved plan) exits cap winners at +5% while stops reach -8%, giving reward:risk below 1:1. Also `hasProfit` in the overbought block uses a flat 500 baht threshold, so large positions are sold on RSI > 65 at ~0.1% gain.

## Todo Checklist
- [x] **1. Fix `hasProfit` threshold** in `TechnicalAnalysis.getDetailedSignal` section 2: overbought / MFI / upper-band exits only once position reaches 1R (profit >= |stop distance|), never on a flat 500 baht.
- [x] **2. R-multiple take-profit**: replace flat +5% / 500 baht target with `TAKE_PROFIT_R_MULTIPLE (2.0) x |dynamicStopLoss|` so target >= 2x stop distance.
- [x] **3. Trailing stop activation at +1R** instead of fixed +3%.
- [x] **4. Tests**: add regression tests for large-position overbought churn and R-based target; update existing tests; run `./gradlew testDebugUnitTest`.
- [x] **5. Rerun BacktestEngine tests** to confirm no regression.

## Later (not in this change)
- Universe-wide backtest vs SET TRI with slippage; walk-forward split.
- Unify live/backtest signal inputs.
- Score AI rankings against outcomes.
- Core-satellite DCA mode.

---

# Plan: Market-Wide Backtest vs SET Index

## Finding
`SetScraper.fetchHistoricalPrices` fetches 1 year; `BacktestEngine` warms up 210 days, so the in-app backtest evaluates only ~35 trading days per stock.

## Approach
Offline JVM harness reusing the real `TechnicalAnalysis` / `BacktestEngine` code (no Android needed), fed by cached daily CSVs. Not shipped in the APK.

## Todo Checklist
- [x] **1. Data fetch script** (`tools/backtest/fetch_history.py`): download 2014-2025 daily OHLCV from Yahoo for SET100 constituents + TDEX (Yahoo serves no daily history for `^SET.BK`); cache CSVs under `tools/backtest/data/` (gitignored). Record which symbols lack full history.
- [x] **2. Portfolio simulator** (`domain/PortfolioBacktest.kt`, pure Kotlin): shared capital, fixed-fraction sizing (1% risk), 15% single-stock cap, 100-share lots, max N positions, next-close fills, fees via existing fee engine, configurable slippage (default 0.15%/side).
- [x] **3. Benchmark**: buy-and-hold SET index over the same window (price index; note dividends excluded, so add ~3%/yr estimate as TRI caveat).
- [x] **4. Walk-forward report**: in-sample 2015-2020, out-of-sample 2021-2025. Metrics: CAGR, max drawdown, expectancy in R, win rate, trades/yr, exposure %, CAGR gap vs benchmark.
- [x] **5. Harness entry**: JUnit test tagged/ignored by default (`-Pbacktest`) that reads CSVs and prints the report to `tools/backtest/report.md`.
- [x] **6. Fix in-app window**: fetch 3 years in `fetchHistoricalPrices` for the Backtest screen (separate call so live refresh stays light).

## Known limits
- Survivorship bias: Yahoo lacks most delisted SET tickers; results will be optimistic. Stated in the report.
- NVDR flow, relative strength, weekly trend and XD dates are not in the replay (live/backtest mismatch remains until unified).

## Results (tools/backtest/report.md)
- Current rules, 2015-2025: CAGR -10.1% vs TDEX +2.5%; MDD 72.5%; expectancy -0.09R; ~190 trades/yr.
- Pre-change rules (157b217): CAGR -16.4%; ~272 trades/yr. The R-multiple exit change helped but did not create an edge.
- Diagnostic (not shipped): disabling Early Breakdown exit gives -7.9% CAGR; entries themselves show no edge.

## Follow-ups
- [x] **Live bug** (TDEX fallback added): `SetScraper.fetchSetIndexHistory` index path (`^SET.BK`) returns no timestamps, so market regime, beta and regime cash buffer have no index data. Switch to TDEX proxy or SET API.
- [x] Rethink entries: tested in harness (pluggable `BacktestRule`).
  - App entries + SMA50/stop exit: -12.8% CAGR (oversold entries sit below SMA50, so they exit almost at once: entry/exit rules contradict).
  - 52w breakout + SMA50/stop exit: +11.1% CAGR full period, but +23.8% 2015-2020 vs -1.0% 2021-2025, and 99% of P/L from DELTA, KTC, JMART (survivorship). No robust edge.
- [x] Rank simultaneous BUYs (`BacktestRule.rank`).
- [ ] Add point-in-time SET50/SET100 membership to reduce survivorship bias.
- [ ] Product decision: reposition signals as education/watchlist context; make index DCA core + risk/discipline tooling the primary flow.

---

# Plan: Reposition to Core-Satellite + Discipline

## Why
Backtest (tools/backtest/report.md): app signals -10.1% CAGR vs TDEX +2.5% (2015-2025); no tested rule shows a robust edge. The app's defensible value is risk control, discipline, Thai fee/tax tooling and the journal.

## Target experience
"Build wealth with an index core; trade a small satellite with strict rules; see honestly whether the satellite beats the core."

## Todo Checklist

### Phase 1: Honest signal labelling (small, ship first)
- [x] Signal card: "Technical Setup" / "On Watch" / "Exit Rule"; `UntestedEdgeNotice` card under BUY/POTENTIAL (static, no link).
- [x] Entry (BUY) notifications from `StockAlertWorker.kt:185` default OFF (Settings toggle; also gates the 15:30 entry-window prompt); risk exits (stop, trailing, saved-plan stop) stay ON.
- [x] Advisor tab header card (Swing playbook only): results summary vs TDEX (surface card per lessons.md #5, not small text); "Accept AI Plan" demoted to secondary (outlined) action.
- [x] Signal copy: removed "High probability value dip", "strong sign of institutional buying", "reversal confirmed".
- [x] Watchlist/advisor badges, sort bubble and entry notification title now use `IndicatorSignal.badgeLabel` (SETUP / WATCH / EXIT). Zone chip fixed: descriptive trend states (Uptrend / Downtrend / Overextended / Near Support / Range) replace Buying/Selling/Potential Zone.

### Phase 2: Core holdings and DCA
- [x] Domain: `isCore(symbol) = symbol == "TDEX"`; all other holdings are SATELLITE. No schema change.
- [x] Settings: target core % (default 80), monthly DCA amount, DCA day.
- [x] Monthly DCA reminder notification with suggested lots (100-share rounding, fees shown) to restore target core %.
- [x] Portfolio screen: Core vs Satellite allocation card with drift from target.

- [x] Verified on emulator: card (no-core state), Settings section, forced worker run produced "Buy 900 TDEX at about ฿10.35 (~฿9,331 incl. fees)".
- [x] Fixed (moved to `v7/finance/spark`, 20-symbol chunks): Yahoo `v7/finance/quote` (`SetScraper.fetchBatchQuotes`) returned HTTP 401; `StockViewModel` refreshes mark every symbol failed and fall back to the slower per-stock path. Replace with chart endpoint or SET API.

### Phase 3: Satellite scorecard
- [x] Stats: satellite money-weighted return vs "same cash flows into TDEX" (shadow portfolio from cash transactions + trades).
- [x] Rolling 12-month verdict card: "Satellite beat / trailed the core by X%"; if trailing 2 consecutive quarters, suggest reducing satellite %.
- [x] Satellite budget guard: Buy dialog warns when a satellite buy pushes satellite above its cap (reuse spendable-cash guard pattern).

- Notes: ledger = BUY_FILL / SELL_FILL / UNDO_SELL journal (sell fee estimated, SELL_FILL stores buy+sell fees combined); symbols whose journal does not reconcile to the current holding are excluded and listed. Comparisons start 30 days after the first fill; trailing windows need full-window history. TDEX uses dividend-adjusted (gross) closes vs net satellite dividends: slight bias toward core.
- Verified on emulator: exclusion state (HMPRO, MBK pre-journal holdings), Buy dialog cap warning. Populated windows covered by unit tests only.

### Phase 4: Evidence gate for signals
- [x] `docs/ADVISOR_EVALUATION.md`: any signal change must beat TDEX in BOTH sub-periods and stay positive after removing its top 3 symbols in `MarketBacktestReport`. Implemented as `EvidenceGate` (also: >= 50% of P/L without top 3, >= 100 trades, positive expectancy); report prints PASS/FAIL per rule. All current rules FAIL.
- [ ] Blocked (no verified data source): add point-in-time index membership to the harness when data source is available (reduces survivorship bias).

### Phase 5: Docs
- [x] README concept rewrite (core-satellite + discipline), CHANGELOG, version bump to 4.0.0 (29).

## Decisions (2026-10-01)
- Core instrument: TDEX only. CORE bucket is derived (symbol == TDEX), so no user tagging; Phase 2 `bucket` column not needed.
- AI Advisor: demote to context. Show backtest result up front; "Accept AI Plan" becomes a secondary action.
- Default split: 80 core / 20 satellite (configurable).

# Core buys exempt from satellite rules, 2026-10-02

- [x] `TradeRiskPolicy.evaluateCoreBuy`: core (TDEX) buys need only a valid price, a 100-share lot and enough cash. No stop, target, single-stock, sector or regime-buffer gate.
- [x] Buy dialog: core path uses it, hides stop/target and position-size sections, and the cash card checks cash only.
- [x] Alert worker: no technical sell reminders on the core; an explicit saved stop still alerts.
- [x] Unit tests for the core policy; `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

# Exit and short-swing repair, 2026-10-02

- [x] Remove "Early Breakdown Warning" from `getDetailedSignal` and the matching early invalidation in `ExitPolicyEvaluator`; rerun `MarketBacktestReport` and keep the change only if App signals improve.
- [x] Short-swing plan: reachable target (recent swing high, capped) plus a 20-session time exit. Write the rule down before the first run; measure with `AdvisorEventStudy` and report every variant tried.
- [x] (Not wired: no variant passed) Wire the plan into the advisor only if it beats the current plan in both sub-periods; otherwise record the result and leave the advisor as context.
- [x] Unit tests, build; changelog; commit. (No app behavior change from the swing study, so no device check.)

## Short-swing plan variants (fixed before the first run, 2026-10-02)

Candidates: advisor replay = app BUY + completed weekly trend up + RS(63d vs TDEX) > 0. Entry next close, stop = suggested ATR stop, fees + 0.15% slippage per side, first touch, stop wins a bar that spans both.
- P0 current: target = 52-week high, R:R >= 2, measured at a 20-session time exit (for comparison).
- P1: target = entry + 2R, time exit after 20 sessions.
- P2: target = 20-session swing high, taken only if >= entry + 1R, time exit after 20 sessions.
- P3: P1 without the BUY signal (weekly trend up + RS > 0 + close > SMA 50).
Wire into the advisor only if a variant has positive mean net return AND beats TDEX over the same holding window in BOTH 2015-2020 and 2021-2025. Report every variant.

# Device UX review fixes, 2026-10-02

- [x] Band proximity: "near lower/upper band" by position in the band (%B), not within 5% of the edge; rerun backtest and report the change.
- [x] Watchlist "Target unavailable" no longer clipped to "Target".
- [x] Advisor "Check Exits" uses the saved stop; card shows a passed stop and the current loss.
- [x] Confirm before History clear, watchlist remove and Undo Sell.
- [x] AI Prompt FAB does not cover content; Gemini button disabled with 0 plans.
- [x] Tests, build, device check, changelog, commit.

# Real-life practicality, 2026-10-02

## 1. Core setup and one-tap DCA
- [x] Portfolio "Core vs Satellite" card: when no monthly DCA is set, show a setup action (amount + day) with the first purchase in whole TDEX lots, fees included.
- [x] "Buy TDEX" action on the card opens the Buy dialog prefilled with TDEX, the suggested lots and the live price.
- [x] Monthly DCA notification opens the app straight into that prefilled Buy dialog.

## 2. Stop decision step
- [x] When price is through the saved stop, the holding card asks for one decision: Sell (existing flow), Move stop (new stop below price + required reason), Hold anyway (required reason).
- [x] Each decision is journaled (advice event + note) and "Hold anyway" silences repeat sell reminders for that stop level until the stop changes.

## Later (not started)
- [x] Auto-filled pending dividends on XD; "start tracking from today's cost" for the scorecard.
- [x] Hide Speculative Watch, AI buy ranking, daily movers and Dividend Stars until they pass the evidence gate.
- [x] Cloud backup: already provided by Android Auto Backup (verified active on device; data < 1 MB); Settings now explains it.
- [x] Remote holiday/SET50 lists: `config/market_lists.json`, fetched daily with validation and built-in fallback.
- [-] Broker statement import: skipped 2026-10-02 (no sample statement available). Screenshot import with Gemini remains the quick-entry path.
- [x] ThaiESG/RMF tracker with December reminders.
- [x] Rebalance nudge (monthly, core more than 5 points under target).
- [x] Single stop model: volatility-based (user decision, 2026-10-02); fixed-% trailing setting removed.

# 1-4 week momentum rules, 2026-10-02 (fixed before the first run)

Common: SET50 frozen universe, 2015-2025, portfolio replay (1% risk, 15% stock cap, 10 positions, board lots, InnovestX fees, 0.15% slippage per side, next-close fills); evidence gate as in docs/ADVISOR_EVALUATION.md.
- M1: Buy when close is the highest close of the last 20 sessions, close > SMA50 and TDEX close > TDEX SMA50. Sell on close at the lowest close of the last 10 sessions, at a 2x ATR stop from entry, or after 20 sessions.
- M2: As M1, but sell on the 20-session low and after 40 sessions.
- M3: As M1 without the TDEX market condition.
Rank simultaneous buys by 126-day momentum. Report every variant.
- [x] Result: none passes (all trail TDEX in 2021-2025 and depend on their top three symbols). See tools/backtest/momentum_report.md.

# Short-term rules from the literature, 2026-10-02 (fixed before the first run)

Basis: short-term reversal documented in Asian markets incl. Thailand (Chang, McLeavey & Rhee style weekly contrarian studies; Hameed & Kusnadi); turn-of-month effect (Lakonishok & Smidt; Ariel). Same universe, replay, costs and evidence gate as the momentum test.
- R1 Weekly reversal: on the last session of the week, buy SET50 stocks whose 5-session return is -5% or worse (worst first, max 10); sell after 5 sessions or at the 2x ATR stop.
- R2 Dip in an uptrend: buy when close > SMA200 and RSI(2) < 10; sell when close > SMA5, after 10 sessions, or at the 2x ATR stop.
- R3 As R2 with RSI(2) < 5 and close > SMA50 as well.
- R4 Turn of month on TDEX: hold TDEX from the close of the 4th-last session of each month to the close of the 3rd session of the next; cash otherwise. Compare with TDEX buy-and-hold after fees.
Report every variant.
- [x] Result: none passes; see tools/backtest/shortterm_report.md.

# 6-month momentum portfolio, 2026-10-02 (fixed before the first run)

Basis: Jegadeesh & Titman (1993) 3-12 month momentum; standard "6-1" ranking skips the latest month.
Common: frozen SET50 universe, 2015-2025, ฿1M, rank at each month's last session, trade at the next session's close, 0.15% slippage per side, InnovestX fees (ATS), 100-share lots, equal target weight 10% per stock (within the 15% cap). Names that stay in the top list are kept without resizing; names that drop out are sold; new names are bought with available cash. Dividend-adjusted prices.
- MOM1: rank by return from 126 to 21 sessions ago (6-1), hold the top 10.
- MOM2: as MOM1, but only stocks with a positive 6-1 return; empty slots stay in cash.
- MOM3: rank by the full 126-session return (no skipped month), hold the top 10.
Evidence gate as before, with average net return per closed position standing in for expectancy R (no stop is used). Report every variant.
- [x] Result: none passes (all trail TDEX in 2021-2025; profit depends on DELTA, JMART and one more). See tools/backtest/momentum_portfolio_report.md.

# 6-month momentum + app signal timing, 2026-10-02 (fixed before the first run)

Same simulator, universe, costs and gate as the momentum portfolio. Target list = top 10 SET50 by 126-session return, re-ranked at each month's last session; names leaving the list are sold at the next close.
- S0: MOM3 baseline (buy new names at the next close after the month-end ranking), to confirm the simulator matches.
- S1: a listed name not yet held is bought at the next close only on a day the app signal is BUY or POTENTIAL; otherwise it waits (cash).
- S2: as S1, BUY only.
Report every variant.
- [x] Result: S0 matches MOM3; signal timing lowers returns (S1 8.55%, S2 3.52%/yr) and none passes. See tools/backtest/momentum_signal_report.md.

# Low volatility and index trend timing, 2026-10-02 (fixed before the first run)

Basis: low-risk anomaly on SET (2004-2015 study); Faber (2007) 10-month / 200-day trend timing.
Portfolio variants use the momentum-portfolio simulator unchanged (monthly re-rank, top 10 at 10% target, kept names not resized, next-close trades, fees + 0.15% slippage, 100-share lots).
- LV1: hold the 10 SET50 stocks with the lowest standard deviation of daily returns over the last 63 sessions.
- LV2: as LV1 over 252 sessions.
- LV3: the 10 with the lowest beta to TDEX over 252 sessions.
- T1: hold TDEX while its close is above its 200-session average (decide at the close, trade at the next close), else cash at 0%.
- T2: decide at each month's last session: hold TDEX if that close is above the average of the last 10 month-end closes, else cash; trade at the next close.
Gate as before for LV; for T1/T2 compare CAGR and max drawdown with TDEX buy-and-hold in both sub-periods. Report every variant.
- [x] Result: no LV variant passes (LV3 low beta closest: 5.22%/yr, fails 2021-2025 by 0.38 points and on concentration). T2 cuts max drawdown 36.5% -> 23.5% but lowers return (1.68% vs 2.52%); T1 whipsaws. See tools/backtest/lowvol_trend_report.md.

# Fundamental screens on history (thaifin), 2026-10-02 (fixed before the first run)

Data: thaifin (Finnomena public API) quarterly financials for the frozen universe, fetched by tools/backtest/fetch_fundamentals.py into tools/backtest/fundamentals/ (git-ignored, like prices).
Point-in-time: a quarter's figures are usable from 50 days after a Q1-Q3 quarter end and 90 days after a Q4 (year) end. ROE = trailing four quarters' net profit / latest equity x 100; net margin = trailing net profit / trailing revenue x 100; debt-to-equity and dividend yield as reported for that quarter. Financial sector = Banking, Finance and Securities, Insurance.
Monthly ranked portfolio, top 10 at 10% target (RuleStudy.rankedPortfolio; empty slots stay in cash), costs and gate as before.
- F1 Quality, old rule: ROE > 15, margin > 10, D/E < 1.5 (all sectors); rank by ROE.
- F2 Quality, SET rule: ROE > 10, margin > 10, D/E < 1.5 except financials; rank by ROE.
- F3 Dividend Stars: dividend yield >= 5% and F2 quality; rank by yield.
- F4 F2 quality ranked by lowest 252-day beta to TDEX.
- F5 Highest dividend yield, no quality filter; rank by yield.
Report every variant.
- [x] Result: F5 passes the gate (7.80%/yr vs TDEX 2.52%; 2015-2020 8.36% vs 1.75%; 2021-2025 5.50% vs 3.19%; 58% of P/L outside the top three). F1-F4 fail. See tools/backtest/fundamental_screens_report.md. Not yet confirmed: robustness and holdout below.

# High-dividend-yield robustness and holdout, 2026-10-02 (fixed before the first run)

F5 passed after 19 tested rules, so it must survive these before it can go in the app. Same simulator, costs and point-in-time rules as F5 unless stated.
- D1: top 5 by yield (20% target each).
- D2: top 15 by yield (1/15 target each).
- D3: rebalance only at quarter ends (March, June, September, December).
- D4: 120-day publication lag for every quarter.
- H: holdout 2011-04-01 to 2014-12-31 (prices fetched from 2008 into tools/backtest/data_holdout/, git-ignored; thaifin starts 2010Q1). Pass = CAGR above TDEX buy-and-hold over the holdout.
F5 counts as confirmed only if D1-D4 each beat TDEX in both sub-periods and H beats TDEX. Report every result.
- [x] Result: all checks pass (holdout 24.95% vs 9.68%). Real-world check with the 1DIV ETF (no survivorship): 3.86% vs TDEX 2.55% over 2015-2025, ahead in 2021-2025, behind in 2015-2020 and 2012-2014. Backtest magnitude is inflated by survivorship. See tools/backtest/dividend_yield_robustness.md and docs/ADVISOR_EVALUATION.md.

# High-dividend list and core mix, 2026-10-02

- [x] Advisor (Dividend tab, untested lists): top 10 current SET50 by dividend yield, ranked once a day from SET quotes, with the backtest and 1DIV real-world results beside it.
- [x] Core as a mix: TDEX plus an optional second SET-listed ETF (e.g. 1DIV) with a split %. isCore covers both; the core card, DCA preview and reminder split the monthly amount by weight; buy buttons per fund. TDEX stays the scorecard benchmark.
- [x] Tests, build, device check, changelog, commit.

# Advisor empty-screen diagnosis, 2026-10-02

- [x] Trace list visibility, candidate filters, data requirements and watchlist scope.
- [x] Compare thresholds with existing SET research and run focused eligibility tests.
- [x] Report verified causes and recommended changes; distinguish code findings from device state.

Scope: diagnosis first; no trading-rule changes without evidence. Repository documentation lives in docs/ (root UI.md and USER_GUIDE.md are absent).

## Diagnosis results

- Connected app and source both report 4.0.0 (29); version equality alone does not prove identical code. Read-only device snapshot: `show_untested_lists=false`.
- 101 tracked stocks; basic filters pass 89 (10 near the 52-week low, 2 below turnover threshold); quality leaves 30; bullish weekly trend leaves 14. Those 14 have cached signals POTENTIAL=11, NEUTRAL=3, BUY=0. The UI recomputes signals and applies holding-specific overrides, so these are cached-input funnel counts, not a captured live UI result.
- 36 stocks yield >=5%; 32 survive basic filters and 11 survive quality, before the dividend SELL exclusion. Dividend candidates do not require BUY or bullish weekly trend. The default visibility setting hides this list too.
- Data freshness is not the dominant blocker in this snapshot: stock and benchmark dates match for every row (100 dated Oct 2; one Sep 25, still within the current seven-day allowance).
- Current quality threshold is ROE >10%, not the stale comment's >15%; nonfinancial D/E <1.5, margin >10% and growth >10% when present. Candidate scope is tracked stocks, not all SET listings; separate ranked SET50 lists have their own universe.
- Recommendation: expose research/watch candidates and first-failure counts clearly, preserve strict actionable-plan checks, and evaluate dividend rules separately from swing timing. Existing SET studies do not demonstrate that merely relaxing BUY requirements improves returns.
- Verification: 15 StockDnaTest tests passed; git diff against main inspected; git diff --check passed. No application rules/settings were changed. Pre-existing Gradle artifacts and untracked scripts were left alone.

# Advisor filter explorer, 2026-10-02

- [x] Add reusable filter stages with cumulative counts and independent Swing/Dividend combinations.
- [x] Add always-visible selectable badges, reset/clear actions and exploratory stock results; preserve existing entry/AI policies.
- [x] Test combinations and default-policy parity, build and lint, verify on device, and inspect diff.

Design: existing Compose components are sufficient; no external Sleek project is needed. Enabled stages combine with AND. Disabled badges preview their count if added at that point in the funnel. Results are research matches, not accepted trade plans. Existing untested ranked lists remain opt-in.

## Filter explorer verification

- Added AdvisorFilters (research-only predicates and cumulative counts), AdvisorFilterExplorer (saveable independent tab selections, toggle badges, reset/clear, paged matching stocks), and six tests including exhaustive default-policy comparisons across signals, weekly trends, quality and market conditions.
- Final `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passed: 169 tests reported, 9 skipped, zero failures/errors; lint has 126 warnings and zero errors, none pointing to the new explorer files.
- Installed the final debug APK on the connected phone without clearing data. Verified live Swing defaults yield zero; disabling Confirmed BUY yields 11. Dividend defaults yield 11; disabling Quality yields 31; Swing retains its independent selection. Clear filters displayed all 101 tracked stocks. Visually inspected `/tmp/tradingmate-advisor-filters.png` at the phone's current font size.
- Existing optional lists/presets and all trade-plan/AI/exit rules remain intact. Git diff against main inspected and whitespace check passed. No commit created.


# Advisor sizing fix and High Yield rebalance, 2026-10-03

Plan: [Advisor sizing fix and High Yield rebalance](plan.md#advisor-sizing-fix-and-high-yield-rebalance-plan).
Status: planned; application implementation has not started.

## Planning

- [x] Draft the risk-adaptive Advisor plan (risk fit, presets, open risk).
- [x] Review value: verified the sizing drop at `DividendAdvisorScreen.kt:1171-1194`, the hardcoded 15% at `:1276` and `:1402`, and unenforced `maxOpenExposure`. Presets and open-risk enforcement cut (low value while swing BUY=0); core drift and pending dividends already exist.
- [x] Narrow scope to the swing sizing fix plus a High Yield rebalance plan for the tested F5 rule.

## Decisions (user)

- [x] Default satellite budget (recommended: current satellite market value).
- [x] Rebalance cadence prompt (recommended: monthly, as tested).
- [x] List sells of satellite names outside the top 10 (recommended: yes, labelled).

## Phase 0: Clean start

- [x] Commit the finished filter explorer (`AdvisorFilters.kt`, `AdvisorFilterExplorer.kt`, `AdvisorFiltersTest.kt`, `DividendAdvisorScreen.kt`, changelog): 741de68.
- [x] Remove or ignore stray scripts (`append_changelog.py`, `append_readme.py`, `patch_ai_accept.py`, `update_tasks.py`) and tracked `.gradle/` churn: scripts moved out of the repo; `.gradle/` untracked in 9cc27c8.

## Part 1: Swing sizing fix

- [x] Pure helper beside `TradeRiskPolicy`: largest whole-lot quantity passing `evaluate`, plus the blocking limit (or minimum-lot block); explicit unavailable result for invalid input.
- [x] Advisor uses it instead of `minOf(sized, affordable)`; record the blocking reason for candidates that still do not fit.
- [x] AI prompt reads the actual stock allocation setting; describe `maxOpenExposure` as advisory.
- [x] Unit fixtures from the plan (Part 1); separate commit.
- [x] Part 1 result: `TradeRiskPolicy.largestFit` (binary search over lots, checked against a linear scan). Example: 10.00 entry, 9.00 stop, 1% of 100k: the old helper gave 1,000 shares, which fails the budget once fees count, so the candidate was dropped; now 900. 175 unit tests (10 skipped), 0 failures; build passes; lint 0 errors, 126 warnings (unchanged). Device: Swing preview renders; 0 candidates reach sizing in today's bear regime, so the blocked line was not exercised live. Untested-lists setting was turned on for the check and restored off.

## Part 2: High Yield rebalance plan

- [x] Pure function: budget, holdings, ranking, prices, fees -> per-symbol held/buy/sell lots, fees, leftover cash, following the tested rule (sell names that left; keep held names without resizing; new names at min(budget / 10, cash) in whole lots); core funds excluded; cap warnings without changing weights.
- [x] Dividend tab UI under the High Yield list: budget field, order table, totals, evidence note; buy buttons prefill the existing buy dialog. Nothing executed automatically.
- [x] Unit fixtures from the plan (Part 2); separate commit.
- [x] Part 2 result: `HighYieldRebalance.plan` plus `HighYieldRebalanceCard`; `StockScreen` routes "Record buy" to Portfolio's Buy dialog through the DCA prefill path. Decisions taken as recommended: budget defaults to the current satellite value; cadence is stated as monthly in the card text (no notification added); sells of names outside the list are listed and labelled. 181 unit tests (10 skipped), 0 failures; build passes; lint 0 errors, 126 warnings. Device: with the user's holdings (satellite about 37k baht) the plan sold MBK, TRUE, CPALL and JMT, bought LH, TFG, CRC and CPF, left six names below one lot and showed the 345,580-baht full-list budget hint; "Record buy" opened the dialog prefilled (CPF, 21.1, 100) and was cancelled without saving. Untested-lists setting turned on for the check and restored off.
- [x] Follow-ups (Dividend purpose and executed-fill prefill; monthly reminder): done in the section below.

## Verification

- [x] `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` pass (181 tests, 10 skipped, 0 failures; lint 0 errors, 126 warnings).
- [x] Device: High Yield plan with real holdings, buy dialog prefill and core funds absent verified. Swing preview renders, but 0 candidates reached sizing (bear regime), so live quantities and blocking reasons are covered by unit tests only.
- [x] Diff against main reviewed, `git diff --check` passes, changelog and user docs (SCREEN_FLOWS.md) updated with actual results.

## Deferred

- Presets (Cautious/Balanced/Adventurous) for the four risk settings.
- Open stop-risk aggregation and `maxOpenExposure` enforcement.
- Unifying PortfolioScreen and `PortfolioBacktest` sizing with the new helper (versioned backtest rerun, no forced parity).
- Preferences in backup/restore (`TradingBackup` has none today).

# Averaging down on the high-dividend list, 2026-10-03 (fixed before the first run)

Question: instead of a stop, does buying more of a held name after it falls improve F5? F5 as tested never resizes held names (`RuleStudy.rankedPortfolio`: names that stay are kept without resizing), so this is a new rule, not already covered by F5.
Same simulator, universe, costs (ATS fees, 0.15% slippage per side), point-in-time fundamentals and periods as F5. Decisions only at month ends; "down" = last close at least 15% below the position's average cost per share including fees.
- A0: F5 baseline rerun; must reproduce 7.80%/yr (2015-2025) before any variant counts.
- A1: top up. A held name still in the top 10 and down >= 15% is bought back up to 10% of equity, at most once per position.
- A2: overweight add. Same trigger, buy up to 15% of equity, at most once per position.
- A3: full monthly resize of every held name to 10% (buys losers and trims winners). Also decides Part 2's held-name rule.
- A4: control in the opposite direction. A held name down >= 15% is sold even if still in the top 10 (stop-like exit at month end).
Report for every variant: CAGR 2015-2025, 2015-2020 and 2021-2025, holdout 2011-04-01 to 2014-12-31, max drawdown, trades, turnover, share of P/L from the top three symbols, and the largest single-name weight reached.
A variant counts as better than F5 only if it beats A0 CAGR in both sub-periods and the holdout, with max drawdown no worse than A0 + 5 points. Any pass is weak evidence: the frozen universe excludes delisted stocks, and survivorship flatters averaging down more than any other rule, because the losers that never recovered are missing. A pass therefore needs a follow-up with a delisting-inclusive universe before it reaches the app.
- [x] Add per-position resize/exit hooks to `rankedPortfolio` (`RuleStudy.HeldRule`) without changing A0 output: A0 reproduces 7.80 / 8.36 / 5.50 / 24.95 exactly.
- [x] Run A0-A4; write tools/backtest/averaging_down_report.md and a summary in docs/ADVISOR_EVALUATION.md; report every result, pass or fail.
- [x] Result: A1 (8.31%), A2 (8.71%) and A3 (8.35%) pass the gate against A0 (7.80%) in both sub-periods and the holdout, with drawdowns 2.5-4.6 points deeper; A4 (sell when down, 7.43%) fails. Gains are small and flattered by survivorship. A2 concentrates (one name up to 22%); A3 keeps the largest name near 13%. Unit suite: 170 tests, 0 failures. Not yet done: delisting-inclusive check; nothing changed in the app.

# Rebalance plan follow-ups, 2026-10-03

- [x] Managed holdings: the plan sells and budgets only satellite holdings with the Dividend purpose; other holdings in the top 10 show as Keep ("held as another purpose, not in the budget"); others are left out. No new setting.
- [x] Buy prefill carries purpose and executed-fill: replace the `Triple` prefill with a `BuyPrefill` data class (symbol, price, shares, purpose, executed fill); "Record buy" opens with Dividend and "Record an already executed broker trade" ticked; DCA prefill unchanged.
- [x] Monthly reminder: first trading session of each month, once, when untested lists are on and at least one Dividend-purpose holding exists; opens the Advisor.
- [x] Tests, build, lint, device check (prefill flags; plan with real holdings), docs, changelog, commit.
- [x] Result: 182 unit tests (10 skipped), 0 failures; build passes; lint 0 errors, 127 warnings (+1 `UseKtx` on the new `alertPrefs.edit()` call, matching the surrounding worker code). Device: plan now sells only MBK and JMT (Dividend purpose), TRUE and CPALL left out; default budget 14,180 baht; "Record buy" opened LH 400 @ 3.44 with Dividend and executed fill preset and no stop message; cancelled without saving. The reminder was not triggered on the device (worker timing); it is gated by month key, open market, untested lists and a Dividend-purpose holding. Untested-lists setting restored off.

# Core-first repositioning, 2026-10-03

Plan: [Core-first repositioning](plan.md#core-first-repositioning-plan). Status: approved and implemented.

- [x] Portfolio is the start screen and first bottom-bar item; deep links unchanged.
- [x] Advisor opens on the Dividend tab (now first in the toggle).
- [x] Evidence notice: the existing `UntestedEdgeNotice` was already on the Swing tab and stock detail; added above the Watchlist list. No rule changes.
- [x] SCREEN_FLOWS updated (core-first order, Advisor focus, Watchlist context). README already led with the index core and does not describe navigation order: no change.
- [x] Re-scope the reliability plan: note at its top; Tasks 1-4, 8, 9, 13 Keep, 6 freshness Keep, 5, 7, 10-12 Deferred. Checkboxes unchanged.
- [x] Tests, build, lint, device checks, changelog, commit.
- [x] Result: 182 unit tests (10 skipped), 0 failures; build passes; lint 0 errors, 127 warnings (unchanged). Device: cold start on Portfolio; bottom bar Portfolio, Advisor, Watchlist, History, Settings; Advisor opens on Dividend; notice on Swing tab and Watchlist; intents matching the notifications open Advisor (START_SCREEN=ADVISOR), Portfolio (sell reminder with OPEN_SYMBOL) and the DCA Buy dialog prefilled TDEX 9.0 x 100 with executed fill unticked (cancelled, nothing saved).

## Dividend holding exits, 2026-10-03

- [x] User decision: Dividend-purpose holdings follow the tested high-yield rule (exit on a saved stop or leaving the top 10; ROE, yield and drawdown are review notes).
- [x] `HoldingSignal.resolve` shared by `StockViewModel` (two sites) and `StockAlertWorker`; `DividendExitPolicy` for the Advisor's dividend alerts; Buy dialog text and ALERT_FLOWS updated (also removed the stale early-breakdown wording).
- [x] 192 unit tests (10 skipped), 0 failures; build passes; lint 0 errors, 127 warnings. Seven new tests: dividend hold ignores technical/quality sells, saved stop still alerts, swing and core unchanged, fixed plan before dividend hold, exit vs review notes, no notes when healthy or unranked.
- [x] Device check: Dividend tab shows 2 alerts: MBK "Left the high dividend yield top 10" (+21.8%) and JMT "Saved stop reached at ฿12.0" (฿10.10, -20.2%; one alert per symbol, highest priority first). No ROE or yield SELLs.
