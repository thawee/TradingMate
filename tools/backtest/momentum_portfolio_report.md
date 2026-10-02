# 6-Month Momentum Portfolio

Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: 48 SET50 stocks as of H1 2025 (tools/backtest/universe.txt), dividend-adjusted. Rank at each month's last session, trade at the next close; ฿1M, 10% target per stock, 100-share lots, InnovestX fees (ATS), 0.15% slippage per side. Benchmark: TDEX buy-and-hold.

| Variant | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Closed positions | Win % | Avg net return / position % | Avg days held | Turnover / yr |
|---|---|---|---|---|---|---|---|---|---|---|---|
| MOM1 6-1 month momentum, top 10 | Full 2015-2025 | 9.39 | 2.52 | 6.87 | 46.55 | 36.53 | 387 | 48.84 | 3.86 | 99.83 | 6.64 |
| MOM1 6-1 month momentum, top 10 | 2015-2020 | 22.18 | 1.75 | 20.43 | 46.55 | 36.53 | 200 | 49.50 | 2.66 | 100.69 | 6.32 |
| MOM1 6-1 month momentum, top 10 | 2021-2025 | -2.87 | 3.19 | -6.05 | 31.35 | 26.55 | 186 | 46.24 | -0.31 | 92.08 | 7.08 |
| MOM2 6-1 month momentum, top 10, positive only | Full 2015-2025 | 6.97 | 2.52 | 4.45 | 46.56 | 36.53 | 383 | 47.78 | 3.10 | 98.19 | 6.57 |
| MOM2 6-1 month momentum, top 10, positive only | 2015-2020 | 17.42 | 1.75 | 15.67 | 46.56 | 36.53 | 195 | 47.69 | 2.18 | 99.98 | 6.26 |
| MOM2 6-1 month momentum, top 10, positive only | 2021-2025 | -3.21 | 3.19 | -6.40 | 32.15 | 26.55 | 185 | 45.95 | -0.41 | 91.40 | 7.05 |
| MOM3 6-0 month momentum, top 10 | Full 2015-2025 | 12.45 | 2.52 | 9.93 | 45.95 | 36.53 | 369 | 42.28 | 6.44 | 105.45 | 6.11 |
| MOM3 6-0 month momentum, top 10 | 2015-2020 | 23.61 | 1.75 | 21.86 | 45.95 | 36.53 | 190 | 46.32 | 3.07 | 105.20 | 5.68 |
| MOM3 6-0 month momentum, top 10 | 2021-2025 | -2.80 | 3.19 | -5.99 | 29.15 | 26.55 | 176 | 37.50 | -0.96 | 98.35 | 6.52 |

## Concentration and evidence gate (full period)

| Variant | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |
|---|---|---|---|---|---|
| MOM1 6-1 month momentum, top 10 | 1,678,914 | DELTA 942,275, JMART 488,857, KTB 216,909 | 30,873 | FAIL | Trails benchmark in 2021-2025 (-2.87% vs 3.19%); Without its top 3 symbols, P/L is ฿30,873 of ฿1,678,914 (need positive and ≥ 50%) |
| MOM2 6-1 month momentum, top 10, positive only | 1,095,903 | DELTA 625,493, JMART 340,376, KTB 171,041 | -41,006 | FAIL | Trails benchmark in 2021-2025 (-3.21% vs 3.19%); Without its top 3 symbols, P/L is ฿-41,006 of ฿1,095,903 (need positive and ≥ 50%) |
| MOM3 6-0 month momentum, top 10 | 2,627,964 | DELTA 1,672,392, JMART 571,273, TRUE 349,291 | 35,007 | FAIL | Trails benchmark in 2021-2025 (-2.80% vs 3.19%); Without its top 3 symbols, P/L is ฿35,007 of ฿2,627,964 (need positive and ≥ 50%) |

## Caveats

- Survivorship bias: today's SET50 applied to 2015-2025 flatters momentum most, since stocks that collapsed and left the index are missing.
- "Expectancy" in the gate is the average net return per closed position (no stop is used), not an R multiple.
- Kept names are not resized, so weights drift between rebalances.
