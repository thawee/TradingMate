package apincer.mobile.tradings.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorePortfolioDraftTest {
    private val draft = CorePortfolioDraft("80", "10000", "1", "1div", "50")

    @Test
    fun `accepts decimals and normalizes the optional symbol`() {
        val edited = draft.copy(corePercent = "80.5", dcaAmount = "10000.75", secondEtf = " 1div ")
        assertTrue(edited.isValid)
        assertEquals(80.5, edited.corePercentValue!!, 0.0)
        assertEquals(10000.75, edited.dcaAmountValue!!, 0.0)
        assertEquals("1DIV", edited.secondEtfValue)
    }

    @Test
    fun `blank amount deliberately disables reminder in a valid draft`() {
        val edited = draft.copy(dcaAmount = "", secondEtf = " ")
        assertTrue(edited.isValid)
        assertEquals(0.0, edited.dcaAmountValue!!, 0.0)
        assertEquals(null, edited.secondEtfValue)
        assertEquals("10000", draft.dcaAmount)
    }

    @Test
    fun `accepts the existing inclusive bounds`() {
        assertTrue(draft.copy(corePercent = "0", dcaAmount = "0", dcaDay = "1", secondPercent = "0").isValid)
        assertTrue(draft.copy(corePercent = "100", dcaDay = "31", secondPercent = "100").isValid)
    }

    @Test
    fun `rejects invalid values instead of silently clamping or saving zero`() {
        listOf("", "text", "-1", "101", "NaN", "Infinity").forEach {
            assertFalse(it, draft.copy(corePercent = it).isValid)
            assertFalse(it, draft.copy(secondPercent = it).isValid)
        }
        listOf("text", "-1", "NaN", "Infinity", "1e999").forEach {
            assertFalse(it, draft.copy(dcaAmount = it).isValid)
        }
        listOf("", "0", "32", "1.5", "text").forEach {
            assertFalse(it, draft.copy(dcaDay = it).isValid)
        }
    }
}
