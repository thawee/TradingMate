# Short-Term Rules From the Literature

Rules fixed before the first run (tasks/todo.md, 2026-10-02). Universe: 48 SET50 stocks as of H1 2025 (tools/backtest/universe.txt), dividend-adjusted. Config: ฿1M start, 1% risk per trade, 15% single-stock cap, max 10 positions, 0.15% slippage per side, InnovestX fees (ATS), next-close fills. Benchmark: TDEX buy-and-hold.

| Rule | Period | CAGR % | TDEX CAGR % | Gap % | MDD % | TDEX MDD % | Trades | Trades/yr | Win % | Expectancy R | Avg calendar days held | Exposure % |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| R1 weekly reversal (5-day return <= -5%, hold 5 days) | Full 2015-2025 | -1.95 | 2.52 | -4.47 | 43.04 | 36.53 | 1307 | 118.99 | 48.58 | -0.02 | 8.13 | 36.30 |
| R1 weekly reversal (5-day return <= -5%, hold 5 days) | 2015-2020 | 1.96 | 1.75 | 0.21 | 35.76 | 36.53 | 646 | 107.94 | 51.70 | 0.01 | 8.11 | 33.15 |
| R1 weekly reversal (5-day return <= -5%, hold 5 days) | 2021-2025 | -6.97 | 3.19 | -10.16 | 42.76 | 26.55 | 657 | 131.78 | 44.44 | -0.07 | 8.12 | 39.71 |
| R2 dip in uptrend (RSI(2) < 10, > SMA200, exit > SMA5) | Full 2015-2025 | -8.76 | 2.52 | -11.28 | 65.14 | 36.53 | 2396 | 218.13 | 46.99 | -0.07 | 5.37 | 42.55 |
| R2 dip in uptrend (RSI(2) < 10, > SMA200, exit > SMA5) | 2015-2020 | -3.90 | 1.75 | -5.65 | 29.96 | 36.53 | 1302 | 217.55 | 49.69 | -0.04 | 5.14 | 40.62 |
| R2 dip in uptrend (RSI(2) < 10, > SMA200, exit > SMA5) | 2021-2025 | -14.80 | 3.19 | -17.99 | 58.48 | 26.55 | 1096 | 219.83 | 43.43 | -0.12 | 5.62 | 44.93 |
| R3 deeper dip (RSI(2) < 5, > SMA200 and SMA50, exit > SMA5) | Full 2015-2025 | -1.71 | 2.52 | -4.23 | 27.64 | 36.53 | 633 | 57.63 | 48.03 | -0.06 | 5.36 | 12.14 |
| R3 deeper dip (RSI(2) < 5, > SMA200 and SMA50, exit > SMA5) | 2015-2020 | 0.95 | 1.75 | -0.80 | 7.98 | 36.53 | 322 | 53.80 | 53.73 | 0.00 | 4.93 | 10.31 |
| R3 deeper dip (RSI(2) < 5, > SMA200 and SMA50, exit > SMA5) | 2021-2025 | -4.84 | 3.19 | -8.02 | 25.58 | 26.55 | 310 | 62.18 | 41.94 | -0.14 | 5.83 | 14.29 |
| R5a R1 entry if SET not bearish, exit +3% or 10 days | Full 2015-2025 | -3.06 | 2.52 | -5.58 | 32.07 | 36.53 | 544 | 49.53 | 53.86 | -0.07 | 9.75 | 19.19 |
| R5a R1 entry if SET not bearish, exit +3% or 10 days | 2015-2020 | -3.26 | 1.75 | -5.01 | 28.37 | 36.53 | 232 | 38.76 | 54.74 | -0.08 | 9.74 | 15.20 |
| R5a R1 entry if SET not bearish, exit +3% or 10 days | 2021-2025 | -3.15 | 3.19 | -6.33 | 23.39 | 26.55 | 302 | 60.57 | 52.98 | -0.07 | 9.76 | 23.79 |
| R5b R1 entry, exit +3% or 10 days (no SET filter) | Full 2015-2025 | -2.95 | 2.52 | -5.47 | 44.51 | 36.53 | 1319 | 120.08 | 57.01 | -0.02 | 9.11 | 41.73 |
| R5b R1 entry, exit +3% or 10 days (no SET filter) | 2015-2020 | -0.94 | 1.75 | -2.69 | 34.79 | 36.53 | 664 | 110.95 | 57.98 | -0.01 | 8.62 | 37.21 |
| R5b R1 entry, exit +3% or 10 days (no SET filter) | 2021-2025 | -6.74 | 3.19 | -9.92 | 46.85 | 26.55 | 645 | 129.37 | 55.35 | -0.05 | 9.63 | 46.91 |

## Exit reasons: R1 weekly reversal (5-day return <= -5%, hold 5 days) (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Time (5 days) | 1150 | 0.16 | 185.51 |
| Stop | 157 | -1.32 | -206.73 |

## Exit reasons: R2 dip in uptrend (RSI(2) < 10, > SMA200, exit > SMA5) (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Close > SMA5 | 2189 | 0.05 | 104.63 |
| Stop | 199 | -1.38 | -273.66 |
| Time (10 days) | 8 | -0.91 | -7.26 |

## Exit reasons: R3 deeper dip (RSI(2) < 5, > SMA200 and SMA50, exit > SMA5) (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Close > SMA5 | 582 | 0.04 | 23.21 |
| Stop | 47 | -1.29 | -60.74 |
| Time (10 days) | 4 | -0.79 | -3.16 |

## Exit reasons: R5a R1 entry if SET not bearish, exit +3% or 10 days (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Target (+3%) | 264 | 0.64 | 168.76 |
| Time (10 days) | 165 | -0.37 | -60.60 |
| Stop | 115 | -1.27 | -146.55 |

## Exit reasons: R5b R1 entry, exit +3% or 10 days (no SET filter) (full period)

| Exit reason | Trades | Avg R | Total R |
|---|---|---|---|
| Target (+3%) | 716 | 0.61 | 440.21 |
| Time (10 days) | 338 | -0.36 | -121.92 |
| Stop | 265 | -1.31 | -348.25 |

## Concentration and evidence gate (full period)

| Rule | Total ฿ | Top 3 symbols | Excl. top 3 ฿ | Verdict | Reasons |
|---|---|---|---|---|---|
| R1 weekly reversal (5-day return <= -5%, hold 5 days) | -191,645 | CPN 56,642, IVL 53,889, TU 50,457 | -352,633 | FAIL | Trails benchmark in 2021-2025 (-6.97% vs 3.19%); Without its top 3 symbols, P/L is ฿-352,633 of ฿-191,645 (need positive and ≥ 50%); Expectancy -0.02R is not positive |
| R2 dip in uptrend (RSI(2) < 10, > SMA200, exit > SMA5) | -635,288 | KTC 55,153, SAWAD 39,626, MEGA 13,229 | -743,296 | FAIL | Trails benchmark in 2015-2020 (-3.90% vs 1.75%); Trails benchmark in 2021-2025 (-14.80% vs 3.19%); Without its top 3 symbols, P/L is ฿-743,296 of ฿-635,288 (need positive and ≥ 50%); Expectancy -0.07R is not positive |
| R3 deeper dip (RSI(2) < 5, > SMA200 and SMA50, exit > SMA5) | -175,883 | KTC 22,519, SPRC 17,265, DELTA 16,950 | -232,618 | FAIL | Trails benchmark in 2015-2020 (0.95% vs 1.75%); Trails benchmark in 2021-2025 (-4.84% vs 3.19%); Without its top 3 symbols, P/L is ฿-232,618 of ฿-175,883 (need positive and ≥ 50%); Expectancy -0.06R is not positive |
| R5a R1 entry if SET not bearish, exit +3% or 10 days | -286,758 | DELTA 59,000, IVL 32,161, SCGP 21,812 | -399,731 | FAIL | Trails benchmark in 2015-2020 (-3.26% vs 1.75%); Trails benchmark in 2021-2025 (-3.15% vs 3.19%); Without its top 3 symbols, P/L is ฿-399,731 of ฿-286,758 (need positive and ≥ 50%); Expectancy -0.07R is not positive |
| R5b R1 entry, exit +3% or 10 days (no SET filter) | -261,585 | IVL 63,334, WHA 53,815, DELTA 53,171 | -431,905 | FAIL | Trails benchmark in 2015-2020 (-0.94% vs 1.75%); Trails benchmark in 2021-2025 (-6.74% vs 3.19%); Without its top 3 symbols, P/L is ฿-431,905 of ฿-261,585 (need positive and ≥ 50%); Expectancy -0.02R is not positive |

## R4 Turn of month on TDEX

Hold TDEX from the close of the 4th-last session of each month to the close of the 3rd session of the next; cash (0%) otherwise. Each round trip pays InnovestX fees (ATS) and 0.15% slippage per side.

| Period | Turn-of-month CAGR % | TDEX buy-and-hold CAGR % | Turn-of-month MDD % | TDEX MDD % | Round trips | Time in market % |
|---|---|---|---|---|---|---|
| Full 2015-2025 | -4.18 | 2.52 | 39.01 | 36.53 | 131 | 29.6 |
| 2015-2020 | -3.63 | 1.75 | 25.01 | 36.53 | 71 | 29.4 |
| 2021-2025 | -5.43 | 3.19 | 30.53 | 26.55 | 59 | 29.6 |

## Caveats

- Survivorship bias: today's SET50 applied to 2015-2025.
- The literature on short-term reversal used older data (Asian markets including Thailand, 1990s-2000s) and often ignored costs; these tests include Thai costs.
- Daily closes only; fills at the next close.
- R5 replays only the price part of the community method; its news check (why the stock fell) cannot be replayed.
