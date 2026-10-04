package dev.denlogv.lexislearned.ai

import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpClientsTest {
    private val fast = HttpJson(retryDelayMs = 1)

    @Test
    fun postSendsJsonAndParsesTheReply() = runBlocking {
        TestServer { _, _ -> 200 to """{"ok":true}""" }.use { server ->
            val reply = fast.post(server.url + "/x", mapOf("x-test" to "1"), buildJsonObject { put("a", "b") })
            assertEquals("""{"ok":true}""", reply.toString())
            val seen = server.requests.single()
            assertEquals("POST", seen.method)
            assertEquals("1", seen.headers["x-test"])
            assertEquals("""{"a":"b"}""", seen.body)
        }
    }

    @Test
    fun serverErrorsAreRetriedThenReported() {
        TestServer { _, _ -> 503 to """{"error":{"message":"busy"}}""" }.use { server ->
            val e = assertThrows(LlmException::class.java) { runBlocking { fast.post(server.url, emptyMap(), buildJsonObject {}) } }
            assertEquals(503, e.code)
            assertEquals("HTTP 503: busy", e.message)
            assertEquals(4, server.requests.size)
        }
    }

    @Test
    fun clientErrorsAreNotRetried() {
        TestServer { _, _ -> 401 to "plain text failure" }.use { server ->
            val e = assertThrows(LlmException::class.java) { runBlocking { fast.post(server.url, emptyMap(), buildJsonObject {}) } }
            assertEquals(401, e.code)
            assertTrue(e.message!!.contains("plain text failure"))
            assertEquals(1, server.requests.size)
        }
    }

    @Test
    fun aRetrySucceedsWhenTheServerRecovers() = runBlocking {
        var calls = 0
        TestServer { _, _ -> if (++calls < 3) 429 to "" else 200 to "{}" }.use { server ->
            assertEquals("{}", fast.post(server.url, emptyMap(), buildJsonObject {}).toString())
            assertEquals(3, server.requests.size)
        }
    }

    @Test
    fun aDroppedConnectionIsRetriedUntilTheServerAnswers() = runBlocking {
        var calls = 0
        TestServer { _, _ -> if (++calls < 3) error("hang up without answering") else 200 to "{}" }.use { server ->
            assertEquals("{}", fast.post(server.url, emptyMap(), buildJsonObject {}).toString())
            assertTrue(server.requests.size >= 3) // the JDK may repeat a dropped request by itself, so this is a lower bound
        }
    }

    @Test
    fun aServerThatNeverAnswersEndsInTheConnectionError() {
        TestServer { _, _ -> error("hang up without answering") }.use { server ->
            assertThrows(IOException::class.java) { runBlocking { fast.post(server.url, emptyMap(), buildJsonObject {}) } }
            assertTrue(server.requests.size >= 4)
        }
    }

    @Test
    fun nonJsonBodyIsAnError() {
        TestServer { _, _ -> 200 to "<html>" }.use { server ->
            assertThrows(LlmException::class.java) { runBlocking { fast.get(server.url, emptyMap()) } }
        }
    }

    @Test
    fun getReportsFailures() {
        TestServer { _, _ -> 500 to "" }.use { server ->
            val e = assertThrows(LlmException::class.java) { runBlocking { fast.get(server.url, emptyMap()) } }
            assertEquals(500, e.code)
            assertEquals(1, server.requests.size)
        }
    }

    @Test
    fun anthropicClientSendsTheKeyAndJoinsTextBlocks() = runBlocking {
        val reply = """{"content":[{"type":"text","text":"Hel"},{"type":"text","text":"lo"}]}"""
        TestServer { _, _ -> 200 to reply }.use { server ->
            val client = AnthropicClient("sk-test", "model-x", server.url, fast)
            assertEquals("Hello", client.complete("sys", "hi"))
            val seen = server.requests.single()
            assertEquals("/v1/messages", seen.path)
            assertEquals("sk-test", seen.headers["x-api-key"])
            assertTrue(seen.body.contains(""""system":"sys""""))
            assertTrue(seen.body.contains("model-x"))
        }
    }

    @Test
    fun anthropicEmptyAnswerIsAnError() {
        TestServer { _, _ -> 200 to """{"content":[]}""" }.use { server ->
            val client = AnthropicClient("k", "m", server.url, fast)
            assertThrows(LlmException::class.java) { runBlocking { client.complete("s", "u") } }
        }
    }

    @Test
    fun openAiClientReadsTheFirstChoice() = runBlocking {
        TestServer { _, _ -> 200 to """{"choices":[{"message":{"content":"{\"a\":1}"}}]}""" }.use { server ->
            val client = OpenAiClient("sk-test", "gpt-x", server.url, fast)
            assertEquals("""{"a":1}""", client.complete("sys", "hi"))
            val seen = server.requests.single()
            assertEquals("/chat/completions", seen.path)
            assertEquals("Bearer sk-test", seen.headers["authorization"])
            assertTrue(seen.body.contains("json_object"))
        }
    }

    @Test
    fun cancellingAbandonsARequestThatIsStillWaitingForTheServer() = runBlocking {
        val received = CountDownLatch(1)
        val release = CountDownLatch(1)
        TestServer { _, _ ->
            received.countDown()
            release.await()
            200 to "{}"
        }.use { server ->
            val call = launch(Dispatchers.Default) { fast.post(server.url, emptyMap(), buildJsonObject {}) }
            withContext(Dispatchers.IO) { received.await() }
            withTimeout(5_000) { call.cancelAndJoin() }
            assertTrue(call.isCancelled)
            release.countDown()
        }
    }

    @Test
    fun serversThatRefuseResponseFormatAreAskedAgainWithoutItAndRemembered() = runBlocking {
        val reject = """{"error":"'response_format.type' must be 'json_schema' or 'text'"}"""
        val ok = """{"choices":[{"message":{"content":"{}"}}]}"""
        var calls = 0
        TestServer { _, _ -> if (++calls == 1) 400 to reject else 200 to ok }.use { server ->
            val client = OpenAiClient("k", "m", server.url, fast)
            assertEquals("{}", client.complete("s", "u"))
            assertEquals("{}", client.complete("s", "u"))
            assertEquals(listOf(true, false, false), server.requests.map { it.body.contains("response_format") })
        }
    }

    @Test
    fun otherBadRequestsAreNotRetried() {
        TestServer { _, _ -> 400 to """{"error":{"message":"unknown model"}}""" }.use { server ->
            val client = OpenAiClient("k", "m", server.url, fast)
            val e = assertThrows(LlmException::class.java) { runBlocking { client.complete("s", "u") } }
            assertEquals("HTTP 400: unknown model", e.message)
            assertEquals(1, server.requests.size)
        }
    }

    @Test
    fun openAiEmptyAnswerIsAnError() {
        TestServer { _, _ -> 200 to """{"choices":[]}""" }.use { server ->
            val client = OpenAiClient("k", "m", server.url, fast)
            assertThrows(LlmException::class.java) { runBlocking { client.complete("s", "u") } }
        }
    }

    @Test
    fun anthropicCatalogListsModelsWithDisplayNames() = runBlocking {
        val reply = """{"data":[{"id":"claude-a","display_name":"Claude A"},{"id":"claude-b"},{"nope":1}]}"""
        TestServer { _, _ -> 200 to reply }.use { server ->
            val models = ModelCatalog(fast).anthropic("k", server.url)
            assertEquals(listOf(ModelInfo("claude-a", "Claude A"), ModelInfo("claude-b", "claude-b")), models)
            assertEquals("k", server.requests.single().headers["x-api-key"])
        }
    }

    @Test
    fun openAiCatalogKeepsChatModelsNewestFirst() = runBlocking {
        val reply = """{"data":[
            {"id":"gpt-old","created":1},{"id":"text-embedding-3","created":9},{"id":"gpt-new","created":5},
            {"id":"o3","created":3},{"id":"gpt-4o-audio-preview","created":8},{"id":"whisper-1","created":7},{"created":2}]}"""
        TestServer { _, _ -> 200 to reply }.use { server ->
            val models = ModelCatalog(fast).openAi("k", server.url)
            assertEquals(listOf("gpt-new", "o3", "gpt-old"), models.map { it.id })
        }
    }

    @Test
    fun compatibleCatalogKeepsEveryModelNewestFirstThenById() = runBlocking {
        val reply = """{"data":[{"id":"llama3:8b"},{"id":"mistral"},{"id":"qwen","created":5},{"id":"text-embedding-3","created":2},{}]}"""
        TestServer { _, _ -> 200 to reply }.use { server ->
            val models = ModelCatalog(fast).openAiCompatible("k", server.url)
            assertEquals(listOf("qwen", "text-embedding-3", "llama3:8b", "mistral"), models.map { it.id })
            val seen = server.requests.single()
            assertEquals("/models", seen.path)
            assertEquals("Bearer k", seen.headers["authorization"])
        }
    }

    @Test
    fun messageHelperBuildsRoleAndContent() {
        assertEquals("""{"role":"user","content":"x"}""", message("user", "x").toString())
    }
}
