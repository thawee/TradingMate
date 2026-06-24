# Alert & Notification Flows

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    TWO ALERT SYSTEMS                             │
│                                                                  │
│  1. PUSH NOTIFICATIONS (Background Worker)                       │
│     - Time-based, fires to phone                                 │
│     - StockAlertWorker runs every 30 minutes                     │
│     - Deduplicated (per day / per week / per season)             │
│                                                                  │
│  2. IN-APP ALERTS (Reactive StateFlow)                           │
│     - Data-based, shows in UI                                    │
│     - alertRoutineState computed reactively                      │
│     - Updates in real-time                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. Push Notification System

### StockAlertWorker (Background)

**Location:** `util/StockAlertWorker.kt`
**Schedule:** Every **30 minutes**, weekdays only (Asia/Bangkok)
**Market hours:** Morning `10:00–12:29`, Lunch `12:30–14:30`, Afternoon `14:31–16:30`
**Skip condition:** Market closed (skips stock scan; time-based alerts still checked)

#### Flow

```
Every 30 min on weekdays
│
├─ 1. Afternoon Entry Window (15:30–16:15, market not CLOSED)
│     Guard: also skipped on public holidays (market CLOSED check)
│     └─ Fire once/day: "🔔 Afternoon Swing Entry Window"
│         "Market close approaching! Scan the Advisor
│          for new daily Swing & Gap candidates."
│
├─ 2. Dividend Season (Jan/Jun, 09:00–17:00)
│     └─ Fire ONCE per season (keyed by year-month, not date):
│         "💰 Dividend Accumulation Season"
│         Jan → "Start researching for April/May XD payouts"
│         Jun → "Start researching for Aug/Sep XD payouts"
│
├─ 3. Market CLOSED? → skip stock scan and return
│     (includes public holidays)
│
├─ 4. For each stock in watchlist:
│     │
│     ├─ Fetch latest price + indicators
│     │
│     ├─ Calculate signal (BUY/POTENTIAL/SELL/NEUTRAL)
│     │
│     ├─ Signal state changed? → showSignalNotification()
│     │   "SELL: KBANK - RSI overbought"
│     │
│     ├─ Active SELL signal? → showSellReminderNotification()
│     │   "⚠️ SELL: KBANK - Take Profit"
│     │
│     ├─ Upcoming XD date (next 7 days)? → showXdAlertNotification()
│     │   "🔔 Upcoming XD: KBANK"
│     │
│     ├─ DIVIDEND purpose + yield ≥ 5% + ROE ≥ 15%?
│     │   → showDividendYieldOpportunityNotification()
│     │   "💰 Yield Opportunity: PTT"
│     │   "Yield 6.2% at ฿33.50 — strong ROE 18.3%."
│     │   Dedup: once per ISO week per stock
│     │
│     └─ Swing exit conditions met?
│         (gain≥10%, loss≤-5%, RSI≥65, SELL signal)
│         └─ hasActiveSwingSellAlert = true
│
└─ 5. Morning Swing Exit (10:00–11:00)
      └─ If hasActiveSwingSellAlert → showPrimeTimeNotification()
          "☀️ Morning Swing Exit Window"
          "Markets are open! Check your active swing
           positions for Sell or Stop Loss alerts."
```

### Notification Types

| Type | Method | Trigger | Priority | Dedup |
|---|---|---|---|---|
| Signal Change | `showSignalNotification` | Signal type changed | DEFAULT | Per stock |
| Sell Reminder | `showSellReminderNotification` | Active SELL on portfolio | HIGH | Per stock |
| XD Alert | `showXdAlertNotification` | XD date within 7 days | DEFAULT | Per stock+date |
| **Yield Opportunity** | `showDividendYieldOpportunityNotification` | **DIVIDEND stock yield ≥ 5% + ROE ≥ 15% (any month)** | DEFAULT | **Per stock per ISO week** |
| Morning Exit | `showPrimeTimeNotification` | Swing alerts + 10:00–11:00 | DEFAULT | Per day |
| Afternoon Entry | `showPrimeTimeNotification` | 15:30–16:15, market open | DEFAULT | Per day |
| Dividend Season | `showDividendSeasonNotification` | Jan/Jun, 09:00–17:00 | DEFAULT | **Once per season (year-month key)** |

---

## 2. In-App Alert System

### AlertRoutineState (Reactive)

**Location:** `ui/StockViewModel.kt`
**Flow type:** Kotlin StateFlow + combine()

#### Data Sources

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ _playbookMode│  │ watchlistInfo│  │  _checklist   │
│  (SWING/     │  │ (all stocks  │  │ (daily/weekly │
│   DIVIDEND)  │  │  + signals)  │  │  AI status)   │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         ▼
              combine(_playbookMode,
                      watchlistInfo,
                      _checklist)
                         │
                         ▼
              ┌─────────────────────────┐
              │   alertRoutineState     │
              │   : StateFlow<Alert     │
              │     RoutineState>       │
              └─────────────────────────┘
```

#### AlertRoutineState Structure

```kotlin
data class AlertRoutineState(
    val playbookMode: PlaybookMode,        // SWING or DIVIDEND
    val swingSellAlerts: List<SellAlertData>,
    val dividendSellAlerts: List<SellAlertData>,
    val combinedSwingPlays: List<StockWatchlistInfo>,
    val dividendPlays: List<StockWatchlistInfo>,
    val portfolioItems: List<StockWatchlistInfo>,
    val checklist: ChecklistEntity
) {
    // Computed properties
    val activeAlerts get() = if (SWING) swingSellAlerts else dividendSellAlerts
    val exitAlertsCount get() = activeAlerts.size
    val activeCandidatesCount get() = if (SWING) combinedSwingPlays.size else dividendPlays.size
    val step1Done get() = if (SWING) checklist.swingDailyDone else checklist.divWeeklyDone
    val step2Done get() = if (SWING) checklist.swingWeeklyDone else checklist.divMonthlyDone
    val step3Done get() = if (SWING) checklist.swingAiDone else checklist.divAiDone
}
```

#### Sell Alert Logic

```
For each portfolio item (quantity > 0):
│
├─ DIVIDEND purpose:
│   ├─ ROE < 15% → "Fundamentals Break (ROE < 15%)"
│   │              → adds to dividendSellAlerts
│   │
│   └─ Yield < 3% → "Yield Dropped (< 3%) (Transition to Swing)"
│                    → adds to swingSellAlerts
│                    → applies swing logic below
│
└─ SWING logic (or DIVIDEND transitioning):
    ├─ netProfit ≥ 10%  → "Take Profit (Gain ≥ 10%)"
    ├─ netProfit ≤ -5%  → "Stop Loss (Loss ≤ -5%)"
    ├─ rsi ≥ 65         → "Overbought (RSI ≥ 65)"
    └─ signal = SELL    → "[signal reason]"
```

#### Candidate Screening

```
dividendPlays:
  - dividendYield ≥ 5% AND roe > 15%
  - Sorted by signal priority, then yield

swingPlays:
  - (roe > 15% OR pe in 0.1-15.0 & pbv in 0.1-1.0)
  - AND (macdHist > 0 OR signal=BUY/POTENTIAL OR rsi < 35)
  - Sorted by signal priority, then rsi, then roe

gapPlays:
  - percentChange ≥ 4%
  - AND (roe > 15% OR netProfitMargin > 10%)
  - Sorted by percentChange

combinedSwingPlays:
  - swingPlays + gapPlays
  - Deduplicated by symbol
  - Sorted by signal, percentChange, rsi
```

---

## 3. Checklist System

### Step Summary (SWING mode - new order)

| Step | Section | Purpose | Push Connection |
|---|---|---|---|
| **Step 1** | 🤖 Ask AI | Copy AI prompt to clipboard | None (auto-mark on copy) |
| **Step 2** | 🚨 Check Exits | Shows sell alerts | Push at 10-11 AM |
| **Step 3** | 🔍 Scan Setups | Shows candidate stocks | Push at 15:30-16:30 |

**DIVIDEND mode**: No checklist — purely informational

### New Flow (AI first for easy access)

```
┌─────────────────────────────────────┐
│  🤖 AI Master Prompts (Step 1)      │  ← Always at top
│  ┌─────────────────────────────┐    │
│  │ Swing Trade AI Prompt       │    │  ← Tap to copy
│  │ [Copy to clipboard]         │    │     auto-marks Step 1
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  🚨 Check Exits (Step 2)            │  ← Sell alerts
│  • KBANK: Take Profit (Gain ≥ 10%)  │
│  • SCB: Overbought (RSI ≥ 65)       │
│  [ ] checkbox                        │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  🔍 Scan Setups (Step 3)            │  ← Candidates
│  • GAP: 5 setups                    │
│  • MOM: 3 setups                    │
│  [ ] checkbox                        │
└─────────────────────────────────────┘
```

### States

```
SWING mode (has tracking):
  step1: swingAiDone       (auto-marked on copy)
  step2: swingDailyDone    (resets daily)
  step3: swingWeeklyDone   (resets daily)

DIVIDEND mode: No checklist states (removed)
```

### Auto-Mark Step 1 (SWING only)

```kotlin
LaunchedEffect(activeAlerts.size, playbookMode) {
    if (activeAlerts.isEmpty() && !step1Done) {
        viewModel.markAlertRoutineStepDone(1)
    }
}
```

If no sell alerts exist → Step 1 auto-checks itself (SWING mode only).

### Step 2: Scan Setups (SWING) / Find Dividend Stars (DIVIDEND)

**SWING mode:**
- Shows `combinedSwingPlays` (swing + gap plays)
- **Afternoon badge**: Shows "📢" when afternoon scan is available
- Count shown in wizard bar: "🔍 Setups (5) 📢"
- User reviews candidates manually
- User checks checkbox when done → `swingWeeklyDone`

**DIVIDEND mode:**
- Shows `dividendPlays` (yield ≥ 5% & quality)
- **No badge** — purely manual
- User reviews candidates manually
- User checks checkbox when done → `divMonthlyDone`

#### Afternoon Scan Connection (SWING only)

```
15:30-16:30 PM notification fires
    │
    ├─ Sets flag: afternoon_scan_available_$date = true
    │
    ▼
Step 2 shows badge: "🔍 Scan Setups 📢"
    │
    ├─ User taps checkbox
    │   ├─ toggleAlertRoutineStep(2)
    │   └─ clearAfternoonScanFlag()
    │
    ▼
Badge disappears
```

### Step 3: Ask AI (Both modes - manual only)

**SWING mode:**
- Shows `AiCopilotCard` with swing trade prompt builder
- User copies prompt → pastes into ChatGPT/Gemini
- User checks checkbox when done → `swingAiDone`
- **No push notification triggers this**

**DIVIDEND mode:**
- Shows `AiCopilotCard` with dividend analysis prompt builder
- User copies prompt → pastes into ChatGPT/Gemini
- User checks checkbox when done → `divAiDone`
- **No push notification triggers this**

### Manual Toggle

```kotlin
fun toggleAlertRoutineStep(step: Int)
// Toggles: step 1, 2, or 3 checkbox

fun markAlertRoutineStepDone(step: Int)
// Sets step to done (used by auto-mark)
```

---

## 4. Wizard Step Bar (SWING mode only)

Shows in bottom bar for SWING mode, displays counts:

```
Step 1: 🤖 Ask AI         ← auto-marked on copy
Step 2: 🚨 Exits (3)      ← alertsCount (alert-related)
Step 3: 🔍 Setups (5) 📢  ← candidatesCount + afternoon badge

Progress: ████░░ 2/3
Next → 🚨 Exits
```

**DIVIDEND mode**: No wizard step bar — layout is different (AI card at top, informational only).

---

## 5. Push → In-App Connection

| Push Notification | Mode | Related Step | Connection |
|---|---|---|---|
| Morning (10:00–11:00) | SWING only | Step 1: Check Exits | Same conditions (gain≥10%, loss≤-5%, RSI≥65) |
| Afternoon (15:30–16:15) | SWING only | Step 2: Scan Setups | Sets badge flag, cleared on checkbox |
| Yield Opportunity | DIVIDEND | Any step | Opens stock directly via OPEN_SYMBOL intent |

**SWING mode**: Push notifications nudge user to open app and complete steps  
**DIVIDEND mode**: Year-round yield opportunity alerts fire independently of the checklist

| Mode | Push Notifications | Checklist | Badge |
|---|---|---|---|
| SWING | ✅ Morning + Afternoon | ✅ 3 steps with tracking | ✅ Step 2 afternoon badge |
| DIVIDEND | ✅ Yield Opportunity (any month) + Season (Jan/Jun) | ❌ None | ❌ None |

---

## 6. Key Files

| File | Purpose |
|---|---|
| `util/StockAlertWorker.kt` | Background push notifications |
| `util/NotificationHelper.kt` | Notification builders |
| `ui/StockViewModel.kt` | AlertRoutineState + reactive logic |
| `ui/DividendAdvisorScreen.kt` | UI for alerts + checklist |
| `domain/TechnicalAnalysis.kt` | Signal calculation |
| `data/RoomModels.kt` | ChecklistEntity persistence |
