package dev.denlogv.lexislearned.ui.epub

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewTargetTest {
    private val book = EpubBook("Lorem", "en", emptyList())
    private val paused = GenState.Paused(deckId = 7, cards = 5, book = book, remaining = setOf(3, 4))

    @Test
    fun aFreshlyReadBookIsAlwaysReviewed() {
        assertEquals(ReviewTarget(book, null), reviewTargetOf(GenState.Ready(book), continuing = false))
        assertEquals(ReviewTarget(book, null), reviewTargetOf(GenState.Ready(book), continuing = true))
    }

    @Test
    fun theSectionsOfAPausedGenerationAreReviewedOnlyWhenTheUserAsked() {
        assertNull(reviewTargetOf(paused, continuing = false))
        assertEquals(ReviewTarget(book, GenState.Continuation(7, setOf(3, 4))), reviewTargetOf(paused, continuing = true))
    }

    @Test
    fun aPausedGenerationWithoutADeckIsReviewedForANewDeck() {
        val target = reviewTargetOf(paused.copy(deckId = null), continuing = true)
        assertEquals(ReviewTarget(book, GenState.Continuation(null, setOf(3, 4))), target)
    }

    @Test
    fun thereIsNothingToReviewInOtherStates() {
        assertNull(reviewTargetOf(GenState.Idle, continuing = true))
        assertNull(reviewTargetOf(GenState.Running(0, 1, 0, ""), continuing = true))
        assertNull(reviewTargetOf(GenState.Finished(7, 5), continuing = true))
    }
}
