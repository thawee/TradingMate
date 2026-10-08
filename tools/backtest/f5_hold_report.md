# High Dividend Yield, Held Longer

Variants fixed before the first run (tasks/todo.md, 2026-10-08). Base rule F5: each month hold the 10 SET50 names with the highest reported dividend yield known at the time, 10% target for new names, kept names not resized. B0 sells a name that leaves the top 10; H1 and H2 keep held names by their own rule and refill free slots from the top of the list. InnovestX fees (ATS), 0.15% slippage per side, 100-share lots, ฿1M start, cash earns nothing.

| Variant | Period | CAGR % | TDEX CAGR % | MDD % | Positions | Avg days held | Turnover x/yr | Max single-name weight % | Top-3 P/L share % |
|---|---|---|---|---|---|---|---|---|---|
| B0 F5 baseline | Full 2015-2025 | 7.80 | 2.52 | 43.35 | 103 | 328.78 | 1.74 | 16.35 | 42.25 |
| B0 F5 baseline | 2015-2020 | 8.36 | 1.75 | 43.35 | 57 | 322.19 | 1.66 | 16.35 | 48.24 |
| B0 F5 baseline | 2021-2025 | 5.50 | 3.19 | 19.94 | 56 | 283.67 | 1.98 | 15.19 | 66.47 |
| B0 F5 baseline | Holdout 2011-04 to 2014 | 24.95 | 9.68 | 17.08 | 36 | 293.77 | 1.62 | 24.88 | 53.84 |
| H1 keep while in the top 30 | Full 2015-2025 | 6.47 | 2.52 | 44.40 | 18 | 1947.00 | 0.25 | 27.88 | 65.04 |
| H1 keep while in the top 30 | 2015-2020 | 6.56 | 1.75 | 44.40 | 11 | 2129.00 | 0.21 | 27.88 | 92.46 |
| H1 keep while in the top 30 | 2021-2025 | 6.25 | 3.19 | 19.52 | 21 | 917.82 | 0.58 | 23.04 | 87.65 |
| H1 keep while in the top 30 | Holdout 2011-04 to 2014 | 27.21 | 9.68 | 33.72 | 10 | 0.00 | 0.13 | 42.36 | 62.93 |
| H2 keep while yield >= 3% or unknown | Full 2015-2025 | 6.44 | 2.52 | 41.95 | 32 | 926.50 | 0.49 | 25.73 | 69.38 |
| H2 keep while yield >= 3% or unknown | 2015-2020 | 8.17 | 1.75 | 41.95 | 17 | 855.86 | 0.40 | 25.73 | 85.36 |
| H2 keep while yield >= 3% or unknown | 2021-2025 | 4.36 | 3.19 | 23.20 | 26 | 459.81 | 0.77 | 20.31 | 112.08 |
| H2 keep while yield >= 3% or unknown | Holdout 2011-04 to 2014 | 29.02 | 9.68 | 18.06 | 19 | 355.11 | 0.70 | 32.58 | 56.79 |

## Verdicts

Better than F5 only if CAGR beats B0 in 2015-2020, 2021-2025 and the holdout, with full-period max drawdown no worse than B0 + 5 points.

- H1 keep while in the top 30: FAIL (not above B0 in 2015-2020)
- H2 keep while yield >= 3% or unknown: FAIL (not above B0 in 2015-2020; not above B0 in 2021-2025)

## Caveats

- The universe is today's SET50 members with history; delisted stocks are missing.
- Selling on a dividend cut is not replayed: the quarterly yield field has gaps and moves with price. H2's 3% floor is a proxy. The interview's P/E and P/BV conditions are not replayed either (no history).
- The holdout ranks fewer names (stocks listed later have no early prices).
