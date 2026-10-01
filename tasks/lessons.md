# Lessons Learned

## 1. `?: return false` in a Boolean `fun` is a silent blocker
Using `?: return false` on a nullable inside a boolean function causes the whole function to early-return false when data is simply missing (not negative). Prefer `?.let { condition } ?: true` for null-tolerant guards where null means "unknown, not disqualified".

## 2. `!isBullish` is wrong for "bearish gating" when NEUTRAL has `isBullish = true`
When an enum value (NEUTRAL) has `isBullish = true` for sizing/text purposes but should still apply bearish gating for candidate filtering, using `!isBullish` silently lets NEUTRAL bypass the gate. Use explicit `== BEARISH` for gating logic.

## 3. AI snapshot race conditions must surface to the user
When a background refresh fires between sending an AI request and receiving the response, plan snapshots change and all recommendations are silently discarded. Always show a snackbar or toast explaining why results are empty, otherwise users assume the AI found nothing.

## 4. Concentration limits: use cost basis, not market value
Using `lastPrice × qty` for concentration checks means limits can be silently breached (positions bought cheap look small) or falsely triggered (positions in drawdown look safe). Always use `cost × qty` for allocation rule enforcement.

## 5. Two-sentence disclaimers in `labelSmall` are invisible
Critical UX warnings (e.g., "AI has no live news") placed as faded small text below a primary action button are reliably ignored. Use a distinct `Surface` card with a colored background and icon to ensure the message is noticed.

## 6. Fallbacks must test sufficiency, not emptiness
Yahoo's `^SET.BK` returned one bar (today) instead of none, so an `ifEmpty` fallback to TDEX never fired and every downstream consumer (regime, RS, beta, candidate freshness gate) silently degraded. Gate fallbacks on the minimum data the consumers need (e.g. `size >= 120`), and verify on the installed build: an emulator snapshot can silently run an older APK.
