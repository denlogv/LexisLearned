package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Chapter
import dev.denlogv.lexislearned.domain.Deck

/**
 * Stores whole decks and loads them back: importing a deck, adding chapters to a deck that is still being generated, and
 * exporting.
 *
 * @param db the database.
 */
class DeckStorage(db: AppDatabase) {
    private val importer = DeckImporter(db)
    private val exporter = DeckExporter(db)

    /**
     * Imports a deck; re-importing the same source deck replaces it but keeps study progress.
     *
     * @param deck the deck to store.
     * @return the database id of the stored deck.
     */
    suspend fun import(deck: Deck): Long = importer.import(deck)

    /**
     * Adds a chapter to the end of a stored deck, for decks that are generated chapter by chapter.
     *
     * @param deckId the database id of the deck, as returned by [import].
     * @param chapter the chapter; its part is created if the deck has no part with that id yet.
     */
    suspend fun appendChapter(deckId: Long, chapter: Chapter) = importer.append(deckId, chapter)

    /**
     * Loads a deck as a domain object, including progress.
     *
     * @param deckId the deck.
     * @return the deck.
     * @throws IllegalStateException if there is no such deck.
     */
    suspend fun export(deckId: Long): Deck = exporter.export(deckId)
}
