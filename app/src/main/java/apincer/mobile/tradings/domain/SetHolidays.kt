package apincer.mobile.tradings.domain

import java.time.LocalDate

/**
 * Full-day SET market closures. Source: SET holiday calendar as published for 2026
 * (cross-checked with markethours.io and calendarlabs.com on 2026-10-02).
 * Add each year when SET publishes it; an unlisted year falls back to weekends only.
 * Declared public holidays are not always closures: SET traded on the 2026-09-28 flood holiday.
 */
object SetHolidays {
    private val byYear: Map<Int, Set<LocalDate>> = mapOf(
        2026 to listOf(
            "2026-01-01", "2026-01-02", // New Year's Day, special holiday
            "2026-03-03",               // Makha Bucha
            "2026-04-06",               // Chakri Day
            "2026-04-13", "2026-04-14", "2026-04-15", // Songkran
            "2026-05-01",               // Labour Day
            "2026-05-04",               // Coronation Day
            "2026-06-01",               // Visakha Bucha (substitution)
            "2026-06-03",               // H.M. Queen Suthida's Birthday
            "2026-07-28",               // H.M. the King's Birthday
            "2026-07-29",               // Asarnha Bucha
            "2026-08-12",               // H.M. Queen Sirikit The Queen Mother's Birthday
            "2026-10-13",               // King Bhumibol Memorial Day
            "2026-10-23",               // Chulalongkorn Day
            "2026-12-07",               // King Bhumibol's Birthday (substitution)
            "2026-12-10",               // Constitution Day
            "2026-12-31"                // New Year's Eve
        ).map(LocalDate::parse).toSet()
    )

    /** A year published in the remote market lists replaces the built-in one. */
    fun isHoliday(date: LocalDate): Boolean =
        (MarketLists.holidaysFor(date.year) ?: byYear[date.year])?.contains(date) == true

    /** False when SET has not published (or the app does not yet carry) that year's calendar. */
    fun isKnownYear(year: Int): Boolean = MarketLists.holidaysFor(year) != null || byYear.containsKey(year)
}
