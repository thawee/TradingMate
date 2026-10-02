# Market-Wide Backtest Report

Universe: 48 current SET50 stocks (Yahoo, dividend-adjusted). Benchmark: TDEX (SET50 ETF) buy-and-hold, dividend-adjusted.
Config: ฿1M start, 1% risk per trade, 15% single-stock cap, max 10 positions, 0.15% slippage per side, InnovestX fees (ATS), next-close fills, 260-bar indicator window.

| Rule | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Trades | Trades/yr | Win % | Expectancy R | Exposure % | Skipped |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| App signals | Full 2015-2025 | -4.31 | 2.52 | -6.83 | 53.83 | 36.53 | 1446 | 131.64 | 46.20 | -0.10 | 92.94 | 4546 |
| App signals | 2015-2020 | -0.61 | 1.75 | -2.36 | 35.08 | 36.53 | 769 | 128.49 | 48.76 | -0.03 | 91.28 | 2137 |
| App signals | 2021-2025 | -9.04 | 3.19 | -12.23 | 52.99 | 26.55 | 678 | 135.99 | 43.36 | -0.18 | 94.79 | 2380 |
| App entries + trend exit | Full 2015-2025 | -12.78 | 2.52 | -15.30 | 78.36 | 36.53 | 3524 | 320.82 | 29.14 | -0.09 | 89.63 | 4513 |
| App entries + trend exit | 2015-2020 | -8.92 | 1.75 | -10.67 | 52.87 | 36.53 | 1793 | 299.59 | 29.34 | -0.07 | 87.61 | 2296 |
| App entries + trend exit | 2021-2025 | -17.80 | 3.19 | -20.99 | 66.36 | 26.55 | 1739 | 348.80 | 28.23 | -0.14 | 91.33 | 2196 |
| 52w breakout + trend exit | Full 2015-2025 | 11.14 | 2.52 | 8.62 | 38.01 | 36.53 | 401 | 36.51 | 39.15 | 0.41 | 68.05 | 645 |
| 52w breakout + trend exit | 2015-2020 | 23.80 | 1.75 | 22.05 | 19.11 | 36.53 | 202 | 33.75 | 44.06 | 0.65 | 68.76 | 460 |
| 52w breakout + trend exit | 2021-2025 | -1.03 | 3.19 | -4.22 | 27.93 | 26.55 | 197 | 39.51 | 32.99 | -0.18 | 67.36 | 187 |

## Exit reasons: App signals (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Stop Loss | 735 | -1.30 | -954.70 |
| Overbought | 352 | 1.20 | 423.09 |
| Upper Band Resistance | 261 | 1.12 | 292.26 |
| Trailing Stop Triggered | 43 | -0.33 | -14.01 |
| Distribution Detected | 27 | 1.19 | 32.16 |
| Exit Target | 17 | 2.89 | 49.06 |
| Scale Out Target | 11 | 2.74 | 30.14 |

## Exit reasons: App entries + trend exit (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Close < SMA50 | 3472 | -0.07 | -231.28 |
| Stop Loss | 52 | -1.42 | -74.05 |

## Exit reasons: 52w breakout + trend exit (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Close < SMA50 | 276 | 1.20 | 332.44 |
| Stop Loss | 125 | -1.34 | -167.87 |

## Concentration of closed-trade P/L (full period)

| Rule | Total ฿ | Top 3 symbols ฿ | Total excl. top 3 ฿ | Top 3 |
|---|---|---|---|---|
| App signals | -373,977 | 182,229 | -556,206 | KCE 79,620, TISCO 55,393, DELTA 47,216 |
| App entries + trend exit | -785,019 | 218,276 | -1,003,295 | KTC 141,505, CBG 41,152, BH 35,619 |
| 52w breakout + trend exit | 1,907,466 | 1,893,735 | 13,731 | DELTA 970,673, KTC 468,737, JMART 454,326 |

## Evidence gate (docs/ADVISOR_EVALUATION.md)

| Rule | Verdict | Reasons |
|---|---|---|
| App signals | FAIL | Trails benchmark in 2015-2020 (-0.61% vs 1.75%); Trails benchmark in 2021-2025 (-9.04% vs 3.19%); Without its top 3 symbols, P/L is ฿-556,206 of ฿-373,977 (need positive and ≥ 50%); Expectancy -0.10R is not positive |
| App entries + trend exit | FAIL | Trails benchmark in 2015-2020 (-8.92% vs 1.75%); Trails benchmark in 2021-2025 (-17.80% vs 3.19%); Without its top 3 symbols, P/L is ฿-1,003,295 of ฿-785,019 (need positive and ≥ 50%); Expectancy -0.09R is not positive |
| 52w breakout + trend exit | FAIL | Trails benchmark in 2021-2025 (-1.03% vs 3.19%); Without its top 3 symbols, P/L is ฿13,731 of ฿1,907,466 (need positive and ≥ 50%) |

## Caveats

- Survivorship bias: universe is today's SET50; delisted and demoted stocks are missing, so results are optimistic.
- Not replayed: NVDR flow, relative strength, weekly trend, XD grace, market regime cash buffer, sector caps, fundamentals, saved plans, AI ranking.
- App thresholds were designed with knowledge of this period; neither sub-period is a true out-of-sample test. Alternative rules use textbook parameters fixed before their first run.
- Simultaneous BUYs: app rules fill in symbol order; the breakout rule fills by 126-day momentum.
