package dev.denlogv.lexislearned.ai

/** The Content-Type of a server-sent event stream. */
const val EVENT_STREAM = "text/event-stream"

/**
 * A server-sent event stream with one event per payload.
 *
 * @param payloads the data of each event.
 * @return the body of the stream.
 */
fun sse(vararg payloads: String): String = payloads.joinToString("") { "data: $it\n\n" }

/**
 * The stream Anthropic's Messages API sends for a reply: the events around the text, a text delta per part, and the stop reason.
 *
 * @param parts the pieces of text, one delta each.
 * @param stopReason why the model stopped.
 * @return the body of the stream.
 */
fun anthropicStream(vararg parts: String, stopReason: String = "end_turn"): String {
    val deltas = parts.map { """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"$it"}}""" }
    val events = listOf("""{"type":"message_start","message":{"role":"assistant","content":[]}}""") +
        """{"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""" + """{"type":"ping"}""" + deltas +
        """{"type":"content_block_stop","index":0}""" + """{"type":"message_delta","delta":{"stop_reason":"$stopReason"}}""" +
        """{"type":"message_stop"}"""
    return events.joinToString("") { "event: ${it.substringAfter("\"type\":\"").substringBefore('"')}\ndata: $it\n\n" }
}

/**
 * The stream a Chat Completions server sends for a reply: a chunk per part, a last chunk with the finish reason, then `[DONE]`.
 *
 * @param parts the pieces of content, one chunk each.
 * @param finishReason why the model stopped.
 * @return the body of the stream.
 */
fun openAiStream(vararg parts: String, finishReason: String = "stop"): String {
    val chunks = parts.map { """{"choices":[{"index":0,"delta":{"content":"$it"},"finish_reason":null}]}""" } +
        """{"choices":[{"index":0,"delta":{},"finish_reason":"$finishReason"}]}""" + """{"choices":[],"usage":{"total_tokens":3}}"""
    return sse(*chunks.toTypedArray(), "[DONE]")
}
