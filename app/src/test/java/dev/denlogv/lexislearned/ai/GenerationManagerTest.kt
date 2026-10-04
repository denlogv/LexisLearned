package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.Provider
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

    private val fx = GenerationFixture()

    @Test
    fun loadingABookMovesToReview() = runBlocking {
        assertEquals("Lorem Book", fx.ready().title)
    }

    @Test
    fun anUnreadableFileFails() = runBlocking {
        fx.manager.load(byteArrayOf(1, 2, 3))
        assertTrue(fx.manager.state.await { it is GenState.Failed } is GenState.Failed)
        fx.manager.reset()
        assertEquals(GenState.Idle, fx.manager.state.value)
    }

    @Test
    fun startingWithoutAKeyFailsAtOnce() = runBlocking {
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val failed = fx.manager.state.value as GenState.Failed
        assertTrue(failed.message.contains("API key"))
        assertEquals(book, failed.book)
    }

    @Test
    fun aCustomServerNeedsAValidAddressAndAModel() = runBlocking {
        fx.settings.setProvider(Provider.OPENAI_COMPATIBLE)
        fx.settings.setApiKey(Provider.OPENAI_COMPATIBLE, "k")
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        assertTrue((fx.manager.state.value as GenState.Failed).message.contains("server address"))
        fx.settings.setBaseUrl("ftp://nope")
        fx.manager.start(book, book.defaultSelection, "en", 8)
        assertTrue((fx.manager.state.value as GenState.Failed).message.contains("server address"))
        fx.settings.setBaseUrl("http://192.168.1.5:11434/v1/")
        fx.manager.start(book, book.defaultSelection, "en", 8)
        assertTrue((fx.manager.state.value as GenState.Failed).message.contains("model"))
        assertTrue(fx.created.isEmpty())
    }

    @Test
    fun aCustomServerIsUsedOnceConfigured() = runBlocking {
        fx.settings.setProvider(Provider.OPENAI_COMPATIBLE)
        fx.settings.setApiKey(Provider.OPENAI_COMPATIBLE, "k")
        fx.settings.setBaseUrl("http://192.168.1.5:11434/v1/")
        fx.settings.setModel("llama3")
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        fx.manager.state.await { it is GenState.Finished }
        assertEquals(Triple(Provider.OPENAI_COMPATIBLE, "k", "llama3"), fx.created.single())
    }

    @Test
    fun generationImportsTheDeck() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val done = fx.manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(2, done.cards) // the second chapter repeats the first one's words, which are dropped
        assertTrue(done.failed.isEmpty())
        assertEquals(1, fx.library.decks(1).first().size)
        assertEquals(Triple(Provider.ANTHROPIC, "sk-test", Provider.ANTHROPIC.defaultModel), fx.created.single())
        fx.manager.reset()
        assertEquals(GenState.Idle, fx.manager.state.value)
    }

    @Test
    fun failingEverySectionEndsInFailure() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        fx.answer = { _, user -> if (user.startsWith("Give")) "{}" else "garbage" }
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val failed = fx.manager.state.await { it is GenState.Failed } as GenState.Failed
        assertTrue(failed.message.isNotBlank())
    }

    @Test
    fun aRejectedKeyIsReported() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-bad")
        fx.answer = { _, _ -> throw LlmException("HTTP 401: invalid x-api-key", 401) }
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val failed = fx.manager.state.await { it is GenState.Failed } as GenState.Failed
        assertTrue(failed.message.contains("401"))
    }

    @Test
    fun cancellingKeepsWhatIsFinished() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        val book = fx.ready()
        fx.manager.cancel()
        fx.answer = { _, user ->
            if (!user.startsWith("Give")) fx.manager.cancel()
            if (user.startsWith("Give")) "{}" else GenerationFixture.CARDS_REPLY
        }
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val done = fx.manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(2, done.cards)
    }

    @Test
    fun aFailureAfterSomeChaptersKeepsThem() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        fx.answer = { _, user ->
            when {
                user.startsWith("Give") -> "{}"
                user.contains("Chapter Two") -> throw LlmException("HTTP 401: rejected", 401)
                else -> GenerationFixture.CARDS_REPLY
            }
        }
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val done = fx.manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(2, done.cards)
        assertEquals(listOf("HTTP 401: rejected"), done.failed)
        assertEquals(1, fx.library.decks(1).first().size)
    }

    @Test
    fun failBeforeGenerationShowsTheMessage() {
        fx.manager.fail("Could not read the file")
        assertEquals(GenState.Failed("Could not read the file"), fx.manager.state.value)
    }
}
