# Dividend-Trap Filters on the High Dividend Yield List

Variants fixed before the first run (tasks/todo.md, 2026-10-08). Base rule F5: each month hold the 10 SET50 names with the highest reported dividend yield known at the time, 10% target for new names, kept names not resized, a name leaves when it is no longer eligible and top 10. TR1: ineligible when the month-end close is at least 20% below the close 252 sessions earlier. TR2: PTT, PTTEP, BCP, SPRC, OR, IVL, PTTGC and SCC are never eligible. InnovestX fees (ATS), 0.15% slippage per side, 100-share lots, ฿1M start, cash earns nothing.

| Variant | Period | CAGR % | TDEX CAGR % | MDD % | Positions | Avg days held | Turnover x/yr | Max single-name weight % | Top-3 P/L share % |
|---|---|---|---|---|---|---|---|---|---|
| B0 F5 baseline | Full 2015-2025 | 7.80 | 2.52 | 43.35 | 103 | 328.78 | 1.74 | 16.35 | 42.25 |
| B0 F5 baseline | 2015-2020 | 8.36 | 1.75 | 43.35 | 57 | 322.19 | 1.66 | 16.35 | 48.24 |
| B0 F5 baseline | 2021-2025 | 5.50 | 3.19 | 19.94 | 56 | 283.67 | 1.98 | 15.19 | 66.47 |
| B0 F5 baseline | Holdout 2011-04 to 2014 | 24.95 | 9.68 | 17.08 | 36 | 293.77 | 1.62 | 24.88 | 53.84 |
| TR1 skip names down 20% or more in 12 months | Full 2015-2025 | 9.37 | 2.52 | 43.20 | 229 | 175.33 | 3.87 | 26.41 | 50.18 |
| TR1 skip names down 20% or more in 12 months | 2015-2020 | 12.59 | 1.75 | 43.20 | 118 | 195.16 | 3.40 | 26.41 | 75.06 |
| TR1 skip names down 20% or more in 12 months | 2021-2025 | 4.36 | 3.19 | 23.63 | 117 | 155.23 | 4.22 | 15.52 | 100.77 |
| TR1 skip names down 20% or more in 12 months | Holdout 2011-04 to 2014 | 26.42 | 9.68 | 17.12 | 54 | 219.16 | 2.44 | 23.61 | 56.94 |
| TR2 skip commodity names | Full 2015-2025 | 11.44 | 2.52 | 27.89 | 90 | 374.21 | 1.46 | 19.95 | 34.14 |
| TR2 skip commodity names | 2015-2020 | 16.23 | 1.75 | 27.89 | 58 | 299.08 | 1.78 | 19.95 | 47.18 |
| TR2 skip commodity names | 2021-2025 | 5.31 | 3.19 | 21.49 | 42 | 387.78 | 1.44 | 16.19 | 82.89 |
| TR2 skip commodity names | Holdout 2011-04 to 2014 | 38.69 | 9.68 | 20.30 | 28 | 229.83 | 1.17 | 40.66 | 59.73 |

## Verdicts

Better than F5 only if CAGR beats B0 in 2015-2020, 2021-2025 and the holdout, with full-period max drawdown no worse than B0 + 5 points.

- TR1 skip names down 20% or more in 12 months: FAIL (not above B0 in 2021-2025)
- TR2 skip commodity names: FAIL (not above B0 in 2021-2025)

## Caveats

- The universe is today's SET50 members with history; delisted stocks are missing.
- The interview's main test, next year's expected dividend and the payout ratio, cannot be replayed: there are no analyst forecasts or share counts in the data. TR1 is a proxy for a yield inflated by a falling price.
- The holdout ranks fewer names (stocks listed later have no early prices).
