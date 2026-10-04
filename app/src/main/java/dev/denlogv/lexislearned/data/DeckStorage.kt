package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Deck

/**
 * Stores whole decks and loads them back: importing a deck and exporting it.
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
     * Loads a deck as a domain object, including progress.
     *
     * @param deckId the deck.
     * @return the deck.
     * @throws IllegalStateException if there is no such deck.
     */
    suspend fun export(deckId: Long): Deck = exporter.export(deckId)
}
