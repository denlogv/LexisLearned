package dev.denlogv.lexislearned.ui.epub

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook

/**
 * What the review screen is about: a book, and the deck its sections will be added to if the review is for continuing one.
 *
 * @property book the book to review.
 * @property continuation the deck that was stopped early and gets the new chapters, or null for a new deck.
 */
data class ReviewTarget(val book: EpubBook, val continuation: GenState.Continuation?)

/**
 * Works out what the review screen shows for the generation state.
 *
 * A freshly read book is always reviewed. The rest of a book is reviewed only when the user asked to continue, so a deck that was
 * stopped early stays as it is, with its result and its Continue button, until then.
 *
 * @param state the generation state.
 * @param continuing whether the user chose to add the rest of the book to a deck that was stopped early.
 * @return the book to review, or null if there is none to review.
 */
fun reviewTargetOf(state: GenState, continuing: Boolean): ReviewTarget? = when {
    state is GenState.Ready -> ReviewTarget(state.book, null)
    continuing && state is GenState.Finished && state.book != null && state.unfinished.isNotEmpty() ->
        ReviewTarget(state.book, GenState.Continuation(state.deckId, state.unfinished))
    else -> null
}
