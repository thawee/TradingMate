# Evaluating advisor outcomes

The software checks whether a saved plan is applied consistently. It does not establish that the strategy is profitable. Evaluate results from recommendations recorded before trades, with actual broker fills and fees.

## Forward paper record

1. Before changing thresholds, fix a strategy version and a future holdout period. Keep all eligible, rejected, and no-setup observations, not only winners.
2. At each daily decision, record the symbol, strategy version, observation date, benchmark date, market regime, eligibility reasons, proposed entry/stop/target, quantity, estimated fees, and whether the user accepted the plan. Preserve plan revisions.
3. Link every later buy, partial sale, full sale, and manual override to that accepted plan. Import broker fees and fill prices where available. A missing link remains unknown; do not infer it from the ticker alone.
4. Report sample size, period, historical-data coverage, and missing observations. Separate paper decisions from executed trades and calculate realized and still-open results separately.
5. Compare net outcome against the accepted risk and target. Show slippage, costs, reason for exit, and any divergence between the alert and the user's actual fill. Stratify by strategy and market regime only when sample sizes are stated.

The current local journal records plan acceptance, AI ranking, fills, and undo events. It does not yet connect an AI ranking to a later accepted plan or retain complete market snapshots for full advisor replay. Until those links exist, the journal supports auditing individual events but not a reliable AI hit rate. The in-app backtest is a separate technical-only daily-close replay.

Data quality is part of evaluation: keep quote, fundamental, stock-bar, benchmark-bar, and model observation dates distinct. Discard or mark missing any setup whose required evidence is unavailable or stale. The in-app 63-observation risk estimates and qualitative AI assessments are neither calibrated win probabilities nor guarantees of future profit.
