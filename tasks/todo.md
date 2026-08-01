# Feature Expansion Plan

- [x] **Task 1: Portfolio Asset & Sector Allocation Chart** <!-- id: 0 -->
  - [x] Build a custom canvas `AllocationPieChart` composable in `StockComponents.kt` with glass legend. <!-- id: 1 -->
  - [x] Add Asset Allocation (% Stocks vs % Cash) and Sector Concentration (% per sector) toggle tabs to `PortfolioScreen.kt`. <!-- id: 2 -->
  - [x] Verify compilation and rendering. <!-- id: 3 -->

- [x] **Task 2: CSV Exporter for Portfolio Holdings & Trade History** <!-- id: 4 -->
  - [x] Create `CsvExporter.kt` utility with file intent / Share Sheet launcher to export Holdings and Closed Trades to `.csv`. <!-- id: 5 -->
  - [x] Add "Export CSV" buttons in `PortfolioScreen.kt` and `TradeHistoryScreen.kt`. <!-- id: 6 -->
  - [x] Verify CSV generation. <!-- id: 7 -->

- [x] **Task 3: SET XD Dividend Calendar Timeline** <!-- id: 8 -->
  - [x] Extract upcoming XD dates from watchlist stocks and sort them chronologically in `DividendAdvisorScreen.kt`. <!-- id: 9 -->
  - [x] Add an "XD Calendar" tab / expandable timeline view with estimated dividend payouts per share & last buy dates. <!-- id: 10 -->
  - [x] Verify layout and calculation. <!-- id: 11 -->

- [x] **Task 4: Background Signal & Price Alert Monitor (`WorkManager`)** <!-- id: 12 -->
  - [x] Add `WorkManager` dependency and `SignalAlertWorker.kt` to periodically check prices/signals during market hours. <!-- id: 13 -->
  - [x] Send system notifications for BUY / SELL alerts. <!-- id: 14 -->
  - [x] Add notification toggle in `SettingsScreen.kt` & register periodic work. <!-- id: 15 -->
  - [x] Run `./gradlew assembleDebug` to verify end-to-end. <!-- id: 16 -->
