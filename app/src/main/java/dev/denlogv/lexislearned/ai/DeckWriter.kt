package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.domain.Chapter
import dev.denlogv.lexislearned.domain.Deck
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Stores a deck chapter by chapter while it is being generated, so the book and each finished chapter show up in the library
 * at once instead of after the last section.
 *
 * @param storage where the deck is stored.
 * @param deckId the deck to add to when continuing one that is already stored, or null to create a new deck with the first chapter.
 * @param cards the number of cards that deck already has.
 */
internal class DeckWriter(private val storage: DeckStorage, deckId: Long? = null, cards: Int = 0) {
    /** The database id of the deck, or null until the first chapter has been stored. */
    var deckId: Long? = deckId
        private set

    /** Number of cards in the deck so far. */
    var cards: Int = cards
        private set

    /**
     * Stores a chapter; the first one also creates the deck. A chapter is never half stored: even if the generation is stopped
     * right now, this call completes first.
     *
     * @param deck the deck's details; its chapters are ignored.
     * @param chapter the chapter to add after the ones stored so far.
     * @return the database id of the deck.
     */
    suspend fun add(deck: Deck, chapter: Chapter): Long = withContext(NonCancellable) {
        val id = deckId?.also { storage.appendChapter(it, chapter) } ?: storage.import(deck.copy(chapters = listOf(chapter)))
        deckId = id
        cards += chapter.cards.size
        id
    }
}
