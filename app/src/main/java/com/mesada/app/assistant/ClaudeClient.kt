package com.mesada.app.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ClaudeException(val code: Int, message: String) : Exception(message)

/**
 * Cliente mínimo de la Messages API.
 * - Desarrollo: baseUrl = https://api.anthropic.com + apiKey en local.properties.
 * - Producción: baseUrl = tu backend (expone POST /v1/messages y agrega la clave del lado del servidor);
 *   apiKey vacío, así la clave nunca viaja dentro del APK.
 */
class ClaudeClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType = "application/json".toMediaType()

    private val callsAnthropicDirectly get() = baseUrl.contains("api.anthropic.com")

    suspend fun createMessage(system: String, messages: JsonArray, tools: JsonArray): JsonObject =
        withContext(Dispatchers.IO) {
            if (callsAnthropicDirectly && apiKey.isBlank()) {
                throw ClaudeException(401, "Falta anthropic.apiKey en local.properties")
            }
            val body = buildJsonObject {
                put("model", model)
                put("max_tokens", 1024)
                put("system", system)
                put("messages", messages)
                put("tools", tools)
            }.toString()

            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/v1/messages")
                .header("content-type", "application/json")
                .header("anthropic-version", "2023-06-01")
                .apply { if (apiKey.isNotBlank()) header("x-api-key", apiKey) }
                .post(body.toRequestBody(jsonType))
                .build()

            http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw ClaudeException(resp.code, text.take(300))
                json.parseToJsonElement(text).jsonObject
            }
        }
}
