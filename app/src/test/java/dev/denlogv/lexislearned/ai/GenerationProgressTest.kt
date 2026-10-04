package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
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
}
