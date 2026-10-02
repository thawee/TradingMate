# Fundamental Screens on History

Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: 48 SET50 stocks as of H1 2025 with thaifin quarterly fundamentals (Finnomena public API). A quarter is used from 50 days after a Q1-Q3 end and 90 days after Q4; ROE and margin are trailing four quarters. Monthly re-rank, top 10 at a 10% target (empty slots in cash), ฿1M, InnovestX fees (ATS), 0.15% slippage per side.

| Variant | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Closed positions | Win % | Avg net return / position % | Avg days held | Turnover / yr |
|---|---|---|---|---|---|---|---|---|---|---|---|
| F1 quality, old rule (ROE > 15), by ROE | Full 2015-2025 | 3.00 | 2.52 | 0.48 | 43.32 | 36.53 | 36 | 47.22 | 4.58 | 611.50 | 0.66 |
| F1 quality, old rule (ROE > 15), by ROE | 2015-2020 | 7.00 | 1.75 | 5.25 | 43.32 | 36.53 | 22 | 54.55 | 9.90 | 747.77 | 0.73 |
| F1 quality, old rule (ROE > 15), by ROE | 2021-2025 | -1.69 | 3.19 | -4.87 | 32.39 | 26.55 | 14 | 14.29 | -16.90 | 312.50 | 0.63 |
| F2 quality, SET rule (ROE > 10, banks exempt from D/E), by ROE | Full 2015-2025 | 6.97 | 2.52 | 4.45 | 49.80 | 36.53 | 46 | 50.00 | 3.58 | 444.50 | 0.65 |
| F2 quality, SET rule (ROE > 10, banks exempt from D/E), by ROE | 2015-2020 | 17.61 | 1.75 | 15.86 | 42.41 | 36.53 | 29 | 51.72 | 5.48 | 422.66 | 0.85 |
| F2 quality, SET rule (ROE > 10, banks exempt from D/E), by ROE | 2021-2025 | -0.79 | 3.19 | -3.97 | 34.28 | 26.55 | 17 | 41.18 | -9.37 | 352.65 | 0.78 |
| F3 Dividend Stars (yield >= 5% + F2 quality), by yield | Full 2015-2025 | 2.64 | 2.52 | 0.12 | 12.76 | 36.53 | 20 | 75.00 | 11.62 | 462.65 | 0.39 |
| F3 Dividend Stars (yield >= 5% + F2 quality), by yield | 2015-2020 | 3.12 | 1.75 | 1.37 | 12.76 | 36.53 | 8 | 87.50 | 16.71 | 468.00 | 0.31 |
| F3 Dividend Stars (yield >= 5% + F2 quality), by yield | 2021-2025 | 1.53 | 3.19 | -1.65 | 7.71 | 26.55 | 12 | 66.67 | 1.11 | 304.17 | 0.51 |
| F4 F2 quality, by lowest beta | Full 2015-2025 | 4.50 | 2.52 | 1.98 | 40.94 | 36.53 | 103 | 45.63 | 3.97 | 260.18 | 1.67 |
| F4 F2 quality, by lowest beta | 2015-2020 | 9.90 | 1.75 | 8.15 | 40.94 | 36.53 | 81 | 48.15 | 4.26 | 205.77 | 2.67 |
| F4 F2 quality, by lowest beta | 2021-2025 | -4.80 | 3.19 | -7.99 | 39.76 | 26.55 | 22 | 40.91 | -7.86 | 345.55 | 0.96 |
| F5 highest dividend yield, no quality filter | Full 2015-2025 | 7.80 | 2.52 | 5.28 | 43.35 | 36.53 | 93 | 50.54 | 7.40 | 328.78 | 1.74 |
| F5 highest dividend yield, no quality filter | 2015-2020 | 8.36 | 1.75 | 6.61 | 43.35 | 36.53 | 47 | 59.57 | 8.84 | 322.19 | 1.66 |
| F5 highest dividend yield, no quality filter | 2021-2025 | 5.50 | 3.19 | 2.31 | 19.94 | 26.55 | 46 | 43.48 | 3.56 | 283.67 | 1.98 |

## Concentration and evidence gate (full period)

| Variant | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |
|---|---|---|---|---|---|
| F1 quality, old rule (ROE > 15), by ROE | 384,247 | DELTA 502,647, SAWAD 55,015, MEGA 52,559 | -225,973 | FAIL | Trails benchmark in 2021-2025 (-1.69% vs 3.19%); Without its top 3 symbols, P/L is ฿-225,973 of ฿384,247 (need positive and ≥ 50%); Only 42 trades (need 100) |
| F2 quality, SET rule (ROE > 10, banks exempt from D/E), by ROE | 1,097,193 | DELTA 677,185, KTC 436,478, TISCO 202,375 | -218,846 | FAIL | Trails benchmark in 2021-2025 (-0.79% vs 3.19%); Without its top 3 symbols, P/L is ฿-218,846 of ฿1,097,193 (need positive and ≥ 50%); Only 55 trades (need 100) |
| F3 Dividend Stars (yield >= 5% + F2 quality), by yield | 330,906 | TISCO 132,609, MEGA 45,230, LH 43,817 | 109,250 | FAIL | Trails benchmark in 2021-2025 (1.53% vs 3.19%); Without its top 3 symbols, P/L is ฿109,250 of ฿330,906 (need positive and ≥ 50%); Only 22 trades (need 100) |
| F4 F2 quality, by lowest beta | 622,125 | TISCO 408,074, DELTA 332,231, KTC 168,376 | -286,555 | FAIL | Trails benchmark in 2021-2025 (-4.80% vs 3.19%); Without its top 3 symbols, P/L is ฿-286,555 of ฿622,125 (need positive and ≥ 50%) |
| F5 highest dividend yield, no quality filter | 1,281,773 | KKP 251,540, TISCO 164,038, PTTEP 125,969 | 740,226 | PASS | - |

## Caveats

- Survivorship bias: today's SET50 applied to 2015-2025; companies that shrank or failed are missing.
- Fundamentals come from a third-party aggregator and may include restatements made after the original release.
- Publication lags are conservative approximations of SET filing deadlines, not actual release dates.
