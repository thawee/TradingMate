# High Dividend Yield With a 200-Day Trend Filter

Variants fixed before the first run (tasks/todo.md, 2026-10-08). Base rule F5: each month hold the 10 SET50 names with the highest reported dividend yield known at the time, 10% target for new names, kept names not resized, a name leaves when it is no longer eligible and top 10. "Above SMA200" = month-end close above the 200-session simple average. InnovestX fees (ATS), 0.15% slippage per side, 100-share lots, ฿1M start, cash earns nothing.

| Variant | Period | CAGR % | TDEX CAGR % | MDD % | Positions | Avg days held | Turnover x/yr | Max single-name weight % | Top-3 P/L share % |
|---|---|---|---|---|---|---|---|---|---|
| B0 F5 baseline | Full 2015-2025 | 7.80 | 2.52 | 43.35 | 103 | 328.78 | 1.74 | 16.35 | 42.25 |
| B0 F5 baseline | 2015-2020 | 8.36 | 1.75 | 43.35 | 57 | 322.19 | 1.66 | 16.35 | 48.24 |
| B0 F5 baseline | 2021-2025 | 5.50 | 3.19 | 19.94 | 56 | 283.67 | 1.98 | 15.19 | 66.47 |
| B0 F5 baseline | Holdout 2011-04 to 2014 | 24.95 | 9.68 | 17.08 | 36 | 293.77 | 1.62 | 24.88 | 53.84 |
| T-F5a F5 above SMA200 | Full 2015-2025 | 8.61 | 2.52 | 33.30 | 359 | 106.08 | 6.01 | 26.29 | 77.14 |
| T-F5a F5 above SMA200 | 2015-2020 | 14.80 | 1.75 | 33.30 | 186 | 116.25 | 5.57 | 26.29 | 65.20 |
| T-F5a F5 above SMA200 | 2021-2025 | 0.32 | 3.19 | 29.19 | 183 | 92.14 | 6.45 | 19.46 | 1334.53 |
| T-F5a F5 above SMA200 | Holdout 2011-04 to 2014 | 25.33 | 9.68 | 22.12 | 98 | 121.48 | 4.65 | 23.19 | 52.83 |
| T-F5b F5 above SMA200, yield >= 5% | Full 2015-2025 | 5.01 | 2.52 | 12.36 | 83 | 149.25 | 1.48 | 21.47 | 56.22 |
| T-F5b F5 above SMA200, yield >= 5% | 2015-2020 | 6.48 | 1.75 | 12.36 | 46 | 155.27 | 1.43 | 21.47 | 68.19 |
| T-F5b F5 above SMA200, yield >= 5% | 2021-2025 | 1.95 | 3.19 | 6.03 | 46 | 129.73 | 1.68 | 13.45 | 104.72 |
| T-F5b F5 above SMA200, yield >= 5% | Holdout 2011-04 to 2014 | 16.52 | 9.68 | 7.63 | 29 | 140.50 | 1.48 | 25.63 | 62.86 |

## Verdicts

Better than F5 only if CAGR beats B0 in 2015-2020, 2021-2025 and the holdout, with full-period max drawdown no worse than B0 + 5 points.

- T-F5a F5 above SMA200: FAIL (not above B0 in 2021-2025)
- T-F5b F5 above SMA200, yield >= 5%: FAIL (not above B0 in 2015-2020; not above B0 in 2021-2025; not above B0 in Holdout 2011-04 to 2014)

## Caveats

- The universe is today's SET50 members with history; delisted stocks are missing.
- The interview's other conditions (a confirmed break of a recent high, P/E around 11) are not replayed; there is no P/E history in the data.
- The holdout ranks fewer names (stocks listed later have no early prices).
