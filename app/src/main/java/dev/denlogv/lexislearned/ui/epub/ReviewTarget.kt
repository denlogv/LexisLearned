package dev.denlogv.lexislearned.ui.epub

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook

/**
 * What the review screen is about: a book, and the deck its sections will be added to if the review is for continuing one.
 *
 * @property book the book to review.
 * @property continuation the deck of a paused generation that gets the new chapters, or null for a new deck.
 */
data class ReviewTarget(val book: EpubBook, val continuation: GenState.Continuation?)

/**
 * Works out what the review screen shows for the generation state.
 *
 * A freshly read book is always reviewed. The rest of a book is reviewed only when the user asked to choose the sections, so a paused
 * generation stays as it is, with its Resume button, until then.
 *
 * @param state the generation state.
 * @param continuing whether the user chose to pick the sections of a paused generation.
 * @return the book to review, or null if there is none to review.
 */
fun reviewTargetOf(state: GenState, continuing: Boolean): ReviewTarget? = when {
    state is GenState.Ready -> ReviewTarget(state.book, null)
    continuing && state is GenState.Paused -> ReviewTarget(state.book, GenState.Continuation(state.deckId, state.remaining))
    else -> null
}
