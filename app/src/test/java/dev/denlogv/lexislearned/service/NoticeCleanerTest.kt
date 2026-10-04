package dev.denlogv.lexislearned.service

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

/** The notification outlives the service that posted it, so something else has to take it away when the generation is gone. */
class NoticeCleanerTest {
    private val book = EpubBook("Lorem", "en", emptyList())
    private val paused = GenState.Paused(7, 12, book, setOf(3))
    private val running = GenState.Running(0, 1, 12, "", "Lorem")

    // The states are set from the test thread and the cleaner follows them unconfined, so it has reacted to each one when the
    // assignment returns: no thread and no waiting is involved.
    private val state = MutableStateFlow<GenState>(GenState.Idle)
    private var cancelled = 0
    private val cleaner = NoticeCleaner(state, CoroutineScope(Dispatchers.Unconfined)) { cancelled++ }

    @Test
    fun discardingAPausedGenerationRemovesItsNotice() {
        state.value = paused
        cleaner.start()
        assertEquals(0, cancelled)
        state.value = GenState.Idle // the user discarded the rest
        assertEquals(1, cancelled)
        cleaner.stop()
    }

    @Test
    fun dismissingAResultRemovesItsNotice() {
        cleaner.start()
        state.value = GenState.Failed("The key was rejected")
        state.value = GenState.Idle
        assertEquals(1, cancelled)
        cleaner.stop()
    }

    @Test
    fun aNoticeThatIsLeftOverFromEarlierIsNotRemovedWhenTheAppStarts() {
        cleaner.start() // the state is Idle: nothing of this process was shown, and a paused run may be restored next
        assertEquals(0, cancelled)
        state.value = paused // restored from the disk
        assertEquals(0, cancelled)
        state.value = GenState.Idle // only now is there something to take away
        assertEquals(1, cancelled)
        cleaner.stop()
    }

    @Test
    fun resumingKeepsTheNoticeBecauseTheServiceReplacesIt() {
        state.value = paused
        cleaner.start()
        state.value = running
        state.value = paused
        assertEquals(0, cancelled)
        state.value = GenState.Idle
        assertEquals(1, cancelled)
        cleaner.stop()
    }

    @Test
    fun aStateWithNothingToTellNeverCancelsTwice() {
        state.value = paused
        cleaner.start()
        state.value = GenState.Idle
        state.value = GenState.Loading
        state.value = GenState.Ready(book)
        assertEquals(1, cancelled)
        cleaner.stop()
    }

    @Test
    fun startingTwiceFollowsTheStateOnlyOnce() {
        state.value = paused
        cleaner.start()
        cleaner.start()
        state.value = GenState.Idle
        assertEquals(1, cancelled)
        cleaner.stop()
    }
}
