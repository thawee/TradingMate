package apincer.mobile.tradings.domain

import java.time.LocalDate
import java.util.Locale

/**
 * Accounting red flags on a holding, as review notes (tasks/todo.md, 2026-10-08). Profit that does not
 * turn into operating cash while new money is raised preceded STARK (FY2021) and EA (FY2023); across
 * SET50 non-financials it fires on about 1 company-year in 40. Not backtested (no delisted companies in
 * the data), so it never triggers a sale.
 */
object AccountingFlags {
    /** One fiscal year from SET company highlights, in thousand baht. */
    data class FiscalYear(val year: Int, val netProfit: Double?, val operatingCash: Double?, val financingCash: Double?)

    /** A rights offering (XR) or warrant issue (XW) from SET corporate actions. */
    data class CapitalAction(val type: String, val xDate: LocalDate, val ratio: String?, val price: Double?)

    private const val RECENT_DAYS = 365L

    /** [industry] and [sector] are SET codes; lenders ("FINCIAL") are skipped for the cash-flow check. */
    fun check(years: List<FiscalYear>, actions: List<CapitalAction>, industry: String?, sector: String?,
              today: LocalDate): List<String> {
        val lines = mutableListOf<String>()
        val latest = years.maxByOrNull { it.year }
        if (latest != null && industry != "FINCIAL") {
            val np = latest.netProfit; val ocf = latest.operatingCash; val fin = latest.financingCash
            if (np != null && ocf != null && fin != null && np > 0 && ocf < 0 && fin > 0) {
                fun m(v: Double) = String.format(Locale.ENGLISH, "฿%,.0fm", v / 1000)
                lines += "Profit without cash in FY${latest.year}: net profit ${m(np)} but operating cash flow ${m(ocf)}, " +
                    "covered by ${m(fin)} of new financing" +
                    if (sector == "PROP") " (common when a developer buys land; check inventory)" else ""
            }
        }
        actions.filter { it.type == "XR" || it.type == "XW" }
            .filter { !it.xDate.isBefore(today.minusDays(RECENT_DAYS)) }
            .sortedBy { it.xDate }
            .forEach { a ->
                val what = if (a.type == "XR") "rights offering (XR)" else "warrants (XW)"
                val terms = listOfNotNull(a.ratio?.takeIf { it.isNotBlank() },
                    a.price?.let { String.format(Locale.ENGLISH, "at ฿%.2f", it) }).joinToString(" ")
                lines += "Capital raise: $what on ${a.xDate}" + if (terms.isNotEmpty()) ", $terms" else ""
            }
        return lines
    }
}
