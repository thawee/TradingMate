package apincer.mobile.tradings.domain

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


/** Structured result extracted from a broker screenshot. */
data class TradeScreenshotData(
    val symbol: String,
    val price: Double,
    val quantity: Int
)

/** A Gemini model discovered from the live ListModels API, filtered to ones usable for generateContent. */
data class AvailableGeminiModel(
    val id: String,
    val displayName: String,
    val description: String
)

/** A selectable Gemini free-tier model with its rate-limit profile shown to the user. */
enum class GeminiModel(val id: String, val label: String, val description: String) {
    FLASH_2_5("gemini-2.5-flash", "Gemini 2.5 Flash (Recommended)", "Balanced quality and speed for screenshot OCR. Free tier: 10 req/min."),
    FLASH_LITE_2_5("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite", "Fastest, highest free-tier quota."),
    PRO_2_5("gemini-2.5-pro", "Gemini 2.5 Pro", "Highest reasoning and vision accuracy.");

    companion object {
        fun fromId(id: String): GeminiModel = entries.find { it.id == id } ?: FLASH_2_5
    }
}

/**
 * Thin client for Google's Gemini API (generateContent, structured JSON output) used for
 * broker trade screenshot extraction and discovering available models for the user's API key.
 */
object GeminiClient {
    private const val TAG = "GeminiClient"
    const val DEFAULT_MODEL_ID = "gemini-2.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches the live list of models available to this API key/account from Gemini's
     * ListModels endpoint, filtered to ones that support generateContent and excluding
     * non-text specialist models (image/audio/embedding/TTS).
     */
    fun listModels(apiKey: String): Result<List<AvailableGeminiModel>> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key is not set. Add it in Settings > AI Integration."))
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey&pageSize=200")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = try {
                        JSONObject(rawBody).optJSONObject("error")?.optString("message")
                    } catch (e: Exception) { null } ?: "HTTP ${response.code}"
                    Log.e(TAG, "Gemini listModels failed: $message")
                    return Result.failure(IOException("Gemini API error: $message"))
                }

                val root = JSONObject(rawBody)
                val modelsArray = root.optJSONArray("models") ?: JSONArray()
                val excludedKeywords = listOf("embedding", "aqa", "-tts", "-image", "imagen", "veo", "-audio", "gemma", "learnlm")
                val results = mutableListOf<AvailableGeminiModel>()

                for (i in 0 until modelsArray.length()) {
                    val item = modelsArray.getJSONObject(i)
                    val fullName = item.optString("name") // e.g. "models/gemini-2.5-flash"
                    val id = fullName.removePrefix("models/")
                    val methods = item.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                    val supportsGenerateContent = (0 until methods.length()).any { methods.optString(it) == "generateContent" }
                    val isExcluded = excludedKeywords.any { id.contains(it, ignoreCase = true) }

                    if (supportsGenerateContent && !isExcluded) {
                        results.add(
                            AvailableGeminiModel(
                                id = id,
                                displayName = item.optString("displayName").ifBlank { id },
                                description = item.optString("description")
                            )
                        )
                    }
                }

                Result.success(results.sortedByDescending { it.id })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini listModels exception", e)
            Result.failure(e)
        }
    }

    fun extractTradeScreenshot(base64Jpeg: String, apiKey: String, modelId: String = DEFAULT_MODEL_ID): Result<TradeScreenshotData> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key is not set. Add it in Settings > AI Integration."))
        }
        val resolvedModel = modelId.ifBlank { DEFAULT_MODEL_ID }
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$resolvedModel:generateContent"

        val responseSchema = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("symbol", JSONObject().put("type", "STRING"))
                put("price", JSONObject().put("type", "NUMBER"))
                put("quantity", JSONObject().put("type", "INTEGER"))
            })
            put("required", JSONArray(listOf("symbol", "price", "quantity")))
        }

        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Extract the exact stock ticker symbol, the execution/buy price, and the total quantity filled from this broker trade screenshot. Format as JSON. If the symbol ends with .BK or similar exchange suffixes, remove the suffix (e.g., return ADVANC instead of ADVANC.BK)."))
                        put(JSONObject().put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Jpeg)
                        }))
                    })
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("responseSchema", responseSchema)
                put("temperature", 0.0)
            })
        }

        val request = Request.Builder()
            .url("$endpoint?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = try {
                        JSONObject(rawBody).optJSONObject("error")?.optString("message")
                    } catch (e: Exception) { null } ?: "HTTP ${response.code}"
                    Log.e(TAG, "Gemini vision failed: $message")
                    return Result.failure(IOException("Vision error: $message"))
                }

                val root = JSONObject(rawBody)
                val text = root.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")

                if (text.isNullOrBlank()) {
                    return Result.failure(IOException("Empty vision response."))
                }

                val parsed = JSONObject(text)
                Result.success(TradeScreenshotData(
                    symbol = parsed.optString("symbol"),
                    price = parsed.optDouble("price", 0.0),
                    quantity = parsed.optInt("quantity", 0)
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini vision exception", e)
            Result.failure(e)
        }
    }
}
