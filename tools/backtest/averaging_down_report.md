# Averaging Down on the High Dividend Yield List

Variants fixed before the first run (tasks/todo.md, 2026-10-03). Base rule F5: each month hold the 10 stocks with the highest reported dividend yield known at the time, 10% target for new names, kept names not resized; InnovestX fees (ATS), 0.15% slippage per side, 100-share lots. "Down" = month-end close at least 15% below average cost per share including fees. New names are bought first with F5's budget; top-ups and resizes use the remaining cash. A name sold by A4 is not re-bought in the same rebalance.

| Variant | Period | CAGR % | TDEX CAGR % | MDD % | Positions | Add/trim orders | Turnover x/yr | Max single-name weight % | Top-3 P/L share % |
|---|---|---|---|---|---|---|---|---|---|
| A0 F5 baseline | Full 2015-2025 | 7.80 | 2.52 | 43.35 | 103 | 0 | 1.74 | 16.35 | 42.25 |
| A0 F5 baseline | 2015-2020 | 8.36 | 1.75 | 43.35 | 57 | 0 | 1.66 | 16.35 | 48.24 |
| A0 F5 baseline | 2021-2025 | 5.50 | 3.19 | 19.94 | 56 | 0 | 1.98 | 15.19 | 66.47 |
| A0 F5 baseline | Holdout 2011-04 to 2014 | 24.95 | 9.68 | 17.08 | 36 | 0 | 1.62 | 24.88 | 53.84 |
| A1 top up to 10% once when down 15% | Full 2015-2025 | 8.31 | 2.52 | 45.81 | 103 | 25 | 1.78 | 17.54 | 42.73 |
| A1 top up to 10% once when down 15% | 2015-2020 | 8.76 | 1.75 | 45.81 | 57 | 15 | 1.71 | 17.54 | 48.75 |
| A1 top up to 10% once when down 15% | 2021-2025 | 5.94 | 3.19 | 21.38 | 55 | 7 | 1.99 | 15.53 | 67.42 |
| A1 top up to 10% once when down 15% | Holdout 2011-04 to 2014 | 25.79 | 9.68 | 16.33 | 36 | 3 | 1.66 | 24.86 | 52.62 |
| A2 add up to 15% once when down 15% | Full 2015-2025 | 8.71 | 2.52 | 47.97 | 102 | 19 | 1.71 | 22.40 | 44.99 |
| A2 add up to 15% once when down 15% | 2015-2020 | 9.07 | 1.75 | 47.97 | 57 | 13 | 1.69 | 22.40 | 52.52 |
| A2 add up to 15% once when down 15% | 2021-2025 | 5.98 | 3.19 | 20.74 | 56 | 7 | 2.02 | 16.17 | 63.57 |
| A2 add up to 15% once when down 15% | Holdout 2011-04 to 2014 | 26.14 | 9.68 | 16.25 | 36 | 3 | 1.68 | 24.86 | 51.68 |
| A3 resize every kept name to 10% monthly | Full 2015-2025 | 8.35 | 2.52 | 46.14 | 103 | 815 | 2.18 | 13.28 | 40.32 |
| A3 resize every kept name to 10% monthly | 2015-2020 | 9.31 | 1.75 | 46.14 | 57 | 428 | 2.06 | 13.25 | 42.64 |
| A3 resize every kept name to 10% monthly | 2021-2025 | 5.89 | 3.19 | 21.86 | 56 | 302 | 2.37 | 12.96 | 60.12 |
| A3 resize every kept name to 10% monthly | Holdout 2011-04 to 2014 | 25.99 | 9.68 | 17.65 | 36 | 298 | 2.10 | 12.98 | 45.97 |
| A4 sell when down 15% (control) | Full 2015-2025 | 7.43 | 2.52 | 45.47 | 139 | 0 | 2.22 | 18.40 | 49.47 |
| A4 sell when down 15% (control) | 2015-2020 | 7.62 | 1.75 | 45.47 | 83 | 0 | 2.31 | 18.40 | 56.11 |
| A4 sell when down 15% (control) | 2021-2025 | 4.70 | 3.19 | 21.60 | 68 | 0 | 2.39 | 15.70 | 78.24 |
| A4 sell when down 15% (control) | Holdout 2011-04 to 2014 | 26.14 | 9.68 | 16.79 | 43 | 0 | 1.80 | 24.78 | 58.52 |

## Verdicts

Better than F5 only if CAGR beats A0 in 2015-2020, 2021-2025 and the holdout, with full-period max drawdown no worse than A0 + 5 points.

- A1 top up to 10% once when down 15%: PASS
- A2 add up to 15% once when down 15%: PASS
- A3 resize every kept name to 10% monthly: PASS
- A4 sell when down 15% (control): FAIL (not above A0 in 2015-2020; not above A0 in 2021-2025)

## Caveats

- The universe is today's SET50 members with history; delisted stocks are missing. Survivorship flatters averaging down most, because losers that never recovered are absent. A pass needs a delisting-inclusive check before it reaches the app.
- The holdout ranks fewer names (stocks listed later have no early prices).
