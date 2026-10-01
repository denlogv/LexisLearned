package dev.denlogv.lexislearned.ai

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** Reads the JSON object out of a model reply, which may wrap it in explanatory text. */
internal object LlmJson {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Decodes the outermost `{...}` in the reply.
     *
     * @param raw the reply text.
     * @param serializer how to decode the object.
     * @return the decoded value.
     * @throws LlmException if the reply contains no JSON object.
     * @throws kotlinx.serialization.SerializationException if the object does not have the expected shape.
     */
    fun <T> parse(raw: String, serializer: KSerializer<T>): T {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end < start) throw LlmException("Model did not return JSON")
        return json.decodeFromString(serializer, raw.substring(start, end + 1))
    }
}
