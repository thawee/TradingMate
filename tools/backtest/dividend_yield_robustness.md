# High Dividend Yield: Robustness and Holdout

Checks fixed before the first run (tasks/todo.md, 2026-10-02). Base rule F5: each month hold the 10 stocks with the highest reported dividend yield known at the time (50-day lag after Q1-Q3, 90 days after Q4), 10% target each; InnovestX fees (ATS), 0.15% slippage per side, 100-share lots.

## Robustness (2015-2025)

| Check | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Closed positions | Avg net return / position % |
|---|---|---|---|---|---|---|---|---|
| F5 base: top 10, monthly | Full 2015-2025 | 7.80 | 2.52 | 5.28 | 43.35 | 36.53 | 93 | 7.40 |
| F5 base: top 10, monthly | 2015-2020 | 8.36 | 1.75 | 6.61 | 43.35 | 36.53 | 47 | 8.84 |
| F5 base: top 10, monthly | 2021-2025 | 5.50 | 3.19 | 2.31 | 19.94 | 26.55 | 46 | 3.56 |
| D1 top 5 | Full 2015-2025 | 6.34 | 2.52 | 3.82 | 48.81 | 36.53 | 59 | 6.97 |
| D1 top 5 | 2015-2020 | 6.60 | 1.75 | 4.85 | 48.81 | 36.53 | 26 | 3.82 |
| D1 top 5 | 2021-2025 | 4.85 | 3.19 | 1.67 | 28.62 | 26.55 | 33 | 3.41 |
| D2 top 15 | Full 2015-2025 | 8.04 | 2.52 | 5.52 | 36.97 | 36.53 | 128 | 9.49 |
| D2 top 15 | 2015-2020 | 9.92 | 1.75 | 8.17 | 36.97 | 36.53 | 61 | 13.98 |
| D2 top 15 | 2021-2025 | 5.20 | 3.19 | 2.01 | 18.92 | 26.55 | 67 | 4.80 |
| D3 quarterly rebalance | Full 2015-2025 | 8.62 | 2.52 | 6.10 | 43.76 | 36.53 | 86 | 10.14 |
| D3 quarterly rebalance | 2015-2020 | 9.48 | 1.75 | 7.73 | 43.76 | 36.53 | 41 | 9.22 |
| D3 quarterly rebalance | 2021-2025 | 4.42 | 3.19 | 1.24 | 22.70 | 26.55 | 40 | 5.14 |
| D4 120-day lag | Full 2015-2025 | 9.61 | 2.52 | 7.09 | 42.22 | 36.53 | 89 | 10.85 |
| D4 120-day lag | 2015-2020 | 12.17 | 1.75 | 10.42 | 42.22 | 36.53 | 44 | 11.92 |
| D4 120-day lag | 2021-2025 | 5.47 | 3.19 | 2.29 | 22.76 | 26.55 | 42 | 5.36 |

- F5 base: top 10, monthly: beats TDEX in both sub-periods
- D1 top 5: beats TDEX in both sub-periods
- D2 top 15: beats TDEX in both sub-periods
- D3 quarterly rebalance: beats TDEX in both sub-periods
- D4 120-day lag: beats TDEX in both sub-periods

## Holdout 2011-04-01 to 2014-12-31

Prices from tools/backtest/data_holdout (2008 onward); same rule and universe. This period was not used to design or choose the rule.

| Rule | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Closed positions | Avg net return / position % |
|---|---|---|---|---|---|---|---|
| F5 top 10, monthly | 24.95 | 9.68 | 15.26 | 17.08 | 25.99 | 26 | 28.15 |

Top three symbols by P/L in the holdout: JMART 257,858, ADVANC 229,022, DELTA 215,258 of 1,304,217 total.

Holdout verdict: beats TDEX buy-and-hold.

## Caveats

- Survivorship bias is stronger in the holdout: today's SET50 applied to 2011-2014.
- Several stocks have no price history before their listing (e.g. BGRIM 2017), so the holdout ranks fewer names.
