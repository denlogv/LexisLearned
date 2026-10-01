package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.DeckRepository
import dev.denlogv.lexislearned.data.Provider
import dev.denlogv.lexislearned.data.memoryDb
import dev.denlogv.lexislearned.data.testSettings
import dev.denlogv.lexislearned.loremEpub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GenerationManagerTest {
    @get:Rule val main = MainDispatcherRule()

    private val settings = testSettings()
    private val repo = DeckRepository(memoryDb())
    private var answer: (String, String) -> String = { _, user -> if (user.startsWith("Give")) "{}" else cardsReply }
    private val created = mutableListOf<Triple<Provider, String, String>>()
    private val manager = GenerationManager(CoroutineScope(Dispatchers.Unconfined), repo, settings) { provider, key, model ->
        created += Triple(provider, key, model)
        ScriptedLlm(answer)
    }

    private val cardsReply = """{"chapter_title_b":"Глава","cards":[{"a":"alpha","b":"альфа"},{"a":"beta","b":"бета"}]}"""

    private suspend fun ready() = (manager.state.also { manager.load(loremEpub()) }.await { it is GenState.Ready } as GenState.Ready).book

    @Test
    fun loadingABookMovesToReview() = runBlocking {
        assertEquals("Lorem Book", ready().title)
    }

    @Test
    fun anUnreadableFileFails() = runBlocking {
        manager.load(byteArrayOf(1, 2, 3))
        assertTrue(manager.state.await { it is GenState.Failed } is GenState.Failed)
        manager.reset()
        assertEquals(GenState.Idle, manager.state.value)
    }

    @Test
    fun startingWithoutAKeyFailsAtOnce() = runBlocking {
        val book = ready()
        manager.start(book, book.defaultSelection, "en", 8)
        val failed = manager.state.value as GenState.Failed
        assertTrue(failed.message.contains("API key"))
        assertEquals(book, failed.book)
    }

    @Test
    fun generationImportsTheDeck() = runBlocking {
        settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        val book = ready()
        manager.start(book, book.defaultSelection, "en", 8)
        val done = manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(2, done.cards) // the second chapter repeats the first one's words, which are dropped
        assertTrue(done.failed.isEmpty())
        assertEquals(1, repo.decks(1).first().size)
        assertEquals(Triple(Provider.ANTHROPIC, "sk-test", Provider.ANTHROPIC.defaultModel), created.single())
        manager.reset()
        assertEquals(GenState.Idle, manager.state.value)
    }

    @Test
    fun failingEverySectionEndsInFailure() = runBlocking {
        settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        answer = { _, user -> if (user.startsWith("Give")) "{}" else "garbage" }
        val book = ready()
        manager.start(book, book.defaultSelection, "en", 8)
        val failed = manager.state.await { it is GenState.Failed } as GenState.Failed
        assertTrue(failed.message.isNotBlank())
    }

    @Test
    fun aRejectedKeyIsReported() = runBlocking {
        settings.setApiKey(Provider.ANTHROPIC, "sk-bad")
        answer = { _, _ -> throw LlmException("HTTP 401: invalid x-api-key", 401) }
        val book = ready()
        manager.start(book, book.defaultSelection, "en", 8)
        val failed = manager.state.await { it is GenState.Failed } as GenState.Failed
        assertTrue(failed.message.contains("401"))
    }

    @Test
    fun cancellingKeepsWhatIsFinished() = runBlocking {
        settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        val book = ready()
        manager.cancel()
        answer = { _, user ->
            if (!user.startsWith("Give")) manager.cancel()
            if (user.startsWith("Give")) "{}" else cardsReply
        }
        manager.start(book, book.defaultSelection, "en", 8)
        val done = manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(2, done.cards)
    }

    @Test
    fun failBeforeGenerationShowsTheMessage() {
        manager.fail("Could not read the file")
        assertEquals(GenState.Failed("Could not read the file"), manager.state.value)
    }
}
