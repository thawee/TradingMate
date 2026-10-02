package apincer.mobile.tradings.domain

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MarketListsTest {
    @After fun reset() = MarketLists.resetForTest()

    private val fifty = (TradingConstants.SET50_SYMBOLS - "TFG" + "NEWCO").joinToString(",") { "\"$it\"" }

    @Test fun validFileReplacesSet50AndAddsAYear() {
        val json = """{"set50":{"effective":"2027-01-01","symbols":[$fifty]},
            "holidays":{"2027":["2027-01-01","2027-02-22","2027-04-06","2027-04-13","2027-04-14","2027-04-15"]}}"""
        assertTrue(MarketLists.apply(json))
        assertTrue(MarketLists.isSet50("newco"))
        assertFalse(MarketLists.isSet50("TFG"))
        assertTrue(SetHolidays.isHoliday(LocalDate.parse("2027-04-13")))
        assertTrue(SetHolidays.isKnownYear(2027))
        // Built-in 2026 still applies.
        assertTrue(SetHolidays.isHoliday(LocalDate.parse("2026-10-13")))
    }

    @Test fun committedConfigFileIsValid() {
        // The app downloads this exact file; a broken edit would be silently ignored on every phone.
        val root = java.io.File(System.getProperty("user.dir")).let { if (java.io.File(it, "config").exists()) it else it.parentFile }
        val json = java.io.File(root, "config/market_lists.json").readText()
        assertTrue(MarketLists.apply(json))
        assertTrue(MarketLists.isSet50("PTT"))
        assertTrue(SetHolidays.isHoliday(LocalDate.parse("2026-12-31")))
    }

    @Test fun invalidFileChangesNothing() {
        assertFalse(MarketLists.apply("""{"set50":{"symbols":["PTT","AOT"]}}""")) // too few
        assertFalse(MarketLists.apply("""{"holidays":{"2027":["2026-01-01","x"]}}"""))
        assertFalse(MarketLists.apply("not json"))
        assertTrue(MarketLists.isSet50("TFG"))
        assertFalse(SetHolidays.isKnownYear(2027))
    }
}
