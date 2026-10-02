package apincer.mobile.tradings.domain

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Thai tax-saving fund room for the tax year (calendar year). Rules as published for 2024-2026:
 * ThaiESG: up to 30% of assessable income and ฿300,000, held 5 years, a limit of its own.
 * RMF: up to 30% of assessable income and ฿500,000, inside the ฿500,000 retirement group shared
 * with SSF, provident fund, GPF, pension insurance and similar. Held to age 55 and at least 5 years.
 * The value of a deduction is the amount times the investor's top income-tax rate.
 */
object TaxFunds {
    enum class Type(val label: String) { THAIESG("ThaiESG"), RMF("RMF") }

    data class Purchase(val type: Type, val amount: Double, val date: LocalDate)

    data class Room(val type: Type, val limit: Double, val bought: Double) {
        val remaining: Double get() = maxOf(0.0, limit - bought)
    }

    const val THAIESG_CAP = 300_000.0
    const val RETIREMENT_GROUP_CAP = 500_000.0
    const val INCOME_SHARE = 0.30

    /** [otherRetirement] is this year's PVD, SSF, GPF, pension insurance and similar contributions. */
    fun room(purchases: List<Purchase>, year: Int, assessableIncome: Double, otherRetirement: Double = 0.0): List<Room> {
        val thisYear = purchases.filter { it.date.year == year }
        val share = maxOf(0.0, assessableIncome) * INCOME_SHARE
        val esgLimit = minOf(share, THAIESG_CAP)
        val rmfLimit = maxOf(0.0, minOf(share, RETIREMENT_GROUP_CAP - maxOf(0.0, otherRetirement)))
        return listOf(
            Room(Type.THAIESG, esgLimit, thisYear.filter { it.type == Type.THAIESG }.sumOf { it.amount }),
            Room(Type.RMF, rmfLimit, thisYear.filter { it.type == Type.RMF }.sumOf { it.amount })
        )
    }

    /** Tax saved by deducting [amount] at a top rate of [marginalRatePercent]. */
    fun taxSaved(amount: Double, marginalRatePercent: Double): Double = maxOf(0.0, amount) * marginalRatePercent / 100.0

    fun toJson(purchases: List<Purchase>): String = JSONArray().apply {
        purchases.forEach { put(JSONObject().put("type", it.type.name).put("amount", it.amount).put("date", it.date.toString())) }
    }.toString()

    fun fromJson(json: String?): List<Purchase> = try {
        val arr = JSONArray(json ?: "[]")
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val amount = o.getDouble("amount")
            if (!amount.isFinite() || amount <= 0.0) null
            else Purchase(Type.valueOf(o.getString("type")), amount, LocalDate.parse(o.getString("date")))
        }
    } catch (e: Exception) {
        emptyList()
    }
}
