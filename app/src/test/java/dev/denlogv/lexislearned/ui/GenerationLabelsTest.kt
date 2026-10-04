package dev.denlogv.lexislearned.ui

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GenerationLabelsTest {
    private val book = EpubBook("Lorem", "en", emptyList())

    @Test
    fun aRunningGenerationIsSummarisedAndSaysWhenItIsPausing() {
        val running = GenState.Running(1, 4, 20, "x")
        assertEquals("1 / 4 sections · 20 cards so far", running.summary())
        assertEquals("1 / 4 sections · 20 cards so far · pausing after this section", running.copy(pausing = true).summary())
    }

    @Test
    fun thePauseThatWaitsForTheSectionIsOfferedOnlyUntilItWasAskedFor() {
        assertEquals("Pause after section", GenState.Running(0, 1, 0, "").pauseAfterSectionLabel())
        assertNull(GenState.Running(0, 1, 0, "", pausing = true).pauseAfterSectionLabel())
        assertEquals("Pause now", PAUSE_NOW_LABEL)
    }

    @Test
    fun aPlainPauseIsResumedAndAProblemIsRetried() {
        val plain = GenState.Paused(1, 3, book, setOf(2, 3))
        assertEquals("Generation paused", plain.headline())
        assertEquals("3 cards · 2 sections left", plain.summary())
        assertEquals("Resume", plain.resumeLabel())
        val problem = plain.copy(remaining = setOf(2), failed = listOf("a", "b"))
        assertEquals("Generation stopped by a problem", problem.headline())
        assertEquals("3 cards · 1 section left · 2 failed", problem.summary())
        assertEquals("Retry", problem.resumeLabel())
    }

    @Test
    fun theWarningBeforeDiscardingSaysWhatIsLostAndWhatStays() {
        val warning = GenState.Paused(1, 12, book, setOf(2, 3)).discardWarning()
        assertEquals(
            "2 sections left will not be generated, and the stored copy of “Lorem” is deleted. " +
                "The 12 cards generated so far stay in your library.",
            warning,
        )
        assertEquals(
            "1 section left will not be generated, and the stored copy of “Lorem” is deleted. Nothing was added to your library yet.",
            GenState.Paused(null, 0, book, setOf(1)).discardWarning(),
        )
    }
}
