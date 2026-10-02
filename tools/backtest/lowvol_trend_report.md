# Low Volatility and Index Trend Timing

Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: 48 SET50 stocks as of H1 2025, dividend-adjusted. Portfolios re-rank at each month end and hold the top 10 at a 10% target (same simulator as the momentum portfolio); ฿1M, InnovestX fees (ATS), 0.15% slippage per side, 100-share lots.

## Low-volatility portfolios

| Variant | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Closed positions | Win % | Avg net return / position % | Avg days held | Turnover / yr |
|---|---|---|---|---|---|---|---|---|---|---|---|
| LV1 lowest 63-day volatility, top 10 | Full 2015-2025 | 1.60 | 2.52 | -0.92 | 42.71 | 36.53 | 272 | 44.85 | 0.06 | 131.11 | 4.83 |
| LV1 lowest 63-day volatility, top 10 | 2015-2020 | 0.02 | 1.75 | -1.73 | 42.71 | 36.53 | 138 | 47.83 | 0.49 | 147.20 | 4.61 |
| LV1 lowest 63-day volatility, top 10 | 2021-2025 | 3.34 | 3.19 | 0.15 | 22.63 | 26.55 | 132 | 43.94 | -0.20 | 104.66 | 5.24 |
| LV2 lowest 252-day volatility, top 10 | Full 2015-2025 | 1.41 | 2.52 | -1.11 | 41.85 | 36.53 | 96 | 47.92 | -0.61 | 341.28 | 1.68 |
| LV2 lowest 252-day volatility, top 10 | 2015-2020 | 0.35 | 1.75 | -1.40 | 41.85 | 36.53 | 55 | 58.18 | 3.23 | 303.11 | 1.92 |
| LV2 lowest 252-day volatility, top 10 | 2021-2025 | 2.60 | 3.19 | -0.58 | 21.65 | 26.55 | 41 | 43.90 | -2.39 | 279.54 | 1.66 |
| LV3 lowest 252-day beta to TDEX, top 10 | Full 2015-2025 | 5.22 | 2.52 | 2.70 | 43.22 | 36.53 | 160 | 50.00 | 3.64 | 223.06 | 2.81 |
| LV3 lowest 252-day beta to TDEX, top 10 | 2015-2020 | 7.39 | 1.75 | 5.64 | 43.22 | 36.53 | 92 | 51.09 | 2.84 | 210.55 | 3.16 |
| LV3 lowest 252-day beta to TDEX, top 10 | 2021-2025 | 2.81 | 3.19 | -0.38 | 27.51 | 26.55 | 67 | 47.76 | -0.22 | 205.66 | 2.57 |

### Concentration and evidence gate (full period)

| Variant | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |
|---|---|---|---|---|---|
| LV1 lowest 63-day volatility, top 10 | 189,894 | TISCO 118,766, TRUE 88,616, BBL 56,358 | -73,845 | FAIL | Trails benchmark in 2015-2020 (0.02% vs 1.75%); Without its top 3 symbols, P/L is ฿-73,845 of ฿189,894 (need positive and ≥ 50%) |
| LV2 lowest 252-day volatility, top 10 | 166,403 | TISCO 176,270, ADVANC 81,166, BDMS 66,667 | -157,701 | FAIL | Trails benchmark in 2015-2020 (0.35% vs 1.75%); Trails benchmark in 2021-2025 (2.60% vs 3.19%); Without its top 3 symbols, P/L is ฿-157,701 of ฿166,403 (need positive and ≥ 50%); Expectancy -0.01R is not positive |
| LV3 lowest 252-day beta to TDEX, top 10 | 748,232 | DELTA 324,297, TISCO 237,165, COM7 126,648 | 60,121 | FAIL | Trails benchmark in 2021-2025 (2.81% vs 3.19%); Without its top 3 symbols, P/L is ฿60,121 of ฿748,232 (need positive and ≥ 50%) |

## TDEX trend timing

Cash earns 0% (a conservative assumption; Thai deposits paid about 0.5-2%).

| Variant | Period | CAGR % | TDEX CAGR % | MDD % | TDEX MDD % | Round trips | Time in market % |
|---|---|---|---|---|---|---|---|
| T1 TDEX above 200-day average | Full 2015-2025 | -0.62 | 2.52 | 31.49 | 36.53 | 38 | 60.1 |
| T1 TDEX above 200-day average | 2015-2020 | -0.23 | 1.75 | 26.76 | 36.53 | 19 | 57.1 |
| T1 TDEX above 200-day average | 2021-2025 | -1.98 | 3.19 | 24.94 | 26.55 | 19 | 63.8 |
| T2 TDEX above 10-month average (month end) | Full 2015-2025 | 1.68 | 2.52 | 23.52 | 36.53 | 10 | 58.5 |
| T2 TDEX above 10-month average (month end) | 2015-2020 | 2.37 | 1.75 | 18.18 | 36.53 | 4 | 55.4 |
| T2 TDEX above 10-month average (month end) | 2021-2025 | 0.31 | 3.19 | 20.86 | 26.55 | 6 | 60.5 |

## Caveats

- Survivorship bias: today's SET50 applied to 2015-2025.
- The cited low-risk study used long-short portfolios over 2004-2015; these are long-only.
- "Expectancy" in the gate is the average net return per closed position.
