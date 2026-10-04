package dev.denlogv.lexislearned.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Client for OpenAI's Chat Completions API, or any server compatible with it, asking for a JSON object as the reply.
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
    @Volatile private var jsonMode = true

    /**
     * Creates a client for the real API.
     *
     * @param apiKey the user's API key.
     * @param model the model id.
     */
    constructor(apiKey: String, model: String) : this(apiKey, model, DEFAULT_BASE_URL, HttpJson())

    /**
     * Creates a client for any server that speaks the same protocol.
     *
     * @param apiKey the key the server expects as a bearer token.
     * @param model the model id.
     * @param baseUrl the server address including the version path, for example `https://host/v1`.
     */
    constructor(apiKey: String, model: String, baseUrl: String) : this(apiKey, model, baseUrl, HttpJson())

    /**
     * Sends a system and a user message to the Chat Completions API.
     *
     * @param system the system prompt.
     * @param user the user message.
     * @return the content of the first choice.
     * @throws LlmException if the request fails or the reply is empty.
     */
    override suspend fun complete(system: String, user: String): String {
        val response = try {
            request(system, user)
        } catch (e: LlmException) {
            if (!jsonMode || !rejectsResponseFormat(e)) throw e
            jsonMode = false
            request(system, user)
        }
        return response["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() } ?: throw LlmException("Empty response from the server")
    }

    /**
     * Posts one chat request, asking for a JSON object unless the server has refused that before.
     *
     * @param system the system prompt.
     * @param user the user message.
     * @return the parsed reply.
     * @throws LlmException if the request fails.
     */
    private suspend fun request(system: String, user: String): JsonObject = http.post(
        "$baseUrl/chat/completions",
        mapOf("authorization" to "Bearer $apiKey"),
        buildJsonObject {
            put("model", model)
            if (jsonMode) put("response_format", buildJsonObject { put("type", "json_object") })
            put(
                "messages",
                buildJsonArray {
                    add(message("system", system))
                    add(message("user", user))
                },
            )
        },
    )

    /**
     * Whether a failure says that the server does not accept `response_format`: some compatible servers, for example LM Studio,
     * only know `json_schema` and `text`. The prompt asks for JSON anyway and [LlmJson] finds it in the reply.
     *
     * @param e the failure.
     * @return true for a 400 that mentions `response_format`.
     */
    private fun rejectsResponseFormat(e: LlmException): Boolean = e.code == HTTP_BAD_REQUEST && "response_format" in e.message.orEmpty()

    /** Constants of the client. */
    companion object {
        /** Address of the real API. */
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        private const val HTTP_BAD_REQUEST = 400
    }
}
