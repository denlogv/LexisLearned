package dev.denlogv.lexislearned.ui

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationInfoTest {
    private val book = EpubBook("Lorem", "en", emptyList())

    @Test
    fun theInfoOfARunningGenerationNamesBothPauseButtonsAndSaysWhatEachCosts() {
        val info = GenState.Running(0, 3, 0, "").info()
        assertTrue(info.contains("“Pause after section” waits for the section in progress"))
        assertTrue(info.contains("or for the first one if none has started yet"))
        assertTrue(info.contains("“Pause now” gives the request in flight up, and the section is done again on resume"))
    }

    @Test
    fun theInfoOfAPlainPauseExplainsTheButtonsAndHasNoProblemList() {
        val info = GenState.Paused(7, 12, book, setOf(3)).info()
        assertTrue(info.contains("are in your library and are not generated or paid for again"))
        assertTrue(info.contains("Resume does only the sections that are left"))
        assertTrue(info.contains("Choose sections"))
        assertTrue(info.contains("the cross in the corner discards the rest"))
        assertFalse(info.contains("What went wrong"))
    }

    @Test
    fun theInfoAfterAProblemListsTheProblemsAndCallsTheButtonRetry() {
        val info = GenState.Paused(7, 12, book, setOf(3), listOf("Chapter Two (busy)", "Chapter Three (busy)")).info()
        assertTrue(info.contains("Retry does only the sections that are left"))
        assertTrue(info.contains("What went wrong:\n• Chapter Two (busy)\n• Chapter Three (busy)\n\nCheck the API key"))
    }

    @Test
    fun onlyTheFirstFewProblemsAreNamed() {
        val info = GenState.Paused(null, 0, book, setOf(1), List(8) { "section $it" }).info()
        assertTrue(info.contains("• section 4"))
        assertFalse(info.contains("• section 5"))
        assertTrue(info.contains("… and 3 more"))
    }

    @Test
    fun theInfoOfAFinishedDeckSaysHowToOpenAndDismissIt() {
        assertEquals(
            "The deck, with 12 cards, is in your library. Tap the card to open it, or the cross to dismiss it.",
            GenState.Finished(7, 12).info(),
        )
    }

    @Test
    fun theInfoOfAFailureRepeatsItAndSaysWhatToCheck() {
        val info = GenState.Failed("The API key was rejected").info()
        assertTrue(info.startsWith("Nothing was added to your library. The API key was rejected"))
        assertTrue(info.contains("settings (the gear icon)"))
    }
}
