package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetTickTest {
    @Test fun tickSizeFollowsSpreadTable() {
        assertEquals(0.01, SetTick.tickSize(1.99), 0.0)
        assertEquals(0.02, SetTick.tickSize(2.0), 0.0)
        assertEquals(0.05, SetTick.tickSize(9.95), 0.0)
        assertEquals(0.10, SetTick.tickSize(10.0), 0.0)
        assertEquals(0.25, SetTick.tickSize(45.68), 0.0)
        assertEquals(0.50, SetTick.tickSize(100.0), 0.0)
        assertEquals(1.00, SetTick.tickSize(399.0), 0.0)
        assertEquals(2.00, SetTick.tickSize(400.0), 0.0)
    }

    @Test fun stopRoundsUpTargetRoundsDown() {
        assertEquals(45.75, SetTick.ceil(45.68), 1e-9)
        assertEquals(45.50, SetTick.floor(45.68), 1e-9)
        assertEquals(6.60, SetTick.ceil(6.57), 1e-9)
        assertEquals(4.42, SetTick.ceil(4.41), 1e-9)
    }

    @Test fun validPricesAreUnchanged() {
        for (p in listOf(0.94, 4.60, 6.95, 24.90, 45.75, 152.50, 398.0, 402.0)) {
            assertEquals(p, SetTick.ceil(p), 1e-9)
            assertEquals(p, SetTick.floor(p), 1e-9)
            assertTrue(SetTick.isValid(p))
        }
        assertEquals(45.75, SetTick.floor(45.75000000001), 1e-9)
    }

    @Test fun crossingBandEdgesLandsOnValidPrices() {
        assertEquals(25.0, SetTick.ceil(24.95), 1e-9)
        assertEquals(100.0, SetTick.ceil(99.9), 1e-9)
        assertEquals(99.75, SetTick.floor(99.99), 1e-9)
        assertFalse(SetTick.isValid(45.68))
    }
}
