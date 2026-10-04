package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.Provider
import dev.denlogv.lexislearned.loremEpub
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What the two pause buttons do while the titles are being translated, before any section has started. */
@RunWith(RobolectricTestRunner::class)
class GenerationPauseTest {
    @get:Rule val main = MainDispatcherRule()

    private val fx = GenerationFixture()
    private val titleStarted = CompletableDeferred<Unit>()
    private val calls = mutableListOf<String>()

    private val titleAnswer = CompletableDeferred<Unit>()

    /** A model whose title request answers only when [titleAnswer] is completed, and that answers every chapter at once. */
    private val llm = object : LlmClient {
        override suspend fun complete(system: String, user: String): String {
            calls += if (user.startsWith("Give")) "title" else "chapter"
            if (user.startsWith("Give")) {
                titleStarted.complete(Unit)
                titleAnswer.await()
                return "{}"
            }
            return GenerationFixture.CARDS_REPLY
        }
    }
    private val manager by lazy {
        GenerationManager(CoroutineScope(Dispatchers.Unconfined), fx.storage, fx.settings, fx.store) { _, _ -> llm }
    }

    private suspend fun startedAndWaitingForTheTitles(): GenState.Running {
        fx.settings.setApiKey(Provider.ANTHROPIC, "k")
        manager.load(loremEpub())
        val book = (manager.state.await { it is GenState.Ready } as GenState.Ready).book
        manager.start(book, book.defaultSelection, "en")
        withTimeout(10_000) { titleStarted.await() }
        return manager.state.value as GenState.Running
    }

    @Test
    fun pausingAfterTheSectionBeforeTheFirstSectionLetsTheFirstSectionFinish() = runBlocking {
        val running = startedAndWaitingForTheTitles()
        assertEquals("Starting…", running.message)
        manager.pauseAfterSection() // no section is running yet: the pause waits for the first one
        assertTrue((manager.state.value as GenState.Running).pausing)
        titleAnswer.complete(Unit)
        val paused = withTimeout(10_000) { manager.state.await { it is GenState.Paused } } as GenState.Paused
        assertEquals(setOf(3), paused.remaining) // the first section was done, the second never started
        assertEquals(2, paused.cards)
        assertEquals(listOf("title", "chapter"), calls)
    }

    @Test
    fun pausingNowBeforeTheFirstSectionGivesUpTheTitleRequest() = runBlocking {
        startedAndWaitingForTheTitles()
        manager.pauseNow()
        val paused = withTimeout(10_000) { manager.state.await { it is GenState.Paused } } as GenState.Paused
        assertEquals(setOf(2, 3), paused.remaining)
        assertNull(paused.deckId)
        assertFalse(fx.store.load()!!.record.remaining.isEmpty()) // and it can still be resumed after a restart
    }
}
