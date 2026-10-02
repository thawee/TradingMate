package apincer.mobile.tradings.domain

import apincer.mobile.tradings.domain.TaxFunds.Purchase
import apincer.mobile.tradings.domain.TaxFunds.Type
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TaxFundsTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun thirtyPercentOfIncomeCapsBothForModestIncome() {
        // ฿600,000 income: 30% = ฿180,000 for each fund type.
        val room = TaxFunds.room(listOf(Purchase(Type.THAIESG, 50_000.0, d("2026-03-01"))), 2026, 600_000.0)
        assertEquals(180_000.0, room[0].limit, 1e-9)
        assertEquals(130_000.0, room[0].remaining, 1e-9)
        assertEquals(180_000.0, room[1].limit, 1e-9)
    }

    @Test fun bahtCapsAndSharedRetirementGroupApplyForHighIncome() {
        // ฿3M income: 30% = ฿900,000, so ThaiESG hits ฿300,000; RMF shares ฿500,000 with ฿200,000 of provident fund.
        val room = TaxFunds.room(emptyList(), 2026, 3_000_000.0, otherRetirement = 200_000.0)
        assertEquals(300_000.0, room[0].limit, 1e-9)
        assertEquals(300_000.0, room[1].limit, 1e-9)
    }

    @Test fun onlyThisYearsPurchasesCountAndJsonRoundTrips() {
        val purchases = listOf(Purchase(Type.RMF, 40_000.0, d("2025-12-20")), Purchase(Type.RMF, 60_000.0, d("2026-06-01")))
        assertEquals(60_000.0, TaxFunds.room(purchases, 2026, 1_000_000.0)[1].bought, 1e-9)
        assertEquals(purchases, TaxFunds.fromJson(TaxFunds.toJson(purchases)))
        assertEquals(emptyList<Purchase>(), TaxFunds.fromJson("garbage"))
    }

    @Test fun taxSavedIsAmountTimesTopRate() {
        assertEquals(30_000.0, TaxFunds.taxSaved(150_000.0, 20.0), 1e-9)
    }
}
