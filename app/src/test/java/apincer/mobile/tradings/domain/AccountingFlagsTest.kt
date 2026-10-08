package apincer.mobile.tradings.domain

import apincer.mobile.tradings.domain.AccountingFlags.CapitalAction
import apincer.mobile.tradings.domain.AccountingFlags.FiscalYear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Figures are SET company-highlight values in thousand baht. */
class AccountingFlagsTest {
    private val today = LocalDate.parse("2026-10-08")

    @Test fun starkFiscal2021ProfitWithoutCash() {
        val years = listOf(
            FiscalYear(2020, 1_608_660.24, 3_003_555.34, 3_272_853.67),
            FiscalYear(2021, 2_783_109.34, -1_225_480.74, 1_560_915.26))
        assertEquals(listOf("Profit without cash in FY2021: net profit ฿2,783m but operating cash flow ฿-1,225m, " +
            "covered by ฿1,561m of new financing"),
            AccountingFlags.check(years, emptyList(), industry = "INDUS", sector = "PETRO", today = today))
    }

    @Test fun eaFiscal2023IsFlaggedOnlyWhileItIsTheLatestYear() {
        val y2023 = FiscalYear(2023, 7_606_172.12, -456_559.4, 7_592_183.99)
        assertEquals(1, AccountingFlags.check(listOf(y2023), emptyList(), "RESOURC", "ENERG", today).size)
        val y2024 = FiscalYear(2024, -4_630_014.15, 7_864_824.16, -8_284_649.95)
        assertTrue(AccountingFlags.check(listOf(y2023, y2024), emptyList(), "RESOURC", "ENERG", today).isEmpty())
    }

    @Test fun healthyCashFlowIsNotFlagged() {
        val cpall = FiscalYear(2025, 28_206_100.41, 74_255_255.14, -38_369_963.28)
        assertTrue(AccountingFlags.check(listOf(cpall), emptyList(), "SERVICE", "COMM", today).isEmpty())
    }

    @Test fun lendersAreSkippedBecauseLendingIsAnOperatingOutflow() {
        val ktc = FiscalYear(2022, 7_000_000.0, -2_000_000.0, 3_000_000.0)
        assertTrue(AccountingFlags.check(listOf(ktc), emptyList(), "FINCIAL", "FIN", today).isEmpty())
    }

    @Test fun propertyDevelopersGetAContextNote() {
        val lh = FiscalYear(2024, 4_000_000.0, -1_000_000.0, 2_000_000.0)
        val line = AccountingFlags.check(listOf(lh), emptyList(), "PROPCON", "PROP", today).single()
        assertTrue(line, line.endsWith("(common when a developer buys land; check inventory)"))
    }

    @Test fun rightsAndWarrantsInTheLastTwelveMonths() {
        val xr = CapitalAction("XR", LocalDate.parse("2024-08-15"), "4.5 : 1", 4.5)
        assertEquals(listOf("Capital raise: rights offering (XR) on 2024-08-15, 4.5 : 1 at ฿4.50"),
            AccountingFlags.check(emptyList(), listOf(xr), "SERVICE", "TRANS", LocalDate.parse("2025-03-01")))
        assertTrue(AccountingFlags.check(emptyList(), listOf(xr), "SERVICE", "TRANS", LocalDate.parse("2025-09-01")).isEmpty())
        val xw = CapitalAction("XW", LocalDate.parse("2026-11-02"), "20 : 1", null)
        assertEquals(listOf("Capital raise: warrants (XW) on 2026-11-02, 20 : 1"),
            AccountingFlags.check(emptyList(), listOf(xw), "SERVICE", "TRANS", today))
    }

    @Test fun missingCashFlowFiguresAreNotFlagged() {
        assertTrue(AccountingFlags.check(listOf(FiscalYear(2025, 1_000.0, null, 500.0)), emptyList(), null, null, today).isEmpty())
    }
}
