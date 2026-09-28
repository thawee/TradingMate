# Advisor reliability repair plan

Status: implementation in progress. Core plan, exit, risk, AI validation and technical replay repairs are in the app; remaining release checks and outcome linkage are tracked in `todo.md`.
Date: 2026-09-24
Task checklist: [todo.md](todo.md#advisor-reliability-repair).

## Objective

Make the advice a user sees, the plan they save, the exit alerts they receive, and the behavior a backtest simulates agree. Remove unsupported claims about reward/risk and confidence. Measure subsequent outcomes using the actual advice and fills. Completing this work establishes consistent, testable behavior; profitability is a separate empirical question.

## Evidence behind the work

| Finding | Current location | Repair tasks |
| --- | --- | --- |
| Entered target is discarded; backup omits stop and plan details | `PortfolioScreen.kt` onConfirm; `StockViewModel.kt` exportBackup; `RoomModels.kt` SimplePortfolio | 1–2 |
| Cards propose at least +10% while advisor/worker can alert around +3–5%; exit logic is duplicated | `TechnicalAnalysis.kt` calculateSuggestedTargetPrice/getDetailedSignal; `StockViewModel.kt` alertRoutineState; `StockAlertWorker.kt` | 3–4, 7 |
| Gap candidates and AI use different entry filters; missing data can pass guards | `StockDna.kt`; `StockViewModel.kt`; `DividendAdvisorScreen.kt` | 5–6, 10 |
| Weekly guard has no production caller supplying its input | `TechnicalAnalysis.kt` isWeeklyTrendBullish | 6 |
| Reward/risk target is chosen to produce the desired ratio | `TechnicalAnalysis.kt` calculateSuggestedTargetPrice | 7 |
| Risk/buffer warnings do not prevent a proposed trade exceeding limits | `PortfolioScreen.kt` BuyStockDialog; `StockRepository.kt` executeBuy | 8–9 |
| AI price strings and confidence are accepted without economic validation | `GeminiClient.kt` analyze response parsing | 10 |
| Backtest omits selection rules, uses asymmetric entry/exit timing, and measures only closed-trade drawdown | `BacktestEngine.kt`; `BacktestScreen.kt` | 11 |
| No advice-to-fill evidence was available for attributing the user's losses | Existing trade records lack the full recommendation snapshot | 12–13 |

Paths above are under `app/src/main/java/apincer/mobile/tradings/` in the existing ui, domain, data, and util packages.

## Proposed behavior and decisions

### One persisted plan for each position

- Introduce a typed `TradePlan` with an ID, version, strategy, planned entry, initial stop, target, exit policy, creation time, and source. Keep actual average cost and actual fills distinct from planned entry.
- Persist a plan with its position; a refresh must not move the accepted stop or target. Explicit edits create a revision. A trailing stop may tighten under the selected policy and must never silently widen initial risk.
- Keep `FocusEntity.targetPrice` as a watchlist/focus objective; do not use it as the authoritative position target.
- Use additive Room migrations and backward-compatible backup defaults. Existing positions without a saved target become `Legacy / plan incomplete`; do not invent a historical target or discard their existing stop.
- Confirm current same-symbol buy behavior during implementation: the repository currently replaces position fields. New buys into a holding must not silently replace quantity, basis, or its accepted plan. Preserve existing recording semantics explicitly until an additive fill path is tested.

### One exit evaluator

- Create a pure evaluator used by the portfolio, advisor, background worker, and backtest. Return a structured action, reason, trigger level, plan version, and evaluation time.
- Default new swing plans to a fixed target and explicit initial stop. Optional trailing exits must be an explicit policy with persisted parameters. Do not introduce partial exits until remaining quantity and realized fees can be modeled consistently.
- Evaluate an explicit price stop even when RSI/MACD is unavailable. A market-data failure must not erase a known stop breach when a usable current price exists.
- Risk exits take precedence. Remove unrelated +3%, +5%, and baht-profit overrides for planned trades. Early technical invalidation can remain as a named reason shared across all consumers; overbought warnings alone must not masquerade as the saved target being reached.
- Keep dividend holding/review policy distinct from swing exits. Honor an explicit user stop; missing fundamental data must be shown as unknown rather than treated as confirmed deterioration. Share XD context between screen and worker.
- Legacy positions retain a documented compatibility policy and a prompt to complete the plan. Changing their exit behavior must be visible.
- Stops are alert levels in this app. The plan does not add broker order execution or promise fills at a stop price.

### Shared eligibility with explicit data quality

- Return `Ready`, `Watch`, or `Blocked`, plus reasons, from one candidate evaluator. Strategy-specific predicates operate inside shared requirements for price validity, data freshness, liquidity, market regime, and applicable trend checks.
- A SELL/invalidation state cannot enter an actionable long-entry list. `POTENTIAL` can remain visible as Watch. Missing evidence does not count as a successful required guard.
- Use per-symbol source timestamps and last completed trading sessions. Account for weekends/holidays; a newly fetched old quote is still old. Strategy inputs must describe compatible observation times.
- Compute weekly trend from dated, completed calendar-week bars and supply it to production callers. Replace the current approximation that groups every five observations, which can misalign holiday weeks. An unfinished week cannot be treated as completed history in replay.
- Rename the existing daily +4% filter to `Strong daily move`. Only label a setup `Gap up` when opening-price versus previous-close data supports it; only claim an earnings catalyst when supplied evidence supports it.
- Pass the same eligible candidates to Gemini that the advisor shows. AI cannot promote a blocked setup.

### Targets and risk that mean what they say

- Separate a user-authored target, a hypothetical target needed to reach a ratio, and a target backed by an identified historical price level. Display target source and observation time.
- Stop treating `entry + max(10%, 2 × risk)` as evidence of an achievable target. With no supported target, show `Target unavailable` / Watch. A hypothetical 2R calculator may remain clearly labeled and must not qualify a candidate by itself.
- Any automated price-level estimator must use only preceding completed bars and a documented deterministic rule. Backtest it as a new strategy rule; do not claim profitability because the level exists.
- Calculate estimated net reward and loss using quantity, actual recorded buy fees where available, broker fee settings, and an explicit fill/slippage assumption. Do not describe gross 2R as net 2R. Track unsupported minimum-daily-fee aggregation honestly.
- Apply risk-per-trade, combined same-symbol allocation, sector allocation, and post-fee cash reserve checks before marking a proposed trade ready. Recompute from current holdings/settings at confirmation.
- The app is also a ledger: allow recording/importing a real broker fill with truthful values and a recorded rule breach. Do not hide an actual loss or prevent a historical fill from being recorded because it violated today's settings. Use a separate explicit intent for recording an executed trade.

### Bound the AI's role

- Gemini ranks/explains eligible, locally computed plans. It receives plan IDs, timestamps, levels, relevant indicators, holdings exposure, and spendable cash.
- Prefer returned plan IDs and explanations over AI-authored executable prices. If the response contains proposed numeric changes, validate them through the same plan and risk policies and mark them unaccepted until reviewed.
- Reject unknown symbols/plan IDs, stale versions, non-finite or invalid levels, excessive allocations, and malformed results before actionable rendering.
- Replace percentage confidence with a clearly qualitative model assessment. There is no calibrated probability unless future outcome data establishes one. An empty result or `No valid setup` is a supported answer.

### Honest replay and evidence

- Reuse the entry/exit policies where historical inputs exist. Never substitute today's fundamentals, NVDR flow, or constituents for historical observations.
- Preserve an explicitly labeled technical-only replay when full historical inputs are unavailable. Full advisor replay is enabled only for complete historical snapshots or newly recorded forward observations; no implication that technical replay validates AI selection.
- Use one disclosed signal-to-fill convention. Baseline: both close-derived entry and exit signals fill at the next available bar, never at the close that created the signal. Prefer next open when valid opens are available; otherwise label the next-close approximation.
- Model spread/slippage assumptions, configured fees, board lots, available cash, and any enabled partial exits. An intrabar stop-fill mode must specify gap handling and the conservative ordering when stop and target both fall inside one bar; otherwise label stops as evaluated at bar close.
- Mark equity to market on every bar, include open positions and estimated exit costs, and report realized/unrealized results separately. Daily-bar drawdown must be labeled with that resolution.
- Version the strategy and record local recommendation snapshots, entry decisions, linked fills, fees, revisions, exit reasons, and manual overrides. Use later outcomes to compare planned versus realized reward/risk and behavior by strategy/regime.
- Historical or paper results must show sample size, period, assumptions, and data coverage. Choose a holdout before tuning; no profit or win-rate threshold is invented as a software release gate.

## Implementation order

1. Tasks 1–2: persist and round-trip the accepted plan.
2. Tasks 3–4: centralize exit decisions and connect every live consumer.
3. Tasks 5–6: centralize candidate checks and supply weekly/freshness evidence.
4. Tasks 7–9: make targets explicit and enforce proposal risk through the UI and repository boundary.
5. Tasks 10–11: constrain AI output and repair replay economics/reporting.
6. Tasks 12–13: capture attributable outcomes, update claims, and complete verification.

Each task is a focused slice with acceptance checks in `todo.md`. Tasks sharing persistence or decision contracts run sequentially. No delegation is required by this plan.

## Verification and release gates

- Begin with deterministic regression fixtures for each reviewed failure. Use fixed prices/time/settings and stub AI responses, not live market services.
- Core example: entry 100, saved stop 95, saved target 110, no invalidation/trailing trigger. A move to 103 or 105 must not produce `Target reached`; 110 must. An explicit stop still works with missing indicators.
- Compare screen and worker decisions for identical inputs. Check migration, app restart, editing, backup round-trip, partial sale and same-symbol additions without corrupting the plan or cash.
- Verify the bearish/SELL gap bypass is closed, weekly bearish data is applied, missing required data yields Watch/Blocked, and the AI cannot reintroduce rejected candidates.
- For proposed trades test exactly-at-limit, one-lot-over-limit, combined holdings exposure, fees, cash reserves, unknown sector, and changed settings between preview and confirmation. Recording a real fill remains possible with its breach noted.
- Backtest fixtures cover next-bar execution, an open losing position, a deep drawdown followed by recovery, costs flipping a small gross win negative, and missing historical selection data.
- Focused tests after each slice. At phase checkpoints run `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Run Room migration/instrumentation checks on an available device with `./gradlew :app:connectedDebugAndroidTest`; add required test support during implementation if absent. Final lint: `./gradlew :app:lintDebug`.
- Read-only shadow comparison on recorded snapshots before replacing live decisions; show and investigate changed reasons. Stop duplicating old paths after parity is verified. User review occurs at the release checkpoint; no deployment or broker action is implied.

## Risks and boundaries

| Risk | Handling |
| --- | --- |
| Existing positions have no recoverable target | Preserve them as incomplete legacy plans; ask for the target during plan editing |
| Stricter data requirements produce fewer recommendations | Display the missing evidence and Watch state; do not silently bypass a guard |
| Unified exits change trade duration/results | Version behavior and compare old/new decisions on the same observations |
| Missing historical fundamentals/flow | Label restricted replay and collect forward snapshots |
| Daily bars cannot reconstruct all intraday fills | Disclose fill convention and resolution; use conservative assumptions |
| Tracker controls mistaken for broker protection | Describe blocked proposals and alert levels precisely |
| Tuning on a small winning sample | Separate software correctness from out-of-sample performance evaluation |

## Items to confirm during implementation

- Fixed-target swing exits are the proposed default; partial scale-out is deferred until modeled consistently.
- Keep current configurable risk budgets initially; this repair does not select new investment risk limits.
- Confirm the user uses the portfolio buy form to plan trades, record executed fills, or both; preserve both intents in the final flow.
- Confirm accessible broker history and contemporaneous recommendations for a personal loss analysis. That evidence is not required to repair the confirmed software issues.
- The plan was initially written before implementation. Implementation and verification status is recorded in `todo.md`.
