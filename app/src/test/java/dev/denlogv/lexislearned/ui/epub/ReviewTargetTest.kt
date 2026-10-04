package dev.denlogv.lexislearned.ui.epub

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewTargetTest {
    private val book = EpubBook("Lorem", "en", emptyList())
    private val partial = GenState.Finished(deckId = 7, cards = 5, failed = emptyList(), book = book, unfinished = setOf(3, 4))

    @Test
    fun aFreshlyReadBookIsAlwaysReviewed() {
        assertEquals(ReviewTarget(book, null), reviewTargetOf(GenState.Ready(book), continuing = false))
        assertEquals(ReviewTarget(book, null), reviewTargetOf(GenState.Ready(book), continuing = true))
    }

    @Test
    fun theRestOfADeckIsReviewedOnlyWhenTheUserAsked() {
        assertNull(reviewTargetOf(partial, continuing = false))
        assertEquals(ReviewTarget(book, GenState.Continuation(7, setOf(3, 4))), reviewTargetOf(partial, continuing = true))
    }

    @Test
    fun thereIsNothingToReviewWithoutABookOrSectionsLeft() {
        assertNull(reviewTargetOf(partial.copy(book = null), continuing = true))
        assertNull(reviewTargetOf(partial.copy(unfinished = emptySet()), continuing = true))
        assertNull(reviewTargetOf(GenState.Idle, continuing = true))
        assertNull(reviewTargetOf(GenState.Running(0, 1, 0, ""), continuing = true))
    }
}
