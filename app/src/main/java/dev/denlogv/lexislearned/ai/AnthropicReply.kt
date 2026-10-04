package dev.denlogv.lexislearned.ai

import kotlinx.serialization.json.jsonObject

/**
 * Assembles the reply of Anthropic's Messages API from its stream of events: the text of every text block, and why the model
 * stopped.
 */
internal class AnthropicReply {
    private val text = StringBuilder()
    private var stopReason: String? = null

    /**
     * Takes in one event of the stream.
     *
     * @param data the event's JSON.
     * @throws LlmException if the event is not JSON or reports an error.
     */
    fun accept(data: String) {
        val event = parseJsonObject(data)
        when (event.text("type")) {
            "content_block_delta" -> event["delta"]?.jsonObject?.takeIf { it.text("type") == "text_delta" }?.text("text")?.let(text::append)
            "message_delta" -> event["delta"]?.jsonObject?.text("stop_reason")?.let { stopReason = it }
            "error" -> throw LlmException(event["error"]?.jsonObject?.text("message") ?: "Anthropic reported an error")
        }
    }

    /**
     * The complete reply once the stream has ended.
     *
     * @return the text.
     * @throws LlmException if the model was cut off at its output limit or wrote no text.
     */
    fun result(): String {
        if (stopReason == "max_tokens") throw LlmException("The reply was cut off because it reached the model's output limit")
        return text.toString().takeIf { it.isNotBlank() } ?: throw LlmException("Empty response from Anthropic")
    }
}
