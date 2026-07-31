package apincer.mobile.tradings.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiClientTest {

    @Test
    fun `analyze fails fast with a clear message when API key is blank`() {
        val result = GeminiClient.analyze(prompt = "Any prompt", apiKey = "")

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("API key"))
    }

    @Test
    fun `analyze fails fast with a clear message when API key is blank whitespace`() {
        val result = GeminiClient.analyze(prompt = "Any prompt", apiKey = "   ")

        assertTrue(result.isFailure)
    }

    @Test
    fun `listModels fails fast with a clear message when API key is blank`() {
        val result = GeminiClient.listModels(apiKey = "")

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("API key"))
    }
}
