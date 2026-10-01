package dev.denlogv.lexislearned.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A chat message as the providers' APIs expect it.
 *
 * @param role "system" or "user".
 * @param content the message text.
 * @return the JSON object `{"role": ..., "content": ...}`.
 */
internal fun message(role: String, content: String): JsonObject = buildJsonObject {
    put("role", role)
    put("content", content)
}
