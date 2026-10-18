package apincer.mobile.tradings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiClientTest {

    @Test
    fun `extractTradeScreenshot fails fast with a clear message when API key is blank`() {
        val result = GeminiClient.extractTradeScreenshot(base64Jpeg = "dGVzdA==", apiKey = "")

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("API key"))
    }

    @Test
    fun `extractTradeScreenshot fails fast with a clear message when API key is blank whitespace`() {
        val result = GeminiClient.extractTradeScreenshot(base64Jpeg = "dGVzdA==", apiKey = "   ")

        assertTrue(result.isFailure)
    }

    @Test
    fun `listModels fails fast with a clear message when API key is blank`() {
        val result = GeminiClient.listModels(apiKey = "")

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("API key"))
    }

    @Test
    fun `GeminiModel fromId resolves known models and falls back to FLASH_2_5 for invalid IDs`() {
        assertEquals(GeminiModel.PRO_2_5, GeminiModel.fromId("gemini-2.5-pro"))
        assertEquals(GeminiModel.FLASH_2_5, GeminiModel.fromId("gemini-3.1-flash-lite-preview"))
    }
}
