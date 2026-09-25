package apincer.mobile.tradings.ui

import apincer.mobile.tradings.data.PortfolioSnapshotEntity
import apincer.mobile.tradings.domain.TechnicalAnalysis
import org.junit.Assert.assertEquals
import org.junit.Test

class NavSnapshotOrderTest {
    @Test fun descendingDaoResultsBecomeChronologicalBeforeDrawdown() {
        val newestFirst = listOf(
            PortfolioSnapshotEntity("2026-09-25", 80.0, 100.0, 0.0),
            PortfolioSnapshotEntity("2026-09-24", 100.0, 100.0, 0.0)
        )
        val ordered = navSnapshotsInDateOrder(newestFirst)
        assertEquals("2026-09-24", ordered.first().date)
        assertEquals(20.0, TechnicalAnalysis.calculateMaxDrawdown(
            ordered.map { it.totalValue + it.cashBalance }).maxDrawdownPercent, 0.001)
    }
}
