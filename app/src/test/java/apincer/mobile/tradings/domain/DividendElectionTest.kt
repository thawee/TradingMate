package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DividendElectionTest {
    // ฿10,000 gross at 20% CIT: credit ฿2,500, WHT ฿1,000, assessable ฿12,500.
    @Test fun zeroBracketGetsCreditAndWithholdingBack() {
        assertEquals(3_500.0, TechnicalAnalysis.dividendElectionBenefit(10_000.0, marginalRate = 0.0), 1e-6)
    }

    @Test fun twentyFivePercentBracketStillGainsSlightly() {
        assertEquals(375.0, TechnicalAnalysis.dividendElectionBenefit(10_000.0, marginalRate = 25.0), 1e-6)
    }

    @Test fun thirtyPercentBracketShouldKeepWithholdingFinal() {
        assertTrue(TechnicalAnalysis.dividendElectionBenefit(10_000.0, marginalRate = 30.0) < 0.0)
    }

    @Test fun breakEvenIsTwentyEightPercentAtStandardCit() {
        assertEquals(28.0, TechnicalAnalysis.dividendElectionBreakEvenRate(), 1e-9)
        assertEquals(0.0, TechnicalAnalysis.dividendElectionBenefit(10_000.0, marginalRate = 28.0), 1e-6)
    }
}
