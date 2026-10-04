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
 * Minimal JSON-over-HTTP client for the LLM providers: timeouts, retries of overloaded servers, readable errors.
 *
 * @param retryDelayMs the pause before the first retry; it doubles for each further retry.
 */
internal class HttpJson(private val retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS) {
    /**
     * A raw HTTP response.
     *
     * @property code the status code.
     * @property text the body as text.
     */
    private class Response(val code: Int, val text: String)

    /**
     * POSTs a JSON body and parses the JSON reply. Rate limiting (429) and server errors (5xx) are retried.
     *
     * @param url the address.
     * @param headers the request headers, for example the API key.
     * @param body the JSON body.
     * @return the reply as a JSON object.
     * @throws LlmException if the request is rejected or all attempts fail.
     */
    suspend fun post(url: String, headers: Map<String, String>, body: JsonObject): JsonObject {
        var failure = LlmException("No request was made")
        repeat(MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(retryDelayMs shl (attempt - 1))
            val response = send("POST", url, headers, body.toString(), POST_READ_TIMEOUT_MS)
            if (response.code in SUCCESS) return parse(response.text)
            failure = failureOf(response)
            if (!isRetryable(response.code)) throw failure
        }
        throw failure
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
        val response = send("GET", url, headers, null, GET_READ_TIMEOUT_MS)
        if (response.code !in SUCCESS) throw failureOf(response)
        return parse(response.text)
    }

    /**
     * Performs one request on the IO dispatcher.
     *
     * @param method the HTTP method.
     * @param url the address.
     * @param headers the request headers.
     * @param body the request body, or null for none.
     * @param readTimeoutMs how long to wait for the reply.
     * @return the response, also for error status codes.
     * @throws kotlinx.coroutines.CancellationException if the caller is cancelled; the request is abandoned at once.
     */
    private suspend fun send(method: String, url: String, headers: Map<String, String>, body: String?, readTimeoutMs: Int): Response =
        withContext(Dispatchers.IO) {
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
                val stream = if (conn.responseCode < HTTP_ERROR) conn.inputStream else conn.errorStream
                Response(conn.responseCode, stream?.readBytes()?.toString(Charsets.UTF_8).orEmpty())
            } catch (e: IOException) {
                ensureActive() // Cancelled: report that instead of the "socket closed" error our own disconnect caused.
                throw e
            } finally {
                watcher.cancel()
                conn.disconnect()
            }
        }

    /**
     * Parses a reply body.
     *
     * @param text the body.
     * @return the JSON object.
     * @throws LlmException if the body is not a JSON object.
     */
    private fun parse(text: String): JsonObject = try {
        json.parseToJsonElement(text).jsonObject
    } catch (e: IllegalArgumentException) {
        throw LlmException("The server did not return JSON: ${e.message}", cause = e)
    }

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
        const val MAX_ATTEMPTS = 4
        const val CONNECT_TIMEOUT_MS = 20_000
        const val POST_READ_TIMEOUT_MS = 180_000
        const val GET_READ_TIMEOUT_MS = 30_000
        const val HTTP_ERROR = 400
        const val TOO_MANY_REQUESTS = 429
        const val SERVER_ERROR = 500
        const val ERROR_PREVIEW_CHARS = 200
        val SUCCESS = 200..299
        val json = Json { ignoreUnknownKeys = true }
    }
}
