package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.Provider
import dev.denlogv.lexislearned.epub.EpubBook
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GenerationProgressTest {
    @get:Rule val main = MainDispatcherRule()

    private val fx = GenerationFixture()

    /** Starts the test book and gives the model's first reply; returns once the second section's request is in flight. */
    private suspend fun GenerationFixture.GatedLlm.runUntilTheSecondRequest(): EpubBook {
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en")
        release()
        fx.manager.state.await { it is GenState.Running && it.deckId != null }
        withTimeout(10_000) {
            started.receive()
            started.receive()
        }
        return book
    }

    private suspend fun pausedAfterTheFirstChapter(llm: GenerationFixture.GatedLlm): GenState.Paused {
        llm.runUntilTheSecondRequest()
        fx.manager.pauseAfterSection()
        fx.manager.pauseNow() // the second request is never released: only giving it up can end it
        return fx.manager.state.await { it is GenState.Paused } as GenState.Paused
    }

    @Test
    fun chaptersAppearInTheLibraryWhileLaterOnesAreStillBeingGenerated() = runBlocking {
        val llm = fx.startGated()
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en")
        llm.release()
        val running = fx.manager.state.await { it is GenState.Running && it.deckId != null } as GenState.Running
        assertEquals("Lorem Book", running.title)
        assertEquals(2, running.cards)
        withTimeout(10_000) {
            llm.started.receive()
            llm.started.receive()
        }
        assertEquals(2, fx.library.decks(1).first().single().total) // the second chapter is still being generated
        llm.release()
        val done = fx.manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(3, done.cards)
        assertEquals(listOf("01 Chapter One", "02 Chapter Two"), fx.storage.export(done.deckId).chapters.map { it.title })
    }

    @Test
    fun pausingWaitsForTheSectionInProgressAndKeepsItsCards() = runBlocking {
        val llm = fx.startGated()
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en")
        withTimeout(10_000) { llm.started.receive() }
        fx.manager.pauseAfterSection()
        assertTrue((fx.manager.state.value as GenState.Running).pausing)
        llm.release()
        val paused = fx.manager.state.await { it is GenState.Paused } as GenState.Paused
        assertEquals(2, paused.cards)
        assertEquals(setOf(3), paused.remaining)
        assertEquals(1, fx.storage.export(paused.deckId!!).chapters.size)
    }

    @Test
    fun pausingNowGivesUpTheRequestInFlight() = runBlocking {
        val llm = fx.startGated()
        val book = llm.runUntilTheSecondRequest()
        fx.manager.pauseAfterSection()
        assertEquals(GenState.Running::class, fx.manager.state.value::class) // the second request is still being waited for
        fx.manager.pauseNow()
        val paused = fx.manager.state.await { it is GenState.Paused } as GenState.Paused
        assertEquals(2, paused.cards)
        assertEquals(setOf(3), paused.remaining)
        assertEquals(book, paused.book)
    }

    @Test
    fun pausingBeforeAnythingIsFinishedKeepsEverythingToDo() = runBlocking {
        val llm = fx.startGated()
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en")
        withTimeout(10_000) { llm.started.receive() }
        fx.manager.pauseAfterSection()
        fx.manager.pauseNow()
        val paused = fx.manager.state.await { it is GenState.Paused } as GenState.Paused
        assertEquals(book.defaultSelection, paused.remaining)
        assertNull(paused.deckId)
        assertTrue(fx.library.decks(1).first().isEmpty())
    }

    @Test
    fun resumingAddsTheRestToTheSameDeck() = runBlocking {
        val llm = fx.startGated()
        val paused = pausedAfterTheFirstChapter(llm)
        fx.manager.resume()
        assertEquals(paused.deckId, (fx.manager.state.value as GenState.Running).deckId)
        llm.release()
        val done = fx.manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(paused.deckId, done.deckId)
        assertEquals(3, done.cards)
        assertEquals(listOf("01 Chapter One", "02 Chapter Two"), fx.storage.export(done.deckId).chapters.map { it.title })
        assertEquals(1, fx.library.decks(1).first().size)
        assertNull(fx.store.load())
    }

    @Test
    fun choosingTheSectionsAddsThemToTheSameDeckToo() = runBlocking {
        val llm = fx.startGated()
        val paused = pausedAfterTheFirstChapter(llm)
        val rest = GenState.Continuation(paused.deckId, paused.remaining)
        fx.manager.start(paused.book, rest.selection, "en", rest)
        llm.release()
        val done = fx.manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(paused.deckId, done.deckId)
        assertEquals(3, done.cards)
    }

    @Test
    fun aSectionThatFailedIsRetriedWithoutPayingForTheOthersAgain() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        fx.answer = { _, user ->
            when {
                user.startsWith("Give") -> "{}"
                user.contains("Chapter Two") -> throw LlmException("HTTP 500: busy", 500)
                else -> GenerationFixture.CARDS_REPLY
            }
        }
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en")
        val first = fx.manager.state.await { it is GenState.Paused } as GenState.Paused
        assertEquals(setOf(3), first.remaining)
        assertEquals(1, first.failed.size)
        val asked = mutableListOf<String>()
        fx.answer = { _, user ->
            if (!user.startsWith("Give")) asked += user
            if (user.startsWith("Give")) "{}" else GenerationFixture.SECOND_REPLY
        }
        fx.manager.resume()
        val done = fx.manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(first.deckId, done.deckId)
        assertEquals(3, done.cards)
        assertEquals(1, asked.size) // only the failed section was sent again
        assertTrue(asked.single().contains("Chapter Two"))
    }

    @Test
    fun resumingADeckThatWasDeletedKeepsTheSectionsAndSaysWhy() = runBlocking {
        val llm = fx.startGated()
        val paused = pausedAfterTheFirstChapter(llm)
        fx.library.delete(paused.deckId!!)
        fx.manager.resume()
        val again = fx.manager.state.await { it is GenState.Paused && it.failed.isNotEmpty() } as GenState.Paused
        assertTrue(again.failed.single().contains("not found"))
        assertEquals(paused.remaining, again.remaining)
    }
}
