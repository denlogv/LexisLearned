package dev.denlogv.lexislearned.ai

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * A text or number field as text.
 *
 * @receiver a JSON object.
 * @param name the field name.
 * @return the value, or null if the field is absent, null or not a plain value.
 */
internal fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull

/**
 * Parses a JSON object that a provider sent.
 *
 * @param text the JSON text.
 * @return the object.
 * @throws LlmException if the text is not a JSON object.
 */
internal fun parseJsonObject(text: String): JsonObject = try {
    Json.parseToJsonElement(text).jsonObject
} catch (e: SerializationException) {
    throw LlmException("The server did not return JSON: ${e.message}", cause = e)
} catch (e: IllegalArgumentException) {
    throw LlmException("The server did not return a JSON object: ${e.message}", cause = e)
}
