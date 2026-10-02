# Advisor Event Study (1-4 weeks)

Universe: SET50 as of H1 2025, frozen in tools/backtest/universe.txt (survivorship-biased), 2015-2025. Entry at next close; returns net of InnovestX fees and 0.15% slippage per side. Target = 52-week high, stop = suggested ATR stop; first touch within 20 sessions, stop assumed first when one bar spans both. Not replayed: fundamentals, NVDR flow, AI ranking.

| Variant | Period | Events | Mean 1w | Mean 2w | Mean 4w | Median 4w | Win 4w | Mean 4w vs TDEX | Target hit | Stop hit | Plan exit mean | Median target dist | Median R:R |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| All stock-days (baseline) | 2015-2025 | 113201 | -0.49% | -0.34% | -0.03% | -0.64% | 46.8% | -0.31% | 14.7% | 46.2% | -0.32% | +16.24% | 3.59 |
| All stock-days (baseline) | 2015-2020 | 58447 | -0.37% | -0.10% | +0.43% | -0.15% | 48.9% | +0.15% | 15.9% | 44.1% | -0.06% | +14.97% | 3.28 |
| All stock-days (baseline) | 2021-2025 | 54754 | -0.61% | -0.59% | -0.53% | -0.71% | 44.6% | -0.79% | 13.4% | 48.4% | -0.60% | +17.71% | 3.92 |
| App BUY signal | 2015-2025 | 2334 | -0.38% | -0.13% | -0.02% | -0.64% | 46.8% | -0.52% | 7.5% | 45.7% | -0.17% | +19.46% | 4.13 |
| App BUY signal | 2015-2020 | 1214 | -0.07% | +0.19% | +0.56% | +0.08% | 50.4% | +0.01% | 9.0% | 40.7% | +0.49% | +17.17% | 3.85 |
| App BUY signal | 2021-2025 | 1120 | -0.71% | -0.48% | -0.65% | -1.16% | 42.9% | -1.10% | 6.0% | 51.2% | -0.89% | +21.51% | 4.40 |
| BUY + weekly trend up + target above | 2015-2025 | 880 | -0.41% | -0.32% | -0.39% | -0.64% | 44.9% | -0.43% | 18.6% | 42.3% | -0.50% | +9.15% | 2.05 |
| BUY + weekly trend up + target above | 2015-2020 | 502 | -0.40% | -0.41% | -0.65% | -0.64% | 46.0% | -0.58% | 21.3% | 38.0% | -0.37% | +8.24% | 1.80 |
| BUY + weekly trend up + target above | 2021-2025 | 378 | -0.42% | -0.20% | -0.03% | -1.01% | 43.4% | -0.23% | 15.1% | 47.9% | -0.68% | +10.61% | 2.31 |
| Advisor replay (+ RS > 0) | 2015-2025 | 545 | -0.41% | -0.35% | -0.79% | -0.64% | 45.9% | -0.68% | 20.0% | 42.4% | -0.59% | +8.45% | 1.79 |
| Advisor replay (+ RS > 0) | 2015-2020 | 322 | -0.37% | -0.38% | -0.97% | -0.64% | 47.2% | -0.51% | 22.7% | 39.4% | -0.41% | +7.84% | 1.63 |
| Advisor replay (+ RS > 0) | 2021-2025 | 223 | -0.47% | -0.31% | -0.53% | -0.66% | 43.9% | -0.93% | 16.1% | 46.6% | -0.86% | +9.39% | 1.99 |
| Advisor replay + R:R >= 2 (shown plans) | 2015-2025 | 242 | -0.32% | -0.35% | -0.96% | -1.01% | 43.0% | -1.17% | 3.3% | 43.8% | -0.39% | +16.19% | 3.18 |
| Advisor replay + R:R >= 2 (shown plans) | 2015-2020 | 131 | -0.24% | -0.42% | -0.72% | -1.00% | 43.5% | -0.27% | 4.6% | 41.2% | -0.20% | +15.00% | 3.09 |
| Advisor replay + R:R >= 2 (shown plans) | 2021-2025 | 111 | -0.41% | -0.27% | -1.25% | -1.43% | 42.3% | -2.24% | 1.8% | 46.8% | -0.62% | +18.19% | 3.83 |

## Short-swing plan variants (20-session time exit)

Variants were fixed before the first run. Net return per trade after costs; vs TDEX = trade return minus TDEX over the same holding days.

| Plan | Period | Trades | Mean net | Median net | Win | Target | Stop | Time exit | Avg days | Mean vs TDEX |
|---|---|---|---|---|---|---|---|---|---|---|
| P0 current: 52w-high target, R:R >= 2 | 2015-2025 | 242 | -0.39% | -2.06% | 39.3% | 3.3% | 43.8% | 52.9% | 15.2 | -0.75% |
| P0 current: 52w-high target, R:R >= 2 | 2015-2020 | 131 | -0.20% | -1.28% | 41.2% | 4.6% | 41.2% | 54.2% | 15.4 | -0.30% |
| P0 current: 52w-high target, R:R >= 2 | 2021-2025 | 111 | -0.62% | -3.49% | 36.9% | 1.8% | 46.8% | 51.4% | 15.0 | -1.28% |
| P1 BUY candidates, 2R target | 2015-2025 | 547 | -0.27% | -1.95% | 42.0% | 20.1% | 43.1% | 36.7% | 13.6 | -0.50% |
| P1 BUY candidates, 2R target | 2015-2020 | 323 | -0.09% | -1.28% | 44.0% | 20.1% | 40.6% | 39.3% | 14.1 | -0.40% |
| P1 BUY candidates, 2R target | 2021-2025 | 224 | -0.52% | -3.24% | 39.3% | 20.1% | 46.9% | 33.0% | 12.8 | -0.64% |
| P2 BUY candidates, 20-day swing-high target | 2015-2025 | 222 | -0.10% | -0.20% | 48.2% | 34.2% | 37.4% | 28.4% | 12.1 | -0.31% |
| P2 BUY candidates, 20-day swing-high target | 2015-2020 | 116 | +0.20% | +0.31% | 52.6% | 37.1% | 34.5% | 28.4% | 12.1 | +0.05% |
| P2 BUY candidates, 20-day swing-high target | 2021-2025 | 106 | -0.43% | -1.76% | 43.4% | 31.1% | 40.6% | 28.3% | 12.2 | -0.71% |
| P3 no BUY (weekly up, RS > 0, close > SMA50), 2R target | 2015-2025 | 39362 | -0.34% | -2.78% | 40.4% | 20.1% | 47.2% | 32.7% | 12.7 | -0.38% |
| P3 no BUY (weekly up, RS > 0, close > SMA50), 2R target | 2015-2020 | 21782 | +0.02% | -1.94% | 42.8% | 23.1% | 45.2% | 31.6% | 12.5 | -0.14% |
| P3 no BUY (weekly up, RS > 0, close > SMA50), 2R target | 2021-2025 | 17580 | -0.77% | -3.62% | 37.4% | 16.3% | 49.7% | 34.1% | 12.8 | -0.67% |
