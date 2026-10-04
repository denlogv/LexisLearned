package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.Provider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GenerationProgressTest {
    @get:Rule val main = MainDispatcherRule()

    private val fx = GenerationFixture()

    private suspend fun stopAfterTheFirstChapter(llm: GenerationFixture.GatedLlm): GenState.Finished {
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        llm.release()
        fx.manager.state.await { it is GenState.Running && it.deckId != null }
        withTimeout(10_000) {
            llm.started.receive()
            llm.started.receive()
        }
        fx.manager.cancel()
        return fx.manager.state.await { it is GenState.Finished } as GenState.Finished
    }

    @Test
    fun chaptersAppearInTheLibraryWhileLaterOnesAreStillBeingGenerated() = runBlocking {
        val llm = fx.startGated()
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
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
    fun stoppingKeepsTheFinishedChaptersAndAbandonsTheRequestInFlight() = runBlocking {
        val llm = fx.startGated()
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        llm.release()
        fx.manager.state.await { it is GenState.Running && it.deckId != null }
        withTimeout(10_000) {
            llm.started.receive()
            llm.started.receive()
        }
        fx.manager.cancel() // the second request is never released: only cancelling can end it
        val done = fx.manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(2, done.cards)
        assertEquals(1, fx.storage.export(done.deckId).chapters.size)
        assertEquals(setOf(3), done.unfinished)
        assertEquals(book, done.book)
    }

    @Test
    fun stoppingBeforeAnythingIsFinishedReturnsToTheReview() = runBlocking {
        val llm = fx.startGated()
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        withTimeout(10_000) { llm.started.receive() }
        fx.manager.cancel()
        assertEquals(GenState.Ready(book), fx.manager.state.await { it is GenState.Ready })
        assertTrue(fx.library.decks(1).first().isEmpty())
    }

    @Test
    fun continuingAfterAStopAddsTheRestToTheSameDeck() = runBlocking {
        val llm = fx.startGated()
        val stopped = stopAfterTheFirstChapter(llm)
        val rest = GenState.Continuation(stopped.deckId, stopped.unfinished)
        fx.manager.start(stopped.book!!, rest.selection, "en", 8, rest)
        assertEquals(stopped.deckId, (fx.manager.state.value as GenState.Running).deckId)
        llm.release()
        val done = fx.manager.state.await { it is GenState.Finished && it.unfinished.isEmpty() } as GenState.Finished
        assertEquals(stopped.deckId, done.deckId)
        assertEquals(3, done.cards)
        assertEquals(listOf("01 Chapter One", "02 Chapter Two"), fx.storage.export(done.deckId).chapters.map { it.title })
        assertEquals(1, fx.library.decks(1).first().size)
    }

    @Test
    fun aSectionThatFailedIsOfferedAgain() = runBlocking {
        fx.settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        fx.answer = { _, user ->
            when {
                user.startsWith("Give") -> "{}"
                user.contains("Chapter Two") -> throw LlmException("HTTP 500: busy", 500)
                else -> GenerationFixture.CARDS_REPLY
            }
        }
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        val first = fx.manager.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(setOf(3), first.unfinished)
        assertEquals(1, first.failed.size)
        fx.answer = { _, user -> if (user.startsWith("Give")) "{}" else GenerationFixture.SECOND_REPLY }
        fx.manager.start(book, first.unfinished, "en", 8, GenState.Continuation(first.deckId, first.unfinished))
        val done = fx.manager.state.await { it is GenState.Finished && it.unfinished.isEmpty() } as GenState.Finished
        assertEquals(first.deckId, done.deckId)
        assertEquals(3, done.cards)
        assertTrue(done.failed.isEmpty())
    }

    @Test
    fun continuingADeckThatWasDeletedFails() = runBlocking {
        val llm = fx.startGated()
        val stopped = stopAfterTheFirstChapter(llm)
        fx.library.delete(stopped.deckId)
        fx.manager.start(stopped.book!!, stopped.unfinished, "en", 8, GenState.Continuation(stopped.deckId, stopped.unfinished))
        val failed = fx.manager.state.await { it is GenState.Failed } as GenState.Failed
        assertTrue(failed.message.contains("not found"))
    }
}
