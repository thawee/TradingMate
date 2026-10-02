package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BollingerBandsTest {
    private val tight = BollingerBands(upper = 13.17, middle = 12.82, lower = 12.47)

    @Test fun midBandPriceIsNeitherNearSupportNorOverextended() {
        // TRUE on 2026-10-02: the old 5%-from-edge rule flagged both.
        assertFalse(tight.isNearLower(12.90))
        assertFalse(tight.isNearUpper(12.90))
        assertEquals(0.614, tight.position(12.90)!!, 0.001)
    }

    @Test fun edgesOfTheBandAreFlagged() {
        assertTrue(tight.isNearLower(12.55))
        assertTrue(tight.isNearUpper(13.10))
        assertTrue(tight.isNearLower(12.30)) // below the band
    }

    @Test fun flatBandHasNoPosition() {
        assertNull(BollingerBands(10.0, 10.0, 10.0).position(10.0))
        assertFalse(BollingerBands(10.0, 10.0, 10.0).isNearUpper(10.0))
    }
}
