# Market-Wide Backtest Report

Universe: 48 current SET50 stocks (Yahoo, dividend-adjusted). Benchmark: TDEX (SET50 ETF) buy-and-hold, dividend-adjusted.
Config: ฿1M start, 1% risk per trade, 15% single-stock cap, max 10 positions, 0.15% slippage per side, InnovestX fees (ATS), next-close fills, 260-bar indicator window.

| Rule | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Trades | Trades/yr | Win % | Expectancy R | Exposure % | Skipped |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| App signals | Full 2015-2025 | -10.10 | 2.52 | -12.62 | 72.53 | 36.53 | 2082 | 189.54 | 36.89 | -0.09 | 88.86 | 3641 |
| App signals | 2015-2020 | -5.10 | 1.75 | -6.85 | 40.97 | 36.53 | 1097 | 183.29 | 38.56 | -0.04 | 86.69 | 1742 |
| App signals | 2021-2025 | -15.51 | 3.19 | -18.70 | 61.14 | 26.55 | 991 | 198.77 | 34.11 | -0.15 | 91.02 | 1877 |
| App entries + trend exit | Full 2015-2025 | -12.78 | 2.52 | -15.30 | 78.36 | 36.53 | 3524 | 320.82 | 29.14 | -0.09 | 89.63 | 4513 |
| App entries + trend exit | 2015-2020 | -8.92 | 1.75 | -10.67 | 52.87 | 36.53 | 1793 | 299.59 | 29.34 | -0.07 | 87.61 | 2296 |
| App entries + trend exit | 2021-2025 | -17.80 | 3.19 | -20.99 | 66.36 | 26.55 | 1739 | 348.80 | 28.23 | -0.14 | 91.33 | 2196 |
| 52w breakout + trend exit | Full 2015-2025 | 11.14 | 2.52 | 8.62 | 38.01 | 36.53 | 401 | 36.51 | 39.15 | 0.41 | 68.05 | 645 |
| 52w breakout + trend exit | 2015-2020 | 23.80 | 1.75 | 22.05 | 19.11 | 36.53 | 202 | 33.75 | 44.06 | 0.65 | 68.76 | 460 |
| 52w breakout + trend exit | 2021-2025 | -1.03 | 3.19 | -4.22 | 27.93 | 26.55 | 197 | 39.51 | 32.99 | -0.18 | 67.36 | 187 |

## Exit reasons: App signals (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Early Breakdown Warning | 1058 | -0.65 | -685.49 |
| Overbought | 370 | 1.19 | 440.59 |
| Upper Band Resistance | 292 | 1.09 | 317.00 |
| Stop Loss | 266 | -1.34 | -357.43 |
| Trailing Stop Triggered | 42 | -0.13 | -5.27 |
| Distribution Detected | 28 | 1.07 | 29.95 |
| Exit Target | 15 | 2.69 | 40.38 |
| Scale Out Target | 11 | 2.55 | 28.08 |

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
| App signals | -689,578 | 109,496 | -799,074 | TISCO 40,247, AOT 36,564, CENTEL 32,685 |
| App entries + trend exit | -785,019 | 218,276 | -1,003,295 | KTC 141,505, CBG 41,152, BH 35,619 |
| 52w breakout + trend exit | 1,907,466 | 1,893,735 | 13,731 | DELTA 970,673, KTC 468,737, JMART 454,326 |

## Caveats

- Survivorship bias: universe is today's SET50; delisted and demoted stocks are missing, so results are optimistic.
- Not replayed: NVDR flow, relative strength, weekly trend, XD grace, market regime cash buffer, sector caps, fundamentals, saved plans, AI ranking.
- App thresholds were designed with knowledge of this period; neither sub-period is a true out-of-sample test. Alternative rules use textbook parameters fixed before their first run.
- Simultaneous BUYs: app rules fill in symbol order; the breakout rule fills by 126-day momentum.
