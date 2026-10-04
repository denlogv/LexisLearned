package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.Provider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals(1, fx.library.decks(1).first().size)
        assertEquals(Triple(Provider.ANTHROPIC, "sk-test", Provider.ANTHROPIC.defaultModel), fx.created.single())
        assertNull(fx.store.load()) // nothing is left to resume
        fx.manager.reset()
        assertEquals(GenState.Idle, fx.manager.state.value)
    }

    @Test
    fun failingEverySectionKeepsTheBookToRetry() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        fx.answer = { _, user -> if (user.startsWith("Give")) "{}" else "garbage" }
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val paused = fx.manager.state.await { it is GenState.Paused } as GenState.Paused
        assertNull(paused.deckId)
        assertEquals(book.defaultSelection, paused.remaining)
        assertEquals(2, paused.failed.size)
        assertEquals(paused.remaining, fx.store.load()!!.record.remaining)
    }

    @Test
    fun aRunWithoutAnyCardsFailsAndCanGoBackToTheReview() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        fx.answer = { _, user -> if (user.startsWith("Give")) "{}" else """{"cards":[]}""" }
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val failed = fx.manager.state.await { it is GenState.Failed } as GenState.Failed
        assertEquals("No cards were generated.", failed.message)
        assertNull(fx.store.load())
        fx.manager.backToReview()
        assertEquals(GenState.Ready(book), fx.manager.state.value)
    }

    @Test
    fun aRejectedKeyIsReportedAndTheRunCanBeRetriedWithAnotherKey() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-bad")
        fx.answer = { _, _ -> throw LlmException("HTTP 401: invalid x-api-key", 401) }
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val paused = fx.manager.state.await { it is GenState.Paused } as GenState.Paused
        assertEquals(listOf("HTTP 401: invalid x-api-key"), paused.failed)
        assertNull(paused.deckId)
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-good")
        fx.answer = { _, user -> if (user.startsWith("Give")) "{}" else GenerationFixture.CARDS_REPLY }
        fx.manager.resume()
        fx.manager.state.await { it is GenState.Finished }
        assertEquals(1, fx.library.decks(1).first().size)
    }

    @Test
    fun pausingKeepsWhatIsFinished() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        val book = fx.ready()
        fx.answer = { _, user ->
            if (!user.startsWith("Give")) fx.manager.pauseAfterSection() // asked while the first section is in progress
            if (user.startsWith("Give")) "{}" else GenerationFixture.CARDS_REPLY
        }
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val paused = fx.manager.state.await { it is GenState.Paused } as GenState.Paused
        assertEquals(2, paused.cards) // the section in progress was finished, not thrown away
        assertEquals(setOf(3), paused.remaining)
        assertTrue(paused.failed.isEmpty())
        assertEquals(paused.remaining, fx.store.load()!!.record.remaining)
    }

    @Test
    fun aFailureAfterSomeChaptersKeepsThemAndOffersTheRest() = runBlocking {
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
        val paused = fx.manager.state.await { it is GenState.Paused } as GenState.Paused
        assertEquals(2, paused.cards)
        assertEquals(listOf("HTTP 401: rejected"), paused.failed)
        assertEquals(setOf(3), paused.remaining)
        assertEquals(1, fx.library.decks(1).first().size)
    }

    @Test
    fun aPausedGenerationThatCannotStartStaysPausedWithTheReason() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        fx.answer = { _, user -> if (user.startsWith("Give")) "{}" else throw LlmException("HTTP 500: busy", 500) }
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val paused = fx.manager.state.await { it is GenState.Paused } as GenState.Paused
        fx.settings.setApiKey(Provider.ANTHROPIC, "")
        fx.manager.resume()
        val still = fx.manager.state.value as GenState.Paused
        assertEquals(paused.remaining, still.remaining)
        assertTrue(still.failed.single().contains("API key"))
        assertEquals(paused.remaining, fx.store.load()!!.record.remaining) // and it can still be resumed after a restart
    }

    @Test
    fun pausingResumingDiscardingAndGoingBackDoNothingOutsideTheirStates() {
        fx.manager.pauseAfterSection()
        fx.manager.pauseNow()
        fx.manager.resume()
        fx.manager.discard()
        fx.manager.backToReview()
        assertEquals(GenState.Idle, fx.manager.state.value)
    }

    @Test
    fun theProcessIsKeptAliveForEveryRunButNotForOneThatCannotStart() = runBlocking {
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8) // no key
        assertEquals(0, fx.held)
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        fx.answer = { _, user -> if (user.startsWith("Give")) "{}" else throw LlmException("HTTP 500: busy", 500) }
        fx.manager.start(book, book.defaultSelection, "en", 8)
        fx.manager.state.await { it is GenState.Paused }
        fx.manager.resume()
        fx.manager.state.await { it is GenState.Paused && it.failed.size == 2 }
        assertEquals(2, fx.held)
    }

    @Test
    fun failBeforeGenerationShowsTheMessage() {
        fx.manager.fail("Could not read the file")
        assertEquals(GenState.Failed("Could not read the file"), fx.manager.state.value)
    }
}
