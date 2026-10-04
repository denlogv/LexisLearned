package dev.denlogv.lexislearned.ai

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
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
    private val headers = mapOf("x-api-key" to apiKey, "anthropic-version" to API_VERSION)
    private val outputLimit = AnthropicOutputLimit(headers, model, baseUrl, http)

    /**
     * Creates a client for the real API.
     *
     * @param apiKey the user's API key.
     * @param model the model id.
     */
    constructor(apiKey: String, model: String) : this(apiKey, model, DEFAULT_BASE_URL, HttpJson())

    /**
     * Sends a system and a user message to the Messages API and reads the reply as it is streamed.
     *
     * @param system the system prompt.
     * @param user the user message.
     * @return the text of the reply.
     * @throws LlmException if the request fails, the reply has no text or the reply was cut off at the model's output limit.
     */
    override suspend fun complete(system: String, user: String): String {
        val reply = AnthropicReply()
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", outputLimit.get())
            put("stream", true)
            put("system", system)
            put("messages", buildJsonArray { add(message("user", user)) })
        }
        http.stream("$baseUrl/v1/messages", headers, body, reply::accept)
        return reply.result()
    }

    /** Constants of the client. */
    companion object {
        /** Address of the real API. */
        const val DEFAULT_BASE_URL = "https://api.anthropic.com"

        /** API version header value. */
        const val API_VERSION = "2023-06-01"
    }
}
