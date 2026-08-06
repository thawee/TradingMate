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

/** A single AI-ranked trade/candidate recommendation. */
data class AiRecommendation(
    val symbol: String,
    val playbookType: String,
    val buyZone: String,
    val targetProfit: String,
    val stopLoss: String,
    val confidenceScore: Int,
    val cashAllocation: String = "",
    val reasoning: String
)

/** Full structured result of an in-app AI analysis call. */
data class AiAnalysisResult(
    val executiveSummary: String,
    val recommendations: List<AiRecommendation>
)

/** A Gemini model discovered from the live ListModels API, filtered to ones usable for generateContent. */
data class AvailableGeminiModel(
    val id: String,
    val displayName: String,
    val description: String
)

/** A selectable Gemini free-tier model with its rate-limit profile shown to the user. */
enum class GeminiModel(val id: String, val label: String, val description: String) {
    FLASH_3_6("gemini-3.6-flash", "Gemini 3.6 Flash (Recommended)", "Latest, balanced quality/speed. Free tier: 10 req/min, 1,500 req/day."),
    FLASH_3_5("gemini-3.5-flash", "Gemini 3.5 Flash", "Prior stable Flash. Free tier: 10 req/min, 1,500 req/day."),
    FLASH_LITE_3_1("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite", "Fastest, highest quota. Free tier: 15 req/min, 1,000 req/day."),
    PRO_3_1("gemini-3.1-pro", "Gemini 3.1 Pro", "Best reasoning quality. Free tier: 5 req/min, 50 req/day.");

    companion object {
        fun fromId(id: String): GeminiModel = entries.find { it.id == id } ?: FLASH_3_6
    }
}

/**
 * Thin client for Google's Gemini API (generateContent, structured JSON output).
 * Calls the model directly with the user's own API key (stored in Settings) so results
 * can be parsed and rendered natively instead of requiring copy/paste into an external AI.
 *
 * Note: unlike the copy/paste prompt (meant for a browser-based AI with live web search),
 * this direct API call has NO live web/news access — it only reasons over the technical
 * and fundamental metrics included in the prompt.
 */
object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val DEFAULT_MODEL_ID = "gemini-3.6-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun buildResponseSchema() = JSONObject().apply {
        put("type", "OBJECT")
        put("properties", JSONObject().apply {
            put("executiveSummary", JSONObject().put("type", "STRING"))
            put("recommendations", JSONObject().apply {
                put("type", "ARRAY")
                put("items", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("symbol", JSONObject().put("type", "STRING"))
                        put("playbookType", JSONObject().put("type", "STRING"))
                        put("buyZone", JSONObject().put("type", "STRING"))
                        put("targetProfit", JSONObject().put("type", "STRING"))
                        put("stopLoss", JSONObject().put("type", "STRING"))
                        put("confidenceScore", JSONObject().put("type", "INTEGER"))
                        put("cashAllocation", JSONObject().put("type", "STRING"))
                        put("reasoning", JSONObject().put("type", "STRING"))
                    })
                    put("required", JSONArray(listOf("symbol", "playbookType", "buyZone", "targetProfit", "stopLoss", "confidenceScore", "reasoning")))
                })
            })
        })
        put("required", JSONArray(listOf("executiveSummary", "recommendations")))
    }

    /** Calls Gemini synchronously (must be invoked from a background dispatcher). */
    fun analyze(prompt: String, apiKey: String, modelId: String = DEFAULT_MODEL_ID): Result<AiAnalysisResult> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key is not set. Add it in Settings > AI Integration."))
        }
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent"

        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("responseSchema", buildResponseSchema())
                put("temperature", 0.3)
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
                    Log.e(TAG, "Gemini call failed: $message")
                    return Result.failure(IOException("Gemini API error: $message"))
                }

                val root = JSONObject(rawBody)
                val text = root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (text.isNullOrBlank()) {
                    return Result.failure(IOException("Gemini returned an empty response."))
                }

                val parsed = JSONObject(text)
                val recommendations = mutableListOf<AiRecommendation>()
                val recArray = parsed.optJSONArray("recommendations") ?: JSONArray()
                for (i in 0 until recArray.length()) {
                    val item = recArray.getJSONObject(i)
                    recommendations.add(
                        AiRecommendation(
                            symbol = item.optString("symbol"),
                            playbookType = item.optString("playbookType"),
                            buyZone = item.optString("buyZone"),
                            targetProfit = item.optString("targetProfit"),
                            stopLoss = item.optString("stopLoss"),
                            confidenceScore = item.optInt("confidenceScore", 0).coerceIn(0, 100),
                            cashAllocation = item.optString("cashAllocation"),
                            reasoning = item.optString("reasoning")
                        )
                    )
                }

                Result.success(
                    AiAnalysisResult(
                        executiveSummary = parsed.optString("executiveSummary"),
                        recommendations = recommendations
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call exception", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the live list of models available to this API key/account from Gemini's
     * ListModels endpoint, filtered to ones that support generateContent (i.e. usable by
     * [analyze]) and excluding non-text specialist models (image/audio/embedding/TTS).
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
                    val fullName = item.optString("name") // e.g. "models/gemini-3.6-flash"
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
}
