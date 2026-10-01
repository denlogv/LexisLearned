package dev.denlogv.lexislearned.ui

import dev.denlogv.lexislearned.domain.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Test

class LabelsTest {
    @Test
    fun pluralUsesSingularOnlyForOne() {
        assertEquals("1 word", plural(1, "word"))
        assertEquals("0 words", plural(0, "word"))
        assertEquals("2 boxes", plural(2, "box", "boxes"))
    }

    @Test
    fun progressLabelLeavesOutZeroParts() {
        assertEquals("3/16 completed · 5 in progress · 33% · 2 due", progressLabel(3, 16, 5, 0.333f, 2))
        assertEquals("0/4 completed · 0%", progressLabel(0, 4, 0, 0f, 0))
        assertEquals("2 chapters · 1/2 completed · 50%", progressLabel(1, 2, 0, 0.5f, 0, "2 chapters"))
    }

    @Test
    fun studyButtonMentionsDueWords() {
        assertEquals("Study book (3 due + new)", studyButtonLabel("book", 3))
        assertEquals("Study chapter (new cards)", studyButtonLabel("chapter", 0))
    }

    @Test
    fun everyModeHasANameAndADescription() {
        StudyMode.entries.forEach {
            assertEquals(false, it.label().isBlank())
            assertEquals(false, it.description().isBlank())
        }
        assertEquals("Learn", StudyMode.LEARN.label())
    }

    @Test
    fun routesRoundTripThePatterns() {
        assertEquals("deck/7", Routes.deck(7))
        assertEquals("part/8", Routes.part(8))
        assertEquals("chapter/9", Routes.chapter(9))
        assertEquals("study/1/-1/-1", Routes.study(1))
        assertEquals("study/1/2/3", Routes.study(1, 2, 3))
    }
}
