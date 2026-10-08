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

## 7. A proxy series can lag one session through a null close
Yahoo listed TDEX's 2026-10-02 session with a null close; skipping null bars left the benchmark on 10-01 while stocks were on 10-02, so the date-equality freshness gate blocked nearly every list. When a gate compares dates from two sources, check both latest dates on the device database, not only bar counts, and treat a mismatch as a reason to refetch rather than wait out a cache.

## 8. Opening a WAL database copy with sqlite3 rewrites the copy
Querying a pulled `stock_database` with `sqlite3` checkpointed its WAL into the main file and emptied the WAL, so the "backup" no longer matched its recorded hashes (same data, different bytes). Keep an untouched backup and query a second copy; after restoring, delete the stale `-shm`. Also run each `adb shell run-as` command separately: a `sh -c` loop failed on quoting and the restore silently did not happen until checked.

## 9. An apostrophe in an Android string breaks the build
- Pattern: Python edited strings.xml to say "app's" and the build failed (aapt: Invalid unicode escape). The commit went in anyway because the build check was a grep inside a pipe that did not stop the chain.
- Rule: in res/values strings, escape apostrophes as \' or rephrase. Gate commits on the gradle exit code (`./gradlew ... && git commit`), not on a grep of its output.

## 10. uiautomator dump is not a timing probe
- Pattern: I timed the refresh spinner by polling `uiautomator dump` for a ProgressBar and reported 193 s -> 4 s. The dump fails while an animation runs and leaves the previous /sdcard/u.xml in place, and the wide Core vs Satellite LinearProgressIndicator also counts as a ProgressBar. The real numbers were 182 s -> 31 s (broken DNS) and 38 s -> 5 s (airplane mode).
- Rule: time app work from an app log line (start/finish with elapsed ms), or from screenshots. Check that a probe can see the change before trusting the number, and measure the baseline with the same probe.
