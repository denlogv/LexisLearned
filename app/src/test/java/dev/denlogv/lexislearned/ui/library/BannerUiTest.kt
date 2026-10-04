package dev.denlogv.lexislearned.ui.library

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        assertEquals("Pause after section", banner.pauseAfterSectionLabel)
        assertEquals("Pause now", banner.pauseNowLabel)
        assertTrue(banner.running)
        assertNull(banner.resumeLabel)
        assertNull(banner.deckId)
        assertFalse(banner.dismissible)
    }

    @Test
    fun aPauseThatWasAskedForLeavesOnlyThePauseNowButton() {
        val banner = GenState.Running(3, 12, 45, "Chapter", "Lorem", pausing = true).toBanner()!!
        assertNull(banner.pauseAfterSectionLabel)
        assertEquals("Pause now", banner.pauseNowLabel)
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
    fun aPausedGenerationIsHandledOnTheBanner() {
        val banner = GenState.Paused(7, 12, book, setOf(3, 4)).toBanner()!!
        assertEquals("Generation paused", banner.title)
        assertEquals("12 cards · 2 sections left", banner.detail)
        assertEquals("Resume", banner.resumeLabel)
        assertNull(banner.pauseAfterSectionLabel)
        assertNull(banner.pauseNowLabel)
        assertFalse(banner.running)
        assertNull(banner.deckId)
        assertTrue(banner.discardWarning!!.startsWith("2 sections left will not be generated"))
        assertFalse(banner.dismissible)
    }

    @Test
    fun theWarningOfAPausedGenerationWithoutAChapterSaysNothingWasAdded() {
        val banner = GenState.Paused(null, 0, book, setOf(1, 2)).toBanner()!!
        assertTrue(banner.discardWarning!!.endsWith("Nothing was added to your library yet."))
    }

    @Test
    fun aGenerationStoppedByAProblemOffersARetryAndKeepsTheProblemsForTheInfo() {
        val banner = GenState.Paused(null, 0, book, setOf(3), listOf("x (busy)")).toBanner()!!
        assertEquals("Generation stopped by a problem", banner.title)
        assertEquals("0 cards · 1 section left · 1 failed", banner.detail) // the problem itself is in the info
        assertTrue(banner.info.contains("• x (busy)"))
        assertEquals("Retry", banner.resumeLabel)
        assertNotNull(banner.discardWarning)
    }

    @Test
    fun onlyAFinishedDeckLeadsAnywhere() {
        assertNull(GenState.Running(0, 1, 0, "", deckId = 7).toBanner()!!.deckId) // even with a deck so far: the list below has it
        assertNull(GenState.Paused(7, 12, book, setOf(3)).toBanner()!!.deckId)
        assertNull(GenState.Failed("HTTP 401").toBanner()!!.deckId)
        assertEquals(7L, GenState.Finished(7, 1).toBanner()!!.deckId)
    }

    @Test
    fun everyBannerHasAnInfoThatExplainsIt() {
        val states = listOf(
            GenState.Running(0, 1, 0, ""),
            GenState.Paused(7, 12, book, setOf(3)),
            GenState.Finished(7, 1),
            GenState.Failed("HTTP 401"),
        )
        states.forEach { assertTrue(it.toString(), it.toBanner()!!.info.isNotBlank()) }
    }

    @Test
    fun theCrossDismissesAResultOrAFailureAndDiscardsTheRestOfAPausedGeneration() {
        val finished = GenState.Finished(7, 1).toBanner()!!
        assertTrue(finished.closable)
        assertEquals("Dismiss", finished.closeLabel)
        val failed = GenState.Failed("HTTP 401").toBanner()!!
        assertTrue(failed.closable)
        assertEquals("Dismiss", failed.closeLabel)
        val paused = GenState.Paused(7, 12, book, setOf(3)).toBanner()!!
        assertTrue(paused.closable)
        assertEquals("Discard the rest", paused.closeLabel)
    }

    @Test
    fun aRunningGenerationHasNoCross() {
        assertFalse(GenState.Running(0, 1, 0, "").toBanner()!!.closable)
    }

    @Test
    fun onlyAPausedGenerationCanBeDiscarded() {
        assertNull(GenState.Running(0, 1, 0, "").toBanner()!!.discardWarning)
        assertNull(GenState.Finished(7, 1).toBanner()!!.discardWarning)
        assertNull(GenState.Failed("HTTP 401").toBanner()!!.discardWarning)
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
