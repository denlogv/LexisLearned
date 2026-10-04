package dev.denlogv.lexislearned.ui.library

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BannerUiTest {
    private val book = EpubBook("Lorem", "en", emptyList())

    @Test
    fun aRunningJobShowsProgressAndCanBeStopped() {
        val banner = GenState.Running(done = 3, total = 12, cards = 45, message = "Chapter", title = "Lorem").toBanner()!!
        assertEquals("Generating “Lorem”", banner.title)
        assertEquals("3 / 12 sections · 45 cards so far", banner.detail)
        assertEquals(0.25f, banner.progress!!, 0.001f)
        assertEquals(true, banner.running)
        assertNull(banner.deckId)
    }

    @Test
    fun aJobWithoutSectionsHasEmptyProgress() {
        assertEquals(0f, GenState.Running(0, 0, 0, "").toBanner()!!.progress!!, 0f)
    }

    @Test
    fun aCompleteDeckOpensWhenTappedAndCannotBeContinued() {
        val banner = GenState.Finished(deckId = 7, cards = 1, failed = emptyList(), book = book, unfinished = emptySet()).toBanner()!!
        assertEquals("Deck ready", banner.title)
        assertEquals("1 card", banner.detail)
        assertEquals(7L, banner.deckId)
        assertEquals(false, banner.running)
        assertEquals(false, banner.resumable)
    }

    @Test
    fun aDeckWithSectionsLeftCanBeContinued() {
        val banner = GenState.Finished(7, 12, listOf("x (busy)"), book, setOf(3, 4)).toBanner()!!
        assertEquals("Deck partly ready", banner.title)
        assertEquals("12 cards · 2 sections left · 1 failed", banner.detail)
        assertEquals(true, banner.resumable)
        assertEquals(false, GenState.Finished(7, 12, emptyList(), null, setOf(3)).toBanner()!!.resumable) // no book kept
    }

    @Test
    fun aFailureShowsItsReason() {
        val banner = GenState.Failed("HTTP 401").toBanner()!!
        assertEquals("HTTP 401", banner.detail)
        assertEquals(false, banner.running)
    }

    @Test
    fun otherStatesShowNothing() {
        assertNull(GenState.Idle.toBanner())
        assertNull(GenState.Loading.toBanner())
    }
}
