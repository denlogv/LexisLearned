package dev.denlogv.lexislearned.ai

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Minimal HTTP client for the LLM providers: timeouts, retries of overloaded servers, readable errors. Replies of the model are
 * read as a stream, so a long reply is never cut short by a limit on the total time, only by the server going silent.
 *
 * @param retryDelayMs the pause before the first retry; it doubles for each further retry.
 * @param idleTimeoutMs how long a streamed reply may stay completely silent before the request fails.
 */
internal class HttpJson(
    private val retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS,
    private val idleTimeoutMs: Int = DEFAULT_IDLE_TIMEOUT_MS,
) {
    /**
     * A raw HTTP response.
     *
     * @property code the status code.
     * @property text the body as text.
     */
    private class Response(val code: Int, val text: String)

    /**
     * POSTs a JSON body and passes on the reply as it arrives. A reply that is a server-sent event stream is delivered event by
     * event (the text after `data:`), so the caller sees the first part while the model is still writing; any other reply is
     * delivered whole, as a single event, for servers that ignore the request to stream. Rate limiting (429), server errors (5xx)
     * and connections that fail or time out are retried as long as nothing was delivered yet, because a deck is paid for per
     * request and a brief network drop should not cost a whole section; once part of the reply was delivered, a retry would hand
     * it over twice.
     *
     * @param url the address.
     * @param headers the request headers, for example the API key.
     * @param body the JSON body.
     * @param onData called with the data of each event, on a background thread, in order. It may throw [LlmException] to abort.
     * @throws LlmException if the request is rejected or all attempts fail on the server's side.
     * @throws IOException if every attempt fails to connect or to read, or the connection fails or stays silent for longer than the
     * idle timeout after part of the reply was delivered.
     */
    suspend fun stream(url: String, headers: Map<String, String>, body: JsonObject, onData: (String) -> Unit) {
        var delivered = false
        val tracked: (String) -> Unit = {
            delivered = true
            onData(it)
        }
        var failure: Exception = LlmException("No request was made")
        repeat(MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(retryDelayMs shl (attempt - 1))
            failure = streamOnce(url, headers, body.toString(), tracked) { delivered } ?: return
        }
        throw failure
    }

    /**
     * Makes one attempt of [stream] and decides whether its failure is worth another.
     *
     * @param url the address.
     * @param headers the request headers.
     * @param body the JSON body as text.
     * @param onData receives the data of each event.
     * @param delivered whether any data was handed to [onData] so far, in this attempt or an earlier one.
     * @return null if the reply was read to the end, or the failure to retry: a rate limit, a server error or a connection that
     * failed or went silent, while nothing was delivered yet.
     * @throws LlmException if the request is rejected, or fails after part of the reply was delivered.
     * @throws IOException if the connection fails or goes silent after part of the reply was delivered.
     */
    private suspend fun streamOnce(
        url: String,
        headers: Map<String, String>,
        body: String,
        onData: (String) -> Unit,
        delivered: () -> Boolean,
    ): Exception? = try {
        exchange("POST", url, headers, body, idleTimeoutMs) { readEvents(it, onData) }
        null
    } catch (e: LlmException) {
        if (delivered() || !isRetryable(e.code)) throw e
        e
    } catch (e: IOException) {
        if (delivered()) throw e
        e
    }

    /**
     * GETs a JSON document. Not retried.
     *
     * @param url the address.
     * @param headers the request headers.
     * @return the reply as a JSON object.
     * @throws LlmException if the server answers with an error.
     */
    suspend fun get(url: String, headers: Map<String, String>): JsonObject {
        val response = exchange("GET", url, headers, null, GET_READ_TIMEOUT_MS) { Response(it.responseCode, bodyOf(it)) }
        if (response.code !in SUCCESS) throw failureOf(response)
        return parseJsonObject(response.text)
    }

    /**
     * Performs one request on the IO dispatcher.
     *
     * @param <T> what the reader returns.
     * @param method the HTTP method.
     * @param url the address.
     * @param headers the request headers.
     * @param body the request body, or null for none.
     * @param readTimeoutMs how long a single read may wait for the server.
     * @param read reads the answer from the connection, also for error status codes.
     * @return what [read] returned.
     * @throws kotlinx.coroutines.CancellationException if the caller is cancelled; the request is abandoned at once.
     */
    private suspend fun <T> exchange(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
        readTimeoutMs: Int,
        read: (HttpURLConnection) -> T,
    ): T = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        // A blocking read ignores coroutine cancellation; closing the connection is what ends it early.
        val watcher = launch {
            try {
                awaitCancellation()
            } finally {
                conn.disconnect()
            }
        }
        try {
            conn.requestMethod = method
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = readTimeoutMs
            conn.setRequestProperty("content-type", "application/json")
            headers.forEach { (name, value) -> conn.setRequestProperty(name, value) }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            read(conn)
        } catch (e: IOException) {
            ensureActive() // Cancelled: report that instead of the "socket closed" error our own disconnect caused.
            throw e
        } finally {
            watcher.cancel()
            conn.disconnect()
        }
    }

    /**
     * Reads the answer to a streaming request and hands its events to [onData].
     *
     * @param conn the connection a request was sent on.
     * @param onData receives the data of each event, or the whole body for an answer that is not an event stream.
     * @throws LlmException if the server answered with an error status.
     */
    private fun readEvents(conn: HttpURLConnection, onData: (String) -> Unit) {
        if (conn.responseCode !in SUCCESS) throw failureOf(Response(conn.responseCode, bodyOf(conn)))
        conn.inputStream.bufferedReader().use { reader ->
            if (conn.contentType.orEmpty().startsWith(EVENT_STREAM)) {
                reader.lineSequence().mapNotNull(::dataOf).forEach(onData)
            } else {
                onData(reader.readText())
            }
        }
    }

    /**
     * The text of a response body, for successful and failed responses alike.
     *
     * @param conn the connection a request was sent on.
     * @return the body, empty if there is none.
     */
    private fun bodyOf(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode < HTTP_ERROR) conn.inputStream else conn.errorStream
        return stream?.readBytes()?.toString(Charsets.UTF_8).orEmpty()
    }

    /**
     * The payload of one line of an event stream.
     *
     * @param line a line of the stream.
     * @return the text after `data:`, or null for any other line (event names, comments, blank lines) and for an empty payload.
     */
    private fun dataOf(line: String): String? =
        line.takeIf { it.startsWith("data:") }?.removePrefix("data:")?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * Builds the error for a failed response, using the provider's own message when it gave one.
     *
     * @param response the failed response.
     * @return the exception to throw.
     */
    private fun failureOf(response: Response): LlmException {
        val message = runCatching {
            json.parseToJsonElement(response.text).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: response.text.take(ERROR_PREVIEW_CHARS)
        return LlmException("HTTP ${response.code}: $message", response.code)
    }

    /**
     * Whether a failed request is worth repeating.
     *
     * @param code the HTTP status code.
     * @return true for rate limiting (429) and server errors (5xx).
     */
    private fun isRetryable(code: Int): Boolean = code == TOO_MANY_REQUESTS || code >= SERVER_ERROR

    private companion object {
        const val DEFAULT_RETRY_DELAY_MS = 2000L
        const val DEFAULT_IDLE_TIMEOUT_MS = 120_000
        const val MAX_ATTEMPTS = 4
        const val CONNECT_TIMEOUT_MS = 20_000
        const val EVENT_STREAM = "text/event-stream"
        const val GET_READ_TIMEOUT_MS = 30_000
        const val HTTP_ERROR = 400
        const val TOO_MANY_REQUESTS = 429
        const val SERVER_ERROR = 500
        const val ERROR_PREVIEW_CHARS = 200
        val SUCCESS = 200..299
        val json = Json { ignoreUnknownKeys = true }
    }
}
