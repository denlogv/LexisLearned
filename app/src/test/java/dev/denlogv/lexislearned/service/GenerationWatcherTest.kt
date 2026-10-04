package dev.denlogv.lexislearned.service

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ai.GenerationFixture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
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
class GenerationWatcherTest {
    @get:Rule val main = MainDispatcherRule()

    private val fx = GenerationFixture()
    private val shown = mutableListOf<GenerationNotice>()
    private val finished = Channel<GenerationNotice?>(Channel.UNLIMITED) // filled on the thread that changed the state
    private val watcher = GenerationWatcher(
        fx.manager,
        CoroutineScope(Dispatchers.Unconfined),
        object : GenerationWatcher.Host {
            override fun show(notice: GenerationNotice) {
                shown += notice
            }

            override fun finish(notice: GenerationNotice?) {
                finished.trySend(notice)
            }
        },
    )

    @Test
    fun theProgressIsShownWhileRunningAndTheEndIsReportedOnce() = runBlocking {
        val llm = fx.startGated()
        val book = fx.ready()
        watcher.start(PauseRequest.NONE)
        assertNull(finished.receive()) // nothing runs yet: the review of a book has nothing to tell
        fx.manager.start(book, book.defaultSelection, "en", 8)
        withTimeout(10_000) { llm.started.receive() }
        assertTrue(shown.isNotEmpty())
        assertEquals("Pause after section", shown.last().pauseAfterSectionLabel)
        assertEquals("Pause now", shown.last().pauseNowLabel)
        llm.release()
        llm.release()
        assertEquals("Deck ready", withTimeout(10_000) { finished.receive() }!!.title)
        watcher.stop()
    }

    @Test
    fun theButtonOfTheNotificationPausesTheGeneration() = runBlocking {
        val llm = fx.startGated()
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        withTimeout(10_000) { llm.started.receive() }
        watcher.start(PauseRequest.AFTER_SECTION)
        assertTrue((fx.manager.state.value as GenState.Running).pausing)
        llm.release()
        assertEquals("Generation paused", withTimeout(10_000) { finished.receive() }!!.title)
        watcher.stop()
    }

    @Test
    fun theNowButtonOfTheNotificationGivesUpTheRequestInFlight() = runBlocking {
        val llm = fx.startGated()
        val book = fx.ready()
        fx.manager.start(book, book.defaultSelection, "en", 8)
        withTimeout(10_000) { llm.started.receive() } // the request is never released: only giving it up can end the run
        watcher.start(PauseRequest.NOW)
        assertEquals("Generation paused", withTimeout(10_000) { finished.receive() }!!.title)
        watcher.stop()
    }

    @Test
    fun startingTwiceFollowsOnlyOnce() = runBlocking {
        watcher.start(PauseRequest.NONE)
        watcher.start(PauseRequest.NONE)
        assertNull(finished.receive())
        watcher.stop()
        assertTrue(finished.isEmpty) // one follower: the second start reported nothing more
    }
}
