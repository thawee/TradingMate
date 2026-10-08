package apincer.mobile.tradings.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Shapes trimmed from SET company-highlight and corporate-action responses (2026-10-08). */
class AccountingInputsParserTest {
    @Test fun keepsFullFiscalYearsOnly() {
        val rows = JSONArray("""[
            {"quarter":"Q9","year":2021,"netProfit":2783109.34,"netOperating":-1225480.74,"netFinancing":1560915.26},
            {"quarter":"Q9","year":2022,"netProfit":-6612131.42,"netOperating":null,"netFinancing":10719496.45},
            {"quarter":"6M","year":2026,"netProfit":105808.0,"netOperating":3690008.0,"netFinancing":-2991217.0}]""")
        val years = SetScraper.parseFiscalYears(rows)
        assertEquals(listOf(2021, 2022), years.map { it.year })
        assertEquals(-1225480.74, years[0].operatingCash!!, 1e-6)
        assertNull(years[1].operatingCash)
    }

    @Test fun keepsRightsAndWarrantsWithTerms() {
        val rows = JSONArray("""[
            {"caType":"XR","xdate":"2024-08-15T00:00:00+07:00","ratio":"4.5 : 1","price":4.5},
            {"caType":"XM","xdate":"2024-08-15T00:00:00+07:00","remark":""},
            {"caType":"XW","xdate":"2021-07-23T00:00:00+07:00","ratio":"20 : 1"},
            {"caType":"XD","xdate":"2024-05-02T00:00:00+07:00"}]""")
        val actions = SetScraper.parseCapitalActions(rows)
        assertEquals(listOf("XR", "XW"), actions.map { it.type })
        assertEquals(LocalDate.parse("2024-08-15"), actions[0].xDate)
        assertEquals(4.5, actions[0].price!!, 0.0)
        assertEquals("20 : 1", actions[1].ratio)
        assertNull(actions[1].price)
    }
}
