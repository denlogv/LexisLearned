package dev.denlogv.lexislearned.ai

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Assembles the reply of a Chat Completions server from its stream of chunks: the content of the first choice, and why the model
 * stopped. A server that ignores the request to stream sends one complete reply instead; it is understood too.
 */
internal class OpenAiReply {
    private val text = StringBuilder()
    private var finishReason: String? = null

    /**
     * Takes in one chunk of the stream, or a complete reply.
     *
     * @param data the chunk's JSON, or `[DONE]` which ends a stream.
     * @throws LlmException if the data is not JSON or the server reports an error in the stream.
     */
    fun accept(data: String) {
        if (data == DONE) return
        val body = parseJsonObject(data)
        body["error"]?.let { throw LlmException(it.errorMessage()) }
        val choice = body["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return
        // A chunk has "delta", a complete reply has "message".
        (choice["delta"] ?: choice["message"])?.jsonObject?.text("content")?.let(text::append)
        choice.text("finish_reason")?.let { finishReason = it }
    }

    /**
     * The complete reply once the stream has ended.
     *
     * @return the text.
     * @throws LlmException if the model was cut off at its output limit or wrote no text.
     */
    fun result(): String {
        if (finishReason == "length") throw LlmException("The reply was cut off because it reached the model's output limit")
        return text.toString().takeIf { it.isNotBlank() } ?: throw LlmException("Empty response from the server")
    }

    /**
     * The message of an error object, or the object itself if it has none: servers differ in what they put under `error`.
     *
     * @receiver the value of the `error` field.
     * @return the text to show.
     */
    private fun JsonElement.errorMessage(): String = (this as? JsonObject)?.text("message") ?: toString()

    private companion object {
        const val DONE = "[DONE]"
    }
}
