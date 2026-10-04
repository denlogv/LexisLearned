package dev.denlogv.lexislearned.ai

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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

class HttpStreamTest {
    private val fast = HttpJson(retryDelayMs = 1)

    private suspend fun HttpJson.events(url: String): List<String> {
        val seen = mutableListOf<String>()
        stream(url, emptyMap(), buildJsonObject {}) { seen += it }
        return seen
    }

    @Test
    fun streamSendsJsonAndHeadersAndDeliversEventsInOrder() = runBlocking {
        TestServer(EVENT_STREAM) { _, _ -> 200 to sse("one", "two") }.use { server ->
            val seen = mutableListOf<String>()
            fast.stream(server.url + "/x", mapOf("x-test" to "1"), buildJsonObject { put("a", "b") }) { seen += it }
            assertEquals(listOf("one", "two"), seen)
            val request = server.requests.single()
            assertEquals("POST", request.method)
            assertEquals("1", request.headers["x-test"])
            assertEquals("""{"a":"b"}""", request.body)
        }
    }

    @Test
    fun streamIgnoresEventNamesCommentsBlankPayloadsAndAcceptsDataWithoutASpace() = runBlocking {
        val body = "event: x\ndata: a\n\n: keep-alive\n\ndata:b\n\ndata:\n\nid: 7\n"
        TestServer(EVENT_STREAM) { _, _ -> 200 to body }.use { server ->
            assertEquals(listOf("a", "b"), fast.events(server.url))
        }
    }

    @Test
    fun anAnswerThatIsNotAnEventStreamIsDeliveredWhole() = runBlocking {
        TestServer("application/json") { _, _ -> 200 to """{"ok":true}""" }.use { server ->
            assertEquals(listOf("""{"ok":true}"""), fast.events(server.url))
        }
    }

    @Test
    fun eventsAreDeliveredWhileTheServerIsStillWriting() = runBlocking {
        val firstArrived = CountDownLatch(1)
        var firstWasSeenBeforeTheSecondWasSent = false
        SseTestServer { send ->
            send(sse("one"))
            firstWasSeenBeforeTheSecondWasSent = firstArrived.await(5, TimeUnit.SECONDS)
            send(sse("two"))
        }.use { server ->
            val seen = mutableListOf<String>()
            fast.stream(server.url, emptyMap(), buildJsonObject {}) {
                seen += it
                firstArrived.countDown()
            }
            assertEquals(listOf("one", "two"), seen)
            assertTrue(firstWasSeenBeforeTheSecondWasSent)
        }
    }

    @Test
    fun aStreamThatGoesSilentFailsOnceTheIdleTimeoutPasses() {
        val keepSilent = CountDownLatch(1)
        SseTestServer { send ->
            send(sse("one"))
            keepSilent.await(10, TimeUnit.SECONDS)
        }.use { server ->
            val seen = mutableListOf<String>()
            assertThrows(IOException::class.java) {
                runBlocking {
                    HttpJson(retryDelayMs = 1, idleTimeoutMs = 300).stream(server.url, emptyMap(), buildJsonObject {}) {
                        seen +=
                            it
                    }
                }
            }
            assertEquals(listOf("one"), seen) // delivered before the silence, and not again: a retry would repeat it
            keepSilent.countDown()
        }
    }

    @Test
    fun serverErrorsAreRetriedThenReported() {
        TestServer { _, _ -> 503 to """{"error":{"message":"busy"}}""" }.use { server ->
            val e = assertThrows(LlmException::class.java) { runBlocking { fast.events(server.url) } }
            assertEquals(503, e.code)
            assertEquals("HTTP 503: busy", e.message)
            assertEquals(4, server.requests.size)
        }
    }

    @Test
    fun clientErrorsAreNotRetried() {
        TestServer { _, _ -> 401 to "plain text failure" }.use { server ->
            val e = assertThrows(LlmException::class.java) { runBlocking { fast.events(server.url) } }
            assertEquals(401, e.code)
            assertTrue(e.message!!.contains("plain text failure"))
            assertEquals(1, server.requests.size)
        }
    }

    @Test
    fun aRetrySucceedsWhenTheServerRecovers() = runBlocking {
        var calls = 0
        TestServer(EVENT_STREAM) { _, _ -> if (++calls < 3) 429 to "" else 200 to sse("{}") }.use { server ->
            assertEquals(listOf("{}"), fast.events(server.url))
            assertEquals(3, server.requests.size)
        }
    }

    @Test
    fun aDroppedConnectionIsRetriedUntilTheServerAnswers() = runBlocking {
        var calls = 0
        TestServer(EVENT_STREAM) { _, _ -> if (++calls < 3) error("hang up without answering") else 200 to sse("{}") }.use { server ->
            assertEquals(listOf("{}"), fast.events(server.url))
            assertTrue(server.requests.size >= 3) // the JDK may repeat a dropped request by itself, so this is a lower bound
        }
    }

    @Test
    fun aServerThatNeverAnswersEndsInTheConnectionError() {
        TestServer(EVENT_STREAM) { _, _ -> error("hang up without answering") }.use { server ->
            assertThrows(IOException::class.java) { runBlocking { fast.events(server.url) } }
            assertTrue(server.requests.size >= 4)
        }
    }

    @Test
    fun aFailureRaisedWhileReadingStopsTheStreamAndIsNotRetried() {
        TestServer(EVENT_STREAM) { _, _ -> 200 to sse("good", "bad", "never") }.use { server ->
            val seen = mutableListOf<String>()
            val e = assertThrows(LlmException::class.java) {
                runBlocking {
                    fast.stream(server.url, emptyMap(), buildJsonObject {}) {
                        if (it == "bad") throw LlmException("model said no")
                        seen += it
                    }
                }
            }
            assertEquals("model said no", e.message)
            assertEquals(listOf("good"), seen)
            assertEquals(1, server.requests.size)
        }
    }

    @Test
    fun cancellingAbandonsARequestThatIsStillWaitingForTheServer() = runBlocking {
        val received = CountDownLatch(1)
        val release = CountDownLatch(1)
        TestServer(EVENT_STREAM) { _, _ ->
            received.countDown()
            release.await()
            200 to sse("{}")
        }.use { server ->
            val call = launch(Dispatchers.Default) { fast.events(server.url) }
            withContext(Dispatchers.IO) { received.await() }
            withTimeout(5_000) { call.cancelAndJoin() }
            assertTrue(call.isCancelled)
            release.countDown()
        }
    }
}
