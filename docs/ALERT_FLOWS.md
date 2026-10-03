# Alert and notification flows

TradingMate has two alert paths: reactive in-app alerts from `StockViewModel` and background notifications from `StockAlertWorker`. Both evaluate a holding's saved swing plan. An alert reports an observed price or technical condition; it does not submit an order or guarantee a fill at that price.

## Background notifications

`MainActivity` schedules `StockAlertWorker` at a 30-minute interval and preserves an existing schedule when the app is reopened. WorkManager timing is approximate. The worker uses Asia/Bangkok time and checks the afternoon entry window (15:30–16:15) and January/June dividend-season reminder separately from its market-hours stock scan. The stock scan is skipped when the market is closed.

During a scan, the worker updates watchlist prices and indicators and checks for signal changes, active sell reminders, XD dates within seven days, and dividend-yield opportunities. A morning exit reminder (10:00–11:00) depends on an active swing sell alert. The notification helper deduplicates these reminders by their relevant stock, date, week, or season.

One function, `HoldingSignal.resolve`, decides the signal for every holding in the Advisor, Portfolio and the worker: a saved stop first, then an accepted `FIXED_TARGET` plan (saved stop, then saved target; no early-breakdown exit), then the core fund's hold signal, then Dividend-purpose holdings, then the technical signal for other holdings. The notification names the saved level when one is reached. Dividend-purpose holdings follow the tested high-yield rule: only a saved stop sends a notification. In the Advisor, `DividendExitPolicy` lists leaving the high-yield top 10 as the exit (acted on at the monthly review) and low ROE, yield below 3% and a 20% drawdown as review notes, not sells.

## In-app advisor alerts

`StockViewModel.alertRoutineState` combines the current playbook mode, watchlist and portfolio observations, and checklist state. The Advisor displays exit alerts and candidate groups. A fixed swing plan uses `ExitPolicyEvaluator` for stop, technical invalidation, and target priority; generic legacy profit thresholds do not replace its saved target. Clearing the target explicitly clears that fixed plan.

Swing candidates are screened for recent aligned stock and SET observations, liquidity, 52-week-low distance, quality, completed-week trend, and signal. The UI distinguishes Ready, Watch, and Blocked. A strong daily move is not proof of an opening gap or an earnings catalyst. Dividend candidates follow their separate dividend screen and are not fully covered by the swing status.

The Swing playbook presents **Check Exits**, **Scan Setups**, and **Ask AI**. The first step can complete automatically when no exit alerts exist; copying or running AI analysis can complete the AI step. The afternoon notification sets a scan badge that clears when the setup step is completed. Dividend mode is informational and does not use the Swing checklist.

## Implementation points

| Component | Responsibility |
|---|---|
| `domain/ExitPolicyEvaluator.kt` | Accepted fixed-plan exit priority |
| `ui/StockViewModel.kt` | Reactive advisor alerts and checklist state |
| `ui/DividendAdvisorScreen.kt` | Advisor presentation and step actions |
| `util/StockAlertWorker.kt` | Background market scan and time-window reminders |
| `util/NotificationHelper.kt` | Notification display and deduplication |
