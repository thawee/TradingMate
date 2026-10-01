# Market-Wide Backtest Report

Universe: 48 current SET50 stocks (Yahoo, dividend-adjusted). Benchmark: TDEX (SET50 ETF) buy-and-hold, dividend-adjusted.
Config: ฿1M start, 1% risk per trade, 15% single-stock cap, max 10 positions, 0.15% slippage per side, InnovestX fees (ATS), next-close fills, 260-bar indicator window.

| Period | Strategy CAGR % | TDEX CAGR % | Gap % | Strategy MDD % | TDEX MDD % | Trades | Trades/yr | Win % | Expectancy R | Exposure % | Skipped |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Full 2015-2025 | -10.10 | 2.52 | -12.62 | 72.53 | 36.53 | 2082 | 189.54 | 36.89 | -0.09 | 88.86 | 3641 |
| 2015-2020 | -5.10 | 1.75 | -6.85 | 40.97 | 36.53 | 1097 | 183.29 | 38.56 | -0.04 | 86.69 | 1742 |
| 2021-2025 | -15.51 | 3.19 | -18.70 | 61.14 | 26.55 | 991 | 198.77 | 34.11 | -0.15 | 91.02 | 1877 |

## Exit reasons (full period)

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

## Caveats

- Survivorship bias: universe is today's SET50; delisted and demoted stocks are missing, so results are optimistic.
- Not replayed: NVDR flow, relative strength, weekly trend, XD grace, market regime cash buffer, sector caps, fundamentals, saved plans, AI ranking.
- Thresholds were designed with knowledge of this period; neither sub-period is a true out-of-sample test.
