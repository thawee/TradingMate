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

### 1-4 week momentum rules

`tools/backtest/momentum_report.md` tests three rules fixed on 2026-10-02 before the first run: buy a new 20-session closing high above SMA50, sell on a new 10- or 20-session low, a 2x ATR stop, or a 20- or 40-session time limit, with and without a TDEX-above-SMA50 market filter. None passes the gate. All three trail TDEX in 2021-2025, and every variant's profit depends on its top three symbols (DELTA, KTC and others). The best, M2 (20-day low exit, 40-day limit), returned 6.26% a year over 2015-2025 against TDEX's 2.52% but -2.62% in 2021-2025. Across variants, exits on the short-term low lost money on average while time-limit exits made it: selling on the first dip cuts the winners.

### Short-term rules from the literature

`tools/backtest/shortterm_report.md` tests four rules fixed on 2026-10-02 before the first run, chosen from effects reported in research: weekly short-term reversal (documented in Asian markets including Thailand), buying a sharp RSI(2) dip inside an uptrend (two variants), and the turn-of-month effect on TDEX. None passes. Weekly reversal earned a small average gain on trades held the full five days (+0.16R) but stops and Thai trading costs (about 0.6% per round trip) erased it; it lost 6.97% a year in 2021-2025. The dip rules lost money in both sub-periods. Turn of the month returned -4.18% a year against TDEX's 2.52%, being in the market 30% of the time and paying costs 131 times.

### 6-month momentum portfolio

`tools/backtest/momentum_portfolio_report.md` tests monthly-rebalanced cross-sectional momentum (Jegadeesh & Titman), three variants fixed on 2026-10-02 before the first run: hold the top 10 SET50 stocks by 6-1 month return, the same with positive returns only, and by 6-0 month return. It is the strongest result of all studies (MOM3: 12.45% a year over 2015-2025 against TDEX's 2.52%, holding positions about 100 days), but it still fails the gate: every variant trails TDEX in 2021-2025 (about -3% a year against +3.19%), and without its top three symbols (DELTA, JMART, KTB or TRUE) the profit is close to zero. Survivorship bias flatters momentum more than any other rule here.

### 6-month momentum with app signal timing

`tools/backtest/momentum_signal_report.md` keeps the MOM3 list but buys a listed stock only after an app BUY or POTENTIAL signal (S1) or a BUY signal (S2), fixed on 2026-10-02 before the first run; the baseline (S0) reproduces MOM3 exactly. Signal timing made results worse: S1 returned 8.55% a year over 2015-2025 against MOM3's 12.45% and lost more in 2021-2025 (-5.36%). S2 cut the worst drop to 23% but sat mostly in cash (38% invested), returning 3.52% a year and -0.36% in 2021-2025. All fail the gate. Waiting for a signal delays entry into stocks that are already rising.

### Low volatility and TDEX trend timing

`tools/backtest/lowvol_trend_report.md` tests five rules fixed on 2026-10-02 before the first run. Low-volatility portfolios (top 10 by lowest 63- or 252-day volatility) did not beat TDEX. Lowest beta to TDEX came closest of any stock rule: 5.22% a year over 2015-2025 against 2.52%, 7.39% against 1.75% in 2015-2020, and 2.81% against 3.19% in 2021-2025, but it still fails on 2021-2025 and on concentration (DELTA, TISCO, COM7). Index trend timing on TDEX does not raise returns: the month-end 10-month rule returned 1.68% a year against 2.52% (0% on cash) but cut the worst drop from 36.5% to 23.5% with 10 round trips in 11 years; the daily 200-day rule whipsawed (-0.62% a year, 38 round trips).

### Fundamental screens and high dividend yield

`tools/backtest/fundamental_screens_report.md` uses point-in-time quarterly fundamentals from thaifin (Finnomena; 50-day lag after Q1-Q3, 90 days after Q4). Quality screens (old ROE > 15 rule and the SET-calibrated one), Dividend Stars (yield >= 5% plus quality) and quality ranked by low beta all fail the gate. The only rule in all studies to pass is F5: each month hold the 10 stocks with the highest known dividend yield (7.80% a year over 2015-2025 against TDEX's 2.52%, ahead in both sub-periods, 58% of profit outside the top three names).

Because F5 was the one pass among about 19 rules, it was checked further (`tools/backtest/dividend_yield_robustness.md`, fixed before running): top 5, top 15, quarterly rebalancing and a 120-day lag all beat TDEX in both sub-periods, and a 2011-2014 holdout returned 24.95% a year against 9.68%.

These numbers are inflated by survivorship: the universe is today's SET50, so high-yield companies whose yield signalled distress before they fell out of the index are missing. A real, investable comparison has none of that bias: the 1DIV ETF (SET High Dividend 30) returned 3.86% a year over 2015-2025 against TDEX's 2.55% (Yahoo adjusted closes), but 0.48% against 1.80% in 2015-2020, 8.05% against 3.25% in 2021-2025 and 5.65% against 13.74% in 2012-2014. A high-dividend tilt has helped over the whole decade and strongly since 2021, but not in every period, and by far less than the backtest suggests.

### Averaging down on the high-dividend list

`tools/backtest/averaging_down_report.md` tests four changes to F5 fixed on 2026-10-03 before the first run, all triggered at month end when a held name is still in the top 10 and its close is at least 15% below average cost: A1 top up once to 10%, A2 add once up to 15%, A3 resize every kept name to 10% each month, and A4 (control) sell instead. The baseline reproduces F5 exactly. A1-A3 beat F5 in 2015-2020, 2021-2025 and the 2011-2014 holdout by 0.4 to 0.9 points a year (A3: 8.35% vs 7.80% over 2015-2025), with drawdowns 2.5 to 4.6 points deeper. A4 trails F5 in both sub-periods, consistent with the momentum finding that selling on a dip cuts winners. A2 lets one name reach 22% of equity; A3 keeps the largest name near 13% and lowers the top-three P/L share. The gains are small and the universe excludes delisted stocks, which flatters buying more of losers more than any other rule, so none of this reaches the app without a delisting-inclusive check.

### Known limits of the replay

- **Survivorship bias.** The universe is the H1 2025 SET50 (frozen in `tools/backtest/universe.txt`) applied to 2015-2025; delisted and demoted stocks are missing, which flatters momentum and breakout rules most. A point-in-time membership source (historical SET50/SET100 constituents) is needed to fix this. The app's `fetchIndexComposition` returns only current members, and no verified historical source is wired in yet.
- **Inputs not replayed.** NVDR flow, relative strength, weekly trend, XD grace, market-regime cash buffer, sector caps, fundamentals and AI ranking. A rule that depends on them cannot pass the gate until they are replayed.
- **Daily closes only.** No intraday fills, gaps are filled at the next close.
