package apincer.mobile.tradings.data

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import apincer.mobile.tradings.domain.CoreSatellite
import apincer.mobile.tradings.domain.HighYieldList
import apincer.mobile.tradings.domain.HighYieldTracker
import apincer.mobile.tradings.domain.MarketLists
import apincer.mobile.tradings.domain.MomentumList
import java.time.LocalDate

/**
 * Ranking and forward record of the high dividend yield list, shared by the Advisor and the background
 * worker so the monthly record continues while the card is hidden. Network calls; run off the main thread.
 */
object HighYieldRecorder {
    const val LIST_KEY = "high_yield_list_cache"
    const val TRACK_KEY = "high_yield_forward_record"
    /** Forward record of the same list without commodity names (variant TR2), for comparison only. */
    const val TR2_TRACK_KEY = "high_yield_tr2_forward_record"
    private const val ATTEMPT_KEY = "high_yield_record_attempt"
    private const val TAG = "HighYieldRecorder"

    /** All current SET50 members with a yield, ranked by dividend yield from SET quotes. */
    fun rankAll(): List<MomentumList.Entry> = HighYieldList.rank(MarketLists.set50().associateWith { symbol ->
        runCatching { SetScraper.fetchStockInfo(symbol).dividendYield }.getOrNull()
    }, topN = Int.MAX_VALUE)

    fun saveList(prefs: SharedPreferences, today: LocalDate, ranked: List<MomentumList.Entry>) {
        prefs.edit { putString(LIST_KEY, MomentumList.toJson(today.toString(), ranked)) }
    }

    fun loadTrack(prefs: SharedPreferences, key: String = TRACK_KEY): List<HighYieldTracker.Snapshot> =
        HighYieldTracker.fromJson(prefs.getString(key, null))

    /**
     * Saves this month's snapshot of [top] (when given and none exists) and measures due horizons.
     * Returns the updated record, unchanged when nothing was due or TDEX has no price.
     */
    fun updateTrack(prefs: SharedPreferences, top: List<String>?, today: LocalDate,
                    key: String = TRACK_KEY): List<HighYieldTracker.Snapshot> {
        val core = CoreSatellite.CORE_SYMBOL
        var track = loadTrack(prefs, key)
        val needSnapshot = top != null && HighYieldTracker.needsSnapshot(track, today)
        val due = track.filter { HighYieldTracker.dueHorizons(it, today).isNotEmpty() }
        if (!needSnapshot && due.isEmpty()) return track
        val symbols = ((if (needSnapshot) top.orEmpty() else emptyList()) + due.flatMap { it.prices.keys } + core).distinct()
        val quotes = SetScraper.fetchBatchQuotes(symbols).filter { it.lastPrice > 0.0 }
            .associate { it.symbol.uppercase() to it.lastPrice }
        val tdexNow = quotes[core] ?: return track
        if (needSnapshot) HighYieldTracker.snapshot(today, top.orEmpty().associateWith { quotes[it] ?: Double.NaN }, tdexNow)
            ?.let { track = track + it }
        if (due.isNotEmpty()) {
            val dividends = (due.flatMap { it.prices.keys } + core).distinct()
                .associateWith { SetScraper.fetchDividendEvents(it, "2y") }
            track = track.map { snap ->
                val horizons = HighYieldTracker.dueHorizons(snap, today)
                val result = if (horizons.isEmpty()) null else HighYieldTracker.checkpoint(snap, today, quotes,
                    dividends, tdexNow, dividends[core].orEmpty())
                if (result == null) snap else snap.copy(checkpoints = snap.checkpoints + horizons.associateWith { result })
            }
        }
        prefs.edit { putString(key, HighYieldTracker.toJson(track)) }
        return track
    }

    /**
     * Worker entry: at most one attempt a day, and only when a snapshot or a checkpoint is due. Ranks the list
     * only when this month's snapshot is missing. Returns true when it did work.
     */
    fun runIfDue(prefs: SharedPreferences, today: LocalDate): Boolean {
        if (prefs.getString(ATTEMPT_KEY, null) == today.toString()) return false
        val keys = listOf(TRACK_KEY, TR2_TRACK_KEY)
        val needSnapshot = keys.any { HighYieldTracker.needsSnapshot(loadTrack(prefs, it), today) }
        val anyDue = keys.any { k -> loadTrack(prefs, k).any { HighYieldTracker.dueHorizons(it, today).isNotEmpty() } }
        if (!needSnapshot && !anyDue) return false
        prefs.edit { putString(ATTEMPT_KEY, today.toString()) }
        return runCatching {
            val all = if (needSnapshot) rankAll() else emptyList()
            if (all.isNotEmpty()) saveList(prefs, today, all.take(HighYieldList.TOP_N))
            updateTracks(prefs, all, today)
            true
        }.onFailure { Log.w(TAG, "Forward record update failed", it) }.getOrDefault(false)
    }

    /** Updates the F5 record and the TR2 comparison record from one full ranking (empty when not re-ranked). */
    fun updateTracks(prefs: SharedPreferences, all: List<MomentumList.Entry>, today: LocalDate): List<HighYieldTracker.Snapshot> {
        fun symbols(list: List<MomentumList.Entry>) = list.map { it.symbol.uppercase() }.takeIf { it.isNotEmpty() }
        runCatching { updateTrack(prefs, symbols(HighYieldList.withoutCommodities(all)), today, TR2_TRACK_KEY) }
            .onFailure { Log.w(TAG, "TR2 forward record update failed", it) }
        return updateTrack(prefs, symbols(all.take(HighYieldList.TOP_N)), today)
    }
}
