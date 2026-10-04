package dev.denlogv.lexislearned.ui.library

import dev.denlogv.lexislearned.ai.GenState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BannerUiTest {
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
    fun aFinishedDeckOpensWhenTapped() {
        val banner = GenState.Finished(deckId = 7, cards = 1, failed = emptyList()).toBanner()!!
        assertEquals("1 card", banner.detail)
        assertEquals(7L, banner.deckId)
        assertEquals(false, banner.running)
        assertEquals("1 card · 2 section(s) skipped", GenState.Finished(7, 1, listOf("a", "b")).toBanner()!!.detail)
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
