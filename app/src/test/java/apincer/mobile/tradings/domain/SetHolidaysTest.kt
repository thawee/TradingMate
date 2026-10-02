package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SetHolidaysTest {
    private val bkk = ZoneId.of("Asia/Bangkok")
    private fun at(date: String, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(LocalDate.parse(date).atTime(hour, minute), bkk)

    @Test fun publishedClosuresAreHolidays() {
        assertTrue(SetHolidays.isHoliday(LocalDate.parse("2026-10-13")))
        assertTrue(SetHolidays.isHoliday(LocalDate.parse("2026-04-14")))
        assertFalse(SetHolidays.isHoliday(LocalDate.parse("2026-10-14")))
        // Flood holiday on which SET still traded.
        assertFalse(SetHolidays.isHoliday(LocalDate.parse("2026-09-28")))
    }

    @Test fun unpublishedYearFallsBackToWeekends() {
        assertFalse(SetHolidays.isKnownYear(2027))
        assertFalse(SetHolidays.isHoliday(LocalDate.parse("2027-01-01")))
    }

    @Test fun marketStatusClosedOnHolidayDuringSessionHours() {
        assertEquals(MarketStatus.CLOSED, TechnicalAnalysis.getMarketStatus(at("2026-10-13", 11)))
        assertEquals(MarketStatus.OPEN, TechnicalAnalysis.getMarketStatus(at("2026-10-14", 11)))
        assertEquals(MarketStatus.LUNCH, TechnicalAnalysis.getMarketStatus(at("2026-10-14", 13)))
        assertEquals(MarketStatus.CLOSED, TechnicalAnalysis.getMarketStatus(at("2026-10-17", 11))) // Saturday
    }

    @Test fun marketStatusUsesBangkokTime() {
        // 04:00 UTC is 11:00 in Bangkok on a trading day.
        assertEquals(MarketStatus.OPEN, TechnicalAnalysis.getMarketStatus(
            ZonedDateTime.of(LocalDate.parse("2026-10-14").atTime(4, 0), ZoneId.of("UTC"))))
    }
}
