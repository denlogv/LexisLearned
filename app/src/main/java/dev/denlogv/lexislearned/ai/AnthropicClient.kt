package dev.denlogv.lexislearned.ai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Client for Anthropic's Messages API (Claude).
 *
 * @param apiKey the user's API key.
 * @param model the model id.
 * @param baseUrl the API address; only tests use anything else than the default.
 * @param http the HTTP helper.
 */
class AnthropicClient internal constructor(
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
     * Sends a system and a user message to the Messages API.
     *
     * @param system the system prompt.
     * @param user the user message.
     * @return the concatenated text blocks of the reply.
     * @throws LlmException if the request fails or the reply has no text.
     */
    override suspend fun complete(system: String, user: String): String {
        val response = http.post(
            "$baseUrl/v1/messages",
            mapOf("x-api-key" to apiKey, "anthropic-version" to API_VERSION),
            buildJsonObject {
                put("model", model)
                put("max_tokens", MAX_TOKENS)
                put("system", system)
                put("messages", buildJsonArray { add(message("user", user)) })
            },
        )
        return (response["content"] as? JsonArray)
            ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
            ?.takeIf { it.isNotBlank() } ?: throw LlmException("Empty response from Anthropic")
    }

    /** Constants of the client. */
    companion object {
        /** Address of the real API. */
        const val DEFAULT_BASE_URL = "https://api.anthropic.com"

        /** API version header value. */
        const val API_VERSION = "2023-06-01"
        private const val MAX_TOKENS = 8192
    }
}
