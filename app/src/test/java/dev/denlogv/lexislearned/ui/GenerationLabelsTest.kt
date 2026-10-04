package dev.denlogv.lexislearned.ui

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import org.junit.Assert.assertEquals
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
    fun theSecondPauseIsLabelledAsImmediate() {
        assertEquals("Pause", GenState.Running(0, 1, 0, "").pauseLabel())
        assertEquals("Pause now", GenState.Running(0, 1, 0, "", pausing = true).pauseLabel())
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
}
