package dev.denlogv.lexislearned.ai

import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * A local server for tests that answers every request with a server-sent event stream it writes piece by piece, so a test decides
 * when each piece is sent and can leave the stream silent. Requests are served at the same time, each on its own thread, so a
 * stream that is kept silent does not hold up a retry.
 *
 * @param script writes one stream: it is given a function that sends text to the client at once, and the stream ends when it
 * returns. It runs once per request.
 */
class SseTestServer(private val script: (send: (String) -> Unit) -> Unit) : AutoCloseable {
    private val socket = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))

    /** The address to send requests to. */
    val url: String get() = "http://127.0.0.1:${socket.localPort}"

    init {
        thread(isDaemon = true) {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { runCatching { serve(client) } }
            }
        }
    }

    /**
     * Answers one request with the stream [script] writes.
     *
     * @param client the connection; closed afterwards.
     */
    private fun serve(client: Socket) = client.use {
        readRequest(BufferedInputStream(it.getInputStream()))
        val out = it.getOutputStream()
        out.write("HTTP/1.1 200 OK\r\nContent-Type: $EVENT_STREAM\r\nConnection: close\r\n\r\n".toByteArray())
        out.flush()
        script { text ->
            out.write(text.toByteArray())
            out.flush()
        }
    }

    /**
     * Reads the request line, the headers and the body, which this server ignores.
     *
     * @param input the connection's input.
     */
    private fun readRequest(input: BufferedInputStream) {
        var length = 0
        while (true) {
            val line = generateSequence {
                input.read().takeIf { it >= 0 }
            }.takeWhile { it != '\n'.code }.map { it.toChar() }.joinToString("").trim()
            if (line.isEmpty()) break
            if (line.startsWith("content-length:", ignoreCase = true)) length = line.substringAfter(":").trim().toInt()
        }
        input.readNBytes(length)
    }

    /** Stops the server. */
    override fun close() = socket.close()
}
