# Advisor Event Study (1-4 weeks)

Universe: current SET50 (survivorship-biased), 2015-2025. Entry at next close; returns net of InnovestX fees and 0.15% slippage per side. Target = 52-week high, stop = suggested ATR stop; first touch within 20 sessions, stop assumed first when one bar spans both. Not replayed: fundamentals, NVDR flow, AI ranking.

| Variant | Period | Events | Mean 1w | Mean 2w | Mean 4w | Median 4w | Win 4w | Mean 4w vs TDEX | Target hit | Stop hit | Plan exit mean | Median target dist | Median R:R |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| All stock-days (baseline) | 2015-2025 | 113201 | -0.49% | -0.34% | -0.03% | -0.64% | 46.8% | -0.31% | 14.7% | 46.2% | -0.32% | +16.24% | 3.59 |
| All stock-days (baseline) | 2015-2020 | 58447 | -0.37% | -0.10% | +0.43% | -0.15% | 48.9% | +0.15% | 15.9% | 44.1% | -0.06% | +14.97% | 3.28 |
| All stock-days (baseline) | 2021-2025 | 54754 | -0.61% | -0.59% | -0.53% | -0.71% | 44.6% | -0.79% | 13.4% | 48.4% | -0.60% | +17.71% | 3.92 |
| App BUY signal | 2015-2025 | 11534 | -0.53% | -0.42% | -0.36% | -0.64% | 45.4% | -0.60% | 12.8% | 44.8% | -0.34% | +14.56% | 3.71 |
| App BUY signal | 2015-2020 | 5987 | -0.38% | -0.19% | +0.00% | -0.23% | 47.6% | -0.32% | 13.1% | 40.9% | +0.08% | +13.55% | 3.46 |
| App BUY signal | 2021-2025 | 5547 | -0.68% | -0.66% | -0.74% | -1.06% | 43.1% | -0.90% | 12.5% | 49.0% | -0.81% | +15.63% | 3.95 |
| BUY + weekly trend up + target above | 2015-2025 | 4981 | -0.50% | -0.45% | -0.32% | -0.64% | 44.7% | -0.33% | 27.2% | 40.8% | -0.51% | +6.71% | 1.82 |
| BUY + weekly trend up + target above | 2015-2020 | 2727 | -0.42% | -0.33% | -0.19% | -0.59% | 45.8% | -0.36% | 26.7% | 37.2% | -0.23% | +6.73% | 1.84 |
| BUY + weekly trend up + target above | 2021-2025 | 2254 | -0.60% | -0.61% | -0.47% | -0.89% | 43.3% | -0.29% | 27.8% | 45.2% | -0.85% | +6.67% | 1.80 |
| Advisor replay (+ RS > 0) | 2015-2025 | 2789 | -0.53% | -0.48% | -0.34% | -0.64% | 45.2% | -0.28% | 30.9% | 39.9% | -0.49% | +5.91% | 1.58 |
| Advisor replay (+ RS > 0) | 2015-2020 | 1474 | -0.42% | -0.27% | -0.11% | -0.27% | 46.9% | -0.10% | 31.1% | 35.6% | -0.16% | +6.04% | 1.60 |
| Advisor replay (+ RS > 0) | 2021-2025 | 1315 | -0.65% | -0.72% | -0.60% | -0.66% | 43.3% | -0.48% | 30.6% | 44.6% | -0.86% | +5.87% | 1.54 |
| Advisor replay + R:R >= 2 (shown plans) | 2015-2025 | 1032 | -0.57% | -0.62% | -0.74% | -1.25% | 41.0% | -0.69% | 7.0% | 47.7% | -0.55% | +12.36% | 3.06 |
| Advisor replay + R:R >= 2 (shown plans) | 2015-2020 | 526 | -0.32% | -0.24% | +0.04% | -0.64% | 45.6% | +0.36% | 8.0% | 39.4% | +0.06% | +12.18% | 2.95 |
| Advisor replay + R:R >= 2 (shown plans) | 2021-2025 | 506 | -0.83% | -1.01% | -1.55% | -1.90% | 36.2% | -1.78% | 5.9% | 56.3% | -1.19% | +12.59% | 3.23 |

## Short-swing plan variants (20-session time exit)

Variants were fixed before the first run. Net return per trade after costs; vs TDEX = trade return minus TDEX over the same holding days.

| Plan | Period | Trades | Mean net | Median net | Win | Target | Stop | Time exit | Avg days | Mean vs TDEX |
|---|---|---|---|---|---|---|---|---|---|---|
| P0 current: 52w-high target, R:R >= 2 | 2015-2025 | 1032 | -0.55% | -2.95% | 35.9% | 7.0% | 47.7% | 45.3% | 14.3 | -0.55% |
| P0 current: 52w-high target, R:R >= 2 | 2015-2020 | 526 | +0.06% | -1.28% | 42.8% | 8.0% | 39.4% | 52.7% | 15.2 | +0.09% |
| P0 current: 52w-high target, R:R >= 2 | 2021-2025 | 506 | -1.19% | -3.76% | 28.9% | 5.9% | 56.3% | 37.7% | 13.4 | -1.21% |
| P1 BUY candidates, 2R target | 2015-2025 | 2891 | -0.37% | -1.66% | 40.5% | 17.9% | 43.4% | 38.7% | 13.6 | -0.31% |
| P1 BUY candidates, 2R target | 2015-2020 | 1517 | -0.01% | -0.96% | 44.4% | 20.0% | 39.4% | 40.6% | 13.8 | -0.16% |
| P1 BUY candidates, 2R target | 2021-2025 | 1374 | -0.77% | -2.91% | 36.2% | 15.5% | 47.8% | 36.7% | 13.5 | -0.46% |
| P2 BUY candidates, 20-day swing-high target | 2015-2025 | 492 | -0.47% | -1.38% | 44.9% | 34.1% | 41.9% | 24.0% | 11.4 | -0.41% |
| P2 BUY candidates, 20-day swing-high target | 2015-2020 | 254 | -0.17% | -0.06% | 49.6% | 36.2% | 39.8% | 24.0% | 11.4 | -0.25% |
| P2 BUY candidates, 20-day swing-high target | 2021-2025 | 238 | -0.80% | -2.06% | 39.9% | 31.9% | 44.1% | 23.9% | 11.3 | -0.58% |
| P3 no BUY (weekly up, RS > 0, close > SMA50), 2R target | 2015-2025 | 39362 | -0.34% | -2.78% | 40.4% | 20.1% | 47.2% | 32.7% | 12.7 | -0.38% |
| P3 no BUY (weekly up, RS > 0, close > SMA50), 2R target | 2015-2020 | 21782 | +0.02% | -1.94% | 42.8% | 23.1% | 45.2% | 31.6% | 12.5 | -0.14% |
| P3 no BUY (weekly up, RS > 0, close > SMA50), 2R target | 2021-2025 | 17580 | -0.77% | -3.62% | 37.4% | 16.3% | 49.7% | 34.1% | 12.8 | -0.67% |
