package dev.denlogv.lexislearned.service

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationNoticeTest {
    private val book = EpubBook("Lorem", "en", emptyList())

    @Test
    fun aRunningGenerationShowsTheProgressAndCanBePaused() {
        val notice = GenerationNotice.of(GenState.Running(3, 12, 45, "x", "Lorem"))!!
        assertEquals("Generating “Lorem”", notice.title)
        assertEquals("3 / 12 sections · 45 cards so far", notice.text)
        assertEquals(25, notice.progressPercent)
        assertEquals("Pause after section", notice.pauseAfterSectionLabel)
        assertEquals("Pause now", notice.pauseNowLabel)
        assertTrue(notice.ongoing)
    }

    @Test
    fun theResultsAreNotOngoing() {
        val paused = GenerationNotice.of(GenState.Paused(1, 2, book, setOf(3)))!!
        assertEquals("Generation paused", paused.title)
        assertNull(paused.pauseAfterSectionLabel)
        assertNull(paused.pauseNowLabel)
        assertNull(paused.progressPercent)
        assertFalse(paused.ongoing)
        assertEquals("Deck ready", GenerationNotice.of(GenState.Finished(1, 2))!!.title)
        assertEquals("Deck generation failed", GenerationNotice.of(GenState.Failed("x"))!!.title)
    }

    @Test
    fun stateWithNothingToTellHasNoNotice() {
        assertNull(GenerationNotice.of(GenState.Idle))
        assertNull(GenerationNotice.of(GenState.Ready(book)))
    }

    @Test
    fun theStartingNoticeIsOngoing() {
        assertTrue(GenerationNotice.STARTING.ongoing)
    }
}
