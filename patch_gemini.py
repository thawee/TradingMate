import sys

with open('app/src/main/java/apincer/mobile/tradings/domain/GeminiClient.kt', 'r') as f:
    content = f.read()

new_class = """
/** Structured result extracted from a broker screenshot. */
data class TradeScreenshotData(
    val symbol: String,
    val price: Double,
    val quantity: Int
)

/** A single AI-ranked trade/candidate recommendation. */"""
content = content.replace("/** A single AI-ranked trade/candidate recommendation. */", new_class)

new_func = """
    fun extractTradeScreenshot(base64Jpeg: String, apiKey: String, modelId: String = "gemini-3.5-flash"): Result<TradeScreenshotData> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key is not set. Add it in Settings > AI Integration."))
        }
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent"

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
"""

content = content[:content.rfind("}")] + new_func

with open('app/src/main/java/apincer/mobile/tradings/domain/GeminiClient.kt', 'w') as f:
    f.write(content)
