# 6-Month Momentum With App Signal Timing

Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: 48 SET50 stocks as of H1 2025, dividend-adjusted. Top 10 by 126-session return, re-ranked at each month end; ฿1M, 10% target per stock, 100-share lots, InnovestX fees (ATS), 0.15% slippage per side. Benchmark: TDEX buy-and-hold.

| Variant | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | Closed positions | Win % | Avg net return / position % | Avg invested % |
|---|---|---|---|---|---|---|---|---|---|
| S0 MOM3 baseline (buy at next close) | Full 2015-2025 | 12.45 | 2.52 | 9.93 | 45.95 | 369 | 42.28 | 6.44 | 98.18 |
| S0 MOM3 baseline (buy at next close) | 2015-2020 | 23.61 | 1.75 | 21.86 | 45.95 | 190 | 46.32 | 3.07 | 97.46 |
| S0 MOM3 baseline (buy at next close) | 2021-2025 | -2.80 | 3.19 | -5.99 | 29.15 | 176 | 37.50 | -0.96 | 97.31 |
| S1 MOM3 + app BUY or POTENTIAL to enter | Full 2015-2025 | 8.55 | 2.52 | 6.03 | 45.12 | 344 | 45.93 | 4.35 | 77.31 |
| S1 MOM3 + app BUY or POTENTIAL to enter | 2015-2020 | 17.48 | 1.75 | 15.73 | 45.12 | 176 | 51.70 | 1.64 | 74.63 |
| S1 MOM3 + app BUY or POTENTIAL to enter | 2021-2025 | -5.36 | 3.19 | -8.54 | 29.08 | 164 | 39.02 | -1.51 | 75.36 |
| S2 MOM3 + app BUY to enter | Full 2015-2025 | 3.52 | 2.52 | 1.00 | 23.10 | 186 | 50.54 | 2.37 | 38.28 |
| S2 MOM3 + app BUY to enter | 2015-2020 | 5.41 | 1.75 | 3.66 | 20.44 | 98 | 56.12 | 2.10 | 33.51 |
| S2 MOM3 + app BUY to enter | 2021-2025 | -0.36 | 3.19 | -3.55 | 22.92 | 87 | 43.68 | 0.20 | 41.89 |

## Concentration and evidence gate (full period)

| Variant | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |
|---|---|---|---|---|---|
| S0 MOM3 baseline (buy at next close) | 2,627,964 | DELTA 1,672,392, JMART 571,273, TRUE 349,291 | 35,007 | FAIL | Trails benchmark in 2021-2025 (-2.80% vs 3.19%); Without its top 3 symbols, P/L is ฿35,007 of ฿2,627,964 (need positive and ≥ 50%) |
| S1 MOM3 + app BUY or POTENTIAL to enter | 1,462,122 | DELTA 1,183,500, JMART 293,138, KTB 188,454 | -202,970 | FAIL | Trails benchmark in 2021-2025 (-5.36% vs 3.19%); Without its top 3 symbols, P/L is ฿-202,970 of ฿1,462,122 (need positive and ≥ 50%) |
| S2 MOM3 + app BUY to enter | 461,635 | KCE 262,437, DELTA 79,324, JMART 59,851 | 60,023 | FAIL | Trails benchmark in 2021-2025 (-0.36% vs 3.19%); Without its top 3 symbols, P/L is ฿60,023 of ฿461,635 (need positive and ≥ 50%) |

## Caveats

- Survivorship bias: today's SET50 applied to 2015-2025 flatters momentum.
- The app signal is computed as for a flat position; "Expectancy" in the gate is the average net return per closed position.
