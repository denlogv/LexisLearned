package dev.denlogv.lexislearned.ai

import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The most output tokens one reply of an Anthropic model may have. The Messages API insists on a `max_tokens` value in every
 * request, so instead of picking a number of our own, the app asks for the model's own maximum and uses that: a reply is then
 * only ever cut off by the model's real limit.
 *
 * @param headers the request headers with the API key and version.
 * @param model the model id.
 * @param baseUrl the API address.
 * @param http the HTTP helper.
 */
internal class AnthropicOutputLimit(
    private val headers: Map<String, String>,
    private val model: String,
    private val baseUrl: String,
    private val http: HttpJson,
) {
    private var known: Int? = null

    /**
     * The model's maximum, looked up once and remembered. If the model does not report one or the lookup fails (other than for a
     * rejected key), [FALLBACK] is used for this request and the lookup is tried again for the next one.
     *
     * @return the value to send as `max_tokens`.
     * @throws LlmException if the provider rejects the API key.
     */
    suspend fun get(): Int {
        known?.let { return it }
        val reported = try {
            http.get("$baseUrl/v1/models/$model", headers)["max_tokens"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }
        } catch (e: LlmException) {
            if (e.code in AUTH_ERRORS) throw e
            null
        }
        known = reported
        return reported ?: FALLBACK
    }

    /** Constants of the lookup. */
    companion object {
        /** Used only while the model's own maximum is unknown, because the API requires some value. */
        const val FALLBACK = 8192
        private val AUTH_ERRORS = setOf(401, 403)
    }
}
