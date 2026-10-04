package dev.denlogv.lexislearned.ai

import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import kotlin.concurrent.thread

/**
 * A tiny local HTTP server for tests, built on plain sockets. Every request is answered by [respond] and recorded.
 *
 * @param contentType the Content-Type of every answer, or null to send none.
 * @param respond decides the answer to a request, given its method and path: (status, body).
 */
class TestServer(private val contentType: String? = null, private val respond: (method: String, path: String) -> Pair<Int, String>) :
    AutoCloseable {
    /**
     * One recorded request.
     *
     * @property method the HTTP method.
     * @property path the request target, including the query.
     * @property headers the request headers with lower-case names.
     * @property body the request body as text.
     */
    class Seen(val method: String, val path: String, val headers: Map<String, String>, val body: String)

    private val socket = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))

    /** All requests received so far, oldest first. */
    val requests: MutableList<Seen> = Collections.synchronizedList(mutableListOf())

    /** The address to send requests to. */
    val url: String get() = "http://127.0.0.1:${socket.localPort}"

    init {
        thread(isDaemon = true) {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                runCatching { serve(client) }
            }
        }
    }

    /**
     * Reads one request from a connection and answers it.
     *
     * @param client the connection; closed afterwards.
     */
    private fun serve(client: Socket) = client.use {
        val input = BufferedInputStream(it.getInputStream())
        val (method, path) = readLine(input).split(" ").let { parts -> parts[0] to parts[1] }
        val headers = generateSequence { readLine(input).takeIf { line -> line.isNotEmpty() } }
            .associate { line -> line.substringBefore(":").lowercase() to line.substringAfter(":").trim() }
        val body = input.readNBytes(headers["content-length"]?.toInt() ?: 0).toString(Charsets.UTF_8)
        requests += Seen(method, path, headers, body)
        val (code, text) = respond(method, path)
        val bytes = text.toByteArray()
        it.getOutputStream().apply {
            val type = contentType?.let { type -> "Content-Type: $type\r\n" }.orEmpty()
            write("HTTP/1.1 $code X\r\n${type}Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
            write(bytes)
            flush()
        }
    }

    /**
     * Reads a line that ends with CRLF.
     *
     * @param input the connection's input.
     * @return the line without its line ending.
     */
    private fun readLine(input: BufferedInputStream): String {
        val out = StringBuilder()
        while (true) {
            val c = input.read()
            if (c < 0 || c == '\n'.code) break
            if (c != '\r'.code) out.append(c.toChar())
        }
        return out.toString()
    }

    /** Stops the server. */
    override fun close() = socket.close()
}
