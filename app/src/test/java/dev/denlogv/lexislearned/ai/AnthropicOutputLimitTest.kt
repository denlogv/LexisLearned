package dev.denlogv.lexislearned.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Anthropic client asks for the model's own output maximum instead of a number of its own. */
class AnthropicOutputLimitTest {
    private val fast = HttpJson(retryDelayMs = 1)
    private val reply = anthropicStream("ok")

    private fun TestServer.maxTokensSent(): List<String> = requests.filter { it.path == "/v1/messages" }
        .map { Regex(""""max_tokens":(\d+)""").find(it.body)!!.groupValues[1] }

    @Test
    fun requestsAskForTheMaximumTheModelReports() = runBlocking {
        TestServer(EVENT_STREAM) { _, path -> 200 to if (path == "/v1/models/m") """{"max_tokens":64000}""" else reply }.use { server ->
            val client = AnthropicClient("k", "m", server.url, fast)
            client.complete("s", "u")
            client.complete("s", "u")
            assertEquals(listOf("64000", "64000"), server.maxTokensSent())
            assertEquals(1, server.requests.count { it.path == "/v1/models/m" }) // looked up once
        }
    }

    @Test
    fun anUnknownLimitFallsBackAndIsLookedUpAgainNextTime() = runBlocking {
        TestServer(EVENT_STREAM) { _, path ->
            if (path ==
                "/v1/models/m"
            ) {
                404 to """{"error":{"message":"no"}}"""
            } else {
                200 to reply
            }
        }.use { server ->
            val client = AnthropicClient("k", "m", server.url, fast)
            client.complete("s", "u")
            client.complete("s", "u")
            assertEquals(listOf("8192", "8192"), server.maxTokensSent())
            assertEquals(2, server.requests.count { it.path == "/v1/models/m" })
        }
    }

    @Test
    fun aModelWithoutAReportedLimitUsesTheFallback() = runBlocking {
        TestServer(EVENT_STREAM) { _, path ->
            200 to if (path ==
                "/v1/models/m"
            ) {
                """{"id":"m","max_tokens":null}"""
            } else {
                reply
            }
        }.use { server ->
            AnthropicClient("k", "m", server.url, fast).complete("s", "u")
            assertEquals(listOf("8192"), server.maxTokensSent())
        }
    }

    @Test
    fun aRejectedKeyIsNotHiddenByTheFallback() {
        TestServer { _, _ -> 401 to """{"error":{"message":"invalid x-api-key"}}""" }.use { server ->
            val failure = assertThrows(LlmException::class.java) {
                runBlocking { AnthropicClient("bad", "m", server.url, fast).complete("s", "u") }
            }
            assertEquals(401, failure.code)
            assertTrue(server.requests.none { it.path == "/v1/messages" })
        }
    }

    @Test
    fun aReplyCutOffAtTheLimitIsReportedAsSuch() {
        val cutOff = anthropicStream("{", stopReason = "max_tokens")
        TestServer(EVENT_STREAM) { _, path -> 200 to if (path == "/v1/models/m") """{"max_tokens":100}""" else cutOff }.use { server ->
            val failure = assertThrows(LlmException::class.java) {
                runBlocking { AnthropicClient("k", "m", server.url, fast).complete("s", "u") }
            }
            assertTrue(failure.message!!.contains("cut off"))
        }
    }
}
