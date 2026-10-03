package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HighYieldTrackerTest {
    private fun d(s: String) = LocalDate.parse(s)
    private val snap = HighYieldTracker.snapshot(d("2026-10-03"), mapOf("AAA" to 10.0, "BBB" to 20.0), 10.0)!!

    @Test fun oneSnapshotPerMonth() {
        assertTrue(HighYieldTracker.needsSnapshot(emptyList(), d("2026-10-03")))
        assertFalse(HighYieldTracker.needsSnapshot(listOf(snap), d("2026-10-29")))
        assertTrue(HighYieldTracker.needsSnapshot(listOf(snap), d("2026-11-02")))
        assertNull(HighYieldTracker.snapshot(d("2026-10-03"), mapOf("AAA" to 10.0), Double.NaN))
    }

    @Test fun horizonsBecomeDueByAge() {
        assertEquals(emptyList<Int>(), HighYieldTracker.dueHorizons(snap, d("2026-11-01")))
        assertEquals(listOf(30), HighYieldTracker.dueHorizons(snap, d("2026-11-02")))
        assertEquals(listOf(30, 91), HighYieldTracker.dueHorizons(snap, d("2027-01-02")))
        val measured = snap.copy(checkpoints = mapOf(30 to HighYieldTracker.Checkpoint("2026-11-02", 0.0, 0.0, 2, 2)))
        assertEquals(listOf(91), HighYieldTracker.dueHorizons(measured, d("2027-01-02")))
    }

    @Test fun totalReturnIncludesDividendsInTheWindowOnly() {
        // AAA: 10 -> 10.5 plus a 0.5 dividend = +10%. BBB: 20 -> 19, its dividend before the snapshot is ignored = -5%.
        val cp = HighYieldTracker.checkpoint(snap, d("2026-11-02"),
            prices = mapOf("AAA" to 10.5, "BBB" to 19.0),
            dividends = mapOf("AAA" to listOf(d("2026-10-20") to 0.5), "BBB" to listOf(d("2026-09-15") to 1.0)),
            tdexNow = 10.2, tdexDividends = emptyList())!!
        assertEquals(2.5, cp.listReturnPercent, 1e-9)
        assertEquals(2.0, cp.tdexReturnPercent, 1e-9)
        assertEquals(2, cp.priced)
    }

    @Test fun unpricedNamesAreCountedNotHidden() {
        val cp = HighYieldTracker.checkpoint(snap, d("2026-11-02"), mapOf("AAA" to 11.0), emptyMap(), 10.0, emptyList())!!
        assertEquals(1, cp.priced)
        assertEquals(2, cp.total)
        assertNull(HighYieldTracker.checkpoint(snap, d("2026-11-02"), emptyMap(), emptyMap(), 10.0, emptyList()))
    }

    @Test fun summaryAndJsonRoundTrip() {
        val a = snap.copy(checkpoints = mapOf(30 to HighYieldTracker.Checkpoint("2026-11-02", 3.0, 1.0, 2, 2)))
        val b = snap.copy(month = "2026-11", date = "2026-11-02",
            checkpoints = mapOf(30 to HighYieldTracker.Checkpoint("2026-12-02", -1.0, 2.0, 2, 2)))
        val s = HighYieldTracker.summary(listOf(a, b)).single()
        assertEquals(30, s.days); assertEquals(2, s.measured); assertEquals(1, s.ahead)
        assertEquals(1.0, s.avgListPercent, 1e-9); assertEquals(1.5, s.avgTdexPercent, 1e-9)
        assertEquals(listOf(a, b), HighYieldTracker.fromJson(HighYieldTracker.toJson(listOf(a, b))))
        assertEquals(emptyList<HighYieldTracker.Snapshot>(), HighYieldTracker.fromJson("not json"))
    }

    @Test fun lateCheckpointsAreLeftOutOfTheSummary() {
        // 30-day horizon measured 95 days after the snapshot (app not opened): not a 30-day result.
        val late = snap.copy(checkpoints = mapOf(30 to HighYieldTracker.Checkpoint("2027-01-06", 9.0, 1.0, 2, 2)))
        assertEquals(emptyList<HighYieldTracker.HorizonSummary>(), HighYieldTracker.summary(listOf(late)))
        val onTime = snap.copy(checkpoints = mapOf(30 to HighYieldTracker.Checkpoint("2026-11-17", 9.0, 1.0, 2, 2)))
        assertEquals(1, HighYieldTracker.summary(listOf(onTime)).single().measured)
    }
}
