package dev.denlogv.lexislearned.ai

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Client for OpenAI's Chat Completions API, asking for a JSON object as the reply.
 *
 * @param apiKey the user's API key.
 * @param model the model id.
 * @param baseUrl the API address including the version path; only tests use anything else than the default.
 * @param http the HTTP helper.
 */
class OpenAiClient internal constructor(
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String,
    private val http: HttpJson,
) : LlmClient {
    /**
     * Creates a client for the real API.
     *
     * @param apiKey the user's API key.
     * @param model the model id.
     */
    constructor(apiKey: String, model: String) : this(apiKey, model, DEFAULT_BASE_URL, HttpJson())

    /**
     * Sends a system and a user message to the Chat Completions API.
     *
     * @param system the system prompt.
     * @param user the user message.
     * @return the content of the first choice.
     * @throws LlmException if the request fails or the reply is empty.
     */
    override suspend fun complete(system: String, user: String): String {
        val response = http.post(
            "$baseUrl/chat/completions",
            mapOf("authorization" to "Bearer $apiKey"),
            buildJsonObject {
                put("model", model)
                put("response_format", buildJsonObject { put("type", "json_object") })
                put(
                    "messages",
                    buildJsonArray {
                        add(message("system", system))
                        add(message("user", user))
                    },
                )
            },
        )
        return response["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() } ?: throw LlmException("Empty response from OpenAI")
    }

    /** Constants of the client. */
    companion object {
        /** Address of the real API. */
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
    }
}
