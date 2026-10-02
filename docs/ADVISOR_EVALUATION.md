# Evaluating advisor outcomes

The software checks whether a saved plan is applied consistently. It does not establish that the strategy is profitable. Evaluate results from recommendations recorded before trades, with actual broker fills and fees.

## Forward paper record

1. Before changing thresholds, fix a strategy version and a future holdout period. Keep all eligible, rejected, and no-setup observations, not only winners.
2. At each daily decision, record the symbol, strategy version, observation date, benchmark date, market regime, eligibility reasons, proposed entry/stop/target, quantity, estimated fees, and whether the user accepted the plan. Preserve plan revisions.
3. Link every later buy, partial sale, full sale, and manual override to that accepted plan. Import broker fees and fill prices where available. A missing link remains unknown; do not infer it from the ticker alone.
4. Report sample size, period, historical-data coverage, and missing observations. Separate paper decisions from executed trades and calculate realized and still-open results separately.
5. Compare net outcome against the accepted risk and target. Show slippage, costs, reason for exit, and any divergence between the alert and the user's actual fill. Stratify by strategy and market regime only when sample sizes are stated.

The current local journal records plan acceptance, AI ranking, fills, and undo events. It does not yet connect an AI ranking to a later accepted plan or retain complete market snapshots for full advisor replay. Until those links exist, the journal supports auditing individual events but not a reliable AI hit rate. The in-app backtest is a separate technical-only daily-close replay.

Data quality is part of evaluation: keep quote, fundamental, stock-bar, benchmark-bar, and model observation dates distinct. Discard or mark missing any setup whose required evidence is unavailable or stale. The in-app 63-observation risk estimates and qualitative AI assessments are neither calibrated win probabilities nor guarantees of future profit.

## Evidence gate for signal rules

A signal rule may drive BUY/SELL wording, entry notifications or a default-on alert only after it passes this gate. Until then it is shown as context ("Technical Setup", "On Watch") with the backtest notice, and entry alerts stay off by default.

The gate is implemented in `domain/EvidenceGate.kt` and evaluated for every rule in `tools/backtest/report.md`:

```
python3 tools/backtest/fetch_history.py
BACKTEST=1 ./gradlew testDebugUnitTest --tests '*MarketBacktestReport*'
```

A rule passes only if all of these hold in the portfolio replay (shared capital, 1% risk per trade, 15% single-stock cap, board lots, InnovestX fees, 0.15% slippage per side, next-close fills):

1. **Beats the benchmark in every sub-period.** CAGR above TDEX buy-and-hold (dividend-adjusted) in both 2015-2020 and 2021-2025. Beating the full period on the strength of one sub-period is not enough.
2. **Not carried by a few names.** After removing the three most profitable symbols, closed-trade P/L stays positive and is at least 50% of the total.
3. **Enough trades.** At least 100 closed trades over the full period.
4. **Positive expectancy.** Average R per closed trade above zero after costs.

Rules for changing a rule:

- Write the rule and its parameters down before the first run. Report every variant tried, not only the one that passed.
- Do not tune parameters on the backtest and then cite the same backtest as evidence. Confirm with a later holdout or the forward paper record above.
- The gate thresholds themselves were set on 2026-10-01. The 50% concentration share was chosen after seeing that the 52-week breakout kept only 0.7% of its P/L without DELTA, KTC and JMART; treat it as a policy choice, not a calibrated number.

Status on 2026-10-01: no rule passes. App signals, app entries with trend exits, and the 52-week breakout all fail (see the report).

Change on 2026-10-02: the "Early Breakdown Warning" and "Weak Trend" exits (sell a losing holding below SMA 50 with negative MACD) were removed from the signal engine and from saved-plan exits. On the same 2015-2025 replay, App signals CAGR moved from -10.10% to -4.31% (2015-2020: -5.10% to -0.61%; 2021-2025: -15.51% to -9.04%) and max drawdown from 72.5% to 53.8%; per-trade expectancy went from -0.09R to -0.10R. This is a removal measured on the data that motivated it, not an out-of-sample result, and App signals still fail the gate.

Fix on 2026-10-02: "near the lower/upper Bollinger band" was measured as within 5% of each band edge, which made both true for most prices in a tight band. It now uses the lower and upper fifth of the band (%B <= 0.2 / >= 0.8). This is a correctness fix, not a tuned parameter, but it changes which bars fire: App signals CAGR moved from -4.31% to -1.39% (2015-2020: -0.61% to +5.20%, ahead of TDEX's 1.75%; 2021-2025: -9.04% to -8.89%), max drawdown 53.8% to 49.8%, expectancy -0.10R to -0.03R. It still fails the gate on 2021-2025, on concentration (P/L without the top three symbols is -฿491,984) and on expectancy. The short-swing variants below were rerun with the fix; none passes either (best: P2 at +0.20% in 2015-2020 and -0.43% in 2021-2025).

### Short-swing plans (1-4 weeks)

`tools/backtest/advisor_event_study.md` replays the advisor's price-based filters with a 20-session time exit. Four plan variants were fixed on 2026-10-02 before the first run: the current 52-week-high target (P0), a 2R target (P1), a 20-session swing-high target (P2), and P1 without the BUY signal (P3). The bar for wiring one into the advisor was a positive mean net return and a lead over TDEX in both 2015-2020 and 2021-2025.

None passed. Reachable targets raise the hit rate (P2 34% vs P0 7%) and narrow the 2021-2025 loss (P1 -0.77% vs P0 -1.19% per trade), but every variant is negative in 2021-2025 and none beats TDEX in both sub-periods. Dropping the BUY signal (P3) changes little, consistent with the signal adding no edge. The advisor remains context, not a short-term buy list.

### Known limits of the replay

- **Survivorship bias.** The universe is the H1 2025 SET50 (frozen in `tools/backtest/universe.txt`) applied to 2015-2025; delisted and demoted stocks are missing, which flatters momentum and breakout rules most. A point-in-time membership source (historical SET50/SET100 constituents) is needed to fix this. The app's `fetchIndexComposition` returns only current members, and no verified historical source is wired in yet.
- **Inputs not replayed.** NVDR flow, relative strength, weekly trend, XD grace, market-regime cash buffer, sector caps, fundamentals and AI ranking. A rule that depends on them cannot pass the gate until they are replayed.
- **Daily closes only.** No intraday fills, gaps are filled at the next close.
