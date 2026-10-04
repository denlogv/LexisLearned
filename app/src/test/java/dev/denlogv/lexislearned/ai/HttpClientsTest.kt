package dev.denlogv.lexislearned.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpClientsTest {
    private val fast = HttpJson(retryDelayMs = 1)

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
