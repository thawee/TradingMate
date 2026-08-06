# Changelog

All notable changes to the TradingMate project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3.0] - 2026-08-06

### Added
- **AI Bull & Bear Market Subagent (`[regime-manager]`)**: Integrated automatic market regime evaluation into all AI prompts (Stock Detail, Swing Advisor, Dividend Advisor) to adapt trade parameters based on prevailing market trends (Price vs. SMA 50/200 & MACD).
- **Available Cash Balance Integration**: AI prompts now read live portfolio cash balance (`cashBalance`) and instruct the `[risk-manager]` subagent to split available cash across top-ranked setups.
- **In-App Cash Allocation Badge**: Extended `GeminiClient.kt` JSON schema and updated `AiRecommendationCard` composable in `DividendAdvisorScreen.kt` to display exact position size in THB and estimated share counts directly in the app.

### Changed
- **AI Prompt Optimization**:
  - Added missing `Price: $lastPrice THB` to `StockScreen.kt` data sheet.
  - Standardized asymmetric **min 2.0:1 Risk/Reward ratio** across all AI prompts.
  - Added direct API fallback guidance for live web search when running in-app without web search grounding.
- **Version Bump**: Updated `versionCode` to `19` and `versionName` to `2.3.0` in `app/build.gradle.kts`.

### Fixed
- Fixed in-app AI recommendation card formatting where cash allocation and position sizing details were previously not displayed in direct API mode.

## [2.2.0] - 2026-08-05

### Added
- Asset Allocation Pie Chart in `PortfolioScreen.kt` with glass legend.
- CSV Exporter for portfolio holdings and trade history (`CsvExporter.kt`).
- SET XD Dividend Calendar timeline in `DividendAdvisorScreen.kt`.
- Background Signal & Price Alert Monitor (`SignalAlertWorker.kt` with `WorkManager`).

## [2.1.0] - 2026-08-01

### Added
- 5-Layer Stock DNA filtering engine (Qual, Val, Div, Mom, Sup).
- Direct in-app Google Gemini API client integration (`GeminiClient.kt`).
- Swing Playbook 3-step daily discipline tracker with floating step bar.
