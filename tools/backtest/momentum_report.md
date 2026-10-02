# 1-4 Week Momentum Rules

Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: 48 SET50 stocks as of H1 2025 (tools/backtest/universe.txt), dividend-adjusted. Config: ฿1M start, 1% risk per trade, 15% single-stock cap, max 10 positions, 0.15% slippage per side, InnovestX fees (ATS), next-close fills. Benchmark: TDEX buy-and-hold.

| Rule | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Trades | Trades/yr | Win % | Expectancy R | Avg calendar days held | Exposure % |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| M1 20d high / 10d low / 20d max, market filter | Full 2015-2025 | -0.79 | 2.52 | -3.31 | 36.95 | 36.53 | 1164 | 105.97 | 34.79 | -0.01 | 19.63 | 62.74 |
| M1 20d high / 10d low / 20d max, market filter | 2015-2020 | 1.87 | 1.75 | 0.12 | 24.87 | 36.53 | 621 | 103.76 | 37.36 | 0.07 | 19.88 | 61.25 |
| M1 20d high / 10d low / 20d max, market filter | 2021-2025 | -6.05 | 3.19 | -9.23 | 37.17 | 26.55 | 540 | 108.31 | 32.22 | -0.11 | 19.29 | 64.35 |
| M2 20d high / 20d low / 40d max, market filter | Full 2015-2025 | 6.26 | 2.52 | 3.74 | 30.53 | 36.53 | 732 | 66.64 | 36.89 | 0.13 | 36.17 | 71.12 |
| M2 20d high / 20d low / 40d max, market filter | 2015-2020 | 11.58 | 1.75 | 9.83 | 30.53 | 36.53 | 365 | 60.99 | 40.82 | 0.26 | 37.85 | 69.22 |
| M2 20d high / 20d low / 40d max, market filter | 2021-2025 | -2.62 | 3.19 | -5.81 | 29.86 | 26.55 | 350 | 70.20 | 33.71 | -0.08 | 34.68 | 72.77 |
| M3 20d high / 10d low / 20d max, no market filter | Full 2015-2025 | 2.80 | 2.52 | 0.28 | 44.70 | 36.53 | 1569 | 142.84 | 38.05 | 0.03 | 19.96 | 86.85 |
| M3 20d high / 10d low / 20d max, no market filter | 2015-2020 | 8.57 | 1.75 | 6.82 | 24.08 | 36.53 | 816 | 136.34 | 41.05 | 0.10 | 20.48 | 84.84 |
| M3 20d high / 10d low / 20d max, no market filter | 2021-2025 | -6.49 | 3.19 | -9.68 | 45.47 | 26.55 | 753 | 151.03 | 34.13 | -0.12 | 19.29 | 89.07 |

## Exit reasons: M1 20d high / 10d low / 20d max, market filter (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| 10-day low | 705 | -0.40 | -282.78 |
| Time (20 days) | 293 | 1.67 | 489.75 |
| Stop | 166 | -1.32 | -218.78 |

## Exit reasons: M2 20d high / 20d low / 40d max, market filter (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| 20-day low | 313 | -0.17 | -54.25 |
| Stop | 238 | -1.38 | -327.40 |
| Time (40 days) | 181 | 2.62 | 473.80 |

## Exit reasons: M3 20d high / 10d low / 20d max, no market filter (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| 10-day low | 980 | -0.38 | -369.44 |
| Time (20 days) | 401 | 1.67 | 671.59 |
| Stop | 188 | -1.39 | -261.38 |

## Concentration and evidence gate (full period)

| Rule | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |
|---|---|---|---|---|---|
| M1 20d high / 10d low / 20d max, market filter | -87,307 | KTC 123,122, COM7 113,438, JMART 94,317 | -418,183 | FAIL | Trails benchmark in 2021-2025 (-6.05% vs 3.19%); Without its top 3 symbols, P/L is ฿-418,183 of ฿-87,307 (need positive and ≥ 50%); Expectancy -0.01R is not positive |
| M2 20d high / 20d low / 40d max, market filter | 931,067 | DELTA 545,562, KTC 295,751, TRUE 255,295 | -165,541 | FAIL | Trails benchmark in 2021-2025 (-2.62% vs 3.19%); Without its top 3 symbols, P/L is ฿-165,541 of ฿931,067 (need positive and ≥ 50%) |
| M3 20d high / 10d low / 20d max, no market filter | 340,685 | DELTA 513,111, KCE 196,700, KTC 121,952 | -491,079 | FAIL | Trails benchmark in 2021-2025 (-6.49% vs 3.19%); Without its top 3 symbols, P/L is ฿-491,079 of ฿340,685 (need positive and ≥ 50%) |

## Caveats

- Survivorship bias: today's SET50 applied to 2015-2025 flatters momentum rules most.
- Daily closes only; gaps fill at the next close. The ATR stop is recomputed from current volatility, as in the app's trend-exit replay.
