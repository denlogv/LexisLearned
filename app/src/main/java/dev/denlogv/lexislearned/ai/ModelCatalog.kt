package dev.denlogv.lexislearned.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A model offered by a provider.
 *
 * @property id the id to send in requests.
 * @property label the name to show the user.
 */
data class ModelInfo(val id: String, val label: String)

/**
 * Asks a provider which models an API key can use.
 *
 * @param http the HTTP helper.
 */
class ModelCatalog internal constructor(private val http: HttpJson) {
    /** Creates a catalog that talks to the real APIs. */
    constructor() : this(HttpJson())

    /**
     * Lists Anthropic's models.
     *
     * @param apiKey the user's API key.
     * @param baseUrl the API address; only tests use anything else than the default.
     * @return the models, in the order Anthropic returns them (newest first).
     * @throws LlmException if the request fails.
     */
    suspend fun anthropic(apiKey: String, baseUrl: String = AnthropicClient.DEFAULT_BASE_URL): List<ModelInfo> {
        val reply = http.get(
            "$baseUrl/v1/models?limit=$PAGE_SIZE",
            mapOf("x-api-key" to apiKey, "anthropic-version" to AnthropicClient.API_VERSION),
        )
        return reply["data"]?.jsonArray.orEmpty().mapNotNull { entry ->
            val obj = entry.jsonObject
            val id = obj.text("id") ?: return@mapNotNull null
            ModelInfo(id, obj.text("display_name") ?: id)
        }
    }

    /**
     * Lists OpenAI's chat models, newest first; embedding, audio, image and similar models are left out.
     *
     * @param apiKey the user's API key.
     * @param baseUrl the API address including the version path; only tests use anything else than the default.
     * @return the chat models.
     * @throws LlmException if the request fails.
     */
    suspend fun openAi(apiKey: String, baseUrl: String = OpenAiClient.DEFAULT_BASE_URL): List<ModelInfo> {
        val reply = http.get("$baseUrl/models", mapOf("authorization" to "Bearer $apiKey"))
        return reply["data"]?.jsonArray.orEmpty()
            .mapNotNull { it.jsonObject.toChatModel() }
            .sortedByDescending { it.second }
            .map { ModelInfo(it.first, it.first) }
    }

    /**
     * Converts an OpenAI model entry if it is a chat model.
     *
     * @receiver one entry of the model list.
     * @return the id with its creation time, or null for models that are not for chat.
     */
    private fun JsonObject.toChatModel(): Pair<String, Long>? {
        val id = text("id") ?: return null
        if (!chatModel.containsMatchIn(id) || notChat.containsMatchIn(id)) return null
        return id to (text("created")?.toLongOrNull() ?: 0L)
    }

    /**
     * A text or number field as text.
     *
     * @param name the field name.
     * @return the value, or null if the field is absent.
     */
    private fun JsonObject.text(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull

    private companion object {
        const val PAGE_SIZE = 100
        val chatModel = Regex("^(gpt-|o\\d|chatgpt-)")
        val notChat = Regex("embedding|whisper|tts|audio|image|realtime|transcribe|moderation|dall-e|search|codex|instruct")
    }
}
