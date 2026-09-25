package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRecommendationValidatorTest {
    private val plan = AiCandidatePlan("PTT", 100.0, 95.0, 110.0, 10_000.0, 100, "snapshot-1")
    private fun recommendation(symbol: String) = AiRecommendation(symbol, "SWING", "99", "999", "1", 99,
        "฿1,000,000", "Model explanation")

    @Test fun unknownAndStaleCandidatesAreDiscarded() {
        val result = AiAnalysisResult("Summary", listOf(recommendation("XYZ"), recommendation("PTT")))
        assertEquals(1, AiRecommendationValidator.validate(result, mapOf("PTT" to plan), mapOf("PTT" to plan)).recommendations.size)
        assertTrue(AiRecommendationValidator.validate(result, mapOf("PTT" to plan),
            mapOf("PTT" to plan.copy(snapshotId = "snapshot-2"))).recommendations.isEmpty())
        assertEquals("", AiRecommendationValidator.validate(result, mapOf("PTT" to plan),
            mapOf("PTT" to plan.copy(snapshotId = "snapshot-2"))).executiveSummary)
    }

    @Test fun localLevelsReplaceModelInventedPricesAndAllocation() {
        val rec = AiRecommendationValidator.validate(AiAnalysisResult("", listOf(recommendation("PTT"))),
            mapOf("PTT" to plan), mapOf("PTT" to plan)).recommendations.single()
        assertTrue(rec.targetProfit.contains("110.00"))
        assertTrue(rec.stopLoss.contains("95.00"))
        assertTrue(rec.cashAllocation.contains("10,000"))
    }
}
