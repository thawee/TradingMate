package apincer.mobile.tradings.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * SET price spread table for stocks: orders and alerts are only valid on these steps.
 * Stops round up (never more risk than planned), targets round down (a reachable sell price).
 */
object SetTick {
    fun tickSize(price: Double): Double = when {
        price < 2.0 -> 0.01
        price < 5.0 -> 0.02
        price < 10.0 -> 0.05
        price < 25.0 -> 0.10
        price < 100.0 -> 0.25
        price < 200.0 -> 0.50
        price < 400.0 -> 1.00
        else -> 2.00
    }

    fun isValid(price: Double): Boolean = price > 0.0 && price.isFinite() && floor(price) == round2(price)

    /** Largest valid price at or below [price]. */
    fun floor(price: Double): Double = snap(price, RoundingMode.FLOOR)

    /** Smallest valid price at or above [price]. Band edges (2, 5, 10, 25, 100, 200, 400) sit on both grids. */
    fun ceil(price: Double): Double = snap(price, RoundingMode.CEILING)

    private fun snap(price: Double, mode: RoundingMode): Double {
        if (!price.isFinite() || price <= 0.0) return price
        // Round to the cent first so binary noise (45.75000000001) does not push a valid price a step away.
        val p = BigDecimal.valueOf(price).setScale(4, RoundingMode.HALF_UP)
        val step = BigDecimal.valueOf(tickSize(price))
        return p.divide(step, 0, mode).multiply(step).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    private fun round2(price: Double): Double =
        BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP).toDouble()
}
