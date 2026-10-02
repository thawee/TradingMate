# Market-Wide Backtest Report

Universe: 48 SET50 stocks as of H1 2025, frozen in tools/backtest/universe.txt (Yahoo, dividend-adjusted). Benchmark: TDEX (SET50 ETF) buy-and-hold, dividend-adjusted.
Config: ฿1M start, 1% risk per trade, 15% single-stock cap, max 10 positions, 0.15% slippage per side, InnovestX fees (ATS), next-close fills, 260-bar indicator window.

| Rule | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Trades | Trades/yr | Win % | Expectancy R | Exposure % | Skipped |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| App signals | Full 2015-2025 | -1.39 | 2.52 | -3.91 | 49.79 | 36.53 | 1044 | 95.05 | 47.80 | -0.03 | 76.60 | 355 |
| App signals | 2015-2020 | 5.20 | 1.75 | 3.45 | 36.34 | 36.53 | 531 | 88.72 | 50.85 | 0.06 | 75.80 | 163 |
| App signals | 2021-2025 | -8.89 | 3.19 | -12.08 | 47.09 | 26.55 | 503 | 100.89 | 44.93 | -0.12 | 76.93 | 200 |
| App entries + trend exit | Full 2015-2025 | -6.68 | 2.52 | -9.20 | 53.75 | 36.53 | 1411 | 128.46 | 33.24 | -0.08 | 60.15 | 190 |
| App entries + trend exit | 2015-2020 | -2.08 | 1.75 | -3.83 | 22.37 | 36.53 | 695 | 116.12 | 34.53 | -0.03 | 61.58 | 104 |
| App entries + trend exit | 2021-2025 | -12.19 | 3.19 | -15.38 | 49.02 | 26.55 | 711 | 142.61 | 31.93 | -0.13 | 57.84 | 89 |
| 52w breakout + trend exit | Full 2015-2025 | 11.14 | 2.52 | 8.62 | 38.01 | 36.53 | 401 | 36.51 | 39.15 | 0.41 | 68.05 | 645 |
| 52w breakout + trend exit | 2015-2020 | 23.80 | 1.75 | 22.05 | 19.11 | 36.53 | 202 | 33.75 | 44.06 | 0.65 | 68.76 | 460 |
| 52w breakout + trend exit | 2021-2025 | -1.03 | 3.19 | -4.22 | 27.93 | 26.55 | 197 | 39.51 | 32.99 | -0.18 | 67.36 | 187 |

## Exit reasons: App signals (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Stop Loss | 504 | -1.27 | -638.40 |
| Overbought | 233 | 1.23 | 287.75 |
| Upper Band Resistance | 218 | 1.14 | 249.45 |
| Trailing Stop Triggered | 50 | -0.24 | -12.06 |
| Distribution Detected | 16 | 1.30 | 20.75 |
| Scale Out Target | 15 | 2.49 | 37.40 |
| Exit Target | 8 | 2.46 | 19.71 |

## Exit reasons: App entries + trend exit (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Close < SMA50 | 1378 | -0.05 | -67.96 |
| Stop Loss | 33 | -1.44 | -47.42 |

## Exit reasons: 52w breakout + trend exit (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Close < SMA50 | 276 | 1.20 | 332.44 |
| Stop Loss | 125 | -1.34 | -167.87 |

## Concentration of closed-trade P/L (full period)

| Rule | Total ฿ | Top 3 symbols ฿ | Total excl. top 3 ฿ | Top 3 |
|---|---|---|---|---|
| App signals | -143,606 | 348,379 | -491,984 | BH 124,106, TTB 120,863, WHA 103,409 |
| App entries + trend exit | -536,380 | 129,067 | -665,446 | AOT 55,340, CENTEL 49,923, HMPRO 23,803 |
| 52w breakout + trend exit | 1,907,466 | 1,893,735 | 13,731 | DELTA 970,673, KTC 468,737, JMART 454,326 |

## Evidence gate (docs/ADVISOR_EVALUATION.md)

| Rule | Verdict | Reasons |
|---|---|---|
| App signals | FAIL | Trails benchmark in 2021-2025 (-8.89% vs 3.19%); Without its top 3 symbols, P/L is ฿-491,984 of ฿-143,606 (need positive and ≥ 50%); Expectancy -0.03R is not positive |
| App entries + trend exit | FAIL | Trails benchmark in 2015-2020 (-2.08% vs 1.75%); Trails benchmark in 2021-2025 (-12.19% vs 3.19%); Without its top 3 symbols, P/L is ฿-665,446 of ฿-536,380 (need positive and ≥ 50%); Expectancy -0.08R is not positive |
| 52w breakout + trend exit | FAIL | Trails benchmark in 2021-2025 (-1.03% vs 3.19%); Without its top 3 symbols, P/L is ฿13,731 of ฿1,907,466 (need positive and ≥ 50%) |

## Caveats

- Survivorship bias: universe is the H1 2025 SET50 applied to 2015-2025; delisted and demoted stocks are missing, so results are optimistic.
- Not replayed: NVDR flow, relative strength, weekly trend, XD grace, market regime cash buffer, sector caps, fundamentals, saved plans, AI ranking.
- App thresholds were designed with knowledge of this period; neither sub-period is a true out-of-sample test. Alternative rules use textbook parameters fixed before their first run.
- Simultaneous BUYs: app rules fill in symbol order; the breakout rule fills by 126-day momentum.
