package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.loremEpub
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

/** A paused generation outlives the app: what a restart brings back, and how it is ended. */
@RunWith(RobolectricTestRunner::class)
class GenerationResumeTest {
    @get:Rule val main = MainDispatcherRule()

    private val fx = GenerationFixture()

    private suspend fun pausedAfterTheFirstChapter(llm: GenerationFixture.GatedLlm): GenState.Paused {
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        llm.release()
        fx.manager.state.await { it is GenState.Running && it.deckId != null }
        withTimeout(10_000) {
            llm.started.receive()
            llm.started.receive()
        }
        fx.manager.pauseAfterSection()
        fx.manager.pauseNow()
        return fx.manager.state.await { it is GenState.Paused } as GenState.Paused
    }

    @Test
    fun aPausedGenerationComesBackAfterARestartAndCanBeResumed() = runBlocking {
        val llm = fx.startGated()
        val paused = pausedAfterTheFirstChapter(llm)
        val restarted = fx.newManager() // the app was closed and started again: nothing is in memory
        assertEquals(GenState.Idle, restarted.state.value)
        restarted.restore()
        val back = restarted.state.await { it is GenState.Paused } as GenState.Paused
        assertEquals(paused, back)
        restarted.resume()
        llm.release()
        val done = restarted.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(paused.deckId, done.deckId)
        assertEquals(3, done.cards)
        assertEquals(1, fx.library.decks(1).first().size)
    }

    @Test
    fun aRunThatWasKilledHalfwayComesBackWithTheChaptersThatWereDone() = runBlocking {
        val llm = fx.startGated()
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        llm.release()
        withTimeout(10_000) {
            llm.started.receive()
            llm.started.receive() // the second section is in flight, as when the process is killed
        }
        val restarted = fx.newManager()
        restarted.restore()
        val back = restarted.state.await { it is GenState.Paused } as GenState.Paused
        assertEquals(setOf(3), back.remaining)
        assertEquals(2, back.cards)
        assertTrue(back.failed.isEmpty())
        assertEquals(book, back.book)
        fx.manager.pauseAfterSection()
        fx.manager.pauseNow() // ends the run of the manager that stays behind
        fx.manager.state.await { it is GenState.Paused }
        Unit
    }

    @Test
    fun theChoicesOfTheRunAreRememberedAcrossARestart() = runBlocking {
        val llm = fx.startGated()
        val paused = pausedAfterTheFirstChapter(llm)
        val record = fx.store.load()!!.record
        assertEquals("en", record.sourceLang)
        assertEquals(8, record.cardsPer1000Words)
        assertEquals(paused.deckId, record.deckId)
    }

    @Test
    fun aResumedRunKeepsTheDeckInItsRecordBeforeItsFirstChapterIsDone() = runBlocking {
        val llm = fx.startGated()
        val paused = pausedAfterTheFirstChapter(llm)
        fx.manager.resume()
        withTimeout(10_000) { llm.started.receive() } // the resumed section is in flight: nothing new is stored yet
        val record = fx.store.load()!!.record
        assertEquals(paused.deckId, record.deckId)
        assertEquals(paused.cards, record.cards)
        assertEquals(paused.remaining, record.remaining)
        fx.manager.pauseAfterSection()
        fx.manager.pauseNow() // ends the run so that the next test starts clean
        fx.manager.state.await { it is GenState.Paused }
        Unit
    }

    @Test
    fun discardingForgetsTheRestButKeepsTheDeck() = runBlocking {
        val llm = fx.startGated()
        val paused = pausedAfterTheFirstChapter(llm)
        fx.manager.reset() // opening the screen again does not end a pause
        assertTrue(fx.manager.state.value is GenState.Paused)
        fx.manager.discard()
        assertEquals(GenState.Idle, fx.manager.state.value)
        withTimeout(10_000) { fx.store.cleared.receive() }
        assertNull(fx.store.load())
        assertEquals(1, fx.storage.export(paused.deckId!!).chapters.size)
    }

    @Test
    fun aJobThatCannotBeReadIsForgotten() = runBlocking {
        fx.store.saveBook(byteArrayOf(1, 2, 3)) // not an EPUB
        fx.store.saveRecord(JobRecord(null, 0, setOf(1), emptyList(), "en", 8))
        val restarted = fx.newManager()
        restarted.restore()
        withTimeout(10_000) { fx.store.cleared.receive() }
        assertEquals(GenState.Idle, restarted.state.value)
        assertNull(fx.store.load())
    }

    @Test
    fun aJobWithNothingLeftIsForgotten() = runBlocking {
        fx.store.saveBook(loremEpub())
        fx.store.saveRecord(JobRecord(1, 3, emptySet(), emptyList(), "en", 8))
        val restarted = fx.newManager()
        restarted.restore()
        withTimeout(10_000) { fx.store.cleared.receive() }
        assertEquals(GenState.Idle, restarted.state.value)
    }

    @Test
    fun nothingIsRestoredWhenNothingWasStored() = runBlocking {
        val restarted = fx.newManager()
        restarted.restore()
        assertEquals(GenState.Idle, restarted.state.value)
    }
}
