package dev.denlogv.lexislearned.ui.library

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BannerUiTest {
    private val book = EpubBook("Lorem", "en", emptyList())

    @Test
    fun aRunningJobShowsProgressAndCanBePaused() {
        val banner = GenState.Running(done = 3, total = 12, cards = 45, message = "Chapter", title = "Lorem").toBanner()!!
        assertEquals("Generating “Lorem”", banner.title)
        assertEquals("3 / 12 sections · 45 cards so far", banner.detail)
        assertEquals(0.25f, banner.progress!!, 0.001f)
        assertEquals("Pause", banner.pauseLabel)
        assertNull(banner.resumeLabel)
        assertNull(banner.deckId)
        assertFalse(banner.dismissible)
    }

    @Test
    fun aPauseThatWasAskedForCanBeMadeImmediate() {
        val banner = GenState.Running(3, 12, 45, "Chapter", "Lorem", pausing = true).toBanner()!!
        assertEquals("Pause now", banner.pauseLabel)
        assertTrue(banner.detail.endsWith("pausing after this section"))
    }

    @Test
    fun aJobWithoutSectionsHasEmptyProgress() {
        assertEquals(0f, GenState.Running(0, 0, 0, "").toBanner()!!.progress!!, 0f)
    }

    @Test
    fun aCompleteDeckOpensWhenTappedAndCanBeDismissed() {
        val banner = GenState.Finished(deckId = 7, cards = 1).toBanner()!!
        assertEquals("Deck ready", banner.title)
        assertEquals("1 card", banner.detail)
        assertEquals(7L, banner.deckId)
        assertTrue(banner.dismissible)
    }

    @Test
    fun aPausedGenerationCanBeResumedAndOpensTheGenerationScreen() {
        val banner = GenState.Paused(7, 12, book, setOf(3, 4)).toBanner()!!
        assertEquals("Generation paused", banner.title)
        assertEquals("12 cards · 2 sections left", banner.detail)
        assertEquals("Resume", banner.resumeLabel)
        assertNull(banner.pauseLabel)
        assertNull(banner.deckId) // the generation screen has the deck and the choice to discard
        assertFalse(banner.dismissible)
    }

    @Test
    fun aGenerationStoppedByAProblemOffersARetry() {
        val banner = GenState.Paused(null, 0, book, setOf(3), listOf("x (busy)")).toBanner()!!
        assertEquals("Generation stopped by a problem", banner.title)
        assertEquals("0 cards · 1 section left · 1 failed", banner.detail)
        assertEquals("Retry", banner.resumeLabel)
    }

    @Test
    fun aFailureShowsItsReason() {
        val banner = GenState.Failed("HTTP 401").toBanner()!!
        assertEquals("HTTP 401", banner.detail)
        assertTrue(banner.dismissible)
    }

    @Test
    fun otherStatesShowNothing() {
        assertNull(GenState.Idle.toBanner())
        assertNull(GenState.Loading.toBanner())
        assertNull(GenState.Ready(book).toBanner())
    }
}
