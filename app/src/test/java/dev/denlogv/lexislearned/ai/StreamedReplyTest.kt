package dev.denlogv.lexislearned.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Anthropic and OpenAI clients put a reply together from the events of a stream. */
class StreamedReplyTest {
    private val fast = HttpJson(retryDelayMs = 1)

    private fun anthropic(server: TestServer) = AnthropicClient("sk-test", "model-x", server.url, fast)

    private fun openAi(server: TestServer) = OpenAiClient("sk-test", "gpt-x", server.url, fast)

    private fun failureOf(block: suspend () -> Unit): LlmException = assertThrows(LlmException::class.java) { runBlocking { block() } }

    @Test
    fun anthropicRequestsAStreamAndJoinsTheTextDeltas() = runBlocking {
        TestServer(EVENT_STREAM) { _, _ -> 200 to anthropicStream("Hel", "lo") }.use { server ->
            assertEquals("Hello", anthropic(server).complete("sys", "hi"))
            val seen = server.requests.single { it.path == "/v1/messages" }
            assertEquals("sk-test", seen.headers["x-api-key"])
            assertTrue(seen.body.contains(""""stream":true"""))
            assertTrue(seen.body.contains(""""system":"sys""""))
        }
    }

    @Test
    fun anthropicEmptyAnswerIsAnError() {
        TestServer(EVENT_STREAM) { _, _ -> 200 to anthropicStream() }.use { server ->
            assertEquals("Empty response from Anthropic", failureOf { anthropic(server).complete("s", "u") }.message)
        }
    }

    @Test
    fun anthropicReplyCutOffAtTheLimitIsReportedAsSuch() {
        TestServer(EVENT_STREAM) { _, _ -> 200 to anthropicStream("{\\\"cards\\\":[", stopReason = "max_tokens") }.use { server ->
            assertTrue(failureOf { anthropic(server).complete("s", "u") }.message!!.contains("cut off"))
        }
    }

    @Test
    fun anthropicErrorEventsAreReported() {
        val error = """{"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}"""
        TestServer(EVENT_STREAM) { _, _ ->
            200 to sse("""{"type":"content_block_delta","delta":{"type":"text_delta","text":"x"}}""", error)
        }.use { server ->
            assertEquals("Overloaded", failureOf { anthropic(server).complete("s", "u") }.message)
        }
    }

    @Test
    fun openAiRequestsAStreamAndJoinsTheChunks() = runBlocking {
        TestServer(EVENT_STREAM) { _, _ -> 200 to openAiStream("{\\\"a\\\"", ":1}") }.use { server ->
            assertEquals("""{"a":1}""", openAi(server).complete("sys", "hi"))
            val seen = server.requests.single()
            assertEquals("/chat/completions", seen.path)
            assertEquals("Bearer sk-test", seen.headers["authorization"])
            assertTrue(seen.body.contains(""""stream":true"""))
            assertTrue(seen.body.contains("json_object"))
        }
    }

    @Test
    fun openAiReplyCutOffAtTheLimitIsReportedAsSuch() {
        TestServer(EVENT_STREAM) { _, _ -> 200 to openAiStream("{", finishReason = "length") }.use { server ->
            assertTrue(failureOf { openAi(server).complete("s", "u") }.message!!.contains("cut off"))
        }
    }

    @Test
    fun openAiErrorsInTheStreamAreReported() {
        val chunk = """{"choices":[{"delta":{"content":"x"}}]}"""
        TestServer(EVENT_STREAM) { _, _ -> 200 to sse(chunk, """{"error":{"message":"model crashed"}}""") }.use { server ->
            assertEquals("model crashed", failureOf { openAi(server).complete("s", "u") }.message)
        }
        TestServer(EVENT_STREAM) { _, _ -> 200 to sse("""{"error":"plain"}""") }.use { server ->
            assertTrue(failureOf { openAi(server).complete("s", "u") }.message!!.contains("plain"))
        }
    }

    @Test
    fun aServerThatIgnoresStreamingAnswersWithOneCompleteReply() = runBlocking {
        TestServer("application/json") { _, _ ->
            200 to """{"choices":[{"message":{"content":"all at once"},"finish_reason":"stop"}]}"""
        }.use { server ->
            assertEquals("all at once", openAi(server).complete("s", "u"))
        }
    }

    @Test
    fun eventsThatAreNotJsonObjectsAreReportedAsAProviderProblem() {
        val cases = mapOf(
            "{broken" to "did not return JSON",
            "<html>" to "did not return a JSON object",
            "[1,2]" to "did not return a JSON object",
        )
        for ((data, expected) in cases) {
            TestServer(EVENT_STREAM) { _, _ -> 200 to sse(data) }.use { server ->
                assertTrue(failureOf { openAi(server).complete("s", "u") }.message!!.contains(expected))
                assertTrue(failureOf { anthropic(server).complete("s", "u") }.message!!.contains(expected))
            }
        }
    }
}
